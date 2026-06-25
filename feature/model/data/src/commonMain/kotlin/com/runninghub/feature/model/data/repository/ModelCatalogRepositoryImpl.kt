package com.runninghub.feature.model.data.repository

import com.runninghub.core.storage.ModelCatalogCacheStore
import com.runninghub.feature.model.data.remote.api.ModelCatalogApi
import com.runninghub.feature.model.data.remote.dto.LlmModelDto
import com.runninghub.feature.model.data.remote.dto.SkuDetailDto
import com.runninghub.feature.model.data.remote.dto.SkuListRequestDto
import com.runninghub.feature.model.data.remote.dto.SkuSummaryDto
import com.runninghub.feature.model.data.remote.dto.SkuTagDto
import com.runninghub.feature.model.domain.ApiModelDetail
import com.runninghub.feature.model.domain.ApiModelField
import com.runninghub.feature.model.domain.ApiModelFieldOption
import com.runninghub.feature.model.domain.ApiModelFieldType
import com.runninghub.feature.model.domain.ApiModelGroup
import com.runninghub.feature.model.domain.ApiModelSummary
import com.runninghub.feature.model.domain.LlmModelSummary
import com.runninghub.feature.model.domain.ModelCatalogException
import com.runninghub.feature.model.domain.ModelCatalogIssue
import com.runninghub.feature.model.domain.ModelCatalogRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val skuPriceJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

private const val STANDARD_MODEL_LIST_CACHE_SCHEMA_VERSION = 3
private const val STANDARD_MODEL_GROUP_CACHE_SCHEMA_VERSION = 1

/**
 * 标准模型目录仓库的数据层实现。
 *
 * 本类负责调用 SKU/LLM 目录 API，并把 DTO 映射为不含 endpoint 的领域模型。
 * SKU endpoint 会登记到 [ModelEndpointRegistry]，供模型调用仓库在提交任务时按 modelId 解析，
 * 避免远端路由继续泄漏到 Domain 或 Presentation。
 *
 * @param api 标准模型目录 API。
 * @param json 解析服务端 inputConfig JSON 的序列化配置。
 * @param endpointRegistry 标准模型 endpoint 的 Data 层缓存，需与调用仓库共享同一个实例。
 * @param cacheStore 标准模型目录本地缓存端口；`null` 仅用于旧测试或无本地存储的降级环境。
 */
