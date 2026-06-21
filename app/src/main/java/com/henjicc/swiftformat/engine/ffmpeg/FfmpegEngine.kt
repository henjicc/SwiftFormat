package com.henjicc.swiftformat.engine.ffmpeg

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputDestination
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.api.ConversionEngine
import com.henjicc.swiftformat.engine.api.ConversionProgress
import com.henjicc.swiftformat.engine.api.ConversionResult
import com.henjicc.swiftformat.engine.media.VideoSizeMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于社区维护的 FFmpegKit 16KB fork（见 build.gradle.kts 依赖说明）的扩展格式引擎（SPEC 10.5）。
 * 范围：
 * - AUDIO → MP3/OGG/FLAC/WAV
 * - VIDEO → MOV/WEBM/MKV
 * - VIDEO → MP3/M4A/WAV/FLAC（提取首条音轨）
 *
 * 所有命令拼接收敛在 [FfmpegCommandBuilder]，本类及 UI/业务层不直接拼接 FFmpeg 参数字符串。
 * 临时文件策略（SPEC 12.2）：源 Uri → 缓存临时文件 → FFmpeg 转换到另一缓存临时文件 → 校验非空 →
 * 拷贝到目标 Uri → 清理两个临时文件，全部在 finally 中保证执行。
 */
