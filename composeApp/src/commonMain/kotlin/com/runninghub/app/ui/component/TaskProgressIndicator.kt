package com.runninghub.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.designsystem.theme.RhTheme
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_progress_indicator_completed_content_description
import runninghub.composeapp.generated.resources.task_progress_indicator_elapsed_seconds_format
import runninghub.composeapp.generated.resources.task_progress_indicator_failed_content_description
import runninghub.composeapp.generated.resources.task_progress_indicator_status_completing
import runninghub.composeapp.generated.resources.task_progress_indicator_status_failed
import runninghub.composeapp.generated.resources.task_progress_indicator_status_idle
import runninghub.composeapp.generated.resources.task_progress_indicator_status_queueing
import runninghub.composeapp.generated.resources.task_progress_indicator_status_running
import runninghub.composeapp.generated.resources.task_progress_indicator_status_submitting
import runninghub.composeapp.generated.resources.task_progress_indicator_status_success
import runninghub.composeapp.generated.resources.task_progress_indicator_step_complete
import runninghub.composeapp.generated.resources.task_progress_indicator_step_generate
import runninghub.composeapp.generated.resources.task_progress_indicator_step_queue
import runninghub.composeapp.generated.resources.task_progress_indicator_step_submit

/**
 * AppDetail 任务进度条支持展示的阶段。
 *
 * 枚举顺序参与进度点完成态判断：后续阶段的 ordinal 大于前序阶段时，
 * 对应步骤会被标记为已完成。因此新增或调整阶段时必须同步检查
 * [TaskProgressIndicator] 中的进度比例和步骤完成条件。
 */
enum class TaskStep {
    /** 任务尚未提交或进度条处于初始化状态。 */
    IDLE,

    /** 正在把用户输入和媒体参数提交到远端任务接口。 */
    SUBMITTING,

    /** 服务端已接收任务，正在等待调度执行。 */
    QUEUEING,

    /** 远端 AI 任务正在生成输出内容。 */
    RUNNING,

    /** 生成任务已接近终态，客户端等待服务端输出整理完成。 */
    COMPLETING,

    /** 任务已成功完成，进度条展示完成态。 */
    SUCCESS,

    /** 任务提交、排队、生成或收尾阶段失败，进度条展示失败态。 */
    FAILED
}

/**
 * 渲染 AppDetail 任务提交后的阶段进度。
 *
 * 组件只接收 Presentation 层映射后的阶段和耗时秒数，不发起轮询或网络请求。
 * 状态标题、步骤标签、耗时格式和图标无障碍描述统一来自 Compose Resources；
 * 动画调试 label 不是用户可见文案，保留为稳定英文标识。
 *
 * @param currentStep 当前任务阶段，决定进度条比例、步骤完成态和状态标题。
 * @param totalSteps 预留的步骤总数参数，当前视觉布局固定为提交、排队、生成、完成四步。
 * 调用方不应依赖该参数改变布局。
 * @param elapsedSeconds 已运行耗时，单位为秒；小于等于 `0` 时仍按传入值格式化，
 * 是否展示由 [currentStep] 是否处于运行中阶段决定。
 * @param modifier 外层调用方用于控制组件位置和尺寸的修饰符。
 */
