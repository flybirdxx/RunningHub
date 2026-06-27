# feature_audio_data

## 模块概述

`:feature:audio:data` 实现音频生成远端 API、DTO 和仓库映射，当前对接 OpenAPI v2 音频 endpoint。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:feature:audio:data` |
| 路径 | `feature/audio/data` |
| 类型 | library |
| 命名空间 | `com.runninghub.feature.audio.data` |
| 完整扫描基线源文件数 | 6 |
| 内部依赖 | `:core:network`, `:feature:audio:domain` |
| 外部依赖 | Ktor, Koin, serialization, coroutines |

## 关键源码

- `di/AudioDataModule.kt`：Koin binding。
- `remote/api/AudioApi.kt`：音频生成和查询 endpoint。
- `remote/dto/AudioDto.kt`：MiniMax/任务查询 DTO。
- `repository/AudioRepositoryImpl.kt`：音频仓库实现。

## 约束

- API endpoint 和任务查询参数由测试固定。
- 音频任务状态映射为 Domain，不向上暴露 DTO。
