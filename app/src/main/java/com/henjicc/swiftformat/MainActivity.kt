package com.henjicc.swiftformat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henjicc.swiftformat.core.designsystem.SwiftFormatTheme
import com.henjicc.swiftformat.core.model.AppSettings
import com.henjicc.swiftformat.ui.navigation.SwiftFormatApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository =
            (application as SwiftFormatApplication).container.settingsRepository
        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            SwiftFormatTheme(
                themeMode = settings.themeMode,
                accentColor = settings.accentColor,
                dynamicColor = settings.dynamicColor,
            ) {
                SwiftFormatApp()
            }
        }
    }
}
