package com.runninghub.app.ui.feature.quickcreate.presentation.modelselector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.app.ui.theme.DarkOutlineVariant
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.Neutral100
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelUi
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_model_selector_close_content_description
import runninghub.composeapp.generated.resources.quick_create_model_selector_empty
import runninghub.composeapp.generated.resources.quick_create_model_selector_loading
import runninghub.composeapp.generated.resources.quick_create_model_selector_title

/**
 * 展示快捷创作的服务端模型选择面板。
 *
 * 该组件属于 modelselector 子区域，只读取 [QuickCreateUiState] 中已经加载好的模型列表、
 * 当前选中模型和加载状态。模型选择通过回调交给 ScreenModel/ModelCatalogInteractor，
 * 组件自身不发起网络请求，也不写入当前创作配置。
 * 面板标题、加载态、空态和关闭按钮无障碍描述使用 Compose Resources；模型名称、
 * 分组和副标题来自 Presentation 状态，避免 UI 层重新理解服务端模型目录语义。
 *
 * @param visible 是否显示模型选择面板，`true` 时播放底部进入动画。
 * @param isImage 当前是否处于图片创作 tab；`true` 读取图片模型，`false` 读取视频模型。
 * @param uiState 快捷创作页面状态，提供模型列表、选中项和加载态。
 * @param onDismiss 用户关闭面板时触发。
 * @param onImageServiceModelSelected 用户选择图片服务端模型时触发，参数为 UI 模型身份键。
 * @param onVideoServiceModelSelected 用户选择视频服务端模型时触发，参数为 UI 模型身份键。
 * @param modifier 外层调用方用于控制面板定位的修饰符。
 */
@Composable
internal fun QuickCreateModelSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    onDismiss: () -> Unit,
    onImageServiceModelSelected: (String) -> Unit,
    onVideoServiceModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val models = if (isImage) uiState.serviceImageModelItems else uiState.serviceVideoModelItems
    val onSelect = if (isImage) onImageServiceModelSelected else onVideoServiceModelSelected

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = Dimens.RadiusXL, topEnd = Dimens.RadiusXL),
            color = DarkSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = Dimens.SpaceLG),
            ) {
                ModelSheetHeader(
                    title = stringResource(Res.string.quick_create_model_selector_title),
                    onDismiss = onDismiss,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(horizontal = Dimens.SpaceLG),
                ) {
                    when {
                        uiState.serviceModelsLoading -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.SpaceLG),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Primary300,
                                strokeWidth = 2.dp,
                            )
                            Text(
                                stringResource(Res.string.quick_create_model_selector_loading),
                                fontSize = 13.sp,
                                color = Neutral400,
                            )
                        }
                        models.isEmpty() -> Text(
                            stringResource(Res.string.quick_create_model_selector_empty),
                            fontSize = 13.sp,
                            color = Neutral500,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = Dimens.SpaceLG),
                        )
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                            // 保留服务端分组顺序，避免客户端重排导致运营配置的模型优先级失效。
                            models
                                .groupBy { it.groupTitle }
                                .forEach { (groupName, groupModels) ->
                                    item {
                                        Text(
                                            text = groupName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Primary300,
                                            modifier = Modifier.padding(top = Dimens.SpaceSM),
                                        )
                                    }
                                    items(groupModels) { model ->
                                        ServiceModelListRow(
                                            model = model,
                                            onClick = { onSelect(model.identityKey) },
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 渲染模型选择面板顶部栏。
 *
 * 标题由上层传入，便于调用方使用资源化文案；关闭按钮的无障碍描述在此处读取资源，
 * 与按钮图标保持同一可访问语义。
 *
 * @param title 面板标题，通常来自 Compose Resources。
 * @param onDismiss 用户点击关闭按钮时触发。
 */
@Composable
private fun ModelSheetHeader(title: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpaceSM, start = Dimens.SpaceLG, end = Dimens.SpaceLG, bottom = Dimens.SpaceSM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Neutral100,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(
                    Res.string.quick_create_model_selector_close_content_description,
                ),
                tint = Neutral500,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * 渲染服务端模型列表中的单行模型。
 *
 * 模型展示名称、分组和副标题均由 Presentation 层映射完成；本组件只根据选中状态渲染颜色、
 * 边框和勾选图标，不生成新的用户可见业务文案。
 *
 * @param model 已映射为 UI 形态的服务端模型条目。
 * @param onClick 用户点击该模型行时触发。
 */
@Composable
private fun ServiceModelListRow(
    model: QuickCreateServiceModelUi,
    onClick: () -> Unit,
) {
    val selected = model.selected
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.RadiusSM),
        color = if (selected) Primary300.copy(alpha = 0.10f) else DarkSurfaceVariant,
        border = BorderStroke(1.dp, if (selected) Primary300 else DarkOutlineVariant),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Primary300 else Neutral100,
                    maxLines = 1,
                )
                model.subtitle.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Neutral500,
                        maxLines = 1,
                    )
                }
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Primary300,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
