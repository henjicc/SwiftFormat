package com.henjicc.swiftformat.conversion

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.henjicc.swiftformat.core.file.OutputMimeTypes
import com.henjicc.swiftformat.core.file.OutputNaming
import com.henjicc.swiftformat.core.model.MediaType
import java.io.File

/**
 * 输出位置解析（见 SPEC 12.3/12.4）：统一写入 `Download/转个格式`（不按媒体类型拆分 MediaStore 分类，
 * 换取"查看位置"对所有类型一致、避免 Pictures/Movies/Music 各分类 RELATIVE_PATH 校验细节差异；
 * 代价是转换后的图片/视频不会出现在系统相册/视频 App，见已知简化）。
 *
 * 调用方必须用同一个 [OutputLocationResolver] 实例并发调用时自行加锁去重——本类只负责单次解析，
 * 同批次并发提交时的命名冲突由 [com.henjicc.swiftformat.conversion.ConversionOrchestrator] 用 Mutex 序列化。
 */
class OutputLocationResolver(context: Context) {

    private val appContext = context.applicationContext

    fun resolve(originalDisplayName: String, outputFormat: String, targetMediaType: MediaType): Uri {
        require(targetMediaType != MediaType.UNKNOWN) { "cannot resolve output location for UNKNOWN media type" }

        val desiredName = OutputNaming.withExtension(originalDisplayName, outputFormat)
        val existingNames = queryExistingNames()
        val finalName = OutputNaming.resolveCollision(desiredName, existingNames)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
            put(MediaStore.MediaColumns.MIME_TYPE, OutputMimeTypes.forFormat(outputFormat))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            } else {
                put(MediaStore.MediaColumns.DATA, File(legacyDownloadsDir(), finalName).absolutePath)
            }
        }
        return appContext.contentResolver.insert(targetCollection(), values)
            ?: error("MediaStore insert failed for $finalName")
    }

    @Suppress("DEPRECATION")
    private fun queryExistingNames(): Set<String> {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val (selection, selectionArgs) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?" to arrayOf(RELATIVE_PATH)
        } else {
            "${MediaStore.MediaColumns.DATA} LIKE ?" to arrayOf("${legacyDownloadsDir().absolutePath}%")
        }
        val names = mutableSetOf<String>()
        appContext.contentResolver.query(
            targetCollection(),
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                names.add(cursor.getString(nameColumn))
            }
        }
        return names
    }

    @Suppress("DEPRECATION")
    private fun targetCollection(): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Files.getContentUri("external")
    }

    @Suppress("DEPRECATION")
    private fun legacyDownloadsDir(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APP_DIRECTORY)
            .apply { mkdirs() }

    private companion object {
        const val APP_DIRECTORY = "转个格式"
        val RELATIVE_PATH = "${Environment.DIRECTORY_DOWNLOADS}/$APP_DIRECTORY/"
    }
}
