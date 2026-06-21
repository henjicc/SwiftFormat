package com.henjicc.swiftformat.core.model

/**
 * 统一质量档位（见 SPEC 5.2 / 11.2）。
 * UI 仅使用这四档，底层引擎负责映射为各媒体类型真实参数（码率/质量值等）。
 */
enum class QualityPreset {
    BEST,
    HIGH,
    STANDARD,
    SMALL_SIZE,
}
