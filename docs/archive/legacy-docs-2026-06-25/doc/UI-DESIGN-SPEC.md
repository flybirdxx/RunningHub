# RunningHub UI/UX 设计规范 v1.0

> **目标平台**: Android (Material3) + iOS (Cupertino 适配) via Compose Multiplatform  
> **品牌主色**: RunningHubTeal `#00FFC2`  
> **设计基调**: 暗色科技感、简洁高效、内容优先  
> **文档版本**: 1.0 | 2026-04-25

---

## 一、设计令牌系统 (Design Tokens)

### 1.1 色彩系统

#### 品牌色阶 (基于 RunningHubTeal #00FFC2)

| 令牌名              | 浅色模式     | 暗色模式     | 用途                 |
|---------------------|-------------|-------------|---------------------|
| `Teal50`            | `#E0FFF5`   | `#E0FFF5`   | 极浅背景高亮          |
| `Teal100`           | `#B3FFE6`   | `#B3FFE6`   | 标签/芯片浅背景       |
| `Teal200`           | `#80FFD6`   | `#80FFD6`   | hover 状态           |
| `Teal300`           | `#4DFFC7`   | `#4DFFC7`   | 次要强调             |
| `Teal400`           | `#1AFFB8`   | `#1AFFB8`   | 活跃指示器           |
| `Teal500`           | `#00FFC2`   | `#00FFC2`   | **品牌主色/Primary**  |
| `Teal600`           | `#00D9A5`   | `#00D9A5`   | 按下态               |
| `Teal700`           | `#00B388`   | `#00B388`   | 深层操作             |
| `Teal800`           | `#008C6B`   | `#008C6B`   | 深色文字/图标         |
| `Teal900`           | `#00664E`   | `#00664E`   | 最深强调             |

#### Material3 ColorScheme 映射

```
暗色模式 (DarkColorScheme) — 当前默认
─────────────────────────────────────────────────────────
令牌                    色值             Kotlin 变量名
─────────────────────────────────────────────────────────
primary               #00FFC2          RunningHubTeal
onPrimary             #003829          DarkOnPrimary
primaryContainer      #005140          DarkPrimaryContainer
onPrimaryContainer    #80FFD6          DarkOnPrimaryContainer

secondary             #B0CCC4          DarkSecondary
onSecondary           #1C352F          DarkOnSecondary
secondaryContainer    #334B45          DarkSecondaryContainer
onSecondaryContainer  #CCE8E1          DarkOnSecondaryContainer

tertiary              #A5CDDE          DarkTertiary
onTertiary            #0A3544          DarkOnTertiary
tertiaryContainer     #274B5C          DarkOnTertiaryContainer
onTertiaryContainer   #C1E9FA          DarkOnTertiaryContainer

error                 #FFB4AB          DarkError
onError               #690005          DarkOnError
errorContainer        #93000A          DarkErrorContainer
onErrorContainer      #FFDAD6          DarkOnErrorContainer

background            #0D0D0D          DarkBackground
onBackground          #E2E2E5          DarkOnBackground
surface               #1A1A1A          DarkSurface
onSurface             #E2E2E5          DarkOnSurface
surfaceVariant        #242424          DarkSurfaceVariant
onSurfaceVariant      #C1C7CE          DarkOnSurfaceVariant

outline               #8B9198          DarkOutline
outlineVariant        #41474D          DarkOutlineVariant
inverseSurface        #E2E2E5          DarkInverseSurface
inverseOnSurface      #2E3133          DarkInverseOnSurface
inversePrimary        #006B52          DarkInversePrimary
surfaceTint           #00FFC2          DarkSurfaceTint
scrim                 #000000          DarkScrim
─────────────────────────────────────────────────────────

浅色模式 (LightColorScheme) — 新增
─────────────────────────────────────────────────────────
令牌                    色值             Kotlin 变量名
─────────────────────────────────────────────────────────
primary               #006B52          LightPrimary
onPrimary             #FFFFFF          LightOnPrimary
primaryContainer      #80FFD6          LightPrimaryContainer
onPrimaryContainer    #002117          LightOnPrimaryContainer

secondary             #4B635C          LightSecondary
onSecondary           #FFFFFF          LightOnSecondary
secondaryContainer    #CDE8DF          LightSecondaryContainer
onSecondaryContainer  #07201A          LightOnSecondaryContainer

tertiary              #3F6373          LightTertiary
onTertiary            #FFFFFF          LightOnTertiary
tertiaryContainer     #C2E8FB          LightTertiaryContainer
onTertiaryContainer   #001F2A          LightOnTertiaryContainer

error                 #BA1A1A          LightError
onError               #FFFFFF          LightOnError
errorContainer        #FFDAD6          LightErrorContainer
onErrorContainer      #410002          LightOnErrorContainer

background            #F8F9FA          LightBackground
onBackground          #191C1E          LightOnBackground
surface               #FFFFFF          LightSurface
onSurface             #191C1E          LightOnSurface
surfaceVariant        #DEE3EA          LightSurfaceVariant
onSurfaceVariant      #41474D          LightOnSurfaceVariant

outline               #72787E          LightOutline
outlineVariant        #C1C7CE          LightOutlineVariant
inverseSurface        #2E3133          LightInverseSurface
inverseOnSurface      #EFF0F4          LightInverseOnSurface
inversePrimary        #00FFC2          LightInversePrimary
surfaceTint           #006B52          LightSurfaceTint
scrim                 #000000          LightScrim
─────────────────────────────────────────────────────────
```

#### 扩展语义色 (ExtendedColors)

在 `MaterialTheme` 之外通过 `CompositionLocal` 注入的语义色彩：

