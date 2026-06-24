package com.henjicc.swiftformat.feature.history

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.conversion.ConversionOrchestrator
import com.henjicc.swiftformat.core.database.ConversionHistoryRepository
import com.henjicc.swiftformat.core.file.FileMetadataReader
import com.henjicc.swiftformat.core.file.ResultFileActions
import com.henjicc.swiftformat.core.model.ConversionHistoryRecord
import com.henjicc.swiftformat.core.model.ConversionError
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.FailureReasonCodec
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiItem(
    val id: Long,
    val displayName: String,
    val originalFormat: String?,
    val outputFormat: String,
    val mediaType: MediaType,
    val startTime: Long,
    val endTime: Long?,
    val status: ConversionStatus,
    val outputSizeBytes: Long?,
    val failureKind: ConversionError.Kind?,
    val failureDetails: String?,
    val outputUri: Uri?,
    val quality: QualityPreset?,
    val size: SizePreset?,
) {
    val isActive: Boolean = status in ACTIVE_STATUSES
    val canConvertAgain: Boolean = !isActive
}

data class HistoryUiState(
    val items: List<HistoryUiItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val activeCount: Int = 0,
) {
    val isSelectionMode: Boolean = selectedIds.isNotEmpty()
    val selectedCount: Int = selectedIds.size
}

sealed interface HistoryEvent {
    data object NavigateToProgress : HistoryEvent
    data class ShowMessage(@param:StringRes val messageRes: Int) : HistoryEvent
}

class HistoryViewModel(
    private val repository: ConversionHistoryRepository,
    private val metadataReader: FileMetadataReader,
    private val orchestrator: ConversionOrchestrator,
    private val resultFileActions: ResultFileActions,
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeAll(),
        orchestrator.tasks,
        selectedIds,
    ) { records, tasks, selected ->
        val terminalItems = records
            .filter { it.status !in ACTIVE_STATUSES }
            .map(ConversionHistoryRecord::toUiItem)
        val availableIds = terminalItems.mapTo(HashSet()) { it.id }
        HistoryUiState(
            items = terminalItems,
            selectedIds = selected.filterTo(LinkedHashSet()) { it in availableIds },
            activeCount = tasks.values.count { it.status in ACTIVE_STATUSES },
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events = _events.asSharedFlow()

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteById(recordId)
            selectedIds.update { it - recordId }
        }
    }

    fun toggleSelection(recordId: Long) {
        selectedIds.update { selected ->
            if (recordId in selected) selected - recordId else selected + recordId
        }
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun deleteSelectedRecords() {
        val ids = selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.deleteById(it) }
            selectedIds.value = emptySet()
        }
    }

    fun deleteOutput(recordId: Long) {
        viewModelScope.launch {
            val record = repository.getById(recordId) ?: return@launch
            val outputUri = record.outputUri ?: return@launch
            if (resultFileActions.delete(outputUri)) {
                repository.update(record.copy(outputUri = null, outputSizeBytes = null))
            } else {
                _events.emit(HistoryEvent.ShowMessage(R.string.file_action_failed))
            }
        }
    }

    fun convertAgain(recordId: Long) {
        viewModelScope.launch {
            val record = repository.getById(recordId) ?: return@launch
            val input = runCatching { metadataReader.read(record.inputUri) }.getOrNull()
            if (input == null) {
                _events.emit(HistoryEvent.ShowMessage(R.string.history_reconvert_failed))
                return@launch
            }
            val taskId = orchestrator.submit(
                input = input,
                outputFormat = record.outputFormat,
                quality = record.quality,
                size = record.size,
            )
            orchestrator.showProgressFor(listOf(taskId))
            _events.emit(HistoryEvent.NavigateToProgress)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SwiftFormatApplication
                HistoryViewModel(
                    repository = app.container.conversionHistoryRepository,
                    metadataReader = app.container.fileMetadataReader,
                    orchestrator = app.container.conversionOrchestrator,
                    resultFileActions = app.container.resultFileActions,
                )
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

private fun ConversionHistoryRecord.toUiItem(): HistoryUiItem {
    val decodedFailure = FailureReasonCodec.decode(failureReason)
    return HistoryUiItem(
        id = id,
        displayName = originalDisplayName,
        originalFormat = originalFormat,
        outputFormat = outputFormat,
        mediaType = mediaType,
        startTime = startTime,
        endTime = endTime,
        status = status,
        outputSizeBytes = outputSizeBytes,
        failureKind = decodedFailure?.kind,
        failureDetails = decodedFailure?.details ?: failureReason,
        outputUri = outputUri,
        quality = quality,
        size = size,
    )
}
