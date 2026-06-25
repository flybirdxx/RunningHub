# RunningHub 官网主体配色抓取文档

> 抓取日期：2026-06-19（Asia/Shanghai）  
> 页面：`https://www.runninghub.cn/`  
> 抓取方式：已打开 Chrome + Chrome DevTools Protocol，只读取 DOM、CSS 变量和 computed styles。  
> 用途：为后续移动端/Compose UI 调整、Banner 替换、品牌色统一提供参考。  
> 安全边界：本文档只记录颜色与视觉角色，不记录 Cookie、Authorization、账号、接口响应或媒体签名 URL。

## 结论摘要

RunningHub 官网首页主体视觉是高对比暗色体系：

- 基础底色：黑色与近黑灰。
- 主品牌强调：荧光绿 `#CCFF00`，主要用于运营主视觉、强 CTA、重点链接和页脚。
- 文本体系：白色主文本，浅灰辅助文本，暗灰弱文本。
- 卡片体系：`#18181B`、`#080808`、`#09090B` 这类近黑层级。
- 交互体系里还存在一组青绿色 CSS 变量，例如 `#02DBA3`、`#01A47A`，更像旧组件/表单/控件 token，不是首页主视觉的第一品牌色。

## 核心调色板

| 角色 | 色值 | 来源/使用场景 | 建议用途 |
| --- | --- | --- | --- |
| `brand.lime` | `#CCFF00` | Seedance 主推面板、重点链接、Footer 背景 | 主品牌强调色、活动 Banner、关键 CTA |
| `base.black` | `#000000` | 页面背景、CTA 黑底、暗色导航 | 深色主背景、强对比按钮底 |
| `surface.900` | `#080808` | 高频近黑背景 | 页面大面积暗底 |
| `surface.850` | `#09090B` | 高频近黑背景 | 卡片深底、暗色分区 |
| `surface.800` | `#18181B` | 模型矩阵卡片、短片卡片底色 | 卡片/媒体容器默认底色 |
| `surface.700` | `#27272A` | 次级面板、深色浮层 | 弹层、控件容器、弱分区 |
| `text.primary` | `#FFFFFF` | 主文本、导航 hover、暗底标题 | 暗底主文本 |
| `text.default` | `#EFEFEF` | CSS 变量 `--rh-text-color` | 默认正文文本 |
| `text.secondary` | `#D8D8D8` | 导航默认文本、次级标题 | 暗底次级文本 |
| `text.muted` | `#9DA2A8` | 搜索/辅助文本、灰色 UI 元素 | placeholder、说明文字 |
| `text.subtle` | `#999999` | 弱化说明、低优先级文本 | 时间、附加信息 |
| `text.disabled` | `#71717A` | 更弱的灰色文本 | 禁用态、弱提示 |
| `border.subtle` | `rgba(255,255,255,0.05)` | 导航边框、暗色卡片边界 | 深色模式细边框 |
| `border.soft` | `rgba(73,74,76,0.4)` | CSS 变量 `--rh-border-color` | 表单/面板边框 |
| `status.error` | `#FF4144` | CSS 变量 `--rh-error-color` | 错误、失败、警告 |
| `promo.pink` | `#FF1493` | 高频运营强调色之一 | 活动点缀，慎用 |
| `control.teal` | `#02DBA3` | CSS 变量 `--rh-primary-rgb` | 控件高亮、旧主色兼容 |
| `control.tealActive` | `#01A47A` | CSS 变量 `--rh-button-ghost-active-color` | 表单 hover/active |

## 官网 CSS 变量摘录

官网当前暴露了以下和配色相关的 CSS 变量：

```css
--rh-bg-dark: #0e1117;
--rh-bg-dark-2: #191d22;
--rh-bg-gray: #1a1d23;
--rh-background-load-panel: #1a1b1f;
--rh-bg-modal: #272727;

--rh-text-color: #efefef;
--rh-text-gray-color: #dae2e2;
--rh-text-gray-color-2: #bebebe;
--rh-secondary-color: #d8d8d8;
--rh-gray-color: #9da2a8;
--rh-cool-gray-color: #bec4ca;

--rh-primary-rgb: 2,219,163;          /* #02DBA3 */
--rh-button-ghost-active-color: #01a47a;
--rh-input-hover-border-color: #08765d;
--rh-primary-disabled-color: #085b4a;

--rh-border-color: rgba(73,74,76,.4);
--rh-error-color: #ff4144;
```

注意：`--swiper-theme-color: #007aff` 是 Swiper 默认蓝色，不应当当作 RunningHub 品牌色。

## 模块级观察

