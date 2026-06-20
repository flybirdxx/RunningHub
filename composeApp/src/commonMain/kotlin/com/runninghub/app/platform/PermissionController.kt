package com.runninghub.app.platform

import androidx.compose.runtime.Composable
import com.runninghub.app.ui.component.MediaType
import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.permission.PermissionStateStore

/**
 * 平台权限与媒体选择能力的 Presentation 边界。
 *
 * commonMain 页面通过该接口发起权限检查和媒体选择，具体 Android/iOS 行为由
 * actual 实现处理，避免平台 SDK 类型进入通用 UI 状态。
 */
interface PermissionController {
    /**
     * 请求选择指定类型的媒体文件。
     *
     * @param mediaPermission 选择媒体前需要确认的领域权限。
     * @param mediaType 需要选择的媒体类型。
     * @param onSuccess 成功选择后返回平台无关 URI 字符串。
     * @param onPermissionDenied 权限被拒绝或无法继续选择时调用。
     */
    fun pickMedia(
        mediaPermission: Permission,
        mediaType: MediaType,
        onSuccess: (String) -> Unit,
        onPermissionDenied: () -> Unit,
    )

    /**
     * 检查并按需申请权限。
     *
     * 回调只表达用户可见的权限结果，平台实现负责更新权限持久状态。
     */
    fun checkAndRequest(
        permission: Permission,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit,
    )

    /**
     * 打开当前应用的系统设置页。
     *
     * Android 与 iOS 的跳转方式不同，因此具体 URL/Intent 细节保留在 actual 实现中。
     */
    fun openAppSettings()
}

/**
 * 创建平台权限控制器。
 *
 * @param permissionStateStore 权限状态领域边界，actual 实现通过它记录授权、拒绝和永久拒绝状态。
 * @return 当前平台可用的权限控制器实例。
 */
@Composable
expect fun rememberPermissionController(
    permissionStateStore: PermissionStateStore,
): PermissionController
