package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatusBadge
import com.runninghub.app.ui.designsystem.components.billing.BillingInfoCard
import com.runninghub.app.ui.designsystem.components.result.ResultPreview
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_history_chevron
import runninghub.composeapp.generated.resources.task_history_detail_close_content_description
import runninghub.composeapp.generated.resources.task_history_detail_loading
import runninghub.composeapp.generated.resources.task_history_detail_prompt_parameters
import runninghub.composeapp.generated.resources.task_history_detail_technical_details
import runninghub.composeapp.generated.resources.task_history_detail_title
import com.runninghub.feature.task.presentation.TaskHistoryDetailUiModel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TaskDetailDrawer(
    detail: TaskHistoryDetailUiModel?,
    isLoading: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.92f)
            .widthIn(max = 380.dp)
            .background(RhTheme.colors.backgroundSecondary)
            .clickable(onClick = {})
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.task_history_detail_close_content_description),
                    tint = RhTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(Res.string.task_history_detail_title),
                color = RhTheme.colors.textPrimary,
                style = RhTypography.cardTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        when {
            isLoading -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = RhTheme.colors.brandPrimary)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.task_history_detail_loading),
                    color = RhTheme.colors.textSecondary,
                    style = RhTypography.caption,
                )
            }
            detail != null -> TaskDetailContent(detail)
        }
    }
}

@Composable
private fun TaskDetailContent(detail: TaskHistoryDetailUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val layout = detail.toTaskDetailLayoutState()
        TaskDetailStatusSummary(detail)
        TaskDetailResultPreview(layout)
        TaskDetailBillingSection(layout)
        TaskDetailPromptParameters(layout)
        TaskDetailTechnicalDetails(layout)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TaskDetailStatusSummary(detail: TaskHistoryDetailUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhTheme.colors.surfaceDefault)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = detail.title,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.bodyStrong,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            detail.sourceLabel?.takeIf { it.isNotBlank() }?.let { SourceBadge(it) }
            RhTaskStatusBadge(
                status = detail.status.toRhTaskStatus(),
                label = statusPillLabelText(detail.status.toLegacyStatusText()),
            )
        }
        Text(
            text = detail.taskId,
            color = RhTheme.colors.textSecondary,
            style = RhTypography.meta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TaskDetailResultPreview(layout: TaskDetailLayoutState) {
    ResultPreview(
        state = layout.resultPreview,
        onAction = {},
        mediaContent = { media ->
            SmartAsyncImage(
                imageUrl = media.renderUrl,
                contentDescription = layout.resultPreview.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        },
    )
    Text(
        text = layout.saveStateLabel,
        color = RhTheme.colors.textSecondary,
        style = RhTypography.meta,
    )
}

@Composable
private fun TaskDetailBillingSection(layout: TaskDetailLayoutState) {
    BillingInfoCard(rows = layout.billingRows)
}

@Composable
private fun TaskDetailPromptParameters(layout: TaskDetailLayoutState) {
    if (layout.promptParameters.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhTheme.colors.surfaceDefault),
    ) {
        Text(
            text = stringResource(Res.string.task_history_detail_prompt_parameters),
            color = RhTheme.colors.textPrimary,
            style = RhTypography.body,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        )
        layout.promptParameters.forEach { param ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RhTheme.colors.borderDefault),
            )
            DetailKeyValueRow(label = param.label, value = param.value)
        }
    }
}

@Composable
private fun TaskDetailTechnicalDetails(layout: TaskDetailLayoutState) {
    if (layout.technicalSections.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.task_history_detail_technical_details),
            color = RhTheme.colors.textPrimary,
            style = RhTypography.body,
            fontWeight = FontWeight.Bold,
        )
        layout.technicalSections.forEach { section ->
            TaskDetailTechnicalSection(section)
        }
    }
}

@Composable
private fun TaskDetailTechnicalSection(section: TaskDetailTechnicalSectionState) {
    var expanded by remember(section.title, section.content) { mutableStateOf(section.initiallyExpanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhTheme.colors.surfaceDefault),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = section.title,
                color = RhTheme.colors.textPrimary,
                style = RhTypography.body,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(Res.string.task_history_chevron), color = RhTheme.colors.textSecondary, style = RhTypography.cardTitle)
        }
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RhTheme.colors.borderDefault),
            )
            Text(
                text = section.content,
                color = RhTheme.colors.textSecondary,
                style = RhTypography.meta,
                fontFamily = FontFamily.Monospace,
                softWrap = true,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
private fun DetailKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = RhTheme.colors.textSecondary,
            style = RhTypography.meta,
            modifier = Modifier.width(92.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.meta,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            softWrap = true,
        )
    }
}
