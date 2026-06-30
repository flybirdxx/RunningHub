package com.runninghub.feature.quickcreate.presentation.modelcatalog

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState

private const val COMPACT_ALL_PURPOSE_IMAGE_NO_SPACE = "\u5168\u80fd\u56fe\u7247G-2.0"
private const val COMPACT_ALL_PURPOSE_IMAGE_WITH_SPACE = "\u5168\u80fd\u56fe\u7247 G-2.0"
private const val COMPACT_ALL_PURPOSE_IMAGE_G2_LEGACY_NO_SPACE = "\u5168\u80fd\u56fe\u7247G-2"
private const val COMPACT_ALL_PURPOSE_IMAGE_G2_LEGACY_WITH_SPACE = "\u5168\u80fd\u56fe\u7247 G-2"
private const val COMPACT_ALL_PURPOSE_IMAGE_G2_LABEL = "\u5168\u80fd\u56fe\u7247 G-2.0"
private const val COMPACT_OFFICIAL_SUFFIX = "\u5b98\u65b9\u7248"
private const val COMPACT_OFFICIAL_STABLE_SUFFIX = "\u5b98\u65b9\u7a33\u5b9a\u7248"

/** 模型选择器中用于主分类和图标映射的生成能力类别。 */
enum class QuickCreateServiceModelKind {
    Image,
    Video,
    Audio,
    Other,
}

/** 模型卡片中用于说明适用场景的稳定语义。 */
enum class QuickCreateServiceModelScene {
    ImageGeneration,
    VideoGeneration,
    AudioGeneration,
    General,
}

/** 模型卡片中的目录价格摘要语义，真实扣费仍以生成前 fee-preview 为准。 */
sealed interface QuickCreateServiceModelPrice {
    data object Unknown : QuickCreateServiceModelPrice

    data object Free : QuickCreateServiceModelPrice

    data class Known(val text: String) : QuickCreateServiceModelPrice
}

/** 模型选择器的分类筛选项。 */
enum class QuickCreateModelPickerFilter {
    Image,
    Video,
    Audio,
    Other,
}

/** 模型选择器空态原因，用于区分目录为空和搜索无结果。 */
enum class QuickCreateModelPickerEmptyReason {
    Loading,
    EmptyCatalog,
    SearchNoResult,
}

/**
 * 模型选择器在 Presentation 层的可渲染状态。
 *
 * @property query 当前搜索词。
 * @property selectedFilter 当前分类筛选项。
 * @property visibleItems 搜索和筛选后可展示的模型卡片。
 * @property loading 是否正在加载目录且没有可用快照。
 * @property emptyReason 列表为空时的原因；非空列表下为 `null`。
 */
data class QuickCreateModelPickerState(
    val query: String,
    val selectedFilter: QuickCreateModelPickerFilter,
    val visibleItems: List<QuickCreateServiceModelUi>,
    val loading: Boolean,
    val emptyReason: QuickCreateModelPickerEmptyReason?,
)

/**
 * 快捷创作服务端模型在 Presentation 层使用的 UI 模型。
 *
 * Domain 模型仍保留给生成、计费和动态字段解析路径使用；Composable 只读取本类型中的展示字段，
 * 避免在布局代码中反复理解服务端名称、分组和字段数量的展示规则。
 *
 * @property source 原始 Domain 模型，用于用户选择后回传给 ModelCatalogInteractor。
 * Composable 不应从该对象读取展示文案，应优先使用下列 UI 字段。
 * @property identityKey 模型在页面内的稳定身份键，由 `bindingId + skuId` 组成。
 * 当服务端返回空 ID 时仍保留分隔符，便于测试暴露异常数据。
 * @property displayName 模型主标题语义；服务端名称保留为运行时文本，空名称交给 UI 边界资源化。
 * @property compactName 紧凑编辑器使用的短标签，会移除常见营销后缀并限制长度。
 * @property groupTitle 模型选择面板中的分组标题语义；服务端未返回分组时交给 UI 边界资源化。
 * @property subtitle 模型副标题语义，当前由服务端分组和可配置参数数量组成。
 * [QuickCreateServiceModelSubtitle.None] 表示没有辅助信息，调用方可不渲染副标题行。
 * @property kind 模型主要输出能力，用于分类、图标和目标 tab 映射。
 * @property targetTab 选择该模型后应该落到的快捷创作 tab，由模型目录来源决定。
 * @property scene 模型适用场景的稳定语义，最终文案由 composeApp 资源层映射。
 * @property price 目录层价格摘要；未知不等于免费，生成前仍需 fee-preview。
 * @property technicalTags 服务端 API 类型或来源等辅助标签，不作为主标题展示。
 * @property selected 当前模型是否为该类别下的选中项。
 * 选中态在映射层按服务身份计算，避免 UI 直接比较 Domain 字段。
 */
