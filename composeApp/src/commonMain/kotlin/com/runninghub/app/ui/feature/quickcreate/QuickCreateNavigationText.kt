package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.runtime.Composable
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateNavigationLabel
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_mode_creation
import runninghub.composeapp.generated.resources.quick_create_mode_inspiration
import runninghub.composeapp.generated.resources.quick_create_tab_image
import runninghub.composeapp.generated.resources.quick_create_tab_video

/**
 * 将 QuickCreate Presentation 暴露的导航文案键映射为 Compose Resources 文案。
 *
 * Presentation 模块只负责区分图片/视频 Tab 与创作/灵感模式；最终中文文案、
 * 后续多语言和资源占位都留在应用壳，避免独立 Presentation 模块依赖 Compose 资源。
 */
@Composable
internal fun quickCreateNavigationText(label: QuickCreateNavigationLabel): String =
    when (label) {
        QuickCreateNavigationLabel.CreationMode -> stringResource(Res.string.quick_create_mode_creation)
        QuickCreateNavigationLabel.ImageTab -> stringResource(Res.string.quick_create_tab_image)
        QuickCreateNavigationLabel.InspirationMode -> stringResource(Res.string.quick_create_mode_inspiration)
        QuickCreateNavigationLabel.VideoTab -> stringResource(Res.string.quick_create_tab_video)
    }
