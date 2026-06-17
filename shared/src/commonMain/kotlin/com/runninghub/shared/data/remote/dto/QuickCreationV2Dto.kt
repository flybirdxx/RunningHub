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
data class QuickCreationModelDto(
    @SerialName("type") val type: String? = null,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("bindingId") val bindingId: String? = null,
    @SerialName("skuId") val skuId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("nameCn") val nameCn: String? = null,
    @SerialName("nameAi") val nameAi: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("fields") val fields: List<QuickCreationFieldDto> = emptyList(),
    @SerialName("children") val children: List<QuickCreationModelDto> = emptyList(),
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
    @SerialName("page") val page: Int = 1,
    @SerialName("size") val size: Int = 10,
    @SerialName("total") val total: Int = 0,
    @SerialName("list") val list: List<QuickCreationTaskRecordDto> = emptyList(),
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
