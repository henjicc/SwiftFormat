package com.henjicc.swiftformat.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 中性基底配色（背景/表面/轮廓等），各强调色共用；
 * 强调色仅覆盖 primary 四元组（见 [accentColorScheme]）。默认强调色为蓝色（SPEC 8.2）。
 */

internal val BaseLightColorScheme = lightColorScheme(
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val BaseDarkColorScheme = darkColorScheme(
    background = Color(0xFF191C1D),
    onBackground = Color(0xFFE1E3E4),
    surface = Color(0xFF191C1D),
    onSurface = Color(0xFFE1E3E4),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** 单个强调色在某一明暗模式下的 primary 四元组。 */
internal data class AccentColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)
