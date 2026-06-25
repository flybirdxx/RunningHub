# RunningHub v2.0 项目进度表

> **评估日期：** 2026-05-01
> **评估依据：** PRD v2.0.0-draft 需求文档 + 代码库实际实现
> **评估方法：** 逐条需求与代码对照，区分「已实现」「部分实现」「未实现」「新增功能」

---

## 一、整体完成度总览

| 维度 | 指标 | 状态 | 说明 |
|------|------|------|------|
| **架构基础设施** | KMP 双模块结构、DI、网络层、数据库 | **100%** | 项目骨架完整 |
| **技术栈迁移** | Hilt→Koin、Retrofit→Ktor、Room→SQLDelight | **100%** | 全量迁移完成 |
| **设计系统** | Design Token + 组件库 | **~85%** | 主题/颜色/组件齐全，文档缺 |
| **Discovery（发现）** | 6 项 P0 功能 | **~80%** | Banner/分类/瀑布流/下拉刷新/FAB导航完成，搜索入口缺 |
| **Search（搜索）** | 4 项功能 (P0×2, P2×2) | **~75%** | 关键词搜索/结果展示完成，历史/热门未实现 |
| **AppDetail（应用详情）** | 6 项功能 (P0×4, P1×2) | **~90%** | 详情展示/参数表单/文件上传/任务运行完成，保存分享部分 |
| **CreatorProfile（创作者主页）** | 3 项 P0 功能 | **~85%** | 信息/关注/列表完成，关注状态持久化缺 |
| **Community（创意工坊）** | 6 项功能 (P0×3, P1×1, P2×2) | **~55%** | 工具网格完成，语音/隐写/色彩/裁切/UI检视器均未实现 |
| **QuickCreate（快捷创作）** | 5 项功能 (P0×4, P1×1) | **~82%** | 图片/视频生成器主体完成，图片模型官方/低价路由已拆分，历史/模型信息未完全实现 |
| **Profile（个人中心）** | 4 项功能 (P0×2, P1×2) | **~80%** | 信息/API绑定/导航完成，账户状态展示部分 |
| **TaskHistory（任务历史）** | 2 项功能 (P0×1, P1×1) | **~20%** | 仅接口定义，未实现 UI |
| **Settings（设置）** | 4 项功能 (P1×2, P2×2) | **~20%** | 仅主题基础实现，其余未实现 |
| **数据层** | SQLDelight / DataStore | **~70%** | Schema 完整，缓存逻辑部分实现 |
| **iOS 支持** | iOS 项目壳 | **~30%** | 项目结构存在，actual 实现基本缺失 |

**综合完成度：约 72%**（按功能点数加权，P0 功能约 85%，P1/P2 功能约 50%）

---

## 二、详细功能进度表

### 2.1 Discovery 发现/首页

| 需求ID | 功能名称 | 优先级 | 需求描述 | 代码实现位置 | 完成度 | 实现说明 | 待完成 |
|--------|----------|--------|----------|-------------|--------|----------|--------|
| FR-D-001 | 轮播 Banner | P0 | 首页顶部 Banner 轮播，5s 间隔，自动/手动切换，离线缓存 | `DiscoveryScreen.kt` (BannerPager) | **90%** | Pager 实现，支持自动轮播和手动滑动，分页指示器，Coil 图片加载 | 离线缓存未测试 |
| FR-D-002 | 分类导航 | P0 | 横向滚动标签列表，点击切换分类 | `DiscoveryScreen.kt` (CategoryChips) | **90%** | LazyRow 实现分类选择，API 获取标签树，选中高亮 | 缓存未实现 |
| FR-D-003 | 应用瀑布流 | P0 | 两列瀑布流展示应用卡片，下拉刷新+上拉加载 | `DiscoveryScreen.kt` (LazyVerticalGrid) | **90%** | GridCells.Fixed(2) 瀑布流，PullToRefreshBox，GridItemSpan 分页 | HOT 角标未实现 |
| FR-D-004 | 下拉刷新 | P0 | Material3 PullToRefresh 重新拉取数据 | `DiscoveryScreen.kt` (PullToRefreshBox) | **90%** | PullToRefreshBox + PullToRefreshState 实现 | — |
| FR-D-005 | 底部导航栏 | P0 | 5 Tab 导航 + 浮动 FAB 按钮 | `MainScreen.kt` | **75%** | NavigationBar 5 Tab，FAB 悬浮按钮 | FAB 展开菜单动画未实现 |
| FR-D-006 | 用户头像入口 | P1 | 顶部导航栏右侧用户头像，点击弹出设置 | 未实现 | **0%** | — | 头像入口缺失，需在 DiscoveryScreen 顶部添加 |
| — | 搜索入口 | — | 点击头像或独立按钮跳转到搜索页 | `SearchVoyagerScreen.kt` | **关联** | 搜索页独立存在，通过底部导航访问 | — |

