package com.runninghub.feature.discovery.presentation

import com.runninghub.feature.discovery.domain.CatalogError

/**
 * 目录与搜索页面可展示的稳定错误语义。
 *
 * 该类型属于 Discovery Presentation 层，只描述 UI 可以理解的错误类别，不携带服务端 `msg`、
 * [Throwable.message] 或本地异常诊断信息。最终中文文案由应用壳通过 Compose Resources 映射。
 */
enum class CatalogPresentationError {
    /**
     * 发现页主目录列表加载失败。
     *
     * 页面可以展示整页错误并允许用户刷新重试；该错误通常来自未归类异常或本地兜底路径。
     */
    LoadFailed,

    /**
     * 搜索页或发现页内联搜索加载失败。
     *
     * 页面可以保留当前关键词并允许用户重新搜索；该错误不代表主目录列表失败。
     */
    SearchFailed,

    /**
     * 服务端返回非成功业务码。
     *
     * 远端原始消息只用于 Data/Domain 侧诊断，Presentation 和 UI 不得直接展示。
     */
    ServiceUnavailable,

    /**
     * 接口成功但缺少业务数据。
     *
     * 该错误用于区分“真的没有结果”和“响应结构异常导致无法读取结果”的场景。
     */
    EmptyResponse,
}

/**
 * 将目录领域错误映射为 Presentation 稳定错误语义。
 *
 * 目录仓库返回的 [CatalogError] 可能包含服务端原始 msg 或底层异常，Presentation 不能直接展示
 * [Throwable.message]。该函数只按稳定错误类型返回枚举，最终文案由应用壳资源映射。
 *
 * @param defaultError 调用场景的默认兜底错误，例如列表加载失败或搜索失败。
 * @return 可交给 UI 层映射文案的稳定错误语义。
 */
fun Throwable.toCatalogPresentationError(
    defaultError: CatalogPresentationError,
): CatalogPresentationError = when (this) {
    is CatalogError.Remote -> CatalogPresentationError.ServiceUnavailable
    is CatalogError.EmptyResponse -> CatalogPresentationError.EmptyResponse
    is CatalogError.Unexpected -> defaultError
    else -> defaultError
}
