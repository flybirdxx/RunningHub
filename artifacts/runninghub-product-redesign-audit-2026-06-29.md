# RunningHub 产品级重设计分析

日期：2026-06-29  
范围：基于用户提供的 10 张截图、本地仓库 `D:\Programs\RunningHub`、CodeGraph 索引和关键源码阅读。  
边界：本次是产品、UX、视觉系统与 KMP/Compose 落地分析，没有修改生产代码，也没有运行 App 做真实交互观察。

## 证据摘要

- 截图 1：Discover 首页。顶部 Banner、分类、应用卡片、底部导航。
- 截图 2：AI 应用详情页。应用信息、统计、作者、参数、固定运行按钮。
- 截图 3：模型选择 Bottom Sheet。
- 截图 4：参数配置 Bottom Sheet。
- 截图 5：Create 创作页。
- 截图 6：Plaza 广场页。
- 截图 7：History 历史页。
- 截图 8-9：Task Detail / Result 抽屉。
- 截图 10：Profile 个人中心。
- CodeGraph：索引最新，612 个文件、11,566 个节点。
- 关键代码证据：
  - 主导航：`composeApp/src/commonMain/kotlin/com/runninghub/app/ui/navigation/MainScreen.kt`，`BottomNavTab` 为 `Discovery / QuickCreate / Studio / History / Profile`，其中 `Studio` 映射 Plaza。
  - 主题：`composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/*`，已有 `BrandLime`、`RhApp*`、`Dimens`、`ExtendedColors`，但语义层不足。
  - QuickCreate 独立视觉：`composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateDesign.kt` 另建 `Purple / Green / Cyan / Pink` 等私有色板。
  - 计费预览：`feature/quickcreate/presentation/.../billing/QuickCreateFeePreviewInteractor.kt`、`QuickCreateBillingUiText.kt` 已有预估费用、计费失败、余额不足语义基础。
  - 历史统一：`feature/task/domain/.../GenerationHistory.kt` 已统一 QuickCreate、WebApp、Workflow、Standard Model 历史模型。
  - 任务详情计费：`feature/task/data/.../WebAppTaskMappers.kt` 已映射 `originalAmount / finalAmount / coinNum`，但 UI 表达像后台字段表。

---

# 一、产品定位诊断

## 结论

RunningHub 现在实际是“AI 应用市场 + 模型 API 调用 + 快捷创作 + 创作者广场 + 任务账单”的混合体，但当前 UI 没有告诉用户哪个才是主线。它看起来像功能堆叠，不像成熟 AI 创作平台。

1. 主定位应该是：AI 创作工作台。用户打开 App 的第一目标应是“快速生成并拿到结果”，不是先理解应用、模型、工作流、广场、任务这些内部概念。
2. 辅助定位是：模板/应用市场 + 创作者灵感社区 + 任务和资产管理器。
3. 最大问题排序：信息架构问题 > 商业信任问题 > 交互路径问题 > 视觉系统问题。视觉不统一是结果，不是根因。
4. 当前 5 个 Tab 职责不清楚。Discover 像应用市场，Create 像模型 API 输入器，Plaza 像作品瀑布流，History 像控制台账单，Profile 像资产入口，但它们之间没有转化关系。
5. 概念需要重组：
   - “应用 / 模型 / 工作流”对用户统一叫“创作模板”或“生成方式”，高级用户再看到技术类型。
   - “作品”是社区内容，“任务”是运行记录，“生成结果”是资产。三者需要可跳转、可复用、可下载。
6. 首次打开不能快速理解产品能做什么。Discover 的应用卡片很丰富，但没有“我现在可以生成什么”的路径提示。
7. 生成一张图的最短路径不够短。Create 页入口是输入框 + 加号，缺少显性的“生成”按钮、预估费用、模型可信解释。
8. Plaza 不能自然复用参数。截图里只有浏览、喜欢、使用数，缺少“使用同款”作为主按钮和参数预填说明。
9. 失败后不够清楚。History 有失败状态，但没有稳定解释“失败原因、是否扣费、是否退款、下一步重试”。
10. 每次生成花费不清楚。History/Task Detail 同时出现 `2 CNY`、`37 RHB`、`RH币`、`优惠后金额`，没有主计价单位和扣费解释。

最严厉但必要的判断：当前版本的核心能力已经接近完成，但产品语言仍像把后台控制台、模型目录和社区瀑布流搬进手机。下一轮不应继续加功能，而要先把“发现内容 -> 生成 -> 状态 -> 结果 -> 再创作 -> 付费可信”这条闭环做成一条路。

---

# 二、核心产品原则

| 原则 | 含义 | 解决的问题 | 落地页面/组件 |
|---|---|---|---|
| 创作优先 | 任意入口都要能在 1-2 步进入生成确认 | 用户主路径不清楚 | Discover 卡片、Plaza 作品、Create 输入栏、App Detail |
| 结果可复用 | 每个结果都可保存、下载、复用参数、重试 | 历史只是记录 | History、Task Detail、Task Result、ResultPreview |
| 价格透明 | 生成前显示预计消耗，成功后显示实际扣费，失败说明退款 | 商业信任不足 | PriceBadge、BillingInfoCard、ConfirmDialog |
| 状态明确 | 提交、排队、运行、成功、失败、取消分开展示 | 用户不知道任务发生了什么 | TaskStatusBadge、HistoryTaskCard、Result 状态区 |
| 参数渐进披露 | 首屏只放模型、比例、分辨率、价格；高级参数进 Sheet | 参数压迫新手 | Create、App Detail、AdvancedSettingsSheet |
| 社区内容可转化 | Plaza 每个作品都是“查看同款参数 / 使用同款”的入口 | 广场只消费不转化 | PlazaWorkCard、作品详情页 |
| 历史即资产库 | 历史记录要能找回结果、复制 Prompt、复用参数、查看账单 | History 价值弱 | History、Task Detail、资产过期提示 |
| 技术概念后置 | 模型、端点、工作流、API 类型默认折叠到技术详情 | 新用户理解成本高 | ModelPickerSheet、Task Detail、App Detail |

---

# 三、信息架构重构

## 方案 A：低成本保留方案，1-3 天

