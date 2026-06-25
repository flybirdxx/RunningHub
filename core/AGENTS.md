# Core Modules Instructions

## Scope

适用于 `core/` 下所有模块。

## Responsibility

Core 只承载跨 Feature 复用且语义稳定的基础能力。新增 Core 代码必须满足：
被多个 Feature 复用、不是具体业务流程、可独立测试。

## Boundaries

- Core 不依赖任何 `feature:*` 模块。
- Core 不依赖 `composeApp`。
- Core 不依赖历史 `shared`。
- Core 不保存用户可见业务文案，除非该文案属于跨功能基础错误或日志分类。
- 平台实现必须放在 `androidMain` 或 `iosMain`。
- commonMain 不得出现 Android/iOS/JVM 专属类型。

## Verification

修改任意 Core 模块后至少执行：

```bash
./gradlew checkArchitectureBoundaries
./gradlew checkLongTermGovernance
./gradlew :core:model:allTests
```

如果目标任务不存在，以 `./gradlew tasks --all` 输出为准，执行受影响模块的 common/JVM/iOS/Android 测试。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
