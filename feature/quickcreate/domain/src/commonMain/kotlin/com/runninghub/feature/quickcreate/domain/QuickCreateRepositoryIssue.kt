package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作非任务状态 Repository 失败时使用的稳定错误码。
 *
 * 这些常量属于 Domain 层错误语义，不携带最终 UI 文案。Data 层在缺少服务端错误摘要、
 * 响应结构异常或本地前置条件不满足时返回这些错误码；Presentation 层再按具体页面场景映射为
 * 本地化展示文案。这样可以避免 Repository 直接生成中文提示，也能让测试断言稳定错误语义。
 */
object QuickCreateRepositoryIssueCode {
    /** 上传素材前缺少可用 API Key 或登录凭据。 */
    const val API_KEY_MISSING = "API_KEY_MISSING"

    /** 媒体上传接口返回失败或远端拒绝当前文件。 */
    const val MEDIA_UPLOAD_FAILED = "MEDIA_UPLOAD_FAILED"

    /** 媒体上传接口返回成功但缺少可提交的远端 URL。 */
    const val MEDIA_UPLOAD_EMPTY_URL = "MEDIA_UPLOAD_EMPTY_URL"

    /** 灵感标签列表加载失败。 */
    const val INSPIRATION_TAGS_LOAD_FAILED = "INSPIRATION_TAGS_LOAD_FAILED"

    /** 灵感模板分页加载失败。 */
    const val INSPIRATION_TEMPLATES_LOAD_FAILED = "INSPIRATION_TEMPLATES_LOAD_FAILED"

    /** 灵感模板详情加载失败。 */
    const val INSPIRATION_TEMPLATE_DETAIL_LOAD_FAILED = "INSPIRATION_TEMPLATE_DETAIL_LOAD_FAILED"

    /** 灵感模板详情响应成功但缺少详情数据。 */
    const val INSPIRATION_TEMPLATE_DETAIL_EMPTY = "INSPIRATION_TEMPLATE_DETAIL_EMPTY"

    /** 快捷创作服务模型目录加载失败。 */
    const val MODEL_LIST_LOAD_FAILED = "MODEL_LIST_LOAD_FAILED"

    /** 最近历史列表加载失败。 */
    const val HISTORY_LOAD_FAILED = "HISTORY_LOAD_FAILED"

    /** 历史输出详情加载失败。 */
    const val HISTORY_DETAIL_LOAD_FAILED = "HISTORY_DETAIL_LOAD_FAILED"

    /** 取消历史任务失败。 */
    const val TASK_CANCEL_FAILED = "TASK_CANCEL_FAILED"

    /** 项目列表加载失败。 */
    const val PROJECT_LIST_LOAD_FAILED = "PROJECT_LIST_LOAD_FAILED"

    /** 项目下任务列表加载失败。 */
    const val PROJECT_TASK_LIST_LOAD_FAILED = "PROJECT_TASK_LIST_LOAD_FAILED"

    /** 创建项目失败。 */
    const val PROJECT_CREATE_FAILED = "PROJECT_CREATE_FAILED"

    /** 重命名项目失败。 */
    const val PROJECT_RENAME_FAILED = "PROJECT_RENAME_FAILED"

    /** 删除项目失败。 */
    const val PROJECT_DELETE_FAILED = "PROJECT_DELETE_FAILED"

    /** 切换项目置顶状态失败。 */
    const val PROJECT_PIN_FAILED = "PROJECT_PIN_FAILED"

    /** 项目详情加载失败。 */
    const val PROJECT_DETAIL_LOAD_FAILED = "PROJECT_DETAIL_LOAD_FAILED"

    /** 服务端项目数据缺少稳定项目 ID，Data 层无法构造 Domain 模型。 */
    const val PROJECT_ID_MISSING = "PROJECT_ID_MISSING"
}

/**
 * 快捷创作 Repository 边界使用的结构化异常。
 *
 * @property issueCode [QuickCreateRepositoryIssueCode] 或任务错误码中的稳定错误语义。
 * @property remoteMessage 服务端返回的业务错误摘要；`null` 表示服务端没有提供可展示摘要，
 * 调用方应按 [issueCode] 映射本地文案。
 * @property remoteStatusCode 服务端业务响应码；仅用于诊断和测试，不应直接展示给用户。
 */
class QuickCreateRepositoryException(
    val issueCode: String,
    val remoteMessage: String? = null,
    val remoteStatusCode: Int? = null,
) : IllegalStateException(remoteMessage?.takeIf { it.isNotBlank() } ?: issueCode)
