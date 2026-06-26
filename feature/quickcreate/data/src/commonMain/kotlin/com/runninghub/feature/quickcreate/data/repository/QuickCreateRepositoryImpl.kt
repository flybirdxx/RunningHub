package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.core.storage.ModelCatalogCacheStore
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.model.domain.ApiModelDetail
import com.runninghub.feature.model.domain.ApiModelField
import com.runninghub.feature.model.domain.ApiModelFieldType
import com.runninghub.feature.model.domain.ApiModelGroup
import com.runninghub.feature.model.domain.ApiModelSummary
import com.runninghub.feature.model.domain.ModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.data.remote.api.QuickCreateApi
import com.runninghub.feature.quickcreate.data.remote.dto.*
import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.ImageModel
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTag
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplate
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplateDetail
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplatePage
import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryException
import com.runninghub.feature.quickcreate.domain.QuickCreateRepositoryIssueCode
import com.runninghub.feature.quickcreate.domain.QuickCreateResultItem
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskIssueCode
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationInspirationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationProject
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectPage
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceKind
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldExtra
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldInputChild
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldOption
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldVisibilityCondition
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationServicePricing
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationUploadMediaKind
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import com.runninghub.feature.quickcreate.domain.VideoModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Suppress("UNUSED_PARAMETER")
private fun debug(tag: String, msg: String) {
    // QuickCreate 仓库默认不把调试信息写入 stdout。生成、上传和计费链路可能包含文件名、
    // 任务 ID、余额状态等敏感上下文；需要排障时应接入统一脱敏日志，而不是裸 println。
}

private fun mapResults(results: List<QuickCreateResultDto>?): List<QuickCreateResultItem> =
    results?.map {
        QuickCreateResultItem(
            url = it.url,
            type = it.outputType ?: it.type ?: "unknown",
            thumbnailUrl = it.thumbnailUrl,
            width = it.width,
            height = it.height,
            duration = it.duration,
        )
    } ?: emptyList()

private fun mapQuickCreationOutputs(outputs: List<QuickCreationOutputDto>): List<QuickCreateResultItem> =
    outputs.mapNotNull { output ->
        val fileUrl = output.fileUrl.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val sizeParts = output.outputSize
            ?.split("x", "X")
            ?.takeIf { it.size == 2 }

        QuickCreateResultItem(
            url = fileUrl,
            type = output.outputType ?: inferResultType(fileUrl),
            thumbnailUrl = output.filePreviewUrl,
            width = sizeParts?.getOrNull(0)?.toIntOrNull(),
            height = sizeParts?.getOrNull(1)?.toIntOrNull(),
            duration = null,
        )
    }

private fun inferResultType(url: String): String {
    val lower = url.lowercase()
    return when {
        lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov") -> "video"
        lower.endsWith(".mp3") || lower.endsWith(".wav") -> "audio"
        else -> "image"
    }
}

private val quickCreationParamJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private const val STANDARD_MODEL_CATALOG_PAGE_SIZE = 30
private const val STANDARD_MODEL_CATALOG_MAX_PAGES = 20
private const val STANDARD_MODEL_CATALOG_GROUP_PAGE_SIZE = 999
private const val QUICK_CREATION_POLL_PAGE_SIZE = 50
private const val QUICK_CREATION_POLL_MAX_PAGES = 5
private const val QUICK_CREATE_MODEL_CATALOG_CACHE_SCHEMA_VERSION = 1
private val STANDARD_MODEL_DETAIL_ENRICHED_GROUP_NAMES = setOf("\u81ea\u90e8\u7f72\u5f00\u6e90\u6a21\u578b")

private fun JsonElement.asParamString(): String? =
    (this as? JsonPrimitive)?.jsonPrimitive?.contentOrNull

private fun JsonElement?.asIntOrZero(): Int =
    (this as? JsonPrimitive)
        ?.jsonPrimitive
        ?.contentOrNull
        ?.toIntOrNull()
        ?: 0

private fun JsonElement.asParamStringList(): List<String>? =
    (this as? JsonArray)
        ?.mapNotNull { it.asParamString()?.takeIf { value -> value.isNotBlank() } }
        ?.takeIf { it.isNotEmpty() }

private fun parseJsonObjectOrNull(raw: String?): JsonObject? =
    raw
        ?.takeIf { it.isNotBlank() }
        ?.let { value ->
            runCatching { quickCreationParamJson.decodeFromString<JsonObject>(value) }.getOrNull()
        }

private fun QuickCreationTaskPageDto.toHistoryPage(): QuickCreationHistoryPage =
    QuickCreationHistoryPage(
        page = current.asIntOrZero().takeIf { it > 0 } ?: page.asIntOrZero(),
        size = size.asIntOrZero(),
        total = total.asIntOrZero(),
        items = records.ifEmpty { list }.map { it.toHistoryItem() },
    )

private val QuickCreationTaskPageDto.taskRecords: List<QuickCreationTaskRecordDto>
    get() = records.ifEmpty { list }

private fun QuickCreationTaskPageDto.hasMorePages(
    currentPage: Int,
    requestedPageSize: Int,
): Boolean {
    hasNext?.let { return it }
    val totalPages = pages.asIntOrZero()
    if (totalPages > 0) {
        return currentPage < totalPages
    }
    val totalItems = total.asIntOrZero()
    if (totalItems > 0) {
        return currentPage * requestedPageSize < totalItems
    }
    return taskRecords.size >= requestedPageSize
}

private fun QuickCreationProjectPageDto.toProjectPage(): QuickCreationProjectPage =
    QuickCreationProjectPage(
        page = current.asIntOrZero(),
        size = size.asIntOrZero(),
        total = total.asIntOrZero(),
        pages = pages.asIntOrZero(),
        hasNext = hasNext,
        hasPrevious = hasPrevious,
        nextCursor = nextCursor,
        items = records.mapNotNull { it.toProjectOrNull() },
    )

private fun QuickCreationProjectDto.toProjectOrNull(): QuickCreationProject? {
    val resolvedProjectId = projectId ?: id ?: return null
    return QuickCreationProject(
        projectId = resolvedProjectId,
        name = name ?: projectName ?: resolvedProjectId,
        coverUrl = coverUrl ?: cover,
        taskCount = taskCount,
        pinned = pin || pinned,
        createdAt = createdAt ?: createTime,
        updatedAt = updatedAt ?: updateTime,
    )
}

private fun QuickCreationProjectDto.toProject(): QuickCreationProject =
    toProjectOrNull() ?: throw QuickCreateRepositoryException(QuickCreateRepositoryIssueCode.PROJECT_ID_MISSING)

private fun QuickCreationTaskRecordDto.toHistoryItem(): QuickCreationHistoryItem {
    val params = parseJsonObjectOrNull(apiRequestParams)
        ?.mapNotNull { (key, value) ->
            val scalar = value.asParamString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            key to scalar
        }
        ?.toMap()
        .orEmpty()

    return QuickCreationHistoryItem(
        taskId = taskId,
        status = taskStatus,
        categoryId = bindingCategoryId,
        bindingId = bindingId,
        skuId = skuId,
        taskType = taskType,
        taskCostTime = taskCostTime,
        params = params,
        cashAmount = prepayRecord?.cashAmount ?: 0.0,
        cashCurrency = prepayRecord?.cashCurrency,
        outputs = outputList.map { it.toHistoryOutput() },
    )
}

/**
 * 将 QuickCreation 标准响应失败转换为 Domain 稳定错误语义。
 *
 * Data 层只保留服务端返回的业务摘要和响应码，不在这里生成最终中文 UI 文案；
 * Presentation 会根据 [fallbackIssueCode] 或远端摘要决定展示内容。
 */
private fun QuickCreationEnvelopeDto<*>.toRepositoryException(
    fallbackIssueCode: String,
): QuickCreateRepositoryException =
    QuickCreateRepositoryException(
        issueCode = fallbackIssueCode,
        remoteMessage = remoteFailureMessage(),
        remoteStatusCode = code,
    )

/**
 * 将媒体上传接口失败转换为 Domain 稳定错误语义。
 *
 * 上传接口使用独立响应结构，仍遵守同一条边界规则：Data 只携带错误码和远端摘要，
 * 不把本地兜底文案直接暴露给 UI。
 */
private fun MediaUploadResponseDto.toRepositoryException(
    fallbackIssueCode: String,
): QuickCreateRepositoryException =
    QuickCreateRepositoryException(
        issueCode = fallbackIssueCode,
        remoteMessage = message?.takeIf { code != 0 && it.isMeaningfulFailureMessage() },
        remoteStatusCode = code,
    )

private fun QuickCreationEnvelopeDto<*>.remoteFailureMessage(): String? =
    if (code == 0) {
        null
    } else {
        listOf(msg, message).firstOrNull { it.isMeaningfulFailureMessage() }
    }

private fun String?.isMeaningfulFailureMessage(): Boolean =
    !isNullOrBlank() && !equals("success", ignoreCase = true)

private fun QuickCreationFeePreviewDto.toDomain(): QuickCreationFeePreview =
    QuickCreationFeePreview(
        passed = passed,
        free = free,
        settlementMode = settlementMode,
        requiredRhAmount = requiredRhAmount,
        requiredCashAmount = requiredCashAmount,
        userCashBalance = userCashBalance,
        insufficientType = insufficientType,
        cashCurrency = cashCurrency,
    )

private fun QuickCreationOutputDto.toHistoryOutput(): QuickCreationHistoryOutput {
    val sizeParts = outputSize
        ?.split("x", "X")
        ?.takeIf { it.size == 2 }

    return QuickCreationHistoryOutput(
        outputId = id,
        url = fileUrl,
        type = outputType ?: inferResultType(fileUrl),
        thumbnailUrl = filePreviewUrl,
        width = sizeParts?.getOrNull(0)?.toIntOrNull(),
        height = sizeParts?.getOrNull(1)?.toIntOrNull(),
        outputName = outputName,
        expireTime = expireTime,
        expireDays = expireDays,
    )
}

