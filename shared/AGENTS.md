# Shared Module Instructions

## Responsibility

本模块包含跨平台 Domain、Data、网络、存储和数据库能力。

## Boundaries

- Domain 不依赖 Data、Ktor、SQLDelight、DataStore 或平台 SDK。
- Repository interface 放在 `domain/repository`。
- Repository implementation 放在 `data/repository`。
- DTO 放在 `data/remote/dto`。
- DTO、数据库对象和 Domain model 之间必须通过显式 mapper 转换。
- API endpoint、header 和序列化细节不得出现在 Domain。
- Android/iOS 差异放在对应 source set。
- 优先测试 Repository、mapper、认证和业务规则。
- 修改 API DTO 后必须验证反序列化和映射。
- 不在共享层产生依赖具体 UI 的最终错误文案。

## 中文注释要求

修改 `shared` 模块时，必须特别为以下内容补充中文 KDoc 或中文块注释：

- Repository 接口的业务语义、调用前提和错误返回约定。
- Repository 实现中的远程接口调用、缓存策略和异常映射。
- DTO、Entity 与 Domain model 的字段差异和兼容逻辑。
- Token 刷新、认证头注入、会话失效和敏感信息脱敏。
- `expect` / `actual` 平台抽象的职责和平台差异。
- DataStore、SQLDelight 或文件存储的读写时机和一致性策略。

注释必须解释业务原因和维护约束，不得只复述代码。