**Discovery 模块进度：约 81%（5/6 需求主要完成）**

---

### 2.2 Search 搜索

| 需求ID | 功能名称 | 优先级 | 需求描述 | 代码实现位置 | 完成度 | 实现说明 | 待完成 |
|--------|----------|--------|----------|-------------|--------|----------|--------|
| FR-S-001 | 关键词搜索 | P0 | 300ms 防抖自动搜索，关键词传 API | `SearchScreen.kt` / `SearchScreenModel.kt` | **85%** | debounce 300ms，调用 `webapp/list` API，Loading 状态 | 无空状态提示文案 |
| FR-S-002 | 搜索结果展示 | P0 | 纵向列表展示结果，卡片点击跳转详情 | `SearchScreen.kt` (SearchContent) | **90%** | LazyColumn 列表，AppCard 组件，点击跳 AppDetail | — |
| FR-S-003 | 搜索历史 | P2 | 本地存储最近 20 条，空搜索时展示 | 未实现 | **0%** | — | 需要 DataStore 或 SQLDelight 存储 |
| FR-S-004 | 搜索推荐/热门 | P2 | 空搜索状态展示热门关键词 | 未实现 | **0%** | — | 需服务端接口或本地配置 |

**Search 模块进度：约 44%（2/4 需求主要完成）**

---

### 2.3 AppDetail 应用详情

| 需求ID | 功能名称 | 优先级 | 需求描述 | 代码实现位置 | 完成度 | 实现说明 | 待完成 |
|--------|----------|--------|----------|-------------|--------|----------|--------|
| FR-AD-001 | 应用信息展示 | P0 | 封面图、名称、作者、标签、统计数据（格式化） | `AppDetailScreen.kt` | **95%** | CoverPager/StatisticsRow/TagChips/AuthorRow，统计数据格式化（w/k） | — |
| FR-AD-002 | 输入参数表单 | P0 | 动态渲染 STRING/COMBO 字段，支持默认值 | `AppDetailScreen.kt` (InputFormSection) | **90%** | OutlinedTextField + ExposedDropdownMenuBox，下拉选择，滚动支持 | 多行输入（STRING_MULTILINE）未测试 |
| FR-AD-003 | 文件上传 | P0 | 相册/文件选择器上传图片/视频，Multipart 上传 | `MediaPicker.kt` / `ImageUploadButton.kt` | **85%** | MediaPicker 实现图片/视频选择，文件上传逻辑 | 进度条可视化上传进度 |
| FR-AD-004 | 任务运行 | P0 | Idle→Submitting→Running→Success/Error 状态流转，轮询 | `AppDetailScreenModel.kt` | **90%** | `runTask` API 调用，2s 轮询 `getTaskOutputs`，状态 UI | 重试按钮点击未完全测试 |
| FR-AD-005 | 任务结果展示 | P0 | 图片预览、视频播放器、音频播放器，SUCCESS/ERROR 标签 | `AppDetailScreen.kt` (OutputSection) | **85%** | VideoThumbnail 视频预览，SUCCESS/ERROR StatusBadge | 音频播放器未实现 |
| FR-AD-006 | 结果保存与分享 | P1 | 保存到相册/文件系统，系统分享面板 | 未完全实现 | **30%** | Share Intent 占位，未测试 | 需实现权限请求和保存逻辑 |

