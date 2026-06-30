package com.runninghub.feature.quickcreate.presentation.state

/**
 * 快捷创作页面的顶层创作类型。
 *
 * 该枚举属于 Presentation 状态边界，用于区分当前编辑的是图片还是视频任务。
 */
enum class QuickCreateTab {
    /** 图片创作 Tab，使用图片模型、图片参数和图片上传字段。 */
    IMAGE,

    /** 视频创作 Tab，使用视频模型、视频参数和视频上传字段。 */
    VIDEO,
}

/**
 * 快捷创作页面的主模式。
 *
 * 当前产品入口只保留创作模式。模式状态仍放在 Presentation 模块中，
 * 避免 composeApp 直接承载具体业务页面的状态枚举，也为历史草稿恢复保留稳定字段。
 */
enum class QuickCreateMode {
    /** 正常创作模式，用户可以编辑参数并提交生成。 */
    CREATION,
}

/**
 * 快捷创作导航控件的稳定文案键。
 *
 * Presentation 只暴露页面状态与文案语义，不直接保存中文字符串或 Compose Resource。
 * composeApp 根据这些稳定键映射最终展示文案，避免独立 Presentation 模块承担本地化职责。
 */
sealed interface QuickCreateNavigationLabel {
    /** 图片创作 Tab 的展示语义。 */
    data object ImageTab : QuickCreateNavigationLabel

    /** 视频创作 Tab 的展示语义。 */
    data object VideoTab : QuickCreateNavigationLabel

    /** 正常创作模式的展示语义。 */
    data object CreationMode : QuickCreateNavigationLabel
}

/**
 * 当前创作 Tab 对应的稳定文案键。
 *
 * @return 图片或视频 Tab 的文案语义，由 composeApp 负责映射为最终本地化文案。
 */
val QuickCreateTab.navigationLabel: QuickCreateNavigationLabel
    get() = when (this) {
        QuickCreateTab.IMAGE -> QuickCreateNavigationLabel.ImageTab
        QuickCreateTab.VIDEO -> QuickCreateNavigationLabel.VideoTab
    }

/**
 * 当前页面模式对应的稳定文案键。
 *
 * @return 创作模式的文案语义，由 composeApp 负责映射为最终本地化文案。
 */
val QuickCreateMode.navigationLabel: QuickCreateNavigationLabel
    get() = when (this) {
        QuickCreateMode.CREATION -> QuickCreateNavigationLabel.CreationMode
    }

/**
 * 快捷创作编辑器中可打开的底部面板。
 *
 * Sheet 类型只表达当前 UI 面板入口，不持有面板内容或业务请求状态。
 */
enum class QuickCreateSheet {
    /** 服务模型选择面板。 */
    MODEL_PICKER,

    /** 当前模型的参数编辑面板。 */
    PARAMS,

    /** 正式提交前的生成价格确认面板。 */
    GENERATION_CONFIRM,
}

/**
 * 描述词允许输入的最大字符数。
 *
 * 该限制同时用于输入控件、状态派生和生成请求校验，统一放在 Presentation 状态模块，
 * 防止 UI 与请求构建层出现不同的长度边界。
 */
const val MAX_PROMPT_CHARS = 500

/**
 * 描述词接近上限时开始展示警告的字符数。
 *
 * 该阈值只影响 Presentation 提示，不会阻止输入或提交；真正的阻断边界由 [MAX_PROMPT_CHARS] 控制。
 */
const val MAX_VISIBLE_CHARS_WARN = 400
