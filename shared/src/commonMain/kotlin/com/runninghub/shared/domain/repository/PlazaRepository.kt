package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.PlazaCreationPage
import com.runninghub.shared.domain.model.PlazaShortCard
import com.runninghub.shared.domain.model.PlazaShortCategory
import com.runninghub.shared.domain.model.PlazaTag

interface PlazaRepository {
    suspend fun getTags(): Result<List<PlazaTag>>

    suspend fun listCreations(
        page: Int = 1,
        size: Int = 30,
        sort: String = "RECOMMEND",
        tags: List<String> = emptyList(),
    ): Result<PlazaCreationPage>

    suspend fun listShortCategories(): Result<List<PlazaShortCategory>>

    suspend fun listShorts(
        page: Int = 1,
        size: Int = 30,
        categoryCode: String? = null,
    ): Result<List<PlazaShortCard>>
}
