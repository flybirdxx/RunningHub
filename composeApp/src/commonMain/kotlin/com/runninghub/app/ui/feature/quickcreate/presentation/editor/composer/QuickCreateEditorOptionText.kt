package com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer

import androidx.compose.runtime.Composable
import com.runninghub.feature.quickcreate.presentation.editor.ImageQualityLabel
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_image_quality_high
import runninghub.composeapp.generated.resources.quick_create_image_quality_low
import runninghub.composeapp.generated.resources.quick_create_image_quality_medium

/**
 * 将图片质量展示语义映射为 composeApp 本地资源文案。
 *
 * feature presentation 只保留质量档位语义和旧接口 `apiValue`，最终中文文案集中在应用壳资源层，
 * 避免本地枚举继续扩大硬编码 UI 文案基线。
 */
@Composable
internal fun ImageQualityLabel.asImageQualityText(): String =
    when (this) {
        ImageQualityLabel.Low -> stringResource(Res.string.quick_create_image_quality_low)
        ImageQualityLabel.Medium -> stringResource(Res.string.quick_create_image_quality_medium)
        ImageQualityLabel.High -> stringResource(Res.string.quick_create_image_quality_high)
    }
