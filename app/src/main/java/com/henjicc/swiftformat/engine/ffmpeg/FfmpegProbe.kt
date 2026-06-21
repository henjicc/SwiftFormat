package com.henjicc.swiftformat.engine.ffmpeg

import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import com.arthenica.ffmpegkit.StreamInformation

internal data class FfmpegProbeResult(
    val mediaInformation: MediaInformation?,
    val streams: List<StreamInformation>,
    val videoStreams: List<StreamInformation>,
    val audioStreams: List<StreamInformation>,
) {
    val primaryVideoStream: StreamInformation? = videoStreams.firstOrNull()
    val primaryAudioStream: StreamInformation? = audioStreams.firstOrNull()
}

internal fun probeMediaInformation(path: String): FfmpegProbeResult? {
    val session = FFprobeKit.getMediaInformation(path)
    val mediaInformation = session.mediaInformation ?: return null
    val streams = mediaInformation.streams.orEmpty()
    return FfmpegProbeResult(
        mediaInformation = mediaInformation,
        streams = streams,
        videoStreams = streams.filter { it.type.equals("video", ignoreCase = true) },
        audioStreams = streams.filter { it.type.equals("audio", ignoreCase = true) },
    )
}
