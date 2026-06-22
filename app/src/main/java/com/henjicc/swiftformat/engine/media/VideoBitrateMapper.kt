package com.henjicc.swiftformat.engine.media

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning.forPresetOrStandard
import kotlin.math.roundToLong

/**
 * 视频质量档位 → 目标码率（见 SPEC 5.4）。纯函数，便于单元测试。
 *
 * 策略：按目标分辨率与帧率估算一个"每像素比特数"基准码率，再与源码率比较取较小值——
 * 保证不会让输出码率无意义地高于源文件（不会拿低码率源「拉高」画质）。基准值见 [QualityPresetTuning]。
 */
object VideoBitrateMapper {

    fun targetBitrateBps(
        preset: QualityPreset,
        targetWidth: Int,
        targetHeight: Int,
        frameRate: Double,
        sourceBitrateBps: Long?,
    ): Long {
        val bpp = QualityPresetTuning.videoBitsPerPixelPerFrame.forPresetOrStandard(preset)
        val effectiveFrameRate = if (frameRate > 0) frameRate else DEFAULT_FRAME_RATE
        val computed = (bpp * targetWidth * targetHeight * effectiveFrameRate).roundToLong()
        return if (sourceBitrateBps != null && sourceBitrateBps > 0) {
            minOf(computed, sourceBitrateBps)
        } else {
            computed
        }
    }

    private const val DEFAULT_FRAME_RATE = 30.0
}