| 当前 Tab | 新名称 | 核心职责 | 移入 | 移出 | 首屏 |
|---|---|---|---|---|---|
| Discover | 发现 | 找模板、应用、模型入口 | 应用市场、推荐模型、搜索 | 技术端点细节 | 顶部搜索 + 推荐模板 + 分类 |
| Create | 创作 | 直接生成 | Prompt、模型、常用参数、最近草稿 | 项目管理、复杂历史 | 大输入区 + 生成按钮 + 费用 |
| Plaza | 灵感 | 看作品并复用同款 | 作品流、作者、同款参数 | 纯模型筛选 | 瀑布流 + 使用同款入口 |
| History | 任务 | 运行状态、结果、账单、再创作 | 生成历史、任务详情、结果过期、重试 | 纯后台字段 | 状态筛选 + 任务卡 |
| Profile | 账户 | 会员、钱包、凭据、设置 | 钱包、会员、消费明细、API Key | 创作入口 | 用户资产卡 + 账户操作 |

优点：不动导航骨架，最适合快速止血。  
缺点：仍保留“发现”和“灵感”两个相近入口，需要靠页面职责文案和 CTA 区分。

## 方案 B：产品级重构方案，1-2 周

推荐底部导航：

1. 创作：默认首页，Prompt 输入、模型/模板选择、参数、费用确认。
2. 发现：应用、模型、工作流统一为“创作模板库”。
3. 灵感：社区作品流，核心 CTA 是“使用同款”。
4. 任务：运行中、历史、结果、账单、重试和复用。
5. 账户：钱包、会员、凭据、消费明细、设置。

主路径：

`创作输入 -> 选择生成方式 -> 价格确认 -> 生成中 -> 结果 -> 保存/复用/发布到灵感`

广场转化：

- 卡片底部保留喜欢/使用数，但作品详情页主按钮必须是“使用同款”。
- 复用后进入 Create，自动带入模型、比例、分辨率、Prompt、参考图，允许用户编辑。

历史转化：

- 成功任务：保存、下载、复用参数、再次生成、查看账单。
- 失败任务：查看原因、重试、复制任务 ID、退款状态。

钱包和会员前置：

- Create 顶部或生成按钮旁展示“余额 6.2k RHB”弱入口。
- 不在创作区插大会员广告；会员优惠只在价格确认里说明“已减免 X”。

优点：能把产品从功能集合变成创作闭环。  
缺点：需要改页面信息层级和组件系统，不只是改文案。

## 方案 C：激进长期方案

新结构：

1. 工作台：项目、草稿、生成队列、最近资产。
2. 模板库：模型、应用、工作流统一为可运行模板。
3. 灵感库：社区作品、收藏、同款复用。
4. 资产库：生成结果、本地保存、云端过期、下载状态。
5. 账户：钱包、会员、团队、API 凭据。

核心闭环：

`模板/灵感 -> 工作台编辑 -> 生成 -> 资产沉淀 -> 发布/复用 -> 增长`

长期需要新增概念：

- 模板：统一应用、模型、工作流。
- 工作台：承载草稿、队列、项目。
- 资产库：承载生成结果和保存状态。
- 灵感库：承载社区内容和收藏。

推荐：当前阶段采用方案 B 作为目标，但第一轮用方案 A 的低成本改名和 CTA 收束启动。C 不适合现在直接做，成本高且会推翻大量已完成页面。

---

# 四、核心用户流程重设计

## 流程 1：首页发现应用 -> 应用详情 -> 配置参数 -> 运行 -> 查看结果

- 当前问题：应用详情把封面、统计、作者、简介、参数、运行按钮堆在一起，价格缺失，运行后结果和账单不够闭环。
- 推荐流程：发现模板 -> 详情页确认用途 -> 默认参数 -> 价格确认 -> 任务进度 -> 结果页。
- 页面标题：发现模板 / 模板详情 / 确认生成 / 生成中 / 生成结果。
- 主按钮：查看详情 / 立即生成 / 确认消耗 X RHB / 查看结果 / 保存到本地。
- 次按钮：收藏、分享、查看作者、展开高级设置、复制任务 ID、再次生成。
- 默认展示参数：上传素材、Prompt、比例、分辨率、预计费用。
- 高级设置：端点、seed、negative prompt、质量档位、工作流节点细节。
- 生成前必须价格确认：需要。低价可用 inline confirm，高价或余额不足用 Dialog/Sheet。
- 生成中：显示提交、排队、生成、保存四段状态；保留任务 ID 和取消入口。
- 成功跳转：跳 Task Result，并在底部提供保存、复用、发布。
- 失败处理：Inline Error + “未扣费/已退回/已扣费原因” + 重试 + 联系支持。

## 流程 2：Create 直接输入 Prompt -> 选择模型 -> 生成

- 当前问题：空白区域太大，底部加号在无 Prompt 时像上传按钮，在有 Prompt 时才变生成，不够显性。
- 推荐流程：进入即看到输入框、模型、参数、费用和“生成”。
- Prompt 输入区：多行输入，顶部 placeholder 写“描述你要生成的画面”，右下角字数，左侧上传参考图。
- 模型选择：显示当前模型名 + 类型 + 价格，点击进入 ModelPickerSheet。
- 参数选择：只露出比例、分辨率、数量；高级设置进 Sheet。
- 加号按钮：不适合作为生成按钮。无 Prompt 时可表示添加素材；有 Prompt 后必须变成文字按钮“生成”或“¥2.00 生成”。
- 预估价格：按钮内显示 `生成 · 37 RHB` 或 `生成 · ¥2.00`；价格刷新中禁用提交。
- 余额不足：按钮禁用 + Inline Notice “余额不足，还差 12 RHB” + 充值入口。

## 流程 3：Plaza 作品 -> 查看作品 -> 复用同款 -> 生成

- 当前问题：瀑布流视觉强，但转化弱，作品卡只有喜欢和使用数，没有同款复用的主路径。
- 推荐流程：作品卡 -> 作品详情 -> 使用同款 -> Create 预填 -> 价格确认 -> 生成。
- 作品详情应有：大图/视频、作者、原 Prompt 摘要、模型/模板、可复用参数、相似作品。
- “使用同款”按钮：固定底部主按钮，文案为“使用同款生成”，旁边显示预计费用。
- 自动填充：模型、模板/SKU、Prompt、比例、分辨率、参考图、风格参数。
- 可修改：Prompt、比例、分辨率、数量、参考图；作者锁定参数只展示不可改。
- 作者保护：保留作者名和来源；复用结果可显示“基于 XXX 的公开参数创作”。
- 广场不应只是瀑布流：每张卡需要二级 CTA “查看参数”，详情页主 CTA “使用同款”。

