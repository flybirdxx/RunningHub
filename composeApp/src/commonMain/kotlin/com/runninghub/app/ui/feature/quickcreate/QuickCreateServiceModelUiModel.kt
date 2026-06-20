package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationServiceModel

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
 * @property displayName 模型主标题，来自服务端名称；空名称降级为“未命名模型”。
 * @property compactName 紧凑编辑器使用的短标签，会移除常见营销后缀并限制长度。
 * @property groupTitle 模型选择面板中的分组标题；服务端未返回分组时降级为“其他模型”。
 * @property subtitle 模型副标题，当前由分组和可配置参数数量组成。
 * 空字符串表示没有辅助信息，调用方可不渲染副标题行。
 * @property selected 当前模型是否为该类别下的选中项。
 * 选中态在映射层按服务身份计算，避免 UI 直接比较 Domain 字段。
 */
data class QuickCreateServiceModelUi(
    val source: QuickCreationServiceModel,
    val identityKey: String,
    val displayName: String,
    val compactName: String,
    val groupTitle: String,
    val subtitle: String,
    val selected: Boolean,
)

/**
 * 判断两个服务端模型是否代表同一个可选项。
 *
 * 服务端模型可能同时包含 bindingId 与 skuId，选择态必须同时比较两者，避免同一 binding
 * 下不同计费 SKU 被错误标记为已选。
 */
internal fun QuickCreationServiceModel.isSameQuickCreationServiceModel(
    other: QuickCreationServiceModel?,
): Boolean = other != null && bindingId == other.bindingId && skuId == other.skuId

/**
 * 生成服务端模型在 Presentation 状态中的稳定身份键。
 *
 * 该键只用于页面事件回传和选择态匹配，不参与远端请求签名；真实生成请求仍使用
 * Domain 模型中的 bindingId 和 skuId，避免 UI 层拼接接口参数。
 */
internal fun QuickCreationServiceModel.quickCreateServiceModelIdentityKey(): String =
    "$bindingId|$skuId"

/**
 * 生成模型副标题。
 *
 * 副标题只用于 Presentation 展示，包含服务端分组和参数数量；计费、接口路径或其他
 * Data 层细节不得在此处拼接为用户可见内容。
 */
internal fun QuickCreationServiceModel.quickCreationServiceModelSubtitle(): String =
    listOfNotNull(
        groupName?.takeIf { it.isNotBlank() },
        "${fields.size} 个参数",
    ).joinToString(" · ")

/**
 * 将服务端模型列表映射为模型选择面板可直接渲染的 UI 模型。
 *
 * 映射阶段保留服务端返回顺序，因为该顺序包含运营侧配置的默认优先级；UI 层只负责按
 * [QuickCreateServiceModelUi.groupTitle] 分组展示，不重新排序。
 */
internal fun List<QuickCreationServiceModel>.toQuickCreateServiceModelUiItems(
    selected: QuickCreationServiceModel?,
): List<QuickCreateServiceModelUi> =
    map { model ->
        model.toQuickCreateServiceModelUi(selected = model.isSameQuickCreationServiceModel(selected))
    }

/**
 * 将单个服务端模型映射为摘要区域使用的 UI 模型。
 */
internal fun QuickCreationServiceModel.toQuickCreateServiceModelUi(
    selected: Boolean = false,
): QuickCreateServiceModelUi {
    val displayName = name.takeIf { it.isNotBlank() } ?: "未命名模型"
    return QuickCreateServiceModelUi(
        source = this,
        identityKey = quickCreateServiceModelIdentityKey(),
        displayName = displayName,
        compactName = displayName.toQuickCreateCompactServiceModelLabel(),
        groupTitle = groupName?.takeIf { it.isNotBlank() } ?: "其他模型",
        subtitle = quickCreationServiceModelSubtitle(),
        selected = selected,
    )
}

/**
 * 根据当前加载状态和选中模型生成紧凑编辑器中的模型标签。
 *
 * 加载态优先展示固定文案；模型为空时使用本地兼容模型名，避免目录加载失败时显示空白控件。
 */
internal fun quickCreateCompactServiceModelLabel(
    model: QuickCreateServiceModelUi?,
    fallback: String,
    loading: Boolean,
): String =
    when {
        loading -> "模型加载中"
        model != null -> model.compactName
        else -> fallback
    }

private fun String.toQuickCreateCompactServiceModelLabel(): String =
    replace("全能图片G-2.0", "G-2.0")
        .replace("全能图片 G-2.0", "G-2.0")
        .replace("官方版", "")
        .trim(' ', '-', '·')
        .take(18)
