package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.media.AudioBitrateMapper
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioBitrateMapperTest {

    @Test
    fun wav_hasNoQualityBitrate() {
        assertNull(AudioBitrateMapper.targetBitrateBps("WAV", QualityPreset.BEST))
        assertNull(AudioBitrateMapper.targetBitrateBps("wav", QualityPreset.HIGH))
    }

    @Test
    fun aac_higherPresetYieldsHigherBitrate() {
        val best = AudioBitrateMapper.targetBitrateBps("AAC", QualityPreset.BEST)!!
        val high = AudioBitrateMapper.targetBitrateBps("AAC", QualityPreset.HIGH)!!
        val standard = AudioBitrateMapper.targetBitrateBps("AAC", QualityPreset.STANDARD)!!
        val small = AudioBitrateMapper.targetBitrateBps("AAC", QualityPreset.SMALL_SIZE)!!
        assertTrue(best > high)
        assertTrue(high > standard)
        assertTrue(standard > small)
    }
}
