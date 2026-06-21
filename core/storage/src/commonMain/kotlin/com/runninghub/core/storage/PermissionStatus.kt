package com.runninghub.core.storage

/**
 * 跨平台权限状态。
 *
 * 状态只表达应用业务层需要的授权结果，不携带 Android/iOS 的平台错误码。平台实现需要把系统回调
 * 映射为这些稳定状态，再由 Presentation 决定提示用户继续申请或跳转系统设置。
 */
enum class PermissionStatus {
    /** 已授权，相关媒体选择或系统能力可以继续执行。 */
    GRANTED,

    /** 已拒绝，但平台仍允许再次弹出权限申请。 */
    DENIED,

    /** 已永久拒绝，需要引导用户到系统设置页修改。 */
    PERMANENTLY_DENIED,

    /** 本地没有记录，或平台暂时无法判断。 */
    UNKNOWN,
}
