package com.runninghub.shared.domain.repository

/**
 * 快捷创作服务字段声明中的媒体类型。
 *
 * 该枚举位于 Domain 层，用于描述服务端字段需要的上传素材类型，不依赖 Compose、
 * 平台媒体选择器或 Presentation 层的媒体引用模型。
 */
enum class QuickCreationUploadMediaKind {
    /** 图片或照片素材，通常用于图生图、参考图或蒙版字段。 */
    IMAGE,

    /** 视频素材，通常用于视频参考、首尾帧以外的视频输入或视频重绘字段。 */
    VIDEO,

    /** 音频素材，通常用于配音、背景音或音视频一体化生成字段。 */
    AUDIO,
}

/**
 * 已上传完成且可参与快捷创作请求规划的媒体。
 *
 * @property mediaKind 媒体类型，来自 Presentation 层对本地素材类型的映射。
 * 该值只描述请求规划需要的业务类型，不携带平台 URI 或文件句柄。
 * @property remoteUrl 上传完成后的远程 URL。
 * 空字符串不应传入本模型；该 URL 会进入生成请求的 `quickCreationListParams`。
 * @property fieldParamKey 绑定的服务字段参数名。
 * `null` 表示全局素材，只有当同类型活跃上传字段唯一时才会自动回填；
 * 非空时表示用户明确为该服务字段上传素材。
 */
data class QuickCreationUploadedMedia(
    val mediaKind: QuickCreationUploadMediaKind,
    val remoteUrl: String,
    val fieldParamKey: String? = null,
)

/**
 * 快捷创作上传字段的别名解析结果。
 *
 * 模板接口可能使用 `fieldKey` 返回素材列表，而生成接口需要 `paramKey` 才能绑定字段。
 * 本模型把两种 key 统一指向同一个字段，并保留字段声明推断出的媒体类型，Presentation 层
 * 只需要把媒体类型映射到自己的 UI 媒体枚举。
 *
 * @property paramKey 生成请求使用的服务字段参数名。
 * 空字符串不应写入别名表；调用方可以把该值作为上传素材的字段绑定 key。
 * @property mediaKind 字段声明推断出的上传媒体类型。
 * `null` 表示服务端字段没有可识别的媒体标记，调用方需要根据模板 key 或业务上下文兜底。
 */
data class QuickCreationServiceUploadFieldAlias(
    val paramKey: String,
    val mediaKind: QuickCreationUploadMediaKind?,
)

/**
 * 快捷创作服务字段在 Domain 层解析后的控件类别。
 *
 * 该枚举只描述字段的业务输入形态，不绑定 Compose 控件、图标或 Presentation 层样式。
 */
enum class QuickCreationResolvedFieldKind {
    /** 字段通过服务端 options 提供固定候选值。 */
    OPTIONS,

    /** 字段接受文本、数字或浮点等键盘输入。 */
    TEXT,

    /** 字段需要用户上传图片、视频或音频素材。 */
    UPLOAD,
}

/**
 * 快捷创作服务字段选项的解析结果。
 *
 * @property label 服务端返回的选项展示名；Presentation 层可以原样展示或再做本地化兜底。
 * @property value 写回服务参数 Map 的原始值。
 */
data class QuickCreationResolvedFieldOption(
    val label: String,
    val value: String,
)

/**
 * 快捷创作服务字段在 Domain 层的解析结果。
 *
 * 本模型把服务端字段声明中的可见性、别名参数、当前值、活跃 child 字段、上传媒体类型和约束
 * 统一解析为平台无关结构。Presentation 层只负责把它映射到具体 UI 控件和可见提示文案，
 * 不再直接解析 `fieldType`、`visibleWhen` 或 `inputExtra`。
 *
 * @property paramKey 字段写回服务参数 Map 的标准参数名。
 * @property title 字段标题，来自服务端标题；为空时使用字段 key 兜底。
 * @property description 字段说明；`null` 表示服务端未提供有效说明。
 * @property kind 字段输入形态，决定 Presentation 层选择选项、文本还是上传控件。
 * @property options 选项字段的候选值；非选项字段为空列表。
 * @property currentValue 当前字段值，优先使用标准 `paramKey`，其次使用 `fieldKey` 和默认值。
 * @property placeholder 文本输入占位内容；未配置时使用 [paramKey] 兜底。
 * @property maxLength 文本最大长度；`null` 表示不限制，非负数表示允许的最大字符数。
 * @property acceptFormats 上传字段接受的格式列表，空列表表示服务端未声明格式约束。
 * @property maxUploadCount 上传字段最大文件数；`null` 表示服务端未声明数量约束。
 * @property maxUploadSizeBytes 单文件最大字节数；`null` 表示服务端未声明大小约束。
 * @property uploadMediaKind 上传字段媒体类型；`null` 表示字段标记无法推断，调用方需要使用通用上传入口。
 * @property childFields 当前参数组合下已经激活的 child 字段，顺序保持服务端声明顺序。
 */
