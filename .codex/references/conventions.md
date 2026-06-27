# RunningHub 编码约定

生成时间：2026-06-27

## 架构约定

- Feature 按 `domain`、`data`、`presentation` 拆分。
- Domain 保存稳定模型、错误语义和 Repository 接口。
- Data 保存 Ktor API、DTO、Mapper、RepositoryImpl 和 Koin data module。
- Presentation 保存 `StateHolder`、`UiState`、`Coordinator`、Interactor 和用户可见语义。
- `composeApp` 保存 Compose UI、Voyager Screen、ScreenModel facade、平台 runtime module 和平台 expect/actual。

## 命名约定

- 模块：`core:{name}`、`feature:{area}:domain|data|presentation`。
- 包名：`com.runninghub.{core|feature|app}...`，与模块层级对齐。
- API：`{Area}Api`。
- DTO：`{Area}{Purpose}Dto` 或 `{Purpose}RequestDto` / `{Purpose}ResponseDto`。
- Repository 接口：`{Area}Repository` 或具体能力名。
- Repository 实现：`{RepositoryName}Impl`。
- Presentation 状态：`{Area}StateHolder`、`{Area}UiState`、`{Area}Coordinator`。
- Voyager ScreenModel：`{Screen}ScreenModel`，只做应用壳 facade。
- 测试：`{被测类}Test`。

## Kotlin/KMP 约定

- `commonMain` 不导入 Android/iOS 平台 API。
- 平台差异使用 `expect` / `actual` 或 `androidMain` / `iosMain` 装配。
- 生产代码使用结构化协程；禁止 `runBlocking`、`GlobalScope` 和空 `catch`。
- 公共 API 和复杂业务边界保留中文 KDoc，具体规则参考 `docs/governance/chinese-commenting.md`。

## Compose/UI 约定

- UI 文案优先进入 `composeApp/src/commonMain/composeResources/values/strings.xml`。
- 颜色、尺寸、排版优先通过主题 token 或局部语义变量表达。
- 页面级复杂状态下沉到 Feature Presentation。
- Compose 页面只做状态收集、事件转发和可视结构。

## 数据与安全约定

- API 层固定 endpoint 和远端字段，Repository 负责映射和错误归一。
- Token、Cookie、API Key、验证码 token 和请求体不得写入日志、截图说明或错误文案。
- 敏感凭据走 Keystore/Keychain，非敏感设置走 DataStore。
- token refresh JSON 用 DTO 解析，不用正则。

## 验证约定

- 依赖或 source set 变更：`checkArchitectureBoundaries`。
- 长期治理文档或发布配置变更：`checkLongTermGovernance`。
- Android 交付路径：`verifyL1Android`。
- iOS 交付路径：macOS `verifyL1Ios`。
- 文档-only：占位符扫描 + references 模块文档数量检查。
