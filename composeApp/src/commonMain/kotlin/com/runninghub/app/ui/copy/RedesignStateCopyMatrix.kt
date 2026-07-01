package com.runninghub.app.ui.copy

import org.jetbrains.compose.resources.StringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.*

/**
 * RM-14 状态文案的承载面。
 *
 * 这里约束最终文案应该出现在哪里，避免把确认、空态、复制反馈和阻塞提示混用到同一个 UI 通道。
 */
internal enum class RedesignStateCopySurface {
    /** 整页或列表空态。 */
    ScreenEmpty,

    /** 页面内就地提示，不打断当前流程。 */
    InlineNotice,

    /** 短暂反馈，用于复制、保存、网络等轻量提示。 */
    Snackbar,

    /** 需要用户明确确认或理解后再继续的对话框。 */
    Dialog,

    /** 承载生成前确认、复用确认等下方面板流程。 */
    BottomSheet,

    /** 非阻塞的即时反馈。 */
    Toast,

    /** 卡片内部动作或状态。 */
    CardAction,
}

/**
 * RM-14 必须覆盖的状态文案 ID。
 *
 * 枚举项只表达稳定语义；最终中文文案必须通过 [RedesignStateCopySpec] 中的资源键提供。
 */
internal enum class RedesignStateCopyId {
    FIRST_APP_ENTRY,
    HOME_LOADING,
    SEARCH_EMPTY_RESULTS,
    MODEL_LOAD_FAILED,
    MODEL_PRICE_LOAD_FAILED,
    UPLOAD_IMAGE_IN_PROGRESS,
    UPLOAD_IMAGE_SUCCEEDED,
    UPLOAD_IMAGE_FAILED,
    UPLOAD_IMAGE_REMOVED,
    PROMPT_EMPTY,
    PROMPT_TOO_SHORT,
    PARAMETER_MISSING,
    PARAMETER_CONFLICT,
    INSUFFICIENT_BALANCE,
    PRE_GENERATION_PRICE_CONFIRM,
    GENERATION_QUEUED,
    GENERATION_RUNNING,
    GENERATION_SUCCEEDED,
    GENERATION_FAILED,
    GENERATION_CANCELED,
    GENERATION_RETRY,
    COPY_PROMPT_OR_TASK_ID,
    REUSE_PARAMETERS,
    SAVE_TO_LOCAL,
    CLOUD_RESULT_EXPIRES_24H,
    HISTORY_EMPTY,
    NETWORK_ERROR,
}

/**
 * 单个状态文案的资源化规格。
 *
 * @property id 稳定状态 ID，供回归测试和页面映射定位。
 * @property title 可选标题资源；没有标题的轻量反馈保持为空。
 * @property message 正文资源；不得保存最终中文字符串。
 * @property surface 文案承载面，用于区分空态、提示、确认面板和轻量反馈。
 * @property primaryAction 可选主动作资源。
 * @property secondaryAction 可选次动作资源。
 * @property blocksProgress 是否阻塞当前流程继续。
 */
internal data class RedesignStateCopySpec(
    val id: RedesignStateCopyId,
    val title: StringResource? = null,
    val message: StringResource,
    val surface: RedesignStateCopySurface,
    val primaryAction: StringResource? = null,
    val secondaryAction: StringResource? = null,
    val blocksProgress: Boolean = false,
)

/**
 * RM-14 状态文案矩阵。
 *
 * 该矩阵是资源和承载面的回归总表；页面仍按各自状态映射资源，不从 Domain/Data 读取最终文案。
 */
