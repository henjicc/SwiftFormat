package com.henjicc.swiftformat.conversion

import com.henjicc.swiftformat.core.model.MediaType

/** 各媒体类型默认并发上限（见 SPEC 13.3）。纯函数，便于单元测试。 */
object ConversionConcurrencyPolicy {
    fun maxConcurrency(mediaType: MediaType): Int = when (mediaType) {
        MediaType.IMAGE -> 2
        MediaType.AUDIO -> 2
        MediaType.VIDEO -> 1
        MediaType.UNKNOWN -> 1
    }
}
