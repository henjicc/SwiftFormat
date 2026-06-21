package com.henjicc.swiftformat.feature.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.henjicc.swiftformat.R

@Composable
internal fun SettingsInfoDialog(
    kind: InfoDialogKind?,
    logs: List<String>,
    onDismiss: () -> Unit,
) {
    if (kind == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when (kind) {
                        InfoDialogKind.PRIVACY -> R.string.settings_privacy
                        InfoDialogKind.OPEN_SOURCE -> R.string.settings_open_source
                        InfoDialogKind.LOGS -> R.string.settings_view_logs
                    },
                ),
            )
        },
        text = {
            when (kind) {
                InfoDialogKind.PRIVACY -> Text(stringResource(R.string.settings_privacy_content))
                InfoDialogKind.OPEN_SOURCE -> Text(stringResource(R.string.settings_open_source_content))
                InfoDialogKind.LOGS -> Text(
                    if (logs.isEmpty()) {
                        stringResource(R.string.settings_logs_empty)
                    } else {
                        logs.joinToString(separator = "\n")
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

internal enum class InfoDialogKind {
    PRIVACY,
    OPEN_SOURCE,
    LOGS,
}
