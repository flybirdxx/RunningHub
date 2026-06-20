package com.runninghub.feature.quickcreate.presentation.state

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.presentation.draft.DraftData
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig
import com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryDetailUiItem
import com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryUiItem
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationTagUi
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationTemplateUi
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelUi
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectDetailUiItem
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectUiItem
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus

/**
 * 快捷创作页面的完整可渲染状态。
 *
 * 该状态属于 QuickCreate Presentation 契约，作为 ScreenModel、StateHolder、Interactor 与 UI
 * 之间的单一事实来源。它只保存页面可展示状态、用户输入、领域模型选择和一次性错误；
 * 网络请求、DataStore、平台 URI 权限和远程任务轮询都应由外部协作者通过不可变 `copy` 回写。
 *
 * 状态不变量：
 * - [estimatedCost] 优先展示服务端计费预览成功后的价格；预览不可用或失败时回退到当前配置的本地估算。
 * - [selectedImageServiceModelUi] 与 [selectedImageServiceModel] 应描述同一个图片服务模型。
 * - [selectedVideoServiceModelUi] 与 [selectedVideoServiceModel] 应描述同一个视频服务模型。
 * - [historyItems] 在选中项目时表示项目任务，否则表示最近历史。
 *
 * @property currentMode 当前一级模式。
 * [QuickCreateMode.CREATION] 表示展示创作编辑区；[QuickCreateMode.INSPIRATION] 表示展示灵感模板。
 * @property currentTab 当前创作类型。
 * [QuickCreateTab.IMAGE] 表示图片创作；[QuickCreateTab.VIDEO] 表示视频创作；该值同时决定计费、生成和草稿恢复目标。
 * @property imageConfig 图片编辑区输入和本地参数。
 * 默认对象表示用户尚未输入图片创作内容；其中素材引用只保存跨平台字符串和远程 URL，不持有平台文件对象。
 * @property videoConfig 视频编辑区输入和本地参数。
 * 默认对象表示用户尚未输入视频创作内容；其中时长、分辨率和开关会参与计费预览与生成请求。
 * @property taskStatus 当前页面主生成任务状态。
 * [QuickCreateTaskUiStatus.IDLE] 表示没有提交中的任务；非空闲状态由生成和轮询流程回写。
 * @property statusText 生成、上传或轮询阶段展示的辅助状态文案。
 * `null` 表示当前没有需要固定展示的阶段说明，非空值由 UI 直接展示。
 * @property results 最近一次成功生成或轮询得到的输出结果。
 * 顺序来自生成流程或服务端输出顺序；空集合表示当前没有可展示结果。
 * @property error 等待页面展示的一次性错误提示。
 * `null` 表示没有待展示错误；非空时由 UI 展示后通过对应事件清理，避免重组重复提示。
 * @property estimatedCost 当前生成入口展示的价格，单位为 RunningHub 业务余额或现金金额。
 * `0.0` 表示免费、尚未计算或本地默认估算为零；不允许为负数。
 * @property feePreviewLoading 是否正在执行计费预览请求。
 * `true` 表示生成入口应避免提交并展示等待状态；`false` 表示当前没有进行中的计费预览。
 * @property feePreviewError 计费预览失败或余额不足时的阻塞原因。
 * `null` 表示当前价格可用或尚未触发远程预览；非空时生成流程应先拦截提交。
 * @property activeSheet 当前打开的底部业务弹层。
 * `null` 表示没有弹层；非空值只能同时表示一个模型选择或参数编辑弹层。
 * @property inspirationLoading 是否正在加载灵感标签、模板列表或模板详情。
 * `true` 时灵感区域展示加载态；`false` 表示当前没有灵感请求在途。
 * @property inspirationTags 灵感模板筛选标签。
 * 顺序来自服务端推荐顺序；空集合表示尚未加载、加载失败或当前没有可用标签。
 * @property inspirationTemplates 当前已加载的灵感模板列表。
 * 顺序来自分页追加结果并按模板 ID 去重；空集合表示尚未加载或服务端没有模板。
 * @property inspirationTemplatesLoadingMore 是否正在追加加载灵感模板下一页。
 * `true` 只代表分页追加请求，不代表第一页加载。
 * @property inspirationTemplatesPage 当前灵感模板已加载页码。
 * `0` 表示尚未成功加载任何页面；成功加载第一页后通常为 `1`。
 * @property inspirationTemplatesHasMore 灵感模板是否仍有下一页。
 * `true` 表示可以继续触发加载更多；`false` 表示没有更多或当前分页状态未知。
 * @property historyLoading 是否正在加载历史第一页或当前历史来源的刷新页。
 * `true` 时历史区域展示主加载态；`false` 表示没有主历史请求在途。
 * @property historyLoadingMore 是否正在追加加载历史下一页。
 * `true` 只代表分页追加请求，不应覆盖主加载态。
 * @property historyPage 当前历史来源已加载页码。
 * `0` 表示尚未成功加载任何历史；选中项目变化或清除筛选时会重置。
 * @property historyTotal 当前历史来源的服务端总数。
 * `0` 表示未知或没有历史；不允许为负数。
 * @property historyHasMore 当前历史来源是否仍有下一页。
 * `true` 表示 UI 可以展示加载更多入口；`false` 表示没有更多或分页状态未知。
 * @property historyItems 当前历史来源的任务卡片。
 * 选中项目时表示该项目任务，否则表示最近历史；顺序遵循服务端返回顺序并在分页合并时按任务 ID 去重。
 * @property historyCancellingTaskIds 当前正在取消的历史任务 ID 集合。
 * 元素来源于用户点击取消的任务；空集合表示没有取消请求在途。
 * @property historyDetailLoading 是否正在加载历史输出详情。
 * `true` 时详情弹窗展示加载态；`false` 表示详情请求已结束或未打开。
 * @property selectedHistoryDetail 当前历史输出详情。
 * `null` 表示没有打开详情或详情尚未加载成功；非空时用于详情弹窗展示。
 * @property projectsLoading 是否正在加载项目第一页。
 * `true` 时项目入口展示主加载态；`false` 表示没有项目主请求在途。
 * @property projectsLoadingMore 是否正在追加加载项目下一页。
 * `true` 只代表项目分页追加请求。
 * @property projects 当前已加载的快捷创作项目列表。
 * 顺序来自服务端分页结果并按项目 ID 去重；空集合表示尚未加载、失败或没有项目。
 * @property projectsPage 当前项目列表已加载页码。
 * `0` 表示尚未成功加载项目；成功加载第一页后通常为 `1`。
 * @property projectsHasMore 项目列表是否仍有下一页。
 * `true` 表示可继续加载更多；`false` 表示没有更多或分页状态未知。
 * @property selectedProjectId 当前作为历史筛选条件的项目 ID。
 * `null` 表示历史区域展示最近历史；非空值表示历史区域展示该项目下任务。
 * @property projectTasksLoading 是否正在加载选中项目的任务第一页或刷新页。
 * `true` 只代表项目任务请求，不代表项目列表请求。
 * @property projectPinningIds 当前正在切换置顶状态的项目 ID 集合。
 * 空集合表示没有置顶请求在途；集合用于防止同一项目重复提交。
 * @property projectMutatingIds 当前正在创建、重命名或删除的项目 mutation 标识集合。
 * 创建项目使用固定内部标识，已有项目变更使用项目 ID；空集合表示没有项目变更在途。
 * @property projectDetailLoading 是否正在加载项目详情。
 * `true` 时项目详情弹窗展示加载态；`false` 表示详情请求已结束或未打开。
 * @property selectedProjectDetail 当前项目详情。
 * `null` 表示没有打开详情或详情尚未加载成功；非空时用于项目详情弹窗展示。
 * @property serviceModelsLoading 是否正在加载快捷创作服务模型目录。
 * `true` 时模型选择入口展示加载态；`false` 表示没有模型目录请求在途。
 * @property serviceImageModels 服务端返回的图片快捷创作模型。
 * 顺序保留服务端排序，空集合表示尚未加载、加载失败或没有图片模型。
 * @property serviceVideoModels 服务端返回的视频快捷创作模型。
 * 顺序保留服务端排序，空集合表示尚未加载、加载失败或没有视频模型。
 * @property selectedImageServiceModel 当前选中的图片服务模型领域对象。
 * `null` 表示模型目录尚未加载或没有可选图片模型。
 * @property selectedVideoServiceModel 当前选中的视频服务模型领域对象。
 * `null` 表示模型目录尚未加载或没有可选视频模型。
 * @property serviceImageModelItems 图片模型选择器使用的 UI 项。
 * 顺序与 [serviceImageModels] 对齐；空集合表示没有可展示的图片模型选择项。
 * @property serviceVideoModelItems 视频模型选择器使用的 UI 项。
 * 顺序与 [serviceVideoModels] 对齐；空集合表示没有可展示的视频模型选择项。
 * @property selectedImageServiceModelUi 当前图片模型选择器中的选中项。
 * `null` 表示没有可展示选中项；非空时应与 [selectedImageServiceModel] 指向同一模型。
 * @property selectedVideoServiceModelUi 当前视频模型选择器中的选中项。
 * `null` 表示没有可展示选中项；非空时应与 [selectedVideoServiceModel] 指向同一模型。
 * @property imageServiceParams 图片服务动态字段参数。
 * Key 为服务端字段参数名，Value 为用户输入或默认值；空 Map 表示当前图片模型没有动态字段或尚未初始化。
 * @property videoServiceParams 视频服务动态字段参数。
 * Key 为服务端字段参数名，Value 为用户输入或默认值；空 Map 表示当前视频模型没有动态字段或尚未初始化。
 * @property draftData 可恢复草稿入口的数据。
 * `null` 表示没有可恢复草稿或当前输入已经保存为最新草稿；非空时 UI 可展示恢复入口。
 */
