package com.runninghub.app.ui.feature.discovery

import com.runninghub.feature.discovery.domain.CatalogError

/**
 * 将目录领域错误映射为用户可见文案。
 *
 * 目录仓库返回的 [CatalogError] 可能包含服务端原始 msg 或底层异常，Presentation 不能直接展示
 * [Throwable.message]。该函数只按稳定错误类型返回本地文案，避免远端协议、内部异常或诊断信息泄露到 UI。
 *
 * @param defaultMessage 调用场景的默认兜底文案，例如列表加载失败或搜索失败。
 * @return 可安全展示给用户的简体中文错误提示。
 */
internal fun Throwable.toCatalogErrorMessage(defaultMessage: String): String = when (this) {
    is CatalogError.Remote -> "目录服务暂时不可用，请稍后重试"
    is CatalogError.EmptyResponse -> "目录数据暂时为空，请稍后重试"
    is CatalogError.Unexpected -> defaultMessage
    else -> defaultMessage
}
