package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.component.TaskStep
import com.runninghub.app.ui.designsystem.components.buttons.RhPrimaryButton
import com.runninghub.app.ui.designsystem.theme.RhTheme
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
 * 承载运行/重跑按钮及运行中的阶段状态文案；按钮由 RhPrimaryButton 承载，
 * 容器使用 backgroundPrimary 背景与 borderSubtle 上边框。
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
    val colors = RhTheme.colors
    val label = when {
        isRunning -> when (taskStep) {
            TaskStep.SUBMITTING -> stringResource(Res.string.app_detail_status_submitting)
            TaskStep.QUEUEING -> stringResource(Res.string.app_detail_status_queueing)
            TaskStep.RUNNING -> stringResource(Res.string.app_detail_status_running)
            TaskStep.COMPLETING -> stringResource(Res.string.app_detail_status_completing)
            else -> stringResource(Res.string.app_detail_status_running_fallback)
        }
        hasResult -> stringResource(Res.string.app_detail_rerun_action)
        else -> stringResource(Res.string.app_detail_run_now_action)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = windowInfo.detailContentMaxWidth)
            .background(colors.backgroundPrimary)
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.borderSubtle)
        RhPrimaryButton(
            text = label,
            onClick = if (hasResult) onReset else onRun,
            loading = isRunning,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        )
    }
}
