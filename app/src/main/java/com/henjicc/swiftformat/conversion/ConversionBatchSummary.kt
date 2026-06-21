package com.henjicc.swiftformat.conversion

import com.henjicc.swiftformat.core.model.ConversionStatus

/** 批量转换汇总（见 SPEC 4.5「已完成数量 / 总数量」）。纯函数，便于单元测试。 */
data class ConversionBatchSummary(
    val total: Int,
    val completed: Int,
    val failed: Int,
    val cancelled: Int,
    val inProgress: Int,
) {
    companion object {
        fun from(tasks: Collection<ConversionTask>): ConversionBatchSummary {
            val total = tasks.size
            val completed = tasks.count { it.status == ConversionStatus.COMPLETED }
            val failed = tasks.count { it.status == ConversionStatus.FAILED }
            val cancelled = tasks.count { it.status == ConversionStatus.CANCELLED }
            val inProgress = total - completed - failed - cancelled
            return ConversionBatchSummary(total, completed, failed, cancelled, inProgress)
        }
    }
}
