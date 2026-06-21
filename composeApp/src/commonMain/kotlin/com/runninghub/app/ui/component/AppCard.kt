package com.runninghub.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_card_use_count_format

/**
 * 渲染应用或工作流列表中的通用卡片。
 *
 * 该组件只消费调用方已经映射好的标题、图片、作者和计数字段，不访问仓库或维护分页状态；
 * 使用次数后缀通过 Compose Resources 格式化，避免通用卡片继续持有中文硬编码文案。
 *
 * @param title 卡片标题，来源于服务端或 Presentation 映射结果；空字符串表示上游没有可展示标题。
 * @param imageUrl 卡片封面远程图片地址；为 `null` 或空字符串时由 [SmartAsyncImage] 保持空占位。
 * @param authorName 作者展示名；为 `null` 时头像可展示但作者文本为空。
 * @param authorAvatar 作者头像远程图片地址；为 `null` 时头像区域由 [SmartAsyncImage] 空占位处理。
 * @param likeCount 点赞数量展示文本，当前组件不直接渲染该字段，保留给后续卡片指标扩展。
 * @param useCount 使用次数展示文本，来源于上游格式化前的数量字符串，会拼接资源化单位后展示。
 * @param modifier 外层布局修饰符，由调用方控制列表宽度、测试标记或额外点击区域。
 * @param onClick 用户点击卡片时触发的回调，默认空实现表示当前卡片只作静态展示。
 */
@Composable
fun AppCard(
    title: String,
    imageUrl: String?,
    authorName: String?,
    authorAvatar: String?,
    likeCount: String,
    useCount: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.RadiusMD),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSM)
    ) {
        Column {
            SmartAsyncImage(
                imageUrl = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
            )

            Column(modifier = Modifier.padding(Dimens.SpaceMD)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SmartAsyncImage(
                        imageUrl = authorAvatar,
                        contentDescription = authorName,
                        modifier = Modifier
                            .size(Dimens.AvatarSizeSM),
                        shape = CircleShape,
                    )
                    Spacer(Modifier.width(Dimens.SpaceSM))
                    Text(
                        text = authorName ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(Res.string.app_card_use_count_format, useCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
