package com.runninghub.app.ui.feature.quickcreate

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationStateHolder
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationStateHolderFactory
import com.runninghub.feature.quickcreate.presentation.draft.DraftData
import kotlinx.coroutines.flow.StateFlow
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageModel
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.editor.VideoAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.VideoDuration
import com.runninghub.feature.quickcreate.presentation.editor.VideoModel
import com.runninghub.feature.quickcreate.presentation.editor.VideoResolution
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreatePlazaReuseIntent
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateModelPickerFilter
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState

/**
 * 快捷创作页面的 Voyager ScreenModel 门面。
 *
 * 本类只保留应用壳的页面生命周期边界和 UI Action 方法外观。唯一 [QuickCreateUiState]
 * 所有权、仓库聚合以及草稿、模型目录、媒体上传、计费预览、任务生成、轮询、历史和项目编排
 * 均由 [QuickCreatePresentationStateHolder] 承担。
 *
 * 这样做的目的是让 ScreenModel 不再继续膨胀为事实上的业务协调器，同时保持现有 UI 和测试调用
 * 的公开方法签名不变，降低迁移过程中的行为风险。
 *
 * @param presentationFactory 创建页面级 Presentation 状态持有者的工厂，由组合根注入领域仓库和平台端口。
 */
class QuickCreateScreenModel(
    presentationFactory: QuickCreatePresentationStateHolderFactory,
) : ScreenModel {

    private val presentation = presentationFactory.create(screenModelScope)

    /**
     * 页面唯一只读状态流。
     *
     * UI 只能观察该 StateFlow 并通过本类公开 action 回传用户操作；实际状态修改由 Coordinator
     * 和局部 StateHolder / Interactor 完成，避免 Composable 直接接触仓库或存储。
     */
    val uiState: StateFlow<QuickCreateUiState> = presentation.uiState

    /**
     * 当前是否存在可恢复草稿。
     *
     * 该属性保留给旧 UI 和测试读取；数据来源仍是 [QuickCreateUiState.draftData]，
     * 不直接访问持久化存储。
     */
    val hasDraft: Boolean
        get() = presentation.hasDraft

    /**
     * 当前内存中的可恢复草稿数据。
     *
     * `null` 表示没有可恢复草稿，或草稿已经被恢复、放弃、提交进入队列后清理。
     */
    val draftData: DraftData?
        get() = presentation.draftData

    /**
     * 释放页面级长生命周期任务。
     *
     * Voyager 会在页面离开导航栈时调用该方法；实际释放顺序由 [QuickCreatePresentationStateHolder.dispose]
     * 统一维护，确保生成轮询、历史轮询、计费防抖、媒体上传和草稿自动保存都被取消。
     */
    override fun onDispose() {
        presentation.dispose()
    }

    /** 重新加载服务端快捷创作模型目录。 */
    fun loadServiceModels() {
        presentation.loadServiceModels()
    }

    /** 加载最近快捷创作历史首页。 */
    fun loadQuickCreationHistory() {
        presentation.loadQuickCreationHistory()
    }

    /** 加载快捷创作项目列表首页。 */
    fun loadQuickCreationProjects() {
        presentation.loadQuickCreationProjects()
    }

    /** 加载更多快捷创作项目。 */
    fun loadMoreQuickCreationProjects() {
        presentation.loadMoreQuickCreationProjects()
    }

    /** 加载更多当前历史区域，可能是最近历史或当前项目任务。 */
    fun loadMoreQuickCreationHistory() {
        presentation.loadMoreQuickCreationHistory()
    }

    /**
     * 选择项目并将历史区域切换为该项目任务。
     *
     * @param projectId 项目稳定标识；空值和重复选择由 Coordinator 下游忽略。
     */
    fun selectProject(projectId: String) {
        presentation.selectProject(projectId)
    }

    /** 清除当前项目筛选并恢复最近历史。 */
    fun clearSelectedProject() {
        presentation.clearSelectedProject()
    }

    /**
     * 切换项目置顶状态。
     *
     * @param projectId 需要置顶或取消置顶的项目稳定标识。
     */
    fun toggleProjectPin(projectId: String) {
        presentation.toggleProjectPin(projectId)
    }

    /**
     * 创建快捷创作项目。
     *
     * @param name 用户输入的新项目名称。
     */
    fun createProject(name: String) {
        presentation.createProject(name)
    }

    /**
     * 重命名快捷创作项目。
     *
     * @param projectId 需要重命名的项目稳定标识。
     * @param name 用户输入的新名称。
     */
    fun renameProject(projectId: String, name: String) {
        presentation.renameProject(projectId, name)
    }

    /**
     * 删除快捷创作项目。
     *
     * 如果删除的是当前选中项目，Coordinator 会恢复最近历史，避免 UI 停留在已删除项目的任务列表。
     */
    fun deleteProject(projectId: String) {
        presentation.deleteProject(projectId)
    }

    /**
     * 打开项目详情。
     *
     * @param projectId 要查看详情的项目稳定标识。
     */
    fun selectProjectDetail(projectId: String) {
        presentation.selectProjectDetail(projectId)
    }

    /** 关闭项目详情。 */
    fun dismissProjectDetail() {
        presentation.dismissProjectDetail()
    }

    /**
     * 取消历史中的远端任务。
     *
     * @param taskId 服务端任务标识。
     */
    fun cancelHistoryTask(taskId: String) {
        presentation.cancelHistoryTask(taskId)
    }

    /**
     * 选择历史输出并加载详情。
     *
     * @param outputId 历史输出项标识。
     */
    fun selectHistoryOutput(outputId: String) {
        presentation.selectHistoryOutput(outputId)
    }

    /** 关闭历史输出详情。 */
    fun dismissHistoryDetail() {
        presentation.dismissHistoryDetail()
    }

    /** 检查本地是否存在可恢复草稿。 */
    fun checkForDraft() {
        presentation.checkForDraft()
    }

    /** 将当前草稿恢复到图片或视频编辑区，并重新调度价格预览。 */
    fun restoreDraft() {
        presentation.restoreDraft()
    }

    /** 放弃当前可恢复草稿。 */
    /**
     * 将 Plaza 使用同款意图交给 QuickCreate Presentation。
     *
     * ScreenModel 只转发跨 Tab 意图；参数落入编辑区后仍由 Presentation 的计费预览与生成确认流程接管。
     *
     * @param intent Plaza 作品转换出的复用参数意图。
     */
    fun applyPlazaReuseIntent(intent: QuickCreatePlazaReuseIntent) {
        presentation.applyPlazaReuseIntent(intent)
    }

    fun discardDraft() {
        presentation.discardDraft()
    }

    /**
     * 切换图片 / 视频编辑 Tab。
     *
     * 切换会通过 Coordinator 触发计费预览和草稿自动保存。
     */
    fun switchTab(tab: QuickCreateTab) {
        presentation.switchTab(tab)
    }

    /**
     * 更新图片 Prompt。
     *
     * @param prompt 用户输入的图片提示词；空字符串表示清空输入。
     */
    fun updateImagePrompt(prompt: String) {
        presentation.updateImagePrompt(prompt)
    }

    /**
     * 更新视频 Prompt。
     *
     * @param prompt 用户输入的视频提示词；空字符串表示清空输入。
     */
    fun updateVideoPrompt(prompt: String) {
        presentation.updateVideoPrompt(prompt)
    }

    /** 打开模型选择弹层。 */
    fun showModelPickerSheet() {
        presentation.showModelPickerSheet()
    }

    /** 更新模型选择器搜索词。 */
    fun updateModelPickerQuery(query: String) {
        presentation.updateModelPickerQuery(query)
    }

    /** 更新模型选择器分类筛选项。 */
    fun selectModelPickerFilter(filter: QuickCreateModelPickerFilter) {
        presentation.selectModelPickerFilter(filter)
    }

    /** 打开参数调节弹层。 */
    fun showParamsSheet() {
        presentation.showParamsSheet()
    }

    /** 关闭当前模型或参数弹层。 */
    fun closeActiveSheet() {
        presentation.closeActiveSheet()
    }

    /**
     * 切换图片本地模型。
     *
     * @param model 图片本地兜底模型枚举。
     */
    fun updateImageModel(model: ImageModel) {
        presentation.updateImageModel(model)
    }

    /**
     * 切换图片服务模型。
     *
     * @param model 来自当前图片服务模型目录的模型对象。
     */
    fun updateImageServiceModel(model: QuickCreationServiceModel) {
        presentation.updateImageServiceModel(model)
    }

    /**
     * 通过 UI 模型身份键切换图片服务模型。
     *
     * @param identityKey 模型选择面板回传的稳定身份键，格式由 Presentation 映射层生成。
     */
    fun updateImageServiceModel(identityKey: String) {
        presentation.updateImageServiceModel(identityKey)
    }

    /**
     * 切换视频服务模型。
     *
     * @param model 来自当前视频服务模型目录的模型对象。
     */
    fun updateVideoServiceModel(model: QuickCreationServiceModel) {
        presentation.updateVideoServiceModel(model)
    }

    /**
     * 通过 UI 模型身份键切换视频服务模型。
     *
     * @param identityKey 模型选择面板回传的稳定身份键，空白或过期 key 会被忽略。
     */
    fun updateVideoServiceModel(identityKey: String) {
        presentation.updateVideoServiceModel(identityKey)
    }

    /**
     * 更新图片服务字段参数。
     *
     * @param paramKey 服务字段参数名。
     * @param value 用户选择或输入的字段值。
     */
    fun updateImageServiceParam(paramKey: String, value: String) {
        presentation.updateImageServiceParam(paramKey, value)
    }

    /**
     * 更新视频服务字段参数。
     *
     * @param paramKey 服务字段参数名。
     * @param value 用户选择或输入的字段值。
     */
    fun updateVideoServiceParam(paramKey: String, value: String) {
        presentation.updateVideoServiceParam(paramKey, value)
    }

    /**
     * 切换视频本地模型。
     *
     * @param model 视频本地兜底模型枚举。
     */
    fun updateVideoModel(model: VideoModel) {
        presentation.updateVideoModel(model)
    }

    /** 更新图片宽高比。 */
    fun updateImageAspectRatio(ratio: ImageAspectRatio) {
        presentation.updateImageAspectRatio(ratio)
    }

    /** 更新图片分辨率。 */
    fun updateImageResolution(res: ImageResolution) {
        presentation.updateImageResolution(res)
    }

    /** 更新图片质量档位。 */
    fun updateImageQuality(quality: ImageQuality) {
        presentation.updateImageQuality(quality)
    }

    /**
     * 更新图片生成数量。
     *
     * @param count 生成数量，只接受当前模型支持的固定值。
     */
    fun updateImageCount(count: Int) {
        presentation.updateImageCount(count)
    }

    /**
     * 更新图片随机种子。
     *
     * @param seed 非负整数表示固定种子，`null` 或负数表示交给服务端随机。
     */
    fun updateImageSeed(seed: Int?) {
        presentation.updateImageSeed(seed)
    }

    /** 更新视频宽高比。 */
    fun updateVideoAspectRatio(ratio: VideoAspectRatio) {
        presentation.updateVideoAspectRatio(ratio)
    }

    /** 更新视频分辨率。 */
    fun updateVideoResolution(res: VideoResolution) {
        presentation.updateVideoResolution(res)
    }

    /** 更新视频时长。 */
    fun updateVideoDuration(duration: VideoDuration) {
        presentation.updateVideoDuration(duration)
    }

    /**
     * 更新视频生成数量。
     *
     * @param count 生成数量，只接受当前模型支持的固定值。
     */
    fun updateVideoCount(count: Int) {
        presentation.updateVideoCount(count)
    }

    /**
     * 更新视频随机种子。
     *
     * @param seed 非负整数表示固定种子，`null` 或负数表示交给服务端随机。
     */
    fun updateVideoSeed(seed: Int?) {
        presentation.updateVideoSeed(seed)
    }

    /** 切换视频真实模式。 */
    fun toggleRealisticMode() {
        presentation.toggleRealisticMode()
    }

    /** 切换视频生成音频开关。 */
    fun toggleGenerateAudio() {
        presentation.toggleGenerateAudio()
    }

    /**
     * 添加图片 Tab 的全局参考图片。
     *
     * @param uriString 用户从平台文件选择器返回的本地 URI 字符串。
     */
    fun pickImageReference(uriString: String) {
        presentation.pickImageReference(uriString)
    }

    /**
     * 添加视频 Tab 的全局参考视频。
     *
     * @param uriString 用户从平台文件选择器返回的本地 URI 字符串。
     */
    fun pickVideoReference(uriString: String) {
        presentation.pickVideoReference(uriString)
    }

    /**
     * 添加视频 Tab 的全局参考音频。
     *
     * @param uriString 用户从平台文件选择器返回的本地 URI 字符串。
     */
    fun pickAudioReference(uriString: String) {
        presentation.pickAudioReference(uriString)
    }

    /**
     * 为动态服务字段添加图片素材。
     *
     * @param uriString 用户选择的本地图片 URI。
     * @param fieldParamKey 服务字段参数名。
     */
    fun pickImageReferenceForField(uriString: String, fieldParamKey: String) {
        presentation.pickImageReferenceForField(uriString, fieldParamKey)
    }

    /**
     * 为动态服务字段添加视频素材。
     *
     * @param uriString 用户选择的本地视频 URI。
     * @param fieldParamKey 服务字段参数名。
     */
    fun pickVideoReferenceForField(uriString: String, fieldParamKey: String) {
        presentation.pickVideoReferenceForField(uriString, fieldParamKey)
    }

    /**
     * 为动态服务字段添加音频素材。
     *
     * @param uriString 用户选择的本地音频 URI。
     * @param fieldParamKey 服务字段参数名。
     */
    fun pickAudioReferenceForField(uriString: String, fieldParamKey: String) {
        presentation.pickAudioReferenceForField(uriString, fieldParamKey)
    }

    /**
     * 删除指定媒体引用。
     *
     * @param id 媒体引用在页面状态中的稳定标识。
     */
    fun removeMediaReference(id: String) {
        presentation.removeMediaReference(id)
    }

    /** 清除页面当前错误提示。 */
    fun dismissError() {
        presentation.dismissError()
    }

    /** 清空当前生成结果并恢复任务展示区域。 */
    fun clearResults() {
        presentation.clearResults()
    }

    /**
     * 将结果卡保存的 Prompt 快照写回当前编辑区。
     *
     * 该入口只恢复用户可编辑 Prompt，不直接复用远端任务 ID、结果 URL 或平台保存状态；恢复后由编辑器状态持有者
     * 重新调度计费预览和草稿保存，避免旧任务参数绕过当前价格确认。
     */
    fun restoreConversationPrompt(prompt: String) {
        val value = prompt.trim()
        if (value.isEmpty()) return
        when (uiState.value.currentTab) {
            QuickCreateTab.IMAGE -> presentation.updateImagePrompt(value)
            QuickCreateTab.VIDEO -> presentation.updateVideoPrompt(value)
        }
    }

    /** 提交当前快捷创作任务。 */
    fun generate() {
        presentation.generate()
    }

    /** 用户确认生成价格后继续提交当前快捷创作任务。 */
    fun confirmGeneration() {
        presentation.confirmGeneration()
    }
}
