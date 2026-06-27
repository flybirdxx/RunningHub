package com.runninghub.feature.quickcreate.presentation.coordinator

import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftRepository
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftSnapshot
import com.runninghub.feature.quickcreate.domain.QuickCreateModelSelectionRepository
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationProject
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectPage
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceKind
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateCoordinatorTest {

    @Test
    fun `initialize checks draft and loads catalog history and projects from feature coordinator`() = runTest {
        val dependencies = RecordingQuickCreateDependencies()
        val state = MutableStateFlow(QuickCreateUiState())
        val coordinator = QuickCreateCoordinator(
            historyRepository = dependencies,
            mediaResolver = RecordingQuickCreateMediaResolver(),
            draftRepository = dependencies,
            modelSelectionRepository = dependencies,
            scope = this,
            uiState = state,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            modelCatalogRepository = dependencies,
            generationRepository = dependencies,
            feePreviewRepository = dependencies,
            mediaUploadRepository = dependencies,
            projectRepository = dependencies,
        )

        coordinator.initialize()
        runCurrent()

        assertEquals(1, dependencies.draftChecks)
        assertEquals(listOf(QuickCreationServiceKind.IMAGE, QuickCreationServiceKind.VIDEO), dependencies.modelRequests)
        assertEquals(listOf(1 to 10), dependencies.historyRequests)
        assertEquals(listOf(1 to 20), dependencies.projectRequests)
        assertTrue(state.value.hasDraft)
        assertEquals("hello image", state.value.draftData?.imagePrompt)
        assertEquals("image-binding", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("video-binding", state.value.selectedVideoServiceModel?.bindingId)
        assertEquals(listOf("history-task"), state.value.historyItems.map { it.taskId })
        assertEquals(listOf("project-1"), state.value.projects.map { it.projectId })
    }

    /**
     * Coordinator 初始化测试使用的窄 fake。
     *
     * 本 fake 同时实现构造器需要的所有领域仓库接口，但只放行初始化会触发的草稿、
     * 模型目录、最近历史和项目列表四个入口；其他方法 fail-fast，避免测试意外覆盖生成、
     * 计费、上传或灵感模板流程。
     */
    private class RecordingQuickCreateDependencies :
        QuickCreateDraftRepository,
        QuickCreateModelSelectionRepository,
        QuickCreationModelCatalogRepository,
        QuickCreationTaskHistoryRepository,
        QuickCreationProjectRepository,
        QuickCreationGenerationRepository,
        QuickCreationFeePreviewRepository,
        QuickCreationMediaUploadRepository {

        var draftChecks = 0
        val modelRequests = mutableListOf<QuickCreationServiceKind>()
        val historyRequests = mutableListOf<Pair<Int, Int>>()
        val projectRequests = mutableListOf<Pair<Int, Int>>()

        override suspend fun getRestorableDraft(): QuickCreateDraftSnapshot {
            draftChecks += 1
            return QuickCreateDraftSnapshot(
                currentTab = "IMAGE",
                imagePrompt = "hello image",
            )
        }

        override suspend fun getLastImageServiceModelIdentityKey(): String? =
            null

        override suspend fun saveLastImageServiceModelIdentityKey(identityKey: String): Unit =
            unexpected("saveLastImageServiceModelIdentityKey")

        override suspend fun getLastVideoServiceModelIdentityKey(): String? =
            null

        override suspend fun saveLastVideoServiceModelIdentityKey(identityKey: String): Unit =
            unexpected("saveLastVideoServiceModelIdentityKey")

        override suspend fun getModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>> {
            modelRequests += kind
            return Result.success(
                when (kind) {
                    QuickCreationServiceKind.IMAGE -> listOf(serviceModel("IMAGE", "image-binding", "image-sku"))
                    QuickCreationServiceKind.VIDEO -> listOf(serviceModel("VIDEO", "video-binding", "video-sku"))
                }
            )
        }

        override suspend fun listQuickCreationHistory(page: Int, size: Int): Result<QuickCreationHistoryPage> {
            historyRequests += page to size
            return Result.success(
                QuickCreationHistoryPage(
                    page = page,
                    size = size,
                    total = 1,
                    items = listOf(historyItem()),
                )
            )
        }

        override suspend fun listQuickCreationProjects(page: Int, size: Int): Result<QuickCreationProjectPage> {
            projectRequests += page to size
            return Result.success(
                QuickCreationProjectPage(
                    page = page,
                    size = size,
                    total = 1,
                    pages = 1,
                    hasNext = false,
                    hasPrevious = false,
                    items = listOf(QuickCreationProject(projectId = "project-1", name = "测试项目")),
                )
            )
        }

        override suspend fun saveDraft(snapshot: QuickCreateDraftSnapshot): Unit =
            unexpected("saveDraft")

        override suspend fun clearDraft(): Unit =
            unexpected("clearDraft")

        override fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus> =
            unexpected("generateImage")

        override fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus> =
            unexpected("generateVideo")

        override suspend fun previewImageQuickCreationFee(
            request: ImageGenerationRequest,
        ): Result<QuickCreationFeePreview> =
            unexpected("previewImageQuickCreationFee")

        override suspend fun previewVideoQuickCreationFee(
            request: VideoGenerationRequest,
        ): Result<QuickCreationFeePreview> =
            unexpected("previewVideoQuickCreationFee")

        override suspend fun uploadMedia(
            fileBytes: ByteArray,
            fileName: String,
            mimeType: String,
        ): Result<String> =
            unexpected("uploadMedia")

        override suspend fun getQuickCreationHistoryDetail(outputId: String): Result<QuickCreationHistoryItem> =
            unexpected("getQuickCreationHistoryDetail")

        override suspend fun cancelQuickCreationTask(taskId: String): Result<Unit> =
            unexpected("cancelQuickCreationTask")

        override suspend fun listQuickCreationProjectTasks(
            projectId: String,
            page: Int,
            size: Int,
        ): Result<QuickCreationHistoryPage> =
            unexpected("listQuickCreationProjectTasks")

        override suspend fun createQuickCreationProject(name: String): Result<QuickCreationProject> =
            unexpected("createQuickCreationProject")

        override suspend fun renameQuickCreationProject(projectId: String, name: String): Result<Unit> =
            unexpected("renameQuickCreationProject")

        override suspend fun deleteQuickCreationProject(projectId: String): Result<Unit> =
            unexpected("deleteQuickCreationProject")

        override suspend fun pinQuickCreationProject(projectId: String, pinned: Boolean): Result<Unit> =
            unexpected("pinQuickCreationProject")

        override suspend fun getQuickCreationProjectDetail(projectId: String): Result<QuickCreationProject> =
            unexpected("getQuickCreationProjectDetail")
    }

    /**
     * 初始化流程不应读取本地媒体，若被调用说明 Coordinator 迁移测试越过了预期边界。
     */
    private class RecordingQuickCreateMediaResolver : QuickCreateMediaResolver {
        override fun readBytes(uri: String): ByteArray = unexpected("readBytes")

        override fun getDisplayName(uri: String): String? = unexpected("getDisplayName")

        override fun getFileSizeBytes(uri: String): Long = unexpected("getFileSizeBytes")
    }
}

private fun serviceModel(
    categoryId: String,
    bindingId: String,
    skuId: String,
): QuickCreationServiceModel =
    QuickCreationServiceModel(
        categoryId = categoryId,
        groupName = null,
        bindingId = bindingId,
        skuId = skuId,
        name = "$bindingId model",
        description = null,
        fields = listOf(serviceField("style")),
    )

private fun serviceField(paramKey: String): QuickCreationServiceField =
    QuickCreationServiceField(
        fieldKey = paramKey,
        paramKey = paramKey,
        fieldType = "TEXT",
        required = false,
        defaultValue = "default",
        options = emptyList(),
    )

private fun historyItem(): QuickCreationHistoryItem =
    QuickCreationHistoryItem(
        taskId = "history-task",
        status = "SUCCESS",
        categoryId = "IMAGE",
        params = mapOf("prompt" to "history prompt"),
        outputs = listOf(
            QuickCreationHistoryOutput(
                outputId = "history-output",
                url = "https://example.com/history.png",
                type = "png",
            )
        ),
    )

private fun unexpected(methodName: String): Nothing =
    throw AssertionError("$methodName should not be called by coordinator initialization")
