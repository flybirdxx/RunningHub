package com.runninghub.app.ui.feature.quickcreate.presentation.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultMediaType
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.ErrorDark
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.app.ui.theme.SuccessDark
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskIndicator
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskPresentationStatus
import com.runninghub.feature.quickcreate.presentation.result.quickCreateTaskStatusDisplay

/**
 * 展示快捷创作任务的当前执行状态。
 *
 * 该组件只负责把 [QuickCreateTaskUiStatus] 渲染为进度、成功或失败提示，不触发轮询、
 * 不读取仓库，也不修改页面状态。任务状态到文案的映射统一复用 [quickCreateTaskStatusDisplay]。
 *
 * @param status 当前任务 UI 状态，来源于 Coordinator 下游的任务轮询控制器。
 * @param statusText 后端状态流或本地提交流程生成的补充文案；为 `null` 时使用状态默认文案。
 */
@Composable
internal fun QuickCreateTaskStatusArea(status: QuickCreateTaskUiStatus, statusText: String?) {
    // 迁移期 composeApp 仍持有旧 UiState 枚举；结果展示规则已经下沉到 feature presentation，
    // 这里只保留无业务分支的薄转换，避免 Composable 继续维护任务文案映射。
    val display = quickCreateTaskStatusDisplay(status.toPresentationStatus(), statusText)
    val indicatorColor = when (display.indicator) {
        QuickCreateTaskIndicator.Progress -> Primary300
        QuickCreateTaskIndicator.Success -> SuccessDark
        QuickCreateTaskIndicator.Error -> ErrorDark
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (display.indicator) {
                QuickCreateTaskIndicator.Progress -> CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = indicatorColor,
                    strokeWidth = 3.dp,
                )
                QuickCreateTaskIndicator.Success -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = indicatorColor,
                )
                QuickCreateTaskIndicator.Error -> Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = indicatorColor,
                )
            }
            Spacer(Modifier.height(Dimens.SpaceXL))
            Text(
                display.text,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
            )
        }
    }
}

private fun QuickCreateTaskUiStatus.toPresentationStatus(): QuickCreateTaskPresentationStatus =
    when (this) {
        QuickCreateTaskUiStatus.IDLE -> QuickCreateTaskPresentationStatus.IDLE
        QuickCreateTaskUiStatus.SUBMITTING -> QuickCreateTaskPresentationStatus.SUBMITTING
        QuickCreateTaskUiStatus.QUEUING -> QuickCreateTaskPresentationStatus.QUEUING
        QuickCreateTaskUiStatus.RUNNING -> QuickCreateTaskPresentationStatus.RUNNING
        QuickCreateTaskUiStatus.SUCCESS -> QuickCreateTaskPresentationStatus.SUCCESS
        QuickCreateTaskUiStatus.FAILED -> QuickCreateTaskPresentationStatus.FAILED
    }

/**
 * 展示快捷创作生成结果列表。
 *
 * 该组件位于 result 子区域，只负责渲染生成结果和“重新创作”入口。结果来自 UiState，
 * 清空结果的行为通过 [onClear] 回传给 ScreenModel 门面，保持 UI 到 Action 的单向数据流。
 *
 * @param results 当前任务完成后得到的结果集合，顺序沿用服务端输出顺序，空集合时不会显示该区域。
 * @param onClear 用户点击“重新创作”时触发，用于清空当前结果并回到编辑状态。
 */
@Composable
internal fun QuickCreateResultArea(results: List<QuickCreateResultUi>, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SpaceMD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "生成结果",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
            )
            TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Primary300,
                )
                Spacer(Modifier.width(4.dp))
                Text("重新创作", color = Primary300, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(Dimens.SpaceSM))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
            items(results) { result ->
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLG),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (result.mediaType) {
                        QuickCreateResultMediaType.VIDEO -> {
                            VideoThumbnail(
                                url = result.url,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                            )
                        }
                        QuickCreateResultMediaType.IMAGE -> {
                            SmartAsyncImage(
                                imageUrl = result.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth,
                            )
                        }
                    }
                }
            }
        }
    }
}
