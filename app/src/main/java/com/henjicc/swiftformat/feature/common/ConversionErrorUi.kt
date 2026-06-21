package com.henjicc.swiftformat.feature.common

import androidx.annotation.StringRes
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.ConversionError

@StringRes
fun errorKindLabelRes(kind: ConversionError.Kind): Int = when (kind) {
    ConversionError.Kind.FILE_NOT_FOUND -> R.string.error_file_not_found
    ConversionError.Kind.PERMISSION_DENIED -> R.string.error_permission_denied
    ConversionError.Kind.UNSUPPORTED_FORMAT -> R.string.error_unsupported_format
    ConversionError.Kind.CORRUPT_INPUT -> R.string.error_corrupt_input
    ConversionError.Kind.NO_AUDIO_TRACK -> R.string.error_no_audio_track
    ConversionError.Kind.NO_ENCODER -> R.string.error_no_encoder
    ConversionError.Kind.UNSUPPORTED_OUTPUT -> R.string.error_unsupported_output
    ConversionError.Kind.UNSUPPORTED_IMAGE_OUTPUT -> R.string.error_unsupported_image_output
    ConversionError.Kind.UNSUPPORTED_VIDEO_OUTPUT -> R.string.error_unsupported_video_output
    ConversionError.Kind.INSUFFICIENT_STORAGE -> R.string.error_insufficient_storage
    ConversionError.Kind.OUTPUT_NOT_WRITABLE -> R.string.error_output_not_writable
    ConversionError.Kind.OUTPUT_VALIDATION_FAILED -> R.string.error_output_validation_failed
    ConversionError.Kind.ENGINE_CRASH -> R.string.error_engine_crash
    ConversionError.Kind.CANCELLED -> R.string.error_cancelled
    ConversionError.Kind.UNKNOWN -> R.string.error_unknown
}
