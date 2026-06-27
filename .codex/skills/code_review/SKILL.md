---
name: code_review
description: Review RunningHub code changes for architecture, KMP boundaries, security, tests, and release risks.
---

# RunningHub 代码审查 Skill

以代码审查姿态输出：先列问题，按严重程度排序，包含文件/行号；最后给简短总结和测试缺口。

## 致命问题（必须修复）

- [ ] 重新引入 `:shared`、`projects.shared`、`com.runninghub.shared.*`。
- [ ] Domain 或 Presentation 依赖 Feature Data 实现。
- [ ] `composeApp/commonMain` 直接依赖 Data 模块、平台 API 或平台实现。
- [ ] 生产源码使用 `runBlocking`、`GlobalScope`、空 `catch` 或不可取消轮询。
- [ ] Token、Cookie、Authorization、API Key、验证码 token、请求体进入日志、文档或 UI。
- [ ] Release 关闭 R8/资源压缩、移除 ProGuard keep 规则或泄露签名配置。
- [ ] 恢复旧 `CreateVoyagerScreen` / `CreateScreenModel`。
- [ ] 声称远端 CI、iOS link、商店上传或真机验证完成但没有证据。

## 警告（建议修复）

- [ ] UI 文案硬编码在 Compose 页面，未进入 Compose Resources。
- [ ] 新增十六进制颜色未进入主题 token 或语义色。
- [ ] Data API 没有 Ktor MockEngine 契约测试。
- [ ] Repository 把远端 message 透传给 Presentation。
- [ ] `ScreenModel` 承载过多业务状态，未下沉到 Feature Presentation。
- [ ] 大文件继续膨胀，未遵守治理 allowlist 或拆分计划。
- [ ] iOS/Android 平台差异缺少 expect/actual 边界。

## 建议（可选优化）

- [ ] 使用 CodeGraph 检查调用方和影响半径。
- [ ] 为新增错误语义补资源文案映射测试。
- [ ] 为长轮询、上传、生成任务补取消路径测试。
- [ ] 同步 `.codex/references/` 模块文档和依赖图。

## 推荐审查命令

```powershell
git diff --name-only
.\gradlew.bat --console=plain checkArchitectureBoundaries
.\gradlew.bat --console=plain checkLongTermGovernance
```

若审查对象只是 `.codex` 或文档初始化，可改为占位符扫描和 references 数量检查。
