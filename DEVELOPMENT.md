# RunningHub Development Guide

本文档用于日常开发和代码审查。迁移历史只在需要追溯 Gate/AC 证据时读取。

## 常用验证

```bash
./gradlew projects
./gradlew checkArchitectureBoundaries
./gradlew checkLongTermGovernance
./gradlew verifyL1Android
./gradlew verifyL1Ios
```

普通 commonMain 或架构边界变更至少执行：

```bash
./gradlew checkArchitectureBoundaries checkLongTermGovernance
```

涉及 Android 发布行为、R8、资源压缩或 Android 平台代码时执行：

```bash
./gradlew verifyL1Android
```

涉及 iOS、expect/actual、平台权限、Keychain、WKWebView 或 Xcode 包装工程时，在 macOS
或 GitHub macOS runner 上执行：

```bash
./gradlew verifyL1Ios
```

## PR 交付信息

启动 AI 协作任务时，优先使用 `docs/governance/ai-task-template.md` 明确任务 ID、唯一目标、
允许/禁止修改目录、验收命令、最大重试次数和完成后更新的文档。

PR 必须说明：

- 变更目标、影响模块和影响平台。
- 架构边界是否变化。
- 实际执行命令和测试结果。
- 未验证项、原因和剩余风险，不得把“未执行”写成“通过”。
- 是否涉及 API、数据库、配置、发布、隐私或安全变化。
- 回滚方案。

## 长期治理口径

`checkLongTermGovernance` 不采集远端 CI 证据，也不替代 GitHub Branch Protection。
它只保证长期治理入口留在仓库中，并防止 `AGENTS.md` 重新膨胀为迁移历史合集或
`composeApp` 文件规模继续失控。
该门禁还会约束无 Presentation 模块的大体量 UI Feature 不得超过登记基线，并拦截生产代码中
明显包含 Token、Cookie、API Key 或密码语义的日志语句。
认证相关变更还会检查 `core:network` 是否保留精确 Host 白名单；不得把
`evilrunninghub.cn`、`runninghub.cn.example.com` 这类相似域名误判为可携带认证头的站内请求。
环境配置变更会检查 Android `RUNNINGHUB_*` BuildConfig 字段和 iOS `RUNNINGHUB_*` 环境变量入口；
没有真实 staging/dev 地址时可以回退 production，但不能删除平台启动层覆盖能力。
凭据存储相关变更会检查 Android Keystore、iOS Keychain、`MigratingCredentialStore` 和双端
runtime module 绑定；`CredentialStore` 不得直接退回 `PreferencesSettingsStore`，旧凭据只能按
字段迁移，不能误删草稿和余额缓存。
iOS 平台能力变更还会检查权限和媒体选择是否保留真实 PhotoKit/系统选择器实现、Info.plist
权限说明和安全作用域 URL 读取；该检查不能替代 Simulator 或真机上传回归。
短信图形验证码变更会检查 Android WebView、iOS WKWebView、TAC HTML 和自定义 scheme 回调契约；
真实 TAC JS/CSS 加载、Cookie/ATS 行为和双端视觉交互仍需运行验证。
