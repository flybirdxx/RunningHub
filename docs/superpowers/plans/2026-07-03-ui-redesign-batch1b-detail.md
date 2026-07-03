# 批 1b 应用详情页重设计实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 应用详情页迁移到 Rh 设计系统,落地已定案的参数区「方案 A · 自适应折叠」(轻应用平铺、重应用核心+分组折叠、修改标记、重置、长下拉弹层化),并把 1440 行主文件拆成职责文件。

**Architecture:** 参数分层逻辑作为**纯函数**放在 `feature:detail:presentation`(TDD,不依赖 Compose);composeApp 只做渲染。折叠展开是短暂 UI 状态,留在 composable `remember`,不进 StateHolder。重置 = 对组内每字段调用既有 `updateInputValue(inputKey, defaultValue)`,StateHolder 零改动。先落 presentation 逻辑,再落 UI,再拆分,最后 token 化。

**Tech Stack:** Kotlin Multiplatform + Compose Multiplatform;presentation 层 kotlin.test TDD;Android 真机截图验收。

**上位文档:** spec §6/§8;本会话定案:参数区方案 A + 长下拉弹层(>6 选项)+ 修改标记/重置 + 必填启发式(媒体上传视为必填,仅视觉星标)。

**现状关键事实**(行号供参考,以内容为准):
- `AppDetailScreen.kt` 1440 行;`AppDetailInputFields.kt` 277 行;`AppDetailTaskResult.kt` 122 行。零 `Color(0x…)` 字面量,但 64 处旧 theme 符号(Dark*/Primary*/Neutral*/SuccessDark/ErrorDark)+ 大量裸 `Color.White/Black`。
- 参数模型:`AppDetailInputPresentation.kt` — `AppDetailInputFieldUiModel(nodeId, fieldName, inputKey, title, currentValue, control)`;`currentValue = inputValues[inputKey] ?: fieldValue ?: ""`,**`InputNode.fieldValue` 即服务端默认值**;`InputNode.nodeName` 可用作分组标题。行模型 `Single`/`ImageUploadGroup`。
- 状态:`AppDetailStateHolder`(feature:detail:presentation)持有;`updateInputValue(inputKey, value)` 已存在;ScreenModel 是纯 facade。
- `RhButton(text, onClick, modifier, enabled, loading, style)` / `RhPrimaryButton(text, onClick, modifier, enabled, loading)` 已存在(RhButtons.kt:50/124);`RhBottomSheetSurface(modifier, content: ColumnScope.() -> Unit)` 已存在;`RhChip(label, onClick, modifier, style, selected, leadingIcon, trailingIcon)`、`RhSegmentedControl(options, selectedIndex, onSelect, modifier)` 已存在。

---

### Task 1: 字段模型补默认值/修改标记/节点名(presentation,TDD)

**Files:**
- Modify: `feature/detail/presentation/src/commonMain/kotlin/com/runninghub/feature/detail/presentation/AppDetailInputPresentation.kt`
- Test: `feature/detail/presentation/src/commonTest/kotlin/com/runninghub/feature/detail/presentation/AppDetailInputPresentationTest.kt`(若不存在则新建;存在则追加)

- [ ] **Step 1: 写失败测试**(构造 `InputNode` 的方式参照该模块既有测试;若无既有构造惯例,用最小必填字段构造):

```kotlin
    @Test
    fun `field model exposes default value and modified flag`() {
        val node = inputNode(nodeId = "3", fieldName = "cfg", fieldValue = "4.5", fieldType = "FLOAT", nodeName = "采样")
        val untouched = appDetailInputRows(listOf(node), emptyMap()).singleField()
        assertEquals("4.5", untouched.defaultValue)
        assertEquals(false, untouched.isModified)
        assertEquals("采样", untouched.nodeName)

        val edited = appDetailInputRows(listOf(node), mapOf(untouched.inputKey to "7.0")).singleField()
        assertEquals(true, edited.isModified)

        val editedBack = appDetailInputRows(listOf(node), mapOf(untouched.inputKey to "4.5")).singleField()
        assertEquals(false, editedBack.isModified)
    }

    private fun List<AppDetailInputRowUiModel>.singleField() =
        (single() as AppDetailInputRowUiModel.Single).field
```

