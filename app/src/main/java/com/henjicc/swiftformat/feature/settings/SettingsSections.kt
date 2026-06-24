package com.henjicc.swiftformat.feature.settings

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.designsystem.accentSwatchColor
import com.henjicc.swiftformat.core.designsystem.resolveDarkTheme
import com.henjicc.swiftformat.core.model.AccentColor
import com.henjicc.swiftformat.core.model.AppLanguage
import com.henjicc.swiftformat.core.model.AppSettings
import com.henjicc.swiftformat.core.model.NameCollisionStrategy
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsContent(
    settings: AppSettings,
    customOutputDirectoryLabel: String?,
    cacheBytes: Long,
    appVersion: String,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onDefaultImageQualityChange: (QualityPreset) -> Unit,
    onDefaultVideoQualityChange: (QualityPreset) -> Unit,
    onDefaultAudioQualityChange: (QualityPreset) -> Unit,
    onPreserveImageMetadataChange: (Boolean) -> Unit,
    onPickOutputDirectory: () -> Unit,
    onResetOutputDirectory: () -> Unit,
    onNameCollisionStrategyChange: (NameCollisionStrategy) -> Unit,
    onScrollFileNamesChange: (Boolean) -> Unit,
    onCompletionNotificationChange: (Boolean) -> Unit,
    onAutoCleanupTempFilesChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onShowInfoDialog: (InfoDialogKind) -> Unit,
    onShowLogs: () -> Unit,
    onShareFeedback: () -> Unit,
) {
    val context = LocalContext.current

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

        AppearanceSection(
            settings = settings,
            onThemeModeChange = onThemeModeChange,
            onAccentColorChange = onAccentColorChange,
            onDynamicColorChange = onDynamicColorChange,
            onLanguageChange = onLanguageChange,
        )
        ConversionDefaultsSection(
            settings = settings,
            onDefaultImageQualityChange = onDefaultImageQualityChange,
            onDefaultVideoQualityChange = onDefaultVideoQualityChange,
            onDefaultAudioQualityChange = onDefaultAudioQualityChange,
            onPreserveImageMetadataChange = onPreserveImageMetadataChange,
        )
        FileSettingsSection(
            settings = settings,
            customOutputDirectoryLabel = customOutputDirectoryLabel,
            cacheBytes = cacheBytes,
            formattedCacheSize = Formatter.formatShortFileSize(context, cacheBytes),
            onPickOutputDirectory = onPickOutputDirectory,
            onResetOutputDirectory = onResetOutputDirectory,
            onNameCollisionStrategyChange = onNameCollisionStrategyChange,
            onScrollFileNamesChange = onScrollFileNamesChange,
            onCompletionNotificationChange = onCompletionNotificationChange,
            onAutoCleanupTempFilesChange = onAutoCleanupTempFilesChange,
            onClearCache = onClearCache,
        )
        AboutSection(
            appVersion = appVersion,
            onShowPrivacy = { onShowInfoDialog(InfoDialogKind.PRIVACY) },
            onShowOpenSource = { onShowInfoDialog(InfoDialogKind.OPEN_SOURCE) },
            onShowLogs = onShowLogs,
            onShareFeedback = onShareFeedback,
        )
    }
}

@Composable
private fun AppearanceSection(
    settings: AppSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_appearance))

    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall)
    ChipRow(
        options = ThemeMode.entries,
        selected = settings.themeMode,
        label = { stringResource(themeModeLabelRes(it)) },
        onSelect = onThemeModeChange,
    )

    Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.titleSmall)
    val isDarkTheme = resolveDarkTheme(settings.themeMode)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AccentColor.entries.forEach { accent ->
            AccentSwatch(
                color = accentSwatchColor(accent, dark = isDarkTheme),
                contentDescription = stringResource(accentLabelRes(accent)),
                selected = accent == settings.accentColor,
                onClick = { onAccentColorChange(accent) },
            )
        }
    }

    ToggleRow(
        title = stringResource(R.string.settings_dynamic_color),
        description = stringResource(R.string.settings_dynamic_color_desc),
        checked = settings.dynamicColor,
        onCheckedChange = onDynamicColorChange,
    )

    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
    ChipRow(
        options = AppLanguage.entries,
        selected = settings.language,
        label = { stringResource(languageLabelRes(it)) },
        onSelect = onLanguageChange,
    )
}

