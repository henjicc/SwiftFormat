package com.henjicc.swiftformat.engine.ffmpeg

import com.henjicc.swiftformat.core.model.QualityPreset

/**
 * Opus 质量档位 → 目标码率。Opus 编码效率高于 MP3，取值略低于 MP3 档位。
 */
object OpusBitrateMapper {

    private val bitrateBps = mapOf(
        QualityPreset.BEST to 192_000,
        QualityPreset.HIGH to 160_000,
        QualityPreset.STANDARD to 128_000,
        QualityPreset.SMALL_SIZE to 96_000,
    )

    fun targetBitrateBps(preset: QualityPreset): Int =
        bitrateBps[preset] ?: bitrateBps.getValue(QualityPreset.HIGH)
}
