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
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val failureReason: String?,
    val outputUri: Uri?,
    val quality: QualityPreset?,
    val size: SizePreset?,
) {
    val isActive: Boolean = status in ACTIVE_STATUSES
    val canConvertAgain: Boolean = !isActive
}

data class HistoryUiState(
    val items: List<HistoryUiItem> = emptyList(),
) {
    val activeCount: Int = items.count { it.isActive }
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

    val uiState: StateFlow<HistoryUiState> = repository.observeAll()
        .map { records -> HistoryUiState(records.map(ConversionHistoryRecord::toUiItem)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events = _events.asSharedFlow()

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteById(recordId)
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
            orchestrator.submit(input, record.outputFormat, record.quality, record.size)
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

private fun ConversionHistoryRecord.toUiItem() = HistoryUiItem(
    id = id,
    displayName = originalDisplayName,
    originalFormat = originalFormat,
    outputFormat = outputFormat,
    mediaType = mediaType,
    startTime = startTime,
    endTime = endTime,
    status = status,
    outputSizeBytes = outputSizeBytes,
    failureReason = failureReason,
    outputUri = outputUri,
    quality = quality,
    size = size,
)
