package com.henjicc.swiftformat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henjicc.swiftformat.core.designsystem.SwiftFormatTheme
import com.henjicc.swiftformat.core.model.AppSettings
import com.henjicc.swiftformat.ui.navigation.SwiftFormatApp

class MainActivity : ComponentActivity() {

    private val container get() = (application as SwiftFormatApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)
        val settingsRepository = container.settingsRepository
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /** 接收来自系统分享菜单的图片/视频/音频（见 SPEC 3.1 / 12.1）。 */
    private fun handleShareIntent(intent: Intent) {
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.let { listOf(it) }
                    .orEmpty()

            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    .orEmpty()

            else -> emptyList()
        }
        if (uris.isNotEmpty()) {
            container.incomingShareFiles.tryEmit(uris)
        }
    }
}
