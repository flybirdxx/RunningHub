package com.runninghub.core.model

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
    fun getDisplayName(): String = owner?.name ?: authorName ?: "Anonymous"
    fun getDisplayAvatar(): String? = owner?.avatar ?: authorAvatar
}

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
    fun getOptions(): List<String> {
        if (fieldData.isNullOrEmpty()) return emptyList()
        return try {
            parseFieldDataOptions(fieldData)
        } catch (_: Exception) {
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
                result.add(s.substring(start, i))
                start = i + 1
            }
        }
    }
    if (start < s.length) result.add(s.substring(start))
    return result
}
