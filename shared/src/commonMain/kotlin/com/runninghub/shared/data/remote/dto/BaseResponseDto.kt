package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseResponseDto<T>(
    @SerialName("code") val code: Int,
    @SerialName("msg") val msg: String = "",
    @SerialName("data") val data: T
)

@Serializable
data class PageDataDto<T>(
    @SerialName("records") val records: List<T>,
    @SerialName("total") val total: Int = 0,
    @SerialName("size") val size: Int = 0,
    @SerialName("current") val current: Int = 0,
    @SerialName("hasNext") val hasNext: Boolean = false,
    @SerialName("hasPrevious") val hasPrevious: Boolean = false
)
