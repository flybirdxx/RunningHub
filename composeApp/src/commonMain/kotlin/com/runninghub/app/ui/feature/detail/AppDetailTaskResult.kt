package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.core.model.TaskOutput
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_video_file

/**
 * App 详情页任务结果展示卡片。
 *
 * 从 AppDetailScreen.kt 拆分而来，只承载任务失败提示卡和任务输出卡两个纯 Compose 叶子；
 * 任务状态与错误文案仍由 DetailContent 计算后传入，颜色统一读取 RhTheme 语义 token。
 */
@Composable
internal fun TaskErrorCard(error: String, modifier: Modifier = Modifier) {
    val colors = RhTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.statusFailed.copy(alpha = 0.12f))
            .padding(14.dp)
    ) {
        Text(
            text = error,
            color = colors.statusFailed,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
internal fun TaskOutputCard(output: TaskOutput, modifier: Modifier = Modifier) {
    val colors = RhTheme.colors
    val url = output.fileUrl.orEmpty()
    val isImage = output.fileType?.startsWith("image") == true ||
        url.endsWith(".png") || url.endsWith(".jpg") ||
        url.endsWith(".jpeg") || url.endsWith(".webp")
    val isVideo = output.fileType?.startsWith("video") == true ||
        url.endsWith(".mp4") || url.endsWith(".mov") || url.endsWith(".webm")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceDefault)
    ) {
        if (isImage && url.isNotBlank()) {
            SmartAsyncImage(
                imageUrl = url,
                contentDescription = output.fileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.FillWidth
            )
            Spacer(Modifier.height(8.dp))
        }

        if (isVideo && url.isNotBlank()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(colors.surfaceElevated)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = colors.brandPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.app_detail_video_file),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = output.fileName ?: url.substringAfterLast("/"),
            color = colors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
