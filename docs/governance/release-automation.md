# Release Automation Governance

## Android Release

发布前必须完成：

- `:composeApp:assembleRelease` 通过。
- R8/minify 和资源压缩开启。
- ProGuard keep 规则覆盖序列化、Koin、Ktor、Coil 和平台入口。
- 签名配置来自安全本地配置或 CI secret，不进入源码。
- Release 包完成安装和登录/退出/QuickCreate 冒烟。

## iOS TestFlight

TestFlight 前必须完成：

- macOS 上 Xcode Debug/Release 构建通过。
- Info.plist Usage Description 与实际权限一致。
- Keychain、WKWebView、PHPicker 或文档选择器路径完成冒烟。
- 图片、视频和音频上传使用真实 iOS 权限/选择器路径完成回归。
- 短信图形验证码在 Android WebView 和 iOS WKWebView 上完成 token、关闭和失败重试冒烟。
- `PrivacyInfo.xcprivacy` 随 iOS target 打包，并声明 UserDefaults 与文件元数据访问原因。
- 上传 App Store Connect 前由人确认账号、证书、bundle id 和隐私表单。

### iOS 人审证据要求

发布负责人必须在发布说明或验收附件中记录以下证据：

- 照片权限首次允许、首次拒绝、有限照片权限和已拒绝后跳转设置页的结果。
- 图片、视频和音频各一次真实选择、取消选择和上传结果。
- Android WebView 与 iOS WKWebView 的验证码 `validToken` 回传、关闭回调、连续打开和失败重试结果。
- TAC 脚本加载、ATS、Cookie、同源策略和跨域请求的真实环境结论。
- 未完成任一项时，对应风险、负责人和补验时间。

## 人工确认边界

自动化可以生成草稿、报告、补丁和构建产物；商店上传、生产签名、外部发布、敏感配置变更和重大版本依赖升级回归必须人工确认。
Dependabot、Dependency Submission 或人工升级产生的 patch/minor 变更可以按影响范围执行自动化验证；
major 级依赖、Gradle、AGP、Kotlin、Xcode 或运行时 SDK 升级必须在 PR 中列出 Android/iOS 人工回归矩阵、负责人和未覆盖平台。

## 自动门禁

`checkLongTermGovernance` 会校验 `docs/governance/release-readiness-checklist.md`
保留 Android Release、iOS TestFlight、依赖重大版本人工回归和人工确认停止条件。该门禁还会校验 Android release
构建继续启用 R8/minify、资源压缩、项目 ProGuard 规则，并确认 `verifyL1Android` 仍执行
`:composeApp:assembleRelease`。该清单用于发布前人审，不能替代商店账号、证书、生产签名和隐私表单的人工确认。
