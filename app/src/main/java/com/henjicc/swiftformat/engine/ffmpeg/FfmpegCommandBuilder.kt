package com.henjicc.swiftformat.engine.ffmpeg

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import com.henjicc.swiftformat.engine.media.AudioBitrateMapper
import com.henjicc.swiftformat.engine.media.VideoBitrateMapper
import com.henjicc.swiftformat.engine.media.VideoSizeMapper

/**
 * 纯函数构建 FFmpeg 命令参数（见 SPEC 10.5：所有 FFmpeg 命令只能在本模块内部生成）。
 * 不依赖 Android 框架，便于单元测试；[FfmpegEngine] 是本对象的唯一调用方。
 */
object FfmpegCommandBuilder {

    fun buildAudioTranscodeArgs(
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
            else -> error("FfmpegEngine 不支持的音频输出格式: $outputFormat")
        }
        return listOf("-y", "-i", inputPath, "-vn") + codecArgs + listOf(outputPath)
    }

    fun buildVideoTranscodeArgs(
        inputPath: String,
        outputPath: String,
        outputFormat: String,
        quality: QualityPreset,
        size: SizePreset?,
        sourceDimensions: VideoSizeMapper.Dimensions,
        frameRate: Double,
        sourceBitrateBps: Long?,
    ): List<String> {
        val targetDimensions = VideoSizeMapper.targetDimensions(sourceDimensions, size)
        val videoBitrateKbps = (
            VideoBitrateMapper.targetBitrateBps(
                preset = quality,
                targetWidth = targetDimensions.width,
                targetHeight = targetDimensions.height,
                frameRate = frameRate,
                sourceBitrateBps = sourceBitrateBps,
            ) / 1000L
            ).coerceAtLeast(1L)

        val scaleArgs = if (targetDimensions != sourceDimensions && targetDimensions.width > 0 && targetDimensions.height > 0) {
            listOf("-vf", "scale=${targetDimensions.width}:${targetDimensions.height}")
        } else {
            emptyList()
        }

        val codecArgs = when (outputFormat.uppercase()) {
            "WEBM" -> {
                val audioBitrateKbps = (OpusBitrateMapper.targetBitrateBps(quality) / 1000).coerceAtLeast(1)
                listOf(
                    "-c:v", "libvpx-vp9",
                    "-b:v", "${videoBitrateKbps}k",
                    "-c:a", "libopus",
                    "-b:a", "${audioBitrateKbps}k",
                )
            }

            "MKV" -> {
                val audioBitrateKbps = ((AudioBitrateMapper.targetBitrateBps("AAC", quality) ?: 192_000) / 1000).coerceAtLeast(1)
                listOf(
                    "-c:v", "libopenh264",
                    "-b:v", "${videoBitrateKbps}k",
                    "-c:a", "aac",
                    "-b:a", "${audioBitrateKbps}k",
                )
            }

            else -> error("FfmpegEngine 不支持的视频输出格式: $outputFormat")
        }

        return listOf("-y", "-i", inputPath, "-map", "0:v:0", "-map", "0:a:0?") +
            scaleArgs + codecArgs + listOf(outputPath)
    }

    fun buildVideoExtractAudioArgs(
        inputPath: String,
        outputPath: String,
        outputFormat: String,
        quality: QualityPreset,
    ): List<String> {
        return when (outputFormat.uppercase()) {
            "MP3" -> {
                val kbps = FfmpegAudioBitrateMapper.mp3TargetBitrateBps(quality) / 1000
                listOf(
                    "-y",
                    "-i",
                    inputPath,
                    "-map",
                    "0:a:0",
                    "-vn",
                    "-acodec",
                    "libmp3lame",
                    "-b:a",
                    "${kbps}k",
                    outputPath,
                )
            }

            else -> error("FfmpegEngine 不支持的视频提取音频格式: $outputFormat")
        }
    }
}
