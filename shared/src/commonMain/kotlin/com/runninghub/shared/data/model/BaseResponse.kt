package com.runninghub.shared.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val code: Int,
    val msg: String,
    val data: T? = null
)

@Serializable
data class PageData<T>(
    val records: List<T>,
    val total: Int,
    val size: Int,
    val current: Int
)