### 导航栏

| 属性 | 观察值 |
| --- | --- |
| 背景 | `rgba(0,0,0,0.8)` 桌面端，移动端约 `rgba(0,0,0,0.4)` |
| 底部分割线 | `rgba(255,255,255,0.05)` / `rgba(255,255,255,0.08)` |
| 导航默认文字 | `#D8D8D8` |
| 导航 hover/强调 | `#FFFFFF` |
| 搜索 placeholder/辅助 | `#9DA2A8` / 白色 50% 透明 |

### 模型推荐矩阵

| 元素 | 观察值 |
| --- | --- |
| 左侧主推背景 | `#CCFF00` |
| 左侧主推文字 | `#000000` |
| CTA 背景 | `#000000` |
| CTA 文字 | `#FFFFFF` |
| 媒体卡片底色 | `#18181B` |
| 卡片圆角 | `0px` |

### 内容卡片

| 元素 | 观察值 |
| --- | --- |
| 卡片底色 | `#18181B` |
| 主文字 | `#FFFFFF` |
| 强调链接 | `#CCFF00` |
| 弱边框 | `rgba(255,255,255,0.05)` |
| 图片/视频遮罩 | 黑色渐变透明层 |

### Footer

| 元素 | 观察值 |
| --- | --- |
| 背景 | `#CCFF00` |
| 文字 | `#000000` |
| 视觉角色 | 高识别度品牌收尾区 |

## Compose Token 映射建议

建议先把官网配色落成应用内独立品牌 token，不要直接散落硬编码：

```kotlin
object RunningHubWebColors {
    val BrandLime = Color(0xFFCCFF00)
    val BaseBlack = Color(0xFF000000)
    val Surface900 = Color(0xFF080808)
    val Surface850 = Color(0xFF09090B)
    val Surface800 = Color(0xFF18181B)
    val Surface700 = Color(0xFF27272A)

    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextDefaultDark = Color(0xFFEFEFEF)
    val TextSecondaryDark = Color(0xFFD8D8D8)
    val TextMutedDark = Color(0xFF9DA2A8)
    val TextSubtleDark = Color(0xFF999999)

    val ControlTeal = Color(0xFF02DBA3)
    val ControlTealActive = Color(0xFF01A47A)
    val StatusError = Color(0xFFFF4144)
}
```

落地优先级：

1. Banner / 活动推荐位优先使用 `BrandLime`、`BaseBlack`、`TextPrimaryDark`。
2. 媒体卡、视频卡、深色卡片使用 `Surface800`。
3. 深色页面背景使用 `BaseBlack` 或 `Surface900`，不要用偏蓝/紫黑替代。
4. 辅助文案使用 `TextSecondaryDark` / `TextMutedDark`，避免在暗底上使用过低对比灰。
5. `ControlTeal` 保留给输入框、控件激活态、旧组件兼容，不建议和 `BrandLime` 在同一关键 CTA 上竞争。

## 可访问性建议

| 组合 | 评价 | 用途 |
| --- | --- | --- |
| `#CCFF00` + `#000000` | 高对比，强品牌识别 | 主推 Banner、强 CTA |
| `#000000` + `#FFFFFF` | 高对比 | 正文、标题、按钮 |
| `#18181B` + `#FFFFFF` | 高对比 | 卡片标题 |
| `#18181B` + `#9DA2A8` | 可用但偏弱 | 辅助文字，字号不宜太小 |
| `#CCFF00` + `#FFFFFF` | 不建议 | 亮底白字对比不足 |

## 后续开发注意点

- 官网主体是暗色品牌系统，但当前移动端发现页仍有浅色页面结构；迁移时应按模块逐步引入，避免整页突然变成黑底导致视觉割裂。
- 对 Banner 这类品牌位，可以先局部采用官网色：黑/荧光绿/白/深灰。
- `#CCFF00` 是强刺激色，不适合大面积用于普通列表背景；适合用于品牌主推、状态亮点、CTA。
- 深色媒体卡建议配合黑色渐变遮罩，保持白字可读性。
- 如果未来后端配置 Banner 色值，建议字段使用语义角色，例如 `backgroundRole=brand.lime`，不要只传裸 Hex，方便客户端做暗色/浅色适配。

## 验证记录

- 已通过 Chrome/CDP 打开 `https://www.runninghub.cn/`。
- 已读取 `documentElement` CSS variables、`body`、导航、运营 Banner、模型矩阵、内容卡片和 Footer 的 computed styles。
- 已统计页面高频 text/background/border 颜色。
- 未执行写操作，未记录登录态或接口敏感数据。