(`inputNode(...)` 为测试内私有工厂,包装 `InputNode` 构造,其余字段给空/默认。)

- [ ] **Step 2: 确认失败**:`.\gradlew.bat --console=plain :feature:detail:presentation:testDebugUnitTest --tests "*AppDetailInputPresentationTest*"` → 编译失败 unresolved `defaultValue`。(任务名不存在时用 `:feature:detail:presentation:tasks --all` 找等价 test 任务并在报告记录。)
- [ ] **Step 3: 实现** — `AppDetailInputFieldUiModel` 追加三个属性(KDoc 全角标点):

```kotlin
    val defaultValue: String,
    val isModified: Boolean,
    val nodeName: String,
```

`toInputFieldUiModel` 内:

```kotlin
    val defaultValue = fieldValue ?: ""
    val edited = inputValues[inputKey]
    return AppDetailInputFieldUiModel(
        nodeId = nodeId,
        fieldName = fieldName,
        inputKey = inputKey,
        title = displayTitle(),
        currentValue = edited ?: defaultValue,
        control = inputControl(),
        defaultValue = defaultValue,
        isModified = edited != null && edited != defaultValue,
        nodeName = nodeName.orEmpty(),
    )
```

(`InputNode.nodeName` 若为非空类型则去掉 `.orEmpty()`,以实际声明为准。)修复所有编译受影响的构造点(仅测试/预览数据)。

- [ ] **Step 4: 测试通过** + 跑全模块:`.\gradlew.bat --console=plain :feature:detail:presentation:testDebugUnitTest` → BUILD SUCCESSFUL。
- [ ] **Step 5: Commit**:`feat(detail-presentation): expose default value, modified flag and node name on input fields`(路径限定)。

### Task 2: 参数分层布局函数(presentation,TDD,方案 A 核心)

**Files:**
- Create: `feature/detail/presentation/src/commonMain/kotlin/com/runninghub/feature/detail/presentation/AppDetailParamsLayout.kt`
- Test: `feature/detail/presentation/src/commonTest/kotlin/com/runninghub/feature/detail/presentation/AppDetailParamsLayoutTest.kt`

- [ ] **Step 1: 写失败测试**(核心规则全覆盖):

```kotlin
class AppDetailParamsLayoutTest {
    @Test
    fun `six or fewer rows stay flat`() {
        val layout = appDetailParamsLayout(rows(count = 6))
        assertEquals(6, layout.coreRows.size)
        assertTrue(layout.advancedGroups.isEmpty())
    }

    @Test
    fun `above threshold media and multiline text stay core`() {
        val all = listOf(
            mediaRow("1", "输入"), multilineTextRow("2", "输入"),
            dropdownRow("3", "采样"), intRow("4", "采样"), decimalRow("5", "采样"),
            switchRow("6", "修复"), segmentedRow("7", "修复"), textRow("8", "输出"),
        )
        val layout = appDetailParamsLayout(all)
        assertEquals(2, layout.coreRows.size)
        assertEquals(listOf("采样", "修复", "输出"), layout.advancedGroups.map { it.title })
        assertEquals(listOf(3, 2, 1), layout.advancedGroups.map { it.rows.size })
    }

    @Test
    fun `consecutive rows sharing node name form one group and blank names coalesce`() {
        val all = listOf(
            mediaRow("1", "输入"),
            intRow("2", "采样"), intRow("3", "采样"),
            intRow("4", ""), intRow("5", ""),
            intRow("6", "采样"), intRow("7", "其他"),
        )
        val layout = appDetailParamsLayout(all)
        assertEquals(1, layout.coreRows.size)
        assertEquals(listOf("采样", null, "采样", "其他"), layout.advancedGroups.map { it.title })
    }

    @Test
    fun `modified count aggregates per group`() {
        val all = listOf(
            mediaRow("1", "输入"),
            intRow("2", "采样", modified = true), intRow("3", "采样"),
            intRow("4", "采样", modified = true),
            intRow("5", "输出"), intRow("6", "输出"), intRow("7", "输出"),
        )
        val layout = appDetailParamsLayout(all)
        assertEquals(listOf(2, 0), layout.advancedGroups.map { it.modifiedCount })
    }

    @Test
    fun `all advanced when no media or multiline keeps first three rows core`() {
        val layout = appDetailParamsLayout(List(8) { intRow("${it + 1}", "组") })
        assertEquals(3, layout.coreRows.size)
        assertEquals(5, layout.advancedGroups.sumOf { it.rows.size })
    }
}
```

