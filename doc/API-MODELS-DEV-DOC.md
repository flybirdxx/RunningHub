# RunningHub 快捷创作 API 模型开发文档

> **文档版本：** v2.0.0
> **创建日期：** 2026-05-01
> **数据来源：** [RunningHub 官方 API 文档](https://www.runninghub.cn/runninghub-api-doc-cn)
> **⚠️ 重要声明：** 以下所有参数定义均来自 RunningHub 官方 OpenAPI 规范，**与当前代码中的 DTO 定义存在大量不一致**，请以本文档为准。
> **API 基础地址：** `https://www.runninghub.cn`

---

## ⚠️ 一、代码与官方文档的重大差异（必读）

> 当前代码中的 DTO 定义与官方 API 规范存在**系统性偏差**，以下为典型问题汇总。建议优先对照本节核实各模型参数。

### 1.1 问题汇总表

| 模型 | 问题类型 | 官方文档 | 代码 DTO |
|------|---------|---------|---------|
| **全能图片 G-2.0** 图生图 | 字段名错误 | `imageUrls`（数组，最多10张） | `imageUrl`（单图） |
| **全能图片 G-2.0** 全部 | 缺失必填字段 | `quality` 必填（low/medium/high） | 无此字段 |
| **全能图片 G-2.0** 文生图 | 默认值错误 | `resolution` 默认 `2k` | 默认 `1K` |
| **全能图片 G-2.0** 文生图 | 缺失必填字段 | `quality` 必填，`aspectRatio` 必填 | 均缺失 |
| **全能图片 X** 文生图 | 无中生有字段 | 仅 `prompt`、`aspectRatio`、`outputFormat` | 多了 `resolution`、`batch_count`、`negative_prompt`、`seed` |
| **Seedream v4/v5** 图生图 | 字段名错误 | `imageUrls`（数组，最多10张） | `imageUrl`（单图） |
| **Seedream v4/v5** | 新增参数 | `width`、`height`、`sequentialImageGeneration`、`maxImages` | 均缺失 |
| **Seedream v5-lite** 文生图 | 新增参数 | `width`、`height`、`sequentialImageGeneration`、`maxImages`、`toolsType` | 均缺失 |
| **万相 2.7** 图生图 | 必填字段错误 | `firstImageUrl` 必填，`resolution` 必填，`prompt` 可选 | `prompt` 必填，`firstImageUrl` 命名不符 |
| **万相 2.7** | 默认值错误 | `resolution` 必填（无默认值） | 默认 `"720P"` |
| **可灵 V3-4K** | 端点路径错误 | `/kling-v3-4k/text-to-video` | 代码中未接入此端点 |
| **可灵 V3-4K** 文生图 | 新增参数 | `cfgScale`、`shotType`、`multiPrompt`、`elementList` | 均缺失 |
| **可灵 O1** 图生图 | 字段名错误 | `firstImageUrl`（必填） | `imageUrl`（命名不符） |
| **可灵 O1** | 新增参数 | `mode`（std/pro） | 缺失 |
| **全能视频 S Pro** | 端点路径错误 | `/rhart-video-s-official/text-to-video-pro` | 代码中端点路径为 `/rhart-video-s-official/text-to-video-pro`（存在但未被路由调用） |
| **全能视频 S Pro** | 参数名错误 | `size`（必填，格式如 `720x1280`） | `resolution`（不匹配） |
| **全能视频 S Pro** | 缺失必填 | `duration` 必填 | — |
| **旧 S/G 档端点** | 未使用 | S档：`/rhart-video-s-official/text-to-video-pro` | 定义了但从未被路由调用（dead code） |

---

## 二、通用接口

### 2.1 任务状态查询

**端点：** `GET /openapi/v2/query?taskId={taskId}`

**响应参数：**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| `taskId` | String | 任务 ID |
| `status` | String | `QUEUED` / `RUNNING` / `SUCCESS` / `FAILED` |
| `errorCode` | String | 错误码 |
| `errorMessage` | String | 错误信息 |
| `results[].url` | String | 输出文件 URL |
| `results[].outputType` | String | 输出类型（png/jpg/mp4 等） |
| `results[].text` | String | 文本输出内容 |
| `clientId` | String | 客户端标识 |
| `promptTips` | String | Prompt 执行反馈（JSON 字符串） |
| `failedReason` | Object | 失败详情 |
| `usage.consumeMoney` | String | 平台消耗金额 |
| `usage.consumeCoins` | String | 平台消耗点数 |
| `usage.taskCostTime` | String | 任务耗时（秒） |

### 2.2 媒体上传

**端点：** `POST /openapi/v2/media/upload/binary`

**Header：** `Authorization: Bearer <apiKey>`

**FormData：** `file`（二进制文件）

**响应：** `{ code, message, data: { download_url, type, size, fileName } }`

---

## 三、图片生成模型

### 3.1 模型总表

| 模型 | API 端点（文生图） | API 端点（图生图） | 支持功能 |
|------|-------------------|-------------------|---------|
| 全能图片 G-2.0 | `/rhart-image-g-2-official/text-to-image` | `/rhart-image-g-2-official/image-to-image` | 文生图、图生图 |
| 全能图片 X | `/rhart-image-x-official/text-to-image` | — | 文生图（图片编辑见编辑接口） |
| 全能图片 Pro | `/rhart-image-n-pro-official/text-to-image-ultra` | `/rhart-image-n-pro-official/edit` | 文生图Ultra、图生图 |
| 全能图片 V2 | 需确认 | `/rhart-image-v2-official/image-to-image` | 图生图 |
| Seedream 5.0 Lite | `/seedream-v5-lite/text-to-image` | `/seedream-v5-lite/image-to-image` | 文生图、图生图 |
| Seedream 4.0 | — | `/seedream-v4/image-to-image` | 仅图生图 |

---

### 3.2 全能图片 G-2.0

**供应商：** RunningHub 自研  
**特点：** 最新一代多模态大模型，支持任意比例自定义分辨率（最大边长 3840px）

#### 3.2.1 文生图

**端点：** `POST /openapi/v2/rhart-image-g-2-official/text-to-image`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 图像描述提示词，1-20000字符 | — |
| `aspectRatio` | String | ✅ | — | 输出图像宽高比 | `1:1` / `2:3` / `3:2` / `3:4` / `4:3` / `4:5` / `5:4` / `9:16` / `16:9` / `21:9` |
| `resolution` | String | ✅ | — | 输出图像分辨率档位 | `1k` / `2k` / `4k` |
| `quality` | String | ✅ | — | 生成质量 | `low` / `medium` / `high` |

#### 3.2.2 图生图

**端点：** `POST /openapi/v2/rhart-image-g-2-official/image-to-image`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 编辑指令描述，1-20000字符 | — |
| `imageUrls` | Array\<String\> | ✅ | — | 参考图片 URL 数组，支持 1-10 张 | JPG / JPEG / PNG / WEBP |
| `aspectRatio` | String | ❌ | `16:9` | 输出图像宽高比 | 同上 |
| `resolution` | String | ✅ | — | 输出图像分辨率档位 | `1k` / `2k` / `4k` |
| `quality` | String | ✅ | — | 生成质量 | `low` / `medium` / `high` |

---

### 3.3 全能图片 X

**供应商：** RunningHub 自研  
**特点：** 支持 11 种预设宽高比，输出 JPEG 或 PNG

#### 3.3.1 文生图

**端点：** `POST /openapi/v2/rhart-image-x-official/text-to-image`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 提示词，5-20000字符 | — |
| `aspectRatio` | String | ❌ | `1:1` | 宽高比 | `2:1` / `20:9` / `16:9` / `4:3` / `3:2` / `1:1` / `2:3` / `3:4` / `9:16` / `9:20` / `1:2` |
| `outputFormat` | String | ✅ | — | 输出格式 | `jpeg` / `png` |

> ⚠️ **代码问题：** 代码 DTO 中有 `resolution`、`batch_count`、`negative_prompt`、`seed` 字段，但官方文档中**完全不存在这些参数**。

#### 3.3.2 图片编辑

**端点：** `POST /openapi/v2/rhart-image-x-official/image-to-image`

> 需进一步确认官方文档参数（文档路径：`/api-448969293`）

---

### 3.4 全能图片 Pro

**供应商：** RunningHub 自研（针对移动端优化）

#### 3.4.1 文生图 Ultra

**端点：** `POST /openapi/v2/rhart-image-n-pro-official/text-to-image-ultra`

**请求参数：**（需确认，文档摘要显示支持 4K/8K 级工业素材、多语言文字渲染）

#### 3.4.2 图生图

**端点：** `POST /openapi/v2/rhart-image-n-pro-official/edit`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `imageUrls` | Array\<String\> | ✅ | — | 参考图片 URL 数组，支持 JPG/PNG，最多10张 | — |
| `prompt` | String | ✅ | — | 文本描述，1-20000字符 | — |
| `resolution` | String | ❌ | 自适应 | 输出分辨率 | `1k` / `2k` / `4k` |
| `aspectRatio` | String | ❌ | 自适应 | 图片纵横比 | `1:1` / `3:2` / `2:3` / `3:4` / `4:3` / `4:5` / `5:4` / `9:16` / `16:9` / `21:9` |

---

### 3.5 全能图片 V2

#### 3.5.1 图生图（官方稳定版）

**端点：** `POST /openapi/v2/rhart-image-v2-official/image-to-image`

**特点：** 支持最多 14 张参考图，高达 4K 输出

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `imageUrls` | Array\<String\> | ✅ | — | 参考图片 URL 数组（最多14张） | JPG / PNG |
| `prompt` | String | ✅ | — | 编辑指令描述 | — |
| `resolution` | String | ❌ | — | 输出分辨率 | `1k` / `2k` / `4k` |
| `aspectRatio` | String | ❌ | — | 宽高比 | `1:1` / `16:9` / `9:16` / `4:3` / `3:4` / `3:2` / `2:3` / `5:4` / `4:5` / `21:9` / `1:4` / `4:1` / `1:8` / `8:1` |

---

### 3.6 Seedream 系列

**供应商：** 字节跳动（豆包大模型）  
**特点：** 支持结构化提示词、图生图多图融合、文生图序列生成

#### 3.6.1 Seedream 5.0 Lite — 文生图

**端点：** `POST /openapi/v2/seedream-v5-lite/text-to-image`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 提示词，5-2000字符 | — |
| `width` | Int | ❌ | `2048` | 输出宽度（8的倍数，1600-4704） | — |
| `height` | Int | ❌ | `2048` | 输出高度（1344-4096） | — |
| `sequentialImageGeneration` | String | ❌ | `disabled` | 文生组图功能 | `disabled` / `auto` |
| `maxImages` | Int | ❌ | `1` | 最大生成图片数（1-15） | — |
| `toolsType` | String | ❌ | `web_search` | 工具类型 | `web_search` |
| `resolution` | String | ❌ | — | 分辨率（优先级高于 width×height） | `2k` / `3k` |

> ⚠️ **代码问题：** 代码中仅有 `resolution`、`style`、`batch_count`，缺失 `width`、`height`、`sequentialImageGeneration`、`maxImages`、`toolsType`。

#### 3.6.2 Seedream 5.0 Lite — 图生图

**端点：** `POST /openapi/v2/seedream-v5-lite/image-to-image`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 提示词，5-2000字符 | — |
| `imageUrls` | Array\<String\> | ✅ | — | 参考图片 URL 数组（最多10张） | PNG / JPEG |
| `width` | Int | ❌ | `2048` | 输出宽度（8的倍数，1600-4704） | — |
| `height` | Int | ❌ | `2048` | 输出高度（1344-4096） | — |
| `sequentialImageGeneration` | String | ❌ | `disabled` | 参考图生组图功能 | `disabled` / `auto` |
| `maxImages` | Int | ❌ | `1` | 最大生成图片数（1-15） | — |
| `resolution` | String | ❌ | — | 分辨率（优先级高于 width×height） | `2k` / `3k` |

> ⚠️ **代码问题：** 代码 DTO 中使用 `imageUrl`（单图）而非 `imageUrls`（数组），且缺失 `width`、`height`、`sequentialImageGeneration`、`maxImages`。

#### 3.6.3 Seedream 4.0 — 图生图

**端点：** `POST /openapi/v2/seedream-v4/image-to-image`

**特点：** 精准换装、妆容调整、产品重塑，保持主体身份一致

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 提示词，5-2000字符，支持结构化格式 | — |
| `imageUrls` | Array\<String\> | ✅ | — | 参考图片 URL 数组（最多10张） | PNG / JPEG |
| `width` | Int | ❌ | `2048` | 输出宽度（8的倍数，512-8192） | — |
| `height` | Int | ❌ | `2048` | 输出高度（512-8192） | — |
| `sequentialImageGeneration` | String | ❌ | `disabled` | 序列生成功能 | `disabled` / `auto` |
| `maxImages` | Int | ❌ | `1` | 最大生成图片数（1-15） | — |
| `resolution` | String | ❌ | — | 分辨率（优先级高于 width×height） | `1k` / `2k` / `4k` |

---

## 四、视频生成模型

### 4.1 模型总表

| 模型 | 供应商 | 文生视频 | 图生视频 | 首尾帧 | 视频续写 | 默认时长 |
|------|--------|---------|---------|--------|---------|---------|
| 可灵 V3-4K | 快手 | ✅ | ❌ | ❌ | ❌ | 5s |
| 可灵 O3-Pro | 快手 | ✅ | ✅ | ❌ | ❌ | 5s |
| 可灵 O3-Std | 快手 | ✅ | ✅ | ❌ | ❌ | 5s |
| 可灵 O1 | 快手 | ✅ | ✅ | ✅ | ❌ | 5s |
| Seedance 2.0 | 字节跳动 | ✅ | ✅ | ✅ | ❌ | 5s |
| 万相 2.7 | 阿里通义 | ✅ | ✅ | ❌ | ✅ | 5s |
| 万相 2.6 | 阿里通义 | ✅ | ✅ | ✅ | ❌ | 5s |
| Vidu Q3-Turbo | Vidu | ✅ | ✅ | ✅ | ❌ | 5s |
| 全能视频 S Pro | RunningHub | ✅ | ✅ | ❌ | ❌ | 12s |
| 全能视频 X | RunningHub | ✅ | ✅ | ❌ | ❌ | 6s |
| 全能视频 V3.1 Fast | RunningHub | ✅ | ✅ | ✅ | ❌ | 5s |
| 全能视频 V3.1 Pro | RunningHub | ✅ | ✅ | ✅ | ❌ | 5s |
| HappyHorse | 阿里云百炼 | ✅ | ✅ | ❌ | ❌ | 5s |

---

### 4.2 可灵 V3-4K（旗舰级文生视频）

**端点：** `POST /openapi/v2/kling-v3-4k/text-to-image` ⚠️ **代码中未接入此端点**

> **⚠️ 代码问题：** 代码中端点为 `/kling-video-o3-4k/image-to-video`，与官方 `/kling-v3-4k/text-to-video` 完全不符。

**特点：** 4K 影院级画质，3-15秒时长，多分镜

#### 4.2.1 文生视频

**端点：** `POST /openapi/v2/kling-v3-4k/text-to-video`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 视频画面/动作/镜头/氛围描述，1-2500字符 | — |
| `duration` | String | ✅ | — | 视频时长（秒） | `3` / `4` / `5` / `6` / `7` / `8` / `9` / `10` / `11` / `12` / `13` / `14` / `15` |
| `negativePrompt` | String | ❌ | — | 需要排除的元素，0-2500字符 | — |
| `aspectRatio` | String | ❌ | `16:9` | 视频宽高比 | `16:9` / `9:16` / `1:1` |
| `cfgScale` | Number | ❌ | `0.5` | 提示词引导强度（0-1，步进0.1） | `0~1` |
| `sound` | Boolean | ❌ | `false` | 是否生成同步音频 | `true` / `false` |
| `shotType` | String | ❌ | `customize` | 分镜方式 | `customize` |
| `multiPrompt` | Array\<String\> | ❌ | `[]` | 多分镜 prompt 列表（最多6个） | — |
| `elementList` | Array\<String\> | ❌ | `[]` | 元素引用列表（最多4个） | — |

---

### 4.3 可灵 O1

#### 4.3.1 文生视频

**端点：** `POST /openapi/v2/kling-video-o1/text-to-video`

**请求参数：**（需补充，官方文档显示支持 `negativePrompt`）

#### 4.3.2 图生视频

**端点：** `POST /openapi/v2/kling-video-o1/image-to-video`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `firstImageUrl` | String | ✅ | — | 首帧图片 URL | JPG / PNG |
| `aspectRatio` | String | ✅ | — | 画面比例 | `1:1` / `9:16` / `16:9` |
| `duration` | String | ✅ | — | 视频时长（秒） | `5` / `10` |
| `mode` | String | ❌ | — | 模式 | `std` / `pro` |
| `prompt` | String | ❌ | — | 动作描述提示词，5-2000字符 | — |

> ⚠️ **代码问题：** 代码中使用 `imageUrl` 而非 `firstImageUrl`，且缺失 `mode` 参数。

---

### 4.4 可灵 O3-Std

#### 4.4.1 文生视频

**端点：** `POST /openapi/v2/kling-video-o3-std/text-to-video`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 文本提示词，最多5000字符 | — |
| `sound` | Boolean | ✅ | — | 是否生成同步音效 | `true` / `false` |
| `duration` | Integer | ✅ | — | 视频时长（秒） | `3` ~ `15` |
| `aspectRatio` | String | ❌ | `16:9` | 画面比例 | `1:1` / `16:9` / `9:16` |
| `multiPrompt` | Array\<String\> | ❌ | — | 多镜头提示词列表（最多6个） | — |
| `multiShot` | Boolean | ❌ | — | 是否生成多镜头视频 | `true` / `false` |
| `shotType` | String | ❌ | `customize` | 分镜方式 | `customize` / `intelligence` |

#### 4.4.2 图生视频

**端点：** `POST /openapi/v2/kling-video-o3-std/image-to-video`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `firstImageUrl` | String | ✅ | — | 首帧图片 URL | JPG / PNG |
| `duration` | Integer | ✅ | — | 视频时长（秒） | `3` ~ `15` |
| `sound` | Boolean | ✅ | — | 是否生成同步音效 | `true` / `false` |
| `prompt` | String | ❌ | — | 动作描述提示词 | — |
| `lastImageUrl` | String | ❌ | — | 尾帧参考图片 URL | JPG / PNG |
| `multiPrompt` | Array\<String\> | ❌ | — | 多镜头提示词（最多6个） | — |
| `multiShot` | Boolean | ❌ | — | 是否生成多镜头视频 | `true` / `false` |
| `shotType` | String | ❌ | `customize` | 分镜方式 | `customize` / `intelligence` |

---

### 4.5 可灵 O3-Pro

#### 4.5.1 文生视频

**端点：** `POST /openapi/v2/kling-video-o3-pro/text-to-video`

**请求参数：**（需补充，与 O3-Std 类似）

#### 4.5.2 图生视频

**端点：** `POST /openapi/v2/kling-video-o3-pro/image-to-video`

**请求参数：**（需补充）

---

### 4.6 万相 2.7（阿里通义）

**特点：** 强指令遵循能力，适合广告/短视频，支持 Prompt 智能扩展

#### 4.6.1 文生视频

**端点：** `POST /openapi/v2/alibaba/wan-2.7/text-to-video`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 提示词，1-5000字符 | — |
| `duration` | String | ✅ | — | 视频时长（秒） | `2` / `3` / ... / `15` |
| `resolution` | String | ✅ | — | 输出分辨率 | `720P` / `1080P` |
| `aspectRatio` | String | ✅ | — | 宽高比 | `16:9` / `9:16` / `1:1` / `4:3` / `3:4` |
| `negativePrompt` | String | ❌ | — | 负向提示词，0-500字符 | — |
| `audioUrl` | String | ❌ | — | 背景音频 URL | MP3 / WAV |
| `promptExtend` | Boolean | ❌ | `true` | 是否启用 Prompt 智能扩展 | `true` / `false` |
| `seed` | Integer | ❌ | — | 随机种子（0-2147483647） | — |

> ⚠️ **代码问题：** 代码中 `resolution` 有默认值 `"720P"`，但官方为必填无默认值；`prompt` 在官方为必填。

#### 4.6.2 图生视频

**端点：** `POST /openapi/v2/alibaba/wan-2.7/image-to-video`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `firstImageUrl` | String | ✅ | — | 首帧图片 URL | JPG / PNG / JPEG / BMP / WEBP |
| `resolution` | String | ✅ | — | 输出分辨率 | `720P` / `1080P` |
| `duration` | String | ✅ | — | 视频时长（秒） | `2` / `3` / ... / `15` |
| `prompt` | String | ❌ | — | 提示词，1-5000字符 | — |
| `lastImageUrl` | String | ❌ | — | 尾帧图片 URL | 同上 |
| `audioUrl` | String | ❌ | — | 背景音频 URL | MP3 / WAV |
| `negativePrompt` | String | ❌ | — | 负向提示词，0-500字符 | — |
| `promptExtend` | Boolean | ❌ | `true` | 是否启用 Prompt 智能扩展 | `true` / `false` |
| `seed` | Integer | ❌ | — | 随机种子（0-2147483647） | — |

> ⚠️ **代码问题：** 代码中 `prompt` 被设为必填，但官方为可选；字段名 `firstImageUrl` 在代码中未体现。

#### 4.6.3 视频续写

**端点：** `POST /openapi/v2/alibaba/wan-2.7/video-extend`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `videoUrl` | String | ✅ | — | 源视频 URL | MP4 / MOV |
| `resolution` | String | ✅ | — | 输出分辨率 | `720P` / `1080P` |
| `duration` | String | ✅ | — | 扩展时长（秒） | `2` / `3` / ... / `15` |
| `prompt` | String | ❌ | — | 提示词，1-5000字符 | — |
| `audioUrl` | String | ❌ | — | 音频 URL（控制节奏） | MP3 / WAV |
| `negativePrompt` | String | ❌ | — | 负向提示词，0-500字符 | — |
| `promptExtend` | Boolean | ❌ | `true` | 是否启用 Prompt 智能扩展 | `true` / `false` |
| `seed` | Integer | ❌ | — | 随机种子（0-2147483647） | — |

---

### 4.7 万相 2.6（阿里通义）

#### 4.7.1 文生视频

**端点：** `POST /openapi/v2/alibaba/wan-2.6/text-to-video`

**请求参数：**（需补充）

#### 4.7.2 图生视频

**端点：** `POST /openapi/v2/alibaba/wan-2.6/image-to-video`

**请求参数：**（需补充）

#### 4.7.3 参考生视频

**端点：** `POST /openapi/v2/alibaba/wan-2.6/reference-to-video`

#### 4.7.4 参考生视频 Flash

**端点：** `POST /openapi/v2/alibaba/wan-2.6/reference-to-video-flash`

---

### 4.8 全能视频 S Pro（RunningHub 自研）

**端点：** `POST /openapi/v2/rhart-video-s-official/text-to-video-pro`

**特点：** 时空物理模拟器，精准模拟动量/惯性/碰撞，完美口型对齐，4K 输出

#### 4.8.1 文生视频

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 提示词 | — |
| `size` | String | ✅ | — | 输出尺寸（**格式为 WxH**） | `720x1280` / `1280x720` / `1024x1792` / `1792x1024` / `1080x1920` / `1920x1080` |
| `duration` | String | ✅ | — | 视频时长（秒） | `4` / `8` / `12` / `16` / `20` |

> ⚠️ **代码问题：** 代码中使用 `resolution`（如 `"720p"`）而非官方的 `size`（如 `"720x1280"`）。

---

### 4.9 全能视频 X（RunningHub 自研）

**特点：** 纯文生视频支持多种画幅，图生视频支持多图参考

#### 4.9.1 文生视频

**端点：** `POST /openapi/v2/rhart-video-g-official/text-to-video`

**请求参数：**

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 可选值 |
|--------|------|------|--------|------|--------|
| `prompt` | String | ✅ | — | 提示词 | — |
| `aspectRatio` | String | ❌ | — | 宽高比 | `16:9` / `9:16` / `1:1` 等 |
| `resolution` | String | ❌ | — | 输出分辨率 | `480p` / `720p` |
| `duration` | Int | ❌ | — | 视频时长（秒） | `6` / `10` |

#### 4.9.2 图生视频

**端点：** `POST /openapi/v2/rhart-video-g-official/image-to-video`

**请求参数：**（需补充）

#### 4.9.3 多图参考生视频

**端点：** `POST /openapi/v2/rhart-video-g-official/image-to-video-v2`（待确认）

**特点：** 支持 1-7 张参考图共同引导生成

---

### 4.10 全能视频 V3.1

#### 4.10.1 V3.1 Fast — 文生视频

**端点：** `POST /openapi/v2/rhart-video-v3.1-fast/text-to-image`

**请求参数：**（需补充）

#### 4.10.2 V3.1 Fast — 图生视频

**端点：** `POST /openapi/v2/rhart-video-v3.1-fast/image-to-image`

**请求参数：**（需补充，文档显示支持最多3张图片输入）

#### 4.10.3 V3.1 Fast — 首尾帧视频

**端点：** `POST /openapi/v2/rhart-video-v3.1-fast/start-end-to-image`（待确认）

#### 4.10.4 V3.1 Pro — 文生视频

**端点：** `POST /openapi/v2/rhart-video-v3.1-pro/text-to-image`

**特点：** 4K 分辨率，原生音频同步，4/6/8秒时长

#### 4.10.5 V3.1 Pro — 图生视频

**端点：** `POST /openapi/v2/rhart-video-v3.1-pro/image-to-image`

#### 4.10.6 V3.1 Pro — 参考生视频

**端点：** `POST /openapi/v2/rhart-video-v3.1-pro/reference-to-video`

#### 4.10.7 V3.1 Pro — 视频扩展

**端点：** `POST /openapi/v2/rhart-video-v3.1-pro/video-extend`

**特点：** 视频扩展最多 7 秒，支持连续扩展至 148 秒

---

### 4.11 Vidu Q3

#### 4.11.1 文生视频

**端点：** `POST /openapi/v2/vidu/text-to-video-q3-turbo`（Turbo 加速版）

**请求参数：**（需补充）

#### 4.11.2 图生视频

**端点：** `POST /openapi/v2/vidu/image-to-video-q3-turbo`

**请求参数：**（需补充）

#### 4.11.3 首尾帧视频

**端点：** `POST /openapi/v2/vidu/start-end-to-video-q3-turbo`

---

### 4.12 HappyHorse（阿里云百炼）

#### 4.12.1 文生视频

**端点：** `POST /openapi/v2/alibaba/happyhorse-1.0/text-to-video`

**请求参数：**（需补充，文档摘要显示支持多时长）

#### 4.12.2 图生视频

**端点：** `POST /openapi/v2/alibaba/happyhorse-1.0/image-to-video`

---

## 五、未接入但官方已有的模型

以下模型在官方 API 文档中存在，但**当前代码中未接入**：

| 模型 | 端点 | 说明 |
|------|------|------|
| 可灵 V3-4K 文生视频 | `/kling-v3-4k/text-to-video` | 旗舰级 4K 视频生成 |
| 可灵 O3-Pro 文生/图生视频 | `/kling-video-o3-pro/*` | 需确认参数 |
| 可灵参考生视频 O1 | `/kling-video-o1/reference-to-video` | 多图参考 |
| 万相 2.6 文生视频 | `/alibaba/wan-2.6/text-to-video` | 需确认参数 |
| 万相 2.6 参考生视频 | `/alibaba/wan-2.6/reference-to-video` | 多图参考 |
| 万相 2.7 视频续写 | `/alibaba/wan-2.7/video-extend` | 视频扩展 |
| 全能视频 V3.1 Pro 视频扩展 | `/rhart-video-v3.1-pro/video-extend` | 视频扩展至148秒 |
| Vidu Q3 所有变体 | `/vidu/*` | q2/q3 多版本 |
| 海螺系列 | `/huanyu/*` | 字节海螺视频模型 |
| seedance v1.5 | `/seedance-v1.5-*/*` | 多档位视频生成 |
| SkyReels V4 | `/skyreels-v4/*` | 最新视频模型 |
| LTX Video | `/ltx-2.3/*` | 长视频模型 |

---

## 六、代码重构建议

### 6.1 高优先级修复

1. **修正全能图片 G-2.0 DTO**：`imageUrl` → `imageUrls`（数组），添加 `quality` 必填字段
2. **修正全能图片 X DTO**：移除不存在的 `resolution`、`batch_count`、`negative_prompt`、`seed`，添加 `outputFormat` 必填字段
3. **修正 Seedream v4/v5 DTO**：`imageUrl` → `imageUrls`，添加 `width`、`height`、`sequentialImageGeneration`、`maxImages` 字段
4. **修正万相 2.7 DTO**：`firstImageUrl` 命名，`resolution` 改为必填，`prompt` 改为可选
5. **修正可灵 O1 DTO**：`imageUrl` → `firstImageUrl`，添加 `mode` 字段
6. **修正全能视频 S Pro DTO**：`resolution` → `size`（格式改为 WxH）
7. **删除 dead code**：旧 S/G 档端点定义（`VIDEO_S_TEXT` 等）在 `QuickCreateApi.kt` 中定义但从未被路由调用

### 6.2 新增接入

1. 接入可灵 V3-4K 文生视频
2. 补全万相 2.7 视频续写
3. 补全全能视频 V3.1 Pro 视频扩展
4. 补全海螺系列视频模型
5. 补全 Vidu Q3 全系列

---

## 七、官方文档目录

完整 API 模型清单（来源：官方侧边栏）：

### 图片生成与处理

- **全能图片 G** — `/rhart-image-g-2-official/*`
- **全能图片 X** — `/rhart-image-x-official/*`
- **全能图片 Pro** — `/rhart-image-n-pro-official/*`
- **全能图片 V2** — `/rhart-image-v2-official/*`
- **Seedream** — `/seedream-v5-lite/*`, `/seedream-v4/*`, `/seedream-v4.5/*`

### 视频生成与处理

- **Vidu** — q2-pro/turbo, q3-pro/turbo 等多版本
- **可灵** — O1 / O3 / V3-4K / 2.5 / 2.6 各版本
- **万相** — 2.2 / 2.5 / 2.6 / 2.7 各版本
- **seedance** — v1.5-pro / v1-lite-reference
- **海螺** — 02 / 2.3 各版本
- **全能视频 S** — `/rhart-video-s-official/*`
- **全能视频 G/X** — `/rhart-video-g-official/*`
- **全能视频 V3.1** — Fast / Pro 多个版本
- **悠船** — `/youchuan/*`
- **SkyReels** — V3 / V4
- **LTX Video** — `/ltx-2.3/*`
