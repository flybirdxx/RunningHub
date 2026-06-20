package com.runninghub.shared.data.repository

import com.runninghub.core.storage.QuickCreateDraftStore
import com.runninghub.shared.domain.repository.QuickCreateDraftSnapshot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateDraftRepositoryImplTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `getRestorableDraft returns snapshot from stored json`() = runBlocking {
        val store = FakeQuickCreateDraftStore(
            draft = """{"currentTab":"VIDEO","imagePrompt":"image draft","videoPrompt":"video draft"}""",
        )
        val repository = QuickCreateDraftRepositoryImpl(store, json)

        val draft = repository.getRestorableDraft()

        assertEquals(
            QuickCreateDraftSnapshot(
                currentTab = "VIDEO",
                imagePrompt = "image draft",
                videoPrompt = "video draft",
            ),
            draft,
        )
        assertEquals(false, store.cleared)
    }

    @Test
    fun `getRestorableDraft clears invalid json`() = runBlocking {
        val store = FakeQuickCreateDraftStore(draft = "{")
        val repository = QuickCreateDraftRepositoryImpl(store, json)

        val draft = repository.getRestorableDraft()

        assertEquals(null, draft)
        assertEquals(true, store.cleared)
        assertEquals(null, store.draft)
    }

    @Test
    fun `getRestorableDraft clears draft without prompt content`() = runBlocking {
        val store = FakeQuickCreateDraftStore(
            draft = """{"currentTab":"IMAGE","imagePrompt":"","videoPrompt":""}""",
        )
        val repository = QuickCreateDraftRepositoryImpl(store, json)

        val draft = repository.getRestorableDraft()

        assertEquals(null, draft)
        assertEquals(true, store.cleared)
        assertEquals(null, store.draft)
    }

    @Test
    fun `saveDraft stores stable json when prompt exists`() = runBlocking {
        val store = FakeQuickCreateDraftStore()
        val repository = QuickCreateDraftRepositoryImpl(store, json)

        repository.saveDraft(
            QuickCreateDraftSnapshot(
                currentTab = "IMAGE",
                imagePrompt = "new image",
                videoPrompt = "",
            )
        )

        val element = json.parseToJsonElement(requireNotNull(store.draft)).jsonObject
        assertEquals("IMAGE", element["currentTab"]?.jsonPrimitive?.contentOrNull)
        assertEquals("new image", element["imagePrompt"]?.jsonPrimitive?.contentOrNull)
        assertEquals("", element["videoPrompt"]?.jsonPrimitive?.contentOrNull)
    }

    @Test
    fun `saveDraft clears storage when prompts are empty`() = runBlocking {
        val store = FakeQuickCreateDraftStore(draft = """{"imagePrompt":"old"}""")
        val repository = QuickCreateDraftRepositoryImpl(store, json)

        repository.saveDraft(QuickCreateDraftSnapshot(imagePrompt = "", videoPrompt = ""))

        assertEquals(true, store.cleared)
        assertEquals(null, store.draft)
    }

    private class FakeQuickCreateDraftStore(
        var draft: String? = null,
    ) : QuickCreateDraftStore {
        var cleared: Boolean = false

        override suspend fun getQuickCreateDraft(): String? =
            draft

        override suspend fun saveQuickCreateDraft(json: String) {
            draft = json
            cleared = false
        }

        override suspend fun clearQuickCreateDraft() {
            draft = null
            cleared = true
        }
    }
}
