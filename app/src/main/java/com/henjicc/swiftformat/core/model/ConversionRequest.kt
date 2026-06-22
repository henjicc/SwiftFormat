package com.henjicc.swiftformat.core.model

import android.net.Uri

/**
 * 单文件转换请求（见 SPEC 11.6）。引擎只通过本对象与上层通信，不感知文件选择/分组等业务细节。
 */
data class ConversionRequest(
    val id: String,
    val input: InputFile,
    val outputFormat: String,
    val targetMediaType: MediaType,
    val quality: QualityPreset?,
    val size: SizePreset?,
    val destination: OutputDestination,
    val preserveMetadata: Boolean = false,
)

/**
 * 输出位置，已由上层（SAF/MediaStore，见 SPEC 12.3）解析为具体 Uri。
 * 引擎不关心 Uri 来源，只负责写入，保持与存储策略解耦。
 */
sealed interface OutputDestination {
    data class ResolvedUri(val uri: Uri) : OutputDestination
}
