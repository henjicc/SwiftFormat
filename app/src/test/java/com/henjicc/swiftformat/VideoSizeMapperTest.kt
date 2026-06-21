package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.SizePreset
import com.henjicc.swiftformat.engine.media.VideoSizeMapper
import com.henjicc.swiftformat.engine.media.VideoSizeMapper.Dimensions
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoSizeMapperTest {

    @Test
    fun original_keepsSourceDimensions() {
        val source = Dimensions(1920, 1080)
        assertEquals(source, VideoSizeMapper.targetDimensions(source, SizePreset.Original))
        assertEquals(source, VideoSizeMapper.targetDimensions(source, null))
    }

    @Test
    fun landscape_scalesByShortEdge_height() {
        val source = Dimensions(1920, 1080) // 短边=1080(高)
        val result = VideoSizeMapper.targetDimensions(source, SizePreset.VideoResolution(720))
        assertEquals(1280, result.width)
        assertEquals(720, result.height)
    }

    @Test
    fun portrait_scalesByShortEdge_width_andKeepsPortraitOrientation() {
        val source = Dimensions(1080, 1920) // 短边=1080(宽)，竖屏
        val result = VideoSizeMapper.targetDimensions(source, SizePreset.VideoResolution(720))
        assertEquals(720, result.width)
        assertEquals(1280, result.height)
        assertEquals(true, result.height > result.width) // 仍是竖屏
    }

    @Test
    fun neverUpscales() {
        val source = Dimensions(640, 480)
        val result = VideoSizeMapper.targetDimensions(source, SizePreset.VideoResolution(1080))
        assertEquals(source, result)
    }

    @Test
    fun resultDimensions_alwaysEven() {
        val source = Dimensions(1921, 1081)
        val result = VideoSizeMapper.targetDimensions(source, SizePreset.VideoResolution(719))
        assertEquals(0, result.width % 2)
        assertEquals(0, result.height % 2)
    }

    @Test
    fun imageLongestEdge_notApplicableToVideo_keepsSource() {
        val source = Dimensions(1920, 1080)
        val result = VideoSizeMapper.targetDimensions(source, SizePreset.ImageLongestEdge(720))
        assertEquals(source, result)
    }

    @Test
    fun custom_bothDimensionsUseExactTarget() {
        val source = Dimensions(1920, 1080)
        val result = VideoSizeMapper.targetDimensions(source, SizePreset.Custom(width = 800, height = 600))
        assertEquals(Dimensions(800, 600), result)
    }
}