data class QuickCreationResolvedServiceField(
    val paramKey: String,
    val title: String,
    val description: String?,
    val kind: QuickCreationResolvedFieldKind,
    val options: List<QuickCreationResolvedFieldOption>,
    val currentValue: String,
    val placeholder: String,
    val maxLength: Int?,
    val acceptFormats: List<String>,
    val maxUploadCount: Int?,
    val maxUploadSizeBytes: Long?,
    val uploadMediaKind: QuickCreationUploadMediaKind?,
    val childFields: List<QuickCreationResolvedServiceField>,
)

/**
 * 解析快捷创作服务模型字段声明的纯 Domain 规则。
 *
 * 本对象集中处理服务端字段的可见性、默认值、激活 child 字段、字段校验和上传素材绑定。
 * Presentation 层只需要提供当前参数和已上传素材，不再直接理解 `fieldType`、`visibleWhen`、
 * `inputExtra` 等服务端字段细节。
 */
object QuickCreationServiceSchema {

    /**
     * 生成服务字段的默认参数。
     *
     * 只读取可见字段和当前已激活 child 字段，避免隐藏字段把默认值带入计费或提交请求。
     *
     * @param model 当前选中的快捷创作服务模型；`null` 表示未加载到服务端模型。
     * @param activeParams 用户已经选择或输入的参数，用于判断条件 child 字段是否激活。
     * @return 可参与请求和计费预览的默认参数 Map；空 Map 表示没有可用默认值。
     */
    fun defaultParams(
        model: QuickCreationServiceModel?,
        activeParams: Map<String, String> = emptyMap(),
    ): Map<String, String> {
        val visibleFields = model?.fields.orEmpty()
            .filter { it.visible }
        val topLevelDefaults = visibleFields
            .mapNotNull { field ->
                val value = field.defaultValue?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                field.paramKey to value
            }
            .toMap()
        val aliasedDefaults = paramsWithFieldAliases(model, topLevelDefaults + activeParams)
        return buildMap {
            putAll(topLevelDefaults)
            visibleFields.forEach { field ->
                field.activeInputChildren(aliasedDefaults)
                    .mapNotNull { child ->
                        val value = child.defaultValue?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        child.paramKey to value
                    }
                    .forEach { (paramKey, value) -> put(paramKey, value) }
            }
        }
    }

    /**
     * 判断服务模型是否声明了指定参数。
     *
     * @param model 当前服务模型。
     * @param paramKey 用户操作回传的字段参数名，空字符串会返回 `false`。
     * @return `true` 表示该参数属于模型顶层字段或 child 字段；`false` 表示未知参数应被忽略。
     */
    fun hasFieldParam(
        model: QuickCreationServiceModel,
        paramKey: String,
    ): Boolean =
        paramKey.isNotBlank() &&
            model.fields.any { field ->
                field.paramKey == paramKey ||
                    field.inputExtra?.inputChildren.orEmpty().any { child -> child.paramKey == paramKey }
            }

    /**
     * 返回当前服务参数下真正激活的字段 key 集合。
     *
     * 该集合用于决定参数变化是否需要刷新计费预览，以及筛选哪些字段值会进入请求。
     *
     * @param model 当前选中的服务模型；`null` 表示没有可用服务字段。
     * @param serviceParams 用户当前选择或输入的服务参数。
     * @return 当前可见顶层字段和已激活 child 字段的参数名集合。
     */
    fun activeParamKeys(
        model: QuickCreationServiceModel?,
        serviceParams: Map<String, String>,
    ): Set<String> {
        val aliasedParams = paramsWithFieldAliases(model, serviceParams)
        return model?.fields.orEmpty()
            .filter { it.visible }
            .flatMap { field ->
                listOf(field.paramKey) + field.activeInputChildren(aliasedParams).map { it.paramKey }
            }
            .toSet()
    }

