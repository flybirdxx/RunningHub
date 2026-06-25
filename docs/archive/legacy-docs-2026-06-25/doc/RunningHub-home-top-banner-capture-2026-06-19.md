# RunningHub 首页顶部 Banner 位抓包文档

> 抓包日期：2026-06-19（Asia/Shanghai）  
> 页面：`https://www.runninghub.cn/`  
> 目标：首页顶部运营 Banner 位  
> 抓包方式：已打开 Chrome + Chrome DevTools Protocol。当前 Chrome 已有登录态，未重新输入账号密码。  
> 安全边界：只读取 Banner 配置接口和页面 DOM；未触发购买、跳转确认、支付、模型调用等写操作。本文档不记录 Cookie、Authorization、账号密码或 token。

## 结论摘要

- 首页顶部 Banner 位接口是 `POST /api/banner/list`。
- 请求体只传 `displayAreas: ["WEBSITE_OPERATION_TOP"]`。
- 当前返回 1 个 Banner 分组：`网站顶部运营位`，分组 `displayArea` 为 `WEBSITE_OPERATION_TOP`。
- 当前只有 1 个 Banner item：图片 `https://rh-images.xiaoyaoyou.com/banner/20260618_top.png`，点击跳转参数为 `/vip-rights/2?defaultPlan=year_top`。
- 页面 DOM 中该 Banner 渲染在固定顶部栈 `.top-stack.fixed.top-0.left-0.right-0.z-[95]` 内，使用 Swiper 容器承载。
- 同页中部的 `618 全新会员限时上线` 营销横幅不是这个接口返回的 Banner，而是页面静态营销模块。

## 接口

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

响应结构：

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
      "createTime": "2025-10-13 10:11:01.0",
      "updateTime": "2025-10-13 10:11:23.0",
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

## 字段含义

| 字段 | 类型 | 当前值 | 说明 |
| --- | --- | --- | --- |
| `id` | number | `6` | Banner 分组 ID |
| `name` | string | `网站顶部运营位` | 分组名称 |
| `interval` | number | `5` | 轮播间隔，推测单位为秒；当前只有 1 个 item |
| `displayArea` | string | `WEBSITE_OPERATION_TOP` | 展示位编码 |
| `items[].id` | number | `72` | Banner item ID |
| `items[].bannerId` | number | `6` | 所属 Banner 分组 ID |
| `items[].content` | string | `https://rh-images.xiaoyaoyou.com/banner/20260618_top.png` | Banner 图片 URL |
| `items[].jumpType` | number | `2` | 跳转类型；结合 `jumpParams` 推断为站内路径跳转 |
| `items[].jumpParams` | string | `/vip-rights/2?defaultPlan=year_top` | 点击跳转目标 |
| `items[].seq` | number | `9` | 排序值 |

## 页面渲染

CDP 抽取到的 DOM 层级：

```text
.top-stack.fixed.top-0.left-0.right-0.z-[95]
  .ad-banner
    .ad-inner
      .ad-swiper-container.swiper-initialized.swiper-vertical.swiper-backface-hidden
        .swiper-wrapper
          .swiper-slide.swiper-slide-active
            .ad-content
              img.ad-image
```

当前视口与尺寸：

| 项 | 值 |
| --- | --- |
| 视口 | `958 x 888` |
| 设备像素比 | `1` |
| 顶部栈 `.top-stack` | `x=0, y=0, width=958, height=132` |
| Banner 外层 `.ad-banner` | `x=0, y=0, width=958, height=60` |
| Banner 内容 `.ad-content` | `x=80, y=0, width=792, height=60` |
| 图片 `img.ad-image` 渲染尺寸 | `x=80, y=18, width=792, height=24` |
| 图片自然尺寸 | `2322 x 70` |

渲染特征：

- Banner 区域固定在页面顶部，位于导航栏上方。
- 外层高度为 `60px`，图片在其中垂直居中，当前渲染高度约 `24px`。
- 当前 DOM 没有 `<a>` 包裹图片，点击行为应由组件事件处理并读取接口返回的 `jumpType/jumpParams`。
- Swiper 当前为 `vertical`，即使只有 1 条 item，也保留了轮播容器。

## 与页面其他横幅的区别

首页中部还存在一个营销横幅：

- DOM 类名：`.new-home-marketing-placard` / `.marketing-placard`
- 文案：`618 全新会员限时上线`、`购会员，低至2.7折...`
- 背景图：`/_nuxt/summary-bg.3FutMcKl.jpg`
- 位置：在 `进入RHSTORY` 模块之后、`无限画布 热门模板` 之前

该模块不是 `POST /api/banner/list` 的返回结果，不能作为 `WEBSITE_OPERATION_TOP` 的同类数据处理。

## 客户端建模建议

建议定义独立的顶部运营 Banner 模型：

```kotlin
data class BannerGroup(
    val id: Long,
    val name: String,
    val intervalSeconds: Int,
    val displayArea: String,
    val items: List<BannerItem>
)

data class BannerItem(
    val id: Long,
    val bannerId: Long,
    val imageUrl: String,
    val jumpType: Int,
    val jumpParams: String,
    val seq: Int
)
```

跳转解析建议：

```kotlin
sealed interface BannerJump {
    data class InternalPath(val path: String) : BannerJump
    data class ExternalUrl(val url: String) : BannerJump
    data object None : BannerJump
}
```

当前规则可先按保守方式实现：

- `jumpParams` 以 `/` 开头：站内路径。
- `jumpParams` 以 `http://` 或 `https://` 开头：外部或完整 URL。
- 空值或无法识别：不跳转。
- `jumpType` 的完整枚举需要后续抓更多 Banner 位或后端约定确认，当前只能确认 `2 + /vip-rights/...` 表示可跳转站内路径。

## 替换现有 Banner 的注意点

- 当前素材自然比例约 `2322:70`，属于超宽窄条，不适合直接当普通大图 Banner 使用。
- Web 端外层高度是 `60px`，图片实际显示约 `24px` 高；移动端如果替换现有首页大 Banner，需要重新定义高度和裁剪策略。
- 当前只有一条 item，但接口结构支持多条轮播；客户端应按 `seq` 排序并支持 `interval`。
- 图片 URL 不带签名参数，可直接作为公共静态资源加载；仍需实现加载失败占位。
- 页面顶部 Banner 会影响导航和内容的 top offset；移动端实现时要明确是否属于固定顶部栏的一部分。
- 不要把 `.new-home-marketing-placard` 的 `618` 横幅和 `WEBSITE_OPERATION_TOP` 混成同一数据源。

## 验证记录

- 已通过 Chrome/CDP 确认当前页面为 `https://www.runninghub.cn/`。
- 已用页面内 `fetch` 读取 `POST /api/banner/list`，避免导出请求头。
- 已通过 DOM 抽取确认 `/banner/20260618_top.png` 渲染在 `.ad-banner` 中。
- 已确认图片自然尺寸为 `2322 x 70`，当前视口渲染为 `792 x 24`。
- 未执行点击跳转或任何写操作。
