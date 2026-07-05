package com.runninghub.feature.quickcreate.presentation.session

import com.runninghub.feature.quickcreate.domain.QuickCreateSessionConversationSnapshot
import com.runninghub.feature.quickcreate.domain.QuickCreateSessionResultSnapshot
import com.runninghub.feature.quickcreate.domain.QuickCreateSessionSnapshot
import com.runninghub.feature.quickcreate.domain.QuickCreateSessionSnapshotRepository
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateConversationItemUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultMediaType
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskStatusText
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 快捷创作会话快照的恢复和自动保存协调器。
 *
 * 草稿模块只保存编辑输入；本类保存生成结果区需要恢复的终态会话，避免 App 重启后刚生成的图片或视频从页面消失。
 */
class QuickCreateSessionSnapshotStateHolder(
    private val sessionSnapshotRepository: QuickCreateSessionSnapshotRepository,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
) {
    private var snapshotJob: Job? = null

    /**
     * 先恢复本地快照，再开始观察页面状态并持久化可恢复投影。
     */
    fun start() {
        if (snapshotJob?.isActive == true) return
        snapshotJob = scope.launch {
            restoreSnapshot()
            uiState
                .map { it.toRestorableSessionSnapshot() }
                .distinctUntilChanged()
                .collect { snapshot ->
                    if (snapshot == null) {
                        sessionSnapshotRepository.clearSnapshot()
                    } else {
                        sessionSnapshotRepository.saveSnapshot(snapshot)
                    }
                }
        }
    }

    /**
     * 停止自动保存任务。
     */
    fun dispose() {
        snapshotJob?.cancel()
        snapshotJob = null
    }

    private suspend fun restoreSnapshot() {
        val snapshot = sessionSnapshotRepository.getSnapshot() ?: return
        uiState.update { state ->
            if (state.conversationItems.isNotEmpty() || state.results.isNotEmpty()) {
                state
            } else {
                state.withRestoredSessionSnapshot(snapshot)
            }
        }
    }
}

private fun QuickCreateUiState.toRestorableSessionSnapshot(): QuickCreateSessionSnapshot? {
    val restorableConversationItems = conversationItems.filter { it.isRestorableSessionItem() }
    val restorableResults = results
        .filter { it.url.isNotBlank() }
        .map { it.toSessionResultSnapshot() }
    if (restorableConversationItems.isEmpty() && restorableResults.isEmpty()) return null

    val latestItem = restorableConversationItems.lastOrNull()
    val snapshotResults = latestItem?.results
        ?.filter { it.url.isNotBlank() }
        ?.map { it.toSessionResultSnapshot() }
        ?.takeIf { it.isNotEmpty() }
        ?: restorableResults
    val snapshotStatus = latestItem?.taskStatus ?: taskStatus.toTerminalOrIdleStatus(snapshotResults)

    return QuickCreateSessionSnapshot(
        currentTab = currentTab.name,
        submittedPrompt = latestItem?.prompt ?: submittedPrompt,
        taskStatus = snapshotStatus.name,
        taskId = latestItem?.taskId ?: taskId,
        results = snapshotResults,
        conversationItems = restorableConversationItems.map { it.toSessionConversationSnapshot() },
    )
}

private fun QuickCreateConversationItemUi.isRestorableSessionItem(): Boolean =
    results.any { it.url.isNotBlank() } || taskStatus in terminalStatuses

private fun QuickCreateConversationItemUi.toSessionConversationSnapshot(): QuickCreateSessionConversationSnapshot =
    QuickCreateSessionConversationSnapshot(
        prompt = prompt,
        taskStatus = taskStatus.name,
        taskId = taskId,
        aspectRatio = aspectRatio,
        resolution = resolution,
        results = results
            .filter { it.url.isNotBlank() }
            .map { it.toSessionResultSnapshot() },
    )

private fun QuickCreateResultUi.toSessionResultSnapshot(): QuickCreateSessionResultSnapshot =
    QuickCreateSessionResultSnapshot(
        url = url,
        type = type,
        mediaType = mediaType.name,
        thumbnailUrl = thumbnailUrl,
        width = width,
        height = height,
        duration = duration,
    )

