package com.runninghub.app.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.runninghub.core.network.NetworkActivityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val NETWORK_ACTIVITY_LOG_TAG = "RunningHubNetwork"
private const val NETWORK_ACTIVITY_HEARTBEAT_MILLIS = 1_000L

/**
 * Android debug 运行期的网络活动观察器。
 *
 * 该类只在可调试应用进程中把 [NetworkActivityTracker] 的聚合计数写入 logcat，
 * 用于 AC-11 人工验收 Tab 切换后的后台请求是否持续增长。日志内容只包含请求数量，
 * 不包含 URL、Header、Cookie、Token、Body 或错误消息，因此不会扩大敏感信息暴露面。
 *
 * @param context Android 应用上下文，用于判断当前安装包是否为 debuggable。
 * @param tracker 主业务 HttpClient 的请求活动计数器。
 */
internal class AndroidNetworkActivityLogObserver(
    context: Context,
    tracker: NetworkActivityTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) {
            scope.launch {
                while (isActive) {
                    val snapshot = tracker.snapshots.value
                    // 稳定窗口必须有连续采样点。即使没有新请求，也定期输出当前聚合计数，
                    // 避免单个冷启动日志被误当成登录态 Tab 后台请求停止的完整证据。
                    Log.d(
                        NETWORK_ACTIVITY_LOG_TAG,
                        "started=${snapshot.startedCount} completed=${snapshot.completedCount} inFlight=${snapshot.inFlightCount}",
                    )
                    delay(NETWORK_ACTIVITY_HEARTBEAT_MILLIS)
                }
            }
        }
    }
}
