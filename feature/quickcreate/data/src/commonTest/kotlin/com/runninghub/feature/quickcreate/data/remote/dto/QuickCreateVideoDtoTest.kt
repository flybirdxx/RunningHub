package com.runninghub.feature.quickcreate.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.boolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class QuickCreateVideoDtoTest {
    private val json = Json {
        explicitNulls = false
    }

    @Test
    fun `seedance image video request serializes only documented request params`() {
        val payload = json.encodeToString(
            SeedanceImageToVideoRequestDto(
                prompt = "green square",
                resolution = "1080p",
                duration = 4,
                firstFrameUrl = "https://example.com/first.png",
                lastFrameUrl = "https://example.com/last.png",
                generateAudio = false,
                ratio = "16:9",
                realPersonMode = false,
                returnLastFrame = true,
            )
        )

        val body = json.parseToJsonElement(payload).jsonObject

        assertEquals(
            setOf(
                "prompt",
                "resolution",
                "duration",
                "firstFrameUrl",
                "lastFrameUrl",
                "generateAudio",
                "ratio",
                "realPersonMode",
                "returnLastFrame",
            ),
            body.keys,
        )
        assertFalse("conversionSlots" in body)
        assertEquals(4, body.getValue("duration").jsonPrimitive.int)
        assertFalse(body.getValue("realPersonMode").jsonPrimitive.boolean)
    }
}
