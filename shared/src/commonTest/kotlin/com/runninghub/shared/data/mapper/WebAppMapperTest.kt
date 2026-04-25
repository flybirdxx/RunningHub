package com.runninghub.shared.data.mapper

import com.runninghub.shared.data.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class WebAppMapperTest {

    @Test
    fun `WebAppDto toDomain maps all fields correctly`() {
        val dto = WebAppDto(
            id = "123",
            title = "Test App",
            desc = "A test app",
            thumbnailUrl = "https://example.com/thumb.jpg",
            preview = PreviewDto(url = "https://example.com/preview.jpg"),
            covers = listOf(CoverDto(url = "https://example.com/cover.jpg")),
            author = AuthorDto(name = "Author", avatar = "https://example.com/avatar.jpg", id = "a1"),
            tags = listOf(TagSimpleDto(id = "t1", name = "AI")),
            statisticsInfo = StatisticsInfo(likeCount = "100", collectCount = "50", useCount = "200", pv = "1000")
        )

        val domain = dto.toDomain()

        assertEquals("123", domain.id)
        assertEquals("Test App", domain.title)
        assertEquals("A test app", domain.description)
        assertEquals("https://example.com/thumb.jpg", domain.thumbnailUrl)
        assertEquals("https://example.com/preview.jpg", domain.previewUrl)
        assertEquals(listOf("https://example.com/cover.jpg"), domain.coverUrls)
        assertEquals("Author", domain.authorName)
        assertEquals("a1", domain.authorId)
        assertEquals(1, domain.tags.size)
        assertEquals("AI", domain.tags[0].name)
        assertEquals(100, domain.likeCount)
        assertEquals(50, domain.collectCount)
        assertEquals(200, domain.useCount)
        assertEquals(1000, domain.viewCount)
    }

    @Test
    fun `WebAppDto toDomain handles null fields gracefully`() {
        val dto = WebAppDto()
        val domain = dto.toDomain()

        assertEquals("", domain.id)
        assertEquals("", domain.title)
        assertEquals("", domain.description)
        assertEquals(null, domain.thumbnailUrl)
        assertEquals(emptyList(), domain.coverUrls)
        assertEquals(0, domain.likeCount)
    }

    @Test
    fun `UserDto toDomain maps member and wallet info`() {
        val dto = UserDto(
            id = "u1",
            nickName = "John",
            headIcon = "https://example.com/icon.jpg",
            memberInfo = MemberInfoDto(memberName = "VIP", memberExpiredTime = "2026-12-31"),
            walletInfo = WalletInfoDto(balance = 99.5, currency = "CNY"),
            fanCount = "500",
            followCount = "100",
            likeCount = "2000",
            collectCount = "300"
        )

        val domain = dto.toDomain()

        assertEquals("u1", domain.id)
        assertEquals("John", domain.nickName)
        assertEquals("VIP", domain.memberName)
        assertEquals(99.5, domain.balance)
        assertEquals(500, domain.fanCount)
        assertEquals(100, domain.followCount)
        assertEquals(2000, domain.likeCount)
    }

    @Test
    fun `WebAppDetailDto getDisplayName fallback chain works`() {
        val withOwner = WebAppDetailDto(
            id = "1", name = "App",
            owner = AuthorDto(name = "OwnerName"),
            authorName = "FallbackName"
        )
        assertEquals("OwnerName", withOwner.getDisplayName())

        val withoutOwner = WebAppDetailDto(
            id = "1", name = "App",
            authorName = "FallbackName"
        )
        assertEquals("FallbackName", withoutOwner.getDisplayName())

        val noName = WebAppDetailDto(id = "1", name = "App")
        assertEquals("Anonymous", noName.getDisplayName())
    }

    @Test
    fun `InputNodeDto getOptions parses comma-separated values`() {
        val node = InputNodeDto(
            nodeId = "n1",
            nodeName = "Resolution",
            fieldName = "resolution",
            fieldType = "STRING",
            fieldData = """[["1k", "2k", "4k"]]"""
        )
        val options = node.getOptions()
        assertEquals(listOf("1k", "2k", "4k"), options)
    }
}
