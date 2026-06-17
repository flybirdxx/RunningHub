package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class QuickCreationEnvelopeDto<T>(
    @SerialName("code") val code: Int = 0,
    @SerialName("msg") val msg: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: T? = null,
)

@Serializable
data class QuickCreationCategoriesRequestDto(
    @SerialName("categoryIds") val categoryIds: List<String>? = null,
)

@Serializable
data class QuickCreationCategoryDto(
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("nameCn") val nameCn: String? = null,
    @SerialName("nameEn") val nameEn: String? = null,
    @SerialName("sortOrder") val sortOrder: Int? = null,
)

@Serializable
data class QuickCreationModelRequestDto(
    @SerialName("categoryIds") val categoryIds: List<String>,
)

@Serializable
data class QuickCreationModelCatalogDto(
    @SerialName("categoryMeta") val categoryMeta: List<QuickCreationModelCategoryMetaDto> = emptyList(),
    @SerialName("categories") val categories: Map<String, List<QuickCreationModelDto>> = emptyMap(),
)

@Serializable
data class QuickCreationModelCategoryMetaDto(
    @SerialName("key") val key: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("sort") val sort: Int? = null,
)

@Serializable
data class QuickCreationModelDto(
    @SerialName("type") val type: String? = null,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("bindingId") val bindingId: String? = null,
    @SerialName("skuId") val skuId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("nameCn") val nameCn: String? = null,
    @SerialName("nameAi") val nameAi: String? = null,
    @SerialName("groupName") val groupName: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("fields") val fields: List<QuickCreationFieldDto> = emptyList(),
    @SerialName("children") val children: List<QuickCreationModelDto> = emptyList(),
    @SerialName("pricing") val pricing: QuickCreationPricingDto? = null,
)

@Serializable
data class QuickCreationPricingDto(
    @SerialName("pricingMode") val pricingMode: String? = null,
    @SerialName("settlementMode") val settlementMode: String? = null,
    @SerialName("paidPriceKind") val paidPriceKind: String? = null,
    @SerialName("flatPrice") val flatPrice: JsonElement? = null,
    @SerialName("dimensionPricing") val dimensionPricing: JsonElement? = null,
    @SerialName("discountPercent") val discountPercent: Int? = null,
    @SerialName("isFree") val isFree: Boolean = false,
    @SerialName("freeRemaining") val freeRemaining: Int = 0,
    @SerialName("isTimeFree") val isTimeFree: Boolean = false,
    @SerialName("promoType") val promoType: String? = null,
)

@Serializable
data class QuickCreationFieldDto(
    @SerialName("fieldKey") val fieldKey: String? = null,
    @SerialName("mappedApiParamKey") val mappedApiParamKey: String? = null,
    @SerialName("fieldType") val fieldType: String? = null,
    @SerialName("required") val required: Boolean = false,
    @SerialName("defaultValue") val defaultValue: JsonElement? = null,
    @SerialName("options") val options: List<QuickCreationFieldOptionDto> = emptyList(),
    @SerialName("maxUploadCount") val maxUploadCount: Int? = null,
    @SerialName("maxUploadSize") val maxUploadSize: Long? = null,
    @SerialName("multipleInputs") val multipleInputs: Boolean = false,
    @SerialName("skuInputExtraJson") val skuInputExtraJson: JsonElement? = null,
)

@Serializable
data class QuickCreationFieldOptionDto(
    @SerialName("label") val label: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("value") val value: JsonElement? = null,
)

@Serializable
data class QuickCreationCreateRequestDto(
    @SerialName("bindingId") val bindingId: String,
    @SerialName("categoryId") val categoryId: String,
    @SerialName("skuId") val skuId: String,
    @SerialName("params") val params: Map<String, JsonElement>,
)

@Serializable
data class QuickCreationCommitRequestDto(
    @SerialName("prepareToken") val prepareToken: String,
    @SerialName("createRequest") val createRequest: QuickCreationCreateRequestDto,
)

