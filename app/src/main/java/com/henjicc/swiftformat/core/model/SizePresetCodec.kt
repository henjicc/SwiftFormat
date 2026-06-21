package com.henjicc.swiftformat.core.model

/**
 * [SizePreset] 与紧凑字符串的互转（见 SPEC 14：历史记录需保存使用的尺寸档位）。
 * 纯函数，不依赖 Room/Android，供 `core/database` 的 Entity 映射复用。
 */
object SizePresetCodec {

    fun encode(size: SizePreset?): String? = when (size) {
        null -> null
        SizePreset.Original -> "ORIGINAL"
        is SizePreset.VideoResolution -> "VIDEO:${size.height}"
        is SizePreset.ImageLongestEdge -> "IMAGE:${size.pixels}"
        is SizePreset.Custom -> "CUSTOM:${size.width ?: ""}x${size.height ?: ""}"
    }

    fun decode(encoded: String?): SizePreset? {
        if (encoded == null) return null
        return when {
            encoded == "ORIGINAL" -> SizePreset.Original
            encoded.startsWith("VIDEO:") -> SizePreset.VideoResolution(encoded.removePrefix("VIDEO:").toInt())
            encoded.startsWith("IMAGE:") -> SizePreset.ImageLongestEdge(encoded.removePrefix("IMAGE:").toInt())
            encoded.startsWith("CUSTOM:") -> {
                val (widthPart, heightPart) = encoded.removePrefix("CUSTOM:").split("x", limit = 2)
                SizePreset.Custom(widthPart.toIntOrNull(), heightPart.toIntOrNull())
            }

            else -> null
        }
    }
}
