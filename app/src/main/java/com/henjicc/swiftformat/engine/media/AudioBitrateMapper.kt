package com.henjicc.swiftformat.engine.media

import com.henjicc.swiftformat.core.model.QualityPreset

/**
 * 音频质量档位 → 目标码率（见 SPEC 5.5）。纯函数，便于单元测试。
 *
 * 本任务（Media3Engine）只处理 AAC/M4A 输出；WAV/MP3/FLAC/Opus 等留给 FFmpeg 引擎。
 * 无损格式会在对应引擎里隐藏质量档位；本对象只提供 AAC/M4A 所需码率。
 * AAC 在相同听感下可用更低码率（见 SPEC 5.5 说明），档位略低于 SPEC 5.5 给出的 MP3 参考表。
 */
object AudioBitrateMapper {

    private val aacBitrateBps = mapOf(
        QualityPreset.BEST to 256_000,
        QualityPreset.HIGH to 192_000,
        QualityPreset.STANDARD to 128_000,
        QualityPreset.SMALL_SIZE to 96_000,
    )

    fun targetBitrateBps(outputFormat: String, preset: QualityPreset): Int? {
        if (outputFormat.equals("WAV", ignoreCase = true)) return null
        return aacBitrateBps[preset] ?: aacBitrateBps.getValue(QualityPreset.HIGH)
    }
}