    /**
     * 返回当前服务参数下真正激活的上传字段参数名集合。
     *
     * @param model 当前选中的服务模型；`null` 表示没有字段级上传需求。
     * @param serviceParams 用户当前选择或输入的服务参数。
     * @return 可参与上传校验和请求绑定的字段参数名集合；空集合表示没有活跃上传字段。
     */
    fun activeUploadParamKeys(
        model: QuickCreationServiceModel?,
        serviceParams: Map<String, String>,
    ): Set<String> {
        val aliasedParams = paramsWithFieldAliases(model, serviceParams)
        return model?.fields.orEmpty()
            .filter { it.visible }
            .flatMap { field ->
                buildList {
                    if (field.isRenderable() && field.isUploadField()) {
                        add(field.paramKey)
                    }
                    addAll(
                        field.activeInputChildren(aliasedParams)
                            .filter { it.isUploadField() }
                            .map { it.paramKey },
                    )
                }
            }
            .toSet()
    }

    /**
     * 解析参数面板需要展示的服务字段。
     *
     * 本函数会统一处理可见字段、可渲染字段、fieldKey/paramKey 别名、默认值、活跃 child 字段
     * 和上传媒体类型。Presentation 层不应再重复遍历 `fields` 或自行判断 `visibleWhen`，
     * 否则参数面板与请求构建可能出现字段可见性分叉。
     *
     * @param model 当前选中的快捷创作服务模型；`null` 表示没有动态服务字段。
     * @param serviceParams 用户当前选择或输入的服务参数，用于解析当前值和 child 字段激活状态。
     * @return 当前应展示的顶层字段列表；每个字段会携带当前已激活的 child 字段。
     */
    fun resolvedFields(
        model: QuickCreationServiceModel?,
        serviceParams: Map<String, String>,
    ): List<QuickCreationResolvedServiceField> {
        val aliasedParams = paramsWithFieldAliases(model, serviceParams)
        return model?.fields.orEmpty()
            .filter { it.isRenderable() }
            .map { field -> field.toResolvedField(aliasedParams) }
    }

    /**
     * 返回当前服务参数下活跃上传字段的模板别名表。
     *
     * 别名表同时包含 `fieldKey -> alias` 和 `paramKey -> alias`，用于把灵感模板详情中的
     * list 参数素材绑定回生成请求字段。未激活 child 字段不会出现在结果中，避免模板素材绑定到
     * 当前参数组合不可见的字段。
     *
     * @param model 当前选中的服务模型；`null` 表示没有字段级上传需求。
     * @param serviceParams 用户当前选择或模板带入的服务参数，用于判断 child 上传字段是否激活。
     * @return 活跃上传字段别名表；空 Map 表示没有可绑定的上传字段。
     */
    fun activeUploadParamAliases(
        model: QuickCreationServiceModel?,
        serviceParams: Map<String, String>,
    ): Map<String, QuickCreationServiceUploadFieldAlias> {
        val aliasedParams = paramsWithFieldAliases(model, serviceParams)
        return buildMap {
            model?.fields.orEmpty()
                .filter { it.visible }
                .forEach { field ->
                    if (field.isRenderable() && field.isUploadField()) {
                        putUploadAlias(
                            fieldKey = field.fieldKey,
                            paramKey = field.paramKey,
                            mediaKind = field.uploadMediaKind(),
                        )
                    }
                    field.activeInputChildren(aliasedParams)
                        .filter { it.isUploadField() }
                        .forEach { child ->
                            putUploadAlias(
                                fieldKey = child.fieldKey,
                                paramKey = child.paramKey,
                                mediaKind = child.uploadMediaKind(),
                            )
                        }
                }
        }
    }

    /**
     * 返回服务模型声明过的上传字段别名表。
     *
     * 该结果不判断 child 字段是否在当前参数下激活，只用于识别模板详情中的字段素材是否属于
     * 服务模型声明的上传字段。调用方可以据此跳过未激活字段素材，避免把字段素材降级成全局素材。
     *
     * @param model 当前选中的服务模型；`null` 表示没有可识别字段。
     * @return `fieldKey` 和 `paramKey` 到标准 `paramKey` 的映射。
     */
    fun declaredUploadParamAliases(
        model: QuickCreationServiceModel?,
    ): Map<String, String> =
        buildMap {
            model?.fields.orEmpty()
                .filter { it.visible }
                .forEach { field ->
                    if (field.isRenderable() && field.isUploadField()) {
                        putSimpleAlias(field.fieldKey, field.paramKey)
                    }
                    field.inputExtra?.inputChildren.orEmpty()
                        .filter { it.isRenderable() && it.isUploadField() }
                        .forEach { child -> putSimpleAlias(child.fieldKey, child.paramKey) }
                }
        }

