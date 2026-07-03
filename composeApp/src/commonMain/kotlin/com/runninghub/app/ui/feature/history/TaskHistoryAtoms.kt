package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.theme.BrandLime
import com.runninghub.app.ui.theme.RhAppBackground
import com.runninghub.app.ui.theme.RhAppCard
import com.runninghub.app.ui.theme.RhAppLine
import com.runninghub.app.ui.theme.RhAppMuted
import com.runninghub.app.ui.theme.RhAppSelected
import com.runninghub.app.ui.theme.RhAppSurface
import com.runninghub.app.ui.theme.RhAppText
import com.runninghub.app.ui.theme.StatusError
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_history_action_retry

internal val RhBackground = RhAppBackground
internal val RhSurface = RhAppSurface
internal val RhCard = RhAppCard
internal val RhSelected = RhAppSelected
internal val RhLine = RhAppLine
internal val RhText = RhAppText
internal val RhMuted = RhAppMuted

@Composable
internal fun SourceBadge(source: String) {
    val color = if (source.contains("api", ignoreCase = true) || source.contains("model", ignoreCase = true)) Color(0xFF53D66A) else Color(0xFF60A5FA)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(sourceLabelText(source), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun TaskStatusPill(status: String) {
    val color = when {
        status.isCompletedStatus() -> Color(0xFF4ADE5C)
        status.equals("failed", ignoreCase = true) -> StatusError
        else -> Color(0xFF2F7DFF)
    }
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(statusPillLabelText(status), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SmallAction(label: String, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .widthIn(min = 46.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (highlighted) RhSelected else RhCard)
            .border(1.dp, if (highlighted) BrandLime else RhLine, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (highlighted) BrandLime else RhText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun LoadingPanel(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandLime)
    }
}

@Composable
internal fun TaskHistoryErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = StatusError, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        SmallAction(stringResource(Res.string.task_history_action_retry), highlighted = true, onClick = onRetry)
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
        Text(message, color = RhMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}
