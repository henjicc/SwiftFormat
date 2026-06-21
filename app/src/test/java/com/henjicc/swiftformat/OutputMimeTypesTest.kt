package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.file.OutputMimeTypes
import org.junit.Assert.assertEquals
import org.junit.Test

class OutputMimeTypesTest {

    @Test
    fun knownFormats_mapToExpectedMimeTypes() {
        assertEquals("image/jpeg", OutputMimeTypes.forFormat("JPG"))
        assertEquals("image/png", OutputMimeTypes.forFormat("png"))
        assertEquals("image/webp", OutputMimeTypes.forFormat("WEBP"))
        assertEquals("video/mp4", OutputMimeTypes.forFormat("MP4"))
        assertEquals("video/webm", OutputMimeTypes.forFormat("WEBM"))
        assertEquals("video/x-matroska", OutputMimeTypes.forFormat("MKV"))
        assertEquals("audio/mpeg", OutputMimeTypes.forFormat("MP3"))
        assertEquals("audio/aac", OutputMimeTypes.forFormat("AAC"))
        assertEquals("audio/wav", OutputMimeTypes.forFormat("WAV"))
        assertEquals("audio/flac", OutputMimeTypes.forFormat("FLAC"))
    }

    @Test
    fun unknownFormat_fallsBackToOctetStream() {
        assertEquals("application/octet-stream", OutputMimeTypes.forFormat("OGG"))
    }
}
