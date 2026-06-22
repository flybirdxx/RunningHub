package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.app.platform.MediaResolver
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationInspirationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateFeePreviewInteractor
import com.runninghub.feature.quickcreate.presentation.draft.QuickCreateDraftStateHolder
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageModel
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateEditorStateHolder
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.VideoAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.VideoDuration
import com.runninghub.feature.quickcreate.presentation.editor.VideoModel
import com.runninghub.feature.quickcreate.presentation.editor.VideoResolution
import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationRequestFactory
import com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryStateHolder
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationStateHolder
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateModelCatalogInteractor
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectStateHolder
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskPollingController
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateMode
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaResolver
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaUploadCoordinator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 协调快捷创作页面各个局部 StateHolder 与 Interactor。
 *
 * 本类位于 Presentation 层，负责把草稿、模型目录、媒体上传、计费预览、任务提交、历史、
 * 项目和灵感模板这些局部组件织成一个页面级单向数据流。它不直接渲染 UI，也不实现网络、
 * 数据库或平台媒体读取细节；这些能力仍通过 Repository、DraftRepository 和 MediaResolver 注入。
 *
 * 并发与生命周期约束：
 * - 所有异步任务都使用调用方传入的 [scope]，随 ScreenModel 生命周期结束而取消。
 * - [dispose] 必须释放每个持有 Job 的局部组件，防止页面销毁后继续写入状态或草稿。
 * - 跨组件回调保持旧顺序：任务进入队列先清草稿；任务成功先写成功状态，再刷新当前历史区域。
 *
 * @param historyRepository 快捷创作历史仓库；单独注入以避免历史区依赖项目管理、生成或计费能力。
 * @param modelCatalogRepository 快捷创作模型目录仓库；单独注入以避免目录加载依赖完整业务仓库能力。
 * @param generationRepository 快捷创作生成仓库；单独注入以避免提交任务依赖历史、项目或灵感能力。
 * @param feePreviewRepository 快捷创作计费预览仓库；单独注入以避免计费流程依赖生成、历史或项目能力。
 * @param inspirationRepository 快捷创作灵感仓库；单独注入以避免模板区域依赖历史、项目或生成能力。
 * @param mediaUploadRepository 快捷创作媒体上传仓库；单独注入以避免上传流程依赖生成、计费或历史能力。
 * @param projectRepository 快捷创作项目仓库；单独注入以避免项目列表和变更动作依赖历史或生成能力。
 * @param mediaResolver 平台媒体读取边界，用于上传前读取用户选择的本地 URI。
 * @param draftRepository 快捷创作草稿领域仓库，只保存可恢复编辑草稿快照。
 * @param scope 页面生命周期协程作用域，所有局部组件的 Job 都绑定到该作用域。
 * @param uiState 页面唯一状态容器，由各局部组件按职责更新。
 * @param ioDispatcher 媒体字节读取使用的调度器；生产环境传 IO，测试环境可传测试调度器。
 */
