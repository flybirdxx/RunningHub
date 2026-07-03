package com.runninghub.app.ui.designsystem

import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.chips.RhChipDefaults
import com.runninghub.app.ui.designsystem.components.chips.RhChipStyle
import com.runninghub.app.ui.designsystem.components.navigation.RhTopBarDefaults
import com.runninghub.app.ui.designsystem.components.segmented.RhSegmentedControlDefaults
import kotlin.test.Test
import kotlin.test.assertEquals

class RhCoreComponentsContractTest {
    @Test
    fun `chip styles are semantic`() {
        assertEquals("neutral", RhChipStyle.Neutral.tokenName)
        assertEquals("brand", RhChipStyle.Brand.tokenName)
        assertEquals(32.dp, RhChipDefaults.height)
    }

    @Test
    fun `segmented control keeps compact height`() {
        assertEquals(32.dp, RhSegmentedControlDefaults.height)
    }

    @Test
    fun `top bar keeps standard height`() {
        assertEquals(56.dp, RhTopBarDefaults.height)
    }
}
