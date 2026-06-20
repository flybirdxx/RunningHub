package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationRequestFactory

import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState

import com.runninghub.app.platform.MediaResolver
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig

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
        repository: QuickCreationMediaUploadRepository,
        mediaResolver: MediaResolver,
        uiState: MutableStateFlow<QuickCreateUiState>,
        dispatcher: CoroutineDispatcher,
    ): QuickCreateMediaUploadCoordinator =
        QuickCreateMediaUploadCoordinator(
            mediaUploadRepository = repository,
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

    private class RecordingQuickCreateRepository : QuickCreationMediaUploadRepository {
        val uploadRequests = mutableListOf<UploadRequest>()

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
    }
}
