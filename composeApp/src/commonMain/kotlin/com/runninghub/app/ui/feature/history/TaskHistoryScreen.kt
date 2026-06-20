package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewTaskHistoryUiState
import com.runninghub.app.ui.theme.BrandLime
import com.runninghub.app.ui.theme.RhAppBackground
import com.runninghub.app.ui.theme.RhAppCard
import com.runninghub.app.ui.theme.RhAppLine
import com.runninghub.app.ui.theme.RhAppMuted
import com.runninghub.app.ui.theme.RhAppSelected
import com.runninghub.app.ui.theme.RhAppSurface
import com.runninghub.app.ui.theme.RhAppText
import com.runninghub.app.ui.theme.StatusError
import com.runninghub.shared.domain.model.GenerationHistoryOutput
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class TaskHistoryVoyagerScreen : Screen, KoinComponent {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { TaskHistoryScreenModel(get()) }
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadHistory() }

        TaskHistoryContent(
            uiState = uiState,
            onFilterSelected = screenModel::setFilter,
            onRetry = screenModel::loadHistory,
            onViewOutput = screenModel::selectOutput,
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
    onReuseParams: (String) -> Unit = {},
    onRetryTask: (String) -> Unit = {},
    onCancelTask: (String) -> Unit = {},
) {
    val loadedEntries = uiState.items
    val useReferenceFallback = loadedEntries.isEmpty() && uiState.error.isAuthError()
    val timelineEntries = if (useReferenceFallback) referenceHistoryEntries() else loadedEntries
    val filteredItems = timelineEntries.filteredBy(uiState.filter)

    Scaffold(
        modifier = modifier,
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
            item { CurrentProjectCard() }
            item { PinnedProjectStrip() }
            item {
                TaskHistoryFilterRow(
                    selectedFilter = uiState.filter,
                    onFilterSelected = onFilterSelected,
                )
            }
            item { NoticeBar(useReferenceFallback = useReferenceFallback) }
            if (uiState.actionMessage != null || uiState.selectedOutput != null || uiState.reuseParams.isNotEmpty()) {
                item {
                    HistoryActionPanel(
                        message = uiState.actionMessage,
                        selectedOutput = uiState.selectedOutput,
                        reuseParams = uiState.reuseParams,
                    )
                }
            }

            when {
                uiState.isLoading && timelineEntries.isEmpty() -> item { LoadingPanel(Modifier.height(360.dp)) }
                !useReferenceFallback && uiState.error != null && timelineEntries.isEmpty() -> item {
                    TaskHistoryErrorState(message = uiState.error.toDisplayHistoryError(), onRetry = onRetry)
                }
                timelineEntries.isEmpty() -> item { TaskHistoryEmptyState(message = "\u6682\u65e0\u751f\u6210\u5386\u53f2") }
                filteredItems.isEmpty() -> item { TaskHistoryEmptyState(message = "\u5f53\u524d\u7b5b\u9009\u4e0b\u6ca1\u6709\u4efb\u52a1") }
                else -> {
                    item { DateGroupHeader(total = filteredItems.size) }
                    items(items = filteredItems, key = { item -> item.taskId }) { item ->
                        TaskTimelineRow(item = item, onViewOutput = onViewOutput, onReuseParams = onReuseParams, onRetryTask = onRetryTask, onCancelTask = onCancelTask)
                    }
                }
            }
        }
    }
}

@Composable

private fun HistoryTopBar(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(44.dp))
        Text(
            text = "\u751f\u6210\u5386\u53f2",
            color = RhText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = RhText, modifier = Modifier.size(28.dp))
        }
        IconButton(onClick = onRetry, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = RhText, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable

private fun CurrentProjectCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("\u5f53\u524d\u9879\u76ee", color = RhMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(76.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(RhCard)
                .border(1.dp, RhLine, RoundedCornerShape(7.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FolderGlyph()
            Text("\u9ed8\u8ba4\u9879\u76ee", color = RhText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("\u2304", color = RhMuted, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable

private fun PinnedProjectStrip() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("\u7f6e\u9876\u9879\u76ee", color = RhMuted, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProjectChip("+ \u7f6e\u9876\u9879\u76ee", selected = false, dashed = true)
            ProjectChip("\u9ed8\u8ba4\u9879\u76ee", selected = true)
            ProjectChip("\u4ea7\u54c1\u5ba3\u4f20\u7247", selected = false)
            ProjectChip("\u89d2\u8272\u6982\u5ff5\u8bbe\u8ba1", selected = false)
        }
    }
}

@Composable
private fun ProjectChip(label: String, selected: Boolean, dashed: Boolean = false) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 108.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) RhSelected else RhCard)
            .border(1.dp, if (selected) BrandLime else RhLine, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!dashed) FolderGlyph()
        Text(label, color = if (selected) RhText else RhMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
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
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            TaskHistoryFilter.ALL to "\u5168\u90e8",
            TaskHistoryFilter.IN_PROGRESS to "\u8fdb\u884c\u4e2d",
            TaskHistoryFilter.COMPLETED to "\u6210\u529f",
            TaskHistoryFilter.FAILED to "\u5931\u8d25",
        ).forEach { (filter, label) ->
            StatusTab(
                label = label,
                selected = selectedFilter == filter,
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun StatusTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = if (selected) BrandLime else RhMuted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) BrandLime else Color.Transparent),
        )
    }
}

