package com.henjicc.swiftformat.feature.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset

@Composable
fun qualityLabel(preset: QualityPreset): String = stringResource(
    when (preset) {
        QualityPreset.BEST -> R.string.quality_best
        QualityPreset.HIGH -> R.string.quality_high
        QualityPreset.STANDARD -> R.string.quality_standard
        QualityPreset.SMALL_SIZE -> R.string.quality_small_size
    },
)

@Composable
fun sizeLabel(preset: SizePreset): String = when (preset) {
    SizePreset.Original -> stringResource(R.string.size_original)
    is SizePreset.VideoResolution -> stringResource(videoResolutionLabelRes(preset.height))
    is SizePreset.ImageLongestEdge -> stringResource(R.string.size_image_edge, preset.pixels)
    is SizePreset.Custom -> stringResource(R.string.size_original)
}

@StringRes
fun statusLabelRes(status: ConversionStatus): Int = when (status) {
    ConversionStatus.PENDING -> R.string.status_pending
    ConversionStatus.PREPARING -> R.string.status_preparing
    ConversionStatus.CONVERTING -> R.string.status_converting
    ConversionStatus.SAVING -> R.string.status_saving
    ConversionStatus.COMPLETED -> R.string.status_completed
    ConversionStatus.CANCELLED -> R.string.status_cancelled
    ConversionStatus.FAILED -> R.string.status_failed
}

@StringRes
private fun videoResolutionLabelRes(height: Int): Int = when (height) {
    2160 -> R.string.size_4k
    1440 -> R.string.size_2k
    1080 -> R.string.size_1080p
    720 -> R.string.size_720p
    480 -> R.string.size_480p
    else -> R.string.size_original
}