@Serializable
data class QuickCreationFeePreviewDto(
    @SerialName("passed") val passed: Boolean = false,
    @SerialName("free") val free: Boolean = false,
    @SerialName("settlementMode") val settlementMode: String? = null,
    @SerialName("requiredRhAmount") val requiredRhAmount: Double = 0.0,
    @SerialName("requiredCashAmount") val requiredCashAmount: Double = 0.0,
    @SerialName("userCashBalance") val userCashBalance: Double = 0.0,
    @SerialName("insufficientType") val insufficientType: String? = null,
    @SerialName("cashCurrency") val cashCurrency: String? = null,
)

@Serializable
data class QuickCreationPrepareDataDto(
    @SerialName("prepareToken") val prepareToken: String,
    @SerialName("expireAtMillis") val expireAtMillis: String? = null,
    @SerialName("ttlSeconds") val ttlSeconds: Int = 0,
    @SerialName("skuId") val skuId: String? = null,
    @SerialName("feePreview") val feePreview: QuickCreationFeePreviewDto? = null,
)

@Serializable
data class QuickCreationCommitDataDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("skuId") val skuId: String? = null,
    @SerialName("taskStatus") val taskStatus: String? = null,
    @SerialName("isFree") val isFree: Boolean = false,
    @SerialName("rhAmount") val rhAmount: Double = 0.0,
    @SerialName("cashAmount") val cashAmount: Double = 0.0,
    @SerialName("feePreview") val feePreview: QuickCreationFeePreviewDto? = null,
    @SerialName("outputWidth") val outputWidth: Int? = null,
    @SerialName("outputHeight") val outputHeight: Int? = null,
    @SerialName("projectId") val projectId: String? = null,
)

@Serializable
data class QuickCreationTaskPageRequestDto(
    @SerialName("page") val page: Int = 1,
    @SerialName("size") val size: Int = 10,
)

@Serializable
data class QuickCreationTaskPageDto(
    @SerialName("page") val page: JsonElement? = null,
    @SerialName("current") val current: JsonElement? = null,
    @SerialName("size") val size: JsonElement? = null,
    @SerialName("total") val total: JsonElement? = null,
    @SerialName("pages") val pages: JsonElement? = null,
    @SerialName("hasNext") val hasNext: Boolean = false,
    @SerialName("hasPrevious") val hasPrevious: Boolean = false,
    @SerialName("nextCursor") val nextCursor: String? = null,
    @SerialName("list") val list: List<QuickCreationTaskRecordDto> = emptyList(),
    @SerialName("records") val records: List<QuickCreationTaskRecordDto> = emptyList(),
)

@Serializable
data class QuickCreationTaskRecordDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("taskStatus") val taskStatus: String,
    @SerialName("taskCostTime") val taskCostTime: String? = null,
    @SerialName("taskType") val taskType: String? = null,
    @SerialName("skuId") val skuId: String? = null,
    @SerialName("bindingId") val bindingId: String? = null,
    @SerialName("bindingCategoryId") val bindingCategoryId: String? = null,
    @SerialName("apiRequestParams") val apiRequestParams: String? = null,
    @SerialName("prepayRecord") val prepayRecord: QuickCreationPrepayRecordDto? = null,
    @SerialName("outputList") val outputList: List<QuickCreationOutputDto> = emptyList(),
) {
    val isRunning: Boolean
        get() = taskStatus == "QUEUED" || taskStatus == "RUNNING" || taskStatus == "PROCESSING"
}

@Serializable
data class QuickCreationPrepayRecordDto(
    @SerialName("settlementMode") val settlementMode: String? = null,
    @SerialName("rhAmount") val rhAmount: Double = 0.0,
    @SerialName("cashAmount") val cashAmount: Double = 0.0,
    @SerialName("cashCurrency") val cashCurrency: String? = null,
    @SerialName("isFree") val isFree: Int = 0,
    @SerialName("status") val status: String? = null,
)

@Serializable
data class QuickCreationOutputDto(
    @SerialName("id") val id: String,
    @SerialName("outputName") val outputName: String? = null,
    @SerialName("outputType") val outputType: String? = null,
    @SerialName("fileUrl") val fileUrl: String,
    @SerialName("filePreviewUrl") val filePreviewUrl: String? = null,
    @SerialName("outputSize") val outputSize: String? = null,
    @SerialName("auditStatus") val auditStatus: Int? = null,
    @SerialName("expireTime") val expireTime: String? = null,
    @SerialName("expireDays") val expireDays: String? = null,
)

