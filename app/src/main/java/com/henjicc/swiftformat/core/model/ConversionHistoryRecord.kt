package com.henjicc.swiftformat.core.model

import android.net.Uri

/**
 * 转换历史记录的领域模型（见 SPEC 14）。不依赖 Room，持久化细节由 `core/database` 负责，
 * 便于 ViewModel/UI 不感知存储实现。
 */
data class ConversionHistoryRecord(
    val id: Long = 0,
    val originalDisplayName: String,
    val originalFormat: String?,
    val outputFormat: String,
    val mediaType: MediaType,
    val inputUri: Uri,
    val startTime: Long,
    val endTime: Long?,
    val status: ConversionStatus,
    val outputUri: Uri?,
    val outputSizeBytes: Long?,
    val failureReason: String?,
    val quality: QualityPreset?,
    val size: SizePreset?,
)
