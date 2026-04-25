package com.runninghub.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WebAppListRequest(
    val size: Int,
    val current: Int,
    val tags: List<String> = emptyList(),
    val sort: String = "RECOMMEND"
)

@Serializable
data class TagTreeRequest(
    val rang: String = "WEBAPP"
)

@Serializable
data class TaskRunRequest(
    val webappId: Long,
    val apiKey: String,
    val nodeInfoList: List<InputNodeDto>,
    val webhookUrl: String? = null,
    val instanceType: String? = null
)

@Serializable
data class TaskRunResponse(
    val netWssUrl: String? = null,
    val taskId: Long,
    val clientId: String? = null,
    val taskStatus: String? = null,
    val promptTips: String? = null
)

@Serializable
data class TaskStatusRequest(
    val taskId: Long,
    val apiKey: String
)

@Serializable
data class TaskOutputDto(
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileType: String? = null,
    val failedReason: TaskFailedReason? = null
)

@Serializable
data class TaskFailedReason(
    val node_name: String? = null,
    val exception_message: String? = null,
    val traceback: String? = null
)

@Serializable
data class UploadResponse(
    val fileName: String? = null,
    val fileType: String? = null
)

@Serializable
data class AccountStatusRequest(
    val apikey: String
)

@Serializable
data class AccountStatusDto(
    val remainCoins: String? = null,
    val currentTaskCounts: String? = null,
    val remainMoney: String? = null,
    val currency: String? = null,
    val apiType: String? = null
)
