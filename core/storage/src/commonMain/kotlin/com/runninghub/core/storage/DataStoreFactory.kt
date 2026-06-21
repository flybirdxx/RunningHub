package com.runninghub.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

internal const val DATASTORE_FILE_NAME = "runninghub_settings.preferences_pb"

/**
 * 创建应用级 Preferences DataStore。
 *
 * 这是非敏感偏好、草稿和权限状态在迁移期共享的底层存储入口。它位于 core:storage，
 * 使 shared 只能作为消费者复用存储能力，而不再拥有平台文件路径和 Android Context 初始化逻辑。
 *
 * 平台差异：
 * - Android 需要先通过平台入口调用 `initDataStore(context)` 注入 Application Context。
 * - iOS 直接使用应用 Documents 目录创建同名偏好文件。
 *
 * @return 指向 `runninghub_settings.preferences_pb` 的 Preferences DataStore。
 */
expect fun createDataStore(): DataStore<Preferences>