data class QuickCreateServiceModelUi(
    val source: QuickCreationServiceModel,
    val identityKey: String,
    val displayName: QuickCreateServiceModelDisplayName,
    val compactName: String,
    val groupTitle: QuickCreateServiceModelGroupTitle,
    val subtitle: QuickCreateServiceModelSubtitle,
    val kind: QuickCreateServiceModelKind,
    val targetTab: QuickCreateTab,
    val scene: QuickCreateServiceModelScene,
    val price: QuickCreateServiceModelPrice,
    val technicalTags: List<String>,
    val selected: Boolean,
)

/**
 * 服务端模型标题的稳定展示语义。
 *
 * Presentation 层不再把本地兜底标题写成中文字符串；服务端返回的真实模型名仍作为运行时数据保留，
 * 只有缺失名称场景由 composeApp 使用 Compose Resources 映射最终文案。
 */
sealed interface QuickCreateServiceModelDisplayName {
    /** 服务端未提供可读模型名，UI 边界应展示资源化的未命名模型文案。 */
    data object Unnamed : QuickCreateServiceModelDisplayName

    /**
     * 服务端或 Data 层提供的模型名。
     *
     * @property value 运行时模型名称，可能包含运营配置或服务端语言，不应在 Presentation 层二次本地化。
     */
    data class ServerText(
        val value: String,
    ) : QuickCreateServiceModelDisplayName
}

/**
 * 服务端模型分组标题的稳定展示语义。
 *
 * 服务端分组名按运行时数据展示；缺失分组使用稳定语义，避免 Presentation 模块继续保存中文兜底文案。
 */
sealed interface QuickCreateServiceModelGroupTitle {
    /** 服务端未返回分组，UI 边界应展示资源化的默认分组文案。 */
    data object Other : QuickCreateServiceModelGroupTitle

    /**
     * 服务端返回的分组名称。
     *
     * @property value 运行时分组名，顺序和内容来自服务端目录。
     */
    data class ServerText(
        val value: String,
    ) : QuickCreateServiceModelGroupTitle
}

/**
 * 服务端模型副标题的稳定展示语义。
 *
 * 参数数量格式属于本地 UI 文案，留到 composeApp 资源层处理；服务端分组名作为运行时数据传递。
 */
sealed interface QuickCreateServiceModelSubtitle {
    /** 当前模型没有需要展示的副标题。 */
    data object None : QuickCreateServiceModelSubtitle

    /**
     * 只展示可配置参数数量。
     *
     * @property parameterCount 可配置服务端字段数量，单位为个，取值不应为负数。
     */
    data class ParameterCount(
        val parameterCount: Int,
    ) : QuickCreateServiceModelSubtitle

    /**
     * 同时展示服务端分组名和可配置参数数量。
     *
     * @property groupName 服务端返回的分组名，空字符串不应传入。
     * @property parameterCount 可配置服务端字段数量，单位为个，取值不应为负数。
     */
    data class GroupAndParameterCount(
        val groupName: String,
        val parameterCount: Int,
    ) : QuickCreateServiceModelSubtitle
}

