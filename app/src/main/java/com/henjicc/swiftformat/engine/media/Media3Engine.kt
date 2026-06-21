package com.henjicc.swiftformat.engine.media

import android.content.Context
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.engine.api.ConversionEngine
import com.henjicc.swiftformat.engine.api.ConversionProgress
import com.henjicc.swiftformat.engine.api.ConversionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基于 Media3 Transformer 的音视频转换引擎（见 SPEC 10.1/10.3）。
 * 本任务范围：视频 → MP4(H.264)；音频 → AAC/M4A。
 * MP3/FLAC/WAV/Opus 等容器 Android `MediaMuxer` 不支持，交给 TASK-05 FFmpeg 引擎。
 *
 * Transformer 只能写本地文件路径，不能直写 content Uri：转换到应用缓存的临时文件后，
 * 再把字节流写入已解析好的目标 Uri（见 SPEC 12.2），最后清理临时文件。
 * Transformer 的创建、`start`、`getProgress`、`cancel` 必须在同一个有 Looper 的线程调用，统一固定在 [Dispatchers.Main]。
 */
@UnstableApi
class Media3Engine(
    context: Context,
    private val logger: Logger,
) : ConversionEngine {

    private val appContext = context.applicationContext
    private val configFactory = Media3ConversionConfigFactory(appContext, logger)
    private val transformerRunner = Media3TransformerRunner(appContext)

    override fun supports(request: ConversionRequest): Boolean = when (request.input.mediaType) {
        MediaType.VIDEO -> request.outputFormat.equals("MP4", ignoreCase = true) &&
            MediaCodecCapabilities.hasEncoderFor(MimeTypes.VIDEO_H264)

        MediaType.AUDIO -> request.outputFormat.uppercase() in SUPPORTED_AUDIO_FORMATS &&
            MediaCodecCapabilities.hasEncoderFor(MimeTypes.AUDIO_AAC)

        else -> false
    }

    override suspend fun convert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult = withContext(Dispatchers.Main) {
        try {
            val config = configFactory.create(request)
            transformerRunner.convert(request, config, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(TAG, "convert failed: ${request.id}", e)
            ConversionResult.Failure(ConversionError(ConversionError.Kind.ENGINE_CRASH, e.message, e))
        }
    }

    override suspend fun cancel(taskId: String) {
        withContext(Dispatchers.Main) {
            transformerRunner.cancel(taskId)
        }
    }

    private companion object {
        const val TAG = "Media3Engine"
        val SUPPORTED_AUDIO_FORMATS = setOf("AAC", "M4A")
    }
}
