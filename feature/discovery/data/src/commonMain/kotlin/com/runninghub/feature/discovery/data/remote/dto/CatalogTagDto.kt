package com.runninghub.feature.discovery.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Discovery 目录标签树 DTO。
 *
 * 标签树由门户接口返回，Data 层保留递归结构并限制 mapper 递归深度，避免异常数据导致无限转换。
 *
 * @property id 标签 ID。
 * @property name 标签名称。
 * @property level 标签层级。
 * @property parentId 父标签 ID。
 * @property rang 标签适用范围。
 * @property enable 标签是否可用。
 * @property childTags 子标签列表。
 */
@Serializable
data class CatalogTagDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("level") val level: Int = 0,
    @SerialName("parentId") val parentId: String? = null,
    @SerialName("rang") val rang: String = "",
    @SerialName("enable") val enable: Boolean = true,
    @SerialName("childTags") val childTags: List<CatalogTagDto>? = null,
)
