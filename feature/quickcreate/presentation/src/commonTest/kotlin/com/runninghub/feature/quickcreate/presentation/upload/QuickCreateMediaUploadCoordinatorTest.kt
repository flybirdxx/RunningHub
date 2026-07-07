package com.runninghub.feature.quickcreate.presentation.upload

import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationUploadMediaKind
import com.runninghub.feature.quickcreate.presentation.QuickCreateRuntimeUiText
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationRequestFactory
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateMediaUploadCoordinatorTest {

    @Test
    fun `add media references ignores blank inputs`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingQuickCreateMediaResolver()
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
        val mediaResolver = RecordingQuickCreateMediaResolver()
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
    fun `video upload preserves picker extension and matching mime type`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingQuickCreateMediaResolver(displayName = "runninghub-picked-1.MOV")
        val uiState = MutableStateFlow(QuickCreateUiState(currentTab = QuickCreateTab.VIDEO))
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.addGlobalMediaReference("file:///tmp/runninghub-picked-1.MOV", QuickCreateMediaType.VIDEO)
        advanceUntilIdle()

        val upload = repository.uploadRequests.single()
        assertTrue(upload.fileName.startsWith("video_"))
        assertTrue(upload.fileName.endsWith(".mov"))
        assertEquals("video/quicktime", upload.mimeType)
    }

    @Test
    fun `audio field upload preserves picker extension and matching mime type`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingQuickCreateMediaResolver(displayName = "voice.m4a")
        val uiState = MutableStateFlow(QuickCreateUiState(currentTab = QuickCreateTab.VIDEO))
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.addFieldMediaReference(
            uriString = "file:///tmp/voice.m4a",
            type = QuickCreateMediaType.AUDIO,
            fieldParamKey = "audioUrls",
        )
        advanceUntilIdle()

        val upload = repository.uploadRequests.single()
        assertTrue(upload.fileName.startsWith("audio_"))
        assertTrue(upload.fileName.endsWith(".m4a"))
        assertEquals("audio/mp4", upload.mimeType)
    }

    @Test
    fun `global media over local upload limit fails before reading bytes`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingQuickCreateMediaResolver(fileSizeBytes = OVERSIZED_MEDIA_BYTES)
        val uiState = MutableStateFlow(QuickCreateUiState(currentTab = QuickCreateTab.VIDEO))
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.addGlobalMediaReference("content://video/oversized", QuickCreateMediaType.VIDEO)
        advanceUntilIdle()

        val reference = uiState.value.videoConfig.mediaReferences.single()
        assertEquals(UploadStatus.FAILED, reference.uploadStatus)
        assertEquals(0f, reference.uploadProgress)
        assertEquals(QuickCreateRuntimeUiText.MediaUploadBlocked.asQuickCreateUiMessage(), reference.errorMessage)
        assertEquals(0, mediaResolver.readRequests)
        assertEquals(0, repository.uploadRequests.size)
    }

    @Test
    fun `field media over service upload limit fails before reading bytes`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingQuickCreateMediaResolver(fileSizeBytes = 3L)
        val uiState = MutableStateFlow(
            QuickCreateUiState(
                selectedImageServiceModel = serviceModel(
                    fields = listOf(
                        QuickCreationServiceField(
                            fieldKey = "referenceImage",
                            paramKey = "referenceImage",
                            fieldType = "IMAGE_UPLOAD",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadSize = 2L,
                            uploadMediaKind = QuickCreationUploadMediaKind.IMAGE,
                        ),
                    ),
                ),
            ),
        )
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.addFieldMediaReference(
            uriString = "content://image/oversized",
            type = QuickCreateMediaType.IMAGE,
            fieldParamKey = "referenceImage",
        )
        advanceUntilIdle()

        val reference = uiState.value.imageConfig.mediaReferences.single()
        assertEquals("referenceImage", reference.fieldParamKey)
        assertEquals(UploadStatus.FAILED, reference.uploadStatus)
        assertEquals(QuickCreateRuntimeUiText.MediaUploadBlocked.asQuickCreateUiMessage(), reference.errorMessage)
        assertEquals(0, mediaResolver.readRequests)
        assertEquals(0, repository.uploadRequests.size)
    }

    @Test
    fun `add field media reference preserves field ownership after upload`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingQuickCreateMediaResolver()
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

    @Test
    fun `await pending uploads fails failed media before generation submit`() = runTest {
        val repository = RecordingQuickCreateRepository(
            uploadResult = Result.failure(IllegalStateException("raw upload failure")),
        )
        val mediaResolver = RecordingQuickCreateMediaResolver()
        val uiState = MutableStateFlow(QuickCreateUiState())
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.addGlobalMediaReference("content://image/bad", QuickCreateMediaType.IMAGE)
        advanceUntilIdle()

        val reference = uiState.value.imageConfig.mediaReferences.single()
        assertEquals(UploadStatus.FAILED, reference.uploadStatus)
        assertEquals(1, repository.uploadRequests.size)

        val error = assertFailsWith<QuickCreateMediaUploadException> {
            coordinator.awaitPendingUploads(uiState.value)
        }

        assertEquals(QuickCreateRuntimeUiText.MediaUploadFailed.asQuickCreateUiMessage(), error.uiMessage)
    }

    @Test
    fun `attach remote media reference joins as done without resolver or upload`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingQuickCreateMediaResolver()
        val uiState = MutableStateFlow(QuickCreateUiState())
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.attachRemoteMediaReference(
            url = "https://example.com/results/output.png",
            type = QuickCreateMediaType.IMAGE,
        )
        advanceUntilIdle()

        val reference = uiState.value.imageConfig.mediaReferences.single()
        assertEquals(UploadStatus.DONE, reference.uploadStatus)
        assertEquals("https://example.com/results/output.png", reference.remoteUrl)
        assertEquals("https://example.com/results/output.png", reference.uri)
        assertEquals("output.png", reference.displayName)
        assertEquals(null, reference.fieldParamKey)
        assertEquals(0, mediaResolver.readRequests)
        assertEquals(0, repository.uploadRequests.size)
    }

    @Test
    fun `attach remote media reference strips query before resolving display name`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingQuickCreateMediaResolver()
        val uiState = MutableStateFlow(QuickCreateUiState())
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        // OSS 签名 URL 的查询串可能含 `/`；展示名必须先剥离 query 再取路径段。
        coordinator.attachRemoteMediaReference(
            url = "https://cdn.example.com/img/cat.png?Signature=ab/cd",
            type = QuickCreateMediaType.IMAGE,
        )
        advanceUntilIdle()

        val reference = uiState.value.imageConfig.mediaReferences.single()
        assertEquals("cat.png", reference.displayName)
        assertEquals("https://cdn.example.com/img/cat.png?Signature=ab/cd", reference.remoteUrl)
    }

    @Test
    fun `attach remote media reference ignores blank url`() = runTest {
        val repository = RecordingQuickCreateRepository()
        val mediaResolver = RecordingQuickCreateMediaResolver()
        val uiState = MutableStateFlow(QuickCreateUiState())
        val coordinator = createCoordinator(
            repository = repository,
            mediaResolver = mediaResolver,
            uiState = uiState,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        coordinator.attachRemoteMediaReference(url = "   ", type = QuickCreateMediaType.IMAGE)
        advanceUntilIdle()

        assertTrue(uiState.value.imageConfig.mediaReferences.isEmpty())
        assertEquals(0, mediaResolver.readRequests)
        assertEquals(0, repository.uploadRequests.size)
    }

    private fun createCoordinator(
        repository: QuickCreationMediaUploadRepository,
        mediaResolver: QuickCreateMediaResolver,
        uiState: MutableStateFlow<QuickCreateUiState>,
        dispatcher: CoroutineDispatcher,
    ): QuickCreateMediaUploadCoordinator =
        QuickCreateMediaUploadCoordinator(
            mediaUploadRepository = repository,
            mediaResolver = mediaResolver,
            generationRequestFactory = QuickCreateGenerationRequestFactory(),
            scope = CoroutineScope(dispatcher),
            uiState = uiState,
            ioDispatcher = dispatcher,
            onFeePreviewRequired = {},
        )

    private class RecordingQuickCreateMediaResolver(
        private val fileSizeBytes: Long = 3L,
        private val displayName: String? = null,
    ) : QuickCreateMediaResolver {
        val readUris = mutableListOf<String>()
        val readRequests: Int
            get() = readUris.size

        override fun readBytes(uri: String): ByteArray {
            readUris += uri
            return byteArrayOf(1, 2, 3)
        }

        override fun getDisplayName(uri: String): String =
            displayName ?: uri.substringAfterLast('/').ifBlank { "media.bin" }

        override fun getFileSizeBytes(uri: String): Long = fileSizeBytes
    }

    private data class UploadRequest(
        val fileName: String,
        val mimeType: String,
        val size: Int,
    )

    private class RecordingQuickCreateRepository(
        private val uploadResult: Result<String>? = null,
    ) : QuickCreationMediaUploadRepository {
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
            uploadResult?.let { return it }
            return Result.success("https://example.com/uploaded-${uploadRequests.size}")
        }
    }

    private fun serviceModel(
        fields: List<QuickCreationServiceField>,
    ): QuickCreationServiceModel =
        QuickCreationServiceModel(
            categoryId = "image",
            groupName = null,
            bindingId = "binding",
            skuId = "sku",
            name = "Image model",
            description = null,
            fields = fields,
        )

    private companion object {
        private const val OVERSIZED_MEDIA_BYTES = 101L * 1024L * 1024L
    }
}