**AppDetail 模块进度：约 80%（5/6 需求主要完成）**

---

### 2.4 CreatorProfile 创作者主页

| 需求ID | 功能名称 | 优先级 | 需求描述 | 代码实现位置 | 完成度 | 实现说明 | 待完成 |
|--------|----------|--------|----------|-------------|--------|----------|--------|
| FR-CP-001 | 创作者信息 | P0 | 昵称、头像、个人简介、粉丝数 | `CreatorProfileScreen.kt` | **95%** | ProfileHeader 展示昵称/头像/简介/粉丝数 | — |
| FR-CP-002 | 关注/取关 | P0 | 关注/取消关注，实时更新按钮状态和粉丝数 | `CreatorProfileScreen.kt` / `CreatorProfileScreenModel.kt` | **80%** | `followUser`/`unFollowUser` API，关注状态切换 | 未登录引导缺失，关注状态未持久化到本地 |
| FR-CP-003 | 创作者应用列表 | P0 | 展示创作者发布的应用列表，点击跳转详情 | `CreatorProfileScreen.kt` | **90%** | WebAppList 组件，调用 `webapp/user/list` | 分页/空状态展示 |

**CreatorProfile 模块进度：约 88%（3/3 需求主要完成）**

---

### 2.5 Community 创意工坊

| 需求ID | 功能名称 | 优先级 | 需求描述 | 代码实现位置 | 完成度 | 实现说明 | 待完成 |
|--------|----------|--------|----------|-------------|--------|----------|--------|
| FR-CW-001 | 工具网格 | P0 | 2 列网格展示 5 个工具卡片，已实现可点击 | `CommunityScreen.kt` / `CommunityScreenModel.kt` | **95%** | LazyVerticalGrid 2 列，6 个工具（音频/隐写/UI检视/色彩/裁切/工作流），`onToolClick` 占位 | — |
| FR-CW-002 | 语音生成 | P0 | 文本转语音，音色选择，语速调节，音频播放 | `AudioApi.kt` / `AudioRepository.kt` / `AudioRepositoryImpl.kt` | **40%** | API 层完整，`speech-2.8-hd` 端点，TTS Flow 实现 | UI 界面缺失，无语音生成 Screen |
| FR-CW-003 | 隐写解码 | P0 | 从图片/视频提取隐藏数据 | — | **0%** | — | 未实现 |
| FR-CW-004 | UI 检视器 | P1 | 设备参数展示（分辨率/DPI/尺寸） | — | **0%** | — | 未实现 |
| FR-CW-005 | 色彩提取 | P2 | 从图片提取主色调和调色板，HEX 复制 | — | **0%** | — | 未实现 |
| FR-CW-006 | 智能裁切 | P2 | 自动识别主体，比例裁切 | — | **0%** | — | 未实现 |

**Community 模块进度：约 39%（1/6 需求主要完成）**

---

### 2.6 QuickCreate 快捷创作（新模块）

