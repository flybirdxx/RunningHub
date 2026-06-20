package com.runninghub.shared.data.remote.api

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.shared.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private fun debug(tag: String, msg: String) {
    println("[$tag] $msg")
}

private const val OPEN_API_V2_PATH_PREFIX = "/openapi/v2/"

/**
 * 将保留完整 path 的模型直调常量转换为 OpenAPI v2 相对路径。
 *
 * 历史常量需要继续暴露完整请求 path 供 MockEngine 测试断言，因此这里不修改常量本身。
 * 运行时 URL 统一交给 [RunningHubApiEnvironment.openApiV2Url] 生成，避免各模型方法继续手写
 * Web origin 拼接。若误传非 `/openapi/v2/` 常量会立即失败，防止 Web 业务接口被归入模型直调分组。
 */
private fun openApiV2EndpointUrl(endpointPath: String): String {
    require(endpointPath.startsWith(OPEN_API_V2_PATH_PREFIX)) {
        "OpenAPI v2 endpoint must start with $OPEN_API_V2_PATH_PREFIX"
    }
    return RunningHubApiEnvironment.openApiV2Url(endpointPath.removePrefix(OPEN_API_V2_PATH_PREFIX))
}

/**
 * 快捷创作与模型直调相关接口的 Data 层访问入口。
 *
 * 本类集中维护当前 Web 快捷创作和 OpenAPI v2 的远端路径，Repository 负责把响应转换为
 * Domain 模型。路径常量仍保留在 Data 层，避免继续向 Presentation 扩散具体 endpoint。
 */
class QuickCreateApi(private val client: HttpClient, private val json: Json) {

