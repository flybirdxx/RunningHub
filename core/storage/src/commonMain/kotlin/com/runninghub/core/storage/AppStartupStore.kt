package com.runninghub.core.storage

/**
 * 应用启动期一次性任务的非敏感状态存储。
 *
 * 本端口只保存是否已经发起过某类启动任务，不保存用户身份、Token、Cookie、API Key 或远端响应。
 * 平台启动层可用它控制只应在首次安装后运行一次的后台预热，避免把启动策略散落在 UI 或 Data 层。
 */
interface AppStartupStore {
    /**
     * 判断首次安装后的模型目录预热是否已经发起过。
     *
     * @return `true` 表示当前安装实例已经发起过模型目录预热；`false` 表示可以在本次启动中发起一次。
     */
    suspend fun hasStartedInitialModelCatalogPreload(): Boolean

    /**
     * 标记首次模型目录预热已经发起。
     *
     * 调用方应在真正访问网络前写入该标记，确保网络失败、进程被杀或用户退出后不会在每次启动重复预热。
     */
    suspend fun markInitialModelCatalogPreloadStarted()
}
