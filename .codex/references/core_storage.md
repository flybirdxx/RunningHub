# core_storage

## 模块概述

`:core:storage` 提供跨平台本地存储接口、DataStore 工厂、权限状态、缓存和安全凭据存储边界。Android 使用 Keystore，iOS 使用 Keychain。

## 元信息

| 项 | 值 |
|---|---|
| Gradle path | `:core:storage` |
| 路径 | `core/storage` |
| 类型 | library |
| 命名空间 | `com.runninghub.core.storage` |
| 完整扫描基线源文件数 | 22 |
| 内部依赖 | `:core:common` |
| 外部依赖 | DataStore Preferences, coroutines |

## 关键源码

- `CredentialStore.kt` / `SecureCredentialStore.kt`：凭据读写接口与安全实现工厂。
- `DataStoreFactory.kt`：跨平台 DataStore expect/actual。
- `Permission.kt`、`PermissionStateStore.kt`、`PermissionDataStore.kt`：权限状态模型和存储。
- `AppStartupStore.kt`、`BalanceCache.kt`、`ModelCatalogCacheStore.kt`：轻量缓存和启动状态。
- `QuickCreateDraftStore.kt`、`QuickCreateModelSelectionStore.kt`：QuickCreate 本地草稿与选择缓存。
- `AndroidSecureCredentialStore.kt`、`IosKeychainCredentialStore.kt`：平台敏感凭据实现。

## 约束

- 敏感凭据不得退回普通 Preferences；Preferences 只可作为迁移源或非敏感缓存。
- commonMain 保持接口和模型，平台细节进入 androidMain/iosMain。
- 新增存储键必须考虑迁移、清理和隐私边界。
