package com.runninghub.shared.domain.model

sealed interface ModelFieldValue {
    data class Text(val value: String) : ModelFieldValue
    data class NumberValue(val value: Double) : ModelFieldValue
    data class BooleanValue(val value: Boolean) : ModelFieldValue
    data class StringList(val values: List<String>) : ModelFieldValue
}

data class ModelInvocationRequest(
    val modelId: String,
    val endpoint: String,
    val fields: List<ApiModelField>,
    val values: Map<String, ModelFieldValue>,
    val webhookUrl: String? = null,
)

data class ModelInvocationTask(
    val taskId: String,
    val status: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val resultUrls: List<String> = emptyList(),
)
