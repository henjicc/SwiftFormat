package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.ffmpeg.OpusBitrateMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpusBitrateMapperTest {

    @Test
    fun opusBitrate_descendsAcrossQualityPresets() {
        val best = OpusBitrateMapper.targetBitrateBps(QualityPreset.BEST)
        val high = OpusBitrateMapper.targetBitrateBps(QualityPreset.HIGH)
        val standard = OpusBitrateMapper.targetBitrateBps(QualityPreset.STANDARD)
        val small = OpusBitrateMapper.targetBitrateBps(QualityPreset.SMALL_SIZE)

        assertEquals(192_000, best)
        assertTrue(best > high)
        assertTrue(high > standard)
        assertTrue(standard > small)
    }
}
