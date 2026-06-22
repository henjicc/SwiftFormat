package com.henjicc.swiftformat.conversion

import android.content.Context
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.database.ConversionHistoryRepository
import com.henjicc.swiftformat.core.datastore.SettingsRepository
import com.henjicc.swiftformat.core.file.FileMetadataReader
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.service.ConversionForegroundService
import kotlinx.coroutines.flow.first

/**
 * 应用进程被系统回收后，基于 Room 中的活跃历史记录把任务重新接回编排层。
 * 这里恢复的是“重新开始该转换任务”，而不是字节级断点续转；对第一版来说这是更稳的最小实现。
 */
class ConversionRecoveryManager(
    private val appContext: Context,
    private val historyRepository: ConversionHistoryRepository,
    private val metadataReader: FileMetadataReader,
    private val orchestrator: ConversionOrchestrator,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
) {

    suspend fun recoverActiveTasks(): Int {
        val activeRecords = historyRepository.getActiveRecords()
        val preserveMetadata = settingsRepository.settings.first().preserveImageMetadata
        var recoveredCount = 0
        for (record in activeRecords) {
            val input = runCatching { metadataReader.read(record.inputUri) }.getOrElse { error ->
                logger.e(TAG, "recover metadata failed: ${record.id}", error)
                historyRepository.update(
                    record.copy(
                        endTime = System.currentTimeMillis(),
                        status = ConversionStatus.FAILED,
                        failureReason = error.message ?: "failed to recover input",
                    ),
                )
                continue
            }
            val recovered = runCatching {
                orchestrator.recover(
                    historyId = record.id,
                    input = input,
                    outputFormat = record.outputFormat,
                    quality = record.quality,
                    size = record.size,
                    existingOutputUri = record.outputUri,
                    preserveMetadata = preserveMetadata,
                )
            }
            recovered.onSuccess {
                recoveredCount += 1
            }.onFailure { error ->
                logger.e(TAG, "recover task failed to queue: ${record.id}", error)
                historyRepository.update(
                    record.copy(
                        endTime = System.currentTimeMillis(),
                        status = ConversionStatus.FAILED,
                        failureReason = error.message ?: "failed to queue recovered task",
                    ),
                )
            }
        }
        if (recoveredCount > 0) {
            if (!ConversionForegroundService.start(appContext)) {
                logger.w(TAG, "failed to start foreground service for recovered tasks")
            }
        }
        return recoveredCount
    }

    private companion object {
        const val TAG = "ConversionRecovery"
    }
}