    companion object {
        /** RunningHub Web 根 origin，不带末尾斜杠，用于兼容当前以 `/` 开头的 endpoint 常量。 */
        const val BASE_URL = RunningHubApiEnvironment.WEB_ORIGIN
        private const val TAG = "QuickCreateApi"

        // 通用端点
        const val TASK_QUERY = "/openapi/v2/query"
        const val MEDIA_UPLOAD = "/openapi/v2/media/upload/binary"

        // Web 快捷创作 v2 端点
        const val QC_CATEGORIES = "/api/qc/v2/categories"
        const val QC_MODELS = "/api/qc/v2/models"
        const val QC_CREATION_MODES = "/api/qc/v2/creation-modes"
        const val QC_FEE_PREVIEW = "/task/quick-creation/fee-preview"
        const val QC_PREPARE = "/task/quick-creation/prepare"
        const val QC_COMMIT = "/task/quick-creation/commit"
        const val QC_TASK_LIST = "/task/quick-creation/list"
        const val QC_TASK_DETAIL = "/task/quick-creation/detail"
        const val QC_TASK_CANCEL = "/task/quick-creation/cancel"
        const val QC_PROJECT_LIST = "/task/quick-creation/project/list"
        const val QC_PROJECT_TASKS = "/task/quick-creation/project/tasks"
        const val QC_PROJECT_CREATE = "/task/quick-creation/project/create"
        const val QC_PROJECT_RENAME = "/task/quick-creation/project/rename"
        const val QC_PROJECT_DELETE = "/task/quick-creation/project/delete"
        const val QC_PROJECT_PIN = "/task/quick-creation/project/pin"
        const val QC_PROJECT_DETAIL = "/task/quick-creation/project/detail"
        const val QC_INSPIRATION_TAGS = "/task/quick-creation/inspiration/tags"
        const val QC_INSPIRATION_TEMPLATES = "/task/quick-creation/inspiration/templates"
        const val QC_INSPIRATION_TEMPLATE_DETAIL = "/task/quick-creation/inspiration/template/detail"

        // 图片模型端点 (全能图片G-2.0 官方)
        const val IMAGE_G2_TEXT = "/openapi/v2/rhart-image-g-2-official/text-to-image"
        const val IMAGE_G2_IMAGE = "/openapi/v2/rhart-image-g-2-official/image-to-image"
        // 全能图片G-2.0 低价
        const val IMAGE_G2_CHEAP_TEXT = "/openapi/v2/rhart-image-g-2/text-to-image"
        const val IMAGE_G2_CHEAP_IMAGE = "/openapi/v2/rhart-image-g-2/image-to-image"

        // 图片模型端点
        // 全能图片 X 官方
        const val IMAGE_X_TEXT = "/openapi/v2/rhart-image-x-official/text-to-image"
        const val IMAGE_X_IMAGE = "/openapi/v2/rhart-image-x-official/image-to-image"
        // 全能图片 X 低价
        const val IMAGE_X_CHEAP_TEXT = "/openapi/v2/rhart-image-x/text-to-image"
        const val IMAGE_X_CHEAP_IMAGE = "/openapi/v2/rhart-image-x/image-to-image"
        // 全能图片 PRO 官方
        const val IMAGE_PRO_TEXT = "/openapi/v2/rhart-image-pro-official/text-to-image"
        const val IMAGE_PRO_IMAGE = "/openapi/v2/rhart-image-pro-official/image-to-image"
        // 全能图片 PRO 低价
        const val IMAGE_PRO_CHEAP_TEXT = "/openapi/v2/rhart-image-n-pro/text-to-image"
        const val IMAGE_PRO_CHEAP_IMAGE = "/openapi/v2/rhart-image-n-pro/image-to-image"
        // 全能图片 2.0 官方
        const val IMAGE_V2_TEXT = "/openapi/v2/rhart-image-v2-official/text-to-image"
        const val IMAGE_V2_IMAGE = "/openapi/v2/rhart-image-v2-official/image-to-image"
        // 全能图片 2.0 低价
        const val IMAGE_V2_CHEAP_TEXT = "/openapi/v2/rhart-image-n-g31-flash/text-to-image"
        const val IMAGE_V2_CHEAP_IMAGE = "/openapi/v2/rhart-image-n-g31-flash/image-to-image"
        // Seedream 5.0 Lite / 4.0
        const val IMAGE_SEEDREAM5_TEXT = "/openapi/v2/seedream-v5-lite/text-to-image"
        const val IMAGE_SEEDREAM5_IMAGE = "/openapi/v2/seedream-v5-lite/image-to-image"
        const val IMAGE_SEEDREAM4_TEXT = "/openapi/v2/seedream-v4/text-to-image"
        const val IMAGE_SEEDREAM4_IMAGE = "/openapi/v2/seedream-v4/image-to-image"

        // 视频模型端点
        // HappyHorse
        const val VIDEO_HH_TEXT = "/openapi/v2/alibaba/happyhorse-1.0/text-to-video"
        const val VIDEO_HH_IMAGE = "/openapi/v2/alibaba/happyhorse-1.0/image-to-video"
        // Seedance 2.0
        const val VIDEO_SD2_TEXT = "/openapi/v2/rhart-video/sparkvideo-2.0/text-to-video"
        const val VIDEO_SD2_IMAGE = "/openapi/v2/rhart-video/sparkvideo-2.0/image-to-video"
        // Seedance 2.0 Fast
        const val VIDEO_SD2F_TEXT = "/openapi/v2/rhart-video/sparkvideo-2.0-fast/text-to-video"
        const val VIDEO_SD2F_IMAGE = "/openapi/v2/rhart-video/sparkvideo-2.0-fast/image-to-video"
        // 可灵 O3-4K
        const val VIDEO_KL3_IMAGE = "/openapi/v2/kling-video-o3-4k/image-to-video"
        // 可灵 O3-Pro
        const val VIDEO_KLO3P_TEXT = "/openapi/v2/kling-video-o3-pro/text-to-video"
        const val VIDEO_KLO3P_IMAGE = "/openapi/v2/kling-video-o3-pro/image-to-video"
        // 可灵 O3-Std
        const val VIDEO_KLO3S_TEXT = "/openapi/v2/kling-video-o3-std/text-to-video"
        const val VIDEO_KLO3S_IMAGE = "/openapi/v2/kling-video-o3-std/image-to-video"
        // 可灵 O1
        const val VIDEO_KLO1_TEXT = "/openapi/v2/kling-video-o1/text-to-video"
        const val VIDEO_KLO1_IMAGE = "/openapi/v2/kling-video-o1/image-to-video"
        // 万相 2.7
        const val VIDEO_WAN27_TEXT = "/openapi/v2/alibaba/wan-2.7/text-to-video"
        const val VIDEO_WAN27_IMAGE = "/openapi/v2/alibaba/wan-2.7/image-to-video"
        // 万相 2.6
        const val VIDEO_WAN26_IMAGE = "/openapi/v2/alibaba/wan-2.6/image-to-video"
        // PixVerse V6
        const val VIDEO_PX6_TEXT = "/openapi/v2/pixverse-v6/text-to-video"
        const val VIDEO_PX6_IMAGE = "/openapi/v2/pixverse-v6/image-to-video"
        // 全能视频 V3.1 Fast
        const val VIDEO_V31F_TEXT = "/openapi/v2/rhart-video-v3.1-fast/text-to-video"
        const val VIDEO_V31F_IMAGE = "/openapi/v2/rhart-video-v3.1-fast/image-to-video"
        const val VIDEO_V31F_START_END = "/openapi/v2/rhart-video-v3.1-fast/start-end-to-video"
        // 全能视频 V3.1 Pro
        const val VIDEO_V31P_TEXT = "/openapi/v2/rhart-video-v3.1-pro/text-to-video"
        const val VIDEO_V31P_IMAGE = "/openapi/v2/rhart-video-v3.1-pro/image-to-video"
        // 全能视频 X
        const val VIDEO_VX_TEXT = "/openapi/v2/rhart-video-g-official/text-to-video"
        const val VIDEO_VX_IMAGE = "/openapi/v2/rhart-video-g-official/image-to-video"
        const val VIDEO_VXC_IMAGE = "/openapi/v2/rhart-video-g/image-to-video"
        // Vidu Q3
        const val VIDEO_VIDUQ3_TEXT = "/openapi/v2/vidu/text-to-video-q3-pro"
        const val VIDEO_VIDUQ3_IMAGE = "/openapi/v2/vidu/image-to-video-q3-pro"
        const val VIDEO_VIDUQ3_FAST_TEXT = "/openapi/v2/vidu/text-to-video-q3-turbo"
        const val VIDEO_VIDUQ3_FAST_IMAGE = "/openapi/v2/vidu/image-to-video-q3-turbo"
    }

