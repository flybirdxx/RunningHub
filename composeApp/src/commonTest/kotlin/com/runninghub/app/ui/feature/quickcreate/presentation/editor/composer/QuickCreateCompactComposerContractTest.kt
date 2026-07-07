package com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer

import androidx.compose.ui.unit.dp
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingAmount
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingUnit
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateSendButtonLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun `compact add media follows active creation tab`() {
        assertEquals(
            CompactMediaAddAction.Image,
            compactMediaAddAction(isImage = true),
        )
        assertEquals(
            CompactMediaAddAction.Video,
            compactMediaAddAction(isImage = false),
        )
    }

    @Test
    fun `priced send labels keep generate intent visible`() {
        listOf(
            QuickCreateSendButtonLabel.Confirming,
            QuickCreateSendButtonLabel.Pending,
            QuickCreateSendButtonLabel.Amount(
                QuickCreateBillingAmount("2.00", QuickCreateBillingUnit.CnyCash),
            ),
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

    @Test
    fun `media strip keeps compact mobile height`() {
        assertTrue(CompactMediaStripHeight <= 96.dp)
        assertTrue(CompactMediaSlotSize <= 88.dp)
    }
}