| 令牌名               | 暗色值      | 浅色值      | 用途              |
|----------------------|------------|------------|------------------|
| `success`            | `#4ADE80`  | `#16A34A`  | 成功状态           |
| `onSuccess`          | `#003314`  | `#FFFFFF`  | 成功状态上文字      |
| `warning`            | `#FBBF24`  | `#D97706`  | 警告状态           |
| `onWarning`          | `#3D2800`  | `#FFFFFF`  | 警告状态上文字      |
| `info`               | `#60A5FA`  | `#2563EB`  | 信息状态           |
| `onInfo`             | `#001A40`  | `#FFFFFF`  | 信息状态上文字      |
| `shimmerBase`        | `#1A1A1A`  | `#E2E2E5`  | Shimmer 基底色     |
| `shimmerHighlight`   | `#2C2C2C`  | `#F5F5F5`  | Shimmer 高亮色     |
| `gradientStart`      | `#00FFC2`  | `#00FFC2`  | FAB 渐变起始       |
| `gradientEnd`        | `#00D1FF`  | `#006B52`  | FAB 渐变终止       |
| `hotBadge`           | `#FFCC00`  | `#FFCC00`  | HOT 角标背景       |
| `onHotBadge`         | `#000000`  | `#000000`  | HOT 角标文字       |
| `overlay`            | `#000000` α60% | `#000000` α40% | 遮罩层       |
| `cardBorder`         | `#FFFFFF` α5%  | `#000000` α8%  | 卡片描边      |

#### 当前硬编码色值清除映射

以下是代码中发现的 63 处硬编码 `Color(...)` 的归类与映射：

| 硬编码色值                             | 应映射为                              |
|---------------------------------------|--------------------------------------|
| `Color(0xFF0D0D0D)`                   | `MaterialTheme.colorScheme.background` |
| `Color(0xFF1A1A1A)`                   | `MaterialTheme.colorScheme.surface`   |
| `Color(0xFF1E1E1E)` / `Color(0xFF1E1F22)` | `MaterialTheme.colorScheme.surfaceVariant` |
| `Color(0xFF242424)` / `Color(0xFF25272B)` | `MaterialTheme.colorScheme.surfaceContainerHigh` |
| `Color(0xFF2C2C2C)`                   | `MaterialTheme.colorScheme.surfaceContainerHighest` |
| `Color(0xFF333333)` / `Color(0xFF35383F)` | `MaterialTheme.colorScheme.outlineVariant` |
| `Color.Black`                          | `MaterialTheme.colorScheme.background` |
| `Color.Black.copy(alpha = 0.6f/0.85f/0.9f/0.95f)` | `ExtendedColors.overlay` |
| `Color.White`                          | `MaterialTheme.colorScheme.onSurface` |
| `Color.White.copy(alpha = 0.05f..0.7f)` | `MaterialTheme.colorScheme.onSurface.copy(alpha)` |
| `Color.Gray`                           | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `Color.Red`                            | `MaterialTheme.colorScheme.error` |
| `Color(0xFFFFCC00)`                    | `ExtendedColors.hotBadge` |
| `Color(0xFF00D1FF)`                    | `ExtendedColors.gradientEnd` |
| `Color(0xFF00BFA5)`                    | `Teal600` (按下态)                   |
| `Color(0xFF6366F1)` / `Color(0xFF3B82F6)` | `ExtendedColors.info` 变体        |
| `Color(0xFF10B981)`                    | `ExtendedColors.success` 变体       |
| `Color(0xFF8B5CF6)` / `Color(0xFFEC4899)` | 工坊工具专用色，定义为 sealed 常量  |
| `Color(0xFFF59E0B)`                    | `ExtendedColors.warning` 变体       |

---

### 1.2 字体排版系统 (Typography Scale)

基于 Material3 Type Scale，12 种角色完整覆盖：

```
角色             大小    行高    字重             字距     用途
──────────────────────────────────────────────────────────────────
displayLarge     57sp   64sp    Normal (400)     -0.25sp  极少使用的超大标题
displayMedium    45sp   52sp    Normal (400)      0sp     少用的大标题
displaySmall     36sp   44sp    Normal (400)      0sp     页面级大标题

headlineLarge    32sp   40sp    Bold (700)        0sp     页面标题 (创意工坊等)
headlineMedium   28sp   36sp    Bold (700)        0sp     详情页应用名
headlineSmall    24sp   32sp    SemiBold (600)    0sp     区段标题

titleLarge       22sp   28sp    Bold (700)        0sp     TopAppBar 标题
titleMedium      16sp   24sp    Bold (700)        0.15sp  卡片标题、区段名
titleSmall       14sp   20sp    SemiBold (600)    0.1sp   次级标题

bodyLarge        16sp   24sp    Normal (400)      0.5sp   正文主体
bodyMedium       14sp   20sp    Normal (400)      0.25sp  正文次体、描述文
bodySmall        12sp   16sp    Normal (400)      0.4sp   辅助说明、时间戳

labelLarge       14sp   20sp    Bold (700)        0.1sp   按钮文字
labelMedium      12sp   16sp    Medium (500)      0.5sp   标签/芯片文字
labelSmall       11sp   16sp    Medium (500)      0.5sp   角标/计数器文字
──────────────────────────────────────────────────────────────────

字体族 (FontFamily):
  - Android: FontFamily.Default (→ Roboto / 系统中文字体)
  - iOS: FontFamily.Default (→ SF Pro / PingFang SC)
  - 无需捆绑自定义字体，依赖系统字体栈
```

**Kotlin 实现参考**:

```kotlin
val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Normal),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
)
```

---

### 1.3 间距系统 (Spacing Scale)

基于 4dp 基准单位的 8 级间距体系：

