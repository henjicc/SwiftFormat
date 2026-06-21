package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.file.OutputNaming
import org.junit.Assert.assertEquals
import org.junit.Test

class OutputNamingTest {

    @Test
    fun withExtension_replacesExtensionKeepingBaseName() {
        assertEquals("photo.webp", OutputNaming.withExtension("photo.jpg", "WEBP"))
        assertEquals("archive.tar.png", OutputNaming.withExtension("archive.tar.heic", "PNG"))
    }

    @Test
    fun withExtension_handlesNoOriginalExtension() {
        assertEquals("noext.jpg", OutputNaming.withExtension("noext", "JPG"))
    }

    @Test
    fun resolveCollision_returnsOriginal_whenNoConflict() {
        assertEquals("video.mp4", OutputNaming.resolveCollision("video.mp4", emptySet()))
    }

    @Test
    fun resolveCollision_appendsIncrementingIndex() {
        val existing = setOf("video.mp4", "video (1).mp4")
        assertEquals("video (2).mp4", OutputNaming.resolveCollision("video.mp4", existing))
    }

    @Test
    fun resolveCollision_handlesNoExtension() {
        val existing = setOf("README")
        assertEquals("README (1)", OutputNaming.resolveCollision("README", existing))
    }
}
