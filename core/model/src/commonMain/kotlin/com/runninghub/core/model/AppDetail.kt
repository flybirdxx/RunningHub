package com.runninghub.core.model

/**
 * 工作流应用详情。
 *
 * 该模型用于详情页和运行前参数展示，字段由 Data 层从详情接口映射而来。它不包含 DTO 注解、
 * 远端 endpoint 或平台类型，Presentation 只根据字段语义展示和构建后续领域请求。
 *
 * @property id 应用 ID，用于详情页、运行入口和收藏等操作。
 * @property name 应用名称，兼容旧数据时可能为空。
 * @property workflowId 工作流 ID，某些运行接口需要；服务端未返回时为空。
 * @property description 应用描述，可能为空。
 * @property tags 应用标签摘要，顺序保留服务端配置。
 * @property owner 作者完整摘要，匿名或旧接口数据可能为空。
 * @property publishTime 发布时间字符串，格式由服务端决定。
 * @property inputNodes 运行前需要展示或填写的输入节点。
 * @property covers 详情页封面列表。
 * @property statisticsInfo 统计摘要，接口缺失时为空。
 * @property authorName 旧接口提供的作者名称兜底字段。
 * @property authorAvatar 旧接口提供的作者头像兜底字段。
 * @property runningSuccessRate 运行成功率展示字符串，服务端未返回时为空。
 * @property avgRunningSeconds 平均运行耗时字符串，单位由服务端决定。
 * @property instanceType 实例类型或算力类型字符串，保持服务端枚举语义。
 */
data class AppDetail(
    val id: String,
    val name: String?,
    val workflowId: String? = null,
    val description: String?,
    val tags: List<TagSimple>,
    val owner: Author?,
    val publishTime: String?,
    val inputNodes: List<InputNode>,
    val covers: List<Cover>,
    val statisticsInfo: StatisticsInfo?,
    val authorName: String?,
    val authorAvatar: String?,
    val runningSuccessRate: String? = null,
    val avgRunningSeconds: String? = null,
    val instanceType: String? = null
) {
    /**
     * 返回详情页可展示的作者名称。
     *
     * 优先使用结构化作者信息；旧接口只返回扁平作者字段时使用 [authorName] 兜底，仍为空时返回匿名展示名。
     *
     * @return 非空作者展示名。
     */
    fun getDisplayName(): String = owner?.name ?: authorName ?: "Anonymous"

    /**
     * 返回详情页可展示的作者头像 URL。
     *
     * 该函数不加载图片，也不验证 URL 可达性，只统一处理新旧接口字段优先级。
     *
     * @return 作者头像 URL；没有头像时返回 `null`。
     */
    fun getDisplayAvatar(): String? = owner?.avatar ?: authorAvatar
}

/**
 * 工作流运行参数输入节点。
 *
 * 节点字段来自服务端工作流定义，客户端仅在 Presentation 层渲染输入控件；运行请求构建应保留节点 ID、
 * 字段名和值之间的对应关系，避免不同节点的同名字段互相覆盖。
 *
 * @property nodeId 工作流节点 ID。
 * @property nodeName 工作流节点名称。
 * @property fieldName 参数字段名，用于提交运行请求。
 * @property fieldValue 默认字段值，可能为空。
 * @property fieldData 字段附加配置，可能是数组、嵌套数组或服务端兼容字符串。
 * @property fieldType 字段类型字符串，Presentation 根据它选择输入控件。
 * @property description 中文或默认字段说明。
 * @property descriptionEn 英文字段说明，缺失时由 Presentation 使用 [description]。
 */
data class InputNode(
    val nodeId: String,
    val nodeName: String,
    val fieldName: String,
    val fieldValue: String?,
    val fieldData: String?,
    val fieldType: String,
    val description: String?,
    val descriptionEn: String? = null
) {
    /**
     * 从服务端字段配置中提取可选项。
     *
     * 服务端历史数据存在数组、嵌套数组和非标准字符串混用的情况；本函数先走结构化解析，
     * 失败后再使用保守拆分兜底，保证详情页不会因为单个字段配置异常而整体不可用。
     *
     * @return 可展示的选项文本列表；没有选项或配置无法解析时返回空列表。
     */
    fun getOptions(): List<String> {
        if (fieldData.isNullOrEmpty()) return emptyList()
        return try {
            parseFieldDataOptions(fieldData)
        } catch (_: Exception) {
            // 兼容旧接口返回的非标准配置串：只保留看起来像纯选项值的片段，避免把键名或 JSON 标点展示给用户。
            fieldData.split(Regex("[\\[\\]{},]"))
                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                .filter { it.isNotEmpty() && !it.contains(":") && !it.contains("\"") }
        }
    }
}

private fun parseFieldDataOptions(data: String): List<String> {
    val options = mutableListOf<String>()
    val cleaned = data.trim()
    if (cleaned.startsWith("[")) {
        val inner = cleaned.removeSurrounding("[", "]")
        val parts = splitTopLevel(inner)
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("[")) {
                // 服务端部分字段把同一组选项再包一层数组；这里只展开一层，保留顶层顺序。
                val subParts = splitTopLevel(trimmed.removeSurrounding("[", "]"))
                options.addAll(subParts.map { it.trim().removeSurrounding("\"").removeSurrounding("'") })
            } else if (!trimmed.startsWith("{")) {
                options.add(trimmed.removeSurrounding("\"").removeSurrounding("'"))
            }
        }
    }
    return options.filter { it.isNotBlank() }
}

private fun splitTopLevel(s: String): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var start = 0
    for (i in s.indices) {
        when (s[i]) {
            '[', '{' -> depth++
            ']', '}' -> depth--
            ',' -> if (depth == 0) {
                // 只在顶层逗号处分割，避免嵌套数组或对象中的逗号破坏单个选项。
                result.add(s.substring(start, i))
                start = i + 1
            }
        }
    }
    if (start < s.length) result.add(s.substring(start))
    return result
}
