package com.henjicc.swiftformat.engine.image

import com.henjicc.swiftformat.core.model.SizePreset
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 图片尺寸档位 → 目标像素尺寸（见 SPEC 5.6）。纯函数，不依赖 Android API，便于单元测试。
 *
 * 规则：
 * - 按最长边缩放，保持宽高比。
 * - 不允许放大（目标最长边 ≥ 原图最长边时保持原图尺寸）。
 * - [SizePreset.VideoResolution] 不适用于图片，按保持原始处理（防御性兜底，正常不会传入）。
 */
object ImageSizeMapper {

    data class Dimensions(val width: Int, val height: Int) {
        /** 用于 EXIF 90°/270° 旋转：解码时的原始像素方向与显示方向宽高互换。 */
        fun swapped(): Dimensions = Dimensions(height, width)
    }

    fun targetDimensions(source: Dimensions, size: SizePreset?): Dimensions {
        if (source.width <= 0 || source.height <= 0) return source
        return when (size) {
            null, SizePreset.Original, is SizePreset.VideoResolution -> source
            is SizePreset.ImageLongestEdge -> scaleToLongestEdge(source, size.pixels)
            is SizePreset.Custom -> scaleToCustom(source, size)
        }
    }

    private fun scaleToLongestEdge(source: Dimensions, longestEdge: Int): Dimensions {
        val currentLongest = max(source.width, source.height)
        if (longestEdge >= currentLongest) return source // 不放大
        val scale = longestEdge.toDouble() / currentLongest
        return Dimensions(
            width = (source.width * scale).roundToInt().coerceAtLeast(1),
            height = (source.height * scale).roundToInt().coerceAtLeast(1),
        )
    }

    private fun scaleToCustom(source: Dimensions, custom: SizePreset.Custom): Dimensions {
        val targetWidth = custom.width
        val targetHeight = custom.height
        return when {
            targetWidth != null && targetHeight != null -> Dimensions(targetWidth, targetHeight)
            targetWidth != null -> {
                val scale = targetWidth.toDouble() / source.width
                Dimensions(targetWidth, (source.height * scale).roundToInt().coerceAtLeast(1))
            }

            targetHeight != null -> {
                val scale = targetHeight.toDouble() / source.height
                Dimensions((source.width * scale).roundToInt().coerceAtLeast(1), targetHeight)
            }

            else -> source
        }
    }

    /** 计算解码采样率（2 的幂），用于在解码阶段就降采样，避免先全尺寸解码再缩放。 */
    fun sampleSizeFor(source: Dimensions, target: Dimensions): Int {
        if (target.width <= 0 || target.height <= 0) return 1
        var sampleSize = 1
        var halfWidth = source.width / 2
        var halfHeight = source.height / 2
        while (halfWidth / sampleSize >= target.width && halfHeight / sampleSize >= target.height) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
