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
 * 输出重名处理策略（见 SPEC 12.4）。SPEC 还列了"每次询问"，但当前批量提交即异步入队的编排模型
 * 不支持中途暂停等待用户输入，第一版先只做这两种不需要打断转换流程的策略。
 */
enum class NameCollisionStrategy {
    AUTO_NUMBER,
    OVERWRITE,
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
    val defaultImageQuality: QualityPreset = QualityPreset.STANDARD,
    val defaultVideoQuality: QualityPreset = QualityPreset.STANDARD,
    val defaultAudioQuality: QualityPreset = QualityPreset.STANDARD,
    val autoCleanupTempFiles: Boolean = true,
    val showCompletionNotification: Boolean = true,
    val preserveImageMetadata: Boolean = true,
    val customOutputDirectoryUri: String? = null,
    val nameCollisionStrategy: NameCollisionStrategy = NameCollisionStrategy.AUTO_NUMBER,
    val scrollFileNames: Boolean = false,
)
