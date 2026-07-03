package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.component.TaskStep
import com.runninghub.app.ui.theme.DarkBackground
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary500
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_rerun_action
import runninghub.composeapp.generated.resources.app_detail_run_now_action
import runninghub.composeapp.generated.resources.app_detail_status_completing
import runninghub.composeapp.generated.resources.app_detail_status_queueing
import runninghub.composeapp.generated.resources.app_detail_status_running
import runninghub.composeapp.generated.resources.app_detail_status_running_fallback
import runninghub.composeapp.generated.resources.app_detail_status_submitting

/**
 * App 详情页底部固定运行栏。
 *
 * 从 AppDetailScreen.kt 拆分而来（纯搬移，无行为变化），承载运行/重跑按钮
 * 及运行中的阶段状态文案。
 */

@Composable
internal fun RunTaskBottomBar(
    isRunning: Boolean,
    taskStep: TaskStep,
    hasResult: Boolean,
    onRun: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowInfo = LocalRhWindowInfo.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = windowInfo.detailContentMaxWidth)
            .background(DarkBackground.copy(alpha = 0.95f))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val buttonColor = when {
            isRunning -> Primary500.copy(alpha = 0.6f)
            hasResult -> Neutral500
            else -> Primary500
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(buttonColor)
                .then(
                    if (isRunning) Modifier
                    else Modifier.clickable(onClick = if (hasResult) onReset else onRun)
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    isRunning -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        val statusText = when (taskStep) {
                            TaskStep.SUBMITTING -> stringResource(Res.string.app_detail_status_submitting)
                            TaskStep.QUEUEING -> stringResource(Res.string.app_detail_status_queueing)
                            TaskStep.RUNNING -> stringResource(Res.string.app_detail_status_running)
                            TaskStep.COMPLETING -> stringResource(Res.string.app_detail_status_completing)
                            else -> stringResource(Res.string.app_detail_status_running_fallback)
                        }
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    hasResult -> {
                        Text(
                            text = stringResource(Res.string.app_detail_rerun_action),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.app_detail_run_now_action),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
