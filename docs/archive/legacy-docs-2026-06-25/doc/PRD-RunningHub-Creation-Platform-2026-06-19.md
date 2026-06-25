# PRD: RunningHub 创作中台、广场、历史与统一 API 模型

> 日期：2026-06-19
> 状态：Draft
> 设计稿：`docs/superpowers/specs/2026-06-19-runninghub-creation-plaza-history-design.md`

## 1. Summary

本文档定义 RunningHub 移动端下一阶段的核心产品改造：统一 API 模型管理与调用、生成历史、广场，以及品牌配色统一。

目标是把当前分散的快捷创作、历史、静态工作坊和硬编码模型调用，整理成一个清晰的创作中台。用户可以找到模型、调用模型、查看结果、复用参数，并浏览广场内容。

## 2. Contacts

| Name | Role | Comment |
|---|---|---|
| 用户 | Product Owner | 提出目标、确认方案、决定发布范围。 |
| Codex | Product / Design / Engineering Agent | 整理抓包文档，产出设计、草图、PRD 和后续实现。 |
| 后端/API 维护者 | API Owner | 需要确认未抓到的写接口、LLM 调用接口和字段稳定性。 |

## 3. Background

RunningHub 当前客户端已经有 KMP 架构、Compose UI、Ktor API、Koin 注入、快捷创作、发现页和部分历史能力。

新的抓包文档补齐了三类关键信息：

- 标准 API 模型可以通过 `/api/sku/list` 和 `/api/sku/detail` 建立目录和动态表单。
- 广场内容可以通过 `/api/portal/creation/list`、`/api/creation/detail` 和短片接口读取。
- 官网品牌已经变为高对比暗色体系，主强调色是 `#CCFF00`。

现在的问题是：客户端仍有大量硬编码模型 endpoint，`Community` 仍是静态工具卡，历史入口和数据源分散，配色也停留在旧紫蓝体系。

## 4. Objective

### Objective

把 RunningHub 移动端改造成一个更完整的 AI 创作中台。

用户应能：

- 浏览可用模型。
- 根据模型字段填写参数。
- 上传素材。
- 提交任务并查询结果。
- 在一个地方管理生成历史。
- 浏览广场作品和短片。
- 在统一品牌视觉下使用这些功能。

### Why It Matters

这会降低模型接入成本，也降低用户理解成本。新增模型时，客户端不应每次都写一套硬编码表单和 endpoint。用户生成内容后，也需要稳定的历史和项目管理入口。

### Key Results

| Key Result | Measurement |
|---|---|
| KR1 | 标准模型目录不再依赖硬编码枚举，首版至少可读取 `/api/sku/list` 和 `/api/sku/detail`。 |
| KR2 | 首版统一表单可渲染 `STRING`、`NUMBER`、`BOOLEAN`、`LIST`、`IMAGE/VIDEO/AUDIO` 这几类字段。 |
| KR3 | 用户可从一级入口进入生成历史，并查看至少快捷创作任务列表和详情。 |
| KR4 | 用户可从一级入口进入广场，并浏览灵感流、分类、排序和短片列表。 |
| KR5 | 新增核心界面使用官网暗色品牌 token，不再新增紫蓝渐变主视觉。 |

## 5. Market Segments

### AI 创作者

他们需要快速找到合适模型，生成图片、视频、音频，并管理结果。

约束：

- 移动端屏幕有限。
- 生成任务可能耗时。
- 输出链接可能过期。

### 模型 API 使用者

他们需要清楚知道模型字段、价格、endpoint 和调用状态。

约束：

- 模型字段很多，且会变化。
- 部分模型字段结构复杂。
- LLM 与标准异步模型不是同一种调用方式。

### 内容浏览用户

他们需要从广场找灵感，看别人生成了什么，并可能复用模型或模板。

约束：

- 写操作接口未完整抓取。
- 媒体 URL 可能有时效。
- 图片和视频混排需要稳定性能。

## 6. Value Propositions

### 对创作者

- 少找入口：创作、广场、历史都是一级入口。
- 少丢结果：所有生成任务集中管理。
- 少重复输入：历史任务可以复用参数。

### 对开发者

- 少硬编码：模型字段来自接口 schema。
- 更好维护：模型目录、调用、历史、广场分仓储边界。
- 更易测试：数据转换和表单渲染可以独立测。

### 对产品

- 更像完整平台，不只是工具集合。
- 新模型上线更快。
- 广场内容可以提升留存和复用。

## 7. Solution

### 7.1 UX / Prototypes

采用 5 个主导航：

- `发现`
- `创作`
- `广场`
- `历史`
- `我的`

草图方向已生成：

- 创作中台：`docs/superpowers/specs/assets/wireframe-01-creation-hub.png`
- 广场优先：`docs/superpowers/specs/assets/wireframe-02-plaza-first.png`
- 历史控制台：`docs/superpowers/specs/assets/wireframe-03-history-console.png`

