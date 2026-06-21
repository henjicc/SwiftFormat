package com.henjicc.swiftformat.engine.image

import com.henjicc.swiftformat.core.model.QualityPreset

/**
 * 图片质量档位 → JPEG/有损 WebP 压缩质量值（见 SPEC 5.3）。
 * 纯函数，不依赖 Android Bitmap API，便于单元测试。
 */
object ImageQualityMapper {
    fun compressQuality(preset: QualityPreset): Int = when (preset) {
        QualityPreset.BEST -> 95
        QualityPreset.HIGH -> 85
        QualityPreset.STANDARD -> 75
        QualityPreset.SMALL_SIZE -> 60
    }
}
