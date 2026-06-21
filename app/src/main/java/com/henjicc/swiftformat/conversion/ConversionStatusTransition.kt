package com.henjicc.swiftformat.conversion

import com.henjicc.swiftformat.core.model.ConversionStatus

/**
 * 由引擎进度回调推导粗粒度任务阶段（见 SPEC 4.5 状态机）。纯函数，便于单元测试。
 *
 * 引擎接口（[com.henjicc.swiftformat.engine.api.ConversionProgress]）只有单一 [Float] 进度，
 * 没有显式的"准备中/转换中/正在保存"阶段信号。约定：[com.henjicc.swiftformat.engine.media.Media3Engine]
 * 与 [com.henjicc.swiftformat.engine.ffmpeg.FfmpegEngine] 都把"拷贝到目标 Uri"阶段计入进度的最后 10%，
 * 故用 0.9 作为 CONVERTING→SAVING 的分界。这是已知简化：若未来新增引擎不遵循该约定，
 * 或希望阶段划分更精确，需要扩展 [com.henjicc.swiftformat.engine.api.ConversionProgress] 携带阶段信息。
 */
object ConversionStatusTransition {

    private const val SAVING_THRESHOLD = 0.9f

    fun fromProgress(current: ConversionStatus, fraction: Float): ConversionStatus = when (current) {
        ConversionStatus.PENDING, ConversionStatus.PREPARING, ConversionStatus.CONVERTING ->
            if (fraction >= SAVING_THRESHOLD) ConversionStatus.SAVING else ConversionStatus.CONVERTING

        else -> current
    }
}
