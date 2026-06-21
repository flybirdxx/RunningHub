package com.runninghub.feature.community.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 社区工具页中的单个入口配置。
 *
 * 该模型属于 Community Presentation 层，表达 UI 可渲染的工具入口和后续导航标识；
 * 它不包含 Compose 图标、颜色或 Voyager Screen，避免 Presentation 契约反向依赖应用壳。
 *
 * @property id 工具入口稳定 ID，来源于客户端内置目录。
 * 该值用于 LazyGrid key、图标配色和测试去重；不能为空，且同一目录内必须唯一。
 * @property title 用户可见标题，来源于客户端内置文案。
 * 当前仍为硬编码迁移期文案；空字符串不允许进入 UI。
 * @property description 用户可见说明，来源于客户端内置文案。
 * 用于工具卡片摘要展示；空字符串表示配置错误，不应作为正常状态。
 * @property iconName 平台无关的图标语义名，来源于客户端内置目录。
 * Compose 层负责把该名称映射为具体 [ImageVector]，未知名称应降级为通用工具图标。
 * @property route 应用壳后续用于导航的稳定路由标识，来源于客户端内置目录。
 * 当前不直接构造 Voyager Screen；空字符串表示该工具暂不可导航。
 */
data class CommunityTool(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val route: String,
)

/**
 * 社区工具页的完整可渲染状态。
 *
 * 该状态只承载静态工具入口目录和加载占位，不保存网络结果、会话凭据或平台资源。
 *
 * @property tools 当前可展示的工具入口列表，来源于 [CommunityStateHolder] 的内置目录。
 * 顺序有业务含义，表示页面默认展示和键盘/读屏遍历顺序；空集合表示目录尚未加载或配置为空。
 * @property isLoading 是否正在准备工具入口目录。
 * `true` 表示 UI 展示加载态；`false` 表示静态目录已经可展示。当前目录为本地同步构建，默认恒为 `false`。
 */
data class CommunityUiState(
    val tools: List<CommunityTool> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * 持有社区工具页的静态工具目录状态。
 *
 * 本类位于 Community Presentation 层，不依赖 Compose、Voyager、Data 或平台 SDK。
 * 应用壳负责根据 [CommunityTool.route] 决定实际导航目标；这样工具目录可以先从 `composeApp`
 * 单体中迁出，而不会提前固定尚未完成的工具路由实现。
 */
class CommunityStateHolder {
    private val _uiState = MutableStateFlow(
        CommunityUiState(
            tools = defaultTools(),
        ),
    )

    /**
     * 社区工具页只读状态流。
     *
     * UI 只收集该状态并通过点击回调把 [CommunityTool] 交回应用壳，不直接修改内部 [MutableStateFlow]。
     */
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private fun defaultTools(): List<CommunityTool> = listOf(
        CommunityTool(
            id = "audio_gen",
            title = "音频生成",
            description = "文本转语音、AI 音乐生成与音频处理",
            iconName = "audiotrack",
            route = "audio_generation",
        ),
        CommunityTool(
            id = "steganography",
            title = "隐写术解码",
            description = "从图片中提取隐藏的秘密数据",
            iconName = "visibility",
            route = "secret_decode",
        ),
        CommunityTool(
            id = "ui_inspector",
            title = "UI 检视器",
            description = "查看设备屏幕参数与系统信息",
            iconName = "info",
            route = "ui_inspector",
        ),
        CommunityTool(
            id = "color_extract",
            title = "色彩提取",
            description = "从图片中提取配色方案与调色板",
            iconName = "palette",
            route = "color_extract",
        ),
        CommunityTool(
            id = "smart_crop",
            title = "智能裁切",
            description = "自动识别主体智能裁切图片",
            iconName = "crop",
            route = "smart_crop",
        ),
        CommunityTool(
            id = "workflow",
            title = "工作流广场",
            description = "探索和运行社区分享的工作流",
            iconName = "hub",
            route = "workflow_plaza",
        ),
    )
}
