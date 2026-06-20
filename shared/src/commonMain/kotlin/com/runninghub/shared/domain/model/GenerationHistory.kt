package com.runninghub.shared.domain.model

data class GenerationHistoryPage(
    val page: Int,
    val size: Int,
    val total: Int,
    val items: List<GenerationHistoryItem>,
)

data class GenerationHistoryItem(
    val taskId: String,
    val source: GenerationHistorySource,
    val status: String,
    val modelId: String? = null,
    val taskType: String? = null,
    val costAmount: Double = 0.0,
    val costCurrency: String? = null,
    val costTime: String? = null,
    val params: Map<String, String> = emptyMap(),
    val outputs: List<GenerationHistoryOutput> = emptyList(),
)

data class GenerationHistoryOutput(
    val outputId: String,
    val url: String,
    val type: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val outputName: String? = null,
    val expireTime: String? = null,
    val expireDays: String? = null,
) {
    val isImage: Boolean
        get() = type.lowercase() in setOf("png", "jpg", "jpeg", "webp", "image")

    val isVideo: Boolean
        get() = type.lowercase() in setOf("mp4", "webm", "mov", "video")
}

enum class GenerationHistorySource(val key: String) {
    QUICK_CREATION("quick_creation"),
    STANDARD_MODEL("standard_model"),
}
