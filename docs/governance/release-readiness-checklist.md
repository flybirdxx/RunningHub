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

- [x] macOS Xcode 构建已通过
- [x] Info.plist Usage Description 与实际权限一致
- [x] Keychain、WKWebView、PHPicker 或文档选择器路径已冒烟
- [ ] 图片、视频和音频上传使用真实 iOS 权限/选择器路径回归通过
- [x] iOS WKWebView 短信图形验证码已完成 token、短信发送、SMS 登录、关闭和失败重试冒烟
- [ ] iOS TestFlight 上传前人工确认账号、证书和隐私表单
- [ ] bundle id、版本号、构建号和隐私清单由负责人确认
- [x] `PrivacyInfo.xcprivacy` 已随 iOS target 打包，并声明 UserDefaults 与文件元数据访问原因

### 当前 iOS TestFlight 人审材料

截至 2026-07-07，当前 iOS 补全提交已通过 `./gradlew --console=plain verifyL1Ios`，并已按当前 Git `HEAD` 重采 macOS iOS link/Simulator evidence；TestFlight 仍需要真实签名、上传和 App Store Connect 证据，L1 封板仍需要推送后刷新 Android/iOS GitHub Actions 外部证据。

| 项目 | 当前证据 | 发布前状态 |
|---|---|---|
| Xcode 构建 | XcodeBuildMCP simulator build 已于 2026-07-07 通过；最新 signed Simulator `build_run_sim` 进程 `22624` 已启动到已登录的发现页，启动崩溃关键词扫描通过；构建日志为 `/Users/yu/Library/Developer/XcodeBuildMCP/workspaces/RunningHub-b34ac473094f/logs/build_run_sim_2026-07-07T03-40-44-748Z_pid88023_a201ea79.log`。同日已有 Release iphoneos archive dry run 历史通过记录：`xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' archive -archivePath /tmp/RunningHub-iOS-Release.xcarchive CODE_SIGNING_ALLOWED=NO` 通过，Xcode store validation 阶段通过，archive 内 `RunningHub.app` 为 arm64 Mach-O，`PrivacyInfo.xcprivacy` 和处理后的 `Info.plist` 已打包；`ComposeApp.framework` 为 static framework，已静态链接进主二进制，app 内无独立 `Frameworks/ComposeApp.framework` 属于当前链接形态。视频显示修复后，当前工作区已复跑 `:composeApp:linkReleaseFrameworkIosArm64` 和 Release iphoneos `xcodebuild ... build CODE_SIGNING_ALLOWED=NO`，两条均通过；一次完整 archive 复跑因耗时被人工中断，不作为通过证据 | 需在最终提交后复跑；TestFlight 仍需真实签名、上传和 App Store Connect 证据 |
| Info.plist 权限说明 | `checkLongTermGovernance` 已锁定 Photo Library 读取、相册写入、Camera、Microphone 四类 Usage Description，并锁定 `PHPhotoLibraryPreventAutomaticLimitedAccessAlert=true`，确保 Limited Photos 由应用内恢复入口手动管理；当前 iOS 代码实际触达 PHPicker/PhotoKit 保存、AVFoundation 相机授权和音频素材入口 | 需负责人确认文案符合 App Store 审核口径 |
| PrivacyInfo.xcprivacy | `checkLongTermGovernance` 已锁定 `NSPrivacyAccessedAPICategoryUserDefaults`、`NSPrivacyAccessedAPICategoryFileTimestamp` 和 Xcode Resources 引用；Release iphoneos archive dry run 已确认 `PrivacyInfo.xcprivacy` 被复制进 `RunningHub.app`，当前声明 `NSPrivacyCollectedDataTypes=[]`、`NSPrivacyTracking=false`、`NSPrivacyTrackingDomains=[]` | 需负责人确认 App Store Connect 隐私表单与实际收集数据一致 |
| Keychain | signed Simulator 密码登录、重启会话恢复、退出登录回 Login 已验证；2026-07-07 已再次退出登录并使用测试账号完成密码登录，回到账户页，凭据未写入文档；当前 evidence 的 `authenticatedSmokeBuildMode=signed-simulator-or-device` | 真机/TestFlight 仍需补验 |
| WKWebView 验证码 | iOS WKWebView 可加载 TAC 资源并渲染滑块；Android/iOS Web 容器的 base URL 均跟随 `RunningHubApiEnvironment.WEB_BASE_URL`，HTML 内 `/tac/js`、`/tac/css` 和 `/uc` 相对路径保持同源；关闭后旧 token 不会触发短信重试，Android/iOS Web 容器 dispose 后会停用旧回调；HTML 包装层已覆盖网络/script 失败、图片解码失败和超时的可重试降级状态，且不会暴露底层 URL 或平台诊断文本；静态门禁已阻止验证码 token 或原始 TAC 响应进入 Web console/native log；2026-07-07 已在 iPhone 17 Pro signed Simulator 上人工完成 TAC 滑块，短信验证码发送成功，随后完成 SMS 登录并进入账号页，runtime/os log 关键字扫描未命中 `validToken`、原始 TAC 响应、token、Cookie、手机号、验证码、远端 URL 或平台异常原文 | Android 真实 WebView、真机/TestFlight 仍需发布前专项复核 |
| 图片选择/上传 | QuickCreate 图片选择、上传 200、生成、历史详情已验证；AppDetail 图片选择器、本地预览、提交状态链和结果 UI 已验证；AppDetail 上传 API 已把 MIME 同步写入 multipart file part 的 `Content-Type`，避免仅依赖普通 `fileType` 字段导致服务端或网关识别不稳定；`PermissionControllerContractTest` 覆盖图片/视频/音频 picker 取消不会进入权限拒绝或永久拒绝，`checkLongTermGovernance` 同步守住 iOS `PhotoPickerDelegate` 空结果、类型不匹配和空 URI 的取消分支 | AppDetail 远端上传 200 明细仍需补验 |
| 视频选择与显示 | QuickCreate 视频 picker 入口与本地素材引用已验证；图片/视频选择已迁到 `PHPickerViewController`，不再为普通素材选择预先申请整库 PhotoKit 权限，且 `checkLongTermGovernance` 会阻止 `presentPhotoPicker` 重新调用 PhotoKit 授权预检；`IosPickedMediaCopyTest` 覆盖图片和视频 picker 临时文件复制到 app-owned 临时 URI 后仍可读取；QuickCreate 上传会保留 picker 返回的 `.mov/.webm/.mp4` 扩展名并提交匹配 MIME，避免 iOS 视频被统一伪装成 mp4；iOS 权限恢复入口已在 PhotoKit Limited 状态下路由到 `presentLimitedLibraryPicker`，其他权限仍走 App Settings；2026-07-07 signed Simulator 已打开系统 Limited Photos 管理页并完成返回，页面未新增素材或错误；Seedance2.0 已按官方接口族白名单展示和提交参数，`conversionSlots` 作为真人素材槽位由客户端忽略，`realPersonMode` 作为真人开关保留；`duration` 已按文档补齐 4-15，并过滤 `imageUrls/videoUrls/audioUrls` 占位默认值；最新 signed Simulator smoke 已确认视频入口显示过滤后的 `10 个参数`，参数面板首屏不再展示 `conversionSlots`；视频 PHPicker 取消后回到页面且不新增素材或错误提示；2026-07-07 已修复视频结果显示路径，历史详情、QuickCreate 对话结果、QuickCreate 历史详情和 AppDetail 任务结果不再把视频原地址当图片解码，签名视频 URL 和无独立封面的视频由自动化测试覆盖 | 视频远端上传 200 和扣费级生成提交仍需补验 |
| 音频选择 | QuickCreate 参数面板音频 Document Picker 入口和取消后回到参数面板已验证；取消不会再误弹音频权限说明；iOS file URL 读取由 `IosMediaResolverTest` 覆盖；QuickCreate 上传会保留 `.m4a/.wav/.aac/.ogg/.mp3` 扩展名并提交匹配 MIME，避免音频被统一伪装成 mp3 | 真实文档选择器文件导入和音频远端上传 200 仍需补验；Simulator Files 注入音频已尝试但索引/打开行为不稳定，不作为当前阻塞项 |
| 上传失败路径 | QuickCreate 失败素材会在生成前由 `awaitPendingUploads` 阻断，且上传文件名使用通用前缀加真实扩展名，不把用户本地文件名暴露给服务端；AppDetail 上传中或上传失败时会阻断 `runTask`，不提交旧远端文件名、本地 URI 或空素材参数；AppDetail 已覆盖 `.m4a -> audio/mp4`、`.mov -> video/quicktime` 从平台 URI 回调到上传仓库的 MIME 传递，且上传 multipart file part 已带 MIME `Content-Type` | 图片、视频、音频三类真实上传 200 仍需补齐运行证据 |
| 图片/视频保存 | PhotoKit 图片保存成功与详情保存状态回写已在 signed Simulator 验证；图片/视频下载失败、写权限拒绝、写入失败已有 `IosMediaSaverTest` 覆盖；2026-07-07 signed Simulator 已真实触发照片权限弹窗并选择“不允许”，页面展示稳定失败提示和权限恢复引导，日志关键字扫描未命中远端 URL、敏感 token 或平台异常原文；历史详情视频输出保存分发已有 `TaskHistoryMediaSaveActionTest` 覆盖 | 真实远端视频保存仍需专项补验 |
| Camera/Notifications | iOS 平台授权实现已编译，授权状态映射已有 `IosPermissionAuthorizationMappingTest` 覆盖；PhotoKit 允许、拒绝和 Limited 三种结果写入 `PermissionStateStore` 已有自动化契约，治理门禁防退化；源码复核显示当前产品 UI 未暴露 Camera/Notifications 调用点，不能为验证新增用户不可见入口 | 有产品入口后再做系统弹窗允许/拒绝/设置页运行验证 |
| 账号、证书和上传 | Release build settings 显示 `PRODUCT_BUNDLE_IDENTIFIER=com.runninghub.app.ios`、`MARKETING_VERSION=1.0`、`CURRENT_PROJECT_VERSION=1`、`CODE_SIGN_STYLE=Automatic`、`CODE_SIGN_IDENTITY=Apple Development`，但 `DEVELOPMENT_TEAM` 为空；不签名 archive 的 `SigningIdentity` 和 `Team` 也为空。本机存在有效 Apple Development/Distribution signing identity，但本地 provisioning profile 数量为 0；2026-07-07 signed archive 复核未带 `CODE_SIGNING_ALLOWED=NO` 时仍 exit 65，错误为 `Signing for "RunningHub" requires a development team`。命令行覆盖 Distribution team 和 Development team 后均失败为 `No Accounts` 与没有匹配 `com.runninghub.app.ios` 的 iOS App Development provisioning profile；Distribution identity 手动覆盖还会触发自动 development signing 与 Apple Distribution identity 冲突。未自动执行 IPA 导出、上传 TestFlight 或 App Store Connect 操作 | 必须先在 Xcode Accounts 登录可管理 `com.runninghub.app.ios` 的 Apple Developer 账号，并配置 Team/provisioning；再确认 App Store Connect、证书、bundle id、版本号和构建号。缺 Xcode account/profile 时不能上传 TestFlight |