data class QuickCreateUiState(
    val currentMode: QuickCreateMode = QuickCreateMode.CREATION,
    val currentTab: QuickCreateTab = QuickCreateTab.IMAGE,
    val imageConfig: ImageConfig = ImageConfig(),
    val videoConfig: VideoConfig = VideoConfig(),
    val taskStatus: QuickCreateTaskUiStatus = QuickCreateTaskUiStatus.IDLE,
    val statusText: String? = null,
    val results: List<QuickCreateResultUi> = emptyList(),
    val error: String? = null,
    val estimatedCost: Double = 0.0,
    val feePreviewLoading: Boolean = false,
    val feePreviewError: String? = null,
    val activeSheet: QuickCreateSheet? = null,
    val inspirationLoading: Boolean = false,
    val inspirationTags: List<QuickCreateInspirationTagUi> = emptyList(),
    val inspirationTemplates: List<QuickCreateInspirationTemplateUi> = emptyList(),
    val inspirationTemplatesLoadingMore: Boolean = false,
    val inspirationTemplatesPage: Int = 0,
    val inspirationTemplatesHasMore: Boolean = false,
    val historyLoading: Boolean = false,
    val historyLoadingMore: Boolean = false,
    val historyPage: Int = 0,
    val historyTotal: Int = 0,
    val historyHasMore: Boolean = false,
    val historyItems: List<QuickCreateHistoryUiItem> = emptyList(),
    val historyCancellingTaskIds: Set<String> = emptySet(),
    val historyDetailLoading: Boolean = false,
    val selectedHistoryDetail: QuickCreateHistoryDetailUiItem? = null,
    val projectsLoading: Boolean = false,
    val projectsLoadingMore: Boolean = false,
    val projects: List<QuickCreateProjectUiItem> = emptyList(),
    val projectsPage: Int = 0,
    val projectsHasMore: Boolean = false,
    val selectedProjectId: String? = null,
    val projectTasksLoading: Boolean = false,
    val projectPinningIds: Set<String> = emptySet(),
    val projectMutatingIds: Set<String> = emptySet(),
    val projectDetailLoading: Boolean = false,
    val selectedProjectDetail: QuickCreateProjectDetailUiItem? = null,
    val serviceModelsLoading: Boolean = false,
    val serviceImageModels: List<QuickCreationServiceModel> = emptyList(),
    val serviceVideoModels: List<QuickCreationServiceModel> = emptyList(),
    val selectedImageServiceModel: QuickCreationServiceModel? = null,
    val selectedVideoServiceModel: QuickCreationServiceModel? = null,
    val serviceImageModelItems: List<QuickCreateServiceModelUi> = emptyList(),
    val serviceVideoModelItems: List<QuickCreateServiceModelUi> = emptyList(),
    val selectedImageServiceModelUi: QuickCreateServiceModelUi? = null,
    val selectedVideoServiceModelUi: QuickCreateServiceModelUi? = null,
    val imageServiceParams: Map<String, String> = emptyMap(),
    val videoServiceParams: Map<String, String> = emptyMap(),
    val draftData: DraftData? = null,
) {
    /**
     * 当前是否应展示创作输入区。
     *
     * `true` 表示页面处于创作模式；`false` 表示当前展示灵感模板等非编辑主界面。
     */
    val showCreationInput: Boolean
        get() = currentMode == QuickCreateMode.CREATION

    /**
     * 当前是否存在可恢复草稿。
     *
     * `true` 表示 [draftData] 非空且 UI 可以展示恢复入口；`false` 表示没有需要提示用户恢复的草稿。
     */
    val hasDraft: Boolean
        get() = draftData != null
}
