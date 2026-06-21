package com.henjicc.swiftformat.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.feature.common.errorKindLabelRes
import com.henjicc.swiftformat.feature.common.qualityLabel
import com.henjicc.swiftformat.feature.common.sizeLabel
import com.henjicc.swiftformat.feature.common.statusLabelRes

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
) {
    var showFailureDetails by rememberSaveable(item.id) { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            HistoryRecordSummary(
                item = item,
                sizeFormatter = sizeFormatter,
                timeFormatter = timeFormatter,
                onShowFailureDetails = { showFailureDetails = true },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            HistoryRecordActions(
                item = item,
                onOpen = onOpen,
                onShare = onShare,
                onShowInFolder = onShowInFolder,
                onDeleteOutput = onDeleteOutput,
                onDeleteRecord = onDeleteRecord,
                onConvertAgain = onConvertAgain,
                onOpenProgress = onOpenProgress,
            )
        }
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

@Composable
private fun HistoryRecordSummary(
    item: HistoryUiItem,
    sizeFormatter: (Long) -> String,
    timeFormatter: (Long) -> String,
    onShowFailureDetails: () -> Unit,
) {
    Text(
        text = item.displayName,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = stringResource(
            R.string.progress_format_transition,
            item.originalFormat?.uppercase() ?: "?",
            item.outputFormat,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    Text(
        text = stringResource(statusLabelRes(item.status)),
        style = MaterialTheme.typography.labelLarge,
        color = statusColor(item.status),
        modifier = Modifier.padding(top = 8.dp),
    )
    Text(
        text = timeFormatter(item.endTime ?: item.startTime),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    item.outputSizeBytes?.let { sizeBytes ->
        Text(
            text = sizeFormatter(sizeBytes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
    if (item.quality != null || item.size != null) {
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item.quality?.let { quality ->
                Text(
                    text = qualityLabel(quality),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item.size?.let { size ->
                Text(
                    text = sizeLabel(size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    if (item.failureKind != null) {
        Text(
            text = stringResource(errorKindLabelRes(item.failureKind)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (!item.failureDetails.isNullOrBlank()) {
            TextButton(onClick = onShowFailureDetails) {
                Text(stringResource(R.string.action_view_details))
            }
        }
    }
}

@Composable
private fun HistoryRecordActions(
    item: HistoryUiItem,
    onOpen: (android.net.Uri) -> Unit,
    onShare: (android.net.Uri) -> Unit,
    onShowInFolder: () -> Unit,
    onDeleteOutput: (Long) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onConvertAgain: (Long) -> Unit,
    onOpenProgress: () -> Unit,
) {
    if (item.isActive) {
        Button(onClick = onOpenProgress) {
            Text(stringResource(R.string.history_open_progress))
        }
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item.outputUri?.let { outputUri ->
            TextButton(onClick = { onOpen(outputUri) }) {
                Text(stringResource(R.string.action_open))
            }
            TextButton(onClick = { onShare(outputUri) }) {
                Text(stringResource(R.string.action_share))
            }
            TextButton(onClick = onShowInFolder) {
                Text(stringResource(R.string.action_show_in_folder))
            }
            TextButton(onClick = { onDeleteOutput(item.id) }) {
                Text(stringResource(R.string.action_delete_result))
            }
        }
        if (item.canConvertAgain) {
            TextButton(onClick = { onConvertAgain(item.id) }) {
                Text(stringResource(R.string.action_convert_again))
            }
        }
        TextButton(onClick = { onDeleteRecord(item.id) }) {
            Text(stringResource(R.string.history_delete_record))
        }
    }
}

@Composable
private fun statusColor(status: ConversionStatus) = when (status) {
    ConversionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    ConversionStatus.FAILED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
