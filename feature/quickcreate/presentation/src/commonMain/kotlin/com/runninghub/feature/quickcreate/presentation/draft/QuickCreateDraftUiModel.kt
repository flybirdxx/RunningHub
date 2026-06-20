package com.runninghub.feature.quickcreate.presentation.draft

import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState

/**
 * 快捷创作页面可恢复草稿的数据快照。
 *
 * 该模型属于 QuickCreate Presentation 契约，只保存恢复编辑所需的最小字段：
 * 当前 Tab、图片提示词和视频提示词。它不保存上传素材、计费结果或任务状态，
 * 避免恢复草稿时误复用已经失效的远程资源和价格。
 *
 * @property currentTab 保存草稿时用户所在的创作 Tab，使用 [QuickCreateTab.name] 的字符串值。
 * 默认值 `"IMAGE"` 表示没有历史 Tab 或旧草稿缺失该字段时回到图片创作；未知值会在恢复时按提示词内容降级。
 * @property imagePrompt 图片创作提示词，来源于用户在图片 Tab 的输入。
 * 空字符串表示图片提示词没有内容，此时不能单独恢复为图片草稿；该字段只在当前草稿生命周期内使用。
 * @property videoPrompt 视频创作提示词，来源于用户在视频 Tab 的输入。
 * 空字符串表示视频提示词没有内容，此时不能单独恢复为视频草稿；该字段不包含上传素材或远程任务信息。
 */
data class DraftData(
    val currentTab: String = "IMAGE",
    val imagePrompt: String = "",
    val videoPrompt: String = "",
)

/**
 * 草稿是否包含至少一个可恢复提示词。
 *
 * `true` 表示图片或视频提示词中至少有一个非空白内容，草稿可以展示恢复入口；
 * `false` 表示草稿没有可恢复业务内容，应由调用方清理本地持久化数据。
 */
val DraftData.hasPromptContent: Boolean
    get() = imagePrompt.isNotBlank() || videoPrompt.isNotBlank()

/**
 * 根据草稿保存的 Tab 和提示词内容推导实际可恢复 Tab。
 *
 * 旧草稿可能保存了无效 Tab 或只保留另一侧提示词，因此恢复时优先使用非空提示词，
 * 确保页面不会切到一个没有内容的创作类型。
 */
val DraftData.restorableTab: QuickCreateTab
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
 * 该函数位于 Presentation 层，因为返回值是页面直接展示的中文摘要。
 * 它根据可恢复 Tab 计算字数，避免视频 Tab 无内容时展示一个无法恢复的视频草稿。
 *
 * @return 面向快捷创作恢复入口展示的短摘要，例如 `上次草稿 · 图片 · 12 字`。
 */
fun DraftData.resumeSummaryText(): String {
    val tab = restorableTab
    val tabLabel = if (tab == QuickCreateTab.VIDEO) "视频" else "图片"
    val promptLength = if (tab == QuickCreateTab.VIDEO) videoPrompt.length else imagePrompt.length
    return "上次草稿 · $tabLabel · $promptLength 字"
}

/**
 * 草稿恢复到编辑区时需要应用的最小状态。
 *
 * StateHolder 只负责从草稿推导恢复结果，真正写入 [QuickCreateUiState] 的动作由调用方完成，
 * 这样草稿模块不会了解计费、模型参数或任务提交等更大的页面编排。
 *
 * @property tab 恢复后应切换到的创作 Tab，由草稿保存的 Tab 和非空提示词共同决定。
 * @property imagePrompt 需要写回图片编辑区的提示词；空字符串表示图片编辑区应保持无提示词内容。
 * @property videoPrompt 需要写回视频编辑区的提示词；空字符串表示视频编辑区应保持无提示词内容。
 */
data class QuickCreateDraftRestore(
    val tab: QuickCreateTab,
    val imagePrompt: String,
    val videoPrompt: String,
)
