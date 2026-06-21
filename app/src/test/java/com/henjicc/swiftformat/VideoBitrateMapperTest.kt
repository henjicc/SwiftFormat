package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.media.VideoBitrateMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoBitrateMapperTest {

    @Test
    fun higherPreset_yieldsHigherBitrate_whenSourceUnconstrained() {
        val best = VideoBitrateMapper.targetBitrateBps(QualityPreset.BEST, 1920, 1080, 30.0, null)
        val high = VideoBitrateMapper.targetBitrateBps(QualityPreset.HIGH, 1920, 1080, 30.0, null)
        val standard = VideoBitrateMapper.targetBitrateBps(QualityPreset.STANDARD, 1920, 1080, 30.0, null)
        val small = VideoBitrateMapper.targetBitrateBps(QualityPreset.SMALL_SIZE, 1920, 1080, 30.0, null)
        assertTrue(best > high)
        assertTrue(high > standard)
        assertTrue(standard > small)
    }

    @Test
    fun neverExceedsSourceBitrate() {
        val lowSourceBitrate = 500_000L // 远低于 1080p 计算出的基准码率
        val result = VideoBitrateMapper.targetBitrateBps(QualityPreset.BEST, 1920, 1080, 30.0, lowSourceBitrate)
        assertEquals(lowSourceBitrate, result)
    }

    @Test
    fun usesComputedValue_whenSourceBitrateUnknown() {
        val result = VideoBitrateMapper.targetBitrateBps(QualityPreset.HIGH, 1920, 1080, 30.0, null)
        assertTrue(result > 0)
    }

    @Test
    fun smallerResolution_yieldsLowerBitrate() {
        val hd = VideoBitrateMapper.targetBitrateBps(QualityPreset.HIGH, 1920, 1080, 30.0, null)
        val sd = VideoBitrateMapper.targetBitrateBps(QualityPreset.HIGH, 1280, 720, 30.0, null)
        assertTrue(hd > sd)
    }
}
