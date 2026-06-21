package com.henjicc.swiftformat

import android.net.Uri
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputDestination
import com.henjicc.swiftformat.core.model.OutputFormatCatalog
import com.henjicc.swiftformat.engine.api.ConversionEngine
import com.henjicc.swiftformat.engine.api.ConversionEngineSelector
import com.henjicc.swiftformat.engine.api.ConversionProgress
import com.henjicc.swiftformat.engine.api.ConversionResult
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock

private class FakeEngine(private val predicate: (ConversionRequest) -> Boolean) : ConversionEngine {
    override fun supports(request: ConversionRequest): Boolean = predicate(request)
    override suspend fun convert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult = throw NotImplementedError()

    override suspend fun cancel(taskId: String) = Unit
}

class ConversionEngineSelectorTest {

    private fun requestFor(mediaType: MediaType, outputFormat: String): ConversionRequest {
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
            outputFormat = outputFormat,
            targetMediaType = OutputFormatCatalog.targetMediaTypeFor(mediaType, outputFormat),
            quality = null,
            size = null,
            destination = OutputDestination.ResolvedUri(uri),
        )
    }

    @Test
    fun selectsFirstEngineThatSupportsRequest() {
        val imageEngine = FakeEngine { it.input.mediaType == MediaType.IMAGE }
        val videoEngine = FakeEngine { it.input.mediaType == MediaType.VIDEO && it.outputFormat == "MP4" }
        val selector = ConversionEngineSelector(listOf(imageEngine, videoEngine))

        assertSame(imageEngine, selector.select(requestFor(MediaType.IMAGE, "JPG")))
        assertSame(videoEngine, selector.select(requestFor(MediaType.VIDEO, "MP4")))
    }

    @Test
    fun returnsNull_whenNoEngineSupportsRequest() {
        val selector = ConversionEngineSelector(listOf(FakeEngine { it.input.mediaType == MediaType.IMAGE }))
        assertNull(selector.select(requestFor(MediaType.AUDIO, "AAC")))
    }

    @Test
    fun ffmpegVideoRoutes_canBeDistinguishedByFormatAndTargetType() {
        val media3Engine = FakeEngine { request ->
            request.input.mediaType == MediaType.VIDEO &&
                request.targetMediaType == MediaType.VIDEO &&
                request.outputFormat == "MP4"
        }
        val ffmpegEngine = FakeEngine { request ->
            request.input.mediaType == MediaType.VIDEO &&
                (
                    request.outputFormat == "WEBM" ||
                        request.outputFormat == "MKV" ||
                        (request.outputFormat == "MP3" && request.targetMediaType == MediaType.AUDIO)
                    )
        }
        val selector = ConversionEngineSelector(listOf(media3Engine, ffmpegEngine))

        assertSame(media3Engine, selector.select(requestFor(MediaType.VIDEO, "MP4")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "WEBM")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "MKV")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "MP3")))
    }
}
