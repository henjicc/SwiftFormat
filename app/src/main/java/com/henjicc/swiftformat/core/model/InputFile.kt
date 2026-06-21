package com.henjicc.swiftformat.core.model

import android.net.Uri

/**
 * 已选输入文件（见 SPEC 11.4）。以 [uri] 为核心，不依赖绝对路径。
 * [id] 取自 uri 字符串，便于去重与列表稳定 key。
 */
data class InputFile(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val extension: String?,
    val sizeBytes: Long?,
    val mediaType: MediaType,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
)
