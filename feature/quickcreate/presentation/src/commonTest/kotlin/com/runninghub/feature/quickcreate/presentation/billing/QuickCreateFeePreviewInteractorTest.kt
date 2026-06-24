package com.runninghub.feature.quickcreate.presentation.billing

import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldExtra
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldOption
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationServicePricing
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationRequestFactory
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
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
        val repository = FakeFeePreviewRepository().apply {
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
    fun `service model price summary is used as local image fallback before fee preview is available`() = runTest {
        val repository = FakeFeePreviewRepository()
        val state = MutableStateFlow(
            QuickCreateUiState(
                selectedImageServiceModel = serviceModel(
                    fields = emptyList(),
                    pricing = QuickCreationServicePricing(priceSummaryRaw = "¥0.0600/次"),
                ),
                imageConfig = ImageConfig(prompt = ""),
            )
        )
        val interactor = createInteractor(repository = repository, state = state, scope = this)

        interactor.schedule()
        runCurrent()

        assertEquals(0, repository.feePreviewRequests.size)
        assertEquals(0.06, state.value.estimatedCost)
        assertFalse(state.value.feePreviewLoading)
        assertNull(state.value.feePreviewError)
    }

    @Test
    fun `service model price summary remains display price after successful fee preview`() = runTest {
        val repository = FakeFeePreviewRepository().apply {
            imageFeePreviewResult = Result.success(feePreview(requiredCashAmount = 0.76))
        }
        val state = MutableStateFlow(
            QuickCreateUiState(
                selectedImageServiceModel = serviceModel(
                    fields = emptyList(),
                    pricing = QuickCreationServicePricing(priceSummaryRaw = "¥0.0600/次"),
                ),
                imageConfig = ImageConfig(prompt = "cat"),
            )
        )
        val interactor = createInteractor(repository = repository, state = state, scope = this)

        interactor.schedule()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(listOf("cat"), repository.feePreviewRequests.map { it.prompt })
        assertEquals(0.06, state.value.estimatedCost)
        assertFalse(state.value.feePreviewLoading)
        assertNull(state.value.feePreviewError)
    }

    @Test
    fun `required service field skips fee preview request without exposing generation block message`() = runTest {
        val repository = FakeFeePreviewRepository()
        val state = MutableStateFlow(
            QuickCreateUiState(
                selectedImageServiceModel = serviceModel(fields = listOf(requiredOptionField())),
                imageConfig = ImageConfig(prompt = "green icon"),
                feePreviewLoading = true,
                feePreviewError = QuickCreatePresentationError.FeePreviewFailed.asQuickCreateUiMessage(),
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
        repository: FakeFeePreviewRepository,
        state: MutableStateFlow<QuickCreateUiState>,
        scope: TestScope,
    ): QuickCreateFeePreviewInteractor =
        QuickCreateFeePreviewInteractor(
            feePreviewRepository = repository,
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

    private fun serviceModel(
        fields: List<QuickCreationServiceField>,
        pricing: QuickCreationServicePricing? = null,
    ): QuickCreationServiceModel =
        QuickCreationServiceModel(
            categoryId = "IMAGE",
            groupName = "图片生成",
            bindingId = "binding-1",
            skuId = "sku-1",
            name = "测试模型",
            description = null,
            fields = fields,
            pricing = pricing,
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

    /**
     * 计费预览 Interactor 的最小仓库替身。
     *
     * 测试只关心请求防抖、旧响应隔离和必填字段拦截，因此 fake 只实现计费预览仓库接口，
     * 避免把 feature presentation 单测重新耦合到 composeApp 的 ScreenModel 综合测试替身。
     */
    private class FakeFeePreviewRepository : QuickCreationFeePreviewRepository {
        val feePreviewRequests = mutableListOf<ImageGenerationRequest>()
        var imageFeePreviewHandler: (suspend (ImageGenerationRequest) -> Result<QuickCreationFeePreview>)? = null
        var imageFeePreviewResult: Result<QuickCreationFeePreview> = Result.success(
            QuickCreationFeePreview(
                passed = true,
                free = false,
                settlementMode = "cash_only",
                requiredCashAmount = 0.76,
                userCashBalance = 156.376,
                cashCurrency = "CNY",
            )
        )

        /**
         * 记录图片计费请求，并按测试场景返回延迟或即时结果。
         */
        override suspend fun previewImageQuickCreationFee(
            request: ImageGenerationRequest,
        ): Result<QuickCreationFeePreview> {
            feePreviewRequests += request
            imageFeePreviewHandler?.let { handler -> return handler(request) }
            return imageFeePreviewResult
        }

        /**
         * 当前测试只覆盖图片计费分支；保留视频入口可以确保 fake 完整满足领域仓库契约。
         */
        override suspend fun previewVideoQuickCreationFee(
            request: VideoGenerationRequest,
        ): Result<QuickCreationFeePreview> =
            imageFeePreviewResult
    }
}
