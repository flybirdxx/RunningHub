package com.runninghub.app.data.remote.model

import com.google.gson.annotations.SerializedName
import org.json.JSONArray
import org.json.JSONObject

/**
 * [INPUT]: WebApp 列表原始数据
 * [OUTPUT]: 结构化的 WebApp 响应对象
 * [POS]: 业务模型的远程映射
 */
data class WebAppDto(
    @SerializedName("id", alternate = ["webappId"]) val id: String? = null,
    @SerializedName("name", alternate = ["webappName", "title"]) val title: String? = null,
    @SerializedName("intro", alternate = ["desc", "description"]) val desc: String? = null,
    val thumbnailUrl: String? = null,
    val preview: PreviewDto? = null,
    val covers: List<CoverDto>? = null,
    @SerializedName("owner", alternate = ["author"]) val author: AuthorDto? = null,
    val tags: List<TagSimpleDto>? = null,
    val statisticsInfo: StatisticsInfo? = null,
    // Fields that sometimes return flatly from the API instead of nested
    val likeCount: String? = null,
    val collectCount: String? = null,
    val useCount: String? = null,
    val pv: String? = null
)

data class WebAppDetailDto(
    @SerializedName("id", alternate = ["webappId"]) val id: String?,
    @SerializedName("webappName", alternate = ["name"]) val name: String?,
    val tags: List<TagSimpleDto>?,
    @SerializedName("owner", alternate = ["author", "user"]) val owner: AuthorDto?,
    @SerializedName("publishTime", alternate = ["createTime", "create_time", "updateTime", "time"]) val publishTime: String?,
    @SerializedName("nodeInfoList", alternate = ["inputNodes"]) val inputNodes: List<InputNodeDto>?,
    val description: String?,
    val covers: List<CoverDto>?,
    val statisticsInfo: StatisticsInfo?,
    // Flat mapping fallback
    @SerializedName("userName", alternate = ["authorName", "nickname", "nickName", "user_name"]) val authorName: String? = null,
    @SerializedName("userAvatar", alternate = ["authorAvatar", "avatar", "user_avatar"]) val authorAvatar: String? = null
) {
    fun getDisplayName(): String = owner?.name ?: authorName ?: "Anonymous"
    fun getDisplayAvatar(): String? = owner?.avatar ?: authorAvatar
}

data class InputNodeDto(
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
     * Parses fieldData like "[[\"1k\", \"2k\"], {\"default\": \"2k\"}]"
     * into a list of selectable options.
     */
    fun getOptions(): List<String> {
        if (fieldData.isNullOrEmpty()) return emptyList()
        val options = mutableListOf<String>()
        try {
            // 1. Try modern JSON parsing
            val root = JSONArray(fieldData)
            for (i in 0 until root.length()) {
                val item = root.opt(i) ?: continue
                when (item) {
                    is JSONArray -> {
                        for (j in 0 until item.length()) {
                            val sub = item.opt(j) ?: continue
                            options.add(parseSingleOption(sub))
                        }
                    }
                    else -> options.add(parseSingleOption(item))
                }
            }
        } catch (e: Exception) {
            // 2. Fallback to refined regex/split for broken fragments
            return fieldData.split(Regex("[\\[\\]{},]"))
                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                .filter { it.isNotEmpty() && !it.contains(":") && !it.contains("\"") }
        }
        return options.filter { it.isNotBlank() && !it.startsWith("{") }
    }

    private fun parseSingleOption(item: Any): String {
        return when (item) {
            is JSONObject -> {
                // For objects, try to find a human-readable name or ID
                // We check name first as it's likely the functional value,
                // then description for display, then fallback to anything available.
                val name = item.optString("name", "")
                if (name.isNotEmpty()) return name
                
                val desc = item.optString("description", "")
                if (desc.isNotEmpty()) return desc
                
                val def = item.optString("default", "")
                if (def.isNotEmpty()) return def
                
                ""
            }
            else -> item.toString()
                .trim()
                .removeSurrounding("[")
                .removeSurrounding("]")
                .removeSurrounding("\"")
                .removeSurrounding("'")
        }
    }
}

data class TagSimpleDto(
    val id: String,
    val name: String
)

data class PreviewDto(
    val url: String? = null
)

data class AuthorDto(
    @SerializedName("name", alternate = ["nickname", "userName", "nickName", "authorName", "user_name"]) val name: String? = null,
    @SerializedName("avatar", alternate = ["userAvatar", "authorAvatar", "user_avatar"]) val avatar: String? = null,
    @SerializedName("id", alternate = ["userId", "uid"]) val id: String? = null,
    @SerializedName("intro", alternate = ["description", "desc", "bio", "signature"]) val intro: String? = null,
    @SerializedName("followCount", alternate = ["attentionCount", "follows"]) val followCount: String? = "0",
    @SerializedName("fansCount", alternate = ["fanCount", "fans", "followers"]) val fansCount: String? = "0",
    @SerializedName("likeCount", alternate = ["likes", "praisedCount"]) val likeCount: String? = "0",
    @SerializedName("collectCount", alternate = ["collections", "collectedCount"]) val collectCount: String? = "0",
    @SerializedName("bgImage", alternate = ["backgroundImage", "banner", "cover"]) val bgImage: String? = null
)

data class StatisticsInfo(
    val likeCount: String? = "0",
    val collectCount: String? = "0",
    val useCount: String? = "0",
    val pv: String? = "0"
)

data class CoverDto(
    val url: String? = null,
    val imageWidth: String? = null,
    val imageHeight: String? = null
)

data class WebAppListRequest(
    val size: Int,
    val current: Int,
    val tags: List<String> = emptyList(),
    val sort: String = "RECOMMEND"
)

data class TagTreeRequest(
    val rang: String = "WEBAPP"
)

// Task Execution Models
data class TaskRunRequest(
    val webappId: Long,
    val apiKey: String,
    val nodeInfoList: List<InputNodeDto>,
    val webhookUrl: String? = null,
    val instanceType: String? = null
)

data class TaskRunResponse(
    val netWssUrl: String?,
    val taskId: Long,
    val clientId: String?,
    val taskStatus: String?,
    val promptTips: String?
)

data class TaskStatusRequest(
    val taskId: Long,
    val apiKey: String
)

data class TaskOutputDto(
    val fileUrl: String?,
    val fileName: String?,
    val fileType: String?,
    val failedReason: TaskFailedReason? = null
)

data class TaskFailedReason(
    val node_name: String?,
    val exception_message: String?,
    val traceback: String?
)

data class UploadResponse(
    val fileName: String?,
    val fileType: String?
)