## 流程 4：History -> 任务详情 -> 保存 / 下载 / 复用 / 重试

- 当前问题：History 已统一任务来源，但卡片像账单行，缺少用户动作优先级。
- 推荐流程：任务列表 -> 任务详情 -> 结果/失败说明 -> 操作。
- 列表卡片显示：缩略图、任务名、状态、生成方式、费用、时间、结果数、过期提示。
- 详情页显示：状态、结果、Prompt/参数、计费、任务 ID、错误/退款。
- 成功任务操作：查看结果、保存、下载、复用参数、再次生成、复制 Prompt。
- 失败任务操作：重试、查看原因、复制任务 ID、退款说明。
- 24 小时过期提示：结果卡顶部 Inline Notice，倒计时低于 6 小时加 warning。
- 本地保存状态：需要。输出项应有 `未保存 / 已保存 / 保存失败`。

## 流程 5：Profile -> 钱包 / 会员 / 消费明细

- 当前问题：RH 币、钱包、会员是三张并列数据，但没有解释谁用于生成、谁用于充值、谁影响折扣。
- 推荐流程：账户页资产卡 -> 钱包详情 -> 消费明细 -> 对应任务详情。
- 命名：UI 统一用 `RHB 点数` 作为主消耗单位；`CNY` 只用于钱包充值/人民币余额；“RH 币”作为营销名不要和 RHB 混用。
- 钱包余额：账户页显示摘要，Create/确认生成页显示小型余额。
- 会员权益：账户页显示剩余天数和权益；生成确认页只显示“会员已减免 X”。
- 消费明细：每条消费必须关联任务 ID，可跳 History detail。
- 花费解释：生成确认、任务详情、消费明细三处用同一 `BillingInfoCard`。

---

# 五、视觉设计方向

## 方向 A：专业工具型

目标是让 RunningHub 更像成熟 AI 创作工具，强调效率、清晰、可靠。适合高频创作者，也是当前最推荐的主方向。

## 方向 B：创作者社区型

目标是让 RunningHub 更像 AI 灵感社区，强调作品、作者、复用和互动。适合 Plaza 和作品详情等增长入口局部采用，不建议全局替代创作工具主线。

## 方向 C：未来科技型

目标是保留黑色背景和霓虹色，但把它们收敛成系统化的品牌语言。适合作为品牌层和少量重点场景，不建议作为当前版本主视觉。

| 方向 | 定位 | 用户 | 关键词 | 主色 | 辅助色 | 背景 | 卡片 | 字体 | 图标 | 图片 | 按钮 | Tab/Sheet | 成本 | 推荐 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| A 专业工具型 | 成熟 AI 创作工具 | 高频创作者 | 清晰、可信、效率 | `#B6FF00` | `#54D6FF` `#8D63FF` | `#050608` | 8-12dp 暗色面板 | 中等字重、层级明确 | 线性图标 | 结果优先 | 文字+价格主按钮 | 稳定 Sheet | 中 | 推荐 |
| B 创作者社区型 | 灵感社区 | 内容消费和轻创作 | 作品、作者、同款 | `#FFB86B` | `#B6FF00` | `#090806` | 图片沉浸卡 | 更情绪化 | 圆润图标 | 大图瀑布流 | 同款 CTA | 作品详情强 | 中高 | 次推荐 |
| C 未来科技型 | AI 平台品牌 | 技术用户 | 黑色、霓虹、平台感 | `#8D63FF` | `#B6FF00` `#16D8FF` | `#020203` | 发光边框少量使用 | 强标题 | 几何图标 | 暗色高对比 | 渐变按钮 | 赛博 Sheet | 高 | 不建议主用 |

主视觉建议：A 专业工具型。原因：当前最大缺口是效率和信任，不是吸睛。保留黑底和 BrandLime，但降低紫色霓虹占比，把价格、状态、参数和结果做清楚。Plaza 可局部采用 B 的作品导向，不要全局切成社区风格。

---

# 六、Design System 设计规范

## 1. 颜色 Token

| Token | 色值 | 使用场景 | 不应使用 |
|---|---:|---|---|
| background.primary | `#050608` | 页面背景 | 卡片内部 |
| background.secondary | `#090B0F` | 次级页面背景 | 主按钮 |
| background.gradient | `#050608 -> #10151C` | Profile/Header 弱渐变 | 大面积花哨 Hero |
| surface.default | `#11151A` | 普通卡片 | 页面背景 |
| surface.elevated | `#171B22` | Sheet/Dialog/浮层 | 列表底色 |
| surface.sunken | `#0B0E13` | 输入框、代码块 | 重要卡片 |
| surface.selected | `#202A14` | 选中 chip/tab | 错误态 |
| surface.disabled | `#1A1D22` | 禁用控件 | 可点击卡片 |
| text.primary | `#F5F7FA` | 标题/正文 | 次要说明 |
| text.secondary | `#C5CBD3` | 辅助正文 | 主标题 |
| text.tertiary | `#8B929D` | Meta/说明 | 低对比小字 |
| text.inverse | `#050608` | Lime 按钮文字 | 暗底文字 |
| border.default | `#2A3038` | 卡片边框 | 活跃态 |
| border.subtle | `#1B2028` | 分割线 | 交互焦点 |
| border.active | `#B6FF00` | 选中、焦点 | 错误态 |
| brand.primary | `#B6FF00` | 主 CTA、选中导航 | 大段背景 |
| brand.secondary | `#8D63FF` | 模型/创作辅助 | 金额/成功 |
| brand.muted | `#3D4D18` | 弱选中背景 | 主要文字 |
| status.success | `#4ADE80` | 成功状态 | 普通标签 |
| status.failed | `#FF4D55` | 失败/危险 | 价格 |
| status.processing | `#54D6FF` | 运行中/排队 | 成功 |
| status.warning | `#FBBF24` | 过期/余额不足 | 常规提示 |
| price.credit | `#B6FF00` | RHB 点数 | 现金余额 |
| price.money | `#FFD166` | CNY/钱包金额 | 主 CTA 背景 |
| overlay.scrim | `#000000B3` | 弹窗遮罩 | 卡片背景 |
| overlay.sheet | `#171B22F2` | Bottom Sheet | 页面背景 |

## 2. 字体系统

