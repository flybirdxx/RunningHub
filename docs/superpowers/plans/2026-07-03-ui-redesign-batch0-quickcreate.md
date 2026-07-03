# 批 0 样板间(快速创作页 + 主壳)实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按已定案的橄榄暗调 mockup 重塑快速创作页与应用壳,把全局 token/核心组件封板,作为后续批次的规范基准。

**Architecture:** 只动 `composeApp` Presentation UI 层。新组件全部进 `ui/designsystem/components/`,从 `RhTheme` 取 token;QuickCreate 页面私有的霓虹 token(`QuickCreateDesignTokens`)整体退役映射到 Rh 语义色。不碰 Domain/Data/StateHolder。

**Tech Stack:** Compose Multiplatform + Voyager + Koin;测试走 `commonTest` 的 kotlin.test 契约测试(项目既有模式,见 `RhDesignSystemContractTest`);真实渲染靠 Android 模拟器截图验收。

**上位文档:** `docs/superpowers/specs/2026-07-03-ui-redesign-design.md`(已批准)。mockup 定案:空态引导 + 对话流 + composer + 底栏,全部取 `RhDarkColors` 现值。

**范围裁剪说明:** spec 列出的 `RhPromptField`/`RhListItem`/`RhDialog` 本批不建——composer 输入框在既有 `QuickCreateCompactComposerContent` 内就地 token 化即可,`RhListItem` 首个使用方在批 1(历史页),`RhDialog` 首个改造点也在批 1;遵循 YAGNI,首个使用方出现时再建。

**每个任务完成即单独 commit;commit 只加本任务列出的文件。**

---

### Task 1: RhTheme 暴露 spacing 入口

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/theme/RhTheme.kt`
- Test: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/designsystem/RhDesignSystemContractTest.kt`

- [ ] **Step 1: 写失败测试** — 在 `RhDesignSystemContractTest` 里加:

```kotlin
    @Test
    fun `theme exposes spacing scale`() {
        assertSame(RhSpacing, RhTheme.spacing)
    }
```

顶部补 import:`import com.runninghub.app.ui.designsystem.theme.RhTheme`、`import kotlin.test.assertSame`。

- [ ] **Step 2: 跑测试确认失败**

Run: `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.designsystem.RhDesignSystemContractTest"`
Expected: 编译失败 `unresolved reference: spacing`。

- [ ] **Step 3: 实现** — `RhTheme.kt` 的 `object RhTheme` 内、`typography` 属性之后加:

```kotlin
    /** 当前静态间距刻度表,页面布局统一从这里取值。 */
    val spacing: RhSpacing
        get() = RhSpacing
```

- [ ] **Step 4: 跑测试确认通过**(同 Step 2 命令,Expected: BUILD SUCCESSFUL)

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/theme/RhTheme.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/designsystem/RhDesignSystemContractTest.kt
git commit -m "feat(designsystem): expose spacing scale via RhTheme"
```

---

### Task 2: 锁定暗色(移除系统深浅色跟随)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/Theme.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/App.kt:43`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/adaptive/RhPreview.kt:54`
- Modify: `composeApp/src/androidMain/res/values/themes.xml`
- Create: `composeApp/src/androidMain/res/values/colors.xml`
- Modify: `composeApp/src/androidMain/kotlin/com/runninghub/app/MainActivity.kt`

- [ ] **Step 1: Theme.kt 去掉 darkTheme 参数** — 把 `RunningHubTheme` 改为:

```kotlin
/**
 * 应用壳主题入口。
 *
 * redesign 决策:App 锁定暗色,不跟随系统深浅色。浅色 Material scheme 与
 * RhLightColors 保留类型定义但不再进入运行路径,收尾批评估删除。
 */
