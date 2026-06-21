package com.henjicc.swiftformat.conversion

import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.common.toDebugMessage
import com.henjicc.swiftformat.core.database.ConversionHistoryRepository
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import com.henjicc.swiftformat.engine.api.ConversionEngine
import com.henjicc.swiftformat.engine.api.ConversionEngineSelector
import com.henjicc.swiftformat.engine.api.ConversionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * 任务编排层（见 SPEC 13）：批量提交、按媒体类型并发限流、状态机推导、Room 历史读写、取消/重试、批量汇总。
 * UI 只通过本类与转换能力交互，不直接持有引擎或历史仓库（见 SPEC 9.4 分层约束）。
 *
 * 不在内部持有 [android.content.Context]：[OutputLocationResolver] 是唯一接触 MediaStore 的依赖，
 * 通过构造函数注入，便于未来替换输出策略而不改动编排逻辑。
 */
class ConversionOrchestrator(
    private val engineSelector: ConversionEngineSelector,
    outputLocationResolver: OutputLocationResolver,
    historyRepository: ConversionHistoryRepository,
    private val logger: Logger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val semaphores = MediaType.entries.associateWith { Semaphore(ConversionConcurrencyPolicy.maxConcurrency(it)) }
    private val requestFactory = ConversionRequestFactory(outputLocationResolver)
    private val historyTracker = ConversionHistoryTracker(historyRepository)

    private val activeEngines = ConcurrentHashMap<String, ConversionEngine>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _tasks = MutableStateFlow<Map<String, ConversionTask>>(emptyMap())
    val tasks: StateFlow<Map<String, ConversionTask>> = _tasks.asStateFlow()

    /** 提交单个文件转换；返回任务 id（即 [ConversionRequest.id]）。 */
    fun submit(
        input: InputFile,
        outputFormat: String,
        targetMediaType: MediaType? = null,
        quality: QualityPreset?,
        size: SizePreset?,
    ): String {
        val requestId = requestFactory.newRequestId()
        launchTask(requestId) {
            try {
                val resolvedRequest = requestFactory.createResolvedRequest(
                    requestId,
                    input,
                    outputFormat,
                    targetMediaType,
                    quality,
                    size,
                )
                val historyId = historyTracker.insertPending(resolvedRequest)
                runTask(resolvedRequest, historyId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logger.e(TAG, "submit failed before queueing: $requestId", e)
                runCatching {
                    historyTracker.recordSubmitFailure(
                        input = input,
                        outputFormat = outputFormat,
                        quality = quality,
                        size = size,
                        exception = e,
                    )
                }.onFailure { historyError ->
                    logger.e(TAG, "failed to persist submit failure: $requestId", historyError)
                }
            }
        }
        return requestId
    }

    fun submitAll(
        inputs: List<InputFile>,
        outputFormat: String,
        targetMediaType: MediaType? = null,
        quality: QualityPreset?,
        size: SizePreset?,
    ): List<String> = inputs.map { submit(it, outputFormat, targetMediaType, quality, size) }

    /**
     * 进程恢复：沿用同一条历史记录继续执行，避免应用被系统回收后历史里出现重复条目。
     * 若旧记录里已经有提交时解析好的目标 Uri，则优先复用，避免生成新的重名输出文件。
     */
    suspend fun recover(
        historyId: Long,
        input: InputFile,
        outputFormat: String,
        targetMediaType: MediaType? = null,
        quality: QualityPreset?,
        size: SizePreset?,
        existingOutputUri: android.net.Uri?,
    ): String {
        val requestId = requestFactory.newRequestId()
        val request = requestFactory.createResolvedRequest(
            id = requestId,
            input = input,
            outputFormat = outputFormat,
            targetMediaType = targetMediaType,
            quality = quality,
            size = size,
            existingOutputUri = existingOutputUri,
        )
        historyTracker.resetForRecovery(historyId, request)
        launchTask(request.id) {
            runTask(request, historyId)
        }
        return request.id
    }

    private suspend fun runTask(request: ConversionRequest, historyId: Long) {
        updateTask(request.id, ConversionTask(request, historyId, ConversionStatus.PENDING))
        val semaphore = semaphores.getValue(request.input.mediaType)
        try {
            semaphore.withPermit {
                updateTaskStatus(request.id, ConversionStatus.PREPARING)
                val engine = engineSelector.select(request)
                if (engine == null) {
                    failTask(
                        request.id,
                        historyId,
                        ConversionError(unsupportedOutputKind(request), "no engine supports this request"),
                    )
                    return@withPermit
                }
                activeEngines[request.id] = engine
                val result = engine.convert(request) { progress ->
                    updateTask(request.id) { current ->
                        current?.copy(
                            status = ConversionStatusTransition.fromProgress(current.status, progress.fraction),
                            progress = progress.fraction,
                        )
                    }
                }
                when (result) {
                    is ConversionResult.Success -> safeCompleteTask(request.id, historyId, result)
                    is ConversionResult.Failure -> safeFailTask(request.id, historyId, result.error)
                }
            }
        } catch (e: CancellationException) {
            safeCancelTask(request.id, historyId)
        } catch (e: Throwable) {
            logger.e(TAG, "task crashed unexpectedly: ${request.id}", e)
            safeFailTask(
                request.id,
                historyId,
                ConversionError(
                    kind = ConversionError.Kind.ENGINE_CRASH,
                    debugMessage = e.toDebugMessage(),
                    cause = e,
                ),
            )
        } finally {
            activeEngines.remove(request.id)
        }
    }

    /** 取消单个任务：未开始的任务直接取消协程；已开始的任务还需通知引擎中断原生工作。 */
    fun cancel(taskId: String) {
        activeEngines[taskId]?.let { engine ->
            scope.launch { engine.cancel(taskId) }
        }
        activeJobs[taskId]?.cancel()
    }

    fun cancelAll() {
        activeJobs.keys.toList().forEach(::cancel)
    }

    /** 重试沿用原始请求（含已解析的目标 Uri），避免在同一批次内产生重名的新文件。 */
    fun retry(taskId: String) {
        val task = _tasks.value[taskId] ?: return
        launchTask(taskId) {
            historyTracker.resetForRetry(task.historyId)
            runTask(task.request, task.historyId)
        }
    }

    /** 再次转换会新建任务与历史记录，并重新解析输出位置，避免覆写既有结果。 */
    fun convertAgain(taskId: String): String? {
        val task = _tasks.value[taskId] ?: return null
        return submit(
            task.request.input,
            task.request.outputFormat,
            task.request.targetMediaType,
            task.request.quality,
            task.request.size,
        )
    }

    fun summary(): ConversionBatchSummary = ConversionBatchSummary.from(_tasks.value.values)

    private suspend fun completeTask(id: String, historyId: Long, result: ConversionResult.Success) {
        updateTask(id) { it?.copy(status = ConversionStatus.COMPLETED, progress = 1f, outputUri = result.outputUri) }
        historyTracker.markCompleted(historyId, result)
    }

    private suspend fun failTask(id: String, historyId: Long, error: ConversionError) {
        val status = if (error.kind == ConversionError.Kind.CANCELLED) ConversionStatus.CANCELLED else ConversionStatus.FAILED
        updateTask(id) { it?.copy(status = status, error = error) }
        historyTracker.markFailed(historyId, error, status)
    }

    private suspend fun cancelTask(id: String, historyId: Long) {
        updateTask(id) { it?.copy(status = ConversionStatus.CANCELLED) }
        historyTracker.markCancelled(historyId)
    }

    private suspend fun safeCompleteTask(id: String, historyId: Long, result: ConversionResult.Success) {
        runCatching {
            completeTask(id, historyId, result)
        }.onFailure { error ->
            logger.e(TAG, "complete task failed unexpectedly: $id", error)
            safeFailTask(
                id,
                historyId,
                ConversionError(
                    kind = ConversionError.Kind.ENGINE_CRASH,
                    debugMessage = error.toDebugMessage(),
                    cause = error,
                ),
            )
        }
    }

    private suspend fun safeFailTask(id: String, historyId: Long, error: ConversionError) {
        runCatching {
            failTask(id, historyId, error)
        }.onFailure { historyError ->
            logger.e(TAG, "failed to persist task failure: $id", historyError)
            val status = if (error.kind == ConversionError.Kind.CANCELLED) {
                ConversionStatus.CANCELLED
            } else {
                ConversionStatus.FAILED
            }
            updateTask(id) { it?.copy(status = status, error = error) }
        }
    }

    private suspend fun safeCancelTask(id: String, historyId: Long) {
        runCatching {
            cancelTask(id, historyId)
        }.onFailure { historyError ->
            logger.e(TAG, "failed to persist task cancellation: $id", historyError)
            updateTask(id) { it?.copy(status = ConversionStatus.CANCELLED) }
        }
    }

    private fun launchTask(taskId: String, block: suspend () -> Unit) {
        val job = scope.launch {
            try {
                block()
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
    }

    private fun unsupportedOutputKind(request: ConversionRequest): ConversionError.Kind = when {
        request.input.mediaType == MediaType.IMAGE -> ConversionError.Kind.UNSUPPORTED_IMAGE_OUTPUT
        request.input.mediaType == MediaType.VIDEO && request.targetMediaType == MediaType.VIDEO ->
            ConversionError.Kind.UNSUPPORTED_VIDEO_OUTPUT

        else -> ConversionError.Kind.UNSUPPORTED_OUTPUT
    }

    private fun updateTaskStatus(id: String, status: ConversionStatus) {
        updateTask(id) { it?.copy(status = status) }
    }

    private fun updateTask(id: String, task: ConversionTask) {
        _tasks.update { it + (id to task) }
    }

    private fun updateTask(id: String, transform: (ConversionTask?) -> ConversionTask?) {
        _tasks.update { current ->
            val updated = transform(current[id]) ?: return@update current
            current + (id to updated)
        }
    }

    private companion object {
        const val TAG = "ConversionOrchestrator"
    }
}
