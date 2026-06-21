package com.henjicc.swiftformat.engine.media

import com.henjicc.swiftformat.core.model.SizePreset
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 视频尺寸档位 → 目标像素尺寸（见 SPEC 5.6）。纯函数，不依赖 Media3/Android API，便于单元测试。
 *
 * 规则：
 * - 按短边缩放并保持宽高比（"1080P" 等档位以短边解释，竖屏视频保持原始方向）。
 * - 不允许放大（目标短边 ≥ 原短边时保持原尺寸）。
 * - 宽高强制取偶数，避免部分硬件编码器拒绝奇数尺寸。
 * - [SizePreset.ImageLongestEdge] 不适用于视频，按保持原始处理（防御性兜底，正常不会传入）。
 */
object VideoSizeMapper {

    data class Dimensions(val width: Int, val height: Int)

    fun targetDimensions(source: Dimensions, size: SizePreset?): Dimensions {
        if (source.width <= 0 || source.height <= 0) return source
        return when (size) {
            null, SizePreset.Original, is SizePreset.ImageLongestEdge -> source
            is SizePreset.VideoResolution -> scaleToShortEdge(source, size.height)
            is SizePreset.Custom -> scaleToCustom(source, size)
        }
    }

    private fun scaleToShortEdge(source: Dimensions, targetShortEdge: Int): Dimensions {
        val currentShortEdge = min(source.width, source.height)
        if (targetShortEdge >= currentShortEdge) return source // 不放大
        val scale = targetShortEdge.toDouble() / currentShortEdge
        return Dimensions(
            width = roundToEven(source.width * scale),
            height = roundToEven(source.height * scale),
        )
    }

    private fun scaleToCustom(source: Dimensions, custom: SizePreset.Custom): Dimensions {
        val targetWidth = custom.width
        val targetHeight = custom.height
        return when {
            targetWidth != null && targetHeight != null ->
                Dimensions(roundToEven(targetWidth.toDouble()), roundToEven(targetHeight.toDouble()))

            targetWidth != null -> {
                val scale = targetWidth.toDouble() / source.width
                Dimensions(roundToEven(targetWidth.toDouble()), roundToEven(source.height * scale))
            }

            targetHeight != null -> {
                val scale = targetHeight.toDouble() / source.height
                Dimensions(roundToEven(source.width * scale), roundToEven(targetHeight.toDouble()))
            }

            else -> source
        }
    }

    private fun roundToEven(value: Double): Int {
        val rounded = value.roundToInt().coerceAtLeast(2)
        return if (rounded % 2 == 0) rounded else rounded + 1
    }
}
