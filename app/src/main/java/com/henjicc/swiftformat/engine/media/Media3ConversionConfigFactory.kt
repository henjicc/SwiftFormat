package com.henjicc.swiftformat.engine.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.VideoEncoderSettings
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.QualityPreset

@UnstableApi
internal class Media3ConversionConfigFactory(
    private val appContext: Context,
    private val logger: Logger,
) {
    fun create(request: ConversionRequest): Media3ConversionConfig = Media3ConversionConfig(
        editedItem = buildEditedMediaItem(request),
        encoderFactory = buildEncoderFactory(request),
    )

    private fun buildEditedMediaItem(request: ConversionRequest): EditedMediaItem {
        val mediaItem = MediaItem.fromUri(request.input.uri)
        val builder = EditedMediaItem.Builder(mediaItem)
        if (request.input.mediaType == MediaType.VIDEO) {
            val source = VideoSizeMapper.Dimensions(
                request.input.width ?: 0,
                request.input.height ?: 0,
            )
            val target = VideoSizeMapper.targetDimensions(source, request.size)
            if (target != source && target.width > 0 && target.height > 0) {
                val shortSide = minOf(target.width, target.height)
                builder.setEffects(Effects(emptyList(), listOf(Presentation.createForShortSide(shortSide))))
            }
        }
        return builder.build()
    }

    private fun buildEncoderFactory(request: ConversionRequest): DefaultEncoderFactory {
        val builder = DefaultEncoderFactory.Builder(appContext)
        when (request.input.mediaType) {
            MediaType.VIDEO -> {
                val source = VideoSizeMapper.Dimensions(
                    request.input.width ?: 0,
                    request.input.height ?: 0,
                )
                val target = VideoSizeMapper.targetDimensions(source, request.size)
                val track = probeVideoTrack(request.input.uri)
                val bitrate = VideoBitrateMapper.targetBitrateBps(
                    preset = request.quality ?: QualityPreset.HIGH,
                    targetWidth = target.width,
                    targetHeight = target.height,
                    frameRate = track?.frameRate ?: 30.0,
                    sourceBitrateBps = track?.bitrate,
                )
                builder.setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder().setBitrate(bitrate.toInt()).build(),
                )
            }

            MediaType.AUDIO -> {
                val bitrate = AudioBitrateMapper.targetBitrateBps(request.outputFormat, request.quality ?: QualityPreset.HIGH)
                if (bitrate != null) {
                    builder.setRequestedAudioEncoderSettings(
                        AudioEncoderSettings.Builder().setBitrate(bitrate).build(),
                    )
                }
            }

            else -> Unit
        }
        return builder.build()
    }

    /** 用 MediaExtractor 读取视频轨真实帧率/码率，供码率映射使用；探测失败时返回 null 走默认值。 */
    private fun probeVideoTrack(uri: Uri): VideoTrackInfo? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(appContext, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/")) continue
                val frameRate = runCatching {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        format.getInteger(MediaFormat.KEY_FRAME_RATE).toDouble()
                    } else {
                        null
                    }
                }.getOrNull() ?: 30.0
                val bitrate = runCatching {
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        format.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
                    } else {
                        null
                    }
                }.getOrNull()
                return VideoTrackInfo(frameRate, bitrate)
            }
            null
        } catch (e: Exception) {
            logger.w(TAG, "probeVideoTrack failed", e)
            null
        } finally {
            extractor.release()
        }
    }

    private data class VideoTrackInfo(val frameRate: Double, val bitrate: Long?)

    private companion object {
        const val TAG = "Media3ConfigFactory"
    }
}

@UnstableApi
internal data class Media3ConversionConfig(
    val editedItem: EditedMediaItem,
    val encoderFactory: DefaultEncoderFactory,
)
