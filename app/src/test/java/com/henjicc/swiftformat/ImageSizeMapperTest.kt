package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.SizePreset
import com.henjicc.swiftformat.engine.image.ImageSizeMapper
import com.henjicc.swiftformat.engine.image.ImageSizeMapper.Dimensions
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizeMapperTest {

    @Test
    fun original_keepsSourceDimensions() {
        val source = Dimensions(4000, 3000)
        assertEquals(source, ImageSizeMapper.targetDimensions(source, SizePreset.Original))
        assertEquals(source, ImageSizeMapper.targetDimensions(source, null))
    }

    @Test
    fun longestEdge_scalesDownPreservingAspectRatio() {
        val source = Dimensions(4000, 2000) // 2:1
        val result = ImageSizeMapper.targetDimensions(source, SizePreset.ImageLongestEdge(2000))
        assertEquals(2000, result.width)
        assertEquals(1000, result.height)
    }

    @Test
    fun longestEdge_neverUpscales() {
        val source = Dimensions(800, 600)
        val result = ImageSizeMapper.targetDimensions(source, SizePreset.ImageLongestEdge(3840))
        assertEquals(source, result)
    }

    @Test
    fun custom_bothDimensionsUseExactTarget() {
        val source = Dimensions(1000, 1000)
        val result = ImageSizeMapper.targetDimensions(source, SizePreset.Custom(width = 500, height = 200))
        assertEquals(Dimensions(500, 200), result)
    }

    @Test
    fun custom_singleDimensionPreservesAspectRatio() {
        val source = Dimensions(2000, 1000) // 2:1
        val result = ImageSizeMapper.targetDimensions(source, SizePreset.Custom(width = 1000, height = null))
        assertEquals(Dimensions(1000, 500), result)
    }

    @Test
    fun videoResolution_notApplicableToImages_keepsSource() {
        val source = Dimensions(1920, 1080)
        val result = ImageSizeMapper.targetDimensions(source, SizePreset.VideoResolution(720))
        assertEquals(source, result)
    }

    @Test
    fun sampleSizeFor_picksPowerOfTwoNotExceedingTarget() {
        val source = Dimensions(4000, 3000)
        val target = Dimensions(1000, 750)
        val sampleSize = ImageSizeMapper.sampleSizeFor(source, target)
        assertEquals(4, sampleSize)
    }

    @Test
    fun sampleSizeFor_returnsOne_whenTargetCloseToSource() {
        val source = Dimensions(1000, 1000)
        val target = Dimensions(900, 900)
        assertEquals(1, ImageSizeMapper.sampleSizeFor(source, target))
    }

    @Test
    fun dimensions_swapped_exchangesWidthAndHeight() {
        assertEquals(Dimensions(3000, 4000), Dimensions(4000, 3000).swapped())
    }
}