private fun pollTaskStatus(
    api: QuickCreateApi,
    taskId: String,
): Flow<QuickCreateTaskStatus> = flow {
    var attempts = 0
    val maxAttempts = 120
    while (attempts < maxAttempts) {
        val queryResponse = api.queryTask(taskId)
        attempts++
        val results = mapResults(queryResponse.results)

        val taskStatus: QuickCreateTaskStatus = when (queryResponse.status) {
            QuickCreateResult.STATUS_SUCCESS -> QuickCreateTaskStatus.Success(taskId, results)
            QuickCreateResult.STATUS_FAILED -> QuickCreateTaskStatus.Failed(
                taskId,
                QuickCreateTaskIssueCode.TASK_FAILED,
            )
            QuickCreateResult.STATUS_RUNNING -> QuickCreateTaskStatus.Running(taskId, queryResponse.progress)
            QuickCreateResult.STATUS_QUEUING -> QuickCreateTaskStatus.Queuing(taskId)
            QuickCreateResult.STATUS_CANCELED,
            QuickCreateResult.STATUS_CANCELLED -> QuickCreateTaskStatus.Cancelled(taskId)
            else -> QuickCreateTaskStatus.Queuing(taskId)
        }

        emit(taskStatus)
        if (taskStatus.isTerminalPollingStatus()) {
            return@flow
        }
        delay(2000)
    }
    emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.TASK_TIMEOUT))
}

private fun pollQuickCreationTaskStatus(
    api: QuickCreateApi,
    taskId: String,
): Flow<QuickCreateTaskStatus> = flow {
    var attempts = 0
    val maxAttempts = 120
    while (attempts < maxAttempts) {
        val lookup = findQuickCreationTaskRecord(api = api, taskId = taskId)
        attempts++

        val record = when (lookup) {
            QuickCreationTaskRecordLookup.QueryFailed -> {
                // 任务轮询失败只向上游暴露稳定错误码，避免远端 msg/message 直接进入页面状态。
                emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.TASK_QUERY_FAILED))
                return@flow
            }
            QuickCreationTaskRecordLookup.Missing -> {
                emit(QuickCreateTaskStatus.Queuing(taskId))
                delay(2000)
                continue
            }
            is QuickCreationTaskRecordLookup.Found -> lookup.record
        }

        val normalizedStatus = record.taskStatus.trim().uppercase()
        val status: QuickCreateTaskStatus = when (normalizedStatus) {
            "SUCCESS" -> QuickCreateTaskStatus.Success(taskId, mapQuickCreationOutputs(record.outputList))
            "FAILED", "FAILURE", "ERROR" -> QuickCreateTaskStatus.Failed(taskId, QuickCreateTaskIssueCode.TASK_FAILED)
            "CANCELED", "CANCELLED" -> QuickCreateTaskStatus.Cancelled(taskId)
            "RUNNING", "PROCESSING" -> QuickCreateTaskStatus.Running(
                taskId = taskId,
                progress = record.normalizedProgressPercent() ?: 0,
            )
            else -> QuickCreateTaskStatus.Queuing(taskId)
        }

        emit(status)
        if (status.isTerminalPollingStatus()) {
            return@flow
        }
        delay(2000)
    }
    emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.TASK_TIMEOUT))
}

private sealed interface QuickCreationTaskRecordLookup {
    data class Found(val record: QuickCreationTaskRecordDto) : QuickCreationTaskRecordLookup
    data object Missing : QuickCreationTaskRecordLookup
    data object QueryFailed : QuickCreationTaskRecordLookup
}

/**
 * 在快捷创作任务列表中按页查找刚提交的任务。
 *
 * 生成接口只返回 taskId，轮询端必须从历史分页中反查记录。真实账号任务多时目标任务未必位于第一页，
 * 因此每轮轮询最多向后扫描少量页面，避免把后台已经成功的任务误判为客户端超时。
 */
private suspend fun findQuickCreationTaskRecord(
    api: QuickCreateApi,
    taskId: String,
): QuickCreationTaskRecordLookup {
    var pageIndex = 1
    while (pageIndex <= QUICK_CREATION_POLL_MAX_PAGES) {
        val page = api.listQuickCreationTasks(page = pageIndex, size = QUICK_CREATION_POLL_PAGE_SIZE)
        if (page.code != 0) {
            return QuickCreationTaskRecordLookup.QueryFailed
        }

        val data = page.data ?: return QuickCreationTaskRecordLookup.Missing
        data.taskRecords.firstOrNull { it.taskId == taskId }?.let { record ->
            return QuickCreationTaskRecordLookup.Found(record)
        }
        if (!data.hasMorePages(currentPage = pageIndex, requestedPageSize = QUICK_CREATION_POLL_PAGE_SIZE)) {
            return QuickCreationTaskRecordLookup.Missing
        }
        pageIndex += 1
    }
    return QuickCreationTaskRecordLookup.Missing
}

/**
 * 判断任务轮询是否应立即停止。
 *
 * 成功、失败和取消都属于服务端终态；收到这些状态后继续轮询只会制造无意义请求，
 * 并可能把用户主动取消误报成客户端超时。
 */
private fun QuickCreateTaskStatus.isTerminalPollingStatus(): Boolean =
    this is QuickCreateTaskStatus.Success ||
        this is QuickCreateTaskStatus.Failed ||
        this is QuickCreateTaskStatus.Cancelled

private val ImageGenerationRequest.hasQuickCreationIdentity: Boolean
    get() = !quickCreationBindingId.isNullOrBlank() && !quickCreationSkuId.isNullOrBlank()

/**
 * 快捷创作远程数据仓库实现。
 *
 * 本类位于 Data 层，负责把 QuickCreate 相关远程接口、凭据读取、Token 失效重试、
 * DTO 到 Domain 的映射和异常包装集中在数据边界内。迁移期间它同时实现生成、历史/项目任务、
 * 项目管理、灵感模板、模型目录、计费预览和媒体上传等多个窄 Repository 接口；Presentation 只能依赖
 * 这些窄接口，不应直接依赖本实现或远程 API。
 *
 * @param quickCreateApi 快捷创作远程 API，封装接口路径、请求头和序列化细节。
 * @param credentialStore 凭据读取边界，用于在请求前获取当前 API Key。
 * @param authRepository 认证仓库；当服务端返回 Token 失效时用于刷新后重试一次。
 * 该依赖必须由 DI 提供，避免生产运行时因缺少刷新能力而把可恢复的 401/TOKEN_INVALID 直接暴露为失败。
 * @param modelCatalogRepository 标准模型目录仓库，用于按 SKU 补齐 `/api/sku/list` 与 `/api/sku/detail`
 * 的脱敏摘要、字段结构和价格展示信息；`null` 只作为旧测试或接口不可用时的降级路径。
 * @param modelCatalogCacheStore 快捷创作合并目录快照缓存；保存的是可直接展示和提交的模型列表，
 * 与标准 SKU 目录缓存分开，避免不同身份键的列表互相覆盖。
 */
