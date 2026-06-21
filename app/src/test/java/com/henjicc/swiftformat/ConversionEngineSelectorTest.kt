package com.henjicc.swiftformat

import android.net.Uri
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputDestination
import com.henjicc.swiftformat.engine.api.ConversionEngine
import com.henjicc.swiftformat.engine.api.ConversionEngineSelector
import com.henjicc.swiftformat.engine.api.ConversionProgress
import com.henjicc.swiftformat.engine.api.ConversionResult
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock

private class FakeEngine(private val supportedType: MediaType) : ConversionEngine {
    override fun supports(request: ConversionRequest): Boolean = request.input.mediaType == supportedType
    override suspend fun convert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult = throw NotImplementedError()

    override suspend fun cancel(taskId: String) = Unit
}

class ConversionEngineSelectorTest {

    private fun requestFor(mediaType: MediaType): ConversionRequest {
        val uri = mock<Uri>()
        return ConversionRequest(
            id = "1",
            input = InputFile(
                id = "1",
                uri = uri,
                displayName = "a",
                mimeType = null,
                extension = null,
                sizeBytes = null,
                mediaType = mediaType,
            ),
            outputFormat = "JPG",
            quality = null,
            size = null,
            destination = OutputDestination.ResolvedUri(uri),
        )
    }

    @Test
    fun selectsFirstEngineThatSupportsRequest() {
        val imageEngine = FakeEngine(MediaType.IMAGE)
        val videoEngine = FakeEngine(MediaType.VIDEO)
        val selector = ConversionEngineSelector(listOf(imageEngine, videoEngine))

        assertSame(imageEngine, selector.select(requestFor(MediaType.IMAGE)))
        assertSame(videoEngine, selector.select(requestFor(MediaType.VIDEO)))
    }

    @Test
    fun returnsNull_whenNoEngineSupportsRequest() {
        val selector = ConversionEngineSelector(listOf(FakeEngine(MediaType.IMAGE)))
        assertNull(selector.select(requestFor(MediaType.AUDIO)))
    }
}
