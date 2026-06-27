# resource-sync Agent

用途：当任务修改 Compose Resources、Android `res/`、用户可见文案、主题 token、权限/隐私配置或发布资源时触发。

## 检查范围

- `composeApp/src/commonMain/composeResources/values/strings.xml`
- `composeApp/src/androidMain/res/**`
- `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/**`
- `composeApp/src/androidMain/AndroidManifest.xml`
- `iosApp/iosApp/Info.plist`
- `iosApp/iosApp/PrivacyInfo.xcprivacy`
- `docs/governance/ui-copy-resources.md`
- `docs/governance/ui-copy-hardcoded-baseline.txt`

## 检查清单

- [ ] UI 文案优先进入 Compose Resources，避免在 Domain/Data 层保存最终展示文案。
- [ ] Android/iOS 权限说明与真实功能一致，隐私声明同步更新。
- [ ] 新增颜色应进入主题 token 或局部语义色，不扩大硬编码十六进制颜色存量。
- [ ] `strings.xml` 不包含调试 token、真实用户信息、请求体或认证头。
- [ ] QuickCreate、History、Auth、Task 等错误文案使用稳定语义映射，不透传远端原始 message。
- [ ] Android launcher、theme、ProGuard 或 packaging 资源变更后至少运行相关 Gradle task。

## 推荐验证

- 文案/主题：运行相关 `commonTest`，并做截图或人工渲染检查。
- Android 资源：`.\gradlew.bat --console=plain :composeApp:lintDebug`。
- 发布资源：`.\gradlew.bat --console=plain :composeApp:assembleRelease` 或纳入 `verifyL1Android`。
