# Release Readiness Checklist

本清单用于生成发布前的人审材料。它不是自动发布脚本，生产签名、商店上传和敏感配置变更不得自动执行。

## Android Release

- [ ] `:composeApp:assembleRelease` 已通过
- [ ] R8/minify 和资源压缩保持开启
- [ ] ProGuard keep 规则覆盖序列化、Koin、Ktor、Coil 和平台入口
- [ ] Android Release 安装回归已完成
- [ ] 登录、退出、QuickCreate 和媒体上传冒烟已完成
- [ ] 签名配置来自安全本地配置或 CI secret，未进入源码

## iOS TestFlight

- [ ] macOS Xcode 构建已通过
- [ ] Info.plist Usage Description 与实际权限一致
- [ ] Keychain、WKWebView、PHPicker 或文档选择器路径已冒烟
- [ ] 图片、视频和音频上传使用真实 iOS 权限/选择器路径回归通过
- [ ] 短信图形验证码在 Android WebView 和 iOS WKWebView 上完成 token/关闭/失败重试冒烟
- [ ] iOS TestFlight 上传前人工确认账号、证书和隐私表单
- [ ] bundle id、版本号、构建号和隐私清单由负责人确认
- [ ] `PrivacyInfo.xcprivacy` 已随 iOS target 打包，并声明 UserDefaults 与文件元数据访问原因

### iOS 权限与媒体回归矩阵

- [ ] 首次请求照片权限：允许、拒绝、有限照片权限三种路径均记录结果。
- [ ] 已拒绝照片权限：再次选择图片或视频时不进入成功回调，并能跳转系统设置页。
- [ ] 图片选择：系统选择器返回的 URI 可被上传链路读取，取消选择不会污染页面状态。
- [ ] 视频选择：系统选择器返回的 URI 可被上传链路读取，取消选择不会污染页面状态。
- [ ] 音频选择：文档选择器导入的文件可读取字节和文件名，取消选择不会污染页面状态。
- [ ] 上传回归：图片、视频、音频各完成一次真实上传，失败路径不会继续提交生成任务。

### 验证码 Web 容器回归矩阵

- [ ] Android WebView 和 iOS WKWebView 均能加载 TAC JS/CSS。
- [ ] 成功验证后能回传 `validToken`，且 token 不进入日志、崩溃报告或截图说明。
- [ ] 用户关闭弹窗时能触发关闭回调，不留下旧 handler 或旧 token。
- [ ] 连续打开两次验证码时，第二次不会收到第一次的回调。
- [ ] 网络失败、脚本失败和超时均展示可重试降级状态。
- [ ] ATS、Cookie、同源策略和跨域请求行为已在真实环境记录结论。

## 依赖升级与重大版本回归

- [ ] Dependabot、Dependency Submission 或人工升级产生的依赖变更已在 PR 中列出影响模块。
- [ ] patch/minor 级依赖升级已执行受影响模块验证命令，并记录未覆盖平台。
- [ ] major 级依赖、Gradle、AGP、Kotlin、Xcode 或运行时 SDK 升级已列出 Android/iOS 人工回归矩阵和负责人。
- [ ] 重大版本升级不得自动合并，必须由负责人确认双端回归结果后再发布。

## 停止条件

- [ ] 生产签名、商店上传和敏感配置变更不得自动执行
- [ ] 任一平台未完成运行验证时，发布说明必须列出未验证项和剩余风险
