# core_common

## 模块概述

`:core:common` 放置跨平台通用能力，目前主要包含凭据问题语义和 MD5 哈希 expect/actual 实现。它为 Core/Feature 提供轻量公共工具，不承载业务流程。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:core:common` |
| 路径 | `core/common` |
| 类型 | library |
| 命名空间 | `com.runninghub.core.common` |
| 完整扫描基线源文件数 | 5 |
| 内部依赖 | 无 |
| 外部依赖 | `libs.kotlinx.coroutines.core`, `kotlin("test")` |

## 关键源码

- `CredentialIssue.kt`：认证/凭据相关的稳定问题语义。
- `Hashing.kt`：`md5` expect 声明。
- `Hashing.android.kt`：Android/JVM MD5 actual。
- `Hashing.ios.kt`：iOS MD5 actual。

## 约束

- 保持工具级职责，不向业务聚合层膨胀。
- commonMain 不导入平台 API；平台差异必须留在对应 source set。
- 哈希能力用于兼容远端契约，不应扩展成通用加密库。
