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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.ErrorDark
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.app.ui.theme.SuccessDark

enum class TaskStep {
    IDLE,
    SUBMITTING,
    QUEUEING,
    RUNNING,
    COMPLETING,
    SUCCESS,
    FAILED
}

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
            .background(DarkSurfaceVariant)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (currentStep) {
                    TaskStep.IDLE -> "准备中"
                    TaskStep.SUBMITTING -> "提交任务中..."
                    TaskStep.QUEUEING -> "排队等待"
                    TaskStep.RUNNING -> "AI 生成中"
                    TaskStep.COMPLETING -> "完成中"
                    TaskStep.SUCCESS -> "生成完成"
                    TaskStep.FAILED -> "任务失败"
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (isRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Primary300
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${elapsedSeconds}s",
                        color = Neutral400,
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
                TaskStep.FAILED -> ErrorDark
                TaskStep.SUCCESS -> SuccessDark
                else -> Primary300
            },
            trackColor = Neutral400.copy(alpha = 0.15f)
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepIndicator(
                label = "提交",
                isActive = currentStep == TaskStep.SUBMITTING,
                isCompleted = currentStep.ordinal > TaskStep.SUBMITTING.ordinal,
                isFailed = currentStep == TaskStep.FAILED
            )
            StepConnector(isActive = currentStep.ordinal > TaskStep.SUBMITTING.ordinal)
            StepIndicator(
                label = "排队",
                isActive = currentStep == TaskStep.QUEUEING,
                isCompleted = currentStep.ordinal > TaskStep.QUEUEING.ordinal,
                isFailed = currentStep == TaskStep.FAILED
            )
            StepConnector(isActive = currentStep.ordinal > TaskStep.QUEUEING.ordinal)
            StepIndicator(
                label = "生成",
                isActive = currentStep == TaskStep.RUNNING,
                isCompleted = currentStep.ordinal > TaskStep.RUNNING.ordinal,
                isFailed = currentStep == TaskStep.FAILED
            )
            StepConnector(isActive = currentStep.ordinal > TaskStep.RUNNING.ordinal)
            StepIndicator(
                label = "完成",
                isActive = currentStep == TaskStep.COMPLETING,
                isCompleted = currentStep == TaskStep.SUCCESS,
                isFailed = currentStep == TaskStep.FAILED
            )
        }
    }
}

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
                        isCompleted -> SuccessDark
                        isFailed -> ErrorDark
                        isActive -> Primary300
                        else -> Neutral400.copy(alpha = 0.3f)
                    }
                )
        ) {
            when {
                isCompleted -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                isFailed -> Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                isActive -> CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = Color.White
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = when {
                isCompleted -> SuccessDark
                isFailed -> ErrorDark
                isActive -> Color.White
                else -> Neutral400
            },
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun StepConnector(isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .height(2.dp)
            .background(
                if (isActive) SuccessDark else Neutral400.copy(alpha = 0.3f),
                RoundedCornerShape(1.dp)
            )
    )
}
