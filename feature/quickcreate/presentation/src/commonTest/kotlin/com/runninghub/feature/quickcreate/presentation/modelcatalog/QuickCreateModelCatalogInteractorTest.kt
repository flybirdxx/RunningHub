package com.runninghub.feature.quickcreate.presentation.modelcatalog

import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceKind
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateModelCatalogInteractorTest {

    @Test
    fun `load service models selects first image and video models with default params`() = runTest {
        val repository = FakeModelCatalogRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        var feePreviewRequests = 0
        val interactor = createInteractor(repository, state, this) {
            feePreviewRequests += 1
        }

        interactor.loadServiceModels()
        runCurrent()

        assertFalse(state.value.serviceModelsLoading)
        assertEquals("全能图片G-2.0-官方版", state.value.selectedImageServiceModel?.name)
        assertEquals("binding-1", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("G-2.0", state.value.selectedImageServiceModelUi?.compactName)
        assertEquals(
            QuickCreateServiceModelSubtitle.GroupAndParameterCount("全能图片", 2),
            state.value.selectedImageServiceModelUi?.subtitle,
        )
        assertEquals(mapOf("style" to "photoreal", "aspectRatio" to "1:1"), state.value.imageServiceParams)
        assertEquals("video-binding-1", state.value.selectedVideoServiceModel?.bindingId)
        assertEquals(1, feePreviewRequests)
    }

    @Test
    fun `selecting image service model by identity key updates selected summary and defaults`() = runTest {
        val repository = FakeModelCatalogRepository().apply {
            imageModels = imageModels + serviceModel(
                bindingId = "binding-key",
                skuId = "sku-key",
                name = "身份键图片模型",
                fields = listOf(serviceField("style", "sketch")),
            )
        }
        val state = MutableStateFlow(QuickCreateUiState())
        var feePreviewRequests = 0
        val interactor = createInteractor(repository, state, this) {
            feePreviewRequests += 1
        }

        interactor.loadServiceModels()
        runCurrent()
        interactor.updateImageServiceModel("binding-key|sku-key")

        assertEquals("binding-key", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("binding-key|sku-key", state.value.selectedImageServiceModelUi?.identityKey)
        assertEquals(
            QuickCreateServiceModelDisplayName.ServerText("身份键图片模型"),
            state.value.selectedImageServiceModelUi?.displayName,
        )
        assertEquals(mapOf("style" to "sketch"), state.value.imageServiceParams)
        assertEquals(listOf(false, true), state.value.serviceImageModelItems.map { it.selected })
        assertEquals(2, feePreviewRequests)
    }

    @Test
    fun `selecting image service model by stale identity key keeps current selection`() = runTest {
        val repository = FakeModelCatalogRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        var feePreviewRequests = 0
        val interactor = createInteractor(repository, state, this) {
            feePreviewRequests += 1
        }

        interactor.loadServiceModels()
        runCurrent()
        interactor.updateImageServiceModel("missing|sku")

        assertEquals("binding-1", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("binding-1|sku-1", state.value.selectedImageServiceModelUi?.identityKey)
        assertEquals(mapOf("style" to "photoreal", "aspectRatio" to "1:1"), state.value.imageServiceParams)
        assertEquals(1, feePreviewRequests)
    }

    @Test
    fun `reload keeps canonical selected image model when identity still exists`() = runTest {
        val repository = FakeModelCatalogRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val interactor = createInteractor(repository, state, this)

        interactor.loadServiceModels()
        runCurrent()
        repository.imageModels = listOf(repository.imageModels.single().copy(name = "Updated image model"))
        interactor.loadServiceModels()
        runCurrent()

        assertEquals("Updated image model", state.value.selectedImageServiceModel?.name)
        assertEquals(
            QuickCreateServiceModelDisplayName.ServerText("Updated image model"),
            state.value.selectedImageServiceModelUi?.displayName,
        )
        assertTrue(state.value.serviceImageModelItems.single().selected)
        assertEquals(mapOf("style" to "photoreal", "aspectRatio" to "1:1"), state.value.imageServiceParams)
    }

    @Test
    fun `reload falls back to first image model defaults when selected identity disappears`() = runTest {
        val repository = FakeModelCatalogRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val interactor = createInteractor(repository, state, this)

        interactor.loadServiceModels()
        runCurrent()
        interactor.updateImageServiceParam("style", "custom")
        repository.imageModels = listOf(
            serviceModel(
                bindingId = "binding-new",
                skuId = "sku-new",
                name = "新默认图片模型",
                fields = listOf(serviceField("style", "line-art")),
            )
        )
        interactor.loadServiceModels()
        runCurrent()

        assertEquals("binding-new", state.value.selectedImageServiceModel?.bindingId)
        assertEquals(
            QuickCreateServiceModelDisplayName.ServerText("新默认图片模型"),
            state.value.selectedImageServiceModelUi?.displayName,
        )
        assertTrue(state.value.serviceImageModelItems.single().selected)
        assertEquals(mapOf("style" to "line-art"), state.value.imageServiceParams)
    }

    @Test
    fun `updating declared image service param triggers fee preview`() = runTest {
        val repository = FakeModelCatalogRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        var feePreviewRequests = 0
        val interactor = createInteractor(repository, state, this) {
            feePreviewRequests += 1
        }

        interactor.loadServiceModels()
        runCurrent()
        interactor.updateImageServiceParam("style", "anime")
        interactor.updateImageServiceParam("unexpected", "value")

        assertEquals("anime", state.value.imageServiceParams["style"])
        assertEquals(null, state.value.imageServiceParams["unexpected"])
        assertEquals(2, feePreviewRequests)
    }

    private fun createInteractor(
        repository: FakeModelCatalogRepository,
        state: MutableStateFlow<QuickCreateUiState>,
        scope: TestScope,
        onFeePreviewRequired: () -> Unit = {},
    ): QuickCreateModelCatalogInteractor =
        QuickCreateModelCatalogInteractor(
            modelCatalogRepository = repository,
            scope = scope,
            uiState = state,
            onFeePreviewRequired = onFeePreviewRequired,
        )

    /**
     * 模型目录 Interactor 的最小仓库替身。
     *
     * 测试只关心图片/视频目录读取、身份键匹配和默认参数回填，因此 fake 只实现目录仓库接口，
     * 避免把 feature presentation 单测耦合到 composeApp 的 ScreenModel、媒体解析或生成流程。
     */
    private class FakeModelCatalogRepository : QuickCreationModelCatalogRepository {
        var imageModels: List<QuickCreationServiceModel> = listOf(
            serviceModel(
                bindingId = "binding-1",
                skuId = "sku-1",
                name = "全能图片G-2.0-官方版",
                fields = listOf(
                    serviceField("style", "photoreal"),
                    serviceField("aspectRatio", "1:1"),
                ),
            )
        )
        var videoModels: List<QuickCreationServiceModel> = listOf(
            serviceModel(
                categoryId = "VIDEO",
                groupName = "全能视频",
                bindingId = "video-binding-1",
                skuId = "video-sku-1",
                name = "视频模型",
                fields = listOf(serviceField("motion", "cinematic")),
            )
        )

        /**
         * 按业务类别返回当前测试设置的模型目录。
         */
        override suspend fun getModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>> =
            when (kind) {
                QuickCreationServiceKind.IMAGE -> Result.success(imageModels)
                QuickCreationServiceKind.VIDEO -> Result.success(videoModels)
            }
    }
}

private fun serviceModel(
    categoryId: String = "IMAGE",
    groupName: String? = "全能图片",
    bindingId: String,
    skuId: String,
    name: String,
    fields: List<QuickCreationServiceField>,
): QuickCreationServiceModel =
    QuickCreationServiceModel(
        categoryId = categoryId,
        groupName = groupName,
        bindingId = bindingId,
        skuId = skuId,
        name = name,
        description = null,
        fields = fields,
    )

private fun serviceField(paramKey: String, defaultValue: String): QuickCreationServiceField =
    QuickCreationServiceField(
        fieldKey = paramKey,
        paramKey = paramKey,
        fieldType = "LIST",
        required = false,
        defaultValue = defaultValue,
        options = emptyList(),
    )
