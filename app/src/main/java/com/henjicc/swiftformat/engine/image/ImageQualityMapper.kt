package com.henjicc.swiftformat.engine.image

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning.forPresetOrStandard

/**
 * 图片质量档位 → JPEG/有损 WebP 压缩质量值（见 SPEC 5.3）。
 * 纯函数，不依赖 Android Bitmap API，便于单元测试。具体数值见 [QualityPresetTuning]。
 */
object ImageQualityMapper {
    fun compressQuality(preset: QualityPreset): Int =
        QualityPresetTuning.imageCompressQuality.forPresetOrStandard(preset)
}
