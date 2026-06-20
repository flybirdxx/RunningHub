# Compose Application Instructions

## Responsibility

本模块包含 Compose Multiplatform UI、导航、应用状态、
ScreenModel 和平台 UI 适配。

## Boundaries

- UI 只依赖 Domain model、Repository interface 或 UseCase。
- Composable 不直接访问网络、数据库或 DataStore。
- ScreenModel 不直接依赖 API 或 DataSource。
- 页面状态使用不可变 `UiState` 和只读 `StateFlow`。
- 用户行为通过明确 Action 或回调进入 ScreenModel。
- 平台类型不得进入 `commonMain` UiState。
- 公共视觉值应进入主题或 Design Token，不在页面内重复硬编码。
- 一个页面文件不要同时承担导航、业务编排、媒体处理和持久化。
- 修改 QuickCreate 时优先拆分职责，不继续扩大现有 ScreenModel。
- UI 修改至少执行 `./gradlew :composeApp:assembleDebug`。

## ScreenModel 注释要求

修改 ScreenModel 时，必须为以下内容添加详细中文注释：

- UiState 每组字段所代表的业务状态。
- Job 的用途、启动条件和取消条件。
- 初始化流程及各请求之间的先后关系。
- 页面 Action 引起的状态转换。
- 防重复提交、防抖和并发覆盖策略。
- 轮询的启动、停止、超时和资源释放条件。
- 错误状态如何映射为用户可见提示。

对于超过 100 行的 ScreenModel，不允许只写一个类级注释；
必须在每个业务分区和关键状态转换附近增加中文说明。

## Compose 注释要求

Composable 不需要给每一行 UI 布局写注释，但下列场景必须解释：

- 非显而易见的自适应布局。
- 与平台差异有关的 UI 行为。
- 复杂动画、过渡、滚动定位或状态恢复。
- 为避免重复请求、重复导航或重组副作用而写的 `LaunchedEffect`。
- 与无障碍、输入法、权限、文件选择或媒体播放相关的特殊处理。
