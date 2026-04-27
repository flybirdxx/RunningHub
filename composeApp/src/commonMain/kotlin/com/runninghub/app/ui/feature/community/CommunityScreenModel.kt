package com.runninghub.app.ui.feature.community

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CommunityTool(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val route: String
)

data class CommunityUiState(
    val tools: List<CommunityTool> = emptyList(),
    val isLoading: Boolean = false
)

class CommunityScreenModel : ScreenModel {

    private val _uiState = MutableStateFlow(
        CommunityUiState(
            tools = defaultTools()
        )
    )
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    private fun defaultTools(): List<CommunityTool> = listOf(
        CommunityTool(
            id = "audio_gen",
            title = "音频生成",
            description = "文本转语音、AI 音乐生成与音频处理",
            iconName = "audiotrack",
            route = "audio_generation"
        ),
        CommunityTool(
            id = "steganography",
            title = "隐写术解码",
            description = "从图片中提取隐藏的秘密数据",
            iconName = "visibility",
            route = "secret_decode"
        ),
        CommunityTool(
            id = "ui_inspector",
            title = "UI 检视器",
            description = "查看设备屏幕参数与系统信息",
            iconName = "info",
            route = "ui_inspector"
        ),
        CommunityTool(
            id = "color_extract",
            title = "色彩提取",
            description = "从图片中提取配色方案与调色板",
            iconName = "palette",
            route = "color_extract"
        ),
        CommunityTool(
            id = "smart_crop",
            title = "智能裁切",
            description = "自动识别主体智能裁切图片",
            iconName = "crop",
            route = "smart_crop"
        ),
        CommunityTool(
            id = "workflow",
            title = "工作流广场",
            description = "探索和运行社区分享的工作流",
            iconName = "hub",
            route = "workflow_plaza"
        )
    )
}
