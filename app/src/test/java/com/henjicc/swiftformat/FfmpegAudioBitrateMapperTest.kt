package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.ffmpeg.FfmpegAudioBitrateMapper
import org.junit.Assert.assertTrue
import org.junit.Test

class FfmpegAudioBitrateMapperTest {

    @Test
    fun mp3_higherPresetYieldsHigherBitrate() {
        val best = FfmpegAudioBitrateMapper.mp3TargetBitrateBps(QualityPreset.BEST)
        val high = FfmpegAudioBitrateMapper.mp3TargetBitrateBps(QualityPreset.HIGH)
        val standard = FfmpegAudioBitrateMapper.mp3TargetBitrateBps(QualityPreset.STANDARD)
        val small = FfmpegAudioBitrateMapper.mp3TargetBitrateBps(QualityPreset.SMALL_SIZE)
        assertTrue(best > high)
        assertTrue(high > standard)
        assertTrue(standard > small)
    }
}
