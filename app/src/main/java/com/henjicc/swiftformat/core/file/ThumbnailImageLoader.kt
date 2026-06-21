package com.henjicc.swiftformat.core.file

import android.content.Context
import coil3.ImageLoader
import coil3.video.VideoFrameDecoder

/**
 * 缩略图加载器：图片直接解码，视频取首帧（见 SPEC 6.4）。
 * Coil 自带内存/磁盘缓存与异步加载，避免手写缓存与解码逻辑。
 */
object ThumbnailImageLoader {
    fun build(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