    // ── 通用任务查询 ───────────────────────────────────

    /**
     * 查询 OpenAPI v2 任务状态。
     *
     * 任务查询接口属于 `/openapi/v2` 分组，统一通过 [RunningHubApiEnvironment.openApiV2Url]
     * 生成地址；[TASK_QUERY] 仍保留完整 path，便于现有测试按实际请求路径匹配。
     */
    suspend fun queryTask(taskId: String): QuickCreateTaskQueryResponseDto =
        client.get(RunningHubApiEnvironment.openApiV2Url("query")) {
            parameter("taskId", taskId)
        }.body()

    // ── Web 快捷创作 v2 ───────────────────────────────

    /**
     * 拉取快捷创作一级分类。
     *
     * 分类接口属于 `/api/qc/v2` 业务分组，统一通过 [RunningHubApiEnvironment.apiUrl]
     * 拼接地址；常量仍保留完整 path，便于现有 MockEngine 测试按实际请求 path 匹配。
     */
    suspend fun getQuickCreationCategories(): QuickCreationEnvelopeDto<List<QuickCreationCategoryDto>> =
        client.post(RunningHubApiEnvironment.apiUrl("qc/v2/categories")) {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    /**
     * 按分类拉取快捷创作模型目录。
     *
     * 该目录会被模型选择器和快捷创作状态恢复流程复用，Data API 只保证命中
     * `/api/qc/v2/models`，目录合并和领域模型转换由 Repository 处理。
     */
    suspend fun getQuickCreationModels(categoryIds: List<String>): QuickCreationEnvelopeDto<QuickCreationModelCatalogDto> =
        client.post(RunningHubApiEnvironment.apiUrl("qc/v2/models")) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationModelRequestDto(categoryIds))
        }.body()

    /**
     * 查询某个快捷创作分类支持的创作模式。
     *
     * 模式值仍按服务端字符串返回，避免 Data API 提前绑定 UI 展示语义；
     * 调用方应在 Repository 或 Presentation 层决定默认模式和兼容降级。
     */
    suspend fun getQuickCreationModes(categoryId: String): QuickCreationEnvelopeDto<List<String>> =
        client.post(RunningHubApiEnvironment.apiUrl("qc/v2/creation-modes")) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("categoryId" to categoryId))
        }.body()

    /**
     * 预览快捷创作任务费用。
     *
     * 费用预览属于 Web 端 `/task/quick-creation` 业务分组，不是 `/task/openapi`。
     * 使用 [RunningHubApiEnvironment.webUrl] 可以保留真实服务端路径，同时避免继续拼接 Web origin。
     */
    suspend fun previewQuickCreationFee(
        request: QuickCreationCreateRequestDto,
    ): QuickCreationEnvelopeDto<QuickCreationFeePreviewDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_FEE_PREVIEW)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 为快捷创作提交前准备服务端令牌。
     *
     * prepare 必须与 fee preview、commit 使用同一 Web 业务分组，避免生成链路在不同
     * base URL 之间漂移；具体余额和参数校验仍由 Repository 处理。
     */
    suspend fun prepareQuickCreation(
        request: QuickCreationCreateRequestDto,
    ): QuickCreationEnvelopeDto<QuickCreationPrepareDataDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_PREPARE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 提交已准备好的快捷创作任务。
     *
     * commit 依赖 prepare 返回的令牌，本 API 只保持 endpoint 分组一致；
     * 任务创建后的状态轮询和失败恢复由 Repository/Interactor 负责。
     */
    suspend fun commitQuickCreation(
        request: QuickCreationCommitRequestDto,
    ): QuickCreationEnvelopeDto<QuickCreationCommitDataDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_COMMIT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * 查询快捷创作任务分页列表。
     *
     * 该接口既用于生成后轮询，也用于历史列表入口；统一通过 Web 根路径拼接，
     * 防止后续误归入 API Key 任务 OpenAPI。
     */
    suspend fun listQuickCreationTasks(
        page: Int = 1,
        size: Int = 10,
    ): QuickCreationEnvelopeDto<QuickCreationTaskPageDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_TASK_LIST)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationTaskPageRequestDto(page, size))
        }.body()

    /**
     * 查询单个快捷创作历史任务详情。
     *
     * 详情接口与历史列表同属 Web 快捷创作业务分组，必须使用 [RunningHubApiEnvironment.webUrl]；
     * outputId 的业务含义和结果映射由 Repository 处理，API 层只负责保持远端路径边界。
     */
    suspend fun getQuickCreationTaskDetail(
        outputId: String,
    ): QuickCreationEnvelopeDto<QuickCreationTaskRecordDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_TASK_DETAIL)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationTaskDetailRequestDto(outputId))
        }.body()

    /**
     * 取消快捷创作任务。
     *
     * 取消动作会改变远端任务状态，因此仍归属于 Web 端任务管理分组；
     * taskId 在发送前编码，避免包含空格或斜杠时破坏服务端参数解析。
     */
    suspend fun cancelQuickCreationTask(
        taskId: String,
    ): QuickCreationEnvelopeDto<JsonElement> =
        client.post(RunningHubApiEnvironment.webUrl(QC_TASK_CANCEL)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationTaskCancelRequestDto(taskId.encodeURLParameter()))
        }.body()

    /**
     * 查询快捷创作项目分页列表。
     *
     * 项目列表是 Web 工作台能力，不属于 API Key OpenAPI；通过环境对象拼接路径，
     * 便于后续统一切换 Web origin 或测试环境。
     */
    suspend fun listQuickCreationProjects(
        page: Int = 1,
        size: Int = 20,
    ): QuickCreationEnvelopeDto<QuickCreationProjectPageDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_PROJECT_LIST)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationProjectPageRequestDto(page, size))
        }.body()

    /**
     * 查询指定快捷创作项目下的任务分页。
     *
     * 该接口复用历史任务分页 DTO，但业务入口仍是项目管理分组；
     * Repository 会把缺省分页字段转换为 Domain 分页模型。
     */
    suspend fun listQuickCreationProjectTasks(
        projectId: String,
        page: Int = 1,
        size: Int = 10,
    ): QuickCreationEnvelopeDto<QuickCreationTaskPageDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_PROJECT_TASKS)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationProjectTasksRequestDto(projectId, page, size))
        }.body()

    /**
     * 创建快捷创作项目。
     *
     * 创建项目会写入 Web 端用户工作区，API 层不缓存结果；
     * 返回项目字段存在多版本别名，具体兼容逻辑留在 Repository 映射阶段。
     */
    suspend fun createQuickCreationProject(
        name: String,
    ): QuickCreationEnvelopeDto<QuickCreationProjectDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_PROJECT_CREATE)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationProjectCreateRequestDto(name))
        }.body()

    /**
     * 重命名快捷创作项目。
     *
     * 该方法只发送远端修改请求，不在 DataSource 层维护本地乐观状态；
     * 调用方需要在成功后刷新或更新 Presentation 状态。
     */
    suspend fun renameQuickCreationProject(
        projectId: String,
        name: String,
    ): QuickCreationEnvelopeDto<JsonElement> =
        client.post(RunningHubApiEnvironment.webUrl(QC_PROJECT_RENAME)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationProjectRenameRequestDto(projectId, name))
        }.body()

    /**
     * 删除快捷创作项目。
     *
     * 删除属于远端破坏性操作，API 层保持最小请求体并把业务错误交给 Repository 统一映射。
     */
    suspend fun deleteQuickCreationProject(
        projectId: String,
    ): QuickCreationEnvelopeDto<JsonElement> =
        client.post(RunningHubApiEnvironment.webUrl(QC_PROJECT_DELETE)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationProjectIdRequestDto(projectId))
        }.body()

    /**
     * 设置快捷创作项目置顶状态。
     *
     * pinned 表示目标置顶状态而不是切换动作，调用方应传入最新用户意图；
     * API 层不推断当前置顶值，避免与远端状态不同步。
     */
    suspend fun pinQuickCreationProject(
        projectId: String,
        pinned: Boolean,
    ): QuickCreationEnvelopeDto<JsonElement> =
        client.post(RunningHubApiEnvironment.webUrl(QC_PROJECT_PIN)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationProjectPinRequestDto(projectId, pinned))
        }.body()

    /**
     * 查询快捷创作项目详情。
     *
     * 项目详情返回字段兼容 projectId/id、name/projectName 等服务端差异；
     * API 层只保持 Web endpoint 分组，字段归一化在 Repository 中完成。
     */
    suspend fun getQuickCreationProjectDetail(
        projectId: String,
    ): QuickCreationEnvelopeDto<QuickCreationProjectDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_PROJECT_DETAIL)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationProjectIdRequestDto(projectId))
        }.body()

    /**
     * 查询快捷创作灵感标签。
     *
     * 灵感标签服务于 Web 工作台筛选，不应暴露为 Domain endpoint；
     * 空请求体用于匹配当前服务端协议，后续协议变化只需调整 Data 层。
     */
    suspend fun getQuickCreationInspirationTags(): QuickCreationEnvelopeDto<List<QuickCreationCategoryDto>> =
        client.post(RunningHubApiEnvironment.webUrl(QC_INSPIRATION_TAGS)) {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    /**
     * 查询快捷创作灵感模板分页。
     *
     * tagId 为空时表示全量模板列表；分页字段存在 page/current 与 list/records 两套返回格式，
     * API 层保留原始 DTO，Repository 负责兼容映射。
     */
    suspend fun getQuickCreationInspirationTemplates(
        page: Int = 1,
        size: Int = 20,
        tagId: String? = null,
    ): QuickCreationEnvelopeDto<QuickCreationInspirationTemplatePageDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_INSPIRATION_TEMPLATES)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationInspirationTemplatePageRequestDto(page, size, tagId))
        }.body()

    /**
     * 查询快捷创作灵感模板详情。
     *
     * 详情中可能包含预设参数快照和绑定模型信息，这些字段会影响编辑器初始化；
     * API 层只反序列化 DTO，不在这里生成 UI 可见状态。
     */
    suspend fun getQuickCreationInspirationTemplateDetail(
        templateId: String,
    ): QuickCreationEnvelopeDto<QuickCreationInspirationTemplateDetailDto> =
        client.post(RunningHubApiEnvironment.webUrl(QC_INSPIRATION_TEMPLATE_DETAIL)) {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationInspirationTemplateDetailRequestDto(templateId))
        }.body()

    // ── 媒体上传 ──────────────────────────────────────

    /**
     * 上传模型直调所需的媒体文件。
     *
     * 上传接口属于 `/openapi/v2` 分组，API Key 仅通过 Authorization 请求头发送。
     * 日志只记录文件元信息和脱敏状态，不输出 API Key 前缀，避免调试日志泄露凭据。
     */
    suspend fun uploadMedia(
        apiKey: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String = "application/octet-stream"
    ): MediaUploadResponseDto {
        val url = RunningHubApiEnvironment.openApiV2Url("media/upload/binary")
        debug(TAG, "uploadMedia: START")
        debug(TAG, "  url        = $url")
        debug(TAG, "  apiKey     = <redacted>")
        debug(TAG, "  fileName   = $fileName")
        debug(TAG, "  contentType= $contentType")
        debug(TAG, "  fileBytes  = ${fileBytes.size} bytes")

        val response: MediaUploadResponseDto = client.submitFormWithBinaryData(
            url = url,
            formData = formData {
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    append(HttpHeaders.ContentType, contentType)
                })
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()

        debug(TAG, "uploadMedia: code=${response.code} message=${response.message}")
        debug(TAG, "  response.url       = ${response.url}")
        debug(TAG, "  response.data.type = ${response.data?.type}")
        debug(TAG, "  response.data.fileName = ${response.data?.fileName}")
        return response
    }

    // ── 图片创作 (全能图片G-2.0) ─────────────────────

    suspend fun imageG2TextToImage(request: AllPowerImageG2TextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_G2_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageG2ImageToImage(request: AllPowerImageG2ImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_G2_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片G-2.0 低价版) ────────────────

    suspend fun imageG2CheapTextToImage(request: AllPowerImageG2TextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_G2_CHEAP_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageG2CheapImageToImage(request: AllPowerImageG2ImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_G2_CHEAP_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片X) ──────────────────────────

    suspend fun imageXTextToImage(request: AllPowerImageXTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_X_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageXImageToImage(request: AllPowerImageXImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_X_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片X 低价版) ───────────────────

    suspend fun imageXCheapTextToImage(request: AllPowerImageXTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_X_CHEAP_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageXCheapImageToImage(request: AllPowerImageXImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_X_CHEAP_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片PRO) ────────────────────────

    suspend fun imageProTextToImage(request: AllPowerImageV2ProTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_PRO_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageProImageToImage(request: AllPowerImageV2ProImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_PRO_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片PRO 低价版) ────────────────

    suspend fun imageProCheapTextToImage(request: AllPowerImageV2ProTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_PRO_CHEAP_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageProCheapImageToImage(request: AllPowerImageV2ProImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_PRO_CHEAP_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片V2) ─────────────────────────

    suspend fun imageV2TextToImage(request: AllPowerImageV2ProTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_V2_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageV2ImageToImage(request: AllPowerImageV2ProImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_V2_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片V2 低价版) ────────────────

    suspend fun imageV2CheapTextToImage(request: AllPowerImageV2ProTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_V2_CHEAP_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageV2CheapImageToImage(request: AllPowerImageV2ProImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_V2_CHEAP_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (Seedream 5.0 Lite) ─────────────────

    suspend fun imageSeedream5TextToImage(request: SeedreamV5LiteTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_SEEDREAM5_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageSeedream5ImageToImage(request: SeedreamV5LiteImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_SEEDREAM5_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (Seedream 4.0) ──────────────────────

    suspend fun imageSeedream4TextToImage(request: SeedreamV5LiteTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_SEEDREAM4_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageSeedream4ImageToImage(request: SeedreamV4ImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post(openApiV2EndpointUrl(IMAGE_SEEDREAM4_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (HappyHorse) ────────────────────────

    suspend fun happyHorseTextToVideo(request: HappyHorseTextToVideoRequestDto): HappyHorseTextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_HH_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun happyHorseImageToVideo(request: HappyHorseImageToVideoRequestDto): HappyHorseTextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_HH_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (Seedance 2.0) ─────────────────────

    suspend fun seedance2TextToVideo(request: SeedanceTextToVideoRequestDto): SeedanceTextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_SD2_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun seedance2ImageToVideo(request: SeedanceImageToVideoRequestDto): SeedanceImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_SD2_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (Seedance 2.0 Fast) ───────────────

    suspend fun seedance2FastTextToVideo(request: SeedanceTextToVideoRequestDto): SeedanceTextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_SD2F_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun seedance2FastImageToVideo(request: SeedanceImageToVideoRequestDto): SeedanceImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_SD2F_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (可灵 O3-4K) ───────────────────────

    suspend fun klingO34KImageToVideo(request: KlingO34KImageToVideoRequestDto): KlingO34KImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_KL3_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (可灵 O3-Pro) ──────────────────────

    suspend fun klingO3ProTextToVideo(request: KlingO3ProTextToVideoRequestDto): KlingO3ProTextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_KLO3P_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun klingO3ProImageToVideo(request: KlingO3ProImageToVideoRequestDto): KlingO3ProImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_KLO3P_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (可灵 O3-Std) ──────────────────────

    suspend fun klingO3StdTextToVideo(request: KlingO3StdTextToVideoRequestDto): KlingO3StdTextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_KLO3S_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun klingO3StdImageToVideo(request: KlingO3StdImageToVideoRequestDto): KlingO3StdImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_KLO3S_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (可灵 O1) ───────────────────────────

    suspend fun klingO1TextToVideo(request: KlingO1TextToVideoRequestDto): KlingO1TextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_KLO1_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun klingO1ImageToVideo(request: KlingO1ImageToVideoRequestDto): KlingO1ImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_KLO1_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (万相 2.7) ─────────────────────────

    suspend fun wan27TextToVideo(request: Wan27TextToVideoRequestDto): Wan27TextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_WAN27_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun wan27ImageToVideo(request: Wan27ImageToVideoRequestDto): Wan27ImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_WAN27_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (万相 2.6) ─────────────────────────

    suspend fun wan26ImageToVideo(request: Wan26ImageToVideoRequestDto): Wan26ImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_WAN26_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (PixVerse V6) ─────────────────────

    suspend fun pixVerseV6TextToVideo(request: PixVerseV6TextToVideoRequestDto): PixVerseV6TextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_PX6_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun pixVerseV6ImageToVideo(request: PixVerseV6ImageToVideoRequestDto): PixVerseV6ImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_PX6_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (全能视频V3.1 Fast) ───────────────

    suspend fun allPowerV31FastTextToVideo(request: AllPowerVideoV31FastTextToVideoRequestDto): AllPowerVideoV31FastTextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_V31F_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerV31FastImageToVideo(request: AllPowerVideoV31FastImageToVideoRequestDto): AllPowerVideoV31FastImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_V31F_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerV31FastStartEndToVideo(request: AllPowerVideoV31FastStartEndToVideoRequestDto): AllPowerVideoV31FastStartEndToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_V31F_START_END)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (全能视频V3.1 Pro) ───────────────

    suspend fun allPowerV31ProTextToVideo(request: AllPowerVideoV31ProTextToVideoRequestDto): AllPowerVideoV31ProTextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_V31P_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerV31ProImageToVideo(request: AllPowerVideoV31ProImageToVideoRequestDto): AllPowerVideoV31ProImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_V31P_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (全能视频X) ────────────────────────

    suspend fun allPowerVXTextToVideo(request: AllPowerVideoXTextToVideoRequestDto): AllPowerVideoXTextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_VX_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerVXImageToVideo(request: AllPowerVideoXImageToVideoRequestDto): AllPowerVideoXImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_VX_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerVXCheapImageToVideo(request: AllPowerVideoXCheapImageToVideoRequestDto): AllPowerVideoXCheapImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_VXC_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (Vidu Q3) ─────────────────────────

    suspend fun viduQ3ProTextToVideo(request: ViduQ3TextToVideoRequestDto): ViduQ3TextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_VIDUQ3_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun viduQ3ProImageToVideo(request: ViduQ3ImageToVideoRequestDto): ViduQ3ImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_VIDUQ3_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun viduQ3TurboTextToVideo(request: ViduQ3TextToVideoRequestDto): ViduQ3TextToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_VIDUQ3_FAST_TEXT)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun viduQ3TurboImageToVideo(request: ViduQ3ImageToVideoRequestDto): ViduQ3ImageToVideoResponseDto =
        client.post(openApiV2EndpointUrl(VIDEO_VIDUQ3_FAST_IMAGE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
