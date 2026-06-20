package com.runninghub.feature.quickcreate.presentation.state

/**
 * 快捷创作页面的顶层创作类型。
 *
 * 该枚举属于 Presentation 状态边界，用于区分当前编辑的是图片还是视频任务。
 *
 * @property displayName 页面 Tab 展示文案。
 */
enum class QuickCreateTab(val displayName: String) {
    /** 图片创作 Tab，使用图片模型、图片参数和图片上传字段。 */
    IMAGE("图片"),

    /** 视频创作 Tab，使用视频模型、视频参数和视频上传字段。 */
    VIDEO("视频"),
}

/**
 * 快捷创作页面的主模式。
 *
 * 创作模式展示编辑器、结果和历史；灵感模式展示模板列表。模式状态放在 Presentation
 * 模块中，避免 composeApp 继续承载具体业务页面的状态枚举。
 *
 * @property displayName 模式切换控件展示文案。
 */
enum class QuickCreateMode(val displayName: String) {
    /** 正常创作模式，用户可以编辑参数并提交生成。 */
    CREATION("创作"),

    /** 灵感模板模式，用户可以浏览模板并一键应用到创作参数。 */
    INSPIRATION("灵感"),
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
