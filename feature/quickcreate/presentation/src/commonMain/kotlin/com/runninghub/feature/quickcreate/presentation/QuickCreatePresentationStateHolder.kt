package com.runninghub.feature.quickcreate.presentation

import com.runninghub.feature.quickcreate.domain.QuickCreateDraftRepository
import com.runninghub.feature.quickcreate.domain.QuickCreateModelSelectionRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationInspirationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.quickcreate.presentation.coordinator.QuickCreateCoordinator
import com.runninghub.feature.quickcreate.presentation.draft.DraftData
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageModel
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.editor.VideoAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.VideoDuration
import com.runninghub.feature.quickcreate.presentation.editor.VideoModel
import com.runninghub.feature.quickcreate.presentation.editor.VideoResolution
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateMode
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaResolver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 创建快捷创作页面级 Presentation 会话。
 *
 * 该工厂由应用组合根注入领域仓库、草稿仓库和平台媒体端口，但不依赖 Voyager 或 Compose。
 * 每次页面进入导航栈时，应用壳只需要传入页面生命周期 [CoroutineScope]，即可得到一份独立的
 * [QuickCreatePresentationStateHolder]。这样仓库聚合、状态所有权和 Coordinator 装配都保留在
 * feature presentation 边界内，composeApp 只承担导航生命周期适配。
 *
 * @param historyRepository 快捷创作历史仓库，用于最近历史、项目任务列表、详情和取消任务。
 * @param modelCatalogRepository 快捷创作模型目录仓库，用于加载图片和视频服务模型。
 * @param generationRepository 快捷创作生成仓库，用于提交生成任务并收集任务状态流。
 * @param feePreviewRepository 快捷创作计费预览仓库，用于刷新远端价格预览。
 * @param inspirationRepository 快捷创作灵感仓库，用于加载模板标签、分页和模板详情。
 * @param mediaUploadRepository 快捷创作媒体上传仓库，用于把本地媒体上传为远端 URL。
 * @param projectRepository 快捷创作项目仓库，用于项目列表、详情和项目变更操作。
 * @param mediaResolver 平台无关媒体读取端口，由 composeApp 在组合根适配 Android/iOS 能力。
 * @param draftRepository 快捷创作草稿仓库，用于保存和清理可恢复编辑草稿快照。
 * @param modelSelectionRepository 快捷创作最近模型选择仓库，用于恢复用户上次主动选择的模型。
 * @param ioDispatcher 媒体字节读取使用的调度器；默认值保持 commonMain 跨平台可用。
 */
class QuickCreatePresentationStateHolderFactory(
    private val historyRepository: QuickCreationTaskHistoryRepository,
    private val modelCatalogRepository: QuickCreationModelCatalogRepository,
    private val generationRepository: QuickCreationGenerationRepository,
    private val feePreviewRepository: QuickCreationFeePreviewRepository,
    private val inspirationRepository: QuickCreationInspirationRepository,
    private val mediaUploadRepository: QuickCreationMediaUploadRepository,
    private val projectRepository: QuickCreationProjectRepository,
    private val mediaResolver: QuickCreateMediaResolver,
    private val draftRepository: QuickCreateDraftRepository,
    private val modelSelectionRepository: QuickCreateModelSelectionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /**
     * 为一次页面生命周期创建独立状态持有者。
     *
     * @param scope Voyager ScreenModel 或测试传入的页面级作用域；dispose 前所有异步任务都绑定到该作用域。
     */
    fun create(scope: CoroutineScope): QuickCreatePresentationStateHolder =
        QuickCreatePresentationStateHolder(
            historyRepository = historyRepository,
            modelCatalogRepository = modelCatalogRepository,
            generationRepository = generationRepository,
            feePreviewRepository = feePreviewRepository,
            inspirationRepository = inspirationRepository,
            mediaUploadRepository = mediaUploadRepository,
            projectRepository = projectRepository,
            mediaResolver = mediaResolver,
            draftRepository = draftRepository,
            modelSelectionRepository = modelSelectionRepository,
            scope = scope,
            ioDispatcher = ioDispatcher,
        )
}

