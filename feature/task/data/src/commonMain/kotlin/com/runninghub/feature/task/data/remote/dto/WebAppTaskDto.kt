package com.runninghub.feature.task.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
 * 控制台任务宽表请求 DTO。
 *
 * 该接口来自 Web 控制台 `/api/billing/usage/wideDetails`，可返回所有任务来源的状态与费用记录，
 * 包括尚未生成 output 的运行中任务。请求使用登录态 Authorization/Cookie，不携带 API Key。
 *
 * @property startDateTime 查询开始时间，北京时间格式 `yyyy-MM-dd HH:mm:ss`。
 * @property endDateTime 查询结束时间，北京时间格式 `yyyy-MM-dd HH:mm:ss`。
 * @property size 每页任务数量。
 * @property includeStats 是否返回汇总统计。
 * @property includeChildTasks 是否在主列表中带出子任务记录。
 */
@Serializable
data class BillingUsageWideDetailsRequestDto(
    @SerialName("startDateTime") val startDateTime: String,
    @SerialName("endDateTime") val endDateTime: String,
    @SerialName("size") val size: Int,
    @SerialName("includeStats") val includeStats: Boolean = true,
    @SerialName("includeChildTasks") val includeChildTasks: Boolean = true,
)

/**
 * 控制台任务宽表分页 DTO。
 *
 * @property records 当前页任务记录。
 * @property total 服务端总数；该接口可能返回 null。
 * @property hasNext 是否还有下一页。
 * @property nextCursor 下一页游标；当前历史页先读取首屏，后续分页可基于该字段扩展。
 */
@Serializable
data class BillingUsageWideDetailsDataDto(
    @SerialName("records") val records: List<BillingUsageTaskDto> = emptyList(),
    @SerialName("total") val total: Int? = null,
    @SerialName("hasNext") val hasNext: Boolean = false,
    @SerialName("nextCursor") val nextCursor: String? = null,
)

/**
 * 控制台任务宽表记录 DTO。
 *
 * 字段来自账单控制台，包含任务状态、父子关系、来源类型和费用信息。敏感字段如 apiKey 虽然服务端会返回，
 * 但 DTO 不声明、不解析，避免进入客户端领域模型或日志。
 */
@Serializable
data class BillingUsageTaskDto(
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("taskName") val taskName: String? = null,
    @SerialName("skuName") val skuName: String? = null,
    @SerialName("skuNameCn") val skuNameCn: String? = null,
    @SerialName("workflowName") val workflowName: String? = null,
    @SerialName("taskStatus") val taskStatus: String? = null,
    @SerialName("createTime") val createTime: String? = null,
    @SerialName("taskStartTime") val taskStartTime: String? = null,
    @SerialName("moneyDuration") val moneyDuration: String? = null,
    @SerialName("coinUsedDuration") val coinUsedDuration: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("moneyAmount") val moneyAmount: Double? = null,
    @SerialName("coinAmount") val coinAmount: Double? = null,
    @SerialName("taskCategoryCode") val taskCategoryCode: String? = null,
    @SerialName("taskCategoryDisplay") val taskCategoryDisplay: String? = null,
    @SerialName("originalTaskCategory") val originalTaskCategory: String? = null,
    @SerialName("taskResourceType") val taskResourceType: String? = null,
    @SerialName("taskRelation") val taskRelation: String? = null,
    @SerialName("parentTaskId") val parentTaskId: String? = null,
    @SerialName("webappId") val webappId: String? = null,
    @SerialName("workflowId") val workflowId: String? = null,
    @SerialName("skuId") val skuId: String? = null,
)

/**
 * 控制台任务详情请求 DTO。
 *
 * 详情接口与 Web 控制台任务详情抽屉一致；浏览器验证显示登录态 Authorization 已足以定位当前用户，
 * 请求体只需要任务 ID，不携带 API Key、Cookie 或 userId。
 *
 * @property taskId 服务端任务稳定标识。
 */
@Serializable
data class OpenApiCallLogDetailRequestDto(
    @SerialName("taskId") val taskId: String,
)

/**
 * 控制台任务详情 DTO。
 *
 * @property basicInfo 顶部摘要与基础任务字段。
 * @property list 生成结果文件列表。
 * @property costInfo 计费详情字段。
 * @property requestInfo 原始请求参数；可能包含 API Key，映射前必须脱敏。
 * @property responseInfo 原始响应对象；映射前必须递归脱敏。
 */
@Serializable
data class OpenApiCallLogDetailDataDto(
    @SerialName("basicInfo") val basicInfo: OpenApiCallLogBasicInfoDto? = null,
    @SerialName("list") val list: List<TaskHistoryOutputDto> = emptyList(),
    @SerialName("costInfo") val costInfo: OpenApiCallLogCostInfoDto? = null,
    @SerialName("requestInfo") val requestInfo: OpenApiCallLogRequestInfoDto? = null,
    @SerialName("responseInfo") val responseInfo: JsonElement? = null,
)

/**
 * 控制台任务基础信息 DTO。
 *
 * 金额、RH 币和部分枚举字段在不同任务类型下可能以数字或字符串返回，因此使用 [JsonElement]
 * 保留原始值，再由 mapper 转成安全文本。
 */
@Serializable
data class OpenApiCallLogBasicInfoDto(
    @SerialName("apiName") val apiName: String? = null,
    @SerialName("apiType") val apiType: String? = null,
    @SerialName("apiKeyType") val apiKeyType: JsonElement? = null,
    @SerialName("taskStatus") val taskStatus: String? = null,
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("callTime") val callTime: String? = null,
    @SerialName("duration") val duration: JsonElement? = null,
    @SerialName("amount") val amount: JsonElement? = null,
    @SerialName("coinNum") val coinNum: JsonElement? = null,
    @SerialName("callType") val callType: String? = null,
    @SerialName("callMethod") val callMethod: String? = null,
    @SerialName("account") val account: String? = null,
    @SerialName("accountId") val accountId: String? = null,
    @SerialName("apiKey") val apiKey: String? = null,
    @SerialName("apiKeyName") val apiKeyName: String? = null,
    @SerialName("mode") val mode: String? = null,
)

/**
 * 控制台任务计费信息 DTO。
 */
@Serializable
data class OpenApiCallLogCostInfoDto(
    @SerialName("amount") val amount: JsonElement? = null,
    @SerialName("coinNum") val coinNum: JsonElement? = null,
    @SerialName("originalAmount") val originalAmount: JsonElement? = null,
    @SerialName("originAmount") val originAmount: JsonElement? = null,
    @SerialName("discountRatio") val discountRatio: JsonElement? = null,
    @SerialName("discountRate") val discountRate: JsonElement? = null,
    @SerialName("discountAmount") val discountAmount: JsonElement? = null,
    @SerialName("finalAmount") val finalAmount: JsonElement? = null,
    @SerialName("afterDiscountAmount") val afterDiscountAmount: JsonElement? = null,
)

/**
 * 控制台任务请求信息 DTO。
 *
 * @property apiRequestParams 服务端保存的原始 OpenAPI 请求参数 JSON 字符串，可能包含 API Key。
 */
@Serializable
data class OpenApiCallLogRequestInfoDto(
    @SerialName("apiRequestParams") val apiRequestParams: String? = null,
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
