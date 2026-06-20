package com.runninghub.feature.quickcreate.presentation.inspiration

import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTag
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplate
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplateDetail
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplatePage
import com.runninghub.feature.quickcreate.domain.QuickCreationInspirationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationUploadMediaKind
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateMode
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateInspirationStateHolderTest {

    @Test
    fun `load inspiration writes tags and first template page`() = runTest {
        val repository = FakeInspirationRepository()
        val state = MutableStateFlow(QuickCreateUiState(error = "old error"))
        val holder = createHolder(repository, state, this)

        holder.loadInspiration()
        runCurrent()

        assertEquals(listOf(1 to 2), repository.requestedTemplatePages)
        assertFalse(state.value.inspirationLoading)
        assertEquals(listOf("热门", "新品"), state.value.inspirationTags.map { it.label })
        assertEquals(listOf(true, false), state.value.inspirationTags.map { it.selected })
        assertEquals(listOf("tpl-1", "tpl-2"), state.value.inspirationTemplates.map { it.id })
        assertEquals(1, state.value.inspirationTemplatesPage)
        assertEquals(true, state.value.inspirationTemplatesHasMore)
        assertEquals(null, state.value.error)
    }

    @Test
    fun `load more templates enters loading synchronously and ignores duplicate trigger`() = runTest {
        val repository = FakeInspirationRepository().apply {
            templatePages = mapOf(
                2 to templatePage(
                    page = 2,
                    hasNext = false,
                    items = listOf(
                        inspirationTemplate("tpl-2", title = "duplicate"),
                        inspirationTemplate("tpl-3", title = "new"),
                    ),
                ),
            )
        }
        val state = MutableStateFlow(
            QuickCreateUiState(
                inspirationTemplates = listOf(inspirationTemplate("tpl-1"), inspirationTemplate("tpl-2"))
                    .map { it.toQuickCreateInspirationTemplateUi() },
                inspirationTemplatesPage = 1,
                inspirationTemplatesHasMore = true,
            )
        )
        val holder = createHolder(repository, state, this)

        holder.loadMoreTemplates()
        holder.loadMoreTemplates()

        assertEquals(emptyList(), repository.requestedTemplatePages)
        assertEquals(true, state.value.inspirationTemplatesLoadingMore)

        runCurrent()

        assertEquals(listOf(2 to 2), repository.requestedTemplatePages)
        assertEquals(listOf("tpl-1", "tpl-2", "tpl-3"), state.value.inspirationTemplates.map { it.id })
        assertEquals(2, state.value.inspirationTemplatesPage)
        assertFalse(state.value.inspirationTemplatesHasMore)
        assertFalse(state.value.inspirationTemplatesLoadingMore)
    }

    @Test
    fun `apply image template writes editor state and field media references`() = runTest {
        var templateAppliedCount = 0
        val imageModel = serviceModel(
            bindingId = "image-binding",
            skuId = "image-sku",
            fields = listOf(
                uploadField(fieldKey = "imageUrls", paramKey = "imageUrls"),
            ),
        )
        val repository = FakeInspirationRepository().apply {
            templateDetail = inspirationTemplateDetail(
                templateId = "tpl-image",
                categoryId = "IMAGE",
                bindingId = "image-binding",
                skuId = "image-sku",
                prompt = "template prompt",
                params = mapOf("aspectRatio" to "3:4"),
                listParams = mapOf("imageUrls" to listOf("https://example.com/template.png")),
            )
        }
        val state = MutableStateFlow(
            QuickCreateUiState(
                currentMode = QuickCreateMode.INSPIRATION,
                serviceImageModels = listOf(imageModel),
                selectedImageServiceModel = imageModel,
            )
        )
        val holder = createHolder(repository, state, this, onTemplateApplied = { templateAppliedCount++ })

        holder.applyTemplate("tpl-image")
        runCurrent()

        val mediaReference = state.value.imageConfig.mediaReferences.single()
        assertEquals(listOf("tpl-image"), repository.requestedTemplateDetailIds)
        assertEquals(1, templateAppliedCount)
        assertEquals(QuickCreateMode.CREATION, state.value.currentMode)
        assertEquals(QuickCreateTab.IMAGE, state.value.currentTab)
        assertEquals("template prompt", state.value.imageConfig.prompt)
        assertEquals(ImageAspectRatio.RATIO_3_4, state.value.imageConfig.aspectRatio)
        assertEquals("image-binding", state.value.selectedImageServiceModel?.bindingId)
        assertEquals("imageUrls", mediaReference.fieldParamKey)
        assertEquals(QuickCreateMediaType.IMAGE, mediaReference.type)
        assertEquals(UploadStatus.DONE, mediaReference.uploadStatus)
        assertEquals("https://example.com/template.png", mediaReference.remoteUrl)
    }

    @Test
    fun `apply template failure keeps editor state and does not invoke callback`() = runTest {
        var templateAppliedCount = 0
        val repository = FakeInspirationRepository().apply {
            templateDetailResult = Result.failure(IllegalStateException("模板不存在"))
        }
        val state = MutableStateFlow(
            QuickCreateUiState(
                currentMode = QuickCreateMode.CREATION,
                currentTab = QuickCreateTab.IMAGE,
            )
        )
        val holder = createHolder(repository, state, this, onTemplateApplied = { templateAppliedCount++ })

        holder.applyTemplate("missing-template")
        runCurrent()

        assertEquals(listOf("missing-template"), repository.requestedTemplateDetailIds)
        assertEquals(0, templateAppliedCount)
        assertEquals(QuickCreateMode.CREATION, state.value.currentMode)
        assertEquals(QuickCreateTab.IMAGE, state.value.currentTab)
        assertFalse(state.value.inspirationLoading)
        assertEquals("模板不存在", state.value.error)
    }

    private fun createHolder(
        repository: FakeInspirationRepository,
        state: MutableStateFlow<QuickCreateUiState>,
        scope: TestScope,
        onTemplateApplied: () -> Unit = {},
        pageSize: Int = 2,
    ): QuickCreateInspirationStateHolder =
        QuickCreateInspirationStateHolder(
            inspirationRepository = repository,
            scope = scope,
            uiState = state,
            onTemplateApplied = onTemplateApplied,
            pageSize = pageSize,
        )

    /**
     * 灵感 StateHolder 的最小仓库替身。
     *
     * 测试只覆盖标签、模板分页和模板详情，因此 fake 不混入生成、计费、上传或项目能力，
     * 确保 feature presentation 层可以独立验证模板应用映射。
     */
    private class FakeInspirationRepository : QuickCreationInspirationRepository {
        var tagsResult: Result<List<QuickCreateInspirationTag>> = Result.success(
            listOf(
                QuickCreateInspirationTag(id = "hot", name = "热门"),
                QuickCreateInspirationTag(id = "new", name = "新品"),
            )
        )
        var templatePages: Map<Int, QuickCreateInspirationTemplatePage>? = null
        var templateDetail = inspirationTemplateDetail()
        var templateDetailResult: Result<QuickCreateInspirationTemplateDetail>? = null
        val requestedTemplatePages = mutableListOf<Pair<Int, Int>>()
        val requestedTemplateDetailIds = mutableListOf<String>()

        /**
         * 返回灵感标签；失败结果用于验证标签和模板任一失败时的降级策略。
         */
        override suspend fun getInspirationTags(): Result<List<QuickCreateInspirationTag>> = tagsResult

        /**
         * 返回模板分页，并记录页码和 page size 以验证 StateHolder 的分页入口。
         */
        override suspend fun getInspirationTemplates(
            page: Int,
            size: Int,
            tagId: String?,
        ): Result<QuickCreateInspirationTemplatePage> {
            requestedTemplatePages += page to size
            return Result.success(templatePages?.get(page) ?: defaultTemplatePage(page = page, size = size))
        }

        /**
         * 返回模板详情，并记录 templateId 以验证空 ID 拦截之外的正常请求路径。
         */
        override suspend fun getInspirationTemplateDetail(
            templateId: String,
        ): Result<QuickCreateInspirationTemplateDetail> {
            requestedTemplateDetailIds += templateId
            return templateDetailResult ?: Result.success(templateDetail.copy(templateId = templateId))
        }
    }
}

