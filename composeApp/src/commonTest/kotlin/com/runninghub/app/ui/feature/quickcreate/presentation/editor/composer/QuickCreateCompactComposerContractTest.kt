package com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer

import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateSendButtonLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateCompactComposerContractTest {
    @Test
    fun `empty prompt uses add media as primary action`() {
        assertEquals(
            CompactPrimaryAction.AddMedia,
            compactPrimaryAction(hasPrompt = false),
        )
    }

    @Test
    fun `prompt content uses generate as primary action`() {
        assertEquals(
            CompactPrimaryAction.Generate,
            compactPrimaryAction(hasPrompt = true),
        )
    }

    @Test
    fun `priced send labels keep generate intent visible`() {
        listOf(
            QuickCreateSendButtonLabel.Confirming,
            QuickCreateSendButtonLabel.Pending,
            QuickCreateSendButtonLabel.Amount("2.00"),
        ).forEach { label ->
            assertEquals(
                CompactGenerateTextMode.GenerateWithDetail,
                compactGenerateTextMode(label),
            )
        }
    }

    @Test
    fun `free generate label keeps compact generate copy`() {
        assertEquals(
            CompactGenerateTextMode.GenerateOnly,
            compactGenerateTextMode(QuickCreateSendButtonLabel.Generate),
        )
    }
}
