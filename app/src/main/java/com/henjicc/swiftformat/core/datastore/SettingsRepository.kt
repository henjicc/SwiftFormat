package com.henjicc.swiftformat.core.datastore

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.henjicc.swiftformat.core.model.AccentColor
import com.henjicc.swiftformat.core.model.AppLanguage
import com.henjicc.swiftformat.core.model.AppSettings
import com.henjicc.swiftformat.core.model.NameCollisionStrategy
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 应用设置仓库，基于 DataStore Preferences 持久化（见 SPEC 8.1/8.2/7.1）。
 * 暴露 [settings] 冷流供 UI 实时观察，修改后界面即时刷新。
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val DYNAMIC_COLOR = stringPreferencesKey("dynamic_color")
        val LANGUAGE = stringPreferencesKey("language")
        val DEFAULT_IMAGE_QUALITY = stringPreferencesKey("default_image_quality")
        val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val DEFAULT_AUDIO_QUALITY = stringPreferencesKey("default_audio_quality")
        val AUTO_CLEANUP_TEMP_FILES = stringPreferencesKey("auto_cleanup_temp_files")
        val SHOW_COMPLETION_NOTIFICATION = stringPreferencesKey("show_completion_notification")
        val PRESERVE_IMAGE_METADATA = stringPreferencesKey("preserve_image_metadata")
        val CUSTOM_OUTPUT_DIRECTORY_URI = stringPreferencesKey("custom_output_directory_uri")
        val NAME_COLLISION_STRATEGY = stringPreferencesKey("name_collision_strategy")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { enumValueOfOrNull<ThemeMode>(it) }
                ?: ThemeMode.SYSTEM,
            accentColor = prefs[Keys.ACCENT_COLOR]?.let { enumValueOfOrNull<AccentColor>(it) }
                ?: AccentColor.BLUE,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR]?.toBooleanStrictOrNull() ?: false,
            language = prefs[Keys.LANGUAGE]?.let { enumValueOfOrNull<AppLanguage>(it) }
                ?: AppLanguage.SYSTEM,
            defaultImageQuality = prefs[Keys.DEFAULT_IMAGE_QUALITY]?.let { enumValueOfOrNull<QualityPreset>(it) }
                ?: QualityPreset.STANDARD,
            defaultVideoQuality = prefs[Keys.DEFAULT_VIDEO_QUALITY]?.let { enumValueOfOrNull<QualityPreset>(it) }
                ?: QualityPreset.STANDARD,
            defaultAudioQuality = prefs[Keys.DEFAULT_AUDIO_QUALITY]?.let { enumValueOfOrNull<QualityPreset>(it) }
                ?: QualityPreset.STANDARD,
            autoCleanupTempFiles = prefs[Keys.AUTO_CLEANUP_TEMP_FILES]?.toBooleanStrictOrNull() ?: true,
            showCompletionNotification = prefs[Keys.SHOW_COMPLETION_NOTIFICATION]?.toBooleanStrictOrNull() ?: true,
            preserveImageMetadata = prefs[Keys.PRESERVE_IMAGE_METADATA]?.toBooleanStrictOrNull() ?: true,
            customOutputDirectoryUri = prefs[Keys.CUSTOM_OUTPUT_DIRECTORY_URI],
            nameCollisionStrategy = prefs[Keys.NAME_COLLISION_STRATEGY]?.let { enumValueOfOrNull<NameCollisionStrategy>(it) }
                ?: NameCollisionStrategy.AUTO_NUMBER,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(color: AccentColor) {
        context.settingsDataStore.edit { it[Keys.ACCENT_COLOR] = color.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled.toString() }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setDefaultImageQuality(quality: QualityPreset) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_IMAGE_QUALITY] = quality.name }
    }

    suspend fun setDefaultVideoQuality(quality: QualityPreset) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_VIDEO_QUALITY] = quality.name }
    }

    suspend fun setDefaultAudioQuality(quality: QualityPreset) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_AUDIO_QUALITY] = quality.name }
    }

    suspend fun setAutoCleanupTempFiles(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_CLEANUP_TEMP_FILES] = enabled.toString() }
    }

    suspend fun setShowCompletionNotification(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_COMPLETION_NOTIFICATION] = enabled.toString() }
    }

    suspend fun setPreserveImageMetadata(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PRESERVE_IMAGE_METADATA] = enabled.toString() }
    }

    /**
     * 设为非空 [uri] 时需要先持久化该 SAF 目录的读写授权（否则进程重启后会失效）；
     * 切换或清空时释放旧目录的授权，避免授权列表无限增长。
     */
    suspend fun setCustomOutputDirectory(uri: Uri?) {
        val previous = context.settingsDataStore.data.first()[Keys.CUSTOM_OUTPUT_DIRECTORY_URI]?.let(Uri::parse)
        if (previous != null && previous != uri) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    previous,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        context.settingsDataStore.edit { prefs ->
            if (uri != null) prefs[Keys.CUSTOM_OUTPUT_DIRECTORY_URI] = uri.toString() else prefs.remove(Keys.CUSTOM_OUTPUT_DIRECTORY_URI)
        }
    }

    suspend fun setNameCollisionStrategy(strategy: NameCollisionStrategy) {
        context.settingsDataStore.edit { it[Keys.NAME_COLLISION_STRATEGY] = strategy.name }
    }
}

private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
