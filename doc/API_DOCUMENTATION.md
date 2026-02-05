# RunningHub API Documentation

## 1. AI 应用列表 (WebApp List)

**基本信息**
- **URL**: `https://www.runninghub.cn/api/webapp/list`
- **Method**: `POST`
- **Auth**: Bearer Token (Authorization Header)

**请求参数 (JSON)**
| 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| size | Integer | 是 | 分页大小 (示例: 30) |
| current | Integer | 是 | 当前页码 (示例: 1) |
| tags | Array | 否 | 标签过滤 (传入分类 ID 数组，示例: ["1671123934319734090"]) |
| sort | String | 否 | 排序方式 (示例: "RECOMMEND") |

**备注**
- 点击具体分类 Tab 时，同样请求此接口，只需在 `tags` 数组中传入对应分类的 ID 即可。

---

## 2. 精选列表 (Carefully Chosen List - Banner 数据源)

**基本信息**
- **URL**: `https://www.runninghub.cn/api/webapp/carefullyChosenList`
- **Method**: `POST`
- **Auth**: Bearer Token (Authorization Header)

**请求参数 (JSON)**
- 通常发送空 JSON 对象 `{}`。

**备注**
- 返回列表中的第一项通常作为首页顶部的 Banner 展示。

---

## 3. 分类/标签树 (Category/Tag Tree)

**基本信息**
- **URL**: `https://www.runninghub.cn/api/portal/tag/tree`
- **Method**: `POST`
- **Auth**: Bearer Token (Authorization Header)

**请求参数 (JSON)**
- 无特殊要求，发送空 JSON 对象 `{}` 即可。

**响应参数 (BaseResponse<List<TagDto>>)**
| 参数名 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | String | 分类 ID |
| name | String | 分类名称 (用于显示) |
| level | Integer | 层级 (根分类通常为 1) |
| parentId | String | 父级 ID |
| rang | String | 应用范围 (示例: "WEBAPP") |
| enable | Boolean | 是否启用 |

---

## 4. AI 应用详情 (WebApp Detail)

**基本信息**
- **URL**: `https://www.runninghub.cn/api/webapp/detail`
- **Method**: `POST`
- **Auth**: Bearer Token (Authorization Header)

**请求参数 (JSON)**
| 参数名 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| webappId | String | 是 | 应用 ID (示例: "2018709606033264641") |

**备注**
- 其中 `inputNodes` 的 `fieldType` 包含 `IMAGE` (图片上传)、`LIST` (列表选择) 和 `STRING` (文本输入)。

---

## 5. 文件上传 (File Upload)

**基本信息**
- **URL**: `https://www.runninghub.cn/task/openapi/upload` (注：不要加 `/api/` 前缀)
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`

**请求参数 (Form Data)**
| 参数名 | 类型 | 说明 |
| :--- | :--- | :--- |
| apiKey | String | 应用 API Key |
| fileType | String | 固定值 "input" |
| file | File | 要上传的二进制文件 |

**备注**
- 上传成功后返回 `fileName`，该名称应填入 Run Task 接口中对应节点的 `fieldValue`。

---

## 6. 发起 AI 应用任务 (Run Task)

**基本信息**
- **URL**: `https://www.runninghub.cn/task/openapi/ai-app/run` (注：不要加 `/api/` 前缀)
- **Method**: `POST`

**请求参数 (JSON)**
| 参数名 | 类型 | 说明 |
| :--- | :--- | :--- |
| webappId | Long | 应用 ID |
| apiKey | String | 应用 API Key |
| nodeInfoList | Array | 节点信息数组 (来自详情页，修改 `fieldValue`) |
| webhookUrl | String | (可选) 任务完成后的回调地址 |
| instanceType | String | (可选) 机器规格: "default"(24G), "plus"(48G) |

---

## 7. 查询任务输出 (Task Outputs)

**基本信息**
- **URL**: `https://www.runninghub.cn/task/openapi/outputs` (注：不要加 `/api/` 前缀)
- **Method**: `POST`

**请求参数 (JSON)**
| 参数名 | 类型 | 说明 |
| :--- | :--- | :--- |
| taskId | Long | 任务 ID |
| apiKey | String | 应用 API Key |

**备注**
- 若任务进行中，返回 code 可能为 804(运行中) 或 813(排队中)。
- 成功后 `data` 数组中包含 `fileUrl`。

---

## 8. 获取账户状态 (Account Status)

**基本信息**
- **URL**: `https://www.runninghub.cn/uc/openapi/accountStatus`
- **Method**: `POST`

**请求参数 (JSON)**
| 参数名 | 类型 | 说明 |
| :--- | :--- | :--- |
| apikey | String | 应用 API Key |

**备注**
- 返回 `remainCoins` (RH币余额) 和 `currentTaskCounts` (运行中任务数) 等。
