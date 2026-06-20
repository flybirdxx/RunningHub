package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.model.ApiModelField
import com.runninghub.shared.domain.model.ModelFieldValue

class ModelInvocationRequestBuilder {
    fun build(
        fields: List<ApiModelField>,
        values: Map<String, ModelFieldValue>,
        webhookUrl: String? = null,
    ): Map<String, Any> {
        val body = linkedMapOf<String, Any>()
        fields.filter { it.visible }.forEach { field ->
            val value = values[field.paramKey] ?: values[field.fieldKey] ?: return@forEach
            body[field.paramKey] = value.toBodyValue()
        }
        if (!webhookUrl.isNullOrBlank()) {
            body["webhookUrl"] = webhookUrl
        }
        return body
    }

    private fun ModelFieldValue.toBodyValue(): Any = when (this) {
        is ModelFieldValue.Text -> value
        is ModelFieldValue.NumberValue -> value
        is ModelFieldValue.BooleanValue -> value
        is ModelFieldValue.StringList -> values
    }
}
