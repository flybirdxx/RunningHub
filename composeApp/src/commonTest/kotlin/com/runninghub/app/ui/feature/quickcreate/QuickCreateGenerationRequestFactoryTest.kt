package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationRequestFactory
import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationRequestBuildResult
import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationBlockReason

import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldExtra
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldInputChild
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldOption
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldVisibilityCondition
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig

class QuickCreateGenerationRequestFactoryTest {
    private val factory = QuickCreateGenerationRequestFactory()

    @Test
    fun `build image request returns null when prompt is required but blank or current tab is video`() {
        val model = serviceModel()

        val blankPrompt = QuickCreateUiState(
            selectedImageServiceModel = model,
            imageConfig = ImageConfig(prompt = "   "),
        )
        val videoTab = blankPrompt.copy(
            currentTab = QuickCreateTab.VIDEO,
            videoConfig = VideoConfig(prompt = "video prompt"),
        )

        assertNull(factory.buildImageGenerationRequest(blankPrompt, requirePrompt = true))
        assertNull(factory.buildImageGenerationRequest(videoTab, requirePrompt = true))
    }

    @Test
    fun `build current generation request blocks invalid prompt before repository request`() {
        val blankPrompt = QuickCreateUiState(
            imageConfig = ImageConfig(prompt = "   "),
        )
        val overLimitPrompt = blankPrompt.copy(
            imageConfig = ImageConfig(prompt = "x".repeat(MAX_PROMPT_CHARS + 1)),
        )

        val blankResult = factory.buildCurrentGenerationRequest(blankPrompt, validateUploads = false)
        val overLimitResult = factory.buildCurrentGenerationRequest(overLimitPrompt, validateUploads = false)

        assertEquals(
            QuickCreateGenerationBlockReason.PromptRequired,
            (blankResult as QuickCreateGenerationRequestBuildResult.Blocked).reason,
        )
        assertEquals(
            QuickCreateGenerationBlockReason.PromptTooLong(MAX_PROMPT_CHARS),
            (overLimitResult as QuickCreateGenerationRequestBuildResult.Blocked).reason,
        )
    }

    @Test
    fun `build current generation request defers upload validation until upload wait finishes`() {
        val state = QuickCreateUiState(
            selectedImageServiceModel = serviceModel(
                fields = listOf(uploadField(paramKey = "imageUrls", required = true)),
            ),
            imageConfig = ImageConfig(prompt = "green icon"),
        )

        val beforeUploadWait = factory.buildCurrentGenerationRequest(state, validateUploads = false)
        val afterUploadWait = factory.buildCurrentGenerationRequest(state, validateUploads = true)

        assertTrue(beforeUploadWait is QuickCreateGenerationRequestBuildResult.ImageReady)
        assertTrue(afterUploadWait is QuickCreateGenerationRequestBuildResult.Blocked)
        assertTrue((afterUploadWait.reason as QuickCreateGenerationBlockReason.CustomMessage).message.contains("不能为空"))
    }

    @Test
    fun `build image request merges built in params defaults and active service params only`() {
        val model = serviceModel(
            fields = listOf(
                optionField(paramKey = "mode", defaultValue = "text"),
                textField(paramKey = "style", defaultValue = "clean"),
                textField(paramKey = "hidden", defaultValue = "secret", visible = false),
            ),
        )
        val state = QuickCreateUiState(
            selectedImageServiceModel = model,
            imageConfig = ImageConfig(prompt = "green icon"),
            imageServiceParams = mapOf(
                "mode" to "image",
                "style" to "flat",
                "unknown" to "ignored",
            ),
        )

        val request = factory.buildImageGenerationRequest(state, requirePrompt = true)

        assertEquals("16:9", request?.quickCreationParams?.get("aspectRatio"))
        assertEquals("1K", request?.quickCreationParams?.get("resolution"))
        assertEquals("medium", request?.quickCreationParams?.get("quality"))
        assertEquals("image", request?.quickCreationParams?.get("mode"))
        assertEquals("flat", request?.quickCreationParams?.get("style"))
        assertNull(request?.quickCreationParams?.get("unknown"))
        assertNull(request?.quickCreationParams?.get("hidden"))
    }

    @Test
    fun `build list params maps global upload only when exactly one active field of same media type exists`() {
        val globalImage = mediaReference(
            id = "global-image",
            type = QuickCreateMediaType.IMAGE,
            remoteUrl = "https://example.com/global.png",
        )
        val singleUploadState = QuickCreateUiState(
            selectedImageServiceModel = serviceModel(fields = listOf(uploadField(paramKey = "imageUrls"))),
            imageConfig = ImageConfig(
                prompt = "prompt",
                mediaReferences = listOf(globalImage),
            ),
        )
        val doubleUploadState = singleUploadState.copy(
            selectedImageServiceModel = serviceModel(
                fields = listOf(
                    uploadField(paramKey = "firstImages"),
                    uploadField(paramKey = "secondImages"),
                ),
            ),
        )

        val singleRequest = factory.buildImageGenerationRequest(singleUploadState, requirePrompt = true)
        val doubleRequest = factory.buildImageGenerationRequest(doubleUploadState, requirePrompt = true)

        assertEquals(listOf("https://example.com/global.png"), singleRequest?.quickCreationListParams?.get("imageUrls"))
        assertTrue(doubleRequest?.quickCreationListParams.orEmpty().isEmpty())
    }

