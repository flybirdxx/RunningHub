package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebAppDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val title: String? = null,
    @SerialName("intro") val desc: String? = null,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerialName("preview") val preview: PreviewDto? = null,
    @SerialName("covers") val covers: List<CoverDto>? = null,
    @SerialName("owner") val author: AuthorDto? = null,
    @SerialName("tags") val tags: List<TagSimpleDto>? = null,
    @SerialName("statisticsInfo") val statisticsInfo: StatisticsInfoDto? = null,
    @SerialName("likeCount") val likeCount: String? = null,
    @SerialName("collectCount") val collectCount: String? = null,
    @SerialName("useCount") val useCount: String? = null,
    @SerialName("pv") val pv: String? = null,
    @SerialName("labels") val labels: String? = null,
    @SerialName("carefullyChosen") val carefullyChosen: Boolean = false,
    @SerialName("publishTime") val publishTime: String? = null
)

@Serializable
data class WebAppDetailDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("workflowId") val workflowId: String? = null,
    @SerialName("tags") val tags: List<TagSimpleDto>? = null,
    @SerialName("owner") val owner: AuthorDto? = null,
    @SerialName("publishTime") val publishTime: String? = null,
    @SerialName("inputNodes") val inputNodes: List<InputNodeDto>? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("covers") val covers: List<CoverDto>? = null,
    @SerialName("statisticsInfo") val statisticsInfo: StatisticsInfoDto? = null,
    @SerialName("userName") val authorName: String? = null,
    @SerialName("userAvatar") val authorAvatar: String? = null,
    @SerialName("runningSuccessRate") val runningSuccessRate: String? = null,
    @SerialName("avgRunningSeconds") val avgRunningSeconds: String? = null,
    @SerialName("instanceType") val instanceType: String? = null
)

@Serializable
data class InputNodeDto(
    @SerialName("nodeId") val nodeId: String,
    @SerialName("nodeName") val nodeName: String = "",
    @SerialName("fieldName") val fieldName: String = "",
    @SerialName("fieldValue") val fieldValue: String? = null,
    @SerialName("fieldData") val fieldData: String? = null,
    @SerialName("fieldType") val fieldType: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("descriptionEn") val descriptionEn: String? = null
)

@Serializable
data class TagSimpleDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("nameEn") val nameEn: String? = null,
    @SerialName("labels") val labels: String? = null
)

@Serializable
data class PreviewDto(
    @SerialName("url") val url: String? = null
)

@Serializable
data class AuthorDto(
    @SerialName("name") val name: String? = null,
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("intro") val intro: String? = null,
    @SerialName("followCount") val followCount: String? = "0",
    @SerialName("fansCount") val fansCount: String? = "0",
    @SerialName("likeCount") val likeCount: String? = "0",
    @SerialName("collectCount") val collectCount: String? = "0",
    @SerialName("bgImage") val bgImage: String? = null
)

@Serializable
data class StatisticsInfoDto(
    @SerialName("likeCount") val likeCount: String? = "0",
    @SerialName("collectCount") val collectCount: String? = "0",
    @SerialName("useCount") val useCount: String? = "0",
    @SerialName("pv") val pv: String? = "0"
)

@Serializable
data class CoverDto(
    @SerialName("url") val url: String? = null,
    @SerialName("thumbnailUri") val thumbnailUri: String? = null,
    @SerialName("imageWidth") val imageWidth: String? = null,
    @SerialName("imageHeight") val imageHeight: String? = null
)

@Serializable
data class WebAppListRequest(
    @SerialName("size") val pageSize: Int,
    @SerialName("current") val pageNum: Int,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("keyword") val keyword: String? = null,
    @SerialName("sort") val sort: String? = null,
    @SerialName("days") val days: Int? = null
)

@Serializable
data class CustomMadeWebappRequest(
    @SerialName("tags") val tags: List<String> = emptyList()
)

@Serializable
data class TagTreeRequest(
    @SerialName("rang") val rang: String = "WEBAPP"
)

@Serializable
data class TaskRunRequest(
    @SerialName("webappId") val webappId: Long,
    @SerialName("apiKey") val apiKey: String,
    @SerialName("nodeInfoList") val nodeInfoList: List<InputNodeDto>,
    @SerialName("webhookUrl") val webhookUrl: String? = null,
    @SerialName("instanceType") val instanceType: String? = null
)

@Serializable
data class TaskRunResponseDto(
    @SerialName("netWssUrl") val netWssUrl: String? = null,
    @SerialName("taskId") val taskId: Long,
    @SerialName("clientId") val clientId: String? = null,
    @SerialName("taskStatus") val taskStatus: String? = null,
    @SerialName("promptTips") val promptTips: String? = null
)

@Serializable
data class TaskStatusRequest(
    @SerialName("taskId") val taskId: Long,
    @SerialName("apiKey") val apiKey: String
)

@Serializable
data class TaskOutputDto(
    @SerialName("fileUrl") val fileUrl: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("fileType") val fileType: String? = null,
    @SerialName("failedReason") val failedReason: TaskFailedReasonDto? = null
)

@Serializable
data class TaskFailedReasonDto(
    @SerialName("node_name") val nodeName: String? = null,
    @SerialName("exception_message") val exceptionMessage: String? = null,
    @SerialName("traceback") val traceback: String? = null
)

@Serializable
data class UploadResponseDto(
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("fileType") val fileType: String? = null
)

@Serializable
data class TaskHistoryItemDto(
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("outputList") val outputList: List<TaskHistoryOutputDto>? = null,
    @SerialName("owner") val owner: AuthorDto? = null,
    @SerialName("taskStatus") val taskStatus: String? = null,
    @SerialName("taskResultDesc") val taskResultDesc: String? = null,
    @SerialName("taskCostTime") val taskCostTime: String? = null,
    @SerialName("createTime") val createTime: String? = null,
    @SerialName("errorType") val errorType: String? = null,
    @SerialName("taskName") val taskName: String? = null,
    @SerialName("taskType") val taskType: String? = null,
    @SerialName("webappId") val webappId: String? = null,
)

@Serializable
data class TaskHistoryOutputDto(
    @SerialName("id") val id: String? = null,
    @SerialName("outputName") val outputName: String? = null,
    @SerialName("outputType") val outputType: String? = null,
    @SerialName("fileUrl") val fileUrl: String? = null,
    @SerialName("filePreviewUrl") val filePreviewUrl: String? = null,
    @SerialName("outputSize") val outputSize: String? = null,
    @SerialName("expireTime") val expireTime: String? = null,
    @SerialName("expireDays") val expireDays: String? = null,
)