| 样式 | 字号 | 字重 | 行高 | 字间距 | 使用场景 |
|---|---:|---:|---:|---:|---|
| Display | 32sp | 700 | 38sp | 0 | 首页/页面大标题 |
| PageTitle | 26sp | 700 | 32sp | 0 | 页面标题 |
| SectionTitle | 18sp | 700 | 24sp | 0 | 分区标题 |
| CardTitle | 16sp | 650 | 22sp | 0 | 卡片标题 |
| Body | 14sp | 400 | 20sp | 0 | 正文 |
| BodyStrong | 14sp | 650 | 20sp | 0 | 重点正文 |
| Caption | 12sp | 400 | 16sp | 0 | 辅助说明 |
| Meta | 11sp | 500 | 14sp | 0 | ID、时间、统计 |
| Button | 15sp | 700 | 20sp | 0 | 按钮 |
| Price | 16sp | 700 | 22sp | 0 | 费用 |
| StatusBadge | 12sp | 700 | 16sp | 0 | 状态标签 |

## 3. 间距系统

统一 scale：4 / 8 / 12 / 16 / 20 / 24 / 32 / 40。

- 页面左右边距：16dp，极窄屏 14dp。
- 卡片内边距：12-16dp。
- 卡片间距：10-12dp。
- 列表间距：8-12dp。
- Bottom Sheet 内边距：16dp，顶部 handle 到标题 16dp。
- 顶部栏高度：56dp。
- 底部导航高度：72dp，含安全区。
- 按钮高度：48dp，大按钮 52dp，小按钮 36dp。
- 输入框高度：48dp，Prompt 输入 96-160dp。
- 标签高度：28-36dp。

## 4. 圆角系统

| Token | 值 | 使用 |
|---|---:|---|
| radius.xs | 4dp | 分割、进度条 |
| radius.sm | 8dp | Chip、图片卡、状态 Badge |
| radius.md | 12dp | 普通卡片、输入框 |
| radius.lg | 16dp | 重点卡片、结果预览 |
| radius.xl | 20dp | 创作输入面板 |
| radius.sheet | 24dp | Bottom Sheet 顶部 |
| radius.full | 999dp | Pill、头像、圆形按钮 |

## 5. 组件系统

| 组件 | 职责与样式 | 状态 | 页面 | 点击/动画 | Compose 注意点 |
|---|---|---|---|---|---|
| AppScaffold | 统一背景、Insets、Top/Bottom 容器 | default/loading/error | 全局 | 页面切换淡入 | 包装 `WindowInsets` 和 `LocalRhWindowInfo` |
| AppTopBar | 标题、返回、搜索、余额 | default/searching | Discover/Plaza/History | 搜索展开 | 避免每页硬编码高度 |
| AppBottomBar | 5 Tab 导航 | selected/disabled | 主导航 | 图标轻缩放 | 复用 `BottomNavTab` |
| PageHeader | 页面标题+副标题+操作 | loading | 二级页 | 无 | 控制标题字号 |
| CategoryTabs | 大分类切换 | selected/disabled | Discover/Plaza/History | 下划线移动 | 与 FilterChip 分开 |
| FilterChip | 过滤项 | selected/disabled | 列表页 | 背景过渡 | 最小 36dp 高 |
| SortDropdown | 排序 | open/closed | Discover/Plaza | Menu | 统一文案 |
| AppCard | 应用/模板卡 | loading/error | Discover | 进详情 | 图片比例稳定 |
| ModelCard | 模型列表项 | selected/loading | ModelPicker | 选中即生效 | 价格独立 slot |
| PlazaWorkCard | 作品卡 | default/video | Plaza | 预览/详情 | 卡内放“使用同款”入口 |
| HistoryTaskCard | 任务卡 | running/success/failed | History | 打开详情 | 状态/费用固定位置 |
| TaskStatusBadge | 任务状态 | queued/running/success/failed/canceled | 全流程 | 无 | 颜色语义固定 |
| PriceBadge | 费用 | loading/pending/free/amount | Create/Detail | 可点说明 | 支持 RHB/CNY |
| CreatorRow | 作者信息 | default/following | Detail/Plaza | 进作者页 | 头像 32/40 |
| ParameterSelector | 参数选择 | selected/error | Detail/Create | 打开 Sheet | 用统一 chip grid |
| AdvancedSettingsSheet | 高级参数 | open/error | Create/Detail | 完成/重置 | 内容最大 70% 高 |
| ModelPickerSheet | 模型选择 | loading/empty/error | Create | 搜索/选中 | 保留模型快照 |
| PromptInputBar | Prompt 输入 | empty/focused/error | Create | 输入/生成 | 字数和价格同屏 |
| UploadImageBox | 上传素材 | uploading/ready/error | Detail/Create | 选/删/重试 | 权限错误走 Sheet |
| PrimaryButton | 主动作 | loading/disabled | 全局 | 点击反馈 | 禁止只用图标表达生成 |
| SecondaryButton | 次动作 | disabled | 全局 | 点击 | 低对比边框 |
| GhostButton | 轻动作 | default | 结果/详情 | 点击 | 不抢主 CTA |
| IconButton | 工具动作 | selected/disabled | 顶栏/卡片 | 点击 | contentDescription 必填 |
| BottomSheet | 选择/设置 | open/dragging | 参数/模型 | 拖拽关闭 | 抽通用 handle 和 scrim |
| ConfirmDialog | 高风险确认 | default/loading | 扣费/退出 | 确认/取消 | 价格确认用它 |
| EmptyState | 空态 | default | 列表 | 主 CTA | 文案给下一步 |
| LoadingState | 加载 | skeleton/spinner | 全局 | 无 | 列表用 skeleton |
| ErrorState | 错误 | retry | 全局 | 重试 | 不透出原始服务端 message |
| ResultPreview | 结果展示 | image/video/expired | Result/History | 预览/保存 | Fit/Crop 可配置 |
| WalletBalanceCard | 余额 | normal/low | Profile/Create | 充值 | 统一 RHB/CNY |
| MembershipCard | 会员 | active/expired | Profile/Confirm | 续费 | 不放创作主页面大广告 |
| BillingInfoCard | 计费解释 | estimate/final/refund | Confirm/Detail | 展开明细 | 与任务 ID 关联 |

---

# 七、逐页重设计建议

## 1. Discover 首页

