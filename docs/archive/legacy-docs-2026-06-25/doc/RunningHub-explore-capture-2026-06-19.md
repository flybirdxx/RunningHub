# RunningHub Explore 页面抓包文档

> 抓包日期：2026-06-19（Asia/Shanghai）
> 页面：`https://www.runninghub.cn/explore`
> 抓包方式：Chrome 插件 + Chrome DevTools Protocol。当前 Chrome 已有登录态，未重新输入账号密码。
> 安全边界：只抓取页面加载、筛选、排序、详情和评论读取接口；未执行点赞、收藏、评论、关注、一键同款等写操作。本文档不记录 Cookie、Authorization、账号密码或完整媒体签名 URL。

## 结论摘要

- `explore` 的“灵感”流核心接口是 `/api/portal/creation/list`。
- “灵感”分类来自 `/api/portal/tag/tree`，请求体固定为 `{"rang":"CREATION"}`。
- “推荐/最热/最新”排序通过列表请求体的 `sort` 字段切换：`RECOMMEND` / `HOT` / `LATEST`。
- 作品详情页路径为 `/works-details-page/{creationId}`，详情数据来自 `/api/creation/detail`。
- 详情页评论读取为 `/api/comment/list`，关注状态读取为 `/uc/follow/isFollow`。
- `explore` 的“短片”tab 不复用灵感流接口，而是走 `/canvas/community/category/list` 与 `/canvas/community/composition/list`。
- 首屏灵感列表本次返回 30 条，总数约 64615；短片列表返回 30 条。

## 接口总览

| 用途 | 方法 | Path | 请求体样例 | 说明 |
|---|---|---|---|---|
| 探索分类树 | POST | `/api/portal/tag/tree` | `{"rang":"CREATION"}` | 获取 explore 顶部分类标签。 |
| 顶部运营 Banner | POST | `/api/banner/list` | `{"displayAreas":["WEBSITE_OPERATION_TOP"]}` | 获取 explore 顶部运营位。 |
| 灵感作品列表 | POST | `/api/portal/creation/list` | `{"current":1,"size":30,"fromId":null,"sort":"RECOMMEND","tags":[]}` | 获取灵感瀑布流，支持排序和标签筛选。 |
| 作品详情 | POST | `/api/creation/detail` | `{"creationId":"2067532558576996354","queryType":"current","sort":"","search":"","tags":[]}` | 获取单个作品详情，详情主体在 data.currentResponse。 |
| 评论列表 | POST | `/api/comment/list` | `{"contentId":"2067532558576996354","current":1,"size":3,"commentType":"CREATION"}` | 获取作品评论列表。 |
| 关注状态 | POST | `/uc/follow/isFollow` | `{"followId":"1970504488576520194"}` | 查询当前用户是否关注作品作者。 |
| 短片分类 | POST | `/canvas/community/category/list` | `{}` | 短片 tab 分类。 |
| 短片列表 | POST | `/canvas/community/composition/list` | `{"page":1,"size":30}` | 短片/RHTV 类内容流。 |

## 灵感列表

### 请求

