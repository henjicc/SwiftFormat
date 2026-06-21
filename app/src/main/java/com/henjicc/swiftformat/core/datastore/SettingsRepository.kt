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
}

private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? =
    enumValues<T>().firstOrNull { it.name == name }
