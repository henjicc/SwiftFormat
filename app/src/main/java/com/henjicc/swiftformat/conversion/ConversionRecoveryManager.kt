package com.henjicc.swiftformat.conversion

import android.content.Context
import androidx.core.content.ContextCompat
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.database.ConversionHistoryRepository
import com.henjicc.swiftformat.core.file.FileMetadataReader
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.service.ConversionForegroundService

/**
 * 应用进程被系统回收后，基于 Room 中的活跃历史记录把任务重新接回编排层。
 * 这里恢复的是“重新开始该转换任务”，而不是字节级断点续转；对第一版来说这是更稳的最小实现。
 */
class ConversionRecoveryManager(
    private val appContext: Context,
    private val historyRepository: ConversionHistoryRepository,
    private val metadataReader: FileMetadataReader,
    private val orchestrator: ConversionOrchestrator,
    private val logger: Logger,
) {

    suspend fun recoverActiveTasks(): Int {
        val activeRecords = historyRepository.getActiveRecords()
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
            orchestrator.recover(
                historyId = record.id,
                input = input,
                outputFormat = record.outputFormat,
                quality = record.quality,
                size = record.size,
                existingOutputUri = record.outputUri,
            )
            recoveredCount += 1
        }
        if (recoveredCount > 0) {
            ContextCompat.startForegroundService(appContext, android.content.Intent(appContext, ConversionForegroundService::class.java))
        }
        return recoveredCount
    }

    private companion object {
        const val TAG = "ConversionRecovery"
    }
}
