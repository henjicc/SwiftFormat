package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.FailureReasonCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FailureReasonCodecTest {

    @Test
    fun decode_restoresKindAndMultilineDetails() {
        val encoded = FailureReasonCodec.encode(
            ConversionError.Kind.UNSUPPORTED_IMAGE_OUTPUT,
            "first line\nsecond line",
        )

        val decoded = FailureReasonCodec.decode(encoded)

        assertEquals(ConversionError.Kind.UNSUPPORTED_IMAGE_OUTPUT, decoded?.kind)
        assertEquals("first line\nsecond line", decoded?.details)
        assertEquals(encoded, decoded?.raw)
    }

    @Test
    fun decode_legacyPlainTextKeepsRawAndNoKind() {
        val decoded = FailureReasonCodec.decode("legacy failure text")

        assertNull(decoded?.kind)
        assertNull(decoded?.details)
        assertEquals("legacy failure text", decoded?.raw)
    }
}
