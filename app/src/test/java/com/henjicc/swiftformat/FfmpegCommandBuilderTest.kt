package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.engine.ffmpeg.FfmpegCommandBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FfmpegCommandBuilderTest {

    @Test
    fun mp3_usesLibmp3lameWithBitrate() {
        val args = FfmpegCommandBuilder.buildAudioArgs("in.wav", "out.mp3", "MP3", QualityPreset.HIGH)
        assertEquals(listOf("-y", "-i", "in.wav", "-vn", "-acodec", "libmp3lame", "-b:a", "256k", "out.mp3"), args)
    }

    @Test
    fun flac_usesFlacCodecWithoutBitrate() {
        val args = FfmpegCommandBuilder.buildAudioArgs("in.wav", "out.flac", "FLAC", null)
        assertEquals(listOf("-y", "-i", "in.wav", "-vn", "-acodec", "flac", "out.flac"), args)
    }

    @Test
    fun wav_usesPcmS16le() {
        val args = FfmpegCommandBuilder.buildAudioArgs("in.mp3", "out.wav", "wav", null)
        assertEquals(listOf("-y", "-i", "in.mp3", "-vn", "-acodec", "pcm_s16le", "out.wav"), args)
    }

    @Test
    fun output_isAlwaysLastArgument() {
        val args = FfmpegCommandBuilder.buildAudioArgs("in.wav", "out.mp3", "MP3", QualityPreset.BEST)
        assertEquals("out.mp3", args.last())
        assertTrue(args.contains("-vn"))
    }

    @Test(expected = IllegalStateException::class)
    fun unsupportedFormat_throws() {
        FfmpegCommandBuilder.buildAudioArgs("in.wav", "out.ogg", "OGG", null)
    }
}
