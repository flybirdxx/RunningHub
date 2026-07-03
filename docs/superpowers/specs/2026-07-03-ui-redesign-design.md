# RunningHub UI 重设计方案(橄榄暗调 · 锁定暗色 · 样板间先行)

日期:2026-07-03
状态:已获用户批准,待实施计划
范围:Presentation/composeApp UI 层,全部 11 个页面,分批推进

## 1. 背景与现状

前期开发以功能实现为主,UI 未系统打磨。现状盘点结论:

- 代码中存在一次**做了一半的 design system 迁移**(roadmap RM-01/RM-02):
  - 新体系 `com.runninghub.app.ui.designsystem`:`RhTheme`(colors/shapes/typography 入口)、`RhColors`(26 个语义槽,暗色 `RhDarkColors` 为验收基准)、`RhTypography`(11 层级,letterSpacing 固定 0)、`RhSpacing`(8 档)、`RhShapes`,以及 Rh* 组件族(按钮/徽章/卡片/底部弹层/状态视图/底栏/参数选择器/计费组件)。
  - 旧体系 `com.runninghub.app.ui.theme`:Material3 `MaterialTheme` + `ExtendedColors`,与新体系在 `RunningHubTheme` 中双轨并存。
- UI 债务:
  - 巨型页面文件:`AppDetailScreen.kt` 1362 行、`TaskHistoryScreen.kt` 1160 行、`PlazaScreen.kt` 1009 行、`DiscoveryScreen.kt` 707 行。
  - 页面文件中约 56 处硬编码 `Color(0xFF…)`(不含色板定义文件),集中在 QuickCreate 相关文件(`QuickCreateDesign.kt` 23 处)。
  - 同名组件新旧两份(如两个 `AppCard`),页面引用来源不一。
- 导航:底部 5 Tab(快速创作[默认]/发现/Studio 广场/历史/我的),宽屏切侧边导航;Tab 外还有搜索、应用详情、创作者主页、社区、登录。

## 2. 已确认的设计决策

| 决策项 | 结论 |
|---|---|
| 视觉方向 | A · 橄榄暗调:完成现有 Rh 设计系统(近黑底 `#050608`、橄榄绿 `#A3B565`、紫辅助 `#8D63FF`),气质为"专业创作工具感"(对标 Midjourney/Runway/剪映) |
| 深浅色策略 | **锁定暗色**:App 始终暗色,不跟随系统深浅色切换 |
| 范围节奏 | 全部 11 页,分批推进,每批可验收 |
| 施工路线 | **样板间先行**:第一批用快速创作页的高保真 mockup 定案全部 token 与核心组件,验收后规范封板,后续页面照规范批量迁移 |
| 每批流程 | mockup 确认 → 实现 → Android 模拟器/真机截图验收;iOS 仅保证编译 |

## 3. 设计语言与暗色锁定

以 `RhDarkColors` 现值为视觉基准。技术动作:

1. `RunningHubTheme`(`composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/Theme.kt`)移除 `isSystemInDarkTheme()` 跟随,固定暗色;Material 组件继续拿到暗色 `colorScheme`(Material 双轨在收尾批拆除)。
2. Android 状态栏/导航栏、启动屏背景、iOS 侧同步锁定暗色外观。
3. `RhLightColors` 保留类型但退出运行路径;样板间批验收后若无回退诉求,收尾批删除。
4. token 值如需微调(样板间 mockup 阶段发现对比度/层次问题),只改 `RhColors.kt` 一处。

## 4. Token 收敛(唯一来源 = RhTheme)

- `RhTheme` 补 `spacing` 入口(`RhSpacing` 已存在但未挂载)。
- 页面统一从 `RhTheme.colors/typography/shapes/spacing` 取值。
- 页面文件中约 56 处硬编码 `Color(0xFF…)` 全部替换为语义 token;`QuickCreateDesign.kt` 在样板间批消化。
- 旧 `ExtendedColors` 引用逐批替换,收尾批删除 `ui/theme` 旧色板。

## 5. 组件体系

- **已有可复用**(以打磨为准):Rh 按钮/徽章/内容卡片(ModelCard、PlazaWorkCard、HistoryTaskCard、AppCard)/底部弹层/状态视图/底栏/参数选择器/计费组件。
- **样板间批新增**:`RhTopBar`、`RhPromptField`、`RhChip`、`RhSegmentedControl`、`RhListItem`、`RhDialog`、`RhSnackbar`。
- **旧组件退役映射**:
  - 视觉类(旧 `AppCard`、`AppSearchBar`、`ErrorState`、`LoadingIndicator`)由 Rh 版本替代;
  - 功能类(`MediaPicker`、`SmartAsyncImage`、`VideoThumbnail`、图片/视频预览浮层)保留逻辑,颜色/间距接入 token。

## 6. 批次计划

每批流程:高保真 mockup 确认 → 实现 → Android 截图验收 → 进入下一批。

| 批次 | 内容 | 备注 |
|---|---|---|
| 批 0 样板间 | 快速创作页 + 主壳(底栏/窗口背景) | mockup 定案全部 token 和核心组件,验收后**规范封板** |
| 批 1 | 任务历史 + 应用详情 | 最大两个文件,顺带按治理阈值拆文件 |
| 批 2 | 发现 + 搜索 | 列表/卡片流,复用批 0-1 组件 |
| 批 3 | 广场 + 创作者主页 + 社区 | 内容流页面 |
| 批 4 收尾 | 我的 + 登录 + 设置 | 之后删除旧 theme/旧组件双轨、全局硬编码清零 |

## 7. 边界、验证与风险

- 只动 Presentation/composeApp UI 层,不碰 Domain/Data/网络/存储。
- 每批验证:`checkArchitectureBoundaries` + 相关模块 commonTest + Android 截图(用户看图验收)。
- iOS 仅保证编译通过;真机视觉不在本次验收范围(无 macOS 证据,按项目规则如实记录)。
- 文案继续走 Compose Resources,业务层不保存展示文案。
- QuickCreate 页面状态所有权在 `feature:quickcreate:presentation`,样板间批只改 UI 渲染层,不动 StateHolder/Coordinator。
- 大文件拆分与视觉改造同批完成,避免同一文件动两次。
- 工作区现存 `feature/kmp-refactoring` 分支上的构建脚本改动为用户既有工作,本方案实施不回滚、不覆盖。
