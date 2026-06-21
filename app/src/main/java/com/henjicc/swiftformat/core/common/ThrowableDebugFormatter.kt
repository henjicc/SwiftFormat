package com.henjicc.swiftformat.core.common

/**
 * 把 Throwable 整理成更适合展示/持久化的文本：
 * - 顶层异常类型 + message
 * - cause 链
 * - 每层前几行关键栈帧
 *
 * 用于“查看详情”场景，避免只剩 `Throwable.message` 导致看不到真正的底层原因
 * （例如 `Error("FFmpegKit failed to start...", cause = UnsatisfiedLinkError(...))`）。
 */
fun Throwable.toDebugMessage(
    maxCauseDepth: Int = 6,
    maxFramesPerCause: Int = 4,
): String = buildString {
    var current: Throwable? = this@toDebugMessage
    var depth = 0
    while (current != null && depth < maxCauseDepth) {
        if (depth == 0) {
            append("Exception: ")
        } else {
            appendLine()
            append("Caused by: ")
        }
        append(current.javaClass.name)
        current.message?.takeIf { it.isNotBlank() }?.let {
            append(": ")
            append(it)
        }
        current.stackTrace.take(maxFramesPerCause).forEach { frame ->
            appendLine()
            append("  at ")
            append(frame.toString())
        }
        current = current.cause
        depth += 1
    }
}
