package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.CollapsibleSection
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.core.model.AppDetail
import com.runninghub.feature.detail.presentation.AppDetailCreationEntryUiModel
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_back_content_description
import runninghub.composeapp.generated.resources.app_detail_creation_cost_title
import runninghub.composeapp.generated.resources.app_detail_creation_cost_unknown
import runninghub.composeapp.generated.resources.app_detail_creation_description_title
import runninghub.composeapp.generated.resources.app_detail_creation_technical_empty
import runninghub.composeapp.generated.resources.app_detail_creation_technical_item_format
import runninghub.composeapp.generated.resources.app_detail_creation_technical_title
import runninghub.composeapp.generated.resources.app_detail_default_app_name

/**
 * App 详情页创作入口卡片。
 *
 * 从 AppDetailScreen.kt 拆分而来，承载创作入口卡片及其内部的紧凑封面、
 * 简介摘要、消耗信息行和技术信息折叠；参数编辑统一由下方配置参数区
 * (AppDetailParamsSection) 承担，此卡片不再重复渲染只读输入预览。
 * 颜色统一读取 RhTheme 语义 token。
 */

@Composable
internal fun AppDetailCreationEntry(
    entry: AppDetailCreationEntryUiModel,
    detail: AppDetail,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceDefault)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.app_detail_back_content_description),
                    tint = colors.textPrimary,
                )
            }
            CompactDetailCover(detail = detail)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = entry.title.ifBlank { stringResource(Res.string.app_detail_default_app_name) },
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.app_detail_creation_description_title),
                    color = colors.brandPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                entry.description?.let { description ->
                    Text(
                        text = description,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        CreationInfoRow(
            title = stringResource(Res.string.app_detail_creation_cost_title),
            value = entry.estimatedCost.amountLabel ?: stringResource(Res.string.app_detail_creation_cost_unknown),
        )

        CollapsibleSection(
            title = stringResource(Res.string.app_detail_creation_technical_title),
            initiallyExpanded = entry.technicalDetailsExpanded,
        ) {
            if (entry.technicalDetails.isEmpty()) {
                Text(
                    text = stringResource(Res.string.app_detail_creation_technical_empty),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    entry.technicalDetails.forEach { detailItem ->
                        Text(
                            text = stringResource(
                                Res.string.app_detail_creation_technical_item_format,
                                detailItem.key,
                                detailItem.value,
                            ),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDetailCover(detail: AppDetail) {
    val coverUrl = detail.covers.firstOrNull()?.url
    Box(
        modifier = Modifier
            .size(86.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(RhTheme.colors.surfaceElevated),
        contentAlignment = Alignment.Center,
    ) {
        if (!coverUrl.isNullOrBlank()) {
            SmartAsyncImage(
                imageUrl = coverUrl,
                contentDescription = detail.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = RhTheme.colors.brandPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun CreationInfoRow(title: String, value: String) {
    val colors = RhTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.textSecondary,
            fontSize = 13.sp,
        )
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