internal class QuickCreateCoordinator(
    private val historyRepository: QuickCreationTaskHistoryRepository,
    private val mediaResolver: MediaResolver,
    private val draftRepository: QuickCreateDraftRepository,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
    private val ioDispatcher: CoroutineDispatcher,
    private val modelCatalogRepository: QuickCreationModelCatalogRepository,
    private val generationRepository: QuickCreationGenerationRepository,
    private val feePreviewRepository: QuickCreationFeePreviewRepository,
    private val inspirationRepository: QuickCreationInspirationRepository,
    private val mediaUploadRepository: QuickCreationMediaUploadRepository,
    private val projectRepository: QuickCreationProjectRepository,
) {
    private val draftStateHolder = QuickCreateDraftStateHolder(
        draftRepository = draftRepository,
        scope = scope,
        stateProvider = { uiState.value },
        updateState = { reducer -> uiState.update(reducer) },
    )
    private val generationRequestFactory = QuickCreateGenerationRequestFactory()
    private val feePreviewInteractor = QuickCreateFeePreviewInteractor(
        feePreviewRepository = feePreviewRepository,
        generationRequestFactory = generationRequestFactory,
        scope = scope,
        uiState = uiState,
    )
    private val modelCatalogInteractor = QuickCreateModelCatalogInteractor(
        modelCatalogRepository = modelCatalogRepository,
        scope = scope,
        uiState = uiState,
        onFeePreviewRequired = { scheduleFeePreview() },
    )
    private val mediaUploadCoordinator = QuickCreateMediaUploadCoordinator(
        mediaUploadRepository = mediaUploadRepository,
        mediaResolver = MediaResolverQuickCreateMediaResolver(mediaResolver),
        generationRequestFactory = generationRequestFactory,
        scope = scope,
        uiState = uiState,
        ioDispatcher = ioDispatcher,
        onFeePreviewRequired = { scheduleFeePreview() },
    )
    private val historyStateHolder = QuickCreateHistoryStateHolder(
        historyRepository = historyRepository,
        scope = scope,
        uiState = uiState,
    )
    private val taskPollingController = QuickCreateTaskPollingController(
        uiState = uiState,
        onTaskQueued = { clearDraft() },
        onTaskSucceeded = { refreshCurrentHistoryArea() },
    )
    private val generationInteractor = QuickCreateGenerationInteractor(
        generationRepository = generationRepository,
        generationRequestFactory = generationRequestFactory,
        feePreviewInteractor = feePreviewInteractor,
        mediaUploadCoordinator = mediaUploadCoordinator,
        taskPollingController = taskPollingController,
        scope = scope,
        uiState = uiState,
    )
    private val inspirationStateHolder = QuickCreateInspirationStateHolder(
        inspirationRepository = inspirationRepository,
        scope = scope,
        uiState = uiState,
        onTemplateApplied = { scheduleFeePreview() },
    )
    private val projectStateHolder = QuickCreateProjectStateHolder(
        projectRepository = projectRepository,
        scope = scope,
        uiState = uiState,
        onSelectedProjectDeleted = { historyStateHolder.loadRecentHistory() },
    )
    private val editorStateHolder = QuickCreateEditorStateHolder(
        uiState = uiState,
        onFeePreviewRequired = { scheduleFeePreview() },
        onDraftChanged = { autoSaveDraft() },
        onInspirationRequired = { inspirationStateHolder.loadInspiration() },
    )

    /**
     * 启动快捷创作页面的初始数据加载。
     *
     * 初始化顺序保持与迁移前一致：先检查草稿，再加载服务模型、最近历史和项目列表。
     * 这些请求彼此独立，由各 StateHolder 内部使用页面作用域启动。
     */
    fun initialize() {
        checkForDraft()
        loadServiceModels()
        loadQuickCreationHistory()
        loadQuickCreationProjects()
    }

    /**
     * 释放页面级长生命周期任务。
     *
     * 该方法由 ScreenModel 的 `onDispose` 调用；它集中释放生成轮询、历史轮询、计费防抖、
     * 媒体上传读取和草稿自动保存，防止页面离开后继续回写状态。
     */
    fun dispose() {
        generationInteractor.dispose()
        historyStateHolder.dispose()
        feePreviewInteractor.dispose()
        mediaUploadCoordinator.dispose()
        draftStateHolder.dispose()
    }

    /**
     * 重新加载服务端模型目录。
     *
     * 初始化和用户手动刷新都会复用该入口；目录合并、选中模型规范化和默认参数回填由
     * [QuickCreateModelCatalogInteractor] 处理，完成后再统一触发价格预览。
     */
    fun loadServiceModels() {
        modelCatalogInteractor.loadServiceModels()
    }

    /**
     * 加载最近快捷创作历史。
     *
     * 该入口只处理未选中项目时的最近历史列表；如果用户已经选中项目，应通过 [selectProject]
     * 或 [refreshCurrentHistoryArea] 加载项目任务，避免两个历史来源互相覆盖。
     */
    fun loadQuickCreationHistory() {
        historyStateHolder.loadRecentHistory()
    }

    /**
     * 加载快捷创作项目列表首页。
     *
     * 项目列表用于筛选历史和保存生成结果归属；分页、去重和错误状态由 [QuickCreateProjectStateHolder] 维护。
     */
    fun loadQuickCreationProjects() {
        projectStateHolder.loadProjects()
    }

    /**
     * 加载更多快捷创作项目。
     *
     * 重复请求、无更多页和请求错误都由项目 StateHolder 处理，Coordinator 只保留页面动作入口。
     */
    fun loadMoreQuickCreationProjects() {
        projectStateHolder.loadMoreProjects()
    }

    /**
     * 加载更多当前历史区域。
     *
     * 当前历史区域可能是最近历史，也可能是某个项目的任务列表；具体分支由历史 StateHolder 根据 UiState 判断。
     */
    fun loadMoreQuickCreationHistory() {
        historyStateHolder.loadMoreHistory()
    }

    /**
     * 选择项目并切换历史区域到该项目任务。
     *
     * @param projectId 项目稳定标识，空字符串或当前已选项目会被历史 StateHolder 忽略。
     */
    fun selectProject(projectId: String) {
        historyStateHolder.selectProject(projectId)
    }

    private fun refreshCurrentHistoryArea() {
        historyStateHolder.refreshCurrentHistoryArea()
    }

    /**
     * 清除当前项目筛选并恢复最近历史。
     */
    fun clearSelectedProject() {
        historyStateHolder.clearSelectedProject()
    }

    /**
     * 切换项目置顶状态。
     *
     * @param projectId 需要置顶或取消置顶的项目稳定标识。
     */
    fun toggleProjectPin(projectId: String) {
        projectStateHolder.toggleProjectPin(projectId)
    }

    /**
     * 创建快捷创作项目。
     *
     * @param name 用户输入的项目名称；空名称会被项目 StateHolder 忽略。
     */
    fun createProject(name: String) {
        projectStateHolder.createProject(name)
    }

    /**
     * 重命名快捷创作项目。
     *
     * @param projectId 需要重命名的项目稳定标识。
     * @param name 用户输入的新项目名称；空名称不会提交到仓库。
     */
    fun renameProject(projectId: String, name: String) {
        projectStateHolder.renameProject(projectId, name)
    }

    /**
     * 删除快捷创作项目。
     *
     * 删除当前选中项目后必须恢复最近历史，这个跨组件回调由项目 StateHolder 的 `onSelectedProjectDeleted` 触发。
     */
    fun deleteProject(projectId: String) {
        projectStateHolder.deleteProject(projectId)
    }

    /**
     * 打开项目详情。
     *
     * @param projectId 要查看详情的项目稳定标识。
     */
    fun selectProjectDetail(projectId: String) {
        projectStateHolder.selectProjectDetail(projectId)
    }

    /**
     * 关闭项目详情弹层。
     */
    fun dismissProjectDetail() {
        projectStateHolder.dismissProjectDetail()
    }

    /**
     * 取消历史中的远端任务。
     *
     * @param taskId 服务端任务标识；取消成功后历史 StateHolder 会刷新当前历史来源。
     */
    fun cancelHistoryTask(taskId: String) {
        historyStateHolder.cancelHistoryTask(taskId)
    }

    /**
     * 选择历史输出并加载输出详情。
     *
     * @param outputId 历史输出项标识，来源于历史列表中的 outputList。
     */
    fun selectHistoryOutput(outputId: String) {
        historyStateHolder.selectHistoryOutput(outputId)
    }

    /**
     * 关闭历史输出详情。
     */
    fun dismissHistoryDetail() {
        historyStateHolder.dismissHistoryDetail()
    }

    /**
     * 检查本地是否存在可恢复草稿。
     */
    fun checkForDraft() {
        draftStateHolder.checkForDraft()
    }

    /**
     * 恢复本地草稿到编辑区。
     *
     * 草稿模块只返回最小恢复结果，编辑器 StateHolder 负责写回 Tab 和 Prompt；
     * Coordinator 只保留跨组件编排，避免草稿模块了解计费流程。
     */
    fun restoreDraft() {
        val restore = draftStateHolder.restoreDraft() ?: return
        editorStateHolder.applyDraftRestore(restore)
        scheduleFeePreview()
    }

    /**
     * 放弃当前可恢复草稿。
     */
    fun discardDraft() {
        draftStateHolder.discardDraft()
    }

    private fun clearDraft() {
        draftStateHolder.clearDraft()
    }

    /**
     * 切换快捷创作页面的一级模式。
     *
     * UI 仍通过 ScreenModel 发送用户动作；具体模式状态、弹层关闭和首次灵感加载触发由编辑器状态持有者处理。
     */
    fun switchMode(mode: QuickCreateMode) {
        editorStateHolder.switchMode(mode)
    }

    /**
     * 加载灵感模板首页。
     */
    fun loadInspiration() {
        inspirationStateHolder.loadInspiration()
    }

    /**
     * 加载更多灵感模板。
     */
    fun loadMoreInspirationTemplates() {
        inspirationStateHolder.loadMoreTemplates()
    }

    /**
     * 应用指定灵感模板。
     *
     * 模板成功写入编辑状态后才触发计费预览；模板加载失败不应覆盖当前编辑状态。
     */
    fun applyInspirationTemplate(templateId: String) {
        inspirationStateHolder.applyTemplate(templateId)
    }

    /**
     * 切换图片 / 视频编辑 Tab。
     *
     * 切换后会刷新当前展示估价、清理旧 Tab 的计费预览状态，并触发草稿保存，避免恢复草稿时 Tab 信息滞后。
     */
    fun switchTab(tab: QuickCreateTab) {
        editorStateHolder.switchTab(tab)
    }

    /**
     * 更新图片 Prompt。
     *
     * Prompt 是生成、计费预览和草稿恢复的共同输入；实际状态更新和副作用调度由编辑器状态持有者完成。
     */
    fun updateImagePrompt(prompt: String) {
        editorStateHolder.updateImagePrompt(prompt)
    }

    /**
     * 更新视频 Prompt。
     *
     * 与图片 Prompt 保持同一套计费预览和草稿自动保存规则，确保用户切换 Tab 后仍能恢复输入。
     */
    fun updateVideoPrompt(prompt: String) {
        editorStateHolder.updateVideoPrompt(prompt)
    }

    private fun autoSaveDraft() {
        draftStateHolder.autoSaveDraft()
    }

    /**
     * 打开模型选择弹层。
     *
     * 弹层互斥状态由编辑器状态持有者维护，避免 Coordinator 直接操作页面展示细节。
     */
    fun showModelPickerSheet() {
        editorStateHolder.showModelPickerSheet()
    }

    /**
     * 打开参数调节弹层。
     *
     * 该入口只改变页面弹层状态，不触发计费、草稿或远端请求。
     */
    fun showParamsSheet() {
        editorStateHolder.showParamsSheet()
    }

    /**
     * 关闭当前模型或参数弹层。
     *
     * 关闭弹层不修改已选择模型或参数，避免用户误触遮罩时丢失编辑状态。
     */
    fun closeActiveSheet() {
        editorStateHolder.closeActiveSheet()
    }

    /**
     * 切换图片本地模型并重置本地图片参数。
     *
     * 本方法不处理服务端模型目录；它只影响本地兜底模型、比例、分辨率、质量和估价。
     */
    fun updateImageModel(model: ImageModel) {
        editorStateHolder.updateImageModel(model)
    }

    /**
     * 切换图片服务模型。
     *
     * 该入口保持给 UI 调用；模型身份校验、默认服务字段重置和价格预览触发由模型目录协调器完成。
     */
    fun updateImageServiceModel(model: QuickCreationServiceModel) {
        modelCatalogInteractor.updateImageServiceModel(model)
    }

    /**
     * 按 Presentation UI 模型身份键切换图片服务模型。
     *
     * 该入口供模型选择面板使用；UI 不需要回传完整 Domain 模型，目录合法性由
     * [QuickCreateModelCatalogInteractor] 统一校验。
     */
    fun updateImageServiceModel(identityKey: String) {
        modelCatalogInteractor.updateImageServiceModel(identityKey)
    }

    /**
     * 切换视频服务模型。
     *
     * 外部传入的模型必须能匹配当前视频目录，否则会被忽略；这样可以避免模型选择弹层的旧对象污染生成参数。
     */
    fun updateVideoServiceModel(model: QuickCreationServiceModel) {
        modelCatalogInteractor.updateVideoServiceModel(model)
    }

    /**
     * 按 Presentation UI 模型身份键切换视频服务模型。
     *
     * 过期、空白或跨类别 key 会被下游忽略，避免 UI 层旧状态污染当前生成参数。
     */
    fun updateVideoServiceModel(identityKey: String) {
        modelCatalogInteractor.updateVideoServiceModel(identityKey)
    }

    /**
     * 更新图片服务字段参数。
     *
     * 字段合法性和是否需要价格预览由模型目录协调器判断，Coordinator 只保留统一用户行为入口。
     */
    fun updateImageServiceParam(paramKey: String, value: String) {
        modelCatalogInteractor.updateImageServiceParam(paramKey, value)
    }

    /**
     * 更新视频服务字段参数。
     *
     * 与图片字段保持同一套校验和预览触发规则，避免隐藏字段变化造成无效计费请求。
     */
    fun updateVideoServiceParam(paramKey: String, value: String) {
        modelCatalogInteractor.updateVideoServiceParam(paramKey, value)
    }

    /**
     * 切换视频本地模型并重置本地视频参数。
     *
     * 新模型不支持的真实模式或生成音频开关会被关闭，防止请求参数超过模型能力。
     */
    fun updateVideoModel(model: VideoModel) {
        editorStateHolder.updateVideoModel(model)
    }

    /**
     * 更新图片比例。
     *
     * 仅当当前图片模型支持目标比例时才写入状态；非法输入会被忽略并保留原参数。
     */
    fun updateImageAspectRatio(ratio: ImageAspectRatio) {
        editorStateHolder.updateImageAspectRatio(ratio)
    }

    /**
     * 更新图片分辨率。
     *
     * 分辨率变化会影响本地估价和远端计费预览，因此合法变更会统一触发价格刷新。
     */
    fun updateImageResolution(res: ImageResolution) {
        editorStateHolder.updateImageResolution(res)
    }

    /**
     * 更新图片质量档位。
     *
     * 只有当前图片模型支持的质量才会被接受，避免生成请求带出无效质量参数。
     */
    fun updateImageQuality(quality: ImageQuality) {
        editorStateHolder.updateImageQuality(quality)
    }

    /**
     * 更新图片生成数量。
     *
     * 当前只接受服务端支持的固定数量集合；非法数量不会覆盖上一次有效选择。
     */
    fun updateImageCount(count: Int) {
        editorStateHolder.updateImageCount(count)
    }

    /**
     * 更新图片随机种子。
     *
     * 负数会被当作清空种子处理，最终请求不提交 seed。
     */
    fun updateImageSeed(seed: Int?) {
        editorStateHolder.updateImageSeed(seed)
    }

    /**
     * 更新视频比例。
     *
     * 仅允许当前视频模型支持的比例，保证 UI 状态与生成请求能力保持一致。
     */
    fun updateVideoAspectRatio(ratio: VideoAspectRatio) {
        editorStateHolder.updateVideoAspectRatio(ratio)
    }

    /**
     * 更新视频分辨率。
     *
     * 分辨率影响本地估价和计费预览，合法变更会触发后续价格刷新。
     */
    fun updateVideoResolution(res: VideoResolution) {
        editorStateHolder.updateVideoResolution(res)
    }

    /**
     * 更新视频时长。
     *
     * 只有当前模型支持的时长才会生效，避免提交服务端无法处理的时长参数。
     */
    fun updateVideoDuration(duration: VideoDuration) {
        editorStateHolder.updateVideoDuration(duration)
    }

    /**
     * 更新视频生成数量。
     *
     * 非法数量会被忽略，防止一次提交超过视频生成接口支持的数量范围。
     */
    fun updateVideoCount(count: Int) {
        editorStateHolder.updateVideoCount(count)
    }

    /**
     * 更新视频随机种子。
     *
     * `null` 表示交给服务端随机；负数会被规范化为 `null`。
     */
    fun updateVideoSeed(seed: Int?) {
        editorStateHolder.updateVideoSeed(seed)
    }

    /**
     * 切换视频真实模式。
     *
     * 当前模型不支持真实模式时该动作会被忽略，避免 UI 开关与生成请求不一致。
     */
    fun toggleRealisticMode() {
        editorStateHolder.toggleRealisticMode()
    }

    /**
     * 切换视频生成音频开关。
     *
     * 该开关会影响视频价格；只有当前模型支持音频生成时才允许改变。
     */
    fun toggleGenerateAudio() {
        editorStateHolder.toggleGenerateAudio()
    }

    /**
     * 添加图片 Tab 的全局参考图片。
     *
     * @param uriString 用户从平台文件选择器返回的本地 URI 字符串；空字符串由上传协调器忽略。
     */
    fun pickImageReference(uriString: String) {
        mediaUploadCoordinator.addGlobalMediaReference(uriString, QuickCreateMediaType.IMAGE)
    }

    /**
     * 添加视频 Tab 的全局参考视频。
     *
     * @param uriString 用户从平台文件选择器返回的本地 URI 字符串；空字符串由上传协调器忽略。
     */
    fun pickVideoReference(uriString: String) {
        mediaUploadCoordinator.addGlobalMediaReference(uriString, QuickCreateMediaType.VIDEO)
    }

    /**
     * 添加视频 Tab 的全局参考音频。
     *
     * @param uriString 用户从平台文件选择器返回的本地 URI 字符串；空字符串由上传协调器忽略。
     */
    fun pickAudioReference(uriString: String) {
        mediaUploadCoordinator.addGlobalMediaReference(uriString, QuickCreateMediaType.AUDIO)
    }

    /**
     * 为动态服务字段添加图片素材。
     *
     * @param uriString 用户选择的本地图片 URI；为空时说明没有有效选择。
     * @param fieldParamKey 服务字段的参数名，必须非空；用于后续把上传结果映射到对应 quickCreationListParams。
     */
    fun pickImageReferenceForField(uriString: String, fieldParamKey: String) {
        mediaUploadCoordinator.addFieldMediaReference(uriString, QuickCreateMediaType.IMAGE, fieldParamKey)
    }

    /**
     * 为动态服务字段添加视频素材。
     *
     * @param uriString 用户选择的本地视频 URI；为空时说明没有有效选择。
     * @param fieldParamKey 服务字段的参数名，必须非空；隐藏字段或未激活 child 字段后续不会参与计费和提交。
     */
    fun pickVideoReferenceForField(uriString: String, fieldParamKey: String) {
        mediaUploadCoordinator.addFieldMediaReference(uriString, QuickCreateMediaType.VIDEO, fieldParamKey)
    }

    /**
     * 为动态服务字段添加音频素材。
     *
     * @param uriString 用户选择的本地音频 URI；为空时说明没有有效选择。
     * @param fieldParamKey 服务字段的参数名，必须非空；上传完成后由请求构建器决定是否属于当前激活字段。
     */
    fun pickAudioReferenceForField(uriString: String, fieldParamKey: String) {
        mediaUploadCoordinator.addFieldMediaReference(uriString, QuickCreateMediaType.AUDIO, fieldParamKey)
    }

    /**
     * 删除指定媒体引用。
     *
     * @param id 媒体引用在页面状态中的稳定标识，来源于添加素材时生成的 id 或灵感模板导入的 id。
     */
    fun removeMediaReference(id: String) {
        mediaUploadCoordinator.removeMediaReference(id)
    }

    /**
     * 清除页面当前错误提示。
     *
     * 错误来源可能属于上传、计费、生成或历史流程；关闭提示只清理展示字段，不改变具体业务状态。
     */
    fun dismissError() {
        editorStateHolder.dismissError()
    }

    /**
     * 清空当前生成结果并恢复任务展示区域。
     *
     * 该入口保留给 UI 操作调用；具体状态恢复逻辑由生成 Interactor 维护，确保结果清理与生成状态映射保持同一边界。
     */
    fun clearResults() {
        generationInteractor.clearResults()
    }

    /**
     * 提交当前快捷创作任务。
     *
     * 方法会先按计费预览状态拦截不可提交场景，再校验服务字段，最后等待当前请求相关的上传素材完成。
     * 上传失败或超时会恢复为空闲状态并写入页面错误，避免在素材未就绪时创建远程生成任务。
     */
    fun generate() {
        generationInteractor.generate()
    }

    private fun scheduleFeePreview() {
        // 计费预览的防抖、旧响应隔离和价格错误映射已下沉到 Interactor；
        // Coordinator 只保留这个统一入口，避免分散在各个参数变更回调中的调用点发生行为漂移。
        feePreviewInteractor.schedule()
    }

    /**
     * 把 composeApp 平台媒体读取能力适配为 QuickCreate Presentation 模块的上传端口。
     *
     * Android/iOS 的 URI 权限、文件选择器和安全作用域读取仍由应用平台层负责；
     * feature presentation 只接收平台无关的字节、展示名和文件大小，避免反向依赖 composeApp。
     */
    private class MediaResolverQuickCreateMediaResolver(
        private val mediaResolver: MediaResolver,
    ) : QuickCreateMediaResolver {
        override fun readBytes(uri: String): ByteArray = mediaResolver.readBytes(uri)

        override fun getDisplayName(uri: String): String? = mediaResolver.getDisplayName(uri)

        override fun getFileSizeBytes(uri: String): Long = mediaResolver.getFileSizeBytes(uri)
    }
}
