package com.henjicc.swiftformat.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.feature.common.errorKindLabelRes
import com.henjicc.swiftformat.feature.common.statusLabelRes

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
        LinearProgressIndicator(
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.progress_format_transition,
                            item.originalFormat?.uppercase() ?: "?",
                            item.outputFormat,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(statusLabelRes(item.status)),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(item.status),
                )
            }
            if (item.isActive) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            if (item.status == ConversionStatus.FAILED && item.failureKind != null) {
                Text(
                    text = stringResource(errorKindLabelRes(item.failureKind)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (item.hasFailureDetails) {
                    TextButton(onClick = { showFailureDetails = true }) {
                        Text(stringResource(R.string.action_view_details))
                    }
                }
            }
            TaskActionRow(
                item = item,
                outputDeleted = outputDeleted,
                originalDeleted = originalDeleted,
                onCancel = onCancel,
                onRetry = onRetry,
                onConvertAgain = onConvertAgain,
                onOpen = onOpen,
                onShare = onShare,
                onShowInFolder = onShowInFolder,
                onDeleteOutput = { outputUri ->
                    if (onDeleteUri(outputUri)) outputDeleted = true
                },
                onRequestDeleteOriginal = { confirmDeleteOriginal = true },
            )
        }
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

@Composable
private fun TaskActionRow(
    item: ConversionTaskUiItem,
    outputDeleted: Boolean,
    originalDeleted: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onConvertAgain: () -> Unit,
    onOpen: (android.net.Uri) -> Unit,
    onShare: (android.net.Uri) -> Unit,
    onShowInFolder: () -> Unit,
    onDeleteOutput: (android.net.Uri) -> Unit,
    onRequestDeleteOriginal: () -> Unit,
) {
    if (item.isActive || item.canRetry || item.canConvertAgain) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (item.isActive) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            }
            if (item.canRetry) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
            if (item.canConvertAgain) {
                TextButton(onClick = onConvertAgain) { Text(stringResource(R.string.action_convert_again)) }
            }
        }
    }

    if (item.status != ConversionStatus.COMPLETED) return

    var menuExpanded by rememberSaveable(item.taskId) { mutableStateOf(false) }
    val outputUri = item.outputUri
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (outputUri != null && !outputDeleted) {
            IconButton(onClick = { onOpen(outputUri) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.action_open))
            }
            IconButton(onClick = { onShare(outputUri) }) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
            }
        }
        if (outputUri != null && !outputDeleted || !originalDeleted) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (outputUri != null && !outputDeleted) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_show_in_folder)) },
                            onClick = { menuExpanded = false; onShowInFolder() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete_result)) },
                            onClick = { menuExpanded = false; onDeleteOutput(outputUri) },
                        )
                    }
                    if (!originalDeleted) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete_original)) },
                            onClick = { menuExpanded = false; onRequestDeleteOriginal() },
                        )
                    }
                }
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
