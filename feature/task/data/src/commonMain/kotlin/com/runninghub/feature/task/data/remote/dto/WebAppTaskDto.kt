package com.runninghub.feature.task.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * WebApp API 调用示例详情 DTO。
 *
 * 该结构复用公开详情的大部分字段，但入口需要 API Key，因此归属于 Task Data。
 *
 * @property id WebApp ID。
 * @property name WebApp 名称。
 * @property workflowId 工作流 ID。
 * @property tags 标签列表。
 * @property owner 作者信息。
 * @property publishTime 发布时间文本。
 * @property inputNodes 可运行输入节点配置。
 * @property description 详情描述。
 * @property covers 封面资源列表。
 * @property statisticsInfo 统计信息。
 * @property authorName 旧接口单独返回的作者名称。
 * @property authorAvatar 旧接口单独返回的作者头像。
 * @property runningSuccessRate 运行成功率文本。
 * @property avgRunningSeconds 平均运行耗时文本。
 * @property instanceType 默认实例类型。
 */
@Serializable
data class WebAppTaskDetailDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("workflowId") val workflowId: String? = null,
    @SerialName("tags") val tags: List<WebAppTaskTagSimpleDto>? = null,
    @SerialName("owner") val owner: WebAppTaskAuthorDto? = null,
    @SerialName("publishTime") val publishTime: String? = null,
    @SerialName("inputNodes") val inputNodes: List<WebAppTaskInputNodeDto>? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("covers") val covers: List<WebAppTaskCoverDto>? = null,
    @SerialName("statisticsInfo") val statisticsInfo: WebAppTaskStatisticsInfoDto? = null,
    @SerialName("userName") val authorName: String? = null,
    @SerialName("userAvatar") val authorAvatar: String? = null,
    @SerialName("runningSuccessRate") val runningSuccessRate: String? = null,
    @SerialName("avgRunningSeconds") val avgRunningSeconds: String? = null,
    @SerialName("instanceType") val instanceType: String? = null,
)

/**
 * WebApp 任务输入节点 DTO。
 *
 * @property nodeId 节点 ID。
 * @property nodeName 节点名称。
 * @property fieldName 字段名称。
 * @property fieldValue 字段值。
 * @property fieldData 字段扩展数据。
 * @property fieldType 字段类型。
 * @property description 中文说明。
 * @property descriptionEn 英文说明。
 */
@Serializable
data class WebAppTaskInputNodeDto(
    @SerialName("nodeId") val nodeId: String,
    @SerialName("nodeName") val nodeName: String = "",
    @SerialName("fieldName") val fieldName: String = "",
    @SerialName("fieldValue") val fieldValue: String? = null,
    @SerialName("fieldData") val fieldData: String? = null,
    @SerialName("fieldType") val fieldType: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("descriptionEn") val descriptionEn: String? = null,
)

/**
 * WebApp 任务轻量标签 DTO。
 *
 * @property id 标签 ID。
 * @property name 标签中文名称。
 * @property nameEn 标签英文名称。
 * @property labels 服务端标签扩展文本。
 */
@Serializable
data class WebAppTaskTagSimpleDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("nameEn") val nameEn: String? = null,
    @SerialName("labels") val labels: String? = null,
)

/**
 * WebApp 任务作者 DTO。
 *
 * @property name 作者昵称。
 * @property avatar 作者头像。
 * @property id 作者 ID。
 * @property intro 作者简介。
 * @property followCount 关注数。
 * @property fansCount 粉丝数。
 * @property likeCount 获赞数。
 * @property collectCount 被收藏数。
 * @property bgImage 个人主页背景图。
 */
@Serializable
data class WebAppTaskAuthorDto(
    @SerialName("name") val name: String? = null,
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("intro") val intro: String? = null,
    @SerialName("followCount") val followCount: String? = "0",
    @SerialName("fansCount") val fansCount: String? = "0",
    @SerialName("likeCount") val likeCount: String? = "0",
    @SerialName("collectCount") val collectCount: String? = "0",
    @SerialName("bgImage") val bgImage: String? = null,
)

/**
 * WebApp 任务统计信息 DTO。
 *
 * @property likeCount 点赞数。
 * @property collectCount 收藏数。
 * @property useCount 使用数。
 * @property pv 浏览数。
 */
@Serializable
data class WebAppTaskStatisticsInfoDto(
    @SerialName("likeCount") val likeCount: String? = "0",
    @SerialName("collectCount") val collectCount: String? = "0",
    @SerialName("useCount") val useCount: String? = "0",
    @SerialName("pv") val pv: String? = "0",
)

/**
 * WebApp 任务封面 DTO。
 *
 * @property url 原始资源地址。
 * @property thumbnailUri 缩略图地址。
 * @property imageWidth 图片或视频宽度文本。
 * @property imageHeight 图片或视频高度文本。
 */
@Serializable
data class WebAppTaskCoverDto(
    @SerialName("url") val url: String? = null,
    @SerialName("thumbnailUri") val thumbnailUri: String? = null,
    @SerialName("imageWidth") val imageWidth: String? = null,
    @SerialName("imageHeight") val imageHeight: String? = null,
)

