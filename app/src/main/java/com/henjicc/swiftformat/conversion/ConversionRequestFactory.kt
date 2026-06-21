package com.henjicc.swiftformat.conversion

import android.net.Uri
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputDestination
import com.henjicc.swiftformat.core.model.OutputFormatCatalog
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

internal class ConversionRequestFactory(
    private val outputLocationResolver: OutputLocationResolver,
) {
    /** 同批次提交的文件可能重名，输出位置解析必须串行化，否则会算出相同的"无冲突"文件名（见 [OutputLocationResolver]）。 */
    private val outputResolutionMutex = Mutex()

    fun newRequestId(): String = UUID.randomUUID().toString()

    suspend fun createResolvedRequest(
        id: String,
        input: InputFile,
        outputFormat: String,
        targetMediaType: MediaType? = null,
        quality: QualityPreset?,
        size: SizePreset?,
        existingOutputUri: Uri? = null,
    ): ConversionRequest {
        val resolvedTargetMediaType = targetMediaType ?: OutputFormatCatalog.targetMediaTypeFor(input.mediaType, outputFormat)
        val destinationUri = existingOutputUri ?: outputResolutionMutex.withLock {
            outputLocationResolver.resolve(input.displayName, outputFormat, resolvedTargetMediaType)
        }
        return ConversionRequest(
            id = id,
            input = input,
            outputFormat = outputFormat,
            targetMediaType = resolvedTargetMediaType,
            quality = quality,
            size = size,
            destination = OutputDestination.ResolvedUri(destinationUri),
        )
    }
}
