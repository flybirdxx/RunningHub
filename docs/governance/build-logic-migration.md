# Build Logic Migration Plan

本文件记录根构建脚本验收逻辑的迁出顺序。复核建议指出 root-build.gradle.kts 中超过千行的
验收逻辑需要迁入 `build-logic` 自定义插件或独立 verification 脚本；当前本地门禁已经禁止
继续向根脚本新增长期治理规则。

## 当前边界

- 新增长期治理规则放入 `LongTermGovernancePlugin`，由 `checkLongTermGovernance` 统一执行。
- `docs/governance/build-script-baseline.txt` 锁定根 `build.gradle.kts` 行数，防止继续增长。
- 根脚本仍暂存既有 L1 Gate 聚合入口，避免一次性大迁移影响当前封板补证。

## 迁出顺序

1. 先迁出 `checkL1CiWorkflows`，因为它主要校验 workflow 文本和脚本入口，副作用最少。
2. 再迁出 `checkMigrationScripts`，同时保留脚本自检和证据文件格式校验。
3. 再迁出 `checkArchitectureBoundaries`，迁移前后必须复跑 Android 与 iOS 本地门禁。
4. 最后迁出 `checkL1SealEvidence`，因为它依赖当前 Git HEAD、暂存区和外部证据文件。

## 执行规则

- 不再新增根脚本验收逻辑；新增检查应进入 `build-logic` 或 `docs/migration/*.ps1`。
- 每迁出一个任务后，下调 `build-script-baseline.txt` 中的 `maxLines`。
- 迁出前后都必须执行 `checkLongTermGovernance checkMigrationScripts checkL1CiWorkflows`。
- `checkArchitectureBoundaries` 和 `checkL1SealEvidence` 迁出时，还必须执行 `verifyL1Android`
  或在 macOS 环境执行 `verifyL1Ios`。