class ModelCatalogRepositoryImpl(
    private val api: ModelCatalogApi,
    private val json: Json,
    private val endpointRegistry: ModelEndpointRegistry = ModelEndpointRegistry(),
    private val cacheStore: ModelCatalogCacheStore? = null,
) : ModelCatalogRepository {
    private val fieldMapper = ApiModelFieldMapper(json)
    private val cacheMutex = Mutex()
    private val standardGroupCache = mutableMapOf<String, List<ApiModelGroup>>()
    private val standardListCache = mutableMapOf<StandardListCacheKey, List<ApiModelSummary>>()
    private val standardDetailCache = mutableMapOf<String, ApiModelDetail>()

    override suspend fun listStandardModelGroups(search: String): Result<List<ApiModelGroup>> =
        runCatching {
            cacheMutex.withLock {
                standardGroupCache[search]?.let { return@runCatching it }
            }
            val response = api.listStandardModelGroups(search = search)
            if (response.code != 0) {
                // 分组接口失败时只抛稳定错误语义，上层可回落到旧的全量 SKU 列表。
                throw ModelCatalogException(ModelCatalogIssue.StandardGroupLoadFailed, response.code)
            }
            val groups = response.data.orEmpty()
                .map { it.toDomain() }
                .filter { it.id.isNotBlank() && it.name.isNotBlank() && it.apiCount > 0 }
            cacheMutex.withLock {
                standardGroupCache[search] = groups
            }
            cacheStore?.saveStandardModelGroups(standardGroupCacheKey(search), encodeStandardGroupCache(groups))
            groups
        }

    override suspend fun getCachedStandardModelGroups(search: String): List<ApiModelGroup> {
        cacheMutex.withLock {
            standardGroupCache[search]?.let { return it }
        }
        val cached = cacheStore
            ?.getStandardModelGroups(standardGroupCacheKey(search))
            ?.let(::decodeStandardGroupCache)
            .orEmpty()
        if (cached.isNotEmpty()) {
            cacheMutex.withLock {
                standardGroupCache[search] = cached
            }
        }
        return cached
    }

    override suspend fun listStandardModels(search: String, page: Int, size: Int): Result<List<ApiModelSummary>> =
        runCatching {
            getCachedStandardModels(search = search, page = page, size = size).takeIf { it.isNotEmpty() }
                ?: refreshStandardModels(search = search, page = page, size = size).getOrThrow()
        }

    override suspend fun getCachedStandardModels(search: String, page: Int, size: Int): List<ApiModelSummary> {
        val cacheKey = StandardListCacheKey(search = search, page = page, size = size)
        return getCachedStandardModels(cacheKey)
    }

    private suspend fun getCachedStandardModels(cacheKey: StandardListCacheKey): List<ApiModelSummary> {
        cacheMutex.withLock {
            standardListCache[cacheKey]?.let { return it }
        }
        val cached = cacheStore
            ?.getStandardModelList(cacheKey.storeKey())
            ?.let(::decodeStandardListCache)
            .orEmpty()
        if (cached.isNotEmpty()) {
            cacheMutex.withLock {
                standardListCache[cacheKey] = cached
            }
        }
        return cached
    }

    override suspend fun refreshStandardModels(search: String, page: Int, size: Int): Result<List<ApiModelSummary>> =
        runCatching {
            val cacheKey = StandardListCacheKey(search = search, page = page, size = size)
            loadStandardModelsFromRemote(
                request = SkuListRequestDto(search = search, pageNum = page, pageSize = size),
                cacheKey = cacheKey,
            )
        }

    override suspend fun listStandardModelsByGroup(
        group: ApiModelGroup,
        search: String,
        page: Int,
        size: Int,
    ): Result<List<ApiModelSummary>> =
        runCatching {
            val cacheKey = StandardListCacheKey(
                search = search,
                page = page,
                size = size,
                categoryTagIds = listOf(group.id),
            )
            getCachedStandardModels(cacheKey).takeIf { it.isNotEmpty() }
                ?: loadStandardModelsFromRemote(
                    request = SkuListRequestDto(
                        search = search,
                        pageNum = page,
                        pageSize = size,
                        categoryTagIds = listOf(group.id),
                    ),
                    cacheKey = cacheKey,
                    groupNameOverride = group.name,
                )
        }

    override suspend fun getCachedStandardModelsByGroup(
        group: ApiModelGroup,
        search: String,
        page: Int,
        size: Int,
    ): List<ApiModelSummary> =
        getCachedStandardModels(
            StandardListCacheKey(
                search = search,
                page = page,
                size = size,
                categoryTagIds = listOf(group.id),
            )
        )

    override suspend fun refreshStandardModelsByGroup(
        group: ApiModelGroup,
        search: String,
        page: Int,
        size: Int,
    ): Result<List<ApiModelSummary>> =
        runCatching {
            val cacheKey = StandardListCacheKey(
                search = search,
                page = page,
                size = size,
                categoryTagIds = listOf(group.id),
            )
            loadStandardModelsFromRemote(
                request = SkuListRequestDto(
                    search = search,
                    pageNum = page,
                    pageSize = size,
                    categoryTagIds = listOf(group.id),
                ),
                cacheKey = cacheKey,
                groupNameOverride = group.name,
            )
        }

    override suspend fun getStandardModelDetail(modelId: String): Result<ApiModelDetail> =
        runCatching {
            cacheMutex.withLock {
                standardDetailCache[modelId]?.let { return@runCatching it }
            }
            cacheStore
                ?.getStandardModelDetail(modelId)
                ?.let(::decodeStandardDetailCache)
                ?.also { cached ->
                    cacheMutex.withLock {
                        standardDetailCache[modelId] = cached
                        standardDetailCache[cached.id] = cached
                    }
                    return@runCatching cached
                }
            val response = api.getStandardModelDetail(modelId)
            // 详情加载失败同样不透传服务端 msg，避免 UI 文案被后端临时诊断信息污染。
            if (response.code != 0) {
                throw ModelCatalogException(ModelCatalogIssue.StandardDetailLoadFailed, response.code)
            }
            val detail = response.data
                ?: throw ModelCatalogException(ModelCatalogIssue.StandardDetailMissing)
            endpointRegistry.register(detail.id, detail.rhEndpoint)
            val model = detail.toDomain(fieldMapper)
            cacheMutex.withLock {
                standardDetailCache[modelId] = model
                standardDetailCache[model.id] = model
            }
            val cacheJson = encodeStandardDetailCache(model)
            cacheStore?.saveStandardModelDetail(modelId, cacheJson)
            cacheStore?.saveStandardModelDetail(model.id, cacheJson)
            model
        }

    override suspend fun listLlmModels(): Result<List<LlmModelSummary>> =
        runCatching {
            val response = api.listLlmModels()
            // LLM 目录与标准模型目录使用相同错误边界：记录稳定语义，不返回远端 msg。
            if (response.code != 0) {
                throw ModelCatalogException(ModelCatalogIssue.LlmListLoadFailed, response.code)
            }
            response.data.orEmpty().map { it.toDomain() }
        }

    private suspend fun loadStandardModelsFromRemote(
        request: SkuListRequestDto,
        cacheKey: StandardListCacheKey,
        groupNameOverride: String? = null,
    ): List<ApiModelSummary> {
        val response = api.listStandardModels(request)
        if (response.code != 0) {
            // Data 层只保留稳定错误语义和服务端 code，避免把远端 msg 直接暴露给 Presentation。
            throw ModelCatalogException(ModelCatalogIssue.StandardListLoadFailed, response.code)
        }
        val models = response.data?.items.orEmpty().map { dto ->
            endpointRegistry.register(dto.id, dto.rhEndpoint)
            dto.toDomain(groupNameOverride = groupNameOverride)
        }
        cacheMutex.withLock {
            standardListCache[cacheKey] = models
        }
        cacheStore?.saveStandardModelList(cacheKey.storeKey(), encodeStandardListCache(models))
        return models
    }

    private fun decodeStandardListCache(cacheJson: String): List<ApiModelSummary> =
        runCatching {
            val cache = json.decodeFromString<StandardModelListCacheDto>(cacheJson)
            if (cache.schemaVersion == STANDARD_MODEL_LIST_CACHE_SCHEMA_VERSION) {
                cache.models.map { it.toDomain() }
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())

    private fun decodeStandardGroupCache(cacheJson: String): List<ApiModelGroup> =
        runCatching {
            val cache = json.decodeFromString<StandardModelGroupListCacheDto>(cacheJson)
            if (cache.schemaVersion == STANDARD_MODEL_GROUP_CACHE_SCHEMA_VERSION) {
                cache.groups.map { it.toDomain() }
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())

    private fun encodeStandardListCache(models: List<ApiModelSummary>): String =
        json.encodeToString(
            StandardModelListCacheDto(
                schemaVersion = STANDARD_MODEL_LIST_CACHE_SCHEMA_VERSION,
                models = models.map { it.toCacheDto() },
            )
        )

    private fun encodeStandardGroupCache(groups: List<ApiModelGroup>): String =
        json.encodeToString(
            StandardModelGroupListCacheDto(
                schemaVersion = STANDARD_MODEL_GROUP_CACHE_SCHEMA_VERSION,
                groups = groups.map { it.toCacheDto() },
            )
        )

    private fun decodeStandardDetailCache(cacheJson: String): ApiModelDetail? =
        runCatching {
            json.decodeFromString<StandardModelDetailCacheDto>(cacheJson).toDomain()
        }.getOrNull()

    private fun encodeStandardDetailCache(model: ApiModelDetail): String =
        json.encodeToString(model.toCacheDto())
}

private fun standardGroupCacheKey(search: String): String =
    "s${search.hashCode()}"

/**
 * 标准模型列表缓存键。
 *
 * `/api/sku/list` 的返回受搜索词、页码、页大小和分组标签影响，这些字段共同决定可复用范围。
 * 缓存值只保存已脱敏的 Domain 摘要，不包含原始响应、Cookie、API Key 或请求头。
 *
 * @property search 用户搜索词；空字符串表示默认标准模型目录。
 * @property page 页码，从 1 开始；调用方不应传入小于 1 的值。
 * @property size 每页数量，单位为条；调用方不应传入小于 1 的值。
 * @property categoryTagIds 服务端模型分组标签 ID；空集合表示全量目录，非空表示某个页面分组。
 */
private data class StandardListCacheKey(
    val search: String,
    val page: Int,
    val size: Int,
    val categoryTagIds: List<String> = emptyList(),
) {
    fun storeKey(): String =
        "s${search.hashCode()}_p${page}_n${size}_c${categoryTagIds.joinToString("|").hashCode()}"
}

@Serializable
private data class StandardModelListCacheDto(
    /**
     * 标准模型列表缓存结构版本；缺失或不匹配时视为旧缓存，调用方会回源刷新。
     */
    val schemaVersion: Int,
    val models: List<StandardModelSummaryCacheDto>,
)

@Serializable
private data class StandardModelGroupListCacheDto(
    /**
     * 标准模型分组缓存结构版本；缺失或不匹配时视为旧缓存，调用方会回源刷新。
     */
    val schemaVersion: Int,
    val groups: List<StandardModelGroupCacheDto>,
)

@Serializable
private data class StandardModelGroupCacheDto(
    val id: String,
    val name: String,
    val nameEn: String? = null,
    val apiCount: Int = 0,
) {
    fun toDomain(): ApiModelGroup =
        ApiModelGroup(
            id = id,
            name = name,
            nameEn = nameEn,
            apiCount = apiCount,
        )
}

@Serializable
private data class StandardModelSummaryCacheDto(
    val id: String,
    val name: String,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val priceSummary: String? = null,
    val requiredFields: List<String> = emptyList(),
    val optionalFields: List<String> = emptyList(),
) {
    fun toDomain(): ApiModelSummary =
        ApiModelSummary(
            id = id,
            name = name,
            type = type,
            groupName = groupName,
            source = source,
            priceSummary = priceSummary,
            requiredFields = requiredFields,
            optionalFields = optionalFields,
        )
}

@Serializable
private data class StandardModelDetailCacheDto(
    val id: String,
    val name: String,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val priceSummary: String? = null,
    val queueSize: Int? = null,
    val concurrencyLimit: Int? = null,
    val fields: List<StandardModelFieldCacheDto> = emptyList(),
) {
    fun toDomain(): ApiModelDetail =
        ApiModelDetail(
            id = id,
            name = name,
            type = type,
            groupName = groupName,
            source = source,
            priceSummary = priceSummary,
            queueSize = queueSize,
            concurrencyLimit = concurrencyLimit,
            fields = fields.map { it.toDomain() },
        )
}

@Serializable
private data class StandardModelFieldCacheDto(
    val fieldKey: String,
    val paramKey: String,
    val type: String,
    val required: Boolean,
    val title: String? = null,
    val description: String? = null,
    val placeholder: String? = null,
    val defaultValue: String? = null,
    val options: List<StandardModelFieldOptionCacheDto> = emptyList(),
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val precision: Int? = null,
    val multipleInputs: Boolean = false,
    val maxInputCount: Int? = null,
    val maxUploadCount: Int? = null,
    val maxUploadSizeBytes: Long? = null,
    val acceptFormats: List<String> = emptyList(),
    val visible: Boolean = true,
    val rawConfigJson: String? = null,
) {
    fun toDomain(): ApiModelField =
        ApiModelField(
            fieldKey = fieldKey,
            paramKey = paramKey,
            type = runCatching { ApiModelFieldType.valueOf(type) }.getOrDefault(ApiModelFieldType.UNKNOWN),
            required = required,
            title = title,
            description = description,
            placeholder = placeholder,
            defaultValue = defaultValue,
            options = options.map { it.toDomain() },
            minLength = minLength,
            maxLength = maxLength,
            min = min,
            max = max,
            step = step,
            precision = precision,
            multipleInputs = multipleInputs,
            maxInputCount = maxInputCount,
            maxUploadCount = maxUploadCount,
            maxUploadSizeBytes = maxUploadSizeBytes,
            acceptFormats = acceptFormats,
            visible = visible,
            rawConfigJson = rawConfigJson,
        )
}

@Serializable
private data class StandardModelFieldOptionCacheDto(
    val label: String,
    val value: String,
) {
    fun toDomain(): ApiModelFieldOption =
        ApiModelFieldOption(label = label, value = value)
}

private fun ApiModelSummary.toCacheDto(): StandardModelSummaryCacheDto =
    StandardModelSummaryCacheDto(
        id = id,
        name = name,
        type = type,
        groupName = groupName,
        source = source,
        priceSummary = priceSummary,
        requiredFields = requiredFields,
        optionalFields = optionalFields,
    )

private fun ApiModelGroup.toCacheDto(): StandardModelGroupCacheDto =
    StandardModelGroupCacheDto(
        id = id,
        name = name,
        nameEn = nameEn,
        apiCount = apiCount,
    )

private fun ApiModelDetail.toCacheDto(): StandardModelDetailCacheDto =
    StandardModelDetailCacheDto(
        id = id,
        name = name,
        type = type,
        groupName = groupName,
        source = source,
        priceSummary = priceSummary,
        queueSize = queueSize,
        concurrencyLimit = concurrencyLimit,
        fields = fields.map { it.toCacheDto() },
    )

private fun ApiModelField.toCacheDto(): StandardModelFieldCacheDto =
    StandardModelFieldCacheDto(
        fieldKey = fieldKey,
        paramKey = paramKey,
        type = type.name,
        required = required,
        title = title,
        description = description,
        placeholder = placeholder,
        defaultValue = defaultValue,
        options = options.map { it.toCacheDto() },
        minLength = minLength,
        maxLength = maxLength,
        min = min,
        max = max,
        step = step,
        precision = precision,
        multipleInputs = multipleInputs,
        maxInputCount = maxInputCount,
        maxUploadCount = maxUploadCount,
        maxUploadSizeBytes = maxUploadSizeBytes,
        acceptFormats = acceptFormats,
        visible = visible,
        rawConfigJson = rawConfigJson,
    )

private fun ApiModelFieldOption.toCacheDto(): StandardModelFieldOptionCacheDto =
    StandardModelFieldOptionCacheDto(label = label, value = value)

/**
 * 把 SKU 列表 DTO 映射为领域摘要。
 *
 * endpoint 不进入返回值；调用方需要提交任务时只能使用 id，由 Data 层再解析实际路由。
 *
 * @param groupNameOverride 按服务端分组标签查询时传入的分组名；非空时优先作为目录归属，
 * 避免依赖列表项标签猜测 Seedance、Suno 等模型族。
 */
fun SkuSummaryDto.toDomain(groupNameOverride: String? = null): ApiModelSummary =
    normalizedTags().let { normalizedTags ->
        ApiModelSummary(
            id = id,
            name = name,
            type = type.takeUnlessBlank() ?: normalizedTags.capabilityTag(),
            groupName = groupNameOverride.takeUnlessBlank()
                ?: groupName.takeUnlessBlank()
                ?: normalizedTags.groupLabel(),
            source = source.takeUnlessBlank() ?: normalizedTags.sourceTag(),
            priceSummary = priceSummary?.takeIf { it.isNotBlank() } ?: price.toSkuPriceSummary(),
        )
    }

/**
 * 把标准模型分组标签 DTO 映射为领域分组。
 *
 * 标签 ID 会作为后续 `categoryTagIds` 请求条件，名称只用于展示和分组归属，不参与接口路由。
 */
fun SkuTagDto.toDomain(): ApiModelGroup =
    ApiModelGroup(
        id = id,
        name = name,
        nameEn = nameEn,
        apiCount = apiCount ?: 0,
    )

/**
 * 把 SKU 详情 DTO 映射为领域详情。
 *
 * inputConfigJson 在 Data 层解析为结构化字段，同时字段级原始 JSON 会被保留在脱敏 Domain 快照中，
 * 供后续动态 UI 继续增量支持子字段和条件字段。
 */
fun SkuDetailDto.toDomain(fieldMapper: ApiModelFieldMapper): ApiModelDetail =
    normalizedTags().let { normalizedTags ->
        ApiModelDetail(
            id = id,
            name = name,
            type = type.takeUnlessBlank() ?: normalizedTags.capabilityTag(),
            groupName = groupName.takeUnlessBlank() ?: normalizedTags.groupLabel(),
            source = source.takeUnlessBlank() ?: normalizedTags.sourceTag(),
            priceSummary = priceSummary?.takeIf { it.isNotBlank() } ?: price.toSkuPriceSummary(),
            queueSize = queueSize,
            concurrencyLimit = concurrencyLimit,
            fields = fieldMapper.parse(inputConfigJson),
        )
    }

private fun SkuSummaryDto.normalizedTags(): List<String> =
    tags.normalizedSkuTags()

private fun SkuDetailDto.normalizedTags(): List<String> =
    tags.normalizedSkuTags()

private fun List<String>.normalizedSkuTags(): List<String> =
    flatMap { tag -> tag.split("|", ",") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }

private fun List<String>.capabilityTag(): String? =
    firstOrNull { tag ->
        val lower = tag.lowercase()
        lower.contains("-to-") ||
            lower.contains("video") ||
            lower.contains("audio") ||
            lower.contains("music") ||
            lower.contains("3d") ||
            lower.contains("image")
    }

private fun List<String>.sourceTag(): String? =
    getOrNull(1)?.takeUnless { it.isRecentTag() }

private fun List<String>.groupLabel(): String? =
    drop(2)
        .filterNot { it.isRecentTag() }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
        ?: drop(1).firstOrNull { !it.isRecentTag() }

private fun String?.takeUnlessBlank(): String? =
    this?.takeIf { it.isNotBlank() }

private fun String.isRecentTag(): Boolean =
    contains("最近上新", ignoreCase = true) ||
        equals("recent", ignoreCase = true) ||
        equals("new", ignoreCase = true)

private fun String.toSkuPriceSummary(): String? {
    val raw = takeIf { it.isNotBlank() } ?: return null
    val objectValue = runCatching { skuPriceJson.parseToJsonElement(raw) as? JsonObject }.getOrNull()
        ?: return raw
    val price = objectValue.string("price")?.takeIf { it.isNotBlank() } ?: return null
    val currency = objectValue.string("currency")
    val unitName = objectValue.string("unitName") ?: objectValue.string("unitNameEn")
    val amountText = if (currency.equals("CNY", ignoreCase = true)) {
        "¥$price"
    } else {
        listOfNotNull(price, currency).joinToString(" ")
    }
    return unitName?.takeIf { it.isNotBlank() }?.let { "$amountText/$it" } ?: amountText
}

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

/**
 * 把 LLM 目录 DTO 映射为领域摘要。
 */
fun LlmModelDto.toDomain(): LlmModelSummary =
    LlmModelSummary(
        modelKey = modelKey,
        provider = provider,
        version = version,
        contextLength = contextLength,
        capabilities = capabilities,
        inputPrice = inputPrice,
        outputPrice = outputPrice,
    )
