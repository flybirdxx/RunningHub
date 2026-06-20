package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.app.platform.MediaResolver
import com.runninghub.shared.domain.repository.ImageGenerationRequest
import com.runninghub.shared.domain.repository.QuickCreateInspirationTag
import com.runninghub.shared.domain.repository.QuickCreateInspirationTemplate
import com.runninghub.shared.domain.repository.QuickCreateInspirationTemplateDetail
import com.runninghub.shared.domain.repository.QuickCreateInspirationTemplatePage
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import com.runninghub.shared.domain.repository.QuickCreationFeePreview
import com.runninghub.shared.domain.repository.QuickCreationHistoryItem
import com.runninghub.shared.domain.repository.QuickCreationHistoryPage
import com.runninghub.shared.domain.repository.QuickCreationProject
import com.runninghub.shared.domain.repository.QuickCreationProjectPage
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import com.runninghub.shared.domain.repository.VideoGenerationRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateMediaUploadCoordinatorTest {

    @Test
    fun `add media references ignores blank inputs`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingMediaResolver()
        val uiState = MutableStateFlow(QuickCreateUiState())
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.addGlobalMediaReference("", QuickCreateMediaType.IMAGE)
        coordinator.addFieldMediaReference("content://image/1", QuickCreateMediaType.IMAGE, "")
        advanceUntilIdle()

        assertTrue(uiState.value.imageConfig.mediaReferences.isEmpty())
        assertEquals(0, mediaResolver.readRequests)
        assertEquals(0, repository.uploadRequests.size)
    }

    @Test
    fun `add global media reference uploads without field ownership`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingMediaResolver()
        val uiState = MutableStateFlow(QuickCreateUiState())
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.addGlobalMediaReference("content://image/global", QuickCreateMediaType.IMAGE)
        advanceUntilIdle()

        val reference = uiState.value.imageConfig.mediaReferences.single()
        assertEquals(null, reference.fieldParamKey)
        assertEquals(UploadStatus.DONE, reference.uploadStatus)
        assertEquals("https://example.com/uploaded-1", reference.remoteUrl)
        assertEquals(listOf("content://image/global"), mediaResolver.readUris)
        assertEquals(1, repository.uploadRequests.size)
    }

    @Test
    fun `add field media reference preserves field ownership after upload`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingMediaResolver()
        val uiState = MutableStateFlow(QuickCreateUiState())
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.addFieldMediaReference(
            uriString = "content://image/child",
            type = QuickCreateMediaType.IMAGE,
            fieldParamKey = "childImages",
        )
        advanceUntilIdle()

        val reference = uiState.value.imageConfig.mediaReferences.single()
        assertEquals("childImages", reference.fieldParamKey)
        assertEquals(UploadStatus.DONE, reference.uploadStatus)
        assertEquals("https://example.com/uploaded-1", reference.remoteUrl)
        assertEquals(listOf("content://image/child"), mediaResolver.readUris)
        assertEquals(1, repository.uploadRequests.size)
    }

    private fun createCoordinator(
        repository: QuickCreateRepository,
        mediaResolver: MediaResolver,
        uiState: MutableStateFlow<QuickCreateUiState>,
        dispatcher: CoroutineDispatcher,
    ): QuickCreateMediaUploadCoordinator =
        QuickCreateMediaUploadCoordinator(
            quickCreateRepository = repository,
            mediaResolver = mediaResolver,
            generationRequestFactory = QuickCreateGenerationRequestFactory(),
            scope = kotlinx.coroutines.CoroutineScope(dispatcher),
            uiState = uiState,
            ioDispatcher = dispatcher,
            onFeePreviewRequired = {},
        )

    private class RecordingMediaResolver : MediaResolver {
        val readUris = mutableListOf<String>()
        val readRequests: Int
            get() = readUris.size

        override fun readBytes(uri: String): ByteArray {
            readUris += uri
            return byteArrayOf(1, 2, 3)
        }

        override fun getDisplayName(uri: String): String =
            uri.substringAfterLast('/').ifBlank { "media.bin" }

        override fun getFileSizeBytes(uri: String): Long = 3L
    }

    private data class UploadRequest(
        val fileName: String,
        val mimeType: String,
        val size: Int,
    )

    private class RecordingQuickCreateRepository : QuickCreateRepository {
        val uploadRequests = mutableListOf<UploadRequest>()

        override fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus> =
            emptyFlow()

        override fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus> =
            emptyFlow()

        override suspend fun previewImageQuickCreationFee(
            request: ImageGenerationRequest,
        ): Result<QuickCreationFeePreview> =
            unsupported()

        override suspend fun previewVideoQuickCreationFee(
            request: VideoGenerationRequest,
        ): Result<QuickCreationFeePreview> =
            unsupported()

        override suspend fun uploadMedia(
            fileBytes: ByteArray,
            fileName: String,
            mimeType: String,
        ): Result<String> {
            uploadRequests += UploadRequest(
                fileName = fileName,
                mimeType = mimeType,
                size = fileBytes.size,
            )
            return Result.success("https://example.com/uploaded-${uploadRequests.size}")
        }

        override suspend fun getInspirationTags(): Result<List<QuickCreateInspirationTag>> =
            unsupported()

        override suspend fun getInspirationTemplates(
            page: Int,
            size: Int,
            tagId: String?,
        ): Result<QuickCreateInspirationTemplatePage> =
            unsupported()

        override suspend fun getInspirationTemplateDetail(
            templateId: String,
        ): Result<QuickCreateInspirationTemplateDetail> =
            unsupported()

        override suspend fun getModels(
            categoryId: String,
        ): Result<List<QuickCreationServiceModel>> =
            unsupported()

        override suspend fun listQuickCreationHistory(
            page: Int,
            size: Int,
        ): Result<QuickCreationHistoryPage> =
            unsupported()

        override suspend fun getQuickCreationHistoryDetail(
            outputId: String,
        ): Result<QuickCreationHistoryItem> =
            unsupported()

        override suspend fun cancelQuickCreationTask(taskId: String): Result<Unit> =
            unsupported()

        override suspend fun listQuickCreationProjects(
            page: Int,
            size: Int,
        ): Result<QuickCreationProjectPage> =
            unsupported()

        override suspend fun listQuickCreationProjectTasks(
            projectId: String,
            page: Int,
            size: Int,
        ): Result<QuickCreationHistoryPage> =
            unsupported()

        override suspend fun createQuickCreationProject(
            name: String,
        ): Result<QuickCreationProject> =
            unsupported()

        override suspend fun renameQuickCreationProject(
            projectId: String,
            name: String,
        ): Result<Unit> =
            unsupported()

        override suspend fun deleteQuickCreationProject(projectId: String): Result<Unit> =
            unsupported()

        override suspend fun pinQuickCreationProject(
            projectId: String,
            pinned: Boolean,
        ): Result<Unit> =
            unsupported()

        override suspend fun getQuickCreationProjectDetail(
            projectId: String,
        ): Result<QuickCreationProject> =
            unsupported()

        private fun <T> unsupported(): Result<T> =
            error("QuickCreateMediaUploadCoordinatorTest should only call uploadMedia")
    }
}
