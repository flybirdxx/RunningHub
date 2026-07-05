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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.app.platform.MediaSaveResult
import com.runninghub.app.platform.MediaSaver
import com.runninghub.app.ui.designsystem.components.feedback.RhSnackbar
import com.runninghub.app.ui.designsystem.components.feedback.RhSnackbarSeverity
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionType
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.feature.task.presentation.TaskHistoryDetailMediaType
import com.runninghub.feature.task.presentation.TaskHistoryFilter
import com.runninghub.feature.task.presentation.TaskHistoryUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_history_download_failure
import runninghub.composeapp.generated.resources.task_history_download_success
import runninghub.composeapp.generated.resources.task_history_empty_filter
import runninghub.composeapp.generated.resources.task_history_empty_history

private const val TASK_HISTORY_DOWNLOAD_BANNER_AUTO_DISMISS_MS = 3000L

internal data class TaskHistoryDownloadBanner(val token: Long, val success: Boolean)

class TaskHistoryVoyagerScreen : Screen, KoinComponent {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { TaskHistoryScreenModel(get(), get()) }
        val uiState by screenModel.uiState.collectAsState()
        val mediaSaver = remember { get<MediaSaver>() }
        val downloadCoroutineScope = rememberCoroutineScope()
        var downloadingResultUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
        var downloadBanner by remember { mutableStateOf<TaskHistoryDownloadBanner?>(null) }

        fun showDownloadBanner(success: Boolean) {
            downloadBanner = TaskHistoryDownloadBanner(
                token = (downloadBanner?.token ?: 0L) + 1L,
                success = success,
            )
        }

        fun saveCurrentDetailResultToGallery() {
            val output = uiState.selectedTaskDetailUi?.result?.outputs?.firstOrNull()
            if (output == null || output.url.isBlank()) {
                showDownloadBanner(success = false)
                return
            }
            if (output.mediaType != TaskHistoryDetailMediaType.IMAGE) {
                showDownloadBanner(success = false)
                return
            }
            val resultUrl = output.url
            if (resultUrl in downloadingResultUrls) return

            downloadingResultUrls = downloadingResultUrls + resultUrl
            downloadCoroutineScope.launch {
                val saveSucceeded = try {
                    mediaSaver.saveImageToGallery(
                        url = resultUrl,
                        displayName = "runninghub_history",
                    ) is MediaSaveResult.Success
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (t: Throwable) {
                    false
                } finally {
                    downloadingResultUrls = downloadingResultUrls - resultUrl
                }
                showDownloadBanner(success = saveSucceeded)
            }
        }

        fun handleTaskDetailResultAction(action: ResultPreviewActionType) {
            val taskId = uiState.selectedTaskDetailUi?.taskId
            when (action) {
                ResultPreviewActionType.Save,
                ResultPreviewActionType.Download -> saveCurrentDetailResultToGallery()
                ResultPreviewActionType.ReuseParameters -> taskId?.let(screenModel::prepareReuseParams)
                ResultPreviewActionType.Retry -> taskId?.let(screenModel::retryTask)
                else -> Unit
            }
        }

        LaunchedEffect(Unit) { screenModel.loadHistory() }
        LaunchedEffect(downloadBanner?.token) {
            if (downloadBanner != null) {
                delay(TASK_HISTORY_DOWNLOAD_BANNER_AUTO_DISMISS_MS)
                downloadBanner = null
            }
        }

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
            onTaskDetailResultAction = { action -> handleTaskDetailResultAction(action) },
            downloadBanner = downloadBanner,
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
    onTaskDetailResultAction: (ResultPreviewActionType) -> Unit = {},
    downloadBanner: TaskHistoryDownloadBanner? = null,
) {
    val loadedEntries = uiState.items
    val timelineEntries = loadedEntries
    val filteredItems = timelineEntries.filteredBy(uiState.filter)
    val errorMessage = uiState.error?.toDisplayHistoryError()
    val actionMessage = uiState.actionMessage?.toDisplayActionMessage()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = RhTheme.colors.backgroundPrimary,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(RhTheme.colors.backgroundPrimary),
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
                    .background(RhTheme.colors.overlayScrim)
                    .clickable(onClick = onCloseTaskDetail),
            )
            TaskDetailDrawer(
                detail = uiState.selectedTaskDetailUi,
                isLoading = uiState.isTaskDetailLoading,
                onClose = onCloseTaskDetail,
                onResultAction = onTaskDetailResultAction,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        downloadBanner?.let { banner ->
            RhSnackbar(
                message = stringResource(
                    if (banner.success) {
                        Res.string.task_history_download_success
                    } else {
                        Res.string.task_history_download_failure
                    }
                ),
                severity = if (banner.success) RhSnackbarSeverity.Info else RhSnackbarSeverity.Error,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .padding(horizontal = RhSpacing.lg),
            )
        }
    }
}
