package com.henjicc.swiftformat.conversion

import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.database.ConversionHistoryRepository
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionHistoryRecord
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputDestination
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.UUID
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
    private val outputLocationResolver: OutputLocationResolver,
    private val historyRepository: ConversionHistoryRepository,
    private val logger: Logger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val semaphores = MediaType.entries.associateWith { Semaphore(ConversionConcurrencyPolicy.maxConcurrency(it)) }

    /** 同批次提交的文件可能重名，输出位置解析必须串行化，否则会算出相同的"无冲突"文件名（见 [OutputLocationResolver]）。 */
    private val outputResolutionMutex = Mutex()

    private val activeEngines = ConcurrentHashMap<String, ConversionEngine>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _tasks = MutableStateFlow<Map<String, ConversionTask>>(emptyMap())
    val tasks: StateFlow<Map<String, ConversionTask>> = _tasks.asStateFlow()

    /** 提交单个文件转换；返回任务 id（即 [ConversionRequest.id]）。 */
    fun submit(input: InputFile, outputFormat: String, quality: QualityPreset?, size: SizePreset?): String {
        val requestId = UUID.randomUUID().toString()
        val job = scope.launch {
            try {
                val destinationUri = outputResolutionMutex.withLock {
                    outputLocationResolver.resolve(input.displayName, outputFormat, input.mediaType)
                }
                val request = ConversionRequest(
                    id = requestId,
                    input = input,
                    outputFormat = outputFormat,
                    quality = quality,
                    size = size,
                    destination = OutputDestination.ResolvedUri(destinationUri),
                )
                val historyId = historyRepository.insert(newPendingRecord(request))
                runTask(request, historyId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 此时尚未进入 runTask，_tasks 里还没有这个任务的条目，没有任务可更新；
                // 仍写一条失败历史，避免用户提交的文件在历史里完全消失，只是不会出现在实时任务列表中。
                logger.e(TAG, "submit failed before queueing: $requestId", e)
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
                        failureReason = e.message,
                        quality = quality,
                        size = size,
                    ),
                )
            } finally {
                activeJobs.remove(requestId)
            }
        }
        activeJobs[requestId] = job
        return requestId
    }

    fun submitAll(inputs: List<InputFile>, outputFormat: String, quality: QualityPreset?, size: SizePreset?): List<String> =
        inputs.map { submit(it, outputFormat, quality, size) }

    /**
     * 进程恢复：沿用同一条历史记录继续执行，避免应用被系统回收后历史里出现重复条目。
     * 若旧记录里已经有提交时解析好的目标 Uri，则优先复用，避免生成新的重名输出文件。
     */
    suspend fun recover(
        historyId: Long,
        input: InputFile,
        outputFormat: String,
        quality: QualityPreset?,
        size: SizePreset?,
        existingOutputUri: android.net.Uri?,
    ): String {
        val requestId = UUID.randomUUID().toString()
        val destinationUri = existingOutputUri ?: outputResolutionMutex.withLock {
            outputLocationResolver.resolve(input.displayName, outputFormat, input.mediaType)
        }
        val request = ConversionRequest(
            id = requestId,
            input = input,
            outputFormat = outputFormat,
            quality = quality,
            size = size,
            destination = OutputDestination.ResolvedUri(destinationUri),
        )
        val existing = historyRepository.getById(historyId)
        if (existing != null) {
            historyRepository.update(
                existing.copy(
                    startTime = System.currentTimeMillis(),
                    endTime = null,
                    status = ConversionStatus.PENDING,
                    outputUri = destinationUri,
                    outputSizeBytes = null,
                    failureReason = null,
                ),
            )
        }
        val job = scope.launch {
            try {
                runTask(request, historyId)
            } finally {
                activeJobs.remove(requestId)
            }
        }
        activeJobs[requestId] = job
        return requestId
    }

    private suspend fun runTask(request: ConversionRequest, historyId: Long) {
        updateTask(request.id, ConversionTask(request, historyId, ConversionStatus.PENDING))
        val semaphore = semaphores.getValue(request.input.mediaType)
        try {
            semaphore.withPermit {
                updateTaskStatus(request.id, ConversionStatus.PREPARING)
                val engine = engineSelector.select(request)
                if (engine == null) {
                    failTask(request.id, historyId, ConversionError(ConversionError.Kind.UNSUPPORTED_OUTPUT, "no engine supports this request"))
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
                    is ConversionResult.Success -> completeTask(request.id, historyId, result)
                    is ConversionResult.Failure -> failTask(request.id, historyId, result.error)
                }
            }
        } catch (e: CancellationException) {
            cancelTask(request.id, historyId)
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
        val job = scope.launch {
            val existing = historyRepository.getById(task.historyId)
            if (existing != null) {
                historyRepository.update(existing.copy(status = ConversionStatus.PENDING, endTime = null, failureReason = null))
            }
            runTask(task.request, task.historyId)
        }
        activeJobs[taskId] = job
    }

    /** 再次转换会新建任务与历史记录，并重新解析输出位置，避免覆写既有结果。 */
    fun convertAgain(taskId: String): String? {
        val task = _tasks.value[taskId] ?: return null
        return submit(task.request.input, task.request.outputFormat, task.request.quality, task.request.size)
    }

    fun summary(): ConversionBatchSummary = ConversionBatchSummary.from(_tasks.value.values)

    private suspend fun completeTask(id: String, historyId: Long, result: ConversionResult.Success) {
        updateTask(id) { it?.copy(status = ConversionStatus.COMPLETED, progress = 1f, outputUri = result.outputUri) }
        updateHistory(historyId) {
            it.copy(
                status = ConversionStatus.COMPLETED,
                endTime = System.currentTimeMillis(),
                outputUri = result.outputUri,
                outputSizeBytes = result.outputSizeBytes,
            )
        }
    }

    private suspend fun failTask(id: String, historyId: Long, error: ConversionError) {
        val status = if (error.kind == ConversionError.Kind.CANCELLED) ConversionStatus.CANCELLED else ConversionStatus.FAILED
        updateTask(id) { it?.copy(status = status, error = error) }
        updateHistory(historyId) { it.copy(status = status, endTime = System.currentTimeMillis(), failureReason = error.debugMessage) }
    }

    private suspend fun cancelTask(id: String, historyId: Long) {
        updateTask(id) { it?.copy(status = ConversionStatus.CANCELLED) }
        updateHistory(historyId) { it.copy(status = ConversionStatus.CANCELLED, endTime = System.currentTimeMillis()) }
    }

    private suspend fun updateHistory(historyId: Long, transform: (ConversionHistoryRecord) -> ConversionHistoryRecord) {
        val record = historyRepository.getById(historyId) ?: return
        historyRepository.update(transform(record))
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

    private companion object {
        const val TAG = "ConversionOrchestrator"
    }
}
