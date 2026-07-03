package com.runninghub.feature.detail.presentation

/** 参数区平铺阈值：总行数不超过该值时不启用折叠。 */
const val APP_DETAIL_PARAMS_FLAT_THRESHOLD = 6

/** 无核心行时兜底保留在核心区的行数。 */
const val APP_DETAIL_PARAMS_FALLBACK_CORE_ROWS = 3

/**
 * 参数区高级分组。
 *
 * @property title 分组标题，来自连续字段共享的节点名；null 表示节点名缺失，UI 应使用通用文案。
 * @property rows 分组内的输入行，顺序与服务端一致。
 * @property modifiedCount 分组内被用户改过默认值的字段数，用于折叠头计数徽章。
 */
data class AppDetailParamsGroup(
    val title: String?,
    val rows: List<AppDetailInputRowUiModel>,
    val modifiedCount: Int,
)

/**
 * 参数区自适应折叠布局（方案 A）。
 *
 * @property coreRows 始终平铺展示的核心行。
 * @property advancedGroups 折叠分组；为空表示参数量少，全部平铺。
 */
data class AppDetailParamsLayoutUiModel(
    val coreRows: List<AppDetailInputRowUiModel>,
    val advancedGroups: List<AppDetailParamsGroup>,
)

/**
 * 把输入行解析为自适应折叠布局。
 *
 * 规则：总行数 ≤ [APP_DETAIL_PARAMS_FLAT_THRESHOLD] 全部平铺；否则媒体上传与多行文本行
 * 视为核心（全无时前 [APP_DETAIL_PARAMS_FALLBACK_CORE_ROWS] 行兜底），其余按“连续相同节点名”
 * 切分为折叠分组，空节点名合并为无标题分组。
 */
fun appDetailParamsLayout(rows: List<AppDetailInputRowUiModel>): AppDetailParamsLayoutUiModel {
    if (rows.size <= APP_DETAIL_PARAMS_FLAT_THRESHOLD) {
        return AppDetailParamsLayoutUiModel(coreRows = rows, advancedGroups = emptyList())
    }
    val core = rows.filter { it.isCoreRow() }.toMutableList()
    val advanced: List<AppDetailInputRowUiModel>
    if (core.isEmpty()) {
        core += rows.take(APP_DETAIL_PARAMS_FALLBACK_CORE_ROWS)
        advanced = rows.drop(APP_DETAIL_PARAMS_FALLBACK_CORE_ROWS)
    } else {
        advanced = rows.filterNot { it.isCoreRow() }
    }
    return AppDetailParamsLayoutUiModel(coreRows = core, advancedGroups = advanced.toGroups())
}

private fun AppDetailInputRowUiModel.isCoreRow(): Boolean = when (this) {
    is AppDetailInputRowUiModel.ImageUploadGroup -> true
    is AppDetailInputRowUiModel.Single -> when (val control = field.control) {
        is AppDetailInputControl.MediaUpload -> true
        is AppDetailInputControl.Text -> control.multiline
        else -> false
    }
}

private fun AppDetailInputRowUiModel.groupTitle(): String? = when (this) {
    is AppDetailInputRowUiModel.Single -> field.nodeName.ifBlank { null }
    is AppDetailInputRowUiModel.ImageUploadGroup -> fields.firstOrNull()?.nodeName?.ifBlank { null }
}

private fun AppDetailInputRowUiModel.modifiedFieldCount(): Int = when (this) {
    is AppDetailInputRowUiModel.Single -> if (field.isModified) 1 else 0
    is AppDetailInputRowUiModel.ImageUploadGroup -> fields.count { it.isModified }
}

private fun List<AppDetailInputRowUiModel>.toGroups(): List<AppDetailParamsGroup> {
    val groups = mutableListOf<AppDetailParamsGroup>()
    var currentTitle: String? = null
    var currentRows = mutableListOf<AppDetailInputRowUiModel>()
    fun flush() {
        if (currentRows.isNotEmpty()) {
            groups += AppDetailParamsGroup(
                title = currentTitle,
                rows = currentRows,
                modifiedCount = currentRows.sumOf { it.modifiedFieldCount() },
            )
            currentRows = mutableListOf()
        }
    }
    forEach { row ->
        val title = row.groupTitle()
        if (currentRows.isNotEmpty() && title != currentTitle) flush()
        currentTitle = title
        currentRows += row
    }
    flush()
    return groups
}
