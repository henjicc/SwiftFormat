package com.henjicc.swiftformat.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.AccentColor
import com.henjicc.swiftformat.core.model.AppLanguage
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.ThemeMode

@Composable
internal fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
internal fun SettingSummary(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChipRow(
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
internal fun AccentSwatch(
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

internal fun themeModeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

internal fun languageLabelRes(language: AppLanguage): Int = when (language) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.CHINESE -> R.string.language_chinese
    AppLanguage.ENGLISH -> R.string.language_english
}

internal fun accentLabelRes(accent: AccentColor): Int = when (accent) {
    AccentColor.BLUE -> R.string.accent_blue
    AccentColor.CYAN -> R.string.accent_cyan
    AccentColor.GREEN -> R.string.accent_green
    AccentColor.PURPLE -> R.string.accent_purple
    AccentColor.PINK -> R.string.accent_pink
    AccentColor.ORANGE -> R.string.accent_orange
    AccentColor.RED -> R.string.accent_red
}

internal fun qualityLabelRes(preset: QualityPreset): Int = when (preset) {
    QualityPreset.BEST -> R.string.quality_best
    QualityPreset.HIGH -> R.string.quality_high
    QualityPreset.STANDARD -> R.string.quality_standard
    QualityPreset.SMALL_SIZE -> R.string.quality_small_size
}
