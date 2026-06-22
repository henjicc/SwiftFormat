package com.henjicc.swiftformat.feature.progress

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.SwiftFormatApplication
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(stringResource(R.string.progress_done))
                }
            }
        }
    }
}