@Composable
fun RunningHubTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalExtendedColors provides DarkExtendedColors,
        LocalRhColors provides RhDarkColors,
        LocalRhShapes provides RhDefaultShapes,
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
```

同文件删除:`private val LightColorScheme = lightColorScheme(...)` 整块、`import androidx.compose.foundation.isSystemInDarkTheme`、`import androidx.compose.material3.lightColorScheme`、`import com.runninghub.app.ui.designsystem.theme.RhLightColors`。

- [ ] **Step 2: 更新两个调用点**
  - `App.kt:43`:`RunningHubTheme(darkTheme = true) {` → `RunningHubTheme {`
  - `RhPreview.kt:54`:同样去掉 `darkTheme = true` 实参。

- [ ] **Step 3: Android 启动窗口锁暗**(消除冷启动白闪 + 状态栏图标反色)

`colors.xml`(新建):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="rh_background_primary">#FF050608</color>
</resources>
```

`themes.xml` 整体替换为:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.RunningHub" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/rh_background_primary</item>
    </style>
</resources>
```

`MainActivity.kt` 的 `enableEdgeToEdge()` 改为强制暗色系统栏(浅色图标):

```kotlin
import androidx.activity.SystemBarStyle
import android.graphics.Color as AndroidColor

enableEdgeToEdge(
    statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
)
```

- [ ] **Step 4: 验证**

Run: `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest`
Expected: BUILD SUCCESSFUL(`RunningHubThemeTokenTest` 不受影响,它只断言旧色板常量)。

Run: `.\gradlew.bat --console=plain :composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/Theme.kt composeApp/src/commonMain/kotlin/com/runninghub/app/App.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/adaptive/RhPreview.kt composeApp/src/androidMain/res/values/themes.xml composeApp/src/androidMain/res/values/colors.xml composeApp/src/androidMain/kotlin/com/runninghub/app/MainActivity.kt
git commit -m "feat(theme): lock app to dark mode incl. android launch window"
```

---

### Task 3: RhChip 组件

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/chips/RhChip.kt`
- Test: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/designsystem/RhCoreComponentsContractTest.kt`(新建,Task 4-6 复用)

- [ ] **Step 1: 写失败测试** — 新建契约测试文件:

```kotlin
package com.runninghub.app.ui.designsystem

import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.chips.RhChipDefaults
import com.runninghub.app.ui.designsystem.components.chips.RhChipStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class RhCoreComponentsContractTest {
    @Test
    fun `chip styles are semantic`() {
        assertEquals("neutral", RhChipStyle.Neutral.tokenName)
        assertEquals("brand", RhChipStyle.Brand.tokenName)
        assertEquals(32.dp, RhChipDefaults.height)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.designsystem.RhCoreComponentsContractTest"`
Expected: 编译失败 `unresolved reference`。

- [ ] **Step 3: 实现组件**(新文件,完整内容):

```kotlin
package com.runninghub.app.ui.designsystem.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/**
 * 芯片语义风格。Neutral 用于常规可选项,Brand 用于品牌强调入口(如当前模型)。
 */
enum class RhChipStyle(val tokenName: String) {
    Neutral("neutral"),
    Brand("brand"),
}

/** RhChip 的固定尺寸契约。 */
object RhChipDefaults {
    val height = 32.dp
}

/**
 * 胶囊形选择芯片。选中态与 Brand 风格使用品牌底色 + 品牌文字,其余使用默认表面色。
 *
 * @param label 芯片文案,调用方负责本地化。
 * @param onClick 点击回调。
 * @param style 语义风格。
 * @param selected 是否处于选中态。
 * @param leadingIcon 可选前置图标插槽。
 * @param trailingIcon 可选后置图标插槽。
 */
@Composable
fun RhChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RhChipStyle = RhChipStyle.Neutral,
    selected: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = RhTheme.colors
    val highlighted = selected || style == RhChipStyle.Brand
    val shape = RoundedCornerShape(RhTheme.shapes.full)
    Row(
        modifier = modifier
            .height(RhChipDefaults.height)
            .clip(shape)
            .background(if (highlighted) colors.brandMuted else colors.surfaceDefault)
            .border(1.dp, if (highlighted) colors.borderActive else colors.borderDefault, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = RhSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
    ) {
        leadingIcon?.invoke()
        Text(
            text = label,
            color = if (highlighted) colors.brandPrimary else colors.textSecondary,
            style = RhTypography.caption,
            maxLines = 1,
        )
        trailingIcon?.invoke()
    }
}
```

- [ ] **Step 4: 跑测试确认通过**(同 Step 2,Expected: BUILD SUCCESSFUL)

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/chips/RhChip.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/designsystem/RhCoreComponentsContractTest.kt
git commit -m "feat(designsystem): add RhChip"
```

---

### Task 4: RhSegmentedControl 组件

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/segmented/RhSegmentedControl.kt`
- Test: `RhCoreComponentsContractTest.kt`(追加)

- [ ] **Step 1: 追加失败测试**:

```kotlin
    @Test
    fun `segmented control keeps compact height`() {
        assertEquals(32.dp, RhSegmentedControlDefaults.height)
    }
```

import 补 `com.runninghub.app.ui.designsystem.components.segmented.RhSegmentedControlDefaults`。

- [ ] **Step 2: 跑测试确认编译失败**(命令同 Task 3 Step 2)

- [ ] **Step 3: 实现组件**(新文件,完整内容):

```kotlin
package com.runninghub.app.ui.designsystem.components.segmented

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** RhSegmentedControl 的固定尺寸契约。 */
object RhSegmentedControlDefaults {
    val height = 32.dp
}

/**
 * 分段切换控件,用于图片/视频这类互斥模式切换。
 * 选中段使用品牌底色胶囊,未选中段保持透明,贴合暗色面板。
 *
 * @param options 分段文案,调用方负责本地化;顺序即展示顺序。
 * @param selectedIndex 当前选中下标。
 * @param onSelect 点击某段时回传其下标。
 */
@Composable
fun RhSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    Row(
        modifier = modifier.height(RhSegmentedControlDefaults.height),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(RhTheme.shapes.full))
                    .background(if (selected) colors.brandMuted else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = RhSpacing.lg, vertical = RhSpacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    color = if (selected) colors.brandPrimary else colors.textTertiary,
                    style = if (selected) RhTypography.bodyStrong else RhTypography.body,
                    maxLines = 1,
                )
            }
        }
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/segmented/RhSegmentedControl.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/designsystem/RhCoreComponentsContractTest.kt
git commit -m "feat(designsystem): add RhSegmentedControl"
```

---

### Task 5: RhTopBar 组件

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/navigation/RhTopBar.kt`
- Test: `RhCoreComponentsContractTest.kt`(追加)

- [ ] **Step 1: 追加失败测试**:

```kotlin
    @Test
    fun `top bar keeps standard height`() {
        assertEquals(56.dp, RhTopBarDefaults.height)
    }
```

import 补 `com.runninghub.app.ui.designsystem.components.navigation.RhTopBarDefaults`。

- [ ] **Step 2: 跑测试确认编译失败**

- [ ] **Step 3: 实现组件**(新文件,完整内容):

```kotlin
package com.runninghub.app.ui.designsystem.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** RhTopBar 的固定尺寸契约。 */
object RhTopBarDefaults {
    val height = 56.dp
}

/**
 * 页面顶栏。三段布局互相覆盖定位,左右插槽显隐不影响标题居中。
 *
 * @param title 居中标题,调用方负责本地化。
 * @param navigationIcon 可选左侧导航插槽(返回/菜单)。
 * @param actions 可选右侧操作插槽。
 */
@Composable
fun RhTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RhTopBarDefaults.height)
            .background(RhTheme.colors.backgroundPrimary),
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = RhSpacing.sm)) {
            navigationIcon?.invoke()
        }
        Text(
            text = title,
            color = RhTheme.colors.textPrimary,
            style = RhTypography.cardTitle,
            modifier = Modifier.align(Alignment.Center),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = RhSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions?.invoke(this)
        }
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/navigation/RhTopBar.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/designsystem/RhCoreComponentsContractTest.kt
git commit -m "feat(designsystem): add RhTopBar"
```

---

### Task 6: RhSnackbar 组件

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/feedback/RhSnackbar.kt`
- Test: `RhCoreComponentsContractTest.kt`(追加)

- [ ] **Step 1: 追加失败测试**:

```kotlin
    @Test
    fun `snackbar severities are semantic`() {
        assertEquals("info", RhSnackbarSeverity.Info.tokenName)
        assertEquals("error", RhSnackbarSeverity.Error.tokenName)
    }
```

import 补 `com.runninghub.app.ui.designsystem.components.feedback.RhSnackbarSeverity`。

- [ ] **Step 2: 跑测试确认编译失败**

- [ ] **Step 3: 实现组件**(新文件,完整内容):

```kotlin
package com.runninghub.app.ui.designsystem.components.feedback

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** 提示条语义级别。文案必须是 Presentation 层归一化后的安全文案。 */
enum class RhSnackbarSeverity(val tokenName: String) {
    Info("info"),
    Error("error"),
}

/**
 * 轻量提示条,由调用方控制显隐与消失时机。
 *
 * @param message 本地化后的提示文案,不得直接透出服务端原始 message。
 * @param severity 语义级别,决定配色。
 */
@Composable
fun RhSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    severity: RhSnackbarSeverity = RhSnackbarSeverity.Info,
) {
    val colors = RhTheme.colors
    val background = when (severity) {
        RhSnackbarSeverity.Info -> colors.surfaceElevated
        RhSnackbarSeverity.Error -> colors.statusFailed
    }
    val content = when (severity) {
        RhSnackbarSeverity.Info -> colors.textPrimary
        RhSnackbarSeverity.Error -> colors.textInverse
    }
    Surface(
        color = background,
        shape = RoundedCornerShape(RhTheme.shapes.md),
        modifier = modifier,
    ) {
        Text(
            text = message,
            color = content,
            style = RhTypography.caption,
            modifier = Modifier.padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
        )
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/feedback/RhSnackbar.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/designsystem/RhCoreComponentsContractTest.kt
git commit -m "feat(designsystem): add RhSnackbar"
```

---

### Task 7: 快速创作空态引导

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateEmptyGuide.kt`
- Test: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateEmptyGuideTest.kt`

- [ ] **Step 1: 写失败测试**(新文件):

```kotlin
package com.runninghub.app.ui.feature.quickcreate

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateEmptyGuideTest {
    @Test
    fun `samples are trimmed filtered and capped at three`() {
        val samples = quickCreateEmptySamples(listOf(" 赛博朋克 ", "", "  ", "水彩猫", "海岸", "多余的第四条"))
        assertEquals(listOf("赛博朋克", "水彩猫", "海岸"), samples)
    }
}
```

- [ ] **Step 2: 跑测试确认编译失败**

Run: `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.quickcreate.QuickCreateEmptyGuideTest"`

- [ ] **Step 3: 加字符串资源** — `strings.xml` 追加(与文件既有语言风格一致):

```xml
    <string name="quick_create_empty_title">描述一个画面，开始创作</string>
    <string name="quick_create_empty_subtitle">支持图片与视频生成</string>
    <string name="quick_create_empty_sample_city">赛博朋克城市夜景</string>
    <string name="quick_create_empty_sample_cat">水彩风格的猫</string>
    <string name="quick_create_empty_sample_coast">日落海岸延时</string>
```

- [ ] **Step 4: 实现组件**(新文件,完整内容):

```kotlin
package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.chips.RhChip
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_empty_sample_cat
import runninghub.composeapp.generated.resources.quick_create_empty_sample_city
import runninghub.composeapp.generated.resources.quick_create_empty_sample_coast
import runninghub.composeapp.generated.resources.quick_create_empty_subtitle
import runninghub.composeapp.generated.resources.quick_create_empty_title

/** 归一化示例提示词:去空白、去空串、最多保留 3 条。 */
internal fun quickCreateEmptySamples(raw: List<String>): List<String> =
    raw.map { it.trim() }.filter { it.isNotEmpty() }.take(3)

/**
 * 无对话时的空态引导:品牌图标、引导文案和示例提示词芯片。
 *
 * @param onSampleClick 点击示例时回传其文案,由调用方写回创作输入框。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickCreateEmptyGuide(
    onSampleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RhTheme.colors
    val samples = quickCreateEmptySamples(
        listOf(
            stringResource(Res.string.quick_create_empty_sample_city),
            stringResource(Res.string.quick_create_empty_sample_cat),
            stringResource(Res.string.quick_create_empty_sample_coast),
        ),
    )
    Column(
        modifier = modifier.fillMaxSize().padding(RhSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(56.dp).background(colors.brandMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = colors.brandPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(RhSpacing.md))
        Text(
            text = stringResource(Res.string.quick_create_empty_title),
            color = colors.textPrimary,
            style = RhTypography.cardTitle,
        )
        Spacer(Modifier.height(RhSpacing.xs))
        Text(
            text = stringResource(Res.string.quick_create_empty_subtitle),
            color = colors.textTertiary,
            style = RhTypography.caption,
        )
        Spacer(Modifier.height(RhSpacing.lg))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        ) {
            samples.forEach { sample ->
                RhChip(label = sample, onClick = { onSampleClick(sample) })
            }
        }
    }
}
```

- [ ] **Step 5: 跑测试确认通过**(同 Step 2 命令)

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/composeResources/values/strings.xml composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateEmptyGuide.kt composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateEmptyGuideTest.kt
git commit -m "feat(quickcreate): add empty-state guide with sample prompts"
```

---

### Task 8: QuickCreateScreen 接入新组件与 token

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreen.kt`

改动点(全部在此文件内,行号基于当前版本):

- [ ] **Step 1: 背景与旧 theme 引用**
  - 两处 `.background(DarkBackground)`(行 237、567)与预览里的同类引用 → `.background(RhTheme.colors.backgroundPrimary)`(在 `@Composable` 上下文内读取,需先提成 `val colors = RhTheme.colors`)。
  - 删除 `import com.runninghub.app.ui.theme.*`,改为按需导入:`com.runninghub.app.ui.designsystem.theme.RhTheme`、`com.runninghub.app.ui.designsystem.theme.RhSpacing`。

- [ ] **Step 2: 顶栏换 RhTopBar** — `QuickCreateTopBar`(行 473-523)与 `QuickCreateModeTitle`(行 525-536)整体替换为:

```kotlin
@Composable
private fun QuickCreateTopBar(
    onBack: (() -> Unit)?,
    onCreateProject: (String) -> Unit,
) {
    RhTopBar(
        title = quickCreateNavigationText(QuickCreateNavigationLabel.CreationMode),
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(
                            Res.string.quick_create_top_bar_back_content_description,
                        ),
                        tint = RhTheme.colors.textSecondary,
                    )
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = stringResource(
                            Res.string.quick_create_top_bar_menu_content_description,
                        ),
                        tint = RhTheme.colors.textSecondary,
                    )
                }
            }
        },
        actions = { QuickCreateCreateProjectAction(onCreateProject = onCreateProject) },
    )
}
```

import 补 `com.runninghub.app.ui.designsystem.components.navigation.RhTopBar`。

- [ ] **Step 3: 错误横幅换 RhSnackbar** — 行 336-347 的 `Surface(color = ErrorDark...)` 块替换为:

```kotlin
RhSnackbar(
    message = errorText.orEmpty(),
    severity = RhSnackbarSeverity.Error,
    modifier = Modifier.padding(horizontal = RhSpacing.lg),
)
```

import 补 `com.runninghub.app.ui.designsystem.components.feedback.RhSnackbar`、`...RhSnackbarSeverity`。

- [ ] **Step 4: 空态接入引导** — `CreationScrollableArea`(行 539-556)签名加 `onSampleClick: (String) -> Unit`,`else` 分支 `Spacer(...)` 替换为:

```kotlin
QuickCreateEmptyGuide(onSampleClick = onSampleClick)
```

两处调用点(行 254、584)传入:主路径 `onSampleClick = screenModel::restoreConversationPrompt`,预览路径 `onSampleClick = {}`。

- [ ] **Step 5: 验证**

Run: `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest`
Expected: BUILD SUCCESSFUL。

Run: `Select-String -Path composeApp\src\commonMain\kotlin\com\runninghub\app\ui\feature\quickcreate\QuickCreateScreen.kt -Pattern "DarkBackground|ErrorDark|Primary300|Dimens\."`
Expected: 无输出。

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreen.kt
git commit -m "refactor(quickcreate): adopt RhTopBar/RhSnackbar/empty guide, drop legacy theme refs"
```

---

### Task 9: 退役 QuickCreateDesignTokens(霓虹 token 归一到 Rh 语义色)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateDesign.kt`(2 处引用 + 删除 token object)
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateConversationContent.kt`(1 处引用 + 6 处裸色)
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/presentation/editor/composer/QuickCreateCompactComposerContent.kt`(20 处引用 + 7 处裸色)
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/presentation/editor/params/QuickCreateParamsSheetContent.kt`(11 处引用 + 5 处裸色)
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/presentation/editor/composer/QuickCreateClassicComposerContent.kt`(如有引用一并处理)

**映射表(唯一裁量标准,替换时按"用途"选列右侧 token):**

| 旧 token | 新 token(`RhTheme.colors.`) |
|---|---|
| `Background` #020203 | `backgroundPrimary` |
| `Panel` 0xEE11151A | `overlaySheet`(浮层面板)/ `surfaceDefault`(实心卡片) |
| `PanelStrong` | `surfaceElevated` |
| `Stroke` | `borderDefault` |
| `StrokeSoft` | `borderSubtle` |
| `Text` | `textPrimary` |
| `Muted` | `textSecondary` |
| `Dim` | `textTertiary` |
| `Purple` / `PurpleSoft` | `brandSecondary` |
| `Green` #16F4A7(荧光绿) | 生成 CTA → `brandPrimary`(文字用 `textInverse`);成功状态 → `statusSuccess` |
| `Cyan` | `statusProcessing` |
| `Pink` | `brandSecondary` |

**裸色处理规则:** 各文件残留的 `Color(0xFF…)` 按视觉角色就近映射到上表右列;用户消息气泡底色 → `brandMuted`;分隔线 → `borderSubtle`。`QuickCreateCatThumbnail` 内的插画色(猫脸/渐变)是内容不是 UI token,**保留不动**。`QuickCreateModelGlyph` 渐变第二色 `Color(0xFF201B3B)` → `RhTheme.colors.surfaceElevated`。

- [ ] **Step 1: 逐文件替换**(顺序:QuickCreateDesign.kt → ConversationContent → CompactComposer → ParamsSheet → ClassicComposer)。`accent` 参数默认值 `QuickCreateDesignTokens.Purple` → 改为在使用点传 `RhTheme.colors.brandSecondary`(composable 默认参数可直接读 `RhTheme`,因为 getter 是 `@Composable` 上下文——若编译报错,把默认值移除、改为必填参数并在调用点显式传入)。

- [ ] **Step 2: 删除 `QuickCreateDesignTokens` object**(QuickCreateDesign.kt 行 35-49 整块及其 KDoc)。

- [ ] **Step 3: 验证归零**

Run: `Select-String -Path composeApp\src\commonMain\kotlin\com\runninghub\app\ui\feature\quickcreate -Pattern "QuickCreateDesignTokens" -Recurse`
Expected: 无输出。

Run: `Select-String -Path composeApp\src\commonMain\kotlin\com\runninghub\app\ui\feature\quickcreate -Pattern "Color\(0x" -Recurse`
Expected: 仅剩 `QuickCreateDesign.kt` 中 `QuickCreateCatThumbnail`/`QuickCreateRhAvatar` 的插画色。

- [ ] **Step 4: 跑测试**

Run: `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest`
Expected: BUILD SUCCESSFUL(`QuickCreateCompactComposerContractTest`、`QuickCreateConversationContentTest`、`QuickCreateParamsSheetContentContractTest` 全绿;若契约测试断言了旧色值,更新断言为新 token 值并在 commit message 注明)。

- [ ] **Step 5: 图片/视频切换接 RhSegmentedControl** — 在 `QuickCreateCompactComposerContent.kt` 中找到现有 tab 切换 UI(读文件定位,围绕 `onTabSwitch` 回调),替换为:

```kotlin
RhSegmentedControl(
    options = listOf(imageTabLabel, videoTabLabel),
    selectedIndex = if (isImage) 0 else 1,
    onSelect = { index ->
        onTabSwitch(if (index == 0) QuickCreateTab.IMAGE else QuickCreateTab.VIDEO)
    },
)
```

`imageTabLabel`/`videoTabLabel` 沿用该文件现有的 tab 文案来源(已本地化的 stringResource),不新增文案。

- [ ] **Step 6: 再跑 Step 3 的 grep 和 Step 4 的测试,然后 Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate
git commit -m "refactor(quickcreate): retire neon design tokens for Rh semantic palette"
```

---

### Task 10: 聚合验证

- [ ] **Step 1:** Run: `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest` → Expected: BUILD SUCCESSFUL
- [ ] **Step 2:** Run: `.\gradlew.bat --console=plain checkArchitectureBoundaries` → Expected: BUILD SUCCESSFUL
- [ ] **Step 3:** Run: `.\gradlew.bat --console=plain :composeApp:assembleDebug` → Expected: BUILD SUCCESSFUL
- [ ] **Step 4:** 任一失败:修复后重跑;不得在失败状态进入 Task 11。

---

### Task 11: Android 真实渲染验收(用户 gate)

- [ ] **Step 1: 安装到模拟器/真机**

Run: `adb devices`(确认有设备)→ `.\gradlew.bat --console=plain :composeApp:installDebug`

- [ ] **Step 2: 截图三个状态**

```powershell
adb shell am start -n com.runninghub.app/.MainActivity
# 手动或引导用户操作到目标状态后:
adb exec-out screencap -p > batch0-empty.png      # 快速创作空态(含引导)
adb exec-out screencap -p > batch0-conversation.png  # 生成对话态(需触发一次生成或用历史会话)
adb exec-out screencap -p > batch0-bottombar.png  # 底栏五 Tab 状态
```

- [ ] **Step 3: 与 mockup 对比自查**:背景 #050608、CTA 橄榄绿 #A3B565+深色文字、空态引导齐全、无霓虹绿/青/粉残留、状态栏图标为浅色、冷启动无白闪。

- [ ] **Step 4: 把截图发给用户确认。用户确认通过才算本任务完成;有意见则回到对应 Task 修改。**

---

### Task 12: 规范封板

- [ ] **Step 1:** 在 `docs/superpowers/specs/2026-07-03-ui-redesign-design.md` 末尾追加:

```markdown
## 8. 批 0 封板记录(实施后填写)

- 封板日期:<实际日期>
- 验收截图:batch0-empty.png / batch0-conversation.png / batch0-bottombar.png(用户已确认)
- 封板结论:RhColors/RhTypography/RhSpacing/RhShapes 现值 + RhChip/RhSegmentedControl/RhTopBar/RhSnackbar/AppBottomBar 为全局规范,后续批次只允许经变更评审调整 token 值。
- 批 0 遗留:RhPromptField/RhListItem/RhDialog 延后到首个使用方批次;RhLightColors 与旧 ui/theme 色板待收尾批删除。
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/specs/2026-07-03-ui-redesign-design.md
git commit -m "docs: seal batch-0 design system baseline"
```

---

## 风险与注意

- 工作区已有用户在 `feature/kmp-refactoring` 上的构建脚本改动(`build.gradle.kts`、`gradle/libs.versions.toml` 等),**不得回滚或混入本计划的 commit**;每次 commit 用显式文件列表。
- `.codex/references/conventions.md` 当前有用户未提交改动,本计划不触碰该文件。
- `QuickCreateScreenModel`、StateHolder、Coordinator、弹层交互逻辑一律不动;只改渲染层。
- 若 `:composeApp:testDebugUnitTest` 任务名不存在(KMP 目标命名差异),用 `.\gradlew.bat --console=plain :composeApp:tasks --all | Select-String test` 找到等价的 Android unit test 任务替换,并在 NOTES 里记录实际任务名。
- iOS:本批只要求 `commonMain` 无平台 API 引入(新组件均为纯 Compose),不做 iOS 链接验证,交付说明如实记录。