    /**
     * 把模板参数归一化为服务字段参数名。
     *
     * 灵感模板详情可能使用 `fieldKey` 返回参数，而计费预览和生成请求需要 `paramKey`。
     * 本函数只根据服务模型声明做 key 归一化，不校验参数值是否属于当前活跃字段；活跃字段筛选
     * 仍由 [activeParamKeys] 和请求规划流程负责。
     *
     * @param model 当前选中的服务模型；`null` 时原样返回 [params]。
     * @param params 模板详情返回的原始参数。
     * @return 使用 `paramKey` 作为主键的参数 Map；无法识别的 key 会被原样保留。
     * 如果模板同时返回 `fieldKey` 和 `paramKey`，则优先保留 `paramKey` 的值。
     */
    fun canonicalParams(
        model: QuickCreationServiceModel?,
        params: Map<String, String>,
    ): Map<String, String> {
        val aliases = serviceParamAliases(model)
        return buildMap {
            params.forEach { (key, value) ->
                put(aliases[key] ?: key, value)
            }
            params.forEach { (key, value) ->
                if (aliases[key] == key) {
                    put(key, value)
                }
            }
        }
    }

    /**
     * 构建服务端列表参数。
     *
     * 字段级素材优先按 [QuickCreationUploadedMedia.fieldParamKey] 精确绑定；
     * 全局素材只在同媒体类型活跃上传字段唯一时自动回填，避免一个素材被错误分配到多个字段。
     *
     * @param model 当前选中的服务模型。
     * @param uploadedMedia 已上传完成且有远程 URL 的素材列表。
     * @param fallbackMediaKind 字段没有明确媒体类型时的兜底类型；`null` 表示不做兜底。
     * @param serviceParams 用户当前选择或输入的服务参数，用于判断 child 上传字段是否激活。
     * @return 生成请求中的 `quickCreationListParams`；空 Map 表示没有可提交的字段素材。
     */
    fun listParams(
        model: QuickCreationServiceModel?,
        uploadedMedia: List<QuickCreationUploadedMedia>,
        fallbackMediaKind: QuickCreationUploadMediaKind?,
        serviceParams: Map<String, String>,
    ): Map<String, List<String>> {
        val urlsByType = uploadedMedia.urlsByType()
        val urlsByField = uploadedMedia.urlsByField()
        val uploadFieldCountByType = uploadFieldCountByType(model, serviceParams, fallbackMediaKind)

        return buildMap {
            model.uploadFields().forEach { field ->
                val mediaKind = field.uploadMediaKind() ?: fallbackMediaKind ?: return@forEach
                val urls = urlsByField[field.paramKey].orEmpty().ifEmpty {
                    urlsByType.fallbackUrlsForSingleUploadField(mediaKind, uploadFieldCountByType)
                }
                if (urls.isNotEmpty()) {
                    val maxCount = field.maxUploadCount ?: urls.size
                    put(field.paramKey, urls.take(maxCount))
                }
            }
            activeChildUploadFields(model, serviceParams).forEach { child ->
                val mediaKind = child.uploadMediaKind() ?: fallbackMediaKind ?: return@forEach
                val urls = urlsByField[child.paramKey].orEmpty().ifEmpty {
                    urlsByType.fallbackUrlsForSingleUploadField(mediaKind, uploadFieldCountByType)
                }
                if (urls.isNotEmpty()) {
                    val maxCount = child.maxInputCount ?: urls.size
                    put(child.paramKey, urls.take(maxCount))
                }
            }
        }
    }

