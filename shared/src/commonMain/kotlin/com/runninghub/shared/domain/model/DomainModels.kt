package com.runninghub.shared.domain.model

data class WebApp(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val previewUrl: String?,
    val coverUrls: List<String>,
    val authorName: String?,
    val authorAvatar: String?,
    val authorId: String?,
    val tags: List<Tag>,
    val likeCount: Int,
    val collectCount: Int,
    val useCount: Int,
    val viewCount: Int
)

data class WebAppDetail(
    val id: String,
    val name: String,
    val description: String?,
    val publishTime: String?,
    val authorName: String,
    val authorAvatar: String?,
    val authorId: String?,
    val tags: List<Tag>,
    val inputNodes: List<InputNode>,
    val coverUrls: List<String>,
    val stats: AppStats
)

data class InputNode(
    val nodeId: String,
    val nodeName: String,
    val fieldName: String,
    val fieldValue: String?,
    val fieldType: String,
    val description: String?,
    val options: List<String>
)

data class Tag(
    val id: String,
    val name: String,
    val children: List<Tag> = emptyList()
)

data class AppStats(
    val likeCount: Int = 0,
    val collectCount: Int = 0,
    val useCount: Int = 0,
    val viewCount: Int = 0
)

data class User(
    val id: String,
    val nickName: String,
    val avatarUrl: String?,
    val mobile: String?,
    val totalCoin: String?,
    val memberName: String?,
    val memberExpiredTime: String?,
    val balance: Double,
    val currency: String?,
    val apiKey: String?,
    val apiType: String?,
    val introduce: String?,
    val fanCount: Int,
    val followCount: Int,
    val likeCount: Int,
    val collectCount: Int
)

data class TaskResult(
    val taskId: Long,
    val status: TaskStatus,
    val outputs: List<TaskOutput> = emptyList(),
    val errorMessage: String? = null
)

enum class TaskStatus {
    PENDING, RUNNING, SUCCESS, FAILED, UNKNOWN
}

data class TaskOutput(
    val fileUrl: String,
    val fileName: String?,
    val fileType: String?
)

data class AccountStatus(
    val remainCoins: String,
    val currentTaskCounts: String,
    val remainMoney: String?,
    val currency: String?,
    val apiType: String
)
