package com.henjicc.swiftformat.feature.progress

import android.widget.Toast
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
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.feature.common.errorKindLabelRes
import com.henjicc.swiftformat.feature.common.statusLabelRes
import com.henjicc.swiftformat.feature.home.mediaIcon

@Composable
internal fun ProgressHeader(state: ConversionProgressUiState, onCancelAll: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.progress_summary, state.completed, state.total),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.hasActiveTasks) {
                TextButton(onClick = onCancelAll) { Text(stringResource(R.string.progress_cancel_all)) }
            }
        }
        ConversionLinearProgressIndicator(
            progress = { state.overallFraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        state.currentItem?.let { current ->
            Text(
                text = "${current.displayName} · ${stringResource(statusLabelRes(current.status))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (!state.hasActiveTasks && state.total > 0) {
            Text(
                text = stringResource(R.string.progress_breakdown, state.completed, state.failed, state.cancelled),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
internal fun ConversionTaskRow(
    item: ConversionTaskUiItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onConvertAgain: () -> Unit,
    onOpen: (android.net.Uri) -> Unit,
    onShare: (android.net.Uri) -> Unit,
    onShowInFolder: () -> Unit,
    onDeleteUri: (android.net.Uri) -> Boolean,
) {
    var outputDeleted by rememberSaveable(item.taskId) { mutableStateOf(false) }
    var originalDeleted by rememberSaveable(item.taskId) { mutableStateOf(false) }
    var confirmDeleteOriginal by rememberSaveable(item.taskId) { mutableStateOf(false) }
    var showFailureDetails by rememberSaveable(item.taskId) { mutableStateOf(false) }

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                Text(
                    text = stringResource(
                        R.string.progress_format_transition,
                        item.originalFormat?.uppercase() ?: "?",
                        item.outputFormat,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (item.isActive) {
                    ConversionLinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
                if (item.status == ConversionStatus.FAILED && item.failureKind != null) {
                    Text(
                        text = stringResource(errorKindLabelRes(item.failureKind)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    if (item.hasFailureDetails) {
                        TextButton(
                            onClick = { showFailureDetails = true },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(stringResource(R.string.action_view_details), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.size(4.dp))
            TaskActionRow(
                item = item,
                outputDeleted = outputDeleted,
                originalDeleted = originalDeleted,
                onCancel = onCancel,
                onRetry = onRetry,
                onConvertAgain = onConvertAgain,
                onShare = onShare,
                onShowInFolder = onShowInFolder,
                onDeleteOutput = { outputUri ->
                    if (onDeleteUri(outputUri)) outputDeleted = true
                },
                onRequestDeleteOriginal = { confirmDeleteOriginal = true },
            )
        }
    }

    // 已完成且结果还没被删除时，整行点击直接打开，跟历史记录卡片的交互保持一致。
    val outputUri = item.outputUri
    if (item.status == ConversionStatus.COMPLETED && outputUri != null && !outputDeleted) {
        Card(onClick = { onOpen(outputUri) }, modifier = Modifier.fillMaxWidth()) { rowContent() }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) { rowContent() }
    }

    if (confirmDeleteOriginal) {
        AlertDialog(
            onDismissRequest = { confirmDeleteOriginal = false },
            title = { Text(stringResource(R.string.delete_original_title)) },
            text = { Text(stringResource(R.string.delete_original_message, item.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onDeleteUri(item.inputUri)) {
                            originalDeleted = true
                        }
                        confirmDeleteOriginal = false
                    },
                ) {
                    Text(stringResource(R.string.action_delete_original))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteOriginal = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showFailureDetails && item.failureDetails != null) {
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        val copiedMessage = stringResource(R.string.error_details_copied)
        AlertDialog(
            onDismissRequest = { showFailureDetails = false },
            title = { Text(stringResource(R.string.error_details_title)) },
            text = { Text(item.failureDetails) },
            confirmButton = {
                TextButton(onClick = { showFailureDetails = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(item.failureDetails))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.action_copy))
                }
            },
        )
    }
}

/**
 * 行尾操作：进行中只显示"取消"，失败只显示"重试"，已完成才有"更多"溢出菜单——
 * 跟历史记录卡片一样，"打开"已经交给整行点击，不在菜单里重复出现。
 */
@Composable
private fun TaskActionRow(
    item: ConversionTaskUiItem,
    outputDeleted: Boolean,
    originalDeleted: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onConvertAgain: () -> Unit,
    onShare: (android.net.Uri) -> Unit,
    onShowInFolder: () -> Unit,
    onDeleteOutput: (android.net.Uri) -> Unit,
    onRequestDeleteOriginal: () -> Unit,
) {
    if (item.isActive) {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        return
    }
    if (item.canRetry) {
        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        return
    }
    if (item.status != ConversionStatus.COMPLETED) return

    var menuExpanded by rememberSaveable(item.taskId) { mutableStateOf(false) }
    val outputUriForActions = item.outputUri?.takeIf { !outputDeleted }

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            outputUriForActions?.let { outputUri ->
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
                    onClick = { menuExpanded = false; onDeleteOutput(outputUri) },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_convert_again)) },
                onClick = { menuExpanded = false; onConvertAgain() },
            )
            if (!originalDeleted) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete_original)) },
                    onClick = { menuExpanded = false; onRequestDeleteOriginal() },
                )
            }
        }
    }
}

@Composable
private fun statusColor(status: ConversionStatus) = when (status) {
    ConversionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    ConversionStatus.FAILED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun ConversionLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        drawStopIndicator = {},
    )
}
