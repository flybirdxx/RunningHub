# RunningHub 首页模型 Banner 模块抓包文档

> 抓包日期：2026-06-19（Asia/Shanghai）  
> 页面：`https://www.runninghub.cn/`  
> 目标模块：用户截图中的 `SEEDANCE 2.0 / Qwen Image 2.0 / Seedream V5 Lite / WAN 2.7` 模型推荐矩阵  
> 抓包方式：已打开 Chrome + Chrome DevTools Protocol。当前 Chrome 已有登录态，未重新输入账号密码。  
> 安全边界：只读取首页加载、DOM、Banner 读取接口和模型 SKU 详情接口；未触发购买、调用模型、点赞、收藏、评论、关注等写操作。本文档不记录 Cookie、Authorization、账号密码、`Rh-Comfy-Auth` 或带签名的完整 URL。

## 结论摘要

- 截图模块不是 `/api/banner/list` 返回的运营 Banner；它在首页 DOM 中以静态模块渲染，核心容器类名为 `.module-grid.grid.grid-cols-12.gap-1`。
- 首页加载时确实请求了 `POST /api/banner/list`，但请求体只有 `{"displayAreas":["WEBSITE_OPERATION_TOP"]}`，返回的是顶部运营位“网站顶部运营位”，不是截图里的模型矩阵。
- 截图模块包含 1 个左侧 CTA 和 6 个模型卡片跳转，所有跳转都指向 `/call-api/api-detail/{skuId}`。
- 每个模型的后续详情数据可用 `POST /api/sku/detail` 获取；运行接口由详情里的 `rhEndpoint` 提供，最终调用规则延续既有 call-api 文档：`POST https://www.runninghub.cn/openapi/v2{rhEndpoint}`。
- 后续替换现有 banner 时，建议将该模块按“本地/远程配置化模型推荐位”建模，而不是直接复用当前 `WEBSITE_OPERATION_TOP` banner 接口，除非后端新增专用 `displayArea`。

## 页面定位

CDP 快照中该模块位于首页后半段：

- 前置模块：`邀请好友赚RH币`
- 目标模块：`Exclusive On RunningHub Seedance 2.0 Get Offer`
- 后置模块：`精选工具-视频超分`

抓到的 DOM 结构要点：

| 项 | 值 |
| --- | --- |
| 容器类名 | `module-grid grid grid-cols-12 gap-1` |
| 布局 | 12 列 grid |
| 左侧主面板 | `panel-left col-span-4 ... bg-[#CCFF00]` |
| 右侧卡片 | `cell-tile bg-zinc-900 block overflow-hidden relative group` |
| 图片行为 | `object-cover`，hover 时 `scale-105` |
| 视频行为 | `autoplay=true`、`muted=true`、`loop=true`、`object-cover` |
| CDP 捕获视口下模块尺寸 | `x=80, y=3522, width=792, height=270` |

## 首页相关接口

### 1. 顶部运营 Banner

```http
POST /api/banner/list
Content-Type: application/json
```

请求体：

```json
{
  "displayAreas": ["WEBSITE_OPERATION_TOP"]
}
```

响应结构摘要：

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 6,
      "name": "网站顶部运营位",
      "interval": 5,
      "displayArea": "WEBSITE_OPERATION_TOP",
      "items": [
        {
          "id": 72,
          "bannerId": 6,
          "content": "https://rh-images.xiaoyaoyou.com/banner/20260618_top.png",
          "jumpType": 2,
          "jumpParams": "/vip-rights/2?defaultPlan=year_top",
          "seq": 9
        }
      ]
    }
  ]
}
```

判断：该接口对应首页顶部横幅，不对应截图模块。

### 2. 首页探索列表

首页同时请求：

```http
POST /api/portal/creation/list
```

请求体：

```json
{
  "current": 1,
  "size": 30,
  "fromId": null,
  "sort": "RECOMMEND",
  "tags": []
}
```

判断：该接口对应首页底部/探索作品流，不对应截图模块。

### 3. 模型详情

截图模块每个模型卡片点击后进入 `/call-api/api-detail/{skuId}`。详情可通过：

```http
POST /api/sku/detail
Content-Type: application/json
```

请求体：

```json
{
  "id": "<skuId>"
}
```

关键响应字段：

| 字段 | 含义 |
| --- | --- |
| `data.id` | 模型 SKU ID，对应详情页路径 |
| `data.name` / `data.nameEn` | 模型展示名 |
| `data.description` / `data.descriptionEn` | 模型说明 |
| `data.rhEndpoint` | OpenAPI 运行路径后缀 |
| `data.cover` / `data.coverEn` | 模型封面资源 |
| `data.inputConfigJson` | 调用参数配置 |
| `data.relationTags` | 关联标签 |
| `data.queueSize` | 队列信息 |
| `data.concurrencyLimit` | 并发限制 |
| `data.priceType` | 计费类型 |

## 截图模块卡片清单

### 模块主面板

| 字段 | 值 |
| --- | --- |
| 展示文案 | `EXCLUSIVE ON RUNNINGHUB` / `SEEDANCE 2.0` |
| CTA | `GET OFFER` |
| 跳转 | `/call-api/api-detail/2034917373414539277` |
| 关联 SKU | `2034917373414539277` |
| 模型名 | `seedance2.0/多模态视频` |
| `rhEndpoint` | `/rhart-video/sparkvideo-2.0/multimodal-video` |

### 右侧模型卡片

| 位置 | 页面视觉 | 类型 | 跳转 SKU | 详情页 | `rhEndpoint` | 媒体资源 |
| --- | --- | --- | --- | --- | --- | --- |
| 右上 1 | Qwen Image 2.0 | 图片 | `2031354034474311686` | `/call-api/api-detail/2031354034474311686` | `/alibaba/qwen-image-2.0/image-edit` | `https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-12/f889a4e2d48abe14e07346396c888af1.png` |
| 右上 2 | Seedream V5 Lite | 图片 | `2026215209183760386` | `/call-api/api-detail/2026215209183760386` | `/seedream-v5-lite/image-to-image` | `https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-12/51c0e118e907ef0b0cafea99667eba6d.png` |
| 右上 3 | WAN 2.7 | 图片 | `2039648613636050946` | `/call-api/api-detail/2039648613636050946` | `/alibaba/wan-2.7/image-edit` | `https://rh-images.xiaoyaoyou.com/fae338274c9053123688d63ac419cd59/2026-04-27/aaeffcd7ce935afdcd816327b4e86a04.png` |
| 右下 1 | Seedance 2.0 | 视频 | `2034917373414539277` | `/call-api/api-detail/2034917373414539277` | `/rhart-video/sparkvideo-2.0/multimodal-video` | `https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-15/7f4f309e26148d357fa9d461e25ddcc5.mp4` |
| 右下 2 | 可灵图生视频 o3-pro | 视频 | `2019623243725737985` | `/call-api/api-detail/2019623243725737985` | `/kling-video-o3-pro/image-to-video` | `https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-15/593abc725f555ef4d79fae1981bb83bc.mp4` |
| 右下 3 | WAN 2.7 参考生视频 | 视频 | `2039648613636050945` | `/call-api/api-detail/2039648613636050945` | `/alibaba/wan-2.7/reference-to-video` | `https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-15/29449b321bfd27201b18dd189dc30d93.mp4` |

