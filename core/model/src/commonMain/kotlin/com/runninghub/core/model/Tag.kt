package com.runninghub.core.model

data class Tag(
    val id: String,
    val name: String,
    val level: Int,
    val parentId: String?,
    val rang: String,
    val enable: Boolean,
    val childTags: List<Tag>?
)
