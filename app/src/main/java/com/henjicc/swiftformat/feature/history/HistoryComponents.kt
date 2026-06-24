package com.henjicc.swiftformat.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.feature.common.errorKindLabelRes
import com.henjicc.swiftformat.feature.common.qualityLabel
import com.henjicc.swiftformat.feature.common.sizeLabel
import com.henjicc.swiftformat.feature.common.statusLabelRes
import com.henjicc.swiftformat.feature.home.mediaIcon

@Composable
internal fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.history_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
internal fun ActiveTasksCard(activeCount: Int, onOpenProgress: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.history_active_title, activeCount),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.history_active_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(onClick = onOpenProgress, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.history_open_progress))
            }
        }
    }
}

/**
 * 单行列表项样式（图标 + 两行文字 + 行尾操作），与 [com.henjicc.swiftformat.feature.home.FileRow]
 * 保持一致的视觉语言。已完成是历史记录里最常见的状态，不再单独展示"已完成"文字占位——
 * 状态文字只用于需要提醒用户注意的失败/取消/进行中场景。
 */
@Composable
internal fun HistoryRecordCard(
    item: HistoryUiItem,
    sizeFormatter: (Long) -> String,
    timeFormatter: (Long) -> String,
    onOpen: (android.net.Uri) -> Unit,
    onShare: (android.net.Uri) -> Unit,
    onShowInFolder: () -> Unit,
    onDeleteOutput: (Long) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onConvertAgain: (Long) -> Unit,
    onOpenProgress: () -> Unit,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: (Long) -> Unit,
) {
    var showFailureDetails by rememberSaveable(item.id) { mutableStateOf(false) }

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelection(item.id) },
                )
                Spacer(Modifier.size(8.dp))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = mediaIcon(item.mediaType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                HistoryRecordSummary(
                    item = item,
                    sizeFormatter = sizeFormatter,
                    timeFormatter = timeFormatter,
                    onShowFailureDetails = { showFailureDetails = true },
                )
            }
            Spacer(Modifier.size(4.dp))
            if (selectionMode) {
                // Selection mode owns row actions; keep the target calm while users batch delete.
            } else if (item.isActive) {
                TextButton(onClick = onOpenProgress) {
                    Text(stringResource(R.string.history_open_progress))
                }
            } else {
                HistoryQuickActions(
                    item = item,
                    onShare = onShare,
                    onShowInFolder = onShowInFolder,
                    onDeleteOutput = onDeleteOutput,
                    onDeleteRecord = onDeleteRecord,
                    onConvertAgain = onConvertAgain,
                )
            }
        }
    }

    // 整行点击打开结果；长按进入多选。选择模式下，整行点击改为切换选中状态。
    val outputUri = item.outputUri
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .historyRecordClick(
                onClick = {
                    if (selectionMode) {
                        onToggleSelection(item.id)
                    } else {
                        outputUri?.let(onOpen)
                    }
                },
                onLongClick = { onToggleSelection(item.id) },
            ),
    ) {
        rowContent()
    }

    if (showFailureDetails && item.failureDetails != null) {
        AlertDialog(
            onDismissRequest = { showFailureDetails = false },
            title = { Text(stringResource(R.string.error_details_title)) },
            text = { Text(item.failureDetails) },
            confirmButton = {
                TextButton(onClick = { showFailureDetails = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.historyRecordClick(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = combinedClickable(
    onClick = onClick,
    onLongClick = onLongClick,
)

@Composable
private fun HistoryRecordSummary(
    item: HistoryUiItem,
    sizeFormatter: (Long) -> String,
    timeFormatter: (Long) -> String,
    onShowFailureDetails: () -> Unit,
) {
    Text(
        text = item.displayName,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    if (item.status != ConversionStatus.COMPLETED) {
        Text(
            text = stringResource(statusLabelRes(item.status)),
            style = MaterialTheme.typography.labelMedium,
            color = statusColor(item.status),
        )
    }
    val detailLine = buildString {
        append(
            stringResource(
                R.string.progress_format_transition,
                item.originalFormat?.uppercase() ?: "?",
                item.outputFormat,
            ),
        )
        append(" · ")
        append(timeFormatter(item.endTime ?: item.startTime))
        item.outputSizeBytes?.let { sizeBytes ->
            append(" · ")
            append(sizeFormatter(sizeBytes))
        }
        item.quality?.let { quality ->
            append(" · ")
            append(qualityLabel(quality))
        }
        item.size?.let { size ->
            append(" · ")
            append(sizeLabel(size))
        }
    }
    Text(
        text = detailLine,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.dp),
    )
    if (item.failureKind != null) {
        Text(
            text = stringResource(errorKindLabelRes(item.failureKind)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (!item.failureDetails.isNullOrBlank()) {
            TextButton(onClick = onShowFailureDetails, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.action_view_details), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * 行尾只留一个"更多"图标——点击整行已经能打开结果文件（最常见操作），其余低频操作全部收进菜单，
 * 把行高让给文件名。
 */
@Composable
private fun HistoryQuickActions(
    item: HistoryUiItem,
    onShare: (android.net.Uri) -> Unit,
    onShowInFolder: () -> Unit,
    onDeleteOutput: (Long) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onConvertAgain: (Long) -> Unit,
) {
    var menuExpanded by rememberSaveable(item.id) { mutableStateOf(false) }

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            item.outputUri?.let { outputUri ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_share)) },
                    onClick = { menuExpanded = false; onShare(outputUri) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_show_in_folder)) },
                    onClick = { menuExpanded = false; onShowInFolder() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete_result)) },
                    onClick = { menuExpanded = false; onDeleteOutput(item.id) },
                )
            }
            if (item.canConvertAgain) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_convert_again)) },
                    onClick = { menuExpanded = false; onConvertAgain(item.id) },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_delete_record)) },
                onClick = { menuExpanded = false; onDeleteRecord(item.id) },
            )
        }
    }
}

@Composable
private fun statusColor(status: ConversionStatus) = when (status) {
    ConversionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    ConversionStatus.FAILED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
