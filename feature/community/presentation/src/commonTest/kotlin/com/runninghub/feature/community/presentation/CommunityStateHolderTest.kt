package com.runninghub.feature.community.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommunityStateHolderTest {
    @Test
    fun `initial state exposes default tools in stable navigation order`() {
        val stateHolder = CommunityStateHolder()

        val state = stateHolder.uiState.value

        assertFalse(state.isLoading)
        assertEquals(
            listOf(
                "audio_gen",
                "steganography",
                "ui_inspector",
                "color_extract",
                "smart_crop",
                "workflow",
            ),
            state.tools.map { it.id },
        )
    }

    @Test
    fun `default tools use unique ids and non blank navigation metadata`() {
        val stateHolder = CommunityStateHolder()

        val tools = stateHolder.uiState.value.tools

        assertEquals(tools.map { it.id }.toSet().size, tools.size)
        assertTrue(tools.all { it.titleKey.isNotBlank() })
        assertTrue(tools.all { it.descriptionKey.isNotBlank() })
        assertTrue(tools.all { it.iconName.isNotBlank() })
        assertTrue(tools.all { it.route.isNotBlank() })
    }

    @Test
    fun `default tools expose stable text keys instead of localized copy`() {
        val stateHolder = CommunityStateHolder()

        val toolKeys = stateHolder.uiState.value.tools.associate {
            it.id to (it.titleKey to it.descriptionKey)
        }

        assertEquals("audio_gen.title", toolKeys.getValue("audio_gen").first)
        assertEquals("audio_gen.description", toolKeys.getValue("audio_gen").second)
        assertEquals("workflow.title", toolKeys.getValue("workflow").first)
        assertEquals("workflow.description", toolKeys.getValue("workflow").second)
    }

    @Test
    fun `workflow tool keeps plaza route for future navigation`() {
        val stateHolder = CommunityStateHolder()

        val workflow = stateHolder.uiState.value.tools.single { it.id == "workflow" }

        assertEquals("workflow_plaza", workflow.route)
        assertEquals("hub", workflow.iconName)
    }
}