private fun QuickCreateUiState.withRestoredSessionSnapshot(snapshot: QuickCreateSessionSnapshot): QuickCreateUiState {
    val restoredItems = snapshot.conversationItems.mapNotNull { it.toConversationItemUiOrNull() }
    val restoredResults = snapshot.results.mapNotNull { it.toResultUiOrNull() }
    val restoredStatus = snapshot.taskStatus.toRestoredTaskStatus(
        hasResults = restoredResults.isNotEmpty() || restoredItems.lastOrNull()?.results?.isNotEmpty() == true,
    )
    return copy(
        currentTab = snapshot.currentTab.toQuickCreateTab(currentTab),
        submittedPrompt = snapshot.submittedPrompt,
        taskStatus = restoredStatus,
        taskId = snapshot.taskId,
        statusText = restoredStatus.toRestoredStatusText(),
        results = restoredResults.ifEmpty { restoredItems.lastOrNull()?.results.orEmpty() },
        conversationItems = restoredItems,
    )
}

private fun QuickCreateSessionConversationSnapshot.toConversationItemUiOrNull(): QuickCreateConversationItemUi? {
    val restoredResults = results.mapNotNull { it.toResultUiOrNull() }
    val restoredStatus = taskStatus.toRestoredTaskStatus(hasResults = restoredResults.isNotEmpty())
    if (prompt.isBlank() && restoredResults.isEmpty() && taskId.isNullOrBlank()) return null
    return QuickCreateConversationItemUi(
        prompt = prompt,
        taskStatus = restoredStatus,
        taskId = taskId,
        aspectRatio = aspectRatio,
        resolution = resolution,
        statusText = restoredStatus.toRestoredStatusText(),
        results = restoredResults,
    )
}

private fun QuickCreateSessionResultSnapshot.toResultUiOrNull(): QuickCreateResultUi? {
    if (url.isBlank()) return null
    return QuickCreateResultUi(
        url = url,
        type = type,
        mediaType = mediaType.toResultMediaType(),
        thumbnailUrl = thumbnailUrl,
        width = width,
        height = height,
        duration = duration,
    )
}

private fun String.toQuickCreateTab(fallback: QuickCreateTab): QuickCreateTab =
    enumValues<QuickCreateTab>().firstOrNull { it.name == this } ?: fallback

private fun String.toResultMediaType(): QuickCreateResultMediaType =
    enumValues<QuickCreateResultMediaType>().firstOrNull { it.name == this } ?: QuickCreateResultMediaType.IMAGE

private fun String.toRestoredTaskStatus(hasResults: Boolean): QuickCreateTaskUiStatus {
    val parsed = enumValues<QuickCreateTaskUiStatus>().firstOrNull { it.name == this }
    return when {
        hasResults -> QuickCreateTaskUiStatus.SUCCESS
        parsed != null && parsed in terminalStatuses -> parsed
        else -> QuickCreateTaskUiStatus.IDLE
    }
}

private fun QuickCreateTaskUiStatus.toTerminalOrIdleStatus(
    restoredResults: List<QuickCreateSessionResultSnapshot>,
): QuickCreateTaskUiStatus = when {
    restoredResults.isNotEmpty() -> QuickCreateTaskUiStatus.SUCCESS
    this in terminalStatuses -> this
    else -> QuickCreateTaskUiStatus.IDLE
}

private fun QuickCreateTaskUiStatus.toRestoredStatusText(): QuickCreateTaskStatusText? =
    when (this) {
        QuickCreateTaskUiStatus.SUCCESS -> QuickCreateTaskStatusText.Success
        QuickCreateTaskUiStatus.FAILED -> QuickCreateTaskStatusText.Failed
        QuickCreateTaskUiStatus.CANCELED -> QuickCreateTaskStatusText.Canceled
        QuickCreateTaskUiStatus.IDLE,
        QuickCreateTaskUiStatus.SUBMITTING,
        QuickCreateTaskUiStatus.QUEUING,
        QuickCreateTaskUiStatus.RUNNING -> null
    }

private val terminalStatuses = setOf(
    QuickCreateTaskUiStatus.SUCCESS,
    QuickCreateTaskUiStatus.FAILED,
    QuickCreateTaskUiStatus.CANCELED,
)
