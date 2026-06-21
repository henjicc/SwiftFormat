package com.henjicc.swiftformat.core.model

/**
 * 各媒体类型可选输出格式与动态显示规则（见 SPEC 3.1 / 5.7）。
 * 纯数据/规则，不含字符串资源依赖，便于单元测试；格式名按 SPEC 7.3 保持大写英文缩写不做翻译。
 *
 * 设备实际编码能力检查（隐藏设备无法生成的格式）留待 TASK-03/04 接入真实引擎后补充，
 * 当前按 SPEC 第一版目标格式静态列出。
 */
object OutputFormatCatalog {

    enum class EngineHint {
        NATIVE_IMAGE,
        HEIF_WRITER,
        MEDIA3,
        FFMPEG,
    }

    data class OutputOption(
        val format: String,
        val targetMediaType: MediaType,
        val preferredEngine: EngineHint,
        val qualityApplicable: Boolean,
        val sizeApplicable: Boolean,
        val sortOrder: Int,
    )

    private val imageOptions = listOf(
        OutputOption("JPG", MediaType.IMAGE, EngineHint.NATIVE_IMAGE, qualityApplicable = true, sizeApplicable = true, sortOrder = 10),
        OutputOption("PNG", MediaType.IMAGE, EngineHint.NATIVE_IMAGE, qualityApplicable = false, sizeApplicable = true, sortOrder = 20),
        OutputOption("WEBP", MediaType.IMAGE, EngineHint.NATIVE_IMAGE, qualityApplicable = true, sizeApplicable = true, sortOrder = 30),
        OutputOption("BMP", MediaType.IMAGE, EngineHint.FFMPEG, qualityApplicable = false, sizeApplicable = true, sortOrder = 40),
        OutputOption("TIFF", MediaType.IMAGE, EngineHint.FFMPEG, qualityApplicable = false, sizeApplicable = true, sortOrder = 50),
        OutputOption("HEIC", MediaType.IMAGE, EngineHint.HEIF_WRITER, qualityApplicable = true, sizeApplicable = true, sortOrder = 60),
        OutputOption("AVIF", MediaType.IMAGE, EngineHint.HEIF_WRITER, qualityApplicable = true, sizeApplicable = true, sortOrder = 70),
    )

    private val videoOptions = listOf(
        OutputOption("MP4", MediaType.VIDEO, EngineHint.MEDIA3, qualityApplicable = true, sizeApplicable = true, sortOrder = 10),
        OutputOption("MOV", MediaType.VIDEO, EngineHint.FFMPEG, qualityApplicable = true, sizeApplicable = true, sortOrder = 20),
        OutputOption("WEBM", MediaType.VIDEO, EngineHint.FFMPEG, qualityApplicable = true, sizeApplicable = true, sortOrder = 30),
        OutputOption("MKV", MediaType.VIDEO, EngineHint.FFMPEG, qualityApplicable = true, sizeApplicable = true, sortOrder = 40),
        OutputOption("MP3", MediaType.AUDIO, EngineHint.FFMPEG, qualityApplicable = true, sizeApplicable = false, sortOrder = 50),
        OutputOption("M4A", MediaType.AUDIO, EngineHint.FFMPEG, qualityApplicable = true, sizeApplicable = false, sortOrder = 60),
        OutputOption("WAV", MediaType.AUDIO, EngineHint.FFMPEG, qualityApplicable = false, sizeApplicable = false, sortOrder = 70),
        OutputOption("FLAC", MediaType.AUDIO, EngineHint.FFMPEG, qualityApplicable = false, sizeApplicable = false, sortOrder = 80),
    )

    private val audioOptions = listOf(
        OutputOption("MP3", MediaType.AUDIO, EngineHint.FFMPEG, qualityApplicable = true, sizeApplicable = false, sortOrder = 10),
        OutputOption("M4A", MediaType.AUDIO, EngineHint.MEDIA3, qualityApplicable = true, sizeApplicable = false, sortOrder = 20),
        OutputOption("AAC", MediaType.AUDIO, EngineHint.MEDIA3, qualityApplicable = true, sizeApplicable = false, sortOrder = 30),
        OutputOption("WAV", MediaType.AUDIO, EngineHint.FFMPEG, qualityApplicable = false, sizeApplicable = false, sortOrder = 40),
        OutputOption("FLAC", MediaType.AUDIO, EngineHint.FFMPEG, qualityApplicable = false, sizeApplicable = false, sortOrder = 50),
        OutputOption("OGG", MediaType.AUDIO, EngineHint.FFMPEG, qualityApplicable = true, sizeApplicable = false, sortOrder = 60),
    )

    fun outputOptions(mediaType: MediaType): List<OutputOption> = when (mediaType) {
        MediaType.IMAGE -> imageOptions.sortedBy(OutputOption::sortOrder)
        MediaType.VIDEO -> videoOptions.sortedBy(OutputOption::sortOrder)
        MediaType.AUDIO -> audioOptions.sortedBy(OutputOption::sortOrder)
        MediaType.UNKNOWN -> emptyList()
    }

    fun outputFormats(mediaType: MediaType): List<String> = when (mediaType) {
        MediaType.UNKNOWN -> emptyList()
        else -> outputOptions(mediaType).map(OutputOption::format)
    }

    fun defaultFormat(mediaType: MediaType): String = when (mediaType) {
        MediaType.IMAGE -> "WEBP"
        MediaType.VIDEO -> "MP4"
        MediaType.AUDIO -> "MP3"
        MediaType.UNKNOWN -> ""
    }

    fun option(mediaType: MediaType, outputFormat: String): OutputOption? =
        outputOptions(mediaType).firstOrNull { it.format.equals(outputFormat, ignoreCase = true) }

    fun targetMediaTypeFor(sourceMediaType: MediaType, outputFormat: String): MediaType =
        option(sourceMediaType, outputFormat)?.targetMediaType ?: sourceMediaType

    fun isQualityApplicable(mediaType: MediaType, outputFormat: String): Boolean =
        option(mediaType, outputFormat)?.qualityApplicable ?: true

    fun isQualityApplicable(outputFormat: String): Boolean =
        (imageOptions + videoOptions + audioOptions)
            .firstOrNull { it.format.equals(outputFormat, ignoreCase = true) }
            ?.qualityApplicable
            ?: true

    fun isSizeApplicable(mediaType: MediaType, outputFormat: String): Boolean =
        option(mediaType, outputFormat)?.sizeApplicable ?: sizePresets(mediaType).isNotEmpty()

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
        targetMediaType = targetMediaTypeFor(mediaType, defaultFormat(mediaType)),
        quality = QualityPreset.HIGH,
        size = sizePresets(mediaType).firstOrNull(), // Original 或 null（音频/未知）
    )
}
