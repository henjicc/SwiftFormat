package com.henjicc.swiftformat.engine.ffmpeg

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning.forPresetOrStandard

/**
 * 图片质量档位 → libaom-av1 CRF 值（0-63，越小质量越高）。
 * 纯函数，不依赖 Android Bitmap API，便于单元测试。具体数值见 [QualityPresetTuning]。
 */
object AvifCrfMapper {
    fun targetCrf(preset: QualityPreset): Int =
        QualityPresetTuning.avifCrf.forPresetOrStandard(preset)
}
