package com.runninghub.feature.quickcreate.presentation.modelcatalog

import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreateModelSelectionRepository
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
        assertEquals("全能图片 G-2.0", state.value.selectedImageServiceModelUi?.compactName)
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
        val selectionRepository = FakeModelSelectionRepository()
        var feePreviewRequests = 0
        val interactor = createInteractor(
            repository = repository,
            state = state,
            scope = this,
            selectionRepository = selectionRepository,
        ) {
            feePreviewRequests += 1
        }

        interactor.loadServiceModels()
        runCurrent()
        interactor.updateImageServiceModel("binding-key|sku-key")
        runCurrent()

        assertEquals("binding-key", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("binding-key|sku-key", selectionRepository.imageIdentityKey)
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
    fun `load service models selects first catalog image model when there is no saved selection`() = runTest {
        val repository = FakeModelCatalogRepository().apply {
            imageModels = listOf(
                serviceModel(
                    groupName = "全能图片 PRO",
                    bindingId = "pro-binding",
                    skuId = "pro-sku",
                    name = "全能图片 PRO-文生图-官方版",
                    fields = listOf(serviceField("style", "pro")),
                ),
                serviceModel(
                    bindingId = "g2-binding",
                    skuId = "g2-sku",
                    name = "全能图片G-2.0-官方版",
                    fields = listOf(serviceField("style", "photoreal")),
                ),
            )
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val interactor = createInteractor(repository, state, this)

        interactor.loadServiceModels()
        runCurrent()

        assertEquals("pro-binding", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("pro-binding|pro-sku", state.value.selectedImageServiceModelUi?.identityKey)
        assertEquals(listOf(true, false), state.value.serviceImageModelItems.map { it.selected })
    }

    @Test
    fun `load service models restores saved image selection before falling back to first catalog item`() = runTest {
        val repository = FakeModelCatalogRepository().apply {
            imageModels = listOf(
                serviceModel(
                    groupName = "全能图片 PRO",
                    bindingId = "pro-binding",
                    skuId = "pro-sku",
                    name = "全能图片 PRO-文生图-官方版",
                    fields = listOf(serviceField("style", "pro")),
                ),
                serviceModel(
                    bindingId = "g2-binding",
                    skuId = "g2-sku",
                    name = "全能图片G-2.0-官方版",
                    fields = listOf(serviceField("style", "photoreal")),
                ),
            )
        }
        val selectionRepository = FakeModelSelectionRepository(
            imageIdentityKey = "g2-binding|g2-sku",
        )
        val state = MutableStateFlow(QuickCreateUiState())
        val interactor = createInteractor(repository, state, this, selectionRepository = selectionRepository)

        interactor.loadServiceModels()
        runCurrent()

        assertEquals("g2-binding", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("g2-binding|g2-sku", state.value.selectedImageServiceModelUi?.identityKey)
        assertEquals(listOf(false, true), state.value.serviceImageModelItems.map { it.selected })
    }

    @Test
    fun `refresh restores saved image selection even when cached list selected first item`() = runTest {
        val repository = FakeModelCatalogRepository().apply {
            hasCachedModels = true
            imageModels = listOf(
                serviceModel(
                    groupName = "全能图片 PRO",
                    bindingId = "pro-cache-binding",
                    skuId = "pro-cache-sku",
                    name = "全能图片 PRO-文生图-官方版",
                    fields = listOf(serviceField("style", "pro-cache")),
                )
            )
            refreshedImageModels = listOf(
                serviceModel(
                    groupName = "全能图片 PRO",
                    bindingId = "pro-live-binding",
                    skuId = "pro-live-sku",
                    name = "全能图片 PRO-文生图-官方版",
                    fields = listOf(serviceField("style", "pro-live")),
                ),
                serviceModel(
                    bindingId = "g2-live-binding",
                    skuId = "g2-live-sku",
                    name = "全能图片G-2.0-官方版",
                    fields = listOf(serviceField("style", "photoreal")),
                ),
            )
        }
        val selectionRepository = FakeModelSelectionRepository(
            imageIdentityKey = "g2-live-binding|g2-live-sku",
        )
        val state = MutableStateFlow(QuickCreateUiState())
        val interactor = createInteractor(repository, state, this, selectionRepository = selectionRepository)

        interactor.loadServiceModels()
        runCurrent()

        assertEquals("g2-live-binding", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("g2-live-binding|g2-live-sku", state.value.selectedImageServiceModelUi?.identityKey)
        assertEquals(mapOf("style" to "photoreal"), state.value.imageServiceParams)
        assertEquals(listOf(false, true), state.value.serviceImageModelItems.map { it.selected })
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
    fun `reload keeps selected image model when refreshed identity changes but signature matches`() = runTest {
        val repository = FakeModelCatalogRepository().apply {
            imageModels = listOf(
                serviceModel(
                    bindingId = "g2-binding",
                    skuId = "g2-sku",
                    name = "全能图片G-2.0-官方版",
                    fields = listOf(serviceField("style", "photoreal")),
                ),
                serviceModel(
                    groupName = "全能图片 PRO",
                    bindingId = "pro-cache-binding",
                    skuId = "pro-cache-sku",
                    name = "全能图片PRO-文生图-官方版",
                    fields = listOf(serviceField("style", "cinematic")),
                ),
            )
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val interactor = createInteractor(repository, state, this)

        interactor.loadServiceModels()
        runCurrent()
        interactor.updateImageServiceModel("pro-cache-binding|pro-cache-sku")
        interactor.updateImageServiceParam("style", "custom")
        repository.imageModels = listOf(
            serviceModel(
                bindingId = "g2-binding",
                skuId = "g2-sku",
                name = "全能图片G-2.0-官方版",
                fields = listOf(serviceField("style", "photoreal")),
            ),
            serviceModel(
                groupName = "全能图片 PRO",
                bindingId = "pro-live-binding",
                skuId = "pro-live-sku",
                name = "全能图片 PRO-文生图-官方版",
                fields = listOf(serviceField("style", "cinematic")),
            ),
        )
        interactor.loadServiceModels()
        runCurrent()

        assertEquals("pro-live-binding", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("pro-live-binding|pro-live-sku", state.value.selectedImageServiceModelUi?.identityKey)
        assertEquals("custom", state.value.imageServiceParams["style"])
        assertEquals(listOf(false, true), state.value.serviceImageModelItems.map { it.selected })
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
        selectionRepository: FakeModelSelectionRepository = FakeModelSelectionRepository(),
        onFeePreviewRequired: () -> Unit = {},
    ): QuickCreateModelCatalogInteractor =
        QuickCreateModelCatalogInteractor(
            modelCatalogRepository = repository,
            modelSelectionRepository = selectionRepository,
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
        var hasCachedModels: Boolean = false
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
        var refreshedImageModels: List<QuickCreationServiceModel>? = null
        var refreshedVideoModels: List<QuickCreationServiceModel>? = null

        /**
         * 按业务类别返回当前测试设置的模型目录。
         */
        override suspend fun getModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>> =
            when (kind) {
                QuickCreationServiceKind.IMAGE -> Result.success(imageModels)
                QuickCreationServiceKind.VIDEO -> Result.success(videoModels)
            }

        /**
         * 测试是否存在本地缓存。
         */
        override suspend fun hasCachedModels(kind: QuickCreationServiceKind): Boolean =
            hasCachedModels

        /**
         * 返回测试设置的远端刷新目录；未设置时沿用当前目录。
         */
        override suspend fun refreshModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>> =
            when (kind) {
                QuickCreationServiceKind.IMAGE -> Result.success(refreshedImageModels ?: imageModels)
                QuickCreationServiceKind.VIDEO -> Result.success(refreshedVideoModels ?: videoModels)
            }
    }

    /**
     * 最近模型选择仓库替身。
     *
     * 测试只关心读取上次选择和用户主动选择后的写入，不需要覆盖底层 DataStore 行为。
     */
    private class FakeModelSelectionRepository(
        var imageIdentityKey: String? = null,
        var videoIdentityKey: String? = null,
    ) : QuickCreateModelSelectionRepository {
        override suspend fun getLastImageServiceModelIdentityKey(): String? =
            imageIdentityKey

        override suspend fun saveLastImageServiceModelIdentityKey(identityKey: String) {
            imageIdentityKey = identityKey
        }

        override suspend fun getLastVideoServiceModelIdentityKey(): String? =
            videoIdentityKey

        override suspend fun saveLastVideoServiceModelIdentityKey(identityKey: String) {
            videoIdentityKey = identityKey
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
