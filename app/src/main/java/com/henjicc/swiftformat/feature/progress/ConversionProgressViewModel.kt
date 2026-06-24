package com.henjicc.swiftformat.feature.progress

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.conversion.ConversionOrchestrator
import com.henjicc.swiftformat.conversion.ConversionTask
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.MediaType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 转换进度页面单个文件行的展示数据（见 SPEC 4.5）。从 [ConversionTask] 派生，不直接把领域模型传给 UI。 */
data class ConversionTaskUiItem(
    val taskId: String,
    val displayName: String,
    val originalFormat: String?,
    val outputFormat: String,
    val mediaType: MediaType,
    val inputUri: Uri,
    val outputUri: Uri?,
    val status: ConversionStatus,
    val progress: Float,
    val failureKind: ConversionError.Kind?,
    val failureDetails: String?,
) {
    val isActive: Boolean = status in ACTIVE_STATUSES
    val canRetry: Boolean = status == ConversionStatus.FAILED
    val canConvertAgain: Boolean = status == ConversionStatus.COMPLETED
    val hasFailureDetails: Boolean = !failureDetails.isNullOrBlank()
}

data class ConversionProgressUiState(
    val items: List<ConversionTaskUiItem> = emptyList(),
) {
    val total: Int = items.size
    val completed: Int = items.count { it.status == ConversionStatus.COMPLETED }
    val failed: Int = items.count { it.status == ConversionStatus.FAILED }
    val cancelled: Int = items.count { it.status == ConversionStatus.CANCELLED }
    val hasActiveTasks: Boolean = items.any { it.isActive }
    val currentItem: ConversionTaskUiItem? = items.firstOrNull { it.isActive }

    /** 已完成/失败/取消都记 100%，进行中按自身进度折算，用于顶部总体进度条。 */
    val overallFraction: Float = if (items.isEmpty()) {
        0f
    } else {
        items.sumOf { item -> if (item.isActive) item.progress.toDouble() else 1.0 }.toFloat() / items.size
    }
}

class ConversionProgressViewModel(private val orchestrator: ConversionOrchestrator) : ViewModel() {

    val uiState: StateFlow<ConversionProgressUiState> = orchestrator.tasks
        .combine(orchestrator.progressTaskIds) { tasks, progressTaskIds ->
            scopedTasks(tasks, progressTaskIds)
        }
        .map { tasks -> ConversionProgressUiState(tasks.map(ConversionTask::toUiItem)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversionProgressUiState())

    fun cancel(taskId: String) = orchestrator.cancel(taskId)
    fun cancelAll() = orchestrator.cancelAll()
    fun retry(taskId: String) = orchestrator.retry(taskId)
    fun convertAgain(taskId: String): String? = orchestrator.convertAgain(taskId)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SwiftFormatApplication
                ConversionProgressViewModel(app.container.conversionOrchestrator)
            }
        }
    }
}

private val ACTIVE_STATUSES = setOf(
    ConversionStatus.PENDING,
    ConversionStatus.PREPARING,
    ConversionStatus.CONVERTING,
    ConversionStatus.SAVING,
)

private fun scopedTasks(tasks: Map<String, ConversionTask>, progressTaskIds: Set<String>): List<ConversionTask> =
    if (progressTaskIds.isEmpty()) {
        tasks.values.filter { it.status in ACTIVE_STATUSES }
    } else {
        progressTaskIds.mapNotNull(tasks::get)
    }

private fun ConversionTask.toUiItem() = ConversionTaskUiItem(
    taskId = request.id,
    displayName = request.input.displayName,
    originalFormat = request.input.extension,
    outputFormat = request.outputFormat,
    mediaType = request.input.mediaType,
    inputUri = request.input.uri,
    outputUri = outputUri,
    status = status,
    progress = progress,
    failureKind = error?.kind,
    failureDetails = error?.debugMessage,
)
