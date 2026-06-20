# RunningHub call-api 模型 API 抓包文档

> 抓包日期：2026-06-18（Asia/Shanghai）
> 抓包方式：Chrome 插件 + Chrome DevTools Protocol，在 `https://www.runninghub.cn/call-api` 登录会话内抓取网络请求。
> 安全处理：未点击 Playground 的“运行”，未创建付费任务；本文档不记录 Cookie、Authorization、账号、API Key 或原始签名素材 URL。

## 结论摘要

- 标准模型入口：`/call-api/search-api/standard-model?search=`，列表返回 276 条，详情可访问 274 条。
- 详情不可访问 2 条，均为列表仍存在但账号无权限/已下架模型，见“不可访问模型”。
- LLM 入口：`/call-api/llm/models`，`GET /llm/api/models` 返回 16 个模型。
- 标准模型运行接口统一为异步任务：`POST https://www.runninghub.cn/openapi/v2{rhEndpoint}`。
- 标准模型查询结果统一为：`POST https://www.runninghub.cn/openapi/v2/query`，请求体 `{"taskId":"..."}`。
- 文件上传统一为：`POST https://www.runninghub.cn/openapi/v2/media/upload/binary`，表单字段 `file`。

## 内部发现接口

| 用途 | 方法 | Path | 请求体 | 说明 |
|---|---|---|---|---|
| 标准模型列表 | POST | `/api/sku/list` | `{"categoryType":"STANDARD_MODEL","tagIds":[],"search":"","categoryTagIds":[],"owners":[],"isCollected":false,"region":"","isWhitelist":false,"pageNum":1,"pageSize":999}` | 返回卡片列表、价格摘要和分类统计。 |
| 标准模型详情 | POST | `/api/sku/detail` | `{"id":"<model id>"}` | 返回 `rhEndpoint`、`inputConfigJson`、标签、队列/并发等。注意字段名是 `id`，不是 `skuId`。 |
| 标准模型标签 | POST | `/api/sku/tag/query` | `{"parentId":4,"levels":[1],"showType":"tree","search":""}` | 返回模型分组树。 |
| LLM 模型列表 | GET | `/llm/api/models` | 无 | 返回 LLM `modelKey`、provider、capabilities、pricing。 |

## 标准模型运行契约

```bash
curl --location --request POST "https://www.runninghub.cn/openapi/v2{rhEndpoint}" \
  --header "Content-Type: application/json" \
  --header "Authorization: Bearer ${RUNNINGHUB_API_KEY}" \
  --data-raw '{ "...fields": "...", "webhookUrl": "https://example.com/webhook" }'
```

- `rhEndpoint` 来自详情接口，例如 `/rhart-image-n-g31-flash/image-to-image`。最终 URL 要补上 `/openapi/v2` 前缀。
- 请求体字段来自 `inputConfigJson`。字段 `required=true` 必填；`type=LIST` 用枚举值；上传/多输入字段可传公共 URL、Base64 data URI 或 RH 上传接口返回的 URL。
- `webhookUrl` 是标准模型通用可选字段，任务结束时回调 `TASK_END`。
- 提交响应返回 `taskId/status/errorCode/errorMessage/results/clientId/promptTips`，提交时 `results` 通常为 `null`。
- 结果 URL 有时效性，页面文档提示生成结果链接有效期约 24 小时，应尽快转存。

## 类型映射建议

| RunningHub 字段类型 | 客户端建议控件 | Kotlin 建议类型 | 备注 |
|---|---|---|---|
| `STRING` | 单行/多行文本 | `String` | 结合 `minLength/maxLength` 做校验。 |
| `NUMBER` / `INTEGER` / `INT` / `FLOAT` | 数字输入/滑杆 | `Double` / `Int` | 结合 `min/max/step/precision`。 |
| `BOOLEAN` | Switch/Checkbox | `Boolean` | 直接写入 JSON boolean。 |
| `LIST` | 单选下拉/分段控件 | `String` | 优先使用 `options[].apiValue`，没有 `apiValue` 时使用 `options[].value`。 |
| `IMAGE` / `VIDEO` / `AUDIO` / 上传类字段 | 上传控件 + URL 输入 | `String` 或 `List<String>` | `multipleInputs=true` 或字段语义为复数时写数组。 |
| `MODEL` | 模型/LoRA 选择器 | `String` | 多见于自部署开源模型，先按字符串透传。 |
| `LIST[]` / 多输入字段 | 多选/动态列表 | `List<String>` | 关注 `maxInputCount/maxUploadCount`。 |

## 分类统计

| 类型 | 数量 |
|---|---:|
| image-to-video | 67 |
| text-to-video | 45 |
| text-to-image | 43 |
| image-to-image | 32 |
| reference-to-video | 17 |
| text-to-audio | 14 |
| image-to-3D | 11 |
| text-to-music | 10 |
| video-tools | 8 |
| video-edit | 6 |
| motion-control | 5 |
| audio-to-audio | 4 |
| video-to-audio | 3 |
| text-to-lyrics | 2 |
| video-extend | 2 |
| image-tools | 2 |
| audio-to-video | 2 |
| video-to-video | 1 |
| upload-file | 1 |
| text-to-3D | 1 |

## 标准模型完整索引

| ID | 模型 | 类型 | 分组 | 来源 | Endpoint | 价格 | 必填字段 | 可选字段 |
|---|---|---|---|---|---|---|---|---|
| 2004543847939751938 | 全能图片PRO-文生图-低价渠道版 | text-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-n-pro/text-to-image` | 0.4 CNY/次 | prompt, resolution | aspectRatio |
| 2004543527918551041 | 全能图片PRO-图生图-低价渠道版 | image-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-n-pro/edit` | 0.4 CNY/次 | imageUrls, prompt, resolution | aspectRatio |
| 2027196343409463297 | 全能图片V2-图生图-低价渠道版 | image-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-n-g31-flash/image-to-image` | 0.16 CNY/次 | imageUrls, prompt, resolution | aspectRatio |
| 2027192837726294017 | 全能图片V2-文生图-低价渠道版 | text-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-n-g31-flash/text-to-image` | 0.16 CNY/次 | prompt, resolution | aspectRatio |
| 2046514150500524045 | suno-single-v5.5 | text-to-music | Suno | rh-ai | `/openapi/v2/rhart-audio/suno-v5.5/single` | 0.72 CNY/次 | description | title, make_instrumental |
| 2046514150500524046 | suno-custom-v5.5 | text-to-music | Suno | rh-ai | `/openapi/v2/rhart-audio/suno-v5.5/custom` | 0.72 CNY/次 | title, prompt, tags | - |
| 2046514150500524047 | suno-歌词生成 | text-to-lyrics | Suno | rh-ai | `/openapi/v2/rhart-audio/suno/lyrics` | 0.014 CNY/次 | prompt | - |
| 2046514150500524048 | suno-single-v5 | text-to-music | Suno | rh-ai | `/openapi/v2/rhart-audio/suno-v5/single` | 0.72 CNY/次 | description | title, make_instrumental |
| 2046514150500524049 | suno-custom-v5 | text-to-music | Suno | rh-ai | `/openapi/v2/rhart-audio/suno-v5/custom` | 0.72 CNY/次 | title, prompt, tags | - |
| 2046514150500524050 | suno-single-v4.5 | text-to-music | Suno | rh-ai | `/openapi/v2/rhart-audio/suno-v4.5/single` | 0.72 CNY/次 | description | title, make_instrumental |
| 2046514150500524051 | suno-custom-v4.5 | text-to-music | Suno | rh-ai | `/openapi/v2/rhart-audio/suno-v4.5/custom` | 0.72 CNY/次 | title, prompt, tags | - |
| 2132764885651525660 | 火山字幕擦除-视频字幕擦除（精细化版） | video-tools | Seedance | rh-ai | `/openapi/v2/volc-subtitle-erase-pro/video` | 0.013 CNY/秒 | videoUrl | eraseType, encodeMode, clientToken, eraseRatioLocation |
| 2065345589362483202 | 周星驰 fast | image-to-video | Seedance | rh-ai | `/openapi/v2/rhart-video/sparkvideo-2.0/multimodal-video-star-fast` | - | resolution, duration, templateId | ratio, imageUrl |
| 2065339476332175362 | 周星驰 | image-to-video | Seedance | rh-ai | `/openapi/v2/rhart-video/sparkvideo-2.0/multimodal-video-star` | - | resolution, duration, templateId | ratio, imageUrl |
| 2064615278630940674 | MiniMax Music 翻唱前处理 | text-to-audio | 最近上新 | minimax | `/openapi/v2/minimax/music-cover-preprocess` | 0.8 CNY/次 | audioUrl | - |
| 2061660305555243009 | 万相 2.2 角色动作迁移 | video-to-video | 自部署开源模型 | rh-ai | `/openapi/v2/rhart-video/wan2.2/character-motion-transfer` | 0.24 CNY/秒 | 299##image, 275##video | - |
| 2059467934809726978 | 火山字幕擦除-视频字幕擦除（标准版） | video-tools | Seedance | rh-ai | `/openapi/v2/volc-subtitle-erase/video` | 0.005 CNY/秒 | videoUrl | clientToken |
| 2055229427329323016 | 即梦图片 4.6 图生图 | image-to-image | 最近上新 | bytedance | `/openapi/v2/bytedance/jimeng-4.6/image-to-image` | 0.17 CNY/张 | prompt, imageUrls | width, height, scale, forceSingle, minRatio, maxRatio |
| 2055229427329323015 | 即梦图片 4.6 文生图 | text-to-image | 最近上新 | bytedance | `/openapi/v2/bytedance/jimeng-4.6/text-to-image` | 0.17 CNY/张 | prompt | width, height, scale, forceSingle, minRatio, maxRatio |
| 2054377360666701825 | 分离音频-Other | video-to-audio | 自部署开源模型 | rh-ai | `/openapi/v2/rhart-audio/extract-background` | 0.1 CNY/次 | 2##file | - |
| 2054375798917607425 | 分离音频-Vocals | video-to-audio | 自部署开源模型 | rh-ai | `/openapi/v2/rhart-audio/extract-vocal` | 0.1 CNY/次 | 3##file | - |
| 2054086928526929955 | 全能图片X-高质量文生图-官方稳定版 | text-to-image | 全能图片X | rh-ai | `/openapi/v2/rhart-imagine-image-quality/text-to-image` | 0.38 CNY/张 | prompt, resolution, numImages | aspectRatio, outputFormat |
| 2054086928526929954 | 全能图片X-高质量图片编辑-官方稳定版 | image-to-image | 全能图片X | rh-ai | `/openapi/v2/rhart-imagine-image-quality/edit` | 0.44 CNY/张 | prompt, imageUrl, resolution, numImages | aspectRatio, outputFormat |
| 2054086928526929953 | 全能视频V3.1-fast-参考生视频-官方稳定版 | reference-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-fast-official/reference-to-video` | 4.03 CNY/次 | prompt, imageUrls, resolution | aspectRatio, generateAudio, negativePrompt, seed |
| 2054086928526929924 | MiniMax Music 翻唱 | text-to-audio | 最近上新 | minimax | `/openapi/v2/minimax/music-cover` | 0.8 CNY/次 | prompt, audioUrl | lyrics, coverFeatureId, sampleRate, format, bitrate |
| 2054086928526929923 | MiniMax Music 2.6 | text-to-audio | 最近上新 | minimax | `/openapi/v2/minimax/music-2.6` | 0.8 CNY/次 | - | prompt, lyrics, sampleRate, bitrate, format, lyricsOptimizer, isInstrumental |
| 2052742537170706434 | Mureka-v9 伴奏生成 | audio-to-audio | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/mureka-v9/generate-bgm` | 0.28 CNY/次 | n | prompt, instrumentalId, stream |
| 2052634166820159501 | Mureka 歌词生成 | text-to-lyrics | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/generate-lyrics` | 0.06 CNY/次 | prompt | - |
| 2052634166820159500 | Mureka 人声克隆 | audio-to-audio | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/vocal-clone` | 31.5 CNY/次 | fileUrl | - |
| 2052634166820159499 | Mureka-v8 短歌延长 | audio-to-audio | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/mureka-v8/extend-song` | 0.63 CNY/次 | lyrics, fileUrl, extendAt | extendType |
| 2052634166820159498 | mureka-v7.6 短歌延长 | audio-to-audio | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/mureka-v7.6/extend-song` | 0.245 CNY/次 | lyrics, extendAt, fileUrl | - |
| 2052634166820159497 | Mureka-v8 伴奏生成 | text-to-audio | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/mureka-v8/generate-bgm` | 0.28 CNY/次 | n | prompt, instrumentalId, stream |
| 2052634166820159495 | Mureka-v7.6 伴奏生成 | text-to-audio | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/mureka-v7.6/generate-bgm` | 0.19 CNY/次 | n | prompt, instrumentalId, stream |
| 2052634166820159494 | Mureka-v9 歌曲生成 | text-to-music | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/mureka-v9/generate-song` | 0.28 CNY/次 | lyrics, n | prompt, referenceId, vocalId, melodyId, stream |
| 2052634166820159493 | Mureka-v8 歌曲生成 | text-to-music | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/mureka-v8/generate-song` | 0.28 CNY/次 | lyrics, n | prompt, referenceId, vocalId, melodyId, stream |
| 2052634166820159492 | Mureka-o2 歌曲生成 | text-to-music | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/mureka-o2/generate-song` | 0.28 CNY/次 | lyrics, n | prompt, referenceId |
| 2052634166820159491 | Mureka-v7.6 歌曲生成 | text-to-music | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/mureka-v7.6/generate-song` | 0.19 CNY/次 | lyrics, n | prompt, referenceId, vocalId, melodyId, stream |
| 2052634166820159490 | 悠船文生图-v8.1 | text-to-image | 悠船 AI 绘图 | rh-ai | `/openapi/v2/youchuan/text-to-image-v81` | 0.54 CNY/次 | prompt, hd | chaos, quality, stylize, raw, imageUrl, iw, sref, sw, sv, aspectRatio |
| 2052288678866534404 | Mureka 文件上传 | upload-file | Mureka Al Models | rh-ai | `/openapi/v2/mureka-ai/files-upload` | 0 CNY/次 | fileUrl, purpose | - |
| 2052288678866534402 | 可灵 o3 创建主体 | video-tools | 可灵 3.0 | rh-ai | `/openapi/v2/kling-elements-advanced` | 0 CNY/次 | elementName, elementDescription, referenceType | elementImageList, elementVideoList, tagList |
| 2052238943728254977 | SkyReels V4 Omni 参考视频-fast | reference-to-video | SkyReels | rh-ai | `/openapi/v2/skyreels-v4/omni-reference-fast` | 0.1 CNY/次 | prompt, resolution | aspectRatio, duration, promptOptimizer, refImages, refVideos |
| 2051994196535197697 | SkyReels V4 图生视频-fast | image-to-video | SkyReels | rh-ai | `/openapi/v2/skyreels-v4/image-to-video-fast` | 0.28 CNY/秒 | prompt, firstImageUrl, promptOptimizer, resolution | duration |
| 2051992463616557057 | SkyReels V4 文生视频-fast | text-to-video | SkyReels | rh-ai | `/openapi/v2/skyreels-v4/text-to-video-fast` | 0.28 CNY/秒 | prompt, promptOptimizer, resolution | duration, aspectRatio |
| 2049745693465419778 | 可灵文生视频2.5-turbo-std | text-to-video | 可灵 | rh-ai | `/openapi/v2/kling-v2.5-turbo-std/text-to-video` | 1.05 CNY/次 | prompt, duration, aspectRatio | negativePrompt, guidanceScale |
| 2048623940504719364 | happyhorse-1.0/video-edit | video-edit | HappyHorse Models | rh-ai | `/openapi/v2/alibaba/happyhorse-1.0/video-edit` | 0.67 CNY/秒 | videoUrl, prompt, resolution | imageUrls, audioSetting, seed |
| 2048623940504719363 | happyhorse-1.0/reference-to-video | reference-to-video | HappyHorse Models | rh-ai | `/openapi/v2/alibaba/happyhorse-1.0/reference-to-video` | 0.67 CNY/秒 | prompt, imageUrls, resolution, duration | aspectRatio, seed |
| 2048623940504719362 | 分离音频 | video-to-audio | 自部署开源模型 | rh-ai | `/openapi/v2/rhart-audio/source-separation` | 0.1 CNY/次 | 3##file | - |
| 2047563851324841990 | 可灵参考生视频o3-4k | reference-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-4k/reference-to-video` | 2.7 CNY/秒 | prompt, duration | imageUrls, keepOriginalSound, sound, aspectRatio, shotType, multiPrompt, elementList, multiShot |
| 2047563851324841989 | 可灵图生视频o3-4k | image-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-4k/image-to-video` | 2.7 CNY/秒 | prompt, firstImageUrl, duration | lastImageUrl, sound, shotType, multiPrompt, elementList, multiShot |
| 2047563851324841988 | 可灵文生视频o3-4k | text-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-4k/text-to-video` | 2.7 CNY/秒 | prompt, duration | aspectRatio, sound, shotType, multiPrompt, elementList, multiShot |
| 2047563851324841987 | 可灵文生视频v3-4k | text-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-v3-4k/text-to-video` | 2.7 CNY/秒 | prompt, duration | negativePrompt, aspectRatio, cfgScale, sound, shotType, multiPrompt, elementList, multiShot |
| 2047563851324841986 | 可灵图生视频v3-4k | image-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-v3-4k/image-to-video` | 2.7 CNY/秒 | imageUrl, duration | prompt, negativePrompt, endImageUrl, cfgScale, sound, shotType, multiPrompt, elementList, multiShot |
| 2046514150500524053 | 全能图片X-文生图片-官方稳定版 | text-to-image | 全能图片X | rh-ai | `/openapi/v2/rhart-image-x-official/text-to-image` | 0.14 CNY/张 | prompt, outputFormat | aspectRatio |
| 2046514150500524052 | 全能图片X-图片编辑-官方稳定版 | image-to-image | 全能图片X | rh-ai | `/openapi/v2/rhart-image-x-official/edit` | 0.14 CNY/张 | prompt, image | - |
| 2046514150500524037 | happyhorse-1.0/image-to-video | image-to-video | HappyHorse Models | rh-ai | `/openapi/v2/alibaba/happyhorse-1.0/image-to-video` | 0.67 CNY/秒 | imageUrl, resolution, duration | prompt, seed |
| 2046514150500524036 | happyhorse-1.0/text-to-video | text-to-video | HappyHorse Models | rh-ai | `/openapi/v2/alibaba/happyhorse-1.0/text-to-video` | 0.67 CNY/秒 | prompt, resolution, duration | aspectRatio, seed |
| 2046514150500524035 | 全能图片G-2-图生图-官方稳定版 | image-to-image | 全能图片G | rh-ai | `/openapi/v2/rhart-image-g-2-official/image-to-image` | 0.19 CNY/次 | prompt, imageUrls, resolution, quality | aspectRatio |
| 2046514150500524034 | 全能图片G-2-文生图-官方稳定版 | text-to-image | 全能图片G | rh-ai | `/openapi/v2/rhart-image-g-2-official/text-to-image` | 0.06 CNY/次 | prompt, resolution, quality | aspectRatio |
| 2046514150500524033 | 全能图片G-2.0-文生图-低价渠道版 | text-to-image | 全能图片G | openai | `/openapi/v2/rhart-image-g-2/text-to-image` | 0.1 CNY/次 | prompt | aspectRatio, resolution |
| 2046503667076751361 | 全能图片G-2.0-图生图-低价渠道版 | image-to-image | 全能图片G | openai | `/openapi/v2/rhart-image-g-2/image-to-image` | 0.1 CNY/次 | prompt, imageUrls | aspectRatio, resolution |
| 2043991611295436803 | 全能视频X-视频续写-官方稳定版 | video-extend | 全能视频X | rh-ai | `/openapi/v2/rhart-video-g-official/video-extend` | 1.89 CNY/次 | videoUrl, prompt, duration | - |
| 2043991611295436802 | 全能视频X-多图参考生视频-官方稳定版 | reference-to-video | 全能视频X | rh-ai | `/openapi/v2/rhart-video-g-official/reference-to-video` | 1.89 CNY/次 | imageUrls, prompt, duration, resolution | - |
| 2042415417236176904 | 全能视频V3.1-Lite首尾帧生视频-官方稳定版 | text-to-video | 全能视频V | rh-ai | `/openapi/v2/rhart-video-v3.1-lite-official/start-end-to-video` | 2.52 CNY/次 | prompt, firstImageUrl, lastImageUrl, resolution | aspectRatio, negativePrompt, seed |
| 2042415417236176903 | 全能视频V3.1-Lite图生视频-官方稳定版 | image-to-video | 全能视频V | rh-ai | `/openapi/v2/rhart-video-v3.1-lite-official/image-to-video` | 0.32 CNY/秒 | prompt, imageUrl, duration, resolution | negativePrompt, aspectRatio, seed |
| 2042415417236176902 | 全能视频V3.1-Lite文生视频-官方稳定版 | text-to-video | 全能视频V | rh-ai | `/openapi/v2/rhart-video-v3.1-lite-official/text-to-video` | 0.32 CNY/秒 | prompt, resolution, duration | negativePrompt, aspectRatio, seed |
| 2042415417236176901 | 万相2.5 Preview 图生图 | image-to-image | Wan Image Models | wan | `/openapi/v2/alibaba/wan-2.5-preview/image-to-image` | 0.13 CNY/张 | prompt, imageUrls, n | negativePrompt, size, promptExtend, seed |
| 2042415417236176900 | 万相2.5 Preview 文生图 | text-to-image | Wan Image Models | wan | `/openapi/v2/alibaba/wan-2.5-preview/text-to-image` | 0.13 CNY/张 | prompt, size | negativePrompt, n, promptExtend, seed |
| 2042415417236176899 | 万相2.5 Preview 图生视频 | image-to-video | Wan Video Models | wan | `/openapi/v2/alibaba/wan-2.5-preview/image-to-video` | 0.2 CNY/秒 | imageUrl, duration, resolution | prompt, audioUrl, negativePrompt, promptExtend, seed |
| 2042415417236176898 | 万相2.5 Preview 文生视频 | text-to-video | Wan Video Models | wan | `/openapi/v2/alibaba/wan-2.5-preview/text-to-video` | 0.2 CNY/秒 | prompt, duration, size | negativePrompt, audioUrl, promptExtend, seed |
| 2041408069159936005 | PixVerse V6 视频续写 | video-tools | Pixverse Al Models | rh-ai | `/openapi/v2/pixverse-v6/extend` | 0.16 CNY/秒 | prompt, videoUrl, resolution, duration, generateAudioSwitch | negativePrompt, style, seed |
| 2041408069159936004 | PixVerse V6 转场 | video-tools | Pixverse Al Models | rh-ai | `/openapi/v2/pixverse-v6/transition` | 0.16 CNY/秒 | prompt, firstImageUrl, endImageUrl, resolution, duration, generateAudioSwitch, generateMultiClipSwitch | aspectRatio, style, negativePrompt, seed, thinkingType |
| 2041408069159936003 | PixVerse V6 图生视频 | image-to-video | Pixverse Al Models | rh-ai | `/openapi/v2/pixverse-v6/image-to-video` | 0.16 CNY/秒 | imageUrl, prompt, resolution, duration, generateAudioSwitch | - |
| 2041408069159936002 | PixVerse V6 文生视频 | text-to-video | Pixverse Al Models | rh-ai | `/openapi/v2/pixverse-v6/text-to-video` | 0.16 CNY/秒 | prompt, resolution, duration, generateAudioSwitch | aspectRatio |
| 2040031546192461825 | 万相2.7-视频编辑 | video-edit | 最近上新 | wan | `/openapi/v2/alibaba/wan-2.7/video-edit` | 0 CNY/次 | prompt, imageUrls, resolution, duration, aspectRatio | videoUrl, negativePrompt, promptExtend, seed |
| 2039648613636050949 | 万相2.7-文生图Pro | text-to-image | 最近上新 | rh-ai | `/openapi/v2/alibaba/wan-2.7/text-to-image-pro` | 0.47 CNY/次 | prompt | width, height, thinkingMode |
| 2039648613636050948 | 万相2.7-文生图 | text-to-image | 最近上新 | rh-ai | `/openapi/v2/alibaba/wan-2.7/text-to-image` | 0.19 CNY/次 | prompt | width, height, thinkingMode |
| 2039648613636050947 | 万相2.7-图像编辑Pro | image-tools | 最近上新 | rh-ai | `/openapi/v2/alibaba/wan-2.7/image-edit-pro` | 0.47 CNY/次 | imageUrls, prompt | width, height |
| 2039648613636050946 | 万相2.7-图像编辑 | image-tools | 最近上新 | rh-ai | `/openapi/v2/alibaba/wan-2.7/image-edit` | 0.19 CNY/次 | imageUrls, prompt | width, height |
| 2039648613636050945 | 万相2.7-参考生视频 | reference-to-video | 最近上新 | wan | `/openapi/v2/alibaba/wan-2.7/reference-to-video` | 0.001 CNY/次 | prompt, resolution, duration, aspectRatio | videoUrls, imageUrls, audioUrl, negativePrompt, promptExtend, seed |
| 2039630309345267713 | 万相2.7-视频续写 | video-extend | 最近上新 | wan | `/openapi/v2/alibaba/wan-2.7/video-extend` | 0.51 CNY/秒 | videoUrl, resolution, duration | prompt, audioUrl, negativePrompt, promptExtend, seed |
| 2039618329897144322 | 万相2.7-图生视频 | image-to-video | 最近上新 | wan | `/openapi/v2/alibaba/wan-2.7/image-to-video` | 0.51 CNY/秒 | firstImageUrl, resolution, duration | prompt, lastImageUrl, audioUrl, negativePrompt, promptExtend, seed |
| 2039544460993695745 | 万相2.7-文生视频 | text-to-video | 最近上新 | wan | `/openapi/v2/alibaba/wan-2.7/text-to-video` | 0.51 CNY/秒 | prompt, duration, resolution, aspectRatio | negativePrompt, audioUrl, promptExtend, seed |
| 2039255701421088770 | Vidu-参考生视频-q3-mix | reference-to-video | Vidu | vidu | `/openapi/v2/vidu/reference-to-video-q3-mix` | 0.55 CNY/秒 | prompt, imageUrls, duration, resolution | aspectRatio, audio |
| 2038503362263322628 | RH视频帧率增强 | video-tools | RH超分 | rh-ai | `/openapi/v2/rhart-video/video-fps-increaser` | 0.07 CNY/秒 | videoUrl | - |
| 2038503362263322627 | RH视频超分 | video-tools | RH超分 | rh-ai | `/openapi/v2/rhart-video/video-upscaler` | 0.14 CNY/秒 | videoUrl, targetResolution | - |
| 2034917373414539278 | seedance2.0-Fast/多模态视频 | reference-to-video | Seedance2.0 | bytedance | `/openapi/v2/rhart-video/sparkvideo-2.0-fast/multimodal-video` | 0.5 CNY/秒 | prompt, resolution, duration | imageUrls, videoUrls, audioUrls, generateAudio, ratio, realPersonMode, conversionSlots, returnLastFrame, seed |
| 2034917373414539277 | seedance2.0/多模态视频 | reference-to-video | Seedance2.0 | bytedance | `/openapi/v2/rhart-video/sparkvideo-2.0/multimodal-video` | 0.6 CNY/秒 | prompt, resolution, duration | imageUrls, videoUrls, audioUrls, generateAudio, ratio, realPersonMode, conversionSlots, returnLastFrame, seed |
| 2034917373414539276 | seedance2.0-Fast/图生视频 | image-to-video | Seedance2.0 | bytedance | `/openapi/v2/rhart-video/sparkvideo-2.0-fast/image-to-video` | 0.5 CNY/秒 | resolution, duration, firstFrameUrl | prompt, lastFrameUrl, generateAudio, ratio, realPersonMode, conversionSlots, returnLastFrame, seed |
| 2034917373414539275 | seedance2.0/图生视频 | image-to-video | Seedance2.0 | bytedance | `/openapi/v2/rhart-video/sparkvideo-2.0/image-to-video` | 0.6 CNY/秒 | resolution, duration, firstFrameUrl | prompt, lastFrameUrl, generateAudio, ratio, realPersonMode, conversionSlots, returnLastFrame, seed |
| 2034917373414539274 | seedance2.0-Fast/文生视频 | text-to-video | Seedance2.0 | bytedance | `/openapi/v2/rhart-video/sparkvideo-2.0-fast/text-to-video` | 0.5 CNY/秒 | prompt, resolution, duration | generateAudio, ratio, webSearch, returnLastFrame, seed |
| 2034917373414539273 | seedance2.0/文生视频 | text-to-video | Seedance2.0 | bytedance | `/openapi/v2/rhart-video/sparkvideo-2.0/text-to-video` | 0.6 CNY/秒 | prompt, resolution, duration | generateAudio, ratio, webSearch, returnLastFrame, seed |
| 2034917373414539265 | f-2-dev/edit-lora | image-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-dev/edit-lora` | 0.25 CNY/次 | 51##image, 16##text, 47##select, 52##file_type | 48##value, 49##value, 18##lora_name, 18##strength_model |
| 2034901418613473282 | f-2-dev/edit | image-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-dev/edit` | 0.24 CNY/次 | 20##image, 17##text, 46##select, 51##file_type | 35##value, 34##value |
| 2034899989190475778 | f-2-dev/text-to-image-lora | text-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-dev/text-to-image-lora` | 0.17 CNY/次 | 12##text, 42##select, 44##file_type | 43##value, 30##value, 16##lora_name, 16##strength_model |
| 2034898928027369473 | f-2-dev/text-to-image | text-to-image | 自部署开源模型 | rh-ai | `/openapi/v2/rhart-image/f-2-dev/text-to-image` | 0.2 CNY/次 | 12##text, 41##select, 43##file_type | 30##value, 29##value |
| 2034892005601247234 | f-2-klein-9b/text-to-image-lora | text-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-klein-9b/text-to-image-lora` | 0.06 CNY/次 | 52##select, 37##text, 55##file_type | 36##value, 35##value, 59##lora_name, 59##strength_model |
| 2034882738118787074 | f-2-klein-9b/text-to-image | text-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-klein-9b/text-to-image` | 0.05 CNY/次 | 36##text, 51##select, 54##file_type | 35##value, 34##value |
| 2034877792577191938 | f-2-klein-9b/edit | image-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-klein-9b/edit` | 0.05 CNY/次 | 53##image, 54##text, 81##select, 55##file_type | 70##value, 69##value |
| 2034876557191086082 | f-2-klein-4b/edit | image-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-klein-4b/edit` | 0.05 CNY/次 | 19##image, 17##text, 47##select, 51##file_type | - |
| 2034827564243288066 | f-2-klein-4b/edit-lora | image-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-klein-4b/edit-lora` | 0.05 CNY/次 | 41##image, 37##select, 16##text, 40##file_type | 18##lora_name, 18##strength_model |
| 2034826108534587393 | f-2-klein-4b/text-to-image | text-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-klein-4b/text-to-image` | 0.05 CNY/次 | 9##text, 94##select, 103##file_type | 25##value, 26##value |
| 2034823170495938562 | f-2-klein-4b/text-to-image-lora | text-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-2-klein-4b/text-to-image-lora` | 0.04 CNY/次 | 9##text, 33##select | 15##lora_name, 15##strength_model |
| 2034581230479212554 | 可灵对口型-视频生成 | audio-to-video | 最近上新 | rh-ai | `/openapi/v2/kling-lip-sync/lip-sync-video` | 0.35 CNY/秒 | sessionId, faceId, soundStartTime, soundEndTime, soundInsertTime | audioId, audioUrl, soundVolume, originalAudioVolume |
| 2034581230479212553 | 可灵对口型-语音合成 | text-to-audio | 最近上新 | rh-ai | `/openapi/v2/kling-lip-sync/tts` | 0.04 CNY/次 | text, voiceId, voiceLanguage | voiceSpeed |
| 2034581230479212552 | SkyReels V4 Omni 参考视频-std | reference-to-video | SkyReels | rh-ai | `/openapi/v2/skyreels-v4/omni-reference-std` | 0.1 CNY/次 | prompt, sound, resolution | aspectRatio, duration, promptOptimizer, refImages, refVideos |
| 2034581230479212547 | SkyReels V4 图生视频-std | image-to-video | SkyReels | rh-ai | `/openapi/v2/skyreels-v4/image-to-video-std` | 0.39 CNY/秒 | prompt, firstImageUrl, sound, promptOptimizer, resolution | duration |
| 2034581230479212546 | f-krea-dev-lora | text-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-krea-dev-lora` | 0.05 CNY/次 | 133##select, 45##text, 135##file_type | 115##lora_name, 115##strength_model |
| 2034579103547654146 | f-dev-lora | text-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-dev-lora` | 0.03 CNY/次 | 104##select, 105##text, 106##file_type | 107##lora_name, 107##strength_model |
| 2034577474668724226 | f-dev | text-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-image/f-dev` | 0.04 CNY/次 | 23##text, 43##select, 48##file_type | - |
| 2034555537645109250 | 万相2.2-图生视频 | image-to-video | Wan Video Models | rh-ai | `/openapi/v2/rhart-video/wan-2.2/image-to-video` | 0.07 CNY/秒 | 219##image, 183##text, 202##select, 218##select | 16##negative_prompt |
| 2034550762903961601 | wan-2.2/text-to-image-lora | text-to-image | 自部署开源模型 | rh-ai | `/openapi/v2/rhart-video/wan-2.2/text-to-image-lora` | 0.06 CNY/次 | 79##text, 225##select, 201##file_type | 216##value, 215##value, 229##lora_name, 229##strength_model |
| 2034549626436321281 | wan-2.2/image-to-image | image-to-image | 自部署开源模型 | rh-ai | `/openapi/v2/rhart-video/wan-2.2/image-to-image` | 0.08 CNY/次 | 272##image, 79##text, 267##select, 242##file_type | 270##value, 271##value |
| 2034532017653415938 | z-image-turbo/image-to-image-lora | image-to-image | Z-Image | rh-ai | `/openapi/v2/rhart-image/z-image-turbo/image-to-image-lora` | 0.03 CNY/次 | 44##image, 18##text, 41##select, 42##file_type | 43##lora_name, 43##strength_model |
| 2034531369067216897 | z-image-turbo/image-to-image | image-to-image | Z-Image | rh-ai | `/openapi/v2/rhart-image/z-image-turbo/image-to-image` | 0.05 CNY/次 | 66##image, 41##text, 64##select, 65##file_type | - |
| 2034530934667345921 | z-image/turbo-lora | text-to-image | Z-Image | rh-ai | `/openapi/v2/rhart-image/z-image/turbo-lora` | 0.03 CNY/次 | 6##text, 30##select, 34##file_type | 38##lora_name, 38##strength_model |
| 2034529136204316673 | z-image/turbo | text-to-image | Z-Image | rh-ai | `/openapi/v2/rhart-image/z-image/turbo` | 0.04 CNY/次 | 10##text, 28##select | 29##file_type |
| 2034517545568174082 | qwen-image/edit-2511-lora | image-to-image | Qwen Image | rh-ai | `/openapi/v2/rhart-image/qwen-image/edit-2511-lora` | 0.07 CNY/次 | 44##image, 38##text, 32##select | 16##lora_name, 16##strength_model, 40##file_type |
| 2034512096265502721 | qwen-image/edit-2511 | image-to-image | Qwen Image | rh-ai | `/openapi/v2/rhart-image/qwen-image/edit-2511` | 0.12 CNY/次 | 57##image, 53##text, 28##select, 52##file_type | 58##image, 59##image |
| 2034492344042258433 | f-kontext-dev-lora | image-to-image | 基础算法F | rh-ai | `/openapi/v2/rhart-video/f-kontext/dev-lora` | 0.09 CNY/次 | 15##image, 41##select, 4##text | 44##value, 45##value, 13##lora_name, 13##strength_model, 16##file_type |
| 2034466657231200258 | ltx-2.3/text-to-video-lora | text-to-video | LTX-2.3 | rh-ai | `/openapi/v2/rhart-video/ltx-2.3/text-to-video-lora` | 0.07 CNY/秒 | 188##prompt, 247##select, 248##select, 227##value | 254##lora_name, 254##strength_model, 257##lora_name, 257##strength_model, 258##lora_name, 258##strength_model |
| 2034465605475917825 | ltx-2.3/text-to-video | text-to-video | LTX-2.3 | rh-ai | `/openapi/v2/rhart-video/ltx-2.3/text-to-video` | 0.07 CNY/秒 | 188##prompt, 247##select, 248##select, 227##value | - |
| 2034462865521664001 | ltx-2.3/image-to-video-lora | image-to-video | LTX-2.3 | rh-ai | `/openapi/v2/rhart-video/ltx-2.3/image-to-video-lora` | 0.07 CNY/秒 | 98##image, 245##select, 240##select, 222##value, 269##prompt | 254##lora_name, 254##strength_model, 257##lora_name, 257##strength_model, 258##lora_name, 258##strength_model |
| 2034461796984971265 | ltx-2.3/image-to-video | image-to-video | LTX-2.3 | rh-ai | `/openapi/v2/rhart-video/ltx-2.3/image-to-video` | 0.07 CNY/秒 | 98##image, 200##prompt, 245##select, 240##select, 222##value | - |
| 2034442710603292673 | qwen-image/text-to-image-2512-lora | text-to-image | Qwen Image | rh-ai | `/openapi/v2/rhart-image/qwen-image/text-to-image-2512-lora` | 0.1 CNY/次 | 24##select, 6##text | 10##lora_name, 10##strength_model, 30##file_type |
| 2034161609456504838 | 可灵对口型-人脸识别 | audio-to-video | 最近上新 | rh-ai | `/openapi/v2/kling-lip-sync/identify-face` | 0.04 CNY/次 | - | videoUrl, videoId |
| 2034161609456504835 | SkyReels V4 文生视频-std | text-to-video | SkyReels | rh-ai | `/openapi/v2/skyreels-v4/text-to-video-std` | 0.39 CNY/秒 | prompt, sound, promptOptimizer, resolution | duration, aspectRatio |
| 2032764885651525635 | 千问2.0-文生图 | text-to-image | Qwen Image | rh-ai | `/openapi/v2/alibaba/qwen-image-2.0/text-to-image` | 0.13 CNY/张 | prompt | negativePrompt, size, imageNum, promptExtend |
| 2032764885651525634 | 千问2.0Pro-文生图 | text-to-image | Qwen Image | rh-ai | `/openapi/v2/alibaba/qwen-image-2.0-pro/text-to-image` | 0.33 CNY/张 | prompt | negativePrompt, size, imageNum, promptExtend |
| 2032764885651525633 | 即梦/动作模仿2.0 | motion-control | 最近上新 | bytedance | `/openapi/v2/bytedance/dreamactor-v2` | 0.32 CNY/秒 | imageUrl, videoUrl | - |
| 2031354034474311687 | Vidu-参考生视频-q3 | reference-to-video | Vidu | vidu | `/openapi/v2/vidu/reference-to-video-q3` | 0.22 CNY/秒 | prompt, imageUrls, duration, resolution | aspectRatio, audio |
| 2031354034474311686 | 千问2.0-图像编辑 | image-to-image | Qwen Image | rh-ai | `/openapi/v2/alibaba/qwen-image-2.0/image-edit` | 0.13 CNY/张 | imageUrls, prompt | negativePrompt, size, imageNum |
| 2031354034474311685 | 千问2.0Pro-图像编辑 | image-to-image | Qwen Image | rh-ai | `/openapi/v2/alibaba/qwen-image-2.0-pro/image-edit` | 0.33 CNY/张 | imageUrls | prompt, negativePrompt, size, imageNum |
| 2031354034474311684 | 可灵动作控制V3.0-pro | motion-control | 可灵 3.0 | rh-ai | `/openapi/v2/kling-v3.0-pro/motion-control` | 1.08 CNY/秒 | imageUrl, videoUrl, characterOrientation | prompt, negativePrompt, keepOriginalSound, elementList |
| 2031354034474311683 | 可灵动作控制V3.0-std | motion-control | 可灵 3.0 | rh-ai | `/openapi/v2/kling-v3.0-std/motion-control` | 0.81 CNY/秒 | imageUrl, videoUrl, characterOrientation | prompt, negativePrompt, keepOriginalSound, elementList |
| 2031354034474311682 | 全能图片X-文生图-低价渠道版 | text-to-image | 全能图片X | rh-ai | `/openapi/v2/rhart-image-g/text-to-image` | 0.08 CNY/次 | model, prompt | aspectRatio |
| 2031353763945897986 | 全能图片X-图生图-低价渠道版 | image-to-image | 全能图片X | rh-ai | `/openapi/v2/rhart-image-g/image-to-image` | 0.08 CNY/次 | model, prompt | imageUrl |
| 2030982909433036801 | 全能图片V1-图生图-官方稳定版 | image-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-v1-official/edit` | 0.2 CNY/张 | prompt, aspectRatio, imageUrls | - |
| 2030980802260844546 | 全能图片V1-文生图-官方稳定版 | text-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-v1-official/text-to-image` | 0.2 CNY/张 | prompt, aspectRatio | - |
| 2030249347301851137 | 万相2.2-文生视频 | text-to-video | Wan Video Models | rh-ai | `/openapi/v2/rhart-video/wan-2.2/text-to-video` | 0.07 CNY/秒 | 133##select, 130##select, 14##positive_prompt | 14##negative_prompt |
| 2030246740046987266 | qwen-image/text-to-image-2512 | text-to-image | Qwen Image | rh-ai | `/openapi/v2/rhart-image/qwen-image/text-to-image-2512` | 0.12 CNY/次 | 3##text, 25##select, 31##file_type | 23##value, 24##value |
| 2030226215631405057 | 万相2.2-首尾帧生视频 | image-to-video | Wan Video Models | rh-ai | `/openapi/v2/rhart-video/wan-2.2/start-to-end` | 0.07 CNY/秒 | 219##image, 222##image, 183##text, 202##select, 218##select | 16##negative_prompt |
| 2029753575120650241 | kling-elements | image-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-elements` | 0.06 CNY/次 | name, description, imageUrl, elementReferList | tagList |
| 2028793089092763649 | 万相2.6-参考生视频 | image-to-video | 最近上新 | wan | `/openapi/v2/alibaba/wan-2.6/reference-to-video` | 0.45 CNY/秒 | prompt, size, duration | negativePrompt, videoUrls, imageUrls, shotType |
| 2028768183504355329 | 万相2.6-参考生视频Flash | image-to-video | 最近上新 | wan | `/openapi/v2/alibaba/wan-2.6/reference-to-video-flash` | 0.11 CNY/秒 | prompt, size, duration, audio | negativePrompt, videoUrls, imageUrls, shotType |
| 2028739604871659521 | hitem3d-portrait-v15/multi-image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-portrait-v15/multi-image-to-3d` | 5.6 CNY/次 | requestType, frontImageUrl, resolution | backImageUrl, leftImageUrl, rightImageUrl, face |
| 2028739462231769089 | hitem3d-portrait-v15/image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-portrait-v15/image-to-3d` | 5.6 CNY/次 | requestType, imageUrl, resolution | face |
| 2028739179481153538 | hitem3d-portrait-v20/multi-image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-portrait-v20/multi-image-to-3d` | 5.6 CNY/次 | requestType, frontImageUrl, resolution | backImageUrl, leftImageUrl, rightImageUrl, face |
| 2028738983598768130 | hitem3d-portrait-v20/image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-portrait-v20/image-to-3d` | 5.6 CNY/次 | requestType, imageUrl, resolution | face |
| 2028733219521970177 | hitem3d-portrait-v21/multi-image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-portrait-v21/multi-image-to-3d` | 5.6 CNY/次 | requestType, frontImageUrl, resolution | backImageUrl, leftImageUrl, rightImageUrl, face |
| 2028733020951035905 | hitem3d-portrait-v21/image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-portrait-v21/image-to-3d` | 5.6 CNY/次 | requestType, imageUrl, resolution | face |
| 2028732142722498561 | hitem3d-v2/multi-image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-v2/multi-image-to-3d` | 5.6 CNY/次 | requestType, frontImageUrl, resolution | backImageUrl, leftImageUrl, rightImageUrl, face |
| 2028731529062268929 | hitem3d-v15/multi-image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-v15/multi-image-to-3d` | 0.7 CNY/次 | requestType, frontImageUrl, resolution | backImageUrl, leftImageUrl, rightImageUrl, face |
| 2028730859546492930 | hitem3d-v2/image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-v2/image-to-3d` | 5.6 CNY/次 | requestType, imageUrl, resolution | face |
| 2028725956430282754 | hitem3d-v15/image-to-3d | image-to-3D | Hitem3D | rh-ai | `/openapi/v2/hitem3d-v15/image-to-3d` | 0.7 CNY/次 | requestType, imageUrl, resolution | face |
| 2028356572536913921 | 全能图片G-1.5-图生图-官方稳定版 | image-to-image | 全能图片G | openai | `/openapi/v2/rhart-image-g-1.5-official/image-to-image` | 0.06 CNY/次 | prompt, imageUrls, size, quality | inputFidelity, background |
| 2028353022540922882 | 全能图片G-1.5-文生图-官方稳定版 | text-to-image | 全能图片G | openai | `/openapi/v2/rhart-image-g-1.5-official/text-to-image` | 0.06 CNY/次 | prompt, size, quality | background |
| 2028310887460519937 | 全能视频X-编辑视频-官方稳定版 | video-edit | 全能视频X | rh-ai | `/openapi/v2/rhart-video-g-official/edit-video` | 0.41 CNY/秒 | prompt, videoUrl, resolution | - |
| 2028308297217753089 | 全能视频X-文生视频-官方稳定版 | text-to-video | 全能视频X | rh-ai | `/openapi/v2/rhart-video-g-official/text-to-video` | 1.89 CNY/次 | prompt, aspectRatio, resolution, duration | - |
| 2028306154142318593 | 全能视频X-图生视频-官方稳定版 | image-to-video | 全能视频X | rh-ai | `/openapi/v2/rhart-video-g-official/image-to-video` | 1.89 CNY/次 | prompt, imageUrl, resolution, duration | - |
| 2027661818379649025 | 全能图片V2-图生图-官方稳定版 | image-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-n-g31-flash-official/image-to-image` | 0.49 CNY/次 | imageUrls, prompt, resolution | aspectRatio |
| 2027658953443524610 | 全能图片V2-文生图-官方稳定版 | text-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-n-g31-flash-official/text-to-image` | 0.49 CNY/次 | prompt, resolution | aspectRatio |
| 2026215209183760386 | seedream-v5-lite-图生图 | image-to-image | Seedream | rh-ai | `/openapi/v2/seedream-v5-lite/image-to-image` | 0.22 CNY/张 | prompt, imageUrls | width, height, sequentialImageGeneration, maxImages, resolution |
| 2026214940576337921 | seedream-v5-lite-文生图 | text-to-image | Seedream | rh-ai | `/openapi/v2/seedream-v5-lite/text-to-image` | 0.22 CNY/张 | prompt | width, height, sequentialImageGeneration, maxImages, toolsType, resolution |
| 2022226376775716865 | 全能视频V3.1-pro-视频扩展-官方稳定版 | text-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-pro-official/video-extend` | 17.4 CNY/次 | video, resolution | prompt, negativePrompt, seed |
| 2022225870330286082 | 全能视频V3.1-fast-图生视频-官方稳定版 | image-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-fast-official/image-to-video` | 2.35 CNY/次 | prompt, imageUrl, resolution, duration, generateAudio | lastImageUrl, negativePrompt, seed, aspectRatio |
| 2022222100292718594 | 全能视频V3.1-fast-视频扩展-官方稳定版 | text-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-fast-official/video-extend` | 6.56 CNY/次 | video, resolution | prompt, negativePrompt, seed |
| 2022221492944916482 | 全能视频V3.1-pro-参考生视频-官方稳定版 | image-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-pro-official/reference-to-video` | 9.4 CNY/次 | prompt, imageUrls, resolution, generateAudio | negativePrompt, seed |
| 2022220635650150401 | 全能视频V3.1-fast-文生视频-官方稳定版 | text-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-fast-official/text-to-video` | 2.35 CNY/次 | prompt, duration, resolution, generateAudio | aspectRatio, negativePrompt, seed |
| 2022213697642188801 | 全能视频V3.1-pro-图生视频-官方稳定版 | image-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-pro-official/image-to-video` | 4.7 CNY/次 | prompt, imageUrl, resolution, duration, generateAudio | lastImageUrl, negativePrompt, seed, aspectRatio |
| 2022195635475992577 | 全能视频V3.1-pro-文生视频-官方稳定版 | text-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-pro-official/text-to-video` | 4.7 CNY/次 | prompt, duration, resolution, generateAudio | aspectRatio, negativePrompt, seed |
| 2021514824548372482 | minimax/voice-clone | text-to-audio | Minmax Hailuo Audio | rh-ai | `/openapi/v2/rhart-audio/text-to-audio/voice-clone` | 3.12 CNY/字符 | audio, custom_voice_id, text, need_noise_reduction, need_volume_normalization, model | accuracy, language_boost |
| 2021508655436025857 | kling-v2.6-pro-动作控制 | motion-control | 可灵 | rh-ai | `/openapi/v2/kling-v2.6-pro/motion-control` | 0.56 CNY/秒 | imageUrl, videoUrl, characterOrientation | prompt, keepOriginalSound |
| 2021500676489891841 | kling-v2.6-std-动作控制 | motion-control | 可灵 | rh-ai | `/openapi/v2/kling-v2.6-std/motion-control` | 0.35 CNY/秒 | imageUrl, videoUrl, characterOrientation | prompt, keepOriginalSound |
| 2020818466334068738 | minimax/music-2.5 | text-to-audio | Minmax Hailuo Audio | rh-ai | `/openapi/v2/rhart-audio/text-to-audio/music-2.5` | 0.8 CNY/次 | prompt, lyrics | bitrate, sampleRate |
| 2019687963136692226 | Vidu-首尾帧生视频-q3-pro | image-to-video | Vidu | vidu | `/openapi/v2/vidu/start-end-to-video-q3-pro` | 0.31 CNY/秒 | prompt, firstImageUrl, lastImageUrl, duration, resolution, movementAmplitude, audio | - |
| 2019677753793908737 | Vidu-首尾帧生视频-q3-turbo | image-to-video | Vidu | vidu | `/openapi/v2/vidu/start-end-to-video-q3-turbo` | 0.18 CNY/秒 | prompt, firstImageUrl, lastImageUrl, duration, resolution, movementAmplitude, audio | - |
| 2019676917055426562 | Vidu-图生视频-q3-turbo | image-to-video | Vidu | vidu | `/openapi/v2/vidu/image-to-video-q3-turbo` | 0.18 CNY/秒 | prompt, imageUrl, duration, resolution, audio | - |
| 2019675673893081089 | Vidu-文生视频-q3-turbo | text-to-video | Vidu | vidu | `/openapi/v2/vidu/text-to-video-q3-turbo` | 0.18 CNY/秒 | prompt, style, aspectRatio, resolution, duration, audio | - |
| 2019638479950254081 | kling-video-o3-pro/reference-to-video | reference-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-pro/reference-to-video` | 0.72 CNY/秒 | prompt, keepOriginalSound, duration | videoUrl, imageUrls, sound, aspectRatio, multiPrompt, elementList, multiShot, shotType |
| 2019634451799412737 | kling-video-o3-std/reference-to-video | reference-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-std/reference-to-video` | 0.54 CNY/秒 | prompt, keepOriginalSound, duration | videoUrl, imageUrls, sound, aspectRatio, multiPrompt, multiShot, shotType |
| 2019630371244937218 | kling-video-o3-pro/video-edit | video-edit | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-pro/video-edit` | 1.08 CNY/秒 | prompt, videoUrl, keepOriginalSound | imageUrls, elementList |
| 2019627228264206337 | kling-video-o3-std/video-edit | video-edit | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-std/video-edit` | 0.81 CNY/秒 | prompt, videoUrl, keepOriginalSound | imageUrls |
| 2019623243725737985 | 可灵图生视频o3-pro | image-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-pro/image-to-video` | 0.69 CNY/秒 | prompt, firstImageUrl, duration, sound | lastImageUrl, multiPrompt, elementList, multiShot, shotType |
| 2019621114621530113 | 可灵图生视频o3-std | image-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-std/image-to-video` | 0.52 CNY/秒 | prompt, firstImageUrl, duration, sound | lastImageUrl, multiPrompt, multiShot, shotType |
| 2019620511153459202 | 可灵文生视频o3-pro | text-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-pro/text-to-video` | 0.69 CNY/秒 | prompt, sound, duration | aspectRatio, multiPrompt, elementList, multiShot, shotType |
| 2019608799960436737 | 可灵文生视频o3-std | text-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-video-o3-std/text-to-video` | 0.52 CNY/秒 | prompt, sound, duration | aspectRatio, multiPrompt, multiShot, shotType |
| 2019395031670263809 | 混元图生3D模型v3.1 | image-to-3D | 混元 3D | tencent | `/openapi/v2/hunyuan3d-v3.1/image-to-3d` | 4.2 CNY/次 | enablePbr, generateType, imageUrl | faceCount, leftImageUrl, rightImageUrl, backImageUrl, topImageUrl, bottomImageUrl, leftFrontImageUrl, rightFrontImageUrl |
| 2019394105442111490 | 混元文生3D模型v3.1 | text-to-3D | 混元 3D | tencent | `/openapi/v2/hunyuan3d-v3.1/text-to-3d` | 1.8 CNY/次 | prompt, enablePbr, generateType | faceCount |
| 2019393210805456897 | 全能视频X-文生视频-低价渠道版-v1.5 | text-to-video | 全能视频X | rh-ai | `/openapi/v2/rhart-video-g/text-to-video` | 0.04 CNY/秒 | prompt, aspectRatio, resolution, duration | - |
| 2019380112598044674 | 全能视频X-图生视频-低价渠道版-v1.5 | image-to-video | 全能视频X | rh-ai | `/openapi/v2/rhart-video-g/image-to-video` | 0.04 CNY/秒 | prompt, aspectRatio, resolution, duration | imageUrls |
| 2019246422731591682 | 可灵图生视频3.0-pro | image-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-v3.0-pro/image-to-video` | 0.69 CNY/秒 | prompt, firstImageUrl, duration, sound | negativePrompt, lastImageUrl, cfgScale, multiPrompt, elementList, multiShot, shotType |
| 2019246066681319426 | 可灵文生视频3.0-pro | text-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-v3.0-pro/text-to-video` | 0.69 CNY/秒 | prompt, duration, sound | negativePrompt, aspectRatio, cfgScale, multiPrompt, multiShot, shotType |
| 2019243340861870082 | 可灵图生视频3.0-std | image-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-v3.0-std/image-to-video` | 0.52 CNY/秒 | prompt, firstImageUrl, duration, sound | negativePrompt, lastImageUrl, cfgScale, multiPrompt, elementList, multiShot, shotType |
| 2019233725814214658 | 可灵文生视频3.0-std | text-to-video | 可灵 3.0 | rh-ai | `/openapi/v2/kling-v3.0-std/text-to-video` | 0.52 CNY/秒 | prompt, duration, sound | negativePrompt, aspectRatio, cfgScale, multiPrompt, multiShot, shotType |
| 2019027887547813889 | minimax/speech-2.8-turbo | text-to-audio | Minmax Hailuo Audio | rh-ai | `/openapi/v2/rhart-audio/text-to-audio/speech-2.8-turbo` | 0.37 CNY/字符 | text, voice_id, enable_base64_output, english_normalization | pronunciation_dict, speed, volume, pitch, emotion |
| 2019020691690819585 | minimax/speech-2.6-turbo | text-to-audio | Minmax Hailuo Audio | rh-ai | `/openapi/v2/rhart-audio/text-to-audio/speech-2.6-turbo` | 0.37 CNY/字符 | text, voice_id, enable_base64_output, english_normalization | pronunciation_dict, speed, volume, pitch, emotion |
| 2019020262185701377 | minimax/speech-2.6-hd | text-to-audio | Minmax Hailuo Audio | rh-ai | `/openapi/v2/rhart-audio/text-to-audio/speech-2.6-hd` | 0.62 CNY/字符 | text, voice_id, enable_base64_output, english_normalization | pronunciation_dict, speed, volume, pitch, emotion |
| 2018996840906952706 | minimax/speech-02-turbo | text-to-audio | Minmax Hailuo Audio | rh-ai | `/openapi/v2/rhart-audio/text-to-audio/speech-02-turbo` | 0.19 CNY/字符 | text, voice_id, enable_base64_output, english_normalization | pronunciation_dict, speed, volume, pitch, emotion |
| 2018994703099564034 | minimax/speech-02-hd | text-to-audio | Minmax Hailuo Audio | rh-ai | `/openapi/v2/rhart-audio/text-to-audio/speech-02-hd` | 0.31 CNY/字符 | text, voice_id, enable_base64_output, english_normalization | pronunciation_dict, speed, volume, pitch, emotion |
| 2018969332367036417 | minimax/speech-2.8-hd | text-to-audio | Minmax Hailuo Audio | rh-ai | `/openapi/v2/rhart-audio/text-to-audio/speech-2.8-hd` | 0.62 CNY/字符 | text, voice_id, enable_base64_output, english_normalization | pronunciation_dict, speed, volume, pitch, emotion |
| 2018603743257628674 | Vidu-首尾帧生视频-q2-pro-fast | image-to-video | Vidu | vidu | `/openapi/v2/vidu/start-end-to-video-q2-pro-fast` | 0.18 CNY/次 | prompt, firstImageUrl, lastImageUrl, duration, resolution, movementAmplitude, bgm | - |
| 2018603399320506370 | Vidu-图生视频-q2-pro-fast | image-to-video | Vidu | vidu | `/openapi/v2/vidu/image-to-video-q2-pro-fast` | 0.18 CNY/次 | prompt, imageUrl, duration, resolution, movementAmplitude, bgm | - |
| 2018599147311271938 | 全能视频V3.1-pro-首尾帧生视频-低价渠道版 | image-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-pro/start-end-to-video` | 0.9 CNY/次 | prompt, firstFrameUrl, aspectRatio, resolution | lastFrameUrl, duration |
| 2018518935961669634 | 全能视频V3.1-pro-图生视频-低价渠道版 | image-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-pro/image-to-video` | 0.8 CNY/次 | prompt, aspectRatio, imageUrl, resolution | duration |
| 2017174622010937346 | Vidu-图生视频-q3-pro | image-to-video | Vidu | vidu | `/openapi/v2/vidu/image-to-video-q3-pro` | 0.31 CNY/秒 | prompt, imageUrl, duration, resolution, audio | - |
| 2017148354741735426 | Vidu-文生视频-q3-pro | text-to-video | Vidu | vidu | `/openapi/v2/vidu/text-to-video-q3-pro` | 0.31 CNY/秒 | prompt, style, aspectRatio, resolution, duration, audio | - |
| 2016834293520994306 | 悠船文生图-v7 | text-to-image | 悠船 AI 绘图 | rh-ai | `/openapi/v2/youchuan/text-to-image-v7` | 0.54 CNY/次 | prompt | negativePrompt, chaos, quality, stylize, weird, raw, imageUrl, iw, sref, sw, sv, oref, ow, tile, aspectRatio |
| 2016833484322312193 | 悠船文生图-niji7 | text-to-image | 悠船 AI 绘图 | rh-ai | `/openapi/v2/youchuan/text-to-image-niji7` | 0.54 CNY/次 | prompt | chaos, stylize, weird, raw, imageUrl, iw, sref, sw, sv, aspectRatio |
| 2016832698641092610 | 悠船文生图-niji6 | text-to-image | 悠船 AI 绘图 | rh-ai | `/openapi/v2/youchuan/text-to-image-niji6` | 0.54 CNY/次 | prompt | chaos, quality, stylize, weird, raw, imageUrl, iw, cref, cw, sref, sw, sv, stop, tile, aspectRatio |
| 2016831146606006273 | 悠船文生图-v61 | text-to-image | 悠船 AI 绘图 | rh-ai | `/openapi/v2/youchuan/text-to-image-v61` | 0.54 CNY/次 | prompt | negativePrompt, chaos, quality, stylize, weird, raw, imageUrl, iw, cref, cw, sref, sw, sv, stop, tile, aspectRatio |
| 2016813986152255489 | 悠船文生图-v6 | text-to-image | 悠船 AI 绘图 | rh-ai | `/openapi/v2/youchuan/text-to-image-v6` | 0.54 CNY/次 | prompt | chaos, quality, stylize, weird, raw, imageUrl, iw, cref, cw, sref, sw, sv, stop, tile, aspectRatio |
| 2016785492810731522 | 悠船图生视频 | image-to-video | 悠船 AI 视频 | rh-ai | `/openapi/v2/youchuan/image-to-video` | 0.54 CNY/次 | prompt, firstImageUrl, resolution | lastImageUrl, motion, raw, loop |
| 2016409516394209282 | Vidu-参考生视频-q2-pro | reference-to-video | Vidu | vidu | `/openapi/v2/vidu/reference-to-video-q2-pro` | 0.44 CNY/次 | prompt | imageUrls, videos, aspectRatio, resolution, duration, movementAmplitude |
| 2016052223404204034 | 全能视频V3.1-fast-首尾帧生视频-低价渠道版 | image-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-fast/start-end-to-video` | 1.5 CNY/次 | prompt, firstFrameUrl, aspectRatio, resolution | lastFrameUrl, duration |
| 2015599839481749506 | 全能图片PRO-图生图Ultra-官方稳定版 | image-to-image | 全能图片 | google | `/openapi/v2/rhart-image-n-pro-official/edit-ultra` | 0.98 CNY/次 | imageUrls, prompt, resolution | aspectRatio |
| 2015599191101071361 | 全能图片PRO-文生图Ultra-官方稳定版 | text-to-image | 全能图片 | google | `/openapi/v2/rhart-image-n-pro-official/text-to-image-ultra` | 0.98 CNY/次 | prompt, resolution | aspectRatio |
| 2014265134358548482 | seedance-v1-lite-reference-to-video | reference-to-video | Seedance | rh-ai | `/openapi/v2/seedance-v1-lite/reference-to-video` | 0.07 CNY/秒 | prompt, imageUrls, duration, cameraFixed, resolution, aspectRatio | - |
| 2014264198047289346 | seedance-v1.5-pro-image-to-video-fast | image-to-video | Seedance | rh-ai | `/openapi/v2/seedance-v1.5-pro/image-to-video-fast` | 0.16 CNY/秒 | prompt, firstImageUrl, aspectRatio, duration, resolution, generateAudio, cameraFixed | lastImageUrl |
| 2014263882631434241 | seedance-v1.5-pro-image-to-video | image-to-video | Seedance | rh-ai | `/openapi/v2/seedance-v1.5-pro/image-to-video` | 0.07 CNY/秒 | prompt, firstImageUrl, aspectRatio, duration, resolution, generateAudio, cameraFixed | lastImageUrl |
| 2014263510567309314 | seedance-v1.5-pro-text-to-video-fast | text-to-video | Seedance | rh-ai | `/openapi/v2/seedance-v1.5-pro/text-to-video-fast` | 0.16 CNY/秒 | prompt, aspectRatio, duration, resolution, generateAudio, cameraFixed | - |
| 2014262901541785601 | seedance-v1.5-pro-text-to-video | text-to-video | Seedance | rh-ai | `/openapi/v2/seedance-v1.5-pro/text-to-video` | 0.07 CNY/秒 | prompt, aspectRatio, duration, resolution, generateAudio, cameraFixed | - |
| 2013851110357684225 | Vidu-参考生视频-q2 | reference-to-video | Vidu | vidu | `/openapi/v2/vidu/reference-to-video-q2` | 0.33 CNY/次 | prompt, imageUrls, aspectRatio, resolution, duration, movementAmplitude | - |
| 2013835030981595138 | Vidu-图生视频-q2-turbo | image-to-video | Vidu | vidu | `/openapi/v2/vidu/image-to-video-q2-turbo` | 0.13 CNY/次 | prompt, imageUrl, duration, resolution, movementAmplitude, bgm | - |
| 2013834337629589505 | Vidu-图生视频-q2-pro | image-to-video | Vidu | vidu | `/openapi/v2/vidu/image-to-video-q2-pro` | 0.18 CNY/次 | prompt, imageUrl, duration, resolution, movementAmplitude, bgm | - |
| 2013140888253165569 | 万相2.6-图生视频Flash | image-to-video | Wan Video Models | wan | `/openapi/v2/alibaba/wan-2.6/image-to-video-flash` | 0.11 CNY/秒 | prompt, imageUrl, resolution, duration, shotType, enablePromptExpansion, enableAudio | negativePrompt, audioUrl |
| 2013097204925132801 | 可灵图生视频2.6-pro | image-to-video | 可灵 | rh-ai | `/openapi/v2/kling-v2.6-pro/image-to-video` | 1.75 CNY/次 | prompt, imageUrl, sound, duration | negativePrompt |
| 2013088838681161729 | 可灵文生视频2.6-pro | text-to-video | 可灵 | rh-ai | `/openapi/v2/kling-v2.6-pro/text-to-video` | 1.75 CNY/次 | prompt, sound, aspectRatio, duration | negativePrompt |
| 2013062293031809026 | seedream-v4-图生图 | image-to-image | Seedream | rh-ai | `/openapi/v2/seedream-v4/image-to-image` | 0.14 CNY/张 | prompt, imageUrls | width, height, sequentialImageGeneration, maxImages, resolution |
| 2013061949732220929 | seedream-v4-文生图 | text-to-image | Seedream | rh-ai | `/openapi/v2/seedream-v4/text-to-image` | 0.14 CNY/张 | prompt | width, height, sequentialImageGeneration, maxImages, resolution |
| 2012067220412493828 | 可灵视频编辑o1 | video-edit | 可灵 | rh-ai | `/openapi/v2/kling-video-o1-std/edit-video` | 6.3 CNY/次 | mode, prompt, videoUrl, keepOriginalSound | imageUrls |
| 2012067220412493827 | 可灵参考生视频o1 | reference-to-video | 可灵 | rh-ai | `/openapi/v2/kling-video-o1-std/refrence-to-video` | 3.15 CNY/次 | mode, prompt, aspectRatio, duration, keepOriginalSound | imageUrls, videoUrl |
| 2012067220412493826 | 全能视频S-图生视频-pro-官方稳定版 | image-to-video | 全能视频S | openai | `/openapi/v2/rhart-video-s-official/image-to-video-pro` | 2.1 CNY/秒 | prompt, resolution, duration, imageUrl | - |
| 2012065966164602881 | 全能视频S-文生视频-pro-官方稳定版 | image-to-video | 全能视频S | openai | `/openapi/v2/rhart-video-s-official/text-to-video-pro` | 2.1 CNY/秒 | prompt, size, duration | - |
| 2012057792137195522 | 全能视频S-图生视频-官方稳定版 | image-to-video | 全能视频S | openai | `/openapi/v2/rhart-video-s-official/image-to-video` | 2.28 CNY/次 | prompt, duration, imageUrl | - |
| 2012030892408893442 | 可灵首尾帧生视频o1 | image-to-video | 可灵 | rh-ai | `/openapi/v2/kling-video-o1/start-to-end` | 2.1 CNY/次 | aspectRatio, duration, firstImageUrl, lastImageUrl, mode | prompt |
| 2012030892408893441 | 海螺-02-fast | image-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-02/fast` | 0.45 CNY/次 | enablePromptExpansion, imageUrl, duration | prompt |
| 2012030193839173634 | 海螺-02-pro | text-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-02/pro` | 2.63 CNY/次 | prompt, enablePromptExpansion, duration | firstImageUrl, lastImageUrl |
| 2012029710558883841 | 海螺-02-标准 | image-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-02/standard` | 1.5 CNY/次 | prompt, enablePromptExpansion, duration | firstImageUrl, lastImageUrl |
| 2012013242253373441 | 海螺-02-文生视频-pro | text-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-02/t2v-pro` | 2.63 CNY/次 | prompt, enablePromptExpansion | - |
| 2012004604507910146 | 可灵图生视频o1 | image-to-video | 可灵 | rh-ai | `/openapi/v2/kling-video-o1/image-to-video` | 2.1 CNY/次 | aspectRatio, duration, firstImageUrl, mode | prompt |
| 2012004604507910145 | 海螺-02-图生视频-pro | image-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-02/i2v-pro` | 2.63 CNY/次 | enablePromptExpansion, firstImageUrl | prompt, lastImageUrl |
| 2012001656184827905 | 可灵文生视频o1 | text-to-video | 可灵 | rh-ai | `/openapi/v2/kling-video-o1/text-to-video` | 2.1 CNY/次 | aspectRatio, duration, mode | prompt |
| 2011999330699112450 | 海螺-2.3-文生视频-pro | text-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-2.3/t2v-pro` | 2.63 CNY/次 | prompt, enablePromptExpansion | - |
| 2011758831593648131 | 海螺-2.3-图生视频-pro | image-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-2.3/image-to-video-pro` | 2.63 CNY/次 | enablePromptExpansion, imageUrl | prompt |
| 2011758831593648130 | seedream-v4.5-图生图 | image-to-image | Seedream | rh-ai | `/openapi/v2/seedream-v4.5/image-to-image` | 0.2 CNY/张 | prompt, imageUrls | width, height, sequentialImageGeneration, maxImages, resolution |
| 2011749656515899394 | 海螺-2.3-fast-pro-图生视频 | image-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-2.3-fast-pro/image-to-video` | 1.73 CNY/次 | prompt, enablePromptExpansion, imageUrl, duration | - |
| 2011738168711507969 | 海螺-2.3-fast-图生视频 | image-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-2.3-fast/image-to-video` | 1.01 CNY/次 | prompt, enablePromptExpansion, imageUrl, duration | - |
| 2011737762002432002 | 海螺-2.3-图生视频-标准 | image-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-2.3/i2v-standard` | 1.5 CNY/次 | enablePromptExpansion, imageUrl, duration | prompt |
| 2011737289136599042 | 海螺-02-图生视频-标准 | image-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-02/i2v-standard` | 1.5 CNY/次 | enablePromptExpansion, firstImageUrl, duration | prompt, lastImageUrl |
| 2011736159161741314 | 海螺-2.3-文生视频-标准 | text-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-2.3/t2v-standard` | 1.5 CNY/次 | prompt, enablePromptExpansion, duration | - |
| 2011735493609582593 | 海螺-02-文生视频-标准 | text-to-video | 海螺AI | rh-ai | `/openapi/v2/minimax/hailuo-02/t2v-standard` | 1.5 CNY/次 | prompt, enablePromptExpansion, duration | - |
| 2011729237939384321 | seedream-v4.5-文生图 | text-to-image | Seedream | rh-ai | `/openapi/v2/seedream-v4.5/text-to-image` | 0.2 CNY/张 | prompt | width, height, sequentialImageGeneration, maxImages, resolution |
| 2011726198578933762 | 全能视频S-文生视频-官方稳定版 | text-to-video | 全能视频S | openai | `/openapi/v2/rhart-video-s-official/text-to-video` | 2.28 CNY/次 | prompt, size, duration | - |
| 2011680658176684034 | Vidu-首尾帧生视频-q2-turbo | image-to-video | Vidu | vidu | `/openapi/v2/vidu/start-end-to-video-q2-turbo` | 0.13 CNY/次 | prompt, firstImageUrl, lastImageUrl, duration, resolution, movementAmplitude, bgm | - |
| 2011680028485824514 | Vidu-首尾帧生视频-q2-pro | image-to-video | Vidu | vidu | `/openapi/v2/vidu/start-end-to-video-q2-pro` | 0.18 CNY/次 | prompt, firstImageUrl, lastImageUrl, duration, resolution, movementAmplitude, bgm | - |
| 2011646882759389186 | Vidu-文生视频-q2 | text-to-video | Vidu | vidu | `/openapi/v2/vidu/text-to-video` | 0.22 CNY/次 | prompt, style, aspectRatio, resolution, movementAmplitude, duration | - |
| 2011410147815272449 | 可灵图生视频2.5-turbo-std | image-to-video | 可灵 | rh-ai | `/openapi/v2/kling-v2.5-turbo-std/image-to-video` | 1.05 CNY/次 | prompt, duration, firstImageUrl | negativePrompt, guidanceScale |
| 2011409246597754881 | 可灵图生视频2.5-turbo-pro | image-to-video | 可灵 | rh-ai | `/openapi/v2/kling-v2.5-turbo-pro/image-to-video` | 1.75 CNY/次 | duration, firstImageUrl | prompt, negativePrompt, guidanceScale, lastImageUrl |
| 2011408949544562690 | 可灵文生视频2.5-turbo-pro | text-to-video | 可灵 | rh-ai | `/openapi/v2/kling-v2.5-turbo-pro/text-to-video` | 1.75 CNY/次 | prompt, duration, aspectRatio | negativePrompt, guidanceScale |
| 2011327434521391105 | 万相2.6-图生视频 | image-to-video | Wan Video Models | wan | `/openapi/v2/alibaba/wan-2.6/image-to-video` | 2.25 CNY/次 | imageUrl, resolution, duration, shotType | prompt, negativePrompt |
| 2011281240097107969 | 万相2.6-文生视频 | text-to-video | Wan Video Models | wan | `/openapi/v2/alibaba/wan-2.6/text-to-video` | 2.25 CNY/次 | prompt, duration, resolution, shotType | negativePrompt |
| 2011055907607490562 | 全能视频S-角色上传-低价渠道版 | video-tools | 全能视频S | rh-ai | `/openapi/v2/rhart-video-s/sora-upload-character` | 0.05 CNY/次 | videoUrl | - |
| 2011003029035495426 | 全能视频S-文生视频-pro-低价渠道版-已下架 | text-to-video | 全能视频S | rh-ai | `/openapi/v2/rhart-video-s/text-to-video-pro-deprecated` | 1 CNY/次 | prompt, duration, aspectRatio | storyboard |
| 2010915780436504578 | 全能视频S-图生视频-pro-低价渠道版-已下架 | image-to-video | 全能视频S | rh-ai | `/openapi/v2/rhart-video-s/image-to-video-pro-deprecated` | 1 CNY/次 | prompt, imageUrl, duration, aspectRatio | storyboard |
| 2005910264819793921 | 全能视频V3.1-fast-图生视频-低价渠道版 | image-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-fast/image-to-video` | 1.5 CNY/次 | prompt, aspectRatio, imageUrls, resolution | duration |
| 2005884653783007234 | 全能视频V3.1-pro-文生视频-低价渠道版 | text-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-pro/text-to-video` | 0.9 CNY/次 | prompt, aspectRatio, resolution | duration |
| 2005884261993070594 | 全能视频V3.1-fast-文生视频-低价渠道版 | text-to-video | 全能视频V | google | `/openapi/v2/rhart-video-v3.1-fast/text-to-video` | 1.5 CNY/次 | prompt, aspectRatio, resolution | duration |
| 2005642874987003905 | 全能图片G-1.5-图生图-低价渠道版-(已下架) 可用全能图片G-2 模型代替 | image-to-image | - | openai | `<详情不可访问>` | 0.03 CNY/次 | - | - |
| 2005642306994356226 | 全能图片G-1.5-文生图-低价渠道版-(已下架) 可用全能图片G-2 模型代替 | text-to-image | - | openai | `<详情不可访问>` | 0.03 CNY/次 | - | - |
| 2004544597055029250 | 全能图片PRO-文生图-官方稳定版 | text-to-image | 全能图片 | google | `/openapi/v2/rhart-image-n-pro-official/text-to-image` | 0.8 CNY/次 | resolution, prompt | aspectRatio |
| 2004544343584849921 | 全能图片PRO-图生图-官方稳定版 | image-to-image | 全能图片 | google | `/openapi/v2/rhart-image-n-pro-official/edit` | 0.8 CNY/次 | imageUrls, prompt, resolution | aspectRatio |
| 2004543090783993858 | 全能图片V1-文生图-低价渠道版 | text-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-v1/text-to-image` | 0.05 CNY/张 | prompt, aspectRatio | - |
| 2004542825494265857 | 全能图片V1-图生图-低价渠道版 | image-to-image | 全能图片 | rh-ai | `/openapi/v2/rhart-image-v1/edit` | 0.05 CNY/张 | prompt, aspectRatio, imageUrls | - |
| 2004499823346368514 | 全能视频S-文生视频-低价渠道版 | text-to-video | 全能视频S | rh-ai | `/openapi/v2/rhart-video-s/text-to-video` | 1 CNY/次 | duration, prompt, aspectRatio | storyboard |
| 2004494607725150210 | 全能视频S-图生视频-低价渠道版 | image-to-video | 全能视频S | rh-ai | `/openapi/v2/rhart-video-s/image-to-video` | 1 CNY/次 | imageUrl, duration, aspectRatio, prompt | storyboard |
| 2004491650426257409 | 全能视频S-图生视频-支持真人-官方稳定版 | image-to-video | 全能视频S | openai | `/openapi/v2/rhart-video-s-official/image-to-video-realistic` | 3.2 CNY/次 | prompt, duration, imageUrl | - |

## 标准模型字段明细

### 全能图片PRO-文生图-低价渠道版

- ID：`2004543847939751938`
- Endpoint：`/openapi/v2/rhart-image-n-pro/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片 / rh-ai
- 价格：0.4 CNY/次
- 队列/并发：queueSize=5000，concurrencyLimit=5000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一群猴子在茂密、阳光斑驳的热带森林中激烈争抢一根非常小的香蕉。香蕉小得近乎滑稽，与猴子们的体型形成鲜明对比。猴子们跳跃、伸手、抓挠，表情夸张，充满急切与渴望。... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 否 | 9:16 | - | 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9 | aspectRatio |
| resolution | LIST | 是 | 1k | priceRelated; ignoreCase | 1k, 2k, 4k | resolution |

### 全能图片PRO-图生图-低价渠道版

- ID：`2004543527918551041`
- Endpoint：`/openapi/v2/rhart-image-n-pro/edit`
- 类型/分组/来源：image-to-image / 全能图片 / rh-ai
- 价格：0.4 CNY/次
- 队列/并发：queueSize=5000，concurrencyLimit=5000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| prompt | STRING | 是 | 基于原图风格，将老奶奶替换为一只年迈慈祥的猴子奶奶，她穿着格子围裙，正用香蕉制作晚餐，锅里是香蕉炖菜或香蕉泥。厨房环境保持不变：木制家具、悬挂的辣椒大蒜、陶罐... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 否 | 3:4 | - | 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9 | 不传 aspectRatio 参数时为自适应图片尺寸 |
| resolution | LIST | 是 | 1k | priceRelated; ignoreCase | 1k, 2k, 4k | resolution |

### 全能图片V2-图生图-低价渠道版

- ID：`2027196343409463297`
- Endpoint：`/openapi/v2/rhart-image-n-g31-flash/image-to-image`
- 类型/分组/来源：image-to-image / 全能图片 / rh-ai
- 价格：0.16 CNY/次
- 队列/并发：queueSize=5000，concurrencyLimit=5000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 30MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| prompt | STRING | 是 | 将这张线稿转换为明代水墨武侠风格的精细彩图。严格保留人物的动作轮廓与黑鸦的形态，将背景替换为风雪交加的竹林。增强水墨晕染的纹理感，整体色调偏冷，烘托出肃杀的氛... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 否 | 9:16 | - | 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9, 1:4, 4:1, 1:8, 8:1 | aspectRatio |
| resolution | LIST | 是 | 1k | priceRelated; ignoreCase | 1k, 2k, 4k | resolution |

### 全能图片V2-文生图-低价渠道版

- ID：`2027192837726294017`
- Endpoint：`/openapi/v2/rhart-image-n-g31-flash/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片 / rh-ai
- 价格：0.16 CNY/次
- 队列/并发：queueSize=5000，concurrencyLimit=5000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一幅精美的明代国漫风格插画。一位穿着飞鱼服的锦衣卫站在古老的城墙上，俯瞰着繁华的京城夜景。画面采用平涂风格，线条硬朗，色彩对比强烈，背景有灯笼的光晕和几缕薄雾。 | length --- | - | prompt |
| aspectRatio | LIST | 否 | 9:16 | - | 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9, 1:4, 4:1, 1:8, 8:1 | aspectRatio |
| resolution | LIST | 是 | 1k | priceRelated; ignoreCase | 1k, 2k, 4k | resolution |

### suno-single-v5.5

- ID：`2046514150500524045`
- Endpoint：`/openapi/v2/rhart-audio/suno-v5.5/single`
- 类型/分组/来源：text-to-music / Suno / rh-ai
- 价格：0.72 CNY/次
- 队列/并发：queueSize=100，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| title | STRING | 否 | - | length 0-80 | - | 歌曲标题；留空时 Suno 会根据内容自动取一个 |
| description | STRING | 是 | 中国古典，琵琶+古琴，宁静，慢速，纯音乐 | length 1-400 | - | 用一段文字描述想要的音乐（风格/情绪/场景），不超过 400 字符 |
| make_instrumental | LIST | 否 | false | - | false, true | 是否生成纯演奏音乐（不含人声）；传字符串 "true"/"false" |

### suno-custom-v5.5

- ID：`2046514150500524046`
- Endpoint：`/openapi/v2/rhart-audio/suno-v5.5/custom`
- 类型/分组/来源：text-to-music / Suno / rh-ai
- 价格：0.72 CNY/次
- 队列/并发：queueSize=100，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| title | STRING | 是 | 心字香 | length 1-80 | - | 歌曲标题 |
| prompt | STRING | 是 | [Intro] 你记得我吗 不想听这些 [Verse] 红灯是桃花 桃花飘飘 白马披铁甲 铁甲销销 你放下我吧 听说你放下我了 再也不爱了吗 [Pre-Cho... | length 1-5000 | - | 完整歌词，支持 [Verse] [Chorus] [Bridge] 等结构标签 |
| tags | STRING | 是 | 流行,民谣,旋律,电影感,女声 | length 1-1000 | - | 用英文逗号分隔的风格描述 |

### suno-歌词生成

- ID：`2046514150500524047`
- Endpoint：`/openapi/v2/rhart-audio/suno/lyrics`
- 类型/分组/来源：text-to-lyrics / Suno / rh-ai
- 价格：0.014 CNY/次
- 队列/并发：queueSize=100，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 描述个人面对成长后的平静，励志，娓娓道来的感觉 | length 1-500 | - | 用一句话描述想要的歌词主题（≤500 字符） |

### suno-single-v5

- ID：`2046514150500524048`
- Endpoint：`/openapi/v2/rhart-audio/suno-v5/single`
- 类型/分组/来源：text-to-music / Suno / rh-ai
- 价格：0.72 CNY/次
- 队列/并发：queueSize=100，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| title | STRING | 否 | - | length --80 | - | 歌曲标题；留空时 Suno 会根据内容自动取一个 |
| description | STRING | 是 | 中国传统民谣，70 BPM，古筝琶音，笛子独奏，柔和女声，五声音阶，古代宫殿，月光，诗意，忧郁，重混响。 | length 1-400 | - | 用一段文字描述想要的音乐（风格/情绪/场景），不超过 400 字符 |
| make_instrumental | LIST | 否 | false | - | false, true | 是否生成纯演奏音乐（不含人声）；传字符串 "true"/"false" |

### suno-custom-v5

- ID：`2046514150500524049`
- Endpoint：`/openapi/v2/rhart-audio/suno-v5/custom`
- 类型/分组/来源：text-to-music / Suno / rh-ai
- 价格：0.72 CNY/次
- 队列/并发：queueSize=100，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| title | STRING | 是 | 峨眉风光 | length 1-80 | - | 歌曲标题 |
| prompt | STRING | 是 | [Verse] 对镜松柏云雾长 映水白练玉桥扬 如画层峦风雨荡 落笔青山谁能仿 [Chorus] 千载古道神道苍 白云藏 清风扬 佛光降 [Verse 2] ... | length 1-5000 | - | 完整歌词，支持 [Verse] [Chorus] [Bridge] 等结构标签 |
| tags | STRING | 是 | 中国风，电影感，管弦乐 | length --1000 | - | 用英文逗号分隔的风格描述 |

### suno-single-v4.5

- ID：`2046514150500524050`
- Endpoint：`/openapi/v2/rhart-audio/suno-v4.5/single`
- 类型/分组/来源：text-to-music / Suno / rh-ai
- 价格：0.72 CNY/次
- 队列/并发：queueSize=100，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| title | STRING | 否 | - | length --80 | - | 歌曲标题；留空时 Suno 会根据内容自动取一个 |
| description | STRING | 是 | 科幻氛围，70 BPM，空灵合成器垫，粒状纹理，深沉低音，未来感声音设计，广阔，空旷，零重力，神秘。 | length 1-400 | - | 用一段文字描述想要的音乐（风格/情绪/场景），不超过 400 字符 |
| make_instrumental | LIST | 否 | false | - | false, true | 是否生成纯演奏音乐（不含人声）；传字符串 "true"/"false" |

### suno-custom-v4.5

- ID：`2046514150500524051`
- Endpoint：`/openapi/v2/rhart-audio/suno-v4.5/custom`
- 类型/分组/来源：text-to-music / Suno / rh-ai
- 价格：0.72 CNY/次
- 队列/并发：queueSize=100，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| title | STRING | 是 | 光 | length 1-80 | - | 歌曲标题 |
| prompt | STRING | 是 | [Verse] 心早有预感 光速地飙燃 浑然不知山前的悬崖 生而无惧是少年 不知光阴有限 [Pre-Chorus] 想哭想笑时 都痛痛快快 全然不理会谁给的评... | length 1-5000 | - | 完整歌词，支持 [Verse] [Chorus] [Bridge] 等结构标签 |
| tags | STRING | 是 | 流行, 娓娓道来,中文 | length --1000 | - | 用英文逗号分隔的风格描述 |

### 火山字幕擦除-视频字幕擦除（精细化版）

- ID：`2132764885651525660`
- Endpoint：`/openapi/v2/volc-subtitle-erase-pro/video`
- 类型/分组/来源：video-tools / Seedance / rh-ai
- 价格：0.013 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 500MB; priceRelated; accept ["MP4","MOV","M4V","WEBM"] | - | Input video URL or uploaded video file |
| eraseType | LIST | 否 | subtitle | - | subtitle, text | Erase target type |
| encodeMode | LIST | 否 | size | - | size, quality | Output encoding preference |
| clientToken | STRING | 否 | - | - | - | 用户请求凭证，用于幂等控制。大小写敏感，不超过 64 个 ASCII 码可打印字符。 |
| eraseRatioLocation | COMPLEX[] | 否 | - | multipleInputs | - | 擦除框位置数组列表。配置此参数后，系统将仅在指定的矩形框选区域内执行文本擦除 |

### 周星驰 fast

- ID：`2065345589362483202`
- Endpoint：`/openapi/v2/rhart-video/sparkvideo-2.0/multimodal-video-star-fast`
- 类型/分组/来源：image-to-video / Seedance / rh-ai
- 价格：-
- 队列/并发：queueSize=1000，concurrencyLimit=600

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, 1080p, 2k, 4k | 视频分辨率。分为模型原生输出的分辨率（480p、720p、native1080p），与基于 720p 原生生成后进行超分放大的分辨率（1080p、2k、4k）。 |
| duration | LIST | 是 | 5 | priceRelated | -1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| ratio | LIST | 否 | adaptive | - | adaptive, 16:9, 4:3, 1:1, 3:4, 9:16, 21:9 | 视频宽高比 |
| templateId | LIST | 是 | ca4e2a5b6ba3429aa32f1b6ce8095e49 | priceRelated | ca4e2a5b6ba3429aa32f1b6ce8095e49, 3939807298934776bcaab866e5411234, ffe89289a144441c8151b119d7a69912, 806e1cb898e343bd817cf601d73bdb05, 98dc545610564969ac7ff547f40c98d2, 0aa9c61ef52a42b58e554bcac588b80c | 模板ID |
| imageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG","JPEG","WEBP"] | - | 图片 |

### 周星驰

- ID：`2065339476332175362`
- Endpoint：`/openapi/v2/rhart-video/sparkvideo-2.0/multimodal-video-star`
- 类型/分组/来源：image-to-video / Seedance / rh-ai
- 价格：-
- 队列/并发：queueSize=1000，concurrencyLimit=600

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, 1080p, native1080p, 2k, 4k | 视频分辨率。分为模型原生输出的分辨率（480p、720p、native1080p），与基于 720p 原生生成后进行超分放大的分辨率（1080p、2k、4k）。 |
| duration | LIST | 是 | 5 | priceRelated | -1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| ratio | LIST | 否 | adaptive | - | adaptive, 16:9, 4:3, 1:1, 3:4, 9:16, 21:9 | 视频宽高比 |
| templateId | LIST | 是 | ca4e2a5b6ba3429aa32f1b6ce8095e49 | priceRelated | ca4e2a5b6ba3429aa32f1b6ce8095e49, 3939807298934776bcaab866e5411234, ffe89289a144441c8151b119d7a69912, 806e1cb898e343bd817cf601d73bdb05, 98dc545610564969ac7ff547f40c98d2, 0aa9c61ef52a42b58e554bcac588b80c | 模板ID |
| imageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG","JPEG","WEBP"] | - | 图片 |

### MiniMax Music 翻唱前处理

- ID：`2064615278630940674`
- Endpoint：`/openapi/v2/minimax/music-cover-preprocess`
- 类型/分组/来源：text-to-audio / 最近上新 / minimax
- 价格：0.8 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| audioUrl | AUDIO | 是 | - | maxSize 50MB; accept ["MP3","WAV","FLAC"] | - | 参考音频，6 秒至 6 分钟 |

### 万相 2.2 角色动作迁移

- ID：`2061660305555243009`
- Endpoint：`/openapi/v2/rhart-video/wan2.2/character-motion-transfer`
- 类型/分组/来源：video-to-video / 自部署开源模型 / rh-ai
- 价格：0.24 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 299##image | IMAGE | 是 | e3915e1b2504962c279fff4dde51bf013ab972cf319cfbce35ff2eecd297003f.png | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 275##video | VIDEO | 是 | 0595caf5e9def567639819ce3ae32400694a882ec83af9b8eaa6b95d57540c79.mp4 | maxSize 10MB; priceRelated; accept ["MP4","MOV"] | - | videoUrl |

### 火山字幕擦除-视频字幕擦除（标准版）

- ID：`2059467934809726978`
- Endpoint：`/openapi/v2/volc-subtitle-erase/video`
- 类型/分组/来源：video-tools / Seedance / rh-ai
- 价格：0.005 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 500MB; priceRelated; accept ["MP4","MOV","M4V","WEBM"] | - | Input video URL or uploaded video file |
| clientToken | STRING | 否 | - | - | - | 用户请求凭证，用于幂等控制。大小写敏感，不超过 64 个 ASCII 码可打印字符。 |

### 即梦图片 4.6 图生图

- ID：`2055229427329323016`
- Endpoint：`/openapi/v2/bytedance/jimeng-4.6/image-to-image`
- 类型/分组/来源：image-to-image / 最近上新 / bytedance
- 价格：0.17 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 写实摄影，细节拉满。模特身穿轻奢晚礼服，剪裁精致、装饰精巧。立于古典宫殿大理石楼梯之上，侧身转头回眸，神态温婉动人。宫殿恢弘大气，暖调灯光错落，水晶灯饰流光溢... | length --800 | - | 用于生成或编辑图像的提示词，中英文均可。建议不超过 800 字符。 |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 15MB; priceRelated; multipleInputs; accept ["JPG","JPEG","PNG"] | - | 输入参考图片，支持 JPEG/PNG，最多 14 张，单图最大 15MB，最大分辨率 4096×4096，官方建议控制在 6 张以内。 |
| width | INT | 否 | - | range 900-6197; step 1; priceRelated | - | 输出图像宽度（像素），范围 900–6197。须与 height 同时传入才会生效，单独传 width 无效。宽高乘积须在 [1024×1024, 4096×4096]（即 1048576–16777216），且宽高比在 [minRati... |
| height | INT | 否 | - | range 768-4096; step 1; priceRelated | - | 输出图像高度（像素），范围 768–4096。须与 width 同时传入才会生效，单独传 height 无效。宽高乘积须在 [1024×1024, 4096×4096]（即 1048576–16777216），且宽高比在 [minRati... |
| scale | INT | 否 | 50 | range 1-100; step 1 | - | 文本描述影响程度。值越大文本影响越强、输入图片影响越小，默认 50。 |
| forceSingle | BOOLEAN | 否 | false | priceRelated | - | 是否强制只生成 1 张图片。开启后可降低延迟和不可控多图计费风险。 |
| minRatio | FLOAT | 否 | 0.333333 | range 0.0625-15.9999; step 0.0001; precision 4 | - | 输出宽高比下限（宽/高 ≥ minRatio），默认 1/3。 |
| maxRatio | FLOAT | 否 | 3 | range 0.0626-16; step 0.0001; precision 4 | - | 输出宽高比上限（宽/高 ≤ maxRatio），默认 3。 |

### 即梦图片 4.6 文生图

- ID：`2055229427329323015`
- Endpoint：`/openapi/v2/bytedance/jimeng-4.6/text-to-image`
- 类型/分组/来源：text-to-image / 最近上新 / bytedance
- 价格：0.17 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 专业日系动漫插画风格，完整全身人像。塑造一位颜值出众的二次元少女，五官刻画精致立体，眼瞳通透莹亮，眼神灵动有神，睫毛纤长分明，脸型线条柔和自然。发型层次丰富，... | length --800 | - | 用于生成图像的提示词，中英文均可。建议不超过 800 字符；可在 prompt 中直接描述画图比例。 |
| width | INT | 否 | - | range 900-6197; step 1; priceRelated | - | 输出图像宽度（像素），范围 900–6197。须与 height 同时传入才会生效，单独传 width 无效。宽高乘积须在 [1024×1024, 4096×4096]（即 1048576–16777216），且宽高比在 [minRati... |
| height | INT | 否 | - | range 768-4096; step 1; priceRelated | - | 输出图像高度（像素），范围 768–4096。须与 width 同时传入才会生效，单独传 height 无效。宽高乘积须在 [1024×1024, 4096×4096]（即 1048576–16777216），且宽高比在 [minRati... |
| scale | INT | 否 | 50 | range 1-100; step 1 | - | 文本描述影响程度。值越大文本描述影响越强，默认 50。 |
| forceSingle | BOOLEAN | 否 | false | priceRelated | - | 是否强制只生成 1 张图片。开启后可降低延迟和不可控多图计费风险。 |
| minRatio | FLOAT | 否 | 0.333333 | range 0.06-16; step 1; precision 2 | - | 输出宽高比下限（宽/高 ≥ minRatio），默认 1/3。 |
| maxRatio | FLOAT | 否 | 3 | range 0.06-16; step 1; precision 2 | - | 输出宽高比上限（宽/高 ≤ maxRatio），默认 3。 |

### 分离音频-Other

- ID：`2054377360666701825`
- Endpoint：`/openapi/v2/rhart-audio/extract-background`
- 类型/分组/来源：video-to-audio / 自部署开源模型 / rh-ai
- 价格：0.1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 2##file | VIDEO | 是 | 2c429fb963a46a9aae25513eaf68f0a592f3ac2f0dd7dbbe2d67a497371199c5.mp4 | maxSize 200MB; accept ["MP4","MOV"] | - | 上传视频 |

### 分离音频-Vocals

- ID：`2054375798917607425`
- Endpoint：`/openapi/v2/rhart-audio/extract-vocal`
- 类型/分组/来源：video-to-audio / 自部署开源模型 / rh-ai
- 价格：0.1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 3##file | VIDEO | 是 | 2c429fb963a46a9aae25513eaf68f0a592f3ac2f0dd7dbbe2d67a497371199c5.mp4 | maxSize 200MB; accept ["MP4","MOV"] | - | 上传视频 |

### 全能图片X-高质量文生图-官方稳定版

- ID：`2054086928526929955`
- Endpoint：`/openapi/v2/rhart-imagine-image-quality/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片X / rh-ai
- 价格：0.38 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 角色： 画仙，绝美空灵，眼波灵动，红唇轻启，气质高贵神秘。手持耀目玉笔，旁边有梦马。顶级国风动漫奇幻CG，电影级东方幻想大片质感，画面整体色彩丰富，超现实主义... | length --4000 | - | 文本提示词，描述希望生成的主体、风格、构图、光影和氛围 |
| aspectRatio | LIST | 否 | 1:1 | - | 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3 | 输出图片比例 |
| resolution | LIST | 是 | 1k | priceRelated | 1k, 2k | 输出分辨率档位 |
| numImages | LIST | 是 | 1 | priceRelated | 1, 2, 3, 4 | 生成图片数量 |
| outputFormat | LIST | 否 | jpeg | - | jpeg, png, webp | 输出图片格式 |

### 全能图片X-高质量图片编辑-官方稳定版

- ID：`2054086928526929954`
- Endpoint：`/openapi/v2/rhart-imagine-image-quality/edit`
- 类型/分组/来源：image-to-image / 全能图片X / rh-ai
- 价格：0.44 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 主角为年轻女性。使用参考图中的精确面部，保留身份特征和自然皮肤纹理。构图排列如滚动的记忆时间轴，捕捉于重叠的电影画幅和底片中。每一帧展现不同的情感微瞬间：行走... | length --4000 | - | 文本提示词，描述希望对图片进行的编辑内容以及需要保留的元素 |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 输入图片，仅支持单张图片 |
| aspectRatio | LIST | 否 | auto | - | auto, 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3 | 输出图片比例，auto 表示保持源图构图 |
| resolution | LIST | 是 | 1k | priceRelated | 1k, 2k | 输出分辨率档位 |
| numImages | LIST | 是 | 1 | priceRelated | 1, 2, 3, 4 | 生成图片数量 |
| outputFormat | LIST | 否 | jpeg | - | jpeg, png, webp | 输出图片格式 |

### 全能视频V3.1-fast-参考生视频-官方稳定版

- ID：`2054086928526929953`
- Endpoint：`/openapi/v2/rhart-video-v3.1-fast-official/reference-to-video`
- 类型/分组/来源：reference-to-video / 全能视频V / google
- 价格：4.03 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 顶级国风动漫奇幻CG，电影级东方幻想大片质感，超高饱和度，色彩爆炸，超现实主义视觉体验。地下巨大岩洞场景，瑰丽、妖异、梦幻又危险。16:9，15秒，镜头流畅丝... | length --8000 | - | 文本提示词，描述视频内容、动作、场景变化和镜头运动 |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG"] | - | 参考图片（1-3张） |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16 | 视频画幅比例 |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p | 视频分辨率 |
| generateAudio | BOOLEAN | 否 | false | priceRelated | - | 是否生成视频音频 |
| negativePrompt | STRING | 否 | - | length --2048 | - | 负向提示词，用于描述需要避免的内容或瑕疵 |
| seed | INT | 否 | - | range 0--; step 1 | - | 随机种子，用于提升结果可复现性 |

### MiniMax Music 翻唱

- ID：`2054086928526929924`
- Endpoint：`/openapi/v2/minimax/music-cover`
- 类型/分组/来源：text-to-audio / 最近上新 / minimax
- 价格：0.8 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 以键盘、吉他、萨克斯、鼓为主奏配器，整体营造日落时分慵懒松弛、温柔隽永的氛围，旋律线条悠扬绵长，多乐器层次交织，氛围感安静治愈又有留白余韵。 | length --2000 | - | 纯音乐描述，用于指定风格、情绪和场景（必填） |
| audioUrl | AUDIO | 是 | - | maxSize 50MB; accept ["MP3","WAV","FLAC"] | - | 参考音频，6 秒至 6 分钟 |
| lyrics | STRING | 否 | - | length 10-1000 | - | 歌曲歌词，如不传则通过 ASR 自动从参考音频中提取歌词，长度限制 [10, 1000] 个字符 |
| coverFeatureId | STRING | 否 | - | - | - | 翻唱前处理 接口返回的特征 ID,coverFeatureId 有效期为 24 小时,传入时 lyrics 为必填 |
| sampleRate | LIST | 否 | 44100 | - | 16000, 24000, 32000, 44100 | 采样率 |
| format | LIST | 否 | mp3 | - | mp3, wav, pcm | 音频格式 |
| bitrate | LIST | 否 | 256000 | - | 32000, 64000, 128000, 256000 | 比特率 |

### MiniMax Music 2.6

- ID：`2054086928526929923`
- Endpoint：`/openapi/v2/minimax/music-2.6`
- 类型/分组/来源：text-to-audio / 最近上新 / minimax
- 价格：0.8 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 描述个人面对成长后的平静，励志，娓娓道来的感觉 | length --2000 | - | 音乐描述，用于指定风格、情绪和场景。纯音乐（isInstrumental: true）：必填，长度限制 [1, 2000] 个字符 |
| lyrics | STRING | 否 | [Verse] 心早有预感 光速地飙燃 浑然不知山前的悬崖 生而无惧是少年 不知光阴有限 [Pre-Chorus] 想哭想笑时 都痛痛快快 全然不理会谁给的评... | length --3500 | - | 歌曲歌词，使用换行分隔每行。支持结构标签：[Intro], [Verse], [Chorus], [Bridge], [Outro]等,非纯音乐（isInstrumental: false）时必填 |
| sampleRate | LIST | 否 | 44100 | - | 16000, 24000, 32000, 44100 | 采样率 |
| bitrate | LIST | 否 | 256000 | - | 32000, 64000, 128000, 256000 | 比特率 |
| format | LIST | 否 | mp3 | - | mp3, wav, pcm | 音频格式 |
| lyricsOptimizer | BOOLEAN | 否 | false | - | - | 是否根据描述自动优化歌词 |
| isInstrumental | BOOLEAN | 否 | false | - | - | 是否纯音乐 |

### Mureka-v9 伴奏生成

- ID：`2052742537170706434`
- Endpoint：`/openapi/v2/mureka-ai/mureka-v9/generate-bgm`
- 类型/分组/来源：audio-to-audio / Mureka Al Models / rh-ai
- 价格：0.28 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 纯音乐，现代流行伤感风，BPM 75，Am 小调，钢琴分解和弦 + 大提琴 + 弦乐铺底 + 轻微鼓点，氛围感拉满、孤独深情，编曲完整，无人声无哼唱，高音质立... | length --1024 | - | 伴奏风格提示词，最长 1024 字符；与其它控制项互斥。 |
| n | INT | 是 | 2 | range 1-3; step 1; priceRelated | - | 每次生成的伴奏数量（默认 2，最大 3，按生成数量计费）。 |
| instrumentalId | STRING | 否 | - | length --64 | - | 参考伴奏文件 ID，由 /mureka/files-upload (purpose=instrumental) 返回；与 prompt 互斥。 |
| stream | BOOLEAN | 否 | false | - | - | 是否启用流式生成（成功后会先进入 streaming 阶段，可读取 stream_url 边播边出）。 |

### Mureka 歌词生成

- ID：`2052634166820159501`
- Endpoint：`/openapi/v2/mureka-ai/generate-lyrics`
- 类型/分组/来源：text-to-lyrics / Mureka Al Models / rh-ai
- 价格：0.06 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 按以下规则创作原创歌词： 标准结构：主歌 1、预副歌、副歌、主歌 2、预副歌、副歌、桥段、结尾。 押韵自然，有画面感，情感细腻，句式长短适合演唱。 曲风：（填... | length --1024; priceRelated | - | 歌词生成的提示词（主题、关键词或描述）。 |

### Mureka 人声克隆

- ID：`2052634166820159500`
- Endpoint：`/openapi/v2/mureka-ai/vocal-clone`
- 类型/分组/来源：audio-to-audio / Mureka Al Models / rh-ai
- 价格：31.5 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| fileUrl | AUDIO | 是 | <示例素材已脱敏> | maxSize 10MB; priceRelated; accept ["mp3","m4a"] | - | 待克隆的人声音频 URL（mp3 / m4a，≤ 10 MB）。同步返回 vocalId，可作为 song/generate 的 vocalId 使用。 |

### Mureka-v8 短歌延长

- ID：`2052634166820159499`
- Endpoint：`/openapi/v2/mureka-ai/mureka-v8/extend-song`
- 类型/分组/来源：audio-to-audio / Mureka Al Models / rh-ai
- 价格：0.63 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| lyrics | STRING | 是 | 我的心在跳 每次想起你 这酸涩滋味 真实又甜蜜 啦啦啦啦 想见你 | length --3000 | - | 延长部分的歌词，最长 3000 字符。 |
| fileUrl | AUDIO | 是 | - | maxSize 10MB; accept ["MP3","M4A"] | - | 待上传的音频文件 URL，用于歌曲延长。 |
| extendAt | INT | 是 | 8000 | range 8000-420000; step 1 | - | 延长起始时间（毫秒），有效区间 [8000, 420000]；超出歌曲时长则使用歌曲末尾。 |
| extendType | LIST | 否 | tail | - | tail, head | 延长方向：tail 从尾部延长，head 从头部延长（仅 mureka-8 模型支持）。 |

### mureka-v7.6 短歌延长

- ID：`2052634166820159498`
- Endpoint：`/openapi/v2/mureka-ai/mureka-v7.6/extend-song`
- 类型/分组/来源：audio-to-audio / Mureka Al Models / rh-ai
- 价格：0.245 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| lyrics | STRING | 是 | 晚风轻拂渡口 月色漫染眉头 往事轻放身后 余生不惊不忧 花开不问缘由 流年静静相守 人间烟火依旧 安然渡尽春秋 | length --3000 | - | 延长部分的歌词，最长 3000 字符。 |
| extendAt | INT | 是 | 8000 | range 8000-420000; step 1 | - | 延长起始时间（毫秒），有效区间 [8000, 420000]；超出歌曲时长则使用歌曲末尾。 |
| fileUrl | AUDIO | 是 | - | maxSize 10MB; accept ["MP3","M4A"] | - | 待上传的音频文件 URL，用于歌曲延长。 |

### Mureka-v8 伴奏生成

- ID：`2052634166820159497`
- Endpoint：`/openapi/v2/mureka-ai/mureka-v8/generate-bgm`
- 类型/分组/来源：text-to-audio / Mureka Al Models / rh-ai
- 价格：0.28 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 纯音乐，乐队音乐伴奏，激昂轻摇滚，对生命的感叹，鼓点+键盘+贝斯+电吉他，编曲完整，无人声无哼唱，高音质立体声，干净消音伴奏 | length --1024 | - | 伴奏风格提示词，最长 1024 字符；与其它控制项互斥。 |
| n | INT | 是 | 2 | range 1-3; step 1; priceRelated | - | 每次生成的伴奏数量（默认 2，最大 3，按生成数量计费）。 |
| instrumentalId | STRING | 否 | - | length --64 | - | 参考伴奏文件 ID，由 /mureka/files-upload (purpose=instrumental) 返回；与 prompt 互斥。 |
| stream | BOOLEAN | 否 | false | - | - | 是否启用流式生成（成功后会先进入 streaming 阶段，可读取 stream_url 边播边出）。 |

### Mureka-v7.6 伴奏生成

- ID：`2052634166820159495`
- Endpoint：`/openapi/v2/mureka-ai/mureka-v7.6/generate-bgm`
- 类型/分组/来源：text-to-audio / Mureka Al Models / rh-ai
- 价格：0.19 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 纯音乐，现代流行，钢琴分解和弦 + 大提琴 + 弦乐铺底 + 轻微鼓点，氛围感拉满、诉说暗恋的青春情愫，，编曲完整，无人声无哼唱，高音质立体声，干净消音伴奏 | length --1024 | - | 伴奏风格提示词，最长 1024 字符；与其它控制项互斥。 |
| instrumentalId | STRING | 否 | - | length --64 | - | 参考伴奏文件 ID，由 /mureka/files-upload (purpose=instrumental) 返回；与 prompt 互斥。 |
| n | INT | 是 | 2 | range 1-3; step 1; priceRelated | - | 每次生成的伴奏数量（默认 2，最大 3，按生成数量计费）。 |
| stream | BOOLEAN | 否 | false | - | - | 是否启用流式生成（成功后会先进入 streaming 阶段，可读取 stream_url 边播边出）。 |

### Mureka-v9 歌曲生成

- ID：`2052634166820159494`
- Endpoint：`/openapi/v2/mureka-ai/mureka-v9/generate-song`
- 类型/分组/来源：text-to-music / Mureka Al Models / rh-ai
- 价格：0.28 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| lyrics | STRING | 是 | [主歌] 你说要离开这熟悉的街 留下一串串欢笑和泪水 我们曾一起走过黄昏和黎明 现在只剩风声和月光 [副歌] 但请不要忧伤 因为离别后是新的希望 就像彩虹在风... | length --3000 | - | 歌曲歌词，最长 3000 字符。可使用 [Verse]/[Chorus] 等结构标记。 |
| prompt | STRING | 否 | 舒缓温柔旋律，简约编曲，温暖氛围感，清澈干净曲风 | length --1024 | - | 音乐风格提示词，最长 1024 字符；可与 vocalId 组合使用。 |
| n | INT | 是 | 2 | range 1-3; step 1; priceRelated | - | 每次生成的歌曲数量（默认 2，最大 3，按生成数量计费）。 |
| referenceId | STRING | 否 | - | length --64 | - | 参考音乐文件 ID，由 /mureka/files-upload (purpose=reference) 返回；可与 vocalId 组合。 |
| vocalId | STRING | 否 | - | length --64 | - | 音色文件 ID，可由 /mureka/files-upload (purpose=vocal) 或 /mureka/vocal-clone 返回；可与 prompt 或 referenceId 组合。 |
| melodyId | STRING | 否 | - | length --64 | - | 旋律文件 ID（mp3/m4a/mid，推荐 MIDI），由 /mureka/files-upload (purpose=melody) 返回；与其它控制项互斥。 |
| stream | BOOLEAN | 否 | false | - | - | 是否启用流式生成（成功后会先进入 streaming 阶段，可读取 stream_url 边播边出）；mureka-o1 不支持。 |

### Mureka-v8 歌曲生成

- ID：`2052634166820159493`
- Endpoint：`/openapi/v2/mureka-ai/mureka-v8/generate-song`
- 类型/分组/来源：text-to-music / Mureka Al Models / rh-ai
- 价格：0.28 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| lyrics | STRING | 是 | [前奏] [副歌] 你在我耳边低语 情话甜如蜜 却躲着我眼神 不愿直视我 我的心在跳 每次想起你 这酸涩滋味 真实又甜蜜 [主歌] 夜晚的城市霓虹闪烁 灯光下... | length --3000 | - | 歌曲歌词，最长 3000 字符。可使用 [Verse]/[Chorus] 等结构标记。 |
| prompt | STRING | 否 | 吉他主旋律，清澈干净曲风，舒缓温柔旋律，简约编曲 | length --1024 | - | 音乐风格提示词，最长 1024 字符；可与 vocalId 组合使用。 |
| n | INT | 是 | 2 | range 1-3; step 1; priceRelated | - | 每次生成的歌曲数量（默认 2，最大 3，按生成数量计费）。 |
| referenceId | STRING | 否 | - | length --64 | - | 参考音乐文件 ID，由 /mureka/files-upload (purpose=reference) 返回；可与 vocalId 组合。 |
| vocalId | STRING | 否 | - | length --64 | - | 音色文件 ID，可由 /mureka/files-upload (purpose=vocal) 或 /mureka/vocal-clone 返回；可与 prompt 或 referenceId 组合。 |
| melodyId | STRING | 否 | - | length --64 | - | 旋律文件 ID（mp3/m4a/mid，推荐 MIDI），由 /mureka/files-upload (purpose=melody) 返回；与其它控制项互斥。 |
| stream | BOOLEAN | 否 | false | - | - | 是否启用流式生成（成功后会先进入 streaming 阶段，可读取 stream_url 边播边出）；mureka-o1 不支持。 |

### Mureka-o2 歌曲生成

- ID：`2052634166820159492`
- Endpoint：`/openapi/v2/mureka-ai/mureka-o2/generate-song`
- 类型/分组/来源：text-to-music / Mureka Al Models / rh-ai
- 价格：0.28 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| lyrics | STRING | 是 | 【Verse 1】 站台的钟声敲响了告别 行李箱拖着昨夜未说的感谢 梧桐叶飘落在空荡的街 我看着车窗里倒映的自己 学着重叠 【Pre-Chorus】 不是所有... | length --3000 | - | 歌曲歌词，最长 3000 字符。可使用 [Verse]/[Chorus] 等结构标记。 |
| prompt | STRING | 否 | 舒缓温柔旋律，温暖氛围感，清澈干净曲风 | length --1024 | - | 音乐风格提示词，最长 1024 字符；可与 vocalId 组合使用。 |
| n | INT | 是 | 2 | range 1-3; step 1; priceRelated | - | 每次生成的歌曲数量（默认 2，最大 3，按生成数量计费）。 |
| referenceId | STRING | 否 | - | length --64 | - | 参考音乐文件 ID，由 /mureka/files-upload (purpose=reference) 返回；可与 vocalId 组合。 |

### Mureka-v7.6 歌曲生成

- ID：`2052634166820159491`
- Endpoint：`/openapi/v2/mureka-ai/mureka-v7.6/generate-song`
- 类型/分组/来源：text-to-music / Mureka Al Models / rh-ai
- 价格：0.19 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| lyrics | STRING | 是 | 晨光漫过窗台 微风捎来温柔 烟火人间走走 心事慢慢罢休 云卷云舒自流 岁月安静温柔 山河皆有白首 平凡亦是自由 | length --3000 | - | 歌曲歌词，最长 3000 字符。可使用 [Verse]/[Chorus] 等结构标记。 |
| prompt | STRING | 否 | 吉他主旋律，清澈干净曲风 | length --1024 | - | 音乐风格提示词，最长 1024 字符；可与 vocalId 组合使用。 |
| n | INT | 是 | 2 | range 1-3; step 1; priceRelated | - | 每次生成的歌曲数量（默认 2，最大 3，按生成数量计费）。 |
| referenceId | STRING | 否 | - | length --64 | - | 参考音乐文件 ID，由 /mureka/files-upload (purpose=reference) 返回；可与 vocalId 组合。 |
| vocalId | STRING | 否 | - | length --64 | - | 音色文件 ID，可由 /mureka/files-upload (purpose=vocal) 或 /mureka/vocal-clone 返回；可与 prompt 或 referenceId 组合。 |
| melodyId | STRING | 否 | - | length --64 | - | 旋律文件 ID（mp3/m4a/mid，推荐 MIDI），由 /mureka/files-upload (purpose=melody) 返回；与其它控制项互斥。 |
| stream | BOOLEAN | 否 | false | - | - | 是否启用流式生成（成功后会先进入 streaming 阶段，可读取 stream_url 边播边出）；mureka-o1 不支持。 |

### 悠船文生图-v8.1

- ID：`2052634166820159490`
- Endpoint：`/openapi/v2/youchuan/text-to-image-v81`
- 类型/分组/来源：text-to-image / 悠船 AI 绘图 / rh-ai
- 价格：0.54 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 原木桌面轻奢咖啡器具组合，陶瓷咖啡杯配银质小勺，袅袅热气，窗边柔和漫射自然光，浅景深虚化，实木与陶瓷细腻材质纹理，暖调极简静物摄影，高清微距细节 | length 1-8192 | - | prompt |
| chaos | INT | 否 | 0 | range 0-100; step 1 | - | 混沌参数，为图像结果增添趣味 (0-100) |
| quality | LIST | 否 | 1 | - | 1, 4 | 图像质量 |
| stylize | INT | 否 | 0 | range 0-1000; step 1 | - | 风格化参数，控制图像的艺术风格强度（0-1000） |
| raw | BOOLEAN | 否 | false | - | - | 原始模式，获得对图像的更多控制 |
| imageUrl | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 输入图片URL（垫图） |
| iw | INT | 否 | 1 | range 0-3; step 1 | - | 图像权重，控制图像提示的影响 |
| sref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 风格参考图片URL |
| sw | INT | 否 | 100 | range 0-1000; step 1 | - | 风格权重，控制风格参考的影响（0-1000，需搭配sref使用） |
| sv | INT | 否 | 6 | range 6-6; step 1 | - | 风格版本 |
| aspectRatio | LIST | 否 | - | - | 1:1, 4:3, 3:2, 16:9, 3:4, 2:3, 9:16 | aspectRatio |
| hd | BOOLEAN | 是 | false | priceRelated | - | 是否开启原生 2K |

### Mureka 文件上传

- ID：`2052288678866534404`
- Endpoint：`/openapi/v2/mureka-ai/files-upload`
- 类型/分组/来源：upload-file / Mureka Al Models / rh-ai
- 价格：0 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| fileUrl | AUDIO | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["mp3","m4a","mid"] | - | 待上传的音频文件 URL；后缀必须与 purpose 匹配：reference / vocal / instrumental / voice / audio / remix 仅支持 mp3 / m4a；melody 额外支持 mid（推荐... |
| purpose | LIST | 是 | reference | - | reference, vocal, melody, instrumental, voice, audio, remix | 用途。各值对应不同的支持格式与时长裁剪：reference/instrumental(30s 截取)、vocal(15-30s)、melody(5-60s, 支持 mid)、voice(5-15s)、audio/remix(普通音频)。 |

### 可灵 o3 创建主体

- ID：`2052288678866534402`
- Endpoint：`/openapi/v2/kling-elements-advanced`
- 类型/分组/来源：video-tools / 可灵 3.0 / rh-ai
- 价格：0 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| elementName | STRING | 是 | - | length 1-20 | - | 主体名称。不能超过20个字符 |
| elementDescription | STRING | 是 | - | length 1-100 | - | elementDescription |
| referenceType | LIST | 是 | image_refer | - | video_refer, image_refer | 主体参考方式，video_refer: 视频角色主体，此时将参考element_video_list定义主体外表 ；image_refer: 多图主体，此时将参考element_image_list定义主体外表 |
| elementImageList | COMPLEX | 否 | - | - | - | elementImageList |
| elementVideoList | VIDEO | 否 | - | maxSize 200MB; accept ["MP4","MOV"] | - | 仅支持时长介于3s～8s之间、宽高比例需为16:9或9:16的1080P视频 |
| tagList | LIST[] | 否 | - | - | o_101, o_102, o_103, o_104, o_105, o_106, o_107, o_108 | tagList |

### SkyReels V4 Omni 参考视频-fast

- ID：`2052238943728254977`
- Endpoint：`/openapi/v2/skyreels-v4/omni-reference-fast`
- 类型/分组/来源：reference-to-video / SkyReels / rh-ai
- 价格：0.1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 视频续写 @video_1, 两个男生向前奔跑，回眸 | length 1-2500 | - | 视频描述提示词，支持 @tag 引用参考图片/视频 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1, 4:3, 3:4 | 视频画面比例。如果提供了参考视频（ref_videos），此参数将被忽略，输出尺寸会自动与参考视频保持一致。 |
| duration | INT | 否 | 5 | range 3-15; step 1; priceRelated | - | 视频时长（秒）。如果提供了 ref_videos（参考视频），此参数将被覆盖；这种情况下，输出视频的时长将与参考视频一致（最长不超过 10 秒）。 |
| promptOptimizer | BOOLEAN | 否 | true | - | - | 是否启用提示词优化 |
| refImages | COMPLEX[] | 否 | - | multipleInputs | - | 参考图片配置。所有项必须为同一 type；grid 类型时列表长度必须为 1，image 类型时最多 3 组。视频续写任务（ref_videos.type=extend）不可与 ref_images 同用。refImages和refVid... |
| refVideos | COMPLEX | 否 | - | - | - | 参考视频配置。最多支持 1 个视频引用（最长 15 秒）。reference 类型时输出时长会与参考视频一致（≤10 秒）。refImages和refVideos必须至少提供一个参考。 |
| resolution | LIST | 是 | 1080p | priceRelated | 480p, 720p, 1080p | 输出视频分辨率。支持480p、720p和1080p。 |

### SkyReels V4 图生视频-fast

- ID：`2051994196535197697`
- Endpoint：`/openapi/v2/skyreels-v4/image-to-video-fast`
- 类型/分组/来源：image-to-video / SkyReels / rh-ai
- 价格：0.28 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 春日郊外的野餐场景，五位年轻朋友坐在开满野花的草地上，围着彩色格子野餐垫，欢声笑语互动：举杯、分享食物、指向远方、相视而笑。背景是开满樱花的山谷与蜿蜒河流，阳... | length 1-2048 | - | 描述视频内容的文本提示词 |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","GIF","BMP"] | - | 视频首帧图片 |
| duration | INT | 否 | 5 | range 3-15; step 1; priceRelated | - | 视频时长（秒） |
| promptOptimizer | BOOLEAN | 是 | true | - | - | 启用自动提示词扩展和优化，以实现更高的视觉保真度和对齐效果。 |
| resolution | LIST | 是 | 1080p | priceRelated | 480p, 720p, 1080p | 输出视频分辨率。支持480p、720p和1080p。 |

### SkyReels V4 文生视频-fast

- ID：`2051992463616557057`
- Endpoint：`/openapi/v2/skyreels-v4/text-to-video-fast`
- 类型/分组/来源：text-to-video / SkyReels / rh-ai
- 价格：0.28 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 微观量子世界，电子云高速旋转、量子纠缠粒子成对闪烁，叠加态粒子同时存在多个位置，量子隧穿效果可视化，深蓝色与紫色霓虹光效，粒子流穿梭、能量场波纹扩散，缓慢环绕... | length 1-2048 | - | 描述视频内容的文本提示词 |
| duration | INT | 否 | 5 | range 3-15; step 1; priceRelated | - | 视频时长（秒） |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1, 4:3, 3:4 | 视频画面比例 |
| promptOptimizer | BOOLEAN | 是 | true | - | - | 启用自动提示词扩展和优化，以实现更高的视觉保真度和对齐效果。 |
| resolution | LIST | 是 | 1080p | priceRelated | 480p, 720p, 1080p | 输出视频分辨率。支持480p、720p和1080p。 |

### 可灵文生视频2.5-turbo-std

- ID：`2049745693465419778`
- Endpoint：`/openapi/v2/kling-v2.5-turbo-std/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 / rh-ai
- 价格：1.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一个女孩在教室里跳舞 | length 1-2000 | - | prompt |
| negativePrompt | STRING | 否 | - | length --2500 | - | negativePrompt |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| guidanceScale | FLOAT | 否 | 0.5 | range 0-1; step 0.1; precision 1 | - | guidanceScale |
| aspectRatio | LIST | 是 | 9:16 | - | 1:1, 16:9, 9:16 | aspectRatio |

### happyhorse-1.0/video-edit

- ID：`2048623940504719364`
- Endpoint：`/openapi/v2/alibaba/happyhorse-1.0/video-edit`
- 类型/分组/来源：video-edit / HappyHorse Models / rh-ai
- 价格：0.67 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; priceRelated; accept ["MP4","MOV"] | - | 输入视频 URL（3–60s，>15s 将被截断为前 15s；建议 H.264） |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG","BMP","WEBP"] | - | 可选参考图（可多张，映射为 reference_image） |
| prompt | STRING | 是 | 将图片中的元素添加到视频中 | length 1-2500 | - | 编辑或生成指令 |
| resolution | LIST | 是 | 1080p | priceRelated | 720p, 1080p | 输出分辨率 |
| audioSetting | LIST | 否 | origin | - | auto, origin | 声音策略：自动或保留原声 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机种子 |

### happyhorse-1.0/reference-to-video

- ID：`2048623940504719363`
- Endpoint：`/openapi/v2/alibaba/happyhorse-1.0/reference-to-video`
- 类型/分组/来源：reference-to-video / HappyHorse Models / rh-ai
- 价格：0.67 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 融合两张图片的内容：以第一张图的简约光影家居为背景，让第二张图的古装女性在场景中做出轻微的自然动作，生成一段过渡自然、风格统一的短视频。整体保持柔和自然光，画... | length 1-2500 | - | 文本提示词；可用「图1/图2」等指代多张参考图的语义关系 |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG","BMP","WEBP"] | - | 至少一张参考图 URL；建议短边≥400px、比例一致且接近目标视频比例 |
| resolution | LIST | 是 | 1080p | priceRelated | 720p, 1080p | 输出分辨率 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 3:4, 4:3, 1:1 | 视频宽高比 |
| duration | LIST | 是 | 5 | priceRelated | 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机种子，不填则随机 |

### 分离音频

- ID：`2048623940504719362`
- Endpoint：`/openapi/v2/rhart-audio/source-separation`
- 类型/分组/来源：video-to-audio / 自部署开源模型 / rh-ai
- 价格：0.1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 3##file | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; accept ["MP4","MOV"] | - | videoUrl |

### 可灵参考生视频o3-4k

- ID：`2047563851324841990`
- Endpoint：`/openapi/v2/kling-video-o3-4k/reference-to-video`
- 类型/分组/来源：reference-to-video / 可灵 3.0 / rh-ai
- 价格：2.7 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 暖色厨房，一位老人用手将面团轻轻揉圆，指尖沾着面粉。阳光从窗户斜射，面粉微粒在光束中浮动。面团落在案板上一声轻响。 | length 1-2500 | - | 视频画面、人物、动作的文字描述 |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 30MB; multipleInputs; accept ["JPG","JPEG","PNG","WEBP"] | - | 参考图片（无参考视频时最多 7 张，有参考视频时最多 4 张） |
| keepOriginalSound | BOOLEAN | 否 | true | - | - | 是否保留参考视频的原始音频 |
| sound | BOOLEAN | 否 | false | - | - | 是否生成 AI 音频（仅在无参考视频时生效） |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1 | 视频宽高比 |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | 视频时长（秒，3-15） |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式,当multi_shot参数为true时，当前参数必填 |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | 多分镜 prompt 列表（可选） |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | 元素引用列表（可选） |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频（为true时，prompt参数无效，为false时，shotType参数及multiPrompt参数无效） |

### 可灵图生视频o3-4k

- ID：`2047563851324841989`
- Endpoint：`/openapi/v2/kling-video-o3-4k/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 3.0 / rh-ai
- 价格：2.7 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 深海热泉喷口附近，一只半透明的发光水母缓缓游动，周围漂浮着雪花般的微生物碎屑。它的触手发出微弱的蓝绿色生物光，偶尔闪烁。镜头缓慢推进，远处可见黑色烟囱喷出矿物... | length 1-2500 | - | 动作、镜头、光影、氛围的文字描述 |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 起始参考图片 |
| lastImageUrl | IMAGE | 否 | - | maxSize 30MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 结束帧参考图片（可选） |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | 视频时长（秒，3-15） |
| sound | BOOLEAN | 否 | false | - | - | 是否同时生成同步音频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | 多分镜 prompt 列表（可选） |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | 元素引用列表（可选） |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频（为true时，prompt参数无效，为false时，shotType参数及multiPrompt参数无效） |

### 可灵文生视频o3-4k

- ID：`2047563851324841988`
- Endpoint：`/openapi/v2/kling-video-o3-4k/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 3.0 / rh-ai
- 价格：2.7 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 午后办公室，一位疲惫的上班族拿起一杯热气腾腾的现磨咖啡。他用鼻子深吸香气，眼神从疲惫转为明亮。时间仿佛暂停，镜头围绕咖啡杯旋转，背景从凌乱工位变成清晨的咖啡种... | length 1-2500 | - | 视频画面、动作、镜头、氛围的文字描述 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1 | 视频宽高比 |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | 视频时长（秒，3-15） |
| sound | BOOLEAN | 否 | false | - | - | 是否同时生成同步音频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式，当 multiShot 参数为true时，当前参数必填 |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | 多分镜 prompt 列表（可选） |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | 元素引用列表（可选） |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频（为true时，prompt参数无效，为false时，shotType参数及multiPrompt参数无效） |

### 可灵文生视频v3-4k

- ID：`2047563851324841987`
- Endpoint：`/openapi/v2/kling-v3-4k/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 3.0 / rh-ai
- 价格：2.7 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 深夜末班地铁，一个穿西装的男人靠窗坐着，领带松散，手背撑着额头。列车灯光忽明忽暗地掠过他的脸，他闭着眼，睫毛微颤。 | length 1-2500 | - | 视频画面、动作、镜头、氛围的文字描述 |
| negativePrompt | STRING | 否 | - | length 0-2500 | - | 需要排除的元素 |
| duration | LIST | 是 | 5 | priceRelated | 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒，3-15） |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1 | 视频宽高比 |
| cfgScale | FLOAT | 否 | 0.5 | range 0-1; step 0.1; precision 2 | - | 提示词引导强度，0-1 |
| sound | BOOLEAN | 否 | false | - | - | 是否同时生成同步音频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | 多分镜 prompt 列表（可选） |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | 元素引用列表（可选） |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频（为true时，prompt参数无效，为false时，shotType参数及multiPrompt参数无效） |

### 可灵图生视频v3-4k

- ID：`2047563851324841986`
- Endpoint：`/openapi/v2/kling-v3-4k/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 3.0 / rh-ai
- 价格：2.7 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=1000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG","JPEG"] | - | 支持的图像格式：.jpg/.jpeg/.png。图像文件的大小不应超过10MB，图像的宽度和高度不应小于300px，图像的纵横比应在1:2.5和2.5:1之间。 |
| prompt | STRING | 否 | 男士侧身漏出侧脸，微微仰头看天，雨水慢慢滴落，地上的水中霓虹闪烁 | length 1-2500 | - | 视频生成的文本提示，必须提供prompt或multi_prompt，但不能同时提供。 |
| negativePrompt | STRING | 否 | - | - | - | negativePrompt |
| endImageUrl | IMAGE | 否 | - | maxSize 30MB; accept ["JPG","PNG"] | - | endImageUrl |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | duration |
| cfgScale | FLOAT | 否 | 0.5 | range 0-1; step 0.01; precision 2 | - | cfgScale |
| sound | BOOLEAN | 否 | false | - | - | sound |
| shotType | LIST | 否 | customize | - | customize, intelligence | shotType |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | 多镜头视频生成提示列表。如果提供，将视频分成多个镜头。 |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | elementList |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频（为true时，prompt参数无效，为false时，shotType参数及multiPrompt参数无效） |

### 全能图片X-文生图片-官方稳定版

- ID：`2046514150500524053`
- Endpoint：`/openapi/v2/rhart-image-x-official/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片X / rh-ai
- 价格：0.14 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 手持纵向细长模切书签，书签内以精致立体纸雕呈现地点地标，部分元素突破边缘。背景为同一地点的电影感实景虚化，光影匹配时段。8K画质，手部与书签清晰对焦。 | length 5-20000 | - | prompt |
| aspectRatio | LIST | 否 | 1:1 | - | 2:1, 20:9, 16:9, 4:3, 3:2, 1:1, 2:3, 3:4, 9:16, 9:20, 1:2 | aspectRatio |
| outputFormat | LIST | 是 | jpeg | - | jpeg, png | outputFormat |

### 全能图片X-图片编辑-官方稳定版

- ID：`2046514150500524052`
- Endpoint：`/openapi/v2/rhart-image-x-official/edit`
- 类型/分组/来源：image-to-image / 全能图片X / rh-ai
- 价格：0.14 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 把图中女孩的衣服换成夏季的连衣裙 | length 5-20000 | - | prompt |
| image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG","JPEG","WEBP"] | - | image |

### happyhorse-1.0/image-to-video

- ID：`2046514150500524037`
- Endpoint：`/openapi/v2/alibaba/happyhorse-1.0/image-to-video`
- 类型/分组/来源：image-to-video / HappyHorse Models / rh-ai
- 价格：0.67 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","BMP","WEBP"] | - | 输入首帧图片（≥300px，宽高比 1:2.5~2.5:1） |
| prompt | STRING | 否 | 大远景开篇，镜头从接近地面的低机位开始向前移动，镜头沿草原方向推进，同时向上移动，将视角从贴地逐渐抬升至略高位置，使猎豹从左侧进入画面并与前方逃窜的羚羊共同处... | length --2500 | - | 文本提示词，可选，用于引导画面动态 |
| resolution | LIST | 是 | 1080p | priceRelated | 720p, 1080p | 视频分辨率 |
| duration | LIST | 是 | 5 | priceRelated | 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机种子（不填则随机生成） |

### happyhorse-1.0/text-to-video

- ID：`2046514150500524036`
- Endpoint：`/openapi/v2/alibaba/happyhorse-1.0/text-to-video`
- 类型/分组/来源：text-to-video / HappyHorse Models / rh-ai
- 价格：0.67 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 宽镜头。一名穿着尘土飞扬的橙色压力服、深蓝灰色背带和黑色靴子的孤独宇航员，在广阔的月球平原上滑雪，留下两条长长的平行轨迹在灰色的风化层中。宇航员正跨步前行，滑... | length --2500 | - | 文本提示词，描述画面、情绪与镜头 |
| resolution | LIST | 是 | 1080p | priceRelated | 720p, 1080p | 视频分辨率 |
| duration | LIST | 是 | 5 | priceRelated | 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1, 4:3, 3:4 | 视频宽高比 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机种子（不填则随机生成） |

### 全能图片G-2-图生图-官方稳定版

- ID：`2046514150500524035`
- Endpoint：`/openapi/v2/rhart-image-g-2-official/image-to-image`
- 类型/分组/来源：image-to-image / 全能图片G / rh-ai
- 价格：0.19 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=1000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 将这个客厅彻底改造为“植物园温室”风格。把原有的沙发替换成复古的绿色天鹅绒材质，墙面变成做旧的红砖墙。整个房间内挂满茂盛的热带藤蔓植物，阳光透过巨大的玻璃穹顶... | length 1-20000 | - | 编辑指令描述 |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG","WEBP"] | - | 参考图片（1-10张） |
| aspectRatio | LIST | 否 | 16:9 | - | 1:1, 1:2, 2:1, 1:3, 3:1, 2:3, 3:2, 3:4, 4:3, 4:5, 5:4, 9:16, 21:9, 9:21, 16:9 | 输出图像宽高比 |
| resolution | LIST | 是 | 2k | priceRelated | 1k, 2k, 4k | 输出图像分辨率档位 |
| quality | LIST | 是 | medium | priceRelated | low, medium, high | quality |

### 全能图片G-2-文生图-官方稳定版

- ID：`2046514150500524034`
- Endpoint：`/openapi/v2/rhart-image-g-2-official/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片G / rh-ai
- 价格：0.06 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=1000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一张高端商业摄影海报。画面正中央是一个采用极简设计的白色磨砂质感智能音箱。音箱放置在浅灰色的水磨石台面上。背景是纯净的低饱和度米色墙面，一束柔和的自然光从斜上... | length 1-20000 | - | 图像描述提示词 |
| aspectRatio | LIST | 否 | 16:9 | - | 1:1, 1:2, 2:1, 1:3, 3:1, 2:3, 3:2, 3:4, 4:3, 4:5, 5:4, 9:16, 21:9, 9:21, 16:9 | 输出图像宽高比 |
| resolution | LIST | 是 | 2k | priceRelated | 1k, 2k, 4k | 输出图像分辨率档位 |
| quality | LIST | 是 | medium | priceRelated | low, medium, high | quality |

### 全能图片G-2.0-文生图-低价渠道版

- ID：`2046514150500524033`
- Endpoint：`/openapi/v2/rhart-image-g-2/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片G / openai
- 价格：0.1 CNY/次
- 队列/并发：queueSize=3000，concurrencyLimit=3000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 生成一张充满未来感的咖啡馆宣传海报。画面中央是一个发光的霓虹灯招牌，上面清晰且准确地拼写着英文单词 "CyberBrew"。背景是带有极简主义和现代高级感的城... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 否 | 16:9 | - | 1:1, 3:2, 2:3, 5:4, 4:5, 16:9, 9:16, 21:9, 3:4, 4:3, 9:21, 21:9, 1:2, 2:1, 1:3, 3:1 | aspectRatio |
| resolution | LIST | 否 | 1k | - | 1k, 2k, 4k | 可选择 1k/2k/4k，因接口稳定性限制，暂不保证精准输出 2k/4k 分辨率，多数情况下仍会输出 1k 图像，介意请使用官方接口。 |

### 全能图片G-2.0-图生图-低价渠道版

- ID：`2046503667076751361`
- Endpoint：`/openapi/v2/rhart-image-g-2/image-to-image`
- 类型/分组/来源：image-to-image / 全能图片G / openai
- 价格：0.1 CNY/次
- 队列/并发：queueSize=4000，concurrencyLimit=4000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 在马克杯的正中央，添加一个精致的几何风格狐狸 Logo，Logo 下方清晰地印着文字 "Wild Fox"。请保持原图的光影结构和陶瓷质感完全不变。 然后生成... | length 1-20000 | - | prompt |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 30MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| aspectRatio | LIST | 否 | 16:9 | - | 1:1, 2:3, 3:2, 4:5, 5:4, 4:3, 3:4, 16:9, 9:16, 21:9, 9:21, 2:1, 1:2, 3:1, 1:3 | aspectRatio |
| resolution | LIST | 否 | 1k | - | 1k, 2k, 4k | 可选择 1k/2k/4k，因接口稳定性限制，暂不保证精准输出 2k/4k 分辨率，多数情况下仍会输出 1k 图像，介意请使用官方接口。 |

### 全能视频X-视频续写-官方稳定版

- ID：`2043991611295436803`
- Endpoint：`/openapi/v2/rhart-video-g-official/video-extend`
- 类型/分组/来源：video-extend / 全能视频X / rh-ai
- 价格：1.89 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; accept ["MP4","MOV"] | - | 待续写的输入视频 |
| prompt | STRING | 是 | 父亲正在给他的女儿讲故事。 | length --2048 | - | 描述视频应如何继续发展 |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | 续写时长（秒） |

### 全能视频X-多图参考生视频-官方稳定版

- ID：`2043991611295436802`
- Endpoint：`/openapi/v2/rhart-video-g-official/reference-to-video`
- 类型/分组/来源：reference-to-video / 全能视频X / rh-ai
- 价格：1.89 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG","WEBP"] | - | 参考图片（1-7张） |
| prompt | STRING | 是 | 让图1中的女生抱着图2中的猫健身，比例16：9 | length --2048 | - | 描述期望的视频动作、运镜和场景变化 |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | 生成视频时长（秒） |
| resolution | LIST | 是 | 720p | - | 720p, 480p | 输出分辨率 |

### 全能视频V3.1-Lite首尾帧生视频-官方稳定版

- ID：`2042415417236176904`
- Endpoint：`/openapi/v2/rhart-video-v3.1-lite-official/start-end-to-video`
- 类型/分组/来源：text-to-video / 全能视频V / rh-ai
- 价格：2.52 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 山野的一年四季变迁，自然过渡 | length 1-20000 | - | 文本提示词，描述期望的过渡效果和场景变化 |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 起始帧图片 |
| lastImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 结束帧图片 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16 | 输出画幅比例 |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p | 输出分辨率 |
| negativePrompt | STRING | 否 | - | length 0-20000 | - | 反向提示词，指定不希望出现的元素 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机数种子，用于复现结果 |

### 全能视频V3.1-Lite图生视频-官方稳定版

- ID：`2042415417236176903`
- Endpoint：`/openapi/v2/rhart-video-v3.1-lite-official/image-to-video`
- 类型/分组/来源：image-to-video / 全能视频V / rh-ai
- 价格：0.32 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 四个朋友开着一辆旧面包车穿越沙漠，窗户开着，头发迎风飞舞。温暖的尘土色调，公路旅行音乐氛围，镜头耀斑，自由的感觉。 | length 1-20000 | - | 文本提示词，描述期望的动画效果和场景氛围 |
| negativePrompt | STRING | 否 | - | length 0-20000 | - | 反向提示词，指定不希望出现的元素 |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 输入图片 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16 | 输出画幅比例 |
| duration | LIST | 是 | 6 | priceRelated | 4, 6, 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p | 输出分辨率 |
| seed | INT | 否 | - | range -1-2147483647; step 1 | - | 随机数种子，用于复现结果 |

### 全能视频V3.1-Lite文生视频-官方稳定版

- ID：`2042415417236176902`
- Endpoint：`/openapi/v2/rhart-video-v3.1-lite-official/text-to-video`
- 类型/分组/来源：text-to-video / 全能视频V / rh-ai
- 价格：0.32 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一位风霜满面的老渔夫在黎明时分坐在被雾气笼罩的码头边，用长满老茧的双手修补破损的渔网。他的脸上带着数十年的沉默。镜头缓缓环绕他。色调去饱和，下面是轻轻拍打的波... | length 1-20000 | - | 文本提示词，描述场景、运动、镜头风格和氛围 |
| negativePrompt | STRING | 否 | - | length 0-20000 | - | 反向提示词，指定不希望出现的元素 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16 | 输出画幅比例 |
| resolution | LIST | 是 | 1080p | priceRelated | 720p, 1080p | 输出分辨率 |
| duration | LIST | 是 | 6 | priceRelated | 4, 6, 8 | duration |
| seed | INT | 否 | - | range -1-2147483647; step 1 | - | 随机数种子，用于复现结果 |

### 万相2.5 Preview 图生图

- ID：`2042415417236176901`
- Endpoint：`/openapi/v2/alibaba/wan-2.5-preview/image-to-image`
- 类型/分组/来源：image-to-image / Wan Image Models / wan
- 价格：0.13 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 把这张照片的背景换成白天，阳光明媚 | length --2000 | - | 正向提示词，描述期望对图片进行的编辑操作 |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG","BMP","WEBP"] | - | 输入图片（1-3张） |
| negativePrompt | STRING | 否 | - | length --500 | - | 反向提示词，描述不希望出现的内容 |
| size | LIST | 否 | 1280*1280 | - | 1280*1280, 1024*1024, 800*1200, 1200*800, 960*1280, 1280*960, 720*1280, 1280*720, 1344*576 | 输出图片分辨率，若未指定size，系统将默认生成总像素为 1280*1280 的图像，并按以下规则保持宽高比（近似值）：单图输入：宽高比与输入图像一致；多图输入：宽高比与最后一张输入图像一致。 |
| n | INT | 是 | 1 | range 1-4; step 1; priceRelated | - | 生成图片数量（1-4张），直接影响费用 |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启智能提示词改写 |
| seed | INT | 否 | - | range -1-2147483647; step 1 | - | 随机数种子，用于复现生成结果 |

### 万相2.5 Preview 文生图

- ID：`2042415417236176900`
- Endpoint：`/openapi/v2/alibaba/wan-2.5-preview/text-to-image`
- 类型/分组/来源：text-to-image / Wan Image Models / wan
- 价格：0.13 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 黄金时刻窗边人像九宫格，同一位年轻成人（默认男性）保持完全一致的面容特征、肤色、眼型与自然表情。长直浅棕发自然披肩，身着简约深色无标T恤。九种姿势包括：仰光而... | length --2000 | - | 正向提示词，描述期望生成图像的内容和视觉特点 |
| negativePrompt | STRING | 否 | - | length --500 | - | 反向提示词，描述不希望出现的内容 |
| size | LIST | 是 | 1280*1280 | - | 1280*1280, 1104*1472, 1472*1104, 960*1696, 1696*960 | 图片分辨率 |
| n | INT | 否 | 1 | range 1-4; step 1; priceRelated | - | 生成图片数量（1-4张），直接影响费用 |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启智能提示词改写 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机数种子，用于复现生成结果 |

### 万相2.5 Preview 图生视频

- ID：`2042415417236176899`
- Endpoint：`/openapi/v2/alibaba/wan-2.5-preview/image-to-video`
- 类型/分组/来源：image-to-video / Wan Video Models / wan
- 价格：0.2 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 现代都市夜景中的双人武术对决。镜头结构：先用广角展示空旷街巷与两人对峙，再用中景跟拍攻防节奏，关键打击点使用近景特写（拳脚命中、呼吸、肌肉发力）。加入低机位环... | length 0-1500 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","BMP","WEBP"] | - | 图像分辨率：图像的宽度和高度范围为[240,8000]，单位为像素。文件大小：不超过10MB。 |
| audioUrl | AUDIO | 否 | - | maxSize 15MB; accept ["MP3","WAV"] | - | 音频文件URL，模型将使用该音频生成视频。时长：3～30s。文件大小：不超过15MB。超限处理：若音频长度超过 duration 值（ 如5秒），自动截取前5秒，其余部分丢弃。若音频长度不足视频时长，超出音频长度部分为无声视频。例如，音频... |
| negativePrompt | STRING | 否 | - | length 0-500 | - | negativePrompt |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| resolution | LIST | 是 | 1080p | priceRelated; ignoreCase | 480p, 720p, 1080p | resolution |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启prompt智能改写 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机数种子 |

### 万相2.5 Preview 文生视频

- ID：`2042415417236176898`
- Endpoint：`/openapi/v2/alibaba/wan-2.5-preview/text-to-video`
- 类型/分组/来源：text-to-video / Wan Video Models / wan
- 价格：0.2 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 镜头跟随黑衣男子快速逃亡,后面一群人在追,镜头转为侧面跟拍,人物惊慌撞倒路边的水果摊爬起来继续逃,人群慌乱的声音 | length 1-1500 | - | prompt |
| negativePrompt | STRING | 否 | - | length 0-500 | - | negativePrompt |
| audioUrl | AUDIO | 否 | - | maxSize 15MB; accept ["MP3","WAV"] | - | 音频文件URL，模型将使用该音频生成视频。时长：3～30s。文件大小：不超过15MB。超限处理：若音频长度超过 duration 值（ 如5秒），自动截取前5秒，其余部分丢弃。若音频长度不足视频时长，超出音频长度部分为无声视频。例如，音频... |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| size | LIST | 是 | 1920*1080 | priceRelated | 832*480, 480*832, 624*624, 1280*720, 720*1280, 960*960, 1088*832, 832*1088, 1920*1080, 1080*1920, 1440*1440, 1632*1248, 1248*1632 | 指定生成的视频分辨率，格式为宽*高 |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启prompt智能改写 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机数种子 |

### PixVerse V6 视频续写

- ID：`2041408069159936005`
- Endpoint：`/openapi/v2/pixverse-v6/extend`
- 类型/分组/来源：video-tools / Pixverse Al Models / rh-ai
- 价格：0.16 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 镜头缓慢拉远，慢慢显示出后面背景中的街景，男人侧头望向窗外。 | length --2048 | - | 描述续写片段中应发生的内容 |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; accept ["MP4","MOV","WEBM"] | - | 待续写的源视频 |
| resolution | LIST | 是 | 720p | priceRelated | 360p, 540p, 720p, 1080p | 输出分辨率 |
| duration | INT | 是 | 5 | range 1-15; step 1; priceRelated | - | 续写时长（秒） |
| generateAudioSwitch | BOOLEAN | 是 | true | priceRelated | - | 是否生成同步音频 |
| negativePrompt | STRING | 否 | - | length --2048 | - | 不希望出现的元素 |
| style | LIST | 否 | - | - | anime, 3d_animation, clay, comic, cyberpunk | 视觉风格 |
| seed | INT | 否 | - | range -1-2147483647; step 1 | - | 随机种子，用于复现结果 |

### PixVerse V6 转场

- ID：`2041408069159936004`
- Endpoint：`/openapi/v2/pixverse-v6/transition`
- 类型/分组/来源：video-tools / Pixverse Al Models / rh-ai
- 价格：0.16 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 更换图片中模特的衣服以实现平滑过渡。 | length --2048 | - | 描述期望的转场和变换过程 |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 起始帧图片 |
| endImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 终止帧图片 |
| resolution | LIST | 是 | 720p | priceRelated | 360p, 540p, 720p, 1080p | 输出分辨率 |
| duration | INT | 是 | 5 | range 1-15; step 1; priceRelated | - | 视频时长（秒） |
| generateAudioSwitch | BOOLEAN | 是 | false | priceRelated | - | 是否生成同步音频 |
| generateMultiClipSwitch | BOOLEAN | 是 | false | priceRelated | - | 是否启用多机位动态切换 |
| aspectRatio | LIST | 否 | 1:1 | - | 16:9, 4:3, 1:1, 3:4, 9:16, 2:3, 3:2, 21:9 | 输出画幅比例 |
| style | LIST | 否 | - | - | anime, 3d_animation, clay, comic, cyberpunk | 视觉风格（如 cyberpunk、cinematic） |
| negativePrompt | STRING | 否 | - | length --2048 | - | 不希望出现的元素 |
| seed | INT | 否 | - | range -1-2147483647; step 1 | - | 随机种子，用于复现结果 |
| thinkingType | BOOLEAN | 否 | false | - | - | 提示词优化模式 |

### PixVerse V6 图生视频

- ID：`2041408069159936003`
- Endpoint：`/openapi/v2/pixverse-v6/image-to-video`
- 类型/分组/来源：image-to-video / Pixverse Al Models / rh-ai
- 价格：0.16 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 参考图片 |
| prompt | STRING | 是 | 一个快乐的四口之家在阳光明媚的沙滩上玩耍。母亲穿着白色背心、牛仔短裤和太阳镜，温暖地微笑着低头看着她的孩子们。父亲穿着白色T恤和薄荷绿色短裤，身体前倾，调皮地... | length --2048 | - | 运动、镜头和场景氛围的文本描述 |
| resolution | LIST | 是 | 720p | priceRelated | 360p, 540p, 720p, 1080p | 输出分辨率 |
| duration | INT | 是 | 5 | range 1-15; step 1; priceRelated | - | 视频时长（秒） |
| generateAudioSwitch | BOOLEAN | 是 | true | priceRelated | - | 是否生成同步音频 |

### PixVerse V6 文生视频

- ID：`2041408069159936002`
- Endpoint：`/openapi/v2/pixverse-v6/text-to-video`
- 类型/分组/来源：text-to-video / Pixverse Al Models / rh-ai
- 价格：0.16 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一位年轻女性站在阳光明媚的城市街道上，棕色编发，佩戴珍珠耳环，戴着豹纹圆形太阳镜，围着深蓝色紫星图案围巾，身穿黑色皮夹克。她温暖地对着镜头微笑。背景是模糊的城... | length --2048 | - | 场景、运动、镜头和氛围的文本描述 |
| resolution | LIST | 是 | 720p | priceRelated | 360p, 540p, 720p, 1080p | 输出分辨率 |
| duration | INT | 是 | 5 | range 1-15; step 1; priceRelated | - | 视频时长（秒） |
| generateAudioSwitch | BOOLEAN | 是 | true | priceRelated | - | 是否生成同步音频 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 4:3, 1:1, 3:4, 9:16, 2:3, 3:2, 21:9 | 输出画幅比例 |

### 万相2.7-视频编辑

- ID：`2040031546192461825`
- Endpoint：`/openapi/v2/alibaba/wan-2.7/video-edit`
- 类型/分组/来源：video-edit / 最近上新 / wan
- 价格：0 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 把视频中的人物替换成图片中的人物 | length 1-5000 | - | prompt |
| videoUrl | VIDEO | 否 | <示例素材已脱敏> | maxSize 100MB; accept ["MP4","MOV"] | - | 视频时长必须为 2-10 秒 |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 20MB; multipleInputs; accept ["JPG","PNG","JPEG","WEBP","BMP"] | - | imageUrls |
| negativePrompt | STRING | 否 | - | length 0-500 | - | negativePrompt |
| resolution | LIST | 是 | 720P | priceRelated; ignoreCase | 720P, 1080P | resolution |
| duration | LIST | 是 | 0 | priceRelated | 0, 2, 3, 4, 5, 6, 7, 8, 9, 10 | 输出视频时长控制。0 = 保留原视频完整时长；2-10 = 从视频开头截取对应秒数；设置值不得超过输入视频原始时长 |
| aspectRatio | LIST | 是 | 16:9 | - | 16:9, 9:16, 1:1, 4:3, 3:4 | aspectRatio |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启prompt智能改写。开启后使用大模型对输入prompt进行智能改写。对于较短的prompt生成效果提升明显，但会增加耗时。 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机数种种子，取值范围为 [0, 2147483647] ，未指定时，系统自动生成随机种子。若需提升生成结果的可复现性，建议固定seed值。 请注意，由于模型生成具有概率性，即使使用相同seed，也不能保证每次生成结果完全一致 |

### 万相2.7-文生图Pro

- ID：`2039648613636050949`
- Endpoint：`/openapi/v2/alibaba/wan-2.7/text-to-image-pro`
- 类型/分组/来源：text-to-image / 最近上新 / rh-ai
- 价格：0.47 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一张雪豹全身肖像照。它的一只爪子抬起，正朝我们走来。地上的雪正在融化，露出一些小小的紫色和黄色花朵以及一些草。天空中有一个幻日。后面，一块尖锐的岩石高耸入云。... | length --5000 | - | 图像描述文本，包含主题、场景、风格、光影和氛围 |
| width | INT | 否 | 1024 | range 512-8192; step 1 | - | 输出宽度。默认1024，为空不传递尺寸。【注意】请求需同时满足3项限制：①宽与高均须在 512-8192 之间；②总像素(宽×高)须介于 589824(即768×768) 至 16777216(即4096×4096) 之间；③宽高比须在 ... |
| height | INT | 否 | 1024 | range 512-8192; step 1 | - | 输出高度。默认1024，为空不传递尺寸。【注意】请求需同时满足3项限制：①宽与高均须在 512-8192 之间；②总像素(宽×高)须介于 589824(即768×768) 至 16777216(即4096×4096) 之间；③宽高比须在 ... |
| thinkingMode | BOOLEAN | 否 | true | - | - | 启用思考模式以提升图像质量，会增加生成时间 |

### 万相2.7-文生图

- ID：`2039648613636050948`
- Endpoint：`/openapi/v2/alibaba/wan-2.7/text-to-image`
- 类型/分组/来源：text-to-image / 最近上新 / rh-ai
- 价格：0.19 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | Vogue封面风格。年轻美丽的亚洲女明星身穿草莓红缎面礼服，高挑站立，一只手叉腰，另一只手撩起头发，裙摆开衩露出长腿，眼神专注，奢华的T台走廊，电影感浅景深，... | length --5000 | - | 图像描述文本，包含主题、场景、风格、光影和氛围 |
| width | INT | 否 | 1024 | range 512-4096; step 1 | - | 输出宽度，范围512-4096，默认1024，为空不传递尺寸 |
| height | INT | 否 | 1024 | range 512-4096; step 1 | - | 输出高度，范围512-4096，默认1024，为空不传递尺寸 |
| thinkingMode | BOOLEAN | 否 | true | - | - | 启用思考模式以提升图像质量，会增加生成时间 |

### 万相2.7-图像编辑Pro

- ID：`2039648613636050947`
- Endpoint：`/openapi/v2/alibaba/wan-2.7/image-edit-pro`
- 类型/分组/来源：image-tools / 最近上新 / rh-ai
- 价格：0.47 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG","WEBP"] | - | 输入图片（1-9张），用于编辑或风格参考 |
| prompt | STRING | 是 | 给女孩带上苗族的银饰头饰 | length --2048 | - | 编辑指令，描述需要修改和保留的内容 |
| width | INT | 否 | 1024 | range 512-4096; step 1 | - | 输出宽度，范围512-4096，默认1024，为空不传递尺寸 |
| height | INT | 否 | 1024 | range 512-4096; step 1 | - | 输出高度，范围512-4096，默认1024，为空不传递尺寸 |

### 万相2.7-图像编辑

- ID：`2039648613636050946`
- Endpoint：`/openapi/v2/alibaba/wan-2.7/image-edit`
- 类型/分组/来源：image-tools / 最近上新 / rh-ai
- 价格：0.19 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG","WEBP"] | - | 输入图片（1-9张），用于编辑或风格参考 |
| prompt | STRING | 是 | 将人物后面的竹林换成更加真实的竹林效果，使整个画面更加真实，写实，摄影风格 | length --2048 | - | 编辑指令，描述需要修改和保留的内容 |
| width | INT | 否 | 1024 | range 512-4096; step 1 | - | 输出宽度，范围512-4096，默认1024，为空不传递尺寸 |
| height | INT | 否 | 1024 | range 512-4096; step 1 | - | 输出高度，范围512-4096，默认1024，为空不传递尺寸 |

### 万相2.7-参考生视频

- ID：`2039648613636050945`
- Endpoint：`/openapi/v2/alibaba/wan-2.7/reference-to-video`
- 类型/分组/来源：reference-to-video / 最近上新 / wan
- 价格：0.001 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 视频1在弹奏图片1 | length 1-5000 | - | prompt |
| videoUrls | VIDEO[] | 否 | <示例素材已脱敏> | maxSize 100MB; multipleInputs; accept ["MP4","MOV"] | - | videoUrls |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 20MB; multipleInputs; accept ["JPG","PNG","JPEG","WEBP","BMP"] | - | imageUrls |
| audioUrl | AUDIO | 否 | - | maxSize 15MB; accept ["MP3","WAV"] | - | 音频URL，用于指定参考素材（图像/视频）中主体角色的音乐 |
| negativePrompt | STRING | 否 | - | length 0-500 | - | negativePrompt |
| resolution | LIST | 是 | 1080P | priceRelated; ignoreCase | 720P, 1080P | resolution |
| duration | LIST | 是 | 5 | priceRelated | 2, 3, 4, 5, 6, 7, 8, 9, 10 | duration |
| aspectRatio | LIST | 是 | 16:9 | - | 16:9, 9:16, 1:1, 4:3, 3:4 | aspectRatio |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启prompt智能改写。开启后使用大模型对输入prompt进行智能改写。对于较短的prompt生成效果提升明显，但会增加耗时。 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机数种种子，取值范围为 [0, 2147483647] ，未指定时，系统自动生成随机种子。若需提升生成结果的可复现性，建议固定seed值。 请注意，由于模型生成具有概率性，即使使用相同seed，也不能保证每次生成结果完全一致 |

### 万相2.7-视频续写

- ID：`2039630309345267713`
- Endpoint：`/openapi/v2/alibaba/wan-2.7/video-extend`
- 类型/分组/来源：video-extend / 最近上新 / wan
- 价格：0.51 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 探险家走入黑暗的山洞中，打开了手中明亮的手电筒。光束缓慢扫过湿润的岩壁，突然照亮了深处巨大的、散发着幽蓝色光芒的水晶簇。镜头紧随探险家的步伐移动，展现出神秘、... | length 1-5000 | - | prompt |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; accept ["MP4","MOV"] | - | 基于⾸段视频⽚段，让模型⽣成后续内容 |
| audioUrl | AUDIO | 否 | - | maxSize 15MB; accept ["MP3","WAV"] | - | audioUrl |
| negativePrompt | STRING | 否 | 模糊，低画质，画面扭曲，卡通动画风格，人物肢体畸形，光线突变不自然，穿模，与前置视频风格不一致。 | length 0-500 | - | negativePrompt |
| resolution | LIST | 是 | 1080P | priceRelated; ignoreCase | 720P, 1080P | resolution |
| duration | LIST | 是 | 5 | priceRelated | 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | duration |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启prompt智能改写。开启后使用大模型对输入prompt进行智能改写。对于较短的prompt生成效果提升明显，但会增加耗时。 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机数种种子，取值范围为 [0, 2147483647] ，未指定时，系统自动生成随机种子。若需提升生成结果的可复现性，建议固定seed值。 请注意，由于模型生成具有概率性，即使使用相同seed，也不能保证每次生成结果完全一致 |

### 万相2.7-图生视频

- ID：`2039618329897144322`
- Endpoint：`/openapi/v2/alibaba/wan-2.7/image-to-video`
- 类型/分组/来源：image-to-video / 最近上新 / wan
- 价格：0.51 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 基于首尾两张竹林武侠对决的画面，生成一段流畅自然的武侠打斗视频，保持人物造型、竹林场景一致，动作连贯，光影过渡自然，还原武侠氛围感。 | length 1-5000 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG","JPEG","BMP","WEBP"] | - | 二选一：1，first_frame=首帧生视频。2，first_frame+last_frame=首尾帧生视频。 |
| lastImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG","JPEG","BMP","WEBP"] | - | 二选一：1，first_frame=首帧生视频。2，first_frame+last_frame=首尾帧生视频。 |
| audioUrl | AUDIO | 否 | - | maxSize 15MB; accept ["MP3","WAV"] | - | audioUrl |
| negativePrompt | STRING | 否 | - | length 0-500 | - | negativePrompt |
| resolution | LIST | 是 | 720P | priceRelated; ignoreCase | 720P, 1080P | resolution |
| duration | LIST | 是 | 5 | priceRelated | 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | duration |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启prompt智能改写。开启后使用大模型对输入prompt进行智能改写。对于较短的prompt生成效果提升明显，但会增加耗时。 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机数种种子，取值范围为 [0, 2147483647] ，未指定时，系统自动生成随机种子。若需提升生成结果的可复现性，建议固定seed值。 请注意，由于模型生成具有概率性，即使使用相同seed，也不能保证每次生成结果完全一致 |

### 万相2.7-文生视频

- ID：`2039544460993695745`
- Endpoint：`/openapi/v2/alibaba/wan-2.7/text-to-video`
- 类型/分组/来源：text-to-video / 最近上新 / wan
- 价格：0.51 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 日光，暖色调，侧光，特写镜头，居中构图。 一位拥有波浪形栗色长发的年轻女子，身穿浅蓝色吊带连衣裙，裙边饰有精致的蕾丝花边，亭亭玉立于阳光普照的花园中。她的脸部... | length 1-5000 | - | prompt |
| negativePrompt | STRING | 否 | - | length 0-500 | - | negativePrompt |
| audioUrl | AUDIO | 否 | - | maxSize 15MB; accept ["MP3","WAV"] | - | 背景音乐 |
| duration | LIST | 是 | 5 | priceRelated | 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | duration |
| resolution | LIST | 是 | 720P | priceRelated; ignoreCase | 720P, 1080P | resolution |
| aspectRatio | LIST | 是 | 16:9 | - | 16:9, 9:16, 1:1, 4:3, 3:4 | 宽⾼比 |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启prompt智能改写。开启后使用大模型对输入prompt进行智能改写。对于较短的prompt生成效果提升明显，但会增加耗时。 |
| seed | INT | 否 | - | range 0-2147483647; step 1 | - | 随机数种种子，取值范围为 [0, 2147483647] ，未指定时，系统自动生成随机种子。若需提升生成结果的可复现性，建议固定seed值。 请注意，由于模型生成具有概率性，即使使用相同seed，也不能保证每次生成结果完全一致 |

### Vidu-参考生视频-q3-mix

- ID：`2039255701421088770`
- Endpoint：`/openapi/v2/vidu/reference-to-video-q3-mix`
- 类型/分组/来源：reference-to-video / Vidu / vidu
- 价格：0.55 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | P1：城市街道近景特写。一位穿着棕色夹克和白色T恤的亚洲年轻男子（戴着背包）在繁华的步行街上走，皱眉看着手机屏幕，寻找方向。背景是模糊的行人和店铺招牌（如“咖... | length 1-5000 | - | 文本提示词，描述期望生成的视频内容。支持中英文，最长5000个字符。 |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 50MB; multipleInputs; accept ["JPG","JPEG","PNG","WEBP"] | - | 图像参考（1-7张），模型将以图片中的主体为参考生成主体一致的视频。支持png/jpeg/jpg/webp格式，像素不小于128x128，比例不超过4:1，单张不超过50MB。 |
| duration | INT | 是 | 5 | range 1-16; step 1; priceRelated | - | 视频时长（秒），默认5秒，可选1-16秒。 |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p | 分辨率，默认720p，可选720p/1080p。 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 4:3, 3:4, 1:1, auto | 视频比例，默认16:9，支持任意比例。auto表示根据输入图或视频自动推荐。 |
| audio | LIST | 否 | true | - | true, false | 是否使用音画同步（音视频直出）。true输出含台词和音效的视频，false输出静音视频。 |

### RH视频帧率增强

- ID：`2038503362263322628`
- Endpoint：`/openapi/v2/rhart-video/video-fps-increaser`
- 类型/分组/来源：video-tools / RH超分 / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| videoUrl | VIDEO | 是 | - | maxSize 500MB; priceRelated; accept ["MP4"] | - | 输入视频 |

### RH视频超分

- ID：`2038503362263322627`
- Endpoint：`/openapi/v2/rhart-video/video-upscaler`
- 类型/分组/来源：video-tools / RH超分 / rh-ai
- 价格：0.14 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 500MB; priceRelated; accept ["MP4"] | - | 输入视频（支持最长10分钟） |
| targetResolution | LIST | 是 | 1080p | priceRelated | 720p, 1080p, 2k, 4k | 目标分辨率 |

### seedance2.0-Fast/多模态视频

- ID：`2034917373414539278`
- Endpoint：`/openapi/v2/rhart-video/sparkvideo-2.0-fast/multimodal-video`
- 类型/分组/来源：reference-to-video / Seedance2.0 / bytedance
- 价格：0.5 CNY/秒
- 队列/并发：queueSize=400，concurrencyLimit=400

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 把视频中的人物替换成图片中的人物，并延长这段视频 | length 1-20480 | - | 视频生成提示词 |
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, 1080p, 2k, 4k | 视频分辨率。分为模型原生输出的分辨率（480p、720p），与基于 720p 原生生成后进行超分放大的分辨率（1080p、2k、4k）。 |
| duration | LIST | 是 | 5 | priceRelated | -1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 30MB; multipleInputs; accept ["JPG","JPEG","PNG","WEBP"] | - | 参考图片（0-9张） |
| videoUrls | VIDEO[] | 否 | <示例素材已脱敏> | maxSize 50MB; priceRelated; multipleInputs; accept ["MP4","MOV"] | - | 参考视频（0-3个，用于多模态参考/视频编辑/视频续写）。单个视频时长 [2, 15] s，最多传入 3 个参考视频，所有视频总时长不超过 15s。 |
| audioUrls | AUDIO[] | 否 | - | maxSize 15MB; multipleInputs; accept ["MP3","WAV"] | - | 参考音频（0-3个，需至少包含1个参考视频或图片）。单个音频时长 [2, 15] s，最多传入 3 段参考音频，所有音频总时长不超过 15 s。 |
| generateAudio | BOOLEAN | 否 | true | - | - | 是否生成视频音频 |
| ratio | LIST | 否 | adaptive | - | adaptive, 16:9, 4:3, 1:1, 3:4, 9:16, 21:9 | 视频宽高比 |
| realPersonMode | BOOLEAN | 否 | true | - | - | 真人模式，开启后系统会自动将图片/视频/音频转为火山资产（asset://），提升生成效果。 |
| conversionSlots | LIST[] | 否 | all | - | all, image1, image2, image3, image4, image5, image6, image7, image8, image9, video1, video2, video3 | 真人素材资产化槽位，多选；all 表示所有图片/视频槽位都做资产化。 |
| returnLastFrame | BOOLEAN | 否 | false | - | - | 是否返回视频尾帧图片 |
| seed | INT | 否 | -1 | range -1-2147483647; step 1 | - | 种子整数，用于控制生成内容的随机性。 |

### seedance2.0/多模态视频

- ID：`2034917373414539277`
- Endpoint：`/openapi/v2/rhart-video/sparkvideo-2.0/multimodal-video`
- 类型/分组/来源：reference-to-video / Seedance2.0 / bytedance
- 价格：0.6 CNY/秒
- 队列/并发：queueSize=1500，concurrencyLimit=1500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | @Image 1 图中的两个人，坐在一起吃火锅，火锅场景是@Video 1 | length 1-20480 | - | 视频生成提示词 |
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, native1080p, 1080p, 2k, 4k | 视频分辨率。分为模型原生输出的分辨率（480p、720p、native1080p），与基于 720p 原生生成后进行超分放大的分辨率（1080p、2k、4k）。 |
| duration | LIST | 是 | 5 | priceRelated | -1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 30MB; multipleInputs; accept ["JPG","JPEG","PNG","WEBP"] | - | 参考图片（0-9张） |
| videoUrls | VIDEO[] | 否 | <示例素材已脱敏> | maxSize 50MB; priceRelated; multipleInputs; accept ["MP4","MOV"] | - | 参考视频（0-3个，用于多模态参考/视频编辑/视频续写）。单个视频时长 [2, 15] s，最多传入 3 个参考视频，所有视频总时长不超过 15s。 |
| audioUrls | AUDIO[] | 否 | - | maxSize 50MB; multipleInputs; accept ["MP3","WAV"] | - | 参考音频（0-3个，需至少包含1个参考视频或图片）。单个音频时长 [2, 15] s，最多传入 3 段参考音频，所有音频总时长不超过 15 s。 |
| generateAudio | BOOLEAN | 否 | true | - | - | 是否生成视频音频 |
| ratio | LIST | 否 | adaptive | - | adaptive, 16:9, 4:3, 1:1, 3:4, 9:16, 21:9 | 视频宽高比 |
| realPersonMode | BOOLEAN | 否 | true | - | - | 真人模式，开启后系统会自动将图片/视频/音频转为火山资产（asset://），提升生成效果。 |
| conversionSlots | LIST[] | 否 | all | - | all, image1, image2, image3, image4, image5, image6, image7, image8, image9, video1, video2, video3 | 真人素材资产化槽位，多选；all 表示所有图片/视频槽位都做资产化。 |
| returnLastFrame | BOOLEAN | 否 | false | - | - | 是否返回视频尾帧图片 |
| seed | INT | 否 | -1 | range -1-2147483647; step 1 | - | 种子整数，用于控制生成内容的随机性。 |

### seedance2.0-Fast/图生视频

- ID：`2034917373414539276`
- Endpoint：`/openapi/v2/rhart-video/sparkvideo-2.0-fast/image-to-video`
- 类型/分组/来源：image-to-video / Seedance2.0 / bytedance
- 价格：0.5 CNY/秒
- 队列/并发：queueSize=400，concurrencyLimit=400

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | - | length 0-20480 | - | 视频生成提示词 |
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, 1080p, 2k, 4k | 视频分辨率。分为模型原生输出的分辨率（480p、720p），与基于 720p 原生生成后进行超分放大的分辨率（1080p、2k、4k）。 |
| duration | LIST | 是 | 5 | priceRelated | -1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| firstFrameUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 首帧图片 |
| lastFrameUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 尾帧图片（可选，首尾帧模式） |
| generateAudio | BOOLEAN | 否 | true | - | - | 是否生成视频音频 |
| ratio | LIST | 否 | adaptive | - | adaptive, 16:9, 4:3, 1:1, 3:4, 9:16, 21:9 | 视频宽高比 |
| realPersonMode | BOOLEAN | 否 | true | - | - | 真人模式，开启后系统会自动将图片/视频/音频转为火山资产（asset://），提升生成效果。 |
| conversionSlots | LIST[] | 否 | all | - | all, firstFrameUrl, lastFrameUrl | 真人素材资产化槽位，多选；all 表示首帧与尾帧都做资产化。 |
| returnLastFrame | BOOLEAN | 否 | false | - | - | 是否返回视频尾帧图片 |
| seed | INT | 否 | -1 | range -1-2147483647; step 1 | - | 种子整数，用于控制生成内容的随机性。 |

### seedance2.0/图生视频

- ID：`2034917373414539275`
- Endpoint：`/openapi/v2/rhart-video/sparkvideo-2.0/image-to-video`
- 类型/分组/来源：image-to-video / Seedance2.0 / bytedance
- 价格：0.6 CNY/秒
- 队列/并发：queueSize=1500，concurrencyLimit=1500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | - | length 0-20480 | - | 视频生成提示词 |
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, native1080p, 1080p, 2k, 4k | 视频分辨率。分为模型原生输出的分辨率（480p、720p、native1080p），与基于 720p 原生生成后进行超分放大的分辨率（1080p、2k、4k）。 |
| duration | LIST | 是 | 5 | priceRelated | -1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| firstFrameUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 首帧图片 |
| lastFrameUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","JPEG","PNG","WEBP"] | - | 尾帧图片（可选，首尾帧模式） |
| generateAudio | BOOLEAN | 否 | true | - | - | 是否生成视频音频 |
| ratio | LIST | 否 | adaptive | - | adaptive, 16:9, 4:3, 1:1, 3:4, 9:16, 21:9 | 视频宽高比 |
| realPersonMode | BOOLEAN | 否 | true | - | - | 真人模式，开启后系统会自动将图片/视频/音频转为火山资产（asset://），提升生成效果。 |
| conversionSlots | LIST[] | 否 | all | - | all, firstFrameUrl, lastFrameUrl | 真人素材资产化槽位，多选；all 表示首帧与尾帧都做资产化。 |
| returnLastFrame | BOOLEAN | 否 | false | - | - | 是否返回视频尾帧图片 |
| seed | INT | 否 | -1 | range -1-2147483647; step 1 | - | 种子整数，用于控制生成内容的随机性。 |

### seedance2.0-Fast/文生视频

- ID：`2034917373414539274`
- Endpoint：`/openapi/v2/rhart-video/sparkvideo-2.0-fast/text-to-video`
- 类型/分组/来源：text-to-video / Seedance2.0 / bytedance
- 价格：0.5 CNY/秒
- 队列/并发：queueSize=400，concurrencyLimit=400

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一部武侠风格的电影级大片。大雨滂沱的暗夜竹林中，一名白衣剑客与一名披着蓑衣的刺客正在激烈近战。镜头首先是广角全景，展示两人在竹林中高速穿梭和见招拆招的肢体互动... | length 1-20480 | - | 视频生成提示词 |
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, 1080p, 2k, 4k | 视频分辨率。分为模型原生输出的分辨率（480p、720p），与基于 720p 原生生成后进行超分放大的分辨率（1080p、2k、4k）。 |
| duration | LIST | 是 | 5 | priceRelated | -1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| generateAudio | BOOLEAN | 否 | true | - | - | 是否生成视频音频 |
| ratio | LIST | 否 | adaptive | - | adaptive, 16:9, 4:3, 1:1, 3:4, 9:16, 21:9 | 视频宽高比 |
| webSearch | BOOLEAN | 否 | false | - | - | 启用联网搜索增强 |
| returnLastFrame | BOOLEAN | 否 | false | - | - | 是否返回视频尾帧图片 |
| seed | INT | 否 | -1 | range -1-2147483647; step 1 | - | 种子整数，用于控制生成内容的随机性。 |

### seedance2.0/文生视频

- ID：`2034917373414539273`
- Endpoint：`/openapi/v2/rhart-video/sparkvideo-2.0/text-to-video`
- 类型/分组/来源：text-to-video / Seedance2.0 / bytedance
- 价格：0.6 CNY/秒
- 队列/并发：queueSize=1500，concurrencyLimit=1500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一部武侠风格的电影级大片。大雨滂沱的暗夜竹林中，一名白衣剑客与一名披着蓑衣的刺客正在激烈近战。镜头首先是广角全景，展示两人在竹林中高速穿梭和见招拆招的肢体互动... | length 1-20480 | - | 视频生成提示词 |
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, native1080p, 1080p, 2k, 4k | 视频分辨率。分为模型原生输出的分辨率（480p、720p、native1080p），与基于 720p 原生生成后进行超分放大的分辨率（1080p、2k、4k）。 |
| duration | LIST | 是 | 5 | priceRelated | -1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | 视频时长（秒） |
| generateAudio | BOOLEAN | 否 | true | - | - | 是否生成视频音频 |
| ratio | LIST | 否 | adaptive | - | adaptive, 16:9, 4:3, 1:1, 3:4, 9:16, 21:9 | 视频宽高比 |
| webSearch | BOOLEAN | 否 | false | - | - | 启用联网搜索增强 |
| returnLastFrame | BOOLEAN | 否 | false | - | - | 是否返回视频尾帧图片 |
| seed | INT | 否 | -1 | range -1-2147483647; step 1 | - | 种子整数，用于控制生成内容的随机性。 |

### f-2-dev/edit-lora

- ID：`2034917373414539265`
- Endpoint：`/openapi/v2/rhart-image/f-2-dev/edit-lora`
- 类型/分组/来源：image-to-image / 基础算法F / rh-ai
- 价格：0.25 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 51##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 16##text | STRING | 是 | 将图像变成扁平风格 | - | - | prompt |
| 47##select | LIST | 是 | 9:16 | - | auto, custom, 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 48##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 49##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHight |
| 18##lora_name | MODEL | 否 | 扁平风场景插画_v2.0.safetensors | - | - | lora |
| 18##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 52##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-dev/edit

- ID：`2034901418613473282`
- Endpoint：`/openapi/v2/rhart-image/f-2-dev/edit`
- 类型/分组/来源：image-to-image / 基础算法F / rh-ai
- 价格：0.24 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 20##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 17##text | STRING | 是 | 增加颜色，有冲击力 | - | - | prompt |
| 46##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2, custom, auto | aspectRatio |
| 35##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 34##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHight |
| 51##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-dev/text-to-image-lora

- ID：`2034899989190475778`
- Endpoint：`/openapi/v2/rhart-image/f-2-dev/text-to-image-lora`
- 类型/分组/来源：text-to-image / 基础算法F / rh-ai
- 价格：0.17 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 12##text | STRING | 是 | 超写实编辑肖像，女生有浓密的长深棕色头发，带有柔和的蓬松感，略显凌乱，环绕脸庞并向前垂落在双肩。非常靠近相机站立，腰部略微前倾。双肩轻轻向后收。下巴微收，眼睛... | - | - | prompt |
| 42##select | LIST | 是 | 9:16 | - | custom, 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 43##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 30##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHight |
| 16##lora_name | MODEL | 否 | F.1-复古勾线插画_v1.safetensors | - | - | lora |
| 16##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 44##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-dev/text-to-image

- ID：`2034898928027369473`
- Endpoint：`/openapi/v2/rhart-image/f-2-dev/text-to-image`
- 类型/分组/来源：text-to-image / 自部署开源模型 / rh-ai
- 价格：0.2 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 12##text | STRING | 是 | 在一片非洲大草原上，一只真实非洲狮的摄影照片，它在树边趴着，但眼睛警觉地望着四周 | - | - | prompt |
| 41##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2, custom | aspectRatio |
| 30##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 29##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHight |
| 43##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-klein-9b/text-to-image-lora

- ID：`2034892005601247234`
- Endpoint：`/openapi/v2/rhart-image/f-2-klein-9b/text-to-image-lora`
- 类型/分组/来源：text-to-image / 基础算法F / rh-ai
- 价格：0.06 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 52##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2, custom | aspectRatio |
| 36##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 35##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHight |
| 37##text | STRING | 是 | 一排排装满黑胶唱片的木制唱片架。墙壁上密密麻麻地挂满了装裱好的音乐海报、专辑封面和霓虹灯牌（一个蓝色的“OPEN”标志和一个粉色的长方形标志）。背景中模糊可见... | - | - | prompt |
| 59##lora_name | MODEL | 否 | CHIZHICKLENB4(1).safetensors | - | - | lora |
| 59##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 55##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-klein-9b/text-to-image

- ID：`2034882738118787074`
- Endpoint：`/openapi/v2/rhart-image/f-2-klein-9b/text-to-image`
- 类型/分组/来源：text-to-image / 基础算法F / rh-ai
- 价格：0.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 36##text | STRING | 是 | 破晓时分，平静的薄雾湖畔矗立着一座宏伟的中国古亭。一位身着优雅飘逸传统汉服的美丽年轻女子正在优雅地弹奏古琴。柔软的翠绿柳枝在微风中摇曳，淡粉色的桃花在空中缓缓... | - | - | prompt |
| 51##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2, custom | aspectRatio |
| 35##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 34##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHight |
| 54##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-klein-9b/edit

- ID：`2034877792577191938`
- Endpoint：`/openapi/v2/rhart-image/f-2-klein-9b/edit`
- 类型/分组/来源：image-to-image / 基础算法F / rh-ai
- 价格：0.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 53##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 54##text | STRING | 是 | 添加色彩 | - | - | prompt |
| 81##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2, custom, auto | aspectRatio |
| 70##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 69##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHight |
| 55##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-klein-4b/edit

- ID：`2034876557191086082`
- Endpoint：`/openapi/v2/rhart-image/f-2-klein-4b/edit`
- 类型/分组/来源：image-to-image / 基础算法F / rh-ai
- 价格：0.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 19##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 17##text | STRING | 是 | 把它变成一张真实的照片 | - | - | prompt |
| 47##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2 | aspectRatio |
| 51##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-klein-4b/edit-lora

- ID：`2034827564243288066`
- Endpoint：`/openapi/v2/rhart-image/f-2-klein-4b/edit-lora`
- 类型/分组/来源：image-to-image / 基础算法F / rh-ai
- 价格：0.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 41##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 37##select | LIST | 是 | 9:16 | - | 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 16##text | STRING | 是 | 变成油画风格 | - | - | prompt |
| 18##lora_name | MODEL | 否 | CHIZHICKLENB4(1).safetensors | - | - | lora |
| 18##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 40##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-klein-4b/text-to-image

- ID：`2034826108534587393`
- Endpoint：`/openapi/v2/rhart-image/f-2-klein-4b/text-to-image`
- 类型/分组/来源：text-to-image / 基础算法F / rh-ai
- 价格：0.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 9##text | STRING | 是 | 一名单板滑雪者在粉雪中转弯，在深蓝色雪地的洁白表面上留下弧形痕迹。获奖摄影作品，现实主义摄影风格。 | - | - | prompt |
| 25##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 26##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHight |
| 94##select | LIST | 是 | 9:16 | - | custom, 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 103##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-2-klein-4b/text-to-image-lora

- ID：`2034823170495938562`
- Endpoint：`/openapi/v2/rhart-image/f-2-klein-4b/text-to-image-lora`
- 类型/分组/来源：text-to-image / 基础算法F / rh-ai
- 价格：0.04 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 9##text | STRING | 是 | 一个美丽的女孩，29-35岁，看起来像欧洲人，善良，自发，微笑着，拥有长而浓密、松散的栗色头发（在阳光下头发有轻微的红色），骑着马，在大自然中，沙质景观，可以... | - | - | prompt |
| 33##select | LIST | 是 | 9:16 | - | 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 15##lora_name | MODEL | 否 | CHIZHICKLENB4(1).safetensors | - | - | lora |
| 15##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |

### 可灵对口型-视频生成

- ID：`2034581230479212554`
- Endpoint：`/openapi/v2/kling-lip-sync/lip-sync-video`
- 类型/分组/来源：audio-to-video / 最近上新 / rh-ai
- 价格：0.35 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| sessionId | STRING | 是 | 865289575831703581 | - | - | 会话ID，由人脸识别接口返回 |
| faceId | STRING | 是 | 0 | - | - | 人脸ID，由人脸识别接口返回 |
| audioId | STRING | 否 | 865272148167389266 | - | - | 通过语音合成接口生成的音频ID，与audioUrl二选一。仅支持30天内生成的、时长2~60秒的音频 |
| audioUrl | AUDIO | 否 | - | maxSize 5MB; accept ["MP3","WAV","M4A"] | - | 音频URL，与audioId二选一。支持.mp3/.wav/.m4a格式，文件不超过5MB，时长2~60秒 |
| soundStartTime | INT | 是 | - | range 0-- | - | 音频裁剪起点时间（单位ms）。以原始音频开始时间为准，开始时间为0分0秒，单位ms，起点之前的音频会被裁剪，裁剪后音频不得短于2秒 |
| soundEndTime | INT | 是 | - | - | - | 音频裁剪终点时间（单位ms）。以原始音频开始时间为准，开始时间为0分0秒，单位ms，终点之后的音频会被裁剪，裁剪后音频不得短于2秒 |
| soundInsertTime | INT | 是 | - | range 0-- | - | 裁剪后音频插入时间（单位ms）。插入音频时间范围需与人脸可对口型时间区间至少重合2秒，插入音频的开始时间不得早于视频开始时间，插入音频的结束时间不 得晚于视频结束时间 |
| soundVolume | FLOAT | 否 | 1 | range 0-2 | - | 音频音量大小，取值范围[0, 2]，默认为1 |
| originalAudioVolume | FLOAT | 否 | 1 | range 0-2 | - | 原始视频音量大小，取值范围[0, 2]，默认为1。原视频无声时参数无效 |

### 可灵对口型-语音合成

- ID：`2034581230479212553`
- Endpoint：`/openapi/v2/kling-lip-sync/tts`
- 类型/分组/来源：text-to-audio / 最近上新 / rh-ai
- 价格：0.04 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| text | STRING | 是 | 欢迎使用可灵对口型模型，可基于人脸识别结果和音频，生成唇形同步的视频，保持人物一致性，生成想要的音频，让人物去对口型。这就是用可灵对口型模型生成的视频哦。 | length --1000 | - | 合成音频的文案。文本内容最大长度1000，内容过长会返回错误 |
| voiceId | LIST | 是 | genshin_klee2 | - | genshin_vindi2, zhinen_xuesheng, tiyuxi_xuedi, ai_shatang, genshin_klee2, genshin_kirara, ai_kaiya, tiexin_nanyou, ai_chenjiahao_712, girlfriend_1_speech02, chat1_female_new-3, girlfriend_2_speech02, cartoon-boy-07, cartoon-girl-01, ai_huangyaoshi_712, you_pingjing, ai_laoguowang_712, chengshu_jiejie, ... (+28) | 音色ID。系统提供多种音色可供选择，具体音色效果和音色ID对应关系请参考官方文档：https://docs.qingque.cn/s/home/eZQDvafJ4vXQkP8T9ZPvmye8S?identityId=2E1MlYrrPk4 |
| voiceLanguage | LIST | 是 | zh | - | zh, en | 音色语种，与音色ID对应。默认为zh |
| voiceSpeed | FLOAT | 否 | 1.0 | range 0.8-2; step 0.1; precision 1 | - | 语速，默认为1.0。有效范围0.8~2.0，精确至小数点后1位 |

### SkyReels V4 Omni 参考视频-std

- ID：`2034581230479212552`
- Endpoint：`/openapi/v2/skyreels-v4/omni-reference-std`
- 类型/分组/来源：reference-to-video / SkyReels / rh-ai
- 价格：0.1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 视频延长 @video_1，乐队在唱歌。 | length 1-2500 | - | 视频描述提示词，支持 @tag 引用参考图片/视频 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1, 4:3, 3:4 | 视频画面比例。如果提供了参考视频（ref_videos），此参数将被忽略，输出尺寸会自动与参考视频保持一致。 |
| duration | INT | 否 | 5 | range 3-15; step 1; priceRelated | - | 视频时长（秒）。如果提供了 ref_videos（参考视频），此参数将被覆盖；这种情况下，输出视频的时长将与参考视频一致（最长不超过 10 秒）。 |
| sound | BOOLEAN | 是 | true | - | - | 是否生成视频音效。注意：mode=fast 不支持音效。 |
| promptOptimizer | BOOLEAN | 否 | true | - | - | 是否启用提示词优化 |
| refImages | COMPLEX[] | 否 | - | multipleInputs | - | 参考图片配置。所有项必须为同一 type；grid 类型时列表长度必须为 1，image 类型时最多 3 组。视频续写任务（ref_videos.type=extend）不可与 ref_images 同用。refImages和refVid... |
| refVideos | COMPLEX | 否 | - | - | - | 参考视频配置。最多支持 1 个视频引用（最长 15 秒）。reference 类型时输出时长会与参考视频一致（≤10 秒）。refImages和refVideos必须至少提供一个参考。 |
| resolution | LIST | 是 | 1080p | priceRelated | 480p, 720p, 1080p | 输出视频分辨率。支持480p、720p和1080p。 |

### SkyReels V4 图生视频-std

- ID：`2034581230479212547`
- Endpoint：`/openapi/v2/skyreels-v4/image-to-video-std`
- 类型/分组/来源：image-to-video / SkyReels / rh-ai
- 价格：0.39 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一个青少年男孩坐在窗边，夏雨中戴着耳机，闭着眼睛，手指随着音乐轻敲，外面的雨滴顺着玻璃快速滑下。 | length --2048 | - | 描述视频内容的文本提示词 |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","JPEG","PNG","GIF","BMP"] | - | 视频首帧图片 |
| sound | BOOLEAN | 是 | true | priceRelated | - | 是否生成视频音效 |
| duration | INT | 否 | 5 | range 3-15; step 1; priceRelated | - | 视频时长（秒） |
| promptOptimizer | BOOLEAN | 是 | true | - | - | 启用自动提示词扩展和优化，以实现更高的视觉保真度和对齐效果。 |
| resolution | LIST | 是 | 1080p | priceRelated | 480p, 720p, 1080p | 输出视频分辨率。支持480p、720p和1080p。 |

### f-krea-dev-lora

- ID：`2034581230479212546`
- Endpoint：`/openapi/v2/rhart-image/f-krea-dev-lora`
- 类型/分组/来源：text-to-image / 基础算法F / rh-ai
- 价格：0.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 133##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2 | aspectRatio |
| 45##text | STRING | 是 | “新世纪福音战士”明日香。明日香穿红色战斗服。期望的表情，真实的光影，真实渲染，俯视角度，经典风格，手部特写，自然人物动作，角色扮演，全身肖像，微距镜头，眼睛... | - | - | prompt |
| 115##lora_name | MODEL | 否 | flux-lora-labi.safetensors | - | - | lora |
| 115##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 135##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-dev-lora

- ID：`2034579103547654146`
- Endpoint：`/openapi/v2/rhart-image/f-dev-lora`
- 类型/分组/来源：text-to-image / 基础算法F / rh-ai
- 价格：0.03 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 104##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2 | aspectRatio |
| 105##text | STRING | 是 | 一位非常漂亮的女性的特写，她有迷人的绿色眼睛、分明的唇峰和长直发，展示了她无瑕的五官和柔和的表情。 | - | - | prompt |
| 107##lora_name | MODEL | 否 | QY_3D潮玩盲盒玩具手办_V1.0.safetensors | - | - | lora |
| 107##strength_model | FLOAT | 否 | 1 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 106##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-dev

- ID：`2034577474668724226`
- Endpoint：`/openapi/v2/rhart-image/f-dev`
- 类型/分组/来源：text-to-image / 基础算法F / rh-ai
- 价格：0.04 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 23##text | STRING | 是 | 一位非常漂亮的女性的特写，她有迷人的绿色眼睛、分明的唇峰和长直发，展示了她无瑕的五官和柔和的表情。 | - | - | prompt |
| 43##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2 | aspectRatio |
| 48##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### 万相2.2-图生视频

- ID：`2034555537645109250`
- Endpoint：`/openapi/v2/rhart-video/wan-2.2/image-to-video`
- 类型/分组/来源：image-to-video / Wan Video Models / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 219##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| 183##text | STRING | 是 | 电影级侏罗纪史前景观，超现实写实风格，金色时段宽广开场镜头，柔和体积光神射穿晨雾。广阔郁郁葱葱的绿色山谷，一条蜿蜒的 turquoise 河流切割茂密的蕨类森... | length 1-20000 | - | prompt |
| 16##negative_prompt | STRING | 否 | - | length 0-500 | - | negative_prompt |
| 202##select | LIST | 是 | 5 | priceRelated | 8, 5 | duration |
| 218##select | LIST | 是 | auto | priceRelated | auto, 1024×1920, 1920×1024, 720×1280, 1280×720, 480×832, 832×480 | resolution |

### wan-2.2/text-to-image-lora

- ID：`2034550762903961601`
- Endpoint：`/openapi/v2/rhart-video/wan-2.2/text-to-image-lora`
- 类型/分组/来源：text-to-image / 自部署开源模型 / rh-ai
- 价格：0.06 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 79##text | STRING | 是 | 一位皮肤惨白的美少女，画着有暗黑、魅惑风格的妆容，戴着红色的美瞳，红唇，脸上有细微的闪光和红色的疤痕装饰）头戴装饰着的红色和黑色玫瑰的蕾丝洛丽塔小礼帽，头发改... | - | - | prompt |
| 225##select | LIST | 是 | 1:1 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2, custom | aspectRatio |
| 216##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 215##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHeight |
| 229##lora_name | MODEL | 否 | sybian-bouncing-wan22-low-noise-e82-az420.safetensors | - | - | lora |
| 229##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 201##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### wan-2.2/image-to-image

- ID：`2034549626436321281`
- Endpoint：`/openapi/v2/rhart-video/wan-2.2/image-to-image`
- 类型/分组/来源：image-to-image / 自部署开源模型 / rh-ai
- 价格：0.08 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 272##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 79##text | STRING | 是 | 换成亚洲女生面孔 | - | - | prompt |
| 267##select | LIST | 是 | 1:1 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2, custom, auto | aspectRatio |
| 270##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 271##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHeight |
| 242##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### z-image-turbo/image-to-image-lora

- ID：`2034532017653415938`
- Endpoint：`/openapi/v2/rhart-image/z-image-turbo/image-to-image-lora`
- 类型/分组/来源：image-to-image / Z-Image / rh-ai
- 价格：0.03 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 44##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB | - | imageUrl |
| 18##text | STRING | 是 | 宝丽来胶片风格，将背景换成埃菲尔铁塔，有光照在人物身上。 | - | - | prompt |
| 41##select | LIST | 是 | 9:16 | - | 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 43##lora_name | MODEL | 否 | ZIT-flow-dpo-lora.safetensors | - | - | lora |
| 43##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 42##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### z-image-turbo/image-to-image

- ID：`2034531369067216897`
- Endpoint：`/openapi/v2/rhart-image/z-image-turbo/image-to-image`
- 类型/分组/来源：image-to-image / Z-Image / rh-ai
- 价格：0.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 66##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 41##text | STRING | 是 | 一位年轻的亚洲女性挺拔自信地站立在自然的户外环境中，温暖的黄金时刻光线洒在她的肌肤上，投下柔和的阴影。 | - | - | prompt |
| 64##select | LIST | 是 | 9:16 | - | 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 65##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### z-image/turbo-lora

- ID：`2034530934667345921`
- Endpoint：`/openapi/v2/rhart-image/z-image/turbo-lora`
- 类型/分组/来源：text-to-image / Z-Image / rh-ai
- 价格：0.03 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 6##text | STRING | 是 | 创作一张超现实电影感夜间肖像，描绘一位站在雨中的女性，精确保留其面部特征和发型，具有戏剧性照明、可见雨滴和强烈的情感氛围。 | - | - | prompt |
| 30##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2 | aspectRatio |
| 38##lora_name | MODEL | 否 | Z-Image _ 清纯高颜值_脸模版V1.0.safetensors | - | - | lora |
| 38##strength_model | FLOAT | 否 | 1 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 34##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### z-image/turbo

- ID：`2034529136204316673`
- Endpoint：`/openapi/v2/rhart-image/z-image/turbo`
- 类型/分组/来源：text-to-image / Z-Image / rh-ai
- 价格：0.04 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 10##text | STRING | 是 | 一幅静物画，以央美考试美术作品的水准呈现，在丰富褶皱布上摆放的一组不锈钢金属器皿，大卫石膏头像、玻璃器皿与葡萄等各种水果的组合，物体摆放疏密有致形成均衡构图，... | - | - | prompt |
| 28##select | LIST | 是 | 9:16 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2 | aspectRatio |
| 29##file_type | LIST | 否 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### qwen-image/edit-2511-lora

- ID：`2034517545568174082`
- Endpoint：`/openapi/v2/rhart-image/qwen-image/edit-2511-lora`
- 类型/分组/来源：image-to-image / Qwen Image / rh-ai
- 价格：0.07 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 44##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 38##text | STRING | 是 | 添加鲜艳的颜色，并转为插画风格 | - | - | prompt |
| 32##select | LIST | 是 | 自定义 | - | 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 16##lora_name | MODEL | 否 | Qwen-Image-Edit-2511-Lightning-4steps-V1.0-fp32.safetensors | - | - | lora |
| 16##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 40##file_type | LIST | 否 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### qwen-image/edit-2511

- ID：`2034512096265502721`
- Endpoint：`/openapi/v2/rhart-image/qwen-image/edit-2511`
- 类型/分组/来源：image-to-image / Qwen Image / rh-ai
- 价格：0.12 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 57##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 58##image | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl2 |
| 59##image | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl3 |
| 53##text | STRING | 是 | 添加色彩 | - | - | prompt |
| 28##select | LIST | 是 | 1:1 | - | 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 52##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### f-kontext-dev-lora

- ID：`2034492344042258433`
- Endpoint：`/openapi/v2/rhart-video/f-kontext/dev-lora`
- 类型/分组/来源：image-to-image / 基础算法F / rh-ai
- 价格：0.09 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 15##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 41##select | LIST | 是 | 1:1 | - | 1:1, 3:4, 4:3, 9:16, 16:9, 2:3, 3:2, custom, auto | aspectRatio |
| 44##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customWidth |
| 45##value | INT | 否 | 1024 | range 256-1536; step 1 | - | customHeight |
| 13##lora_name | MODEL | 否 | Kontextsouldrawing2_15.safetensors | - | - | lora |
| 13##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 4##text | STRING | 是 | 将图像变成油画风格 | - | - | prompt |
| 16##file_type | LIST | 否 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### ltx-2.3/text-to-video-lora

- ID：`2034466657231200258`
- Endpoint：`/openapi/v2/rhart-video/ltx-2.3/text-to-video-lora`
- 类型/分组/来源：text-to-video / LTX-2.3 / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 188##prompt | STRING | 是 | 一个广阔的空中镜头缓缓掠过金色时刻的浓密雨林。浓雾从树冠升起，温暖的橙色光束穿透树木。一条蜿蜒的河流映照着下方日落的天空。镜头逐渐向下倾斜，呈现出一条瀑布倾泻... | - | - | prompt |
| 247##select | LIST | 是 | 480p | priceRelated | 480p, 720p, 1080p | resolution |
| 248##select | LIST | 是 | 16:9 | - | 9:16, 16:9 | aspectRatio |
| 227##value | INT | 是 | 5 | range 5-15; step 1; priceRelated | - | duration |
| 254##lora_name | MODEL | 否 | framee_4000.safetensors | - | - | lora1 |
| 254##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora1_strength_model |
| 257##lora_name | MODEL | 否 | framee_4000.safetensors | - | - | lora2 |
| 257##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora2_strength_model |
| 258##lora_name | MODEL | 否 | framee_4000.safetensors | - | - | lora3 |
| 258##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora3_strength_model |

### ltx-2.3/text-to-video

- ID：`2034465605475917825`
- Endpoint：`/openapi/v2/rhart-video/ltx-2.3/text-to-video`
- 类型/分组/来源：text-to-video / LTX-2.3 / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 188##prompt | STRING | 是 | 第一视角无人机跟拍视角，主体为一只自由飞翔的鸟，镜头快速划过城市上空，与飞鸟同步高速飞行，沉浸式第一视角，流畅丝滑运镜，低空急速掠过楼宇街道，强烈速度感与飞行... | - | - | prompt |
| 247##select | LIST | 是 | 720p | priceRelated | 1080p, 720p, 480p | resolution |
| 248##select | LIST | 是 | 16:9 | - | 16:9, 9:16 | aspectRatio |
| 227##value | INT | 是 | 5 | range 5-15; step 1; priceRelated | - | duration |

### ltx-2.3/image-to-video-lora

- ID：`2034462865521664001`
- Endpoint：`/openapi/v2/rhart-video/ltx-2.3/image-to-video-lora`
- 类型/分组/来源：image-to-video / LTX-2.3 / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 98##image | IMAGE | 是 | 89f8673002dc2ec7743dcbdf1386fa85610bd0f70f62ed911c8daed8a193834c.png | maxSize 10MB | - | imageUrl |
| 245##select | LIST | 是 | 480p | priceRelated | 1080p, 720p, 480p | resolution |
| 240##select | LIST | 是 | 16:9 | - | 16:9, 9:16 | aspectRatio |
| 222##value | INT | 是 | 5 | range 5-15; step 1; priceRelated | - | duration |
| 254##lora_name | MODEL | 否 | framee_4000.safetensors | - | - | lora1 |
| 254##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora1_strength_model |
| 257##lora_name | MODEL | 否 | framee_4000.safetensors | - | - | lora2 |
| 257##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora2_strength_model |
| 258##lora_name | MODEL | 否 | framee_4000.safetensors | - | - | lora3 |
| 258##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora3_strength_model |
| 269##prompt | STRING | 是 | 从低角度开始缓慢环绕机器人调酒师运镜，镜头匀速 360° 环形移动，机器人金属头部与机械手臂流畅调酒，复古木质吧台摆满酒瓶，背景霓虹招牌忽明忽暗，暖光射灯打在... | - | - | prompt |

### ltx-2.3/image-to-video

- ID：`2034461796984971265`
- Endpoint：`/openapi/v2/rhart-video/ltx-2.3/image-to-video`
- 类型/分组/来源：image-to-video / LTX-2.3 / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 98##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | imageUrl |
| 200##prompt | STRING | 是 | 一点透视构图，太空驾驶舱内身着红色宇航服的人物紧握游戏手柄打游戏，头盔护目镜反射驾驶舱仪表盘光影；驾驶舱内宇航员在打游戏，驾驶舱外宇宙星云、星际尘埃呈流光状快... | - | - | prompt |
| 245##select | LIST | 是 | 480p | priceRelated | 480p, 720p, 1080p | resolution |
| 240##select | LIST | 是 | 16:9 | - | 9:16, 16:9 | aspectRatio |
| 222##value | INT | 是 | 5 | range 5-20; step 1; priceRelated | - | duration |

### qwen-image/text-to-image-2512-lora

- ID：`2034442710603292673`
- Endpoint：`/openapi/v2/rhart-image/qwen-image/text-to-image-2512-lora`
- 类型/分组/来源：text-to-image / Qwen Image / rh-ai
- 价格：0.1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 24##select | LIST | 是 | 9:16 | - | 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 6##text | STRING | 是 | 展示了在逆光环境下，模特轮廓线条更加分明，金色的光线以及丝绸环绕在模特周围，形成梦幻般的光环效果。整个场景充满艺术气息，展现了高水准的摄影技术和创意。 | - | - | prompt |
| 10##lora_name | MODEL | 否 | 赛博朋克风格2512.safetensors | - | - | lora |
| 10##strength_model | FLOAT | 否 | 0 | range -100-100; step 0.01; precision 2 | - | lora_strength |
| 30##file_type | LIST | 否 | png | - | png, jpeg, webp (lossless), webp (lossy) | outputFormat |

### 可灵对口型-人脸识别

- ID：`2034161609456504838`
- Endpoint：`/openapi/v2/kling-lip-sync/identify-face`
- 类型/分组/来源：audio-to-video / 最近上新 / rh-ai
- 价格：0.04 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| videoUrl | VIDEO | 否 | <示例素材已脱敏> | maxSize 100MB; accept ["MP4","MOV"] | - | 视频URL，与video_id二选一填写。支持.mp4/.mov格式，文件不超过100MB，视频时长2-60秒，仅支持720p和1080p，长宽边长512px~2160px |
| videoId | STRING | 否 | - | - | - | 通过可灵AI生成的视频ID，与videoUrl二选一填写。仅支持30天内生成的时长不超过60秒的视频 |

### SkyReels V4 文生视频-std

- ID：`2034161609456504835`
- Endpoint：`/openapi/v2/skyreels-v4/text-to-video-std`
- 类型/分组/来源：text-to-video / SkyReels / rh-ai
- 价格：0.39 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 真人实拍电影感追逐场景。两名骑手骑马疾驰穿过沙漠峡谷的超远景镜头。切换到领队骑手坚毅面庞的特写，皮肤上有尘土。 | length --2048 | - | 描述视频内容的文本提示词 |
| sound | BOOLEAN | 是 | true | priceRelated | - | 是否生成视频音效 |
| duration | INT | 否 | 5 | range 3-15; step 1; priceRelated | - | 视频时长（秒） |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1, 4:3, 3:4 | 视频画面比例 |
| promptOptimizer | BOOLEAN | 是 | true | - | - | 启用自动提示词扩展和优化，以实现更高的视觉保真度和对齐效果。 |
| resolution | LIST | 是 | 1080p | priceRelated | 480p, 720p, 1080p | 输出视频分辨率。支持480p、720p和1080p。 |

### 千问2.0-文生图

- ID：`2032764885651525635`
- Endpoint：`/openapi/v2/alibaba/qwen-image-2.0/text-to-image`
- 类型/分组/来源：text-to-image / Qwen Image / rh-ai
- 价格：0.13 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一幅精美的 3D 等距视角插画，展示了一间温馨的森林魔法药水小店。木制货架上摆满了装着发光的绿色和紫色液体的玻璃瓶。一只毛茸茸的小黑猫蜷缩在木制柜台上睡觉。柔... | length --800 | - | 正向提示词，描述期望生成的图像内容、风格和构图。支持中英文，长度不超过800个字符。 |
| negativePrompt | STRING | 否 | - | length --500 | - | 反向提示词，描述不希望在画面中出现的内容。支持中英文，长度不超过500个字符。 |
| size | LIST | 否 | 1024*1024 | - | 1024*1024, 1536*1536, 768*1152, 1024*1536, 1152*768, 1536*1024, 960*1280, 1080*1440, 1280*960, 1440*1080, 720*1280, 1080*1920, 1280*720, 1920*1080, 1344*576, 2048*872 | 输出图像分辨率，格式为宽*高。图像总像素需在512*512至2048*2048之间，默认1024*1024。 |
| imageNum | LIST | 否 | 1 | priceRelated | 1, 2, 3, 4, 5, 6 | 输出图片数量，默认1张，最多6张。 |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启提示词智能改写。开启后模型将对提示词进行优化与润色，生成内容更多样化。 |

### 千问2.0Pro-文生图

- ID：`2032764885651525634`
- Endpoint：`/openapi/v2/alibaba/qwen-image-2.0-pro/text-to-image`
- 类型/分组/来源：text-to-image / Qwen Image / rh-ai
- 价格：0.33 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一张超写实的未来赛博朋克咖啡师特写肖像，她在一个霓虹灯闪烁的下雨天咖啡馆里工作。她穿着半透明的材质雨衣，里面是一件带有发光电路纹理的衬衫。窗户上的雨滴清晰可见... | length --800 | - | 正向提示词，描述期望生成的图像内容、风格和构图。支持中英文，长度不超过800个字符。 |
| negativePrompt | STRING | 否 | - | length --500 | - | 反向提示词，描述不希望在画面中出现的内容。支持中英文，长度不超过500个字符。 |
| size | LIST | 否 | 1024*1024 | - | 1024*1024, 1536*1536, 768*1152, 1024*1536, 1152*768, 1536*1024, 960*1280, 1080*1440, 1280*960, 1440*1080, 720*1280, 1080*1920, 1280*720, 1920*1080, 1344*576, 2048*872 | 输出图像分辨率，格式为宽*高。图像总像素需在512*512至2048*2048之间，默认1024*1024。 |
| imageNum | LIST | 否 | 1 | priceRelated | 1, 2, 3, 4, 5, 6 | 输出图片数量，默认1张，最多6张。 |
| promptExtend | BOOLEAN | 否 | true | - | - | 是否开启提示词智能改写。开启后模型将对提示词进行优化与润色，生成内容更多样化。 |

### 即梦/动作模仿2.0

- ID：`2032764885651525633`
- Endpoint：`/openapi/v2/bytedance/dreamactor-v2`
- 类型/分组/来源：motion-control / 最近上新 / bytedance
- 价格：0.32 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 5MB; accept ["JPG","JPEG","PNG"] | - | 角色图片，分辨率480x480~1920x1080 |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; priceRelated; accept ["MP4","MOV","WEBM"] | - | 驱动视频，最长30秒，分辨率200x200~2048x1440 |

### Vidu-参考生视频-q3

- ID：`2031354034474311687`
- Endpoint：`/openapi/v2/vidu/reference-to-video-q3`
- 类型/分组/来源：reference-to-video / Vidu / vidu
- 价格：0.22 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 让 @图1 的人物在海边马路中漫步，阳光透过树叶洒下斑驳的光影 | length --5000 | - | 文本提示词，描述期望生成的视频内容。支持中英文，最长5000个字符。 |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 50MB; multipleInputs; accept ["JPG","JPEG","PNG","WEBP"] | - | 图像参考（1-7张），模型将以图片中的主体为参考生成主体一致的视频。支持png/jpeg/jpg/webp格式，像素不小于128x128，比例不超过4:1，单张不超过50MB。 |
| duration | INT | 是 | 5 | range 3-16; step 1; priceRelated | - | 视频时长（秒），默认5秒，可选3-16秒。 |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | 分辨率，默认720p，可选720p/1080p。 |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 4:3, 3:4, 1:1, auto | 视频比例，默认16:9，支持任意比例。auto表示根据输入图或视频自动推荐。 |
| audio | LIST | 否 | true | - | true, false | 是否使用音画同步（音视频直出）。true输出含台词和音效的视频，false输出静音视频。 |

### 千问2.0-图像编辑

- ID：`2031354034474311686`
- Endpoint：`/openapi/v2/alibaba/qwen-image-2.0/image-edit`
- 类型/分组/来源：image-to-image / Qwen Image / rh-ai
- 价格：0.13 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG","BMP","TIFF","WEBP","GIF"] | - | 输入图像（1-3张），支持单图编辑和多图融合。多图输入时按数组顺序定义图像编号，输出图像比例以最后一张为准。图像分辨率建议384~3072像素，大小不超过10MB。 |
| prompt | STRING | 是 | 金色的阳光从地平线破晓而出，将温暖的定向光投射在海岸线上。高动态范围，色彩对比鲜明——饱和的湛蓝天空与被阳光晒得发白的沙滩以及深邃的碧绿海水形成鲜明对比。湿漉... | length --800 | - | 正向提示词，描述期望生成的图像内容、风格和构图。支持中英文，每个汉字、字母、数字或符号计为一个字符。仅支持传入一个text。 |
| negativePrompt | STRING | 否 | - | length --500 | - | 反向提示词，描述不希望在画面中出现的内容。支持中英文，每个汉字、字母、数字或符号计为一个字符。 |
| size | LIST | 否 | - | - | 1024*1024, 1536*1536, 768*1152, 1024*1536, 1152*768, 1536*1024, 960*1280, 1080*1440, 1280*960, 1440*1080, 720*1280, 1080*1920, 1280*720, 1920*1080, 1344*576, 2048*872 | 输出图像分辨率，格式为宽*高。图像总像素需在512*512至2048*2048之间。不设置则与输入图（多图输入时为最后一张）一致。 |
| imageNum | LIST | 否 | 1 | priceRelated | 1, 2, 3, 4, 5, 6 | 输出图片数量，默认1张，最多6张。 |

### 千问2.0Pro-图像编辑

- ID：`2031354034474311685`
- Endpoint：`/openapi/v2/alibaba/qwen-image-2.0-pro/image-edit`
- 类型/分组/来源：image-to-image / Qwen Image / rh-ai
- 价格：0.33 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","JPEG","PNG","BMP","TIFF","WEBP","GIF"] | - | 输入图像（1-3张），支持单图编辑和多图融合。多图输入时按数组顺序定义图像编号，输出图像比例以最后一张为准。图像分辨率建议384~3072像素，大小不超过10MB。 |
| prompt | STRING | 否 | 阳光海滩场景，广角镜头，浅景深，黄金时段光线。一位年轻女士身着飘逸亚麻吊带裙，赤脚立于细沙之上，回眸展露温暖笑容。波浪长发随风轻扬，宽檐草帽随意后戴。身后碧蓝... | length --800 | - | 正向提示词，描述期望生成的图像内容、风格和构图。支持中英文，每个汉字、字母、数字或符号计为一个字符。仅支持传入一个text。 |
| negativePrompt | STRING | 否 | - | length --500 | - | 反向提示词，描述不希望在画面中出现的内容。支持中英文，每个汉字、字母、数字或符号计为一个字符。 |
| size | LIST | 否 | - | - | 1024*1024, 1536*1536, 768*1152, 1024*1536, 1152*768, 1536*1024, 960*1280, 1080*1440, 1280*960, 1440*1080, 720*1280, 1080*1920, 1280*720, 1920*1080, 1344*576, 2048*872 | 输出图像分辨率，格式为宽*高。图像总像素需在512*512至2048*2048之间。不设置则与输入图（多图输入时为最后一张）一致。 |
| imageNum | LIST | 否 | 1 | priceRelated | 1, 2, 3, 4, 5, 6 | 输出图片数量，默认1张，最多6张。 |

### 可灵动作控制V3.0-pro

- ID：`2031354034474311684`
- Endpoint：`/openapi/v2/kling-v3.0-pro/motion-control`
- 类型/分组/来源：motion-control / 可灵 3.0 / rh-ai
- 价格：1.08 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | 角色参考图片。宽/高尺寸介于 300px ~ 65536px；宽高比介于 1:2.5 ~ 2.5:1。 |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; priceRelated; accept ["MP4"] | - | 动作参考视频，时长3-30秒。宽/高尺寸均需介于 340px ~ 3850px 之间。 |
| characterOrientation | LIST | 是 | video | - | image, video | 角色朝向模式：image=跟随图片朝向(视频最长10s)，video=跟随视频朝向(视频最长30s) |
| prompt | STRING | 否 | - | length 0-2500 | - | 引导动作迁移的文本提示 |
| negativePrompt | STRING | 否 | - | length 0-2500 | - | 负向提示词 |
| keepOriginalSound | BOOLEAN | 否 | true | - | - | 是否保留原始视频声音 |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | 参考元素列表 |

### 可灵动作控制V3.0-std

- ID：`2031354034474311683`
- Endpoint：`/openapi/v2/kling-v3.0-std/motion-control`
- 类型/分组/来源：motion-control / 可灵 3.0 / rh-ai
- 价格：0.81 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | 角色参考图片。宽/高尺寸介于 300px ~ 65536px；宽高比介于 1:2.5 ~ 2.5:1。 |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; priceRelated; accept ["MP4"] | - | 动作参考视频，时长3-30秒。宽/高尺寸均需介于 340px ~ 3850px 之间。 |
| characterOrientation | LIST | 是 | video | - | image, video | 角色朝向模式：image=跟随图片朝向(视频最长10s)，video=跟随视频朝向(视频最长30s) |
| prompt | STRING | 否 | - | length 0-2500 | - | 引导动作迁移的文本提示 |
| negativePrompt | STRING | 否 | - | length 0-2500 | - | 负向提示词 |
| keepOriginalSound | BOOLEAN | 否 | true | - | - | 是否保留原始视频声音 |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | 参考元素列表 |

### 全能图片X-文生图-低价渠道版

- ID：`2031354034474311682`
- Endpoint：`/openapi/v2/rhart-image-g/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片X / rh-ai
- 价格：0.08 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| model | LIST | 是 | g-4.2 | - | g-3, g-4, g-4.1, g-4.2 | model |
| prompt | STRING | 是 | 画面中心是一位古代侠客，身穿飘逸的暗青色汉服，站在陡峭的悬崖边缘。他的身后，一只由赤色火焰和金色光芒交织而成的巨大凤凰正在展翅腾飞。电影级光影，史诗感，极高清... | - | - | prompt |
| aspectRatio | LIST | 否 | - | - | 960x960, 720x1280, 1280x720, 1168x784, 784x1168 | aspectRatio |

### 全能图片X-图生图-低价渠道版

- ID：`2031353763945897986`
- Endpoint：`/openapi/v2/rhart-image-g/image-to-image`
- 类型/分组/来源：image-to-image / 全能图片X / rh-ai
- 价格：0.08 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| model | LIST | 是 | g-4.2 | - | g-3, g-4, g-4.1, g-4.2 | model |
| prompt | STRING | 是 | 将这张手绘草图转换为高保真的现代 SaaS 开发者平台 UI 设计。采用深色模式（Dark Mode），主色调为赛博朋克风格的霓虹蓝和紫色。严格保留原有的导航... | - | - | prompt |
| imageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |

### 全能图片V1-图生图-官方稳定版

- ID：`2030982909433036801`
- Endpoint：`/openapi/v2/rhart-image-v1-official/edit`
- 类型/分组/来源：image-to-image / 全能图片 / rh-ai
- 价格：0.2 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 将人物手中的普通折扇替换为一把精致的白玉骨丝绸团扇，并在背景的木质回廊上悬挂两盏点亮的红灯笼。请保持原图明代官服的丝绸质感与阴影走势完全不变。 | length 1-20000 | - | prompt |
| aspectRatio | LIST | 是 | auto | - | auto, 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9 | aspectRatio |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |

### 全能图片V1-文生图-官方稳定版

- ID：`2030980802260844546`
- Endpoint：`/openapi/v2/rhart-image-v1-official/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片 / rh-ai
- 价格：0.2 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 写实风格照片，一位未来的手工艺人正在一间质朴、布满灰尘的赛博朋克工作室里，细致地制作一朵发光的霓虹玻璃玫瑰。温暖、自然的金色时刻阳光透过污浊的窗户射入，与玻璃... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 是 | 3:4 | - | auto, 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9 | aspectRatio |

### 万相2.2-文生视频

- ID：`2030249347301851137`
- Endpoint：`/openapi/v2/rhart-video/wan-2.2/text-to-video`
- 类型/分组/来源：text-to-video / Wan Video Models / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 133##select | LIST | 是 | 1280×720 | priceRelated | 832×480, 480×832, 1280×720, 720×1280, 1920×1024, 1024×1920 | resolution |
| 130##select | LIST | 是 | 5 | priceRelated | 8, 5 | duration） |
| 14##positive_prompt | STRING | 是 | 破晓时分，平静的薄雾湖畔矗立着一座宏伟的中国古亭。一位身着优雅飘逸传统汉服的美丽年轻女子正在优雅地弹奏古琴。柔软的翠绿柳枝在微风中摇曳，淡粉色的桃花在空中缓缓... | length 1-20000 | - | prompt |
| 14##negative_prompt | STRING | 否 | - | - | - | negativePrompt |

### qwen-image/text-to-image-2512

- ID：`2030246740046987266`
- Endpoint：`/openapi/v2/rhart-image/qwen-image/text-to-image-2512`
- 类型/分组/来源：text-to-image / Qwen Image / rh-ai
- 价格：0.12 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 3##text | STRING | 是 | 展示了远景镜头，在壮丽的雪山背景下，两个小小的人影站在远处山顶，背对着镜头，静静地观赏着日落的美景。夕阳的余晖洒在雪山上，呈现出一片金黄色的光辉，与蔚蓝的天空... | - | - | prompt |
| 25##select | LIST | 是 | 16:9 | - | custom, 3:2, 2:3, 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| 23##value | INT | 否 | 1024 | range 1-5000; step 1 | - | customWidth |
| 24##value | INT | 否 | 1024 | range 1-5000; step 1 | - | customHeight |
| 31##file_type | LIST | 是 | png | - | png, jpeg, webp(lossless), webp(lossy) | outputFormat |

### 万相2.2-首尾帧生视频

- ID：`2030226215631405057`
- Endpoint：`/openapi/v2/rhart-video/wan-2.2/start-to-end`
- 类型/分组/来源：image-to-video / Wan Video Models / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| 219##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| 222##image | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | lastImageUrl |
| 183##text | STRING | 是 | 她优雅地整理发丝，画面从车内平滑过渡到一个温馨且阳光充足的室内空间。 | - | - | prompt |
| 16##negative_prompt | STRING | 否 | - | - | - | negative_prompt |
| 202##select | LIST | 是 | 5 | priceRelated | 8, 5 | duration） |
| 218##select | LIST | 是 | auto | priceRelated | auto, 1024×1920, 1920×1024, 720×1280, 1280×720, 480×832, 832×480 | resolution |

### kling-elements

- ID：`2029753575120650241`
- Endpoint：`/openapi/v2/kling-elements`
- 类型/分组/来源：image-to-video / 可灵 3.0 / rh-ai
- 价格：0.06 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| name | STRING | 是 | 灰色羊毛商务西装 | - | - | Name for the element (e.g., "Work Suit", "Main Character") |
| description | STRING | 是 | 精选女士商务西装，采用混纺灰格纹羊毛面料。单排扣西装外套，翻领设计，配两颗黑色纽扣，腰部与胸部设有翻盖口袋。同色系直筒裤装。面料呈现细腻人字纹纹理。专业极简风... | - | - | Description of the element's visual characteristics |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | Primary reference image |
| elementReferList | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 20MB; multipleInputs; accept ["JPG","PNG"] | - | Additional reference images |
| tagList | COMPLEX[] | 否 | - | multipleInputs | - | Tags for organizing and categorizing elements |

### 万相2.6-参考生视频

- ID：`2028793089092763649`
- Endpoint：`/openapi/v2/alibaba/wan-2.6/reference-to-video`
- 类型/分组/来源：image-to-video / 最近上新 / wan
- 价格：0.45 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | character1 坐在靠窗的椅子上，手持 character2，在演奏一首舒缓的美国乡村民谣。 | length --- | - | prompt |
| negativePrompt | STRING | 否 | - | length --- | - | negativePrompt |
| videoUrls | VIDEO[] | 否 | <示例素材已脱敏> | maxSize 100MB; multipleInputs; accept ["MP4","MOV"] | - | Reference videos for subject consistency (0-3 videos).Combined with image_urls, total references cannot exceed 5. Refer... |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | Reference images for subject consistency (0-5 images). Combined with video_urls, total references cannot exceed 5. Refe... |
| size | LIST | 是 | 1920*1080 | priceRelated | 1280*720, 720*1280, 960*960, 1088*832, 832*1088, 1920*1080, 1080*1920, 1440*1440, 1632*1248, 1248*1632 | size |
| duration | LIST | 是 | 5 | priceRelated | 2, 3, 4, 5, 6, 7, 8, 9, 10 | duration |
| shotType | LIST | 否 | single | - | single, multi | shotType |

### 万相2.6-参考生视频Flash

- ID：`2028768183504355329`
- Endpoint：`/openapi/v2/alibaba/wan-2.6/reference-to-video-flash`
- 类型/分组/来源：image-to-video / 最近上新 / wan
- 价格：0.11 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 核心指令： 使用上传的视频作为环境、光影和车辆参考。使用上传的线稿图片作为人物参考。 画面风格： 电影感，超写实，好莱坞大片美学。将线稿人物完全转化为具有真实... | length --- | - | prompt |
| negativePrompt | STRING | 否 | - | length --- | - | negativePrompt |
| videoUrls | VIDEO[] | 否 | <示例素材已脱敏> | maxSize 100MB; multipleInputs; accept ["MP4","MOV"] | - | Reference videos for subject consistency (0-3 videos).Combined with image_urls, total references cannot exceed 5. Refer... |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | Reference images for subject consistency (0-5 images). Combined with video_urls, total references cannot exceed 5. Refe... |
| size | LIST | 是 | 1920*1080 | priceRelated | 1280*720, 720*1280, 960*960, 1088*832, 832*1088, 1920*1080, 1080*1920, 1440*1440, 1632*1248, 1248*1632 | size |
| duration | LIST | 是 | 5 | priceRelated | 2, 3, 4, 5, 6, 7, 8, 9, 10 | duration |
| shotType | LIST | 否 | single | - | single, multi | shotType |
| audio | BOOLEAN | 是 | true | priceRelated | - | audio |

### hitem3d-portrait-v15/multi-image-to-3d

- ID：`2028739604871659521`
- Endpoint：`/openapi/v2/hitem3d-portrait-v15/multi-image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：5.6 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| frontImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["PNG","JPEG","JPG","WEBP"] | - | 前视图 |
| backImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | 后视图 |
| leftImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | 左视图 |
| rightImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | 右视图 |
| resolution | LIST | 是 | 1536 | priceRelated | 1536 | resolution |
| face | INT | 否 | 2000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：1536：2000000 |

### hitem3d-portrait-v15/image-to-3d

- ID：`2028739462231769089`
- Endpoint：`/openapi/v2/hitem3d-portrait-v15/image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：5.6 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["PNG","JPEG","JPG","WEBP"] | - | 上传图像，模型将以此参数中传入的图片来生成3D模型 |
| resolution | LIST | 是 | 1536 | priceRelated | 1536 | resolution |
| face | INT | 否 | 2000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：1536：2000000 |

### hitem3d-portrait-v20/multi-image-to-3d

- ID：`2028739179481153538`
- Endpoint：`/openapi/v2/hitem3d-portrait-v20/multi-image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：5.6 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| frontImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["PNG","JPEG","JPG","WEBP"] | - | frontImageUrl |
| backImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | backImageUrl |
| leftImageUrl | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG"] | - | leftImageUrl |
| rightImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | rightImageUrl |
| resolution | LIST | 是 | 1536pro | priceRelated | 1536pro | resolution |
| face | INT | 否 | 2000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：1536Pro：2000000 |

### hitem3d-portrait-v20/image-to-3d

- ID：`2028738983598768130`
- Endpoint：`/openapi/v2/hitem3d-portrait-v20/image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：5.6 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["PNG","JPEG","JPG","WEBP"] | - | 上传图像，模型将以此参数中传入的图片来生成3D模型 |
| resolution | LIST | 是 | 1536pro | priceRelated | 1536pro | resolution |
| face | INT | 否 | 2000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：1536Pro：2000000 |

### hitem3d-portrait-v21/multi-image-to-3d

- ID：`2028733219521970177`
- Endpoint：`/openapi/v2/hitem3d-portrait-v21/multi-image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：5.6 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| frontImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["PNG","JPEG","JPG","WEBP"] | - | frontImageUrl |
| backImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | backImageUrl |
| leftImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | leftImageUrl |
| rightImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | rightImageUrl |
| resolution | LIST | 是 | 1536pro | priceRelated | 1536pro | resolution |
| face | INT | 否 | 2000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：1536Pro：2000000 |

### hitem3d-portrait-v21/image-to-3d

- ID：`2028733020951035905`
- Endpoint：`/openapi/v2/hitem3d-portrait-v21/image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：5.6 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["PNG","JPEG","JPG","WEBP"] | - | 上传图像，模型将以此参数中传入的图片来生成3D模型 |
| resolution | LIST | 是 | 1536pro | priceRelated | 1536pro | resolution |
| face | INT | 否 | 2000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：1536Pro：2000000 |

### hitem3d-v2/multi-image-to-3d

- ID：`2028732142722498561`
- Endpoint：`/openapi/v2/hitem3d-v2/multi-image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：5.6 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| frontImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["PNG","JPEG","JPG","WEBP"] | - | frontImageUrl |
| backImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | backImageUrl |
| leftImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | leftImageUrl |
| rightImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | rightImageUrl |
| resolution | LIST | 是 | 1536 | priceRelated | 1536, 1536pro | resolution |
| face | INT | 否 | 1000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：1536：2000000；1536Pro：2000000 |

### hitem3d-v15/multi-image-to-3d

- ID：`2028731529062268929`
- Endpoint：`/openapi/v2/hitem3d-v15/multi-image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：0.7 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| frontImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["PNG","JPEG","JPG","WEBP"] | - | frontImageUrl |
| backImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | backImageUrl |
| leftImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | leftImageUrl |
| rightImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | rightImageUrl |
| resolution | LIST | 是 | 1024 | priceRelated | 512, 1024, 1536, 1536pro | resolution |
| face | INT | 否 | 1000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：512：500000；1024：1000000；1536：2000000；1536Pro：2000000 |

### hitem3d-v2/image-to-3d

- ID：`2028730859546492930`
- Endpoint：`/openapi/v2/hitem3d-v2/image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：5.6 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["PNG","JPEG","JPG","WEBP"] | - | 上传图像，模型将以此参数中传入的图片来生成3D模型 |
| resolution | LIST | 是 | 1536 | priceRelated | 1536, 1536pro | resolution |
| face | INT | 否 | 1000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：1536：2000000；1536Pro：2000000 |

### hitem3d-v15/image-to-3d

- ID：`2028725956430282754`
- Endpoint：`/openapi/v2/hitem3d-v15/image-to-3d`
- 类型/分组/来源：image-to-3D / Hitem3D / rh-ai
- 价格：0.7 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| requestType | LIST | 是 | mesh | priceRelated | mesh, both | 请求类型，mesh：仅生成纯几何；both：一次性生成几何+纹理模型；默认为几何 |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["PNG","JPEG","JPG","WEBP"] | - | 上传图像，模型将以此参数中传入的图片来生成3D模型 |
| resolution | LIST | 是 | 1024 | priceRelated | 512, 1024, 1536, 1536pro | resolution |
| face | INT | 否 | 1000000 | range 100000-2000000; step 10000 | - | 模型面数，取值范围100000~2000000，推荐：512：500000；1024：1000000；1536：2000000；1536Pro：2000000 |

### 全能图片G-1.5-图生图-官方稳定版

- ID：`2028356572536913921`
- Endpoint：`/openapi/v2/rhart-image-g-1.5-official/image-to-image`
- 类型/分组/来源：image-to-image / 全能图片G / openai
- 价格：0.06 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 晴空骤然转阴，暴雨倾盆而下，雨帘密集模糊远景。前景三人慌忙应对：一人双手护头起身找伞，一人紧抱书本蜷缩遮挡，第三人拽起餐布试图保护食物。雨水在地面溅起水花，打... | length --- | - | prompt |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 50MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| size | LIST | 是 | 1024*1024 | priceRelated | 1024*1024, 1024*1536, 1536*1024 | size |
| quality | LIST | 是 | medium | priceRelated | low, medium, high | quality |
| inputFidelity | LIST | 否 | - | - | low, high | input fidelity, which allows you to better preserve details from the input images in the output |
| background | LIST | 否 | auto | - | auto, transparent, opaque | background |

### 全能图片G-1.5-文生图-官方稳定版

- ID：`2028353022540922882`
- Endpoint：`/openapi/v2/rhart-image-g-1.5-official/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片G / openai
- 价格：0.06 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 冬日清晨早餐铺，蒸汽升腾，顾客排队等热豆浆、油条。摊主在油锅前翻炸麻团和油条，油花翻滚，香气弥漫。桌上摆满豆腐脑、小笼包、茶叶蛋，背景是来往的自行车和电动车。... | length --- | - | prompt |
| size | LIST | 是 | 1024*1024 | priceRelated | 1024*1024, 1024*1536, 1536*1024 | size |
| quality | LIST | 是 | medium | priceRelated | low, medium, high | quality |
| background | LIST | 否 | - | - | auto, transparent, opaque | background |

### 全能视频X-编辑视频-官方稳定版

- ID：`2028310887460519937`
- Endpoint：`/openapi/v2/rhart-video-g-official/edit-video`
- 类型/分组/来源：video-edit / 全能视频X / rh-ai
- 价格：0.41 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 将整个视频转换为经典的吉卜力2D动漫风格。画面色彩需要变得明亮温暖，阳光呈现出柔和的丁达尔效应。人物的衣着和狗的毛发需转化为细腻的手绘水彩质感。在人物奔跑和镜... | length 5-800 | - | prompt |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 50MB; priceRelated; accept ["MP4"] | - | videoUrl |
| resolution | LIST | 是 | 480p | - | 720p, 480p | resolution |

### 全能视频X-文生视频-官方稳定版

- ID：`2028308297217753089`
- Endpoint：`/openapi/v2/rhart-video-g-official/text-to-video`
- 类型/分组/来源：text-to-video / 全能视频X / rh-ai
- 价格：1.89 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 电影级广角跟拍镜头。在下着连绵细雨的赛博朋克城市街道上，镜头缓慢向前推进。地面上的水洼真实地倒映着上空闪烁的粉色和蓝色全息广告牌。两台流线型的飞行汽车在半空中... | length 5-800 | - | prompt |
| aspectRatio | LIST | 是 | 16:9 | - | 16:9, 9:16, 1:1 | aspectRatio |
| resolution | LIST | 是 | 720p | - | 720p, 480p | resolution |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | duration |

### 全能视频X-图生视频-官方稳定版

- ID：`2028306154142318593`
- Endpoint：`/openapi/v2/rhart-video-g-official/image-to-video`
- 类型/分组/来源：image-to-video / 全能视频X / rh-ai
- 价格：1.89 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 电影级环绕运镜。镜头缓慢而平滑地绕着这辆红色复古跑车移动。下方的海浪动态地拍打着礁石，激起自然真实的水花。随着太阳逐渐沉入地平线，天空光影自然过渡，跑车的车头... | length 5-800 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| resolution | LIST | 是 | 720p | - | 720p, 480p | resolution |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | duration |

### 全能图片V2-图生图-官方稳定版

- ID：`2027661818379649025`
- Endpoint：`/openapi/v2/rhart-image-n-g31-flash-official/image-to-image`
- 类型/分组/来源：image-to-image / 全能图片 / rh-ai
- 价格：0.49 CNY/次
- 队列/并发：queueSize=5000，concurrencyLimit=5000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| prompt | STRING | 是 | 操作指令： 参考我上传的这张人物线稿图。必须百分之百忠实于原线稿的线条结构，不得有任何线条的增加或减少。 上色要求： 将其处理成一种复古的日式赛博朋克漫画风格... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 否 | 9:16 | - | 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9, 1:4, 4:1, 1:8, 8:1 | aspectRatio |
| resolution | LIST | 是 | 1k | priceRelated; ignoreCase | 1k, 2k, 4k | resolution |

### 全能图片V2-文生图-官方稳定版

- ID：`2027658953443524610`
- Endpoint：`/openapi/v2/rhart-image-n-g31-flash-official/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片 / rh-ai
- 价格：0.49 CNY/次
- 队列/并发：queueSize=5000，concurrencyLimit=5000

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一张 21:9 的电影级特写镜头。画面中心是一个复古的霓虹灯牌，上面用清晰的英文字体写着 "全能图片 V2"。灯牌背景是雨后的赛博朋克街道，地面有极度真实的积... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 否 | 21:9 | - | 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9, 1:4, 4:1, 1:8, 8:1 | aspectRatio |
| resolution | LIST | 是 | 1k | priceRelated; ignoreCase | 1k, 2k, 4k | resolution |

### seedream-v5-lite-图生图

- ID：`2026215209183760386`
- Endpoint：`/openapi/v2/seedream-v5-lite/image-to-image`
- 类型/分组/来源：image-to-image / Seedream / rh-ai
- 价格：0.22 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 请融合输入的图1与图2，生成一组共3张连贯的国漫风格（Guoman style）横版概念设计图。画面主体为青年方文与他的宠物黑羽乌鸦。第一张：方文穿着图1中锦... | length 5-2000 | - | prompt |
| width | INT | 否 | 2048 | range 1600-4704; step 8 | - | width |
| height | INT | 否 | 2048 | range 1344-4096; step 1 | - | height |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["PNG","JPEG"] | - | imageUrls |
| sequentialImageGeneration | LIST | 否 | disabled | - | disabled, auto | sequentialImageGeneration，基于您输入的内容，生成的一组内容关联的图片，需配置为auto |
| maxImages | INT | 否 | 1 | range 1-15; step 1; priceRelated | - | maxImages |
| resolution | LIST | 否 | - | - | 2k, 3k | 优先级高于widthxheight，传递resolution则使用resolution，不再使用widthxheight |

### seedream-v5-lite-文生图

- ID：`2026214940576337921`
- Endpoint：`/openapi/v2/seedream-v5-lite/text-to-image`
- 类型/分组/来源：text-to-image / Seedream / rh-ai
- 价格：0.22 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 制作一张上海未来5日的天气预报图，采用现代扁平化插画风格，清晰展示每日天气、温度和穿搭建议。 整体为横向排版，标题为“上海未来5日天气预报”，包含5个等宽的垂... | length 5-2000 | - | prompt |
| width | INT | 否 | 2048 | range 1600-4704; step 8 | - | width |
| height | INT | 否 | 2048 | range 1344-4096; step 1 | - | height |
| sequentialImageGeneration | LIST | 否 | disabled | - | disabled, auto | sequentialImageGeneration，基于您输入的内容，生成的一组内容关联的图片，需配置为auto。 |
| maxImages | INT | 否 | 1 | range 1-15; step 1; priceRelated | - | maxImages |
| toolsType | LIST | 否 | web_search | - | web_search | toolsType |
| resolution | LIST | 否 | - | - | 2k, 3k | 优先级高于widthxheight，传递resolution则使用resolution，不再使用widthxheight |

### 全能视频V3.1-pro-视频扩展-官方稳定版

- ID：`2022226376775716865`
- Endpoint：`/openapi/v2/rhart-video-v3.1-pro-official/video-extend`
- 类型/分组/来源：text-to-video / 全能视频V / google
- 价格：17.4 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| video | VIDEO | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["MP4"] | - | video |
| prompt | STRING | 否 | - | length 5-8000 | - | prompt |
| resolution | LIST | 是 | 720p | - | 720p, 1080p | resolution |
| negativePrompt | STRING | 否 | - | - | - | negativePrompt |
| seed | INT | 否 | - | step 1 | - | seed |

### 全能视频V3.1-fast-图生视频-官方稳定版

- ID：`2022225870330286082`
- Endpoint：`/openapi/v2/rhart-video-v3.1-fast-official/image-to-video`
- 类型/分组/来源：image-to-video / 全能视频V / google
- 价格：2.35 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 侦探抬起头，雨水从他的帽檐滴落。他带着严肃的表情直视镜头说道：“嫌疑人往这边走了。” 背景中的煤气灯在风中闪烁。音频：大雨声，远处的马车轮声，以及他低沉沙哑的... | length 5-8000 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| lastImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | lastImageUrl |
| negativePrompt | STRING | 否 | - | - | - | negativePrompt |
| seed | INT | 否 | - | step 1 | - | seed |
| aspectRatio | LIST | 否 | 9:16 | - | 16:9, 9:16 | aspectRatio |
| resolution | LIST | 是 | 720p | - | 720p, 1080p, 4k | resolution |
| duration | LIST | 是 | 8 | priceRelated | 4, 6, 8 | duration |
| generateAudio | BOOLEAN | 是 | false | priceRelated | - | generateAudio |

### 全能视频V3.1-fast-视频扩展-官方稳定版

- ID：`2022222100292718594`
- Endpoint：`/openapi/v2/rhart-video-v3.1-fast-official/video-extend`
- 类型/分组/来源：text-to-video / 全能视频V / google
- 价格：6.56 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| video | VIDEO | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["MP4"] | - | video |
| prompt | STRING | 否 | - | length 5-8000 | - | prompt |
| resolution | LIST | 是 | 720p | - | 720p, 1080p | resolution |
| negativePrompt | STRING | 否 | - | - | - | negativePrompt |
| seed | INT | 否 | - | step 1 | - | seed |

### 全能视频V3.1-pro-参考生视频-官方稳定版

- ID：`2022221492944916482`
- Endpoint：`/openapi/v2/rhart-video-v3.1-pro-official/reference-to-video`
- 类型/分组/来源：image-to-video / 全能视频V / google
- 价格：9.4 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 以第一张图作为背景，第二张图和第三张图的人物在这个场景中相遇，发生一段有趣的故事 | length 5-8000 | - | prompt |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| negativePrompt | STRING | 否 | - | - | - | negativePrompt |
| seed | INT | 否 | - | step 1 | - | seed |
| resolution | LIST | 是 | 1080p | priceRelated | 720p, 1080p, 4k | resolution |
| generateAudio | BOOLEAN | 是 | false | priceRelated | - | generateAudio |

### 全能视频V3.1-fast-文生视频-官方稳定版

- ID：`2022220635650150401`
- Endpoint：`/openapi/v2/rhart-video-v3.1-fast-official/text-to-video`
- 类型/分组/来源：text-to-video / 全能视频V / google
- 价格：2.35 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一位粗犷的探险家身处雪山洞穴中。他直视镜头，微微颤抖，说道：“我们找到了入口。” 光线昏暗，仅由一支闪烁的火把照亮。音频：洞外呼啸的风声，火把燃烧的噼啪声，以... | length 5-8000 | - | prompt |
| aspectRatio | LIST | 否 | 9:16 | - | 16:9, 9:16 | aspectRatio |
| duration | LIST | 是 | 8 | priceRelated | 4, 6, 8 | duration |
| resolution | LIST | 是 | 720p | - | 720p, 1080p, 4k | resolution |
| generateAudio | BOOLEAN | 是 | true | priceRelated | - | generateAudio |
| negativePrompt | STRING | 否 | - | - | - | negativePrompt |
| seed | INT | 否 | - | step 1 | - | seed |

### 全能视频V3.1-pro-图生视频-官方稳定版

- ID：`2022213697642188801`
- Endpoint：`/openapi/v2/rhart-video-v3.1-pro-official/image-to-video`
- 类型/分组/来源：image-to-video / 全能视频V / google
- 价格：4.7 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 镜头缓慢向前推进穿过街道。霓虹灯牌不规则地闪烁，在潮湿的路面上投射出不断变化的彩色倒影。光柱中可见大雨倾盆而下。音频：大雨击打地面的声音，远处的雷声，以及霓虹... | length 5-8000 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| lastImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | lastImageUrl |
| negativePrompt | STRING | 否 | - | - | - | negativePrompt |
| seed | INT | 否 | - | step 1 | - | seed |
| aspectRatio | LIST | 否 | 9:16 | - | 16:9, 9:16 | aspectRatio |
| resolution | LIST | 是 | 720p | - | 720p, 1080p, 4k | resolution |
| duration | LIST | 是 | 8 | priceRelated | 4, 6, 8 | duration |
| generateAudio | BOOLEAN | 是 | false | priceRelated | - | generateAudio |

### 全能视频V3.1-pro-文生视频-官方稳定版

- ID：`2022195635475992577`
- Endpoint：`/openapi/v2/rhart-video-v3.1-pro-official/text-to-video`
- 类型/分组/来源：text-to-video / 全能视频V / google
- 价格：4.7 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一只精力充沛的小狗在一块鲜艳的黄色冲浪板上冲浪，乘坐汹涌的蓝色波浪。快速动作，水花四溅，戏剧化的海洋喷雾，动态镜头从侧面跟随冲浪板，阳光明媚，天空晴朗，对比度... | length 5-8000 | - | prompt |
| aspectRatio | LIST | 否 | 9:16 | - | 16:9, 9:16 | aspectRatio |
| duration | LIST | 是 | 8 | priceRelated | 4, 6, 8 | duration |
| resolution | LIST | 是 | 720p | - | 720p, 1080p, 4k | resolution |
| generateAudio | BOOLEAN | 是 | false | priceRelated | - | generateAudio |
| negativePrompt | STRING | 否 | - | - | - | negativePrompt |
| seed | INT | 否 | - | step 1 | - | seed |

### minimax/voice-clone

- ID：`2021514824548372482`
- Endpoint：`/openapi/v2/rhart-audio/text-to-audio/voice-clone`
- 类型/分组/来源：text-to-audio / Minmax Hailuo Audio / rh-ai
- 价格：3.12 CNY/字符
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| audio | AUDIO | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["MP3","WAV"] | - | audio |
| custom_voice_id | STRING | 是 | Elegant_Man | - | - | 自定义用户定义 ID：必须至少有 8 个字符，以字母开头，并包括字母和数字（例如，RH-20250717-1050）。重复的语音 ID 将导致错误。此 ID 可用于以下型号：minimax/speech-02-hd minimax/spe... |
| text | STRING | 是 | 基于 Speech-02 与最新 Speech 2.6 HD/Turbo 系列打造的尖端声纹克隆引擎。它仅需数秒音频样本即可实现高保真的零样本（Zero-sh... | - | - | 你好！欢迎来到 RunningHub！这是您克隆的声音预览。希望您喜欢！ |
| accuracy | FLOAT | 否 | 0.7 | range 0-1; step 0.1; precision 2 | - | accuracy |
| need_noise_reduction | BOOLEAN | 是 | false | - | - | need_noise_reduction |
| need_volume_normalization | BOOLEAN | 是 | false | - | - | need_volume_normalization |
| model | LIST | 是 | speech-02-hd | - | speech-02-hd, speech-02-turbo, speech-2.5-hd-preview, speech-2.5-turbo-preview, speech-2.6-hd, speech-2.6-turbo, speech-2.8-turbo, speech-2.8-hd | 指定用于预览的 TTS 模型。这只是克隆后的预览。模型生成后，任何 Minimax Turbo 或 HD 语音模型都可用于推理。 |
| language_boost | LIST | 否 | - | - | Chinese, Chinese,Yue, English, Arabic, Russian, Spanish, French, Portuguese, German, Turkish, Dutch, Ukrainian, Vietnamese, Indonesian, Japanese, Italian, Korean, Thai, ... (+7) | 增强对指定语言和方言的识别能力 |

### kling-v2.6-pro-动作控制

- ID：`2021508655436025857`
- Endpoint：`/openapi/v2/kling-v2.6-pro/motion-control`
- 类型/分组/来源：motion-control / 可灵 / rh-ai
- 价格：0.56 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | 宽/高尺寸介于 300px ~ 65536px；宽高比介于 1:2.5 ~ 2.5:1。 |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; priceRelated; accept ["MP4"] | - | 宽/高尺寸均需介于 340px ~ 3850px 之间。 |
| characterOrientation | LIST | 是 | video | - | image, video | 其中image：与图片中人物朝向一致；此时参考视频时长不得超过10秒； - 其中video：与视频中人物朝向一致；此时参考视频时长不得超过30秒 |
| prompt | STRING | 否 | - | length 0-2500 | - | prompt |
| keepOriginalSound | LIST | 否 | yes | - | yes, no | keepOriginalSound |

### kling-v2.6-std-动作控制

- ID：`2021500676489891841`
- Endpoint：`/openapi/v2/kling-v2.6-std/motion-control`
- 类型/分组/来源：motion-control / 可灵 / rh-ai
- 价格：0.35 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | 宽/高尺寸介于 300px ~ 65536px；宽高比介于 1:2.5 ~ 2.5:1。 |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 100MB; priceRelated; accept ["MP4"] | - | 宽/高尺寸均需介于 340px ~ 3850px 之间。 |
| characterOrientation | LIST | 是 | video | - | image, video | 其中image：与图片中人物朝向一致；此时参考视频时长不得超过10秒； - 其中video：与视频中人物朝向一致；此时参考视频时长不得超过30秒 |
| prompt | STRING | 否 | - | length 0-2500 | - | prompt |
| keepOriginalSound | LIST | 否 | yes | - | yes, no | keepOriginalSound |

### minimax/music-2.5

- ID：`2020818466334068738`
- Endpoint：`/openapi/v2/rhart-audio/text-to-audio/music-2.5`
- 类型/分组/来源：text-to-audio / Minmax Hailuo Audio / rh-ai
- 价格：0.8 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | > 80s Synth-pop, Retro Future, Female Vocal, Groovy, Minneapolis Sound style, a... | - | - | prompt |
| lyrics | STRING | 是 | [Intro] (梦幻的合成器开场，伴随强烈的鼓点倒计时) 3, 2, 1, Let's go! [Verse 1] 霓虹灯在雨后街头闪烁， 你的影子消失在转... | - | - | lyrics |
| bitrate | LIST | 否 | 256000 | - | 32000, 60000, 64000, 128000, 256000 | bitrate |
| sampleRate | LIST | 否 | 44100 | - | 16000, 24000, 32000, 44100 | sampleRate |

### Vidu-首尾帧生视频-q3-pro

- ID：`2019687963136692226`
- Endpoint：`/openapi/v2/vidu/start-end-to-video-q3-pro`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.31 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 轨迹描述： 重点捕捉 5 秒内冰块受热破裂、融化、再到水滴受热蒸发的复杂物理过程。光影需根据冰晶的折射变化进行动态补偿。 音频： 冰块破裂的清脆声、冰融化成水... | length 1-4000 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |
| audio | BOOLEAN | 是 | true | - | - | audio |

### Vidu-首尾帧生视频-q3-turbo

- ID：`2019677753793908737`
- Endpoint：`/openapi/v2/vidu/start-end-to-video-q3-turbo`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.18 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 动作补全： 补全 16 秒的疾速俯冲过程。镜头伴随运动员在雪地上高速滑行并完成两次大幅度转弯，展示极速下的动态稳定性，并确保轨迹在起始图与结束图之间逻辑严密。... | length 1-4000 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |
| audio | BOOLEAN | 是 | true | - | - | audio |

### Vidu-图生视频-q3-turbo

- ID：`2019676917055426562`
- Endpoint：`/openapi/v2/vidu/image-to-video-q3-turbo`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.18 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 将军缓慢举起满是缺口的横刀，雨水打在金属盔甲上产生真实的飞溅效果。随后将军发出一声怒吼，镜头缓慢拉近，由于远处火光的闪烁，将军瞳孔中映射出跳动的火影。动作需保... | length 1-4000 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| audio | BOOLEAN | 是 | true | - | - | audio |

### Vidu-文生视频-q3-turbo

- ID：`2019675673893081089`
- Endpoint：`/openapi/v2/vidu/text-to-video-q3-turbo`
- 类型/分组/来源：text-to-video / Vidu / vidu
- 价格：0.18 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 视觉： 远景：夕阳下的中式科幻城市，霓虹灯逐渐亮起。镜头切换至中景：一位戴着智能眼镜的少年走在热闹的夜市。镜头切换至特写：少年停下脚步，面对镜头微笑并清晰地说... | length 1-4000 | - | prompt |
| style | LIST | 是 | general | - | general, anime | style |
| aspectRatio | LIST | 是 | 16:9 | - | 4:3, 3:4, 16:9, 9:16, 1:1 | aspectRatio |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 | duration |
| audio | BOOLEAN | 是 | true | - | - | 是否使用音视频直出能力 |

### kling-video-o3-pro/reference-to-video

- ID：`2019638479950254081`
- Endpoint：`/openapi/v2/kling-video-o3-pro/reference-to-video`
- 类型/分组/来源：reference-to-video / 可灵 3.0 / rh-ai
- 价格：0.72 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 在暴雨中的赛博朋克小巷，[参考图] 中的流浪武士根据 [参考视频] 的动作轨迹，迅速拔出背后的长刀并完成一个华丽的转身。雨水击打在他那带有独特刻痕的机械左臂上... | length 1-2500 | - | prompt |
| videoUrl | VIDEO | 否 | <示例素材已脱敏> | maxSize 50MB; priceRelated; accept ["MP4"] | - | The video URL. Video duration can not be longer than 10s. |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 50MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| keepOriginalSound | BOOLEAN | 是 | true | - | - | keepOriginalSound |
| sound | BOOLEAN | 否 | false | priceRelated | - | sound |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1 | aspectRatio |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | duration |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | List of multi-prompt elements for the generation. |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | Element reference list |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | - | - | customize, intelligence | 分镜方式 |

### kling-video-o3-std/reference-to-video

- ID：`2019634451799412737`
- Endpoint：`/openapi/v2/kling-video-o3-std/reference-to-video`
- 类型/分组/来源：reference-to-video / 可灵 3.0 / rh-ai
- 价格：0.54 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 把图中的人物融入到视频里 | length 1-2500 | - | prompt |
| videoUrl | VIDEO | 否 | <示例素材已脱敏> | maxSize 50MB; priceRelated; accept ["MP4"] | - | The video URL. Video duration can not be longer than 10s. 720px-2160px |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 50MB; multipleInputs; accept ["JPG","PNG"] | - | 有参考视频时，参考图片数量不得超过4；无参考视频时，参考图片数量不得超过7 |
| keepOriginalSound | BOOLEAN | 是 | true | - | - | keepOriginalSound |
| sound | BOOLEAN | 否 | false | priceRelated | - | sound |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 1:1 | aspectRatio |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | duration |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | List of multi-prompt elements for the generation. |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | - | - | customize, intelligence | 分镜方式 |

### kling-video-o3-pro/video-edit

- ID：`2019630371244937218`
- Endpoint：`/openapi/v2/kling-video-o3-pro/video-edit`
- 类型/分组/来源：video-edit / 可灵 3.0 / rh-ai
- 价格：1.08 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 将视频中男子的衣服替换为图 1 的衣服 | length 1-2500 | - | prompt |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 50MB; priceRelated; accept ["MP4"] | - | The video URL. Video duration can not be longer than 10s. |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 50MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| keepOriginalSound | BOOLEAN | 是 | true | - | - | keepOriginalSound |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | Element reference list |

### kling-video-o3-std/video-edit

- ID：`2019627228264206337`
- Endpoint：`/openapi/v2/kling-video-o3-std/video-edit`
- 类型/分组/来源：video-edit / 可灵 3.0 / rh-ai
- 价格：0.81 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | Change the time to the night. | length 1-2500 | - | prompt |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 50MB; priceRelated; accept ["MP4"] | - | The video URL. Video duration can not be longer than 10s. |
| imageUrls | IMAGE[] | 否 | - | maxSize 50MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| keepOriginalSound | BOOLEAN | 是 | true | priceRelated | - | keepOriginalSound |

### 可灵图生视频o3-pro

- ID：`2019623243725737985`
- Endpoint：`/openapi/v2/kling-video-o3-pro/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 3.0 / rh-ai
- 价格：0.69 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 少女从街道深处款款走，裙摆随步伐自然摆动。镜头伴随她前进，街道两旁的红灯笼随风轻摇。在 10 秒处，她驻足转身，回眸一笑，背景的天空中绽放出绚丽的烟花，光影交... | length 1-2500 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | duration |
| sound | BOOLEAN | 是 | true | priceRelated | - | sound |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | List of multi-prompt elements for the generation. |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | Element reference list |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |

### 可灵图生视频o3-std

- ID：`2019621114621530113`
- Endpoint：`/openapi/v2/kling-video-o3-std/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 3.0 / rh-ai
- 价格：0.52 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 动作描述：探险家缓缓向城市深处走去，风吹过斗篷产生自然的布料物理摆动。随着脚步前进，周围的废墟阴影随光线位置产生实时偏移。探险家发现晶体并驻足，蓝色光芒在头盔... | length 1-2500 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | duration |
| sound | BOOLEAN | 是 | true | priceRelated | - | sound |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | List of multi-prompt elements for the generation. |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |

### 可灵文生视频o3-pro

- ID：`2019620511153459202`
- Endpoint：`/openapi/v2/kling-video-o3-pro/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 3.0 / rh-ai
- 价格：0.69 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 视觉： 一名身着繁复刺绣丝绸长袍的炼金术师。 动作： 炼金术师正缓慢地将一种发光的金色液体从透明琉璃瓶倒入古老的石碗中。液体落入碗中时产生细腻的涟漪和溅射，符... | length 1-2500 | - | prompt |
| aspectRatio | LIST | 否 | 16:9 | - | 1:1, 16:9, 9:16 | aspectRatio |
| sound | BOOLEAN | 是 | true | priceRelated | - | sound |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | duration |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | List of multi-prompt elements for the generation. |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | Element reference list |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |

### 可灵文生视频o3-std

- ID：`2019608799960436737`
- Endpoint：`/openapi/v2/kling-video-o3-std/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 3.0 / rh-ai
- 价格：0.52 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 视觉： 一只色彩斑斓的机械蝴蝶静止在一朵正在缓慢盛开的金属花卉上。 动作： 蝴蝶突然扇动半透明的翅膀飞起，在空中盘旋，翅膀上的光纤纹理随着飞舞变幻颜色。周围空... | length 1-2500 | - | prompt |
| aspectRatio | LIST | 否 | 16:9 | - | 1:1, 16:9, 9:16 | aspectRatio |
| sound | BOOLEAN | 是 | true | priceRelated | - | sound |
| duration | INT | 是 | 5 | range 3-15; step 1; priceRelated | - | duration |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | List of multi-prompt elements for the generation. |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |

### 混元图生3D模型v3.1

- ID：`2019395031670263809`
- Endpoint：`/openapi/v2/hunyuan3d-v3.1/image-to-3d`
- 类型/分组/来源：image-to-3D / 混元 3D / tencent
- 价格：4.2 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| faceCount | INT | 否 | 500000 | range 10000-1500000; step 1 | - | faceCount |
| enablePbr | BOOLEAN | 是 | false | priceRelated | - | enablePbr |
| generateType | LIST | 是 | Normal | priceRelated | Normal, Geometry, Sketch | generateType |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| leftImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | leftImageUrl |
| rightImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | rightImageUrl |
| backImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | backImageUrl |
| topImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | topImageUrl |
| bottomImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | bottomImageUrl |
| leftFrontImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | leftFrontImageUrl |
| rightFrontImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | rightFrontImageUrl |

### 混元文生3D模型v3.1

- ID：`2019394105442111490`
- Endpoint：`/openapi/v2/hunyuan3d-v3.1/text-to-3d`
- 类型/分组/来源：text-to-3D / 混元 3D / tencent
- 价格：1.8 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一个极其精细的工业级科幻侦察无人机。它拥有四个铰接式的旋翼臂，每个臂上都有独立的螺旋桨。机身中央是一个带有玻璃透镜的光学传感器吊舱，周围有裸露的液压管线和金色... | - | - | prompt |
| faceCount | INT | 否 | 500000 | range 10000-1500000; step 1 | - | faceCount |
| enablePbr | BOOLEAN | 是 | false | priceRelated | - | enablePbr |
| generateType | LIST | 是 | Normal | priceRelated | Normal, Geometry, Sketch | generateType |

### 全能视频X-文生视频-低价渠道版-v1.5

- ID：`2019393210805456897`
- Endpoint：`/openapi/v2/rhart-video-g/text-to-video`
- 类型/分组/来源：text-to-video / 全能视频X / rh-ai
- 价格：0.04 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一个微观视角下的森林。一滴巨大的晨露从翠绿的叶尖滑落，正好击中下方平静的水洼，产生扩散的彩色涟漪。涟漪中映射出天空飞过的机械飞鸟。随着镜头上摇，阳光穿透厚重的... | length 5-20000 | - | prompt |
| aspectRatio | LIST | 是 | 2:3 | - | 2:3, 3:2, 1:1, 16:9, 9:16 | aspectRatio |
| resolution | LIST | 是 | 720p | - | 720p, 480p | resolution |
| duration | INT | 是 | 6 | range 6-30; step 1; priceRelated | - | 视频时长（秒），范围6-30秒 |

### 全能视频X-图生视频-低价渠道版-v1.5

- ID：`2019380112598044674`
- Endpoint：`/openapi/v2/rhart-video-g/image-to-video`
- 类型/分组/来源：image-to-video / 全能视频X / rh-ai
- 价格：0.04 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 画面场景固定，可爱小狗在草地上跑动玩球，动作自然灵动，毛发随风轻轻飘动，皮球来回滚动，动态流畅，户外自然光，细节写实。 | length 5-20000 | - | prompt |
| aspectRatio | LIST | 是 | 16:9 | - | 2:3, 3:2, 1:1, 16:9, 9:16 | aspectRatio |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| resolution | LIST | 是 | 480p | - | 720p, 480p | resolution |
| duration | INT | 是 | 6 | range 6-30; step 1; priceRelated | - | 视频时长（秒），范围6-30秒 |

### 可灵图生视频3.0-pro

- ID：`2019246422731591682`
- Endpoint：`/openapi/v2/kling-v3.0-pro/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 3.0 / rh-ai
- 价格：0.69 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 欧洲别墅户外露台场景，铺着蓝白格纹桌布的餐桌旁，年轻白人女性穿蓝白条纹短袖衬衫、卡其色短裤，系棕色腰带，赤脚坐着，对面是穿白色 T 恤的年轻白人男性，镜头推进... | length 1-2500 | - | prompt |
| negativePrompt | STRING | 否 | - | length 0-2500 | - | negativePrompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | - | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 5 | priceRelated | 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | duration |
| cfgScale | FLOAT | 否 | 0.5 | range 0-1; step 0.1; precision 2 | - | cfgScale |
| sound | BOOLEAN | 是 | true | priceRelated | - | sound |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | Additional prompts for complex compositions |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | 参考元素列表 |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |

### 可灵文生视频3.0-pro

- ID：`2019246066681319426`
- Endpoint：`/openapi/v2/kling-v3.0-pro/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 3.0 / rh-ai
- 价格：0.69 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 视觉： 赛博朋克风的雨夜街道，霓虹灯招牌在积水的地面形成流动的倒影。特写镜头对准一名身披半透明雨衣的男子。 动作： 雨滴撞击雨衣并滑落，男子缓缓抬头，瞳孔中清... | length 1-2500 | - | prompt |
| negativePrompt | STRING | 否 | 画面闪烁，模糊，皮肤塑料感，手指畸变，水印，低对比度。 | length 0-2500 | - | negativePrompt |
| duration | LIST | 是 | 5 | priceRelated | 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | duration |
| aspectRatio | LIST | 否 | 16:9 | - | 1:1, 16:9, 9:16 | aspectRatio |
| cfgScale | FLOAT | 否 | 0.5 | range 0-1; step 0.1; precision 2 | - | cfgScale |
| sound | BOOLEAN | 是 | true | priceRelated | - | sound |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | Additional prompts for complex compositions |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |

### 可灵图生视频3.0-std

- ID：`2019243340861870082`
- Endpoint：`/openapi/v2/kling-v3.0-std/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 3.0 / rh-ai
- 价格：0.52 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 动作描述： 探险家缓缓抬头，眼神中充满惊讶。随着他慢慢起身，手中的碎片光芒逐渐加强，并在石门上产生流动的阴影效果。 对话（配音 1）： “它真的被唤醒了……这... | length 1-2500 | - | prompt |
| negativePrompt | STRING | 否 | - | length 0-2500 | - | negativePrompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 5 | priceRelated | 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | duration |
| cfgScale | FLOAT | 否 | 0.8 | range 0-1; step 0.1; precision 2 | - | cfgScale |
| sound | BOOLEAN | 是 | true | priceRelated | - | sound |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | Additional prompts for complex compositions |
| elementList | COMPLEX[] | 否 | - | multipleInputs | - | 参考元素列表 |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |

### 可灵文生视频3.0-std

- ID：`2019233725814214658`
- Endpoint：`/openapi/v2/kling-v3.0-std/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 3.0 / rh-ai
- 价格：0.52 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 视觉： 一名身着战损机甲的女性探险家行走在风沙弥漫的废弃城市。她停下脚步，低头拨弄闪烁蓝光的通讯器。 动作： 风卷起尘埃掠过镜头，通讯器跳动着微弱的火花。 对... | length 1-2500; priceRelated | - | prompt |
| negativePrompt | STRING | 否 | 画面闪烁，模糊，肢体畸变，水印。 | length 0-2500 | - | negativePrompt |
| duration | LIST | 是 | 5 | priceRelated | 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | duration |
| aspectRatio | LIST | 否 | 16:9 | - | 1:1, 16:9, 9:16 | aspectRatio |
| cfgScale | FLOAT | 否 | 0.5 | range 0-1; step 0.1; precision 2 | - | cfgScale |
| sound | BOOLEAN | 是 | true | priceRelated | - | sound |
| multiPrompt | COMPLEX[] | 否 | - | multipleInputs | - | Additional prompts for complex compositions |
| multiShot | BOOLEAN | 否 | false | - | - | 是否生成多镜头视频 |
| shotType | LIST | 否 | customize | - | customize, intelligence | 分镜方式 |

### minimax/speech-2.8-turbo

- ID：`2019027887547813889`
- Endpoint：`/openapi/v2/rhart-audio/text-to-audio/speech-2.8-turbo`
- 类型/分组/来源：text-to-audio / Minmax Hailuo Audio / rh-ai
- 价格：0.37 CNY/字符
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| text | STRING | 是 | 大家好！欢迎来到 MiniMax (发音：Mini-Max) 科技频道。 今天我们要聊聊这款超酷的 Speech 2.8 Turbo。它甚至能听出我的疲惫，比... | priceRelated | - | Supported interjections: (laughs), (chuckle), (coughs), (clear-throat), (groans), (breath), (pant), (inhale), (exhale),... |
| pronunciation_dict | STRING[] | 否 | ASAP/As soon as possible | multipleInputs | - | pronunciation_dict |
| voice_id | STRING | 是 | Elegant_Man | - | - | system voice IDs: Wise_Woman, Friendly_Person, Inspirational_girl, Deep_Voice_Man, Calm_Woman, Casual_Guy, Lively_Girl,... |
| speed | FLOAT | 否 | 1 | range 0.5-2; step 0.01; precision 2 | - | speed |
| volume | FLOAT | 否 | 1 | range 0.1-10; step 0.01; precision 2 | - | volume |
| pitch | INT | 否 | 0 | range -12-12; step 1 | - | pitch |
| emotion | LIST | 否 | happy | - | happy, sad, angry, fearful, disgusted, surprised, neutral | emotion |
| enable_base64_output | BOOLEAN | 是 | false | - | - | enable_base64_output |
| english_normalization | BOOLEAN | 是 | false | - | - | english_normalization |

### minimax/speech-2.6-turbo

- ID：`2019020691690819585`
- Endpoint：`/openapi/v2/rhart-audio/text-to-audio/speech-2.6-turbo`
- 类型/分组/来源：text-to-audio / Minmax Hailuo Audio / rh-ai
- 价格：0.37 CNY/字符
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| text | STRING | 是 | - | priceRelated | - | Supported interjections: (laughs), (chuckle), (coughs), (clear-throat), (groans), (breath), (pant), (inhale), (exhale),... |
| pronunciation_dict | STRING[] | 否 | ASAP/As soon as possible | multipleInputs | - | pronunciation_dict |
| voice_id | STRING | 是 | Wise_Woman | - | - | system voice IDs: Wise_Woman, Friendly_Person, Inspirational_girl, Deep_Voice_Man, Calm_Woman, Casual_Guy, Lively_Girl,... |
| speed | FLOAT | 否 | 1 | range 0.5-2; step 0.01; precision 2 | - | speed |
| volume | FLOAT | 否 | 1 | range 0.1-10; step 0.01; precision 2 | - | volume |
| pitch | INT | 否 | 0 | range -12-12; step 1 | - | pitch |
| emotion | LIST | 否 | happy | - | happy, sad, angry, fearful, disgusted, surprised, neutral | emotion |
| enable_base64_output | BOOLEAN | 是 | false | - | - | enable_base64_output |
| english_normalization | BOOLEAN | 是 | false | - | - | english_normalization |

### minimax/speech-2.6-hd

- ID：`2019020262185701377`
- Endpoint：`/openapi/v2/rhart-audio/text-to-audio/speech-2.6-hd`
- 类型/分组/来源：text-to-audio / Minmax Hailuo Audio / rh-ai
- 价格：0.62 CNY/字符
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| text | STRING | 是 | 我们的 Speech 2.6 HD 模型现已支持超过 40 种语言。 比如，当我说到“第 12,800 个并发节点”时，它的吐字依然如此清晰。归一化升级带来的... | priceRelated | - | Supported interjections: (laughs), (chuckle), (coughs), (clear-throat), (groans), (breath), (pant), (inhale), (exhale),... |
| pronunciation_dict | STRING[] | 否 | ASAP/As soon as possible | multipleInputs | - | pronunciation_dict |
| voice_id | STRING | 是 | Wise_Woman | - | - | system voice IDs: Wise_Woman, Friendly_Person, Inspirational_girl, Deep_Voice_Man, Calm_Woman, Casual_Guy, Lively_Girl,... |
| speed | FLOAT | 否 | 1 | range 0.5-2; step 0.01; precision 2 | - | speed |
| volume | FLOAT | 否 | 1 | range 0.1-10; step 0.01; precision 2 | - | volume |
| pitch | INT | 否 | 0 | range -12-12; step 1 | - | pitch |
| emotion | LIST | 否 | happy | - | happy, sad, angry, fearful, disgusted, surprised, neutral | emotion |
| enable_base64_output | BOOLEAN | 是 | false | - | - | enable_base64_output |
| english_normalization | BOOLEAN | 是 | false | - | - | english_normalization |

### minimax/speech-02-turbo

- ID：`2018996840906952706`
- Endpoint：`/openapi/v2/rhart-audio/text-to-audio/speech-02-turbo`
- 类型/分组/来源：text-to-audio / Minmax Hailuo Audio / rh-ai
- 价格：0.19 CNY/字符
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| text | STRING | 是 | 嘿！准备好开启全球声音之旅了吗？我们的 Speech-02-Turbo 即使在处理小语种时，依然能保持出色的韵律感。 | priceRelated | - | Supported interjections: (laughs), (chuckle), (coughs), (clear-throat), (groans), (breath), (pant), (inhale), (exhale),... |
| pronunciation_dict | STRING[] | 否 | ASAP/As soon as possible | multipleInputs | - | pronunciation_dict |
| voice_id | STRING | 是 | Energetic_Girl | - | - | system voice IDs: Wise_Woman, Friendly_Person, Inspirational_girl, Deep_Voice_Man, Calm_Woman, Casual_Guy, Lively_Girl,... |
| speed | FLOAT | 否 | 1 | range 0.5-2; step 0.01; precision 2 | - | speed |
| volume | FLOAT | 否 | 1 | range 0.1-10; step 0.01; precision 2 | - | volume |
| pitch | INT | 否 | 0 | range -12-12; step 1 | - | pitch |
| emotion | LIST | 否 | happy | - | happy, sad, angry, fearful, disgusted, surprised, neutral | emotion |
| enable_base64_output | BOOLEAN | 是 | false | - | - | enable_base64_output |
| english_normalization | BOOLEAN | 是 | false | - | - | english_normalization |

### minimax/speech-02-hd

- ID：`2018994703099564034`
- Endpoint：`/openapi/v2/rhart-audio/text-to-audio/speech-02-hd`
- 类型/分组/来源：text-to-audio / Minmax Hailuo Audio / rh-ai
- 价格：0.31 CNY/字符
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| text | STRING | 是 | 在这个追求极致的时代，声音的稳定性是专业创作的基石。 哪怕是处理超过 5,000 字的复杂剧本，它的表现依旧如一。 感受到了吗？这种极高的音色复刻相似度，让每... | priceRelated | - | Supported interjections: (laughs), (chuckle), (coughs), (clear-throat), (groans), (breath), (pant), (inhale), (exhale),... |
| pronunciation_dict | STRING[] | 否 | ASAP/As soon as possible | multipleInputs | - | pronunciation_dict |
| voice_id | STRING | 是 | Wise_Woman | - | - | system voice IDs: Wise_Woman, Friendly_Person, Inspirational_girl, Deep_Voice_Man, Calm_Woman, Casual_Guy, Lively_Girl,... |
| speed | FLOAT | 否 | 1 | range 0.5-2; step 0.01; precision 2 | - | speed |
| volume | FLOAT | 否 | 1 | range 0.1-10; step 0.01; precision 2 | - | volume |
| pitch | INT | 否 | 0 | range -12-12; step 1 | - | pitch |
| emotion | LIST | 否 | happy | - | happy, sad, angry, fearful, disgusted, surprised, neutral | emotion |
| enable_base64_output | BOOLEAN | 是 | false | - | - | enable_base64_output |
| english_normalization | BOOLEAN | 是 | false | - | - | english_normalization |

### minimax/speech-2.8-hd

- ID：`2018969332367036417`
- Endpoint：`/openapi/v2/rhart-audio/text-to-audio/speech-2.8-hd`
- 类型/分组/来源：text-to-audio / Minmax Hailuo Audio / rh-ai
- 价格：0.62 CNY/字符
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| text | STRING | 是 | Bonjour! How are you today? 今天过得怎么样？Me encanta este lugar — it’s so peaceful. ¿... | priceRelated | - | Supported interjections: (laughs), (chuckle), (coughs), (clear-throat), (groans), (breath), (pant), (inhale), (exhale),... |
| pronunciation_dict | STRING[] | 否 | ASAP/As soon as possible | multipleInputs | - | pronunciation_dict |
| voice_id | STRING | 是 | Wise_Woman | - | - | system voice IDs: Wise_Woman, Friendly_Person, Inspirational_girl, Deep_Voice_Man, Calm_Woman, Casual_Guy, Lively_Girl,... |
| speed | FLOAT | 否 | 1 | range 0.5-2; step 0.01; precision 2 | - | speed |
| volume | FLOAT | 否 | 1 | range 0.1-10; step 0.01; precision 2 | - | volume |
| pitch | INT | 否 | 0 | range -12-12; step 1 | - | pitch |
| emotion | LIST | 否 | happy | - | happy, sad, angry, fearful, disgusted, surprised, neutral | emotion |
| enable_base64_output | BOOLEAN | 是 | false | - | - | enable_base64_output |
| english_normalization | BOOLEAN | 是 | false | - | - | english_normalization |

### Vidu-首尾帧生视频-q2-pro-fast

- ID：`2018603743257628674`
- Endpoint：`/openapi/v2/vidu/start-end-to-video-q2-pro-fast`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.18 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 根据提供的首帧与尾帧，生成一段连贯视频。要求平滑补全抬手、取墨镜、佩戴、抬头的完整肢体动作，全程保持面部特征稳定无偏移。背景行人自然流动，光影随动作和时间产生... | length 1-4000 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p | resolution |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |
| bgm | BOOLEAN | 是 | true | - | - | bgm |

### Vidu-图生视频-q2-pro-fast

- ID：`2018603399320506370`
- Endpoint：`/openapi/v2/vidu/image-to-video-q2-pro-fast`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.18 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 电影感特写，画面中的女性缓缓转头并对着镜头微笑，她的手轻轻拂过发丝，背景中的城市霓虹灯伴随镜头推入（Push-in）产生柔和的焦外虚化。动作自然流畅，保持面部... | length 1-4000 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p | resolution |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |
| bgm | BOOLEAN | 是 | true | - | - | bgm |

### 全能视频V3.1-pro-首尾帧生视频-低价渠道版

- ID：`2018599147311271938`
- Endpoint：`/openapi/v2/rhart-video-v3.1-pro/start-end-to-video`
- 类型/分组/来源：image-to-video / 全能视频V / google
- 价格：0.9 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 首尾帧插值测试。起始帧：一名乐手在雨中街道架起小提琴；结束帧：乐手闭眼沉浸在演奏中，背景霓虹灯光拉成绚丽的流光。要求：模型需补全乐手抬起琴弓并开始拉奏的动作，... | length 5-8000 | - | prompt |
| firstFrameUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | firstFrameUrl |
| lastFrameUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | lastFrameUrl |
| aspectRatio | LIST | 是 | 9:16 | - | 16:9, 9:16 | aspectRatio |
| duration | LIST | 否 | 8 | - | 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p, 4k | resolution |

### 全能视频V3.1-pro-图生视频-低价渠道版

- ID：`2018518935961669634`
- Endpoint：`/openapi/v2/rhart-video-v3.1-pro/image-to-video`
- 类型/分组/来源：image-to-video / 全能视频V / google
- 价格：0.8 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 人物摇晃酒杯的视频 | length 5-8000 | - | prompt |
| aspectRatio | LIST | 是 | 16:9 | - | 16:9, 9:16 | aspectRatio |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 否 | 8 | - | 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p, 4k | resolution |

### Vidu-图生视频-q3-pro

- ID：`2017174622010937346`
- Endpoint：`/openapi/v2/vidu/image-to-video-q3-pro`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.31 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 轻柔的女声，语速缓慢清晰，用韩语说：영화는 세상에 온기를 주며, 관객들이 빛과 그림자가 교차하는 사이에서 타인의 삶에 공감할 수 있게 합니다. | length 1-4000 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 360p, 540p, 720p, 1080p, 2k | resolution |
| audio | BOOLEAN | 是 | true | priceRelated | - | audio |

### Vidu-文生视频-q3-pro

- ID：`2017148354741735426`
- Endpoint：`/openapi/v2/vidu/text-to-video-q3-pro`
- 类型/分组/来源：text-to-video / Vidu / vidu
- 价格：0.31 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 分镜 1（远景）： 深夜的明朝古镇街道，大雨滂沱，方文身穿黑色长袍，孤身一人走在青石板路上。伴随沉重的雨声。 分镜 2（中景/背后跟随）： 镜头紧跟方文的背影... | length 1-4000 | - | prompt |
| style | LIST | 是 | general | - | general, anime | style |
| aspectRatio | LIST | 是 | 16:9 | - | 4:3, 3:4, 16:9, 9:16, 1:1 | aspectRatio |
| resolution | LIST | 是 | 720p | priceRelated | 360p, 540p, 720p, 1080p | resolution |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 | duration |
| audio | BOOLEAN | 是 | true | priceRelated | - | 是否使用音视频直出能力 |

### 悠船文生图-v7

- ID：`2016834293520994306`
- Endpoint：`/openapi/v2/youchuan/text-to-image-v7`
- 类型/分组/来源：text-to-image / 悠船 AI 绘图 / rh-ai
- 价格：0.54 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 迷雾古老森林中的生物发光白鹿，柔和的微光颗粒，电影级光影，超写实纹理 | length 1-8192 | - | prompt |
| negativePrompt | STRING | 否 | - | length --2000 | - | negativePrompt |
| chaos | INT | 否 | 0 | range 0-100; step 1 | - | 混沌参数，为图像结果增添趣味 (0-100) |
| quality | LIST | 否 | 1 | - | 1, 2, 4 | 图像质量 |
| stylize | INT | 否 | 0 | range 0-1000; step 1 | - | 风格化参数，控制图像的艺术风格强度（0-1000） |
| weird | INT | 否 | 0 | range 0-3000; step 1 | - | 怪异参数，控制图像的非常规和古怪程度（0-3000） |
| raw | BOOLEAN | 否 | false | - | - | 原始模式，获得对图像的更多控制 |
| imageUrl | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 输入图片URL（垫图） |
| iw | INT | 否 | 1 | range 0-3; step 1 | - | 图像权重，控制图像提示的影响 |
| sref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 风格参考图片URL |
| sw | INT | 否 | 100 | range 0-1000; step 1 | - | 风格权重，控制风格参考的影响（0-1000，需搭配sref使用） |
| sv | INT | 否 | 4 | range 1-6; step 1 | - | 风格版本 |
| oref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG"] | - | 万物引用图片URL |
| ow | INT | 否 | 100 | range 1-1000; step 1 | - | 万物引用权重，控制万物引用的影响（1-1000，需搭配oref使用） |
| tile | BOOLEAN | 否 | false | - | - | 平铺参数，创建无缝重复图案 |
| aspectRatio | LIST | 否 | - | - | 1:1, 4:3, 3:2, 16:9, 3:4, 2:3, 9:16 | aspectRatio |

### 悠船文生图-niji7

- ID：`2016833484322312193`
- Endpoint：`/openapi/v2/youchuan/text-to-image-niji7`
- 类型/分组/来源：text-to-image / 悠船 AI 绘图 / rh-ai
- 价格：0.54 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 电影级广角镜头，一位未来赛博朋克武士在湿漉漉的霓虹街道上与机械巨龙战斗，低视角动态透视，复杂的机甲细节，剧场版构图，锐利焦距，浮世绘与现代动漫风格融合，插画海... | length 1-8192 | - | prompt |
| chaos | INT | 否 | 0 | range 0-100; step 1 | - | 混沌参数，为图像结果增添趣味 (0-100) |
| stylize | INT | 否 | 0 | range 0-1000; step 1 | - | 风格化参数，控制图像的艺术风格强度（0-1000） |
| weird | INT | 否 | 0 | range 0-3000; step 1 | - | 怪异参数，控制图像的非常规和古怪程度（0-3000） |
| raw | BOOLEAN | 否 | false | - | - | 原始模式，获得对图像的更多控制 |
| imageUrl | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 输入图片URL（垫图） |
| iw | INT | 否 | 1 | range 0-2; step 1 | - | 图像权重，控制图像提示的影响，0-2 |
| sref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 风格参考图片URL |
| sw | INT | 否 | 100 | range 0-1000; step 1 | - | 风格权重，控制风格参考的影响（0-1000，需搭配sref使用） |
| sv | INT | 否 | 4 | range 1-4; step 1 | - | 风格版本 |
| aspectRatio | LIST | 否 | 1:1 | - | 1:1, 4:3, 3:2, 16:9, 3:4, 2:3, 9:16 | aspectRatio |

### 悠船文生图-niji6

- ID：`2016832698641092610`
- Endpoint：`/openapi/v2/youchuan/text-to-image-niji6`
- 类型/分组/来源：text-to-image / 悠船 AI 绘图 / rh-ai
- 价格：0.54 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一名年轻少女在日落时分站在火车站窗边，樱花随风飘落，戏剧性的动漫光影，丁达尔效应，平涂色彩，赛璐璐风格，干净的线条，90年代复古动漫画风，极高细节的场景。 | length 1-8192 | - | prompt |
| chaos | INT | 否 | 0 | range 0-100; step 1 | - | 混沌参数，为图像结果增添趣味 (0-100) |
| quality | LIST | 否 | 1 | - | 0.5, 1, 2 | 图像质量 |
| stylize | INT | 否 | 0 | range 0-1000; step 1 | - | 风格化参数，控制图像的艺术风格强度（0-1000） |
| weird | INT | 否 | 0 | range 0-3000; step 1 | - | 怪异参数，控制图像的非常规和古怪程度（0-3000） |
| raw | BOOLEAN | 否 | false | - | - | 原始模式，获得对图像的更多控制 |
| imageUrl | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 输入图片URL（垫图） |
| iw | INT | 否 | 1 | range 0-3; step 1 | - | 图像权重，控制图像提示的影响 |
| cref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 角色参考图片URL |
| cw | INT | 否 | 100 | range 0-100; step 1 | - | 角色权重，控制角色参考的影响（0-100，需搭配cref使用） |
| sref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 风格参考图片URL |
| sw | INT | 否 | 100 | range 0-1000; step 1 | - | 风格权重，控制风格参考的影响（0-1000，需搭配sref使用） |
| sv | INT | 否 | 4 | range 1-4; step 1 | - | 风格版本 |
| stop | INT | 否 | 100 | range 10-100; step 1 | - | 停止参数，在半途完成图像以获得更柔和或独特的外观（10-100） |
| tile | BOOLEAN | 否 | false | - | - | 平铺参数，创建无缝重复图案 |
| aspectRatio | LIST | 否 | 1:1 | - | 1:1, 4:3, 3:2, 16:9, 3:4, 2:3, 9:16 | aspectRatio |

### 悠船文生图-v61

- ID：`2016831146606006273`
- Endpoint：`/openapi/v2/youchuan/text-to-image-v61`
- 类型/分组/来源：text-to-image / 悠船 AI 绘图 / rh-ai
- 价格：0.54 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 日落时分广阔的电影感荒野，一位孤独的旅行者走向巨大的废弃飞船，橙色尘埃在风中盘旋，戏剧性的镜头光晕，超写实的末日废土美学。 | length 1-8192 | - | prompt |
| negativePrompt | STRING | 否 | - | length --2000 | - | negativePrompt |
| chaos | INT | 否 | 0 | range 0-100; step 1 | - | 混沌参数，为图像结果增添趣味 (0-100) |
| quality | LIST | 否 | 1 | - | 0.5, 1, 2 | 图像质量 |
| stylize | INT | 否 | 0 | range 0-1000; step 1 | - | 风格化参数，控制图像的艺术风格强度（0-1000） |
| weird | INT | 否 | 0 | range 0-3000; step 1 | - | 怪异参数，控制图像的非常规和古怪程度（0-3000） |
| raw | BOOLEAN | 否 | false | - | - | 原始模式，获得对图像的更多控制 |
| imageUrl | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 输入图片URL（垫图） |
| iw | INT | 否 | 1 | range 0-3; step 1 | - | 图像权重，控制图像提示的影响 |
| cref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 角色参考图片URL |
| cw | INT | 否 | 100 | range 0-100; step 1 | - | 角色权重，控制角色参考的影响（0-100，需搭配cref使用） |
| sref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 风格参考图片URL |
| sw | INT | 否 | 100 | range 0-1000; step 1 | - | 风格权重，控制风格参考的影响（0-1000，需搭配sref使用） |
| sv | INT | 否 | 4 | range 1-4; step 1 | - | 风格版本 |
| stop | INT | 否 | 100 | range 10-100; step 1 | - | 停止参数，在半途完成图像以获得更柔和或独特的外观（10-100） |
| tile | BOOLEAN | 否 | false | - | - | 平铺参数，创建无缝重复图案 |
| aspectRatio | LIST | 否 | - | - | 1:1, 4:3, 3:2, 16:9, 3:4, 2:3, 9:16 | aspectRatio |

### 悠船文生图-v6

- ID：`2016813986152255489`
- Endpoint：`/openapi/v2/youchuan/text-to-image-v6`
- 类型/分组/来源：text-to-image / 悠船 AI 绘图 / rh-ai
- 价格：0.54 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 夜晚的电影感赛博朋克街道，霓虹灯招牌倒映在水洼中，雨水以慢动作落下，超写实纹理。 | length 1-8192 | - | prompt |
| chaos | INT | 否 | 0 | range 0-100; step 1 | - | 混沌参数，为图像结果增添趣味 (0-100) |
| quality | LIST | 否 | 1 | - | 0.5, 1, 2 | 图像质量 |
| stylize | INT | 否 | 0 | range 0-1000; step 1 | - | 风格化参数，控制图像的艺术风格强度（0-1000） |
| weird | INT | 否 | 0 | range 0-3000; step 1 | - | 怪异参数，控制图像的非常规和古怪程度（0-3000） |
| raw | BOOLEAN | 否 | false | - | - | 原始模式，获得对图像的更多控制 |
| imageUrl | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 输入图片URL（垫图） |
| iw | INT | 否 | 1 | range 0-3; step 1 | - | 图像权重，控制图像提示的影响 |
| cref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 角色参考图片URL |
| cw | INT | 否 | 100 | range 0-100; step 1 | - | 角色权重，控制角色参考的影响（0-100，需搭配cref使用） |
| sref | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 风格参考图片URL |
| sw | INT | 否 | 100 | range 0-1000; step 1 | - | 风格权重，控制风格参考的影响（0-1000，需搭配sref使用） |
| sv | INT | 否 | 4 | range 1-4; step 1 | - | 风格版本 |
| stop | INT | 否 | 100 | range 10-100; step 1 | - | 停止参数，在半途完成图像以获得更柔和或独特的外观（10-100） |
| tile | BOOLEAN | 否 | false | - | - | 平铺参数，创建无缝重复图案 |
| aspectRatio | LIST | 否 | 1:1 | - | 1:1, 4:3, 3:2, 16:9, 3:4, 2:3, 9:16 | aspectRatio |

### 悠船图生视频

- ID：`2016785492810731522`
- Endpoint：`/openapi/v2/youchuan/image-to-video`
- 类型/分组/来源：image-to-video / 悠船 AI 视频 / rh-ai
- 价格：0.54 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=100

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 平滑的魔法过渡：法术书封面自然抬起，书页翻转并向上飘浮，金色光粒子动态旋转增强，符文亮度逐渐增强，从闭合到展开的无缝形态变化，电影级动态效果，流畅的延时动画 | length 1-8192 | - | 提示词，描述要生成的视频内容 |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 首帧图片URL |
| lastImageUrl | IMAGE | 否 | - | maxSize 20MB; accept ["JPG","PNG","WEBP"] | - | 尾帧图片URL（将作为--end参数） |
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p | resolution |
| motion | LIST | 否 | low | - | low, high | motion |
| raw | BOOLEAN | 否 | true | - | - | raw |
| loop | BOOLEAN | 否 | false | - | - | 生成首尾帧一样的视频 |

### Vidu-参考生视频-q2-pro

- ID：`2016409516394209282`
- Endpoint：`/openapi/v2/vidu/reference-to-video-q2-pro`
- 类型/分组/来源：reference-to-video / Vidu / vidu
- 价格：0.44 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 让 @图1 的人物复刻 @视频1 的场景 | length --2000 | - | prompt |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 50MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| videos | VIDEO[] | 否 | <示例素材已脱敏> | maxSize 100MB; multipleInputs; accept ["MP4","AVI","MOV"] | - | videos |
| aspectRatio | LIST | 否 | 16:9 | - | 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| resolution | LIST | 否 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| duration | LIST | 否 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 | duration |
| movementAmplitude | LIST | 否 | auto | - | auto, small, medium, large | movementAmplitude |

### 全能视频V3.1-fast-首尾帧生视频-低价渠道版

- ID：`2016052223404204034`
- Endpoint：`/openapi/v2/rhart-video-v3.1-fast/start-end-to-video`
- 类型/分组/来源：image-to-video / 全能视频V / google
- 价格：1.5 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 无缝衔接首尾帧，呈现物理驱动的跑酷飞跃：爆发起跳→滞空顶点（身体舒展）→受控下落（含风阻细节）。核心要求： 流畅动态模糊 + 影子自然位移 角色全程一致性（面... | length 5-8000 | - | prompt |
| firstFrameUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | firstFrameUrl |
| lastFrameUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | lastFrameUrl |
| aspectRatio | LIST | 是 | 9:16 | - | 16:9, 9:16 | aspectRatio |
| duration | LIST | 否 | 8 | - | 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p, 4k | resolution |

### 全能图片PRO-图生图Ultra-官方稳定版

- ID：`2015599839481749506`
- Endpoint：`/openapi/v2/rhart-image-n-pro-official/edit-ultra`
- 类型/分组/来源：image-to-image / 全能图片 / google
- 价格：0.98 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| prompt | STRING | 是 | 将人类主体替换为一只穿着白色实验服、戴着圆框眼镜的高智商黑猩猩。修改平板电脑屏幕，使其显示一幅精细的香蕉树植物插图，并配以简洁的无衬线字体文字“Organic... | length 1-20000 | - | prompt |
| resolution | LIST | 是 | 8k | priceRelated | 4k, 8k | resolution |
| aspectRatio | LIST | 否 | 3:4 | - | 1:1, 3:2, 2:3, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9 | aspectRatio |

### 全能图片PRO-文生图Ultra-官方稳定版

- ID：`2015599191101071361`
- Endpoint：`/openapi/v2/rhart-image-n-pro-official/text-to-image-ultra`
- 类型/分组/来源：text-to-image / 全能图片 / google
- 价格：0.98 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一只身穿精致丝绸长袍、气质高贵的金丝猴正坐在传统的茶馆里，手里端着一个精巧的陶瓷茶杯。木桌上放着一根小巧、熟透的香蕉，上面贴着一个发光的全息标签。采用 50m... | length 1-20000 | - | prompt |
| resolution | LIST | 是 | 8k | priceRelated | 4k, 8k | resolution |
| aspectRatio | LIST | 否 | 3:4 | - | 1:1, 3:2, 2:3, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9 | aspectRatio |

### seedance-v1-lite-reference-to-video

- ID：`2014265134358548482`
- Endpoint：`/openapi/v2/seedance-v1-lite/reference-to-video`
- 类型/分组/来源：reference-to-video / Seedance / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一个温馨的电影级镜头，参考图中的年轻女性正坐在草坪上，与参考图中的黄金猎犬一起玩耍。女性开心地大笑，同时温柔地轻拍狗狗的头；她的面部特征和服装保持完美。狗狗兴... | length 1-5000 | - | prompt |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| duration | LIST | 是 | 5 | priceRelated | 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 | duration |
| cameraFixed | LIST | 是 | false | - | false, true | cameraFixed |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 480p | resolution |
| aspectRatio | LIST | 是 | 16:9 | - | 16:9, 9:16, 4:3, 3:4, 21:9, 1:1 | aspectRatio |

### seedance-v1.5-pro-image-to-video-fast

- ID：`2014264198047289346`
- Endpoint：`/openapi/v2/seedance-v1.5-pro/image-to-video-fast`
- 类型/分组/来源：image-to-video / Seedance / rh-ai
- 价格：0.16 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 基于参考图，人物身份和服装保持完美一致。摄像机执行缓慢且具有电影感的环绕拍摄（Orbit shot）。人物缓慢眨眼，露出一抹细微、自信的微笑，同时发丝在微风中... | length 1-5000 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | lastImageUrl |
| aspectRatio | LIST | 是 | adaptive | - | 16:9, 9:16, 1:1, 4:3, 3:4, 21:9, adaptive | aspectRatio |
| duration | LIST | 是 | 5 | priceRelated | 4, 5, 6, 7, 8, 9, 10, 11, 12 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p | resolution |
| generateAudio | LIST | 是 | true | priceRelated | true, false | generateAudio |
| cameraFixed | LIST | 是 | false | - | false, true | cameraFixed |

### seedance-v1.5-pro-image-to-video

- ID：`2014263882631434241`
- Endpoint：`/openapi/v2/seedance-v1.5-pro/image-to-video`
- 类型/分组/来源：image-to-video / Seedance / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 基于参考图，豪华香水瓶保持在中心位置。摄像机执行缓慢且具有电影感的推近镜头（Dolly-in），揭示出精致的玻璃纹理和反射。瓶内的液体中闪烁着细微的焦散光影涟... | length 1-5000 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | lastImageUrl |
| aspectRatio | LIST | 是 | adaptive | - | 16:9, 9:16, 1:1, 4:3, 3:4, 21:9, adaptive | aspectRatio |
| duration | LIST | 是 | 5 | priceRelated | 4, 5, 6, 7, 8, 9, 10, 11, 12 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, 1080p | resolution |
| generateAudio | LIST | 是 | true | priceRelated | true, false | generateAudio |
| cameraFixed | LIST | 是 | false | - | false, true | cameraFixed |

### seedance-v1.5-pro-text-to-video-fast

- ID：`2014263510567309314`
- Endpoint：`/openapi/v2/seedance-v1.5-pro/text-to-video-fast`
- 类型/分组/来源：text-to-video / Seedance / rh-ai
- 价格：0.16 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一段电影级高速追踪镜头，跟拍一架流线型银色无人机在夜晚穿梭于生物发光森林。空气中漂浮着霓虹孢子，随着无人机的气流扰动产生真实的旋动反馈。无人机的推进器发出柔和... | length 1-5000 | - | prompt |
| aspectRatio | LIST | 是 | 16:9 | - | 16:9, 9:16, 4:3, 3:4, 1:1, 21:9 | aspectRatio |
| duration | LIST | 是 | 5 | priceRelated | 4, 5, 6, 7, 8, 9, 10, 11, 12 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p | resolution |
| generateAudio | LIST | 是 | true | priceRelated | true, false | generateAudio |
| cameraFixed | LIST | 是 | false | - | false, true | cameraFixed |

### seedance-v1.5-pro-text-to-video

- ID：`2014262901541785601`
- Endpoint：`/openapi/v2/seedance-v1.5-pro/text-to-video`
- 类型/分组/来源：text-to-video / Seedance / rh-ai
- 价格：0.07 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 电影级中景镜头。一名眼神坚毅的女侦探站在夜晚下雨街道的闪烁霓虹灯下。她缓慢抬起头注视镜头，眼中充满悲伤与决心的复杂情感，一颗泪珠顺着脸颊滑落。她湿润的头发贴在... | length 1-5000 | - | prompt |
| aspectRatio | LIST | 是 | 3:4 | - | 16:9, 9:16, 4:3, 3:4, 1:1, 21:9 | aspectRatio |
| duration | LIST | 是 | 5 | priceRelated | 4, 5, 6, 7, 8, 9, 10, 11, 12 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 480p, 720p, 1080p | resolution |
| generateAudio | LIST | 是 | true | priceRelated | true, false | generateAudio |
| cameraFixed | LIST | 是 | false | - | false, true | cameraFixed |

### Vidu-参考生视频-q2

- ID：`2013851110357684225`
- Endpoint：`/openapi/v2/vidu/reference-to-video-q2`
- 类型/分组/来源：reference-to-video / Vidu / vidu
- 价格：0.33 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 基于参考图生成电影级人像短视频，精准还原角色面部特征与光影氛围一致性；启用微表情还原功能，呈现自然的眨眼动作、眼球缓慢转动轨迹及颈部呼吸起伏微动；运动幅度设置... | length 1-4000 | - | prompt |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 50MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| aspectRatio | LIST | 是 | 3:4 | - | 16:9, 9:16, 4:3, 3:4, 1:1 | aspectRatio |
| resolution | LIST | 是 | 1080p | priceRelated | 540p, 720p, 1080p | resolution |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 | duration |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |

### Vidu-图生视频-q2-turbo

- ID：`2013835030981595138`
- Endpoint：`/openapi/v2/vidu/image-to-video-q2-turbo`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.13 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 基于原图生成电影级短视频；镜头以手表为中心缓慢环绕移动，呈现真实空间位移感；表带随镜头角度轻微晃动，表盘指针匀速微动，背景光影自然渐变；消除画面闪烁；全程精准... | length 1-4000 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |
| bgm | BOOLEAN | 是 | true | - | - | bgm |

### Vidu-图生视频-q2-pro

- ID：`2013834337629589505`
- Endpoint：`/openapi/v2/vidu/image-to-video-q2-pro`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.18 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 基于原图生成电影感短视频，镜头缓慢从近景推拉至远景，呈现自然景深变化；少女发丝随微风轻拂飘动，竹叶轻轻摇曳，溪流泛起细微涟漪；全程保留原图面部特征、发丝纹理、... | length 1-4000 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |
| bgm | BOOLEAN | 是 | true | - | - | bgm |

### 万相2.6-图生视频Flash

- ID：`2013140888253165569`
- Endpoint：`/openapi/v2/alibaba/wan-2.6/image-to-video-flash`
- 类型/分组/来源：image-to-video / Wan Video Models / wan
- 价格：0.11 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 基于原图，将这个神秘森林场景转化动画。动作： 一只发光的半透明鹿轻轻低下头从清澈见底的池塘饮水，产生真实的扩散圆周涟漪。当它饮水时，一层柔和的魔法雾气开始从水... | length --- | - | prompt |
| negativePrompt | STRING | 否 | - | length --- | - | negativePrompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| audioUrl | AUDIO | 否 | - | maxSize 10MB; accept ["MP3","WAV"] | - | audioUrl |
| resolution | LIST | 是 | 1080p | priceRelated | 720p, 1080p | resolution |
| duration | LIST | 是 | 5 | priceRelated | 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 | duration |
| shotType | LIST | 是 | single | - | single, multi | shotType |
| enablePromptExpansion | BOOLEAN | 是 | false | - | - | enablePromptExpansion |
| enableAudio | BOOLEAN | 是 | true | priceRelated | - | enableAudio |

### 可灵图生视频2.6-pro

- ID：`2013097204925132801`
- Endpoint：`/openapi/v2/kling-v2.6-pro/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 / rh-ai
- 价格：1.75 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 基于原图。摄像机执行平滑的电影级推近镜头，聚焦于角色的面部。 音频： 一段深沉、宏亮的大提琴旋律。 | length 1-2000 | - | prompt |
| negativePrompt | STRING | 否 | - | length --2500 | - | negativePrompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | imageUrl |
| sound | LIST | 是 | true | priceRelated | true, false | sound |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |

### 可灵文生视频2.6-pro

- ID：`2013088838681161729`
- Endpoint：`/openapi/v2/kling-v2.6-pro/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 / rh-ai
- 价格：1.75 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一个高科技机器人厨师在下雨的赛博朋克街头市场炒面的特写。镜头从滋滋作响的炒锅缓慢平移到机器人发光的感应器。音效： 炒锅发出响亮且有节奏的滋滋声和爆裂声，金属铲... | length --2000 | - | prompt |
| negativePrompt | STRING | 否 | - | length --2500 | - | negativePrompt |
| sound | LIST | 是 | true | priceRelated | true, false | sound |
| aspectRatio | LIST | 是 | 9:16 | - | 1:1, 16:9, 9:16 | aspectRatio |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |

### seedream-v4-图生图

- ID：`2013062293031809026`
- Endpoint：`/openapi/v2/seedream-v4/image-to-image`
- 类型/分组/来源：image-to-image / Seedream / rh-ai
- 价格：0.14 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | [动作：更换服装] 针对 [对象：参考图中的女性模特]，将其服装替换为 [目标特征：一件带有精致金色刺绣的高级祖母绿丝绸晚礼服]。[约束：保持主体身份、面部特... | length 5-2000 | - | prompt |
| width | INT | 否 | 2048 | range 512-8192; step 8 | - | width |
| height | INT | 否 | 2048 | range 512-8192; step 1 | - | height |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["PNG","JPEG"] | - | imageUrls |
| sequentialImageGeneration | LIST | 否 | disabled | - | disabled, auto | sequentialImageGeneration |
| maxImages | INT | 否 | 1 | range 1-15; step 1; priceRelated | - | maxImages |
| resolution | LIST | 否 | - | - | 1k, 2k, 4k | 优先级高于widthxheight，传递resolution则使用resolution，不再使用widthxheight |

### seedream-v4-文生图

- ID：`2013061949732220929`
- Endpoint：`/openapi/v2/seedream-v4/text-to-image`
- 类型/分组/来源：text-to-image / Seedream / rh-ai
- 价格：0.14 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一组主题为“禅之和谐”的专业 2x2 网格海报系列。 画格 1： 传统日式茶室的极简远景，带有柔和的晨光。 画格 2： 带有细腻竹纹的抹茶刷的微距特写。 画格... | length 5-2000 | - | prompt |
| width | INT | 否 | 2048 | range 512-8192; step 8 | - | width |
| height | INT | 否 | 2048 | range 512-8192; step 1 | - | height |
| sequentialImageGeneration | LIST | 否 | disabled | - | disabled, auto | sequentialImageGeneration |
| maxImages | INT | 否 | 1 | range 1-15; step 1; priceRelated | - | maxImages |
| resolution | LIST | 否 | - | - | 1k, 2k, 4k | 优先级高于widthxheight，传递resolution则使用resolution，不再使用widthxheight |

### 可灵视频编辑o1

- ID：`2012067220412493828`
- Endpoint：`/openapi/v2/kling-video-o1-std/edit-video`
- 类型/分组/来源：video-edit / 可灵 / rh-ai
- 价格：6.3 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| mode | LIST | 是 | std | priceRelated | std, pro | mode |
| prompt | STRING | 是 | 移除背景中所有的行人和车辆，让街道变得空旷。将当前的晴天光光照改为电影感的雨夜，并在潮湿的地面上增加霓虹灯的反光。最后，将人物穿着的休闲T恤替换为细节丰富的银... | length 5-2000 | - | prompt |
| imageUrls | IMAGE[] | 否 | - | maxSize 20MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| videoUrl | VIDEO | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["MP4"] | - | videoUrl |
| keepOriginalSound | BOOLEAN | 是 | true | - | - | keepOriginalSound |

### 可灵参考生视频o1

- ID：`2012067220412493827`
- Endpoint：`/openapi/v2/kling-video-o1-std/refrence-to-video`
- 类型/分组/来源：reference-to-video / 可灵 / rh-ai
- 价格：3.15 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| mode | LIST | 是 | std | priceRelated | std, pro | mode |
| prompt | STRING | 是 | 一个广角横向镜头，展示了参考视频中湿漉漉的赛博朋克街道场景里同时出现的两个截然不同的人物。 人物一（闯入者）： 基于参考图像，武侠大侠站在场景中，拔刀准备战斗... | length 5-2000 | - | prompt |
| aspectRatio | LIST | 是 | 9:16 | - | 1:1, 9:16, 16:9 | aspectRatio |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| imageUrls | IMAGE[] | 否 | <示例素材已脱敏> | maxSize 20MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| videoUrl | VIDEO | 否 | <示例素材已脱敏> | maxSize 10MB; accept ["MP4"] | - | videoUrl |
| keepOriginalSound | BOOLEAN | 是 | true | - | - | keepOriginalSound |

### 全能视频S-图生视频-pro-官方稳定版

- ID：`2012067220412493826`
- Endpoint：`/openapi/v2/rhart-video-s-official/image-to-video-pro`
- 类型/分组/来源：image-to-video / 全能视频S / openai
- 价格：2.1 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 基于参考图，以高保真电影级动态激活场景。一阵强风吹过，使人物的丝绸衣物和散开的发丝随真实的物理规律摆动。人物转身看向摄像机，面部特征和皮肤纹理与原图保持完美一... | - | - | prompt |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p | resolution |
| duration | LIST | 是 | 4 | priceRelated | 4, 8, 12, 16, 20 | duration |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | 图像尺寸须与模型生成视频长宽比一致（允许的值：720x1280，1280x720，1024x1792，1792x1024） |

### 全能视频S-文生视频-pro-官方稳定版

- ID：`2012065966164602881`
- Endpoint：`/openapi/v2/rhart-video-s-official/text-to-video-pro`
- 类型/分组/来源：image-to-video / 全能视频S / openai
- 价格：2.1 CNY/秒
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 在极其写实的手工艺工作室中，一位老工匠向吹管吹气，塑造一个发光的熔融玻璃花瓶。花瓶意外在石桌上碎裂，成百上千的碎片以真实的惯性飞溅碰撞，并伴有清脆的玻璃撞击声... | - | - | prompt |
| size | LIST | 是 | 720x1280 | priceRelated | 720x1280, 1280x720, 1024x1792, 1792x1024, 1080x1920, 1920x1080 | size |
| duration | LIST | 是 | 12 | priceRelated | 4, 8, 12, 16, 20 | duration |

### 全能视频S-图生视频-官方稳定版

- ID：`2012057792137195522`
- Endpoint：`/openapi/v2/rhart-video-s-official/image-to-video`
- 类型/分组/来源：image-to-video / 全能视频S / openai
- 价格：2.28 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 动作描述： 小狗突然冲向地上的橡胶球，兴奋地用前爪猛拍，接着翻滚扑咬，叼起球后甩头狂摇，最后仰躺在地，四爪紧抱球，欢快地用后腿蹬踹，尾巴高速拍打地板。 环境音... | - | - | prompt |
| duration | LIST | 是 | 4 | priceRelated | 4, 8, 12 | duration |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | 图像尺寸须与模型生成视频长宽比一致（允许的值：720x1280，1280x720） |

### 可灵首尾帧生视频o1

- ID：`2012030892408893442`
- Endpoint：`/openapi/v2/kling-video-o1/start-to-end`
- 类型/分组/来源：image-to-video / 可灵 / rh-ai
- 价格：2.1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 衔接提供的首帧和尾帧。书页开始像被魔法风吹动一样快速翻动，边缘溢出金色火星。随后发生剧烈的光芒爆发，书页演变成尾帧中所见的火焰凤凰羽翼。摄像机执行平滑的拉远镜... | length 5-2000 | - | prompt |
| aspectRatio | LIST | 是 | 9:16 | - | 1:1, 9:16, 16:9 | aspectRatio |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | lastImageUrl |
| mode | LIST | 是 | std | priceRelated | std, pro | mode |

### 海螺-02-fast

- ID：`2012030892408893441`
- Endpoint：`/openapi/v2/minimax/hailuo-02/fast`
- 类型/分组/来源：image-to-video / 海螺AI / rh-ai
- 价格：0.45 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 基于原图的快节奏电影感动画。在雪山背景下，一阵强风吹过，使真实的雪粒围绕人物旋动，同时他们厚重的斗篷伴随自然的布料物理规律摆动。摄像机执行平滑、稳定的侧向平移... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | duration |

### 海螺-02-pro

- ID：`2012030193839173634`
- Endpoint：`/openapi/v2/minimax/hailuo-02/pro`
- 类型/分组/来源：text-to-video / 海螺AI / rh-ai
- 价格：2.63 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=50

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一名中世纪骑士的重型钢锤击中一面水晶盾牌，导致其碎裂成数千个锋利的半透明碎片，并以真实的物理碰撞规律向镜头飞溅。骑士的丝绸斗篷因撞击产生的冲击波而剧烈摆动。摄... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| firstImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 6 | - | 6 | duration |

### 海螺-02-标准

- ID：`2012029710558883841`
- Endpoint：`/openapi/v2/minimax/hailuo-02/standard`
- 类型/分组/来源：image-to-video / 海螺AI / rh-ai
- 价格：1.5 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 在茂密的热带雨林中，瀑布倾泻入清澈见底的池塘，产生真实的物理水花和雾气。五彩缤纷的异域花瓣在微风吹拂下飞向空中，以自然的织物感有机动律摆动。摄像机以细微的手持... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| firstImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | - | maxSize 10MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | duration |

### 海螺-02-文生视频-pro

- ID：`2012013242253373441`
- Endpoint：`/openapi/v2/minimax/hailuo-02/t2v-pro`
- 类型/分组/来源：text-to-video / 海螺AI / rh-ai
- 价格：2.63 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一段电影级远景镜头。在高度科技化的沙漠哨所中，一场突如其来的沙尘暴袭击了一顶厚重的帆布帐篷，展示了在风力物理作用下真实的布料褶皱与张力。金属碎片和砂砾以可信的... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |

### 可灵图生视频o1

- ID：`2012004604507910146`
- Endpoint：`/openapi/v2/kling-video-o1/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 / rh-ai
- 价格：2.1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 基于参考图，以自然的动效和真实的物理规律激活场景。人物的发丝和衣物在真实的微风中轻轻摆动，同时保持完美的主体身份和面部一致性。随着摄像机执行平滑、稳定的追踪拍... | length 5-2000 | - | prompt |
| aspectRatio | LIST | 是 | 9:16 | - | 1:1, 9:16, 16:9 | aspectRatio |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 20MB; accept ["JPG","PNG"] | - | firstImageUrl |
| mode | LIST | 是 | std | priceRelated | std, pro | mode |

### 海螺-02-图生视频-pro

- ID：`2012004604507910145`
- Endpoint：`/openapi/v2/minimax/hailuo-02/i2v-pro`
- 类型/分组/来源：image-to-video / 海螺AI / rh-ai
- 价格：2.63 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 专业电影级序列。基于原图，发光的宇宙粒子开始围绕人物手部以真实的流体动力学旋动。摄像机执行平滑的希区柯克变焦（Dolly-zoom），在保持纹理完美清晰的同时... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | - | maxSize 30MB; accept ["JPG","PNG"] | - | lastImageUrl |

### 可灵文生视频o1

- ID：`2012001656184827905`
- Endpoint：`/openapi/v2/kling-video-o1/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 / rh-ai
- 价格：2.1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 一段具有自然动效的高质量电影级视频。一名专业机器人厨师正在充满蒸汽的繁忙厨房里精细地制作传统手工拉面。摄像机执行平滑的追踪拍摄，记录机器人以精准的力度将面团拉... | length --2000 | - | prompt |
| aspectRatio | LIST | 是 | 9:16 | - | 1:1, 9:16, 16:9 | aspectRatio |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| mode | LIST | 是 | std | priceRelated | std, pro | mode |

### 海螺-2.3-文生视频-pro

- ID：`2011999330699112450`
- Endpoint：`/openapi/v2/minimax/hailuo-2.3/t2v-pro`
- 类型/分组/来源：text-to-video / 海螺AI / rh-ai
- 价格：2.63 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 专业电影级追踪镜头。在一个未来感十足的赛博朋克实验室里，一个机械臂正小心翼翼地将闪烁着蓝色荧光的液体倒入玻璃药瓶中。场景中实验室的金属表面和玻璃仪器反射出极其... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |

### 海螺-2.3-图生视频-pro

- ID：`2011758831593648131`
- Endpoint：`/openapi/v2/minimax/hailuo-2.3/image-to-video-pro`
- 类型/分组/来源：image-to-video / 海螺AI / rh-ai
- 价格：2.63 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 专业电影级追踪镜头。角色身披的丝绸斗篷随着真实的物理风力自然摆动。柔和的琥珀色光影在场景中推移，在金属表面产生动态反射，并在地面投射出精细的阴影。摄像机执行平... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | imageUrl |

### seedream-v4.5-图生图

- ID：`2011758831593648130`
- Endpoint：`/openapi/v2/seedream-v4.5/image-to-image`
- 类型/分组/来源：image-to-image / Seedream / rh-ai
- 价格：0.2 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 严格保留人物姿态、服装、光影和面部特征不变。将背景雨林替换为雪夜中的山间古道：地面覆雪，远处有微弱灯笼光，空中飘落细雪，地面升起薄雾。保留原图中湿润石径与落叶... | length 5-2000 | - | prompt |
| width | INT | 否 | 2048 | range 512-8192; step 8 | - | width |
| height | INT | 否 | 2048 | range 512-8192; step 1 | - | height |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["PNG","JPEG"] | - | imageUrls |
| sequentialImageGeneration | LIST | 否 | disabled | - | disabled, auto | sequentialImageGeneration |
| maxImages | INT | 否 | 1 | range 1-15; step 1; priceRelated | - | maxImages |
| resolution | LIST | 否 | - | - | 2k, 4k | 优先级高于widthxheight，传递resolution则使用resolution，不再使用widthxheight |

### 海螺-2.3-fast-pro-图生视频

- ID：`2011749656515899394`
- Endpoint：`/openapi/v2/minimax/hailuo-2.3-fast-pro/image-to-video`
- 类型/分组/来源：image-to-video / 海螺AI / rh-ai
- 价格：1.73 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 图中人物进行一系列流畅且复杂的后空翻，随后优雅着地的极速电影感动画。摄像机保持稳定的追踪拍摄，捕捉自然的运动模糊和真实的服装物理效果（布料在空中摆动）。高度保... | length 5-2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 6 | - | 6 | duration |

### 海螺-2.3-fast-图生视频

- ID：`2011738168711507969`
- Endpoint：`/openapi/v2/minimax/hailuo-2.3-fast/image-to-video`
- 类型/分组/来源：image-to-video / 海螺AI / rh-ai
- 价格：1.01 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一个动态的全身动作序列。图中人物进行一段快节奏、流畅的现代舞，四肢动作遵循精确的人体物理规律。服装布料随着剧烈运动产生真实的褶皱与流动感。摄像机进行快速追踪拍... | length 5-2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | duration |

### 海螺-2.3-图生视频-标准

- ID：`2011737762002432002`
- Endpoint：`/openapi/v2/minimax/hailuo-2.3/i2v-standard`
- 类型/分组/来源：image-to-video / 海螺AI / rh-ai
- 价格：1.5 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 电影级特写镜头，聚焦于人物面部。人物表现出细腻的微表情，包括缓慢的眨眼和一抹温柔、会心的微笑。真实的反射光在眼睛和皮肤纹理上闪烁，随着摄像机执行极其缓慢且稳定... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | duration |

### 海螺-02-图生视频-标准

- ID：`2011737289136599042`
- Endpoint：`/openapi/v2/minimax/hailuo-02/i2v-standard`
- 类型/分组/来源：image-to-video / 海螺AI / rh-ai
- 价格：1.5 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 高速追逐动作。图中的车辆呼啸前行，轮胎飞转，地面摩擦出烟雾和火花。镜头进行低角度追踪拍摄，与车速保持同步，同时捕捉背景疾速掠过的强烈运动模糊。随着车辆穿梭躲避... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 30MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | duration |

### 海螺-2.3-文生视频-标准

- ID：`2011736159161741314`
- Endpoint：`/openapi/v2/minimax/hailuo-2.3/t2v-standard`
- 类型/分组/来源：text-to-video / 海螺AI / rh-ai
- 价格：1.5 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 场景中心突然发生剧烈爆炸。玻璃碎片和金属残骸以真实的物理规律和深度感向镜头飞溅。浓烟滚滚而上，橙色火球迅速膨胀。镜头因爆炸冲击产生剧烈的真实手持抖动，随后在灰... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | duration |

### 海螺-02-文生视频-标准

- ID：`2011735493609582593`
- Endpoint：`/openapi/v2/minimax/hailuo-02/t2v-standard`
- 类型/分组/来源：text-to-video / 海螺AI / rh-ai
- 价格：1.5 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一个电影感特写镜头：一颗发光的半透明水晶球掉入一潭深色的旋涡液体中。撞击瞬间，水晶球破碎成数千块闪烁的碎片，产生具有复杂物理效果的真实水花和涟漪。球体内部的金... | length --2000 | - | prompt |
| enablePromptExpansion | BOOLEAN | 是 | true | - | - | enablePromptExpansion |
| duration | LIST | 是 | 6 | priceRelated | 6, 10 | duration |

### seedream-v4.5-文生图

- ID：`2011729237939384321`
- Endpoint：`/openapi/v2/seedream-v4.5/text-to-image`
- 类型/分组/来源：text-to-image / Seedream / rh-ai
- 价格：0.2 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一张极具电影感的武侠海报，夜雨中的悬崖古寺，一位白衣侠客背对镜头立于断桥边缘，长发与衣袂在风中翻飞，手中长剑滴血，远处雷光映照出追兵剪影。背景是云雾缭绕的群山... | length 5-2000 | - | prompt |
| width | INT | 否 | 2048 | range 512-8192; step 8 | - | width |
| height | INT | 否 | 2048 | range 512-8192; step 8 | - | height |
| sequentialImageGeneration | LIST | 否 | disabled | - | disabled, auto | sequentialImageGeneration |
| maxImages | INT | 否 | 1 | range 1-15; step 1; priceRelated | - | maxImages |
| resolution | LIST | 否 | - | - | 2k, 4k | 优先级高于widthxheight，传递resolution则使用resolution，不再使用widthxheight |

### 全能视频S-文生视频-官方稳定版

- ID：`2011726198578933762`
- Endpoint：`/openapi/v2/rhart-video-s-official/text-to-video`
- 类型/分组/来源：text-to-video / 全能视频S / openai
- 价格：2.28 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一段穿梭于繁华未来街道市场的电影级高保真追踪镜头。一名街头艺人正在敲击一套透明玻璃架子鼓，每一次敲击的声效都与鼓内跳动的荧光涟漪完美同步。一个小型机器人宠物不... | - | - | prompt |
| size | LIST | 是 | 720x1280 | - | 720x1280, 1280x720 | size |
| duration | LIST | 是 | 4 | priceRelated | 4, 8, 12 | duration |

### Vidu-首尾帧生视频-q2-turbo

- ID：`2011680658176684034`
- Endpoint：`/openapi/v2/vidu/start-end-to-video-q2-turbo`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.13 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 从首帧到尾帧的电影级高速过渡。戴着空间头盔的少女抬起手，举起一颗发光的蓝色能量水晶。随着水晶升起，强烈的金色能量丝线从中喷涌而出，交织成一个巨大的、闪烁的星际... | length 1-4000 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |
| bgm | BOOLEAN | 是 | true | - | - | bgm |

### Vidu-首尾帧生视频-q2-pro

- ID：`2011680028485824514`
- Endpoint：`/openapi/v2/vidu/start-end-to-video-q2-pro`
- 类型/分组/来源：image-to-video / Vidu / vidu
- 价格：0.18 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 暴雨中的悬崖石桥上，白衣剑客猛然拔剑冲向静立的蒙面忍者。然而就在他跃起的瞬间，脚下湿滑，身形失控——长剑脱手飞出，身体重重撞上石栏。他试图稳住，却因旧伤崩裂，... | length 1-4000 | - | prompt |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |
| bgm | BOOLEAN | 是 | true | - | - | bgm |

### Vidu-文生视频-q2

- ID：`2011646882759389186`
- Endpoint：`/openapi/v2/vidu/text-to-video`
- 类型/分组/来源：text-to-video / Vidu / vidu
- 价格：0.22 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一位孤独的宇航员身穿磨损的太空服，独自探索一座被遗弃的外星基地。基地内部布满奇异的几何结构、发光的古老符号和锈蚀的金属通道，空气中弥漫着微弱蓝光与尘埃。他手持... | length 1-4000 | - | prompt |
| style | LIST | 是 | general | - | general, anime | style |
| aspectRatio | LIST | 是 | 16:9 | - | 4:3, 3:4, 16:9, 9:16, 1:1 | aspectRatio |
| resolution | LIST | 是 | 720p | priceRelated | 540p, 720p, 1080p | resolution |
| movementAmplitude | LIST | 是 | auto | - | auto, small, medium, large | movementAmplitude |
| duration | LIST | 是 | 5 | priceRelated | 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 | duration |

### 可灵图生视频2.5-turbo-std

- ID：`2011410147815272449`
- Endpoint：`/openapi/v2/kling-v2.5-turbo-std/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 / rh-ai
- 价格：1.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 图中人物对着手机不停的流泪 | length --2000 | - | prompt |
| negativePrompt | STRING | 否 | - | length --2500 | - | negativePrompt |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| guidanceScale | FLOAT | 否 | 0.5 | range 0-1; step 0.1; precision 1 | - | guidanceScale |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |

### 可灵图生视频2.5-turbo-pro

- ID：`2011409246597754881`
- Endpoint：`/openapi/v2/kling-v2.5-turbo-pro/image-to-video`
- 类型/分组/来源：image-to-video / 可灵 / rh-ai
- 价格：1.75 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 否 | 镜头轻微旋转移动，自然过渡一个三只头的妖兽从屏幕外跳跃进去，从男人上面掠过，男人一个下腰闪躲，妖兽错开了男人冲到了后面，妖兽原地转身继续朝正在转身的男人奔跑过... | length --2000 | - | prompt |
| negativePrompt | STRING | 否 | - | length --2500 | - | negativePrompt |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| guidanceScale | FLOAT | 否 | 0.5 | range 0-1; step 0.1; precision 1 | - | guidanceScale |
| firstImageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | firstImageUrl |
| lastImageUrl | IMAGE | 否 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | lastImageUrl |

### 可灵文生视频2.5-turbo-pro

- ID：`2011408949544562690`
- Endpoint：`/openapi/v2/kling-v2.5-turbo-pro/text-to-video`
- 类型/分组/来源：text-to-video / 可灵 / rh-ai
- 价格：1.75 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 1 cat in classroom dance | length 1-2000 | - | prompt |
| negativePrompt | STRING | 否 | - | length --2500 | - | negativePrompt |
| duration | LIST | 是 | 5 | priceRelated | 5, 10 | duration |
| guidanceScale | FLOAT | 否 | 0.5 | range 0-1; step 0.1; precision 1 | - | guidanceScale |
| aspectRatio | LIST | 是 | 9:16 | - | 1:1, 16:9, 9:16 | aspectRatio |

### 万相2.6-图生视频

- ID：`2011327434521391105`
- Endpoint：`/openapi/v2/alibaba/wan-2.6/image-to-video`
- 类型/分组/来源：image-to-video / Wan Video Models / wan
- 价格：2.25 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| prompt | STRING | 否 | 人物的沾血的手轻轻划过卷轴，受伤的手指上不小心滑落一滴鲜血滴在了卷轴上，血液渗透进了卷轴消失不见，等了1-2秒后卷轴上的所有字体开始有序的渐次变成隐隐散发很弱... | length --- | - | prompt |
| negativePrompt | STRING | 否 | - | length --- | - | negativePrompt |
| resolution | LIST | 是 | 1080p | priceRelated | 720p, 1080p | resolution |
| duration | LIST | 是 | 5 | priceRelated | 5, 10, 15 | duration |
| shotType | LIST | 是 | single | - | single, multi | shotType |

### 万相2.6-文生视频

- ID：`2011281240097107969`
- Endpoint：`/openapi/v2/alibaba/wan-2.6/text-to-video`
- 类型/分组/来源：text-to-video / Wan Video Models / wan
- 价格：2.25 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一只小巧可爱的卡通小猫将军，身穿细节精致的金色盔甲，头戴一个稍大的头盔，勇敢地站在悬崖上 | length 5-5000 | - | prompt |
| negativePrompt | STRING | 否 | - | - | - | negativePrompt |
| duration | LIST | 是 | 5 | priceRelated | 5, 10, 15 | duration |
| resolution | LIST | 是 | 1080*1920 | priceRelated | 1280*720, 720*1280, 1920*1080, 1080*1920 | resolution |
| shotType | LIST | 是 | single | - | single, multi | 指定生成视频的镜头类型，即视频是由一个连续镜头还是多个切换镜头组成 |

### 全能视频S-角色上传-低价渠道版

- ID：`2011055907607490562`
- Endpoint：`/openapi/v2/rhart-video-s/sora-upload-character`
- 类型/分组/来源：video-tools / 全能视频S / rh-ai
- 价格：0.05 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| videoUrl | VIDEO | 是 | - | maxSize 10MB; accept ["MP4"] | - | videoUrl |

### 全能视频S-文生视频-pro-低价渠道版-已下架

- ID：`2011003029035495426`
- Endpoint：`/openapi/v2/rhart-video-s/text-to-video-pro-deprecated`
- 类型/分组/来源：text-to-video / 全能视频S / rh-ai
- 价格：1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 90 年代纪录片风格访谈，一位饱经风霜的老渔民坐在温馨的海边小屋里，说道：“我至今还能闻到年轻时海风的咸腥，还有鲜鱼的气息。” | length 5-4000 | - | prompt |
| duration | LIST | 是 | 15 | priceRelated | 15, 25 | duration |
| aspectRatio | LIST | 是 | 9:16 | priceRelated | 9:16, 16:9 | aspectRatio |
| storyboard | BOOLEAN | 否 | false | - | - | storyboard |

### 全能视频S-图生视频-pro-低价渠道版-已下架

- ID：`2010915780436504578`
- Endpoint：`/openapi/v2/rhart-video-s/image-to-video-pro-deprecated`
- 类型/分组/来源：image-to-video / 全能视频S / rh-ai
- 价格：1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一只毛茸茸的白色小兔子站在森林里温馨的小木屋门前，眼睛亮晶晶，开心地挥着爪子说：“欢迎大家来我家玩！”阳光透过树叶洒在它身上，周围有野花和蝴蝶飞舞。镜头采用柔... | length 5-4000 | - | prompt |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 15 | priceRelated | 15, 25 | duration |
| aspectRatio | LIST | 是 | 9:16 | priceRelated | 9:16, 16:9 | aspectRatio |
| storyboard | BOOLEAN | 否 | false | - | - | storyboard |

### 全能视频V3.1-fast-图生视频-低价渠道版

- ID：`2005910264819793921`
- Endpoint：`/openapi/v2/rhart-video-v3.1-fast/image-to-video`
- 类型/分组/来源：image-to-video / 全能视频V / google
- 价格：1.5 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 保持主体和背景不变，生成人物跑步的视频，发丝随风飘荡 | length 5-8000 | - | prompt |
| aspectRatio | LIST | 是 | 16:9 | - | 16:9, 9:16 | aspectRatio |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 30MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| duration | LIST | 否 | 8 | - | 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p, 4k | resolution |

### 全能视频V3.1-pro-文生视频-低价渠道版

- ID：`2005884653783007234`
- Endpoint：`/openapi/v2/rhart-video-v3.1-pro/text-to-video`
- 类型/分组/来源：text-to-video / 全能视频V / google
- 价格：0.9 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 一座悬浮在云端的古老城堡，周围环绕着金色飞龙与漂浮的岛屿，夕阳将天空染成紫红色，魔法能量在空气中闪烁，镜头缓缓推进，展现恢弘而神秘的世界。 | length 5-8000 | - | prompt |
| aspectRatio | LIST | 是 | 9:16 | - | 16:9, 9:16 | aspectRatio |
| duration | LIST | 否 | 8 | - | 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p, 4k | resolution |

### 全能视频V3.1-fast-文生视频-低价渠道版

- ID：`2005884261993070594`
- Endpoint：`/openapi/v2/rhart-video-v3.1-fast/text-to-video`
- 类型/分组/来源：text-to-video / 全能视频V / google
- 价格：1.5 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 春日午后，樱花纷飞的乡间小路，一位少女骑着老旧自行车经过稻田，微风拂过她的发梢，远处传来风铃声，画面温暖柔和，充满宁静与希望。 | length 5-8000 | - | prompt |
| aspectRatio | LIST | 是 | 9:16 | - | 16:9, 9:16 | aspectRatio |
| duration | LIST | 否 | 8 | - | 8 | duration |
| resolution | LIST | 是 | 720p | priceRelated | 720p, 1080p, 4k | resolution |

### 全能图片PRO-文生图-官方稳定版

- ID：`2004544597055029250`
- Endpoint：`/openapi/v2/rhart-image-n-pro-official/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片 / google
- 价格：0.8 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| resolution | LIST | 是 | 1k | priceRelated | 1k, 2k, 4k | resolution |
| prompt | STRING | 是 | 在一片广阔无垠的大海边，一只快乐的猴子坐在沙滩上，享受着温暖的阳光。它的旁边有一个小小的香蕉，象征着自然的馈赠。天空湛蓝，阳光明媚，海浪轻拍沙滩，营造出轻松愉... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 否 | 3:4 | - | 1:1, 3:2, 2:3, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9 | aspectRatio |

### 全能图片PRO-图生图-官方稳定版

- ID：`2004544343584849921`
- Endpoint：`/openapi/v2/rhart-image-n-pro-official/edit`
- 类型/分组/来源：image-to-image / 全能图片 / google
- 价格：0.8 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |
| prompt | STRING | 是 | 海边沙滩变成夏日祭典现场：猴子戴着纸折小帽，小香蕉插着蜡烛当作“生日蛋糕”，周围有彩旗、西瓜、贝壳风铃。其他小猴子在远处玩耍，烟花在晴空中绽放。风格欢乐卡通，... | length 1-20000 | - | prompt |
| resolution | LIST | 是 | 1k | priceRelated | 1k, 2k, 4k | resolution |
| aspectRatio | LIST | 否 | 3:4 | - | 1:1, 3:2, 2:3, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9 | 不传 aspectRatio 参数时为自适应图片尺寸 |

### 全能图片V1-文生图-低价渠道版

- ID：`2004543090783993858`
- Endpoint：`/openapi/v2/rhart-image-v1/text-to-image`
- 类型/分组/来源：text-to-image / 全能图片 / rh-ai
- 价格：0.05 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | A cute baby monkey with hyper-realistic soft fur, its body covered in intricate... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 是 | 3:4 | - | auto, 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9 | aspectRatio |

### 全能图片V1-图生图-低价渠道版

- ID：`2004542825494265857`
- Endpoint：`/openapi/v2/rhart-image-v1/edit`
- 类型/分组/来源：image-to-image / 全能图片 / rh-ai
- 价格：0.05 CNY/张
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | Replace the dragon fruit in the image with an adorable, stylized monkey that ma... | length 1-20000 | - | prompt |
| aspectRatio | LIST | 是 | auto | - | auto, 1:1, 16:9, 9:16, 4:3, 3:4, 3:2, 2:3, 5:4, 4:5, 21:9 | aspectRatio |
| imageUrls | IMAGE[] | 是 | <示例素材已脱敏> | maxSize 10MB; multipleInputs; accept ["JPG","PNG"] | - | imageUrls |

### 全能视频S-文生视频-低价渠道版

- ID：`2004499823346368514`
- Endpoint：`/openapi/v2/rhart-video-s/text-to-video`
- 类型/分组/来源：text-to-video / 全能视频S / rh-ai
- 价格：1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| duration | LIST | 是 | 10 | priceRelated | 10, 15 | duration |
| prompt | STRING | 是 | 阳光透过高大的橡树洒在一片魔法森林中，小狐狸与会说话的蘑菇一起跳舞，远处有瀑布和漂浮的蒲公英。画面柔和温暖，手绘动画质感，色彩明亮饱和，镜头缓慢推进，风格致敬... | length 5-4000 | - | prompt |
| aspectRatio | LIST | 是 | 9:16 | - | 9:16, 16:9 | aspectRatio |
| storyboard | BOOLEAN | 否 | false | - | - | storyboard |

### 全能视频S-图生视频-低价渠道版

- ID：`2004494607725150210`
- Endpoint：`/openapi/v2/rhart-video-s/image-to-video`
- 类型/分组/来源：image-to-video / 全能视频S / rh-ai
- 价格：1 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 50MB; accept ["JPG","PNG"] | - | imageUrl |
| duration | LIST | 是 | 10 | priceRelated | 10, 15 | duration |
| aspectRatio | LIST | 是 | 9:16 | - | 9:16, 16:9 | aspectRatio |
| prompt | STRING | 是 | 基于原图生成10秒动态视频，角色使用地道男声京腔配音，语气带点胡同大爷的调侃劲儿。 0–2秒：狸花猫翘着二郎腿，爪尖轻敲桌面，斜眼笑：“咱这节目今天炸了啊！”... | length 5-4000 | - | prompt |
| storyboard | BOOLEAN | 否 | false | - | - | storyboard |

### 全能视频S-图生视频-支持真人-官方稳定版

- ID：`2004491650426257409`
- Endpoint：`/openapi/v2/rhart-video-s-official/image-to-video-realistic`
- 类型/分组/来源：image-to-video / 全能视频S / openai
- 价格：3.2 CNY/次
- 队列/并发：queueSize=1000，concurrencyLimit=500

| 字段 | 类型 | 必填 | 默认值 | 约束 | 枚举/选项 | 说明 |
|---|---|---|---|---|---|---|
| prompt | STRING | 是 | 生成一段视频 | - | - | prompt |
| duration | LIST | 是 | 4 | priceRelated | 4, 8, 12 | duration |
| imageUrl | IMAGE | 是 | <示例素材已脱敏> | maxSize 10MB; accept ["JPG","PNG"] | - | imageUrl |

## 不可访问模型

| ID | 模型 | 类型 | 返回信息 |
|---|---|---|---|
| 2005642874987003905 | 全能图片G-1.5-图生图-低价渠道版-(已下架) 可用全能图片G-2 模型代替 | image-to-image | 您暂无权限访问该应用，请联系作者开通 |
| 2005642306994356226 | 全能图片G-1.5-文生图-低价渠道版-(已下架) 可用全能图片G-2 模型代替 | text-to-image | 您暂无权限访问该应用，请联系作者开通 |

## LLM 模型 API

LLM 页面只抓到模型目录接口，模型调用应按页面的通用 LLM 接入层实现，具体运行接口需在后续进入 Playground 或官方 LLM 文档时再确认。当前可确认的数据如下。

### LLM Provider 统计

| Provider | 数量 |
|---|---:|
| alibaba | 5 |
| zhipu | 4 |
| bytedance | 4 |
| deepseek | 2 |
| minimax | 1 |

### LLM 模型清单

| modelKey | Provider | Version | Context | Capabilities | 价格摘要 |
|---|---|---|---:|---|---|
| `glm-5.1` | zhipu | stable | 204800 | chat, tools, thinking, streaming | 输入 0.006 / 输出 0.024 CNY/1K |
| `glm-5-turbo` | zhipu | stable | 204800 | chat, tools, thinking, streaming | 输入 0.005 / 输出 0.022 CNY/1K |
| `glm-5` | zhipu | stable | 204800 | chat, tools, thinking, streaming | 输入 0.004 / 输出 0.018 CNY/1K |
| `qwen/qwen3.7-max` | alibaba | stable | 1000000 | chat, tools, reasoning, streaming, webSearch, structuredOutputs | 输入 0.009 / 输出 0.027 CNY/1K |
| `glm-5v-turbo` | zhipu | stable | 204800 | chat, tools, vision, thinking, streaming, multimodal | 输入 0.005 / 输出 0.022 CNY/1K |
| `qwen/qwen3.7-plus` | alibaba | stable | 1000000 | chat, tools, vision, streaming, structuredOutputs | 输入 0.0017 / 输出 0.0068 CNY/1K |
| `deepseek/deepseek-v4-pro` | deepseek | stable | 1048576 | chat, tools, streaming, structuredOutputs | 输入 0.0021 / 输出 0.0042 CNY/1K |
| `qwen/qwen3.6-plus` | alibaba | stable | 1000000 | chat, tools, vision, streaming, structuredOutputs | 输入 0.00306 / 输出 0.01836 CNY/1K |
| `bytedance/doubao-seed-2.0-pro` | bytedance | stable | 262144 | chat, vision, streaming | 输入 0.00272 / 输出 0.0136 CNY/1K |
| `bytedance/doubao-seed-2.0-code` | bytedance | stable | 262144 | chat, tools, streaming | 输入 0.00272 / 输出 0.0136 CNY/1K |
| `deepseek/deepseek-v4-flash` | deepseek | stable | 1048576 | chat, tools, streaming, structuredOutputs | 输入 0.0007 / 输出 0.0014 CNY/1K |
| `qwen/qwen3.6-flash` | alibaba | stable | 1000000 | chat, tools, streaming, structuredOutputs | 输入 0.00153 / 输出 0.00918 CNY/1K |
| `bytedance/doubao-seed-2.0-lite` | bytedance | stable | 262144 | chat, vision, streaming | 输入 0.00051 / 输出 0.00306 CNY/1K |
| `bytedance/doubao-seed-2.0-mini` | bytedance | stable | 262144 | chat, vision, streaming | 输入 0.00017 / 输出 0.0017 CNY/1K |
| `minimax/minimax-m2.7` | minimax | stable | 204800 | chat, tools, streaming, structuredOutputs | 输入 0.00216 / 输出 0.00864 CNY/1K |
| `qwen/qwen3.6-max-preview` | alibaba | stable | 256000 | chat, tools, streaming, structuredOutputs | 输入 0.00936 / 输出 0.05616 CNY/1K |

## 后续实现建议

1. 标准模型不要继续硬编码 endpoint/字段，优先建立 `ModelCatalogRepository`，缓存 `/api/sku/list` + `/api/sku/detail` 的脱敏结构。
2. 表单渲染以 `fieldKey/type/required/options/defaultValue/multipleInputs` 为核心，`skuInputExtraJson`、子字段和条件字段先保留原始 JSON，逐步补动态 UI。
3. 提交层统一拼接 `/openapi/v2{rhEndpoint}`，公共处理 `webhookUrl`、`query`、`media/upload/binary`、结果 URL 转存提示。
4. LLM 与标准模型分开建模：LLM 是同步/流式 token 计费模型目录，标准模型是异步任务模型目录。
5. 价格只作为展示和预估参考，真正扣费以服务端提交/价格预览结果为准；本次抓包未执行付费任务。
