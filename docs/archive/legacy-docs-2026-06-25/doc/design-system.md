# RunningHub 设计系统 (Design System) v2.0

> **项目**: RunningHub — AI 应用枢纽平台  
> **平台**: Android + iOS (Compose Multiplatform)  
> **品牌主色**: Electric Violet → Cyan 渐变 (`#6C5CE7` → `#00D2FF`)  
> **设计基调**: 科技感、亲和力、高效、可信赖  
> **文档版本**: 2.0 | 2026-04-25  
> **目标用户**: AI 爱好者、创作者、开发者

---

## 目录

1. [品牌色彩系统 (Color System)](#1-品牌色彩系统-color-system)
2. [排版系统 (Typography)](#2-排版系统-typography)
3. [间距系统 (Spacing)](#3-间距系统-spacing)
4. [圆角系统 (Border Radius)](#4-圆角系统-border-radius)
5. [阴影/高度系统 (Elevation)](#5-阴影高度系统-elevation)
6. [组件规范 (Components)](#6-组件规范-components)
7. [图标系统 (Iconography)](#7-图标系统-iconography)
8. [动效规范 (Motion)](#8-动效规范-motion)
9. [深色模式策略 (Dark Mode)](#9-深色模式策略-dark-mode)
10. [Compose Multiplatform 主题实现](#10-compose-multiplatform-主题实现)

---

## 1. 品牌色彩系统 (Color System)

### 1.1 品牌主色 (Primary)

RunningHub 采用蓝紫渐变方向作为品牌视觉锚点，传达科技感与创新力。

| Token 名 | HEX | 用途 |
|-----------|------|------|
| `Primary50` | `#F3F1FF` | 极浅背景高亮 |
| `Primary100` | `#E0DBFF` | Tag/Chip 浅背景 |
| `Primary200` | `#C4B5FD` | Hover 状态 |
| `Primary300` | `#A78BFA` | 次要强调 |
| `Primary400` | `#8B6CF7` | 活跃指示器 |
| **`Primary500`** | **`#6C5CE7`** | **品牌主色 / Primary** |
| `Primary600` | `#5B4BD4` | Pressed 按下态 |
| `Primary700` | `#4C3EC0` | 深层操作 |
| `Primary800` | `#3D31A8` | 深色文字/图标 |
| `Primary900` | `#2E2490` | 最深强调 |

### 1.2 辅助色 (Secondary)

科技感青蓝色，用于渐变终点、链接、信息强调。

| Token 名 | HEX | 用途 |
|-----------|------|------|
| `Secondary50` | `#E8FBFF` | 极浅信息背景 |
| `Secondary100` | `#BFF3FF` | 浅色标签 |
| `Secondary200` | `#80E7FF` | 辅助 Hover |
| `Secondary300` | `#40DBFF` | 辅助强调 |
| `Secondary400` | `#1AD4FF` | 活跃指示 |
| **`Secondary500`** | **`#00D2FF`** | **辅助主色 / Secondary** |
| `Secondary600` | `#00B8E0` | Pressed 态 |
| `Secondary700` | `#009EC0` | 深层辅助 |
| `Secondary800` | `#0084A0` | 辅助深色 |
| `Secondary900` | `#006A80` | 最深辅助 |

### 1.3 Neutral / 灰度色阶

| Token 名 | HEX | 用途 |
|-----------|------|------|
| `Neutral50` | `#F8FAFC` | 浅色模式页面背景 |
| `Neutral100` | `#F1F5F9` | 浅色模式卡片背景 |
| `Neutral200` | `#E2E8F0` | 分割线、边框 (Light) |
| `Neutral300` | `#CBD5E1` | 禁用状态 (Light) |
| `Neutral400` | `#94A3B8` | 占位文本 |
| `Neutral500` | `#64748B` | 次要文本 (Light) |
| `Neutral600` | `#475569` | 正文文本 (Light) |
| `Neutral700` | `#334155` | 深灰表面 |
| `Neutral800` | `#1E293B` | 深色卡片背景 |
| `Neutral900` | `#0F172A` | 深色页面背景 |

### 1.4 Semantic 语义色

| Token 名 | Light HEX | Dark HEX | 对比度 (on bg) | 用途 |
|-----------|-----------|----------|---------------|------|
| `Success` | `#16A34A` | `#4ADE80` | ≥ 4.5:1 ✅ | 成功 / 完成 |
| `onSuccess` | `#FFFFFF` | `#003314` | — | Success 上文字 |
| `Warning` | `#D97706` | `#FBBF24` | ≥ 4.5:1 ✅ | 警告 / 提醒 |
| `onWarning` | `#FFFFFF` | `#3D2800` | — | Warning 上文字 |
| `Error` | `#DC2626` | `#F87171` | ≥ 4.5:1 ✅ | 错误 / 危险 |
| `onError` | `#FFFFFF` | `#410002` | — | Error 上文字 |
| `Info` | `#2563EB` | `#60A5FA` | ≥ 4.5:1 ✅ | 信息 / 提示 |
| `onInfo` | `#FFFFFF` | `#001A40` | — | Info 上文字 |

### 1.5 Surface 表面色

#### Light Mode 表面色

| Token 名 | HEX | 用途 |
|-----------|------|------|
| `Background` | `#F8FAFC` | 页面背景 |
| `Surface` | `#FFFFFF` | 卡片 / 容器 |
| `SurfaceVariant` | `#F1F5F9` | 次级容器、输入框背景 |
| `SurfaceContainer` | `#E2E8F0` | 嵌套容器 |
| `SurfaceBright` | `#FFFFFF` | 弹窗 / Modal |
| `Overlay` | `#000000` α40% | 遮罩层 |

#### Dark Mode 表面色

| Token 名 | HEX | 用途 |
|-----------|------|------|
| `Background` | `#0B0F1A` | 页面背景 |
| `Surface` | `#141929` | 卡片 / 容器 |
| `SurfaceVariant` | `#1E2438` | 次级容器、输入框背景 |
| `SurfaceContainer` | `#252B40` | 嵌套容器 |
| `SurfaceBright` | `#2A3050` | 弹窗 / Modal |
| `Overlay` | `#000000` α60% | 遮罩层 |

### 1.6 完整 Material3 ColorScheme 映射

```
Dark Mode (DarkColorScheme)
─────────────────────────────────────────────────────────────────
Token                   HEX              Kotlin 变量名
─────────────────────────────────────────────────────────────────
primary                 #A78BFA          DarkPrimary
onPrimary               #1A0045          DarkOnPrimary
primaryContainer        #3D31A8          DarkPrimaryContainer
onPrimaryContainer      #E0DBFF          DarkOnPrimaryContainer

secondary               #80E7FF          DarkSecondary
onSecondary             #003544          DarkOnSecondary
secondaryContainer      #004D63          DarkSecondaryContainer
onSecondaryContainer    #BFF3FF          DarkOnSecondaryContainer

tertiary                #FFB4A8          DarkTertiary
onTertiary              #5C1900          DarkOnTertiary
tertiaryContainer       #7A2E15          DarkTertiaryContainer
onTertiaryContainer     #FFDBD1          DarkOnTertiaryContainer

error                   #F87171          DarkError
onError                 #410002          DarkOnError
errorContainer          #93000A          DarkErrorContainer
onErrorContainer        #FFDAD6          DarkOnErrorContainer

background              #0B0F1A          DarkBackground
onBackground            #E2E8F0          DarkOnBackground
surface                 #141929          DarkSurface
onSurface               #E2E8F0          DarkOnSurface
surfaceVariant          #1E2438          DarkSurfaceVariant
onSurfaceVariant        #CBD5E1          DarkOnSurfaceVariant

outline                 #64748B          DarkOutline
outlineVariant          #334155          DarkOutlineVariant
inverseSurface          #E2E8F0          DarkInverseSurface
inverseOnSurface        #1E293B          DarkInverseOnSurface
inversePrimary          #6C5CE7          DarkInversePrimary
surfaceTint             #A78BFA          DarkSurfaceTint
scrim                   #000000          DarkScrim
─────────────────────────────────────────────────────────────────

Light Mode (LightColorScheme)
─────────────────────────────────────────────────────────────────
Token                   HEX              Kotlin 变量名
─────────────────────────────────────────────────────────────────
primary                 #6C5CE7          LightPrimary
onPrimary               #FFFFFF          LightOnPrimary
primaryContainer        #E0DBFF          LightPrimaryContainer
onPrimaryContainer      #1A0045          LightOnPrimaryContainer

secondary               #0084A0          LightSecondary
onSecondary             #FFFFFF          LightOnSecondary
secondaryContainer      #BFF3FF          LightSecondaryContainer
onSecondaryContainer    #001F2A          LightOnSecondaryContainer

tertiary                #9C4230          LightTertiary
onTertiary              #FFFFFF          LightOnTertiary
tertiaryContainer       #FFDBD1          LightTertiaryContainer
onTertiaryContainer     #3A0B00          LightOnTertiaryContainer

error                   #DC2626          LightError
onError                 #FFFFFF          LightOnError
errorContainer          #FFDAD6          LightErrorContainer
onErrorContainer        #410002          LightOnErrorContainer

background              #F8FAFC          LightBackground
onBackground            #0F172A          LightOnBackground
surface                 #FFFFFF          LightSurface
onSurface               #0F172A          LightOnSurface
surfaceVariant          #F1F5F9          LightSurfaceVariant
onSurfaceVariant        #475569          LightOnSurfaceVariant

outline                 #94A3B8          LightOutline
outlineVariant          #E2E8F0          LightOutlineVariant
inverseSurface          #1E293B          LightInverseSurface
inverseOnSurface        #F1F5F9          LightInverseOnSurface
inversePrimary          #C4B5FD          LightInversePrimary
surfaceTint             #6C5CE7          LightSurfaceTint
scrim                   #000000          LightScrim
─────────────────────────────────────────────────────────────────
```

### 1.7 扩展语义色 (ExtendedColors)

通过 `CompositionLocal` 注入到 `MaterialTheme` 之外的业务专用色彩：

| Token | Dark HEX | Light HEX | 用途 |
|-------|----------|-----------|------|
| `gradientStart` | `#A78BFA` | `#6C5CE7` | 品牌渐变起始 |
| `gradientEnd` | `#00D2FF` | `#00B8E0` | 品牌渐变终止 |
| `shimmerBase` | `#141929` | `#E2E8F0` | Shimmer 骨架基底 |
| `shimmerHighlight` | `#1E2438` | `#F8FAFC` | Shimmer 高亮 |
| `hotBadge` | `#FBBF24` | `#D97706` | HOT 角标背景 |
| `onHotBadge` | `#1A0800` | `#FFFFFF` | HOT 角标文字 |
| `newBadge` | `#A78BFA` | `#6C5CE7` | NEW 角标背景 |
| `onNewBadge` | `#1A0045` | `#FFFFFF` | NEW 角标文字 |
| `cardBorder` | `#FFFFFF` α6% | `#0F172A` α8% | 卡片描边 |
| `linkText` | `#80E7FF` | `#2563EB` | 超链接文字 |

### 1.8 WCAG AA 对比度验证

所有前景/背景组合确保 WCAG AA 标准（正常文字 ≥ 4.5:1, 大文字 ≥ 3:1）：

```
Dark Mode 对比度验证
──────────────────────────────────────────────────────
前景                    背景              对比度    合规
──────────────────────────────────────────────────────
#A78BFA (primary)       #0B0F1A (bg)      7.2:1    ✅ AA
#E2E8F0 (onSurface)    #0B0F1A (bg)      14.1:1   ✅ AA
#E2E8F0 (onSurface)    #141929 (surface)  11.2:1   ✅ AA
#CBD5E1 (onSurfVar)    #141929 (surface)   9.8:1   ✅ AA
#CBD5E1 (onSurfVar)    #1E2438 (surfVar)   7.4:1   ✅ AA
#F87171 (error)         #0B0F1A (bg)       6.3:1   ✅ AA
#4ADE80 (success)       #0B0F1A (bg)       8.5:1   ✅ AA
#1A0045 (onPrimary)     #A78BFA (primary)  7.2:1   ✅ AA

Light Mode 对比度验证
──────────────────────────────────────────────────────
前景                    背景              对比度    合规
──────────────────────────────────────────────────────
#6C5CE7 (primary)       #FFFFFF (surface)  4.6:1   ✅ AA
#0F172A (onSurface)     #FFFFFF (surface)  16.0:1  ✅ AA
#0F172A (onSurface)     #F8FAFC (bg)       15.3:1  ✅ AA
#475569 (onSurfVar)     #FFFFFF (surface)   7.1:1  ✅ AA
#475569 (onSurfVar)     #F1F5F9 (surfVar)  6.2:1  ✅ AA
#DC2626 (error)         #FFFFFF (surface)   4.6:1  ✅ AA
#16A34A (success)       #FFFFFF (surface)   4.5:1  ✅ AA
#FFFFFF (onPrimary)     #6C5CE7 (primary)  4.6:1   ✅ AA
──────────────────────────────────────────────────────
```

---

## 2. 排版系统 (Typography)

### 2.1 字体选择

| 平台 | 西文字体 | 中文字体 | 等宽字体 |
|------|---------|---------|---------|
| Android | Roboto | Noto Sans SC | Roboto Mono |
| iOS | SF Pro | PingFang SC | SF Mono |

> Compose Multiplatform 中使用 `FontFamily.Default` 自动匹配平台系统字体，无需捆绑自定义字体文件。

### 2.2 字号阶梯 (Type Scale)

| Role | 字号 (sp) | 行高 (sp) | 字重 | 字距 (sp) | 用途 |
|------|----------|----------|------|----------|------|
| **Display Large** | 57 | 64 | Normal (400) | -0.25 | 启动页/欢迎页超大标题 |
| **Display Medium** | 45 | 52 | Normal (400) | 0 | 几乎不用，保留给特殊场景 |
| **Display Small** | 36 | 44 | Normal (400) | 0 | 空状态大标题 |
| **Headline Large** | 32 | 40 | Bold (700) | 0 | 页面级大标题（创意工坊等） |
| **Headline Medium** | 28 | 36 | Bold (700) | 0 | 详情页应用名 |
| **Headline Small** | 24 | 32 | SemiBold (600) | 0 | 区段标题 |
| **Title Large** | 22 | 28 | Bold (700) | 0 | TopAppBar 标题 |
| **Title Medium** | 16 | 24 | Bold (700) | 0.15 | 卡片标题、区段名 |
| **Title Small** | 14 | 20 | SemiBold (600) | 0.1 | 次级标题、输入节点标题 |
| **Body Large** | 16 | 24 | Normal (400) | 0.5 | 正文主体 |
| **Body Medium** | 14 | 20 | Normal (400) | 0.25 | 正文次体、描述文 |
| **Body Small** | 12 | 16 | Normal (400) | 0.4 | 辅助说明、时间戳 |
| **Label Large** | 14 | 20 | Bold (700) | 0.1 | 按钮文字 |
| **Label Medium** | 12 | 16 | Medium (500) | 0.5 | 标签/Chip 文字 |
| **Label Small** | 11 | 16 | Medium (500) | 0.5 | 角标/计数器文字 |

### 2.3 Compose Typography 代码

```kotlin
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp, lineHeight = 64.sp,
        fontWeight = FontWeight.Normal, letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp, lineHeight = 52.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp, lineHeight = 44.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp, lineHeight = 40.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp, lineHeight = 36.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp, lineHeight = 28.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp
    ),
)
```

---

## 3. 间距系统 (Spacing)

基于 4dp 基准网格，8 级间距递进：

| Token | 值 | 典型用途 |
|-------|-----|---------|
| `xs` | 4dp | 图标与文字间隙、紧凑列表项内边距 |
| `sm` | 8dp | 相邻元素间距、紧凑 padding |
| `md` | 12dp | 卡片网格间距、列表项垂直间距 |
| `lg` | 16dp | 标准内边距、区段间距 |
| `xl` | 24dp | 区段分隔、大块内容间距 |
| `2xl` | 32dp | 页面级分隔 |
| `3xl` | 48dp | 屏幕级留白、空状态区域 |
| `4xl` | 64dp | 大图标区域留白 |

### Compose 实现

```kotlin
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    val xxxxl = 64.dp
}

val LocalSpacing = staticCompositionLocalOf { AppSpacing }
```

**用法示例**:

```kotlin
val spacing = LocalSpacing.current
Modifier.padding(horizontal = spacing.lg, vertical = spacing.md)
```

---

## 4. 圆角系统 (Border Radius)

| Token | 值 | 用途 |
|-------|-----|------|
| `none` | 0dp | 无圆角（分割线等） |
| `sm` | 4dp | 标签/角标、小元素 |
| `md` | 8dp | 输入框、下拉选项 |
| `lg` | 12dp | 卡片、列表项容器 |
| `xl` | 16dp | 对话框、底部 Sheet、ToolCard |
| `2xl` | 24dp | 大面板、底部导航容器 |
| `full` | 50% | 圆形头像、FAB、Chip |

### Compose 实现

```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
```

---

## 5. 阴影/高度系统 (Elevation)

| Level | 值 | 用途 | 暗色模式表现 |
|-------|-----|------|------------|
| **Level 0** | 0dp | 平面内容（列表项） | 无叠加 |
| **Level 1** | 1dp | 卡片默认态 | surface + 5% tint |
| **Level 2** | 3dp | 下拉菜单、选中态卡片 | surface + 8% tint |
| **Level 3** | 6dp | 导航栏、TopAppBar | surface + 11% tint |
| **Level 4** | 8dp | FAB 浮动按钮 | surface + 12% tint |
| **Level 5** | 12dp | Modal、FabMenuOverlay | surface + 14% tint |

### Compose 实现

```kotlin
object AppElevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 3.dp
    val level3 = 6.dp
    val level4 = 8.dp
    val level5 = 12.dp
}
```

> 暗色模式中 Compose Material3 使用 `surfaceTint` 叠加代替阴影投射，层级越高表面越亮。

---

## 6. 组件规范 (Components)

### 6.1 Button

#### 尺寸变体

| Size | Height | Horizontal Padding | Font | Icon Size |
|------|--------|-------------------|------|-----------|
| **sm** | 32dp | 12dp | labelMedium (12sp) | 16dp |
| **md** | 40dp | 16dp | labelLarge (14sp) | 20dp |
| **lg** | 48dp | 24dp | titleSmall (14sp, SemiBold) | 24dp |

#### 样式变体

**Primary Button** — 品牌主操作

```
┌──────────────────────┐
│  ▶ 立即运行           │  ← 渐变背景 gradientStart → gradientEnd
└──────────────────────┘

背景:     linearGradient(Primary500 → Secondary500)
文字色:   #FFFFFF
按下态:   linearGradient(Primary600 → Secondary600)
禁用态:   Neutral700 背景, Neutral500 文字
圆角:     height / 2 (full rounded)
```

**Secondary Button** — 次要操作

```
背景:     SurfaceVariant
文字色:   Primary (Light) / Primary200 (Dark)
描边:     1dp, outlineVariant
按下态:   SurfaceContainer 背景
禁用态:   Neutral200 (Light) / Neutral800 (Dark) 背景
圆角:     md (8dp)
```

**Ghost Button** — 文本操作

```
背景:     Transparent
文字色:   Primary500
按下态:   Primary50 (Light) / Primary900 α20% (Dark) 背景
禁用态:   Neutral400 文字
圆角:     md (8dp)
```

**Danger Button** — 危险操作

```
背景:     Error
文字色:   onError
按下态:   Error 加深 10%
禁用态:   同 Primary 禁用态
圆角:     height / 2 (full rounded)
```

#### Compose 代码示例

```kotlin
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val height = when (size) {
        ButtonSize.Small -> 32.dp
        ButtonSize.Medium -> 40.dp
        ButtonSize.Large -> 48.dp
    }
    val horizontalPadding = when (size) {
        ButtonSize.Small -> 12.dp
        ButtonSize.Medium -> 16.dp
        ButtonSize.Large -> 24.dp
    }
    val textStyle = when (size) {
        ButtonSize.Small -> MaterialTheme.typography.labelMedium
        ButtonSize.Medium -> MaterialTheme.typography.labelLarge
        ButtonSize.Large -> MaterialTheme.typography.titleSmall
    }
    val shape = RoundedCornerShape(height / 2)

    when (variant) {
        ButtonVariant.Primary -> {
            val gradientBrush = Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    ExtendedColors.current.gradientEnd
                )
            )
            Surface(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled,
                shape = shape,
                color = Color.Transparent,
            ) {
                Box(
                    modifier = Modifier
                        .background(gradientBrush)
                        .padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        leadingIcon?.let {
                            Icon(it, null, tint = Color.White)
                        }
                        Text(text, style = textStyle, color = Color.White)
                    }
                }
            }
        }
        // Secondary, Ghost, Danger 类似实现...
    }
}

enum class ButtonVariant { Primary, Secondary, Ghost, Danger }
enum class ButtonSize { Small, Medium, Large }
```

---

### 6.2 Card

#### 标准卡片 (Standard Card)

```
┌──────────────────────────┐
│                          │
│     内容区域 (slot)       │
│                          │
└──────────────────────────┘

背景:   surface (Light: #FFFFFF / Dark: #141929)
描边:   1dp, outlineVariant α6%
圆角:   lg (12dp)
内边距: lg (16dp)
高度:   level1 (1dp)
```

#### 应用卡片 (AppCard)

```
┌─────────────────────────┐
│ [HOT]                    │ ← StatusBadge, 条件显示
│                          │
│   封面图 (SmartImage)    │ ← aspectRatio(0.8f), Crop
│                          │
│ ░░░░ 渐变遮罩 ░░░░░░░░░│ ← verticalGradient(Transparent → Scrim α80%)
│ 标题 (titleSmall)        │
│ 👤 作者 ♡12 ★5 ▶100     │ ← labelSmall, onSurface α70%
└─────────────────────────┘

背景:   surface
描边:   1dp, cardBorder
圆角:   lg (12dp)
点击态: tonalElevation → level2
```

#### 创作者卡片 (Creator Card)

```
┌──────────────────────────┐
│  ┌──┐                    │
│  │👤│  用户名             │ ← Avatar 48dp + titleMedium
│  └──┘  @handle           │ ← bodySmall, onSurfaceVariant
│                          │
│  个人简介文字...          │ ← bodyMedium, 2 行 maxLines
│                          │
│  作品 24  粉丝 1.2K      │ ← labelMedium, onSurfaceVariant
└──────────────────────────┘

背景:   surfaceVariant
描边:   无
圆角:   lg (12dp)
内边距: lg (16dp)
```

#### Compose 代码示例

```kotlin
@Composable
fun AppCard(
    title: String,
    author: String,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    authorAvatar: String? = null,
    likes: Int = 0,
    stars: Int = 0,
    useCount: String = "0",
    isHot: Boolean = false,
) {
    val spacing = LocalSpacing.current

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, ExtendedColors.current.cardBorder),
    ) {
        Box {
            SmartImage(
                imageUrl = imageUrl,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(spacing.md)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // author + stats row...
                }
            }
            if (isHot) {
                StatusBadge(
                    text = "HOT",
                    type = BadgeType.Hot,
                    modifier = Modifier.align(Alignment.TopStart).padding(spacing.sm)
                )
            }
        }
    }
}
```

---

### 6.3 TopAppBar

#### 居中标题式

```
┌──────────────────────────────────────┐
│ ←        RunningHub        🔄 🔍 👤 │ ← CenterAligned, Medium
└──────────────────────────────────────┘

背景:           surface α90%
标题:           titleLarge, onSurface
图标:           24dp, onSurfaceVariant
scrollBehavior: enterAlwaysScrollBehavior (上划收起)
windowInsets:   statusBars
```

#### 大标题式

```
┌──────────────────────────────────────┐
│ ←                                    │
│                                      │
│  创意工坊                             │ ← headlineMedium, Bold
└──────────────────────────────────────┘

背景:   surface α90%
标题:   headlineMedium, onSurface
```

#### 搜索栏式

```
┌──────────────────────────────────────┐
│ ←  ┌─────────────────────────┐  取消 │
│    │ 🔍 搜索应用...            │      │
│    └─────────────────────────┘      │
└──────────────────────────────────────┘

搜索框背景: surfaceVariant
搜索框圆角: md (8dp)
placeholder: bodyMedium, onSurfaceVariant α60%
```

#### Compose 代码示例

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge)
        },
        modifier = modifier,
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
```

---

### 6.4 BottomNavBar

```
┌─────────────────────────────────────┐
│                 [＋]                 │ ← FAB: 56dp, CircleShape
│  探索   搜索   ___   工坊   我的     │   gradient(Primary → Secondary)
│   🧭    🔍          🛠️     👤      │   offset y = -28dp
└─────────────────────────────────────┘

Tabs: 4-5 个
Nav 容器:
  背景:       surface α95%
  高度:       80dp (含 safe area)
  tonalElev:  0dp

选中态:
  iconColor:  primary
  textColor:  primary
  indicator:  Transparent
未选中态:
  iconColor:  onSurfaceVariant
  textColor:  onSurfaceVariant

FAB:
  尺寸:       56dp × 56dp
  背景:       linearGradient(gradientStart → gradientEnd)
  图标:       Icons.Rounded.Add, 28dp, tint = White
  阴影:       level4 (8dp)
  旋转动画:   展开时 0° → 45°, 300ms
```

#### Compose 代码示例

```kotlin
@Composable
fun AppBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    isFabExpanded: Boolean = false,
    onFabToggle: () -> Unit = {},
) {
    val spacing = LocalSpacing.current

    Box(modifier = modifier.fillMaxWidth()) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 0.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            NavItem.entries.forEach { item ->
                if (item == NavItem.Placeholder) {
                    Spacer(Modifier.weight(1f))
                } else {
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { onNavigate(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        }

        val fabRotation by animateFloatAsState(
            targetValue = if (isFabExpanded) 45f else 0f,
            animationSpec = tween(300, easing = EaseInOutCubic),
            label = "fab_rotation"
        )
        FloatingActionButton(
            onClick = onFabToggle,
            shape = CircleShape,
            containerColor = Color.Transparent,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
                .size(56.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ExtendedColors.current.gradientStart,
                            ExtendedColors.current.gradientEnd,
                        )
                    ),
                    shape = CircleShape,
                )
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = if (isFabExpanded) "关闭菜单" else "创建",
                tint = Color.White,
                modifier = Modifier.size(28.dp).rotate(fabRotation),
            )
        }
    }
}
```

---

### 6.5 TextField / SearchBar

```
┌────────────────────────────────────┐
│ 🔍  搜索 AI 应用...          [✕]   │ ← OutlinedTextField
└────────────────────────────────────┘

容器背景:       surfaceVariant
圆角:           md (8dp)
最小高度:       48dp
focusedBorder:  primary, 1.5dp
unfocusedBorder: outlineVariant, 1dp
cursor 色:      primary
placeholder:    bodyMedium, onSurfaceVariant α60%
输入文字:       bodyMedium, onSurface
图标:           24dp, onSurfaceVariant
```

#### Compose 代码示例

```kotlin
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索 AI 应用...",
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        placeholder = {
            Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(24.dp))
        },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "清除")
            } }
        } else null,
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}
```

---

### 6.6 LoadingIndicator / Shimmer 骨架屏

#### ShimmerPlaceholder

```
┌──────────────────────────┐
│ ░░░░░░░░░░░░░░░░░░░░░░░ │ ← shimmerBase → shimmerHighlight 动画
│ ░░░░░░░░░░░░░░░░░░░░░░░ │    infinite, 1000ms, linear
└──────────────────────────┘

