package com.henjicc.swiftformat.feature.progress

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.feature.common.statusLabelRes
import com.henjicc.swiftformat.service.ConversionForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionProgressScreen(
    onBack: () -> Unit,
    viewModel: ConversionProgressViewModel = viewModel(factory = ConversionProgressViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fileActions = remember(context) {
        (context.applicationContext as SwiftFormatApplication).container.resultFileActions
    }
    val showActionFailed = {
        Toast.makeText(context, R.string.file_action_failed, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.hasActiveTasks) R.string.progress_title_active else R.string.progress_title_done,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ProgressHeader(state = state, onCancelAll = viewModel::cancelAll)
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it.taskId }) { item ->
                    ConversionTaskRow(
                        item = item,
                        onCancel = { viewModel.cancel(item.taskId) },
                        onRetry = { viewModel.retry(item.taskId) },
                        onConvertAgain = {
                            if (viewModel.convertAgain(item.taskId) != null) {
                                ConversionForegroundService.start(context)
                            } else {
                                showActionFailed()
                            }
                        },
                        onOpen = { uri ->
                            if (!fileActions.open(uri)) showActionFailed()
                        },
                        onShare = { uri ->
                            if (!fileActions.share(uri)) showActionFailed()
                        },
                        onShowInFolder = {
                            if (!fileActions.showInFolder()) showActionFailed()
                        },
                        onDeleteUri = { uri ->
                            if (fileActions.delete(uri)) {
                                true
                            } else {
                                showActionFailed()
                                false
                            }
                        },
                    )
                }
            }
            if (state.items.isNotEmpty() && !state.hasActiveTasks) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(stringResource(R.string.progress_done))
                }
            }
        }
    }
}

@Composable
private fun ProgressHeader(state: ConversionProgressUiState, onCancelAll: () -> Unit) {
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
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
private fun ConversionTaskRow(
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            if (item.status == ConversionStatus.FAILED && item.failureReason != null) {
                Text(
                    text = item.failureReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
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
            if (item.status == ConversionStatus.COMPLETED) {
                val outputUri = item.outputUri
                if (outputUri != null && !outputDeleted) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(onClick = { onOpen(outputUri) }) {
                            Text(stringResource(R.string.action_open))
                        }
                        TextButton(onClick = { onShare(outputUri) }) {
                            Text(stringResource(R.string.action_share))
                        }
                        TextButton(onClick = onShowInFolder) {
                            Text(stringResource(R.string.action_show_in_folder))
                        }
                        TextButton(
                            onClick = {
                                if (onDeleteUri(outputUri)) outputDeleted = true
                            },
                        ) {
                            Text(stringResource(R.string.action_delete_result))
                        }
                    }
                }
                if (!originalDeleted) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(onClick = { confirmDeleteOriginal = true }) {
                            Text(stringResource(R.string.action_delete_original))
                        }
                    }
                }
            }
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
}

@Composable
private fun statusColor(status: ConversionStatus) = when (status) {
    ConversionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    ConversionStatus.FAILED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
