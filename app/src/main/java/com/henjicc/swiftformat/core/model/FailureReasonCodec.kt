package com.henjicc.swiftformat.core.model

/**
 * 把失败原因压缩进单个字符串字段里，兼容当前 Room schema 不变的前提下，
 * 仍能同时保存用户态错误种类与技术详情。
 */
object FailureReasonCodec {

    private const val KIND_PREFIX = "KIND::"
    private const val DETAIL_PREFIX = "DETAIL::"

    fun encode(kind: ConversionError.Kind, details: String?): String = buildString {
        append(KIND_PREFIX).appendLine(kind.name)
        if (!details.isNullOrBlank()) {
            append(DETAIL_PREFIX).append(details)
        }
    }

    fun decode(encoded: String?): DecodedFailureReason? {
        if (encoded.isNullOrBlank()) return null
        val firstLine = encoded.lineSequence().firstOrNull()
        val kind = firstLine
            ?.removePrefix(KIND_PREFIX)
            ?.takeIf { it != firstLine }
            ?.let { runCatching { ConversionError.Kind.valueOf(it) }.getOrNull() }
        val details = encoded.substringAfter(DETAIL_PREFIX, "").takeIf { it.isNotBlank() }
        return DecodedFailureReason(kind = kind, details = details, raw = encoded)
    }
}

data class DecodedFailureReason(
    val kind: ConversionError.Kind?,
    val details: String?,
    val raw: String,
)
