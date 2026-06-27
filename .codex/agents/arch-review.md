# arch-review Agent

用途：当一次任务修改 3 个及以上 Gradle 模块、改动 `settings.gradle.kts` / `build.gradle.kts` / `build-logic`，或改变 Domain/Data/Presentation 边界时触发。

## 审查输入

- `git diff --name-only`
- `settings.gradle.kts`
- 根 `build.gradle.kts` 中的 `checkArchitectureBoundaries`
- 受影响模块的 `build.gradle.kts`
- `.codex/references/dependencies.md`

## 模块依赖检查

- [ ] `:shared` 未重新出现在 settings、Gradle 依赖、源码导入或 Git 跟踪文件中。
- [ ] `composeApp/commonMain` 未直接依赖 `feature:*:data` 或 `core:network` 以外的平台实现。
- [ ] `composeApp/androidMain` / `iosMain` 才装配运行期 Data 模块。
- [ ] `feature:*:domain` 未导入 Ktor、Koin、DataStore、Compose、Android/iOS 平台 API 或 Data 实现。
- [ ] `feature:*:presentation` 未依赖 Feature Data 模块。
- [ ] `core:*` 未依赖 Feature 或 `composeApp`。
- [ ] 跨 Feature 依赖通过 Domain 契约或应用壳协调，而不是实现类互调。

## 禁止模式搜索

使用 `rg` 或 CodeGraph 检查：

- `projects.shared|project\(":shared"\)|com\.runninghub\.shared`
- `runBlocking\(|GlobalScope|Thread\.sleep\(`
- `catch\s*\([^)]*\)\s*\{\s*\}`
- `CreateVoyagerScreen|CreateScreenModel`
- `SessionManager\(\s*\)`
- `Regex\(` 在 `TokenRefresher.kt` 中
- `Authorization|Cookie|apiKey|validToken` 与日志/文档输出同现

## 必跑验证

- 依赖或 source set 改动：`.\gradlew.bat --console=plain checkArchitectureBoundaries`
- 长期治理相关改动：`.\gradlew.bat --console=plain checkLongTermGovernance`
- 发布或 CI 路径改动：对应运行 `verifyL1Android` 或在 macOS 上运行 `verifyL1Ios`
