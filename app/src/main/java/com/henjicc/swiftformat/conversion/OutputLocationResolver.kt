package com.henjicc.swiftformat.conversion

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.henjicc.swiftformat.core.datastore.SettingsRepository
import com.henjicc.swiftformat.core.file.OutputMimeTypes
import com.henjicc.swiftformat.core.file.OutputNaming
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.NameCollisionStrategy
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * 输出位置解析（见 SPEC 12.3/12.4）：默认统一写入 `Download/转个格式`（不按媒体类型拆分 MediaStore 分类，
 * 换取"查看位置"对所有类型一致、避免 Pictures/Movies/Music 各分类 RELATIVE_PATH 校验细节差异；
 * 代价是转换后的图片/视频不会出现在系统相册/视频 App，见已知简化）。用户可在设置中指定自定义目录
 * （SAF 树 Uri），此时改用 [DocumentsContract] 在该目录下创建文件，不再经过 MediaStore。
 *
 * 调用方必须用同一个 [OutputLocationResolver] 实例并发调用时自行加锁去重——本类只负责单次解析，
 * 同批次并发提交时的命名冲突由 [com.henjicc.swiftformat.conversion.ConversionOrchestrator] 用 Mutex 序列化。
 */
class OutputLocationResolver(
    context: Context,
    private val settingsRepository: SettingsRepository,
) {

    private val appContext = context.applicationContext

    suspend fun resolve(originalDisplayName: String, outputFormat: String, targetMediaType: MediaType): Uri {
        require(targetMediaType != MediaType.UNKNOWN) { "cannot resolve output location for UNKNOWN media type" }

        val settings = settingsRepository.settings.first()
        val desiredName = OutputNaming.withExtension(originalDisplayName, outputFormat)
        val treeUri = settings.customOutputDirectoryUri?.let(Uri::parse)
        return if (treeUri != null) {
            resolveInCustomDirectory(treeUri, desiredName, outputFormat, settings.nameCollisionStrategy)
        } else {
            resolveInDefaultDirectory(desiredName, outputFormat, settings.nameCollisionStrategy)
        }
    }

    private fun resolveInCustomDirectory(
        treeUri: Uri,
        desiredName: String,
        outputFormat: String,
        strategy: NameCollisionStrategy,
    ): Uri {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)

        val existingDocumentIdsByName = mutableMapOf<String, String>()
        appContext.contentResolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                existingDocumentIdsByName[cursor.getString(nameIndex)] = cursor.getString(idIndex)
            }
        }

        val finalName = when (strategy) {
            NameCollisionStrategy.AUTO_NUMBER -> OutputNaming.resolveCollision(desiredName, existingDocumentIdsByName.keys)
            NameCollisionStrategy.OVERWRITE -> {
                existingDocumentIdsByName[desiredName]?.let { documentId ->
                    runCatching {
                        DocumentsContract.deleteDocument(
                            appContext.contentResolver,
                            DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                        )
                    }
                }
                desiredName
            }
        }

        return DocumentsContract.createDocument(
            appContext.contentResolver,
            parentDocumentUri,
            OutputMimeTypes.forFormat(outputFormat),
            finalName,
        ) ?: error("SAF createDocument failed for $finalName")
    }

    private fun resolveInDefaultDirectory(
        desiredName: String,
        outputFormat: String,
        strategy: NameCollisionStrategy,
    ): Uri {
        val finalName = when (strategy) {
            NameCollisionStrategy.AUTO_NUMBER -> OutputNaming.resolveCollision(desiredName, queryExistingNames())
            NameCollisionStrategy.OVERWRITE -> {
                deleteExistingByName(desiredName)
                desiredName
            }
        }

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
    private fun deleteExistingByName(name: String) {
        val (selection, selectionArgs) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?" to
                arrayOf(RELATIVE_PATH, name)
        } else {
            "${MediaStore.MediaColumns.DATA} = ?" to arrayOf(File(legacyDownloadsDir(), name).absolutePath)
        }
        runCatching { appContext.contentResolver.delete(targetCollection(), selection, selectionArgs) }
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