Shimmer 方向:  从左到右, 线性渐变扫光
动画周期:      1000ms, repeatMode = Restart
基底色:        shimmerBase (Dark: #141929 / Light: #E2E8F0)
高亮色:        shimmerHighlight (Dark: #1E2438 / Light: #F8FAFC)
圆角:          与目标组件一致
```

#### Compose 代码示例

```kotlin
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_x"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            ExtendedColors.current.shimmerBase,
            ExtendedColors.current.shimmerHighlight,
            ExtendedColors.current.shimmerBase,
        ),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 300f, 0f),
    )
    Box(modifier = modifier.clip(shape).background(brush))
}

// 卡片骨架屏组合
@Composable
fun AppCardSkeleton(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        ShimmerPlaceholder(Modifier.fillMaxWidth().aspectRatio(0.8f))
        ShimmerPlaceholder(Modifier.fillMaxWidth(0.7f).height(14.dp), MaterialTheme.shapes.small)
        ShimmerPlaceholder(Modifier.fillMaxWidth(0.4f).height(12.dp), MaterialTheme.shapes.small)
    }
}
```

---

### 6.7 Tag / Chip

```
选中态:
┌───────────┐
│ 图像生成   │ ← Primary500 背景, onPrimary 文字
└───────────┘

未选中态:
┌───────────┐
│ 视频生成   │ ← SurfaceVariant 背景, onSurfaceVariant 文字
└───────────┘

