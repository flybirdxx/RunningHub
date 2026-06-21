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
        assertTrue(tools.all { it.title.isNotBlank() })
        assertTrue(tools.all { it.description.isNotBlank() })
        assertTrue(tools.all { it.iconName.isNotBlank() })
        assertTrue(tools.all { it.route.isNotBlank() })
    }

    @Test
    fun `workflow tool keeps plaza route for future navigation`() {
        val stateHolder = CommunityStateHolder()

        val workflow = stateHolder.uiState.value.tools.single { it.id == "workflow" }

        assertEquals("workflow_plaza", workflow.route)
        assertEquals("hub", workflow.iconName)
    }
}
