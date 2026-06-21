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
    fun oggAudioTranscode_usesLibvorbis() {
        val args = FfmpegCommandBuilder.buildAudioTranscodeArgs("in.wav", "out.ogg", "OGG", QualityPreset.STANDARD)
        assertTrue(args.containsAll(listOf("-acodec", "libvorbis", "-b:a", "128k")))
        assertEquals("out.ogg", args.last())
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
    fun movVideoTranscode_usesOpenh264AndAac() {
        val args = FfmpegCommandBuilder.buildVideoTranscodeArgs(
            inputPath = "in.mp4",
            outputPath = "out.mov",
            outputFormat = "MOV",
            quality = QualityPreset.HIGH,
            size = SizePreset.Original,
            sourceDimensions = VideoSizeMapper.Dimensions(1920, 1080),
            frameRate = 30.0,
            sourceBitrateBps = 3_000_000,
        )

        assertTrue(args.containsAll(listOf("-map", "0:v:0", "-map", "0:a:0?", "-c:v", "libopenh264", "-c:a", "aac")))
        assertEquals("out.mov", args.last())
    }

    @Test
    fun videoExtractMp3_mapsFirstAudioTrackOnly() {
        val args = FfmpegCommandBuilder.buildVideoExtractAudioArgs("in.mp4", "out.mp3", "MP3", QualityPreset.BEST)
        assertEquals(listOf("-y", "-i", "in.mp4", "-map", "0:a:0", "-vn", "-acodec", "libmp3lame", "-b:a", "320k", "out.mp3"), args)
    }

    @Test
    fun videoExtractM4a_usesAac() {
        val args = FfmpegCommandBuilder.buildVideoExtractAudioArgs("in.mp4", "out.m4a", "M4A", QualityPreset.HIGH)
        assertTrue(args.containsAll(listOf("-map", "0:a:0", "-vn", "-acodec", "aac", "-b:a", "192k")))
        assertEquals("out.m4a", args.last())
    }

    @Test
    fun videoExtractLosslessFormats_useExpectedCodecs() {
        val wavArgs = FfmpegCommandBuilder.buildVideoExtractAudioArgs("in.mp4", "out.wav", "WAV", QualityPreset.HIGH)
        assertTrue(wavArgs.containsAll(listOf("-map", "0:a:0", "-vn", "-acodec", "pcm_s16le")))
        val flacArgs = FfmpegCommandBuilder.buildVideoExtractAudioArgs("in.mp4", "out.flac", "FLAC", QualityPreset.HIGH)
        assertTrue(flacArgs.containsAll(listOf("-map", "0:a:0", "-vn", "-acodec", "flac")))
    }

    @Test
    fun stillImageTranscode_usesExpectedCodec() {
        val bmpArgs = FfmpegCommandBuilder.buildStillImageTranscodeArgs("in.png", "out.bmp", "BMP")
        assertEquals(listOf("-y", "-i", "in.png", "-frames:v", "1", "-c:v", "bmp", "out.bmp"), bmpArgs)

        val tiffArgs = FfmpegCommandBuilder.buildStillImageTranscodeArgs("in.png", "out.tiff", "TIFF")
        assertEquals(listOf("-y", "-i", "in.png", "-frames:v", "1", "-c:v", "tiff", "out.tiff"), tiffArgs)
    }

    @Test(expected = IllegalStateException::class)
    fun unsupportedVideoFormat_throws() {
        FfmpegCommandBuilder.buildVideoTranscodeArgs(
            inputPath = "in.mp4",
            outputPath = "out.avi",
            outputFormat = "AVI",
            quality = QualityPreset.HIGH,
            size = SizePreset.Original,
            sourceDimensions = VideoSizeMapper.Dimensions(1280, 720),
            frameRate = 30.0,
            sourceBitrateBps = 2_000_000,
        )
    }
}
