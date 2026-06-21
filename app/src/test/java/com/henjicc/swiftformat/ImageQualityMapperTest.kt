package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.image.ImageQualityMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageQualityMapperTest {

    @Test
    fun matchesSpecMapping() {
        assertEquals(95, ImageQualityMapper.compressQuality(QualityPreset.BEST))
        assertEquals(85, ImageQualityMapper.compressQuality(QualityPreset.HIGH))
        assertEquals(75, ImageQualityMapper.compressQuality(QualityPreset.STANDARD))
        assertEquals(60, ImageQualityMapper.compressQuality(QualityPreset.SMALL_SIZE))
    }
}