测试文件顶部提供私有工厂:`mediaRow/multilineTextRow/textRow/dropdownRow/intRow/decimalRow/switchRow/segmentedRow(nodeId, nodeName, modified = false)`,各自构造 `AppDetailInputRowUiModel.Single`(字段用对应 `AppDetailInputControl`,`isModified = modified`,`inputKey = nodeId + "#f"`,其余字符串字段随意非空);另加 `rows(count: Int) = List(count) { intRow("${it + 1}", "组$it") }`(节点名各不相同,保证平铺判定只看数量)。

- [ ] **Step 2: 确认失败**(unresolved `appDetailParamsLayout`)。
- [ ] **Step 3: 实现**(新文件完整内容,KDoc 全角标点):

```kotlin
package com.runninghub.feature.detail.presentation

/** 参数区平铺阈值:总行数不超过该值时不启用折叠。 */
const val APP_DETAIL_PARAMS_FLAT_THRESHOLD = 6

/** 无核心行时兜底保留在核心区的行数。 */
const val APP_DETAIL_PARAMS_FALLBACK_CORE_ROWS = 3

/**
 * 参数区高级分组。
 *
 * @property title 分组标题,来自连续字段共享的节点名;null 表示节点名缺失,UI 应使用通用文案。
 * @property rows 分组内的输入行,顺序与服务端一致。
 * @property modifiedCount 分组内被用户改过默认值的字段数,用于折叠头计数徽章。
 */
data class AppDetailParamsGroup(
    val title: String?,
    val rows: List<AppDetailInputRowUiModel>,
    val modifiedCount: Int,
)

/**
 * 参数区自适应折叠布局(方案 A)。
 *
 * @property coreRows 始终平铺展示的核心行。
 * @property advancedGroups 折叠分组;为空表示参数量少,全部平铺。
 */
data class AppDetailParamsLayoutUiModel(
    val coreRows: List<AppDetailInputRowUiModel>,
    val advancedGroups: List<AppDetailParamsGroup>,
)

/**
 * 把输入行解析为自适应折叠布局。
 *
 * 规则:总行数 ≤ [APP_DETAIL_PARAMS_FLAT_THRESHOLD] 全部平铺;否则媒体上传与多行文本行
 * 视为核心(全无时前 [APP_DETAIL_PARAMS_FALLBACK_CORE_ROWS] 行兜底),其余按"连续相同节点名"
 * 切分为折叠分组,空节点名合并为无标题分组。
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
```

- [ ] **Step 4: 测试通过**:`.\gradlew.bat --console=plain :feature:detail:presentation:testDebugUnitTest` → BUILD SUCCESSFUL。
- [ ] **Step 5: Commit**:`feat(detail-presentation): adaptive collapse layout for detail params (plan A)`(路径限定,两个文件)。

### Task 3: 长下拉弹层选择器(composeApp)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/detail/AppDetailOptionPickerSheet.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`(追加 2 条)

- [ ] **Step 1: 字符串资源**(与文件既有格式一致):

```xml
    <string name="app_detail_option_picker_search_hint">搜索选项…</string>
    <string name="app_detail_option_picker_empty">没有匹配的选项</string>
```

- [ ] **Step 2: 实现**(新文件完整内容;遮罩+底部弹层;选项 >12 显示搜索框;禁用实验性 API):

