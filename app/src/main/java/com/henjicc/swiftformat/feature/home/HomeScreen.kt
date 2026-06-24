package com.henjicc.swiftformat.feature.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.service.ConversionForegroundService

@Composable
fun HomeScreen(
    onConversionStarted: () -> Unit = {},
    onOpenActiveTask: () -> Unit = onConversionStarted,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageLoader = (context.applicationContext as SwiftFormatApplication).container.thumbnailImageLoader

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            viewModel.addFiles(uris)
        }
    }
    val launchPicker = { picker.launch(arrayOf("image/*", "video/*", "audio/*")) }

    // Android 13+ 通知需要运行时授权；未授权也不阻塞转换，只是看不到前台服务通知（见 TASK-06 已知简化）。
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    val onStartConversion = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (viewModel.startConversion().isNotEmpty()) {
            ConversionForegroundService.start(context)
            onConversionStarted()
        }
    }

    HomeContent(
        state = state,
        onPick = launchPicker,
        onAddMore = launchPicker,
        onClear = viewModel::clear,
        onRemove = viewModel::removeFile,
        onFormatChange = viewModel::setOutputFormat,
        onQualityChange = viewModel::setQuality,
        onSizeChange = viewModel::setSize,
        onStartConversion = onStartConversion,
        onOpenActiveTask = onOpenActiveTask,
        sizeFormatter = { Formatter.formatShortFileSize(context, it) },
        imageLoader = imageLoader,
    )
}
