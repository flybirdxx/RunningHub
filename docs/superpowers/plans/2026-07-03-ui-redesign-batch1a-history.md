# 批 1a 任务历史页重设计实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 任务历史页迁移到批 0 封板的 Rh 设计系统(RhTopBar/RhChip 筛选/RhTaskStatusBadge/语义 token),并把 1234 行单文件拆成 6 个职责文件。

**Architecture:** 先做零视觉变化的纯文件拆分(3 个任务),再做视觉迁移(2 个任务)。状态层(`TaskHistoryStateHolder`/`TaskHistoryScreenModel`)零改动;所有改动在 composeApp 渲染层。每任务单独 commit、路径限定提交(工作区有用户未提交的构建脚本改动,严禁混入)。

**Tech Stack:** Compose Multiplatform;测试 kotlin.test 契约模式;验收走 Android 真机截图(遵循批 0 封板流程)。

**上位文档:** `docs/superpowers/specs/2026-07-03-ui-redesign-design.md`(§8 批 0 封板:强制禁用实验性布局 API;token 标准写法);mockup 定案:RhChip 筛选行、语义状态徽章、RhTopBar。

**现状关键事实**(实施前自行核对行号,以内容为准):
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryScreen.kt` 1234 行,全部 Composable 集中于此。
- 文件底部(约 :1228-1234)用私有别名把语义色映射回旧 `RhApp*` 常量(`private val RhBackground = RhAppBackground` 等)。
- 5 处硬编码 `Color(0x…)`:缩略图状态底(:533-535)、SourceBadge(:820)、TaskStatusPill(:834/:836)。
- 已复用 designsystem:`HistoryTaskCard`、`ResultPreview`、`BillingInfoCard`、`RhTaskStatus` 枚举(含现成映射器 `toRhTaskStatus()` 与文案 `statusPillLabelText()`)。
- 详情为同页右滑抽屉(交互不动)。

---

### Task 1: 拆出映射层(零视觉变化)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryUiMappers.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryScreen.kt`

- [ ] **Step 1:** 把 TaskHistoryScreen.kt 中 `:901-1152` 区段的全部非 Composable 扩展/映射函数(`toHistoryTaskCardState`、`toTaskDetailLayoutState`、`toResultPreviewMediaState`、`toRhTaskStatus`、`toDisplayHistoryError`、`toDisplayActionMessage`、`statusPillLabelText`、`sourceLabelText`、`filteredBy` 及同区段其余 `toXxx()`/文案函数)**原样搬移**到新文件 `TaskHistoryUiMappers.kt`(package 不变 `com.runninghub.app.ui.feature.history`;`internal` 可见性;imports 按需迁移)。函数体一个字符都不改。
- [ ] **Step 2:** 编译验证:`.\gradlew.bat --console=plain :composeApp:compileDebugKotlinAndroid` → BUILD SUCCESSFUL。
- [ ] **Step 3:** 跑既有测试:`.\gradlew.bat --console=plain :composeApp:testDebugUnitTest --rerun` → BUILD SUCCESSFUL(尤其 `TaskDetailResultLayoutContractTest`、`QuickCreateGenerationHistoryRepositoryAdapterTest`)。
- [ ] **Step 4:** Commit:

```bash
git add composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryUiMappers.kt
git commit -m "refactor(history): extract ui mappers to TaskHistoryUiMappers (no behavior change)" -- composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryUiMappers.kt composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryScreen.kt
```

### Task 2: 拆出详情抽屉(零视觉变化)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskDetailDrawer.kt`
- Modify: `TaskHistoryScreen.kt`

- [ ] **Step 1:** 把 `:563-816` 的抽屉群组 **原样搬移** 到 `TaskDetailDrawer.kt`:`TaskDetailDrawer`、`TaskDetailContent`、`TaskDetailStatusSummary`、`TaskDetailResultPreview`、`TaskDetailBillingSection`、`TaskDetailPromptParameters`、`TaskDetailTechnicalDetails`、`TaskDetailTechnicalSection`、`DetailKeyValueRow`。同包、internal、函数体不改。搬移后若这些函数引用了仍留在 TaskHistoryScreen.kt 底部的私有色别名(`RhCard`/`RhLine`/`RhText`/`RhMuted` 等),把别名声明**临时提升**为同包 `internal`(仍指向旧常量,Task 5 统一替换)。
- [ ] **Step 2:** 编译 + 测试(同 Task 1 Step 2/3 命令)→ 全绿。
- [ ] **Step 3:** Commit:`refactor(history): extract task detail drawer (no behavior change)`(路径限定,两个文件)。

