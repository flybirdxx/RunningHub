package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.shared.domain.model.TaskHistoryItem
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class TaskHistoryVoyagerScreen : Screen, KoinComponent {
    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { TaskHistoryScreenModel(get(), get()) }
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadHistory() }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("任务历史") })
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                // Filter chips
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TaskHistoryFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.filter == filter,
                            onClick = { screenModel.setFilter(filter) },
                            label = { Text(filter.label) },
                        )
                    }
                }

                when {
                    uiState.isLoading -> LoadingIndicator()
                    uiState.error != null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = screenModel::loadHistory) { Text("重试") }
                            }
                        }
                    }
                    uiState.items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无任务记录", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    else -> {
                        val filtered = when (uiState.filter) {
                            TaskHistoryFilter.ALL -> uiState.items
                            TaskHistoryFilter.COMPLETED -> uiState.items.filter { it.taskStatus == "completed" }
                            TaskHistoryFilter.FAILED -> uiState.items.filter { it.taskStatus == "failed" }
                            TaskHistoryFilter.IN_PROGRESS -> uiState.items.filter { it.taskStatus !in listOf("completed", "failed") }
                        }
                        LazyColumn {
                            items(filtered) { item ->
                                TaskHistoryCard(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskHistoryCard(item: TaskHistoryItem) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.taskName ?: "创作任务", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(item.createTime ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("耗时: ${item.taskCostTime ?: "--"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.width(8.dp))
            val statusColor = when (item.taskStatus) {
                "completed" -> MaterialTheme.colorScheme.primary
                "failed" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.secondary
            }
            Text(item.taskStatus ?: "unknown", color = statusColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}
