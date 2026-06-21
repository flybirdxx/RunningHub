package com.runninghub.core.model

/**
 * 发现页和目录筛选使用的标签树节点。
 *
 * 标签结构由服务端配置驱动，客户端需要保留父子关系和排序语义。该模型不包含接口参数名，
 * Presentation 应通过领域查询对象表达筛选意图，再由 Data 层映射为远端协议值。
 *
 * @property id 标签 ID，用于筛选、去重和选中状态。
 * @property name 标签展示名。
 * @property level 标签层级，根节点和子节点的具体数值由服务端定义。
 * @property parentId 父标签 ID，根节点可能为空。
 * @property rang 服务端标签范围值，当前保持原始字符串以兼容既有接口。
 * @property enable 标签是否可用于当前筛选。
 * @property childTags 子标签列表，服务端未返回时可能为空。
 */
data class Tag(
    val id: String,
    val name: String,
    val level: Int,
    val parentId: String?,
    val rang: String,
    val enable: Boolean,
    val childTags: List<Tag>?
)
