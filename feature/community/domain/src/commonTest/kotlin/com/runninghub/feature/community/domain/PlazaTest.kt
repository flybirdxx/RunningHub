package com.runninghub.feature.community.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlazaTest {
    @Test
    fun `creation page preserves server ordering`() {
        val page = PlazaCreationPage(
            page = 1,
            total = 2,
            items = listOf(
                PlazaCreationCard(id = "recommended-first"),
                PlazaCreationCard(id = "recommended-second"),
            ),
        )

        // 广场推荐顺序由服务端排序决定，Domain 模型只承载结果，不在客户端重新排序。
        assertEquals(listOf("recommended-first", "recommended-second"), page.items.map { it.id })
        assertTrue(page.total >= page.items.size)
    }
}
