# RunningHub UI 改进开发计划

> 创建: 2026-05-01 | 基于 UI 框架评估报告

## P0 - 立即修复

### P0-1: 底部导航改用 M3 NavigationBar
- **文件**: `composeApp/.../navigation/MainScreen.kt`
- **内容**: 自定义 Row 替换为 Material 3 `NavigationBar` + `NavigationBarItem`
- **收益**: 原生选中指示器动画、涟漪效果、无障碍语义
- **工作量**: ~1h

### P0-2: 消除硬编码颜色
- **文件**: `MainScreen.kt`, `CollapsibleSection.kt`, `LoginScreen.kt` 等
- **内容**: `Color(0xFF...)` → `MaterialTheme.colorScheme.*`
- **收益**: 亮色/暗色模式自动切换
- **工作量**: ~2h

## P1 - 本周完成

### P1-1: 补全 Icon contentDescription
- **文件**: 所有 Feature Screen 和 Component
- **内容**: `contentDescription = null` → 有意义的描述文本
- **收益**: TalkBack 可用性
- **工作量**: ~1h

### P1-2: ScreenModel 单元测试
- **文件**: 新建 `composeApp/src/commonTest/`
- **内容**: 给 DiscoveryScreenModel、QuickCreateScreenModel 写测试
- **收益**: 回归保护
- **工作量**: ~3h

## 进度

| 编号 | 任务 | 状态 | 完成时间 |
|------|------|------|---------|
| P0-1 | 底部导航改用 M3 NavigationBar | ⬜ 待开始 | - |
| P0-2 | 消除硬编码颜色 | ⬜ 待开始 | - |
| P1-1 | 补全 Icon contentDescription | ⬜ 待开始 | - |
| P1-2 | ScreenModel 单元测试 | ⬜ 待开始 | - |
