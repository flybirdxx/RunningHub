package com.runninghub.app.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * [INPUT]: WebApp 列表原始数据
 * [OUTPUT]: 结构化的 WebApp 响应对象
 * [POS]: 业务模型的远程映射
 */
data class WebAppDto(
    val id: String,
    @SerializedName("name") val title: String,
    @SerializedName("intro") val desc: String?,
    val preview: PreviewDto? = null,
    val covers: List<CoverDto>?,
    val owner: AuthorDto? = null,
    @SerializedName("author") val author: AuthorDto?,
    val tags: List<TagSimpleDto>? = null,
    val statisticsInfo: StatisticsInfo?
)

data class WebAppDetailDto(
    @SerializedName("id", alternate = ["webappId"]) val id: String?,
    @SerializedName("webappName", alternate = ["name"]) val name: String,
    val tags: List<TagSimpleDto>?,
    val owner: AuthorDto?,
    val publishTime: String?,
    @SerializedName("nodeInfoList", alternate = ["inputNodes"]) val inputNodes: List<InputNodeDto>?,
    val description: String?,
    val covers: List<CoverDto>?,
    val statisticsInfo: StatisticsInfo?
)

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
        return try {
            // Simple extraction for the "[["..."]]" format
            val firstArray = fieldData.substringAfter("[[").substringBefore("]]")
            firstArray.split(",").map { 
                it.trim().removeSurrounding("\"").removeSurrounding("'") 
            }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
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
    val name: String? = null,
    val nickname: String? = "Anonymous",
    val avatar: String? = null
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
