# R8 配置分析报告 — RunningHub

## 1. R8 构建配置分析

### 1.1 release 构建类型 (`app/build.gradle.kts`)

| 配置项 | 当前值 | 推荐值 | 状态 |
|---|---|---|---|
| `isMinifyEnabled` | `false` | `true` | ⚠️ 未启用 |
| `isShrinkResources` | 未设置 | `true` | ⚠️ 缺失 |
| 默认 ProGuard 文件 | `proguard-android-optimize.txt` | `proguard-android-optimize.txt` | ✅ 正确 |

**问题：R8 代码缩减和资源缩减均未启用。** 当前 release 构建与 debug 构建在包体积优化上没有差异。应将配置修改为：

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 1.2 AGP 版本 (`build.gradle.kts`)

当前 AGP 版本为 **8.13.2**。AGP 9.0 包含了更多 R8 优化改进，建议在条件允许时升级至 AGP 9.0。

### 1.3 Gradle 属性 (`gradle.properties`)

| 属性 | 当前状态 | 评估 |
|---|---|---|
| `android.enableR8.fullMode` | 未设置（AGP 8.0+ 默认启用） | ✅ 正确 |
| `android.r8.optimizedResourceShrinking` | 未设置 | ⚠️ 建议启用 |

当前 AGP 版本 > 8.6，建议在 `gradle.properties` 中添加以下配置以启用优化的资源缩减：

```properties
android.r8.optimizedResourceShrinking=true
```

---

## 2. Keep 规则分析 (`app/proguard-rules.pro`)

当前文件包含 3 条规则：

```proguard
-keep class com.runninghub.app.data.remote.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
```

---

### 规则 1：`-keep class com.runninghub.app.data.remote.** { *; }`

**影响等级：** 包级别通配符 — 最高影响范围

此规则保留了 `com.runninghub.app.data.remote` 包及所有子包中的 **全部类和全部成员**，覆盖了 7 个文件：

- `api/WebAppApi.kt` — Retrofit API 接口
- `api/AudioApi.kt` — Retrofit API 接口
- `model/BaseResponse.kt` — 通用响应包装
- `model/WebAppResponse.kt` — WebApp 模型（23+ 个 data class）
- `model/AccountResponse.kt` — 账户模型
- `model/AudioModel.kt` — 音频模型
- `model/TagResponse.kt` — 标签模型

**操作：细化此规则**

此规则包含两类需要区分处理的代码：

**A) Retrofit API 接口 (`api/` 子包)**

项目使用 Retrofit **2.9.0**，该版本已内置 consumer keep rules，会自动保留 HTTP 注解标记的接口方法。`WebAppApi` 和 `AudioApi` 不需要手动 keep 规则。

**B) Gson 数据模型 (`model/` 子包)**

对数据模型的代码检查发现以下情况：

| 文件 | `@SerializedName` 使用 | 状态 |
|---|---|---|
| `WebAppResponse.kt` | 部分字段使用 | ⚠️ 不完整 |
| `AccountResponse.kt` | 大部分字段使用 | ⚠️ 不完整 |
| `AudioModel.kt` | 部分字段使用 | ⚠️ 不完整 |
| `TagResponse.kt` | 未使用 | ❌ |
| `BaseResponse.kt` | 未使用 | ❌ |

以下 data class 的字段缺少 `@SerializedName`，在 R8 混淆后 Gson 将无法正确反序列化：