class QuickCreateRepositoryImpl(
    private val quickCreateApi: QuickCreateApi,
    private val credentialStore: CredentialStore,
    private val authRepository: AuthRepository,
    private val modelCatalogRepository: ModelCatalogRepository? = null,
    private val modelCatalogCacheStore: ModelCatalogCacheStore? = null,
) : QuickCreationTaskHistoryRepository,
    QuickCreationFeePreviewRepository,
    QuickCreationGenerationRepository,
    QuickCreationInspirationRepository,
    QuickCreationMediaUploadRepository,
    QuickCreationModelCatalogRepository,
    QuickCreationProjectRepository {
    private val modelCatalogCacheMutex = Mutex()
    private val quickCreateModelCatalogMemoryCache =
        mutableMapOf<QuickCreationServiceKind, List<QuickCreationServiceModel>>()

    private suspend fun <T> quickCreationRequestWithTokenRetry(
        request: suspend () -> QuickCreationEnvelopeDto<T>,
    ): QuickCreationEnvelopeDto<T> {
        val first = request()
        if (!first.isTokenInvalid()) return first

        val refreshed = authRepository.refreshTokenIfNeeded().isSuccess
        debug("QuickCreationV2", "credential refresh retry refreshed=$refreshed")
        return if (refreshed) request() else first
    }

    private fun QuickCreationEnvelopeDto<*>.isTokenInvalid(): Boolean =
        code == 412 && (msg.equals("TOKEN_INVALID", ignoreCase = true) ||
            message.equals("TOKEN_INVALID", ignoreCase = true))

    private fun QuickCreationEnvelopeDto<*>.isPrepareTokenExpired(): Boolean {
        val text = listOfNotNull(msg, message).joinToString(" ").lowercase()
        return (text.contains("prepare") || text.contains("token")) &&
            (text.contains("expire") ||
                text.contains("expired") ||
                text.contains("invalid") ||
                text.contains("过期") ||
                text.contains("失效"))
    }

    private suspend fun commitQuickCreationWithPrepareRetry(
        createRequest: QuickCreationCreateRequestDto,
        prepareToken: String,
    ): QuickCreationEnvelopeDto<QuickCreationCommitDataDto> {
        debug("QuickCreationV2", "commit start")
        val firstCommit = quickCreationRequestWithTokenRetry {
            quickCreateApi.commitQuickCreation(
                QuickCreationCommitRequestDto(
                    prepareToken = prepareToken,
                    createRequest = createRequest,
                )
            )
        }
        debug("QuickCreationV2", "commit response code=${firstCommit.code}")
        if (!firstCommit.isPrepareTokenExpired()) return firstCommit

        debug("QuickCreationV2", "commit prepare credential expired; re-prepare")
        val refreshedPrepare = quickCreationRequestWithTokenRetry {
            quickCreateApi.prepareQuickCreation(createRequest)
        }
        debug("QuickCreationV2", "re-prepare response code=${refreshedPrepare.code}")
        if (refreshedPrepare.code != 0 || refreshedPrepare.data == null) {
            return QuickCreationEnvelopeDto(
                code = refreshedPrepare.code,
                msg = QuickCreateTaskIssueCode.PREPARE_FAILED,
                data = null,
            )
        }
        debug("QuickCreationV2", "commit retry start")

        return quickCreationRequestWithTokenRetry {
            quickCreateApi.commitQuickCreation(
                QuickCreationCommitRequestDto(
                    prepareToken = refreshedPrepare.data.prepareToken,
                    createRequest = createRequest,
                )
            )
        }
    }

    override suspend fun previewImageQuickCreationFee(
        request: ImageGenerationRequest,
    ): Result<QuickCreationFeePreview> = runCatching {
        val createRequest = QuickCreationV2Defaults.imageG2CreateRequest(request)
        val response = quickCreationRequestWithTokenRetry {
            quickCreateApi.previewQuickCreationFee(createRequest)
        }
        if (response.code != 0 || response.data == null) {
            throw response.toRepositoryException(QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED)
        }
        response.data.toDomain()
    }

    override suspend fun previewVideoQuickCreationFee(
        request: VideoGenerationRequest,
    ): Result<QuickCreationFeePreview> = runCatching {
        val createRequest = QuickCreationV2Defaults.videoCreateRequest(request)
        val response = quickCreationRequestWithTokenRetry {
            quickCreateApi.previewQuickCreationFee(createRequest)
        }
        if (response.code != 0 || response.data == null) {
            throw response.toRepositoryException(QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED)
        }
        response.data.toDomain()
    }

    private fun generateImageWithQuickCreationV2(
        request: ImageGenerationRequest,
    ): Flow<QuickCreateTaskStatus> = flow {
        val createRequest = QuickCreationV2Defaults.imageG2CreateRequest(request)

        val feePreview = quickCreationRequestWithTokenRetry {
            quickCreateApi.previewQuickCreationFee(createRequest)
        }
        debug("QuickCreationV2", "image fee-preview response code=${feePreview.code}")
        if (feePreview.code != 0) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED))
            return@flow
        }
        val fee = feePreview.data
        if (fee != null && (!fee.passed || fee.insufficientType != null)) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.FEE_PREVIEW_BLOCKED))
            return@flow
        }

        val prepare = quickCreationRequestWithTokenRetry {
            quickCreateApi.prepareQuickCreation(createRequest)
        }
        debug("QuickCreationV2", "image prepare response code=${prepare.code}")
        if (prepare.code != 0 || prepare.data == null) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.PREPARE_FAILED))
            return@flow
        }

        val commit = commitQuickCreationWithPrepareRetry(
            createRequest = createRequest,
            prepareToken = prepare.data.prepareToken,
        )
        if (commit.code != 0 || commit.data == null) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.COMMIT_FAILED))
            return@flow
        }

        val taskId = commit.data.taskId
        debug("QuickCreationV2", "image commit success taskId=$taskId")
        emit(QuickCreateTaskStatus.Queuing(taskId))
        pollQuickCreationTaskStatus(quickCreateApi, taskId).collect { emit(it) }
    }

    private fun generateVideoWithQuickCreationV2(
        request: VideoGenerationRequest,
    ): Flow<QuickCreateTaskStatus> = flow {
        val createRequest = QuickCreationV2Defaults.videoCreateRequest(request)

        val feePreview = quickCreationRequestWithTokenRetry {
            quickCreateApi.previewQuickCreationFee(createRequest)
        }
        debug("QuickCreationV2", "video fee-preview response code=${feePreview.code}")
        if (feePreview.code != 0) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.FEE_PREVIEW_FAILED))
            return@flow
        }
        val fee = feePreview.data
        if (fee != null && (!fee.passed || fee.insufficientType != null)) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.FEE_PREVIEW_BLOCKED))
            return@flow
        }

        val prepare = quickCreationRequestWithTokenRetry {
            quickCreateApi.prepareQuickCreation(createRequest)
        }
        debug("QuickCreationV2", "video prepare response code=${prepare.code}")
        if (prepare.code != 0 || prepare.data == null) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.PREPARE_FAILED))
            return@flow
        }

        val commit = commitQuickCreationWithPrepareRetry(
            createRequest = createRequest,
            prepareToken = prepare.data.prepareToken,
        )
        if (commit.code != 0 || commit.data == null) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.COMMIT_FAILED))
            return@flow
        }

        val taskId = commit.data.taskId
        debug("QuickCreationV2", "video commit success taskId=$taskId")
        emit(QuickCreateTaskStatus.Queuing(taskId))
        pollQuickCreationTaskStatus(quickCreateApi, taskId).collect { emit(it) }
    }
    // ── 图片创作 ───────────────────────────────────────

    override fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus> = flow {
        emit(QuickCreateTaskStatus.Submitting)

        try {
            if (request.hasQuickCreationIdentity || request.model == "all-power-image-g2") {
                generateImageWithQuickCreationV2(request).collect { emit(it) }
                return@flow
            }

            val hasRef = !request.referenceImageUri.isNullOrBlank()
            val refImageUrl = request.referenceImageUri ?: ""  // validated non-null reference, replaces all !! usage
            val model = ImageModel.entries.find { it.modelKey == request.model }
                ?: ImageModel.ALL_POWER_IMAGE_G_2_OFFICIAL

            // 根据模型路由到对应 API 端点
            val response: TaskResponse = when (model) {

                // 全能图片 G-2.0 官方版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_G_2_OFFICIAL -> {
                    if (hasRef) {
                        quickCreateApi.imageG2ImageToImage(
                            AllPowerImageG2ImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                quality = request.quality,
                            )
                        )
                    } else {
                        quickCreateApi.imageG2TextToImage(
                            AllPowerImageG2TextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                quality = request.quality,
                            )
                        )
                    }
                }

                // 全能图片 G-2.0 低价版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_G_2_CHEAP -> {
                    if (hasRef) {
                        quickCreateApi.imageG2CheapImageToImage(
                            AllPowerImageG2ImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                quality = request.quality,
                            )
                        )
                    } else {
                        quickCreateApi.imageG2CheapTextToImage(
                            AllPowerImageG2TextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                quality = request.quality,
                            )
                        )
                    }
                }

                // 全能图片 X 官方版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_X_OFFICIAL -> {
                    if (hasRef) {
                        quickCreateApi.imageXImageToImage(
                            AllPowerImageXImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.imageXTextToImage(
                            AllPowerImageXTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                outputFormat = "png",
                            )
                        )
                    }
                }

                // 全能图片 X 低价版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_X_CHEAP -> {
                    if (hasRef) {
                        quickCreateApi.imageXCheapImageToImage(
                            AllPowerImageXImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.imageXCheapTextToImage(
                            AllPowerImageXTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                outputFormat = "png",
                            )
                        )
                    }
                }

                // 全能图片 PRO 官方版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_PRO_OFFICIAL -> {
                    if (hasRef) {
                        quickCreateApi.imageProImageToImage(
                            AllPowerImageV2ProImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageProTextToImage(
                            AllPowerImageV2ProTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    }
                }

                // 全能图片 PRO 低价版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_PRO_CHEAP -> {
                    if (hasRef) {
                        quickCreateApi.imageProCheapImageToImage(
                            AllPowerImageV2ProImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageProCheapTextToImage(
                            AllPowerImageV2ProTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    }
                }

                // 全能图片 2.0 官方版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_2_OFFICIAL -> {
                    if (hasRef) {
                        quickCreateApi.imageV2ImageToImage(
                            AllPowerImageV2ProImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageV2TextToImage(
                            AllPowerImageV2ProTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    }
                }

                // 全能图片 2.0 低价版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_2_CHEAP -> {
                    if (hasRef) {
                        quickCreateApi.imageV2CheapImageToImage(
                            AllPowerImageV2ProImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageV2CheapTextToImage(
                            AllPowerImageV2ProTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    }
                }

                // Seedream 5.0 Lite (仅文生图)
                ImageModel.SEEDREAM_5_0_LITE -> {
                    quickCreateApi.imageSeedream5TextToImage(
                        SeedreamV5LiteTextToImageRequestDto(
                            prompt = request.prompt,
                            resolution = request.resolution,
                        )
                    )
                }

                // Seedream 4.0 (文生图 + 图生图)
                ImageModel.SEEDREAM_4_0 -> {
                    if (hasRef) {
                        quickCreateApi.imageSeedream4ImageToImage(
                            SeedreamV4ImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageSeedream4TextToImage(
                            SeedreamV5LiteTextToImageRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                            )
                        )
                    }
                }
            }

            val taskId = response.taskId

            if (response.status == QuickCreateResult.STATUS_FAILED || response.errorCode.isNotBlank()) {
                emit(QuickCreateTaskStatus.Failed(taskId, QuickCreateTaskIssueCode.TASK_FAILED))
                return@flow
            }

            emit(QuickCreateTaskStatus.Queuing(taskId))
            pollTaskStatus(quickCreateApi, taskId).collect { emit(it) }

        } catch (_: Exception) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.UNKNOWN_ERROR))
        }
    }

    // ── 视频创作 ───────────────────────────────────────

    override fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus> = flow {
        emit(QuickCreateTaskStatus.Submitting)

        try {
            if (
                !request.quickCreationBindingId.isNullOrBlank() &&
                !request.quickCreationSkuId.isNullOrBlank()
            ) {
                generateVideoWithQuickCreationV2(request).collect { emit(it) }
                return@flow
            }

            val hasImageRef = !request.referenceImageUri.isNullOrBlank()
            val refImageUrl = request.referenceImageUri ?: ""  // validated non-null reference, replaces all !! usage
            val hasFirstFrame = !request.firstFrameImageUri.isNullOrBlank()
            val hasLastFrame = !request.lastFrameImageUri.isNullOrBlank()

            val model = VideoModel.entries.find { it.modelKey == request.model }
                ?: VideoModel.HAPPYHORSE

            val response: TaskResponse = when (model) {

                // HappyHorse: 文生视频 + 图生视频
                VideoModel.HAPPYHORSE -> {
                    if (hasImageRef) {
                        quickCreateApi.happyHorseImageToVideo(
                            HappyHorseImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.happyHorseTextToVideo(
                            HappyHorseTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                seed = request.seed,
                            )
                        )
                    }
                }

                // Seedance 2.0: 文生视频 + 图生视频(含首尾帧)
                VideoModel.SEEDANCE_2_0 -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.seedance2ImageToVideo(
                            SeedanceImageToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                firstFrameUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastFrameUrl = request.lastFrameImageUri,
                                generateAudio = request.generateAudio,
                                ratio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.seedance2TextToVideo(
                            SeedanceTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                generateAudio = request.generateAudio,
                                ratio = request.aspectRatio,
                            )
                        )
                    }
                }

                // Seedance 2.0 Fast: 文生视频 + 图生视频(含首尾帧)
                VideoModel.SEEDANCE_2_0_FAST -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.seedance2FastImageToVideo(
                            SeedanceImageToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                firstFrameUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastFrameUrl = request.lastFrameImageUri,
                                generateAudio = request.generateAudio,
                                ratio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.seedance2FastTextToVideo(
                            SeedanceTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                generateAudio = request.generateAudio,
                                ratio = request.aspectRatio,
                            )
                        )
                    }
                }

                // 可灵 3.0-4K: 仅图生视频
                VideoModel.KLING_3_0_4K -> {
                    quickCreateApi.klingO34KImageToVideo(
                        KlingO34KImageToVideoRequestDto(
                            prompt = request.prompt,
                            firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                            lastImageUrl = request.lastFrameImageUri,
                            duration = request.duration,
                            sound = request.generateAudio,
                        )
                    )
                }

                // 可灵 O3-Pro: 文生视频 + 图生视频
                VideoModel.KLING_O3_PRO -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.klingO3ProImageToVideo(
                            KlingO3ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastImageUrl = request.lastFrameImageUri,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.klingO3ProTextToVideo(
                            KlingO3ProTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // 可灵 O3-Std: 文生视频 + 图生视频
                VideoModel.KLING_O3_STD -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.klingO3StdImageToVideo(
                            KlingO3StdImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastImageUrl = request.lastFrameImageUri,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.klingO3StdTextToVideo(
                            KlingO3StdTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // 可灵 O1: 文生视频 + 图生视频
                VideoModel.KLING_O1 -> {
                    if (hasImageRef) {
                        quickCreateApi.klingO1ImageToVideo(
                            KlingO1ImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.klingO1TextToVideo(
                            KlingO1TextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                                negativePrompt = request.negativePrompt,
                            )
                        )
                    }
                }

                // 万相 2.7: 文生视频 + 图生视频
                VideoModel.WAN_2_7 -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.wan27ImageToVideo(
                            Wan27ImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: request.referenceImageUri ?: "",
                                lastImageUrl = request.lastFrameImageUri,
                                audioUrl = request.referenceAudioUri,
                                negativePrompt = request.negativePrompt,
                                resolution = request.resolution,
                                duration = request.duration.toString(),
                                promptExtend = request.promptExtend,
                                seed = request.seed,
                            )
                        )
                    } else {
                        quickCreateApi.wan27TextToVideo(
                            Wan27TextToVideoRequestDto(
                                prompt = request.prompt,
                                negativePrompt = request.negativePrompt,
                                audioUrl = request.referenceAudioUri,
                                duration = request.duration.toString(),
                                resolution = request.resolution,
                                aspectRatio = request.aspectRatio,
                                promptExtend = request.promptExtend,
                                seed = request.seed,
                            )
                        )
                    }
                }

                // 万相 2.6: 仅图生视频
                VideoModel.WAN_2_6 -> {
                    quickCreateApi.wan26ImageToVideo(
                        Wan26ImageToVideoRequestDto(
                            firstImageUrl = refImageUrl,
                            prompt = request.prompt,
                            resolution = request.resolution,
                            duration = request.duration,
                            shotType = if (hasFirstFrame) "single" else "single",
                        )
                    )
                }

                // PixVerse V6: 文生视频 + 图生视频
                VideoModel.PIXVERSE_V6 -> {
                    if (hasImageRef) {
                        quickCreateApi.pixVerseV6ImageToVideo(
                            PixVerseV6ImageToVideoRequestDto(
                                imageUrl = refImageUrl,
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                generateAudioSwitch = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.pixVerseV6TextToVideo(
                            PixVerseV6TextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                generateAudioSwitch = request.generateAudio,
                                aspectRatio = request.aspectRatio,
                            )
                        )
                    }
                }

                // 全能视频 V3.1 Fast 官方版: 文生视频 + 图生视频 + 首尾帧
                VideoModel.ALL_POWER_VIDEO_V_FAST_OFFICIAL -> {
                    if (hasFirstFrame && hasLastFrame) {
                        quickCreateApi.allPowerV31FastStartEndToVideo(
                            AllPowerVideoV31FastStartEndToVideoRequestDto(
                                prompt = request.prompt,
                                firstFrameUrl = request.firstFrameImageUri!!,
                                lastFrameUrl = request.lastFrameImageUri,
                                aspectRatio = request.aspectRatio,
                                duration = request.duration,
                                resolution = request.resolution,
                            )
                        )
                    } else if (hasImageRef) {
                        quickCreateApi.allPowerV31FastImageToVideo(
                            AllPowerVideoV31FastImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.allPowerV31FastTextToVideo(
                            AllPowerVideoV31FastTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    }
                }

                // 全能视频 V3.1 Fast 低价版: 文生视频 + 图生视频 + 首尾帧
                VideoModel.ALL_POWER_VIDEO_V_FAST_CHEAP -> {
                    if (hasFirstFrame && hasLastFrame) {
                        quickCreateApi.allPowerV31FastStartEndToVideo(
                            AllPowerVideoV31FastStartEndToVideoRequestDto(
                                prompt = request.prompt,
                                firstFrameUrl = request.firstFrameImageUri!!,
                                lastFrameUrl = request.lastFrameImageUri,
                                aspectRatio = request.aspectRatio,
                                duration = request.duration,
                                resolution = request.resolution,
                            )
                        )
                    } else if (hasImageRef) {
                        quickCreateApi.allPowerV31FastImageToVideo(
                            AllPowerVideoV31FastImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.allPowerV31FastTextToVideo(
                            AllPowerVideoV31FastTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    }
                }

                // 全能视频 V3.1 Pro 官方版: 文生视频 + 图生视频 + 首尾帧
                VideoModel.ALL_POWER_VIDEO_V_PRO_OFFICIAL -> {
                    if (hasFirstFrame && hasLastFrame) {
                        quickCreateApi.allPowerV31ProImageToVideo(
                            AllPowerVideoV31ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = request.firstFrameImageUri!!,
                                firstFrameUrl = request.firstFrameImageUri,
                                lastFrameUrl = request.lastFrameImageUri,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else if (hasImageRef) {
                        quickCreateApi.allPowerV31ProImageToVideo(
                            AllPowerVideoV31ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.allPowerV31ProTextToVideo(
                            AllPowerVideoV31ProTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // 全能视频 V3.1 Pro 低价版: 文生视频 + 图生视频 + 首尾帧
                VideoModel.ALL_POWER_VIDEO_V_PRO_CHEAP -> {
                    if (hasFirstFrame && hasLastFrame) {
                        quickCreateApi.allPowerV31ProImageToVideo(
                            AllPowerVideoV31ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = request.firstFrameImageUri!!,
                                firstFrameUrl = request.firstFrameImageUri,
                                lastFrameUrl = request.lastFrameImageUri,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else if (hasImageRef) {
                        quickCreateApi.allPowerV31ProImageToVideo(
                            AllPowerVideoV31ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.allPowerV31ProTextToVideo(
                            AllPowerVideoV31ProTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // 全能视频 X 官方版: 文生视频 + 图生视频
                VideoModel.ALL_POWER_VIDEO_X_OFFICIAL -> {
                    if (hasImageRef) {
                        quickCreateApi.allPowerVXImageToVideo(
                            AllPowerVideoXImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration.toString(),
                            )
                        )
                    } else {
                        quickCreateApi.allPowerVXTextToVideo(
                            AllPowerVideoXTextToVideoRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                            )
                        )
                    }
                }

                // 全能视频 X 低价版: 仅图生视频(多图)
                VideoModel.ALL_POWER_VIDEO_X_CHEAP -> {
                    val images = listOfNotNull(
                        request.firstFrameImageUri ?: request.referenceImageUri,
                        request.lastFrameImageUri,
                    ).filter { it.isNotBlank() }
                    quickCreateApi.allPowerVXCheapImageToVideo(
                        AllPowerVideoXCheapImageToVideoRequestDto(
                            prompt = request.prompt,
                            aspectRatio = request.aspectRatio,
                            imageUrls = images.ifEmpty { listOf(refImageUrl) },
                            resolution = request.resolution,
                            duration = request.duration,
                        )
                    )
                }

                // Vidu Q3-Pro: 文生视频 + 图生视频
                VideoModel.VIDU_Q3_PRO -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.viduQ3ProImageToVideo(
                            ViduQ3ImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastImageUrl = request.lastFrameImageUri,
                                style = request.style,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.viduQ3ProTextToVideo(
                            ViduQ3TextToVideoRequestDto(
                                prompt = request.prompt,
                                style = request.style,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // Vidu Q3-Pro-Fast: 文生视频 + 图生视频
                VideoModel.VIDU_Q3_PRO_FAST -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.viduQ3TurboImageToVideo(
                            ViduQ3ImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastImageUrl = request.lastFrameImageUri,
                                style = request.style,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.viduQ3TurboTextToVideo(
                            ViduQ3TextToVideoRequestDto(
                                prompt = request.prompt,
                                style = request.style,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }
            }

            val taskId = when (val resp = response) {
                is HappyHorseTextToVideoResponseDto -> resp.taskId
                is SeedanceTextToVideoResponseDto -> resp.taskId
                is SeedanceImageToVideoResponseDto -> resp.taskId
                is KlingO34KImageToVideoResponseDto -> resp.taskId
                is KlingO3ProTextToVideoResponseDto -> resp.taskId
                is KlingO3ProImageToVideoResponseDto -> resp.taskId
                is KlingO3StdTextToVideoResponseDto -> resp.taskId
                is KlingO3StdImageToVideoResponseDto -> resp.taskId
                is KlingO1TextToVideoResponseDto -> resp.taskId
                is KlingO1ImageToVideoResponseDto -> resp.taskId
                is Wan27TextToVideoResponseDto -> resp.taskId
                is Wan27ImageToVideoResponseDto -> resp.taskId
                is Wan26ImageToVideoResponseDto -> resp.taskId
                is PixVerseV6TextToVideoResponseDto -> resp.taskId
                is PixVerseV6ImageToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31FastTextToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31FastImageToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31FastStartEndToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31ProTextToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31ProImageToVideoResponseDto -> resp.taskId
                is AllPowerVideoXTextToVideoResponseDto -> resp.taskId
                is AllPowerVideoXImageToVideoResponseDto -> resp.taskId
                is AllPowerVideoXCheapImageToVideoResponseDto -> resp.taskId
                is ViduQ3TextToVideoResponseDto -> resp.taskId
                is ViduQ3ImageToVideoResponseDto -> resp.taskId
                else -> ""
            }

            if (response.status == QuickCreateResult.STATUS_FAILED || response.errorCode.isNotBlank()) {
                emit(QuickCreateTaskStatus.Failed(taskId, QuickCreateTaskIssueCode.TASK_FAILED))
                return@flow
            }

            emit(QuickCreateTaskStatus.Queuing(taskId))
            pollTaskStatus(quickCreateApi, taskId).collect { emit(it) }

        } catch (_: Exception) {
            emit(QuickCreateTaskStatus.Error(QuickCreateTaskIssueCode.UNKNOWN_ERROR))
        }
    }

    // ── 媒体上传 ───────────────────────────────────────

    override suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): Result<String> = runCatching {
        val TAG = "QuickCreateRepo"
        val hasMediaName = fileName.isNotBlank()
        debug(TAG, "uploadMedia: START")
        debug(TAG, "  mediaNamePresent = $hasMediaName")
        debug(TAG, "  mimeType  = $mimeType")
        debug(TAG, "  fileBytes = ${fileBytes.size} bytes")

        val apiKey = credentialStore.getApiKey()
        val hasCredential = !apiKey.isNullOrBlank()
        debug(TAG, "  credential found = $hasCredential")
        if (apiKey.isNullOrBlank()) {
            throw QuickCreateRepositoryException(QuickCreateRepositoryIssueCode.API_KEY_MISSING)
        }

        debug(TAG, "  calling QuickCreateApi.uploadMedia...")
        val uploadResp = quickCreateApi.uploadMedia(apiKey, fileBytes, fileName, mimeType)
        val hasMediaLocation = !uploadResp.url.isNullOrBlank()
        debug(TAG, "  response.code    = ${uploadResp.code}")
        debug(TAG, "  response.message = ${uploadResp.message}")
        debug(TAG, "  response.hasMediaLocation = $hasMediaLocation")

        if (!uploadResp.isSuccess) {
            throw uploadResp.toRepositoryException(QuickCreateRepositoryIssueCode.MEDIA_UPLOAD_FAILED)
        }

        uploadResp.url ?: throw QuickCreateRepositoryException(QuickCreateRepositoryIssueCode.MEDIA_UPLOAD_EMPTY_URL)
    }.onFailure { e ->
        val TAG = "QuickCreateRepo"
        debug(TAG, "uploadMedia: FAILED")
        debug(TAG, "  exception = ${e::class.simpleName}: ${e.message}")
    }

    override suspend fun getInspirationTags(): Result<List<QuickCreateInspirationTag>> = runCatching {
        val response = quickCreateApi.getQuickCreationInspirationTags()
        if (response.code != 0) {
            throw response.toRepositoryException(QuickCreateRepositoryIssueCode.INSPIRATION_TAGS_LOAD_FAILED)
        }

        response.data.orEmpty().mapNotNull { tag ->
            val id = tag.categoryId ?: tag.id ?: tag.name ?: tag.nameCn ?: return@mapNotNull null
            val name = tag.nameCn ?: tag.name ?: tag.nameEn ?: id
            QuickCreateInspirationTag(id = id, name = name)
        }
    }

    override suspend fun getInspirationTemplates(
        page: Int,
        size: Int,
        tagId: String?,
    ): Result<QuickCreateInspirationTemplatePage> = runCatching {
        val response = quickCreateApi.getQuickCreationInspirationTemplates(page, size, tagId)
        if (response.code != 0) {
            throw response.toRepositoryException(QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATES_LOAD_FAILED)
        }

        val pageDto = response.data
        val templates = (pageDto?.records?.takeIf { it.isNotEmpty() } ?: pageDto?.list).orEmpty().map { template ->
            QuickCreateInspirationTemplate(
                templateId = template.templateId,
                title = template.nameCn ?: template.nameAi ?: template.templateId,
                categoryId = template.categoryId,
                coverUrl = template.coverUrl,
                videoUrl = template.videoUrl,
                tagHot = template.tagHot,
                tagNew = template.tagNew,
            )
        }
        val resolvedPage = pageDto?.current.asIntOrZero().takeIf { it > 0 }
            ?: pageDto?.page.asIntOrZero().takeIf { it > 0 }
            ?: page
        val resolvedSize = pageDto?.size.asIntOrZero().takeIf { it > 0 } ?: size
        val total = pageDto?.total.asIntOrZero()
        val pages = pageDto?.pages.asIntOrZero().takeIf { it > 0 }
            ?: if (total > 0 && resolvedSize > 0) ((total + resolvedSize - 1) / resolvedSize) else 0
        val hasNext = pageDto?.hasNext ?: (pages > 0 && resolvedPage < pages)
        QuickCreateInspirationTemplatePage(
            page = resolvedPage,
            size = resolvedSize,
            total = total,
            pages = pages,
            hasNext = hasNext,
            hasPrevious = pageDto?.hasPrevious ?: (resolvedPage > 1),
            nextCursor = pageDto?.nextCursor,
            items = templates,
        )
    }

    override suspend fun getInspirationTemplateDetail(
        templateId: String,
    ): Result<QuickCreateInspirationTemplateDetail> = runCatching {
        val response = quickCreateApi.getQuickCreationInspirationTemplateDetail(templateId)
        if (response.code != 0) {
            throw response.toRepositoryException(QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATE_DETAIL_LOAD_FAILED)
        }
        val detail = response.data ?: throw QuickCreateRepositoryException(
            QuickCreateRepositoryIssueCode.INSPIRATION_TEMPLATE_DETAIL_EMPTY
        )
        val paramsObject = (detail.snapshot?.presetParams as? JsonObject)
            ?: parseJsonObjectOrNull(detail.apiRequestParamsRaw)
            ?: JsonObject(emptyMap())
        val scalarParams = paramsObject
            .mapNotNull { (key, value) ->
                val scalar = value.asParamString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                key to scalar
            }
            .toMap()
        val listParams = paramsObject
            .mapNotNull { (key, value) ->
                val values = value.asParamStringList() ?: return@mapNotNull null
                key to values
            }
            .toMap()

        QuickCreateInspirationTemplateDetail(
            templateId = detail.templateId,
            title = detail.nameCn ?: detail.nameAi ?: detail.templateId,
            categoryId = detail.categoryId,
            bindingId = detail.bindingId,
            skuId = detail.skuId,
            prompt = scalarParams["prompt"] ?: scalarParams["promptAi"],
            params = scalarParams,
            listParams = listParams,
            coverUrl = detail.coverUrl ?: detail.snapshot?.coverUrl,
            videoUrl = detail.videoUrl ?: detail.snapshot?.videoUrl,
        )
    }

    override suspend fun getModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>> =
        runCatching {
            getCachedQuickCreateModelCatalog(kind).takeIf { it.isNotEmpty() }
                ?: loadModels(kind = kind, forceRefresh = true).also { saveQuickCreateModelCatalog(kind, it) }
        }

    override suspend fun hasCachedModels(kind: QuickCreationServiceKind): Boolean =
        getCachedQuickCreateModelCatalog(kind).isNotEmpty()

    override suspend fun refreshModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>> =
        runCatching {
            loadModels(kind = kind, forceRefresh = true).also { saveQuickCreateModelCatalog(kind, it) }
        }

    private suspend fun loadModels(
        kind: QuickCreationServiceKind,
        forceRefresh: Boolean,
    ): List<QuickCreationServiceModel> {
        val cachedStandardModels = loadCachedStandardModelCatalog(modelCatalogRepository)
            .toQuickCreationServiceModels(kind)
        catalogDebug(
            "loadModels kind=$kind forceRefresh=$forceRefresh cachedStandard=${cachedStandardModels.size} " +
                "cachedStandardTypes=${cachedStandardModels.serviceModelTypeDistributionLog()}",
        )

        // Repository 边界只接受领域类别，远端 categoryId 在 Data 层统一映射和记录。
        val categoryId = kind.toRemoteCategoryId()
        val response = quickCreateApi.getQuickCreationModels(listOf(categoryId))
        debug("QuickCreationV2", "models response category=$categoryId code=${response.code}")
        if (response.code != 0) {
            throw response.toRepositoryException(QuickCreateRepositoryIssueCode.MODEL_LIST_LOAD_FAILED)
        }
        val quickCreationModels = QuickCreationModelMapper.flatten(
            fallbackCategoryId = categoryId,
            models = response.data?.categories?.get(categoryId).orEmpty(),
        )
        catalogDebug(
            "quickCreation category=$categoryId count=${quickCreationModels.size} " +
                "types=${quickCreationModels.serviceModelTypeDistributionLog()}",
        )
        return enrichWithStandardModelCatalog(
            models = quickCreationModels,
            kind = kind,
            forceRefresh = forceRefresh || cachedStandardModels.isEmpty(),
        )
    }

    private suspend fun getCachedQuickCreateModelCatalog(
        kind: QuickCreationServiceKind,
    ): List<QuickCreationServiceModel> {
        modelCatalogCacheMutex.withLock {
            quickCreateModelCatalogMemoryCache[kind]?.let { return it }
        }
        val cached = modelCatalogCacheStore
            ?.getQuickCreateModelCatalog(kind.cacheKey())
            ?.let(::decodeQuickCreateModelCatalogCache)
            .orEmpty()
        if (cached.isNotEmpty()) {
            modelCatalogCacheMutex.withLock {
                quickCreateModelCatalogMemoryCache[kind] = cached
            }
        }
        return cached
    }

    private suspend fun saveQuickCreateModelCatalog(
        kind: QuickCreationServiceKind,
        models: List<QuickCreationServiceModel>,
    ) {
        if (models.isEmpty()) return
        modelCatalogCacheMutex.withLock {
            quickCreateModelCatalogMemoryCache[kind] = models
        }
        modelCatalogCacheStore?.saveQuickCreateModelCatalog(
            kindKey = kind.cacheKey(),
            json = encodeQuickCreateModelCatalogCache(models),
        )
    }

    private suspend fun enrichWithStandardModelCatalog(
        models: List<QuickCreationServiceModel>,
        kind: QuickCreationServiceKind,
        forceRefresh: Boolean,
    ): List<QuickCreationServiceModel> {
        val repository = modelCatalogRepository ?: return models
        val standardSummaries = loadStandardModelCatalog(repository, forceRefresh)
        catalogDebug(
            "standardCatalog kind=$kind forceRefresh=$forceRefresh count=${standardSummaries.size} " +
                "types=${standardSummaries.apiModelTypeDistributionLog()}",
        )
        val summaryById = standardSummaries.associateBy { it.id }

        val quickCreationModels = coroutineScope {
            models.map { model ->
                async {
                    // `/api/qc/v2/models` 仍提供可提交的 bindingId；标准模型详情只用于补齐
                    // `/api/sku/detail` 的脱敏字段、类型、来源和价格展示，不把 endpoint 暴露给 Presentation。
                    val detail = repository.getStandardModelDetail(model.skuId).getOrNull()
                    model.withStandardModelMetadata(summaryById[model.skuId], detail)
                }
            }.awaitAll()
        }
        val quickSkuIds = quickCreationModels.map { it.skuId }.toSet()
        val standardModels = standardSummaries
            .toQuickCreationServiceModels(kind)
            .filterNot { it.skuId in quickSkuIds }
            .enrichWithStandardModelDetails(repository, summaryById)
        return mergeStandardServiceModels(
            quickCreationModels = quickCreationModels,
            standardModels = standardModels,
        )
    }

    override suspend fun listQuickCreationHistory(
        page: Int,
        size: Int,
    ): Result<QuickCreationHistoryPage> = runCatching {
        val response = quickCreateApi.listQuickCreationTasks(page = page, size = size)
        debug("QuickCreationV2", "history list response code=${response.code}")
        if (response.code != 0 || response.data == null) {
            throw response.toRepositoryException(QuickCreateRepositoryIssueCode.HISTORY_LOAD_FAILED)
        }
        response.data.toHistoryPage()
    }

    override suspend fun getQuickCreationHistoryDetail(outputId: String): Result<QuickCreationHistoryItem> =
        runCatching {
            val response = quickCreateApi.getQuickCreationTaskDetail(outputId)
            debug("QuickCreationV2", "history detail response code=${response.code}")
            if (response.code != 0 || response.data == null) {
                throw response.toRepositoryException(QuickCreateRepositoryIssueCode.HISTORY_DETAIL_LOAD_FAILED)
            }
            response.data.toHistoryItem()
        }

    override suspend fun cancelQuickCreationTask(taskId: String): Result<Unit> =
        runCatching {
            val response = quickCreateApi.cancelQuickCreationTask(taskId)
            if (response.code != 0) {
                throw response.toRepositoryException(QuickCreateRepositoryIssueCode.TASK_CANCEL_FAILED)
            }
        }

    override suspend fun listQuickCreationProjects(
        page: Int,
        size: Int,
    ): Result<QuickCreationProjectPage> =
        runCatching {
            val response = quickCreateApi.listQuickCreationProjects(page = page, size = size)
            if (response.code != 0 || response.data == null) {
                throw response.toRepositoryException(QuickCreateRepositoryIssueCode.PROJECT_LIST_LOAD_FAILED)
            }
            response.data.toProjectPage()
        }

    override suspend fun listQuickCreationProjectTasks(
        projectId: String,
        page: Int,
        size: Int,
    ): Result<QuickCreationHistoryPage> =
        runCatching {
            val response = quickCreateApi.listQuickCreationProjectTasks(
                projectId = projectId,
                page = page,
                size = size,
            )
            if (response.code != 0 || response.data == null) {
                throw response.toRepositoryException(QuickCreateRepositoryIssueCode.PROJECT_TASK_LIST_LOAD_FAILED)
            }
            response.data.toHistoryPage()
        }

    override suspend fun createQuickCreationProject(name: String): Result<QuickCreationProject> =
        runCatching {
            val response = quickCreateApi.createQuickCreationProject(name = name)
            if (response.code != 0 || response.data == null) {
                throw response.toRepositoryException(QuickCreateRepositoryIssueCode.PROJECT_CREATE_FAILED)
            }
            response.data.toProject()
        }

    override suspend fun renameQuickCreationProject(projectId: String, name: String): Result<Unit> =
        runCatching {
            val response = quickCreateApi.renameQuickCreationProject(projectId = projectId, name = name)
            if (response.code != 0) {
                throw response.toRepositoryException(QuickCreateRepositoryIssueCode.PROJECT_RENAME_FAILED)
            }
        }

    override suspend fun deleteQuickCreationProject(projectId: String): Result<Unit> =
        runCatching {
            val response = quickCreateApi.deleteQuickCreationProject(projectId = projectId)
            if (response.code != 0) {
                throw response.toRepositoryException(QuickCreateRepositoryIssueCode.PROJECT_DELETE_FAILED)
            }
        }

    override suspend fun pinQuickCreationProject(projectId: String, pinned: Boolean): Result<Unit> =
        runCatching {
            val response = quickCreateApi.pinQuickCreationProject(projectId = projectId, pinned = pinned)
            if (response.code != 0) {
                throw response.toRepositoryException(QuickCreateRepositoryIssueCode.PROJECT_PIN_FAILED)
            }
        }

    override suspend fun getQuickCreationProjectDetail(projectId: String): Result<QuickCreationProject> =
        runCatching {
            val response = quickCreateApi.getQuickCreationProjectDetail(projectId = projectId)
            if (response.code != 0 || response.data == null) {
                throw response.toRepositoryException(QuickCreateRepositoryIssueCode.PROJECT_DETAIL_LOAD_FAILED)
            }
        response.data.toProject()
    }
}

@Serializable
private data class QuickCreateModelCatalogCacheDto(
    val schemaVersion: Int,
    val models: List<QuickCreateServiceModelCacheDto>,
)

@Serializable
private data class QuickCreateServiceModelCacheDto(
    val categoryId: String,
    val groupName: String? = null,
    val bindingId: String,
    val skuId: String,
    val name: String,
    val description: String? = null,
    val apiType: String? = null,
    val apiSource: String? = null,
    val fields: List<QuickCreateServiceFieldCacheDto> = emptyList(),
    val pricing: QuickCreateServicePricingCacheDto? = null,
) {
    fun toDomain(): QuickCreationServiceModel =
        QuickCreationServiceModel(
            categoryId = categoryId,
            groupName = groupName,
            bindingId = bindingId,
            skuId = skuId,
            name = name,
            description = description,
            apiType = apiType,
            apiSource = apiSource,
            fields = fields.map { it.toDomain() },
            pricing = pricing?.toDomain(),
        )
}

@Serializable
private data class QuickCreateServiceFieldCacheDto(
    val fieldKey: String,
    val paramKey: String,
    val fieldType: String,
    val required: Boolean,
    val defaultValue: String? = null,
    val options: List<QuickCreateServiceFieldOptionCacheDto> = emptyList(),
    val maxUploadCount: Int? = null,
    val maxUploadSize: Long? = null,
    val multipleInputs: Boolean = false,
    val uploadMediaKind: String? = null,
    val inputExtra: QuickCreateServiceFieldExtraCacheDto? = null,
    val visible: Boolean = true,
    val rawInputExtraJson: String? = null,
) {
    fun toDomain(): QuickCreationServiceField =
        QuickCreationServiceField(
            fieldKey = fieldKey,
            paramKey = paramKey,
            fieldType = fieldType,
            required = required,
            defaultValue = defaultValue,
            options = options.map { it.toDomain() },
            maxUploadCount = maxUploadCount,
            maxUploadSize = maxUploadSize,
            multipleInputs = multipleInputs,
            uploadMediaKind = uploadMediaKind?.let { runCatching { QuickCreationUploadMediaKind.valueOf(it) }.getOrNull() },
            inputExtra = inputExtra?.toDomain(),
            visible = visible,
            rawInputExtraJson = rawInputExtraJson,
        )
}

@Serializable
private data class QuickCreateServiceFieldExtraCacheDto(
    val title: String? = null,
    val titleEn: String? = null,
    val paramDescription: String? = null,
    val paramDescriptionEn: String? = null,
    val placeholder: String? = null,
    val acceptFormats: List<String> = emptyList(),
    val maxLength: Int? = null,
    val minLength: Int? = null,
    val maxInputCount: Int? = null,
    val ignoreListValueCaseSensitive: Boolean = false,
    val inputChildren: List<QuickCreateServiceFieldInputChildCacheDto> = emptyList(),
) {
    fun toDomain(): QuickCreationServiceFieldExtra =
        QuickCreationServiceFieldExtra(
            title = title,
            titleEn = titleEn,
            paramDescription = paramDescription,
            paramDescriptionEn = paramDescriptionEn,
            placeholder = placeholder,
            acceptFormats = acceptFormats,
            maxLength = maxLength,
            minLength = minLength,
            maxInputCount = maxInputCount,
            ignoreListValueCaseSensitive = ignoreListValueCaseSensitive,
            inputChildren = inputChildren.map { it.toDomain() },
        )
}

@Serializable
private data class QuickCreateServiceFieldInputChildCacheDto(
    val fieldKey: String,
    val paramKey: String,
    val fieldType: String,
    val required: Boolean = false,
    val visible: Boolean = true,
    val defaultValue: String? = null,
    val title: String? = null,
    val paramDescription: String? = null,
    val placeholder: String? = null,
    val maxLength: Int? = null,
    val minLength: Int? = null,
    val maxInputCount: Int? = null,
    val uploadMediaKind: String? = null,
    val options: List<QuickCreateServiceFieldOptionCacheDto> = emptyList(),
    val visibleWhen: QuickCreateServiceFieldVisibilityConditionCacheDto? = null,
    val rawInputExtraJson: String? = null,
    val rawVisibilityConditionJson: String? = null,
) {
    fun toDomain(): QuickCreationServiceFieldInputChild =
        QuickCreationServiceFieldInputChild(
            fieldKey = fieldKey,
            paramKey = paramKey,
            fieldType = fieldType,
            required = required,
            visible = visible,
            defaultValue = defaultValue,
            title = title,
            paramDescription = paramDescription,
            placeholder = placeholder,
            maxLength = maxLength,
            minLength = minLength,
            maxInputCount = maxInputCount,
            uploadMediaKind = uploadMediaKind?.let { runCatching { QuickCreationUploadMediaKind.valueOf(it) }.getOrNull() },
            options = options.map { it.toDomain() },
            visibleWhen = visibleWhen?.toDomain(),
            rawInputExtraJson = rawInputExtraJson,
            rawVisibilityConditionJson = rawVisibilityConditionJson,
        )
}

@Serializable
private data class QuickCreateServiceFieldOptionCacheDto(
    val label: String,
    val value: String,
) {
    fun toDomain(): QuickCreationServiceFieldOption =
        QuickCreationServiceFieldOption(label = label, value = value)
}

@Serializable
private data class QuickCreateServiceFieldVisibilityConditionCacheDto(
    val fieldKey: String,
    val values: List<String> = emptyList(),
) {
    fun toDomain(): QuickCreationServiceFieldVisibilityCondition =
        QuickCreationServiceFieldVisibilityCondition(fieldKey = fieldKey, values = values)
}

@Serializable
private data class QuickCreateServicePricingCacheDto(
    val pricingMode: String? = null,
    val settlementMode: String? = null,
    val paidPriceKind: String? = null,
    val flatPriceRaw: String? = null,
    val dimensionPricingRaw: String? = null,
    val priceSummaryRaw: String? = null,
    val discountPercent: Int? = null,
    val isFree: Boolean = false,
    val freeRemaining: Int = 0,
    val isTimeFree: Boolean = false,
    val promoType: String? = null,
) {
    fun toDomain(): QuickCreationServicePricing =
        QuickCreationServicePricing(
            pricingMode = pricingMode,
            settlementMode = settlementMode,
            paidPriceKind = paidPriceKind,
            flatPriceRaw = flatPriceRaw,
            dimensionPricingRaw = dimensionPricingRaw,
            priceSummaryRaw = priceSummaryRaw,
            discountPercent = discountPercent,
            isFree = isFree,
            freeRemaining = freeRemaining,
            isTimeFree = isTimeFree,
            promoType = promoType,
        )
}

private fun encodeQuickCreateModelCatalogCache(models: List<QuickCreationServiceModel>): String =
    quickCreationParamJson.encodeToString(
        QuickCreateModelCatalogCacheDto(
            schemaVersion = QUICK_CREATE_MODEL_CATALOG_CACHE_SCHEMA_VERSION,
            models = models.map { it.toCacheDto() },
        )
    )

private fun decodeQuickCreateModelCatalogCache(cacheJson: String): List<QuickCreationServiceModel> =
    runCatching {
        val cache = quickCreationParamJson.decodeFromString<QuickCreateModelCatalogCacheDto>(cacheJson)
        if (cache.schemaVersion == QUICK_CREATE_MODEL_CATALOG_CACHE_SCHEMA_VERSION) {
            cache.models.map { it.toDomain() }
        } else {
            emptyList()
        }
    }.getOrDefault(emptyList())

private fun QuickCreationServiceKind.cacheKey(): String =
    name.lowercase()

private fun QuickCreationServiceModel.toCacheDto(): QuickCreateServiceModelCacheDto =
    QuickCreateServiceModelCacheDto(
        categoryId = categoryId,
        groupName = groupName,
        bindingId = bindingId,
        skuId = skuId,
        name = name,
        description = description,
        apiType = apiType,
        apiSource = apiSource,
        fields = fields.map { it.toCacheDto() },
        pricing = pricing?.toCacheDto(),
    )

private fun QuickCreationServiceField.toCacheDto(): QuickCreateServiceFieldCacheDto =
    QuickCreateServiceFieldCacheDto(
        fieldKey = fieldKey,
        paramKey = paramKey,
        fieldType = fieldType,
        required = required,
        defaultValue = defaultValue,
        options = options.map { it.toCacheDto() },
        maxUploadCount = maxUploadCount,
        maxUploadSize = maxUploadSize,
        multipleInputs = multipleInputs,
        uploadMediaKind = uploadMediaKind?.name,
        inputExtra = inputExtra?.toCacheDto(),
        visible = visible,
        rawInputExtraJson = rawInputExtraJson,
    )

private fun QuickCreationServiceFieldExtra.toCacheDto(): QuickCreateServiceFieldExtraCacheDto =
    QuickCreateServiceFieldExtraCacheDto(
        title = title,
        titleEn = titleEn,
        paramDescription = paramDescription,
        paramDescriptionEn = paramDescriptionEn,
        placeholder = placeholder,
        acceptFormats = acceptFormats,
        maxLength = maxLength,
        minLength = minLength,
        maxInputCount = maxInputCount,
        ignoreListValueCaseSensitive = ignoreListValueCaseSensitive,
        inputChildren = inputChildren.map { it.toCacheDto() },
    )

private fun QuickCreationServiceFieldInputChild.toCacheDto(): QuickCreateServiceFieldInputChildCacheDto =
    QuickCreateServiceFieldInputChildCacheDto(
        fieldKey = fieldKey,
        paramKey = paramKey,
        fieldType = fieldType,
        required = required,
        visible = visible,
        defaultValue = defaultValue,
        title = title,
        paramDescription = paramDescription,
        placeholder = placeholder,
        maxLength = maxLength,
        minLength = minLength,
        maxInputCount = maxInputCount,
        uploadMediaKind = uploadMediaKind?.name,
        options = options.map { it.toCacheDto() },
        visibleWhen = visibleWhen?.toCacheDto(),
        rawInputExtraJson = rawInputExtraJson,
        rawVisibilityConditionJson = rawVisibilityConditionJson,
    )

private fun QuickCreationServiceFieldOption.toCacheDto(): QuickCreateServiceFieldOptionCacheDto =
    QuickCreateServiceFieldOptionCacheDto(label = label, value = value)

private fun QuickCreationServiceFieldVisibilityCondition.toCacheDto():
    QuickCreateServiceFieldVisibilityConditionCacheDto =
    QuickCreateServiceFieldVisibilityConditionCacheDto(fieldKey = fieldKey, values = values)

private fun QuickCreationServicePricing.toCacheDto(): QuickCreateServicePricingCacheDto =
    QuickCreateServicePricingCacheDto(
        pricingMode = pricingMode,
        settlementMode = settlementMode,
        paidPriceKind = paidPriceKind,
        flatPriceRaw = flatPriceRaw,
        dimensionPricingRaw = dimensionPricingRaw,
        priceSummaryRaw = priceSummaryRaw,
        discountPercent = discountPercent,
        isFree = isFree,
        freeRemaining = freeRemaining,
        isTimeFree = isTimeFree,
        promoType = promoType,
    )

/**
 * 将快捷创作服务领域类别映射为当前远程接口需要的分类 ID。
 *
 * 映射保留在 Data 层，避免 Domain 和 Presentation 依赖服务端字符串常量；
 * 如果后续接口调整分类编码，只需要修改本函数和相关 mapper 测试。
 */
private fun QuickCreationServiceKind.toRemoteCategoryId(): String =
    when (this) {
        QuickCreationServiceKind.IMAGE -> "IMAGE"
        QuickCreationServiceKind.VIDEO -> "VIDEO"
    }

private suspend fun loadCachedStandardModelCatalog(
    repository: ModelCatalogRepository?,
): List<ApiModelSummary> {
    repository ?: return emptyList()
    val groupedModels = loadCachedGroupedStandardModelCatalog(repository)
    if (groupedModels.isNotEmpty()) {
        return groupedModels
    }
    val models = mutableListOf<ApiModelSummary>()
    for (page in 1..STANDARD_MODEL_CATALOG_MAX_PAGES) {
        val pageModels = repository.getCachedStandardModels(
            page = page,
            size = STANDARD_MODEL_CATALOG_PAGE_SIZE,
        )
        catalogDebug("cachedStandard page=$page size=${pageModels.size}")
        if (pageModels.isEmpty()) {
            break
        }
        models += pageModels
        if (pageModels.size < STANDARD_MODEL_CATALOG_PAGE_SIZE) {
            break
        }
    }
    return models.distinctBy { it.id }
}

private suspend fun loadCachedGroupedStandardModelCatalog(
    repository: ModelCatalogRepository,
): List<ApiModelSummary> {
    val groups = repository.getCachedStandardModelGroups()
    catalogDebug("cachedStandardGroups count=${groups.size} groups=${groups.modelGroupDistributionLog()}")
    if (groups.isEmpty()) {
        return emptyList()
    }

    val models = mutableListOf<ApiModelSummary>()
    groups.forEach { group ->
        val groupModels = repository.getCachedStandardModelsByGroup(
            group = group,
            page = 1,
            size = STANDARD_MODEL_CATALOG_GROUP_PAGE_SIZE,
        )
        catalogDebug(
            "cachedStandardGroup name=${group.name} id=${group.id} size=${groupModels.size} " +
                "types=${groupModels.apiModelTypeDistributionLog()}",
        )
        models += groupModels
    }
    // 分组缓存是快捷创作冷启动快照的主来源，按分组顺序去重可保留与目录页一致的模型族归属。
    return models.distinctBy { it.id }
}

private suspend fun loadStandardModelCatalog(
    repository: ModelCatalogRepository,
    forceRefresh: Boolean,
): List<ApiModelSummary> {
    val groupedModels = loadGroupedStandardModelCatalog(repository, forceRefresh)
    if (groupedModels.isNotEmpty()) {
        return groupedModels
    }
    return loadPagedStandardModelCatalog(repository, forceRefresh)
}

private suspend fun loadGroupedStandardModelCatalog(
    repository: ModelCatalogRepository,
    forceRefresh: Boolean,
): List<ApiModelSummary> {
    val groups = repository.listStandardModelGroups().getOrNull().orEmpty()
    catalogDebug("standardGroups count=${groups.size} groups=${groups.modelGroupDistributionLog()}")
    if (groups.isEmpty()) {
        return emptyList()
    }

    val models = mutableListOf<ApiModelSummary>()
    groups.forEach { group ->
        val groupResult = if (forceRefresh) {
            repository.refreshStandardModelsByGroup(
                group = group,
                page = 1,
                size = STANDARD_MODEL_CATALOG_GROUP_PAGE_SIZE,
            )
        } else {
            repository.listStandardModelsByGroup(
                group = group,
                page = 1,
                size = STANDARD_MODEL_CATALOG_GROUP_PAGE_SIZE,
            )
        }
        val groupModels = groupResult
            .getOrNull()
            .orEmpty()
        catalogDebug(
            "standardGroup forceRefresh=$forceRefresh name=${group.name} id=${group.id} size=${groupModels.size} " +
                "types=${groupModels.apiModelTypeDistributionLog()}",
        )
        models += groupModels
    }
    // 部分服务端分组如“最近上新”会与模型族重复，按服务端分组顺序保留第一个归属。
    return models.distinctBy { it.id }
}

private suspend fun loadPagedStandardModelCatalog(
    repository: ModelCatalogRepository,
    forceRefresh: Boolean,
): List<ApiModelSummary> {
    val models = mutableListOf<ApiModelSummary>()
    for (page in 1..STANDARD_MODEL_CATALOG_MAX_PAGES) {
        val pageModels = if (forceRefresh) {
            repository.refreshStandardModels(
                page = page,
                size = STANDARD_MODEL_CATALOG_PAGE_SIZE,
            )
        } else {
            repository.listStandardModels(
                page = page,
                size = STANDARD_MODEL_CATALOG_PAGE_SIZE,
            )
        }.getOrNull().orEmpty()
        catalogDebug(
            "standardPage forceRefresh=$forceRefresh page=$page size=${pageModels.size} " +
                "types=${pageModels.apiModelTypeDistributionLog()}",
        )

        if (pageModels.isEmpty()) {
            break
        }
        models += pageModels
        if (pageModels.size < STANDARD_MODEL_CATALOG_PAGE_SIZE) {
            break
        }
    }
    return models.distinctBy { it.id }
}

private fun catalogDebug(message: String) {
    quickCreateDataDebugLog("ModelCatalog", message)
}

private fun List<ApiModelSummary>.toQuickCreationServiceModels(
    kind: QuickCreationServiceKind,
): List<QuickCreationServiceModel> {
    val outputKindByGroup = dominantOutputKindByGroup()
    return filter { model ->
        model.belongsToQuickCreationKind(kind, outputKindByGroup[model.normalizedGroupName()])
    }.map { model ->
        model.toQuickCreationServiceModel(kind, outputKindByGroup[model.normalizedGroupName()])
    }
}

private fun ApiModelSummary.belongsToQuickCreationKind(
    kind: QuickCreationServiceKind,
    groupOutputKind: StandardModelOutputKind?,
): Boolean {
    val outputKind = outputKind(groupOutputKind)
    return when (kind) {
        QuickCreationServiceKind.IMAGE -> outputKind == StandardModelOutputKind.IMAGE
        // 当前 Domain 查询仍只有图片/视频两个入口；VIDEO 查询在数据层承载非图片模型桶，
        // 具体展示大类由接口返回的 type 与服务端模型分组共同归为 VIDEO、AUDIO 或 OTHER。
        QuickCreationServiceKind.VIDEO -> outputKind != StandardModelOutputKind.IMAGE
    }
}

private fun List<ApiModelSummary>.apiModelTypeDistributionLog(): String =
    map { it.type?.takeIf { type -> type.isNotBlank() } ?: "unknown" }
        .toDistributionLog()

private fun List<QuickCreationServiceModel>.serviceModelTypeDistributionLog(): String =
    map { it.apiType?.takeIf { type -> type.isNotBlank() } ?: "unknown" }
        .toDistributionLog()

private fun List<String>.toDistributionLog(): String =
    groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(8)
        .joinToString(prefix = "[", postfix = "]") { "${it.key}:${it.value}" }

private fun List<ApiModelGroup>.modelGroupDistributionLog(): String =
    take(8).joinToString(prefix = "[", postfix = "]") { "${it.name}:${it.apiCount}" }

private fun ApiModelSummary.toQuickCreationServiceModel(
    kind: QuickCreationServiceKind,
    groupOutputKind: StandardModelOutputKind?,
): QuickCreationServiceModel =
    QuickCreationServiceModel(
        categoryId = toQuickCreationServiceCategoryId(kind, groupOutputKind),
        groupName = groupName,
        // 标准目录模型没有 quickcreate bindingId；这里使用 skuId 形成稳定 UI 身份。
        // 真正提交标准模型时仍应走 ModelInvocationRepository，而不是旧 quickcreate binding 路由。
        bindingId = id,
        skuId = id,
        name = name.ifBlank { id },
        description = null,
        apiType = type,
        apiSource = source,
        fields = emptyList(),
        pricing = QuickCreationServicePricing(priceSummaryRaw = priceSummary),
    )

private suspend fun List<QuickCreationServiceModel>.enrichWithStandardModelDetails(
    repository: ModelCatalogRepository,
    summaryById: Map<String, ApiModelSummary>,
): List<QuickCreationServiceModel> =
    map { model ->
        val summary = summaryById[model.skuId]
        if (!model.requiresStandardDetailEnrichment(summary)) {
            return@map model
        }
        // 只有自部署开源模型依赖 `/api/sku/detail` 暴露 LoRA、输出格式和泛文件输入等动态字段；
        // 普通标准目录模型已由列表摘要满足展示，不能在打开模型弹层时全量回源数百个详情请求。
        val detail = repository.getStandardModelDetail(model.skuId).getOrNull()
        model.withStandardModelMetadata(summary, detail)
    }

private fun QuickCreationServiceModel.requiresStandardDetailEnrichment(summary: ApiModelSummary?): Boolean {
    val groupName = summary?.groupName?.takeIf { it.isNotBlank() } ?: groupName
    return groupName?.trim() in STANDARD_MODEL_DETAIL_ENRICHED_GROUP_NAMES
}

private fun ApiModelSummary.toQuickCreationServiceCategoryId(
    kind: QuickCreationServiceKind,
    groupOutputKind: StandardModelOutputKind?,
): String {
    val outputKind = outputKind(groupOutputKind)
    return when {
        kind == QuickCreationServiceKind.IMAGE -> "IMAGE"
        else -> outputKind.categoryId
    }
}

// 标准模型页的顶部 tag 表示模型分组，左侧筛选表示单个 API 的能力类型。
// motion-control、text-to-lyrics、upload-file 这类能力类型本身无法表达最终大类，
// 因此只在 type 没有明确输出时，使用同一服务端分组内其它模型的明确输出分布兜底。
private fun List<ApiModelSummary>.dominantOutputKindByGroup(): Map<String, StandardModelOutputKind> =
    groupBy { it.normalizedGroupName() }
        .filterKeys { it.isNotEmpty() }
        .mapNotNull { (groupName, groupModels) ->
            val dominantKind = groupModels
                .mapNotNull { it.explicitOutputKind() }
                .groupingBy { it }
                .eachCount()
                .maxWithOrNull(compareBy<Map.Entry<StandardModelOutputKind, Int>> { it.value }.thenBy { it.key.priority })
                ?.key
            dominantKind?.let { groupName to it }
        }
        .toMap()

private fun ApiModelSummary.outputKind(groupOutputKind: StandardModelOutputKind?): StandardModelOutputKind =
    explicitOutputKind() ?: groupOutputKind ?: StandardModelOutputKind.OTHER

private fun ApiModelSummary.explicitOutputKind(): StandardModelOutputKind? =
    type.explicitOutputKind()

private fun String?.explicitOutputKind(): StandardModelOutputKind? {
    val text = orEmpty().trim().lowercase()
    return when {
        text.isBlank() -> null
        text.contains("-to-3d") || text.contains("to-3d") -> StandardModelOutputKind.OTHER
        text.contains("-to-music") || text.contains("-to-audio") -> StandardModelOutputKind.AUDIO
        text.contains("-to-video") || text.contains("reference-to-video") -> StandardModelOutputKind.VIDEO
        text.contains("-to-image") -> StandardModelOutputKind.IMAGE
        text.contains("video") -> StandardModelOutputKind.VIDEO
        text.contains("audio") || text.contains("music") -> StandardModelOutputKind.AUDIO
        text.contains("image") -> StandardModelOutputKind.IMAGE
        text.contains("3d") -> StandardModelOutputKind.OTHER
        else -> null
    }
}

private fun ApiModelSummary.normalizedGroupName(): String =
    groupName.orEmpty().trim()

private enum class StandardModelOutputKind(
    val categoryId: String,
    val priority: Int,
) {
    IMAGE(categoryId = "IMAGE", priority = 0),
    VIDEO(categoryId = "VIDEO", priority = 1),
    AUDIO(categoryId = "AUDIO", priority = 2),
    OTHER(categoryId = "OTHER", priority = 3),
}

private fun mergeStandardServiceModels(
    quickCreationModels: List<QuickCreationServiceModel>,
    standardModels: List<QuickCreationServiceModel>,
): List<QuickCreationServiceModel> {
    val quickSkuIds = quickCreationModels.map { it.skuId }.toSet()
    return quickCreationModels + standardModels.filterNot { it.skuId in quickSkuIds }
}

private fun QuickCreationServiceModel.withStandardModelMetadata(
    summary: ApiModelSummary?,
    detail: ApiModelDetail?,
): QuickCreationServiceModel {
    if (summary == null && detail == null) return this
    val standardFields = detail
        ?.fields
        ?.takeIf { it.isNotEmpty() }
        ?.map { it.toQuickCreationServiceField() }
        ?: fields
    val standardPrice = detail?.priceSummary?.takeIf { it.isNotBlank() }
        ?: summary?.priceSummary?.takeIf { it.isNotBlank() }

    return copy(
        groupName = detail?.groupName?.takeIf { it.isNotBlank() }
            ?: summary?.groupName?.takeIf { it.isNotBlank() }
            ?: groupName,
        name = detail?.name?.takeIf { it.isNotBlank() }
            ?: summary?.name?.takeIf { it.isNotBlank() }
            ?: name,
        apiType = detail?.type?.takeIf { it.isNotBlank() }
            ?: summary?.type?.takeIf { it.isNotBlank() }
            ?: apiType,
        apiSource = detail?.source?.takeIf { it.isNotBlank() }
            ?: summary?.source?.takeIf { it.isNotBlank() }
            ?: apiSource,
        fields = standardFields,
        pricing = pricing.withStandardPriceSummary(standardPrice),
    )
}

private fun ApiModelField.toQuickCreationServiceField(): QuickCreationServiceField =
    QuickCreationServiceField(
        fieldKey = fieldKey,
        paramKey = paramKey,
        fieldType = type.name,
        required = required,
        defaultValue = defaultValue,
        options = options.map { option ->
            QuickCreationServiceFieldOption(label = option.label, value = option.value)
        },
        maxUploadCount = maxUploadCount ?: maxInputCount,
        maxUploadSize = maxUploadSizeBytes,
        multipleInputs = multipleInputs,
        uploadMediaKind = type.toQuickCreationUploadMediaKind(),
        inputExtra = toQuickCreationServiceFieldExtra(),
        visible = visible,
        rawInputExtraJson = rawConfigJson,
    )

private fun ApiModelField.toQuickCreationServiceFieldExtra(): QuickCreationServiceFieldExtra? {
    val extra = QuickCreationServiceFieldExtra(
        title = title,
        paramDescription = description,
        placeholder = placeholder,
        acceptFormats = acceptFormats,
        maxLength = maxLength,
        minLength = minLength,
        maxInputCount = maxInputCount,
    )
    return extra.takeIf {
        !it.title.isNullOrBlank() ||
            !it.paramDescription.isNullOrBlank() ||
            !it.placeholder.isNullOrBlank() ||
            it.acceptFormats.isNotEmpty() ||
            it.maxLength != null ||
            it.minLength != null ||
            it.maxInputCount != null
    }
}

private fun QuickCreationServicePricing?.withStandardPriceSummary(
    priceSummary: String?,
): QuickCreationServicePricing? =
    when {
        priceSummary.isNullOrBlank() -> this
        this == null -> QuickCreationServicePricing(priceSummaryRaw = priceSummary)
        else -> copy(priceSummaryRaw = priceSummary)
    }

private fun ApiModelFieldType.toQuickCreationUploadMediaKind(): QuickCreationUploadMediaKind? =
    when (this) {
        ApiModelFieldType.IMAGE -> QuickCreationUploadMediaKind.IMAGE
        ApiModelFieldType.VIDEO -> QuickCreationUploadMediaKind.VIDEO
        ApiModelFieldType.AUDIO -> QuickCreationUploadMediaKind.AUDIO
        else -> null
    }
