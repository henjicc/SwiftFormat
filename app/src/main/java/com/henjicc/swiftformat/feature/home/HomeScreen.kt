package com.henjicc.swiftformat.feature.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputFormatCatalog
import com.henjicc.swiftformat.core.model.QualityPreset
import com.henjicc.swiftformat.core.model.SizePreset
import com.henjicc.swiftformat.service.ConversionForegroundService

@Composable
fun HomeScreen(
    onConversionStarted: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageLoader = (context.applicationContext as SwiftFormatApplication).container.thumbnailImageLoader

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

    // Android 13+ 通知需要运行时授权；未授权也不阻塞转换，只是看不到前台服务通知（见 TASK-06 已知简化）。
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    val onStartConversion = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.startConversion()
        ConversionForegroundService.start(context)
        onConversionStarted()
    }

    if (!state.hasFiles) {
        EmptyState(onPick = launchPicker, modifier = Modifier.fillMaxSize())
    } else {
        FileList(
            state = state,
            onAddMore = launchPicker,
            onClear = viewModel::clear,
            onRemove = viewModel::removeFile,
            onFormatChange = viewModel::setOutputFormat,
            onQualityChange = viewModel::setQuality,
            onSizeChange = viewModel::setSize,
            onStartConversion = onStartConversion,
            sizeFormatter = { Formatter.formatShortFileSize(context, it) },
            imageLoader = imageLoader,
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
    onFormatChange: (MediaType, String) -> Unit,
    onQualityChange: (MediaType, QualityPreset) -> Unit,
    onSizeChange: (MediaType, SizePreset) -> Unit,
    onStartConversion: () -> Unit,
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
internal fun GroupHeader(label: String, count: Int) {
    Text(
        text = stringResource(R.string.group_count, label, count),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
internal fun FileRow(
    file: InputFile,
    sizeFormatter: (Long) -> String,
    onRemove: (String) -> Unit,
    imageLoader: ImageLoader,
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
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (!unsupported && file.mediaType in THUMBNAIL_TYPES) {
                    AsyncImage(
                        model = file.uri,
                        contentDescription = null,
                        imageLoader = imageLoader,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = rememberVectorPainter(mediaIcon(file.mediaType)),
                    )
                } else {
                    Icon(
                        imageVector = mediaIcon(file.mediaType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

/** 仅图片/视频可生成缩略图（见 SPEC 6.4）；音频统一用图标。 */
internal val THUMBNAIL_TYPES = setOf(MediaType.IMAGE, MediaType.VIDEO)

internal fun mediaIcon(type: MediaType): ImageVector = when (type) {
    MediaType.IMAGE -> Icons.Filled.Image
    MediaType.VIDEO -> Icons.Filled.Movie
    MediaType.AUDIO -> Icons.Filled.Audiotrack
    MediaType.UNKNOWN -> Icons.AutoMirrored.Filled.InsertDriveFile
}

internal fun groupLabel(type: MediaType): Int = when (type) {
    MediaType.VIDEO -> R.string.group_video
    MediaType.IMAGE -> R.string.group_image
    MediaType.AUDIO -> R.string.group_audio
    MediaType.UNKNOWN -> R.string.unsupported_title
}