```json
{
  "current": 1,
  "size": 30,
  "fromId": null,
  "sort": "RECOMMEND",
  "tags": []
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `current` | number | 页码。首屏为 1。 |
| `size` | number | 每页数量。页面首屏为 30。 |
| `fromId` | string/null | Cursor 分页字段。本次抓到首屏和采样响应的 `nextCursor` 均为空；后续实现需兼容非空。 |
| `sort` | string | 排序：`RECOMMEND`、`HOT`、`LATEST`。 |
| `tags` | array | 分类 tag id 数组。无筛选为空数组。 |

### 排序与筛选采样

| 样本 | 请求体 | 返回条数 | total | nextCursor |
|---|---|---:|---:|---|
| sort_RECOMMEND | `{"current":1,"size":30,"fromId":null,"sort":"RECOMMEND","tags":[]}` | 30 | 64615 | - |
| sort_HOT | `{"current":1,"size":30,"fromId":null,"sort":"HOT","tags":[]}` | 30 | 64612 | - |
| sort_LATEST | `{"current":1,"size":30,"fromId":null,"sort":"LATEST","tags":[]}` | 30 | 64612 | - |
| tag_数字人 | `{"current":1,"size":30,"fromId":null,"sort":"RECOMMEND","tags":["1671123934319735000"]}` | 0 | 0 | - |

### 响应结构

| 字段 | 类型 | 说明/样例 |
|---|---|---|
| `id` | string | 2067532558576996354 |
| `intro` | string | 美女 |
| `publishTime` | string | 2026-06-18 16:59:10 |
| `owner` | object | 对象字段：id, avatar, name |
| `owner.id` | string | 1970504488576520194 |
| `owner.avatar` | string | <媒体/跳转 URL，文档已脱敏> |
| `owner.name` | string |  kitten HZ |
| `statisticsInfo` | object | 对象字段：likeCount, downloadCount, useCount, pv, collectCount |
| `statisticsInfo.likeCount` | string | 2 |
| `statisticsInfo.downloadCount` | string | 0 |
| `statisticsInfo.useCount` | string | 76 |
| `statisticsInfo.pv` | string | 0 |
| `statisticsInfo.collectCount` | string | 1 |
| `creationShowreelInfo` | object | 对象字段：outputId, fileUrl, outputName, fileSize, imageWidth, imageHeight, fileType, seq, isWatermark |
| `creationShowreelInfo.outputId` | string | 2067532451395362818 |
| `creationShowreelInfo.fileUrl` | string | <媒体/跳转 URL，文档已脱敏> |
| `creationShowreelInfo.outputName` | null | - |
| `creationShowreelInfo.fileSize` | null | - |
| `creationShowreelInfo.imageWidth` | number | 1664 |
| `creationShowreelInfo.imageHeight` | number | 2496 |
| `creationShowreelInfo.fileType` | string | png |
| `creationShowreelInfo.seq` | number | 0 |
| `creationShowreelInfo.isWatermark` | number | 1 |
| `outputName` | null | - |
| `fileSize` | null | - |
| `seq` | string | 1781776491 |
| `liked` | boolean | false |
| `collected` | boolean | false |
| `isShowreel` | boolean | false |
| `state` | number | 1 |
| `status` | null | - |
| `labels` | null | - |
| `isAllWatermark` | number | 1 |
| `isFromQuickCreationV2` | null | - |

### 关键字段说明

- `creationShowreelInfo.fileUrl` 是卡片主媒体 URL，可能是图片或视频，结合 `fileType/imageWidth/imageHeight` 渲染。
- `statisticsInfo` 包含 `likeCount/downloadCount/useCount/pv/collectCount`。
- `liked`、`collected` 是当前登录用户态字段；未登录或匿名模式需要验证默认值。
- `labels` 是作品标签数组；本次样本中部分作品为空。
- `isFromQuickCreationV2` 可用于判断是否由快捷创作 v2 生成。

## 分类与 Banner

### 分类树 `/api/portal/tag/tree`

- 请求体：`{"rang":"CREATION"}`
- 返回一级分类数量：21

| id | name | level | childTags | enable |
|---|---|---:|---:|---|
| 1671123934319735000 | 数字人 | 1 | 1 | true |
| 1875941016195785389 | 图片生成 | 1 | 3 | true |
| 1875941016195785390 | 视频生成 | 1 | 3 | true |
| 1671123934319735010 | 视频特效 | 1 | 6 | true |
| 1671123934319735011 | 二次元 | 1 | 13 | true |
| 1875941016195785252 | 风格转换 | 1 | 20 | true |
| 1875941016195785253 | 海报 | 1 | 2 | true |
| 1875941016195785391 | 音频生成 | 1 | 1 | true |
| 1875941016195785249 | 图片处理 | 1 | 15 | true |
| 1875941016195785241 | 摄影 | 1 | 8 | true |
| 1875941016195785242 | 影视游戏 | 1 | 7 | true |
| 1671123934319735012 | 3D模型 | 1 | 7 | true |
| 1875941016195785243 | 创意玩法 | 1 | 5 | true |
| 1875941016195785244 | 平面设计 | 1 | 4 | true |
| 1875941016195785245 | 电商产品 | 1 | 8 | true |
| 1875941016195785246 | 室内外设计 | 1 | 6 | true |
| 1875941016195785247 | 风格画作 | 1 | 11 | true |
| 1875941016195785248 | API | 1 | 22 | true |
| 1875941016195789657 | AI漫剧 | 1 | 4 | true |
| 1875941016195785250 | 视频处理 | 1 | 8 | true |
| 1875941016195785251 | 其他 | 1 | 3 | true |

### Banner `/api/banner/list`

- 请求体：`{"displayAreas":["WEBSITE_OPERATION_TOP"]}`
- 返回 banner 分组：1

| id | name | displayArea | items |
|---|---|---|---:|
| 6 | 网站顶部运营位 | WEBSITE_OPERATION_TOP | 1 |

## 作品详情

### 请求

```json
{
  "creationId": "2067532558576996354",
  "queryType": "current",
  "sort": "",
  "search": "",
  "tags": []
}
```

- `creationId` 为作品 ID，对应详情页 URL `/works-details-page/{creationId}`。
- `queryType` 本次为 `current`。
- `sort/search/tags` 会保留从列表进入详情时的上下文，用于详情页前后切换或相关推荐。

### 响应结构 `data.currentResponse`

| 字段 | 类型 | 说明/样例 |
|---|---|---|
| `id` | string | 2067532558576996354 |
| `intro` | string | 美女 |
| `publishTime` | string | 2026-06-18 16:59:10 |
| `owner` | object | 对象字段：id, avatar, name |
| `owner.id` | string | 1970504488576520194 |
| `owner.avatar` | string | <媒体/跳转 URL，文档已脱敏> |
| `owner.name` | string |  kitten HZ |
| `statisticsInfo` | object | 对象字段：likeCount, downloadCount, useCount, pv, collectCount |
| `statisticsInfo.likeCount` | string | 2 |
| `statisticsInfo.downloadCount` | string | 0 |
| `statisticsInfo.useCount` | string | 70 |
| `statisticsInfo.pv` | string | 0 |
| `statisticsInfo.collectCount` | string | 1 |
| `creationDetailInfos` | Array | 数组，样例长度 1 |
| `liked` | boolean | false |
| `collected` | boolean | false |
| `tags` | Array | 数组，样例长度 2 |
| `seq` | string | 1781776491 |
| `isAllWatermark` | number | 1 |
| `isFromQuickCreationV2` | boolean | false |

### `creationDetailInfos[]` 样例字段

| 字段 | 类型 | 说明/样例 |
|---|---|---|
| `outputId` | string | 2067532451395362818 |
| `fileUrl` | string | <媒体/跳转 URL，文档已脱敏> |
| `fileType` | string | png |
| `seq` | number | 0 |
| `contentType` | string | WEBAPP |
| `workflowId` | string | 2064895128690577409 |
| `workflowState` | number | 1 |
| `webappId` | string | 2064905518770118658 |
| `webappState` | number | 1 |
| `fastTemplateCode` | null | - |
| `creationType` | null | - |
| `isWatermark` | number | 1 |
| `imageSize` | string | 1664×2496 |
| `webappWorkflowId` | string | 2064895128690577409 |
| `webappWorkflowState` | string | 1 |
| `displayType` | string | COMMON |
| `fastParams` | null | - |
| `baseModelInfo` | null | - |
| `modelRelations` | null | - |

## 评论与关注状态

### 评论列表 `/api/comment/list`

- 请求体：`{"contentId":"2067532558576996354","current":1,"size":3,"commentType":"CREATION"}`
- 本次样本评论数：0，分页 total：0

请求字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| `contentId` | string | 作品 ID。 |
| `current` | number | 评论页码。 |
| `size` | number | 评论页大小。详情页首屏抓到为 3。 |
| `commentType` | string | 本次为 `CREATION`。 |

### 关注状态 `/uc/follow/isFollow`

- 请求体：`{"followId":"1970504488576520194"}`
- 返回：`{"code":0,"msg":"success","errorMessages":null,"data":false}`

## 短片 Tab

短片 tab 与灵感流是不同接口组，适合在客户端单独建 repository/use case。

### 分类 `/canvas/community/category/list`

- 请求体：`{}`
- 返回分类数量：10

| id | code | name | nameEn |
|---|---|---|---|
| - | ALL | 全部 | ALL |
| 8 | HOT | 爆款集锦 | Hot |
| 2 | NARRATIVE_SHORT | 叙事短片 | Narrative Short |
| 1 | TVC | 电视广告 | TVC |
| 3 | WEBTOON | 动画漫剧 | Webtoon |
| 4 | CREATIVE_SHORT | 创意短片 | Creative Short |
| 9 | MV | 音乐MV | MV |
| 12 | DAILY_VLOG | Vlog日常 | Daily Vlog |
| 11 | ON_CAMERA | 口播探店 | On Camera |
| 10 | SCIENCE_RECORD | 科普记录 | Science Record |

### 列表 `/canvas/community/composition/list`

- 请求体：`{"page":1,"size":30}`
- 返回条数：30，total：274

| 字段 | 类型 | 说明/样例 |
|---|---|---|
| `id` | string | 2063090624344010753 |
| `name` | string | 不扫兴的父母：不讲大道理，却让人红了眼 |
| `description` | null | - |
| `compositionUrl` | string | <媒体/跳转 URL，文档已脱敏> |
| `compositionDuration` | number | 42 |
| `thumbnail` | string | <媒体/跳转 URL，文档已脱敏> |
| `categoryCode` | string | NARRATIVE_SHORT |
| `categoryName` | string | 叙事短片 |
| `likeCount` | number | 0 |
| `authorId` | number | 1969700973281185800 |
| `authorName` | string | selene |
| `authorAvatar` | string | <媒体/跳转 URL，文档已脱敏> |

## 建模建议

### Explore 灵感流

建议分为三层模型：

- `ExploreTag`：`id/name/level/childTags/enable`，用于顶部分类筛选。
- `ExploreCreationCard`：列表卡片，核心字段为 `id/intro/publishTime/owner/statisticsInfo/creationShowreelInfo/liked/collected/labels`。
- `ExploreCreationDetail`：详情页，核心字段为 `id/intro/owner/statisticsInfo/creationDetailInfos/liked/collected/tags`。

### 分页与刷新

- 首屏请求使用 `current=1,size=30,fromId=null`。
- 下拉刷新重置 `current/fromId`，切换 `sort/tags` 时也应重置。
- 响应包含 `hasNext/pages/nextCursor`，虽然本次 `nextCursor` 为空，客户端仍应优先支持 cursor + page 双模式。

### 用户态字段

- `liked/collected` 和 `/uc/follow/isFollow` 都依赖登录态。未登录时应降级为 false，并在用户点击写操作时引导登录。
- 本次未抓写接口，点赞、收藏、关注、评论发布、一键同款需要单独抓包确认，不能从读取接口推断。

### 媒体渲染

- 图片/视频媒体 URL 不应长期缓存为业务真相，建议存原始响应并设置短缓存。
- `fileType`、`imageWidth`、`imageHeight` 可用于瀑布流宽高比预排布。
- 短片接口的 `compositionUrl/compositionDuration/thumbnail` 对应视频播放卡片。