    /**
     * 校验当前服务字段值。
     *
     * Prompt 字段由编辑器主输入负责校验，因此此处会跳过 `prompt` / `promptAi`；
     * 隐藏字段和未激活 child 字段不会阻塞生成。
     *
     * @param model 当前选中的服务模型；`null` 表示没有服务字段需要校验。
     * @param serviceParams 用户当前选择或输入的服务参数。
     * @return 第一条可展示错误；全部通过时返回 `null`。
     */
    fun validateFields(
        model: QuickCreationServiceModel?,
        serviceParams: Map<String, String>,
    ): String? {
        val defaults = defaultParams(model, serviceParams)
        val aliasedParams = paramsWithFieldAliases(model, serviceParams)
        return model?.fields.orEmpty()
            .filter { it.visible }
            .firstNotNullOfOrNull { field ->
                if (field.isPromptField()) {
                    return@firstNotNullOfOrNull null
                }
                if (field.options.isNotEmpty()) {
                    val value = serviceParams[field.paramKey] ?: defaults[field.paramKey].orEmpty()
                    if (field.required && value.isBlank()) {
                        return@firstNotNullOfOrNull "${field.title()} 不能为空"
                    }
                    if (value.isNotBlank() && field.options.none { option -> option.value == value }) {
                        return@firstNotNullOfOrNull "${field.title()} 选项无效"
                    }
                }
                if (field.supportsTextEntry()) {
                    val value = serviceParams[field.paramKey] ?: defaults[field.paramKey].orEmpty()
                    field.textValidationError(value)?.let { return@firstNotNullOfOrNull it }
                }
                field.activeInputChildren(aliasedParams)
                    .firstNotNullOfOrNull { child ->
                        val value = serviceParams[child.paramKey] ?: child.defaultValue.orEmpty()
                        if (child.options.isNotEmpty() && child.required && value.isBlank()) {
                            return@firstNotNullOfOrNull "${child.title()} 不能为空"
                        }
                        if (child.options.isNotEmpty() && value.isNotBlank() && child.options.none { option -> option.value == value }) {
                            return@firstNotNullOfOrNull "${child.title()} 选项无效"
                        }
                        if (child.supportsTextEntry()) {
                            return@firstNotNullOfOrNull child.textValidationError(value)
                        }
                        null
                    }
            }
    }

    /**
     * 校验当前服务上传字段。
     *
     * @param model 当前选中的服务模型；`null` 表示没有服务上传字段。
     * @param uploadedMedia 已上传完成且有远程 URL 的素材列表。
     * @param fallbackMediaKind 字段没有明确媒体类型时的兜底类型；图片创作通常传图片，视频创作通常传 `null`。
     * @param serviceParams 用户当前选择或输入的服务参数。
     * @return 第一条可展示错误；全部通过时返回 `null`。
     */
    fun validateUploads(
        model: QuickCreationServiceModel?,
        uploadedMedia: List<QuickCreationUploadedMedia>,
        fallbackMediaKind: QuickCreationUploadMediaKind?,
        serviceParams: Map<String, String>,
    ): String? {
        val urlsByType = uploadedMedia.urlsByType()
        val urlsByField = uploadedMedia.urlsByField()
        val uploadFieldCountByType = uploadFieldCountByType(model, serviceParams, fallbackMediaKind)
        model.uploadFields()
            .firstNotNullOfOrNull { field ->
                val mediaKind = field.uploadMediaKind() ?: fallbackMediaKind ?: return@firstNotNullOfOrNull null
                val uploadedCount = urlsByField[field.paramKey].orEmpty()
                    .ifEmpty { urlsByType.fallbackUrlsForSingleUploadField(mediaKind, uploadFieldCountByType) }
                    .size
                field.uploadValidationError(uploadedCount)
            }
            ?.let { return it }
        return activeChildUploadFields(model, serviceParams)
            .firstNotNullOfOrNull { child ->
                val mediaKind = child.uploadMediaKind() ?: fallbackMediaKind ?: return@firstNotNullOfOrNull null
                val uploadedCount = urlsByField[child.paramKey].orEmpty()
                    .ifEmpty { urlsByType.fallbackUrlsForSingleUploadField(mediaKind, uploadFieldCountByType) }
                    .size
                child.uploadValidationError(uploadedCount)
            }
    }
}