/**
 * 紧凑编辑器模型入口的稳定标签语义。
 *
 * Presentation 层只表达当前标签来自加载态、服务端模型名还是本地兼容模型名，
 * 不直接返回加载态的最终中文 UI 文案；composeApp 负责把 [Loading] 映射到 Compose Resources。
 */
sealed interface QuickCreateCompactServiceModelLabel {
    /** 服务端模型目录仍在加载，调用方应展示资源化的加载态文案。 */
    data object Loading : QuickCreateCompactServiceModelLabel

    /**
     * 使用服务端模型的紧凑展示名。
     *
     * @property value 由服务端模型名称清洗得到的短标签；空字符串不应传入。
     */
    data class ModelName(
        val value: String,
    ) : QuickCreateCompactServiceModelLabel

    /**
     * 使用本地兼容模型的展示名。
     *
     * @property value 来自本地图片或视频模型枚举的展示名，用于服务端模型缺失时保持入口可读。
     */
    data class FallbackName(
        val value: String,
    ) : QuickCreateCompactServiceModelLabel
}

/**
 * 判断两个服务端模型是否代表同一个可选项。
 *
 * 服务端模型可能同时包含 bindingId 与 skuId，选择态必须同时比较两者，避免同一 binding
 * 下不同计费 SKU 被错误标记为已选。
 *
 * @param other 另一个服务端模型；`null` 表示当前没有选中项。
 * @return `true` 表示两个模型的 `bindingId` 和 `skuId` 完全一致。
 */
fun QuickCreationServiceModel.isSameQuickCreationServiceModel(
    other: QuickCreationServiceModel?,
): Boolean = other != null && bindingId == other.bindingId && skuId == other.skuId

/**
 * 生成服务端模型在 Presentation 状态中的稳定身份键。
 *
 * 该键只用于页面事件回传和选择态匹配，不参与远端请求签名；真实生成请求仍使用
 * Domain 模型中的 bindingId 和 skuId，避免 UI 层拼接接口参数。
 *
 * @return 页面内部使用的模型身份键，格式为 `bindingId|skuId`。
 */
fun QuickCreationServiceModel.quickCreateServiceModelIdentityKey(): String =
    "$bindingId|$skuId"

/**
 * 生成模型副标题语义。
 *
 * 副标题只用于 Presentation 展示，包含服务端分组和参数数量语义；计费、接口路径或其他
 * Data 层细节不得在此处拼接为用户可见内容。最终固定格式留到 composeApp 资源层映射，
 * 避免 Presentation 模块继续保存中文 UI 文案。
 *
 * @return 可由 UI 边界映射为最终文案的模型副标题语义，缺失分组时只保留参数数量语义。
 */
fun QuickCreationServiceModel.quickCreationServiceModelSubtitle(): QuickCreateServiceModelSubtitle {
    val parameterCount = fields.size
    val group = groupName?.takeIf { it.isNotBlank() }
    return when {
        group != null && parameterCount > 0 ->
            QuickCreateServiceModelSubtitle.GroupAndParameterCount(group, parameterCount)
        parameterCount > 0 -> QuickCreateServiceModelSubtitle.ParameterCount(parameterCount)
        group != null -> QuickCreateServiceModelSubtitle.GroupAndParameterCount(group, parameterCount)
        else -> QuickCreateServiceModelSubtitle.None
    }
}

/**
 * 将服务端模型列表映射为模型选择面板可直接渲染的 UI 模型。
 *
 * 映射阶段保留服务端返回顺序，因为该顺序包含运营侧配置的默认优先级；UI 层只负责按
 * [QuickCreateServiceModelUi.groupTitle] 分组展示，不重新排序。
 *
 * @param selected 当前类别下选中的 Domain 模型；为空表示列表中没有选中项。
 * @return 与输入顺序一致的服务模型 UI 列表。
 */
fun List<QuickCreationServiceModel>.toQuickCreateServiceModelUiItems(
    selected: QuickCreationServiceModel?,
    targetTab: QuickCreateTab? = null,
): List<QuickCreateServiceModelUi> =
    map { model ->
        model.toQuickCreateServiceModelUi(
            selected = model.isSameQuickCreationServiceModel(selected),
            targetTab = targetTab,
        )
    }

