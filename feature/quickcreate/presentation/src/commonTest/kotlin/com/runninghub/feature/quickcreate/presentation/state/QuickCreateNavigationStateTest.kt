package com.runninghub.feature.quickcreate.presentation.state

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateNavigationStateTest {
    @Test
    fun `tab navigation labels expose stable keys instead of localized copy`() {
        assertEquals(QuickCreateNavigationLabel.ImageTab, QuickCreateTab.IMAGE.navigationLabel)
        assertEquals(QuickCreateNavigationLabel.VideoTab, QuickCreateTab.VIDEO.navigationLabel)
    }

    @Test
    fun `mode navigation labels expose stable keys instead of localized copy`() {
        assertEquals(QuickCreateNavigationLabel.CreationMode, QuickCreateMode.CREATION.navigationLabel)
    }
}