```
令牌名          值      典型用途
────────────────────────────────────────────────
Spacing.xxs     4dp    图标与文字间隙、紧凑列表项内边距
Spacing.xs      8dp    相邻元素间距、紧凑 padding
Spacing.sm     12dp    卡片网格间距、列表项垂直间距
Spacing.md     16dp    标准内边距、区段间距
Spacing.lg     24dp    区段分隔、大块内容间距
Spacing.xl     32dp    页面级分隔
Spacing.xxl    48dp    屏幕级留白
Spacing.xxxl   64dp    空状态/大图标区域留白
────────────────────────────────────────────────

实现方式: object Spacing { val xxs = 4.dp; val xs = 8.dp; ... }
通过 CompositionLocal 注入，允许平台覆写。
```

---

### 1.4 圆角系统 (Corner Radius / Shape)

```
令牌名                值       用途
──────────────────────────────────────────────
Shape.none           0dp      无圆角 (分割线等)
Shape.extraSmall     4dp      标签/角标
Shape.small          8dp      输入框、下拉选项
Shape.medium        12dp      卡片、列表项容器
Shape.large         16dp      对话框、底部 Sheet
Shape.extraLarge    24dp      底部导航容器、大面板
Shape.full          50%       圆形头像、FAB、芯片
──────────────────────────────────────────────

Kotlin Shapes:
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
```

---

### 1.5 阴影/海拔层级 (Elevation)

```
层级       值       用途
────────────────────────────────────
Level0     0dp     平面内容 (列表项)
Level1     1dp     卡片默认
Level2     3dp     下拉菜单、标签选中
Level3     6dp     导航栏、TopAppBar
Level4    12dp     FAB 浮动按钮
Level5    20dp     FabMenuOverlay、Modal
────────────────────────────────────
```

---

## 二、组件库规范 (Component Library)

### 通用约定

1. **所有组件必须接受 `modifier: Modifier = Modifier` 作为第一个可选参数**
2. **所有组件必须附带至少一个 `@Preview` 注解** (含暗色主题 Preview)
3. **禁止在组件内部硬编码 Color/TextStyle/Shape，全部通过 MaterialTheme 或 CompositionLocal 获取**
4. **组件放置路径**: `shared/src/commonMain/kotlin/com/runninghub/shared/ui/component/`

---

### 2.1 AppCard — 应用瀑布流卡片

**用途**: Discovery 页面瀑布流中每个 AI 应用的展示卡片

```
参数 (Props):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制
title            String                   (必填)        应用标题
author           String                   (必填)        作者昵称
authorAvatar     String?                  null         作者头像 URL
imageUrl         String                   (必填)        封面图 URL
likes            Int                      0            点赞数
stars            Int                      0            收藏数
useCount         String                   "0"          使用次数
isHot            Boolean                  false        是否显示 HOT 角标
onClick          () -> Unit               (必填)        点击回调
──────────────────────────────────────────────────────

布局结构:
┌─────────────────────────┐
│  [HOT] (条件显示)        │ ← Teal50 背景 角标
│                          │
│    封面图 (SmartImage)    │ ← aspectRatio(0.8f)
│    Crop 填充              │
│                          │
│ ░░░░ 渐变遮罩 ░░░░░░░░░░│ ← verticalGradient → overlay
│ 标题 (titleSmall, Bold)  │
│ 👤 作者  ♡12 ★5  ▶100   │ ← labelSmall
└─────────────────────────┘

圆角: Shape.medium (12dp)
描边: colorScheme.outlineVariant, 1dp
最小触控区域: 48dp × 48dp (整个卡片可点击)

状态:
  - default: 常规展示
  - pressed: Surface tonalElevation 提升至 Level2
  - loading: 整体 shimmer 占位
```

---

### 2.2 BottomNavBar — 底部导航栏

**用途**: 所有主页面共享的底部导航，含中央悬浮 FAB

```
参数 (Props):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制
currentRoute     String                   (必填)        当前选中路由
onNavigate       (String) -> Unit         (必填)        路由跳转回调
isFabExpanded    Boolean                  false        FAB 菜单展开态
onFabToggle      () -> Unit               (必填)        FAB 切换回调
──────────────────────────────────────────────────────

布局结构:
┌─────────────────────────────────────┐
│                 [＋]                 │ ← FAB 悬浮，offset y = -36dp
│  探索   搜索   ___   创意工坊   我的  │
│   🔍     🔎          🛠️      👤     │
└─────────────────────────────────────┘

NavigationBar:
  - containerColor: colorScheme.surface.copy(alpha = 0.95f)
  - height: 80dp
  - tonalElevation: 0dp
  - 中央 item 为 disabled 占位符

FAB 悬浮按钮:
  - 尺寸: 56dp × 56dp
  - 形状: CircleShape
  - 背景: linearGradient(gradientStart → gradientEnd)
  - 阴影: Level4 (12dp)
  - 图标: Icons.Default.Add, 32dp, tint = onPrimary
  - 旋转动画: 展开时 45°

选中态:
  - iconColor: primary (#00FFC2)
  - textColor: primary
  - indicatorColor: Transparent
未选中态:
  - iconColor: onSurfaceVariant
  - textColor: onSurfaceVariant

安全区域: navigationBarsPadding() 自动适配
```

---

### 2.3 FabMenuOverlay — 悬浮操作菜单

**用途**: 中央 FAB 展开后的操作菜单面板（统一实现，消除 3 处拷贝）

