package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.feature.quickcreate.presentation.editor.quickCreationGlobalMediaReferences
import com.runninghub.feature.quickcreate.presentation.editor.quickCreationFieldMediaReferences
import com.runninghub.feature.quickcreate.presentation.editor.quickCreationRelevantMediaReferences

import kotlin.test.Test
import kotlin.test.assertEquals
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference

class QuickCreationMediaReferenceFilterTest {
    @Test
    fun `global media references exclude field bound uploads`() {
        val globalRef = mediaReference(id = "global", fieldParamKey = null)
        val fieldRef = mediaReference(id = "field", fieldParamKey = "imageUrls")

        assertEquals(
            listOf(globalRef),
            listOf(globalRef, fieldRef).quickCreationGlobalMediaReferences(),
        )
    }

    @Test
    fun `field media references match only exact param key`() {
        val imageRef = mediaReference(id = "image", fieldParamKey = "imageUrls")
        val maskRef = mediaReference(id = "mask", fieldParamKey = "maskUrls")
        val globalRef = mediaReference(id = "global", fieldParamKey = null)

        assertEquals(
            listOf(imageRef),
            listOf(imageRef, maskRef, globalRef).quickCreationFieldMediaReferences("imageUrls"),
        )
    }

    @Test
    fun `relevant media references keep global uploads and active field uploads only`() {
        val globalRef = mediaReference(id = "global", fieldParamKey = null)
        val activeFieldRef = mediaReference(id = "active", fieldParamKey = "imageUrls")
        val inactiveFieldRef = mediaReference(id = "inactive", fieldParamKey = "maskUrls")

        assertEquals(
            listOf(globalRef, activeFieldRef),
            listOf(globalRef, activeFieldRef, inactiveFieldRef)
                .quickCreationRelevantMediaReferences(activeFieldParamKeys = setOf("imageUrls")),
        )
    }
}

private fun mediaReference(
    id: String,
    fieldParamKey: String?,
): MediaReference =
    MediaReference(
        id = id,
        type = QuickCreateMediaType.IMAGE,
        uri = "content://$id",
        displayName = "$id.jpg",
        fileSizeBytes = 123,
        fieldParamKey = fieldParamKey,
        uploadStatus = UploadStatus.DONE,
        uploadProgress = 1f,
        remoteUrl = "https://example.com/$id.jpg",
    )
