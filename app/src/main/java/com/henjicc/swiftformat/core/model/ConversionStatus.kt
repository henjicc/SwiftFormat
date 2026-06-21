package com.henjicc.swiftformat.core.model

/** 任务状态机（见 SPEC 4.5）。 */
enum class ConversionStatus {
    PENDING,
    PREPARING,
    CONVERTING,
    SAVING,
    COMPLETED,
    CANCELLED,
    FAILED,
}
