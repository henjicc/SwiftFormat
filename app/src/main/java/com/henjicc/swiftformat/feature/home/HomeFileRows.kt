package com.henjicc.swiftformat.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType

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
