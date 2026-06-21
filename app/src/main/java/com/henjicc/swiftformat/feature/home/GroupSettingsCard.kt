package com.henjicc.swiftformat.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.GroupConversionSettings
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputFormatCatalog
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import com.henjicc.swiftformat.feature.common.qualityLabel
import com.henjicc.swiftformat.feature.common.sizeLabel

/** 折叠状态下每组默认展示的文件数（见 SPEC 4.4「默认只展示前几个」）。 */
private const val COLLAPSED_VISIBLE_COUNT = 3

/** 单个媒体分组卡片：分组头 + 统一参数行（格式/质量/尺寸） + 折叠文件列表（见 SPEC 4.3）。 */
@Composable
internal fun GroupCard(
    mediaType: MediaType,
    files: List<InputFile>,
    settings: GroupConversionSettings,
    onFormatChange: (String) -> Unit,
    onQualityChange: (QualityPreset) -> Unit,
    onSizeChange: (SizePreset) -> Unit,
    onRemove: (String) -> Unit,
    sizeFormatter: (Long) -> String,
    imageLoader: ImageLoader,
) {
    var activeSheet by rememberSaveable { mutableStateOf<SheetKind?>(null) }
    var expanded by rememberSaveable(mediaType) { mutableStateOf(false) }

    val sizeOptions = OutputFormatCatalog.sizePresets(mediaType)
    val showQuality = OutputFormatCatalog.isQualityApplicable(settings.outputFormat)
    val showSize = sizeOptions.isNotEmpty()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            GroupHeader(stringResource(groupLabel(mediaType)), files.size)

            SettingRow(
                label = stringResource(R.string.row_output_format),
                value = settings.outputFormat,
                onClick = { activeSheet = SheetKind.FORMAT },
            )
            if (showQuality) {
                SettingRow(
                    label = stringResource(R.string.row_quality),
                    value = qualityLabel(settings.quality ?: QualityPreset.HIGH),
                    onClick = { activeSheet = SheetKind.QUALITY },
                )
            }
            if (showSize) {
                SettingRow(
                    label = stringResource(R.string.row_size),
                    value = sizeLabel(settings.size ?: SizePreset.Original),
                    onClick = { activeSheet = SheetKind.SIZE },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val visibleFiles = if (expanded) files else files.take(COLLAPSED_VISIBLE_COUNT)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleFiles.forEach { file ->
                    FileRow(file, sizeFormatter, onRemove, imageLoader)
                }
            }
            if (files.size > COLLAPSED_VISIBLE_COUNT) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        if (expanded) {
                            stringResource(R.string.files_collapse)
                        } else {
                            stringResource(R.string.files_show_all, files.size)
                        },
                    )
                }
            }
        }
    }

    when (activeSheet) {
        SheetKind.FORMAT -> OptionsBottomSheet(
            options = OutputFormatCatalog.outputFormats(mediaType),
            optionLabel = { it },
            isSelected = { it == settings.outputFormat },
            onSelect = onFormatChange,
            onDismiss = { activeSheet = null },
        )

        SheetKind.QUALITY -> OptionsBottomSheet(
            options = QualityPreset.entries,
            optionLabel = { qualityLabel(it) },
            isSelected = { it == settings.quality },
            onSelect = onQualityChange,
            onDismiss = { activeSheet = null },
        )

        SheetKind.SIZE -> OptionsBottomSheet(
            options = sizeOptions,
            optionLabel = { sizeLabel(it) },
            isSelected = { it == settings.size },
            onSelect = onSizeChange,
            onDismiss = { activeSheet = null },
        )

        null -> Unit
    }
}

private enum class SheetKind { FORMAT, QUALITY, SIZE }

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionsBottomSheet(
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn {
            items(options) { option ->
                val selected = isSelected(option)
                ListItem(
                    headlineContent = { Text(optionLabel(option)) },
                    trailingContent = {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    modifier = Modifier.clickable {
                        onSelect(option)
                        onDismiss()
                    },
                )
            }
        }
    }
}