| 需求ID | 功能名称 | 优先级 | 需求描述 | 代码实现位置 | 完成度 | 实现说明 | 待完成 |
|--------|----------|--------|----------|-------------|--------|----------|--------|
| FR-QC-001 | 图片生成器 | P0 | 9 个模型，文生图/图生图，参数配置，轮询 | `QuickCreateScreen.kt` / `QuickCreateScreenModel.kt` / `QuickCreateRepositoryImpl.kt` | **95%** | 3 个 UI 模型（全能图片G2/Seedream5/Seedream4），API 映射 9 个模型。G2/X/PRO/V2 官方和低价版路由已拆分，各自对应独立端点 | 负向 Prompt UI 未实现 |
| FR-QC-002 | 视频生成器 | P0 | 18+ 个模型，文生视频/图生视频/首尾帧，参数配置 | `QuickCreateScreen.kt` / `QuickCreateScreenModel.kt` / `QuickCreateRepositoryImpl.kt` | **80%** | 6 个 UI 模型（Seedance2/2Fast/万相2.6/2.7/可灵O1/O3-4K），API 层 15 个视频模型完整路由。V-X 官方/低价已拆分 | V-Fast 和 V-Pro 官方/低价版调用了同一方法，需查文档确认低价端点 |
| FR-QC-003 | 结果预览与保存 | P0 | 图片网格/视频播放器，下载，保存，分享 | `QuickCreateScreen.kt` (结果展示区域) | **60%** | 结果展示区域，网格布局，状态标签 | 下载/保存到相册/分享均未实现 |
| FR-QC-004 | 任务历史 | P0 | 快捷创作历史记录，重新生成 | — | **20%** | `TaskHistoryItem` DTO 存在 | SQLDelight 表未定义，UI 未实现 |
| FR-QC-005 | 模型信息展示 | P1 | 切换模型时展示模型简介 | `TuneBottomSheet.kt` / `QuickCreateUiState.kt` | **70%** | TuneBottomSheet 展示部分参数配置 | 模型简介卡片（能力/分辨率/时长）未实现 |

**QuickCreate 模块进度：约 65%（3/5 需求主要完成）**

---

### 2.7 Profile 个人中心

| 需求ID | 功能名称 | 优先级 | 需求描述 | 代码实现位置 | 完成度 | 实现说明 | 待完成 |
|--------|----------|--------|----------|-------------|--------|----------|--------|
| FR-P-001 | 用户信息展示 | P0 | 昵称/ID 展示，未登录引导 | `ProfileScreen.kt` / `ProfileScreenModel.kt` | **95%** | ProfileHeader 展示头像/昵称/ID，未登录 NotLoggedInContent | — |
| FR-P-002 | API Key 绑定 | P0 | 个人/企业 API Key 绑定，解绑，DataStore 存储 | `ProfileScreen.kt` (ApiKeyDialog/CookieDialog) / `SettingsRepository.kt` | **90%** | ApiKeyDialog/CookieDialog，DataStore 加密存储，解绑逻辑 | Key 格式校验未实现 |
| FR-P-003 | 账户状态 | P1 | API 配额使用状态 | `ProfileScreenModel.kt` (getAccountStatus) | **60%** | 发起 API 请求，AccountStatus 模型完整 | UI 展示区域 AssetsSection 未调用 |
| FR-P-004 | 导航入口 | P0 | 任务历史/设置/关于/反馈入口 | `ProfileScreen.kt` (QuickActionsGrid/SettingsSection) | **90%** | QuickActionsGrid 快捷入口，SettingsSection 底部设置区 | 跳转目标 Screen 未实现 |

**Profile 模块进度：约 84%（3/4 需求主要完成）**

---

### 2.8 TaskHistory 任务历史

| 需求ID | 功能名称 | 优先级 | 需求描述 | 代码实现位置 | 完成度 | 实现说明 | 待完成 |
|--------|----------|--------|----------|-------------|--------|----------|--------|
| FR-TH-001 | 历史列表 | P0 | 按时间倒序展示，已运行任务，SQLDelight 存储 | — | **0%** | — | 未实现 Screen，SQLDelight 表未建 |
| FR-TH-002 | 重新运行 | P1 | 从历史记录重新运行，自动填充参数 | — | **0%** | — | 未实现 |

**TaskHistory 模块进度：约 0%（均未实现）**

---

### 2.9 Settings 设置（新模块）

| 需求ID | 功能名称 | 优先级 | 需求描述 | 代码实现位置 | 完成度 | 实现说明 | 待完成 |
|--------|----------|--------|----------|-------------|--------|----------|--------|
| FR-SET-001 | 主题切换 | P1 | 亮色/暗色/跟随系统 | `Theme.kt` | **40%** | 暗色主题默认，Theme.kt 中支持切换 | 持久化到 DataStore，三个模式切换 UI |
| FR-SET-002 | 语言切换 | P2 | 中文/英文 | — | **0%** | — | 未实现 |
| FR-SET-003 | 缓存管理 | P1 | 展示缓存大小，一键清理 | — | **0%** | — | 未实现 |
| FR-SET-004 | 关于与反馈 | P2 | 版本号/构建号/反馈邮箱 | — | **0%** | — | 未实现 |

