package com.runninghub.core.model

/**
 * 跨 Feature 复用的分页结果。
 *
 * 页码采用服务端返回语义，不在 core 层假设从 0 还是从 1 开始。调用方应使用 [hasNext]
 * 判断是否继续加载，避免不同接口的页码规则污染 Presentation 状态机。
 *
 * @param T 分页记录的业务模型类型。
 * @property records 当前页记录，顺序保留服务端排序。
 * @property total 服务端声明的总记录数。
 * @property size 当前请求或响应页大小。
 * @property current 当前页码，具体基准由对应 Repository 契约说明。
 * @property hasNext 是否还有下一页；当服务端未显式返回时由 Data 层按业务规则推导。
 */
data class PageData<T>(
    val records: List<T>,
    val total: Int,
    val size: Int,
    val current: Int,
    val hasNext: Boolean = false
)
