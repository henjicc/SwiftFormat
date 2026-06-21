package com.henjicc.swiftformat.core.model

/**
 * 统一尺寸档位（见 SPEC 5.6 / 11.3）。
 * 视频按分辨率高度、图片按最长边像素表示；音频不使用尺寸。
 */
sealed interface SizePreset {
    /** 保持原始尺寸（默认）。 */
    data object Original : SizePreset

    /** 视频分辨率，按短边/标准高度解释并保持比例。 */
    data class VideoResolution(val height: Int) : SizePreset

    /** 图片最长边像素。 */
    data class ImageLongestEdge(val pixels: Int) : SizePreset

    /** 自定义宽高（保持比例时另一边可为空）。 */
    data class Custom(val width: Int?, val height: Int?) : SizePreset
}
