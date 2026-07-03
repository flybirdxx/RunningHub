package com.runninghub.app.ui.feature.quickcreate

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateEmptyGuideTest {
    @Test
    fun `samples are trimmed filtered and capped at three`() {
        val samples = quickCreateEmptySamples(listOf(" 赛博朋克 ", "", "  ", "水彩猫", "海岸", "多余的第四条"))
        assertEquals(listOf("赛博朋克", "水彩猫", "海岸"), samples)
    }
}