```
参数 (Props):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制
isVisible        Boolean                  (必填)        菜单可见性
menuItems        List<FabMenuItem>        (必填)        菜单项列表
onDismiss        () -> Unit               (必填)        关闭回调
onItemClick      (FabMenuItem) -> Unit    (必填)        菜单项点击
──────────────────────────────────────────────────────

data class FabMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val tintColor: Color,     // 通过主题令牌指定
    val route: String? = null
)

布局结构:
┌─────────────────────────────────┐
│  ░░░░░ 暗色遮罩 (scrim) ░░░░░░  │ ← overlay α60%
│                                  │
│     ┌─────────────────────┐     │
│     │ 🎨 图像生成 API      │     │ ← Surface, Shape.extraLarge (24dp)
│     │ 📹 视频专业 API      │     │    width: 260dp
│     │ 🎵 音频处理 API      │     │    border: outlineVariant 1dp
│     │ 🧊 3D 渲染 API      │     │    elevation: Level5 (20dp)
│     └─────────────────────┘     │
│              [✕]                 │ ← FAB 已旋转 45°
│  探索  搜索  ___  创意工坊  我的   │
└─────────────────────────────────┘

动画:
  - 遮罩: fadeIn / fadeOut (200ms)
  - 面板: fadeIn + expandVertically from Bottom (300ms)
  
每个菜单项:
  - 左侧: 36dp 圆形图标容器, tintColor α15% 背景
  - 右侧: 15sp, Bold, onSurface
  - 点击区域: 全宽, minHeight 48dp
  - 圆角: Shape.medium (12dp)
```

---

### 2.4 SmartImage — 跨平台智能图片

**用途**: 替代当前 `SmartAsyncImage`，支持图片/GIF/视频帧/跨平台

```
参数 (Props):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制
imageUrl         String                   (必填)        图片/视频 URL
contentDescription String?               null         无障碍描述
contentScale     ContentScale             Fit          缩放模式
showBrokenIcon   Boolean                  false        错误时显示破图标
showVideoIndicator Boolean               true         视频显示播放指示
onSuccess        () -> Unit               {}           加载成功回调
onError          () -> Unit               {}           加载失败回调
──────────────────────────────────────────────────────

状态机:
  Loading → shimmer 占位 (shimmerBase + shimmerHighlight)
  Success → 正常显示图片
  Error   → 可选破图标 (BrokenImage, onSurfaceVariant α20%)

跨平台实现:
  - Android: Coil3 + VideoFrameDecoder + GifDecoder
  - iOS: 基于 Ktor + SkiaSharp (或 KamelImage)
  - expect/actual 分离解码器工厂
```

---

### 2.5 LoadMoreIndicator — 加载更多指示器

**用途**: 列表底部"加载更多/已到底"的统一 UI

```
参数 (Props):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制
state            LoadMoreState            (必填)        当前状态枚举
onLoadMore       () -> Unit               {}           手动触发加载
──────────────────────────────────────────────────────

enum class LoadMoreState {
    Loading,     // 显示旋转 indicator (24dp, primary, strokeWidth 2dp)
    HasMore,     // 显示"下拉或点击加载更多" (bodySmall, onSurfaceVariant)
    NoMore,      // 显示"没有更多了" (bodySmall, onSurfaceVariant, 居中)
    Error        // 显示"加载失败，点击重试" (bodySmall, error)
}

布局: Box, fillMaxWidth, padding = Spacing.md (16dp), contentAlignment = Center
底部留白: Spacer(Spacing.lg) 防止被 BottomNavBar 遮挡
```

---

### 2.6 ShimmerPlaceholder — 骨架屏占位

**用途**: 内容加载时的骨架屏动画效果

```
参数 (Props):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制 (必须设定尺寸)
shape            Shape                    Shape.medium 占位形状
──────────────────────────────────────────────────────

实现: Box + shimmer() + background(shimmerBase)
不含任何内部子元素，纯几何占位

组合使用示例:
  ShimmerPlaceholder(Modifier.fillMaxWidth().height(200.dp))          // 大图
  ShimmerPlaceholder(Modifier.width(120.dp).height(16.dp), Shape.small) // 文字行
```

---

### 2.7 SearchBar — 搜索栏

**用途**: 搜索页面顶部输入栏

```
参数 (Props):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制
query            String                   (必填)        当前输入值
onQueryChange    (String) -> Unit         (必填)        输入变化回调
onSearch         (String) -> Unit         (必填)        确认搜索回调
placeholder      String                   "搜索应用..."  占位文字
──────────────────────────────────────────────────────

布局:
┌────────────────────────────────────┐
│ 🔍  搜索应用...              [✕]   │ ← OutlinedTextField
└────────────────────────────────────┘

样式:
  - Shape: Shape.medium (12dp)
  - focusedBorder: primary
  - unfocusedBorder: outlineVariant
  - 背景: surfaceVariant
  - cursor: primary
  - 最小高度: 48dp (满足触控目标)
```

---

### 2.8 CategoryChip / CategoryPills — 分类标签

**用途**: Discovery 页面的横向分类滚动选择器

```
参数 (Props):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制
categories       List<Category>           (必填)        分类列表
selectedId       String                   (必填)        当前选中 ID
onSelected       (Category) -> Unit       (必填)        选择回调
──────────────────────────────────────────────────────

单个 Chip 规格:
  选中态:
    - containerColor: primary (#00FFC2)
    - contentColor: onPrimary (#003829)
    - fontWeight: SemiBold (600)
  未选中态:
    - containerColor: surfaceVariant
    - contentColor: onSurfaceVariant
    - fontWeight: Medium (500)
  
  通用:
    - shape: Shape.full (50%)
    - padding: horizontal 16dp, vertical 8dp
    - fontSize: 14sp (bodyMedium)
    - minHeight: 36dp, minWidth: 触控 48dp

LazyRow:
  - contentPadding: horizontal = Spacing.md (16dp)
  - horizontalArrangement: spacedBy(Spacing.sm = 12dp)
  - verticalPadding: Spacing.sm (12dp)
```

