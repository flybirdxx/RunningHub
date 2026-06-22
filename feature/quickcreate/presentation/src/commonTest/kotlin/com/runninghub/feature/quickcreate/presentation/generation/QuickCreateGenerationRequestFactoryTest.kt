package com.runninghub.feature.quickcreate.presentation.generation

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceValidationIssue
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
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
}