```kotlin
package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.sheets.RhBottomSheetSurface
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_option_picker_empty
import runninghub.composeapp.generated.resources.app_detail_option_picker_search_hint

/** 选项数超过该值时展示搜索过滤框。 */
internal const val APP_DETAIL_OPTION_SEARCH_THRESHOLD = 12

/** 归一化搜索过滤:空白查询返回全量,否则忽略大小写包含匹配。 */
internal fun filterPickerOptions(options: List<String>, query: String): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return options
    return options.filter { it.contains(trimmed, ignoreCase = true) }
}

/**
 * 长下拉字段的底部弹层选择器。
 *
 * @param title 字段标题。
 * @param options 服务端候选项。
 * @param selected 当前选中值。
 * @param onSelect 用户选择后回传选项;调用方负责关闭弹层并写回状态。
 * @param onDismiss 点击遮罩关闭。
 */
@Composable
internal fun AppDetailOptionPickerSheet(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = RhTheme.colors
    var query by remember { mutableStateOf("") }
    val visibleOptions = filterPickerOptions(options, query)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.overlayScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        RhBottomSheetSurface(modifier = Modifier.imePadding()) {
            Text(text = title, color = colors.textPrimary, style = RhTypography.cardTitle)
            if (options.size > APP_DETAIL_OPTION_SEARCH_THRESHOLD) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = RhTypography.body.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.brandPrimary),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = RhSpacing.sm)
                                .background(colors.surfaceSunken, RoundedCornerShape(RhTheme.shapes.sm))
                                .padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.app_detail_option_picker_search_hint),
                                    color = colors.textTertiary,
                                    style = RhTypography.body,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp).padding(top = RhSpacing.sm)) {
                items(visibleOptions) { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .background(
                                if (isSelected) colors.surfaceSelected else colors.overlaySheet,
                                RoundedCornerShape(RhTheme.shapes.sm),
                            )
                            .padding(horizontal = RhSpacing.md, vertical = RhSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) colors.brandPrimary else colors.textPrimary,
                            style = RhTypography.body,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.brandPrimary,
                            )
                        }
                    }
                }
                if (visibleOptions.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.app_detail_option_picker_empty),
                            color = colors.textTertiary,
                            style = RhTypography.caption,
                            modifier = Modifier.padding(RhSpacing.lg),
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: 契约测试** — 新建 `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/detail/AppDetailOptionPickerTest.kt`:

```kotlin
package com.runninghub.app.ui.feature.detail

import kotlin.test.Test
import kotlin.test.assertEquals

class AppDetailOptionPickerTest {
    @Test
    fun `filter matches case-insensitively and blank query returns all`() {
        val options = listOf("Karras", "Euler A", "DPM++ 2M")
        assertEquals(options, filterPickerOptions(options, "  "))
        assertEquals(listOf("Karras"), filterPickerOptions(options, "kar"))
        assertEquals(listOf("Euler A"), filterPickerOptions(options, "euler"))
    }
}
```

- [ ] **Step 4:** `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest --rerun` → BUILD SUCCESSFUL。
- [ ] **Step 5: Commit**:`feat(detail): add option picker sheet for long dropdowns`(路径限定,三个文件)。

### Task 4: 参数区渲染(方案 A 折叠 UI + 必填星标 + 重置)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/detail/AppDetailParamsSection.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/detail/AppDetailScreen.kt`(`InputNodesContent` 调用点改为新组件)
- Modify: `strings.xml`(追加 3 条)

- [ ] **Step 1: 字符串资源**:

```xml
    <string name="app_detail_params_group_fallback">更多参数</string>
    <string name="app_detail_params_modified_count">已调 %1$d 项</string>
    <string name="app_detail_params_reset_group">重置</string>
```

- [ ] **Step 2: 实现 AppDetailParamsSection**(新文件完整内容;行渲染复用既有 `RenderInputNodeField` 分发——该函数随 Task 5 搬入本目录的 `AppDetailInputSection.kt`,本任务先按现名引用):