---

### 2.9 InputNodeCard — 输入节点卡片

**用途**: App Detail 页面中各种输入参数的统一容器

```
基础容器 (BaseInputCard):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制
content          @Composable ColumnScope  (必填)        内容插槽
──────────────────────────────────────────────────────

样式:
  - background: surfaceContainerHigh (#25272B → 令牌化)
  - border: 1dp, outlineVariant α8%
  - shape: Shape.medium (12dp)
  - padding: horizontal 16dp, vertical 12dp

子类型:

  TextInputNode:
    ┌──────────────────────────────────┐
    │ 参数名称 (titleSmall, Bold)       │
    │ ┌──────────────────────────────┐ │
    │ │ 文本输入...                    │ │ ← OutlinedTextField
    │ │                                │ │    focus: primary
    │ └──────────────────────────────┘ │
    └──────────────────────────────────┘

  MediaInputNode:
    ┌──────────────────────────────────┐
    │ ┌─────┐  参数名称 (titleSmall)    │
    │ │ 📷  │  提示文字 (bodySmall)     │ ← 80dp × 80dp 预览框
    │ │[+🔄]│                          │    Shape.large (16dp)
    │ └─────┘                          │
    └──────────────────────────────────┘

  ListInputNode:
    ┌──────────────────────────────────┐
    │ 参数名称          [选择选项 ▾]    │ ← 点击弹出 BottomSheet
    └──────────────────────────────────┘
```

---

### 2.10 StatusBadge — 状态角标

**用途**: 卡片右上角 HOT/NEW/推荐 等标签

```
参数 (Props):
──────────────────────────────────────────────────────
名称              类型                     默认值        说明
modifier         Modifier                 Modifier     外部布局控制
text             String                   (必填)        角标文字
type             BadgeType                Hot          角标类型
──────────────────────────────────────────────────────

enum class BadgeType(val containerColor: @Composable () -> Color, val contentColor: @Composable () -> Color) {
    Hot    → (hotBadge, onHotBadge)        // #FFCC00 / #000000
    New    → (primary, onPrimary)          // #00FFC2 / #003829
    Featured → (info, onInfo)              // #60A5FA / #001A40
}

样式:
  - shape: Shape.extraSmall (4dp)
  - fontSize: 8sp (自定义)
  - fontWeight: Black (900)
  - padding: horizontal 4dp, vertical 1dp
  - 位置: Modifier.align(TopStart).padding(6dp)
```

---

## 三、屏幕布局规范 (Screen Layouts)

### 3.1 Discovery 探索页

```
┌──────────────────────────────────────┐
│ ✨ RunningHUB        🔄  🔍  [👤]   │ ← TopAppBar, α80% background
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │                                  │ │
│ │      Banner Carousel             │ │ ← HorizontalPager
│ │      (16:9, Shape.extraLarge)    │ │    autoPlay 5s
│ │                                  │ │    padding: 16dp
│ │ [TAG]  标题文字                   │ │ ← verticalGradient 遮罩
│ │        副标题                     │ │
│ │            ● ● ○ ○              │ │ ← PagerIndicator
│ └──────────────────────────────────┘ │
│                                      │
│ [全部] [图像生成] [视频生成] [3D] →   │ ← CategoryPills, LazyRow
│                                      │
│ ┌──────────┐ ┌──────────┐           │
│ │  AppCard  │ │  AppCard  │           │ ← 2列瀑布流
│ │  0.8:1    │ │  0.8:1    │           │    gap: 12dp
│ └──────────┘ └──────────┘           │    padding: horizontal 16dp
│ ┌──────────┐ ┌──────────┐           │
│ │  AppCard  │ │  AppCard  │           │
│ └──────────┘ └──────────┘           │
│                                      │
│      [LoadMoreIndicator]             │
│                                      │
├──────────────────────────────────────┤
│  探索   搜索   [＋]   创意工坊   我的  │ ← BottomNavBar
└──────────────────────────────────────┘

空状态:
┌──────────────────────────────────────┐
│         📦 (64dp, Gray)             │
│         暂无作品                      │ ← bodyMedium, onSurfaceVariant
└──────────────────────────────────────┘

加载状态:
┌──────────────────────────────────────┐
│      ⟳ (48dp, primary)              │
│    正在寻找优质作品...                │ ← bodyMedium, onSurface α60%
└──────────────────────────────────────┘
```

---

### 3.2 App Detail 应用详情页

```
┌──────────────────────────────────────┐
│ ← 应用详情                           │ ← TopAppBar, α90% background
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │                                  │ │
│ │     Cover Carousel (1.2:1)       │ │ ← HorizontalPager
│ │     支持 Image + Video           │ │
│ │              ● ● ○              │ │ ← PagerIndicator
│ └──────────────────────────────────┘ │
│                                      │
│  应用标题 (headlineMedium, Bold)      │ ← padding: 16dp
│  [标签1] [标签2] [标签3]             │ ← Tag chips, primary α10%
│                                      │
│  ┌────────────────────────────────┐  │
│  │ 👤 作者名  ·  发布于 2024-01-01 │  │ ← Author row, Surface
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │  ♡ 128  ★ 56   ▶ 1.2K  👁 5K │  │ ← Stats row, surfaceVariant α5%
│  └────────────────────────────────┘  │    Shape.medium, padding 16dp
│                                      │
│  ── 生成结果 ──────────────────────   │ ← 条件显示 (任务完成后)
│  ┌────────────────────────────────┐  │
│  │     Result Image / Video       │  │ ← SmartImage, Shape.medium
│  └────────────────────────────────┘  │
│                                      │
│  ── 简介 ────────────────────────    │
│  应用描述文字...                      │ ← bodyMedium, onSurface α80%
│                                      │
│  ── 配置参数 ────────────────────    │
│  ┌────────────────────────────────┐  │
│  │ [TextInputNode]                │  │ ← InputNodeCard (动态列表)
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │ [MediaInputNode]               │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │ [ListInputNode]                │  │
│  └────────────────────────────────┘  │
│                                      │
│  (120dp 底部留白)                     │
├──────────────────────────────────────┤
│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │ ← verticalGradient 遮罩
│ ┌────────────────────────────────┐   │
│ │  ▶ 立即运行  (18sp, Bold)      │   │ ← RunActionButton
│ └────────────────────────────────┘   │    height: 56dp, Shape 28dp
│                                      │    gradient: primary → Teal600
└──────────────────────────────────────┘

运行中状态:
│ ┌────────────────────────────────┐   │
│ │  ⟳ 运行中... / 排队中...       │   │ ← Gray gradient, disabled
│ └────────────────────────────────┘   │
```