- `BaseResponse` — `code`, `msg`, `data`
- `PageData` — `records`, `total`, `size`, `current`
- `TagDto` — `id`, `name`, `level`, `parentId`, `rang`, `enable`, `childTags`
- `TagSimpleDto` — `id`, `name`
- `PreviewDto` — `url`
- `StatisticsInfo` — `likeCount`, `collectCount`, `useCount`, `pv`
- `CoverDto` — `url`, `imageWidth`, `imageHeight`
- `WebAppListRequest` — `size`, `current`, `tags`, `sort`
- `TagTreeRequest` — `rang`
- `TaskRunRequest` — `webappId`, `apiKey`, `nodeInfoList`, `webhookUrl`, `instanceType`
- `TaskRunResponse` — `netWssUrl`, `taskId`, `clientId`, `taskStatus`, `promptTips`
- `TaskStatusRequest` — `taskId`, `apiKey`
- `TaskOutputDto` — `fileUrl`, `fileName`, `fileType`, `failedReason`
- `TaskFailedReason` — `node_name`, `exception_message`, `traceback`
- `UploadResponse` — `fileName`, `fileType`
- `InputNodeDto` — `nodeId`, `nodeName`, `fieldName`, `fieldValue`, `fieldData`, `fieldType`, `description`, `descriptionEn`
- `MiniMaxAudioResponse` — `taskId`, `status`, `errorCode`, `errorMessage`, `results`, `clientId`, `promptTips`
- `TaskQueryResult` — `taskId`, `status`, `errorCode`, `errorMessage`, `results`, `usage`
- `AudioResult` — `url`, `outputType`, `text`
- `TaskUsage` — `consumeMoney`, `consumeCoins`, `taskCostTime`, `thirdPartyConsumeMoney`
- `TaskQueryRequest` — `taskId`
- `AccountStatusRequest` — `apikey`

**推荐修复步骤：**

1. 为上述所有 data class 的每个字段添加 `@SerializedName` 注解，确保 Gson 在字段被混淆后仍能正确匹配 JSON 键名
2. 将 Gson 升级至 **2.11.0+**（当前通过 `converter-gson:2.9.0` 间接依赖的 Gson 版本较旧），或显式添加 `implementation("com.google.code.gson:gson:2.11.0")`。Gson 2.11.0+ 内置了对 `@SerializedName` 字段的 consumer keep rules
3. 完成上述两步后，删除此包级别通配符规则

如果暂时无法完成全部 `@SerializedName` 注解工作，可先将规则缩小为仅覆盖 model 包：

```proguard
-keep class com.runninghub.app.data.remote.model.** { *; }
```

这至少可以释放 `api/` 子包中 Retrofit 接口的优化空间。

---

### 规则 2：`-keepattributes Signature`

**操作：移除**

`proguard-android-optimize.txt` 默认文件已包含 `-keepattributes Signature`。此规则完全冗余。

---

### 规则 3：`-keepattributes *Annotation*`

**操作：移除**

`proguard-android-optimize.txt` 默认文件已包含对关键注解的保留。此规则过于宽泛，保留了所有注解（包括不需要的注解），阻碍 R8 优化。Retrofit、Hilt、Room 等库的 consumer rules 已各自处理了它们需要的注解保留。

---

## 3. 操作总结

| 优先级 | 操作 | 详情 |
|---|---|---|
| **P0** | 启用 R8 | 将 `isMinifyEnabled` 设为 `true`，添加 `isShrinkResources = true` |
| **P1** | 为所有 Gson 模型字段添加 `@SerializedName` | 覆盖上述列出的 22 个 data class |
| **P1** | 升级 Gson 至 2.11.0+ | 显式声明依赖或升级 Retrofit converter-gson |
| **P2** | 细化包级别 keep 规则 | 完成 P1 后删除整个规则；过渡期缩小为 `model.**` |
| **P2** | 移除冗余 `-keepattributes Signature` | 已被默认文件覆盖 |
| **P2** | 移除冗余 `-keepattributes *Annotation*` | 已被默认文件和库规则覆盖 |
| **P3** | 启用优化资源缩减 | 在 `gradle.properties` 添加 `android.r8.optimizedResourceShrinking=true` |
| **P3** | 升级至 AGP 9.0 | AGP 9.0 包含更多 R8 优化改进 |

---

## 4. 验证建议

完成上述变更后，请使用 [UI Automator](https://developer.android.com/training/testing/other-components/ui-automator) 运行测试，重点验证以下场景：

- 所有网络请求的数据解析（覆盖 `data.remote.model` 下的全部模型类）
- WebApp 列表加载和详情页展示
- 音频任务提交和查询
- 账户状态查询和用户信息获取
- 标签树加载
- 文件上传功能
