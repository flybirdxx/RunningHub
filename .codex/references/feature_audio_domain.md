# feature_audio_domain

## 模块概述

`:feature:audio:domain` 定义音频生成请求、任务状态、结果和仓库契约。它保持音频能力的平台无关领域边界。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:audio:domain` |
| 路径 | `feature/audio/domain` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.audio.domain` |
| 完整扫描基线源文件数 | 2 |
| 内部依赖 | 无 |
| 外部依赖 | coroutines, `kotlin("test")` |

## 关键源码

- `AudioGeneration.kt`：音频请求、任务状态和结果模型。
- `AudioRepository.kt`：音频生成仓库契约。

## 约束

- 不绑定具体 MiniMax/OpenAPI DTO。
- 任务状态需要可表达运行、成功、失败和取消语义。
