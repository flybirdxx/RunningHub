package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.states.RhEmptyState
import com.runninghub.app.ui.designsystem.components.states.RhErrorState
import com.runninghub.app.ui.designsystem.components.states.RhLoadingState
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_history_action_retry
import runninghub.composeapp.generated.resources.task_history_loading

@Composable
internal fun SourceBadge(source: String) {
    val colors = RhTheme.colors
    val color = if (source.contains("api", ignoreCase = true) || source.contains("model", ignoreCase = true)) {
        colors.statusSuccess
    } else {
        colors.statusProcessing
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(sourceLabelText(source), color = color, style = RhTypography.meta, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun LoadingPanel(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        RhLoadingState(title = stringResource(Res.string.task_history_loading))
    }
}

@Composable
internal fun TaskHistoryErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        RhErrorState(
            title = message,
            actionLabel = stringResource(Res.string.task_history_action_retry),
            onAction = onRetry,
        )
    }
}

@Composable
internal fun TaskHistoryEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        RhEmptyState(title = message)
    }
}