## 详情接口样例

以 `2031354034474311686` 为例：

```json
{
  "id": "2031354034474311686",
  "name": "千问2.0-图像编辑",
  "description": "阿里巴巴通义千问团队推出的智能图像编辑模型，用户上传图片后可通过文字指令对图像进行修改。",
  "rhEndpoint": "/alibaba/qwen-image-2.0/image-edit",
  "cover": {
    "id": "2032064133983473666",
    "objName": "134b1e30-f8c3-4133-8c00-8d7df25d8dae.png",
    "imageWidth": "1184",
    "imageHeight": "864"
  }
}
```

注意：`inputConfigJson` 中可能包含示例输入 URL，其中部分带 `Rh-Comfy-Auth`，落库和文档都必须脱敏，只保留字段类型、是否必填、默认值摘要。

## 建模建议

建议在客户端侧先抽象为独立的首页推荐模块，不要直接塞进普通 Banner：

```kotlin
data class HomeModelBannerModule(
    val id: String,
    val eyebrow: String,
    val title: String,
    val ctaText: String,
    val ctaTarget: BannerTarget,
    val tiles: List<HomeModelBannerTile>
)

data class HomeModelBannerTile(
    val id: String,
    val skuId: String,
    val title: String?,
    val mediaType: MediaType,
    val mediaUrl: String,
    val target: BannerTarget,
    val rhEndpoint: String?
)

enum class MediaType {
    IMAGE,
    VIDEO
}

sealed interface BannerTarget {
    data class CallApiDetail(val skuId: String) : BannerTarget
    data class InternalPath(val path: String) : BannerTarget
    data class ExternalUrl(val url: String) : BannerTarget
}
```

后续如果后端愿意配置化，建议新增类似：

```json
{
  "displayArea": "WEBSITE_HOME_MODEL_MATRIX",
  "items": [
    {
      "skuId": "2031354034474311686",
      "title": "Qwen Image 2.0",
      "mediaType": "IMAGE",
      "mediaUrl": "...",
      "jumpType": "CALL_API_DETAIL",
      "jumpParams": "2031354034474311686",
      "seq": 1
    }
  ]
}
```

## 替换现有 Banner 时的注意点

- 当前 `WEBSITE_OPERATION_TOP` 只有单张顶部运营图，字段模型不足以表达 12 列矩阵、左侧 CTA、图片/视频混排和多 SKU 跳转。
- 如果移动端要替换现有 banner，需要单独定义布局规则；不要假设 Web 的 12 列比例能直接落到手机屏幕。
- 视频卡片在 Web 上是静音自动循环，移动端应考虑流量、首帧占位、列表回收、暂停策略和失败兜底。
- Web 当前视频节点 `poster` 抓到的是站点根地址，不适合作为客户端 poster；移动端应从后端配置或视频首帧服务拿真实缩略图。
- 模型详情接口 `/api/sku/detail` 适合作为点击前的补充数据源，但不适合作为首页模块列表源，因为它需要逐个 SKU 请求。
- 媒体 URL 当前是 `rh-images.xiaoyaoyou.com` 公共资源；客户端实现仍应设置图片/视频加载失败占位。
- 后续接 OpenAPI 调用时，不应从首页模块硬编码参数字段，应继续读取 `/api/sku/detail` 的 `inputConfigJson`。

## 验证记录

- 已通过 Chrome/CDP 打开 `https://www.runninghub.cn/` 并读取页面快照。
- 已确认 Chrome 页面处于登录态，但本次未输入账号密码。
- 已抓取首页 XHR/fetch，请求中只有顶部运营位 `POST /api/banner/list`，未发现截图模块对应的独立 Banner 接口。
- 已通过 DOM 抽取确认截图模块 7 个跳转项和媒体资源。
- 已用 `POST /api/sku/detail` 读取 6 个唯一 SKU 的详情字段。
- 未执行任何写操作。
