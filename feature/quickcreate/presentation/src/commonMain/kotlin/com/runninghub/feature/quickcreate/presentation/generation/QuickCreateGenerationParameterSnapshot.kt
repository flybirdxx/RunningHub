package com.runninghub.feature.quickcreate.presentation.generation

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceSchema
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState

private const val PARAM_ASPECT_RATIO = "aspectRatio"
private const val PARAM_RESOLUTION = "resolution"

/**
 * 点击生成瞬间真正参与提交的核心参数快照。
 *
 * 服务端快捷创作模型会下发默认字段，且这些字段会覆盖本地兼容配置。对话历史和底部摘要必须读取
 * 合并后的有效参数，避免 UI 显示 `1K`，而后台实际收到 `2K`。
 *
 * @property aspectRatio 提交给远端的宽高比协议值，例如 `16:9`；空字符串表示当前模型没有可用值。
 * @property resolution 提交给远端的分辨率协议值，例如 `2K`；空字符串表示当前模型没有可用值。
 */
data class QuickCreateGenerationParameterSnapshot(
    val aspectRatio: String,
    val resolution: String,
)

/**
 * 返回当前页面编辑态的有效提交参数快照。
 *
 * 该函数只读取同步 UiState，不触发网络或上传；生成前后的展示逻辑可复用它来保持参数文案与正式请求一致。
 */
fun QuickCreateUiState.quickCreateGenerationParameterSnapshot(): QuickCreateGenerationParameterSnapshot =
    when (currentTab) {
        QuickCreateTab.IMAGE -> {
            val params = imageQuickCreationEffectiveParams(
                model = selectedImageServiceModel,
                config = imageConfig,
                serviceParams = imageServiceParams,
            )
            QuickCreateGenerationParameterSnapshot(
                aspectRatio = params[PARAM_ASPECT_RATIO]?.trim().orEmpty()
                    .ifBlank { imageConfig.aspectRatio.apiValue },
                resolution = params[PARAM_RESOLUTION]?.normalizedResolutionLabel().orEmpty()
                    .ifBlank { imageConfig.resolution.displayName },
            )
        }
        QuickCreateTab.VIDEO -> {
            val params = videoQuickCreationEffectiveParams(
                model = selectedVideoServiceModel,
                config = videoConfig,
                serviceParams = videoServiceParams,
            )
            QuickCreateGenerationParameterSnapshot(
                aspectRatio = params[PARAM_ASPECT_RATIO]?.trim().orEmpty()
                    .ifBlank { videoConfig.aspectRatio.apiValue },
                resolution = params[PARAM_RESOLUTION]?.normalizedResolutionLabel().orEmpty()
                    .ifBlank { videoConfig.resolution.apiValue },
            )
        }
    }

/**
 * 合并图片快捷创作的内置参数、服务默认值和用户参数。
 *
 * 合并顺序必须与正式请求一致：服务默认值和用户显式选择可以覆盖本地兼容模型的兜底值。
 */
internal fun imageQuickCreationEffectiveParams(
    model: QuickCreationServiceModel?,
    config: ImageConfig,
    serviceParams: Map<String, String>,
): Map<String, String> =
    buildMap {
        put(PARAM_ASPECT_RATIO, config.aspectRatio.apiValue)
        put(PARAM_RESOLUTION, config.resolution.apiValue)
        put("quality", config.quality.apiValue)
        putAll(QuickCreationServiceSchema.defaultParams(model, serviceParams))
        putAll(
            serviceParams
                .filterKeys { key -> key in QuickCreationServiceSchema.activeParamKeys(model, serviceParams) }
                .filterValues { it.isNotBlank() },
        )
    }

/**
 * 合并视频快捷创作的内置参数、服务默认值和用户参数。
 *
 * 合并顺序必须与正式请求一致，供提交请求、计费预览和参数展示共享。
 */
internal fun videoQuickCreationEffectiveParams(
    model: QuickCreationServiceModel?,
    config: VideoConfig,
    serviceParams: Map<String, String>,
): Map<String, String> =
    buildMap {
        put(PARAM_ASPECT_RATIO, config.aspectRatio.apiValue)
        put(PARAM_RESOLUTION, config.resolution.apiValue)
        put("duration", config.duration.seconds.toString())
        putAll(QuickCreationServiceSchema.defaultParams(model, serviceParams))
        putAll(
            serviceParams
                .filterKeys { key -> key in QuickCreationServiceSchema.activeParamKeys(model, serviceParams) }
                .filterValues { it.isNotBlank() },
        )
    }

private fun String.normalizedResolutionLabel(): String {
    val trimmed = trim()
    return when {
        trimmed.endsWith("k", ignoreCase = true) ->
            trimmed.dropLast(1) + "K"
        trimmed.endsWith("p", ignoreCase = true) ->
            trimmed.dropLast(1) + "p"
        else -> trimmed
    }
}
