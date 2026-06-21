package com.henjicc.swiftformat.engine.media

import androidx.media3.transformer.ExportException
import com.henjicc.swiftformat.core.model.ConversionError

internal fun mapMedia3Error(errorCode: Int): ConversionError.Kind = when (errorCode) {
    ExportException.ERROR_CODE_ENCODER_INIT_FAILED,
    ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED,
    ExportException.ERROR_CODE_DECODER_INIT_FAILED,
    ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
    -> ConversionError.Kind.NO_ENCODER

    ExportException.ERROR_CODE_IO_FILE_NOT_FOUND -> ConversionError.Kind.FILE_NOT_FOUND
    ExportException.ERROR_CODE_IO_NO_PERMISSION -> ConversionError.Kind.PERMISSION_DENIED
    else -> ConversionError.Kind.ENGINE_CRASH
}
