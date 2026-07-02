package com.runninghub.app.ui.feature.quickcreate.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryDetailUiItem
import com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryOutputMediaType
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_history_detail_close
import runninghub.composeapp.generated.resources.quick_create_history_detail_loading_body
import runninghub.composeapp.generated.resources.quick_create_history_detail_loading_title
import runninghub.composeapp.generated.resources.quick_create_history_detail_title

/**
 * 展示快捷创作历史详情弹窗。
 *
 * 弹窗只消费外层已经选中的历史详情，不在自身内部发起请求。这样可以由 HistoryStateHolder
 * 统一管理详情加载态、失败恢复和关闭后状态清理。
 *
 * @param isLoading 是否正在加载历史详情，`true` 时 [item] 可以为 `null` 并展示加载提示。
 * @param item 当前选中的历史记录详情；`null` 且 [isLoading] 为 `false` 时表示没有详情可展示。
 * @param onDismiss 用户关闭详情弹窗时触发。
 */
@Composable
internal fun QuickCreateHistoryDetailDialog(
    isLoading: Boolean,
    item: QuickCreateHistoryDetailUiItem?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.quick_create_history_detail_close))
            }
        },
        title = {
            Text(
                if (isLoading) {
                    stringResource(Res.string.quick_create_history_detail_loading_title)
                } else {
                    stringResource(Res.string.quick_create_history_detail_title)
                },
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            if (isLoading || item == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.quick_create_history_detail_loading_body))
                }
            } else {
                val output = item.primaryOutput
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    val previewUrl = output?.previewUrl
                    if (previewUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(Dimens.RadiusMD)),
                            contentAlignment = Alignment.Center,
                        ) {
                            SmartAsyncImage(
                                imageUrl = previewUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            if (output.mediaType == QuickCreateHistoryOutputMediaType.VIDEO) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp),
                                    tint = Color.White.copy(alpha = 0.86f),
                                )
                            }
                        }
                    }
                    Text(
                        item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.metadataText,
                        color = Neutral500,
                        fontSize = 12.sp,
                    )
                    item.cashText?.let { cashText ->
                        Text(
                            cashText,
                            color = Primary300,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        },
        containerColor = DarkSurface,
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.82f),
    )
}
