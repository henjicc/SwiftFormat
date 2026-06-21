package com.henjicc.swiftformat.core.file

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 读取 Uri 的文件元数据（见 SPEC 6.4 / 18.1）。所有 I/O 在 [Dispatchers.IO] 执行，不阻塞主线程。
 * 失败时尽量降级（仍返回可用的基本信息），不抛出以免中断批量导入。
 */
class FileMetadataReader(
    context: Context,
    private val logger: Logger,
) {
    private val appContext = context.applicationContext
    private val resolver get() = appContext.contentResolver

    suspend fun read(uri: Uri): InputFile = withContext(Dispatchers.IO) {
        val (displayName, sizeBytes) = queryNameAndSize(uri)
        val mimeType = runCatching { resolver.getType(uri) }.getOrNull()
        val extension = MediaTypeResolver.extensionOf(displayName).ifEmpty { null }
        val mediaType = MediaTypeResolver.resolve(mimeType, displayName)

        var width: Int? = null
        var height: Int? = null
        var durationMs: Long? = null
        when (mediaType) {
            MediaType.IMAGE -> readImageBounds(uri)?.let { (w, h) -> width = w; height = h }
            MediaType.VIDEO, MediaType.AUDIO -> readAvMetadata(uri)?.let {
                width = it.width; height = it.height; durationMs = it.durationMs
            }

            MediaType.UNKNOWN -> Unit
        }

        InputFile(
            id = uri.toString(),
            uri = uri,
            displayName = displayName ?: uri.lastPathSegment.orEmpty(),
            mimeType = mimeType,
            extension = extension,
            sizeBytes = sizeBytes,
            mediaType = mediaType,
            width = width,
            height = height,
            durationMs = durationMs,
        )
    }

    private fun queryNameAndSize(uri: Uri): Pair<String?, Long?> {
        var name: String? = null
        var size: Long? = null
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && !cursor.isNull(nameIdx)) name = cursor.getString(nameIdx)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
        }.onFailure { logger.w(TAG, "queryNameAndSize failed", it) }
        return name to size
    }

    private fun readImageBounds(uri: Uri): Pair<Int, Int>? = runCatching {
        decodeImageBounds(appContext, uri, logger, TAG)?.let { it.width to it.height }
    }.onFailure { logger.w(TAG, "readImageBounds failed", it) }.getOrNull()

    private data class AvMetadata(val width: Int?, val height: Int?, val durationMs: Long?)

    private fun readAvMetadata(uri: Uri): AvMetadata? = runCatching {
        // MediaMetadataRetriever 自 API 29 才实现 AutoCloseable，minSdk 26 下手动 release。
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, uri)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
            AvMetadata(width, height, duration)
        } finally {
            retriever.release()
        }
    }.onFailure { logger.w(TAG, "readAvMetadata failed", it) }.getOrNull()

    private companion object {
        const val TAG = "FileMetadataReader"
    }
}