/**
 * 将单个服务端模型映射为摘要区域使用的 UI 模型。
 *
 * @param selected `true` 表示当前模型应在选择器中显示为已选；`false` 表示普通候选项。
 * @return 可供模型选择器和编辑器摘要直接展示的 UI 模型。
 */
fun QuickCreationServiceModel.toQuickCreateServiceModelUi(
    selected: Boolean = false,
    targetTab: QuickCreateTab? = null,
): QuickCreateServiceModelUi {
    val displayNameText = name.takeIf { it.isNotBlank() }
    val compactDisplayName = (displayNameText ?: "").toQuickCreateCompactServiceModelLabel()
    val compactGroupName = groupName
        ?.takeIf { it.isNotBlank() }
        ?.toQuickCreateCompactServiceModelLabel()
    val compactNameText = compactGroupName
        ?.takeIf { it.hasQuickCreateModelFamilyToken() }
        ?: compactDisplayName.takeIf { it.isNotBlank() }
        ?: compactGroupName.orEmpty()
    val displayName = displayNameText
        ?.let(QuickCreateServiceModelDisplayName::ServerText)
        ?: QuickCreateServiceModelDisplayName.Unnamed
    val kind = quickCreateServiceModelKind()
    return QuickCreateServiceModelUi(
        source = this,
        identityKey = quickCreateServiceModelIdentityKey(),
        displayName = displayName,
        compactName = compactNameText,
        groupTitle = groupName
            ?.takeIf { it.isNotBlank() }
            ?.let(QuickCreateServiceModelGroupTitle::ServerText)
            ?: QuickCreateServiceModelGroupTitle.Other,
        subtitle = quickCreationServiceModelSubtitle(),
        kind = kind,
        targetTab = targetTab ?: kind.quickCreateServiceModelTargetTab(),
        scene = kind.quickCreateServiceModelScene(),
        price = quickCreateServiceModelPrice(),
        technicalTags = quickCreateServiceModelTechnicalTags(),
        selected = selected,
    )
}

/** 根据当前页面状态生成模型选择器的过滤结果。 */
fun quickCreateModelPickerState(
    state: QuickCreateUiState,
): QuickCreateModelPickerState {
    val currentItems = (state.serviceImageModelItems + state.serviceVideoModelItems)
        .distinctBy { it.identityKey }
    val allItems = currentItems.ifEmpty { state.modelPickerModelSnapshot }
    val query = state.modelPickerQuery.trim()
    val visibleItems = allItems
        .filter { state.modelPickerFilter.accepts(it.kind) }
        .filter { query.isBlank() || it.matchesModelPickerQuery(query) }
    val loading = state.serviceModelsLoading && allItems.isEmpty()
    val emptyReason = when {
        visibleItems.isNotEmpty() -> null
        loading -> QuickCreateModelPickerEmptyReason.Loading
        allItems.isEmpty() -> QuickCreateModelPickerEmptyReason.EmptyCatalog
        query.isNotBlank() -> QuickCreateModelPickerEmptyReason.SearchNoResult
        else -> QuickCreateModelPickerEmptyReason.EmptyCatalog
    }

    return QuickCreateModelPickerState(
        query = state.modelPickerQuery,
        selectedFilter = state.modelPickerFilter,
        visibleItems = visibleItems,
        loading = loading,
        emptyReason = emptyReason,
    )
}

/**
 * 根据当前选中模型生成紧凑编辑器中的模型标签。
 *
 * 主输入条在服务端模型尚未回填时展示加载语义，避免把本地兼容模型误当作当前已选模型；
 * 目录加载完成但仍无服务模型时才使用本地兼容模型名兜底，保证旧接口或离线场景仍可读。
 *
 * @param model 当前服务端选中模型 UI 摘要；`null` 表示还没有可展示的服务端模型。
 * @param fallback 服务端模型缺失时展示的本地兼容模型名称。
 * @param loading `true` 表示目录仍在加载，此时若 [model] 为空不应展示硬编码兼容模型名。
 * @return 紧凑模型入口的稳定标签语义。
 */
