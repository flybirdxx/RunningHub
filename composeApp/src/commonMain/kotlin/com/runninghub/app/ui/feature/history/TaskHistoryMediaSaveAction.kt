package com.runninghub.app.ui.feature.history

import com.runninghub.app.platform.MediaSaveFailureReason
import com.runninghub.app.platform.MediaSaveResult
import com.runninghub.app.platform.MediaSaver
import com.runninghub.feature.task.presentation.TaskHistoryDetailMediaType
import com.runninghub.feature.task.presentation.TaskHistoryDetailOutputUi

/**
 * 历史详情结果保存分发。
 *
 * 页面层只知道任务输出的稳定媒体类型，不直接判断平台 API；这里把图片和视频分别转交给
 * [MediaSaver]，文件类输出保持不可保存，避免把未知文件写入系统相册造成错误语义。
 */
internal suspend fun saveTaskHistoryDetailOutputToGallery(
    mediaSaver: MediaSaver,
    output: TaskHistoryDetailOutputUi,
): MediaSaveResult {
    if (output.url.isBlank()) {
        return MediaSaveResult.Failure(MediaSaveFailureReason.FETCH_FAILED)
    }
    return when (output.mediaType) {
        TaskHistoryDetailMediaType.IMAGE -> mediaSaver.saveImageToGallery(
            url = output.url,
            displayName = TASK_HISTORY_SAVE_DISPLAY_NAME,
        )
        TaskHistoryDetailMediaType.VIDEO -> mediaSaver.saveVideoToGallery(
            url = output.url,
            displayName = TASK_HISTORY_SAVE_DISPLAY_NAME,
        )
        TaskHistoryDetailMediaType.FILE -> MediaSaveResult.Failure(MediaSaveFailureReason.UNSUPPORTED_PLATFORM)
    }
}

private const val TASK_HISTORY_SAVE_DISPLAY_NAME = "runninghub_history"
