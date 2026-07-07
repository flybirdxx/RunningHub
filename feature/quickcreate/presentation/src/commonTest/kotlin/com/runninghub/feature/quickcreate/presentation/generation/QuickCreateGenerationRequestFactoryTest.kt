package com.runninghub.feature.quickcreate.presentation.generation

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldOption
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceValidationIssue
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateGenerationRequestFactoryTest {
    private val factory = QuickCreateGenerationRequestFactory()

    @Test
    fun `build current generation request blocks invalid prompt with stable reason`() {
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
    fun `build current generation request blocks service validation with stable issue`() {
        val state = QuickCreateUiState(
            imageConfig = ImageConfig(prompt = "ready"),
            selectedImageServiceModel = QuickCreationServiceModel(
                categoryId = "IMAGE",
                groupName = "group",
                bindingId = "binding",
                skuId = "sku",
                name = "model",
                description = null,
                fields = listOf(
                    QuickCreationServiceField(
                        fieldKey = "tagline",
                        paramKey = "tagline",
                        fieldType = "STRING",
                        required = true,
                        defaultValue = null,
                        options = emptyList(),
                    ),
                ),
            ),
            imageServiceParams = mapOf("tagline" to ""),
        )

        val result = factory.buildCurrentGenerationRequest(state, validateUploads = false)

        assertEquals(
            QuickCreateGenerationBlockReason.ServiceValidation(
                QuickCreationServiceValidationIssue.Required("tagline"),
            ),
            (result as QuickCreateGenerationRequestBuildResult.Blocked).reason,
        )
    }

    @Test
    fun `build current video request follows documented seedance params when catalog options are incomplete`() {
        val state = QuickCreateUiState(
            currentTab = QuickCreateTab.VIDEO,
            videoConfig = VideoConfig(prompt = "ready"),
            selectedVideoServiceModel = QuickCreationServiceModel(
                categoryId = "VIDEO",
                groupName = "Seedance2.0",
                bindingId = "binding",
                skuId = "sku",
                name = "seedance2.0-Mini/多模态视频",
                description = null,
                fields = listOf(
                    QuickCreationServiceField(
                        fieldKey = "duration",
                        paramKey = "duration",
                        fieldType = "LIST",
                        required = true,
                        defaultValue = "5",
                        options = (6..15).map { seconds ->
                            QuickCreationServiceFieldOption(
                                label = seconds.toString(),
                                value = seconds.toString(),
                            )
                        },
                    ),
                    QuickCreationServiceField(
                        fieldKey = "imageUrls",
                        paramKey = "imageUrls",
                        fieldType = "IMAGE",
                        required = false,
                        defaultValue = "9",
                        options = emptyList(),
                        maxUploadCount = 9,
                    ),
                    QuickCreationServiceField(
                        fieldKey = "videoUrls",
                        paramKey = "videoUrls",
                        fieldType = "VIDEO",
                        required = false,
                        defaultValue = "3",
                        options = emptyList(),
                        maxUploadCount = 3,
                    ),
                    QuickCreationServiceField(
                        fieldKey = "ratio",
                        paramKey = "ratio",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "16:9",
                        options = listOf(
                            QuickCreationServiceFieldOption(label = "16:9", value = "16:9"),
                            QuickCreationServiceFieldOption(label = "9:16", value = "9:16"),
                        ),
                    ),
                    QuickCreationServiceField(
                        fieldKey = "realPersonMode",
                        paramKey = "realPersonMode",
                        fieldType = "BOOLEAN",
                        required = false,
                        defaultValue = "true",
                        options = emptyList(),
                    ),
                    QuickCreationServiceField(
                        fieldKey = "conversionSlots",
                        paramKey = "conversionSlots",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "[\"all\"]",
                        options = listOf(QuickCreationServiceFieldOption(label = "all", value = "all")),
                    ),
                ),
            ),
            videoServiceParams = mapOf(
                "duration" to "4",
                "ratio" to "9:16",
                "realPersonMode" to "false",
                "conversionSlots" to "[\"all\"]",
            ),
        )

        val result = factory.buildCurrentGenerationRequest(state, validateUploads = false)

        val request = (result as QuickCreateGenerationRequestBuildResult.VideoReady).request
        assertEquals("4", request.quickCreationParams["duration"])
        assertEquals("9:16", request.quickCreationParams["ratio"])
        assertEquals("false", request.quickCreationParams["realPersonMode"])
        assertEquals(null, request.quickCreationParams["conversionSlots"])
        assertEquals(null, request.quickCreationParams["imageUrls"])
        assertEquals(null, request.quickCreationParams["videoUrls"])
        assertEquals(null, request.quickCreationListParams["imageUrls"])
        assertEquals(null, request.quickCreationListParams["videoUrls"])
    }
}
