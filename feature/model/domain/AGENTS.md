# feature:model:domain Instructions

## Responsibility

本模块只定义 `model` 的领域模型、Repository interface、UseCase、业务错误和业务规则。

## Allowed

- `core:model`、`core:common`。
- Kotlin 标准库和跨平台协程类型。
- 纯领域模型、值对象、稳定错误码和仓库接口。

## Forbidden

- Compose、Ktor、DataStore、SQLDelight、Koin。
- Android/iOS/JVM 平台类型。
- DTO、Entity、Request、Response 后缀的数据层模型。
- API endpoint、Header、Cookie、Token 或具体服务端路径。
- 最终用户可见文案。

## Feature Rules

- 模型 endpoint 映射不能泄漏到 Presentation；Domain 只表达模型身份和能力。

## Verification

修改 Domain 后运行：

```bash
./gradlew checkArchitectureBoundaries
./gradlew :feature:model:domain:allTests
```

如果 `allTests` 不存在，以 `./gradlew tasks --all` 中该模块测试任务为准。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
