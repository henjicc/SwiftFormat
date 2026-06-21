package com.henjicc.swiftformat.core.model

/**
 * 各媒体类型可选输出格式与动态显示规则（见 SPEC 3.1 / 5.7）。
 * 纯数据/规则，不含字符串资源依赖，便于单元测试；格式名按 SPEC 7.3 保持大写英文缩写不做翻译。
 *
 * 设备实际编码能力检查（隐藏设备无法生成的格式）留待 TASK-03/04 接入真实引擎后补充，
 * 当前按 SPEC 第一版目标格式静态列出。
 */
object OutputFormatCatalog {

    private val imageFormats = listOf("JPG", "PNG", "WEBP")
    private val videoFormats = listOf("MP4", "WEBM")
    private val audioFormats = listOf("MP3", "AAC", "WAV", "FLAC")

    /** 无损/不适用画质档位的格式，按 SPEC 5.7 隐藏质量行。 */
    private val qualityHiddenFormats = setOf("PNG", "WAV", "FLAC")

    fun outputFormats(mediaType: MediaType): List<String> = when (mediaType) {
        MediaType.IMAGE -> imageFormats
        MediaType.VIDEO -> videoFormats
        MediaType.AUDIO -> audioFormats
        MediaType.UNKNOWN -> emptyList()
    }

    fun defaultFormat(mediaType: MediaType): String = when (mediaType) {
        MediaType.IMAGE -> "WEBP"
        MediaType.VIDEO -> "MP4"
        MediaType.AUDIO -> "MP3"
        MediaType.UNKNOWN -> ""
    }

    fun isQualityApplicable(outputFormat: String): Boolean = outputFormat !in qualityHiddenFormats

    /** 尺寸选项，按 SPEC 5.6；音频/未知类型不提供尺寸（空列表 = 不显示该行）。 */
    fun sizePresets(mediaType: MediaType): List<SizePreset> = when (mediaType) {
        MediaType.IMAGE -> listOf(
            SizePreset.Original,
            SizePreset.ImageLongestEdge(3840),
            SizePreset.ImageLongestEdge(2560),
            SizePreset.ImageLongestEdge(1920),
            SizePreset.ImageLongestEdge(1280),
        )

        MediaType.VIDEO -> listOf(
            SizePreset.Original,
            SizePreset.VideoResolution(2160),
            SizePreset.VideoResolution(1440),
            SizePreset.VideoResolution(1080),
            SizePreset.VideoResolution(720),
            SizePreset.VideoResolution(480),
        )

        MediaType.AUDIO, MediaType.UNKNOWN -> emptyList()
    }

    /**
     * [quality] 始终保留用户选择（默认高），是否展示由 [isQualityApplicable] 在 UI 层按
     * 当前 [GroupConversionSettings.outputFormat] 动态判断，切换格式不丢失已选质量。
     */
    fun defaultSettings(mediaType: MediaType): GroupConversionSettings = GroupConversionSettings(
        mediaType = mediaType,
        outputFormat = defaultFormat(mediaType),
        quality = QualityPreset.HIGH,
        size = sizePresets(mediaType).firstOrNull(), // Original 或 null（音频/未知）
    )
}
