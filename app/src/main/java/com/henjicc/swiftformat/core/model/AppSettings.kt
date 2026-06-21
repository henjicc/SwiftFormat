package com.henjicc.swiftformat.core.model

/** 主题模式（见 SPEC 8.1）。 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** 预设强调色（见 SPEC 8.2），默认 [BLUE]。色值映射在 designsystem 层。 */
enum class AccentColor {
    BLUE,
    CYAN,
    GREEN,
    PURPLE,
    PINK,
    ORANGE,
    RED,
}

/** 应用语言（见 SPEC 7.1）。 */
enum class AppLanguage {
    SYSTEM,
    CHINESE,
    ENGLISH,
}

/**
 * 持久化的应用设置。默认值见 SPEC 22。
 * 第一版先承载外观相关项，转换默认值/文件项后续在 TASK-07 扩展。
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.BLUE,
    val dynamicColor: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val defaultImageQuality: QualityPreset = QualityPreset.HIGH,
    val defaultVideoQuality: QualityPreset = QualityPreset.HIGH,
    val defaultAudioQuality: QualityPreset = QualityPreset.HIGH,
    val autoCleanupTempFiles: Boolean = true,
    val showCompletionNotification: Boolean = true,
)
