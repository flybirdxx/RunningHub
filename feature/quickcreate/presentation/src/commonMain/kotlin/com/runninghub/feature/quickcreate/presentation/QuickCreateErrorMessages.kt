package com.runninghub.feature.quickcreate.presentation

import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryException
import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryIssueCode
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskIssueCode

/**
 * 将 QuickCreate Domain/Data 错误语义映射为页面可展示文案。
 *
 * Repository 可能返回 [QuickCreateRepositoryException]、任务错误码字符串，或服务端提供的业务错误摘要。
 * Presentation 在这里集中处理这些情况：稳定错误码映射为本地文案，服务端摘要和普通异常摘要按原样保留，
 * 空错误则使用调用方给定的场景兜底文案。
 *
 * @param fallbackMessage 当前 UI 场景的兜底文案，用于未知空错误。
 * @return 可直接写入 [com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState.error]
 * 或计费错误状态的展示文本。
 */
fun Throwable.toQuickCreateDisplayMessage(fallbackMessage: String): String =
    when (this) {
        is QuickCreateRepositoryException ->
            remoteMessage?.takeIf { it.isNotBlank() } ?: issueCode.toQuickCreateIssueMessage(fallbackMessage)
        else -> message?.let { it.toQuickCreateIssueMessageOrNull() ?: it.takeIf { value -> value.isNotBlank() } }
            ?: fallbackMessage
    }

/**
 * 将稳定错误码映射为 QuickCreate 页面文案。
 *
 * 本函数只处理客户端定义的错误码；服务端返回的非空业务摘要不应在这里改写，
 * 以免丢失运营侧配置的具体失败原因。
 */
private fun String.toQuickCreateIssueMessageOrNull(): String? =
    when (this) {
        QuickCreateTaskIssueCode.TASK_FAILED -> "任务失败"
        QuickCreateTaskIssueCode.TASK_TIMEOUT -> "任务超时"
        QuickCreateTaskIssueCode.TASK_QUERY_FAILED -> "任务查询失败"
        QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED -> "价格预览失败"
        QuickCreateTaskIssueCode.FEE_PREVIEW_BLOCKED -> "余额不足或价格预览未通过"
        QuickCreateTaskIssueCode.PREPARE_FAILED -> "任务预提交失败"
        QuickCreateTaskIssueCode.COMMIT_FAILED -> "任务提交失败"
        QuickCreateTaskIssueCode.UNKNOWN_ERROR -> "生成失败，请稍后重试"
        QuickCreateRepositoryIssueCode.API_KEY_MISSING -> "请先登录后再上传素材"
        QuickCreateRepositoryIssueCode.MEDIA_UPLOAD_FAILED -> "素材上传失败"
        QuickCreateRepositoryIssueCode.MEDIA_UPLOAD_EMPTY_URL -> "素材上传成功但缺少远端地址"
        QuickCreateRepositoryIssueCode.INSPIRATION_TAGS_LOAD_FAILED -> "灵感标签加载失败"
        QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATES_LOAD_FAILED -> "灵感模板加载失败"
        QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATE_DETAIL_LOAD_FAILED -> "模板详情加载失败"
        QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATE_DETAIL_EMPTY -> "模板详情为空"
        QuickCreateRepositoryIssueCode.MODEL_LIST_LOAD_FAILED -> "模型列表加载失败"
        QuickCreateRepositoryIssueCode.HISTORY_LOAD_FAILED -> "历史加载失败"
        QuickCreateRepositoryIssueCode.HISTORY_DETAIL_LOAD_FAILED -> "历史详情加载失败"
        QuickCreateRepositoryIssueCode.TASK_CANCEL_FAILED -> "取消任务失败"
        QuickCreateRepositoryIssueCode.PROJECT_LIST_LOAD_FAILED -> "项目加载失败"
        QuickCreateRepositoryIssueCode.PROJECT_TASK_LIST_LOAD_FAILED -> "项目任务加载失败"
        QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED -> "项目创建失败"
        QuickCreateRepositoryIssueCode.PROJECT_RENAME_FAILED -> "项目重命名失败"
        QuickCreateRepositoryIssueCode.PROJECT_DELETE_FAILED -> "项目删除失败"
        QuickCreateRepositoryIssueCode.PROJECT_PIN_FAILED -> "项目置顶失败"
        QuickCreateRepositoryIssueCode.PROJECT_DETAIL_LOAD_FAILED -> "项目详情加载失败"
        QuickCreateRepositoryIssueCode.PROJECT_ID_MISSING -> "项目数据缺少必要标识"
        else -> null
    }

private fun String.toQuickCreateIssueMessage(fallbackMessage: String): String =
    toQuickCreateIssueMessageOrNull() ?: takeIf { it.isNotBlank() } ?: fallbackMessage
