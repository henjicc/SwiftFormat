package com.henjicc.swiftformat.engine.media

import android.media.MediaCodecList

/** 设备编码器能力探测（见 SPEC 10.1「根据设备能力自动选择引擎」）。不可单元测试，仅 Android 运行时可用。 */
object MediaCodecCapabilities {
    fun hasEncoderFor(mimeType: String): Boolean = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
        }
    }.getOrDefault(false)
}
