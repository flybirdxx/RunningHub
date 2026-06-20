package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.draft.DraftData

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.app.platform.MediaResolver
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationInspirationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateMode
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageModel
import com.runninghub.feature.quickcreate.presentation.editor.VideoAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.VideoResolution
import com.runninghub.feature.quickcreate.presentation.editor.VideoDuration
import com.runninghub.feature.quickcreate.presentation.editor.VideoModel

/**
 * 快捷创作页面的 Voyager ScreenModel 门面。
 *
 * 本类现在只保留 Presentation 层的页面生命周期边界、依赖注入入口、唯一 [QuickCreateUiState]
 * 所有权以及 UI Action 方法外观。草稿、模型目录、媒体上传、计费预览、任务生成、轮询、
 * 历史、项目和灵感模板的跨流程编排已经下沉到 [QuickCreateCoordinator]。
 *
 * 这样做的目的是让 ScreenModel 不再继续膨胀为事实上的业务协调器，同时保持现有 UI 和测试调用
 * 的公开方法签名不变，降低迁移过程中的行为风险。
 *
 * @param historyRepository 快捷创作历史仓库，只用于最近历史、项目任务列表、详情和取消任务。
 * @param modelCatalogRepository 快捷创作模型目录仓库，只用于加载和刷新服务模型列表。
 * @param generationRepository 快捷创作生成仓库，只用于提交图片或视频生成任务并收集状态流。
 * @param feePreviewRepository 快捷创作计费预览仓库，只用于刷新远端价格预览。
 * @param inspirationRepository 快捷创作灵感仓库，只用于加载模板标签、模板分页和模板详情。
 * @param mediaUploadRepository 快捷创作媒体上传仓库，只用于把本地媒体上传为远端 URL。
 * @param projectRepository 快捷创作项目仓库，只用于项目列表、详情和项目变更操作。
 * @param mediaResolver 跨平台媒体读取能力，用于把本地 URI 转交给上传协调器处理。
 * @param draftRepository 快捷创作草稿领域仓库，只保存和清理可恢复编辑草稿快照。
 * @param ioDispatcher 媒体字节读取使用的调度器；commonMain 默认使用跨平台可用的 Default，
 * Android/iOS 如需专用 IO 调度器可在组合根或测试中显式注入。
 */
