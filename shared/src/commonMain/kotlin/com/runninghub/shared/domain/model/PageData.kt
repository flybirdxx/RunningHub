package com.runninghub.shared.domain.model

data class PageData<T>(
    val records: List<T>,
    val total: Int,
    val size: Int,
    val current: Int,
    val hasNext: Boolean = false
)
