package com.runninghub.feature.detail.presentation

import com.runninghub.core.model.InputNode
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDetailInputPresentationTest {
    @Test
    fun `maps service field types to stable input controls`() {
        val rows = appDetailInputRows(
            inputNodes = listOf(
                inputNode(nodeId = "list", fieldType = "LIST", fieldData = "[\"A\",\"B\"]"),
                inputNode(nodeId = "switch", fieldType = "SWITCH", fieldData = "[\"Fast\",\"Quality\"]"),
                inputNode(nodeId = "switch-empty", fieldType = "SWITCH", fieldData = null),
                inputNode(nodeId = "int", fieldType = "INT"),
                inputNode(nodeId = "float", fieldType = "FLOAT"),
                inputNode(nodeId = "string-options", fieldType = "STRING", fieldData = "[\"Red\",\"Blue\"]"),
                inputNode(nodeId = "multiline", fieldType = "STRING", fieldData = "multiline"),
                inputNode(nodeId = "unknown", fieldType = "CUSTOM"),
            ),
            inputValues = emptyMap(),
        )

        assertEquals(AppDetailInputControl.Dropdown(listOf("A", "B")), rows.singleControl("list"))
        assertEquals(AppDetailInputControl.Segmented(listOf("Fast", "Quality")), rows.singleControl("switch"))
        assertEquals(AppDetailInputControl.BooleanSwitch, rows.singleControl("switch-empty"))
        assertEquals(AppDetailInputControl.IntegerText, rows.singleControl("int"))
        assertEquals(AppDetailInputControl.DecimalText, rows.singleControl("float"))
        assertEquals(AppDetailInputControl.Dropdown(listOf("Red", "Blue")), rows.singleControl("string-options"))
        assertEquals(AppDetailInputControl.Text(multiline = true), rows.singleControl("multiline"))
        assertEquals(AppDetailInputControl.Text(multiline = false), rows.singleControl("unknown"))
    }

    @Test
    fun `infers media controls from field type and service labels`() {
        val rows = appDetailInputRows(
            inputNodes = listOf(
                inputNode(nodeId = "image-type", fieldType = "IMAGE_UPLOAD"),
                inputNode(nodeId = "video-label", fieldName = "video", description = "上传视频"),
                inputNode(nodeId = "audio-label", fieldName = "audio", descriptionEn = "Upload audio file"),
                inputNode(nodeId = "image-label", fieldName = "cover", descriptionEn = "image upload"),
            ),
            inputValues = emptyMap(),
        )

        assertEquals(AppDetailInputControl.MediaUpload(AppDetailMediaType.IMAGE), rows.singleControl("image-type"))
        assertEquals(AppDetailInputControl.MediaUpload(AppDetailMediaType.VIDEO), rows.singleControl("video-label"))
        assertEquals(AppDetailInputControl.MediaUpload(AppDetailMediaType.AUDIO), rows.singleControl("audio-label"))
        assertEquals(AppDetailInputControl.MediaUpload(AppDetailMediaType.IMAGE), rows.singleControl("image-label"))
    }

    @Test
    fun `groups only consecutive image upload fields`() {
        val rows = appDetailInputRows(
            inputNodes = listOf(
                inputNode(nodeId = "image-1", fieldType = "IMAGE"),
                inputNode(nodeId = "image-2", fieldType = "IMAGE_UPLOAD"),
                inputNode(nodeId = "video", description = "上传视频"),
                inputNode(nodeId = "image-3", fieldType = "IMAGE"),
                inputNode(nodeId = "text", fieldType = "STRING"),
                inputNode(nodeId = "image-4", fieldType = "IMAGE"),
            ),
            inputValues = emptyMap(),
        )

        val firstGroup = rows[0] as AppDetailInputRowUiModel.ImageUploadGroup
        assertEquals(listOf("image-1", "image-2"), firstGroup.fields.map { it.nodeId })
        assertEquals(AppDetailMediaType.VIDEO, rows.mediaUploadType("video"))
        assertEquals(AppDetailInputControl.MediaUpload(AppDetailMediaType.IMAGE), rows.singleControl("image-3"))
        assertEquals(AppDetailInputControl.Text(multiline = false), rows.singleControl("text"))
        assertEquals(AppDetailInputControl.MediaUpload(AppDetailMediaType.IMAGE), rows.singleControl("image-4"))
    }

    @Test
    fun `uses edited value before node default and falls back to blank`() {
        val editedKey = appDetailInputKey("edited", "prompt")
        val rows = appDetailInputRows(
            inputNodes = listOf(
                inputNode(nodeId = "edited", fieldName = "prompt", fieldValue = "default"),
                inputNode(nodeId = "default", fieldName = "prompt", fieldValue = "server default"),
                inputNode(nodeId = "blank", fieldName = "prompt", fieldValue = null),
                inputNode(nodeId = "title", fieldName = "prompt", description = null),
            ),
            inputValues = mapOf(editedKey to "user value"),
        )

        assertEquals("user value", rows.singleField("edited").currentValue)
        assertEquals("server default", rows.singleField("default").currentValue)
        assertEquals("", rows.singleField("blank").currentValue)
        assertEquals("prompt", rows.singleField("title").title)
    }

    private fun List<AppDetailInputRowUiModel>.singleField(nodeId: String): AppDetailInputFieldUiModel =
        flatMap { row ->
            when (row) {
                is AppDetailInputRowUiModel.ImageUploadGroup -> row.fields
                is AppDetailInputRowUiModel.Single -> listOf(row.field)
            }
        }.single { it.nodeId == nodeId }

    private fun List<AppDetailInputRowUiModel>.singleControl(nodeId: String): AppDetailInputControl =
        singleField(nodeId).control

    private fun List<AppDetailInputRowUiModel>.mediaUploadType(nodeId: String): AppDetailMediaType {
        val control = singleControl(nodeId) as AppDetailInputControl.MediaUpload
        return control.mediaType
    }

    private fun inputNode(
        nodeId: String,
        fieldName: String = "field",
        fieldValue: String? = "default",
        fieldData: String? = null,
        fieldType: String = "STRING",
        description: String? = "Description",
        descriptionEn: String? = null,
    ): InputNode =
        InputNode(
            nodeId = nodeId,
            nodeName = "Node $nodeId",
            fieldName = fieldName,
            fieldValue = fieldValue,
            fieldData = fieldData,
            fieldType = fieldType,
            description = description,
            descriptionEn = descriptionEn,
        )
}