class QuickCreateScreenModel(
    private val historyRepository: QuickCreationTaskHistoryRepository,
    private val modelCatalogRepository: QuickCreationModelCatalogRepository,
    private val generationRepository: QuickCreationGenerationRepository,
    private val feePreviewRepository: QuickCreationFeePreviewRepository,
    private val inspirationRepository: QuickCreationInspirationRepository,
    private val mediaUploadRepository: QuickCreationMediaUploadRepository,
    private val projectRepository: QuickCreationProjectRepository,
    private val mediaResolver: MediaResolver,
    private val draftRepository: QuickCreateDraftRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ScreenModel {

    private val _uiState = MutableStateFlow(QuickCreateUiState())

    /**
     * 页面唯一只读状态流。
     *
     * UI 只能观察该 StateFlow 并通过本类公开 action 回传用户操作；实际状态修改由 Coordinator
     * 和局部 StateHolder / Interactor 完成，避免 Composable 直接接触仓库或存储。
     */
    val uiState: StateFlow<QuickCreateUiState> = _uiState.asStateFlow()

    private val coordinator = QuickCreateCoordinator(
        historyRepository = historyRepository,
        mediaResolver = mediaResolver,
        draftRepository = draftRepository,
        scope = screenModelScope,
        uiState = _uiState,
        ioDispatcher = ioDispatcher,
        modelCatalogRepository = modelCatalogRepository,
        generationRepository = generationRepository,
        feePreviewRepository = feePreviewRepository,
        inspirationRepository = inspirationRepository,
        mediaUploadRepository = mediaUploadRepository,
        projectRepository = projectRepository,
    )

    /**
     * 当前是否存在可恢复草稿。
     *
     * 该属性保留给旧 UI 和测试读取；数据来源仍是 [QuickCreateUiState.draftData]，
     * 不直接访问持久化存储。
     */
    val hasDraft: Boolean
        get() = _uiState.value.hasDraft

    /**
     * 当前内存中的可恢复草稿数据。
     *
     * `null` 表示没有可恢复草稿，或草稿已经被恢复、放弃、提交进入队列后清理。
     */
    val draftData: DraftData?
        get() = _uiState.value.draftData

    init {
        coordinator.initialize()
    }

    /**
     * 释放页面级长生命周期任务。
     *
     * Voyager 会在页面离开导航栈时调用该方法；实际释放顺序由 [QuickCreateCoordinator.dispose]
     * 统一维护，确保生成轮询、历史轮询、计费防抖、媒体上传和草稿自动保存都被取消。
     */
    override fun onDispose() {
        coordinator.dispose()
    }

    /** 重新加载服务端快捷创作模型目录。 */
    fun loadServiceModels() {
        coordinator.loadServiceModels()
    }

    /** 加载最近快捷创作历史首页。 */
    fun loadQuickCreationHistory() {
        coordinator.loadQuickCreationHistory()
    }

    /** 加载快捷创作项目列表首页。 */
    fun loadQuickCreationProjects() {
        coordinator.loadQuickCreationProjects()
    }

    /** 加载更多快捷创作项目。 */
    fun loadMoreQuickCreationProjects() {
        coordinator.loadMoreQuickCreationProjects()
    }

    /** 加载更多当前历史区域，可能是最近历史或当前项目任务。 */
    fun loadMoreQuickCreationHistory() {
        coordinator.loadMoreQuickCreationHistory()
    }

    /**
     * 选择项目并将历史区域切换为该项目任务。
     *
     * @param projectId 项目稳定标识；空值和重复选择由 Coordinator 下游忽略。
     */
    fun selectProject(projectId: String) {
        coordinator.selectProject(projectId)
    }

    /** 清除当前项目筛选并恢复最近历史。 */
    fun clearSelectedProject() {
        coordinator.clearSelectedProject()
    }

    /**
     * 切换项目置顶状态。
     *
     * @param projectId 需要置顶或取消置顶的项目稳定标识。
     */
    fun toggleProjectPin(projectId: String) {
        coordinator.toggleProjectPin(projectId)
    }

    /**
     * 创建快捷创作项目。
     *
     * @param name 用户输入的新项目名称。
     */
    fun createProject(name: String) {
        coordinator.createProject(name)
    }

    /**
     * 重命名快捷创作项目。
     *
     * @param projectId 需要重命名的项目稳定标识。
     * @param name 用户输入的新名称。
     */
    fun renameProject(projectId: String, name: String) {
        coordinator.renameProject(projectId, name)
    }

    /**
     * 删除快捷创作项目。
     *
     * 如果删除的是当前选中项目，Coordinator 会恢复最近历史，避免 UI 停留在已删除项目的任务列表。
     */
    fun deleteProject(projectId: String) {
        coordinator.deleteProject(projectId)
    }

    /**
     * 打开项目详情。
     *
     * @param projectId 要查看详情的项目稳定标识。
     */
    fun selectProjectDetail(projectId: String) {
        coordinator.selectProjectDetail(projectId)
    }

    /** 关闭项目详情。 */
    fun dismissProjectDetail() {
        coordinator.dismissProjectDetail()
    }

    /**
     * 取消历史中的远端任务。
     *
     * @param taskId 服务端任务标识。
     */
    fun cancelHistoryTask(taskId: String) {
        coordinator.cancelHistoryTask(taskId)
    }

    /**
     * 选择历史输出并加载详情。
     *
     * @param outputId 历史输出项标识。
     */
    fun selectHistoryOutput(outputId: String) {
        coordinator.selectHistoryOutput(outputId)
    }

    /** 关闭历史输出详情。 */
    fun dismissHistoryDetail() {
        coordinator.dismissHistoryDetail()
    }

    /** 检查本地是否存在可恢复草稿。 */
    fun checkForDraft() {
        coordinator.checkForDraft()
    }

    /** 将当前草稿恢复到图片或视频编辑区，并重新调度价格预览。 */
    fun restoreDraft() {
        coordinator.restoreDraft()
    }

    /** 放弃当前可恢复草稿。 */
    fun discardDraft() {
        coordinator.discardDraft()
    }

    /**
     * 切换快捷创作一级模式。
     *
     * @param mode 目标模式，普通创作或灵感模板模式。
     */
    fun switchMode(mode: QuickCreateMode) {
        coordinator.switchMode(mode)
    }

    /** 加载灵感模板首页。 */
    fun loadInspiration() {
        coordinator.loadInspiration()
    }

    /** 加载更多灵感模板。 */
    fun loadMoreInspirationTemplates() {
        coordinator.loadMoreInspirationTemplates()
    }

    /**
     * 应用灵感模板到编辑区。
     *
     * @param templateId 模板稳定标识，来源于灵感模板列表。
     */
    fun applyInspirationTemplate(templateId: String) {
        coordinator.applyInspirationTemplate(templateId)
    }

    /**
     * 切换图片 / 视频编辑 Tab。
     *
     * 切换会通过 Coordinator 触发计费预览和草稿自动保存。
     */
    fun switchTab(tab: QuickCreateTab) {
        coordinator.switchTab(tab)
    }

    /**
     * 更新图片 Prompt。
     *
     * @param prompt 用户输入的图片提示词；空字符串表示清空输入。
     */
    fun updateImagePrompt(prompt: String) {
        coordinator.updateImagePrompt(prompt)
    }

    /**
     * 更新视频 Prompt。
     *
     * @param prompt 用户输入的视频提示词；空字符串表示清空输入。
     */
    fun updateVideoPrompt(prompt: String) {
        coordinator.updateVideoPrompt(prompt)
    }

    /** 打开模型选择弹层。 */
    fun showModelPickerSheet() {
        coordinator.showModelPickerSheet()
    }

    /** 打开参数调节弹层。 */
    fun showParamsSheet() {
        coordinator.showParamsSheet()
    }

    /** 关闭当前模型或参数弹层。 */
    fun closeActiveSheet() {
        coordinator.closeActiveSheet()
    }

    /**
     * 切换图片本地模型。
     *
     * @param model 图片本地兜底模型枚举。
     */
    fun updateImageModel(model: ImageModel) {
        coordinator.updateImageModel(model)
    }

    /**
     * 切换图片服务模型。
     *
     * @param model 来自当前图片服务模型目录的模型对象。
     */
    fun updateImageServiceModel(model: QuickCreationServiceModel) {
        coordinator.updateImageServiceModel(model)
    }

    /**
     * 通过 UI 模型身份键切换图片服务模型。
     *
     * @param identityKey 模型选择面板回传的稳定身份键，格式由 Presentation 映射层生成。
     */
    fun updateImageServiceModel(identityKey: String) {
        coordinator.updateImageServiceModel(identityKey)
    }

    /**
     * 切换视频服务模型。
     *
     * @param model 来自当前视频服务模型目录的模型对象。
     */
    fun updateVideoServiceModel(model: QuickCreationServiceModel) {
        coordinator.updateVideoServiceModel(model)
    }

    /**
     * 通过 UI 模型身份键切换视频服务模型。
     *
     * @param identityKey 模型选择面板回传的稳定身份键，空白或过期 key 会被忽略。
     */
    fun updateVideoServiceModel(identityKey: String) {
        coordinator.updateVideoServiceModel(identityKey)
    }

    /**
     * 更新图片服务字段参数。
     *
     * @param paramKey 服务字段参数名。
     * @param value 用户选择或输入的字段值。
     */
    fun updateImageServiceParam(paramKey: String, value: String) {
        coordinator.updateImageServiceParam(paramKey, value)
    }

    /**
     * 更新视频服务字段参数。
     *
     * @param paramKey 服务字段参数名。
     * @param value 用户选择或输入的字段值。
     */
    fun updateVideoServiceParam(paramKey: String, value: String) {
        coordinator.updateVideoServiceParam(paramKey, value)
    }

    /**
     * 切换视频本地模型。
     *
     * @param model 视频本地兜底模型枚举。
     */
    fun updateVideoModel(model: VideoModel) {
        coordinator.updateVideoModel(model)
    }

    /** 更新图片宽高比。 */
    fun updateImageAspectRatio(ratio: ImageAspectRatio) {
        coordinator.updateImageAspectRatio(ratio)
    }

    /** 更新图片分辨率。 */
    fun updateImageResolution(res: ImageResolution) {
        coordinator.updateImageResolution(res)
    }

    /** 更新图片质量档位。 */
    fun updateImageQuality(quality: ImageQuality) {
        coordinator.updateImageQuality(quality)
    }

    /**
     * 更新图片生成数量。
     *
     * @param count 生成数量，只接受当前模型支持的固定值。
     */
    fun updateImageCount(count: Int) {
        coordinator.updateImageCount(count)
    }

    /**
     * 更新图片随机种子。
     *
     * @param seed 非负整数表示固定种子，`null` 或负数表示交给服务端随机。
     */
    fun updateImageSeed(seed: Int?) {
        coordinator.updateImageSeed(seed)
    }

    /** 更新视频宽高比。 */
    fun updateVideoAspectRatio(ratio: VideoAspectRatio) {
        coordinator.updateVideoAspectRatio(ratio)
    }

    /** 更新视频分辨率。 */
    fun updateVideoResolution(res: VideoResolution) {
        coordinator.updateVideoResolution(res)
    }

    /** 更新视频时长。 */
    fun updateVideoDuration(duration: VideoDuration) {
        coordinator.updateVideoDuration(duration)
    }

    /**
     * 更新视频生成数量。
     *
     * @param count 生成数量，只接受当前模型支持的固定值。
     */
    fun updateVideoCount(count: Int) {
        coordinator.updateVideoCount(count)
    }

    /**
     * 更新视频随机种子。
     *
     * @param seed 非负整数表示固定种子，`null` 或负数表示交给服务端随机。
     */
    fun updateVideoSeed(seed: Int?) {
        coordinator.updateVideoSeed(seed)
    }

    /** 切换视频真实模式。 */
    fun toggleRealisticMode() {
        coordinator.toggleRealisticMode()
    }

    /** 切换视频生成音频开关。 */
    fun toggleGenerateAudio() {
        coordinator.toggleGenerateAudio()
    }

    /**
     * 添加图片 Tab 的全局参考图片。
     *
     * @param uriString 用户从平台文件选择器返回的本地 URI 字符串。
     */
    fun pickImageReference(uriString: String) {
        coordinator.pickImageReference(uriString)
    }

    /**
     * 添加视频 Tab 的全局参考视频。
     *
     * @param uriString 用户从平台文件选择器返回的本地 URI 字符串。
     */
    fun pickVideoReference(uriString: String) {
        coordinator.pickVideoReference(uriString)
    }

    /**
     * 添加视频 Tab 的全局参考音频。
     *
     * @param uriString 用户从平台文件选择器返回的本地 URI 字符串。
     */
    fun pickAudioReference(uriString: String) {
        coordinator.pickAudioReference(uriString)
    }

    /**
     * 为动态服务字段添加图片素材。
     *
     * @param uriString 用户选择的本地图片 URI。
     * @param fieldParamKey 服务字段参数名。
     */
    fun pickImageReferenceForField(uriString: String, fieldParamKey: String) {
        coordinator.pickImageReferenceForField(uriString, fieldParamKey)
    }

    /**
     * 为动态服务字段添加视频素材。
     *
     * @param uriString 用户选择的本地视频 URI。
     * @param fieldParamKey 服务字段参数名。
     */
    fun pickVideoReferenceForField(uriString: String, fieldParamKey: String) {
        coordinator.pickVideoReferenceForField(uriString, fieldParamKey)
    }

    /**
     * 为动态服务字段添加音频素材。
     *
     * @param uriString 用户选择的本地音频 URI。
     * @param fieldParamKey 服务字段参数名。
     */
    fun pickAudioReferenceForField(uriString: String, fieldParamKey: String) {
        coordinator.pickAudioReferenceForField(uriString, fieldParamKey)
    }

    /**
     * 删除指定媒体引用。
     *
     * @param id 媒体引用在页面状态中的稳定标识。
     */
    fun removeMediaReference(id: String) {
        coordinator.removeMediaReference(id)
    }

    /** 清除页面当前错误提示。 */
    fun dismissError() {
        coordinator.dismissError()
    }

    /** 清空当前生成结果并恢复任务展示区域。 */
    fun clearResults() {
        coordinator.clearResults()
    }

    /** 提交当前快捷创作任务。 */
    fun generate() {
        coordinator.generate()
    }
}
