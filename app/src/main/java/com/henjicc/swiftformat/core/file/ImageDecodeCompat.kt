package com.henjicc.swiftformat.core.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.henjicc.swiftformat.core.common.Logger
import java.io.File

internal data class ImageBounds(val width: Int, val height: Int) {
    fun swapped(): ImageBounds = ImageBounds(height, width)
}

internal fun decodeImageBounds(
    context: Context,
    uri: Uri,
    logger: Logger,
    tag: String,
): ImageBounds? {
    val resolver = context.contentResolver
    val bitmapFactoryBounds = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                ImageBounds(options.outWidth, options.outHeight)
            } else {
                null
            }
        }
    }.onFailure { logger.w(tag, "decodeImageBounds(BitmapFactory) failed", it) }.getOrNull()
    if (bitmapFactoryBounds != null) return bitmapFactoryBounds

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return runCatching {
        val source = ImageDecoder.createSource(resolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source)
        bitmap.useBounds()
    }.onFailure { logger.w(tag, "decodeImageBounds(ImageDecoder) failed", it) }.getOrNull()
}

internal fun decodeImageBounds(file: File, logger: Logger, tag: String): ImageBounds? {
    val bitmapFactoryBounds = runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth > 0 && options.outHeight > 0) {
            ImageBounds(options.outWidth, options.outHeight)
        } else {
            null
        }
    }.onFailure { logger.w(tag, "decodeImageBounds(file) failed", it) }.getOrNull()
    if (bitmapFactoryBounds != null) return bitmapFactoryBounds

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return runCatching {
        val source = ImageDecoder.createSource(file)
        val bitmap = ImageDecoder.decodeBitmap(source)
        bitmap.useBounds()
    }.onFailure { logger.w(tag, "decodeImageBounds(file ImageDecoder) failed", it) }.getOrNull()
}

internal fun decodeSampledBitmap(
    context: Context,
    uri: Uri,
    sampleSize: Int,
    logger: Logger,
    tag: String,
): Bitmap? {
    val resolver = context.contentResolver
    val bitmapFactoryBitmap = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeStream(stream, null, options)
        }
    }.onFailure { logger.w(tag, "decodeSampledBitmap(BitmapFactory) failed", it) }.getOrNull()
    if (bitmapFactoryBitmap != null) return bitmapFactoryBitmap

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return runCatching {
        val source = ImageDecoder.createSource(resolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            if (sampleSize > 1) decoder.setTargetSampleSize(sampleSize)
            decoder.isMutableRequired = true
        }
    }.onFailure { logger.w(tag, "decodeSampledBitmap(ImageDecoder) failed", it) }.getOrNull()
}

internal fun readExifOrientation(context: Context, uri: Uri): Int = runCatching {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }
}.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

internal fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = android.graphics.Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}

private fun Bitmap.useBounds(): ImageBounds {
    val bounds = ImageBounds(width, height)
    recycle()
    return bounds
}