- 当前问题：Banner 很强但像广告位，应用卡片信息密度高却缺少“这是模板/模型/工作流”的统一解释。
- 重设计目标：把“发现可运行模板”讲清楚。
- 推荐布局：搜索栏 -> 推荐模板 Banner -> 分类 -> 应用卡片两列。
- 首屏：可搜索、今日推荐、3-5 个核心分类。
- 信息层级：标题 > 能力类型 > 价格/耗时/成功率 > 作者/使用数。
- 核心组件：AppTopBar、CategoryTabs、AppCard、SortDropdown。
- 主操作：查看详情 / 立即使用。
- 次操作：收藏、查看作者。
- 删减：卡片内过多营销字、重复标签。
- 前置：价格、生成类型、是否需要上传图。
- 视觉：减少超亮卡片之间的割裂，用统一遮罩和标题区。
- 交互：搜索保持页内结果，不要跳断主路径。
- 空/加载/错误：Skeleton 卡片、空态推荐 Create、错误可重试。
- 优先级：P1。

## 2. AI 应用详情页

- 当前问题：封面、统计、作者、简介都在参数前占位过多；运行前没有价格和余额。
- 目标：详情页变成“确认用途 + 配置 + 生成”。
- 布局：Hero 收短，标题区下方放价格/耗时/成功率，参数区默认展开必要项。
- 首屏：封面缩略、标题、类型、价格、主按钮。
- 信息层级：用途 > 必填参数 > 费用 > 高级参数。
- 核心组件：PageHeader、StatsRow、UploadImageBox、ParameterSelector、PrimaryButton。
- 主操作：确认生成。
- 次操作：收藏、作者、展开高级设置。
- 删减：大面积封面占比。
- 前置：费用、必填参数缺失。
- 视觉：参数卡统一 surface，按钮用 brand.primary。
- 交互：运行前价格确认，失败 inline。
- 状态：缺参数、上传中、价格待确认、余额不足。
- 优先级：P1。

## 3. 模型选择弹窗

- 当前问题：列表清楚，但技术标签强，缺少“适合做什么”和费用解释。
- 目标：让用户选生成方式，不只是选 API 模型。
- 布局：搜索 -> 类型 segmented control -> 推荐/最近/全部 -> ModelCard。
- 首屏：当前选中模型、价格、能力标签。
- 信息层级：模型名 > 能力 > 价格 > 速度/质量。
- 主操作：选择模型。
- 次操作：查看详情。
- 删减：默认暴露端点。
- 前置：价格和是否支持图生图/文生图。
- 视觉：选中态用边框+勾，不要只靠紫色。
- 交互：点击即选中，但底部可保留“完成”用于多参数确认版本。
- 状态：加载、空搜索、价格未知。
- 优先级：P1。

## 4. 参数配置弹窗

- 当前问题：字段名 `aspectRatio / resolution` 像开发调试界面。
- 目标：把技术字段翻译成用户参数。
- 布局：当前模型卡 -> 常用参数 -> 高级参数 -> 底部完成。
- 首屏：比例、清晰度、数量。
- 信息层级：常用 > 高级 > 技术详情。
- 主操作：完成。
- 次操作：恢复默认。
- 删减：端点 URL 默认展示。
- 前置：影响价格的参数。
- 视觉：使用 chip grid，选中态统一。
- 交互：改参数立即刷新价格。
- 状态：参数冲突、价格刷新中。
- 优先级：P1。

## 5. Create 创作页

- 当前问题：空白区域过大，生成动作弱，用户不知道当前模型是否可用、价格多少。
- 目标：成为全 App 的主创作入口。
- 布局：顶部标题/余额 -> 输入区 -> 模型/参数 -> 生成按钮 -> 运行状态。
- 首屏：Prompt 输入、模型、比例、分辨率、预计费用、生成按钮。
- 信息层级：Prompt > 生成方式 > 费用 > 高级参数。
- 主操作：生成。
- 次操作：上传参考图、选模型、高级设置。
- 删减：无意义空白。
- 前置：余额、价格、必填错误。
- 视觉：输入面板像工具，不像聊天气泡。
- 交互：无 Prompt 时按钮文案“添加素材”可以，但有 Prompt 后必须显示“生成”。
- 状态：Prompt 空、太短、价格确认、余额不足、生成中。
- 优先级：P0。

## 6. Plaza 广场页

- 当前问题：图片漂亮，但内容与创作工具断开。
- 目标：灵感消费转化为同款生成。
- 布局：灵感/短片 -> 分类 -> 作品流 -> 详情 Sheet。
- 首屏：作品卡必须显示“使用同款”或底部 CTA。
- 信息层级：作品 > 作者 > 参数摘要 > 使用数。
- 主操作：使用同款。
- 次操作：喜欢、收藏、查看作者。
- 删减：重复“精选”徽标。
- 前置：模型/模板类型、是否可复用。
- 视觉：保留沉浸图，但底部信息加可读 scrim。
- 交互：点击卡片进详情，不只开预览。
- 状态：无内容、图片加载失败、参数不可复用。
- 优先级：P2。

## 7. History 生成历史页

- 当前问题：像后台任务表，费用单位混乱，操作弱。
- 目标：任务状态 + 结果资产 + 账单入口。
- 布局：状态筛选 -> 过期提醒 -> 任务卡。
- 首屏：运行中优先，其次最近成功/失败。
- 信息层级：状态 > 结果缩略图 > 任务名 > 费用/耗时 > 操作。
- 主操作：查看结果/重试。
- 次操作：复用参数、复制 ID。
- 删减：不必要的长任务 ID 默认展示。
- 前置：费用和过期。
- 视觉：失败卡不应整卡红，只用状态和左边条。
- 交互：左滑/更多菜单承载次操作。
- 状态：空历史、筛选为空、网络错误。
- 优先级：P1。

## 8. Task Detail 任务详情页

- 当前问题：详情抽屉像 JSON/控制台，基础信息和计费信息按字段表展示，不像用户结果页。
- 目标：先告诉用户结果和扣费，再给技术详情。
- 布局：状态摘要 -> 结果 -> 费用解释 -> 参数 -> 技术详情折叠。
- 首屏：任务名、状态、实际扣费、结果预览。
- 信息层级：结果 > 扣费 > 参数 > 任务 ID。
- 主操作：保存/下载/重试。
- 次操作：复制 Prompt、复制任务 ID、查看技术详情。
- 删减：默认展开请求 JSON。
- 前置：失败原因、退款状态。
- 视觉：结果卡更大，字段表降级。
- 交互：技术详情折叠。
- 状态：结果为空、结果过期、详情加载失败。
- 优先级：P1。

