package com.henjicc.swiftformat.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import com.henjicc.swiftformat.R

/** 底部导航三个顶层目的地（见 SPEC 6.2）。 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    CONVERT("convert", R.string.nav_convert, Icons.Filled.SwapHoriz),
    HISTORY("history", R.string.nav_history, Icons.Filled.History),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings),
}
