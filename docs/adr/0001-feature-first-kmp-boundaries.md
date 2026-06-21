# ADR 0001: Feature-first KMP Boundaries

日期：2026-06-21

## 决策

RunningHub 采用 Feature-first 组织方式，并保持 `Presentation -> Domain <- Data`
依赖方向。Domain 定义业务模型和 Repository interface；Data 实现 Repository 并完成
DTO、Entity 与 Domain Model 映射；Presentation 只通过 Domain 契约驱动 UI 状态。

## 原因

迁移前的 `shared` 与 `composeApp` 承担了过多职责，导致 UI、网络、存储和平台能力互相泄漏。
Feature-first 能让业务边界与 Gradle 模块边界对齐，便于测试、替换数据实现和双端验证。

## 约束

- Domain 不依赖 Compose、Ktor、SQLDelight、DataStore、Android SDK 或 iOS SDK。
- Data 不依赖 `composeApp` 或 Presentation 类型。
- Presentation 不直接调用 API、DataSource、DataStore、Keychain 或 Keystore。
- 新业务不得继续扩大 `shared`。

## 后果

简单 Repository 转发不强制创建 UseCase；跨 Repository、计费、轮询、上传、会话和任务编排逻辑
必须进入 UseCase、Interactor、Coordinator 或 StateHolder，并配套测试。
