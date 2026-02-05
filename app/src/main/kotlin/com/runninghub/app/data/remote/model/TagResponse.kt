package com.runninghub.app.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [INPUT]: 标签/分类原始数据
 * [OUTPUT]: 结构化的标签 DTO
 * [POS]: 业务分类模型的远程映射
 */
data class TagDto(
    val id: String,
    val name: String,
    val level: Int,
    val parentId: String?,
    val rang: String,
    val enable: Boolean,
    val childTags: List<TagDto>? = null
)