---

### 3.3 Profile 我的页面

```
┌──────────────────────────────────────┐
│ ┌──────────────────────────────────┐ │
│ │  背景横幅图 (120dp)               │ │ ← SmartImage, Crop
│ └──────────────────────────────────┘ │
│ ┌──┐                     [+ 关注]   │ ← Avatar 80dp, offset -32dp
│ │👤│                                │    border: 3dp background
│ └──┘                                │
│  用户名 (titleLarge, Bold)  ID:xxx   │
│  个人简介...                         │ ← bodyMedium, onSurface α70%
│                                      │
│  关注 12  粉丝 56  获赞 128  收藏 45 │ ← StatLabelValue 行
│                                      │
│ ┌──────────┐ ┌──────────┐           │
│ │  AppCard  │ │  AppCard  │           │ ← 2列瀑布流 (我的作品)
│ └──────────┘ └──────────┘           │
│ ┌──────────┐ ┌──────────┐           │
│ │  AppCard  │ │  AppCard  │           │
│ └──────────┘ └──────────┘           │
│                                      │
├──────────────────────────────────────┤
│  探索   搜索   [＋]   创意工坊   我的  │ ← BottomNavBar
└──────────────────────────────────────┘

未登录状态:
┌──────────────────────────────────────┐
│                                      │
│   请在首页右上角绑定 API Key 登录     │ ← 居中, bodyMedium, onSurfaceVariant
│                                      │
├──────────────────────────────────────┤
│  探索   搜索   [＋]   创意工坊   我的  │
└──────────────────────────────────────┘

空作品状态:
┌──────────────────────────────────────┐
│  (CreatorHeader)                     │
│                                      │
│         ℹ️ (64dp, Gray)              │
│          暂无作品                     │
│                                      │
└──────────────────────────────────────┘
```

---

### 3.4 Community 创意工坊页

```
┌──────────────────────────────────────┐
│                                      │
│  创意工坊 (headlineMedium, Bold)      │ ← padding: 24dp
│                                      │
│ ┌──────────┐ ┌──────────┐           │
│ │ 🔵       │ │ 🟣       │           │ ← ToolCard 2×2 网格
│ │ UI 检视器 │ │ 隐写解码  │           │    aspectRatio(1f)
│ │ 查看设备  │ │ 提取隐藏  │           │    gap: 16dp
│ │ 屏幕参数  │ │ 秘密数据  │           │    padding: 16dp
│ └──────────┘ └──────────┘           │
│ ┌──────────┐ ┌──────────┐           │
│ │ 🩷       │ │ 🟢       │           │
│ │ 色彩提取  │ │ 智能裁切  │           │
│ │ (开发中)  │ │ (开发中)  │           │
│ └──────────┘ └──────────┘           │
│                                      │
├──────────────────────────────────────┤
│  探索   搜索   [＋]   创意工坊   我的  │ ← BottomNavBar
└──────────────────────────────────────┘

ToolCard 规格:
  ┌────────────────────┐
  │ ┌────┐             │ ← 图标容器 48dp, Shape.medium
  │ │ 🔵 │             │    background: toolColor α20%
  │ └────┘             │    icon: 24dp, tint: toolColor
  │                    │
  │ 工具名称            │ ← 16sp, Bold, onSurface
  │ 工具描述            │ ← 12sp, onSurfaceVariant, lineHeight 16sp
  └────────────────────┘
  
  Shape: Shape.large (16dp)
  背景: surfaceVariant (#1E1E1E → 令牌化)
  描边: outlineVariant α5%
  padding: 16dp
  间距: SpaceBetween 垂直分布
```

---

## 四、平台适配规范

### 4.1 Android 平台

#### Dynamic Color (动态取色)

```kotlin
@Composable
fun RunningHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Android 12+ (API 31)
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    // ExtendedColors 仍使用品牌色，不随 Dynamic Color 变化
}
```

#### Edge-to-Edge

```
- enableEdgeToEdge() 在 Activity.onCreate() 中调用
- 所有 Scaffold 自动处理 WindowInsets
- TopAppBar: 使用 windowInsetsPadding(WindowInsets.statusBars)
- BottomNavBar: 使用 navigationBarsPadding()
- LazyColumn: contentPadding 包含 WindowInsets.systemBars
- 系统栏样式: 深色图标 (浅色主题) / 浅色图标 (暗色主题)
```

#### Predictive Back (预测性返回)

```
- targetSdk 35+ 启用
- 所有页面转场使用 material3 motion patterns
- AppDetail → Discovery: 共享元素转场 (封面图片)
- 底部 Sheet: 支持手势下拉关闭
- 导航动画: fade + slide (不使用旧的 Activity transition)
```

---

### 4.2 iOS 平台

#### SF Symbols 对标