private fun QuickCreationServiceField.toResolvedField(
    params: Map<String, String>,
): QuickCreationResolvedServiceField {
    val currentValue = params[paramKey] ?: params[fieldKey] ?: defaultValue.orEmpty()
    val maxLength = inputExtra?.maxLength?.takeIf { it >= 0 }
    return QuickCreationResolvedServiceField(
        paramKey = paramKey,
        title = title(),
        description = inputExtra?.paramDescription?.takeIf { it.isNotBlank() },
        kind = resolvedKind(),
        options = options.map { option ->
            QuickCreationResolvedFieldOption(
                label = option.label,
                value = option.value,
            )
        },
        currentValue = currentValue,
        placeholder = inputExtra?.placeholder?.takeIf { it.isNotBlank() } ?: paramKey,
        maxLength = maxLength,
        acceptFormats = inputExtra?.acceptFormats.orEmpty(),
        maxUploadCount = inputExtra?.maxInputCount ?: maxUploadCount,
        maxUploadSizeBytes = maxUploadSize,
        uploadMediaKind = uploadMediaKind(),
        childFields = activeInputChildren(params).map { child -> child.toResolvedField(params) },
    )
}

private fun QuickCreationServiceFieldInputChild.toResolvedField(
    params: Map<String, String>,
): QuickCreationResolvedServiceField {
    val currentValue = params[paramKey] ?: params[fieldKey] ?: defaultValue.orEmpty()
    val maxLength = maxLength?.takeIf { it >= 0 }
    return QuickCreationResolvedServiceField(
        paramKey = paramKey,
        title = title(),
        description = paramDescription?.takeIf { it.isNotBlank() },
        kind = resolvedKind(),
        options = options.map { option ->
            QuickCreationResolvedFieldOption(
                label = option.label,
                value = option.value,
            )
        },
        currentValue = currentValue,
        placeholder = placeholder?.takeIf { it.isNotBlank() } ?: paramKey,
        maxLength = maxLength,
        acceptFormats = emptyList(),
        maxUploadCount = maxInputCount,
        maxUploadSizeBytes = null,
        uploadMediaKind = uploadMediaKind(),
        childFields = emptyList(),
    )
}

private fun QuickCreationServiceField.resolvedKind(): QuickCreationResolvedFieldKind =
    when {
        options.isNotEmpty() -> QuickCreationResolvedFieldKind.OPTIONS
        supportsTextEntry() -> QuickCreationResolvedFieldKind.TEXT
        else -> QuickCreationResolvedFieldKind.UPLOAD
    }

private fun QuickCreationServiceFieldInputChild.resolvedKind(): QuickCreationResolvedFieldKind =
    when {
        options.isNotEmpty() -> QuickCreationResolvedFieldKind.OPTIONS
        supportsTextEntry() -> QuickCreationResolvedFieldKind.TEXT
        else -> QuickCreationResolvedFieldKind.UPLOAD
    }

private fun MutableMap<String, QuickCreationServiceUploadFieldAlias>.putUploadAlias(
    fieldKey: String,
    paramKey: String,
    mediaKind: QuickCreationUploadMediaKind?,
) {
    val alias = QuickCreationServiceUploadFieldAlias(paramKey = paramKey, mediaKind = mediaKind)
    if (fieldKey.isNotBlank()) {
        put(fieldKey, alias)
    }
    if (paramKey.isNotBlank()) {
        put(paramKey, alias)
    }
}

private fun serviceParamAliases(
    model: QuickCreationServiceModel?,
): Map<String, String> =
    buildMap {
        model?.fields.orEmpty()
            .filter { it.visible }
            .forEach { field ->
                putSimpleAlias(field.fieldKey, field.paramKey)
                field.inputExtra?.inputChildren.orEmpty()
                    .filter { it.isRenderable() }
                    .forEach { child -> putSimpleAlias(child.fieldKey, child.paramKey) }
            }
    }

private fun MutableMap<String, String>.putSimpleAlias(fieldKey: String, paramKey: String) {
    if (fieldKey.isNotBlank()) {
        put(fieldKey, paramKey)
    }
    if (paramKey.isNotBlank()) {
        put(paramKey, paramKey)
    }
}

private fun paramsWithFieldAliases(
    model: QuickCreationServiceModel?,
    params: Map<String, String>,
): Map<String, String> {
    val fields = model?.fields.orEmpty().filter { it.visible }
    if (fields.isEmpty()) return params
    return buildMap {
        fields.forEach { field ->
            putParamAliases(
                fieldKey = field.fieldKey,
                paramKey = field.paramKey,
                defaultValue = field.defaultValue,
                params = params,
            )
        }
        var changed: Boolean
        do {
            changed = false
            val snapshot = toMap()
            fields.forEach { field ->
                field.activeInputChildren(snapshot).forEach { child ->
                    val beforeSize = size
                    putParamAliases(
                        fieldKey = child.fieldKey,
                        paramKey = child.paramKey,
                        defaultValue = child.defaultValue,
                        params = params,
                    )
                    if (size != beforeSize) {
                        changed = true
                    }
                }
            }
        } while (changed)
    }
}

