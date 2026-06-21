package com.henjicc.swiftformat.engine.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputDestination
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.api.ConversionEngine
import com.henjicc.swiftformat.engine.api.ConversionProgress
import com.henjicc.swiftformat.engine.api.ConversionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

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
    private val activeTransformers = ConcurrentHashMap<String, Transformer>()

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
            doConvert(request, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(TAG, "convert failed: ${request.id}", e)
            ConversionResult.Failure(ConversionError(ConversionError.Kind.ENGINE_CRASH, e.message, e))
        } finally {
            activeTransformers.remove(request.id)
        }
    }

    override suspend fun cancel(taskId: String) {
        withContext(Dispatchers.Main) {
            activeTransformers[taskId]?.cancel()
        }
    }

    private suspend fun doConvert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        val destinationUri = (request.destination as? OutputDestination.ResolvedUri)?.uri
            ?: return failure(ConversionError.Kind.OUTPUT_NOT_WRITABLE, "destination not resolved")

        onProgress(ConversionProgress(0f))

        val tempFile = File(appContext.cacheDir, "media3_${request.id}.tmp")
        try {
            val editedItem = buildEditedMediaItem(request)
            val encoderFactory = buildEncoderFactory(request)

            val outcome = coroutineScope {
                val resultDeferred = CompletableDeferred<TransformOutcome>()
                val transformer = Transformer.Builder(appContext)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(encoderFactory)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            resultDeferred.complete(TransformOutcome.Success(exportResult))
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException,
                        ) {
                            resultDeferred.complete(TransformOutcome.Error(exportException))
                        }
                    })
                    .build()

                activeTransformers[request.id] = transformer
                transformer.start(editedItem, tempFile.absolutePath)

                val progressJob = launch {
                    val holder = ProgressHolder()
                    while (isActive) {
                        val state = transformer.getProgress(holder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            // 导出阶段占总进度 0~90%，剩余 10% 留给临时文件拷贝到目标 Uri。
                            onProgress(ConversionProgress(holder.progress / 100f * 0.9f))
                        }
                        delay(PROGRESS_POLL_INTERVAL_MS)
                    }
                }
                val result = resultDeferred.await()
                progressJob.cancel()
                transformer.cancel()
                result
            }

            return when (outcome) {
                is TransformOutcome.Error -> failure(
                    mapErrorCode(outcome.exception.errorCode),
                    outcome.exception.message ?: "export failed",
                )

                is TransformOutcome.Success -> {
                    val sizeBytes = withContext(Dispatchers.IO) {
                        copyToDestination(tempFile, destinationUri)
                    }
                    onProgress(ConversionProgress(1f))
                    ConversionResult.Success(destinationUri, sizeBytes)
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun buildEditedMediaItem(request: ConversionRequest): EditedMediaItem {
        val mediaItem = MediaItem.fromUri(request.input.uri)
        val builder = EditedMediaItem.Builder(mediaItem)
        if (request.input.mediaType == MediaType.VIDEO) {
            val source = VideoSizeMapper.Dimensions(
                request.input.width ?: 0,
                request.input.height ?: 0,
            )
            val target = VideoSizeMapper.targetDimensions(source, request.size)
            if (target != source && target.width > 0 && target.height > 0) {
                val shortSide = minOf(target.width, target.height)
                builder.setEffects(Effects(emptyList(), listOf(Presentation.createForShortSide(shortSide))))
            }
        }
        return builder.build()
    }

    private fun buildEncoderFactory(request: ConversionRequest): DefaultEncoderFactory {
        val builder = DefaultEncoderFactory.Builder(appContext)
        when (request.input.mediaType) {
            MediaType.VIDEO -> {
                val source = VideoSizeMapper.Dimensions(
                    request.input.width ?: 0,
                    request.input.height ?: 0,
                )
                val target = VideoSizeMapper.targetDimensions(source, request.size)
                val track = probeVideoTrack(request.input.uri)
                val bitrate = VideoBitrateMapper.targetBitrateBps(
                    preset = request.quality ?: QualityPreset.HIGH,
                    targetWidth = target.width,
                    targetHeight = target.height,
                    frameRate = track?.frameRate ?: 30.0,
                    sourceBitrateBps = track?.bitrate,
                )
                builder.setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder().setBitrate(bitrate.toInt()).build(),
                )
            }

            MediaType.AUDIO -> {
                val bitrate = AudioBitrateMapper.targetBitrateBps(request.outputFormat, request.quality ?: QualityPreset.HIGH)
                if (bitrate != null) {
                    builder.setRequestedAudioEncoderSettings(
                        AudioEncoderSettings.Builder().setBitrate(bitrate).build(),
                    )
                }
            }

            else -> Unit
        }
        return builder.build()
    }

    private fun copyToDestination(tempFile: File, destinationUri: Uri): Long {
        val outputStream = appContext.contentResolver.openOutputStream(destinationUri)
            ?: error("cannot open output stream")
        outputStream.use { out ->
            tempFile.inputStream().use { input -> input.copyTo(out) }
        }
        return tempFile.length()
    }

    private data class VideoTrackInfo(val frameRate: Double, val bitrate: Long?)

    /** 用 MediaExtractor 读取视频轨真实帧率/码率，供码率映射使用；探测失败时返回 null 走默认值。 */
    private fun probeVideoTrack(uri: Uri): VideoTrackInfo? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(appContext, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/")) continue
                val frameRate = runCatching {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        format.getInteger(MediaFormat.KEY_FRAME_RATE).toDouble()
                    } else {
                        null
                    }
                }.getOrNull() ?: 30.0
                val bitrate = runCatching {
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        format.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
                    } else {
                        null
                    }
                }.getOrNull()
                return VideoTrackInfo(frameRate, bitrate)
            }
            null
        } catch (e: Exception) {
            logger.w(TAG, "probeVideoTrack failed", e)
            null
        } finally {
            extractor.release()
        }
    }

    private fun failure(kind: ConversionError.Kind, message: String) =
        ConversionResult.Failure(ConversionError(kind, message))

    private fun mapErrorCode(errorCode: Int): ConversionError.Kind = when (errorCode) {
        ExportException.ERROR_CODE_ENCODER_INIT_FAILED,
        ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED,
        ExportException.ERROR_CODE_DECODER_INIT_FAILED,
        ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> ConversionError.Kind.NO_ENCODER

        ExportException.ERROR_CODE_IO_FILE_NOT_FOUND -> ConversionError.Kind.FILE_NOT_FOUND
        ExportException.ERROR_CODE_IO_NO_PERMISSION -> ConversionError.Kind.PERMISSION_DENIED
        else -> ConversionError.Kind.ENGINE_CRASH
    }

    private sealed interface TransformOutcome {
        data class Success(val exportResult: ExportResult) : TransformOutcome
        data class Error(val exception: ExportException) : TransformOutcome
    }

    private companion object {
        const val TAG = "Media3Engine"
        const val PROGRESS_POLL_INTERVAL_MS = 300L
        val SUPPORTED_AUDIO_FORMATS = setOf("AAC", "M4A")
    }
}
