package com.henjicc.swiftformat

import com.henjicc.swiftformat.conversion.ConversionConcurrencyPolicy
import com.henjicc.swiftformat.core.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversionConcurrencyPolicyTest {

    @Test
    fun video_isSerial() {
        assertEquals(1, ConversionConcurrencyPolicy.maxConcurrency(MediaType.VIDEO))
    }

    @Test
    fun imageAndAudio_allowTwoConcurrent() {
        assertEquals(2, ConversionConcurrencyPolicy.maxConcurrency(MediaType.IMAGE))
        assertEquals(2, ConversionConcurrencyPolicy.maxConcurrency(MediaType.AUDIO))
    }

    @Test
    fun unknown_fallsBackToSerial() {
        assertEquals(1, ConversionConcurrencyPolicy.maxConcurrency(MediaType.UNKNOWN))
    }
}