首版推荐以“创作中台”为主线，实现统一模型目录和动态表单。广场和历史作为一级入口同步规划，分阶段开发。

### 7.2 Key Features

#### 统一 API 模型管理

- 标准模型列表。
- 标准模型详情。
- 字段 schema 解析。
- 模型分类、搜索、筛选。
- 价格摘要和 endpoint 展示。
- LLM 模型目录只做展示或预留，调用等接口确认后再做。

#### 统一模型调用

- 根据字段 schema 生成表单。
- 支持文本、数字、布尔、枚举、上传字段。
- 支持媒体上传。
- 支持标准模型任务提交。
- 支持任务查询。
- 显示错误、排队、运行、成功、失败状态。

#### 生成历史

- 一级 `历史` tab。
- 状态筛选：全部、进行中、成功、失败。
- 来源标识：快捷创作、API 模型、WebApp。
- 任务详情。
- 输出预览。
- 复用参数。
- 运行中任务取消。
- 项目筛选和置顶项目。

#### 广场

- 一级 `广场` tab。
- 灵感流：推荐、最热、最新。
- 分类标签。
- 瀑布流媒体卡片。
- 作品详情。
- 评论读取。
- 短片 tab。
- 首版不做点赞、收藏、评论发布、关注等写操作，除非补充抓包。

#### 统一配色

核心 token：

| Role | Value |
|---|---|
| `brand.lime` | `#CCFF00` |
| `base.black` | `#000000` |
| `surface.900` | `#080808` |
| `surface.850` | `#09090B` |
| `surface.800` | `#18181B` |
| `surface.700` | `#27272A` |
| `text.primary` | `#FFFFFF` |
| `text.default` | `#EFEFEF` |
| `text.secondary` | `#D8D8D8` |
| `text.muted` | `#9DA2A8` |
| `status.error` | `#FF4144` |

`#CCFF00` 只用于主 CTA、选中态、关键提示和运营重点，不做大面积普通列表背景。

### 7.3 Technology

#### Shared Layer

新增或重构仓储：

- `ModelCatalogRepository`
- `ModelInvocationRepository`
- `PlazaRepository`
- `GenerationHistoryRepository`

新增域模型：

- `ApiModelSummary`
- `ApiModelDetail`
- `ApiModelField`
- `ApiModelFieldOption`
- `ModelInvocationRequest`
- `ModelInvocationTask`
- `PlazaCreationCard`
- `PlazaCreationDetail`
- `GenerationHistoryItem`

#### Compose UI

新增或改造：

- `ui/feature/modelcatalog`
- `ui/feature/create`
- `ui/feature/plaza`
- `ui/feature/history`
- `ui/theme` token
- `ui/navigation/MainScreen.kt`

#### Tests

优先测试：

- `inputConfigJson` 字段解析。
- endpoint 拼接规则。
- request body 构造。
- 历史数据归一化。
- 广场 DTO 映射。
- theme token 不再引用旧主色作为新主视觉。

### 7.4 Assumptions

- `/api/sku/list` 和 `/api/sku/detail` 在登录态下可由移动端访问。
- 标准模型调用统一为 `/openapi/v2{rhEndpoint}`。
- `/openapi/v2/query` 可查询标准模型任务结果。
- 广场写操作暂不做，因为写接口未完整抓取。
- LLM 调用暂不做，因为当前只确认了模型目录接口。
- 输出 URL 可能过期，所以历史页要提示用户尽快保存。

## 8. Release

### Version 1

范围：

- 新品牌 token。
- 5-tab 信息架构。
- 模型目录读取。
- 模型详情读取。
- 基础 schema 表单。
- 标准模型提交和查询。
- 生成历史一级入口。
- 广场只读列表。

不做：

- LLM streaming。
- 广场写操作。
- 完整替换所有旧硬编码模型。
- 大规模重构 legacy `app/`。

### Version 2

范围：

- 更多字段类型。
- 更强表单条件渲染。
- 历史参数复用完善。
- 项目管理完善。
- 广场详情增强。
- 写操作补充。

### Version 3

范围：

- LLM 调用。
- 流式响应。
- 模型收藏。
- 模型推荐策略。
- 本地缓存和离线草稿。

## Verification Plan

开发完成后至少验证：

- `gradle :shared:compileKotlinAndroid`
- `gradle :composeApp:assembleDebug`
- 相关单元测试。
- Android 真机或模拟器手动检查：
  - 创作表单渲染。
  - 媒体上传。
  - 任务提交。
  - 历史列表和详情。
  - 广场列表和详情。
  - 暗色主题和底部导航。

## Document Links

- Design: `docs/superpowers/specs/2026-06-19-runninghub-creation-plaza-history-design.md`
- API capture: `doc/RunningHub-call-api-models-capture-2026-06-18.md`
- Explore capture: `doc/RunningHub-explore-capture-2026-06-19.md`
- Color capture: `doc/RunningHub-home-color-palette-capture-2026-06-19.md`
