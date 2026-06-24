package com.henjicc.swiftformat

import android.content.Context
import android.net.Uri
import com.henjicc.swiftformat.conversion.ConversionOrchestrator
import com.henjicc.swiftformat.conversion.ConversionRecoveryManager
import com.henjicc.swiftformat.conversion.OutputLocationResolver
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.database.ConversionHistoryRepository
import com.henjicc.swiftformat.core.datastore.SettingsRepository
import com.henjicc.swiftformat.core.file.FileMetadataReader
import com.henjicc.swiftformat.core.model.AppSettings
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionHistoryRecord
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.engine.api.ConversionEngine
import com.henjicc.swiftformat.engine.api.ConversionEngineSelector
import com.henjicc.swiftformat.engine.api.ConversionProgress
import com.henjicc.swiftformat.engine.api.ConversionResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mockito.Mockito.timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ConversionCrashSafetyTest {

    @Test
    fun submit_engineThrowsError_marksTaskFailedInsteadOfCrashing() = runBlocking {
        val outputResolver = mock<OutputLocationResolver>()
        val historyRepository = mock<ConversionHistoryRepository>()
        val logger = mock<Logger>()
        val inputUri = mock<Uri>()
        val outputUri = mock<Uri>()
        val input = testInput(inputUri)
        val pendingRecord = pendingRecord(input, outputUri)
        val engine = object : ConversionEngine {
            override fun supports(request: com.henjicc.swiftformat.core.model.ConversionRequest): Boolean = true

            override suspend fun convert(
                request: com.henjicc.swiftformat.core.model.ConversionRequest,
                onProgress: (ConversionProgress) -> Unit,
            ): ConversionResult {
                throw UnsatisfiedLinkError("native ffmpeg load failed")
            }

            override suspend fun cancel(taskId: String) = Unit
        }
        val orchestrator = ConversionOrchestrator(
            engineSelector = ConversionEngineSelector(listOf(engine)),
            outputLocationResolver = outputResolver,
            historyRepository = historyRepository,
            logger = logger,
        )

        whenever(outputResolver.resolve(any(), eq("WEBP"), eq(MediaType.IMAGE))).thenReturn(outputUri)
        whenever(historyRepository.insert(any())).thenReturn(1L)
        whenever(historyRepository.getById(1L)).thenReturn(pendingRecord)

        val taskId = orchestrator.submit(
            input = input,
            outputFormat = "WEBP",
            quality = null,
            size = null,
        )

        withTimeout(3_000) {
            while (orchestrator.tasks.value[taskId]?.status != ConversionStatus.FAILED) {
                delay(10)
            }
        }

        val task = orchestrator.tasks.value.getValue(taskId)
        assertEquals(ConversionStatus.FAILED, task.status)
        assertEquals(ConversionError.Kind.ENGINE_CRASH, task.error?.kind)
        assertTrue(task.error?.debugMessage?.contains("native ffmpeg load failed") == true)
    }

    @Test
    fun cancel_activeTask_marksCancelledImmediatelyAndPersistsCancellation() = runBlocking {
        val outputResolver = mock<OutputLocationResolver>()
        val historyRepository = mock<ConversionHistoryRepository>()
        val logger = mock<Logger>()
        val inputUri = mock<Uri>()
        val outputUri = mock<Uri>()
        val input = testInput(inputUri)
        val pendingRecord = pendingRecord(input, outputUri)
        val convertStarted = CompletableDeferred<Unit>()
        val engine = object : ConversionEngine {
            override fun supports(request: com.henjicc.swiftformat.core.model.ConversionRequest): Boolean = true

            override suspend fun convert(
                request: com.henjicc.swiftformat.core.model.ConversionRequest,
                onProgress: (ConversionProgress) -> Unit,
            ): ConversionResult {
                convertStarted.complete(Unit)
                delay(Long.MAX_VALUE)
                error("unreachable")
            }

            override suspend fun cancel(taskId: String) = Unit
        }
        val orchestrator = ConversionOrchestrator(
            engineSelector = ConversionEngineSelector(listOf(engine)),
            outputLocationResolver = outputResolver,
            historyRepository = historyRepository,
            logger = logger,
        )

        whenever(outputResolver.resolve(any(), eq("WEBP"), eq(MediaType.IMAGE))).thenReturn(outputUri)
        whenever(historyRepository.insert(any())).thenReturn(1L)
        whenever(historyRepository.getById(1L)).thenReturn(pendingRecord)

        val taskId = orchestrator.submit(
            input = input,
            outputFormat = "WEBP",
            quality = null,
            size = null,
        )
        withTimeout(3_000) { convertStarted.await() }

        orchestrator.cancel(taskId)

        assertEquals(ConversionStatus.CANCELLED, orchestrator.tasks.value.getValue(taskId).status)
        verify(historyRepository, timeout(1_000).atLeastOnce()).update(
            check {
                assertEquals(pendingRecord.id, it.id)
                assertEquals(ConversionStatus.CANCELLED, it.status)
            },
        )
    }

    @Test
    fun recover_queueFailure_marksRecordFailedInsteadOfCrashing() = runBlocking {
        val appContext = mock<Context>()
        val historyRepository = mock<ConversionHistoryRepository>()
        val metadataReader = mock<FileMetadataReader>()
        val orchestrator = mock<ConversionOrchestrator>()
        val settingsRepository = mock<SettingsRepository>()
        val logger = mock<Logger>()
        val inputUri = mock<Uri>()
        val outputUri = mock<Uri>()
        val input = testInput(inputUri)
        val activeRecord = pendingRecord(input, outputUri)
        val recoveryManager = ConversionRecoveryManager(
            appContext = appContext,
            historyRepository = historyRepository,
            metadataReader = metadataReader,
            orchestrator = orchestrator,
            settingsRepository = settingsRepository,
            logger = logger,
        )

        whenever(historyRepository.getActiveRecords()).thenReturn(listOf(activeRecord))
        whenever(metadataReader.read(inputUri)).thenReturn(input)
        whenever(settingsRepository.settings).thenReturn(flowOf(AppSettings()))
        whenever(
            orchestrator.recover(
                historyId = activeRecord.id,
                input = input,
                outputFormat = activeRecord.outputFormat,
                quality = activeRecord.quality,
                size = activeRecord.size,
                existingOutputUri = activeRecord.outputUri,
                preserveMetadata = AppSettings().preserveImageMetadata,
            ),
        ).thenThrow(IllegalStateException("queue failed"))

        val recoveredCount = recoveryManager.recoverActiveTasks()

        assertEquals(0, recoveredCount)
        assertTrue(
            "recover queue failure should mark record failed",
            runCatching {
                org.mockito.kotlin.verify(historyRepository).update(
                    org.mockito.kotlin.check {
                        assertEquals(activeRecord.id, it.id)
                        assertEquals(ConversionStatus.FAILED, it.status)
                        assertTrue(it.failureReason?.contains("queue failed") == true)
                    },
                )
            }.isSuccess,
        )
    }

    private fun testInput(uri: Uri) = InputFile(
        id = "input-1",
        uri = uri,
        displayName = "sample.jpg",
        mimeType = "image/jpeg",
        extension = "jpg",
        sizeBytes = 123L,
        mediaType = MediaType.IMAGE,
    )

    private fun pendingRecord(input: InputFile, outputUri: Uri) = ConversionHistoryRecord(
        id = 1L,
        originalDisplayName = input.displayName,
        originalFormat = input.extension,
        outputFormat = "WEBP",
        mediaType = input.mediaType,
        inputUri = input.uri,
        startTime = 1L,
        endTime = null,
        status = ConversionStatus.PENDING,
        outputUri = outputUri,
        outputSizeBytes = null,
        failureReason = null,
        quality = null,
        size = null,
    )
}