/**
 * 快捷创作页面级 Presentation 状态持有者。
 *
 * 本类拥有页面唯一 [QuickCreateUiState]，并把所有 UI action 转发给 [QuickCreateCoordinator]。
 * 它不依赖 Voyager、Koin、Compose Resources 或平台类型，因此可以在 feature presentation
 * 测试中直接验证业务状态流；应用壳 ScreenModel 只负责把导航生命周期转接到 [dispose]。
 *
 * @param historyRepository 快捷创作历史仓库，只用于最近历史、项目任务列表、详情和取消任务。
 * @param modelCatalogRepository 快捷创作模型目录仓库，只用于加载和刷新服务模型列表。
 * @param generationRepository 快捷创作生成仓库，只用于提交图片或视频生成任务并收集状态流。
 * @param feePreviewRepository 快捷创作计费预览仓库，只用于刷新远端价格预览。
 * @param inspirationRepository 快捷创作灵感仓库，只用于加载模板标签、模板分页和模板详情。
 * @param mediaUploadRepository 快捷创作媒体上传仓库，只用于把本地媒体上传为远端 URL。
 * @param projectRepository 快捷创作项目仓库，只用于项目列表、详情和项目变更操作。
 * @param mediaResolver 快捷创作媒体读取端口，由 app 组合根把平台媒体能力适配后注入。
 * @param draftRepository 快捷创作草稿领域仓库，只保存和清理可恢复编辑草稿快照。
 * @param modelSelectionRepository 快捷创作最近模型选择仓库，只保存用户主动选择的模型身份键。
 * @param scope 页面生命周期协程作用域，所有异步任务随该作用域取消。
 * @param ioDispatcher 媒体字节读取使用的调度器。
 */
class QuickCreatePresentationStateHolder(
    historyRepository: QuickCreationTaskHistoryRepository,
    modelCatalogRepository: QuickCreationModelCatalogRepository,
    generationRepository: QuickCreationGenerationRepository,
    feePreviewRepository: QuickCreationFeePreviewRepository,
    inspirationRepository: QuickCreationInspirationRepository,
    mediaUploadRepository: QuickCreationMediaUploadRepository,
    projectRepository: QuickCreationProjectRepository,
    mediaResolver: QuickCreateMediaResolver,
    draftRepository: QuickCreateDraftRepository,
    modelSelectionRepository: QuickCreateModelSelectionRepository,
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
) {
    private val mutableUiState = MutableStateFlow(QuickCreateUiState())

    /**
     * 页面唯一只读状态流。
     *
     * UI 只能观察该 StateFlow 并通过本类公开 action 回传用户操作；实际状态修改由 Coordinator
     * 和局部 StateHolder / Interactor 完成，避免 Composable 直接接触仓库或存储。
     */
    val uiState: StateFlow<QuickCreateUiState> = mutableUiState.asStateFlow()

    private val coordinator = QuickCreateCoordinator(
        historyRepository = historyRepository,
        mediaResolver = mediaResolver,
        draftRepository = draftRepository,
        modelSelectionRepository = modelSelectionRepository,
        scope = scope,
        uiState = mutableUiState,
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
        get() = mutableUiState.value.hasDraft

    /**
     * 当前内存中的可恢复草稿数据。
     *
     * `null` 表示没有可恢复草稿，或草稿已经被恢复、放弃、提交进入队列后清理。
     */
    val draftData: DraftData?
        get() = mutableUiState.value.draftData

    init {
        coordinator.initialize()
    }

    /**
     * 释放页面级长生命周期任务。
     *
     * 实际释放顺序由 [QuickCreateCoordinator.dispose] 统一维护，确保生成轮询、历史轮询、
     * 计费防抖、媒体上传和草稿自动保存都被取消。
     */
    fun dispose() {
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