```kotlin
package com.runninghub.app.ui.feature.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.feature.detail.presentation.AppDetailInputRowUiModel
import com.runninghub.feature.detail.presentation.AppDetailParamsGroup
import com.runninghub.feature.detail.presentation.AppDetailParamsLayoutUiModel
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_params_group_fallback
import runninghub.composeapp.generated.resources.app_detail_params_modified_count
import runninghub.composeapp.generated.resources.app_detail_params_reset_group

/**
 * 参数区容器:核心行平铺,高级分组折叠(方案 A)。
 *
 * @param layout Presentation 解析出的自适应布局。
 * @param renderRow 单行渲染插槽,由调用方接既有输入行分发函数。
 * @param onResetGroup 分组重置:调用方对组内每个字段写回默认值。
 */
@Composable
internal fun AppDetailParamsSection(
    layout: AppDetailParamsLayoutUiModel,
    renderRow: @Composable (AppDetailInputRowUiModel) -> Unit,
    onResetGroup: (AppDetailParamsGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
        layout.coreRows.forEach { row -> renderRow(row) }
        layout.advancedGroups.forEachIndexed { index, group ->
            AdvancedGroupCard(
                group = group,
                groupKey = "params-group-$index",
                renderRow = renderRow,
                onReset = { onResetGroup(group) },
            )
        }
    }
}

@Composable
private fun AdvancedGroupCard(
    group: AppDetailParamsGroup,
    groupKey: String,
    renderRow: @Composable (AppDetailInputRowUiModel) -> Unit,
    onReset: () -> Unit,
) {
    val colors = RhTheme.colors
    var expanded by rememberSaveable(groupKey) { mutableStateOf(false) }
    val borderColor = if (expanded) colors.borderActive else colors.borderDefault
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceDefault, RoundedCornerShape(RhTheme.shapes.md))
            .padding(RhSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.title ?: stringResource(Res.string.app_detail_params_group_fallback),
                color = colors.textPrimary,
                style = RhTypography.bodyStrong,
                modifier = Modifier.weight(1f),
            )
            if (group.modifiedCount > 0) {
                Text(
                    text = stringResource(Res.string.app_detail_params_modified_count, group.modifiedCount),
                    color = colors.brandPrimary,
                    style = RhTypography.meta,
                    modifier = Modifier
                        .background(colors.brandMuted, RoundedCornerShape(RhTheme.shapes.full))
                        .padding(horizontal = RhSpacing.sm, vertical = 2.dp),
                )
                Spacer(Modifier.width(RhSpacing.sm))
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (expanded) colors.brandPrimary else colors.textTertiary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
                group.rows.forEach { row -> renderRow(row) }
                if (group.modifiedCount > 0) {
                    Text(
                        text = stringResource(Res.string.app_detail_params_reset_group),
                        color = colors.textSecondary,
                        style = RhTypography.caption,
                        modifier = Modifier
                            .clickable(onClick = onReset)
                            .padding(vertical = RhSpacing.xs),
                    )
                }
            }
        }
    }
    // 注:borderColor 变量用于展开态描边,如实现时未使用请通过 Modifier.border(1.dp, borderColor, shape) 接上。
}
```

(实现者注意:上面注释行是给你的施工提示,落地时把描边接到 Column 的 Modifier 上并删除该注释——最终代码不允许保留悬空变量。)

- [ ] **Step 3: 接线** — `AppDetailScreen.kt` 的 `DetailContent` 中,参数区 item 由 `InputNodesContent(rows...)` 平铺改为:

```kotlin
val paramsLayout = appDetailParamsLayout(uiState.inputRows())
AppDetailParamsSection(
    layout = paramsLayout,
    renderRow = { row -> RenderInputNodeField(row, /* 既有参数原样透传 */) },
    onResetGroup = { group ->
        group.rows.forEach { row ->
            when (row) {
                is AppDetailInputRowUiModel.Single ->
                    screenModel.updateInputValue(row.field.inputKey, row.field.defaultValue)
                is AppDetailInputRowUiModel.ImageUploadGroup ->
                    row.fields.forEach { screenModel.updateInputValue(it.inputKey, it.defaultValue) }
            }
        }
    },
)
```

