package com.henjicc.swiftformat.engine.ffmpeg

import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.common.toDebugMessage

/**
 * 在真正执行 FFmpeg/FFprobe 命令前，先做一次运行时可用性探测。
 *
 * 目的不是提前初始化所有 native 能力，而是把“FFmpegKit 连库都起不来”的情况
 * 尽量收敛成可诊断的失败结果，而不是在首个命令执行点直接把进程打崩。
 */
internal object FfmpegRuntimeSupport {
    @Volatile
    private var probeResult: ProbeResult? = null

    fun unavailableReason(logger: Logger): String? =
        probe(logger).failureDebugMessage

    private fun probe(logger: Logger): ProbeResult {
        probeResult?.let { return it }
        return synchronized(this) {
            probeResult?.let { return@synchronized it }
            val result = runCatching {
                ProbeResult(version = FFmpegKitConfig.getVersion(), failureDebugMessage = null)
            }.getOrElse { error ->
                val debugMessage = buildString {
                    append("FFmpegKit startup probe failed.")
                    appendLine()
                    append(error.toDebugMessage())
                }
                logger.e(TAG, debugMessage, error)
                ProbeResult(version = null, failureDebugMessage = debugMessage)
            }
            probeResult = result
            result
        }
    }

    private data class ProbeResult(
        val version: String?,
        val failureDebugMessage: String?,
    )

    private const val TAG = "FfmpegRuntime"
}
