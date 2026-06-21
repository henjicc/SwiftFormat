package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.SizePreset
import com.henjicc.swiftformat.core.model.SizePresetCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SizePresetCodecTest {

    @Test
    fun nullRoundTrips() {
        assertNull(SizePresetCodec.encode(null))
        assertNull(SizePresetCodec.decode(null))
    }

    @Test
    fun original_roundTrips() {
        val encoded = SizePresetCodec.encode(SizePreset.Original)
        assertEquals(SizePreset.Original, SizePresetCodec.decode(encoded))
    }

    @Test
    fun videoResolution_roundTrips() {
        val original = SizePreset.VideoResolution(1080)
        assertEquals(original, SizePresetCodec.decode(SizePresetCodec.encode(original)))
    }

    @Test
    fun imageLongestEdge_roundTrips() {
        val original = SizePreset.ImageLongestEdge(3840)
        assertEquals(original, SizePresetCodec.decode(SizePresetCodec.encode(original)))
    }

    @Test
    fun custom_bothDimensions_roundTrips() {
        val original = SizePreset.Custom(1920, 1080)
        assertEquals(original, SizePresetCodec.decode(SizePresetCodec.encode(original)))
    }

    @Test
    fun custom_widthOnly_roundTrips() {
        val original = SizePreset.Custom(1920, null)
        assertEquals(original, SizePresetCodec.decode(SizePresetCodec.encode(original)))
    }

    @Test
    fun custom_heightOnly_roundTrips() {
        val original = SizePreset.Custom(null, 1080)
        assertEquals(original, SizePresetCodec.decode(SizePresetCodec.encode(original)))
    }

    @Test
    fun unknownString_decodesToNull() {
        assertNull(SizePresetCodec.decode("GARBAGE"))
    }
}
