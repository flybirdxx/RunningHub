package com.runninghub.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * RunningHub 主业务网络请求的聚合活动快照。
 *
 * 本模型只记录请求数量，不保存 URL、Header、Body、错误消息或任何认证凭据。
 * 它服务于 AC-11 的运行期验收：登录态下切换 Tab 后，可以观察后台请求是否仍持续增长。
 *
 * @property startedCount 当前进程内已进入主业务 HttpClient 发送链路的请求总数。
 * 该值从 `0` 开始单调递增，包含普通请求以及客户端内部明确发起的重试请求。
 *
 * @property completedCount 当前进程内已经离开发送链路的请求总数。
 * 成功、失败和取消都会计入完成；`0` 表示尚无请求结束。
 *
 * @property inFlightCount 当前仍在发送链路中的请求数量。
 * 单位为个，最小值为 `0`；如果 Tab 不可见后该值长期大于 `0` 或 [startedCount]
 * 持续增长，说明仍有页面任务没有随生命周期停止。
 */
data class NetworkActivitySnapshot(
    val startedCount: Long = 0L,
    val completedCount: Long = 0L,
    val inFlightCount: Int = 0,
)

/**
 * 记录主业务 HttpClient 的请求活动。
 *
 * 该接口位于 core/network，只表达跨平台、无敏感内容的计数语义。Android 运行图可以把
 * [snapshots] 接到 debug logcat，测试也可以直接断言计数变化；生产业务代码不应依赖它做流程控制。
 */
interface NetworkActivityTracker {
    /**
     * 请求计数快照流。
     *
     * 每次请求进入或离开发送链路时都会发出新快照。调用方只能观察该流，不得通过它反向修改网络状态。
     */
    val snapshots: StateFlow<NetworkActivitySnapshot>

    /**
     * 记录一个请求进入发送链路。
     *
     * 该函数不接收请求对象，避免 URL、Header、Cookie 或 Token 被带入观测层。
     */
    fun onRequestStarted()

    /**
     * 记录一个请求离开发送链路。
     *
     * 成功、异常和协程取消都应调用本函数，使 [NetworkActivitySnapshot.inFlightCount]
     * 不会因为失败请求而永久偏大。
     */
    fun onRequestCompleted()
}

/**
 * 默认的内存请求活动计数器。
 *
 * 计数器使用 [MutableStateFlow.update] 更新快照，适合 KMP 端在多个协程请求同时完成时保持
 * 计数递增的一致性。它不持久化数据，应用进程重启后计数从零开始。
 */
class CountingNetworkActivityTracker : NetworkActivityTracker {
    private val _snapshots = MutableStateFlow(NetworkActivitySnapshot())

    override val snapshots: StateFlow<NetworkActivitySnapshot> = _snapshots.asStateFlow()

    override fun onRequestStarted() {
        _snapshots.update { current ->
            current.copy(
                startedCount = current.startedCount + 1,
                inFlightCount = current.inFlightCount + 1,
            )
        }
    }

    override fun onRequestCompleted() {
        _snapshots.update { current ->
            current.copy(
                completedCount = current.completedCount + 1,
                inFlightCount = (current.inFlightCount - 1).coerceAtLeast(0),
            )
        }
    }
}

/**
 * 为 HttpClient 安装请求活动计数拦截器。
 *
 * 本函数只包裹发送链路并更新 [NetworkActivityTracker]，不读取请求内容，也不改变响应、
 * 认证刷新或错误映射行为。调用方应只在主业务客户端安装它，refresh 专用客户端不安装，
 * 以免把 token 刷新流量混入页面生命周期验收数据。
 *
 * @param tracker 请求活动计数器；通常由应用组合根注册为单例。
 */
fun HttpClient.installRunningHubNetworkActivityTracking(tracker: NetworkActivityTracker) {
    plugin(HttpSend).intercept { request ->
        tracker.onRequestStarted()
        try {
            execute(request)
        } finally {
            tracker.onRequestCompleted()
        }
    }
}
