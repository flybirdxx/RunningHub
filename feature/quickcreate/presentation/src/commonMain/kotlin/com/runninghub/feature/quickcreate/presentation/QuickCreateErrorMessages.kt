package com.runninghub.feature.quickcreate.presentation

import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryException
import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryIssueCode
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskIssueCode

/**
 * 将 QuickCreate Domain/Data 错误语义映射为页面可展示文案。
 *
 * Repository 可能返回 [QuickCreateRepositoryException]、任务错误码字符串或普通异常摘要。
 * Presentation 在这里集中处理这些情况：稳定错误码映射为本地文案，未识别摘要统一使用调用方给定的
 * 场景兜底文案，避免服务端 `msg/message` 或内部异常消息直接成为最终 UI 文案。
 *
 * @param fallbackMessage 当前 UI 场景的兜底文案，用于未知空错误。
 * @return 可直接写入 [com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState.error]
 * 或计费错误状态的展示文本。
 */
fun Throwable.toQuickCreateDisplayMessage(fallbackMessage: String): String =
    when (this) {
        is QuickCreateRepositoryException ->
            issueCode.toQuickCreateIssueMessage(fallbackMessage)
        else -> message?.let { it.toQuickCreateIssueMessageOrNull() } ?: fallbackMessage
    }

/**
 * QuickCreate Presentation 层仍需要保留的本地错误兜底文案。
 *
 * 这些值作为 `toQuickCreateDisplayMessage` 的 fallback 传入，确保未知异常、
 * 服务端原始摘要或调试信息不会直接写入页面状态。后续当 QuickCreate UI 壳整体迁出
 * composeApp 或接入注入式 TextProvider 时，可把这些稳定场景继续改为资源 key。
 */
internal object QuickCreateErrorFallbackText {
    /** 计费预览请求失败时的页面兜底文案，覆盖网络异常、服务端异常和未知预览失败。 */
    const val FEE_PREVIEW_FAILED: String = "价格预览失败"

    /** 计费预览明确未通过时的业务拦截文案，覆盖余额不足或价格校验失败。 */
    const val FEE_PREVIEW_NOT_PASSED: String = "余额不足或价格预览未通过"

    /** 灵感标签请求失败时的页面兜底文案，覆盖标签列表初始化流程。 */
    const val INSPIRATION_TAGS_LOAD_FAILED: String = "灵感标签加载失败"

    /** 灵感模板第一页或下一页请求失败时的页面兜底文案，覆盖模板分页流程。 */
    const val INSPIRATION_TEMPLATES_LOAD_FAILED: String = "灵感模板加载失败"

    /** 灵感模板详情请求失败时的页面兜底文案，覆盖模板应用前的详情加载流程。 */
    const val INSPIRATION_TEMPLATE_DETAIL_LOAD_FAILED: String = "模板详情加载失败"

    /** 项目列表第一页或下一页请求失败时的页面兜底文案，覆盖项目入口和分页加载流程。 */
    const val PROJECT_LIST_LOAD_FAILED: String = "项目加载失败"

    /** 项目创建请求失败时的页面兜底文案，覆盖新增项目 mutation 流程。 */
    const val PROJECT_CREATE_FAILED: String = "项目创建失败"

    /** 项目重命名请求失败时的页面兜底文案，覆盖已有项目名称变更流程。 */
    const val PROJECT_RENAME_FAILED: String = "项目重命名失败"

    /** 项目删除请求失败时的页面兜底文案，覆盖普通删除和当前选中项目删除流程。 */
    const val PROJECT_DELETE_FAILED: String = "项目删除失败"

    /** 项目置顶状态切换失败时的页面兜底文案，覆盖置顶和取消置顶两个方向。 */
    const val PROJECT_PIN_FAILED: String = "项目置顶失败"

    /** 项目详情请求失败时的页面兜底文案，覆盖详情弹窗打开后的远端加载流程。 */
    const val PROJECT_DETAIL_LOAD_FAILED: String = "项目详情加载失败"
}

