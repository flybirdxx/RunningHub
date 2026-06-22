package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.runtime.Composable
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.QuickCreateRuntimeUiText
import com.runninghub.feature.quickcreate.presentation.QuickCreateUiMessage
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_error_auth_required_for_upload
import runninghub.composeapp.generated.resources.quick_create_error_commit_failed
import runninghub.composeapp.generated.resources.quick_create_error_fee_preview_blocked
import runninghub.composeapp.generated.resources.quick_create_error_fee_preview_failed
import runninghub.composeapp.generated.resources.quick_create_error_generation_failed
import runninghub.composeapp.generated.resources.quick_create_error_history_detail_load_failed
import runninghub.composeapp.generated.resources.quick_create_error_history_load_failed
import runninghub.composeapp.generated.resources.quick_create_error_history_refresh_failed
import runninghub.composeapp.generated.resources.quick_create_error_inspiration_tags_load_failed
import runninghub.composeapp.generated.resources.quick_create_error_inspiration_template_detail_empty
import runninghub.composeapp.generated.resources.quick_create_error_inspiration_template_detail_load_failed
import runninghub.composeapp.generated.resources.quick_create_error_inspiration_templates_load_failed
import runninghub.composeapp.generated.resources.quick_create_error_media_upload_empty_url
import runninghub.composeapp.generated.resources.quick_create_error_media_upload_failed
import runninghub.composeapp.generated.resources.quick_create_error_model_list_load_failed
import runninghub.composeapp.generated.resources.quick_create_error_prepare_failed
import runninghub.composeapp.generated.resources.quick_create_error_project_create_failed
import runninghub.composeapp.generated.resources.quick_create_error_project_delete_failed
import runninghub.composeapp.generated.resources.quick_create_error_project_detail_load_failed
import runninghub.composeapp.generated.resources.quick_create_error_project_id_missing
import runninghub.composeapp.generated.resources.quick_create_error_project_list_load_failed
import runninghub.composeapp.generated.resources.quick_create_error_project_pin_failed
import runninghub.composeapp.generated.resources.quick_create_error_project_rename_failed
import runninghub.composeapp.generated.resources.quick_create_error_project_task_list_load_failed
import runninghub.composeapp.generated.resources.quick_create_error_task_cancel_failed
import runninghub.composeapp.generated.resources.quick_create_error_task_failed
import runninghub.composeapp.generated.resources.quick_create_error_task_query_failed
import runninghub.composeapp.generated.resources.quick_create_error_task_timeout
import runninghub.composeapp.generated.resources.quick_create_runtime_duplicate_generation
import runninghub.composeapp.generated.resources.quick_create_runtime_fee_confirming
import runninghub.composeapp.generated.resources.quick_create_runtime_fee_pending
import runninghub.composeapp.generated.resources.quick_create_runtime_media_upload_blocked
import runninghub.composeapp.generated.resources.quick_create_runtime_media_upload_failed
import runninghub.composeapp.generated.resources.quick_create_runtime_media_upload_timeout
import runninghub.composeapp.generated.resources.quick_create_runtime_prompt_required
import runninghub.composeapp.generated.resources.quick_create_runtime_prompt_too_long_format
import runninghub.composeapp.generated.resources.quick_create_runtime_service_field_invalid_option_format
import runninghub.composeapp.generated.resources.quick_create_runtime_service_field_min_length_format
import runninghub.composeapp.generated.resources.quick_create_runtime_service_field_required_format
import runninghub.composeapp.generated.resources.quick_create_runtime_service_upload_max_count_format

/**
 * 将 QuickCreate Presentation 的页面消息语义映射为最终展示文本。
 *
 * 运行时提示和错误语义通过 Compose Resources 获取文案；服务端字段标题等动态内容只作为格式串参数传入，
 * 不由 Presentation 保存最终中文句子。
 */
@Composable
internal fun QuickCreateUiMessage.asQuickCreateText(): String =
    when (this) {
        is QuickCreateUiMessage.PresentationErrorText -> error.asQuickCreateErrorText()
        is QuickCreateUiMessage.RuntimeText -> text.asQuickCreateRuntimeText()
    }

/**
 * 将 QuickCreate Presentation 错误语义映射为最终本地化文案。
 */
