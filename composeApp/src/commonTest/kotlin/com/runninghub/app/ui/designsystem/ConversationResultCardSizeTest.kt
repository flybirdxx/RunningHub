package com.runninghub.app.ui.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.result.conversationResultCardSize
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `extreme wide ratio is coerced to upper bound`() {
        val size = conversationResultCardSize(availableWidth = 300.dp, aspectRatio = 10f)

        // 10 被截断到 2.4：宽度上限 210dp 先生效，height = 210 / 2.4 = 87.5。
        assertDpEquals(300.dp * 0.70f, size.width)
        assertDpEquals(300.dp * 0.70f / 2.4f, size.height)
    }

    @Test
    fun `invalid ratios fall back to square`() {
        val expected = conversationResultCardSize(availableWidth = 300.dp, aspectRatio = 1f)

        // NaN、0、负数均为非法比例，回退 1:1，与显式 1f 结果一致。
        assertEquals(expected, conversationResultCardSize(availableWidth = 300.dp, aspectRatio = Float.NaN))
        assertEquals(expected, conversationResultCardSize(availableWidth = 300.dp, aspectRatio = 0f))
        assertEquals(expected, conversationResultCardSize(availableWidth = 300.dp, aspectRatio = -1f))
    }

    @Test
    fun `narrow container caps portrait width before height`() {
        val size = conversationResultCardSize(availableWidth = 200.dp, aspectRatio = 0.5625f)

        // 窄容器下宽度上限 200 × 0.70 = 140dp 先于高度上限生效：height = 140 / 0.5625 ≈ 248.889。
        assertDpEquals(200.dp * 0.70f, size.width)
        assertDpEquals(200.dp * 0.70f / 0.5625f, size.height)
    }

    private fun assertDpEquals(expected: Dp, actual: Dp, tolerance: Float = 0.01f) {
        assertTrue(
            abs(expected.value - actual.value) <= tolerance,
            "期望 $expected，实际 $actual（容差 $tolerance）",
        )
    }
}