private fun defaultTemplatePage(page: Int, size: Int): QuickCreateInspirationTemplatePage =
    templatePage(
        page = page,
        size = size,
        hasNext = page == 1,
        items = listOf(
            inspirationTemplate("tpl-1", title = "Template 1"),
            inspirationTemplate("tpl-2", title = "Template 2"),
        ),
    )

private fun templatePage(
    page: Int = 1,
    size: Int = 2,
    hasNext: Boolean = true,
    items: List<QuickCreateInspirationTemplate>,
): QuickCreateInspirationTemplatePage =
    QuickCreateInspirationTemplatePage(
        page = page,
        size = size,
        total = if (hasNext) items.size + size else items.size,
        pages = if (hasNext) page + 1 else page,
        hasNext = hasNext,
        hasPrevious = page > 1,
        items = items,
    )

private fun inspirationTemplate(
    templateId: String,
    title: String = templateId,
    categoryId: String? = "IMAGE",
): QuickCreateInspirationTemplate =
    QuickCreateInspirationTemplate(
        templateId = templateId,
        title = title,
        categoryId = categoryId,
        coverUrl = "https://example.com/$templateId.png",
        videoUrl = null,
        tagHot = false,
        tagNew = false,
    )

private fun inspirationTemplateDetail(
    templateId: String = "tpl-image",
    categoryId: String? = "IMAGE",
    bindingId: String? = "image-binding",
    skuId: String? = "image-sku",
    prompt: String? = "template prompt",
    params: Map<String, String> = emptyMap(),
    listParams: Map<String, List<String>> = emptyMap(),
): QuickCreateInspirationTemplateDetail =
    QuickCreateInspirationTemplateDetail(
        templateId = templateId,
        title = "Template detail",
        categoryId = categoryId,
        bindingId = bindingId,
        skuId = skuId,
        prompt = prompt,
        params = params,
        listParams = listParams,
        coverUrl = "https://example.com/$templateId-cover.png",
        videoUrl = null,
    )

private fun serviceModel(
    bindingId: String,
    skuId: String,
    fields: List<QuickCreationServiceField> = emptyList(),
): QuickCreationServiceModel =
    QuickCreationServiceModel(
        categoryId = "IMAGE",
        groupName = "Image",
        bindingId = bindingId,
        skuId = skuId,
        name = "Image Model",
        description = null,
        fields = fields,
    )

private fun uploadField(
    fieldKey: String,
    paramKey: String,
    mediaKind: QuickCreationUploadMediaKind = QuickCreationUploadMediaKind.IMAGE,
): QuickCreationServiceField =
    QuickCreationServiceField(
        fieldKey = fieldKey,
        paramKey = paramKey,
        fieldType = "upload",
        required = false,
        defaultValue = null,
        options = emptyList(),
        uploadMediaKind = mediaKind,
    )