private fun MutableMap<String, String>.putParamAliases(
    fieldKey: String,
    paramKey: String,
    defaultValue: String?,
    params: Map<String, String>,
) {
    val value = params[paramKey] ?: params[fieldKey] ?: defaultValue?.takeIf { it.isNotBlank() } ?: return
    if (fieldKey.isNotBlank() && fieldKey !in this) {
        put(fieldKey, value)
    }
    if (paramKey.isNotBlank() && paramKey !in this) {
        put(paramKey, value)
    }
}

private fun QuickCreationServiceField.activeInputChildren(
    params: Map<String, String>,
): List<QuickCreationServiceFieldInputChild> {
    val parentValue = params[paramKey] ?: params[fieldKey] ?: defaultValue.orEmpty()
    return inputExtra?.inputChildren.orEmpty()
        .filter { it.isRenderable() }
        .filter { child ->
            val condition = child.visibleWhen ?: return@filter true
            val conditionValue = params[condition.fieldKey]
                ?: if (condition.fieldKey == fieldKey || condition.fieldKey == paramKey) parentValue else null
            if (condition.values.isEmpty()) {
                !conditionValue.isNullOrBlank()
            } else {
                conditionValue in condition.values
            }
        }
}

private fun QuickCreationServiceField.supportsTextEntry(): Boolean {
    val type = fieldType.uppercase()
    return type.contains("STRING") ||
        type.contains("TEXT") ||
        type.contains("NUMBER") ||
        type.contains("INTEGER") ||
        type.contains("FLOAT")
}

private fun QuickCreationServiceFieldInputChild.supportsTextEntry(): Boolean {
    val type = fieldType.uppercase()
    return type.contains("STRING") ||
        type.contains("TEXT") ||
        type.contains("NUMBER") ||
        type.contains("INTEGER") ||
        type.contains("FLOAT")
}

private fun QuickCreationServiceField.isUploadField(): Boolean {
    val type = fieldType.uppercase()
    return type.contains("UPLOAD") ||
        type.contains("IMAGE") ||
        type.contains("VIDEO") ||
        type.contains("AUDIO")
}

private fun QuickCreationServiceFieldInputChild.isUploadField(): Boolean {
    val type = fieldType.uppercase()
    return type.contains("UPLOAD") ||
        type.contains("IMAGE") ||
        type.contains("VIDEO") ||
        type.contains("AUDIO")
}

private fun QuickCreationServiceField.isRenderable(): Boolean =
    visible && (options.isNotEmpty() || supportsTextEntry() || isUploadField())

private fun QuickCreationServiceFieldInputChild.isRenderable(): Boolean =
    visible && (options.isNotEmpty() || supportsTextEntry() || isUploadField())

private fun QuickCreationServiceField.title(): String =
    inputExtra?.title?.takeIf { it.isNotBlank() } ?: fieldKey

private fun QuickCreationServiceFieldInputChild.title(): String =
    title?.takeIf { it.isNotBlank() } ?: fieldKey

private fun QuickCreationServiceField.textValidationError(value: String): String? {
    val title = title()
    val trimmed = value.trim()
    if (required && trimmed.isEmpty()) {
        return "$title 不能为空"
    }
    val minLength = inputExtra?.minLength?.takeIf { it > 0 }
    if (minLength != null && trimmed.isNotEmpty() && trimmed.length < minLength) {
        return "$title 至少 $minLength 个字符"
    }
    return null
}

private fun QuickCreationServiceFieldInputChild.textValidationError(value: String): String? {
    val title = title()
    val trimmed = value.trim()
    if (required && trimmed.isEmpty()) {
        return "$title 不能为空"
    }
    val minLength = minLength?.takeIf { it > 0 }
    if (minLength != null && trimmed.isNotEmpty() && trimmed.length < minLength) {
        return "$title 至少 $minLength 个字符"
    }
    return null
}