### iOS 人工验收记录模板

| 项目 | 记录要求 |
|---|---|
| Limited Photos 管理页 | 2026-07-07 已在 iPhone 17 Pro Simulator 上选择“限制访问…”进入系统 Limited Photos 管理页，完成返回后 QuickCreate 页面未新增素材或错误；后续真机发布前可专项复核追加选择的明确勾选反馈。 |
| 相册写入拒绝 | 2026-07-07 已在 iPhone 17 Pro Simulator 上重置 `photos-add` 后触发保存并选择“不允许”；页面展示稳定失败提示和权限恢复引导，runtime/os log 关键字扫描未命中远端 URL、平台异常原文或敏感字段。 |
| Camera/Notifications 弹窗 | 仅在产品 UI 暴露入口时验证；分别记录允许、拒绝和再次进入设置页的截图或日志。 |
| 验证码成功回传 | 2026-07-07 已在 iPhone 17 Pro signed Simulator 人工完成 TAC 滑块，短信发送成功并完成 SMS 登录；runtime/os log 脱敏扫描未命中 `validToken`、原始 TAC 响应、token、Cookie、手机号、验证码、远端 URL 或平台异常原文。 |
| TestFlight 人审 | 负责人记录 App Store Connect 账号、证书、Team、provisioning profile、bundle id、版本号、构建号、隐私表单和导出合规确认；当前代码已确认 Release framework link 和不签名 Xcode build 通过，但 `DEVELOPMENT_TEAM`、archive `SigningIdentity` 和 `Team` 为空，签名 archive 探测因缺 Xcode account/profile 失败。 |
| 上传专项补验 | 仅在真实运行发现用户可感知失败或发布负责人要求专项补证时执行；当前不要把远端 video/audio/AppDetail 上传 200 作为自动化主线反复推进。 |

