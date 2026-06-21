package com.henjicc.swiftformat.engine.image

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.henjicc.swiftformat.core.common.Logger
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
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/**
 * 原生图片转换引擎（见 SPEC 10.1/10.3）：JPG/PNG/WebP 互转，按目标尺寸采样解码避免 OOM，
 * 自动校正 EXIF 旋转，不修改源文件。仅处理 [MediaType.IMAGE]。
 */
class NativeImageEngine(
    context: Context,
    private val logger: Logger,
) : ConversionEngine {

    private val appContext = context.applicationContext
    private val activeJobs = ConcurrentHashMap<String, Job>()

    override fun supports(request: ConversionRequest): Boolean =
        request.input.mediaType == MediaType.IMAGE &&
            request.outputFormat.uppercase() in SUPPORTED_OUTPUT_FORMATS

    override suspend fun convert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult = withContext(Dispatchers.Default) {
        activeJobs[request.id] = coroutineContext[Job]!!
        try {
            doConvert(request, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(TAG, "convert failed: ${request.id}", e)
            ConversionResult.Failure(ConversionError(ConversionError.Kind.ENGINE_CRASH, e.message, e))
        } finally {
            activeJobs.remove(request.id)
        }
    }

    private suspend fun doConvert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        val resolver = appContext.contentResolver
        val destinationUri = (request.destination as? OutputDestination.ResolvedUri)?.uri
            ?: return failure(ConversionError.Kind.OUTPUT_NOT_WRITABLE, "destination not resolved")

        onProgress(ConversionProgress(0f))

        // EXIF 旋转会交换宽高，目标尺寸必须按“旋正后”的视觉方向计算，否则旋转后会被强行拉伸变形。
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

        val (format, quality) = compressFormatAndQuality(request.outputFormat, request.quality)
        val outputStream = runCatching { resolver.openOutputStream(destinationUri) }.getOrNull()
            ?: return failure(ConversionError.Kind.OUTPUT_NOT_WRITABLE, "cannot open output stream")

        val sizeBytes = outputStream.use { stream ->
            val counting = CountingOutputStream(stream)
            resized.compress(format, quality, counting)
            counting.count
        }
        resized.recycle()

        onProgress(ConversionProgress(1f))
        return ConversionResult.Success(destinationUri, sizeBytes)
    }

    override suspend fun cancel(taskId: String) {
        activeJobs[taskId]?.cancel()
    }

    private fun failure(kind: ConversionError.Kind, message: String) =
        ConversionResult.Failure(ConversionError(kind, message))

    private fun resizeIfNeeded(bitmap: Bitmap, target: ImageSizeMapper.Dimensions): Bitmap {
        if (bitmap.width == target.width && bitmap.height == target.height) return bitmap
        val scaled = Bitmap.createScaledBitmap(bitmap, target.width, target.height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun compressFormatAndQuality(
        outputFormat: String,
        quality: QualityPreset?,
    ): Pair<Bitmap.CompressFormat, Int> {
        val q = ImageQualityMapper.compressQuality(quality ?: QualityPreset.HIGH)
        return when (outputFormat.uppercase()) {
            "PNG" -> Bitmap.CompressFormat.PNG to 100
            "WEBP" -> webpLossyFormat() to q
            else -> Bitmap.CompressFormat.JPEG to q
        }
    }

    @Suppress("DEPRECATION")
    private fun webpLossyFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

    /** 统计实际写出的字节数，避免转换后再额外查询输出 Uri 的大小。 */
    private class CountingOutputStream(private val delegate: OutputStream) : OutputStream() {
        var count: Long = 0
            private set

        override fun write(b: Int) {
            delegate.write(b)
            count += 1
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            count += len
        }

        override fun flush() = delegate.flush()
        override fun close() = delegate.close()
    }

    private companion object {
        const val TAG = "NativeImageEngine"
        val SUPPORTED_OUTPUT_FORMATS = setOf("JPG", "PNG", "WEBP")
    }
}
