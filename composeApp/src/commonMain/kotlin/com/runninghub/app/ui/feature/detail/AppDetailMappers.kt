package com.runninghub.app.ui.feature.detail

import androidx.compose.runtime.Composable
import com.runninghub.app.ui.component.MediaType
import com.runninghub.app.ui.component.TaskStep
import com.runninghub.core.storage.Permission
import com.runninghub.feature.detail.presentation.AppDetailErrorText
import com.runninghub.feature.detail.presentation.AppDetailMediaType
import com.runninghub.feature.detail.presentation.AppDetailTaskStep
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_error_load_failed
import runninghub.composeapp.generated.resources.app_detail_error_task_failed
import runninghub.composeapp.generated.resources.app_detail_error_media_upload_failed
import runninghub.composeapp.generated.resources.app_detail_error_media_upload_pending
import runninghub.composeapp.generated.resources.app_detail_error_task_submit_failed
import runninghub.composeapp.generated.resources.app_detail_error_task_timeout

/**
 * App 详情页展示层映射辅助。
 *
 * 从 AppDetailScreen.kt 拆分而来（纯搬移，无行为变化），承载 Presentation 枚举
 * 到组件层类型的纯映射函数与错误文案解析。
 */

internal fun AppDetailMediaType.permission(): Permission = when (this) {
    AppDetailMediaType.IMAGE -> Permission.MediaImages
    AppDetailMediaType.VIDEO -> Permission.MediaVideo
    AppDetailMediaType.AUDIO -> Permission.MediaAudio
}

internal fun AppDetailMediaType.toComponentMediaType(): MediaType = when (this) {
    AppDetailMediaType.IMAGE -> MediaType.IMAGE
    AppDetailMediaType.VIDEO -> MediaType.VIDEO
    AppDetailMediaType.AUDIO -> MediaType.AUDIO
}

@Composable
internal fun appDetailErrorMessage(error: AppDetailErrorText): String =
    when (error) {
        AppDetailErrorText.DetailLoadFailed -> stringResource(Res.string.app_detail_error_load_failed)
        AppDetailErrorText.TaskSubmitFailed -> stringResource(Res.string.app_detail_error_task_submit_failed)
        AppDetailErrorText.TaskFailed -> stringResource(Res.string.app_detail_error_task_failed)
        AppDetailErrorText.TaskTimeout -> stringResource(Res.string.app_detail_error_task_timeout)
        AppDetailErrorText.MediaUploadPending -> stringResource(Res.string.app_detail_error_media_upload_pending)
        AppDetailErrorText.MediaUploadFailed -> stringResource(Res.string.app_detail_error_media_upload_failed)
    }

internal fun AppDetailTaskStep.toComponentTaskStep(): TaskStep = when (this) {
    AppDetailTaskStep.IDLE -> TaskStep.IDLE
    AppDetailTaskStep.SUBMITTING -> TaskStep.SUBMITTING
    AppDetailTaskStep.QUEUEING -> TaskStep.QUEUEING
    AppDetailTaskStep.RUNNING -> TaskStep.RUNNING
    AppDetailTaskStep.COMPLETING -> TaskStep.COMPLETING
    AppDetailTaskStep.SUCCESS -> TaskStep.SUCCESS
    AppDetailTaskStep.FAILED -> TaskStep.FAILED
}
