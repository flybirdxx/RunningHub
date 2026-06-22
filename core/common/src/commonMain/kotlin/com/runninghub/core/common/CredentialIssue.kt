package com.runninghub.core.common

/**
 * 本地凭据前置条件失败的稳定语义。
 *
 * 该类型用于 Data 层在发起网络请求前表达“缺少某类本地凭据”的业务前置条件失败，
 * 不携带最终用户可见文案。Presentation 或应用壳应根据 [credential] 映射资源化提示。
 *
 * @property credential 缺失的本地凭据类型。
 */
class MissingCredentialException(
    val credential: MissingCredential,
) : IllegalStateException(credential.code)

/**
 * Data 层可识别的本地凭据类型。
 *
 * @property code 稳定诊断码，可用于测试、日志分类和上层错误映射；不得作为最终 UI 文案。
 */
enum class MissingCredential(val code: String) {
    /**
     * 用户尚未绑定 API Key，不能调用依赖 OpenAPI 凭据的接口。
     */
    ApiKey("MISSING_API_KEY"),
}
