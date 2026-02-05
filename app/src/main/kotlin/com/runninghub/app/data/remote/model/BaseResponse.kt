package com.runninghub.app.data.remote.model

/**
 * [INPUT]: 任意数据类型 T
 * [OUTPUT]: 统一的 API 响应包装
 * [POS]: 数据层的原子定义，确保网络响应的一致性
 */
data class BaseResponse<T>(
    val code: Int,
    val msg: String,
    val data: T
)

data class PageData<T>(
    val records: List<T>,
    val total: Int,
    val size: Int,
    val current: Int
)
