package com.henjicc.swiftformat.engine.ffmpeg

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.common.toDebugMessage
import com.henjicc.swiftformat.core.file.applyExifOrientation
import com.henjicc.swiftformat.core.file.decodeImageBounds
import com.henjicc.swiftformat.core.file.decodeSampledBitmap
import com.henjicc.swiftformat.core.file.readExifOrientation
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputDestination
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.api.ConversionEngine
import com.henjicc.swiftformat.engine.api.ConversionProgress
import com.henjicc.swiftformat.engine.api.ConversionResult
import com.henjicc.swiftformat.engine.image.ImageSizeMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/**
 * 静态图片扩展格式引擎：负责 BMP/TIFF/AVIF 输出。
 * 先复用原生位图链路完成解码、EXIF 旋正与尺寸缩放，再交给 FFmpeg 写出目标容器，
 * 避免直接走 FFmpeg 丢失 JPEG 方向修正等现有图片链路行为。
 * AVIF 走 FFmpeg 的 libaom-av1 软件编码而非系统 `AvifWriter`：部分设备的硬件 AV1 编码器
 * 产出的文件连系统自带解码器都读不回来（实机已验证），libaom 软编码不依赖设备硬件，更稳定。
 */
class FfmpegStillImageEngine(
    context: Context,
    private val logger: Logger,
) : ConversionEngine {

    private val appContext = context.applicationContext
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeSessions = ConcurrentHashMap<String, FFmpegSession>()

    override fun supports(request: ConversionRequest): Boolean {
        if (request.input.mediaType != MediaType.IMAGE || request.targetMediaType != MediaType.IMAGE) return false
        val format = request.outputFormat.uppercase()
        if (format !in SUPPORTED_OUTPUT_FORMATS) return false
        return format != "AVIF" || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    override suspend fun convert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult = withContext(Dispatchers.Default) {
        activeJobs[request.id] = coroutineContext[Job]!!
        try {
            doConvert(request, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.e(TAG, "convert failed: ${request.id}", e)
            ConversionResult.Failure(ConversionError(ConversionError.Kind.ENGINE_CRASH, e.toDebugMessage(), e))
        } finally {
            activeJobs.remove(request.id)
            activeSessions.remove(request.id)
        }
    }

    override suspend fun cancel(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeSessions[taskId]?.cancel()
    }

    private suspend fun doConvert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        FfmpegRuntimeSupport.unavailableReason(logger)?.let { debugMessage ->
            return failure(ConversionError.Kind.ENGINE_CRASH, debugMessage)
        }

        val destinationUri = (request.destination as? OutputDestination.ResolvedUri)?.uri
            ?: return failure(ConversionError.Kind.OUTPUT_NOT_WRITABLE, "destination not resolved")
        if (request.outputFormat.uppercase() !in SUPPORTED_OUTPUT_FORMATS) {
            return failure(ConversionError.Kind.UNSUPPORTED_IMAGE_OUTPUT, "unsupported still image output: ${request.outputFormat}")
        }

        val sourceSize = request.input.sizeBytes ?: 0L
        val requiredFreeBytes = sourceSize * 3 + MIN_FREE_BYTES_MARGIN
        if (appContext.cacheDir.usableSpace < requiredFreeBytes) {
            return failure(ConversionError.Kind.INSUFFICIENT_STORAGE, "not enough cache space")
        }

        onProgress(ConversionProgress(0f))

        val preprocessedTemp = File(appContext.cacheDir, "ffmpeg_image_pre_${request.id}.png")
        val outputTemp = File(appContext.cacheDir, "ffmpeg_image_out_${request.id}.${request.outputFormat.lowercase()}")
        try {
            val rawBounds = decodeImageBounds(appContext, request.input.uri, logger, TAG)
                ?: return failure(ConversionError.Kind.CORRUPT_INPUT, "cannot decode image bounds")
            coroutineContext.ensureActive()
            onProgress(ConversionProgress(0.15f))

            val orientation = readExifOrientation(appContext, request.input.uri)
            val displayBounds = if (orientationSwapsDimensions(orientation)) rawBounds.swapped() else rawBounds
            val target = ImageSizeMapper.targetDimensions(
                ImageSizeMapper.Dimensions(displayBounds.width, displayBounds.height),
                request.size,
            )
            val rawTarget = if (orientationSwapsDimensions(orientation)) target.swapped() else target
            val sampleSize = ImageSizeMapper.sampleSizeFor(
                ImageSizeMapper.Dimensions(rawBounds.width, rawBounds.height),
                rawTarget,
            )

            val sampled = decodeSampledBitmap(appContext, request.input.uri, sampleSize, logger, TAG)
                ?: return failure(ConversionError.Kind.CORRUPT_INPUT, "cannot decode image bitmap")
            coroutineContext.ensureActive()

            val oriented = applyExifOrientation(sampled, orientation)
            val resized = resizeIfNeeded(oriented, target)
            preprocessedTemp.outputStream().use { stream ->
                resized.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            resized.recycle()
            coroutineContext.ensureActive()
            onProgress(ConversionProgress(0.55f))

            val sessionDeferred = CompletableDeferred<FFmpegSession>()
            val session = FFmpegKit.executeWithArgumentsAsync(
                FfmpegCommandBuilder.buildStillImageTranscodeArgs(
                    inputPath = preprocessedTemp.absolutePath,
                    outputPath = outputTemp.absolutePath,
                    outputFormat = request.outputFormat,
                    quality = request.quality ?: QualityPreset.HIGH,
                ).toTypedArray(),
                { completed -> sessionDeferred.complete(completed) },
            )
            activeSessions[request.id] = session
            val completedSession = sessionDeferred.await()

            return when {
                ReturnCode.isSuccess(completedSession.returnCode) -> {
                    onProgress(ConversionProgress(0.9f))
                    if (!outputTemp.exists() || outputTemp.length() == 0L) {
                        failure(ConversionError.Kind.OUTPUT_VALIDATION_FAILED, "ffmpeg produced empty still image output")
                    } else if (decodeImageBounds(outputTemp, logger, TAG) == null) {
                        failure(ConversionError.Kind.OUTPUT_VALIDATION_FAILED, "still image output bounds are unreadable")
                    } else {
                        val sizeBytes = withContext(Dispatchers.IO) { copyToDestination(outputTemp, destinationUri) }
                        onProgress(ConversionProgress(1f))
                        ConversionResult.Success(destinationUri, sizeBytes)
                    }
                }

                ReturnCode.isCancel(completedSession.returnCode) ->
                    failure(ConversionError.Kind.CANCELLED, "ffmpeg still image session cancelled")

                else -> failure(
                    ConversionError.Kind.ENGINE_CRASH,
                    completedSession.failStackTrace ?: completedSession.allLogsAsString,
                )
            }
        } finally {
            preprocessedTemp.delete()
            outputTemp.delete()
        }
    }

    private fun resizeIfNeeded(bitmap: Bitmap, target: ImageSizeMapper.Dimensions): Bitmap {
        if (bitmap.width == target.width && bitmap.height == target.height) return bitmap
        val scaled = Bitmap.createScaledBitmap(bitmap, target.width, target.height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun copyToDestination(tempFile: File, destinationUri: android.net.Uri): Long {
        val outputStream = appContext.contentResolver.openOutputStream(destinationUri)
            ?: error("cannot open output stream")
        outputStream.use { out -> tempFile.inputStream().use { input -> input.copyTo(out) } }
        return tempFile.length()
    }

    private fun orientationSwapsDimensions(orientation: Int): Boolean =
        orientation == android.media.ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == android.media.ExifInterface.ORIENTATION_ROTATE_270

    private fun failure(kind: ConversionError.Kind, message: String?) =
        ConversionResult.Failure(ConversionError(kind, message))

    private companion object {
        const val TAG = "FfmpegStillImageEngine"
        const val MIN_FREE_BYTES_MARGIN = 16L * 1024 * 1024
        val SUPPORTED_OUTPUT_FORMATS = setOf("BMP", "TIFF", "AVIF")
    }
}