private fun QuickCreationServiceField.uploadValidationError(uploadedCount: Int): String? {
    val title = title()
    if (required && uploadedCount <= 0) {
        return "$title 不能为空"
    }
    val maxCount = inputExtra?.maxInputCount ?: maxUploadCount
    if (maxCount != null && uploadedCount > maxCount) {
        return "$title 最多 $maxCount 个文件"
    }
    return null
}

private fun QuickCreationServiceFieldInputChild.uploadValidationError(uploadedCount: Int): String? {
    val title = title()
    if (required && uploadedCount <= 0) {
        return "$title 不能为空"
    }
    val maxCount = maxInputCount
    if (maxCount != null && uploadedCount > maxCount) {
        return "$title 最多 $maxCount 个文件"
    }
    return null
}

private fun QuickCreationServiceField.isPromptField(): Boolean =
    fieldKey.isPromptParamKey() || paramKey.isPromptParamKey()

private fun String.isPromptParamKey(): Boolean =
    equals("prompt", ignoreCase = true) || equals("promptAi", ignoreCase = true)

private fun QuickCreationServiceModel?.uploadFields(): List<QuickCreationServiceField> =
    this?.fields.orEmpty().filter { it.isRenderable() && it.isUploadField() }

private fun activeChildUploadFields(
    model: QuickCreationServiceModel?,
    serviceParams: Map<String, String>,
): List<QuickCreationServiceFieldInputChild> {
    val aliasedParams = paramsWithFieldAliases(model, serviceParams)
    return model?.fields.orEmpty()
        .filter { it.visible }
        .flatMap { field -> field.activeInputChildren(aliasedParams) }
        .filter { it.isUploadField() }
}

private fun QuickCreationServiceField.uploadMediaKind(): QuickCreationUploadMediaKind? {
    val marker = listOfNotNull(fieldType, fieldKey, paramKey, inputExtraJson)
        .joinToString(" ")
        .uppercase()
    return marker.uploadMediaKindFromMarker()
}

private fun QuickCreationServiceFieldInputChild.uploadMediaKind(): QuickCreationUploadMediaKind? {
    val marker = listOf(fieldType, fieldKey, paramKey)
        .joinToString(" ")
        .uppercase()
    return marker.uploadMediaKindFromMarker()
}

private fun String.uploadMediaKindFromMarker(): QuickCreationUploadMediaKind? =
    when {
        contains("AUDIO") -> QuickCreationUploadMediaKind.AUDIO
        contains("VIDEO") -> QuickCreationUploadMediaKind.VIDEO
        contains("IMAGE") || contains("PHOTO") || contains("IMG") -> QuickCreationUploadMediaKind.IMAGE
        else -> null
    }

private fun List<QuickCreationUploadedMedia>.urlsByType(): Map<QuickCreationUploadMediaKind, List<String>> =
    filter { it.fieldParamKey.isNullOrBlank() }
        .groupBy { it.mediaKind }
        .mapValues { (_, refs) -> refs.map { it.remoteUrl }.filter { it.isNotBlank() } }

private fun List<QuickCreationUploadedMedia>.urlsByField(): Map<String, List<String>> =
    filter { !it.fieldParamKey.isNullOrBlank() }
        .groupBy { it.fieldParamKey.orEmpty() }
        .mapValues { (_, refs) -> refs.map { it.remoteUrl }.filter { it.isNotBlank() } }

private fun uploadFieldCountByType(
    model: QuickCreationServiceModel?,
    serviceParams: Map<String, String>,
    fallbackMediaKind: QuickCreationUploadMediaKind?,
): Map<QuickCreationUploadMediaKind, Int> =
    buildMap {
        model.uploadFields().forEach { field ->
            val mediaKind = field.uploadMediaKind() ?: fallbackMediaKind ?: return@forEach
            put(mediaKind, getOrDefault(mediaKind, 0) + 1)
        }
        activeChildUploadFields(model, serviceParams).forEach { child ->
            val mediaKind = child.uploadMediaKind() ?: fallbackMediaKind ?: return@forEach
            put(mediaKind, getOrDefault(mediaKind, 0) + 1)
        }
    }

private fun Map<QuickCreationUploadMediaKind, List<String>>.fallbackUrlsForSingleUploadField(
    mediaKind: QuickCreationUploadMediaKind,
    uploadFieldCountByType: Map<QuickCreationUploadMediaKind, Int>,
): List<String> =
    if (uploadFieldCountByType[mediaKind] == 1) {
        get(mediaKind).orEmpty()
    } else {
        emptyList()
    }
