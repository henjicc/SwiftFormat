package com.henjicc.swiftformat.feature.settings

import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.designsystem.accentSwatchColor
import com.henjicc.swiftformat.core.model.AccentColor
import com.henjicc.swiftformat.core.model.AppLanguage
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var infoDialog by rememberSaveable { mutableStateOf<InfoDialogKind?>(null) }
    val versionLabel = stringResource(R.string.settings_version)
    val languageLabel = stringResource(R.string.settings_language)
    val themeLabel = stringResource(R.string.settings_theme)
    val feedbackHeader = stringResource(R.string.settings_feedback_header)
    val feedbackLogs = stringResource(R.string.settings_feedback_logs)
    val emptyLogsText = stringResource(R.string.settings_logs_empty)
    val selectedLanguageLabel = stringResource(languageLabel(settings.language))
    val selectedThemeLabel = stringResource(themeModeLabel(settings.themeMode))

    LaunchedEffect(viewModel) {
        viewModel.events.collect { messageRes ->
            Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        SectionHeader(stringResource(R.string.settings_appearance))

        // 主题模式
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
        ChipRow(
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { stringResource(themeModeLabel(it)) },
            onSelect = viewModel::setThemeMode,
        )

        // 强调色
        Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccentColor.entries.forEach { accent ->
                AccentSwatch(
                    color = accentSwatchColor(accent),
                    contentDescription = stringResource(accentLabel(accent)),
                    selected = accent == settings.accentColor,
                    onClick = { viewModel.setAccentColor(accent) },
                )
            }
        }

        // 系统动态配色
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_dynamic_color),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.settings_dynamic_color_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )
        }

        // 语言
        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
        ChipRow(
            options = AppLanguage.entries,
            selected = settings.language,
            label = { stringResource(languageLabel(it)) },
            onSelect = viewModel::setLanguage,
        )

        SectionHeader(stringResource(R.string.settings_conversion_defaults))
        Text(stringResource(R.string.settings_default_image_quality), style = MaterialTheme.typography.titleSmall)
        ChipRow(
            options = QualityPreset.entries,
            selected = settings.defaultImageQuality,
            label = { stringResource(qualityLabel(it)) },
            onSelect = viewModel::setDefaultImageQuality,
        )

        Text(stringResource(R.string.settings_default_video_quality), style = MaterialTheme.typography.titleSmall)
        ChipRow(
            options = QualityPreset.entries,
            selected = settings.defaultVideoQuality,
            label = { stringResource(qualityLabel(it)) },
            onSelect = viewModel::setDefaultVideoQuality,
        )

        Text(stringResource(R.string.settings_default_audio_quality), style = MaterialTheme.typography.titleSmall)
        ChipRow(
            options = QualityPreset.entries,
            selected = settings.defaultAudioQuality,
            label = { stringResource(qualityLabel(it)) },
            onSelect = viewModel::setDefaultAudioQuality,
        )

        SectionHeader(stringResource(R.string.settings_files))
        SettingSummary(
            title = stringResource(R.string.settings_save_location),
            value = stringResource(R.string.settings_save_location_value),
        )
        SettingSummary(
            title = stringResource(R.string.settings_name_collision),
            value = stringResource(R.string.settings_name_collision_value),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_completion_notification),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.settings_completion_notification_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = settings.showCompletionNotification,
                onCheckedChange = viewModel::setShowCompletionNotification,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_auto_cleanup),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.settings_auto_cleanup_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = settings.autoCleanupTempFiles,
                onCheckedChange = viewModel::setAutoCleanupTempFiles,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_cache_size),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    Formatter.formatShortFileSize(context, cacheBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = viewModel::clearCache) {
                Text(stringResource(R.string.settings_clear_cache))
            }
        }

        SectionHeader(stringResource(R.string.settings_about))
        Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.titleSmall)
        Text(
            text = viewModel.appVersion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { infoDialog = InfoDialogKind.PRIVACY }) {
            Text(stringResource(R.string.settings_privacy))
        }
        TextButton(onClick = { infoDialog = InfoDialogKind.OPEN_SOURCE }) {
            Text(stringResource(R.string.settings_open_source))
        }
        TextButton(
            onClick = {
                viewModel.refreshLogs()
                infoDialog = InfoDialogKind.LOGS
            },
        ) {
            Text(stringResource(R.string.settings_view_logs))
        }
        TextButton(
            onClick = {
                viewModel.refreshLogs()
                val payload = buildString {
                    appendLine(feedbackHeader)
                    appendLine("$versionLabel: ${viewModel.appVersion}")
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
                val shareIntent = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, payload)
                context.startActivity(Intent.createChooser(shareIntent, null))
            },
        ) {
            Text(stringResource(R.string.settings_feedback))
        }
    }

    if (infoDialog != null) {
        val kind = infoDialog ?: InfoDialogKind.PRIVACY
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { infoDialog = null },
            title = {
                Text(
                    stringResource(
                        when (kind) {
                            InfoDialogKind.PRIVACY -> R.string.settings_privacy
                            InfoDialogKind.OPEN_SOURCE -> R.string.settings_open_source
                            InfoDialogKind.LOGS -> R.string.settings_view_logs
                        },
                    ),
                )
            },
            text = {
                when (kind) {
                    InfoDialogKind.PRIVACY -> Text(stringResource(R.string.settings_privacy_content))
                    InfoDialogKind.OPEN_SOURCE -> Text(stringResource(R.string.settings_open_source_content))
                    InfoDialogKind.LOGS -> Text(
                        if (logs.isEmpty()) {
                            stringResource(R.string.settings_logs_empty)
                        } else {
                            logs.joinToString(separator = "\n")
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { infoDialog = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingSummary(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun AccentSwatch(
    color: Color,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor =
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .clip(CircleShape)
            .background(color)
            .border(width = if (selected) 3.dp else 1.dp, color = borderColor, shape = CircleShape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

private fun themeModeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun languageLabel(language: AppLanguage): Int = when (language) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.CHINESE -> R.string.language_chinese
    AppLanguage.ENGLISH -> R.string.language_english
}

private fun accentLabel(accent: AccentColor): Int = when (accent) {
    AccentColor.BLUE -> R.string.accent_blue
    AccentColor.CYAN -> R.string.accent_cyan
    AccentColor.GREEN -> R.string.accent_green
    AccentColor.PURPLE -> R.string.accent_purple
    AccentColor.PINK -> R.string.accent_pink
    AccentColor.ORANGE -> R.string.accent_orange
    AccentColor.RED -> R.string.accent_red
}

private fun qualityLabel(preset: QualityPreset): Int = when (preset) {
    QualityPreset.BEST -> R.string.quality_best
    QualityPreset.HIGH -> R.string.quality_high
    QualityPreset.STANDARD -> R.string.quality_standard
    QualityPreset.SMALL_SIZE -> R.string.quality_small_size
}

private enum class InfoDialogKind {
    PRIVACY,
    OPEN_SOURCE,
    LOGS,
}
