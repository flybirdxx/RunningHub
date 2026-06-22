package com.runninghub.feature.quickcreate.presentation

import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryException
import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryIssueCode
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskIssueCode

/**
 * QuickCreate 页面错误提示的稳定语义。
 *
 * 该枚举位于 feature presentation 层，只描述用户可见错误的业务含义，不保存最终中文文案。
 * Domain/Data 层返回的 Repository 错误码、任务错误码和未知异常都会先映射到这里，再由 composeApp
 * 使用 Compose Resources 转成最终展示文本，避免 `Throwable.message` 或服务端摘要直接进入页面状态。
 */
enum class QuickCreatePresentationError {
    /** 计费预览接口失败、响应缺字段或本地无法确认当前价格。 */
    FeePreviewFailed,

    /** 计费预览明确表示余额不足，或当前价格预览未通过业务校验。 */
    FeePreviewNotPassed,

    /** 生成任务失败且没有更具体的任务错误语义。 */
    GenerationFailed,

    /** 远端任务以失败终态结束。 */
    TaskFailed,

    /** 任务轮询超过客户端允许的等待窗口。 */
    TaskTimeout,

    /** 查询任务状态或任务列表失败。 */
    TaskQueryFailed,

    /** 任务预提交阶段失败，无法继续 commit。 */
    PrepareFailed,

    /** 任务 commit 阶段失败，远端没有创建可轮询任务。 */
    CommitFailed,

    /** 上传素材前缺少登录态或 API Key。 */
    ApiKeyMissing,

    /** 媒体上传接口失败或远端拒绝当前素材。 */
    MediaUploadFailed,

    /** 媒体上传响应成功但没有返回可提交的远端 URL。 */
    MediaUploadEmptyUrl,

    /** 灵感标签列表加载失败。 */
    InspirationTagsLoadFailed,

    /** 灵感模板分页加载失败。 */
    InspirationTemplatesLoadFailed,

    /** 灵感模板详情加载失败。 */
    InspirationTemplateDetailLoadFailed,

    /** 灵感模板详情响应成功但缺少详情数据。 */
    InspirationTemplateDetailEmpty,

    /** 快捷创作模型目录加载失败。 */
    ModelListLoadFailed,

    /** 最近历史列表加载失败。 */
    HistoryLoadFailed,

    /** 历史输出详情加载失败。 */
    HistoryDetailLoadFailed,

    /** 取消历史任务失败。 */
    TaskCancelFailed,

    /** 历史区域轮询刷新失败。 */
    HistoryRefreshFailed,

    /** 项目列表加载失败。 */
    ProjectListLoadFailed,

    /** 项目任务列表加载失败。 */
    ProjectTaskListLoadFailed,

    /** 创建项目失败。 */
    ProjectCreateFailed,

    /** 重命名项目失败。 */
    ProjectRenameFailed,

    /** 删除项目失败。 */
    ProjectDeleteFailed,

    /** 切换项目置顶状态失败。 */
    ProjectPinFailed,

    /** 项目详情加载失败。 */
    ProjectDetailLoadFailed,

    /** 服务端项目数据缺少稳定项目 ID。 */
    ProjectIdMissing,
}

/**
 * 将异常按当前页面场景映射为 QuickCreate 稳定错误语义。
 *
 * Repository 的稳定错误码优先级最高；普通异常只识别客户端定义的任务错误码。
 * 未识别的远端摘要或异常消息统一降级到调用方传入的 [fallback]，防止内部诊断信息进入 UI。
 *
 * @param fallback 当前调用场景的兜底错误语义，用于未知异常、空消息和未识别服务端摘要。
 * @return 可写入 [QuickCreateUiMessage] 的稳定错误语义。
 */
fun Throwable.toQuickCreatePresentationError(
    fallback: QuickCreatePresentationError,
): QuickCreatePresentationError =
    when (this) {
        is QuickCreateRepositoryException -> issueCode.toQuickCreatePresentationErrorOrNull() ?: fallback
        else -> message?.toQuickCreatePresentationErrorOrNull() ?: fallback
    }

/**
 * 将异常映射为可写入 QuickCreate 页面错误槽的消息语义。
 *
 * @param fallback 当前调用场景的兜底错误语义。
 * @return 不含最终中文文案的页面消息。
 */
fun Throwable.toQuickCreateUiMessage(
    fallback: QuickCreatePresentationError,
): QuickCreateUiMessage =
    toQuickCreatePresentationError(fallback).asQuickCreateUiMessage()