```
Material Icon           →  SF Symbol 等效
──────────────────────────────────────────
Icons.Default.Explore   →  safari
Icons.Default.Search    →  magnifyingglass
Icons.Default.Add       →  plus
Icons.Default.Build     →  wrench.and.screwdriver
Icons.Default.Person    →  person.circle
Icons.Default.ArrowBack →  chevron.left
Icons.Default.Refresh   →  arrow.clockwise
Icons.Default.Favorite  →  heart.fill
Icons.Default.Star      →  star.fill
Icons.Default.PlayArrow →  play.fill
Icons.Default.Visibility→  eye.fill

实现: expect/actual IconProvider 抽象
  - Android: 直接使用 Material Icons
  - iOS: 映射到 SF Symbols (通过 Compose Multiplatform 的 painterResource)
```

#### Safe Area 适配

```
- iOS 使用 WindowInsets.safeDrawing 替代 Android 的 statusBars/navigationBars
- 圆角屏 (Dynamic Island): 顶部留白自动处理
- 底部 Home Indicator: 通过 navigationBarsPadding() 兼容
- 横屏: 左右安全区域也需要考虑
```

#### iOS 导航模式

```
- 返回手势: iOS 用户习惯从左边缘滑动返回
  → Compose Navigation 默认支持
  → 不需要额外处理，但确保没有全屏手势冲突
  
- 大标题模式: iOS 用户习惯下拉时标题放大
  → 可选适配，通过 expect/actual TopBar 实现
  → 优先级低，v1.0 不要求
  
- 底部 Tab 样式: iOS TabBar 更扁平
  → 通过 Platform.isIOS 条件调整 BottomNavBar:
    - height: 49dp (iOS HIG 标准)
    - 移除 FAB 凸起效果 (iOS 不常见此模式)
    - 使用 SF Symbols
```

---

## 五、无障碍 (Accessibility) 要求

### 5.1 触控目标尺寸

```
所有可交互元素最小触控区域: 48dp × 48dp (Material3 标准)

当前违规项及修复方案:
──────────────────────────────────────────────────
组件                  当前尺寸    修复方案
──────────────────────────────────────────────────
PagerIndicator dot    6dp        增加 padding 至 48dp 触控区
Stats Icon (10dp)     10dp       增加 Modifier.sizeIn(minWidth=48dp, minHeight=48dp)
Author avatar (16dp)  16dp       在 AiAppMasonryItem 中不作为独立交互元素
MediaNode 右下角 [+]  24dp       增加 clickable padding 至 48dp
BottomSheet drag      默认OK     Material3 DragHandle 自带合规区域
──────────────────────────────────────────────────

实现辅助:
Modifier.minimumInteractiveComponentSize() // Material3 内置
```

### 5.2 色彩对比度 (WCAG AA)

```
WCAG AA 标准:
  - 正常文字 (< 18sp): 对比度 ≥ 4.5:1
  - 大文字 (≥ 18sp Bold 或 ≥ 24sp): 对比度 ≥ 3:1
  - UI 控件/图形: 对比度 ≥ 3:1

当前合规验证:
──────────────────────────────────────────────────
前景色            背景色            对比度    合规
──────────────────────────────────────────────────
#00FFC2 (Teal)   #0D0D0D (bg)     12.8:1   ✅ AA
#FFFFFF (white)  #0D0D0D (bg)     19.4:1   ✅ AA
#FFFFFF α60%     #0D0D0D (bg)      7.2:1   ✅ AA
#FFFFFF α40%     #0D0D0D (bg)      4.0:1   ⚠️ 仅大字
#808080 (Gray)   #0D0D0D (bg)      4.1:1   ⚠️ 仅大字
#FFFFFF α70%     #000000 α85%      6.8:1   ✅ AA (叠加)
#000000 (black)  #00FFC2 (Teal)   12.8:1   ✅ AA
──────────────────────────────────────────────────

需修复项:
  1. Color.Gray 用于小字体 (< 18sp) 时对比度不足
     → 替换为 onSurfaceVariant (#C1C7CE, 对比度 9.2:1)
  2. Color.White α40% 用于 stats 小标签 (10sp)
     → 提升至 α60% (对比度 7.2:1)
  3. Color.White α50% 用于 publish time (bodySmall)
     → 提升至 α60%
```

### 5.3 内容描述 (Content Descriptions)

```
所有需要 contentDescription 的元素:
──────────────────────────────────────────────────
元素                         描述策略
──────────────────────────────────────────────────
AppCard 封面图               "${app.title} 封面图"
Author Avatar               "${author.name} 的头像"
Banner 封面                  "${banner.title} 横幅"
Stats Icon (♡★▶👁)          null (decorative，文字已说明)
BottomNav Icons              对应中文标签 ("探索"/"搜索" 等)
FAB (+)                      "创建" / "关闭菜单"
Back Arrow                   "返回"
Refresh Icon                 "刷新"
Search Icon                  "搜索"
FabMenu Items Icon           null (文字标签已充分说明)
ToolCard Icon                null (decorative，标题已说明)
Video Play Indicator         "视频内容"
Broken Image Icon            "图片加载失败"
Upload Camera Icon           "上传${mediaType}"
──────────────────────────────────────────────────

实现原则:
  - 装饰性图标 (有伴随文字): contentDescription = null
  - 功能性图标 (独立交互): 必须提供 contentDescription
  - 图片内容: 提供有意义的描述 (不是 "Image" 或 "图片")
  - 动态内容: 使用字符串模板而非硬编码
```

### 5.4 焦点与键盘导航

