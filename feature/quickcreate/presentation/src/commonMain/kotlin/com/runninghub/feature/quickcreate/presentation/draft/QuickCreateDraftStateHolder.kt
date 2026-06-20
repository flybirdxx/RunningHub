package com.runninghub.feature.quickcreate.presentation.draft

import com.runninghub.feature.quickcreate.domain.QuickCreateDraftRepository
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState

/**
 * 管理快捷创作草稿的读取、恢复、清理和自动保存。
 *
 * 本类是从 QuickCreateScreenModel 中拆出的局部 StateHolder。它只处理草稿相关状态和
 * [QuickCreateDraftRepository] 领域仓库，不负责计费预览、模型加载或任务提交。恢复草稿后是否触发
 * 价格预览仍由 Coordinator 编排，避免草稿组件反向了解生成流程。
 *
 * 并发约束：
 * - 自动保存使用单一 Job，并在新输入、恢复或清理草稿时取消旧 Job。
 * - 读取到空草稿、无提示词草稿或解析失败时，由 Data 仓库清理本地存储，Presentation 只接收可恢复快照。
 * - scope 应来自 ScreenModel，确保页面销毁后草稿任务随生命周期取消。
 *
 * @param draftRepository 快捷创作草稿领域仓库，负责读取、保存和清理可恢复草稿快照。
 * @param scope ScreenModel 生命周期作用域，用于启动草稿读取、清理和防抖保存任务。
 * @param stateProvider 读取当前页面状态的函数，自动保存时从这里提取最新提示词。
 * @param updateState 写回页面状态的函数，只允许修改草稿入口相关状态。
 */
class QuickCreateDraftStateHolder(
    private val draftRepository: QuickCreateDraftRepository,
    private val scope: CoroutineScope,
    private val stateProvider: () -> QuickCreateUiState,
    private val updateState: ((QuickCreateUiState) -> QuickCreateUiState) -> Unit,
) {
    private var draftSaveJob: Job? = null

    /**
     * 从本地存储检查是否存在可恢复草稿。
     *
     * 只有至少包含一个非空提示词的草稿才会进入 UiState；损坏或空草稿由 Data 层仓库清理，
     * 防止页面持续展示无法恢复的草稿入口。
     */
    fun checkForDraft() {
        scope.launch {
            setDraftData(draftRepository.getRestorableDraft()?.toDraftData())
        }
    }

    /**
     * 把当前 UiState 中的草稿恢复到编辑区。
     *
     * @return 成功恢复时返回需要应用到编辑区的状态；没有可恢复草稿时返回 null。
     */
    fun restoreDraft(): QuickCreateDraftRestore? {
        val draft = stateProvider().draftData ?: return null
        val restore = QuickCreateDraftRestore(
            tab = draft.restorableTab,
            imagePrompt = draft.imagePrompt,
            videoPrompt = draft.videoPrompt,
        )
        clearDraft()
        return restore
    }

    /** 用户主动放弃草稿时清理内存状态和本地存储。 */
    fun discardDraft() {
        clearDraft()
    }

    /**
     * 取消待执行的自动保存并清理草稿。
     *
     * 提交任务进入远程队列或用户恢复草稿后都应调用该方法，避免旧草稿覆盖新状态。
     */
    fun clearDraft() {
        draftSaveJob?.cancel()
        draftSaveJob = null
        setDraftData(null)
        scope.launch { draftRepository.clearDraft() }
    }

    /**
     * 延迟保存当前提示词草稿。
     *
     * 500ms 防抖用于合并连续输入，避免每个字符都写入 DataStore。保存完成后会隐藏
     * 旧草稿入口，因为当前编辑内容已经成为最新草稿，不需要再提示恢复旧版本。
     */
    fun autoSaveDraft() {
        draftSaveJob?.cancel()
        draftSaveJob = scope.launch {
            delay(DRAFT_AUTOSAVE_DEBOUNCE_MS)
            val state = stateProvider()
            val draft = DraftData(
                currentTab = state.currentTab.name,
                imagePrompt = state.imageConfig.prompt,
                videoPrompt = state.videoConfig.prompt,
            )
            if (draft.hasPromptContent) {
                draftRepository.saveDraft(draft.toSnapshot())
            } else {
                draftRepository.clearDraft()
            }
            setDraftData(null)
        }
    }

    /**
     * 释放草稿模块持有的延迟保存任务。
     *
     * 页面销毁后不应继续把旧输入写入本地草稿；否则用户重新进入页面时可能看到已经离开页面后的过期内容。
     * 本方法只取消尚未执行的防抖保存，不清理已经存在的持久化草稿，避免页面关闭本身丢失可恢复内容。
     */
    fun dispose() {
        draftSaveJob?.cancel()
        draftSaveJob = null
    }

    private fun setDraftData(draft: DraftData?) {
        updateState { it.copy(draftData = draft) }
    }

    private fun DraftData.toSnapshot(): QuickCreateDraftSnapshot =
        QuickCreateDraftSnapshot(
            currentTab = currentTab,
            imagePrompt = imagePrompt,
            videoPrompt = videoPrompt,
        )

    private fun QuickCreateDraftSnapshot.toDraftData(): DraftData =
        DraftData(
            currentTab = currentTab,
            imagePrompt = imagePrompt,
            videoPrompt = videoPrompt,
        )

    private companion object {
        const val DRAFT_AUTOSAVE_DEBOUNCE_MS = 500L
    }
}
