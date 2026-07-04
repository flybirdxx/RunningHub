package com.runninghub.feature.quickcreate.presentation.upload

import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.presentation.QuickCreateRuntimeUiText
import com.runninghub.feature.quickcreate.presentation.QuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationRequestFactory
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskStatusText
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

private const val UPLOAD_WAIT_MAX_TICKS = 120
private const val UPLOAD_WAIT_TICK_MILLIS = 500L

/**
 * 生成前等待媒体上传失败时使用的受控异常。
 *
 * 该异常只在 QuickCreate Presentation 内部传播，不依赖 [Throwable.message] 承载 UI 文案；
 * 调用方应读取 [uiMessage] 中的稳定语义并映射为页面错误提示。
 *
 * @property uiMessage 等待上传失败后需要展示给用户的稳定页面消息。
 */
class QuickCreateMediaUploadException(
    val uiMessage: QuickCreateUiMessage,
) : RuntimeException()

/**
 * 快捷创作上传协调器使用的平台媒体读取端口。
 *
 * 该接口位于 feature presentation 模块，避免上传状态机直接依赖 composeApp 的 Android/iOS
 * `MediaResolver` 实现。应用壳负责把平台选择器返回的 URI 适配到该端口，Coordinator 只处理
 * 页面状态、上传 Repository 和生成前等待规则。
 */
interface QuickCreateMediaResolver {
    /**
     * 读取本地媒体 URI 对应的文件内容。
     *
     * @param uri 平台媒体选择器返回的 URI 字符串；具体权限和安全作用域由应用壳负责。
     * @return 上传到远端仓库的原始字节；读取失败时允许抛出异常，由 Coordinator 映射为上传失败状态。
     */
    fun readBytes(uri: String): ByteArray

    /**
     * 读取用于页面展示的媒体名称。
     *
     * @param uri 平台媒体选择器返回的 URI 字符串。
     * @return 可展示文件名；`null` 表示平台无法解析名称，Coordinator 会使用类型和时间戳生成降级名称。
     */
    fun getDisplayName(uri: String): String?

    /**
     * 读取本地媒体大小。
     *
     * @param uri 平台媒体选择器返回的 URI 字符串。
     * @return 文件大小，单位为字节；无法解析时返回 `0`，表示仅隐藏大小展示而不阻塞上传。
     */
    fun getFileSizeBytes(uri: String): Long
}

/**
 * 协调快捷创作页面的媒体选择、上传和待上传素材等待流程。
 *
 * 该 Coordinator 属于 QuickCreate presentation 层，负责把用户选择的本地 URI 转换为页面中的
 * [MediaReference]，读取跨平台媒体字节并调用 Repository 上传，同时维护上传 Job 生命周期。
 * ScreenModel 只保留公开的页面事件入口，避免继续直接管理媒体读取、远程上传和上传状态回写。
 *
 * 并发与生命周期约束：
 * - 每个媒体引用只允许存在一个上传 Job；重复上传同一 id 时会取消旧 Job。
 * - 页面销毁时必须调用 [dispose]，否则本地文件读取或远程上传可能在页面失效后继续回写状态。
 * - 上传完成、失败或删除与当前计费请求相关的素材后，通过 [onFeePreviewRequired] 触发计费预览刷新。
 *
 * @param mediaUploadRepository 快捷创作媒体上传仓库，只提供远端媒体上传能力。
 * @param mediaResolver 平台无关媒体读取端口，负责读取 URI 的展示名、大小和字节内容。
 * @param generationRequestFactory 生成请求构建器，用于判断哪些媒体引用会影响当前生成和计费请求。
 * @param scope 页面生命周期作用域，上传 Job 与等待流程都挂在该作用域下。
 * @param uiState 页面状态流，Coordinator 只更新媒体引用、上传进度和提交阶段状态文案。
 * @param ioDispatcher 读取本地媒体字节使用的调度器，避免阻塞主线程；测试可替换为可控调度器。
 * @param onFeePreviewRequired 当前媒体变化影响计费请求时调用，用于重新安排计费预览。
 */