@Composable
internal fun QuickCreatePresentationError.asQuickCreateErrorText(): String =
    when (this) {
        QuickCreatePresentationError.FeePreviewFailed ->
            stringResource(Res.string.quick_create_error_fee_preview_failed)
        QuickCreatePresentationError.FeePreviewNotPassed ->
            stringResource(Res.string.quick_create_error_fee_preview_blocked)
        QuickCreatePresentationError.GenerationFailed ->
            stringResource(Res.string.quick_create_error_generation_failed)
        QuickCreatePresentationError.TaskFailed ->
            stringResource(Res.string.quick_create_error_task_failed)
        QuickCreatePresentationError.TaskTimeout ->
            stringResource(Res.string.quick_create_error_task_timeout)
        QuickCreatePresentationError.TaskQueryFailed ->
            stringResource(Res.string.quick_create_error_task_query_failed)
        QuickCreatePresentationError.PrepareFailed ->
            stringResource(Res.string.quick_create_error_prepare_failed)
        QuickCreatePresentationError.CommitFailed ->
            stringResource(Res.string.quick_create_error_commit_failed)
        QuickCreatePresentationError.ApiKeyMissing ->
            stringResource(Res.string.quick_create_error_auth_required_for_upload)
        QuickCreatePresentationError.MediaUploadFailed ->
            stringResource(Res.string.quick_create_error_media_upload_failed)
        QuickCreatePresentationError.MediaUploadEmptyUrl ->
            stringResource(Res.string.quick_create_error_media_upload_empty_url)
        QuickCreatePresentationError.InspirationTagsLoadFailed ->
            stringResource(Res.string.quick_create_error_inspiration_tags_load_failed)
        QuickCreatePresentationError.InspirationTemplatesLoadFailed ->
            stringResource(Res.string.quick_create_error_inspiration_templates_load_failed)
        QuickCreatePresentationError.InspirationTemplateDetailLoadFailed ->
            stringResource(Res.string.quick_create_error_inspiration_template_detail_load_failed)
        QuickCreatePresentationError.InspirationTemplateDetailEmpty ->
            stringResource(Res.string.quick_create_error_inspiration_template_detail_empty)
        QuickCreatePresentationError.ModelListLoadFailed ->
            stringResource(Res.string.quick_create_error_model_list_load_failed)
        QuickCreatePresentationError.HistoryLoadFailed ->
            stringResource(Res.string.quick_create_error_history_load_failed)
        QuickCreatePresentationError.HistoryDetailLoadFailed ->
            stringResource(Res.string.quick_create_error_history_detail_load_failed)
        QuickCreatePresentationError.TaskCancelFailed ->
            stringResource(Res.string.quick_create_error_task_cancel_failed)
        QuickCreatePresentationError.HistoryRefreshFailed ->
            stringResource(Res.string.quick_create_error_history_refresh_failed)
        QuickCreatePresentationError.ProjectListLoadFailed ->
            stringResource(Res.string.quick_create_error_project_list_load_failed)
        QuickCreatePresentationError.ProjectTaskListLoadFailed ->
            stringResource(Res.string.quick_create_error_project_task_list_load_failed)
        QuickCreatePresentationError.ProjectCreateFailed ->
            stringResource(Res.string.quick_create_error_project_create_failed)
        QuickCreatePresentationError.ProjectRenameFailed ->
            stringResource(Res.string.quick_create_error_project_rename_failed)
        QuickCreatePresentationError.ProjectDeleteFailed ->
            stringResource(Res.string.quick_create_error_project_delete_failed)
        QuickCreatePresentationError.ProjectPinFailed ->
            stringResource(Res.string.quick_create_error_project_pin_failed)
        QuickCreatePresentationError.ProjectDetailLoadFailed ->
            stringResource(Res.string.quick_create_error_project_detail_load_failed)
        QuickCreatePresentationError.ProjectIdMissing ->
            stringResource(Res.string.quick_create_error_project_id_missing)
    }

@Composable
private fun QuickCreateRuntimeUiText.asQuickCreateRuntimeText(): String =
    when (this) {
        QuickCreateRuntimeUiText.FeeConfirming ->
            stringResource(Res.string.quick_create_runtime_fee_confirming)
        QuickCreateRuntimeUiText.FeePending ->
            stringResource(Res.string.quick_create_runtime_fee_pending)
        QuickCreateRuntimeUiText.DuplicateGeneration ->
            stringResource(Res.string.quick_create_runtime_duplicate_generation)
        QuickCreateRuntimeUiText.PromptRequired ->
            stringResource(Res.string.quick_create_runtime_prompt_required)
        is QuickCreateRuntimeUiText.PromptTooLong ->
            stringResource(Res.string.quick_create_runtime_prompt_too_long_format, maxChars)
        QuickCreateRuntimeUiText.MediaUploadFailed ->
            stringResource(Res.string.quick_create_runtime_media_upload_failed)
        QuickCreateRuntimeUiText.MediaUploadBlocked ->
            stringResource(Res.string.quick_create_runtime_media_upload_blocked)
        QuickCreateRuntimeUiText.MediaUploadTimeout ->
            stringResource(Res.string.quick_create_runtime_media_upload_timeout)
        is QuickCreateRuntimeUiText.ServiceFieldRequired ->
            stringResource(Res.string.quick_create_runtime_service_field_required_format, fieldTitle)
        is QuickCreateRuntimeUiText.ServiceFieldInvalidOption ->
            stringResource(Res.string.quick_create_runtime_service_field_invalid_option_format, fieldTitle)
        is QuickCreateRuntimeUiText.ServiceFieldMinLength ->
            stringResource(Res.string.quick_create_runtime_service_field_min_length_format, fieldTitle, minLength)
        is QuickCreateRuntimeUiText.ServiceUploadMaxCount ->
            stringResource(Res.string.quick_create_runtime_service_upload_max_count_format, fieldTitle, maxCount)
    }
