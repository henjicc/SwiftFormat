package com.henjicc.swiftformat.engine.ffmpeg

import com.henjicc.swiftformat.core.model.QualityPreset

/**
 * 图片质量档位 → libaom-av1 CRF 值（0-63，越小质量越高）。
 * 纯函数，不依赖 Android Bitmap API，便于单元测试。
 */
object AvifCrfMapper {
    fun targetCrf(preset: QualityPreset): Int = when (preset) {
        QualityPreset.BEST -> 18
        QualityPreset.HIGH -> 24
        QualityPreset.STANDARD -> 30
        QualityPreset.SMALL_SIZE -> 38
    }
}