/**
 * WebApp 任务提交请求 DTO。
 *
 * @property webappId WebApp 数字 ID。
 * @property apiKey 当前用户绑定的 API Key。
 * @property nodeInfoList 输入节点参数列表。
 * @property webhookUrl 任务回调地址，为空表示不启用回调。
 * @property instanceType 运行实例类型，空值表示使用服务端默认值。
 */
@Serializable
data class TaskRunRequestDto(
    @SerialName("webappId") val webappId: Long,
    @SerialName("apiKey") val apiKey: String,
    @SerialName("nodeInfoList") val nodeInfoList: List<WebAppTaskInputNodeDto>,
    @SerialName("webhookUrl") val webhookUrl: String? = null,
    @SerialName("instanceType") val instanceType: String? = null,
)

/**
 * WebApp 任务提交响应 DTO。
 *
 * @property netWssUrl 服务端 websocket 地址。
 * @property taskId 远端任务 ID。
 * @property clientId 远端客户端 ID。
 * @property taskStatus 远端任务状态协议值。
 * @property promptTips 服务端提示信息。
 */
@Serializable
data class TaskRunResponseDto(
    @SerialName("netWssUrl") val netWssUrl: String? = null,
    @SerialName("taskId") val taskId: Long,
    @SerialName("clientId") val clientId: String? = null,
    @SerialName("taskStatus") val taskStatus: String? = null,
    @SerialName("promptTips") val promptTips: String? = null,
)

/**
 * WebApp 任务状态请求 DTO。
 *
 * @property taskId 远端任务 ID。
 * @property apiKey 当前用户绑定的 API Key。
 */
@Serializable
data class TaskStatusRequestDto(
    @SerialName("taskId") val taskId: Long,
    @SerialName("apiKey") val apiKey: String,
)

/**
 * WebApp 任务历史请求 DTO。
 *
 * 使用结构化 DTO 而不是 `Map<String, Any>`，避免 Ktor 序列化混合 String/Int Map 时无法推断统一元素类型。
 *
 * @property apiKey 当前用户绑定的 API Key。
 * @property pageNum 页码，从 1 开始。
 * @property pageSize 每页数量。
 */
@Serializable
data class TaskHistoryRequestDto(
    @SerialName("apiKey") val apiKey: String,
    @SerialName("pageNum") val pageNum: Int,
    @SerialName("pageSize") val pageSize: Int,
)

/**
 * WebApp 任务输出 DTO。
 *
 * @property fileUrl 输出文件地址。
 * @property fileName 输出文件名。
 * @property fileType 输出文件类型。
 * @property failedReason 节点失败原因。
 */
@Serializable
data class TaskOutputDto(
    @SerialName("fileUrl") val fileUrl: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("fileType") val fileType: String? = null,
    @SerialName("failedReason") val failedReason: TaskFailedReasonDto? = null,
)

/**
 * WebApp 任务失败原因 DTO。
 *
 * @property nodeName 失败节点名称。
 * @property exceptionMessage 服务端异常消息。
 * @property traceback 服务端异常堆栈文本。
 */
@Serializable
data class TaskFailedReasonDto(
    @SerialName("node_name") val nodeName: String? = null,
    @SerialName("exception_message") val exceptionMessage: String? = null,
    @SerialName("traceback") val traceback: String? = null,
)

/**
 * WebApp 任务文件上传响应 DTO。
 *
 * @property fileName 服务端保存的文件名。
 * @property fileType 服务端识别的文件类型。
 */
@Serializable
data class TaskUploadResponseDto(
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("fileType") val fileType: String? = null,
)

/**
 * WebApp 任务历史条目 DTO。
 *
 * @property taskId 任务 ID。
 * @property outputList 输出列表。
 * @property owner 历史任务所属用户。
 * @property taskStatus 远端任务状态协议值。
 * @property taskResultDesc 任务结果描述。
 * @property taskCostTime 任务耗时文本。
 * @property createTime 创建时间文本。
 * @property errorType 错误类型。
 * @property taskName 任务名称。
 * @property taskType 任务类型。
 * @property webappId WebApp ID。
 */
@Serializable
data class TaskHistoryItemDto(
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("outputList") val outputList: List<TaskHistoryOutputDto>? = null,
    @SerialName("owner") val owner: WebAppTaskAuthorDto? = null,
    @SerialName("taskStatus") val taskStatus: String? = null,
    @SerialName("taskResultDesc") val taskResultDesc: String? = null,
    @SerialName("taskCostTime") val taskCostTime: String? = null,
    @SerialName("createTime") val createTime: String? = null,
    @SerialName("errorType") val errorType: String? = null,
    @SerialName("taskName") val taskName: String? = null,
    @SerialName("taskType") val taskType: String? = null,
    @SerialName("webappId") val webappId: String? = null,
)

/**
 * WebApp 任务历史输出 DTO。
 *
 * @property id 输出 ID。
 * @property outputName 输出名称。
 * @property outputType 输出类型。
 * @property fileUrl 输出文件地址。
 * @property filePreviewUrl 输出预览地址。
 * @property outputSize 输出大小文本。
 * @property expireTime 过期时间文本。
 * @property expireDays 过期天数文本。
 */
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
