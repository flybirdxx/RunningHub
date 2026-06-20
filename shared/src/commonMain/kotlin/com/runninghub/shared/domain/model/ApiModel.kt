package com.runninghub.shared.domain.model

data class ApiModelSummary(
    val id: String,
    val name: String,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val endpoint: String? = null,
    val priceSummary: String? = null,
    val requiredFields: List<String> = emptyList(),
    val optionalFields: List<String> = emptyList(),
)

data class ApiModelDetail(
    val id: String,
    val name: String,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val endpoint: String,
    val priceSummary: String? = null,
    val queueSize: Int? = null,
    val concurrencyLimit: Int? = null,
    val fields: List<ApiModelField> = emptyList(),
    val rawInputConfigJson: String? = null,
)

data class ApiModelField(
    val fieldKey: String,
    val paramKey: String,
    val type: ApiModelFieldType,
    val required: Boolean,
    val title: String? = null,
    val description: String? = null,
    val placeholder: String? = null,
    val defaultValue: String? = null,
    val options: List<ApiModelFieldOption> = emptyList(),
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val precision: Int? = null,
    val multipleInputs: Boolean = false,
    val maxInputCount: Int? = null,
    val maxUploadCount: Int? = null,
    val maxUploadSizeBytes: Long? = null,
    val acceptFormats: List<String> = emptyList(),
    val visible: Boolean = true,
)

data class ApiModelFieldOption(
    val label: String,
    val value: String,
)

enum class ApiModelFieldType {
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    LIST,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    MODEL,
    UNKNOWN,
}

data class LlmModelSummary(
    val modelKey: String,
    val provider: String,
    val version: String? = null,
    val contextLength: Int? = null,
    val capabilities: List<String> = emptyList(),
    val inputPrice: String? = null,
    val outputPrice: String? = null,
)
