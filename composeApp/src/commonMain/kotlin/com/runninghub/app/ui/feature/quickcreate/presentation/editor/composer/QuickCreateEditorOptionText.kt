package com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer

import androidx.compose.runtime.Composable
import com.runninghub.feature.quickcreate.presentation.editor.ImageModelLabel
import com.runninghub.feature.quickcreate.presentation.editor.ImageQualityLabel
import com.runninghub.feature.quickcreate.presentation.editor.VideoModelLabel
import com.runninghub.feature.quickcreate.presentation.editor.VideoDurationLabel
import com.runninghub.feature.quickcreate.presentation.editor.VideoResolutionLabel
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_image_model_all_power_image_g2
import runninghub.composeapp.generated.resources.quick_create_image_quality_high
import runninghub.composeapp.generated.resources.quick_create_image_quality_low
import runninghub.composeapp.generated.resources.quick_create_image_quality_medium
import runninghub.composeapp.generated.resources.quick_create_video_duration_10_seconds
import runninghub.composeapp.generated.resources.quick_create_video_duration_5_seconds
import runninghub.composeapp.generated.resources.quick_create_video_model_kling_o1
import runninghub.composeapp.generated.resources.quick_create_video_model_kling_o3_4k
import runninghub.composeapp.generated.resources.quick_create_video_model_wanxiang_2_6
import runninghub.composeapp.generated.resources.quick_create_video_model_wanxiang_2_7
import runninghub.composeapp.generated.resources.quick_create_video_resolution_native_1080p

/**
 * 将本地兼容图片模型展示语义映射为应用资源文案。
 *
 * 这些模型只作为服务端模型不可用时的旧参数兜底；固定中文名称必须留在资源层，
 * 第三方英文商品名可作为运行时文本直接展示。
 */
@Composable
internal fun ImageModelLabel.asImageModelText(): String =
    when (this) {
        ImageModelLabel.AllPowerImageG2 ->
            stringResource(Res.string.quick_create_image_model_all_power_image_g2)
        is ImageModelLabel.RuntimeName -> value
    }

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

/**
 * 将本地兼容视频模型展示语义映射为应用资源文案。
 *
 * 固定中文模型名集中在资源层；不含本地化语义的英文商品名保留为运行时文本。
 */
@Composable
internal fun VideoModelLabel.asVideoModelText(): String =
    when (this) {
        is VideoModelLabel.RuntimeName -> value
        VideoModelLabel.Wanxiang26 -> stringResource(Res.string.quick_create_video_model_wanxiang_2_6)
        VideoModelLabel.Wanxiang27 -> stringResource(Res.string.quick_create_video_model_wanxiang_2_7)
        VideoModelLabel.KlingO1 -> stringResource(Res.string.quick_create_video_model_kling_o1)
        VideoModelLabel.KlingO34k -> stringResource(Res.string.quick_create_video_model_kling_o3_4k)
    }

/**
 * 将视频分辨率展示语义映射为旧版编辑器参数 chip 文案。
 *
 * 标准规格值本身不含中文本地化内容，可以直接展示；原生 1080p 是固定 UI 文案，
 * 因此从资源读取，避免 feature presentation 枚举保存中文字符串。
 */
@Composable
internal fun VideoResolutionLabel.asVideoResolutionText(): String =
    when (this) {
        is VideoResolutionLabel.Standard -> value
        VideoResolutionLabel.Native1080p -> stringResource(
            Res.string.quick_create_video_resolution_native_1080p,
        )
    }

/**
 * 将视频时长展示语义映射为旧版编辑器参数 chip 文案。
 *
 * 时长秒数仍通过 [com.runninghub.feature.quickcreate.presentation.editor.VideoDuration.seconds]
 * 参与旧接口请求和计费估算；这里仅处理最终用户可见文案。
 */
@Composable
internal fun VideoDurationLabel.asVideoDurationText(): String =
    when (this) {
        VideoDurationLabel.Seconds5 -> stringResource(Res.string.quick_create_video_duration_5_seconds)
        VideoDurationLabel.Seconds10 -> stringResource(Res.string.quick_create_video_duration_10_seconds)
    }