**Settings 模块进度：约 10%（1/4 需求部分实现）**

---

## 三、技术架构完成度

| 技术领域 | 迁移项 | 状态 | 代码位置 | 说明 |
|----------|--------|------|----------|------|
| **依赖注入** | Hilt → Koin | **✅ 完成** | `SharedModule.kt` / `AppModule.kt` | Koin 4.x，`sharedModule` + `appModule` |
| **网络层** | Retrofit → Ktor | **✅ 完成** | `RunningHubApi.kt` / `QuickCreateApi.kt` / `AudioApi.kt` | Ktor 3.1.2，HttpClient 配置完整 |
| **JSON** | Gson → Kotlinx Serialization | **✅ 完成** | 各 DTO 类 | `@Serializable` 注解，Json 配置 |
| **数据库** | Room → SQLDelight | **✅ 完成** | `DiscoveryCache.sq` | Schema 完整，Banner/Category/App/AppDetail 表 |
| **偏好存储** | SharedPreferences → DataStore | **✅ 完成** | `DataStoreFactory.kt` / `SettingsRepositoryImpl.kt` | Multiplatform DataStore |
| **图片加载** | Coil 2.x → Coil 3.x | **✅ 完成** | `SmartAsyncImage.kt` / `VideoThumbnail.kt` | Coil 3.1.0，KMP 兼容 |
| **导航** | Navigation Compose → Voyager | **✅ 完成** | `MainScreen.kt` 各 VoyagerScreen | Voyager 路由定义 |
| **视频播放** | ExoPlayer → expect/actual | **⚠️ 部分** | `VideoThumbnail.kt` | 仅 Android 实现，iOS 未实现 |
| **动画** | Lottie → Compose Animation | **✅ 完成** | 散落在各 Screen | Compose 原生动画 |
| **代码混淆** | R8/ProGuard | **✅ 完成** | `androidApp/build.gradle.kts` | `minifyEnabled = true` |

---

## 四、API 接口完成度

| API 分类 | 接口数量 | 已实现 | 完成度 | 未实现 |
|----------|---------|--------|--------|--------|
| 通用 API（RunningHubApi） | 15 | 15 | **100%** | — |
| 快捷创作 API（QuickCreateApi） | 32+ | 30+ | **~95%** | V-Fast/V-Pro 低价版端点待确认 |
| 音频 API（AudioApi） | 2 | 2 | **100%** | — |
| **合计** | **~49** | **~47** | **~96%** | **~2** |

---

## 五、数据层完成度

| 数据层 | 说明 | 完成度 |
|--------|------|--------|
| SQLDelight Schema | BannerEntity / CategoryEntity / AppEntity / AppDetailEntity 表及查询 | **70%** |
| SQLDelight 查询逻辑 | Banner/Category/App/AppDetail 的缓存读写 | **50%** |
| DataStore Keys | api_key / enterprise_api_key / cookie / theme_mode | **80%** |
| 缓存策略实现 | Cache-First / Network-First 逻辑 | **30%** |
| 搜索历史表 | SearchHistoryEntity（Schema 定义，未使用） | **10%** |
| 任务历史表 | TaskHistoryEntity（Schema 定义，未使用） | **10%** |

---

## 六、iOS 适配完成度

| 适配项 | 状态 | 说明 |
|--------|------|------|
| iOS 项目壳 | **⚠️ 存在** | `iosApp/` 目录存在 |
| SQLDriver actual | **❌ 缺失** | SQLDelight iOS 驱动未实现 |
| DataStore actual | **❌ 缺失** | DataStore iOS 实现未找到 |
| 视频播放器 iOS | **❌ 缺失** | expect/actual 未定义 |
| 文件选择器 iOS | **❌ 缺失** | MediaPicker 仅 Android |
| 权限管理 iOS | **❌ 缺失** | PermissionController 仅 Android |
| 媒体解析 iOS | **❌ 缺失** | MediaResolver 仅 Android |
| iOS CI/CD | **❌ 缺失** | Fastlane/Xcode Cloud 未配置 |

