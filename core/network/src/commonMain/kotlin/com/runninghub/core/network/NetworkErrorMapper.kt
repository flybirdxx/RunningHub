package com.runninghub.core.network

/**
 * 网络异常的稳定分类。
 *
 * 该枚举属于 core/network 边界，用来把 Ktor engine、平台网络栈或 MockEngine 抛出的异常
 * 归一为跨平台可判断的错误类型。它不包含最终用户可见文案，Presentation 或调用方应根据自身场景映射提示。
 */
enum class NetworkErrorCategory {
    /** DNS、无网络、连接被拒绝或连接中断等网络连通性问题。 */
    CONNECTIVITY,

    /** 请求、连接或 socket 超时。 */
    TIMEOUT,

    /** 当前无法稳定识别为网络问题的异常。 */
    UNKNOWN,
}

/**
 * 将底层异常映射为稳定网络错误分类。
 *
 * commonMain 无法依赖 JVM 的 `IOException` 或 Android 网络异常类型，因此当前实现按异常消息和
 * cause 链路做保守识别。该逻辑集中在 core/network，避免各 Repository 复制脆弱的字符串判断。
 */
object NetworkErrorMapper {

    /**
     * 判断异常是否属于可归因于网络层的问题。
     *
     * @param throwable 远程调用过程中捕获的异常。
     * @return true 表示调用方可以按网络失败处理；false 表示应继续按业务或未知异常处理。
     */
    fun isNetworkError(throwable: Throwable): Boolean =
        map(throwable) != NetworkErrorCategory.UNKNOWN

    /**
     * 把异常映射成稳定分类。
     *
     * @param throwable 远程调用过程中捕获的异常。
     * @return 匹配到 timeout 相关信息时返回 [NetworkErrorCategory.TIMEOUT]；
     * 匹配到 DNS、连接失败或网络断开时返回 [NetworkErrorCategory.CONNECTIVITY]；
     * 无法识别时返回 [NetworkErrorCategory.UNKNOWN]。
     */
    fun map(throwable: Throwable): NetworkErrorCategory {
        val messages = generateSequence(throwable) { it.cause }
            .mapNotNull { it.message?.lowercase() }
            .toList()

        return when {
            messages.any { message -> TIMEOUT_MARKERS.any(message::contains) } -> NetworkErrorCategory.TIMEOUT
            messages.any { message -> CONNECTIVITY_MARKERS.any(message::contains) } -> NetworkErrorCategory.CONNECTIVITY
            else -> NetworkErrorCategory.UNKNOWN
        }
    }

    private val CONNECTIVITY_MARKERS = listOf(
        "unable to resolve host",
        "unknownhost",
        "network",
        "connect",
        "connection",
        "refused",
        "unreachable",
    )
    private val TIMEOUT_MARKERS = listOf(
        "timeout",
        "timed out",
        "socket timeout",
    )
}