### Task 3: 拆出列表区/原子件/预览(零视觉变化)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryListSections.kt`(`HistoryTopBar`、`TaskHistoryFilterRow`、`StatusTab`、`NoticeBar`、`HistoryActionPanel`、`DateGroupHeader`、`TaskTimelineRow`、`TaskThumbnail`,原 :297-560)
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryAtoms.kt`(`SourceBadge`、`TaskStatusPill`、`SmallAction`、`LoadingPanel`、`TaskHistoryErrorState`、`TaskHistoryEmptyState`,原 :819-898)
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryPreviews.kt`(9 个 `@Preview`,原 :1155-1234)
- Modify: `TaskHistoryScreen.kt`(拆完只留 `TaskHistoryVoyagerScreen` + `TaskHistoryContent` 骨架,约 120-150 行)

- [ ] **Step 1:** 三个新文件按上述清单**原样搬移**(同包 internal;色别名声明集中放到 TaskHistoryAtoms.kt 顶部作 internal,其他文件引用)。
- [ ] **Step 2:** 编译 + 测试 → 全绿;`(Get-Content <TaskHistoryScreen.kt> | Measure-Object -Line).Lines` 确认主文件 ≤ 200 行。
- [ ] **Step 3:** Commit:`refactor(history): split list sections, atoms and previews (no behavior change)`(路径限定,四个文件)。

### Task 4: 顶栏与筛选迁移(视觉变化 · 按 mockup)

**Files:**
- Modify: `TaskHistoryListSections.kt`

- [ ] **Step 1: HistoryTopBar 改为委托 RhTopBar**(保留搜索/刷新 action;搜索现为 no-op 占位,维持现状不新增功能):

```kotlin
@Composable
internal fun HistoryTopBar(
    title: String,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    RhTopBar(
        title = title,
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = null, tint = RhTheme.colors.textSecondary)
            }
            IconButton(onClick = onRefreshClick) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = RhTheme.colors.textSecondary)
            }
        },
    )
}
```

调用点参数对齐(原实现如带 contentDescription 资源则原样传递,不得删除无障碍文案)。import `com.runninghub.app.ui.designsystem.components.navigation.RhTopBar`。