fun quickCreateCompactServiceModelLabel(
    model: QuickCreateServiceModelUi?,
    fallback: String,
    loading: Boolean,
): QuickCreateCompactServiceModelLabel =
    when {
        model != null -> QuickCreateCompactServiceModelLabel.ModelName(model.compactName)
        loading -> QuickCreateCompactServiceModelLabel.Loading
        else -> QuickCreateCompactServiceModelLabel.FallbackName(fallback.toQuickCreateCompactServiceModelLabel())
    }

private fun String.toQuickCreateCompactServiceModelLabel(): String {
    // 这里处理的是服务端模型名称的运行时规整，不是本地 UI 文案；
    // G-2 服务端会在缓存和远端目录之间返回 “G-2.0 / G-2-文生图 / G-2-图生图”
    // 等不同命名，底部紧凑入口必须统一到本地兜底同款短名，避免异步刷新时标签跳变。
    if (startsWith(COMPACT_ALL_PURPOSE_IMAGE_NO_SPACE) ||
        startsWith(COMPACT_ALL_PURPOSE_IMAGE_WITH_SPACE) ||
        startsWith(COMPACT_ALL_PURPOSE_IMAGE_G2_LEGACY_NO_SPACE) ||
        startsWith(COMPACT_ALL_PURPOSE_IMAGE_G2_LEGACY_WITH_SPACE)
    ) {
        return COMPACT_ALL_PURPOSE_IMAGE_G2_LABEL
    }
    return replace(COMPACT_OFFICIAL_STABLE_SUFFIX, "")
        .replace(COMPACT_OFFICIAL_SUFFIX, "")
        .trim(' ', '-', '·')
        .take(18)
}

private fun String.hasQuickCreateModelFamilyToken(): Boolean =
    any { it.isDigit() }

private fun QuickCreateModelPickerFilter.accepts(kind: QuickCreateServiceModelKind): Boolean =
    when (this) {
        QuickCreateModelPickerFilter.Image -> kind == QuickCreateServiceModelKind.Image
        QuickCreateModelPickerFilter.Video -> kind == QuickCreateServiceModelKind.Video
        QuickCreateModelPickerFilter.Audio -> kind == QuickCreateServiceModelKind.Audio
        QuickCreateModelPickerFilter.Other -> kind == QuickCreateServiceModelKind.Other
    }

private fun QuickCreateServiceModelKind.quickCreateServiceModelScene(): QuickCreateServiceModelScene =
    when (this) {
        QuickCreateServiceModelKind.Image -> QuickCreateServiceModelScene.ImageGeneration
        QuickCreateServiceModelKind.Video -> QuickCreateServiceModelScene.VideoGeneration
        QuickCreateServiceModelKind.Audio -> QuickCreateServiceModelScene.AudioGeneration
        QuickCreateServiceModelKind.Other -> QuickCreateServiceModelScene.General
    }

private fun QuickCreateServiceModelKind.quickCreateServiceModelTargetTab(): QuickCreateTab =
    if (this == QuickCreateServiceModelKind.Image) QuickCreateTab.IMAGE else QuickCreateTab.VIDEO

private fun QuickCreationServiceModel.quickCreateServiceModelKind(): QuickCreateServiceModelKind {
    val type = apiType.normalizedCapabilityText()
    return when {
        type.isVideoOutputCapability() -> QuickCreateServiceModelKind.Video
        type.isAudioOutputCapability() -> QuickCreateServiceModelKind.Audio
        type.isImageOutputCapability() -> QuickCreateServiceModelKind.Image
        else -> when (categoryId.uppercase()) {
            "IMAGE" -> QuickCreateServiceModelKind.Image
            "VIDEO" -> QuickCreateServiceModelKind.Video
            "AUDIO" -> QuickCreateServiceModelKind.Audio
            else -> QuickCreateServiceModelKind.Other
        }
    }
}

