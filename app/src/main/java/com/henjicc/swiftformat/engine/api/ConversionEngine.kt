package com.henjicc.swiftformat.engine.api

import android.net.Uri
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionRequest

/**
 * 转换能力抽象（见 SPEC 10.2）。UI/业务层只通过本接口与具体引擎
 * （NativeImageEngine/Media3Engine/FfmpegEngine）交互，互相隔离。
 */
interface ConversionEngine {
    fun supports(request: ConversionRequest): Boolean

    suspend fun convert(
        request: ConversionRequest,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult

    suspend fun cancel(taskId: String)
}

data class ConversionProgress(val fraction: Float)

sealed interface ConversionResult {
    data class Success(val outputUri: Uri, val outputSizeBytes: Long) : ConversionResult
    data class Failure(val error: ConversionError) : ConversionResult
}
