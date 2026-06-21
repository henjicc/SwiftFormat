package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import com.henjicc.swiftformat.engine.ffmpeg.FfmpegCommandBuilder
import com.henjicc.swiftformat.engine.media.VideoSizeMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FfmpegCommandBuilderTest {

    @Test
    fun mp3AudioTranscode_usesLibmp3lameWithBitrate() {
        val args = FfmpegCommandBuilder.buildAudioTranscodeArgs("in.wav", "out.mp3", "MP3", QualityPreset.HIGH)
        assertEquals(listOf("-y", "-i", "in.wav", "-vn", "-acodec", "libmp3lame", "-b:a", "256k", "out.mp3"), args)
    }

    @Test
    fun webmVideoTranscode_usesVp9OpusAndKeepsAudioMap() {
        val args = FfmpegCommandBuilder.buildVideoTranscodeArgs(
            inputPath = "in.mp4",
            outputPath = "out.webm",
            outputFormat = "WEBM",
            quality = QualityPreset.HIGH,
            size = SizePreset.VideoResolution(720),
            sourceDimensions = VideoSizeMapper.Dimensions(1920, 1080),
            frameRate = 30.0,
            sourceBitrateBps = 4_000_000,
        )

        assertTrue(args.containsAll(listOf("-map", "0:v:0", "-c:v", "libvpx-vp9", "-c:a", "libopus")))
        assertTrue(args.contains("scale=1280:720"))
        assertEquals("out.webm", args.last())
    }

    @Test
    fun mkvVideoTranscode_usesOpenh264AndAac() {
        val args = FfmpegCommandBuilder.buildVideoTranscodeArgs(
            inputPath = "in.mp4",
            outputPath = "out.mkv",
            outputFormat = "MKV",
            quality = QualityPreset.STANDARD,
            size = SizePreset.Original,
            sourceDimensions = VideoSizeMapper.Dimensions(1280, 720),
            frameRate = 24.0,
            sourceBitrateBps = 2_000_000,
        )

        assertTrue(args.containsAll(listOf("-c:v", "libopenh264", "-c:a", "aac")))
        assertEquals("out.mkv", args.last())
    }

    @Test
    fun videoExtractMp3_mapsFirstAudioTrackOnly() {
        val args = FfmpegCommandBuilder.buildVideoExtractAudioArgs("in.mp4", "out.mp3", "MP3", QualityPreset.BEST)
        assertEquals(listOf("-y", "-i", "in.mp4", "-map", "0:a:0", "-vn", "-acodec", "libmp3lame", "-b:a", "320k", "out.mp3"), args)
    }

    @Test(expected = IllegalStateException::class)
    fun unsupportedVideoFormat_throws() {
        FfmpegCommandBuilder.buildVideoTranscodeArgs(
            inputPath = "in.mp4",
            outputPath = "out.mov",
            outputFormat = "MOV",
            quality = QualityPreset.HIGH,
            size = SizePreset.Original,
            sourceDimensions = VideoSizeMapper.Dimensions(1280, 720),
            frameRate = 30.0,
            sourceBitrateBps = 2_000_000,
        )
    }
}