## 9. Task Result 结果页

- 当前问题：结果预览存在，但请求信息太靠前，像调试页。
- 目标：让用户处理结果。
- 布局：结果预览 -> 保存/下载/复用 -> 过期提醒 -> 参数详情。
- 首屏：结果图/视频和保存按钮。
- 信息层级：结果 > 操作 > 过期 > 参数。
- 主操作：保存到本地。
- 次操作：再次生成、复制 Prompt、查看请求。
- 删减：默认大段 JSON。
- 前置：24 小时过期和保存状态。
- 视觉：结果使用最大可视区域。
- 交互：支持全屏预览。
- 状态：保存中、保存成功、保存失败、过期。
- 优先级：P1。

## 10. Profile 个人中心

- 当前问题：资产信息有了，但不解释 RH 币、钱包、会员之间关系。
- 目标：可信账户中心。
- 布局：用户信息 -> 资产总览 -> 会员权益 -> 钱包/消费明细 -> 设置。
- 首屏：RHB 点数、钱包余额、会员状态。
- 信息层级：可用余额 > 会员权益 > 交易入口 > 设置。
- 主操作：充值/查看明细。
- 次操作：续费、编辑资料、凭据管理。
- 删减：低频设置不要挤占资产卡。
- 前置：消费明细入口。
- 视觉：账户卡与商业卡分组。
- 交互：消费明细可跳任务。
- 状态：未登录、余额加载失败、会员过期。
- 优先级：P2。

---

# 八、交互状态和文案设计

| 状态 | 触发条件 | UI 表现 | 推荐文案 | 主操作 | 次操作 | 载体 |
|---|---|---|---|---|---|---|
| 首次进入 App | 无历史/新用户 | 首页引导卡 | 选择一个模板，或直接输入 Prompt 开始生成 | 开始创作 | 浏览模板 | Inline |
| 首页加载中 | 首屏数据请求 | Skeleton | 正在加载推荐内容 | 无 | 无 | Loading |
| 首页无推荐内容 | 列表为空 | EmptyState | 暂无推荐，试试直接创作 | 去创作 | 刷新 | Empty |
| 搜索无结果 | 搜索返回空 | EmptyState | 没找到相关模板 | 清空搜索 | 去创作 | Inline |
| 模型加载失败 | 模型目录失败 | ErrorState | 模型列表加载失败 | 重试 | 使用当前模型 | Inline |
| 模型价格获取失败 | fee preview 失败 | PriceBadge pending | 价格待确认，请稍后重试 | 重试计费 | 换模型 | Inline |
| 上传图片中 | 上传任务进行中 | 缩略图进度 | 上传中 %d%% | 取消 | 无 | Card |
| 上传图片成功 | 上传完成 | ready badge | 图片已上传 | 继续生成 | 删除 | Card |
| 上传图片失败 | 上传失败 | error badge | 上传失败，请重新选择 | 重试 | 删除 | Card |
| 删除上传图片 | 点删除 | Confirm/Undo | 已移除参考图 | 撤销 | 无 | Snackbar |
| 图片格式不支持 | 文件校验失败 | Error | 仅支持 JPG、PNG、WEBP | 重新选择 | 无 | Snackbar |
| Prompt 为空 | 点生成且为空 | 输入框错误 | 先输入你要生成的内容 | 聚焦输入框 | 上传参考图 | Inline |
| Prompt 太短 | 长度不足 | 输入框错误 | 描述太短，至少补充主体和风格 | 继续编辑 | 用示例 | Inline |
| 参数缺失 | 必填为空 | 参数红框 | 还缺少 %s | 去设置 | 恢复默认 | Inline |
| 参数冲突 | 服务端/本地冲突 | 参数提示 | 当前模型不支持该组合 | 自动修正 | 手动调整 | Inline |
| 余额不足 | fee passed=false | Warning card | 余额不足，还差 %s | 充值 | 换低价模型 | Bottom Sheet |
| 会员过期 | 会员权益失效 | Notice | 会员已过期，本次按普通价格计费 | 继续 | 续费 | Inline |
| 生成前价格确认 | 有费用 | ConfirmDialog | 本次预计消耗 %s，确认生成？ | 确认生成 | 取消 | Dialog/Sheet |
| 生成中 | RUNNING | 进度卡 | AI 正在生成，已用时 %s | 查看任务 | 取消 | Inline |
| 队列中 | QUEUING | 队列卡 | 已加入队列，预计稍后开始 | 后台运行 | 取消 | Inline |
| 生成成功 | SUCCESS | Result | 生成完成 | 查看结果 | 再来一张 | Snackbar + Result |
| 生成失败 | FAILED | Error card | 生成失败，%s | 重试 | 查看详情 | Inline |
| 任务取消 | CANCELED | Status card | 任务已取消 | 重新生成 | 关闭 | Inline |
| 任务重试 | 点重试 | Loading | 正在重新提交 | 无 | 取消 | Inline |
| 复制 Prompt | 点复制 | Snackbar | Prompt 已复制 | 无 | 无 | Snackbar |
| 复制任务 ID | 点复制 | Snackbar | 任务 ID 已复制 | 无 | 无 | Snackbar |
| 复用参数 | 从历史/广场 | Notice | 已带入同款参数，可继续调整 | 去生成 | 查看参数 | Snackbar |
| 保存到本地 | 点保存 | 保存状态 | 正在保存到本地 | 无 | 取消 | Inline |
| 云端结果 24 小时过期 | 输出有过期 | Warning | 云端结果仅保留 24 小时，请及时保存 | 保存 | 稍后 | Inline |
| 历史记录为空 | 无历史 | EmptyState | 暂无生成记录 | 去创作 | 浏览灵感 | Empty |
| 网络错误 | 请求失败 | ErrorState | 网络连接失败，请检查后重试 | 重试 | 稍后 | Inline |
| 钱包明细为空 | 无交易 | EmptyState | 暂无消费记录 | 去创作 | 充值 | Empty |
| 扣费成功 | 任务成功后 | BillingInfo | 已扣除 %s | 查看明细 | 无 | Inline |
| 扣费失败 | 支付异常 | Error | 扣费未完成，任务未提交 | 重试 | 联系支持 | Dialog |
| 失败是否退款 | 失败任务详情 | BillingInfo | 本次失败未扣费 / 已原路退回 | 查看明细 | 联系支持 | Inline |

---

# 九、商业化和信任感设计

## 关键判断