private fun QuickCreationServiceModel.quickCreateServiceModelPrice(): QuickCreateServiceModelPrice {
    if (pricing?.isFree == true) return QuickCreateServiceModelPrice.Free
    val text = pricing?.priceSummaryRaw
        ?: pricing?.flatPriceRaw
        ?: pricing?.dimensionPricingRaw
    return text
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.trimInsignificantPriceZeros()
        ?.let(QuickCreateServiceModelPrice::Known)
        ?: QuickCreateServiceModelPrice.Unknown
}

private fun QuickCreationServiceModel.quickCreateServiceModelTechnicalTags(): List<String> =
    buildList {
        apiType.cleanCapabilityTag()?.let(::add)
        apiSource.cleanCapabilityTag()?.let(::add)
    }.ifEmpty {
        inferredCapabilityTags()
    }.distinct().take(2)

private fun QuickCreateServiceModelUi.matchesModelPickerQuery(query: String): Boolean {
    val normalizedQuery = query.lowercase()
    return buildList {
        add(compactName)
        displayName.serverTextOrNull()?.let(::add)
        groupTitle.serverTextOrNull()?.let(::add)
        technicalTags.forEach(::add)
        price.knownTextOrNull()?.let(::add)
        add(identityKey)
    }.any { it.lowercase().contains(normalizedQuery) }
}

private fun QuickCreateServiceModelDisplayName.serverTextOrNull(): String? =
    when (this) {
        QuickCreateServiceModelDisplayName.Unnamed -> null
        is QuickCreateServiceModelDisplayName.ServerText -> value
    }

private fun QuickCreateServiceModelGroupTitle.serverTextOrNull(): String? =
    when (this) {
        QuickCreateServiceModelGroupTitle.Other -> null
        is QuickCreateServiceModelGroupTitle.ServerText -> value
    }

private fun QuickCreateServiceModelPrice.knownTextOrNull(): String? =
    when (this) {
        QuickCreateServiceModelPrice.Free,
        QuickCreateServiceModelPrice.Unknown,
        -> null
        is QuickCreateServiceModelPrice.Known -> text
    }

private fun QuickCreationServiceModel.inferredCapabilityTags(): List<String> {
    val text = apiType.normalizedCapabilityText()
    val explicitTag = listOf(
        "multi-image-to-3d",
        "image-to-3d",
        "text-to-3d",
        "reference-to-video",
        "image-to-video",
        "text-to-video",
        "image-to-image",
        "text-to-image",
        "text-to-audio",
        "image-to-audio",
    ).firstOrNull(text::contains)

    return listOf(
        explicitTag
            ?: categoryId.cleanCapabilityTag()
            ?: "unknown",
    ).filterNot { it.equals("unknown", ignoreCase = true) }
}

private fun String?.cleanCapabilityTag(): String? =
    orEmpty()
        .trim()
        .takeIf { it.isNotBlank() }
        ?.takeUnless { it.equals("unknown", ignoreCase = true) }

private fun String?.normalizedCapabilityText(): String =
    orEmpty().lowercase()

private fun String.isVideoOutputCapability(): Boolean =
    contains("video") ||
        contains("\u89c6\u9891")

private fun String.isAudioOutputCapability(): Boolean =
    contains("audio") ||
        contains("music") ||
        contains("\u97f3\u9891") ||
        contains("\u97f3\u4e50")

private fun String.isImageOutputCapability(): Boolean =
    contains("image") &&
        !isVideoOutputCapability() &&
        !isAudioOutputCapability() &&
        !contains("3d")

private fun String.trimInsignificantPriceZeros(): String =
    replace(Regex("""(\d+)\.(\d*?[1-9])0+(?=\D|$)|(\d+)\.0+(?=\D|$)""")) { match ->
        val integer = match.groups[1]?.value ?: match.groups[3]?.value.orEmpty()
        val fraction = match.groups[2]?.value
        if (fraction == null) integer else "$integer.$fraction"
    }
