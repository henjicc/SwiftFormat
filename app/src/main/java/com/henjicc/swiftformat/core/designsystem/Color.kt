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
    // 以下中性表面角色（NavigationBar/Card/BottomSheet 等默认用色）若不显式指定，
    // 会落到 Compose Material3 自带的基线配色（偏紫粉），与强调色无关也不随之变化，
    // 这正是底栏/列表卡片背景看起来发粉的根因，此处统一收口为中性灰阶。
    surfaceDim = Color(0xFFD8DAE0),
    surfaceBright = Color(0xFFF8F9FA),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F3F5),
    surfaceContainer = Color(0xFFECEEF1),
    surfaceContainerHigh = Color(0xFFE6E8EC),
    surfaceContainerHighest = Color(0xFFE0E3E7),
    inverseSurface = Color(0xFF191C1D),
    inverseOnSurface = Color(0xFFE1E3E4),
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
    surfaceDim = Color(0xFF111416),
    surfaceBright = Color(0xFF373A3B),
    surfaceContainerLowest = Color(0xFF0C0F10),
    surfaceContainerLow = Color(0xFF1D2021),
    surfaceContainer = Color(0xFF212425),
    surfaceContainerHigh = Color(0xFF2C2F30),
    surfaceContainerHighest = Color(0xFF36393A),
    inverseSurface = Color(0xFFF8F9FA),
    inverseOnSurface = Color(0xFF191C1D),
)

/** 单个强调色在某一明暗模式下的 primary 四元组。 */
internal data class AccentColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)
