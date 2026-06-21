package com.henjicc.swiftformat.core.file

import com.henjicc.swiftformat.core.model.MediaType

/**
 * 媒体类型识别：优先用 MIME 大类，回退到扩展名（见 SPEC 3.1 / 18 自动识别）。
 * 纯函数、无 Android 依赖，便于单元测试。无法判定时返回 [MediaType.UNKNOWN]（按不支持处理）。
 */
object MediaTypeResolver {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif", "avif")
    private val videoExtensions = setOf("mp4", "mov", "mkv", "webm", "avi", "m4v", "3gp", "ts", "flv")
    private val audioExtensions = setOf("mp3", "aac", "m4a", "wav", "flac", "ogg", "opus", "oga", "amr", "wma")

    fun resolve(mimeType: String?, fileName: String?): MediaType {
        when {
            mimeType == null -> Unit
            mimeType.startsWith("image/", ignoreCase = true) -> return MediaType.IMAGE
            mimeType.startsWith("video/", ignoreCase = true) -> return MediaType.VIDEO
            mimeType.startsWith("audio/", ignoreCase = true) -> return MediaType.AUDIO
        }
        return when (extensionOf(fileName)) {
            in imageExtensions -> MediaType.IMAGE
            in videoExtensions -> MediaType.VIDEO
            in audioExtensions -> MediaType.AUDIO
            else -> MediaType.UNKNOWN
        }
    }

    /** 提取小写扩展名（不含点），无扩展名返回空串。 */
    fun extensionOf(fileName: String?): String {
        if (fileName.isNullOrBlank()) return ""
        val dot = fileName.lastIndexOf('.')
        if (dot < 0 || dot == fileName.lastIndex) return ""
        return fileName.substring(dot + 1).lowercase()
    }
}
