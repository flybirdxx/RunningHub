package com.runninghub.app.ui.feature.quickcreate.presentation.editor.params

import androidx.compose.ui.unit.dp
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceUploadMediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickCreateParamsSheetContentContractTest {
    @Test
    fun `parameter field chrome stays compact for small screens`() {
        assertTrue(QuickCreateParamsSectionGap <= 10.dp)
        assertTrue(QuickCreateParamFieldMinHeight <= 72.dp)
        assertTrue(QuickCreateParamFieldPadding <= 10.dp)
        assertTrue(QuickCreateParamFieldGap <= 8.dp)
        assertTrue(QuickCreateParamsSheetMaxContentHeight >= 640.dp)
        assertTrue(QuickCreateParamsSheetBottomSlack >= 72.dp)
    }

    @Test
    fun `missing upload media type does not infer image picker`() {
        assertEquals(
            QuickCreateMediaType.IMAGE,
            QuickCreationServiceUploadMediaType.IMAGE.toQuickCreateMediaType(),
        )
        assertEquals(
            QuickCreateMediaType.VIDEO,
            QuickCreationServiceUploadMediaType.VIDEO.toQuickCreateMediaType(),
        )
        assertEquals(
            QuickCreateMediaType.AUDIO,
            QuickCreationServiceUploadMediaType.AUDIO.toQuickCreateMediaType(),
        )
        assertNull((null as QuickCreationServiceUploadMediaType?).toQuickCreateMediaType())
    }
}
