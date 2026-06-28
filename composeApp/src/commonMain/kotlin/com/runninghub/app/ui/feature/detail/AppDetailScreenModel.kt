package com.runninghub.app.ui.feature.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.app.platform.MediaResolver
import com.runninghub.app.ui.feature.history.WebAppTaskHistoryOverlayStore
import com.runninghub.core.model.InputNode
import com.runninghub.feature.detail.presentation.AppDetailMediaReader
import com.runninghub.feature.detail.presentation.AppDetailMediaType
import com.runninghub.feature.detail.presentation.AppDetailStateHolder
import com.runninghub.feature.detail.presentation.AppDetailUiState
import com.runninghub.feature.detail.presentation.appDetailInputKey
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import com.runninghub.feature.task.domain.WebAppTaskRepository
import com.runninghub.feature.task.presentation.TaskHistoryInvalidationNotifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * AppDetail Voyager 页面对 Detail Presentation 状态持有器的生命周期适配层。
 *
 * 详情加载、输入编辑、媒体上传状态、任务提交和输出轮询由 [AppDetailStateHolder] 承担；
 * 本类只负责把 Voyager 的 [screenModelScope]、平台 [MediaResolver] 和 Koin 注入的领域仓库传入，
 * 并保留原有 ScreenModel 方法签名供 Compose 页面调用。
 *
 * @param webAppCatalogRepository WebApp 目录仓库，用于加载公开详情。
 * @param webAppTaskRepository WebApp 任务仓库，用于 API demo fallback、上传、提交和输出轮询。
 * @param mediaResolver composeApp 平台媒体读取能力，负责把本地 URI 转换为上传文件。
 * @param taskHistoryInvalidationNotifier History 列表刷新通知端口。
 * @param webAppTaskHistoryOverlayStore 本机已提交 WebApp 任务兜底缓存，用于服务端宽表同步前展示运行中任务。
 * @param ioDispatcher 媒体字节读取使用的调度器；默认使用跨平台可用的 [Dispatchers.Default]，
 * 测试可注入可控调度器。
 */
class AppDetailScreenModel(
    webAppCatalogRepository: WebAppCatalogRepository,
    webAppTaskRepository: WebAppTaskRepository,
    mediaResolver: MediaResolver,
    taskHistoryInvalidationNotifier: TaskHistoryInvalidationNotifier = TaskHistoryInvalidationNotifier {},
    webAppTaskHistoryOverlayStore: WebAppTaskHistoryOverlayStore = WebAppTaskHistoryOverlayStore(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ScreenModel {
    private val stateHolder = AppDetailStateHolder(
        webAppCatalogRepository = webAppCatalogRepository,
        webAppTaskRepository = webAppTaskRepository,
        mediaReader = MediaResolverAppDetailMediaReader(mediaResolver),
        coroutineScope = screenModelScope,
        ioDispatcher = ioDispatcher,
        onTaskHistoryInvalidated = taskHistoryInvalidationNotifier::notifyTaskHistoryInvalidated,
        onTaskHistoryTaskChanged = webAppTaskHistoryOverlayStore::trackSubmittedTask,
    )

    /**
     * AppDetail 只读 UI 状态。
     *
     * UI 只能收集该状态并通过本类公开动作发送事件，避免 composeApp 重新实现 Detail Presentation 状态规则。
     */
    val uiState: StateFlow<AppDetailUiState> = stateHolder.uiState

    /**
     * 加载指定 WebApp 详情。
     *
     * @param appId WebApp ID，来源于详情页导航参数。
     */
    fun loadDetail(appId: String) {
        stateHolder.loadDetail(appId)
    }

    /**
     * 更新单个任务输入字段。
     *
     * @param nodeId 输入节点 ID。
     * @param fieldName 输入字段名。
     * @param value 用户当前输入值。
     */
    fun updateInputValue(nodeId: String, fieldName: String, value: String) {
        stateHolder.updateInputValue(nodeId, fieldName, value)
    }

    /**
     * 记录等待平台媒体选择器处理的输入节点。
     *
     * @param nodeId 输入节点 ID。
     * @param fieldName 输入字段名。
     * @param mediaType 期望选择的媒体类型。
     */
    fun setPendingMediaPick(nodeId: String, fieldName: String, mediaType: AppDetailMediaType) {
        stateHolder.setPendingMediaPick(nodeId, fieldName, mediaType)
    }

    /**
     * 清理挂起的媒体选择请求。
     */
    fun clearPendingMediaPick() {
        stateHolder.clearPendingMediaPick()
    }

    /**
     * 处理平台媒体选择器返回的本地 URI。
     *
     * @param uri 平台媒体选择器返回的本地 URI 字符串。
     */
    fun onMediaUriReceived(uri: String) {
        stateHolder.onMediaUriReceived(uri)
    }

    /**
     * 直接上传本地媒体文件。
     *
     * @param nodeId 输入节点 ID。
     * @param fieldName 输入字段名。
     * @param localUri 本地媒体 URI 字符串。
     * @param mediaType 文件媒体类型。
     */
    fun uploadFile(
        nodeId: String,
        fieldName: String,
        localUri: String,
        mediaType: AppDetailMediaType = AppDetailMediaType.IMAGE,
    ) {
        stateHolder.uploadFile(nodeId, fieldName, localUri, mediaType)
    }

    /**
     * 移除输入节点关联的本地文件和远端文件名。
     *
     * @param nodeId 输入节点 ID。
     * @param fieldName 输入字段名。
     */
    fun removeLocalFile(nodeId: String, fieldName: String) {
        stateHolder.removeLocalFile(nodeId, fieldName)
    }

    /**
     * 提交当前详情页任务。
     */
    fun runTask() {
        stateHolder.runTask()
    }

    /**
     * 重置任务结果展示状态。
     */
    fun resetTask() {
        stateHolder.resetTask()
    }

    override fun onDispose() {
        stateHolder.dispose()
        super.onDispose()
    }

    private class MediaResolverAppDetailMediaReader(
        private val mediaResolver: MediaResolver,
    ) : AppDetailMediaReader {
        override fun readBytes(uri: String): ByteArray = mediaResolver.readBytes(uri)

        override fun getDisplayName(uri: String): String? = mediaResolver.getDisplayName(uri)
    }

    companion object {
        fun inputKey(node: InputNode): String = appDetailInputKey(node)
        fun inputKey(nodeId: String, fieldName: String): String = appDetailInputKey(nodeId, fieldName)
    }
}
