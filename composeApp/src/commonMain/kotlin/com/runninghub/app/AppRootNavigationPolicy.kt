package com.runninghub.app

import com.runninghub.feature.auth.domain.SessionState

/**
 * 根导航可展示的顶层页面目标。
 *
 * 该枚举只描述应用壳层的页面归属，不引用 Voyager、Compose 或具体 Screen 类型，
 * 便于在 commonTest 中验证会话状态到根页面的映射策略。
 */
internal enum class RootScreenTarget {
    /** 已认证会话进入业务主页面栈。 */
    Main,

    /** 未认证或失效会话进入登录页，并且不得保留旧业务页面栈。 */
    Login,
}

/**
 * 根据当前会话状态生成根导航动作。
 *
 * @property target 需要替换到根栈的页面；`null` 表示仍在恢复会话，暂不创建业务或登录页面。
 * @property resetExpiredSession 是否需要在完成导航后清理一次性失效状态，避免旧 401 事件影响下一次会话。
 */
internal data class RootNavigationDecision(
    val target: RootScreenTarget?,
    val resetExpiredSession: Boolean,
)

/**
 * 将 Auth Domain 的会话状态映射为应用壳层根导航策略。
 *
 * 这段逻辑是 Gate G 的关键边界：会话失效时必须清空业务页面栈并回到登录页，
 * 主动注销后的未认证状态也必须回到登录页，但不需要再次消费失效事件。
 * 将映射提取为纯函数，可以用单元测试证明根导航不会保留已失效的业务页面。
 */
internal fun rootNavigationDecisionFor(
    sessionState: SessionState,
): RootNavigationDecision =
    when (sessionState) {
        SessionState.Restoring -> RootNavigationDecision(
            target = null,
            resetExpiredSession = false,
        )
        SessionState.Authenticated -> RootNavigationDecision(
            target = RootScreenTarget.Main,
            resetExpiredSession = false,
        )
        SessionState.Unauthenticated -> RootNavigationDecision(
            target = RootScreenTarget.Login,
            resetExpiredSession = false,
        )
        SessionState.Expired -> RootNavigationDecision(
            target = RootScreenTarget.Login,
            resetExpiredSession = true,
        )
    }
