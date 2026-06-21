package com.henjicc.swiftformat.conversion

import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.ConversionStatus

/**
 * 单个转换任务的运行态（见 SPEC 4.5）。由 [ConversionOrchestrator] 维护并通过 [ConversionOrchestrator.tasks]
 * 暴露给 UI；[historyId] 关联到同一任务在 Room 中的历史记录，便于完成/失败时回写。
 */
data class ConversionTask(
    val request: ConversionRequest,
    val historyId: Long,
    val status: ConversionStatus,
    val progress: Float = 0f,
    val error: ConversionError? = null,
)
