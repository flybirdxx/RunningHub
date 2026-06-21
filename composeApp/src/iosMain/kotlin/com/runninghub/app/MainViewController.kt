package com.runninghub.app

import androidx.compose.ui.window.ComposeUIViewController

/**
 * 创建 iOS 包装应用使用的 Compose 根控制器。
 *
 * iOS 的 SwiftUI 壳只负责承载该 UIViewController，真正的主题、根导航和会话恢复仍由
 * commonMain 的 [App] 管理。调用方必须先执行 [startRunningHubKoin]，否则根组合函数中的
 * Koin 注入无法解析运行期 Repository 和 SessionManager。
 *
 * @return 承载 RunningHub Compose Multiplatform UI 的 UIKit 控制器。
 */
fun MainViewController() = ComposeUIViewController {
    App()
}