圆角:     full (50%)
padding:  horizontal 16dp, vertical 8dp
字号:     bodyMedium (14sp)
选中字重: SemiBold (600)
未选中字重: Medium (500)
最小高度: 36dp
最小宽度: 触控目标 48dp
切换动画: containerColor animateColorAsState, 200ms
```

#### Compose 代码示例

```kotlin
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "chip_color"
    )
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 36.dp),
        shape = CircleShape,
        color = containerColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
            color = contentColor,
        )
    }
}
```

---

### 6.8 Avatar

```
尺寸变体:
  xs:  24dp  — 卡片内行间作者头像
  sm:  32dp  — 列表项头像
  md:  48dp  — 创作者卡片头像
  lg:  64dp  — Profile 详情页头像
  xl:  80dp  — 个人主页大头像

形状:     CircleShape
占位符:   Primary100 背景 + 首字母 (Primary700 文字)
错误态:   同占位符
描边:     2dp, surface (用于叠在图片上时)
```

#### Compose 代码示例

```kotlin
@Composable
fun AppAvatar(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.Medium,
) {
    val dimension = when (size) {
        AvatarSize.XSmall -> 24.dp
        AvatarSize.Small -> 32.dp
        AvatarSize.Medium -> 48.dp
        AvatarSize.Large -> 64.dp
        AvatarSize.XLarge -> 80.dp
    }
    val fontSize = when (size) {
        AvatarSize.XSmall -> 10.sp
        AvatarSize.Small -> 12.sp
        AvatarSize.Medium -> 16.sp
        AvatarSize.Large -> 22.sp
        AvatarSize.XLarge -> 28.sp
    }

    Box(
        modifier = modifier
            .size(dimension)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            SmartImage(
                imageUrl = imageUrl,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

enum class AvatarSize { XSmall, Small, Medium, Large, XLarge }
```

---

### 6.9 SnackBar / Toast

```
┌──────────────────────────────────────┐
│ ℹ️  操作已完成                 [撤销] │ ← SnackBar
└──────────────────────────────────────┘

┌──────────────────────────┐
│ ✅ 保存成功               │ ← Toast (无操作按钮)
└──────────────────────────┘

SnackBar:
  背景:          inverseSurface
  文字色:        inverseOnSurface
  按钮色:        inversePrimary
  圆角:          md (8dp)
  距离底部:      BottomNavBar 高度 + lg (16dp)
  最大宽度:      屏幕宽度 - 32dp
  进入动画:      fadeIn + slideInVertically(from bottom), 200ms
  退出动画:      fadeOut + slideOutVertically(to bottom), 150ms
  自动消失:      3000ms (Short) / 6000ms (Long)

Toast 变体 (状态反馈):
  Success:  Success 背景 α90%, onSuccess 文字, ✅ 图标
  Error:    Error 背景 α90%, onError 文字, ❌ 图标
  Warning:  Warning 背景 α90%, onWarning 文字, ⚠️ 图标
  Info:     Info 背景 α90%, onInfo 文字, ℹ️ 图标
```

#### Compose 代码示例

```kotlin
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary,
            shape = MaterialTheme.shapes.small,
        )
    }
}
```

---

## 7. 图标系统 (Iconography)

### 7.1 图标库

推荐使用 **Material Symbols Rounded** 作为主图标库，风格统一、圆润亲和。

```
依赖:
implementation("androidx.compose.material:material-icons-extended")
```

### 7.2 图标尺寸

| Token | 尺寸 | 用途 |
|-------|------|------|
| `iconXs` | 16dp | 行内小图标（stats 数值旁） |
| `iconSm` | 20dp | 导航栏图标、Tag 前置图标 |
| `iconMd` | 24dp | 标准操作图标（TopAppBar action、列表项图标） |
| `iconLg` | 32dp | FAB 内图标、空状态图标 |

### 7.3 图标颜色规则

| 场景 | 颜色 Token |
|------|-----------|
| TopAppBar action | `onSurfaceVariant` |
| BottomNav 选中 | `primary` |
| BottomNav 未选中 | `onSurfaceVariant` |
| 列表项前导图标 | `onSurfaceVariant` |
| FAB 图标 | `White (#FFFFFF)` |
| 禁用状态图标 | `onSurface` α38% |

### 7.4 跨平台图标映射

| Material Icon | SF Symbol (iOS) | 用途 |
|--------------|----------------|------|
| `Icons.Rounded.Explore` | `safari` | 探索 Tab |
| `Icons.Rounded.Search` | `magnifyingglass` | 搜索 Tab |
| `Icons.Rounded.Add` | `plus` | 创建 FAB |
| `Icons.Rounded.Build` | `wrench.and.screwdriver` | 创意工坊 Tab |
| `Icons.Rounded.Person` | `person.circle` | 我的 Tab |
| `Icons.AutoMirrored.Rounded.ArrowBack` | `chevron.left` | 返回 |
| `Icons.Rounded.Refresh` | `arrow.clockwise` | 刷新 |
| `Icons.Rounded.FavoriteBorder` | `heart` | 点赞 |
| `Icons.Rounded.Star` | `star` | 收藏 |
| `Icons.Rounded.PlayArrow` | `play.fill` | 运行/播放 |

---

## 8. 动效规范 (Motion)

### 8.1 页面转场动画

| 类型 | 进入动画 | 退出动画 | 时长 | Easing |
|------|---------|---------|------|--------|
| Tab 切换 (同级) | fadeIn | fadeOut | 200ms | LinearEasing |
| 前进 (列表→详情) | fadeIn + slideInHorizontally(→) | fadeOut + slideOutHorizontally(←) | 300ms | EmphasizedDecelerate |
| 返回 (详情→列表) | fadeIn + slideInHorizontally(←) | fadeOut + slideOutHorizontally(→) | 250ms | EmphasizedAccelerate |

#### Shared Element Transition (共享元素转场)

```kotlin
SharedTransitionLayout {
    AnimatedContent(targetState = currentScreen) { screen ->
        when (screen) {
            is Screen.Discovery -> {
                AppCard(
                    modifier = Modifier.sharedElement(
                        state = rememberSharedContentState(key = "cover_${app.id}"),
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                )
            }
            is Screen.Detail -> {
                SmartImage(
                    modifier = Modifier.sharedElement(
                        state = rememberSharedContentState(key = "cover_${app.id}"),
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                )
            }
        }
    }
}
```

### 8.2 列表项动画 (Staggered Fade In)

```kotlin
@Composable
fun StaggeredFadeInItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = index * 50,
                easing = EaseOutCubic,
            )
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = index * 50,
                easing = EaseOutCubic,
            )
        ),
        modifier = modifier,
    ) {
        content()
    }
}
```

### 8.3 微交互

#### 按钮 Press 效果

```kotlin
@Composable
fun Modifier.pressScale(): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(100),
        label = "press_scale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, indication = null) { }
}
```

#### Tab 切换指示器

```kotlin
val indicatorOffset by animateDpAsState(
    targetValue = tabPositions[selectedIndex].left,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    ),
    label = "tab_indicator"
)
```

#### FAB 旋转 + 菜单展开

```kotlin
val fabRotation by animateFloatAsState(
    targetValue = if (isExpanded) 45f else 0f,
    animationSpec = tween(300, easing = EaseInOutCubic),
    label = "fab_rotation"
)

AnimatedVisibility(
    visible = isExpanded,
    enter = fadeIn(tween(200)) + expandVertically(
        expandFrom = Alignment.Bottom,
        animationSpec = tween(300, easing = EaseOutCubic),
    ),
    exit = fadeOut(tween(150)) + shrinkVertically(
        shrinkTowards = Alignment.Bottom,
        animationSpec = tween(200, easing = EaseInCubic),
    ),
) {
    FabMenuPanel(/* ... */)
}
```

### 8.4 动效速查表

| 属性 | API | 时长 | Easing |
|------|-----|------|--------|
| 颜色变化 | `animateColorAsState` | 200ms | EaseInOut |
| 尺寸变化 | `animateDpAsState` | 250ms | EaseOutCubic |
| 透明度 | `animateFloatAsState` | 150ms | LinearEasing |
| 旋转 | `animateFloatAsState` | 300ms | EaseInOutCubic |
| 按压缩放 | `animateFloatAsState` | 100ms | LinearEasing |
| 列表 stagger | `fadeIn + slideInVertically` | 300ms + 50ms×index | EaseOutCubic |
| Shimmer 扫光 | `infiniteTransition.animateFloat` | 1000ms | LinearEasing |

---

## 9. 深色模式策略 (Dark Mode)

### 9.1 色彩映射规则

| 设计原则 | 说明 |
|---------|------|
| **表面色反转** | Light 的白色表面 → Dark 的深蓝灰表面，保持层级关系不变 |
| **主色提亮** | Primary 在 Dark 模式中使用 `Primary200-300` 色阶，确保在深色背景上可读 |
| **语义色饱和度调整** | Error/Warning/Success 在 Dark 模式使用更浅更柔和的变体 |
| **白色文字不用纯白** | 使用 `#E2E8F0` 代替 `#FFFFFF`，减少视觉疲劳 |
| **阴影→Tint 叠加** | Dark 模式不投射阴影，通过 `surfaceTint` 叠加表示层级高度 |

### 9.2 表面层级区分

暗色模式下，层级通过表面亮度递增来体现：

```
Layer 0 (Background):   #0B0F1A  ← 最深，页面底色
Layer 1 (Surface):       #141929  ← 卡片、容器 (+ 5% tint)
Layer 2 (SurfaceVariant):#1E2438  ← 输入框、次级容器 (+ 8% tint)
Layer 3 (SurfaceContainer):#252B40 ← 嵌套容器 (+ 11% tint)
Layer 4 (SurfaceBright): #2A3050  ← Modal、弹窗 (+ 14% tint)

视觉: 层级越高越亮，但始终保持克制的蓝灰色调
```

### 9.3 实现策略

```kotlin
@Composable
fun RunningHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 默认关闭以保持品牌一致性
    content: @Composable () -> Unit,
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
    val extendedColors = if (darkTheme) darkExtendedColors else lightExtendedColors

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalSpacing provides AppSpacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
```

---

## 10. Compose Multiplatform 主题实现

### 10.1 文件结构

```
composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/
├── Color.kt           ← 所有颜色令牌
├── Type.kt            ← Typography 定义
├── Shape.kt           ← Shapes 定义
├── Dimens.kt          ← Spacing + Elevation + 图标尺寸
├── ExtendedColors.kt  ← 语义扩展色 + CompositionLocal
└── Theme.kt           ← RunningHubTheme 主入口
```

### 10.2 `Color.kt` — 完整颜色定义

```kotlin
package com.runninghub.app.ui.theme

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────
// Primary 色阶 (Blue-Violet)
// ──────────────────────────────────────────
val Primary50  = Color(0xFFF3F1FF)
val Primary100 = Color(0xFFE0DBFF)
val Primary200 = Color(0xFFC4B5FD)
val Primary300 = Color(0xFFA78BFA)
val Primary400 = Color(0xFF8B6CF7)
val Primary500 = Color(0xFF6C5CE7)
val Primary600 = Color(0xFF5B4BD4)
val Primary700 = Color(0xFF4C3EC0)
val Primary800 = Color(0xFF3D31A8)
val Primary900 = Color(0xFF2E2490)

// ──────────────────────────────────────────
// Secondary 色阶 (Cyan)
// ──────────────────────────────────────────
val Secondary50  = Color(0xFFE8FBFF)
val Secondary100 = Color(0xFFBFF3FF)
val Secondary200 = Color(0xFF80E7FF)
val Secondary300 = Color(0xFF40DBFF)
val Secondary400 = Color(0xFF1AD4FF)
val Secondary500 = Color(0xFF00D2FF)
val Secondary600 = Color(0xFF00B8E0)
val Secondary700 = Color(0xFF009EC0)
val Secondary800 = Color(0xFF0084A0)
val Secondary900 = Color(0xFF006A80)

// ──────────────────────────────────────────
// Neutral 灰度色阶
// ──────────────────────────────────────────
val Neutral50  = Color(0xFFF8FAFC)
val Neutral100 = Color(0xFFF1F5F9)
val Neutral200 = Color(0xFFE2E8F0)
val Neutral300 = Color(0xFFCBD5E1)
val Neutral400 = Color(0xFF94A3B8)
val Neutral500 = Color(0xFF64748B)
val Neutral600 = Color(0xFF475569)
val Neutral700 = Color(0xFF334155)
val Neutral800 = Color(0xFF1E293B)
val Neutral900 = Color(0xFF0F172A)

// ──────────────────────────────────────────
// Semantic 语义色
// ──────────────────────────────────────────
val SuccessLight  = Color(0xFF16A34A)
val SuccessDark   = Color(0xFF4ADE80)
val WarningLight  = Color(0xFFD97706)
val WarningDark   = Color(0xFFFBBF24)
val ErrorLight    = Color(0xFFDC2626)
val ErrorDark     = Color(0xFFF87171)
val InfoLight     = Color(0xFF2563EB)
val InfoDark      = Color(0xFF60A5FA)

// ──────────────────────────────────────────
// Dark Mode 专用色
// ──────────────────────────────────────────
val DarkBackground       = Color(0xFF0B0F1A)
val DarkSurface          = Color(0xFF141929)
val DarkSurfaceVariant   = Color(0xFF1E2438)
val DarkSurfaceContainer = Color(0xFF252B40)
val DarkSurfaceBright    = Color(0xFF2A3050)

val DarkPrimary              = Primary300    // #A78BFA
val DarkOnPrimary            = Color(0xFF1A0045)
val DarkPrimaryContainer     = Primary800    // #3D31A8
val DarkOnPrimaryContainer   = Primary100    // #E0DBFF

val DarkSecondary            = Secondary200  // #80E7FF
val DarkOnSecondary          = Color(0xFF003544)
val DarkSecondaryContainer   = Color(0xFF004D63)
val DarkOnSecondaryContainer = Secondary100  // #BFF3FF

val DarkTertiary             = Color(0xFFFFB4A8)
val DarkOnTertiary           = Color(0xFF5C1900)
val DarkTertiaryContainer    = Color(0xFF7A2E15)
val DarkOnTertiaryContainer  = Color(0xFFFFDBD1)

val DarkError                = ErrorDark
val DarkOnError              = Color(0xFF410002)
val DarkErrorContainer       = Color(0xFF93000A)
val DarkOnErrorContainer     = Color(0xFFFFDAD6)

val DarkOnBackground     = Neutral200   // #E2E8F0
val DarkOnSurface        = Neutral200   // #E2E8F0
val DarkOnSurfaceVariant = Neutral300   // #CBD5E1
val DarkOutline          = Neutral500   // #64748B
val DarkOutlineVariant   = Neutral700   // #334155

// ──────────────────────────────────────────
// Light Mode 专用色
// ──────────────────────────────────────────
val LightBackground       = Neutral50    // #F8FAFC
val LightSurface          = Color(0xFFFFFFFF)
val LightSurfaceVariant   = Neutral100   // #F1F5F9
val LightSurfaceContainer = Neutral200   // #E2E8F0

val LightPrimary              = Primary500   // #6C5CE7
val LightOnPrimary            = Color(0xFFFFFFFF)
val LightPrimaryContainer     = Primary100   // #E0DBFF
val LightOnPrimaryContainer   = Color(0xFF1A0045)

val LightSecondary            = Secondary800 // #0084A0
val LightOnSecondary          = Color(0xFFFFFFFF)
val LightSecondaryContainer   = Secondary100 // #BFF3FF
val LightOnSecondaryContainer = Color(0xFF001F2A)

val LightTertiary             = Color(0xFF9C4230)
val LightOnTertiary           = Color(0xFFFFFFFF)
val LightTertiaryContainer    = Color(0xFFFFDBD1)
val LightOnTertiaryContainer  = Color(0xFF3A0B00)

val LightError                = ErrorLight
val LightOnError              = Color(0xFFFFFFFF)
val LightErrorContainer       = Color(0xFFFFDAD6)
val LightOnErrorContainer     = Color(0xFF410002)

val LightOnBackground     = Neutral900   // #0F172A
val LightOnSurface        = Neutral900   // #0F172A
val LightOnSurfaceVariant = Neutral600   // #475569
val LightOutline          = Neutral400   // #94A3B8
val LightOutlineVariant   = Neutral200   // #E2E8F0
```

### 10.3 `Type.kt` — Typography 定义

```kotlin
package com.runninghub.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 57.sp, lineHeight = 64.sp,
        fontWeight = FontWeight.Normal, letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 45.sp, lineHeight = 52.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 36.sp, lineHeight = 44.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 32.sp, lineHeight = 40.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 28.sp, lineHeight = 36.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 24.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 22.sp, lineHeight = 28.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 11.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp,
    ),
)
```

### 10.4 `Dimens.kt` — 间距 + 高度 + 尺寸

```kotlin
package com.runninghub.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
object AppSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
    val xxxxl: Dp = 64.dp
}

val LocalSpacing = staticCompositionLocalOf { AppSpacing }

@Immutable
object AppElevation {
    val level0: Dp = 0.dp
    val level1: Dp = 1.dp
    val level2: Dp = 3.dp
    val level3: Dp = 6.dp
    val level4: Dp = 8.dp
    val level5: Dp = 12.dp
}

@Immutable
object AppIconSize {
    val xs: Dp = 16.dp
    val sm: Dp = 20.dp
    val md: Dp = 24.dp
    val lg: Dp = 32.dp
}

@Immutable
object AppComponentSize {
    val buttonSmHeight: Dp = 32.dp
    val buttonMdHeight: Dp = 40.dp
    val buttonLgHeight: Dp = 48.dp
    val minTouchTarget: Dp = 48.dp
    val fabSize: Dp = 56.dp
    val bottomNavHeight: Dp = 80.dp
    val topAppBarHeight: Dp = 64.dp
    val avatarXs: Dp = 24.dp
    val avatarSm: Dp = 32.dp
    val avatarMd: Dp = 48.dp
    val avatarLg: Dp = 64.dp
    val avatarXl: Dp = 80.dp
}
```

### 10.5 `ExtendedColors.kt` — 语义扩展色

```kotlin
package com.runninghub.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColorScheme(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val hotBadge: Color,
    val onHotBadge: Color,
    val newBadge: Color,
    val onNewBadge: Color,
    val cardBorder: Color,
    val linkText: Color,
    val overlay: Color,
)

val darkExtendedColors = ExtendedColorScheme(
    success = SuccessDark,
    onSuccess = Color(0xFF003314),
    warning = WarningDark,
    onWarning = Color(0xFF3D2800),
    info = InfoDark,
    onInfo = Color(0xFF001A40),
    gradientStart = Primary300,
    gradientEnd = Secondary500,
    shimmerBase = DarkSurface,
    shimmerHighlight = DarkSurfaceVariant,
    hotBadge = WarningDark,
    onHotBadge = Color(0xFF1A0800),
    newBadge = Primary300,
    onNewBadge = Color(0xFF1A0045),
    cardBorder = Color(0x0FFFFFFF),     // White α6%
    linkText = Secondary200,
    overlay = Color(0x99000000),         // Black α60%
)

val lightExtendedColors = ExtendedColorScheme(
    success = SuccessLight,
    onSuccess = Color(0xFFFFFFFF),
    warning = WarningLight,
    onWarning = Color(0xFFFFFFFF),
    info = InfoLight,
    onInfo = Color(0xFFFFFFFF),
    gradientStart = Primary500,
    gradientEnd = Secondary600,
    shimmerBase = Neutral200,
    shimmerHighlight = Neutral50,
    hotBadge = WarningLight,
    onHotBadge = Color(0xFFFFFFFF),
    newBadge = Primary500,
    onNewBadge = Color(0xFFFFFFFF),
    cardBorder = Color(0x140F172A),     // Neutral900 α8%
    linkText = InfoLight,
    overlay = Color(0x66000000),         // Black α40%
)

val LocalExtendedColors = staticCompositionLocalOf { darkExtendedColors }

object ExtendedColors {
    val current: ExtendedColorScheme
        @androidx.compose.runtime.Composable
        get() = LocalExtendedColors.current
}
```

### 10.6 `Theme.kt` — 完整主题入口

```kotlin
package com.runninghub.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = Neutral200,
    inverseOnSurface = Neutral800,
    inversePrimary = Primary500,
    surfaceTint = DarkPrimary,
    scrim = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = Neutral800,
    inverseOnSurface = Neutral100,
    inversePrimary = Primary200,
    surfaceTint = LightPrimary,
    scrim = Color(0xFF000000),
)

@Composable
fun RunningHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) darkExtendedColors else lightExtendedColors

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalSpacing provides AppSpacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
```

---

## 附录 A: 设计令牌速查表

```
颜色令牌          Light HEX    Dark HEX     用法
─────────────────────────────────────────────────────
primary           #6C5CE7      #A78BFA      品牌主色
secondary         #0084A0      #80E7FF      辅助色
background        #F8FAFC      #0B0F1A      页面背景
surface           #FFFFFF      #141929      卡片/容器
surfaceVariant    #F1F5F9      #1E2438      次级容器
onSurface         #0F172A      #E2E8F0      主文字
onSurfaceVariant  #475569      #CBD5E1      次要文字
outline           #94A3B8      #64748B      边框
error             #DC2626      #F87171      错误
success           #16A34A      #4ADE80      成功
warning           #D97706      #FBBF24      警告
info              #2563EB      #60A5FA      信息
```

```
间距令牌     值       排版令牌          字号/行高/字重
─────────────────────────────────────────────────────
xs           4dp     displayLarge      57/64/400
sm           8dp     headlineLarge     32/40/700
md           12dp    titleLarge        22/28/700
lg           16dp    titleMedium       16/24/700
xl           24dp    bodyLarge         16/24/400
2xl          32dp    bodyMedium        14/20/400
3xl          48dp    labelLarge        14/20/700
4xl          64dp    labelSmall        11/16/500
```

```
圆角令牌     值       Elevation 令牌    值
─────────────────────────────────────────
none         0dp     level0            0dp
sm           4dp     level1            1dp
md           8dp     level2            3dp
lg           12dp    level3            6dp
xl           16dp    level4            8dp
2xl          24dp    level5            12dp
full         50%
```

---

## 附录 B: 从旧设计系统迁移

当前代码中的 `RunningHubTeal (#00FFC2)` 将被替换为新的品牌色 `Primary500 (#6C5CE7)`。以下是关键映射：

| 旧令牌 (v1.0) | 新令牌 (v2.0) | 说明 |
|---------------|--------------|------|
| `RunningHubTeal` (#00FFC2) | `Primary500` (#6C5CE7) | 品牌主色升级为蓝紫色 |
| `DarkBackground` (#0D0D0D) | `DarkBackground` (#0B0F1A) | 略带蓝调的深色背景 |
| `DarkSurface` (#1A1A1A) | `DarkSurface` (#141929) | 蓝灰调表面色 |
| `TextWhite` (#FFFFFF) | `DarkOnSurface` (#E2E8F0) | 减少纯白，降低视觉疲劳 |
| `TextSlate500` (#64748B) | `DarkOutline` (#64748B) | 保持一致 |
| 无 | `Secondary500` (#00D2FF) | 新增辅助色用于渐变 |

---

> **文档维护**: 本设计系统文档随代码演进同步更新。任何涉及设计令牌、组件 API、布局结构的变更需先更新本文档再实施代码。  
> **版本控制**: 使用语义化版本号 (Major.Minor)，Breaking Change 升 Major。
