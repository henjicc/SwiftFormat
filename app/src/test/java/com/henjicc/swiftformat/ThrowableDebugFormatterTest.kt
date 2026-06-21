package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.common.toDebugMessage
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrowableDebugFormatterTest {
    @Test
    fun toDebugMessage_includesCauseChain() {
        val throwable = Error(
            "FFmpegKit failed to start on brand: vivo.",
            UnsatisfiedLinkError("dlopen failed: missing DT_HASH/DT_GNU_HASH in libffmpegkit_abidetect.so"),
        )

        val debugMessage = throwable.toDebugMessage()

        assertTrue(debugMessage.contains("java.lang.Error"))
        assertTrue(debugMessage.contains("FFmpegKit failed to start on brand: vivo."))
        assertTrue(debugMessage.contains("Caused by: java.lang.UnsatisfiedLinkError"))
        assertTrue(debugMessage.contains("dlopen failed: missing DT_HASH/DT_GNU_HASH"))
    }
}
