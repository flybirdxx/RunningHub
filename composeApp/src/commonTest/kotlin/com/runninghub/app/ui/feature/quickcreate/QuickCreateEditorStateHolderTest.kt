package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateEditorStateHolder

import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.draft.QuickCreateDraftRestore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig

class QuickCreateEditorStateHolderTest {

    @Test
    fun `apply draft restore updates editor state without triggering side effect callbacks`() {
        var feePreviewRequests = 0
        var draftSaveRequests = 0
        var inspirationRequests = 0
        val uiState = MutableStateFlow(
            QuickCreateUiState(
                currentTab = QuickCreateTab.IMAGE,
                imageConfig = ImageConfig(prompt = "old image prompt"),
                videoConfig = VideoConfig(prompt = "old video prompt"),
            )
        )
        val stateHolder = QuickCreateEditorStateHolder(
            uiState = uiState,
            onFeePreviewRequired = { feePreviewRequests++ },
            onDraftChanged = { draftSaveRequests++ },
            onInspirationRequired = { inspirationRequests++ },
        )

        // 草稿恢复只负责同步编辑区状态；计费刷新和草稿清理由 Coordinator 在恢复流程外层编排。
        stateHolder.applyDraftRestore(
            QuickCreateDraftRestore(
                tab = QuickCreateTab.VIDEO,
                imagePrompt = "restored image prompt",
                videoPrompt = "restored video prompt",
            )
        )

        assertEquals(QuickCreateTab.VIDEO, uiState.value.currentTab)
        assertEquals("restored image prompt", uiState.value.imageConfig.prompt)
        assertEquals("restored video prompt", uiState.value.videoConfig.prompt)
        assertEquals(0, feePreviewRequests)
        assertEquals(0, draftSaveRequests)
        assertEquals(0, inspirationRequests)
    }
}
