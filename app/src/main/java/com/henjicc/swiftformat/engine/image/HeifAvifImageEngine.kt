package com.henjicc.swiftformat.engine.image

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.heifwriter.HeifWriter
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/**
 * HEIC 图片输出引擎。
 * 复用现有位图解码、EXIF 旋正与尺寸缩放链路，再使用 AndroidX HeifWriter 编码到目标容器。
 * AVIF 不在本引擎处理：AndroidX `AvifWriter` 依赖设备硬件 AV1 编码器，实机验证发现部分设备
 * 编出的文件连系统自带解码器都读不回来，已改由 [com.henjicc.swiftformat.engine.ffmpeg.FfmpegStillImageEngine]
 * 用 libaom-av1 软件编码处理。
 */
class HeifAvifImageEngine(
    context: Context,
    private val logger: Logger,
) : ConversionEngine {

    private val appContext = context.applicationContext
    private val activeJobs = ConcurrentHashMap<String, Job>()

    override fun supports(request: ConversionRequest): Boolean {
        if (request.input.mediaType != MediaType.IMAGE || request.targetMediaType != MediaType.IMAGE) return false
        return request.outputFormat.uppercase() == "HEIC" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
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
        }
    }

    override suspend fun cancel(taskId: String) {
        activeJobs[taskId]?.cancel()
    }

    private suspend fun doConvert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        val destinationUri = (request.destination as? OutputDestination.ResolvedUri)?.uri
            ?: return failure(ConversionError.Kind.OUTPUT_NOT_WRITABLE, "destination not resolved")
        if (!supports(request)) {
            return failure(ConversionError.Kind.UNSUPPORTED_IMAGE_OUTPUT, "unsupported heif request: ${request.outputFormat}")
        }

        onProgress(ConversionProgress(0f))

        val rawBounds = decodeImageBounds(appContext, request.input.uri, logger, TAG)
            ?.let { ImageSizeMapper.Dimensions(it.width, it.height) }
            ?: return failure(ConversionError.Kind.CORRUPT_INPUT, "cannot decode bounds")
        coroutineContext.ensureActive()

        val orientation = readExifOrientation(appContext, request.input.uri)
        val swapsDimensions = orientation == android.media.ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == android.media.ExifInterface.ORIENTATION_ROTATE_270
        val displayBounds = if (swapsDimensions) rawBounds.swapped() else rawBounds
        val target = ImageSizeMapper.targetDimensions(displayBounds, request.size)
        val rawTarget = if (swapsDimensions) target.swapped() else target
        val sampleSize = ImageSizeMapper.sampleSizeFor(rawBounds, rawTarget)

        val sampled = decodeSampledBitmap(appContext, request.input.uri, sampleSize, logger, TAG)
            ?: return failure(ConversionError.Kind.CORRUPT_INPUT, "cannot decode bitmap")
        coroutineContext.ensureActive()
        onProgress(ConversionProgress(0.4f))

        val oriented = applyExifOrientation(sampled, orientation)
        val resized = resizeIfNeeded(oriented, target)
        coroutineContext.ensureActive()
        onProgress(ConversionProgress(0.7f))

        val tempFile = File(appContext.cacheDir, "heif_${request.id}.${request.outputFormat.lowercase()}")
        try {
            val quality = ImageQualityMapper.compressQuality(request.quality ?: QualityPreset.HIGH)
            val encodeResult = runCatching {
                when (request.outputFormat.uppercase()) {
                    "HEIC" -> encodeHeic(tempFile, resized, quality)
                    else -> error("unsupported heif output: ${request.outputFormat}")
                }
            }
            resized.recycle()
            encodeResult.exceptionOrNull()?.let { exception ->
                return failure(ConversionError.Kind.NO_ENCODER, exception.message ?: "image encoder unavailable")
            }

            coroutineContext.ensureActive()
            if (!tempFile.exists() || tempFile.length() == 0L) {
                return failure(ConversionError.Kind.OUTPUT_VALIDATION_FAILED, "encoded image is empty")
            }
            if (decodeImageBounds(tempFile, logger, TAG) == null) {
                return failure(ConversionError.Kind.OUTPUT_VALIDATION_FAILED, "encoded image cannot be decoded")
            }
            onProgress(ConversionProgress(0.9f))

            val sizeBytes = withContext(Dispatchers.IO) { copyToDestination(tempFile, destinationUri) }
            onProgress(ConversionProgress(1f))
            return ConversionResult.Success(destinationUri, sizeBytes)
        } finally {
            if (!resized.isRecycled) resized.recycle()
            tempFile.delete()
        }
    }

    private fun encodeHeic(file: File, bitmap: Bitmap, quality: Int) {
        val writer = HeifWriter.Builder(file.absolutePath, bitmap.width, bitmap.height, HeifWriter.INPUT_MODE_BITMAP)
            .setQuality(quality)
            .setMaxImages(1)
            .build()
        writer.use {
            it.start()
            it.addBitmap(bitmap)
            it.stop(0)
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

    private fun failure(kind: ConversionError.Kind, message: String?) =
        ConversionResult.Failure(ConversionError(kind, message))

    private companion object {
        const val TAG = "HeifAvifImageEngine"
    }
}