`RenderInputNodeField` 的既有参数(回调、上传状态)按现签名原样透传;import `com.runninghub.feature.detail.presentation.appDetailParamsLayout`。**必填星标**:在媒体上传行的标题渲染处(`RenderInputNodeField`/`MultiImageUploadRow` 内标题 Text 后)追加 ` *`(`statusFailed` 色,`RhTypography.caption`)——启发式:`control is AppDetailInputControl.MediaUpload` 即标星。

- [ ] **Step 4:** `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest --rerun` + `:composeApp:assembleDebug` → 全绿。
- [ ] **Step 5: Commit**:`feat(detail): adaptive collapse params section with modified badge and group reset`(路径限定)。

### Task 5: 拆分 AppDetailScreen.kt(零视觉变化)

**Files:**
- Create: `composeApp/.../detail/AppDetailHero.kt`(`AppDetailHero`、`CoverCarousel`、`StatsCard`、`StatItem`、`StatDivider`、`AuthorRow`,原 :602-905)
- Create: `composeApp/.../detail/AppDetailDescription.kt`(`DescriptionSection` + presentation 数据/纯函数/常量,原 :907-1077)
- Create: `composeApp/.../detail/AppDetailCreationEntry.kt`(`AppDetailCreationEntry`、`CompactDetailCover`、`CreationInfoRow`、`CreationInputRow`,原 :381-600)
- Create: `composeApp/.../detail/AppDetailInputSection.kt`(`InputNodesContent`(若 Task 4 后仍有引用)、`RenderInputNodeField`、`MultiImageUploadRow`、`InputDivider`、`InputNodeField`,原 :1099-1352)
- Create: `composeApp/.../detail/AppDetailRunBar.kt`(`RunTaskBottomBar`,原 :1354-1440)
- Create: `composeApp/.../detail/AppDetailMappers.kt`(`permission()`、`toComponentMediaType()`、`appDetailErrorMessage()`、`toComponentTaskStep()`,原 :1210-1241)
- Modify: `AppDetailScreen.kt`(只留 `AppDetailScreen` 入口 + `DetailContent` + `SectionHeader`,≤ 300 行)

- [ ] **Step 1:** 按清单**原样搬移**(同包 internal、函数体不改);`AppDetailDescriptionPresentationTest` 若因搬移需要改 import,只改 import。
- [ ] **Step 2:** 编译 + 测试(`:composeApp:testDebugUnitTest --rerun`,重点 `AppDetailDescriptionPresentationTest`、`AppDetailScreenModelTest`、`AppDetailTextFieldLineLimitsTest`)→ 全绿;主文件行数 ≤ 300 验证。
- [ ] **Step 3:** Commit:`refactor(detail): split AppDetailScreen into responsibility files (no behavior change)`(路径限定,七个文件)。

### Task 6: Token 化迁移 + 组件替换

**Files:**
- Modify: 上述拆分后的全部 detail 文件 + `AppDetailInputFields.kt` + `AppDetailTaskResult.kt`

**映射表(唯一裁量标准):**

| 旧符号 | 新 token(`RhTheme.colors.`) |
|---|---|
| `DarkBackground` | `backgroundPrimary` |
| `DarkSurface` | `surfaceDefault` |
| `DarkSurfaceVariant` | `surfaceElevated` |
| `Primary300` / `Primary500` | `brandPrimary` |
| `Neutral400` | `textSecondary` |
| `Neutral500` | `textTertiary` |
| `SuccessDark` | `statusSuccess` |
| `ErrorDark` | `statusFailed` |
| 裸 `Color.White`(深底文字/图标) | `textPrimary` |
| 裸 `Color.White`(品牌底上) | `textInverse` |
| 裸 `Color.Black`(遮罩) | `overlayScrim` |
| `Color.Transparent` | 保留 |

