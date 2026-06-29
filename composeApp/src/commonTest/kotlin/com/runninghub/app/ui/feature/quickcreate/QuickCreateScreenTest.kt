package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import kotlin.test.Test
import kotlin.test.assertEquals
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_empty_starter_portrait_title
import runninghub.composeapp.generated.resources.quick_create_empty_video_starter_cinematic_title

class QuickCreateScreenTest {

    @Test
    fun `empty workbench uses image prompt starters on image tab`() {
        val starters = quickCreatePromptStarters(QuickCreateTab.IMAGE)

        assertEquals(Res.string.quick_create_empty_starter_portrait_title, starters.first().title)
    }

    @Test
    fun `empty workbench uses video prompt starters on video tab`() {
        val starters = quickCreatePromptStarters(QuickCreateTab.VIDEO)

        assertEquals(Res.string.quick_create_empty_video_starter_cinematic_title, starters.first().title)
    }
}
