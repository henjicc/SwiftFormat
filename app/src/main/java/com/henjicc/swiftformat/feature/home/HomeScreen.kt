package com.henjicc.swiftformat.feature.home

import android.content.Intent
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            viewModel.addFiles(uris)
        }
    }
    val launchPicker = { picker.launch(arrayOf("image/*", "video/*", "audio/*")) }

    if (!state.hasFiles) {
        EmptyState(onPick = launchPicker, modifier = Modifier.fillMaxSize())
    } else {
        FileList(
            state = state,
            onAddMore = launchPicker,
            onClear = viewModel::clear,
            onRemove = viewModel::removeFile,
            sizeFormatter = { Formatter.formatShortFileSize(context, it) },
        )
    }
}

@Composable
private fun EmptyState(onPick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
            Icon(Icons.Filled.Add, contentDescription = null)
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
    sizeFormatter: (Long) -> String,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
            item(key = "header-$type") {
                GroupHeader(stringResource(groupLabel(type)), files.size)
            }
            items(files, key = { it.id }) { file ->
                FileRow(file, sizeFormatter, onRemove)
            }
        }

        if (state.unsupported.isNotEmpty()) {
            item(key = "header-unsupported") {
                GroupHeader(stringResource(R.string.unsupported_title), state.unsupported.size)
            }
            items(state.unsupported, key = { it.id }) { file ->
                FileRow(file, sizeFormatter, onRemove, unsupported = true)
            }
        }
    }
}

@Composable
private fun GroupHeader(label: String, count: Int) {
    Text(
        text = stringResource(R.string.group_count, label, count),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun FileRow(
    file: InputFile,
    sizeFormatter: (Long) -> String,
    onRemove: (String) -> Unit,
    unsupported: Boolean = false,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = mediaIcon(file.mediaType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = if (unsupported) {
                    stringResource(R.string.unsupported_reason)
                } else {
                    buildString {
                        file.extension?.let { append(it.uppercase()) }
                        file.sizeBytes?.let {
                            if (isNotEmpty()) append(" · ")
                            append(sizeFormatter(it))
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { onRemove(file.id) }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.file_remove),
                )
            }
        }
    }
}

private fun mediaIcon(type: MediaType): ImageVector = when (type) {
    MediaType.IMAGE -> Icons.Filled.Image
    MediaType.VIDEO -> Icons.Filled.Movie
    MediaType.AUDIO -> Icons.Filled.Audiotrack
    MediaType.UNKNOWN -> Icons.AutoMirrored.Filled.InsertDriveFile
}

private fun groupLabel(type: MediaType): Int = when (type) {
    MediaType.VIDEO -> R.string.group_video
    MediaType.IMAGE -> R.string.group_image
    MediaType.AUDIO -> R.string.group_audio
    MediaType.UNKNOWN -> R.string.unsupported_title
}
