package com.henjicc.swiftformat.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.henjicc.swiftformat.core.model.AppLanguage

/**
 * 在 Compose 树内切换字符串资源语言，避免 Activity 重建造成黑屏。
 */
@Composable
fun AppLocaleProvider(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedContext = remember(baseContext, language) {
        AppLocaleManager.localizedContext(baseContext, language)
    }
    val localizedConfiguration = remember(baseConfiguration, baseContext, language) {
        AppLocaleManager.localizedConfiguration(baseContext, language)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        content = content,
    )
}
