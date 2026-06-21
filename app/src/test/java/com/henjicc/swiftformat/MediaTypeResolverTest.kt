package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.file.MediaTypeResolver
import com.henjicc.swiftformat.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTypeResolverTest {

    @Test
    fun mimeType_takesPriority() {
        assertEquals(MediaType.IMAGE, MediaTypeResolver.resolve("image/png", "x.dat"))
        assertEquals(MediaType.VIDEO, MediaTypeResolver.resolve("video/mp4", "x.dat"))
        assertEquals(MediaType.AUDIO, MediaTypeResolver.resolve("audio/mpeg", "x.dat"))
    }

    @Test
    fun fallsBackToExtension_whenMimeNullOrGeneric() {
        assertEquals(MediaType.IMAGE, MediaTypeResolver.resolve(null, "photo.HEIC"))
        assertEquals(MediaType.VIDEO, MediaTypeResolver.resolve(null, "clip.mkv"))
        assertEquals(MediaType.AUDIO, MediaTypeResolver.resolve(null, "song.flac"))
        assertEquals(MediaType.AUDIO, MediaTypeResolver.resolve("application/octet-stream", "song.mp3"))
    }

    @Test
    fun unknown_whenNoSignal() {
        assertEquals(MediaType.UNKNOWN, MediaTypeResolver.resolve(null, "archive.zip"))
        assertEquals(MediaType.UNKNOWN, MediaTypeResolver.resolve(null, "noextension"))
        assertEquals(MediaType.UNKNOWN, MediaTypeResolver.resolve(null, null))
    }

    @Test
    fun extensionOf_handlesEdgeCases() {
        assertEquals("jpg", MediaTypeResolver.extensionOf("a.JPG"))
        assertEquals("gz", MediaTypeResolver.extensionOf("a.tar.gz"))
        assertEquals("", MediaTypeResolver.extensionOf("trailingdot."))
        assertEquals("", MediaTypeResolver.extensionOf("nodot"))
        assertEquals("", MediaTypeResolver.extensionOf(null))
    }
}