@Composable
private fun ConversionDefaultsSection(
    settings: AppSettings,
    onDefaultImageQualityChange: (QualityPreset) -> Unit,
    onDefaultVideoQualityChange: (QualityPreset) -> Unit,
    onDefaultAudioQualityChange: (QualityPreset) -> Unit,
    onPreserveImageMetadataChange: (Boolean) -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_conversion_defaults))

    Text(stringResource(R.string.settings_default_image_quality), style = MaterialTheme.typography.titleSmall)
    ChipRow(
        options = QualityPreset.entries,
        selected = settings.defaultImageQuality,
        label = { stringResource(qualityLabelRes(it)) },
        onSelect = onDefaultImageQualityChange,
    )

    Text(stringResource(R.string.settings_default_video_quality), style = MaterialTheme.typography.titleSmall)
    ChipRow(
        options = QualityPreset.entries,
        selected = settings.defaultVideoQuality,
        label = { stringResource(qualityLabelRes(it)) },
        onSelect = onDefaultVideoQualityChange,
    )

    Text(stringResource(R.string.settings_default_audio_quality), style = MaterialTheme.typography.titleSmall)
    ChipRow(
        options = QualityPreset.entries,
        selected = settings.defaultAudioQuality,
        label = { stringResource(qualityLabelRes(it)) },
        onSelect = onDefaultAudioQualityChange,
    )

    ToggleRow(
        title = stringResource(R.string.settings_preserve_metadata),
        description = stringResource(R.string.settings_preserve_metadata_desc),
        checked = settings.preserveImageMetadata,
        onCheckedChange = onPreserveImageMetadataChange,
    )
}

@Composable
private fun FileSettingsSection(
    settings: AppSettings,
    customOutputDirectoryLabel: String?,
    cacheBytes: Long,
    formattedCacheSize: String,
    onPickOutputDirectory: () -> Unit,
    onResetOutputDirectory: () -> Unit,
    onNameCollisionStrategyChange: (NameCollisionStrategy) -> Unit,
    onScrollFileNamesChange: (Boolean) -> Unit,
    onCompletionNotificationChange: (Boolean) -> Unit,
    onAutoCleanupTempFilesChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_files))

    val hasCustomDirectory = settings.customOutputDirectoryUri != null
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingSummary(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.settings_save_location),
            value = if (hasCustomDirectory) {
                customOutputDirectoryLabel ?: stringResource(R.string.settings_save_location_custom_unknown)
            } else {
                stringResource(R.string.settings_save_location_value)
            },
        )
        if (hasCustomDirectory) {
            TextButton(onClick = onResetOutputDirectory) {
                Text(stringResource(R.string.settings_reset_directory))
            }
        }
        TextButton(onClick = onPickOutputDirectory) {
            Text(stringResource(R.string.settings_choose_directory))
        }
    }

    Text(stringResource(R.string.settings_name_collision), style = MaterialTheme.typography.titleSmall)
    ChipRow(
        options = NameCollisionStrategy.entries,
        selected = settings.nameCollisionStrategy,
        label = { stringResource(nameCollisionStrategyLabelRes(it)) },
        onSelect = onNameCollisionStrategyChange,
    )

    ToggleRow(
        title = stringResource(R.string.settings_scroll_file_names),
        description = stringResource(R.string.settings_scroll_file_names_desc),
        checked = settings.scrollFileNames,
        onCheckedChange = onScrollFileNamesChange,
    )
    ToggleRow(
        title = stringResource(R.string.settings_completion_notification),
        description = stringResource(R.string.settings_completion_notification_desc),
        checked = settings.showCompletionNotification,
        onCheckedChange = onCompletionNotificationChange,
    )
    ToggleRow(
        title = stringResource(R.string.settings_auto_cleanup),
        description = stringResource(R.string.settings_auto_cleanup_desc),
        checked = settings.autoCleanupTempFiles,
        onCheckedChange = onAutoCleanupTempFilesChange,
    )
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
                formattedCacheSize,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onClearCache, enabled = cacheBytes >= 0L) {
            Text(stringResource(R.string.settings_clear_cache))
        }
    }
}

@Composable
private fun AboutSection(
    appVersion: String,
    onShowPrivacy: () -> Unit,
    onShowOpenSource: () -> Unit,
    onShowLogs: () -> Unit,
    onShareFeedback: () -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_about))
    Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.titleSmall)
    Text(
        text = appVersion,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(onClick = onShowPrivacy) {
        Text(stringResource(R.string.settings_privacy))
    }
    TextButton(onClick = onShowOpenSource) {
        Text(stringResource(R.string.settings_open_source))
    }
    TextButton(onClick = onShowLogs) {
        Text(stringResource(R.string.settings_view_logs))
    }
    TextButton(onClick = onShareFeedback) {
        Text(stringResource(R.string.settings_feedback))
    }
}