@Composable

private fun NoticeBar(useReferenceFallback: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.dp, RhMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("i", color = RhMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            text = if (useReferenceFallback) "\u8bbf\u5ba2\u6a21\u5f0f\u5c55\u793a\u53c2\u8003\u4efb\u52a1\uff0c\u767b\u5f55\u540e\u540c\u6b65\u771f\u5b9e\u751f\u6210\u5386\u53f2\u3002" else "\u4e91\u7aef\u8f93\u51fa\u94fe\u63a5\u53ef\u80fd\u5b58\u5728\u6709\u6548\u671f\uff0c\u8bf7\u53ca\u65f6\u4fdd\u5b58\u5230\u672c\u5730\u3002",
            color = RhMuted,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("\u203a", color = RhMuted, style = MaterialTheme.typography.titleMedium)
    }
}


@Composable
private fun HistoryActionPanel(
    message: String?,
    selectedOutput: GenerationHistoryOutput?,
    reuseParams: Map<String, String>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, BrandLime.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        message?.let {
            Text(
                text = it,
                color = BrandLime,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        selectedOutput?.let { output ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "\u8f93\u51fa\u8be6\u60c5",
                    color = RhText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = listOfNotNull(output.type.uppercase(), output.sizeLabel(), output.expireLabel()).joinToString(" / "),
                    color = RhMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = output.url,
                    color = RhMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (reuseParams.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "\u53ef\u590d\u7528\u53c2\u6570",
                    color = RhText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                reuseParams.entries.take(3).forEach { (key, value) ->
                    Text(
                        text = "$key: $value",
                        color = RhMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable

private fun DateGroupHeader(total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("\u4eca\u5929", color = RhText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Text("2026-06-19", color = RhMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text("\u5171 $total \u6761", color = RhMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TaskTimelineRow(
    item: TaskHistoryEntry,
    onViewOutput: (String) -> Unit,
    onReuseParams: (String) -> Unit,
    onRetryTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
) {
    val failed = item.status.equals("failed", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, if (failed) StatusError.copy(alpha = 0.45f) else RhLine, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TaskThumbnail(item)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceBadge(item.source)
                Text(
                    text = item.title,
                    color = RhText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("\u4efb\u52a1ID\uff1a${item.taskId}", color = RhMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            ProgressLine(item)
            Text(item.timelineMeta(), color = RhMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(
            modifier = Modifier.width(104.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskStatusPill(status = item.status)
            Text(item.costLabel(), color = RhText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(item.outputCountLabel(), color = RhMuted, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallAction("\u67e5\u770b", highlighted = false, onClick = { item.outputId?.let(onViewOutput) })
                if (item.status.isCompletedStatus()) {
                    SmallAction("\u590d\u7528", highlighted = true, onClick = { onReuseParams(item.taskId) })
                } else if (failed) {
                    SmallAction("\u91cd\u8bd5", highlighted = true, onClick = { onRetryTask(item.taskId) })
                } else {
                    SmallAction("\u53d6\u6d88", highlighted = true, onClick = { onCancelTask(item.taskId) })
                }
            }
        }
    }
}

@Composable
private fun TaskThumbnail(item: TaskHistoryEntry) {
    val color = when {
        item.status.isCompletedStatus() -> Color(0xFF2F3F2C)
        item.status.equals("failed", ignoreCase = true) -> Color(0xFF3F2020)
        else -> Color(0xFF1D2A35)
    }
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .border(1.dp, RhLine, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(item.title.take(1), color = RhText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SourceBadge(source: String) {
    val color = if (source.contains("api", ignoreCase = true) || source.contains("model", ignoreCase = true)) Color(0xFF53D66A) else Color(0xFF60A5FA)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(sourceLabel(source), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ProgressLine(item: TaskHistoryEntry) {
    val running = !item.status.isCompletedStatus() && !item.status.equals("failed", ignoreCase = true)
    val failed = item.status.equals("failed", ignoreCase = true)
    val color = when {
        failed -> StatusError
        running -> BrandLime
        else -> BrandLime
    }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(item.statusDisplay(), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (running) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF41464A)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(4.dp)
                        .background(BrandLime),
                )
            }
        }
    }
}

@Composable
private fun TaskStatusPill(status: String) {
    val color = when {
        status.isCompletedStatus() -> Color(0xFF4ADE5C)
        status.equals("failed", ignoreCase = true) -> StatusError
        else -> Color(0xFF2F7DFF)
    }
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(status.statusPillLabel(), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SmallAction(label: String, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .widthIn(min = 46.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (highlighted) RhSelected else RhCard)
            .border(1.dp, if (highlighted) BrandLime else RhLine, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (highlighted) BrandLime else RhText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FolderGlyph() {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, RhMuted, RoundedCornerShape(3.dp)),
    )
}

@Composable
private fun LoadingPanel(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandLime)
    }
}

@Composable
private fun TaskHistoryErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = StatusError, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        SmallAction("\u91cd\u8bd5", highlighted = true, onClick = onRetry)
    }
}

@Composable
private fun TaskHistoryEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = RhMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}


private fun GenerationHistoryOutput.sizeLabel(): String? =
    width?.let { w -> height?.let { h -> "${w}x$h" } }

private fun GenerationHistoryOutput.expireLabel(): String? =
    expireDays?.takeIf { it.isNotBlank() }?.let { "\u5269\u4f59 $it \u5929" }
        ?: expireTime?.takeIf { it.isNotBlank() }

private fun List<TaskHistoryEntry>.filteredBy(filter: TaskHistoryFilter): List<TaskHistoryEntry> = when (filter) {
    TaskHistoryFilter.ALL -> this
    TaskHistoryFilter.COMPLETED -> filter { it.status.isCompletedStatus() }
    TaskHistoryFilter.FAILED -> filter { it.status.equals("failed", ignoreCase = true) }
    TaskHistoryFilter.IN_PROGRESS -> filter { !it.status.isCompletedStatus() && !it.status.equals("failed", ignoreCase = true) }
}

private fun String.isCompletedStatus(): Boolean = lowercase() in listOf("success", "completed", "done")


private fun String.statusPillLabel(): String = when {
    isCompletedStatus() -> "\u6210\u529f"
    equals("failed", ignoreCase = true) -> "\u5931\u8d25"
    else -> "\u8fdb\u884c\u4e2d"
}


private fun TaskHistoryEntry.statusDisplay(): String = when {
    status.isCompletedStatus() -> "\u6210\u529f"
    status.equals("failed", ignoreCase = true) -> "\u5931\u8d25"
    else -> "\u8fdb\u884c\u4e2d 65%"
}


private fun TaskHistoryEntry.timelineMeta(): String = when {
    status.isCompletedStatus() -> "\u5b8c\u6210 13:58    \u8017\u65f6 ${costTime ?: "00:42"}"
    status.equals("failed", ignoreCase = true) -> "\u5931\u8d25 12:31    \u8017\u65f6 ${costTime ?: "01:15"}"
    else -> "\u5f00\u59cb 14:32    \u9884\u8ba1\u5269\u4f59 ${costTime ?: "02:18"}"
}


private fun TaskHistoryEntry.costLabel(): String = when {
    title.contains("\u89c6\u9891") -> "$0.176"
    title.contains("\u89d2\u8272") -> "$0.063"
    status.equals("failed", ignoreCase = true) -> "$0.051"
    else -> "$0.024"
}


private fun TaskHistoryEntry.outputCountLabel(): String = when {
    status.equals("failed", ignoreCase = true) -> "0 / 1 \u4e2a"
    !status.isCompletedStatus() -> "2 / 4 \u4e2a"
    title.contains("\u89c6\u9891") -> "1 \u4e2a\u89c6\u9891"
    else -> "4 \u4e2a"
}


private fun sourceLabel(source: String): String = when {
    source.contains("api", ignoreCase = true) || source.contains("model", ignoreCase = true) -> "API \u6a21\u578b"
    source.contains("web", ignoreCase = true) -> "WebApp"
    else -> "\u5feb\u901f\u521b\u4f5c"
}

private fun String?.isAuthError(): Boolean = this?.contains("TOKEN", ignoreCase = true) == true || this?.contains("401") == true


private fun String.toDisplayHistoryError(): String = if (isAuthError()) {
    "\u767b\u5f55\u540e\u53ef\u540c\u6b65\u4e91\u7aef\u751f\u6210\u5386\u53f2"
} else {
    this.ifBlank { "\u5386\u53f2\u52a0\u8f7d\u5931\u8d25" }
}


private fun referenceHistoryEntries(): List<TaskHistoryEntry> = listOf(
    TaskHistoryEntry("a1b2c3d4", "\u6982\u5ff5\u56fe XL Pro", "running", "02:18", "quick_creation"),
    TaskHistoryEntry("e5f6g7h8", "PortraitMaster v2", "completed", "00:42", "api_model"),
    TaskHistoryEntry("i9j0k1l2", "3D \u6a21\u578b\u6e32\u67d3", "failed", "01:15", "webapp"),
    TaskHistoryEntry("m3n4o5p6", "\u751f\u6210\u89c6\u9891 Turbo", "completed", "02:36", "quick_creation"),
    TaskHistoryEntry("q7r8s9t0", "\u89d2\u8272\u8bbe\u5b9a XL", "completed", "00:58", "api_model"),
    TaskHistoryEntry("u1v2w3x4", "\u4ea7\u54c1\u5ba3\u4f20\u7247", "running", "03:42", "webapp"),
)

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
            onViewOutput = {},
            onReuseParams = {},
            onRetryTask = {},
            onCancelTask = {},
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

private val RhBackground = RhAppBackground
private val RhSurface = RhAppSurface
private val RhCard = RhAppCard
private val RhSelected = RhAppSelected
private val RhLine = RhAppLine
private val RhText = RhAppText
private val RhMuted = RhAppMuted
