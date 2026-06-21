package com.henjicc.swiftformat.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputFormatCatalog
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onPick: () -> Unit,
    onAddMore: () -> Unit,
    onClear: () -> Unit,
    onRemove: (String) -> Unit,
    onFormatChange: (MediaType, String) -> Unit,
    onQualityChange: (MediaType, QualityPreset) -> Unit,
    onSizeChange: (MediaType, SizePreset) -> Unit,
    onStartConversion: () -> Unit,
    onOpenActiveTask: () -> Unit,
    sizeFormatter: (Long) -> String,
    imageLoader: ImageLoader,
) {
    if (!state.hasFiles) {
        EmptyState(
            state = state,
            onPick = onPick,
            onOpenActiveTask = onOpenActiveTask,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    FileList(
        state = state,
        onAddMore = onAddMore,
        onClear = onClear,
        onRemove = onRemove,
        onFormatChange = onFormatChange,
        onQualityChange = onQualityChange,
        onSizeChange = onSizeChange,
        onStartConversion = onStartConversion,
        onOpenActiveTask = onOpenActiveTask,
        sizeFormatter = sizeFormatter,
        imageLoader = imageLoader,
    )
}

@Composable
private fun EmptyState(
    state: HomeUiState,
    onPick: () -> Unit,
    onOpenActiveTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.hasActiveTasks) {
            ActiveTaskCard(
                state = state,
                onOpen = onOpenActiveTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
        }
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onPick, modifier = Modifier.padding(top = 24.dp)) {
            androidx.compose.material3.Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.home_select_files))
        }
    }
}

@Composable
private fun FileList(
    state: HomeUiState,
    onAddMore: () -> Unit,
    onClear: () -> Unit,
    onRemove: (String) -> Unit,
    onFormatChange: (MediaType, String) -> Unit,
    onQualityChange: (MediaType, QualityPreset) -> Unit,
    onSizeChange: (MediaType, SizePreset) -> Unit,
    onStartConversion: () -> Unit,
    onOpenActiveTask: () -> Unit,
    sizeFormatter: (Long) -> String,
    imageLoader: ImageLoader,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (state.hasActiveTasks) {
                    ActiveTaskCard(
                        state = state,
                        onOpen = onOpenActiveTask,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_summary,
                            state.totalCount,
                            sizeFormatter(state.totalSizeBytes),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onClear) { Text(stringResource(R.string.home_clear)) }
                    Spacer(Modifier.size(8.dp))
                    Button(onClick = onAddMore) { Text(stringResource(R.string.home_add_more)) }
                }
            }

            state.groups.forEach { (type, files) ->
                item(key = "group-$type") {
                    val settings = state.settings[type] ?: OutputFormatCatalog.defaultSettings(type)
                    GroupCard(
                        mediaType = type,
                        files = files,
                        settings = settings,
                        onFormatChange = { onFormatChange(type, it) },
                        onQualityChange = { onQualityChange(type, it) },
                        onSizeChange = { onSizeChange(type, it) },
                        onRemove = onRemove,
                        sizeFormatter = sizeFormatter,
                        imageLoader = imageLoader,
                    )
                }
            }

            if (state.unsupported.isNotEmpty()) {
                item(key = "header-unsupported") {
                    GroupHeader(stringResource(R.string.unsupported_title), state.unsupported.size)
                }
                items(state.unsupported, key = { it.id }) { file ->
                    FileRow(file, sizeFormatter, onRemove, imageLoader, unsupported = true)
                }
            }
        }

        Button(
            onClick = onStartConversion,
            enabled = state.groups.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.convert_start))
        }
    }
}

@Composable
private fun ActiveTaskCard(
    state: HomeUiState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.home_active_tasks_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.home_active_tasks_summary,
                    state.activeTaskSummary.completed,
                    state.activeTaskSummary.total,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            state.activeTaskDisplayName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Button(onClick = onOpen, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.home_active_tasks_open))
            }
        }
    }
}
