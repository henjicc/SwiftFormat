package com.henjicc.swiftformat.core.file

/**
 * 输出格式 → MIME 类型，供写入 MediaStore 时填充元数据（见 SPEC 12.3）。纯函数，便于单元测试。
 * 与 [MediaTypeResolver] 方向相反：[MediaTypeResolver] 从已有文件推断类型，本对象从目标输出格式推断 MIME。
 */
object OutputMimeTypes {
    fun forFormat(outputFormat: String): String = when (outputFormat.uppercase()) {
        "JPG", "JPEG" -> "image/jpeg"
        "PNG" -> "image/png"
        "WEBP" -> "image/webp"
        "MP4" -> "video/mp4"
        "WEBM" -> "video/webm"
        "MP3" -> "audio/mpeg"
        "AAC", "M4A" -> "audio/aac"
        "WAV" -> "audio/wav"
        "FLAC" -> "audio/flac"
        else -> "application/octet-stream"
    }
}
