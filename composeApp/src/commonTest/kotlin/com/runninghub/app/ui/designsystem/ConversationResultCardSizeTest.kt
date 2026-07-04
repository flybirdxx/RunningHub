package com.runninghub.app.ui.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.result.conversationResultCardSize
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ConversationResultCardSizeTest {
    @Test
    fun `portrait 9 to 16 hits height cap and derives width`() {
        val size = conversationResultCardSize(availableWidth = 300.dp, aspectRatio = 0.5625f)

        // 高度上限 320dp 生效：width = 320 × 0.5625 = 180，height = 180 / 0.5625 = 320。
        assertDpEquals(180.dp, size.width)
        assertDpEquals(320.dp, size.height)
    }

    @Test
    fun `landscape 16 to 9 hits width cap and derives height`() {
        val size = conversationResultCardSize(availableWidth = 300.dp, aspectRatio = 1.7778f)

        // 宽度上限 300 × 0.70 = 210dp 生效：height = 210 / 1.7778 ≈ 118.125。
        assertDpEquals(300.dp * 0.70f, size.width)
        assertDpEquals(300.dp * 0.70f / 1.7778f, size.height)
    }

    @Test
    fun `square ratio produces width capped square`() {
        val size = conversationResultCardSize(availableWidth = 300.dp, aspectRatio = 1f)

        // 1:1 时宽度上限 210dp 先于高度上限生效，得到 210×210。
        assertDpEquals(300.dp * 0.70f, size.width)
        assertDpEquals(300.dp * 0.70f, size.height)
    }

    @Test
    fun `null ratio falls back to square`() {
        val size = conversationResultCardSize(availableWidth = 300.dp, aspectRatio = null)

        // null 回退 1:1，与显式 1f 结果一致。
        assertDpEquals(300.dp * 0.70f, size.width)
        assertDpEquals(300.dp * 0.70f, size.height)
    }

    @Test
    fun `extreme thin ratio is coerced to lower bound`() {
        val size = conversationResultCardSize(availableWidth = 300.dp, aspectRatio = 0.1f)

        // 0.1 被截断到 0.35：width = 320 × 0.35 = 112，height = 112 / 0.35 = 320。
        assertDpEquals(320.dp * 0.35f, size.width)
        assertDpEquals(320.dp, size.height)
    }

    private fun assertDpEquals(expected: Dp, actual: Dp, tolerance: Float = 0.01f) {
        assertTrue(
            abs(expected.value - actual.value) <= tolerance,
            "期望 $expected，实际 $actual（容差 $tolerance）",
        )
    }
}
