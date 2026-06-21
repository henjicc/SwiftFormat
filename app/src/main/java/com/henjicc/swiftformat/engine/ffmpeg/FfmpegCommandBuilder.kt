package com.henjicc.swiftformat.engine.ffmpeg

import com.henjicc.swiftformat.core.model.QualityPreset

/**
 * 纯函数构建 FFmpeg 命令参数（见 SPEC 10.5：所有 FFmpeg 命令只能在本模块内部生成）。
 * 不依赖 Android 框架，便于单元测试；[FfmpegEngine] 是本对象的唯一调用方。
 */
object FfmpegCommandBuilder {

    /** 仅覆盖 [OutputFormatCatalog] 中交给本引擎的音频格式：MP3/FLAC/WAV。 */
    fun buildAudioArgs(
        inputPath: String,
        outputPath: String,
        outputFormat: String,
        quality: QualityPreset?,
    ): List<String> {
        val codecArgs = when (outputFormat.uppercase()) {
            "MP3" -> {
                val kbps = FfmpegAudioBitrateMapper.mp3TargetBitrateBps(quality ?: QualityPreset.HIGH) / 1000
                listOf("-acodec", "libmp3lame", "-b:a", "${kbps}k")
            }

            "FLAC" -> listOf("-acodec", "flac")
            "WAV" -> listOf("-acodec", "pcm_s16le")
            else -> error("FfmpegEngine 不支持的输出格式: $outputFormat")
        }
        return listOf("-y", "-i", inputPath, "-vn") + codecArgs + listOf(outputPath)
    }
}
