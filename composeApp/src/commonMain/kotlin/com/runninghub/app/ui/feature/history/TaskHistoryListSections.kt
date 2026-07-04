package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCard
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardActionType
import com.runninghub.app.ui.designsystem.components.chips.RhChip
import com.runninghub.app.ui.designsystem.components.navigation.RhTopBar
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.presentation.TaskHistoryEntry
import com.runninghub.feature.task.presentation.TaskHistoryFilter
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_history_chevron
import runninghub.composeapp.generated.resources.task_history_filter_all
import runninghub.composeapp.generated.resources.task_history_filter_completed
import runninghub.composeapp.generated.resources.task_history_filter_failed
import runninghub.composeapp.generated.resources.task_history_filter_in_progress
import runninghub.composeapp.generated.resources.task_history_group_all
import runninghub.composeapp.generated.resources.task_history_notice_cloud_output
import runninghub.composeapp.generated.resources.task_history_notice_icon
import runninghub.composeapp.generated.resources.task_history_output_detail_title
import runninghub.composeapp.generated.resources.task_history_refresh_content_description
import runninghub.composeapp.generated.resources.task_history_reusable_params_title
import runninghub.composeapp.generated.resources.task_history_search_content_description
import runninghub.composeapp.generated.resources.task_history_title
import runninghub.composeapp.generated.resources.task_history_total_count_format

@Composable
internal fun HistoryTopBar(onRetry: () -> Unit) {
    RhTopBar(
        title = stringResource(Res.string.task_history_title),
        actions = {
            IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(Res.string.task_history_search_content_description),
                    tint = RhTheme.colors.textSecondary,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onRetry, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(Res.string.task_history_refresh_content_description),
                    tint = RhTheme.colors.textSecondary,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
    )
}

@Composable
internal fun TaskHistoryFilterRow(
    selectedFilter: TaskHistoryFilter,
    onFilterSelected: (TaskHistoryFilter) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
        listOf(
            TaskHistoryFilter.ALL to stringResource(Res.string.task_history_filter_all),
            TaskHistoryFilter.IN_PROGRESS to stringResource(Res.string.task_history_filter_in_progress),
            TaskHistoryFilter.COMPLETED to stringResource(Res.string.task_history_filter_completed),
            TaskHistoryFilter.FAILED to stringResource(Res.string.task_history_filter_failed),
        ).forEach { (filter, label) ->
            RhChip(
                label = label,
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
internal fun NoticeBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(RhTheme.colors.backgroundSecondary)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.dp, RhTheme.colors.textSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.task_history_notice_icon),
                color = RhTheme.colors.textSecondary,
                style = RhTypography.meta,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = stringResource(Res.string.task_history_notice_cloud_output),
            color = RhTheme.colors.textSecondary,
            style = RhTypography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(Res.string.task_history_chevron), color = RhTheme.colors.textSecondary, style = RhTypography.cardTitle)
    }
}


@Composable
internal fun HistoryActionPanel(
    message: String?,
    selectedOutput: GenerationHistoryOutput?,
    hasPreparedParams: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhTheme.colors.backgroundSecondary)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        message?.let {
            Text(
                text = it,
                color = RhTheme.colors.brandPrimary,
                style = RhTypography.body,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        selectedOutput?.let { output ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.task_history_output_detail_title),
                    color = RhTheme.colors.textPrimary,
                    style = RhTypography.body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = listOfNotNull(output.type.uppercase(), output.sizeLabel(), output.expireLabelText()).joinToString(" / "),
                    color = RhTheme.colors.textSecondary,
                    style = RhTypography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = output.url,
                    color = RhTheme.colors.textSecondary,
                    style = RhTypography.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (hasPreparedParams && message == null) {
            Text(
                text = stringResource(Res.string.task_history_reusable_params_title),
                color = RhTheme.colors.textPrimary,
                style = RhTypography.body,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun DateGroupHeader(total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.task_history_group_all), color = RhTheme.colors.textPrimary, style = RhTypography.bodyStrong, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(stringResource(Res.string.task_history_total_count_format, total), color = RhTheme.colors.textSecondary, style = RhTypography.body)
    }
}

@Composable
internal fun TaskTimelineRow(
    item: TaskHistoryEntry,
    onOpenTaskDetail: (String) -> Unit,
    onViewOutput: (String) -> Unit,
    onReuseParams: (String) -> Unit,
    onRetryTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
) {
    HistoryTaskCard(
        state = item.toHistoryTaskCardState(),
        onClick = { onOpenTaskDetail(item.taskId) },
        onAction = { action ->
            when (action) {
                HistoryTaskCardActionType.ViewResult -> item.outputId?.let(onViewOutput)
                HistoryTaskCardActionType.Retry -> onRetryTask(item.taskId)
                HistoryTaskCardActionType.Cancel -> onCancelTask(item.taskId)
                HistoryTaskCardActionType.ReuseParameters -> onReuseParams(item.taskId)
                HistoryTaskCardActionType.ViewDetail -> onOpenTaskDetail(item.taskId)
            }
        },
        thumbnailContent = {
            TaskThumbnail(
                item = item,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun TaskThumbnail(
    item: TaskHistoryEntry,
    modifier: Modifier = Modifier.size(84.dp),
) {
    val colors = RhTheme.colors
    val (containerColor, contentColor) = when {
        item.status.isCompletedStatus() -> colors.brandMuted to colors.textPrimary
        item.status.equals("failed", ignoreCase = true) -> colors.surfaceSunken to colors.statusFailed
        else -> colors.surfaceSunken to colors.statusProcessing
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = item.thumbnailUrl?.takeIf { it.isNotBlank() }
        if (imageUrl != null) {
            SmartAsyncImage(
                imageUrl = imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                item.title.take(1),
                color = contentColor,
                style = RhTypography.sectionTitle,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