```
Tab 键顺序:
  - 遵循视觉从上到下、从左到右的顺序
  - BottomNavBar 项目按 Tab 顺序排列
  - FAB 在 Tab 序列中位于 BottomNavBar 之后
  - 弹出菜单 (FabMenuOverlay) 获焦时 trap focus 在菜单内

焦点指示器:
  - 使用 Material3 默认焦点环 (primary 色, 2dp offset)
  - 自定义 Surface: 添加 Modifier.focusable() + focusedBorder

屏幕阅读器:
  - 所有页面标题通过 semantics { heading() } 标记
  - 列表项通过 semantics { collectionItemInfo(...) } 标记
  - 加载状态通过 LiveRegion 通知: semantics { liveRegion = LiveRegionMode.Polite }
```

---

## 六、动效规范 (Motion)

### 6.1 转场动画

```
页面切换:
  - 同级导航 (Tab 切换): fadeIn/fadeOut, 200ms
  - 前进 (列表→详情): fadeIn + slideInHorizontally(from right), 300ms
  - 返回 (详情→列表): fadeOut + slideOutHorizontally(to right), 250ms

共享元素 (Compose 1.7+):
  - AppCard 封面 → AppDetail 封面: SharedTransitionLayout
  - 使用 animatedVisibilityScope + sharedElement()

Easing:
  - 进入: EmphasizedDecelerateEasing (Material3)
  - 退出: EmphasizedAccelerateEasing (Material3)
```

### 6.2 微交互

```
FAB 旋转:
  - 0° → 45° (展开), animateFloatAsState, 300ms, EaseInOutCubic
  
PullToRefresh:
  - Material3 PullToRefreshBox 默认行为
  
列表加载:
  - 新项目: fadeIn + slideInVertically(from bottom), stagger 50ms
  
按钮按下:
  - scale: 1.0 → 0.96, 100ms (animateFloatAsState)
  
Category Chip 切换:
  - containerColor 渐变: animateColorAsState, 200ms
```

---

## 七、文件结构规划

```
shared/
└── src/
    └── commonMain/
        └── kotlin/com/runninghub/shared/
            ├── ui/
            │   ├── theme/
            │   │   ├── Color.kt           // 所有颜色令牌 (替代当前 60+ 硬编码)
            │   │   ├── Type.kt            // 完整 12 角色字体系统
            │   │   ├── Shape.kt           // 圆角体系
            │   │   ├── Spacing.kt         // 间距令牌 + CompositionLocal
            │   │   ├── Elevation.kt       // 海拔层级
            │   │   ├── ExtendedColors.kt  // 语义扩展色 + CompositionLocal
            │   │   └── Theme.kt           // RunningHubTheme 主函数
            │   │
            │   └── component/
            │       ├── AppCard.kt
            │       ├── BottomNavBar.kt
            │       ├── FabMenuOverlay.kt
            │       ├── SmartImage.kt         // expect/actual
            │       ├── LoadMoreIndicator.kt
            │       ├── ShimmerPlaceholder.kt
            │       ├── SearchBar.kt
            │       ├── CategoryChip.kt
            │       ├── InputNodeCard.kt
            │       ├── StatusBadge.kt
            │       └── StatItem.kt
            │
            └── platform/
                └── IconProvider.kt            // expect: 图标映射
```

---

## 八、迁移优先级与检查清单

### Phase 1: 设计令牌 (预计 2-3 天)

- [ ] 创建 `Color.kt` — 完整 Material3 暗色/浅色 ColorScheme
- [ ] 创建 `ExtendedColors.kt` — 语义扩展色 + `LocalExtendedColors`
- [ ] 扩展 `Type.kt` — 从 3 角色 → 15 角色完整定义
- [ ] 创建 `Shape.kt` — 统一圆角系统
- [ ] 创建 `Spacing.kt` — 间距令牌 + `LocalSpacing`
- [ ] 更新 `Theme.kt` — 支持 dark/light + Dynamic Color

### Phase 2: 组件库 (预计 4-5 天)

- [ ] 抽取 `AppCard` (从 `AiAppMasonryItem`)
- [ ] 抽取 `BottomNavBar` (从 `DiscoveryBottomNav`)
- [ ] 统一 `FabMenuOverlay` (消除 3 处拷贝)
- [ ] 抽取 `SmartImage` (从 `SmartAsyncImage`, expect/actual)
- [ ] 抽取 `LoadMoreIndicator` (从 Discovery 底部逻辑)
- [ ] 创建 `ShimmerPlaceholder` (替代内联 shimmer Box)
- [ ] 抽取 `SearchBar`
- [ ] 抽取 `CategoryChip` (从 `CategoryPills`)
- [ ] 抽取 `InputNodeCard` (已有 BaseInputCard，需令牌化)
- [ ] 创建 `StatusBadge` (从 HOT 角标逻辑)
- [ ] 为所有组件添加 `@Preview`

### Phase 3: 硬编码清除 (预计 2-3 天)

- [ ] 遍历全部 63 处 `Color(0xFF...)` → 替换为主题令牌
- [ ] 遍历全部内联 `fontSize` → 替换为 `MaterialTheme.typography.*`
- [ ] 遍历全部内联 `RoundedCornerShape(Xdp)` → 替换为 `MaterialTheme.shapes.*`
- [ ] 遍历全部内联 `padding(Xdp)` → 替换为 `Spacing.*`
- [ ] 为 12+ composable 补充 `modifier` 参数

### Phase 4: 无障碍修复 (预计 1-2 天)

- [ ] 补充所有 `contentDescription`
- [ ] 修复触控目标 < 48dp 的元素
- [ ] 修复对比度不合规的颜色配对 (3 处)
- [ ] 添加 semantics 标记 (heading, liveRegion)

---

> **文档维护**: 本规范随代码演进同步更新。任何涉及设计令牌、组件 API、布局结构的变更需先更新本文档再实施代码。
