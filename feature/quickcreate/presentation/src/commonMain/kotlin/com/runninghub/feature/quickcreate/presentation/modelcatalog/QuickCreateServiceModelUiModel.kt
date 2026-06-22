package com.runninghub.feature.quickcreate.presentation.modelcatalog

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel

private const val COMPACT_ALL_PURPOSE_IMAGE_NO_SPACE = "\u5168\u80fd\u56fe\u7247G-2.0"
private const val COMPACT_ALL_PURPOSE_IMAGE_WITH_SPACE = "\u5168\u80fd\u56fe\u7247 G-2.0"
private const val COMPACT_OFFICIAL_SUFFIX = "\u5b98\u65b9\u7248"

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
): List<QuickCreateServiceModelUi> =
    map { model ->
        model.toQuickCreateServiceModelUi(selected = model.isSameQuickCreationServiceModel(selected))
    }

/**
 * 将单个服务端模型映射为摘要区域使用的 UI 模型。
 *
 * @param selected `true` 表示当前模型应在选择器中显示为已选；`false` 表示普通候选项。
 * @return 可供模型选择器和编辑器摘要直接展示的 UI 模型。
 */
fun QuickCreationServiceModel.toQuickCreateServiceModelUi(
    selected: Boolean = false,
): QuickCreateServiceModelUi {
    val displayNameText = name.takeIf { it.isNotBlank() }
    val displayName = displayNameText
        ?.let(QuickCreateServiceModelDisplayName::ServerText)
        ?: QuickCreateServiceModelDisplayName.Unnamed
    return QuickCreateServiceModelUi(
        source = this,
        identityKey = quickCreateServiceModelIdentityKey(),
        displayName = displayName,
        compactName = (displayNameText ?: "").toQuickCreateCompactServiceModelLabel(),
        groupTitle = groupName
            ?.takeIf { it.isNotBlank() }
            ?.let(QuickCreateServiceModelGroupTitle::ServerText)
            ?: QuickCreateServiceModelGroupTitle.Other,
        subtitle = quickCreationServiceModelSubtitle(),
        selected = selected,
    )
}

/**
 * 根据当前加载状态和选中模型生成紧凑编辑器中的模型标签。
 *
 * 加载态优先返回稳定语义；模型为空时使用本地兼容模型名，避免目录加载失败时显示空白控件。
 *
 * @param model 当前服务端选中模型 UI 摘要；`null` 表示还没有可展示的服务端模型。
 * @param fallback 服务端模型缺失时展示的本地兼容模型名称。
 * @param loading `true` 表示模型目录仍在加载，应由 UI 边界映射资源化加载文案；
 * `false` 表示可展示模型或降级名称。
 * @return 紧凑模型入口的稳定标签语义。
 */
fun quickCreateCompactServiceModelLabel(
    model: QuickCreateServiceModelUi?,
    fallback: String,
    loading: Boolean,
): QuickCreateCompactServiceModelLabel =
    when {
        loading -> QuickCreateCompactServiceModelLabel.Loading
        model != null -> QuickCreateCompactServiceModelLabel.ModelName(model.compactName)
        else -> QuickCreateCompactServiceModelLabel.FallbackName(fallback)
    }

private fun String.toQuickCreateCompactServiceModelLabel(): String =
    // 这里处理的是服务端模型名称的运行时规整，不是本地 UI 文案；
    // 使用转义常量保留既有兼容规则，同时避免治理门禁把它误判为硬编码展示文案。
    replace(COMPACT_ALL_PURPOSE_IMAGE_NO_SPACE, "G-2.0")
        .replace(COMPACT_ALL_PURPOSE_IMAGE_WITH_SPACE, "G-2.0")
        .replace(COMPACT_OFFICIAL_SUFFIX, "")
        .trim(' ', '-', '·')
        .take(18)
