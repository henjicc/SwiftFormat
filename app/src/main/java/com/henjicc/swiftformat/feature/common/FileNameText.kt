package com.henjicc.swiftformat.feature.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val FILE_NAME_MARQUEE_DELAY_MILLIS = 1_200
private val FILE_NAME_MARQUEE_SPACING = 36.dp
private val FILE_NAME_MARQUEE_VELOCITY = 28.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileNameText(
    text: String,
    scrollEnabled: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLinesWhenStatic: Int = 2,
) {
    Text(
        text = text,
        style = style,
        maxLines = if (scrollEnabled) 1 else maxLinesWhenStatic,
        overflow = if (scrollEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = if (scrollEnabled) {
            modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                repeatDelayMillis = FILE_NAME_MARQUEE_DELAY_MILLIS,
                initialDelayMillis = FILE_NAME_MARQUEE_DELAY_MILLIS,
                spacing = MarqueeSpacing(FILE_NAME_MARQUEE_SPACING),
                velocity = FILE_NAME_MARQUEE_VELOCITY,
            )
        } else {
            modifier
        },
    )
}