/**
 * 将任务轮询返回的错误码或异常摘要映射为稳定页面错误语义。
 *
 * 轮询控制器只应处理状态转换，不维护第二套中文文案。未知字符串统一降级为
 * [QuickCreatePresentationError.GenerationFailed]，避免服务端 `msg/message` 直接展示给用户。
 */
internal fun String.toQuickCreateTaskIssueError(): QuickCreatePresentationError =
    toQuickCreatePresentationErrorOrNull() ?: QuickCreatePresentationError.GenerationFailed

/**
 * 将稳定错误语义包装为页面消息。
 *
 * @return 可由 composeApp 资源层映射为最终文案的页面消息。
 */
internal fun QuickCreatePresentationError.asQuickCreateUiMessage(): QuickCreateUiMessage =
    QuickCreateUiMessage.PresentationErrorText(this)

private fun String.toQuickCreatePresentationErrorOrNull(): QuickCreatePresentationError? =
    when (this) {
        QuickCreateTaskIssueCode.TASK_FAILED -> QuickCreatePresentationError.TaskFailed
        QuickCreateTaskIssueCode.TASK_TIMEOUT -> QuickCreatePresentationError.TaskTimeout
        QuickCreateTaskIssueCode.TASK_QUERY_FAILED -> QuickCreatePresentationError.TaskQueryFailed
        QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED -> QuickCreatePresentationError.FeePreviewFailed
        QuickCreateTaskIssueCode.FEE_PREVIEW_BLOCKED -> QuickCreatePresentationError.FeePreviewNotPassed
        QuickCreateTaskIssueCode.PREPARE_FAILED -> QuickCreatePresentationError.PrepareFailed
        QuickCreateTaskIssueCode.COMMIT_FAILED -> QuickCreatePresentationError.CommitFailed
        QuickCreateTaskIssueCode.UNKNOWN_ERROR -> QuickCreatePresentationError.GenerationFailed
        QuickCreateRepositoryIssueCode.API_KEY_MISSING -> QuickCreatePresentationError.ApiKeyMissing
        QuickCreateRepositoryIssueCode.MEDIA_UPLOAD_FAILED -> QuickCreatePresentationError.MediaUploadFailed
        QuickCreateRepositoryIssueCode.MEDIA_UPLOAD_EMPTY_URL -> QuickCreatePresentationError.MediaUploadEmptyUrl
        QuickCreateRepositoryIssueCode.INSPIRATION_TAGS_LOAD_FAILED ->
            QuickCreatePresentationError.InspirationTagsLoadFailed
        QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATES_LOAD_FAILED ->
            QuickCreatePresentationError.InspirationTemplatesLoadFailed
        QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATE_DETAIL_LOAD_FAILED ->
            QuickCreatePresentationError.InspirationTemplateDetailLoadFailed
        QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATE_DETAIL_EMPTY ->
            QuickCreatePresentationError.InspirationTemplateDetailEmpty
        QuickCreateRepositoryIssueCode.MODEL_LIST_LOAD_FAILED -> QuickCreatePresentationError.ModelListLoadFailed
        QuickCreateRepositoryIssueCode.HISTORY_LOAD_FAILED -> QuickCreatePresentationError.HistoryLoadFailed
        QuickCreateRepositoryIssueCode.HISTORY_DETAIL_LOAD_FAILED ->
            QuickCreatePresentationError.HistoryDetailLoadFailed
        QuickCreateRepositoryIssueCode.TASK_CANCEL_FAILED -> QuickCreatePresentationError.TaskCancelFailed
        QuickCreateRepositoryIssueCode.PROJECT_LIST_LOAD_FAILED -> QuickCreatePresentationError.ProjectListLoadFailed
        QuickCreateRepositoryIssueCode.PROJECT_TASK_LIST_LOAD_FAILED ->
            QuickCreatePresentationError.ProjectTaskListLoadFailed
        QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED -> QuickCreatePresentationError.ProjectCreateFailed
        QuickCreateRepositoryIssueCode.PROJECT_RENAME_FAILED -> QuickCreatePresentationError.ProjectRenameFailed
        QuickCreateRepositoryIssueCode.PROJECT_DELETE_FAILED -> QuickCreatePresentationError.ProjectDeleteFailed
        QuickCreateRepositoryIssueCode.PROJECT_PIN_FAILED -> QuickCreatePresentationError.ProjectPinFailed
        QuickCreateRepositoryIssueCode.PROJECT_DETAIL_LOAD_FAILED -> QuickCreatePresentationError.ProjectDetailLoadFailed
        QuickCreateRepositoryIssueCode.PROJECT_ID_MISSING -> QuickCreatePresentationError.ProjectIdMissing
        else -> null
    }