**组件替换:**
1. `RunTaskBottomBar` 的自绘按钮 → `RhPrimaryButton(text = <现有运行文案含积分>, onClick = ..., enabled = ..., loading = uiState.isRunningTask)`(RhButtons.kt:124 签名;底栏容器保留固定定位与背景 `backgroundPrimary` + 上边框 `borderSubtle`)。
2. Hero 标签墙(chunked Row + Box 自绘)→ `RhChip(label = tag, onClick = {}, style = RhChipStyle.Neutral)` 的 FlowRow 替代——**禁用实验性 FlowRow**:复用批 0 的稳定换行布局。本任务把 `QuickCreateSampleWrapRow` 从 `QuickCreateEmptyGuide.kt` **提升为公共组件**:新建 `composeApp/.../designsystem/components/layout/RhWrapRow.kt`,函数改名 `RhWrapRow(spacing: Dp, modifier, content)`(实现原样搬移,KDoc 改为通用描述),`QuickCreateEmptyGuide` 与 Hero 标签墙都调用它;在 `RhCoreComponentsContractTest` 加一条 `assertEquals(0, 0)` 级别的存在性断言不必要——改为不加契约测试,由两处调用方编译保证。
3. `AppDetailInputFields.kt` 的 `SegmentedSelector` → `RhSegmentedControl`;`ListDropdown` 改造:`options.size > 6` 时点击不再展开内联菜单,而是回调 `onOpenPicker()` 由页面弹 `AppDetailOptionPickerSheet`(页面持 `remember { mutableStateOf<AppDetailInputFieldUiModel?>(null) }` 控制显隐,选择后 `updateInputValue` 并关闭);`≤6` 保持内联下拉但 token 化。
4. `AppDetailTaskResult.kt` 的 `TaskOutputCard` 本批只 token 化(不替换为 ResultPreview——输出交互耦合上传/保存逻辑,列入批 1 债务)。
5. 修改标记:`RenderInputNodeField` 标题左侧,`field.isModified` 时画 2dp 宽 `brandPrimary` 竖条(`Modifier.width(2.dp).background(colors.brandPrimary)`)。

- [ ] **Step 1:** 逐文件替换(顺序:InputFields → TaskResult → Hero → Description → CreationEntry → InputSection → RunBar → Screen 骨架),每文件完成即编译。
- [ ] **Step 2: 归零验证**:

```powershell
Get-ChildItem -Filter *.kt composeApp\src\commonMain\kotlin\com\runninghub\app\ui\feature\detail | Select-String -Pattern "ui\.theme|Primary300|Primary500|Neutral4|Neutral5|DarkSurface|DarkBackground|SuccessDark|ErrorDark"
```

Expected: 无输出。

- [ ] **Step 3:** `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest --rerun` + `:composeApp:assembleDebug` → 全绿。
- [ ] **Step 4:** Commit:`refactor(detail): retire legacy palette, adopt Rh components (RhPrimaryButton/RhChip/RhSegmentedControl/RhWrapRow)`(路径限定)。

### Task 7: 聚合验证

- [ ] `:composeApp:testDebugUnitTest --rerun`、`:feature:detail:presentation:testDebugUnitTest`、`checkArchitectureBoundaries`、`:composeApp:assembleDebug` 依次 BUILD SUCCESSFUL;失败先修复。

### Task 8: Android 真机截图验收(用户 gate)

- [ ] 安装启动(同批 1a 流程),导航到一个**轻参数**应用详情 + 一个**重参数**应用详情(参数 >6,验证折叠分组/已调徽章/展开态/长下拉弹层)。
- [ ] 截图:`batch1b-detail-light.png`、`batch1b-detail-heavy.png`、`batch1b-detail-picker.png`(长下拉弹层打开态);`logcat -d -b crash` 无 FATAL。
- [ ] 发用户确认;通过后在 spec §8 追加批 1 封板记录(含批 1 债务:TaskOutputCard 未换 ResultPreview、搜索入口仍 no-op、必填仅启发式星标)并 commit `docs: seal batch-1 baseline`。

---

**风险与注意:** presentation 模块改动(Task 1/2)是本批唯一跨模块工作,必须跑 `checkArchitectureBoundaries`;`AppDetailInputFieldUiModel` 加字段是 additive,但所有手工构造点(测试/预览)需同步补参;折叠展开用 `rememberSaveable` 保证旋转/重组存活;用户 staged 构建脚本改动全程不碰;禁实验性布局 API。
