package com.runninghub.app.ui.copy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RedesignStateCopyMatrixContractTest {
    @Test
    fun `matrix covers every RM14 roadmap state`() {
        assertEquals(
            RedesignStateCopyId.entries.toSet(),
            RedesignStateCopyMatrix.all.map { it.id }.toSet(),
        )
        assertEquals(
            RedesignStateCopyId.entries.size,
            RedesignStateCopyMatrix.all.map { it.id }.distinct().size,
        )
    }

    @Test
    fun `copy specs keep required resources and blocking semantics explicit`() {
        val bottomSheetIds = RedesignStateCopyMatrix.all
            .filter { it.surface == RedesignStateCopySurface.BottomSheet }
            .map { it.id }
            .toSet()
        val blockingIds = RedesignStateCopyMatrix.all
            .filter { it.blocksProgress }
            .map { it.id }
            .toSet()

        RedesignStateCopyMatrix.all.forEach { spec ->
            assertNotNull(spec.title, "Missing title resource for ${spec.id}")
        }
        RedesignStateCopyMatrix.all
            .filter { it.surface == RedesignStateCopySurface.BottomSheet }
            .forEach { spec ->
                assertNotNull(spec.primaryAction, "Missing primary action for ${spec.id}")
                assertNotNull(spec.secondaryAction, "Missing secondary action for ${spec.id}")
            }
        assertEquals(
            setOf(
                RedesignStateCopyId.PRE_GENERATION_PRICE_CONFIRM,
                RedesignStateCopyId.REUSE_PARAMETERS,
            ),
            bottomSheetIds,
        )
        assertEquals(
            setOf(
                RedesignStateCopyId.UPLOAD_IMAGE_IN_PROGRESS,
                RedesignStateCopyId.PROMPT_EMPTY,
                RedesignStateCopyId.PROMPT_TOO_SHORT,
                RedesignStateCopyId.PARAMETER_MISSING,
                RedesignStateCopyId.PARAMETER_CONFLICT,
                RedesignStateCopyId.INSUFFICIENT_BALANCE,
                RedesignStateCopyId.PRE_GENERATION_PRICE_CONFIRM,
            ),
            blockingIds,
        )
    }

    @Test
    fun `critical copy surfaces stay explicit`() {
        assertEquals(
            RedesignStateCopySurface.BottomSheet,
            RedesignStateCopyMatrix[RedesignStateCopyId.PRE_GENERATION_PRICE_CONFIRM].surface,
        )
        assertEquals(
            RedesignStateCopySurface.BottomSheet,
            RedesignStateCopyMatrix[RedesignStateCopyId.REUSE_PARAMETERS].surface,
        )
        assertEquals(
            RedesignStateCopySurface.Snackbar,
            RedesignStateCopyMatrix[RedesignStateCopyId.COPY_PROMPT_OR_TASK_ID].surface,
        )
        assertEquals(
            RedesignStateCopySurface.ScreenEmpty,
            RedesignStateCopyMatrix[RedesignStateCopyId.HISTORY_EMPTY].surface,
        )
    }
}
