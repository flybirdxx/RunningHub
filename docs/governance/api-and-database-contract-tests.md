# API and Database Contract Test Governance

## API 契约测试

修改 API DTO、Repository mapper 或错误映射时，必须覆盖：

- DTO 反序列化，包括空字段、兼容字段和异常服务端数据。
- DTO-to-Domain 映射，确保 Presentation 不接触 DTO。
- 认证失效、Token refresh 和业务错误码映射。
- 分页、去重、空数据和重试语义。

## SQLDelight migration 测试

新增或修改 SQLDelight schema、`.sq` 查询或 migration 时，必须覆盖：

- 旧 schema 到新 schema 的 migration。
- 索引、唯一约束和外键约束。
- 空表、有数据表和异常历史数据。
- Android 与 iOS driver 差异。

当前仓库的 SQLDelight 仍集中在 `shared`，迁出前仍按本规范补测试。

## 自动门禁

`checkLongTermGovernance` 会读取：

- `docs/governance/api-contract-test-baseline.txt`
- `docs/governance/database-contract-test-baseline.txt`

所有生产 `commonMain` 中的 `*Dto.kt`、`*Request.kt`、`*Response.kt` 必须登记对应契约测试入口；
所有 `.sq` / `.sqm` 文件必须登记数据库契约测试状态。迁移期既有 SQLDelight schema 可以用
`legacy-debt` 标明缺口，但该例外只允许当前已登记的 `DiscoveryCache.sq`；新增 schema 不得使用该标记。