    @Test
    fun `validate service uploads ignores hidden and inactive child fields but blocks active required child`() {
        val model = serviceModel(
            fields = listOf(
                uploadField(paramKey = "hiddenImages", required = true, visible = false),
                optionField(
                    paramKey = "mode",
                    defaultValue = "text",
                    children = listOf(
                        childUploadField(
                            paramKey = "childImages",
                            title = "子图片",
                            required = true,
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "mode",
                                values = listOf("image"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val inactiveState = QuickCreateUiState(
            selectedImageServiceModel = model,
            imageServiceParams = mapOf("mode" to "text"),
        )
        val activeState = inactiveState.copy(
            imageServiceParams = mapOf("mode" to "image"),
        )

        assertNull(factory.validateCurrentServiceUploads(inactiveState))
        assertEquals("子图片 不能为空", factory.validateCurrentServiceUploads(activeState))
    }

    @Test
    fun `current relevant media references keeps global refs and active field refs only`() {
        val model = serviceModel(
            fields = listOf(
                optionField(
                    paramKey = "mode",
                    defaultValue = "text",
                    children = listOf(
                        childUploadField(
                            paramKey = "activeImages",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "mode",
                                values = listOf("image"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val global = mediaReference(id = "global", fieldParamKey = null)
        val active = mediaReference(id = "active", fieldParamKey = "activeImages")
        val inactive = mediaReference(id = "inactive", fieldParamKey = "inactiveImages")
        val state = QuickCreateUiState(
            selectedImageServiceModel = model,
            imageServiceParams = mapOf("mode" to "image"),
            imageConfig = ImageConfig(
                prompt = "prompt",
                mediaReferences = listOf(global, active, inactive),
            ),
        )

        val relevantIds = factory.currentRelevantMediaReferences(state).map { it.id }

        assertEquals(listOf("global", "active"), relevantIds)
    }

    private fun serviceModel(
        fields: List<QuickCreationServiceField> = emptyList(),
    ): QuickCreationServiceModel =
        QuickCreationServiceModel(
            categoryId = "IMAGE",
            groupName = "图片生成",
            bindingId = "binding-1",
            skuId = "sku-1",
            name = "测试模型",
            description = null,
            fields = fields,
        )

    private fun optionField(
        paramKey: String,
        defaultValue: String,
        children: List<QuickCreationServiceFieldInputChild> = emptyList(),
    ): QuickCreationServiceField =
        QuickCreationServiceField(
            fieldKey = paramKey,
            paramKey = paramKey,
            fieldType = "LIST",
            required = false,
            defaultValue = defaultValue,
            options = listOf(
                QuickCreationServiceFieldOption("文本", "text"),
                QuickCreationServiceFieldOption("图片", "image"),
            ),
            inputExtra = QuickCreationServiceFieldExtra(inputChildren = children),
        )

    private fun textField(
        paramKey: String,
        defaultValue: String,
        visible: Boolean = true,
    ): QuickCreationServiceField =
        QuickCreationServiceField(
            fieldKey = paramKey,
            paramKey = paramKey,
            fieldType = "STRING",
            required = false,
            defaultValue = defaultValue,
            options = emptyList(),
            visible = visible,
        )

    private fun uploadField(
        paramKey: String,
        required: Boolean = false,
        visible: Boolean = true,
    ): QuickCreationServiceField =
        QuickCreationServiceField(
            fieldKey = paramKey,
            paramKey = paramKey,
            fieldType = "IMAGE_UPLOAD",
            required = required,
            defaultValue = null,
            options = emptyList(),
            maxUploadCount = 1,
            visible = visible,
        )

    private fun childUploadField(
        paramKey: String,
        title: String = paramKey,
        required: Boolean = false,
        visibleWhen: QuickCreationServiceFieldVisibilityCondition? = null,
    ): QuickCreationServiceFieldInputChild =
        QuickCreationServiceFieldInputChild(
            fieldKey = paramKey,
            paramKey = paramKey,
            fieldType = "IMAGE_UPLOAD",
            required = required,
            title = title,
            maxInputCount = 1,
            visibleWhen = visibleWhen,
        )

    private fun mediaReference(
        id: String,
        type: QuickCreateMediaType = QuickCreateMediaType.IMAGE,
        fieldParamKey: String? = null,
        remoteUrl: String = "https://example.com/$id.png",
    ): MediaReference =
        MediaReference(
            id = id,
            type = type,
            uri = "content://$id",
            displayName = "$id.png",
            fileSizeBytes = 10L,
            fieldParamKey = fieldParamKey,
            uploadStatus = UploadStatus.DONE,
            uploadProgress = 1f,
            remoteUrl = remoteUrl,
        )
}