/**
 * 将稳定错误码映射为 QuickCreate 页面文案。
 *
 * 本函数只处理客户端定义的错误码；未识别字符串统一交给调用方兜底，
 * 避免服务端原始摘要、异常消息或调试信息越过 Presentation 的文案边界。
 */
private fun String.toQuickCreateIssueMessageOrNull(): String? =
    when (this) {
        QuickCreateTaskIssueCode.TASK_FAILED -> "任务失败"
        QuickCreateTaskIssueCode.TASK_TIMEOUT -> "任务超时"
        QuickCreateTaskIssueCode.TASK_QUERY_FAILED -> "任务查询失败"
        QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED -> QuickCreateErrorFallbackText.FEE_PREVIEW_FAILED
        QuickCreateTaskIssueCode.FEE_PREVIEW_BLOCKED -> QuickCreateErrorFallbackText.FEE_PREVIEW_NOT_PASSED
        QuickCreateTaskIssueCode.PREPARE_FAILED -> "任务预提交失败"
        QuickCreateTaskIssueCode.COMMIT_FAILED -> "任务提交失败"
        QuickCreateTaskIssueCode.UNKNOWN_ERROR -> "生成失败，请稍后重试"
        QuickCreateRepositoryIssueCode.API_KEY_MISSING -> "请先登录后再上传素材"
        QuickCreateRepositoryIssueCode.MEDIA_UPLOAD_FAILED -> "素材上传失败"
        QuickCreateRepositoryIssueCode.MEDIA_UPLOAD_EMPTY_URL -> "素材上传成功但缺少远端地址"
        QuickCreateRepositoryIssueCode.INSPIRATION_TAGS_LOAD_FAILED ->
            QuickCreateErrorFallbackText.INSPIRATION_TAGS_LOAD_FAILED
        QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATES_LOAD_FAILED ->
            QuickCreateErrorFallbackText.INSPIRATION_TEMPLATES_LOAD_FAILED
        QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATE_DETAIL_LOAD_FAILED ->
            QuickCreateErrorFallbackText.INSPIRATION_TEMPLATE_DETAIL_LOAD_FAILED
        QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATE_DETAIL_EMPTY -> "模板详情为空"
        QuickCreateRepositoryIssueCode.MODEL_LIST_LOAD_FAILED -> "模型列表加载失败"
        QuickCreateRepositoryIssueCode.HISTORY_LOAD_FAILED -> "历史加载失败"
        QuickCreateRepositoryIssueCode.HISTORY_DETAIL_LOAD_FAILED -> "历史详情加载失败"
        QuickCreateRepositoryIssueCode.TASK_CANCEL_FAILED -> "取消任务失败"
        QuickCreateRepositoryIssueCode.PROJECT_LIST_LOAD_FAILED ->
            QuickCreateErrorFallbackText.PROJECT_LIST_LOAD_FAILED
        QuickCreateRepositoryIssueCode.PROJECT_TASK_LIST_LOAD_FAILED -> "项目任务加载失败"
        QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED ->
            QuickCreateErrorFallbackText.PROJECT_CREATE_FAILED
        QuickCreateRepositoryIssueCode.PROJECT_RENAME_FAILED ->
            QuickCreateErrorFallbackText.PROJECT_RENAME_FAILED
        QuickCreateRepositoryIssueCode.PROJECT_DELETE_FAILED ->
            QuickCreateErrorFallbackText.PROJECT_DELETE_FAILED
        QuickCreateRepositoryIssueCode.PROJECT_PIN_FAILED ->
            QuickCreateErrorFallbackText.PROJECT_PIN_FAILED
        QuickCreateRepositoryIssueCode.PROJECT_DETAIL_LOAD_FAILED ->
            QuickCreateErrorFallbackText.PROJECT_DETAIL_LOAD_FAILED
        QuickCreateRepositoryIssueCode.PROJECT_ID_MISSING -> "项目数据缺少必要标识"
        else -> null
    }

private fun String.toQuickCreateIssueMessage(fallbackMessage: String): String =
    toQuickCreateIssueMessageOrNull() ?: fallbackMessage
