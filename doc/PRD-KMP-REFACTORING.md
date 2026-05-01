# RunningHub KMP 重构产品需求文档 (PRD)

> **文档版本**: v1.0  
> **创建日期**: 2026-04-25  
> **状态**: 草案  
> **目标**: 将 RunningHub 从纯 Android 应用重构为 KMP + Compose Multiplatform 双平台（Android + iOS）应用

---

## 目录

1. [执行摘要](#1-执行摘要)
2. [当前功能清单](#2-当前功能清单)
3. [平台策略](#3-平台策略)
4. [架构需求](#4-架构需求)
5. [设计系统需求](#5-设计系统需求)
6. [质量需求](#6-质量需求)
7. [阶段计划](#7-阶段计划)
8. [功能验收标准](#8-功能验收标准)
9. [风险与缓解](#9-风险与缓解)
10. [附录](#10-附录)

---

## 1. 执行摘要

### 1.1 为什么重构

RunningHub 是一个 AI 应用聚合平台客户端，当前仅有 Android 版本。Phase 0 审计结果暴露了以下核心问题：

| 维度 | 现状 | 目标 |
|------|------|------|
| Compose 审计评分 | **53/100** | **80+/100** |
| 架构评分 | **65/100** | **85+/100** |
| 技术债务指数 | **3.3/10**（较差） | **7+/10** |
| KMP 可迁移纯 Kotlin 代码 | **~9%** | **70%+** |
| 测试覆盖率 | **0%** | **60%+** |
| 支持平台 | Android 单平台 | Android + iOS 双平台 |

**关键技术债务**：
- **无领域层**：ViewModel 直接调用 Repository，业务逻辑与 UI 和数据层耦合
- **循环依赖**：DI 模块间存在隐式耦合（SharedPreferences 直接读取绕过 Repository）
- **60+ 硬编码颜色值**：未使用 MaterialTheme token，阻碍主题化和跨平台一致性
- **零测试**：无单元测试、无集成测试、无 UI 测试
- **Android 专属绑定**：Hilt、Room、Retrofit、SharedPreferences 均为 Android 独占库

### 1.2 目标平台

| 平台 | 最低版本 | 目标版本 |
|------|----------|----------|
| Android | API 24 (Android 7.0) | API 35 (Android 15) |
| iOS | 15.0 | 18.x |

### 1.3 总时间线

**总工期预估**: 36-44 周（9 个月 ~ 11 个月）

| 里程碑 | 时间 | 关键交付 |
|--------|------|----------|
| M0: 基础设施就绪 | 第 1-4 周 | KMP 项目骨架、CI/CD、设计系统 token |
| M1: 共享核心上线 | 第 5-12 周 | Domain 层、网络层、数据层完成迁移 |
| M2: 首屏双平台 | 第 13-20 周 | Discovery + Profile 在 Android/iOS 运行 |
| M3: 功能齐全 | 第 21-32 周 | 全部 9 个功能模块双平台交付 |
| M4: 质量达标 | 第 33-40 周 | 测试覆盖 60%+、Compose 评分 80+、性能调优 |
| M5: 发布 | 第 41-44 周 | App Store + Google Play 双平台上架 |

---

## 2. 当前功能清单

### 2.1 功能矩阵

| # | 功能模块 | 路由 | 复杂度 | 当前状态 | KMP 可迁移性 |
|---|---------|------|--------|----------|-------------|
| F1 | 发现页（主页） | `discovery` | 高 | 已实现 | 中 — 瀑布流依赖 Compose LazyStaggeredGrid |
| F2 | 应用详情 | `app/{appId}` | 高 | 已实现 | 中 — 含文件上传、视频播放、任务轮询 |
| F3 | 搜索 | `search` | 低 | 已实现 | 高 — 纯 UI + API 调用 |
| F4 | 个人中心 | `profile` | 中 | 已实现 | 中 — API Key 管理涉及 SharedPreferences |
| F5 | 创作者主页 | `creator/{userId}` | 中 | 已实现 | 高 — 纯展示 + 关注逻辑 |
| F6 | 社区 | `community` | 中 | 已实现 | 高 — 工具卡片网格 |
| F7 | 音频生成 | `audioGeneration` | 中 | 已实现 | 低 — 依赖 MiniMax 私有 API + ExoPlayer |
| F8 | 隐写解码 | `secretDecode` | 中 | 已实现 | 低 — 依赖 Android Bitmap/MediaPlayer |
| F9 | UI Inspector | `uiInspector` | 低 | 已实现 | 高 — 工具页面 |

### 2.2 导航结构

```
MainActivity
└── NavHost
    ├── Discovery (底部导航 Tab 1)
    │   ├── Banner 轮播
    │   ├── 分类标签 (21 个一级分类，100+ 二级分类)
    │   ├── AI 应用瀑布流 (无限滚动)
    │   └── FAB 菜单覆盖层
    ├── Community (底部导航 Tab 2)
    │   ├── 工具卡片网格
    │   ├── → AudioGeneration
    │   ├── → SecretDecode
    │   └── → UiInspector
    ├── Profile (底部导航 Tab 3)
    │   ├── 用户信息
    │   ├── API Key 管理
    │   ├── 账户状态
    │   ├── 发布应用列表
    │   └── → TaskHistory
    ├── AppDetail (由 Discovery 进入)
    │   ├── 应用信息头
    │   ├── API Demo 展示
    │   ├── 输入表单 (文字/图片/视频上传)
    │   ├── 任务执行 + 轮询
    │   └── 输出展示 (图片/视频)
    ├── CreatorProfile (由 AppDetail 进入)
    ├── Search (由 Discovery 进入)
    └── TaskHistory (由 Profile 进入)
```

### 2.3 现有技术栈详情

| 类别 | 当前实现 | 版本 |
|------|---------|------|
| 语言 | Kotlin | 1.9.23 |
| UI 框架 | Jetpack Compose + Material3 | BOM 2024.10.00 |
| Compose Compiler | Extension 模式 | 1.5.11 |
| 构建工具 | AGP | 8.13.2 |
| DI | Hilt (Dagger) | 2.50 |
| 网络 | Retrofit + OkHttp + Gson | 2.9.0 / 4.12.0 |
| 数据库 | Room | 2.6.1 |
| 图片加载 | Coil | 2.6.0 |
| 视频播放 | Media3 ExoPlayer | 1.3.1 |
| 动画 | Lottie Compose | 6.4.0 |
| 加载占位 | Shimmer | 1.2.0 |
| 导航 | Navigation Compose (字符串路由) | 2.7.7 |
| 本地存储 | SharedPreferences | Android 内置 |

### 2.4 源码文件统计

| 层级 | 文件数 | 说明 |
|------|--------|------|
| `ui/feature/` | 22 | 屏幕 + ViewModel + UiState |
| `ui/component/` | 3 | SmartAsyncImage, VideoPlayer, FabMenuOverlay |
| `ui/theme/` | 3 | Color, Type, Theme |
| `ui/navigation/` | 2 | Screen, AppNavigation |
| `data/remote/` | 7 | API 接口 + 数据模型 |
| `data/local/` | 4 | Room 数据库 + DAO + 实体 + SharedPreferences |
| `data/repository/` | 4 | Repository 实现 |
| `di/` | 3 | Hilt Module |
| `util/` | 2 | ResourceCacheManager, PermissionManager |
| 根包 | 3 | MainActivity, SplashActivity, RunningHubApp |
| **合计** | **53** | |

---

## 3. 平台策略

### 3.1 共享功能（双平台完全一致）

以下功能的业务逻辑、数据模型、网络请求、状态管理均在 `shared` 模块中实现，UI 使用 Compose Multiplatform 共享：

| 功能 | 共享范围 | 说明 |
|------|---------|------|
| F1 发现页 | 100% 逻辑 + 95% UI | Banner 轮播、分类标签、瀑布流、下拉刷新、无限滚动 |
| F2 应用详情 | 100% 逻辑 + 85% UI | 应用信息、输入表单、任务执行/轮询、输出展示 |
| F3 搜索 | 100% 逻辑 + 100% UI | 搜索输入、结果列表 |
| F4 个人中心 | 100% 逻辑 + 90% UI | 用户信息、API Key 管理、账户状态、发布应用列表 |
| F5 创作者主页 | 100% 逻辑 + 95% UI | 创作者信息、关注/取关、应用列表 |
| F6 社区 | 100% 逻辑 + 95% UI | 工具卡片网格、分类 |
| F9 UI Inspector | 100% 逻辑 + 100% UI | 纯展示工具页 |

**共享层合计**：
- **Domain 层**: 100% 共享（UseCase、Domain Model、Mapper）
- **Data 层**: 95% 共享（Repository 接口 + Ktor 网络层 + SQLDelight 缓存层）
- **UI 层**: ~90% 共享（Compose Multiplatform 屏幕 + 组件库）

### 3.2 平台特定功能

#### 3.2.1 需要平台适配的功能

| 功能 | 适配点 | Android | iOS |
|------|--------|---------|-----|
| F2 文件上传 | 文件选择器 | `ActivityResultContracts` | `PHPicker` / `UIDocumentPicker` |
| F2 视频播放 | 播放器 | Media3 ExoPlayer | AVPlayer |
| F7 音频生成 | 音频播放 | MediaPlayer / ExoPlayer | AVAudioPlayer |
| F8 隐写解码 | 图像像素操作 | `android.graphics.Bitmap` | `CoreGraphics` / `UIImage` |
| F8 音频播放 | MediaPlayer | ExoPlayer | AVAudioPlayer |

#### 3.2.2 平台 UI 差异

| UI 元素 | Android | iOS |
|---------|---------|-----|
| 系统栏适配 | Edge-to-Edge + `WindowInsets` | Safe Area + `UIEdgeInsets` |
| 图标体系 | Material Icons Extended | SF Symbols |
| 导航手势 | 系统返回 + 预测返回动画 | 右滑返回 (iOS native swipe) |
| 下拉刷新 | Material3 `PullToRefreshBox` | `UIRefreshControl` 风格 |
| 底部导航 | Material3 `NavigationBar` | iOS Tab Bar 风格 |
| 权限请求 | `ActivityResultContracts.RequestPermission` | `Info.plist` + 原生弹窗 |
| Toast/Snackbar | Material3 `SnackbarHost` | iOS 风格的 Toast |
| 启动页 | `SplashActivity` | Launch Storyboard |

#### 3.2.3 expect/actual 声明清单

```
expect interface Platform
  ├── actual AndroidPlatform  (Android Context)
  └── actual IosPlatform      (UIKit 桥接)

expect class FilePickerLauncher
  ├── actual AndroidFilePickerLauncher  (ActivityResult API)
  └── actual IosFilePickerLauncher      (PHPicker)

expect class VideoPlayerFactory
  ├── actual AndroidVideoPlayerFactory  (Media3 ExoPlayer)
  └── actual IosVideoPlayerFactory      (AVPlayer)

expect class AudioPlayerFactory
  ├── actual AndroidAudioPlayerFactory  (Media3)
  └── actual IosAudioPlayerFactory      (AVAudioPlayer)

expect class ImageDecoder
  ├── actual AndroidImageDecoder  (android.graphics.Bitmap)
  └── actual IosImageDecoder      (CoreGraphics)

expect class PermissionHandler
  ├── actual AndroidPermissionHandler  (ActivityResult)
  └── actual IosPermissionHandler      (Info.plist + native)

expect class LocalStorageFactory
  ├── actual AndroidLocalStorageFactory  (DataStore)
  └── actual IosLocalStorageFactory      (NSUserDefaults)
```

---

## 4. 架构需求

### 4.1 KMP 模块结构

```
RunningHub/
├── build.gradle.kts                  # 根构建配置 (Convention Plugins)
├── settings.gradle.kts               # 模块注册
├── gradle/
│   └── libs.versions.toml            # 版本目录统一管理
│
├── shared/                           # ★ KMP 共享模块
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/               # 跨平台业务代码
│       │   └── kotlin/com/runninghub/
│       │       ├── domain/           # ★ 领域层 (新增)
│       │       │   ├── model/        # 领域模型
│       │       │   ├── usecase/      # 用例
│       │       │   └── repository/   # Repository 接口
│       │       ├── data/             # 数据层
│       │       │   ├── remote/       # Ktor 网络实现
│       │       │   │   ├── api/      # API 定义
│       │       │   │   ├── dto/      # 数据传输对象
│       │       │   │   └── mapper/   # DTO → Domain 映射器
│       │       │   ├── local/        # SQLDelight 本地缓存
│       │       │   │   ├── db/       # 数据库定义
│       │       │   │   └── mapper/   # Entity → Domain 映射器
│       │       │   └── repository/   # Repository 实现
│       │       └── util/             # 公共工具
│       ├── commonTest/               # 共享测试
│       ├── androidMain/              # Android 平台实现
│       └── iosMain/                  # iOS 平台实现
│
├── composeApp/                       # ★ Compose Multiplatform UI 模块
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/               # 跨平台 UI
│       │   └── kotlin/com/runninghub/app/
│       │       ├── ui/
│       │       │   ├── designsystem/ # ★ 统一设计系统
│       │       │   │   ├── theme/    # 颜色/排版/间距 Token
│       │       │   │   └── component/# 共享组件库
│       │       │   ├── feature/      # 功能屏幕
│       │       │   │   ├── discovery/
│       │       │   │   ├── detail/
│       │       │   │   ├── search/
│       │       │   │   ├── profile/
│       │       │   │   ├── creator/
│       │       │   │   ├── community/
│       │       │   │   └── tools/    # 音频生成、隐写解码、UI Inspector
│       │       │   └── navigation/   # 导航图
│       │       └── di/               # Koin 模块定义
│       ├── commonTest/
│       ├── androidMain/              # Android 入口 + 平台组件
│       │   └── kotlin/
│       │       ├── MainActivity.kt
│       │       └── platform/         # Android 专属组件
│       └── iosMain/                  # iOS 入口 + 平台组件
│           └── kotlin/
│               ├── MainViewController.kt
│               └── platform/         # iOS 专属组件
│
├── iosApp/                           # iOS 原生壳应用
│   ├── iosApp/
│   │   ├── Info.plist
│   │   ├── ContentView.swift
│   │   └── iOSApp.swift
│   └── iosApp.xcodeproj
│
└── convention-plugins/               # 构建约定插件
    └── src/main/kotlin/
        ├── kmp.library.gradle.kts
        └── compose.app.gradle.kts
```

### 4.2 依赖迁移表

| 类别 | 当前 (Android) | 目标 (KMP) | 迁移复杂度 | 备注 |
|------|---------------|-----------|-----------|------|
| **语言** | Kotlin 1.9.23 | Kotlin 2.1.x | 中 | 需升级以获取 Strong Skipping + Compose Compiler Plugin |
| **DI** | Hilt 2.50 | Koin 4.x | 高 | 全量重写 3 个 Module，但 Koin 更简洁 |
| **网络层** | Retrofit 2.9.0 + OkHttp 4.12.0 + Gson | Ktor 3.x + kotlinx.serialization | 高 | 需重写全部 API 定义和拦截器逻辑 |
| **JSON** | Gson (via Retrofit Converter) | kotlinx.serialization | 中 | 所有 DTO 需添加 `@Serializable` 注解 |
| **数据库** | Room 2.6.1 | SQLDelight 2.x | 中 | 需将 Entity/DAO 转写为 `.sq` 文件 |
| **本地存储** | SharedPreferences | multiplatform-settings 或 DataStore | 低 | 只有 `user_prefs` 一处 |
| **图片加载** | Coil 2.6.0 | Coil 3.x (KMP 版) | 低 | Coil 3.0+ 原生支持 KMP |
| **视频播放** | Media3 ExoPlayer 1.3.1 | expect/actual 封装 | 中 | Android = Media3, iOS = AVPlayer |
| **动画** | Lottie Compose 6.4.0 | lottie-compose-multiplatform | 低 | 或使用 Compose Animation API 替代 |
| **加载占位** | compose-shimmer 1.2.0 | compose-shimmer-multiplatform | 低 | 已有 KMP 分支 |
| **导航** | Navigation Compose 2.7.7 | Compose Navigation (Multiplatform) 或 Voyager | 中 | 建议升级到类型安全路由 |
| **Compose BOM** | 2024.10.00 | Compose Multiplatform 1.7.x+ | 中 | 由 JetBrains BOM 统一管理 |
| **构建注解** | KSP 1.9.23-1.0.19 | KSP 2.1.x-匹配版本 | 低 | 跟随 Kotlin 版本升级 |
| **AGP** | 8.13.2 | 保持不变 | 无 | |

### 4.3 领域层引入（Domain Layer）

当前项目 **完全没有领域层**，ViewModel 直接调用 Repository，Repository 返回 DTO。重构需要引入完整的 Clean Architecture 领域层。

#### 4.3.1 领域模型（Domain Models）

从当前 DTO 中提取纯业务模型，去除网络/数据库关注点：

```kotlin
// domain/model/AiApp.kt — 共享领域模型
data class AiApp(
    val id: String,
    val name: String,
    val description: String,
    val coverImageUrl: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val categoryTags: List<String>,
    val runCount: Long,
    val likeCount: Long,
    val isFeatured: Boolean
)

// domain/model/AppDetail.kt
data class AppDetail(
    val id: String,
    val name: String,
    val description: String,
    val coverImageUrl: String,
    val author: Creator,
    val inputNodes: List<InputNode>,
    val apiDemoCode: String,
    val runCount: Long,
    val likeCount: Long
)

// domain/model/InputNode.kt
data class InputNode(
    val nodeId: String,
    val fieldName: String,
    val fieldType: InputFieldType, // TEXT, IMAGE, VIDEO, COMBO
    val defaultValue: String,
    val options: List<String> // for COMBO type
)

// domain/model/TaskResult.kt
data class TaskResult(
    val taskId: String,
    val status: TaskStatus, // PENDING, RUNNING, COMPLETED, FAILED
    val outputs: List<TaskOutput>,
    val progress: Float, // 0.0 ~ 1.0
    val errorMessage: String?
)

// domain/model/User.kt
data class User(
    val id: String,
    val nickname: String,
    val avatarUrl: String,
    val bio: String,
    val followerCount: Long,
    val followingCount: Long,
    val publishedAppCount: Int,
    val accountBalance: Double
)

// domain/model/Creator.kt
data class Creator(
    val id: String,
    val nickname: String,
    val avatarUrl: String,
    val bio: String,
    val followerCount: Long,
    val isFollowed: Boolean
)

// domain/model/Category.kt
data class Category(
    val id: String,
    val name: String,
    val parentId: String?,
    val level: Int,
    val children: List<Category>,
    val labels: String? // "IOS" = iOS 可见
)
```

#### 4.3.2 UseCase 清单

| UseCase | 输入 | 输出 | 说明 |
|---------|------|------|------|
| `GetDiscoveryFeedUseCase` | page, categoryId | `Flow<PagingData<AiApp>>` | 分页获取发现页应用列表 |
| `GetFeaturedAppsUseCase` | 无 | `Flow<List<AiApp>>` | 获取精选应用（Banner） |
| `GetCategoryTreeUseCase` | 无 | `Flow<List<Category>>` | 获取分类标签树 |
| `GetAppDetailUseCase` | appId | `Flow<AppDetail>` | 获取应用详情 |
| `RunTaskUseCase` | appId, inputs | `Flow<TaskResult>` | 执行任务并轮询状态 |
| `UploadFileUseCase` | fileBytes, fileType | `Flow<UploadResult>` | 上传文件 |
| `SearchAppsUseCase` | query, page | `Flow<PagingData<AiApp>>` | 搜索应用 |
| `GetUserProfileUseCase` | 无 | `Flow<User>` | 获取当前用户信息 |
| `GetCreatorProfileUseCase` | userId | `Flow<Creator>` | 获取创作者信息 |
| `ToggleFollowUseCase` | userId | `Result<Boolean>` | 关注/取关 |
| `ManageApiKeyUseCase` | apiKey, action | `Result<Unit>` | 绑定/解绑 API Key |
| `GetAccountStatusUseCase` | apiKey | `Flow<AccountStatus>` | 查询账户状态 |
| `GetUserAppsUseCase` | userId, page | `Flow<PagingData<AiApp>>` | 获取用户发布的应用 |
| `GetTaskHistoryUseCase` | 无 | `Flow<List<TaskHistoryItem>>` | 获取任务历史 |
| `GenerateAudioUseCase` | text, voiceId | `Flow<TaskResult>` | TTS 音频生成 |
| `DecodeSteganographyUseCase` | imageBytes | `Flow<DecodeResult>` | LSB 隐写解码 |

#### 4.3.3 Mapper 规范

```kotlin
// data/remote/mapper/AppMapper.kt
interface DtoMapper<DTO, DOMAIN> {
    fun toDomain(dto: DTO): DOMAIN
    fun fromDomain(domain: DOMAIN): DTO // 仅需双向时实现
}

class WebAppDtoMapper : DtoMapper<WebAppDto, AiApp> {
    override fun toDomain(dto: WebAppDto) = AiApp(
        id = dto.id,
        name = dto.name ?: "",
        description = dto.appDetail ?: "",
        coverImageUrl = dto.image ?: "",
        // ...
    )
}
```

### 4.4 Repository 接口在 Domain 层定义

```kotlin
// domain/repository/AppRepository.kt
interface AppRepository {
    fun getDiscoveryFeed(page: Int, categoryId: String?): Flow<List<AiApp>>
    fun getFeaturedApps(): Flow<List<AiApp>>
    fun getAppDetail(appId: String): Flow<AppDetail>
    fun searchApps(query: String, page: Int): Flow<List<AiApp>>
    suspend fun runTask(appId: String, inputs: Map<String, String>): TaskResult
    suspend fun getTaskOutputs(taskId: String): List<TaskOutput>
}

// domain/repository/UserRepository.kt
interface UserRepository {
    fun getCurrentUser(): Flow<User>
    fun getCreatorProfile(userId: String): Flow<Creator>
    suspend fun toggleFollow(userId: String): Boolean
    suspend fun bindApiKey(apiKey: String)
    suspend fun unbindApiKey()
    fun getAccountStatus(apiKey: String): Flow<AccountStatus>
}

// domain/repository/AudioRepository.kt
interface AudioRepository {
    suspend fun generateAudio(text: String, voiceId: String): String
    fun getTaskStatus(taskId: String): Flow<TaskResult>
}
```

---

## 5. 设计系统需求

### 5.1 现状问题

审计发现的设计系统问题：
- **60+ 硬编码颜色**：`Color(0xFFXXXXXX)` 散布在 16+ 个文件中
- **仅定义 11 个 Color Token**，实际使用时大量绕过 `MaterialTheme.colorScheme`
- **硬编码排版**：`fontSize = 24.sp, fontWeight = FontWeight.Bold` 替代 `MaterialTheme.typography`
- **硬编码间距**：`padding(16.dp)`, `padding(12.dp)` 等无语义化间距
- **仅有暗色主题**：`DarkColorScheme` 一套，无亮色主题
- **硬编码中文字符串**：阻碍国际化

### 5.2 统一颜色 Token 体系

#### 5.2.1 品牌色

| Token 名 | 暗色值 | 亮色值 | 用途 |
|----------|--------|--------|------|
| `brand.primary` | `#00FFC2` | `#00D9A5` | 主品牌色 (RunningHub Teal) |
| `brand.primaryVariant` | `#00CC9A` | `#00B88A` | 主品牌色变体 |
| `brand.onPrimary` | `#0D0D0D` | `#FFFFFF` | 主品牌色上的文字/图标 |

#### 5.2.2 表面色

| Token 名 | 暗色值 | 亮色值 | 用途 |
|----------|--------|--------|------|
| `surface.background` | `#0D0D0D` | `#F8F9FA` | 页面背景 |
| `surface.card` | `#1A1A1A` | `#FFFFFF` | 卡片/容器背景 |
| `surface.cardElevated` | `#242424` | `#F0F0F0` | 提升卡片背景 |
| `surface.overlay` | `#2C2C2C` | `#E8E8E8` | 覆盖层/分隔线 |
| `surface.input` | `#1A1A1A` | `#F5F5F5` | 输入框背景 |

#### 5.2.3 文字色

| Token 名 | 暗色值 | 亮色值 | 用途 |
|----------|--------|--------|------|
| `text.primary` | `#FFFFFF` | `#0F172A` | 主要文字 |
| `text.secondary` | `#94A3B8` | `#64748B` | 辅助文字 |
| `text.tertiary` | `#64748B` | `#94A3B8` | 占位/提示文字 |
| `text.disabled` | `#475569` | `#CBD5E1` | 禁用文字 |
| `text.link` | `#00FFC2` | `#00B88A` | 链接文字 |

#### 5.2.4 语义色

| Token 名 | 暗色值 | 亮色值 | 用途 |
|----------|--------|--------|------|
| `semantic.success` | `#22C55E` | `#16A34A` | 成功状态 |
| `semantic.warning` | `#FFCC00` | `#EAB308` | 警告状态 |
| `semantic.error` | `#EF4444` | `#DC2626` | 错误状态 |
| `semantic.info` | `#3B82F6` | `#2563EB` | 信息提示 |

#### 5.2.5 特殊场景色

| Token 名 | 暗色值 | 亮色值 | 用途 |
|----------|--------|--------|------|
| `fab.background` | `#FFCC00` | `#FFCC00` | FAB 按钮背景 |
| `fab.onBackground` | `#0D0D0D` | `#0D0D0D` | FAB 按钮图标 |
| `shimmer.base` | `#1A1A1A` | `#E0E0E0` | 骨架屏基色 |
| `shimmer.highlight` | `#2C2C2C` | `#F5F5F5` | 骨架屏高亮 |
| `badge.new` | `#EF4444` | `#DC2626` | 新功能徽标 |
| `tag.background` | `#1A1A1A80` | `#F0F0F080` | 标签背景 |

### 5.3 排版系统

基于 Material3 Type Scale，适配中英文混排：

| Token 名 | 字号 | 字重 | 行高 | 用途 |
|----------|------|------|------|------|
| `display.large` | 34sp | Bold | 42sp | 大标题（未使用） |
| `display.medium` | 28sp | Bold | 36sp | 页面标题 |
| `headline.large` | 24sp | Bold | 32sp | Banner 标题 |
| `headline.medium` | 20sp | SemiBold | 28sp | 分区标题 |
| `title.large` | 18sp | SemiBold | 26sp | 卡片标题 |
| `title.medium` | 16sp | Medium | 24sp | 列表项标题 |
| `body.large` | 16sp | Regular | 24sp | 正文 |
| `body.medium` | 14sp | Regular | 20sp | 副文 |
| `body.small` | 12sp | Regular | 16sp | 辅助文字 |
| `label.large` | 14sp | Medium | 20sp | 按钮文字 |
| `label.medium` | 12sp | Medium | 16sp | Tab/标签文字 |
| `label.small` | 10sp | Medium | 14sp | 角标/徽标 |

**字体栈**：
- Android: `sans-serif` (Roboto) / 中文系统字体
- iOS: `SF Pro` / `PingFang SC`
- 通过 `expect/actual` 提供平台字体 Family

### 5.4 间距系统

采用 4dp 基准网格：

| Token 名 | 值 | 用途示例 |
|----------|------|---------|
| `spacing.xxs` | 2dp | 图标与文字间距 |
| `spacing.xs` | 4dp | 紧凑内边距 |
| `spacing.sm` | 8dp | 小间距 |
| `spacing.md` | 12dp | 列表项间距 |
| `spacing.lg` | 16dp | 标准内边距、卡片间距 |
| `spacing.xl` | 20dp | 分区间距 |
| `spacing.xxl` | 24dp | 页面边距 |
| `spacing.xxxl` | 32dp | 大分区间距 |
| `spacing.huge` | 48dp | 页面顶部间距 |

**圆角系统**：

| Token 名 | 值 | 用途 |
|----------|------|------|
| `radius.none` | 0dp | 无圆角 |
| `radius.xs` | 4dp | 小标签 |
| `radius.sm` | 8dp | 输入框、小卡片 |
| `radius.md` | 12dp | 标准卡片 |
| `radius.lg` | 16dp | 大卡片、Bottom Sheet |
| `radius.xl` | 24dp | 对话框 |
| `radius.full` | 999dp | 胶囊形按钮/头像 |

### 5.5 共享组件库

#### 5.5.1 基础组件

| 组件 | 当前状态 | 需要的变更 |
|------|---------|-----------|
| `RhButton` | 无，使用 Material3 Button | 新增：Primary/Secondary/Ghost/FAB 四种变体 |
| `RhIconButton` | 无 | 新增：统一图标按钮 |
| `RhTextField` | 无，使用 Material3 | 新增：统一输入框样式 |
| `RhCard` | 无，使用 Material3 Surface | 新增：统一卡片容器 |
| `RhChip` | 无，使用手动 Surface | 新增：分类标签 Chip |
| `RhBadge` | 无 | 新增：数字/圆点徽标 |
| `RhDivider` | 无 | 新增：分隔线 |
| `RhLoadingIndicator` | 无统一方案 | 新增：CircularProgress + Shimmer |

#### 5.5.2 业务组件

| 组件 | 当前状态 | 需要的变更 |
|------|---------|-----------|
| `SmartAsyncImage` | 已有，有 modifier | 迁移到 Coil 3.x KMP，添加 @Preview |
| `AiAppCard` | 已有(AiAppMasonryItem)，modifier 无默认值 | 重构：添加 `modifier = Modifier`，提取设计 token |
| `BannerCarousel` | 已有(BannerSection)，缺 modifier | 重构：独立组件化，添加 modifier，自动轮播修复 |
| `CategoryTabRow` | 已有(CategoryPills)，缺 modifier | 重构：独立组件化，添加 modifier 和 key |
| `BottomNavBar` | 已有(DiscoveryBottomNav)，缺 modifier | 重构：平台适配(Android: M3, iOS: Tab Bar 风格) |
| `FabMenuOverlay` | 已有，缺 modifier | 重构：添加 modifier，提取硬编码 ListOf |
| `VideoPlayerView` | 已有(VideoPlayer)，有 modifier | 重构：expect/actual 封装 |
| `CreatorInfoHeader` | 已有(CreatorHeader)，缺 modifier | 重构：独立组件化 |
| `StatItem` | 已有，缺 modifier | 重构：添加 modifier |
| `RunActionButton` | 已有，缺 modifier | 重构：添加 modifier，使用设计 token |
| `TaskResultView` | 分散在 AppDetailScreen 内 | 新增：独立任务结果展示组件 |
| `InputFormField` | 分散在 AppDetailScreen 内 | 新增：统一输入表单字段组件 |
| `UserAvatar` | 无 | 新增：统一头像组件(圆形/带边框) |
| `EmptyStateView` | 硬编码文字 | 新增：统一空状态组件(图标+文字+操作) |
| `ErrorStateView` | 无 | 新增：统一错误状态组件 |
| `PullToRefreshWrapper` | 使用 Material3 内置 | 新增：跨平台下拉刷新包装 |

---

## 6. 质量需求

### 6.1 Compose 审计目标

| 类别 | 当前 | 目标 | 关键措施 |
|------|------|------|---------|
| Performance | 4/10 | 8/10 | 启用 R8 + 基准配置文件、Kotlin 2.1+ Strong Skipping、@Immutable 注解、ImmutableList、remember 缓存 |
| State Management | 6/10 | 8/10 | 全部使用 collectAsStateWithLifecycle、rememberSaveable 保存 UI 状态、ViewModel 在 NavGraph 入口获取 |
| Side Effects | 7/10 | 9/10 | 修复 backwards write、LaunchedEffect key 正确性、rememberUpdatedState |
| API Quality | 5/10 | 8/10 | 所有组件 `modifier = Modifier`、@Preview 覆盖、设计 token 替换硬编码、stringResource i18n |
| **总分** | **53/100** | **80+/100** | |

### 6.2 测试覆盖率目标

| 测试类型 | 当前 | 目标 | 工具 |
|---------|------|------|------|
| 单元测试 (Domain) | 0% | 80% | kotlin.test + kotest |
| 单元测试 (Data) | 0% | 60% | kotlin.test + MockK/Fakes |
| 单元测试 (ViewModel) | 0% | 70% | Turbine (Flow 测试) + kotlin.test |
| UI 测试 (Compose) | 0% | 40% | compose.ui.test (Multiplatform) |
| 集成测试 | 0% | 30% | Ktor MockEngine |
| **总体覆盖率** | **0%** | **60%+** | |

### 6.3 性能指标

| 指标 | Android 目标 | iOS 目标 | 测量工具 |
|------|-------------|----------|---------|
| 冷启动 | < 800ms | < 1000ms | Macrobenchmark / Instruments |
| 首屏渲染 (TTID) | < 500ms | < 600ms | Macrobenchmark / MetricKit |
| 列表滚动帧率 | ≥ 58fps (P95) | ≥ 58fps (P95) | GPU Profiler |
| 内存峰值 (发现页) | < 150MB | < 150MB | Profiler / Instruments |
| APK 大小 | < 25MB | — | Bundletool |
| IPA 大小 | — | < 40MB | Xcode Organizer |
| 网络请求首字节 | < 200ms (P95) | < 200ms (P95) | OkHttp/Ktor 日志 |
| 图片加载 (缓存命中) | < 50ms | < 50ms | Coil metrics |
| 图片加载 (网络) | < 1500ms | < 1500ms | Coil metrics |

### 6.4 代码质量标准

| 标准 | 要求 |
|------|------|
| Kotlin 版本 | 2.1.x (Compose Compiler Plugin 内置) |
| 编译警告 | 零警告 (warnings as errors) |
| Detekt 规则 | 通过默认规则集 + 自定义 Compose 规则 |
| API 兼容性 | binary-compatibility-validator 保护 shared 模块 |
| Git 提交规范 | Conventional Commits |
| PR 要求 | ≥1 reviewer + CI 全绿 |

---

## 7. 阶段计划

### Phase 1: 项目骨架搭建（第 1-4 周）

**目标**: 建立 KMP 项目结构，CI/CD 通过，空壳应用双平台可运行

| 任务 | 优先级 | 工时 | 产出 |
|------|--------|------|------|
| 创建 KMP + Compose Multiplatform 项目骨架 | P0 | 3d | `shared/` + `composeApp/` + `iosApp/` 模块 |
| 配置 `gradle/libs.versions.toml` 版本目录 | P0 | 1d | 统一依赖版本管理 |
| 升级 Kotlin 到 2.1.x + Compose Compiler Plugin | P0 | 2d | Strong Skipping 默认启用 |
| 配置 CI/CD (GitHub Actions) | P0 | 2d | Android 构建 + iOS 构建 + 测试 + Lint |
| 创建设计系统 Token 文件 | P0 | 3d | Color/Typography/Spacing Token |
| 配置 Detekt + Compose Lint 规则 | P1 | 1d | 代码质量基线 |
| 创建 `convention-plugins` 统一构建配置 | P1 | 2d | kmp.library + compose.app 插件 |

**里程碑 M0**: 双平台空壳应用编译运行成功 + CI 全绿

### Phase 2: 共享基础设施（第 5-8 周）

**目标**: 完成网络层、本地存储、DI 框架迁移

| 任务 | 优先级 | 工时 | 产出 |
|------|--------|------|------|
| Hilt → Koin 迁移 | P0 | 3d | `shared/di/` + `composeApp/di/` Koin 模块 |
| Retrofit → Ktor 迁移 | P0 | 5d | 全部 API 接口 + 拦截器逻辑 |
| Gson → kotlinx.serialization 迁移 | P0 | 3d | 全部 DTO @Serializable |
| SharedPreferences → multiplatform-settings | P0 | 2d | UserPreferences 跨平台 |
| Room → SQLDelight 迁移 | P1 | 3d | Discovery 缓存 .sq 文件 |
| Coil 2 → Coil 3 (KMP) 迁移 | P1 | 2d | SmartAsyncImage 跨平台 |
| 为全部基础设施编写单元测试 | P0 | 5d | Ktor MockEngine 测试 + Settings 测试 |

**里程碑 M1-a**: 网络请求在双平台成功返回数据

### Phase 3: 领域层构建（第 9-12 周）

**目标**: 引入 Domain 层，建立 Clean Architecture

| 任务 | 优先级 | 工时 | 产出 |
|------|--------|------|------|
| 定义全部领域模型 (Domain Models) | P0 | 3d | 15+ 领域模型类 |
| 实现 DTO → Domain Mapper | P0 | 3d | 全部 Mapper 实现 |
| 定义 Repository 接口 (domain/repository/) | P0 | 2d | AppRepository, UserRepository, AudioRepository |
| 实现 Repository (data/repository/) | P0 | 5d | 实现全部 Repository 接口 |
| 实现全部 UseCase | P0 | 5d | 16 个 UseCase |
| UseCase 单元测试 | P0 | 5d | 80% 覆盖 Domain 层 |

**里程碑 M1-b**: Domain 层完成，全部 UseCase 测试通过

### Phase 4: 设计系统实现（第 13-16 周）

**目标**: 完成跨平台设计系统和组件库

| 任务 | 优先级 | 工时 | 产出 |
|------|--------|------|------|
| 实现 `RunningHubTheme` (暗色 + 亮色) | P0 | 3d | 跨平台主题系统 |
| 替换全部 60+ 硬编码颜色 | P0 | 3d | 零硬编码颜色 |
| 替换全部硬编码排版 | P0 | 2d | 统一使用 Typography token |
| 替换全部硬编码间距 | P1 | 2d | 统一使用 Spacing token |
| 实现 8 个基础组件 | P0 | 5d | RhButton, RhCard, RhChip... |
| 迁移 8 个业务组件到共享组件库 | P0 | 5d | SmartAsyncImage, AiAppCard... |
| 新增 5 个业务组件 | P1 | 3d | EmptyStateView, ErrorStateView... |
| 为所有组件添加 @Preview | P1 | 2d | Preview 覆盖全部组件 |

**里程碑 M2-a**: 设计系统组件库完成，组件 Preview 全部可渲染

### Phase 5: 核心功能屏幕迁移（第 17-22 周）

**目标**: Discovery + Profile + Search 屏幕双平台运行

| 任务 | 优先级 | 工时 | 产出 |
|------|--------|------|------|
| 迁移 DiscoveryScreen (发现页) | P0 | 5d | 跨平台发现页 |
| 迁移 ProfileScreen (个人中心) | P0 | 4d | 跨平台个人中心 |
| 迁移 SearchScreen (搜索) | P0 | 2d | 跨平台搜索页 |
| 实现跨平台导航图 | P0 | 3d | 底部导航 + 路由 |
| 实现下拉刷新 + 无限滚动 | P0 | 3d | 跨平台分页加载 |
| 迁移 TaskHistoryScreen | P1 | 2d | 跨平台任务历史 |
| 为 ViewModel 编写测试 (Turbine) | P0 | 5d | DiscoveryVM, ProfileVM 测试 |
| iOS 平台适配 (Safe Area, 手势) | P0 | 3d | iOS 原生交互体验 |

**里程碑 M2-b**: Discovery + Profile + Search 在 Android/iOS 双平台运行

### Phase 6: 详情与社交功能迁移（第 23-28 周）

**目标**: AppDetail + CreatorProfile + Community 双平台运行

| 任务 | 优先级 | 工时 | 产出 |
|------|--------|------|------|
| 迁移 AppDetailScreen (应用详情) | P0 | 5d | 跨平台应用详情 |
| 实现跨平台文件上传 (expect/actual) | P0 | 4d | 图片/视频选择 + 上传 |
| 实现跨平台视频播放 (expect/actual) | P0 | 4d | Android Media3 / iOS AVPlayer |
| 迁移任务执行 + 轮询逻辑 | P0 | 3d | RunTask + 输出展示 |
| 迁移 CreatorProfileScreen | P1 | 3d | 跨平台创作者主页 |
| 迁移 CommunityScreen | P1 | 2d | 跨平台社区页 |
| ViewModel 测试 | P0 | 4d | AppDetailVM, CreatorVM 测试 |

**里程碑 M3-a**: 应用详情页含任务执行全流程双平台跑通

### Phase 7: 工具类功能迁移（第 29-32 周）

**目标**: AudioGeneration + SecretDecode + UiInspector 双平台运行

| 任务 | 优先级 | 工时 | 产出 |
|------|--------|------|------|
| 迁移 AudioGenerationScreen | P1 | 4d | 跨平台 TTS 功能 |
| 实现跨平台音频播放 (expect/actual) | P1 | 3d | Android Media3 / iOS AVAudioPlayer |
| 迁移 SecretDecodeScreen | P2 | 4d | 跨平台隐写解码 |
| 实现跨平台图像像素操作 (expect/actual) | P2 | 3d | Android Bitmap / iOS CoreGraphics |
| 迁移 UiInspectorScreen | P2 | 1d | 跨平台工具页 |
| FAB 菜单覆盖层迁移 | P1 | 2d | 跨平台 FAB 菜单 |
| 集成测试 | P1 | 3d | 全链路集成测试 |

**里程碑 M3-b**: 全部 9 个功能模块双平台交付

### Phase 8: 质量优化（第 33-40 周）

**目标**: 达到质量基线要求

| 任务 | 优先级 | 工时 | 产出 |
|------|--------|------|------|
| 启用 R8 + ProGuard 规则调试 | P0 | 3d | Release 包体优化 |
| 基准配置文件 (Baseline Profile) | P0 | 3d | 启动 + 滚动性能优化 |
| 稳定性注解全量添加 | P0 | 3d | @Immutable + ImmutableList |
| 全部 lazy list 添加 key | P0 | 1d | 列表性能优化 |
| 修复全部 collectAsState 为生命周期感知版本 | P0 | 1d | 状态管理优化 |
| 添加 rememberSaveable | P1 | 2d | 配置变更恢复 |
| 性能测试 + Profiling | P0 | 5d | 满足 6.3 性能指标 |
| 补充测试覆盖至 60%+ | P0 | 10d | 达到测试覆盖目标 |
| 国际化 (stringResource) | P1 | 5d | 中/英双语支持 |
| 无障碍 (Accessibility) | P2 | 3d | contentDescription + 焦点管理 |
| Compose 审计复审 | P0 | 2d | 验证评分 80+ |

**里程碑 M4**: Compose 审计 80+/100, 测试覆盖 60%+

### Phase 9: 发布准备（第 41-44 周）

**目标**: 双平台应用商店上架

| 任务 | 优先级 | 工时 | 产出 |
|------|--------|------|------|
| Android 签名 + Google Play 配置 | P0 | 2d | 生产签名密钥 + 商店资料 |
| iOS 证书 + App Store Connect 配置 | P0 | 3d | 证书 + Provisioning Profile |
| 灰度发布 (Android) | P0 | 3d | 内部测试 → 封闭测试 → 公开 |
| TestFlight 内测 (iOS) | P0 | 3d | TestFlight 分发 |
| 崩溃监控集成 (Crashlytics/Sentry) | P0 | 2d | 双平台崩溃上报 |
| 分析埋点 (Firebase/自研) | P1 | 3d | 核心事件埋点 |
| 热修复方案调研 | P2 | 2d | Android 热修复预案 |
| 正式上架 | P0 | 2d | 双平台正式发布 |

**里程碑 M5**: Google Play + App Store 双平台上架

---

## 8. 功能验收标准

### F1 发现页（主页）

| AC 编号 | 验收标准 | 测试方法 |
|---------|---------|---------|
| F1-AC1 | Banner 轮播展示精选应用，3 秒自动切换，手动左右滑动，指示器同步 | UI 测试 + 手动测试 |
| F1-AC2 | 分类标签横向滚动，支持 21 个一级分类切换，切换后列表刷新 | UI 测试 |
| F1-AC3 | 瀑布流双列展示 AI 应用，卡片包含封面图、名称、作者头像、运行次数 | UI 测试 |
| F1-AC4 | 下拉刷新回到首页，加载指示器显示 | UI 测试 |
| F1-AC5 | 滚动到底部自动加载下一页，加载中显示进度指示 | UI 测试 |
| F1-AC6 | 底部导航栏正确高亮当前 Tab，切换 Tab 保持状态 | UI 测试 |
| F1-AC7 | FAB 菜单点击展开覆盖层，包含工具入口 | UI 测试 |
| F1-AC8 | Android/iOS 双平台视觉一致（颜色 Token、间距、排版一致） | 截图对比 |
| F1-AC9 | 列表滚动帧率 ≥ 58fps (P95) | 性能测试 |
| F1-AC10 | 空状态/错误状态正确展示，包含重试操作 | UI 测试 |

### F2 应用详情

| AC 编号 | 验收标准 | 测试方法 |
|---------|---------|---------|
| F2-AC1 | 展示应用名称、描述、封面图、作者信息、运行次数、点赞数 | UI 测试 |
| F2-AC2 | API Demo 代码正确展示 | UI 测试 |
| F2-AC3 | 动态表单根据 inputNodes 渲染：TEXT 显示输入框，IMAGE 显示图片选择器，VIDEO 显示视频选择器，COMBO 显示下拉框 | UI 测试 |
| F2-AC4 | 图片上传：选择图片 → 上传 → 显示预览 → 返回文件 URL | 集成测试 |
| F2-AC5 | 视频上传：选择视频 → 上传进度 → 显示预览 → 返回文件 URL | 集成测试 |
| F2-AC6 | 点击运行按钮：发起任务 → 显示轮询进度 → 展示输出结果 | 集成测试 |
| F2-AC7 | 任务输出为图片时正确展示，可长按保存（平台适配） | 手动测试 |
| F2-AC8 | 任务输出为视频时自动播放，Android 使用 Media3，iOS 使用 AVPlayer | 手动测试 |
| F2-AC9 | 点击作者头像/名称跳转到创作者主页 | UI 测试 |
| F2-AC10 | 返回按钮正确 popBackStack | UI 测试 |

### F3 搜索

| AC 编号 | 验收标准 | 测试方法 |
|---------|---------|---------|
| F3-AC1 | 搜索输入框聚焦时键盘弹出 | UI 测试 |
| F3-AC2 | 输入关键词实时搜索（debounce 300ms）或手动搜索 | 单元测试 |
| F3-AC3 | 搜索结果以列表/网格展示 | UI 测试 |
| F3-AC4 | 点击搜索结果跳转到应用详情 | UI 测试 |
| F3-AC5 | 空结果显示友好提示 | UI 测试 |

### F4 个人中心

| AC 编号 | 验收标准 | 测试方法 |
|---------|---------|---------|
| F4-AC1 | 展示用户头像、昵称、简介 | UI 测试 |
| F4-AC2 | API Key 绑定：输入 Key → 保存 → 显示已绑定状态 | 集成测试 |
| F4-AC3 | 企业 API Key 绑定：独立输入 → 保存 | 集成测试 |
| F4-AC4 | API Key 解绑：确认弹窗 → 清除 → 显示未绑定状态 | UI 测试 |
| F4-AC5 | 账户状态展示：余额、已用量、配额 | UI 测试 |
| F4-AC6 | 已发布应用列表展示，点击跳转应用详情 | UI 测试 |
| F4-AC7 | 点击任务历史跳转到历史页面 | UI 测试 |
| F4-AC8 | API Key 加密存储（multiplatform-settings + EncryptedPreferences） | 安全测试 |

### F5 创作者主页

| AC 编号 | 验收标准 | 测试方法 |
|---------|---------|---------|
| F5-AC1 | 展示创作者头像、昵称、简介、粉丝数 | UI 测试 |
| F5-AC2 | 关注按钮状态正确：已关注/未关注 | 单元测试 |
| F5-AC3 | 点击关注/取关后按钮状态切换，粉丝数更新 | 集成测试 |
| F5-AC4 | 创作者发布的应用列表展示，支持无限滚动 | UI 测试 |
| F5-AC5 | 点击应用卡片跳转到应用详情 | UI 测试 |

### F6 社区

| AC 编号 | 验收标准 | 测试方法 |
|---------|---------|---------|
| F6-AC1 | 工具卡片网格展示所有可用工具 | UI 测试 |
| F6-AC2 | 分类标签过滤：音频处理、UI Inspector、隐写解码 | UI 测试 |
| F6-AC3 | 点击工具卡片导航到对应工具页面 | UI 测试 |
| F6-AC4 | 底部导航栏正确高亮 Community Tab | UI 测试 |

### F7 音频生成

| AC 编号 | 验收标准 | 测试方法 |
|---------|---------|---------|
| F7-AC1 | 输入文本 → 选择音色 → 点击生成 | UI 测试 |
| F7-AC2 | 任务轮询显示进度 | 集成测试 |
| F7-AC3 | 生成完成后可播放音频，Android/iOS 均可播放 | 手动测试 |
| F7-AC4 | 返回按钮正确工作 | UI 测试 |

### F8 隐写解码

| AC 编号 | 验收标准 | 测试方法 |
|---------|---------|---------|
| F8-AC1 | 选择图片（从相册/相机）| 手动测试 |
| F8-AC2 | LSB 解码正确提取隐藏信息 | 单元测试 |
| F8-AC3 | 解码结果正确展示 | UI 测试 |
| F8-AC4 | Android 使用 Bitmap、iOS 使用 CoreGraphics，结果一致 | 交叉验证 |

### F9 UI Inspector

| AC 编号 | 验收标准 | 测试方法 |
|---------|---------|---------|
| F9-AC1 | 工具页面正确渲染 | UI 测试 |
| F9-AC2 | 返回导航正确 | UI 测试 |

---

## 9. 风险与缓解

| # | 风险 | 概率 | 影响 | 缓解措施 |
|---|------|------|------|---------|
| R1 | Compose Multiplatform iOS 稳定性不足 | 中 | 高 | 保持最新稳定版，关注 JetBrains 发布周期；回退方案：iOS 使用 SwiftUI 壳 + shared KMP 逻辑 |
| R2 | Ktor 在 iOS 上网络性能低于预期 | 低 | 中 | 使用 Darwin engine；性能测试前置到 Phase 2 |
| R3 | SQLDelight 迁移数据丢失 | 低 | 高 | 数据迁移脚本 + 全量数据备份 + Room 并行运行过渡期 |
| R4 | 视频播放 expect/actual 维护成本高 | 中 | 中 | 封装统一 VideoPlayer 接口，各平台实现独立维护 |
| R5 | 工期超出预估 | 中 | 中 | 按 Phase 交付，每个 Phase 独立可发布；Phase 7 (工具类) 可推迟 |
| R6 | iOS 审核政策不确定性 | 低 | 高 | 提前准备审核资料，预留 2 周审核缓冲 |
| R7 | 团队 KMP/iOS 技术储备不足 | 中 | 中 | Phase 1 安排技术培训，结对编程 Android+iOS 开发 |

---

## 10. 附录

### 10.1 术语表

| 术语 | 含义 |
|------|------|
| KMP | Kotlin Multiplatform — JetBrains 跨平台方案 |
| CMP | Compose Multiplatform — Compose UI 跨平台框架 |
| DTO | Data Transfer Object — 网络传输数据对象 |
| Domain Model | 领域模型 — 纯业务数据，不含序列化/数据库注解 |
| UseCase | 用例 — 封装单一业务操作 |
| Mapper | 映射器 — DTO↔Domain 转换 |
| expect/actual | KMP 平台声明机制 — 共享接口+平台实现 |
| Strong Skipping | Compose 编译器优化 — Kotlin 2.0+ 默认跳过未变更组件 |

### 10.2 参考文档

- [KMP 官方文档](https://www.jetbrains.com/kotlin-multiplatform/)
- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
- [Ktor 文档](https://ktor.io/docs/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Koin](https://insert-koin.io/)
- [Coil KMP](https://coil-kt.github.io/coil/)
- [Android Compose 性能](https://developer.android.com/develop/ui/compose/performance)
- [Android Compose 稳定性](https://developer.android.com/develop/ui/compose/performance/stability)

### 10.3 当前 API 端点清单

| # | 方法 | 端点 | 用途 |
|---|------|------|------|
| 1 | POST | `/api/webapp/list` | 获取应用列表（分页） |
| 2 | POST | `/api/webapp/carefullyChosenList` | 获取精选应用（Banner） |
| 3 | POST | `/api/webapp/user/list` | 获取用户发布的应用列表 |
| 4 | POST | `/api/portal/tag/tree` | 获取分类标签树 |
| 5 | GET | `/api/webapp/apiCallDemo` | 获取应用 API Demo |
| 6 | POST | `/api/webapp/detail` | 获取应用详情 |
| 7 | POST | `/task/openapi/ai-app/run` | 执行 AI 应用任务 |
| 8 | POST | `/task/openapi/outputs` | 查询任务输出 |
| 9 | POST | `/task/openapi/upload` | 上传文件（Multipart） |
| 10 | POST | `/uc/openapi/accountStatus` | 查询账户状态 |
| 11 | POST | `/uc/getUserInfo` | 获取用户信息 |
| 12 | POST | `/uc/follow/isFollow` | 查询关注状态 |
| 13 | POST | `/uc/follow/followUser` | 关注用户 |
| 14 | POST | `/uc/follow/unFollowUser` | 取消关注 |
| 15 | POST | (MiniMax TTS API) | 音频生成 |
| 16 | POST | (MiniMax Query API) | 音频任务查询 |

### 10.4 当前应用分类体系

后端共提供 **21 个一级分类**，**100+ 个二级分类**：

| # | 一级分类 | 二级分类数 | iOS 标记 |
|---|---------|-----------|---------|
| 1 | 数字人 | 1 | - |
| 2 | 图片生成 | 3 | IOS |
| 3 | 视频生成 | 3 | IOS |
| 4 | 视频特效 | 6 | - |
| 5 | 二次元 | 13 | - |
| 6 | 风格转换 | 20 | - |
| 7 | 海报 | 2 | IOS |
| 8 | 音频生成 | 1 | IOS |
| 9 | 图片处理 | 15 | - |
| 10 | 摄影 | 8 | - |
| 11 | 影视游戏 | 8 | IOS |
| 12 | 3D模型 | 7 | - |
| 13 | 创意玩法 | 5 | - |
| 14 | 平面设计 | 4 | IOS |
| 15 | 电商产品 | 8 | IOS |
| 16 | 室内外设计 | 6 | IOS |
| 17 | 风格画作 | 11 | IOS |
| 18 | API | 20 | - |
| 19 | AI漫剧 | 4 | - |
| 20 | 视频处理 | 9 | - |
| 21 | 其他 | 3 | - |

> 注：`labels: "IOS"` 标记的分类在后端已预标记为 iOS 可见，可作为 iOS 版分类过滤依据。

---

> **文档结束** — 本 PRD 基于 Phase 0 审计数据和源码分析生成，将随项目推进持续更新。
