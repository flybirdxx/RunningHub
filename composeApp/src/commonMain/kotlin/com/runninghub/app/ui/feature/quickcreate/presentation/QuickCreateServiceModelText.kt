package com.runninghub.app.ui.feature.quickcreate.presentation

import androidx.compose.runtime.Composable
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelDisplayName
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelGroupTitle
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelSubtitle
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_service_model_other_group
import runninghub.composeapp.generated.resources.quick_create_service_model_parameter_count_format
import runninghub.composeapp.generated.resources.quick_create_service_model_subtitle_separator
import runninghub.composeapp.generated.resources.quick_create_service_model_unnamed

/**
 * 将服务模型标题语义映射为 composeApp 可见文案。
 *
 * 服务端返回的模型名作为运行时数据直接展示；只有 Presentation 标记为缺失名称时，
 * 才读取 Compose Resources 中的本地兜底文案，避免 feature presentation 保存固定中文 UI 字符串。
 */
@Composable
internal fun QuickCreateServiceModelDisplayName.asServiceModelText(): String =
    when (this) {
        QuickCreateServiceModelDisplayName.Unnamed ->
            stringResource(Res.string.quick_create_service_model_unnamed)
        is QuickCreateServiceModelDisplayName.ServerText -> value
    }

/**
 * 将服务模型分组语义映射为 composeApp 可见文案。
 *
 * 服务端分组名保留原值展示；缺失分组时使用资源化默认分组，确保模型选择器分组标题可本地化。
 */
@Composable
internal fun QuickCreateServiceModelGroupTitle.asServiceModelText(): String =
    when (this) {
        QuickCreateServiceModelGroupTitle.Other ->
            stringResource(Res.string.quick_create_service_model_other_group)
        is QuickCreateServiceModelGroupTitle.ServerText -> value
    }

/**
 * 将服务模型副标题语义映射为 composeApp 可见文案。
 *
 * 参数数量格式和分隔符属于本地 UI 文案，由资源层统一控制；服务端分组名仍作为运行时数据拼入。
 */
@Composable
internal fun QuickCreateServiceModelSubtitle.asServiceModelText(): String =
    when (this) {
        QuickCreateServiceModelSubtitle.None -> ""
        is QuickCreateServiceModelSubtitle.ParameterCount ->
            stringResource(Res.string.quick_create_service_model_parameter_count_format, parameterCount)
        is QuickCreateServiceModelSubtitle.GroupAndParameterCount -> listOf(
            groupName,
            stringResource(Res.string.quick_create_service_model_parameter_count_format, parameterCount),
        ).joinToString(stringResource(Res.string.quick_create_service_model_subtitle_separator))
    }
