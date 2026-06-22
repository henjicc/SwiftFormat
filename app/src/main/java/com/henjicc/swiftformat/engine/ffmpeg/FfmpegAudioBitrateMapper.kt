package com.henjicc.swiftformat.engine.ffmpeg

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning
import com.henjicc.swiftformat.engine.tuning.QualityPresetTuning.forPresetOrStandard

/**
 * MP3 质量档位 → 目标码率（见 SPEC 5.5）。纯函数，便于单元测试。
 *
 * FLAC/WAV 为无损格式，不显示质量档位（[OutputFormatCatalog] 已隐藏），故本对象只覆盖 MP3。
 * MP3 编解码效率低于 AAC，同档位码率高于 [com.henjicc.swiftformat.engine.media.AudioBitrateMapper]。
 * 具体数值见 [QualityPresetTuning]。
 */
object FfmpegAudioBitrateMapper {

    fun mp3TargetBitrateBps(preset: QualityPreset): Int =
        QualityPresetTuning.mp3BitrateBps.forPresetOrStandard(preset)
}
