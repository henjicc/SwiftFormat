package com.henjicc.swiftformat.engine.ffmpeg

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning.forPresetOrStandard

/**
 * Opus 质量档位 → 目标码率。Opus 编码效率高于 MP3，取值略低于 MP3 档位。数值见 [QualityPresetTuning]。
 */
object OpusBitrateMapper {

    fun targetBitrateBps(preset: QualityPreset): Int =
        QualityPresetTuning.opusBitrateBps.forPresetOrStandard(preset)
}