class FfmpegEngine(
    context: Context,
    private val logger: Logger,
) : ConversionEngine {

    private val appContext = context.applicationContext
    private val activeSessions = ConcurrentHashMap<String, FFmpegSession>()

    override fun supports(request: ConversionRequest): Boolean = when (request.input.mediaType) {
        MediaType.AUDIO ->
            request.targetMediaType == MediaType.AUDIO && request.outputFormat.uppercase() in SUPPORTED_AUDIO_FORMATS

        MediaType.VIDEO -> when {
            request.targetMediaType == MediaType.VIDEO -> request.outputFormat.uppercase() in SUPPORTED_VIDEO_FORMATS
            request.targetMediaType == MediaType.AUDIO -> request.outputFormat.uppercase() in SUPPORTED_VIDEO_EXTRACT_FORMATS
            else -> false
        }

        else -> false
    }

    override suspend fun convert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult = withContext(Dispatchers.IO) {
        try {
            doConvert(request, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.e(TAG, "convert failed: ${request.id}", e)
            ConversionResult.Failure(ConversionError(ConversionError.Kind.ENGINE_CRASH, e.message, e))
        } finally {
            activeSessions.remove(request.id)
        }
    }

    override suspend fun cancel(taskId: String) {
        activeSessions[taskId]?.cancel()
    }

    private suspend fun doConvert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        val destinationUri = (request.destination as? OutputDestination.ResolvedUri)?.uri
            ?: return failure(ConversionError.Kind.OUTPUT_NOT_WRITABLE, "destination not resolved")
        val mode = try {
            resolveMode(request)
        } catch (e: IllegalArgumentException) {
            return unsupportedOutputFailure(request, e.message)
        }

        onProgress(ConversionProgress(0f))

        val inputTemp = File(appContext.cacheDir, "ffmpeg_in_${request.id}")
        val outputTemp = File(appContext.cacheDir, "ffmpeg_out_${request.id}.${extensionFor(request.outputFormat)}")
        try {
            val sourceSize = request.input.sizeBytes ?: 0L
            val requiredFreeBytes = sourceSize * 2 + MIN_FREE_BYTES_MARGIN
            if (appContext.cacheDir.usableSpace < requiredFreeBytes) {
                return failure(ConversionError.Kind.INSUFFICIENT_STORAGE, "not enough cache space")
            }

            withContext(Dispatchers.IO) { copyUriToFile(request.input.uri, inputTemp) }

            val probe = probeMediaInformation(inputTemp.absolutePath)
            val quality = request.quality ?: QualityPreset.HIGH
            val args = try {
                when (mode) {
                    FfmpegMode.AUDIO_TRANSCODE -> FfmpegCommandBuilder.buildAudioTranscodeArgs(
                        inputPath = inputTemp.absolutePath,
                        outputPath = outputTemp.absolutePath,
                        outputFormat = request.outputFormat,
                        quality = quality,
                    )

                    FfmpegMode.VIDEO_TRANSCODE -> {
                        val requiredProbe = probe
                            ?: return failure(ConversionError.Kind.CORRUPT_INPUT, "ffprobe failed to inspect input")
                        if (requiredProbe.videoStreams.isEmpty()) {
                            return failure(ConversionError.Kind.CORRUPT_INPUT, "ffprobe found no video stream")
                        }
                        val primaryVideoStream = requiredProbe.primaryVideoStream
                        val sourceDimensions = VideoSizeMapper.Dimensions(
                            width = request.input.width ?: primaryVideoStream?.width?.toInt() ?: 0,
                            height = request.input.height ?: primaryVideoStream?.height?.toInt() ?: 0,
                        )
                        val sourceBitrate = primaryVideoStream?.bitrate?.toLongOrNull()
                            ?: requiredProbe.mediaInformation?.bitrate?.toLongOrNull()
                        FfmpegCommandBuilder.buildVideoTranscodeArgs(
                            inputPath = inputTemp.absolutePath,
                            outputPath = outputTemp.absolutePath,
                            outputFormat = request.outputFormat,
                            quality = quality,
                            size = request.size,
                            sourceDimensions = sourceDimensions,
                            frameRate = primaryVideoStream?.averageFrameRate.parseFrameRate(),
                            sourceBitrateBps = sourceBitrate,
                        )
                    }

                    FfmpegMode.VIDEO_EXTRACT_AUDIO -> {
                        val requiredProbe = probe
                            ?: return failure(ConversionError.Kind.CORRUPT_INPUT, "ffprobe failed to inspect input")
                        if (requiredProbe.audioStreams.isEmpty()) {
                            return failure(ConversionError.Kind.NO_AUDIO_TRACK, "source video has no audio stream")
                        }
                        FfmpegCommandBuilder.buildVideoExtractAudioArgs(
                            inputPath = inputTemp.absolutePath,
                            outputPath = outputTemp.absolutePath,
                            outputFormat = request.outputFormat,
                            quality = quality,
                        )
                    }
                }
            } catch (e: IllegalArgumentException) {
                return unsupportedOutputFailure(request, e.message)
            }.toTypedArray()

            val durationMs = request.input.durationMs
            val sessionDeferred = CompletableDeferred<FFmpegSession>()
            val session = FFmpegKit.executeWithArgumentsAsync(
                args,
                { completed -> sessionDeferred.complete(completed) },
                null,
                { statistics ->
                    if (durationMs != null && durationMs > 0) {
                        val fraction = (statistics.time / durationMs).toFloat().coerceIn(0f, 1f)
                        onProgress(ConversionProgress(fraction * 0.9f))
                    }
                },
            )
            activeSessions[request.id] = session
            val completedSession = sessionDeferred.await()

            return when {
                ReturnCode.isSuccess(completedSession.returnCode) -> {
                    if (!outputTemp.exists() || outputTemp.length() == 0L) {
                        failure(ConversionError.Kind.OUTPUT_VALIDATION_FAILED, "ffmpeg produced empty output")
                    } else if (!validateOutput(mode, outputTemp.absolutePath)) {
                        failure(ConversionError.Kind.OUTPUT_VALIDATION_FAILED, "ffprobe validation failed")
                    } else {
                        val sizeBytes = withContext(Dispatchers.IO) { copyToDestination(outputTemp, destinationUri) }
                        onProgress(ConversionProgress(1f))
                        ConversionResult.Success(destinationUri, sizeBytes)
                    }
                }

                ReturnCode.isCancel(completedSession.returnCode) ->
                    failure(ConversionError.Kind.CANCELLED, "ffmpeg session cancelled")

                else -> failure(
                    ConversionError.Kind.ENGINE_CRASH,
                    completedSession.failStackTrace ?: completedSession.allLogsAsString,
                )
            }
        } finally {
            inputTemp.delete()
            outputTemp.delete()
        }
    }

    private fun copyUriToFile(uri: Uri, destination: File) {
        val inputStream = appContext.contentResolver.openInputStream(uri)
            ?: error("cannot open input stream")
        inputStream.use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
    }

    private fun copyToDestination(tempFile: File, destinationUri: Uri): Long {
        val outputStream = appContext.contentResolver.openOutputStream(destinationUri)
            ?: error("cannot open output stream")
        outputStream.use { out -> tempFile.inputStream().use { input -> input.copyTo(out) } }
        return tempFile.length()
    }

    private fun extensionFor(outputFormat: String): String = outputFormat.lowercase()

    private fun resolveMode(request: ConversionRequest): FfmpegMode = when {
        request.input.mediaType == MediaType.AUDIO && request.targetMediaType == MediaType.AUDIO ->
            FfmpegMode.AUDIO_TRANSCODE

        request.input.mediaType == MediaType.VIDEO && request.targetMediaType == MediaType.VIDEO ->
            FfmpegMode.VIDEO_TRANSCODE

        request.input.mediaType == MediaType.VIDEO && request.targetMediaType == MediaType.AUDIO ->
            FfmpegMode.VIDEO_EXTRACT_AUDIO

        request.input.mediaType == MediaType.VIDEO ->
            throw IllegalArgumentException("unsupported video target media type: ${request.targetMediaType}")

        else -> throw IllegalArgumentException("unsupported ffmpeg request: ${request.input.mediaType} -> ${request.outputFormat}")
    }

    private fun validateOutput(mode: FfmpegMode, outputPath: String): Boolean {
        val probe = probeMediaInformation(outputPath) ?: return false
        return when (mode) {
            FfmpegMode.AUDIO_TRANSCODE -> probe.audioStreams.isNotEmpty()
            FfmpegMode.VIDEO_TRANSCODE -> probe.videoStreams.isNotEmpty()
            FfmpegMode.VIDEO_EXTRACT_AUDIO -> probe.audioStreams.isNotEmpty() && probe.videoStreams.isEmpty()
        }
    }

    private fun failure(kind: ConversionError.Kind, message: String?) =
        ConversionResult.Failure(ConversionError(kind, message))

    private fun unsupportedOutputFailure(request: ConversionRequest, message: String?) =
        failure(
            kind = if (request.input.mediaType == MediaType.VIDEO) {
                ConversionError.Kind.UNSUPPORTED_VIDEO_OUTPUT
            } else {
                ConversionError.Kind.UNSUPPORTED_OUTPUT
            },
            message = message,
        )

    private enum class FfmpegMode {
        AUDIO_TRANSCODE,
        VIDEO_TRANSCODE,
        VIDEO_EXTRACT_AUDIO,
    }

    private companion object {
        const val TAG = "FfmpegEngine"
        const val MIN_FREE_BYTES_MARGIN = 16L * 1024 * 1024
        val SUPPORTED_AUDIO_FORMATS = setOf("MP3", "OGG", "FLAC", "WAV")
        val SUPPORTED_VIDEO_FORMATS = setOf("MOV", "WEBM", "MKV")
        val SUPPORTED_VIDEO_EXTRACT_FORMATS = setOf("MP3", "M4A", "WAV", "FLAC")
    }
}

private fun String?.parseFrameRate(): Double {
    if (this.isNullOrBlank()) return 30.0
    val parts = split('/')
    return when (parts.size) {
        2 -> {
            val numerator = parts[0].toDoubleOrNull()
            val denominator = parts[1].toDoubleOrNull()
            if (numerator != null && denominator != null && denominator != 0.0) numerator / denominator else 30.0
        }

        else -> toDoubleOrNull() ?: 30.0
    }
}
