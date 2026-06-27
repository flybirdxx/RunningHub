---
name: plan_mode
description: Plan RunningHub development tasks using the current Kotlin Multiplatform architecture and repository gates.
---

# RunningHub 任务规划 Skill

## 入口检查

1. 读取根 `AGENTS.md`、`.codex/rules/project_rule.md` 和目标目录最近的局部 `AGENTS.md`。
2. 运行或查看 `git status --short`，标记已有用户改动。
3. 用 CodeGraph 或 `.codex/references/` 定位受影响模块。
4. 明确成功标准、允许修改目录、禁止修改目录和 verifier。

## 高频任务模板

### 新增 Feature Domain 能力

1. 在 `feature/{area}/domain` 定义稳定模型和 Repository 接口。
2. 在 `feature/{area}/data` 实现接口，DTO 保持在 Data 层。
3. 在 Data module 注册 Koin binding。
4. 在平台 runtime module 或应用壳组合根装配依赖。
5. 补充 Data 契约测试和 Presentation 状态测试。

### 新增 Compose 页面

1. 页面 UI 放在 `composeApp/commonMain`，复杂状态放入对应 Feature Presentation。
2. Voyager `ScreenModel` 仅持有 facade 和生命周期，不聚合业务仓库。
3. 用户可见文案优先进入 Compose Resources。
4. 平台能力通过 expect/actual 或平台 source set 注入。
5. 验证 UI 渲染、状态流和导航回退。

### 修改 API/DTO

1. 在 Data `remote/api` 固定 endpoint、method、header 和请求体契约。
2. DTO 放在 Data 层，Repository 负责映射为 Domain 模型。
3. 非 0 code、空 body、认证失效和取消流程映射为稳定错误语义。
4. 使用 Ktor MockEngine 测试路径、请求体和响应兼容。
5. 禁止把远端原始 message 直接传到 UI。

### 修改架构边界

1. 更新目标模块 `build.gradle.kts`。
2. 检查 `commonMain` 是否仍平台无关。
3. 更新 `.codex/references/dependencies.md` 和相关模块文档。
4. 运行 `checkArchitectureBoundaries`。
5. 若触及治理文档，运行 `checkLongTermGovernance`。

## 默认验证建议

- 文档-only：占位符扫描 + references 文档数量检查。
- 单模块 Kotlin：相关模块 test task。
- 依赖/source set：`.\gradlew.bat --console=plain checkArchitectureBoundaries`。
- Android 交付路径：`.\gradlew.bat --console=plain verifyL1Android`。
- iOS 交付路径：macOS 上 `./gradlew --console=plain verifyL1Ios`。
