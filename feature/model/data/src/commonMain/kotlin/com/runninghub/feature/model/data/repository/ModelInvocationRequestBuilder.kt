package com.runninghub.feature.model.data.repository

import com.runninghub.feature.model.domain.ApiModelField
import com.runninghub.feature.model.domain.ModelFieldValue

/**
 * 构造标准模型 OpenAPI 请求体。
 *
 * 本类只处理 Domain 字段值到远端请求参数的映射，不读取凭据、不访问网络，也不保存状态。
 * 字段配置中的 [ApiModelField.paramKey] 是服务端真正接收的参数名，Presentation 只需要维护
 * 领域字段值。
 */
class ModelInvocationRequestBuilder {
    /**
     * 根据字段配置和用户输入构造请求体。
     *
     * @param fields SKU 详情解析得到的可输入字段，顺序来自服务端配置。
     * @param values 页面或调用方提交的字段值，key 可以是 paramKey 或 fieldKey。
     * @param webhookUrl 可选回调地址；`null` 或空白字符串表示本次调用不配置回调。
     * @return 可直接序列化为 JSON 的请求体；不可见字段和未填写字段不会写入请求体。
     */
    fun build(
        fields: List<ApiModelField>,
        values: Map<String, ModelFieldValue>,
        webhookUrl: String? = null,
    ): Map<String, Any> {
        val body = linkedMapOf<String, Any>()
        fields.filter { it.visible }.forEach { field ->
            val value = values[field.paramKey] ?: values[field.fieldKey] ?: return@forEach
            // 请求体必须使用服务端参数名 paramKey；fieldKey 只作为客户端状态和兼容旧数据的查找入口。
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
