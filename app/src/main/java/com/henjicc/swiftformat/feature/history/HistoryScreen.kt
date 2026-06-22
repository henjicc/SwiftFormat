package com.henjicc.swiftformat.feature.history

import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.service.ConversionForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenProgress: () -> Unit = {},
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fileActions = (context.applicationContext as SwiftFormatApplication).container.resultFileActions

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HistoryEvent.NavigateToProgress -> {
                    ConversionForegroundService.start(context)
                    onOpenProgress()
                }

                is HistoryEvent.ShowMessage ->
                    Toast.makeText(context, event.messageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.history_title)) }, modifier = Modifier.fillMaxWidth())
        if (state.items.isEmpty()) {
            EmptyHistory(modifier = Modifier.weight(1f).fillMaxWidth())
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.activeCount > 0) {
                    item("active-banner") {
                        ActiveTasksCard(
                            activeCount = state.activeCount,
                            onOpenProgress = onOpenProgress,
                        )
                    }
                }
                items(state.items, key = { it.id }) { item ->
                    HistoryRecordCard(
                        item = item,
                        sizeFormatter = { Formatter.formatShortFileSize(context, it) },
                        timeFormatter = { millis ->
                            DateUtils.formatDateTime(
                                context,
                                millis,
                                DateUtils.FORMAT_SHOW_DATE or
                                    DateUtils.FORMAT_SHOW_TIME or
                                    DateUtils.FORMAT_ABBREV_MONTH,
                            )
                        },
                        onOpen = {
                            if (!fileActions.open(it)) {
                                Toast.makeText(context, R.string.file_action_failed, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = {
                            if (!fileActions.share(it)) {
                                Toast.makeText(context, R.string.file_action_failed, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShowInFolder = {
                            if (!fileActions.showInFolder()) {
                                Toast.makeText(context, R.string.file_action_failed, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteOutput = viewModel::deleteOutput,
                        onDeleteRecord = viewModel::deleteRecord,
                        onConvertAgain = viewModel::convertAgain,
                        onOpenProgress = onOpenProgress,
                    )
                }
            }
        }
    }
}