1. RH 币、RHB、CNY 现在命名混乱。
2. 需要统一主计价单位：面向创作消耗统一显示 `RHB 点数`；CNY 只用于钱包充值和现金余额。
3. 生成前必须显示预估费用。
4. 生成失败是否扣费必须明确。推荐规则：提交失败不扣费；生成失败按服务端策略显示“未扣费 / 已退回 / 已扣费”，不能留空。
5. 成功后扣费信息出现在 Result 顶部和 Task Detail 的 BillingInfoCard。
6. 钱包余额露出在 Create 顶部、生成确认、Profile 资产卡。
7. 会员权益只在账户页和价格确认里表达，不打断创作。
8. Task Detail 计费信息重排为“实际扣费、优惠、支付方式、退款状态、交易 ID”。
9. 消费明细必须和任务记录打通。
10. 用户必须能看到：预计费用、实际扣费、费用组成、失败退款。

## 新的价格展示规范

- 列表卡：`约 37 RHB` 或 `约 ¥2.00`。
- 生成按钮：`生成 · 37 RHB`。
- 确认弹窗：`预计消耗 37 RHB，余额 6.2k RHB`。
- 成功结果：`实际扣除 37 RHB，会员优惠 0 RHB`。
- 失败结果：`生成失败，未扣费` 或 `已退回 37 RHB`。
- CNY 仅写 `钱包 ¥103.18`，不和 RHB 同一行混排。

## 扣费确认流程

1. 用户完成必填参数。
2. 计费预览刷新。
3. 按钮显示费用。
4. 用户点击生成。
5. 若费用为 0，可直接提交并显示“免费生成”。
6. 若费用 > 阈值或首次扣费，弹确认 Sheet。
7. 成功后写入任务账单。
8. 失败后写明扣费/退款状态。

## 钱包卡片

- 主数值：`6,200 RHB 点数`。
- 次数值：`钱包余额 ¥103.18`。
- 行动：充值、明细。
- 风险提示：余额不足时显示 “预计还可生成约 N 次”。

## 会员卡片

- 主状态：`专业版 Plus · 剩余 13 天`。
- 权益：生成折扣、队列优先、云端保存时长。
- 行动：续费、查看权益。
- 不建议在 Create 首页大面积展示会员广告。

## 任务计费信息组件

字段顺序：

1. 实际扣费：37 RHB。
2. 计费单位：RHB 点数。
3. 原价：45 RHB。
4. 会员优惠：-8 RHB。
5. 支付方式：RHB 余额。
6. 失败/退款：未扣费 / 已退回。
7. 消费时间。
8. 关联任务 ID。

## 消费明细结构

- 时间。
- 类型：生成 / 退款 / 充值 / 会员。
- 金额：`-37 RHB` / `+37 RHB` / `+¥100.00`。
- 关联任务。
- 状态：成功 / 处理中 / 已退回 / 失败。
- 点击跳 Task Detail。

---

# 十、Kotlin Multiplatform / Compose 开发落地方案

## 1. 推荐包结构

短期不建议马上新增 Gradle 模块，先在 `composeApp` 内收敛 UI 系统，避免打乱已有分层。

```text
composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/
  theme/
    RhTheme.kt
    RhColors.kt
    RhTypography.kt
    RhSpacing.kt
    RhShapes.kt
  components/
    scaffold/
    navigation/
    buttons/
    cards/
    chips/
    sheets/
    states/
    billing/
    result/
  patterns/
    CreationConfirmFlow.kt
    TaskStatusPattern.kt
    ReuseParametersPattern.kt

composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/
  discovery/
  detail/
  quickcreate/
  plaza/
  history/
  profile/

feature/*/presentation/
  保持 UiState、StateHolder、Coordinator、稳定文案语义，不放 Compose 组件。
```

中期如果组件稳定，再考虑新增 `:core:designsystem` 或 `:core:ui`，但必须是纯 UI 模块，不依赖 Feature/Data。

## 2. Theme 设计

- `ColorTokens`：使用语义字段，例如 `backgroundPrimary`、`surfaceElevated`、`priceCredit`。
- `TypographyTokens`：不要直接裸用 Material 3 的 `headlineLarge` 表达业务层级，封装 `RhTextStyle.PageTitle`。
- `SpacingTokens`：固定 4dp scale，对应 `RhSpacing.xs/sm/md/lg/xl`。
- `ShapeTokens`：固定 `xs/sm/md/lg/xl/sheet/full`。
- `AppTheme`：`RunningHubTheme` 内继续包 MaterialTheme，同时提供 `LocalRhColors / LocalRhSpacing / LocalRhShapes`。
- 暗色默认：App 启动默认 dark，不跟随系统也可以，但要保留参数扩展。
- 浅色扩展：先让 token 成对，不急着实现浅色页面。

## 3. 组件拆分顺序

| 优先级 | 组件 | 原因 | 影响页面 |
|---|---|---|---|
| P0 | PrimaryButton / PriceBadge / TaskStatusBadge | 直接影响生成、扣费、状态可信 | Create、Detail、History |
| P0 | PromptInputBar | 主路径入口 | Create |
| P1 | BottomSheet / ModelPickerSheet / AdvancedSettingsSheet | 当前弹窗割裂明显 | Create、App Detail |
| P1 | AppCard / ModelCard / HistoryTaskCard | 列表体验统一 | Discover、Model、History |
| P1 | ResultPreview / BillingInfoCard | 结果和计费闭环 | Task Result、Task Detail |
| P2 | AppTopBar / AppBottomBar | 统一导航视觉 | 全局 |
| P2 | WalletBalanceCard / MembershipCard | 商业表达 | Profile、Confirm |

## 4. 页面迁移顺序

### 第一阶段：视觉统一，不改业务逻辑

- 改：Create、模型 Sheet、参数 Sheet、History 卡片。
- 新增：RhColors、RhButton、PriceBadge、TaskStatusBadge。
- Token：颜色、字体、圆角。
- 业务影响：无。
- 风险：局部硬编码未清干净。
- 验收：截图中 Create/History/Sheet 看起来属于同一系统；相关 compose/viewmodel 测试通过。

### 第二阶段：抽 Design System 组件

- 改：Discover、Plaza、Profile 的卡片和 Chip。
- 新增：AppCard、PlazaWorkCard、HistoryTaskCard、FilterChip。
- 业务影响：低。
- 风险：组件过早抽象导致 slot 不够。
- 验收：重复颜色/圆角减少，页面功能不回退。