internal object RedesignStateCopyMatrix {
    /** 按 roadmap 顺序排列的状态文案规格。 */
    val all: List<RedesignStateCopySpec> = listOf(
        spec(
            RedesignStateCopyId.FIRST_APP_ENTRY,
            title = Res.string.rm14_state_first_app_entry_title,
            message = Res.string.rm14_state_first_app_entry_message,
            surface = RedesignStateCopySurface.ScreenEmpty,
            primaryAction = Res.string.rm14_state_first_app_entry_primary,
        ),
        spec(
            RedesignStateCopyId.HOME_LOADING,
            title = Res.string.rm14_state_home_loading_title,
            message = Res.string.rm14_state_home_loading_message,
            surface = RedesignStateCopySurface.InlineNotice,
        ),
        spec(
            RedesignStateCopyId.SEARCH_EMPTY_RESULTS,
            title = Res.string.rm14_state_search_empty_title,
            message = Res.string.rm14_state_search_empty_message,
            surface = RedesignStateCopySurface.ScreenEmpty,
        ),
        spec(
            RedesignStateCopyId.MODEL_LOAD_FAILED,
            title = Res.string.rm14_state_model_load_failed_title,
            message = Res.string.rm14_state_model_load_failed_message,
            surface = RedesignStateCopySurface.InlineNotice,
            primaryAction = Res.string.rm14_state_retry_action,
        ),
        spec(
            RedesignStateCopyId.MODEL_PRICE_LOAD_FAILED,
            title = Res.string.rm14_state_model_price_failed_title,
            message = Res.string.rm14_state_model_price_failed_message,
            surface = RedesignStateCopySurface.InlineNotice,
            primaryAction = Res.string.rm14_state_retry_action,
        ),
        spec(
            RedesignStateCopyId.UPLOAD_IMAGE_IN_PROGRESS,
            title = Res.string.rm14_state_uploading_title,
            message = Res.string.rm14_state_uploading_message,
            surface = RedesignStateCopySurface.InlineNotice,
            blocksProgress = true,
        ),
        spec(
            RedesignStateCopyId.UPLOAD_IMAGE_SUCCEEDED,
            title = Res.string.rm14_state_upload_success_title,
            message = Res.string.rm14_state_upload_success_message,
            surface = RedesignStateCopySurface.Snackbar,
        ),
        spec(
            RedesignStateCopyId.UPLOAD_IMAGE_FAILED,
            title = Res.string.rm14_state_upload_failed_title,
            message = Res.string.rm14_state_upload_failed_message,
            surface = RedesignStateCopySurface.InlineNotice,
            primaryAction = Res.string.rm14_state_retry_action,
        ),
        spec(
            RedesignStateCopyId.UPLOAD_IMAGE_REMOVED,
            title = Res.string.rm14_state_upload_removed_title,
            message = Res.string.rm14_state_upload_removed_message,
            surface = RedesignStateCopySurface.Snackbar,
        ),
        spec(
            RedesignStateCopyId.PROMPT_EMPTY,
            title = Res.string.rm14_state_prompt_empty_title,
            message = Res.string.rm14_state_prompt_empty_message,
            surface = RedesignStateCopySurface.InlineNotice,
            blocksProgress = true,
        ),
        spec(
            RedesignStateCopyId.PROMPT_TOO_SHORT,
            title = Res.string.rm14_state_prompt_too_short_title,
            message = Res.string.rm14_state_prompt_too_short_message,
            surface = RedesignStateCopySurface.InlineNotice,
            blocksProgress = true,
        ),
        spec(
            RedesignStateCopyId.PARAMETER_MISSING,
            title = Res.string.rm14_state_parameter_missing_title,
            message = Res.string.rm14_state_parameter_missing_message,
            surface = RedesignStateCopySurface.InlineNotice,
            blocksProgress = true,
        ),
        spec(
            RedesignStateCopyId.PARAMETER_CONFLICT,
            title = Res.string.rm14_state_parameter_conflict_title,
            message = Res.string.rm14_state_parameter_conflict_message,
            surface = RedesignStateCopySurface.InlineNotice,
            blocksProgress = true,
        ),
        spec(
            RedesignStateCopyId.INSUFFICIENT_BALANCE,
            title = Res.string.rm14_state_insufficient_balance_title,
            message = Res.string.rm14_state_insufficient_balance_message,
            surface = RedesignStateCopySurface.InlineNotice,
            primaryAction = Res.string.rm14_state_recharge_action,
            blocksProgress = true,
        ),
        spec(
            RedesignStateCopyId.PRE_GENERATION_PRICE_CONFIRM,
            title = Res.string.rm14_state_price_confirm_title,
            message = Res.string.rm14_state_price_confirm_message,
            surface = RedesignStateCopySurface.BottomSheet,
            primaryAction = Res.string.rm14_state_confirm_action,
            secondaryAction = Res.string.rm14_state_cancel_action,
            blocksProgress = true,
        ),
        spec(
            RedesignStateCopyId.GENERATION_QUEUED,
            title = Res.string.rm14_state_generation_queued_title,
            message = Res.string.rm14_state_generation_queued_message,
            surface = RedesignStateCopySurface.InlineNotice,
        ),
        spec(
            RedesignStateCopyId.GENERATION_RUNNING,
            title = Res.string.rm14_state_generation_running_title,
            message = Res.string.rm14_state_generation_running_message,
            surface = RedesignStateCopySurface.InlineNotice,
        ),
        spec(
            RedesignStateCopyId.GENERATION_SUCCEEDED,
            title = Res.string.rm14_state_generation_succeeded_title,
            message = Res.string.rm14_state_generation_succeeded_message,
            surface = RedesignStateCopySurface.InlineNotice,
            primaryAction = Res.string.rm14_state_view_result_action,
        ),
        spec(
            RedesignStateCopyId.GENERATION_FAILED,
            title = Res.string.rm14_state_generation_failed_title,
            message = Res.string.rm14_state_generation_failed_message,
            surface = RedesignStateCopySurface.InlineNotice,
            primaryAction = Res.string.rm14_state_retry_action,
        ),
        spec(
            RedesignStateCopyId.GENERATION_CANCELED,
            title = Res.string.rm14_state_generation_canceled_title,
            message = Res.string.rm14_state_generation_canceled_message,
            surface = RedesignStateCopySurface.InlineNotice,
        ),
        spec(
            RedesignStateCopyId.GENERATION_RETRY,
            title = Res.string.rm14_state_generation_retry_title,
            message = Res.string.rm14_state_generation_retry_message,
            surface = RedesignStateCopySurface.CardAction,
            primaryAction = Res.string.rm14_state_retry_action,
        ),
        spec(
            RedesignStateCopyId.COPY_PROMPT_OR_TASK_ID,
            title = Res.string.rm14_state_copy_title,
            message = Res.string.rm14_state_copy_message,
            surface = RedesignStateCopySurface.Snackbar,
        ),
        spec(
            RedesignStateCopyId.REUSE_PARAMETERS,
            title = Res.string.rm14_state_reuse_parameters_title,
            message = Res.string.rm14_state_reuse_parameters_message,
            surface = RedesignStateCopySurface.BottomSheet,
            primaryAction = Res.string.rm14_state_reuse_action,
            secondaryAction = Res.string.rm14_state_cancel_action,
        ),
        spec(
            RedesignStateCopyId.SAVE_TO_LOCAL,
            title = Res.string.rm14_state_save_local_title,
            message = Res.string.rm14_state_save_local_message,
            surface = RedesignStateCopySurface.Snackbar,
        ),
        spec(
            RedesignStateCopyId.CLOUD_RESULT_EXPIRES_24H,
            title = Res.string.rm14_state_cloud_expire_title,
            message = Res.string.rm14_state_cloud_expire_message,
            surface = RedesignStateCopySurface.InlineNotice,
        ),
        spec(
            RedesignStateCopyId.HISTORY_EMPTY,
            title = Res.string.rm14_state_history_empty_title,
            message = Res.string.rm14_state_history_empty_message,
            surface = RedesignStateCopySurface.ScreenEmpty,
        ),
        spec(
            RedesignStateCopyId.NETWORK_ERROR,
            title = Res.string.rm14_state_network_error_title,
            message = Res.string.rm14_state_network_error_message,
            surface = RedesignStateCopySurface.Snackbar,
            primaryAction = Res.string.rm14_state_retry_action,
        ),
    )

    private val byId: Map<RedesignStateCopyId, RedesignStateCopySpec> = all.associateBy { it.id }

    /** 按稳定 ID 获取状态文案规格；缺失表示矩阵没有覆盖该 roadmap 状态。 */
    operator fun get(id: RedesignStateCopyId): RedesignStateCopySpec =
        byId.getValue(id)

    private fun spec(
        id: RedesignStateCopyId,
        title: StringResource,
        message: StringResource,
        surface: RedesignStateCopySurface,
        primaryAction: StringResource? = null,
        secondaryAction: StringResource? = null,
        blocksProgress: Boolean = false,
    ): RedesignStateCopySpec = RedesignStateCopySpec(
        id = id,
        title = title,
        message = message,
        surface = surface,
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
        blocksProgress = blocksProgress,
    )
}
