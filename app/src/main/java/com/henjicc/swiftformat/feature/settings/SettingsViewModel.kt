package com.henjicc.swiftformat.feature.settings

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.core.common.InMemoryLogStore
import com.henjicc.swiftformat.conversion.ConversionOrchestrator
import com.henjicc.swiftformat.core.datastore.SettingsRepository
import com.henjicc.swiftformat.core.file.CacheMaintenance
import com.henjicc.swiftformat.core.model.AccentColor
import com.henjicc.swiftformat.core.model.AppLanguage
import com.henjicc.swiftformat.core.model.AppSettings
import com.henjicc.swiftformat.core.model.NameCollisionStrategy
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val cacheMaintenance: CacheMaintenance,
    private val orchestrator: ConversionOrchestrator,
    private val appContext: Context,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    /** 自定义输出目录的人类可读名称（取 SAF 树根文档的 DISPLAY_NAME），未设置时为 null。 */
    val customOutputDirectoryLabel: StateFlow<String?> = settings
        .map { it.customOutputDirectoryUri }
        .distinctUntilChanged()
        .map { uriString -> uriString?.let { resolveDirectoryDisplayName(appContext, Uri.parse(it)) } }
        .flowOn(Dispatchers.IO)
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)
    private val _cacheBytes = MutableStateFlow(cacheMaintenance.cacheSizeBytes())
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

    private val _events = MutableSharedFlow<Int>()
    val events = _events.asSharedFlow()
    private val _logs = MutableStateFlow(InMemoryLogStore.snapshot())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    @Suppress("DEPRECATION")
    val appVersion: String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "1.0"
    }.getOrDefault("1.0")

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setAccentColor(color: AccentColor) = viewModelScope.launch { repository.setAccentColor(color) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setDynamicColor(enabled) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { repository.setLanguage(language) }
    fun setDefaultImageQuality(quality: QualityPreset) = viewModelScope.launch { repository.setDefaultImageQuality(quality) }
    fun setDefaultVideoQuality(quality: QualityPreset) = viewModelScope.launch { repository.setDefaultVideoQuality(quality) }
    fun setDefaultAudioQuality(quality: QualityPreset) = viewModelScope.launch { repository.setDefaultAudioQuality(quality) }
    fun setAutoCleanupTempFiles(enabled: Boolean) = viewModelScope.launch { repository.setAutoCleanupTempFiles(enabled) }
    fun setShowCompletionNotification(enabled: Boolean) = viewModelScope.launch { repository.setShowCompletionNotification(enabled) }
    fun setPreserveImageMetadata(enabled: Boolean) = viewModelScope.launch { repository.setPreserveImageMetadata(enabled) }
    fun setCustomOutputDirectory(uri: Uri?) = viewModelScope.launch { repository.setCustomOutputDirectory(uri) }
    fun setNameCollisionStrategy(strategy: NameCollisionStrategy) = viewModelScope.launch { repository.setNameCollisionStrategy(strategy) }
    fun setScrollFileNames(enabled: Boolean) = viewModelScope.launch { repository.setScrollFileNames(enabled) }

    fun clearCache() = viewModelScope.launch {
        if (orchestrator.summary().inProgress > 0) {
            _events.emit(com.henjicc.swiftformat.R.string.settings_clear_cache_busy)
            return@launch
        }
        cacheMaintenance.clearAppCache()
        _cacheBytes.value = cacheMaintenance.cacheSizeBytes()
        _events.emit(com.henjicc.swiftformat.R.string.settings_cache_cleared)
    }

    fun refreshLogs() {
        _logs.value = InMemoryLogStore.snapshot()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SwiftFormatApplication
                SettingsViewModel(
                    repository = app.container.settingsRepository,
                    cacheMaintenance = app.container.cacheMaintenance,
                    orchestrator = app.container.conversionOrchestrator,
                    appContext = app.applicationContext,
                )
            }
        }
    }
}

private fun resolveDirectoryDisplayName(context: Context, treeUri: Uri): String? = runCatching {
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
    context.contentResolver.query(
        documentUri,
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}.getOrNull()
