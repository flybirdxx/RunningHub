package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.feature.task.presentation.TaskHistoryFilter
import com.runninghub.feature.task.presentation.TaskHistoryUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_history_empty_filter
import runninghub.composeapp.generated.resources.task_history_empty_history

class TaskHistoryVoyagerScreen : Screen, KoinComponent {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { TaskHistoryScreenModel(get(), get()) }
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadHistory() }

        TaskHistoryContent(
            uiState = uiState,
            onFilterSelected = screenModel::setFilter,
            onRetry = screenModel::loadHistory,
            onViewOutput = screenModel::selectOutput,
            onOpenTaskDetail = screenModel::openTaskDetail,
            onCloseTaskDetail = screenModel::closeTaskDetail,
            onReuseParams = screenModel::prepareReuseParams,
            onRetryTask = screenModel::retryTask,
            onCancelTask = screenModel::cancelTask,
        )
    }
}

@Composable
internal fun TaskHistoryContent(
    uiState: TaskHistoryUiState,
    modifier: Modifier = Modifier,
    onFilterSelected: (TaskHistoryFilter) -> Unit = {},
    onRetry: () -> Unit = {},
    onViewOutput: (String) -> Unit = {},
    onOpenTaskDetail: (String) -> Unit = {},
    onCloseTaskDetail: () -> Unit = {},
    onReuseParams: (String) -> Unit = {},
    onRetryTask: (String) -> Unit = {},
    onCancelTask: (String) -> Unit = {},
) {
    val loadedEntries = uiState.items
    val timelineEntries = loadedEntries
    val filteredItems = timelineEntries.filteredBy(uiState.filter)
    val errorMessage = uiState.error?.toDisplayHistoryError()
    val actionMessage = uiState.actionMessage?.toDisplayActionMessage()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = RhBackground,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(RhBackground),
                contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { HistoryTopBar(onRetry = onRetry) }
                item {
                    TaskHistoryFilterRow(
                        selectedFilter = uiState.filter,
                        onFilterSelected = onFilterSelected,
                    )
                }
                item { NoticeBar() }
                if (actionMessage != null || uiState.selectedOutput != null || uiState.reuseParams.isNotEmpty()) {
                    item {
                        HistoryActionPanel(
                            message = actionMessage,
                            selectedOutput = uiState.selectedOutput,
                            hasPreparedParams = uiState.reuseParams.isNotEmpty(),
                        )
                    }
                }

                when {
                    uiState.isLoading && timelineEntries.isEmpty() -> item { LoadingPanel(Modifier.height(360.dp)) }
                    errorMessage != null && timelineEntries.isEmpty() -> item {
                        TaskHistoryErrorState(message = errorMessage, onRetry = onRetry)
                    }
                    timelineEntries.isEmpty() -> item {
                        TaskHistoryEmptyState(message = stringResource(Res.string.task_history_empty_history))
                    }
                    filteredItems.isEmpty() -> item {
                        TaskHistoryEmptyState(message = stringResource(Res.string.task_history_empty_filter))
                    }
                    else -> {
                        item { DateGroupHeader(total = filteredItems.size) }
                        items(items = filteredItems, key = { item -> item.taskId }) { item ->
                            TaskTimelineRow(
                                item = item,
                                onOpenTaskDetail = onOpenTaskDetail,
                                onViewOutput = onViewOutput,
                                onReuseParams = onReuseParams,
                                onRetryTask = onRetryTask,
                                onCancelTask = onCancelTask,
                            )
                        }
                    }
                }
            }
        }
        if (uiState.selectedTaskDetailUi != null || uiState.isTaskDetailLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f))
                    .clickable(onClick = onCloseTaskDetail),
            )
            TaskDetailDrawer(
                detail = uiState.selectedTaskDetailUi,
                isLoading = uiState.isTaskDetailLoading,
                onClose = onCloseTaskDetail,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}
