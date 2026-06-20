package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreateDraftRepository
import com.runninghub.shared.domain.repository.QuickCreateDraftSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 快捷创作页面可恢复草稿的数据快照。
 *
 * 该模型只保存恢复编辑所需的最小字段：当前 Tab、图片提示词和视频提示词。
 * 它不保存上传素材、计费结果或任务状态，避免恢复草稿时误复用已经失效的远程资源和价格。
 *
 * @property currentTab 保存草稿时用户所在的创作 Tab，使用 [QuickCreateTab.name] 的字符串值。
 * 空字符串或未知值会在恢复时按提示词内容降级到图片 Tab。
 * @property imagePrompt 图片创作提示词，来源于用户在图片 Tab 的输入。
 * 空字符串表示图片提示词没有内容，此时不能单独恢复为图片草稿。
 * @property videoPrompt 视频创作提示词，来源于用户在视频 Tab 的输入。
 * 空字符串表示视频提示词没有内容，此时不能单独恢复为视频草稿。
 */
data class DraftData(
    val currentTab: String = "IMAGE",
    val imagePrompt: String = "",
    val videoPrompt: String = "",
)

internal val DraftData.hasPromptContent: Boolean
    get() = imagePrompt.isNotBlank() || videoPrompt.isNotBlank()

internal val DraftData.restorableTab: QuickCreateTab
    get() = when {
        currentTab == "VIDEO" && videoPrompt.isNotBlank() -> QuickCreateTab.VIDEO
        currentTab != "VIDEO" && imagePrompt.isNotBlank() -> QuickCreateTab.IMAGE
        imagePrompt.isNotBlank() -> QuickCreateTab.IMAGE
        videoPrompt.isNotBlank() -> QuickCreateTab.VIDEO
        else -> QuickCreateTab.IMAGE
    }

/**
 * 生成草稿恢复提示文案。
 *
 * 该函数位于 presentation 层，因为返回值是页面直接展示的中文摘要。
 * 它根据可恢复 Tab 计算字数，避免视频 Tab 无内容时展示一个无法恢复的视频草稿。
 */
internal fun DraftData.resumeSummaryText(): String {
    val tab = restorableTab
    val tabLabel = if (tab == QuickCreateTab.VIDEO) "视频" else "图片"
    val promptLength = if (tab == QuickCreateTab.VIDEO) videoPrompt.length else imagePrompt.length
    return "上次草稿 · $tabLabel · $promptLength 字"
}

/**
 * 草稿恢复到编辑区时需要应用的最小状态。
 *
 * StateHolder 只负责从草稿推导恢复结果，真正写入 QuickCreateUiState 的动作由 ScreenModel 完成，
 * 这样草稿模块不会了解计费、模型参数或任务提交等更大的页面编排。
 *
 * @property tab 恢复后应切换到的创作 Tab，由草稿保存的 Tab 和非空提示词共同决定。
 * @property imagePrompt 需要写回图片编辑区的提示词；空字符串表示图片编辑区应保持无提示词内容。
 * @property videoPrompt 需要写回视频编辑区的提示词；空字符串表示视频编辑区应保持无提示词内容。
 */
internal data class QuickCreateDraftRestore(
    val tab: QuickCreateTab,
    val imagePrompt: String,
    val videoPrompt: String,
)

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
internal class QuickCreateDraftStateHolder(
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
