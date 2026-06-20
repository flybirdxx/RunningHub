package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationFeePreview
import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldExtra
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldOption
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateFeePreviewInteractorTest {
    @Test
    fun `stale image fee preview result does not overwrite latest interactor state`() = runTest {
        val repository = QuickCreateScreenModelTest.FakeQuickCreateRepository().apply {
            imageFeePreviewHandler = { request ->
                if (request.prompt == "slow prompt") {
                    withContext(NonCancellable) {
                        delay(1_000)
                    }
                    Result.success(feePreview(requiredCashAmount = 3.33))
                } else {
                    Result.success(feePreview(requiredCashAmount = 0.76))
                }
            }
        }
        val state = MutableStateFlow(
            QuickCreateUiState(
                imageConfig = ImageConfig(prompt = "slow prompt"),
            )
        )
        val interactor = createInteractor(repository = repository, state = state, scope = this)

        interactor.schedule()
        advanceTimeBy(500)
        runCurrent()
        state.value = state.value.copy(imageConfig = state.value.imageConfig.copy(prompt = "fast prompt"))
        interactor.schedule()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(0.76, state.value.estimatedCost)

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf("slow prompt", "fast prompt"), repository.feePreviewRequests.map { it.prompt })
        assertEquals(0.76, state.value.estimatedCost)
        assertFalse(state.value.feePreviewLoading)
        assertNull(state.value.feePreviewError)
    }

    @Test
    fun `required service field skips fee preview request without exposing generation block message`() = runTest {
        val repository = QuickCreateScreenModelTest.FakeQuickCreateRepository()
        val state = MutableStateFlow(
            QuickCreateUiState(
                selectedImageServiceModel = serviceModel(fields = listOf(requiredOptionField())),
                imageConfig = ImageConfig(prompt = "green icon"),
                feePreviewLoading = true,
                feePreviewError = "旧的计费错误",
                estimatedCost = 9.99,
            )
        )
        val interactor = createInteractor(repository = repository, state = state, scope = this)

        interactor.schedule()
        runCurrent()

        assertEquals(0, repository.feePreviewRequests.size)
        assertEquals(state.value.imageConfig.estimatedCost, state.value.estimatedCost)
        assertFalse(state.value.feePreviewLoading)
        assertNull(state.value.feePreviewError)
    }

    private fun createInteractor(
        repository: QuickCreateScreenModelTest.FakeQuickCreateRepository,
        state: MutableStateFlow<QuickCreateUiState>,
        scope: TestScope,
    ): QuickCreateFeePreviewInteractor =
        QuickCreateFeePreviewInteractor(
            quickCreateRepository = repository,
            generationRequestFactory = QuickCreateGenerationRequestFactory(),
            scope = scope,
            uiState = state,
        )

    private fun feePreview(requiredCashAmount: Double): QuickCreationFeePreview =
        QuickCreationFeePreview(
            passed = true,
            free = false,
            settlementMode = "cash_only",
            requiredCashAmount = requiredCashAmount,
            userCashBalance = 156.376,
            cashCurrency = "CNY",
        )

    private fun serviceModel(fields: List<QuickCreationServiceField>): QuickCreationServiceModel =
        QuickCreationServiceModel(
            categoryId = "IMAGE",
            groupName = "图片生成",
            bindingId = "binding-1",
            skuId = "sku-1",
            name = "测试模型",
            description = null,
            fields = fields,
        )

    private fun requiredOptionField(): QuickCreationServiceField =
        QuickCreationServiceField(
            fieldKey = "stylePreset",
            paramKey = "stylePreset",
            fieldType = "LIST",
            required = true,
            defaultValue = null,
            options = listOf(
                QuickCreationServiceFieldOption(label = "Realistic", value = "realistic"),
            ),
            inputExtra = QuickCreationServiceFieldExtra(title = "Style preset"),
        )
}