### 第三阶段：重构核心生成流程

- 改：Create、App Detail、QuickCreate Coordinator 的计费确认入口。
- 新增：GenerationConfirmSheet、BillingInfoCard。
- 业务影响：中。
- 风险：生成前拦截错误、价格过期。
- 验收：Prompt 空、余额不足、价格待确认、成功、失败都有状态。

### 第四阶段：重构广场复用流程

- 改：Plaza 卡片、作品详情、复用参数到 Create。
- 新增：ReuseTemplateSheet。
- 业务影响：中。
- 风险：公开作品参数不完整。
- 验收：作品详情能一键带参数进入 Create。

### 第五阶段：重构历史和计费流程

- 改：History、Task Detail、Result。
- 新增：TaskDetailScreen/Sheet、BillingInfoCard、ResultActionBar。
- 业务影响：中高。
- 风险：多来源历史字段不一致。
- 验收：成功/失败/退款/过期/保存状态可读。

### 第六阶段：优化个人中心和商业化表达

- 改：Profile、钱包明细、会员卡。
- 新增：WalletBalanceCard、MembershipCard、TransactionList。
- 业务影响：中。
- 风险：后端字段命名和 UI 计价单位不一致。
- 验收：消费明细可跳任务详情。

## 5. Compose 实现注意事项

- 避免重复样式：禁止新页面直接 `Color(0x...)`，统一从 token 取；例外需要局部语义命名。
- 状态管理：`selected/loading/error/disabled` 放进组件参数，不让每个页面自己拼颜色。
- Bottom Sheet：抽 `RhBottomSheetScaffold(handle, title, actions, content)`，保留拖拽和 insets。
- 列表卡片：使用 slot，固定缩略图、状态、费用、操作区域尺寸。
- 瀑布流：Plaza 继续用 StaggeredGrid，但卡片 CTA 和 scrim 固定。
- SafeArea/Insets：AppScaffold 统一处理，不在页面底部重复 padding。
- 屏幕适配：继续使用 `LocalRhWindowInfo`，但组件宽高不要依赖 magic number。
- Android/iOS 一致：权限、文件选择保留 expect/actual；UI 状态不要持有平台 URI 类型。
- Preview：用 `RhPreviewData` 扩展每个核心状态。
- 降低大改风险：每轮只替换一个组件族，先保留 ScreenModel 和 StateHolder。

---

# 十一、最终输出总结

## 当前最严重的 10 个问题

1. 主定位不清：创作、市场、社区、控制台混在一起。
2. 生成路径不够明确：Create 页主按钮表达弱。
3. 价格体系不可信：RHB/RH 币/CNY 混排。
4. 失败和退款状态不清楚。
5. Plaza 不能自然转化为创作。
6. History 没有成为资产和再创作入口。
7. 任务详情像后台字段表，不像用户结果页。
8. Design System 有基础但不成体系，QuickCreate 私有视觉割裂。
9. 参数字段技术化，缺少用户语义。
10. 空/错/加载状态没有形成统一文案和组件。

## 推荐定位

AI 创作工作台，辅助为模板库、灵感社区、任务资产和钱包会员。

## 推荐信息架构

采用方案 B：创作 / 发现 / 灵感 / 任务 / 账户。第一轮用方案 A 的低成本动作先落地。

## 推荐视觉方向

专业工具型：保留暗色和 BrandLime，但降低霓虹紫，用清晰的状态、费用、参数和结果层级建立可信度。

## 三套视觉方案对比

| 方案 | 优势 | 风险 | 当前建议 |
|---|---|---|---|
| 专业工具型 | 快速提升效率和信任 | 情绪感较弱 | 主方向 |
| 创作者社区型 | Plaza 转化更强 | 可能削弱工具效率 | 局部用于 Plaza |
| 未来科技型 | 品牌冲击强 | 容易像 Demo/游戏皮肤 | 暂不主用 |

## 新底部导航

`创作 / 发现 / 灵感 / 任务 / 账户`

## 新核心路径

`输入或选择模板 -> 配置必要参数 -> 价格确认 -> 生成中状态 -> 结果处理 -> 历史复用或发布灵感`

## Token 草案

优先实现颜色、字体、间距、圆角四类语义 token：`background.*`、`surface.*`、`text.*`、`brand.*`、`status.*`、`price.*`。

## 关键组件

P0：PromptInputBar、PrimaryButton、PriceBadge、TaskStatusBadge、BillingInfoCard、ResultPreview。  
P1：ModelPickerSheet、AdvancedSettingsSheet、AppCard、HistoryTaskCard、PlazaWorkCard。  
P2：WalletBalanceCard、MembershipCard、AppTopBar、AppBottomBar。

## 页面优先级

| 页面 | 优先级 |
|---|---|
| Create | P0 |
| AI 应用详情 | P1 |
| 模型/参数 Sheet | P1 |
| History | P1 |
| Task Detail/Result | P1 |
| Discover | P1 |
| Plaza | P2 |
| Profile | P2 |

## 1-2 天低成本改造

- 导航中文化或职责明确化：发现/创作/灵感/任务/账户。
- Create 按钮改成有文案的“生成 · 预计费用”。
- History 卡片统一费用单位显示。
- 任务详情默认折叠 JSON，请求信息移到技术详情。
- 补 Prompt 空、余额不足、价格待确认三类状态。

## 3-5 天中成本改造

- 抽 `PriceBadge / TaskStatusBadge / PrimaryButton / FilterChip`。
- 重做 ModelPicker 和 ParamsSheet 的视觉与字段命名。
- Task Detail 改成结果优先布局。
- Profile 加 WalletBalanceCard 和消费明细入口。

## 1-2 周产品级改造

- 完成方案 B 的导航和页面职责。
- Plaza 增加作品详情和使用同款流程。
- History 打通复用参数、结果保存、消费明细。
- 生成前确认和生成后账单闭环完整落地。

## 长期版本建议

- 引入工作台、模板库、灵感库、资产库四个长期概念。
- 把生成结果沉淀为资产，不只是历史输出。
- 把社区内容转化为可复用模板。
- 会员权益围绕生成效率和资产保存，而不是只做价格促销。

## 下一步建议

下一步最应该做“Create + ModelPicker + ParamsSheet + BillingInfoCard”的第一轮落地设计和代码改造。这四处是截图里成熟度最低、但代码已有计费和状态基础的区域，改动收益最大，风险可控。