- [ ] **Step 2: 筛选行改 RhChip**——`TaskHistoryFilterRow` 内的自绘 `StatusTab`(底部指示条样式)替换为:

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm)) {
    filters.forEach { filter ->
        RhChip(
            label = filter.labelText(),   // 沿用现有文案函数,不新增字符串
            selected = filter == selectedFilter,
            onClick = { onFilterSelected(filter) },
        )
    }
}
```

`filters`/`labelText()`/回调名以现文件实际为准对齐;删除 `StatusTab` 函数。import `com.runninghub.app.ui.designsystem.components.chips.RhChip`。

- [ ] **Step 3:** 编译 + 测试 → 全绿。
- [ ] **Step 4:** Commit:`refactor(history): adopt RhTopBar and RhChip filter row`(路径限定)。

### Task 5: 原子件语义化 + 旧色板别名清零

**Files:**
- Modify: `TaskHistoryAtoms.kt`、`TaskHistoryListSections.kt`、`TaskDetailDrawer.kt`、`TaskHistoryScreen.kt`、`TaskHistoryPreviews.kt`

**映射表(唯一裁量标准):**

| 旧符号/硬编码 | 新 token(`RhTheme.colors.`) |
|---|---|
| `RhBackground`(RhAppBackground/BaseBlack) | `backgroundPrimary` |
| `RhSurface`(RhAppSurface) | `backgroundSecondary` |
| `RhCard`(RhAppCard) | `surfaceDefault` |
| `RhSelected`(#1B2117) | `surfaceSelected`(现值完全相同) |
| `RhLine`(#30363A) | `borderDefault` |
| `RhText` / `RhMuted` | `textPrimary` / `textSecondary` |
| `BrandLime` | `brandPrimary` |
| `StatusError` | `statusFailed` |
| 缩略图完成底 `Color(0xFF2F3F2C)` | `brandMuted` |
| 缩略图失败底 `Color(0xFF3F2020)` | `surfaceSunken`(图标/文字用 `statusFailed`) |
| 缩略图处理中底 `Color(0xFF1D2A35)` | `surfaceSunken`(图标/文字用 `statusProcessing`) |
| SourceBadge `Color(0xFF53D66A)` / `0xFF60A5FA` | `statusSuccess` / `statusProcessing` |
| 遮罩 `Color.Black.copy(alpha = 0.48f)` | `overlayScrim` |
| 裸 `Color.White` | 按底色:深底上 → `textPrimary`;品牌底上 → `textInverse` |

- [ ] **Step 1: TaskStatusPill 替换为 RhTaskStatusBadge**——删除自绘 `TaskStatusPill`(含 :834/:836 两处硬编码色),调用点改为:

```kotlin
RhTaskStatusBadge(
    status = entry.status.toRhTaskStatus(),
    label = entry.status.statusPillLabelText(),
)
```

(两个映射函数已在 `TaskHistoryUiMappers.kt`,receiver 类型以实际为准对齐。)import `com.runninghub.app.ui.designsystem.components.badges.RhTaskStatusBadge`。

- [ ] **Step 2:** `SourceBadge`/`TaskThumbnail`/`SmallAction`/三态/`NoticeBar`/`HistoryActionPanel`/抽屉全部按映射表替换;三态(`LoadingPanel`/`TaskHistoryErrorState`/`TaskHistoryEmptyState`)改为委托 designsystem 的 `RhLoadingState`/`RhErrorState`/`RhEmptyState`(`components/states/RhStates.kt`,文案沿用现有资源)。
- [ ] **Step 3:** 删除 TaskHistoryAtoms.kt 顶部的全部旧色别名声明及 `com.runninghub.app.ui.theme` import。
- [ ] **Step 4: 归零验证**:

```powershell
Get-ChildItem -Filter *.kt composeApp\src\commonMain\kotlin\com\runninghub\app\ui\feature\history | Select-String -Pattern "ui\.theme|RhApp|BrandLime|StatusError|Color\(0x"
```

Expected: 无输出。

- [ ] **Step 5:** 编译 + 测试 + `.\gradlew.bat --console=plain :composeApp:assembleDebug` → 全绿。
- [ ] **Step 6:** Commit:`refactor(history): retire legacy palette for Rh semantic tokens`(路径限定,五个文件)。

### Task 6: 聚合验证

- [ ] `.\gradlew.bat --console=plain :composeApp:testDebugUnitTest --rerun` → BUILD SUCCESSFUL
- [ ] `.\gradlew.bat --console=plain checkArchitectureBoundaries` → BUILD SUCCESSFUL
- [ ] `.\gradlew.bat --console=plain :composeApp:assembleDebug` → BUILD SUCCESSFUL
- [ ] 任一失败:修复后重跑,不得带失败进 Task 7。

### Task 7: Android 真机截图验收(用户 gate)

- [ ] adb(`"D:\Program Files\Android\SDK\platform-tools\adb.exe"`,设备 5d692d82)`install -r` debug APK → 启动 `com.runninghub.app.debug/com.runninghub.app.MainActivity` → 切到「任务」Tab。
- [ ] 截图(screencap 到 /sdcard 再 pull,**禁止 PowerShell `>` 重定向**):`batch1a-history-list.png`(列表 + 筛选选中态)、`batch1a-history-drawer.png`(点开一条任务的详情抽屉)。
- [ ] `logcat -d -b crash` 无 FATAL;对照 mockup 自查:筛选芯片选中橄榄、状态徽章语义色、无旧霓虹绿/蓝残留。
- [ ] 截图发用户确认;通过才算完成,有意见回对应 Task 修。

---

**风险与注意:** 用户 staged 的构建脚本改动与 `.codex/references/conventions.md` 全程不碰;拆分任务(1-3)必须零逻辑变更,diff 里出现函数体修改即返工;抽屉交互(拖拽/遮罩点击关闭)不动;禁用实验性布局 API(spec §8)。