class QuickCreateMediaUploadCoordinator(
    private val mediaUploadRepository: QuickCreationMediaUploadRepository,
    private val mediaResolver: QuickCreateMediaResolver,
    private val generationRequestFactory: QuickCreateGenerationRequestFactory,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
    private val ioDispatcher: CoroutineDispatcher,
    private val onFeePreviewRequired: () -> Unit,
) {
    private val uploadJobs = mutableMapOf<String, Job>()

    /**
     * 新增全局参考素材并启动上传。
     *
     * 空 URI 表示平台选择器取消或返回无效结果，此时不创建媒体引用，避免空路径进入上传协程。
     *
     * @param uriString 用户选择的本地媒体 URI。
     * @param type 当前引用的媒体类型，决定上传 MIME 类型和默认文件扩展名。
     */
    fun addGlobalMediaReference(uriString: String, type: QuickCreateMediaType) {
        if (uriString.isBlank()) return
        addMediaReference(uriString = uriString, type = type, fieldParamKey = null)
    }

    /**
     * 新增动态服务字段素材并启动上传。
     *
     * 字段素材必须同时具备本地 URI 和服务字段参数名。参数名为空时无法映射到
     * quickCreationListParams，因此直接忽略，避免后续计费和提交链路收到无归属素材。
     *
     * @param uriString 用户选择的本地媒体 URI。
     * @param type 当前引用的媒体类型，决定上传 MIME 类型和默认文件扩展名。
     * @param fieldParamKey 服务动态上传字段的参数名，上传完成后用于构建字段级列表参数。
     */
    fun addFieldMediaReference(
        uriString: String,
        type: QuickCreateMediaType,
        fieldParamKey: String,
    ) {
        if (uriString.isBlank() || fieldParamKey.isBlank()) return
        addMediaReference(uriString = uriString, type = type, fieldParamKey = fieldParamKey)
    }

    /**
     * 直接挂载已在远端的素材引用，不经过本地读取和上传。
     *
     * 复用 Plaza「使用同款」的远端引用机制：引用以 [UploadStatus.DONE] 加 remoteUrl 的形式
     * 进入当前 Tab 的全局素材区，生成请求直接使用 remoteUrl，不触发 [QuickCreateMediaResolver]
     * 或上传 Repository。适用于结果图「复制到素材区」等已知远端地址的场景。
     *
     * @param url 远端可访问的媒体地址；空白时忽略，与本地选择器返回空 URI 的处理一致。
     * @param type 素材媒体类型，决定生成请求的参数映射。
     */
    fun attachRemoteMediaReference(url: String, type: QuickCreateMediaType) {
        if (url.isBlank()) return
        val now = Clock.System.now().toEpochMilliseconds()
        val targetTab = uiState.value.currentTab
        val id = "${targetTab.name}_${type.name}_remote_$now"
        val newRef = MediaReference(
            id = id,
            type = type,
            uri = url,
            displayName = url.substringAfterLast('/').substringBefore('?')
                .ifBlank { "${type.name.lowercase()}_$now" },
            fileSizeBytes = 0L,
            fieldParamKey = null,
            uploadStatus = UploadStatus.DONE,
            uploadProgress = 1f,
            remoteUrl = url,
        )
        uiState.update { state ->
            state.withMediaReference(targetTab) { mediaReferences ->
                mediaReferences + newRef
            }
        }
        scheduleFeePreviewForMediaReference(id)
    }

    private fun addMediaReference(
        uriString: String,
        type: QuickCreateMediaType,
        fieldParamKey: String?,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val targetTab = uiState.value.currentTab
        val id = "${targetTab.name}_${type.name}_$now"
        val fileName = mediaResolver.getDisplayName(uriString) ?: "${type.name.lowercase()}_$now"
        val fileSize = mediaResolver.getFileSizeBytes(uriString)

        val newRef = MediaReference(
            id = id,
            type = type,
            uri = uriString,
            displayName = fileName,
            fileSizeBytes = fileSize,
            fieldParamKey = fieldParamKey,
            uploadStatus = UploadStatus.UPLOADING,
            uploadProgress = 0f,
        )
        uiState.update { state ->
            state.withMediaReference(targetTab) { mediaReferences ->
                mediaReferences + newRef
            }
        }

        uploadReference(id, uriString, type, targetTab)
    }

    /**
     * 删除媒体引用并取消仍在进行的上传任务。
     *
     * 删除前先判断该引用是否属于当前生成请求，保证隐藏字段或未激活 child 字段不会无意义刷新计费预览。
     */
    fun removeMediaReference(id: String) {
        val shouldRefreshFeePreview = generationRequestFactory.currentRelevantMediaReferences(uiState.value)
            .any { it.id == id }
        uploadJobs[id]?.cancel()
        uploadJobs.remove(id)
        uiState.update { state ->
            state.copy(
                imageConfig = state.imageConfig.copy(
                    mediaReferences = state.imageConfig.mediaReferences.filter { it.id != id },
                ),
                videoConfig = state.videoConfig.copy(
                    mediaReferences = state.videoConfig.mediaReferences.filter { it.id != id },
                ),
            )
        }
        if (shouldRefreshFeePreview) {
            onFeePreviewRequired()
        }
    }

    /**
     * 等待指定生成快照所需素材完成上传。
     *
     * 生成提交前调用该方法。已失败的素材会立即中断生成；上传中的素材最多等待
     * [UPLOAD_WAIT_MAX_TICKS] 次，每次 [UPLOAD_WAIT_TICK_MILLIS] 毫秒。超时仍未完成时抛出业务错误，
     * 由 ScreenModel 映射为页面错误并恢复空闲状态。
     *
     * @param stateSnapshot 用户点击生成时的页面快照。等待上传期间页面仍可能继续编辑，
     * 因此本方法只跟踪快照中相关素材的上传结果，并把 remoteUrl 回填到同一份快照后返回。
     * @return 已合并上传终态的提交快照，可直接用于最终生成请求构建。
     */
    suspend fun awaitPendingUploads(
        stateSnapshot: QuickCreateUiState = uiState.value,
    ): QuickCreateUiState {
        val mediaRefs = generationRequestFactory.currentRelevantMediaReferences(stateSnapshot)
        val alreadyFailed = mediaRefs.filter { it.uploadStatus == UploadStatus.FAILED }
        if (alreadyFailed.isNotEmpty()) {
            throw QuickCreateMediaUploadException(MEDIA_UPLOAD_FAILED_MESSAGE)
        }
        val pending = mediaRefs.filter { it.isUploadPending() }
        if (pending.isEmpty()) return stateSnapshot

        uiState.update { it.copy(statusText = QuickCreateTaskStatusText.UploadingMedia(pending.size)) }

        val pendingIds = pending.map { it.id }.toSet()
        var waited = 0
        while (waited < UPLOAD_WAIT_MAX_TICKS) {
            delay(UPLOAD_WAIT_TICK_MILLIS)
            waited++
            val currentReferences = uiState.value.mediaReferencesById()
            val removed = pendingIds.filter { it !in currentReferences.keys }
            if (removed.isNotEmpty()) {
                throw QuickCreateMediaUploadException(MEDIA_UPLOAD_FAILED_MESSAGE)
            }
            val failed = currentReferences.values.filter { it.id in pendingIds && it.uploadStatus == UploadStatus.FAILED }
            if (failed.isNotEmpty()) {
                throw QuickCreateMediaUploadException(MEDIA_UPLOAD_FAILED_MESSAGE)
            }
            val stillPending = currentReferences.values
                .filter { it.id in pendingIds && it.isUploadPending() }
                .map { it.id }
                .toSet()
            if (stillPending.isEmpty()) {
                return stateSnapshot.withUploadedMediaFrom(uiState.value, pendingIds)
            }
        }

        val currentReferences = uiState.value.mediaReferencesById()
        val failed = currentReferences.values.filter { it.id in pendingIds && it.uploadStatus == UploadStatus.FAILED }
        if (failed.isNotEmpty()) {
            throw QuickCreateMediaUploadException(MEDIA_UPLOAD_FAILED_MESSAGE)
        }
        val timedOut = pending.filter { reference ->
            currentReferences[reference.id]?.isUploadPending() != false
        }
        if (timedOut.isNotEmpty()) {
            throw QuickCreateMediaUploadException(MEDIA_UPLOAD_TIMEOUT_MESSAGE)
        }
        return stateSnapshot.withUploadedMediaFrom(uiState.value, pendingIds)
    }

    /**
     * 取消所有上传任务。
     *
     * 该方法只处理生命周期收尾，不改写媒体状态；页面离开后保留最后状态可以帮助测试和后续恢复逻辑判断。
     */
    fun dispose() {
        uploadJobs.values.forEach { it.cancel() }
        uploadJobs.clear()
    }

    private fun uploadReference(
        id: String,
        uriString: String,
        type: QuickCreateMediaType,
        targetTab: QuickCreateTab,
    ) {
        uploadJobs[id]?.cancel()
        uploadJobs[id] = scope.launch {
            val mimeType = type.uploadMimeType()
            val actualFileName = type.remoteUploadFileName()

            try {
                updateReferenceStatus(id, UploadStatus.UPLOADING, 0.1f, targetTab)
                val bytes = withContext(ioDispatcher) {
                    mediaResolver.readBytes(uriString)
                }
                updateReferenceStatus(id, UploadStatus.UPLOADING, 0.7f, targetTab)

                val remoteUrl = mediaUploadRepository.uploadMedia(
                    fileBytes = bytes,
                    fileName = actualFileName,
                    mimeType = mimeType,
                ).getOrElse { error ->
                    // 上传失败会在下方统一映射为页面错误状态；这里不写控制台日志，避免生产包泄露本地文件名或服务端异常细节。
                    throw error
                }

                updateReferenceStatus(id, UploadStatus.PROCESSING, 0.85f, targetTab)
                uiState.update { state ->
                    state.withUpdatedMediaReference(targetTab, id) { reference ->
                        reference.copy(
                            uploadStatus = UploadStatus.DONE,
                            uploadProgress = 1f,
                            remoteUrl = remoteUrl,
                            errorMessage = null,
                        )
                    }
                }
                scheduleFeePreviewForMediaReference(id)
            } catch (error: CancellationException) {
                // 取消来自页面销毁、删除素材或测试 teardown，属于正常生命周期收尾；
                // 不应把媒体标记为失败，也不能继续触发计费预览。
                throw error
            } catch (error: Exception) {
                uiState.update { state ->
                    state.withUpdatedMediaReference(targetTab, id) { reference ->
                        reference.copy(
                            uploadStatus = UploadStatus.FAILED,
                            errorMessage = QuickCreateRuntimeUiText.MediaUploadFailed.asQuickCreateUiMessage(),
                        )
                    }
                }
                scheduleFeePreviewForMediaReference(id)
            }
        }
    }

    private fun updateReferenceStatus(
        id: String,
        status: UploadStatus,
        progress: Float,
        targetTab: QuickCreateTab,
    ) {
        uiState.update { state ->
            state.withUpdatedMediaReference(targetTab, id) { reference ->
                reference.copy(uploadStatus = status, uploadProgress = progress)
            }
        }
        scheduleFeePreviewForMediaReference(id)
    }

    private fun scheduleFeePreviewForMediaReference(id: String) {
        if (generationRequestFactory.currentRelevantMediaReferences(uiState.value).any { it.id == id }) {
            onFeePreviewRequired()
        }
    }

    private fun QuickCreateUiState.withMediaReference(
        targetTab: QuickCreateTab,
        transform: (List<MediaReference>) -> List<MediaReference>,
    ): QuickCreateUiState =
        if (targetTab == QuickCreateTab.IMAGE) {
            copy(imageConfig = imageConfig.copy(mediaReferences = transform(imageConfig.mediaReferences)))
        } else {
            copy(videoConfig = videoConfig.copy(mediaReferences = transform(videoConfig.mediaReferences)))
        }

    private fun QuickCreateUiState.withUpdatedMediaReference(
        targetTab: QuickCreateTab,
        id: String,
        transform: (MediaReference) -> MediaReference,
    ): QuickCreateUiState =
        withMediaReference(targetTab) { mediaReferences ->
            mediaReferences.map { reference ->
                if (reference.id == id) transform(reference) else reference
            }
        }

    private fun QuickCreateUiState.mediaReferencesById(): Map<String, MediaReference> =
        (imageConfig.mediaReferences + videoConfig.mediaReferences).associateBy { it.id }

    private fun QuickCreateUiState.withUploadedMediaFrom(
        currentState: QuickCreateUiState,
        ids: Set<String>,
    ): QuickCreateUiState {
        val currentReferences = currentState.mediaReferencesById()
        return copy(
            imageConfig = imageConfig.copy(
                mediaReferences = imageConfig.mediaReferences.mergeUploadedMedia(currentReferences, ids),
            ),
            videoConfig = videoConfig.copy(
                mediaReferences = videoConfig.mediaReferences.mergeUploadedMedia(currentReferences, ids),
            ),
        )
    }

    private fun List<MediaReference>.mergeUploadedMedia(
        currentReferences: Map<String, MediaReference>,
        ids: Set<String>,
    ): List<MediaReference> =
        map { snapshotReference ->
            val currentReference = currentReferences[snapshotReference.id]
            if (snapshotReference.id in ids && currentReference != null) {
                snapshotReference.copy(
                    uploadStatus = currentReference.uploadStatus,
                    uploadProgress = currentReference.uploadProgress,
                    remoteUrl = currentReference.remoteUrl,
                )
            } else {
                snapshotReference
            }
        }

    private fun MediaReference.isUploadPending(): Boolean =
        uploadStatus == UploadStatus.UPLOADING || uploadStatus == UploadStatus.PROCESSING

    private fun QuickCreateMediaType.uploadMimeType(): String =
        when (this) {
            QuickCreateMediaType.IMAGE -> "image/jpeg"
            QuickCreateMediaType.VIDEO -> "video/mp4"
            QuickCreateMediaType.AUDIO -> "audio/mpeg"
        }

    private fun QuickCreateMediaType.remoteUploadFileName(): String {
        val extension = when (this) {
            QuickCreateMediaType.IMAGE -> "jpg"
            QuickCreateMediaType.VIDEO -> "mp4"
            QuickCreateMediaType.AUDIO -> "mp3"
        }
        return "${name.lowercase()}_${Clock.System.now().toEpochMilliseconds()}.$extension"
    }
}

// 等待上传完成时抛给页面的错误不拼接 displayName，避免本地媒体文件名进入错误上报或日志链路。
private val MEDIA_UPLOAD_FAILED_MESSAGE = QuickCreateRuntimeUiText.MediaUploadBlocked.asQuickCreateUiMessage()
private val MEDIA_UPLOAD_TIMEOUT_MESSAGE = QuickCreateRuntimeUiText.MediaUploadTimeout.asQuickCreateUiMessage()
