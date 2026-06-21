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
        val stillImageEngine = FakeEngine { request ->
            request.input.mediaType == MediaType.IMAGE && request.outputFormat in setOf("BMP", "TIFF")
        }
        val media3Engine = FakeEngine { request ->
            when (request.input.mediaType) {
                MediaType.VIDEO ->
                    request.targetMediaType == MediaType.VIDEO &&
                        request.outputFormat == "MP4"

                MediaType.AUDIO ->
                    request.targetMediaType == MediaType.AUDIO &&
                        request.outputFormat in setOf("AAC", "M4A")

                else -> false
            }
        }
        val ffmpegEngine = FakeEngine { request ->
            when (request.input.mediaType) {
                MediaType.AUDIO ->
                    request.targetMediaType == MediaType.AUDIO &&
                        request.outputFormat in setOf("MP3", "WAV", "FLAC", "OGG")

                MediaType.VIDEO ->
                    (request.targetMediaType == MediaType.VIDEO &&
                        request.outputFormat in setOf("MOV", "WEBM", "MKV")) ||
                        (request.targetMediaType == MediaType.AUDIO &&
                            request.outputFormat in setOf("MP3", "M4A", "WAV", "FLAC"))

                else -> false
            }
        }
        val selector = ConversionEngineSelector(listOf(stillImageEngine, media3Engine, ffmpegEngine))

        assertSame(stillImageEngine, selector.select(requestFor(MediaType.IMAGE, "BMP")))
        assertSame(stillImageEngine, selector.select(requestFor(MediaType.IMAGE, "TIFF")))
        assertSame(media3Engine, selector.select(requestFor(MediaType.VIDEO, "MP4")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "MOV")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "WEBM")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "MKV")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "MP3")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "M4A")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "WAV")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.VIDEO, "FLAC")))
        assertSame(media3Engine, selector.select(requestFor(MediaType.AUDIO, "M4A")))
        assertSame(media3Engine, selector.select(requestFor(MediaType.AUDIO, "AAC")))
        assertSame(ffmpegEngine, selector.select(requestFor(MediaType.AUDIO, "OGG")))
    }
}
