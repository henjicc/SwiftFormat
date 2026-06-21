package com.henjicc.swiftformat.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.henjicc.swiftformat.core.model.AccentColor

/** 各预设强调色的 primary 四元组（浅色 / 深色）。 */
private fun lightAccent(accent: AccentColor): AccentColors = when (accent) {
    AccentColor.BLUE -> AccentColors(Color(0xFF1565C0), Color.White, Color(0xFFD3E4FF), Color(0xFF001D36))
    AccentColor.CYAN -> AccentColors(Color(0xFF00696E), Color.White, Color(0xFF6FF6FF), Color(0xFF002021))
    AccentColor.GREEN -> AccentColors(Color(0xFF356A20), Color.White, Color(0xFFB6F2A0), Color(0xFF042100))
    AccentColor.PURPLE -> AccentColors(Color(0xFF6750A4), Color.White, Color(0xFFEADDFF), Color(0xFF21005D))
    AccentColor.PINK -> AccentColors(Color(0xFF9A4057), Color.White, Color(0xFFFFD9E2), Color(0xFF3E0021))
    AccentColor.ORANGE -> AccentColors(Color(0xFF8B5000), Color.White, Color(0xFFFFDCC0), Color(0xFF2D1600))
    AccentColor.RED -> AccentColors(Color(0xFFB3261E), Color.White, Color(0xFFF9DEDC), Color(0xFF410E0B))
}

private fun darkAccent(accent: AccentColor): AccentColors = when (accent) {
    AccentColor.BLUE -> AccentColors(Color(0xFF9FCBFF), Color(0xFF003258), Color(0xFF00497D), Color(0xFFD3E4FF))
    AccentColor.CYAN -> AccentColors(Color(0xFF4DD9E0), Color(0xFF00363A), Color(0xFF004F53), Color(0xFF6FF6FF))
    AccentColor.GREEN -> AccentColors(Color(0xFF9BD67F), Color(0xFF0C3900), Color(0xFF1E5200), Color(0xFFB6F2A0))
    AccentColor.PURPLE -> AccentColors(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF))
    AccentColor.PINK -> AccentColors(Color(0xFFFFB1C5), Color(0xFF5E1133), Color(0xFF7B2949), Color(0xFFFFD9E2))
    AccentColor.ORANGE -> AccentColors(Color(0xFFFFB870), Color(0xFF4A2800), Color(0xFF693C00), Color(0xFFFFDCC0))
    AccentColor.RED -> AccentColors(Color(0xFFF2B8B5), Color(0xFF601410), Color(0xFF8C1D18), Color(0xFFF9DEDC))
}

/** 用于设置页色块展示的代表色（取浅色 primary）。 */
fun accentSwatchColor(accent: AccentColor): Color = lightAccent(accent).primary

/** 根据强调色与明暗模式生成 Material3 配色（中性基底 + 强调 primary 四元组）。 */
fun accentColorScheme(accent: AccentColor, dark: Boolean): ColorScheme {
    val a = if (dark) darkAccent(accent) else lightAccent(accent)
    val base = if (dark) BaseDarkColorScheme else BaseLightColorScheme
    return base.copy(
        primary = a.primary,
        onPrimary = a.onPrimary,
        primaryContainer = a.primaryContainer,
        onPrimaryContainer = a.onPrimaryContainer,
        secondary = a.primary,
        onSecondary = a.onPrimary,
        secondaryContainer = a.primaryContainer,
        onSecondaryContainer = a.onPrimaryContainer,
    )
}