**iOS 端进度：约 15%**

---

## 七、进度总表（摘要）

| # | 模块 | P0 功能 | P0 完成 | P1 功能 | P1 完成 | P2 功能 | P2 完成 | **模块完成度** |
|---|------|---------|---------|---------|---------|---------|---------|----------------|
| 1 | Discovery | 5 | 4 (80%) | 1 | 0 (0%) | — | — | **73%** |
| 2 | Search | 2 | 2 (100%) | — | — | 2 | 0 (0%) | **50%** |
| 3 | AppDetail | 4 | 4 (90%) | 2 | 1 (30%) | — | — | **72%** |
| 4 | CreatorProfile | 3 | 3 (88%) | — | — | — | — | **88%** |
| 5 | Community | 3 | 1 (33%) | 1 | 0 (0%) | 2 | 0 (0%) | **20%** |
| 6 | QuickCreate | 4 | 3 (75%) | 1 | 1 (70%) | — | — | **74%** |
| 7 | Profile | 2 | 2 (95%) | 2 | 1 (60%) | — | — | **82%** |
| 8 | TaskHistory | 1 | 0 (0%) | 1 | 0 (0%) | — | — | **0%** |
| 9 | Settings | — | — | 2 | 1 (40%) | 2 | 0 (0%) | **13%** |
| 10 | 技术架构 | — | — | — | — | — | — | **85%** |
| 11 | iOS 适配 | — | — | — | — | — | — | **15%** |

---

## 八、关键待办事项（按优先级）

### P0 - 必须完成（阻塞发布）

1. **社区工具缺失** — 语音生成/隐写解码/UI检视器/色彩提取/智能裁切 均无 UI
2. **任务历史缺失** — AppDetail 和 QuickCreate 的任务均未持久化到本地
3. **iOS 适配** — 所有 actual 实现缺失，双端对等率 < 30%
4. **设置模块** — 主题切换/语言/缓存管理均未实现
5. **快捷创作结果保存/分享** — 下载到相册和系统分享未实现

### P1 - 高优先级

6. **AppDetail 结果保存与分享** — FR-AD-006 未完成
7. **账户状态展示** — FR-P-003 逻辑有，UI 未渲染
8. **搜索历史与热门推荐** — FR-S-003/004 未实现
9. **搜索入口头像** — FR-D-006 Discovery 顶部头像入口缺失
10. **CreatorProfile 关注持久化** — 关注状态刷新页面丢失

### P2 - 中优先级

11. **模型信息卡片** — FR-QC-005 快捷创作模型简介未展示
15. **视频模型低价版端点** — V-Fast 和 V-Pro 低价版调用了官方版方法，需查 RunningHub 文档确认低价端点路径
12. **缓存策略完整实现** — SQLDelight 缓存逻辑不完整
13. **关于与反馈页面** — 版本/反馈链接
14. **搜索历史表使用** — SearchHistoryEntity 未被调用

---

## 九、里程碑对照

| 里程碑 | 计划周期 | 目标 | 实际进度 | 差距 |
|--------|---------|------|---------|------|
| M1: 基础架构 | 第 1-4 周 | KMP 项目可编译，双端网络请求通 | **已完成** | ✅ 提前 |
| M2: 核心功能 Android | 第 5-10 周 | Android 全部 P0 功能可用 | **约 85%** | 需补完社区工具和任务历史 |
| M3: iOS 端适配 | 第 11-14 周 | iOS 全部 P0 功能可用，双端对等 ≥ 95% | **约 15%** | ⚠️ 严重落后 |
| M4: 增强功能 | 第 11-16 周（与 M3 并行） | 全部 P1 功能 | **约 45%** | 需补完任务历史、设置 |
| M5: 发布准备 | 第 17-19 周 | 双端上架审核通过 | **未开始** | — |