当前自动化停线：未收到新的真实失败证据或负责人授权前，不再因为“证据更完整”扩展上传、权限弹窗或验证码自动化。发布前只接受三类后续动作：推送后按当前 `HEAD` 重采 Android/iOS GitHub Actions 外部证据、人工补齐上表真实交互记录、或基于真实用户可感知失败回到实现修复。

### iOS 权限与媒体回归矩阵

- [x] PHPicker 图片/视频选择：未预先授予整库 PhotoKit 权限时仍能打开选择器并返回 app-owned 临时文件 URI。
- [x] Limited Photos 管理入口静态契约：PhotoKit Limited 状态下图片/视频权限恢复入口打开系统有限照片管理页，其他权限仍进入 App Settings，且 Info.plist 禁用系统自动 limited alert。
- [x] Limited Photos 管理页：有限照片权限下可打开系统管理页并完成返回，不污染页面状态；追加选择的明确勾选反馈可在真机发布前专项复核。
- [x] 显式照片权限请求：允许、拒绝、有限照片权限三种路径均记录结果。
- [x] 已拒绝照片权限：需要显式整库权限的入口不进入成功回调；设置页恢复入口由自动化契约覆盖，本轮运行证据覆盖真实拒绝弹窗和应用内恢复引导。
- [x] 相册写入拒绝运行验证：系统照片权限弹窗出现，选择“不允许”后页面展示稳定失败提示和权限恢复引导，日志未暴露远端 URL、敏感 token 或平台异常原文。
- [x] 图片选择：系统选择器返回的 URI 可被上传链路读取，取消选择不会污染页面状态。
- [x] 视频选择：系统选择器返回的 URI 可被上传链路读取，取消选择不会污染页面状态。
- [x] 视频结果显示：历史详情、QuickCreate 对话结果、QuickCreate 历史详情和 AppDetail 任务结果不再把视频原地址当图片解码；签名视频 URL 和无封面视频缩略图已有 iOS Simulator 自动化覆盖。
- [x] 音频选择取消：文档选择器取消后回到参数面板，不新增素材、错误提示或权限说明。
- [ ] 音频文件导入：文档选择器导入的文件可读取字节和文件名。
- [x] 上传失败路径：上传失败或上传中不会继续提交生成任务。
- [x] 图片真实上传回归：QuickCreate iOS 图片素材上传返回 200，并完成图片生成、轮询和历史详情结果查看。
- [ ] 视频真实上传回归：视频素材远端上传返回 200，并完成视频任务提交或失败归因。
- [ ] 音频真实上传回归：真实音频文件经 Document Picker 导入后远端上传返回 200。
- [ ] 真实上传回归：图片、视频、音频各完成一次真实上传。

