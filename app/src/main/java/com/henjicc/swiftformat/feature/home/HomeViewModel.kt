package com.henjicc.swiftformat.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.core.file.FileMetadataReader
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 媒体分组在界面上的固定展示顺序（见 SPEC 4.3）。 */
private val GROUP_ORDER = listOf(MediaType.VIDEO, MediaType.IMAGE, MediaType.AUDIO)

data class HomeUiState(
    val files: List<InputFile> = emptyList(),
    val isLoading: Boolean = false,
) {
    val unsupported: List<InputFile> = files.filter { it.mediaType == MediaType.UNKNOWN }

    /** 受支持文件按 视频/图片/音频 顺序分组，空组不出现。 */
    val groups: List<Pair<MediaType, List<InputFile>>> =
        GROUP_ORDER.mapNotNull { type ->
            files.filter { it.mediaType == type }.takeIf { it.isNotEmpty() }?.let { type to it }
        }

    val totalCount: Int = files.size
    val totalSizeBytes: Long = files.sumOf { it.sizeBytes ?: 0L }
    val hasFiles: Boolean = files.isNotEmpty()
}

class HomeViewModel(
    private val metadataReader: FileMetadataReader,
    incomingShareFiles: MutableSharedFlow<List<Uri>>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            incomingShareFiles.collect { uris -> addFiles(uris) }
        }
    }

    /** 读取元数据并追加（按 id 去重），读取在 IO 线程，不阻塞主线程。 */
    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val existingIds = _uiState.value.files.mapTo(HashSet()) { it.id }
            val added = ArrayList<InputFile>()
            for (uri in uris) {
                val file = metadataReader.read(uri)
                if (existingIds.add(file.id)) added += file
            }
            _uiState.update { state ->
                state.copy(files = state.files + added, isLoading = false)
            }
        }
    }

    fun removeFile(id: String) {
        _uiState.update { state -> state.copy(files = state.files.filterNot { it.id == id }) }
    }

    fun clear() {
        _uiState.update { HomeUiState() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SwiftFormatApplication
                HomeViewModel(
                    metadataReader = app.container.fileMetadataReader,
                    incomingShareFiles = app.container.incomingShareFiles,
                )
            }
        }
    }
}
