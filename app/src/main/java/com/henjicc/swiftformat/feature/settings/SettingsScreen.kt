package com.henjicc.swiftformat.feature.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.henjicc.swiftformat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val customOutputDirectoryLabel by viewModel.customOutputDirectoryLabel.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var infoDialog by rememberSaveable { mutableStateOf<InfoDialogKind?>(null) }

    val pickDirectoryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.setCustomOutputDirectory(uri)
    }

    val versionLabel = stringResource(R.string.settings_version)
    val languageLabel = stringResource(R.string.settings_language)
    val themeLabel = stringResource(R.string.settings_theme)
    val feedbackHeader = stringResource(R.string.settings_feedback_header)
    val feedbackLogs = stringResource(R.string.settings_feedback_logs)
    val emptyLogsText = stringResource(R.string.settings_logs_empty)
    val selectedLanguageLabel = stringResource(languageLabelRes(settings.language))
    val selectedThemeLabel = stringResource(themeModeLabelRes(settings.themeMode))

    LaunchedEffect(viewModel) {
        viewModel.events.collect { messageRes ->
            Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            modifier = Modifier.fillMaxWidth(),
            // 外层 Scaffold（无 topBar）已经把状态栏内边距留在了 NavHost 的 innerPadding 里，同 HistoryScreen。
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        SettingsContent(
            modifier = Modifier.weight(1f),
            settings = settings,
            customOutputDirectoryLabel = customOutputDirectoryLabel,
            cacheBytes = cacheBytes,
            appVersion = viewModel.appVersion,
            onThemeModeChange = viewModel::setThemeMode,
            onAccentColorChange = viewModel::setAccentColor,
            onDynamicColorChange = viewModel::setDynamicColor,
            onLanguageChange = viewModel::setLanguage,
            onDefaultImageQualityChange = viewModel::setDefaultImageQuality,
            onDefaultVideoQualityChange = viewModel::setDefaultVideoQuality,
            onDefaultAudioQualityChange = viewModel::setDefaultAudioQuality,
            onPreserveImageMetadataChange = viewModel::setPreserveImageMetadata,
            onPickOutputDirectory = { pickDirectoryLauncher.launch(null) },
            onResetOutputDirectory = { viewModel.setCustomOutputDirectory(null) },
            onNameCollisionStrategyChange = viewModel::setNameCollisionStrategy,
            onScrollFileNamesChange = viewModel::setScrollFileNames,
            onCompletionNotificationChange = viewModel::setShowCompletionNotification,
            onAutoCleanupTempFilesChange = viewModel::setAutoCleanupTempFiles,
            onClearCache = viewModel::clearCache,
            onShowInfoDialog = { infoDialog = it },
            onShowLogs = {
                viewModel.refreshLogs()
                infoDialog = InfoDialogKind.LOGS
            },
            onShareFeedback = {
                viewModel.refreshLogs()
                val payload = buildFeedbackPayload(
                    versionLabel = versionLabel,
                    appVersion = viewModel.appVersion,
                    languageLabel = languageLabel,
                    selectedLanguageLabel = selectedLanguageLabel,
                    themeLabel = themeLabel,
                    selectedThemeLabel = selectedThemeLabel,
                    feedbackHeader = feedbackHeader,
                    feedbackLogs = feedbackLogs,
                    emptyLogsText = emptyLogsText,
                    logs = logs,
                )
                val shareIntent = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, payload)
                context.startActivity(Intent.createChooser(shareIntent, null))
            },
        )
    }

    SettingsInfoDialog(
        kind = infoDialog,
        logs = logs,
        onDismiss = { infoDialog = null },
    )
}

private fun buildFeedbackPayload(
    versionLabel: String,
    appVersion: String,
    languageLabel: String,
    selectedLanguageLabel: String,
    themeLabel: String,
    selectedThemeLabel: String,
    feedbackHeader: String,
    feedbackLogs: String,
    emptyLogsText: String,
    logs: List<String>,
): String = buildString {
    appendLine(feedbackHeader)
    appendLine("$versionLabel: $appVersion")
    appendLine("$languageLabel: $selectedLanguageLabel")
    appendLine("$themeLabel: $selectedThemeLabel")
    appendLine()
    appendLine(feedbackLogs)
    append(
        if (logs.isEmpty()) {
            emptyLogsText
        } else {
            logs.takeLast(40).joinToString(separator = "\n")
        },
    )
}
