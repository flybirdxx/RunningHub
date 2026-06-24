package com.runninghub.app.platform

import androidx.compose.runtime.Composable

/**
 * 跨平台系统返回事件拦截入口。
 *
 * commonMain 只声明页面需要消费返回事件的语义；Android 侧接入系统返回键和返回手势，
 * iOS 侧当前没有等价的系统返回分发，因此保持空实现。
 *
 * @param enabled `true` 表示当前页面有优先于导航栈的临时层级需要消费返回事件；
 * `false` 表示返回事件继续交给平台或导航框架处理。
 * @param onBack 系统返回触发时执行的页面内处理逻辑，通常用于关闭弹层或清理临时状态。
 */
@Composable
expect fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
