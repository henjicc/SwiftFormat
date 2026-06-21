package com.henjicc.swiftformat.conversion

import com.henjicc.swiftformat.core.database.ConversionHistoryRepository
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionHistoryRecord
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.FailureReasonCodec
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.OutputDestination
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import com.henjicc.swiftformat.engine.api.ConversionResult

internal class ConversionHistoryTracker(
    private val historyRepository: ConversionHistoryRepository,
) {
    suspend fun insertPending(request: ConversionRequest): Long =
        historyRepository.insert(newPendingRecord(request))

    suspend fun recordSubmitFailure(
        input: InputFile,
        outputFormat: String,
        quality: QualityPreset?,
        size: SizePreset?,
        exception: Throwable,
    ) {
        historyRepository.insert(
            ConversionHistoryRecord(
                originalDisplayName = input.displayName,
                originalFormat = input.extension,
                outputFormat = outputFormat,
                mediaType = input.mediaType,
                inputUri = input.uri,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                status = ConversionStatus.FAILED,
                outputUri = null,
                outputSizeBytes = null,
                failureReason = FailureReasonCodec.encode(ConversionError.Kind.UNKNOWN, exception.message),
                quality = quality,
                size = size,
            ),
        )
    }

    suspend fun resetForRecovery(historyId: Long, request: ConversionRequest) {
        val existing = historyRepository.getById(historyId) ?: return
        historyRepository.update(
            existing.copy(
                startTime = System.currentTimeMillis(),
                endTime = null,
                status = ConversionStatus.PENDING,
                outputUri = (request.destination as? OutputDestination.ResolvedUri)?.uri,
                outputSizeBytes = null,
                failureReason = null,
            ),
        )
    }

    suspend fun resetForRetry(historyId: Long) {
        val existing = historyRepository.getById(historyId) ?: return
        historyRepository.update(
            existing.copy(
                status = ConversionStatus.PENDING,
                endTime = null,
                failureReason = null,
            ),
        )
    }

    suspend fun markCompleted(historyId: Long, result: ConversionResult.Success) {
        updateHistory(historyId) {
            it.copy(
                status = ConversionStatus.COMPLETED,
                endTime = System.currentTimeMillis(),
                outputUri = result.outputUri,
                outputSizeBytes = result.outputSizeBytes,
            )
        }
    }

    suspend fun markFailed(historyId: Long, error: ConversionError, status: ConversionStatus) {
        updateHistory(historyId) {
            it.copy(
                status = status,
                endTime = System.currentTimeMillis(),
                failureReason = FailureReasonCodec.encode(error.kind, error.debugMessage),
            )
        }
    }

    suspend fun markCancelled(historyId: Long) {
        updateHistory(historyId) {
            it.copy(
                status = ConversionStatus.CANCELLED,
                endTime = System.currentTimeMillis(),
            )
        }
    }

    private suspend fun updateHistory(historyId: Long, transform: (ConversionHistoryRecord) -> ConversionHistoryRecord) {
        val record = historyRepository.getById(historyId) ?: return
        historyRepository.update(transform(record))
    }

    private fun newPendingRecord(request: ConversionRequest) = ConversionHistoryRecord(
        originalDisplayName = request.input.displayName,
        originalFormat = request.input.extension,
        outputFormat = request.outputFormat,
        mediaType = request.input.mediaType,
        inputUri = request.input.uri,
        startTime = System.currentTimeMillis(),
        endTime = null,
        status = ConversionStatus.PENDING,
        outputUri = (request.destination as? OutputDestination.ResolvedUri)?.uri,
        outputSizeBytes = null,
        failureReason = null,
        quality = request.quality,
        size = request.size,
    )
}
