# Build Logic Instructions

## Scope

适用于 `build-logic/` included build。

## Responsibility

本目录只维护 Gradle convention plugin、统一 Android/KMP/Compose 配置和仓库级验证任务。
不得放入业务代码、Feature 逻辑、UI 逻辑或运行期依赖。

## Rules

- 插件版本以根工程 `gradle/libs.versions.toml` 为唯一事实来源。
- JVM target、Android SDK、KMP target、Compose 编译器配置必须在 convention plugin 中集中维护。
- 新增验证任务时优先写成可复用 task 或 plugin，不继续膨胀根 `build.gradle.kts`。
- 验证任务必须失败即终止，不能只打印 warning。
- 不得在 build logic 中读取 Token、Cookie、API Key、local.properties 或用户私有路径。
- 不得为单个 Feature 写硬编码业务例外；必要例外应放入受版本控制的 allowlist 并注明删除条件。

## Verification

修改本目录后至少执行：

```bash
./gradlew projects
./gradlew checkArchitectureBoundaries
./gradlew checkLongTermGovernance
```

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
