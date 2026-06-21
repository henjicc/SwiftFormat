package com.henjicc.swiftformat.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.henjicc.swiftformat.core.model.AccentColor
import com.henjicc.swiftformat.core.model.AppLanguage
import com.henjicc.swiftformat.core.model.AppSettings
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
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
                ?: QualityPreset.HIGH,
            defaultVideoQuality = prefs[Keys.DEFAULT_VIDEO_QUALITY]?.let { enumValueOfOrNull<QualityPreset>(it) }
                ?: QualityPreset.HIGH,
            defaultAudioQuality = prefs[Keys.DEFAULT_AUDIO_QUALITY]?.let { enumValueOfOrNull<QualityPreset>(it) }
                ?: QualityPreset.HIGH,
            autoCleanupTempFiles = prefs[Keys.AUTO_CLEANUP_TEMP_FILES]?.toBooleanStrictOrNull() ?: true,
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
}

private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
