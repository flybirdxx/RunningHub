package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhHeightClass
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewTaskHistoryUiState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.shared.domain.model.TaskHistoryItem
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

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
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskHistoryContent(
    uiState: TaskHistoryUiState,
    modifier: Modifier = Modifier,
    onFilterSelected: (TaskHistoryFilter) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val windowInfo = LocalRhWindowInfo.current
    val topBarHeight = if (windowInfo.heightClass == RhHeightClass.Compact) 48.dp else Dimens.AppBarHeight
    val filteredItems = remember(uiState.items, uiState.filter) {
        uiState.items.filteredBy(uiState.filter)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("任务历史", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.height(topBarHeight),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = windowInfo.formContentMaxWidth),
            ) {
                TaskHistoryFilterRow(
                    selectedFilter = uiState.filter,
                    onFilterSelected = onFilterSelected,
                )

                when {
                    uiState.isLoading -> LoadingIndicator(modifier = Modifier.fillMaxSize())
                    uiState.error != null -> TaskHistoryErrorState(
                        message = uiState.error,
                        onRetry = onRetry,
                    )
                    uiState.items.isEmpty() -> TaskHistoryEmptyState(message = "暂无任务记录")
                    filteredItems.isEmpty() -> TaskHistoryEmptyState(message = "当前筛选暂无任务记录")
                    else -> TaskHistoryList(items = filteredItems)
                }
            }
        }
    }
}

@Composable
private fun TaskHistoryFilterRow(
    selectedFilter: TaskHistoryFilter,
    onFilterSelected: (TaskHistoryFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Dimens.SpaceLG, vertical = Dimens.SpaceSM),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        TaskHistoryFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun TaskHistoryList(items: List<TaskHistoryItem>) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Dimens.Space3XL),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            items = items,
            key = { item -> item.taskId ?: "${item.taskName}-${item.createTime}" },
        ) { item ->
            TaskHistoryCard(item)
        }
    }
}

@Composable
private fun TaskHistoryErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SpaceXXL),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.SpaceSM))
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun TaskHistoryEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SpaceXXL),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TaskHistoryCard(item: TaskHistoryItem) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceLG),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.SpaceMD),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.taskName ?: "创作任务",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.createTime ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "耗时: ${item.taskCostTime ?: "--"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            TaskStatusPill(status = item.taskStatus)
        }
    }
}

@Composable
private fun TaskStatusPill(status: String?) {
    val statusColor = when (status) {
        "completed" -> MaterialTheme.colorScheme.primary
        "failed" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = RoundedCornerShape(Dimens.RadiusFull),
        color = statusColor.copy(alpha = 0.12f),
        modifier = Modifier.widthIn(max = 112.dp),
    ) {
        Text(
            text = status ?: "unknown",
            color = statusColor,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimens.SpaceSM, vertical = 4.dp),
        )
    }
}

private fun List<TaskHistoryItem>.filteredBy(filter: TaskHistoryFilter): List<TaskHistoryItem> = when (filter) {
    TaskHistoryFilter.ALL -> this
    TaskHistoryFilter.COMPLETED -> filter { it.taskStatus == "completed" }
    TaskHistoryFilter.FAILED -> filter { it.taskStatus == "failed" }
    TaskHistoryFilter.IN_PROGRESS -> filter { it.taskStatus !in listOf("completed", "failed") }
}

@Composable
private fun TaskHistoryAdaptivePreview(
    spec: RhPreviewSpec,
    filter: TaskHistoryFilter = TaskHistoryFilter.ALL,
) {
    RhAdaptivePreview(spec = spec) {
        TaskHistoryContent(
            uiState = previewTaskHistoryUiState(filter = filter),
            onFilterSelected = {},
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun TaskHistoryPhone320Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun TaskHistoryPhone360Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun TaskHistoryPhone430Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Phone430)
}

@Preview
@Composable
private fun TaskHistoryMedium600Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Medium600)
}

@Preview
@Composable
private fun TaskHistoryExpanded840Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Expanded840)
}

@Preview
@Composable
private fun TaskHistoryLandscapePreview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Landscape800)
}

@Preview
@Composable
private fun TaskHistoryFontScale13Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.FontScale13)
}

@Preview
@Composable
private fun TaskHistoryFontScale15Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.FontScale15)
}

@Preview
@Composable
private fun TaskHistoryFailedFilterPreview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Phone360, filter = TaskHistoryFilter.FAILED)
}
