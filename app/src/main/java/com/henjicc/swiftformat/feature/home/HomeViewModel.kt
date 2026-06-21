package com.henjicc.swiftformat.feature.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.conversion.ConversionOrchestrator
import com.henjicc.swiftformat.core.datastore.SettingsRepository
import com.henjicc.swiftformat.core.file.FileMetadataReader
import com.henjicc.swiftformat.core.model.AppSettings
import com.henjicc.swiftformat.core.model.GroupConversionSettings
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputFormatCatalog
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
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
    val settings: Map<MediaType, GroupConversionSettings> = emptyMap(),
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
    private val orchestrator: ConversionOrchestrator,
    private val settingsRepository: SettingsRepository,
    incomingShareFiles: MutableSharedFlow<List<Uri>>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var currentAppSettings: AppSettings = AppSettings()

    init {
        viewModelScope.launch {
            incomingShareFiles.collect { uris -> addFiles(uris) }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings -> currentAppSettings = settings }
        }
    }

    /**
     * 读取元数据并追加（按 id 去重），读取在 IO 线程，不阻塞主线程。
     * 为新出现的分组补齐默认参数设置；已有分组的设置保持不变（见 SPEC 4.4「添加更多文件后原设置不会无故丢失」）。
     */
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
                val newFiles = state.files + added
                val activeTypes = newFiles.mapTo(HashSet()) { it.mediaType } - MediaType.UNKNOWN
                val newSettings = state.settings.toMutableMap()
                activeTypes.forEach { type ->
                    newSettings.getOrPut(type) { defaultSettings(type) }
                }
                state.copy(files = newFiles, settings = newSettings, isLoading = false)
            }
        }
    }

    fun removeFile(id: String) {
        _uiState.update { state -> state.copy(files = state.files.filterNot { it.id == id }) }
    }

    fun clear() {
        _uiState.update { HomeUiState() }
    }

    /**
     * 提交所有受支持分组到任务编排层（见 SPEC 4.4「开始转换」），随后清空首页文件列表
     * （已提交的文件由 [ConversionOrchestrator] 持有自己的快照，清空不影响已提交的任务）。
     * 不支持的文件（[HomeUiState.unsupported]）不会被提交。
     */
    fun startConversion() {
        val state = _uiState.value
        state.groups.forEach { (type, files) ->
            val settings = state.settings[type] ?: OutputFormatCatalog.defaultSettings(type)
            orchestrator.submitAll(files, settings.outputFormat, settings.quality, settings.size)
        }
        clear()
    }

    fun setOutputFormat(mediaType: MediaType, format: String) {
        updateSettings(mediaType) { it.copy(outputFormat = format) }
    }

    fun setQuality(mediaType: MediaType, quality: QualityPreset) {
        updateSettings(mediaType) { it.copy(quality = quality) }
    }

    fun setSize(mediaType: MediaType, size: SizePreset) {
        updateSettings(mediaType) { it.copy(size = size) }
    }

    private inline fun updateSettings(
        mediaType: MediaType,
        transform: (GroupConversionSettings) -> GroupConversionSettings,
    ) {
        _uiState.update { state ->
            val current = state.settings[mediaType] ?: OutputFormatCatalog.defaultSettings(mediaType)
            state.copy(settings = state.settings + (mediaType to transform(current)))
        }
    }

    private fun defaultSettings(mediaType: MediaType): GroupConversionSettings {
        val base = OutputFormatCatalog.defaultSettings(mediaType)
        val quality = when (mediaType) {
            MediaType.IMAGE -> currentAppSettings.defaultImageQuality
            MediaType.VIDEO -> currentAppSettings.defaultVideoQuality
            MediaType.AUDIO -> currentAppSettings.defaultAudioQuality
            MediaType.UNKNOWN -> base.quality ?: QualityPreset.HIGH
        }
        return base.copy(quality = quality)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SwiftFormatApplication
                HomeViewModel(
                    metadataReader = app.container.fileMetadataReader,
                    orchestrator = app.container.conversionOrchestrator,
                    settingsRepository = app.container.settingsRepository,
                    incomingShareFiles = app.container.incomingShareFiles,
                )
            }
        }
    }
}
