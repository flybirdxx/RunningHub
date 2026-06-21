package com.runninghub.feature.model.data.repository

import com.runninghub.feature.model.domain.ApiModelField
import com.runninghub.feature.model.domain.ApiModelFieldType
import com.runninghub.feature.model.domain.ModelFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelInvocationRequestBuilderTest {
    @Test
    fun `builds request body from typed values and visible fields`() {
        val body = ModelInvocationRequestBuilder().build(
            fields = listOf(
                ApiModelField("prompt", "prompt", ApiModelFieldType.STRING, required = true),
                ApiModelField("aspectRatio", "aspectRatio", ApiModelFieldType.LIST, required = false),
                ApiModelField("private", "private", ApiModelFieldType.STRING, required = false, visible = false),
            ),
            values = mapOf(
                "prompt" to ModelFieldValue.Text("a cat"),
                "aspectRatio" to ModelFieldValue.Text("16:9"),
                "private" to ModelFieldValue.Text("internal"),
            ),
            webhookUrl = "https://example.com/hook",
        )

        assertEquals("a cat", body["prompt"])
        assertEquals("16:9", body["aspectRatio"])
        assertEquals("https://example.com/hook", body["webhookUrl"])
        assertEquals(false, body.containsKey("private"))
    }

    @Test
    fun `builds list value for multi media field`() {
        val body = ModelInvocationRequestBuilder().build(
            fields = listOf(
                ApiModelField(
                    fieldKey = "imageUrls",
                    paramKey = "imageUrls",
                    type = ApiModelFieldType.IMAGE,
                    required = true,
                    multipleInputs = true,
                )
            ),
            values = mapOf("imageUrls" to ModelFieldValue.StringList(listOf("https://a.png", "https://b.png"))),
        )

        assertEquals(listOf("https://a.png", "https://b.png"), body["imageUrls"])
    }
}