@Composable
fun TaskProgressIndicator(
    currentStep: TaskStep,
    totalSteps: Int = 4,
    elapsedSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val isRunning = currentStep.ordinal in 1..4

    val animatedProgress by animateFloatAsState(
        targetValue = when (currentStep) {
            TaskStep.IDLE -> 0f
            TaskStep.SUBMITTING -> 0.1f
            TaskStep.QUEUEING -> 0.25f
            TaskStep.RUNNING -> 0.55f
            TaskStep.COMPLETING -> 0.8f
            TaskStep.SUCCESS -> 1f
            TaskStep.FAILED -> 0f
        },
        animationSpec = tween(600),
        label = "progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RhTheme.colors.surfaceElevated)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (currentStep) {
                    TaskStep.IDLE -> stringResource(Res.string.task_progress_indicator_status_idle)
                    TaskStep.SUBMITTING -> stringResource(Res.string.task_progress_indicator_status_submitting)
                    TaskStep.QUEUEING -> stringResource(Res.string.task_progress_indicator_status_queueing)
                    TaskStep.RUNNING -> stringResource(Res.string.task_progress_indicator_status_running)
                    TaskStep.COMPLETING -> stringResource(Res.string.task_progress_indicator_status_completing)
                    TaskStep.SUCCESS -> stringResource(Res.string.task_progress_indicator_status_success)
                    TaskStep.FAILED -> stringResource(Res.string.task_progress_indicator_status_failed)
                },
                color = RhTheme.colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (isRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = RhTheme.colors.brandPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            Res.string.task_progress_indicator_elapsed_seconds_format,
                            elapsedSeconds,
                        ),
                        color = RhTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = when (currentStep) {
                TaskStep.FAILED -> RhTheme.colors.statusFailed
                TaskStep.SUCCESS -> RhTheme.colors.brandPrimary
                else -> RhTheme.colors.brandPrimary
            },
            trackColor = RhTheme.colors.surfaceElevated
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepIndicator(
                label = stringResource(Res.string.task_progress_indicator_step_submit),
                isActive = currentStep == TaskStep.SUBMITTING,
                isCompleted = currentStep.ordinal > TaskStep.SUBMITTING.ordinal,
                isFailed = currentStep == TaskStep.FAILED
            )
            StepConnector(isActive = currentStep.ordinal > TaskStep.SUBMITTING.ordinal)
            StepIndicator(
                label = stringResource(Res.string.task_progress_indicator_step_queue),
                isActive = currentStep == TaskStep.QUEUEING,
                isCompleted = currentStep.ordinal > TaskStep.QUEUEING.ordinal,
                isFailed = currentStep == TaskStep.FAILED
            )
            StepConnector(isActive = currentStep.ordinal > TaskStep.QUEUEING.ordinal)
            StepIndicator(
                label = stringResource(Res.string.task_progress_indicator_step_generate),
                isActive = currentStep == TaskStep.RUNNING,
                isCompleted = currentStep.ordinal > TaskStep.RUNNING.ordinal,
                isFailed = currentStep == TaskStep.FAILED
            )
            StepConnector(isActive = currentStep.ordinal > TaskStep.RUNNING.ordinal)
            StepIndicator(
                label = stringResource(Res.string.task_progress_indicator_step_complete),
                isActive = currentStep == TaskStep.COMPLETING,
                isCompleted = currentStep == TaskStep.SUCCESS,
                isFailed = currentStep == TaskStep.FAILED
            )
        }
    }
}

/**
 * 渲染单个进度步骤的圆点、图标和标签。
 *
 * 三个布尔状态由 [TaskProgressIndicator] 根据 [TaskStep] 顺序计算：
 * [isCompleted] 为 `true` 表示该步骤已经跨过并展示完成图标；
 * [isFailed] 为 `true` 表示整条任务链路失败，步骤使用错误色和失败图标；
 * [isActive] 为 `true` 表示任务当前停留在该步骤，圆点展示加载态。
 *
 * @param label 步骤标签，来自 Compose Resources，不在本函数内生成业务文案。
 * @param isActive 当前步骤是否正在执行，`true` 时展示小型进度圈；`false` 表示不是当前执行点。
 * @param isCompleted 当前步骤是否已经完成，`true` 时展示完成图标；`false` 表示尚未完成或失败态接管。
 * @param isFailed 整体任务是否失败，`true` 时展示失败图标和错误色；`false` 表示按普通进度态渲染。
 */
@Composable
private fun StepIndicator(
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    isFailed: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> RhTheme.colors.brandPrimary
                        isFailed -> RhTheme.colors.statusFailed
                        isActive -> RhTheme.colors.brandPrimary
                        else -> RhTheme.colors.surfaceElevated
                    }
                )
        ) {
            when {
                isCompleted -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(
                        Res.string.task_progress_indicator_completed_content_description,
                    ),
                    tint = RhTheme.colors.textInverse,
                    modifier = Modifier.size(16.dp)
                )
                isFailed -> Icon(
                    Icons.Default.Error,
                    contentDescription = stringResource(
                        Res.string.task_progress_indicator_failed_content_description,
                    ),
                    tint = RhTheme.colors.textInverse,
                    modifier = Modifier.size(16.dp)
                )
                isActive -> CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = RhTheme.colors.textInverse
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = when {
                isCompleted -> RhTheme.colors.brandPrimary
                isFailed -> RhTheme.colors.statusFailed
                isActive -> RhTheme.colors.textPrimary
                else -> RhTheme.colors.textSecondary
            },
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/**
 * 渲染两个任务步骤之间的连接线。
 *
 * @param isActive 连接线左侧步骤是否已经跨过，`true` 时使用主色表示链路推进；
 * `false` 表示后续步骤尚未开始。
 */
@Composable
private fun StepConnector(isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .height(2.dp)
            .background(
                if (isActive) RhTheme.colors.brandPrimary else RhTheme.colors.surfaceElevated,
                RoundedCornerShape(1.dp)
            )
    )
}
