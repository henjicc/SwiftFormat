package com.henjicc.swiftformat.core.model

/**
 * 统一转换错误模型（见 SPEC 17）。
 * 用于向用户展示可理解原因，技术细节放在 [debugMessage] 供「查看详情」使用，不直接展示堆栈。
 */
data class ConversionError(
    val kind: Kind,
    val debugMessage: String? = null,
    val cause: Throwable? = null,
) {
    enum class Kind {
        FILE_NOT_FOUND,
        PERMISSION_DENIED,
        UNSUPPORTED_FORMAT,
        CORRUPT_INPUT,
        NO_ENCODER,
        UNSUPPORTED_OUTPUT,
        INSUFFICIENT_STORAGE,
        OUTPUT_NOT_WRITABLE,
        ENGINE_CRASH,
        CANCELLED,
        UNKNOWN,
    }
}
