package com.henjicc.swiftformat.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
    scrollFileNames: Boolean,
) {
    var activeSheet by rememberSaveable { mutableStateOf<SheetKind?>(null) }
    var expanded by rememberSaveable(mediaType) { mutableStateOf(false) }

    val sizeOptions = OutputFormatCatalog.sizePresets(mediaType)
    val showQuality = OutputFormatCatalog.isQualityApplicable(mediaType, settings.outputFormat)
    val showSize = OutputFormatCatalog.isSizeApplicable(mediaType, settings.outputFormat) && sizeOptions.isNotEmpty()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            GroupHeader(stringResource(groupLabel(mediaType)), files.size)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showSize) {
                    DropdownSettingChip(
                        label = stringResource(R.string.row_size),
                        value = sizeLabel(settings.size ?: SizePreset.Original),
                        onClick = { activeSheet = SheetKind.SIZE },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (showQuality) {
                    DropdownSettingChip(
                        label = stringResource(R.string.row_quality),
                        value = qualityLabel(settings.quality ?: QualityPreset.STANDARD),
                        onClick = { activeSheet = SheetKind.QUALITY },
                        modifier = Modifier.weight(1f),
                    )
                }
                DropdownSettingChip(
                    label = stringResource(R.string.row_output_format),
                    value = settings.outputFormat,
                    onClick = { activeSheet = SheetKind.FORMAT },
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            val visibleFiles = if (expanded) files else files.take(COLLAPSED_VISIBLE_COUNT)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                visibleFiles.forEach { file ->
                    FileRow(
                        file = file,
                        sizeFormatter = sizeFormatter,
                        onRemove = onRemove,
                        imageLoader = imageLoader,
                        compact = true,
                        scrollFileNames = scrollFileNames,
                    )
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
            options = OutputFormatCatalog.outputOptions(mediaType),
            optionLabel = { it.format },
            isSelected = { it.format == settings.outputFormat },
            onSelect = { onFormatChange(it.format) },
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

/** 尺寸/质量/格式三个参数并排展示的下拉框样式：上方小标签 + 下方带边框的"当前值 + ▾"。 */
@Composable
private fun DropdownSettingChip(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
        )
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
