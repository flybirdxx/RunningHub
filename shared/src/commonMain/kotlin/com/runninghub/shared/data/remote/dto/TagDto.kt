package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("level") val level: Int = 0,
    @SerialName("parentId") val parentId: String? = null,
    @SerialName("rang") val rang: String = "",
    @SerialName("enable") val enable: Boolean = true,
    @SerialName("childTags") val childTags: List<TagDto>? = null
)
