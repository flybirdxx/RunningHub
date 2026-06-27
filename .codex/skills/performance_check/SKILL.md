---
name: performance_check
description: Check RunningHub performance, memory, security, and release-readiness risks for Android/iOS KMP flows.
---

# RunningHub 性能与安全检查 Skill

## 触发场景

- 修改启动流程、DI、网络环境、图片/视频/音频加载、长轮询、上传、缓存或发布配置。
- 修改 QuickCreate、History、Plaza、Model Catalog、Auth 等高频路径。
- 引入新依赖、后台任务、媒体资源或平台权限。

## 检查项

- 启动：Koin eager binding、预加载、首屏网络请求、平台环境配置是否阻塞。
- 内存：图片/视频缩略图、Coil cache、Media3、上传字节数组、长列表状态是否可释放。
- 长轮询：任务查询必须可取消，页面销毁后不能继续请求。
- 网络：Ktor client 复用、超时、token refresh、错误映射和敏感日志脱敏。
- UI：Compose 重组热点、巨型 Composable、硬编码尺寸和不可滚动内容。
- 发布：R8、资源压缩、ProGuard、依赖版本、签名与商店上传人工边界。
- iOS：framework link、权限声明、Keychain、WKWebView 验证码和文件选择器。

## 推荐验证

- Android 本地：`.\gradlew.bat --console=plain verifyL1Android`。
- 架构快速：`.\gradlew.bat --console=plain checkArchitectureBoundaries`。
- iOS：macOS 上 `./gradlew --console=plain verifyL1Ios`。
- 运行期性能：记录设备、构建类型、操作路径、采样窗口和证据文件；没有真实采样时不得声称通过。

## 输出格式

1. 风险结论。
2. 证据：命令、截图、日志摘要或代码位置。
3. 未验证项和补验条件。
4. 最小修复建议。
