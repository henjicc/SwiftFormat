package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputFormatCatalog
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputFormatCatalogTest {

    @Test
    fun audioHasNoSizeOptions() {
        assertTrue(OutputFormatCatalog.sizePresets(MediaType.AUDIO).isEmpty())
    }

    @Test
    fun imageAndVideoDefaultToOriginalSize() {
        assertEquals(SizePreset.Original, OutputFormatCatalog.sizePresets(MediaType.IMAGE).first())
        assertEquals(SizePreset.Original, OutputFormatCatalog.sizePresets(MediaType.VIDEO).first())
    }

    @Test
    fun qualityHiddenForLosslessFormats() {
        assertTrue(OutputFormatCatalog.isQualityApplicable("JPG"))
        assertTrue(OutputFormatCatalog.isQualityApplicable("WEBP"))
        assertTrue(OutputFormatCatalog.isQualityApplicable("MP4"))
        assertTrue(OutputFormatCatalog.isQualityApplicable("MP3"))
        assertTrue(OutputFormatCatalog.isQualityApplicable("AAC"))

        assertTrue(!OutputFormatCatalog.isQualityApplicable("PNG"))
        assertTrue(!OutputFormatCatalog.isQualityApplicable("WAV"))
        assertTrue(!OutputFormatCatalog.isQualityApplicable("FLAC"))
    }

    @Test
    fun videoMp3_hidesSizeAndTargetsAudio() {
        val option = OutputFormatCatalog.option(MediaType.VIDEO, "MP3")
        assertEquals(MediaType.AUDIO, option?.targetMediaType)
        assertFalse(OutputFormatCatalog.isSizeApplicable(MediaType.VIDEO, "MP3"))
        assertTrue(OutputFormatCatalog.isQualityApplicable(MediaType.VIDEO, "MP3"))
    }

    @Test
    fun defaultSettings_matchSpecDefaults() {
        val image = OutputFormatCatalog.defaultSettings(MediaType.IMAGE)
        assertEquals("WEBP", image.outputFormat)
        assertEquals(MediaType.IMAGE, image.targetMediaType)
        assertEquals(QualityPreset.HIGH, image.quality)
        assertEquals(SizePreset.Original, image.size)

        val video = OutputFormatCatalog.defaultSettings(MediaType.VIDEO)
        assertEquals("MP4", video.outputFormat)
        assertEquals(MediaType.VIDEO, video.targetMediaType)
        assertEquals(QualityPreset.HIGH, video.quality)
        assertEquals(SizePreset.Original, video.size)

        val audio = OutputFormatCatalog.defaultSettings(MediaType.AUDIO)
        assertEquals("MP3", audio.outputFormat)
        assertEquals(MediaType.AUDIO, audio.targetMediaType)
        assertEquals(QualityPreset.HIGH, audio.quality)
        assertNull(audio.size)
    }
}
