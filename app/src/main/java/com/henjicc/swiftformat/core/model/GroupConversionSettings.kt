package com.henjicc.swiftformat.core.model

/**
 * 单个媒体分组（视频/图片/音频）的转换设置（见 SPEC 11.5）。
 * UI 仅持有这三个统一概念，底层引擎负责映射为真实参数。
 */
data class GroupConversionSettings(
    val mediaType: MediaType,
    val outputFormat: String,
    val targetMediaType: MediaType,
    val quality: QualityPreset?,
    val size: SizePreset?,
)
