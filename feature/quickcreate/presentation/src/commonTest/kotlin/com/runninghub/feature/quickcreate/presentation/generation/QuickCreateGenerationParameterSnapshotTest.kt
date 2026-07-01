package com.runninghub.feature.quickcreate.presentation.generation

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateGenerationParameterSnapshotTest {
    @Test
    fun `snapshot uses effective service params instead of local fallback config`() {
        val serviceModel = QuickCreationServiceModel(
            categoryId = "IMAGE",
            groupName = null,
            bindingId = "image-binding",
            skuId = "image-sku",
            name = "全能图片 G-2.0",
            description = null,
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "aspectRatio",
                    paramKey = "aspectRatio",
                    fieldType = "select",
                    required = true,
                    defaultValue = "16:9",
                    options = emptyList(),
                ),
                QuickCreationServiceField(
                    fieldKey = "resolution",
                    paramKey = "resolution",
                    fieldType = "select",
                    required = true,
                    defaultValue = "2k",
                    options = emptyList(),
                ),
            ),
        )
        val uiState = QuickCreateUiState(
            currentTab = QuickCreateTab.IMAGE,
            imageConfig = ImageConfig(
                aspectRatio = ImageAspectRatio.RATIO_1_1,
                resolution = ImageResolution.RES_1K,
            ),
            selectedImageServiceModel = serviceModel,
        )

        val snapshot = uiState.quickCreateGenerationParameterSnapshot()

        assertEquals("16:9", snapshot.aspectRatio)
        assertEquals("2K", snapshot.resolution)
    }

    @Test
    fun `snapshot does not invent ratio or resolution when service fields are absent`() {
        val uiState = QuickCreateUiState(
            currentTab = QuickCreateTab.IMAGE,
            imageConfig = ImageConfig(
                aspectRatio = ImageAspectRatio.RATIO_1_1,
                resolution = ImageResolution.RES_1K,
            ),
            selectedImageServiceModel = QuickCreationServiceModel(
                categoryId = "IMAGE",
                groupName = null,
                bindingId = "image-binding",
                skuId = "image-sku",
                name = "No params model",
                description = null,
                fields = emptyList(),
            ),
        )

        val snapshot = uiState.quickCreateGenerationParameterSnapshot()

        assertEquals("", snapshot.aspectRatio)
        assertEquals("", snapshot.resolution)
    }
}