@Serializable
data class QuickCreationTaskDetailRequestDto(
    @SerialName("outputId") val outputId: String,
)

@Serializable
data class QuickCreationTaskCancelRequestDto(
    @SerialName("taskId") val taskId: String,
)

@Serializable
data class QuickCreationProjectPageRequestDto(
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
)

@Serializable
data class QuickCreationProjectTasksRequestDto(
    @SerialName("projectId") val projectId: String,
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
)

@Serializable
data class QuickCreationProjectCreateRequestDto(
    @SerialName("name") val name: String,
)

@Serializable
data class QuickCreationProjectRenameRequestDto(
    @SerialName("projectId") val projectId: String,
    @SerialName("name") val name: String,
)

@Serializable
data class QuickCreationProjectIdRequestDto(
    @SerialName("projectId") val projectId: String,
)

@Serializable
data class QuickCreationProjectPinRequestDto(
    @SerialName("projectId") val projectId: String,
    @SerialName("pinned") val pinned: Boolean,
)

@Serializable
data class QuickCreationProjectPageDto(
    @SerialName("records") val records: List<QuickCreationProjectDto> = emptyList(),
    @SerialName("size") val size: JsonElement? = null,
    @SerialName("current") val current: JsonElement? = null,
    @SerialName("total") val total: JsonElement? = null,
    @SerialName("pages") val pages: JsonElement? = null,
    @SerialName("hasNext") val hasNext: Boolean = false,
    @SerialName("hasPrevious") val hasPrevious: Boolean = false,
    @SerialName("nextCursor") val nextCursor: String? = null,
)

@Serializable
data class QuickCreationProjectDto(
    @SerialName("projectId") val projectId: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("projectName") val projectName: String? = null,
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("taskCount") val taskCount: Int = 0,
    @SerialName("pin") val pin: Boolean = false,
    @SerialName("pinned") val pinned: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("createTime") val createTime: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("updateTime") val updateTime: String? = null,
)

@Serializable
data class QuickCreationInspirationTemplatePageRequestDto(
    @SerialName("page") val page: Int = 1,
    @SerialName("size") val size: Int = 20,
    @SerialName("tagId") val tagId: String? = null,
)

@Serializable
data class QuickCreationInspirationTemplatePageDto(
    @SerialName("page") val page: Int = 1,
    @SerialName("size") val size: Int = 20,
    @SerialName("total") val total: Int = 0,
    @SerialName("list") val list: List<QuickCreationInspirationTemplateDto> = emptyList(),
)

@Serializable
data class QuickCreationInspirationTemplateDto(
    @SerialName("templateId") val templateId: String,
    @SerialName("nameCn") val nameCn: String? = null,
    @SerialName("nameAi") val nameAi: String? = null,
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("tagHot") val tagHot: Boolean = false,
    @SerialName("tagNew") val tagNew: Boolean = false,
    @SerialName("sortOrder") val sortOrder: Int? = null,
)

@Serializable
data class QuickCreationInspirationTemplateDetailRequestDto(
    @SerialName("templateId") val templateId: String,
)

@Serializable
data class QuickCreationInspirationTemplateDetailDto(
    @SerialName("templateId") val templateId: String,
    @SerialName("nameCn") val nameCn: String? = null,
    @SerialName("nameAi") val nameAi: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("descriptionAi") val descriptionAi: String? = null,
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("snapshot") val snapshot: QuickCreationInspirationTemplateSnapshotDto? = null,
    @SerialName("bindingId") val bindingId: String? = null,
    @SerialName("bindingCnName") val bindingCnName: String? = null,
    @SerialName("bindingAiName") val bindingAiName: String? = null,
    @SerialName("apiRequestParamsRaw") val apiRequestParamsRaw: String? = null,
    @SerialName("skuId") val skuId: String? = null,
)

@Serializable
data class QuickCreationInspirationTemplateSnapshotDto(
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("taskSite") val taskSite: String? = null,
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("presetParams") val presetParams: JsonElement,
    @SerialName("sourceTaskId") val sourceTaskId: String? = null,
    @SerialName("snapshotAt") val snapshotAt: String? = null,
)