### 验证码 Web 容器回归矩阵

- [x] Android WebView 和 iOS WKWebView 的 TAC JS/CSS 同源加载配置已由平台环境契约测试覆盖。
- [x] 验证码关闭和失败重试：关闭弹窗、连续打开、网络/script 失败和超时均有自动化防线。
- [x] 验证码 token 静态脱敏：HTML 包装层和平台回调不得把 `validToken` 或原始 TAC 响应写入 Web console/native log。
- [x] iOS WKWebView 成功验证后能回传 `validToken` 并触发短信发送，且 token 不进入日志、崩溃报告或截图说明。
- [x] 用户关闭弹窗时能关闭状态层，旧 token 不会在关闭后触发短信重试。
- [x] 连续打开两次验证码时，旧 Web 容器回调不会交给当前状态层。
- [x] 网络失败、脚本失败和超时均展示可重试降级状态。
- [x] iOS signed Simulator 已记录 ATS、Cookie、同源策略和跨域请求行为结论：TAC 资源加载、短信发送和 SMS 登录未出现相关错误；Android 真实 WebView 和 TestFlight 仍需专项复核。

## 依赖升级与重大版本回归

- [ ] Dependabot、Dependency Submission 或人工升级产生的依赖变更已在 PR 中列出影响模块。
- [ ] patch/minor 级依赖升级已执行受影响模块验证命令，并记录未覆盖平台。
- [ ] major 级依赖、Gradle、AGP、Kotlin、Xcode 或运行时 SDK 升级已列出 Android/iOS 人工回归矩阵和负责人。
- [ ] 重大版本升级不得自动合并，必须由负责人确认双端回归结果后再发布。

## 停止条件

- [ ] 生产签名、商店上传和敏感配置变更不得自动执行
- [ ] 任一平台未完成运行验证时，发布说明必须列出未验证项和剩余风险
