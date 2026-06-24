package com.henjicc.swiftformat

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.DragAndDropPermissions
import android.view.DragEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henjicc.swiftformat.core.designsystem.SwiftFormatTheme
import com.henjicc.swiftformat.core.localization.AppLocaleProvider
import com.henjicc.swiftformat.core.model.AppSettings
import com.henjicc.swiftformat.ui.navigation.SwiftFormatApp
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private val container get() = (application as SwiftFormatApplication).container
    private val activeDragPermissions = mutableListOf<DragAndDropPermissions>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)
        val settingsRepository = container.settingsRepository
        setContent {
            val settingsFlow = remember(settingsRepository) {
                settingsRepository.settings.map<AppSettings, AppSettings?> { it }
            }
            val loadedSettings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            val settings = loadedSettings ?: AppSettings()
            AppLocaleProvider(language = settings.language) {
                SwiftFormatTheme(
                    themeMode = settings.themeMode,
                    accentColor = settings.accentColor,
                    dynamicColor = settings.dynamicColor,
                ) {
                    SwiftFormatApp()
                }
            }
        }
        installFileDropListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    override fun onDestroy() {
        activeDragPermissions.forEach { permissions ->
            runCatching { permissions.release() }
        }
        activeDragPermissions.clear()
        super.onDestroy()
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

    /** 接收支持 Android 系统拖放的相册/文件管理器拖入的媒体 Uri，并复用首页追加导入链路。 */
    private fun installFileDropListener() {
        window.decorView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DROP -> {
                    requestDragAndDropPermissions(event)?.let { permissions ->
                        activeDragPermissions += permissions
                    }
                    val uris = event.clipData.extractUris()
                    uris.forEach(::tryPersistReadPermission)
                    if (uris.isNotEmpty()) container.incomingShareFiles.tryEmit(uris)
                    uris.isNotEmpty()
                }
                DragEvent.ACTION_DRAG_ENDED,
                DragEvent.ACTION_DRAG_ENTERED,
                DragEvent.ACTION_DRAG_EXITED,
                DragEvent.ACTION_DRAG_LOCATION,
                -> true
                else -> false
            }
        }
    }

    private fun ClipData?.extractUris(): List<Uri> {
        if (this == null) return emptyList()
        val uris = ArrayList<Uri>(itemCount)
        for (index in 0 until itemCount) {
            val item = getItemAt(index)
            val uri = item.uri ?: item.intent?.data
            if (uri != null) uris += uri
        }
        return uris.distinctBy { it.toString() }
    }

    private fun tryPersistReadPermission(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
