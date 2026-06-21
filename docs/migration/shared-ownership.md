# shared 兼容模块归属

`shared` 在 L1 阶段只作为历史兼容模块保留，不再承接新 Feature、新 Repository 或新的跨平台业务模型。
新增业务代码应优先进入 `feature/*` 或 `core/*`，除非当前任务明确属于遗留维护。

## 当前边界

| 遗留区域 | 当前职责 | 目标归属 | 删除条件 |
|---|---|---|---|
| `shared/data/remote/api` | 标准模型目录 API、Audio API、Auth 旧 `RunningHubApi`、Plaza、WebApp 公开目录和 WebApp Task API 均已迁出或删除；shared 不再承载业务远端 API。 | 对应 Feature 的 `data` 模块或 `core:network`。 | 删除 shared 中已无人使用的业务远端 API 目录。 |
| `shared/data/remote/dto` | 标准模型 DTO/响应信封、Audio DTO、Auth/User DTO、Plaza DTO、WebApp 公开目录 DTO 和 WebApp Task DTO 均已迁出或删除；shared 不再承载业务 DTO。 | 对应 Feature 的 `data/remote/dto`。 | 删除 shared 中已无人使用的业务 DTO 目录。 |
| `shared/data/repository` | 标准模型 Data、Audio Data、Auth/User/ProfileCredential/BalanceSnapshot/SessionRestore、Plaza、WebApp 公开目录和 WebApp Task 实现均已迁出或删除；shared 不再承载业务 Repository 实现。 | 对应 Feature 的 `data/repository`。 | 删除 shared 中已无人使用的业务 Repository 目录。 |
| `shared/domain/model` | 旧 UI 和旧仓库共享的业务模型；权限模型、WebApp 任务模型、统一生成历史模型、Plaza 模型、Audio 模型、标准模型目录/调用模型，以及 `User`、`WebApp`、`Tag`、`PageData`、`AppDetail` 的旧 typealias 兼容层均已迁出或删除。 | `core:model`、`core:storage` 或对应 Feature Domain。 | shared domain 不再承载业务模型。 |
| `shared/domain/repository` | 旧仓库接口；统一生成历史仓库契约、WebApp 任务执行仓库契约和 WebApp 任务历史仓库契约已迁出到 `feature:task:domain`，Plaza 仓库契约已迁出到 `feature:community:domain`，Audio 仓库契约已迁出到 `feature:audio:domain`，标准模型目录和调用仓库契约已迁出到 `feature:model:domain`。 | 对应 Feature Domain。 | shared domain 不再承载仓库契约。 |
| `shared/data/local` | 旧设置存储实现、权限状态临时实现、DataStore 平台工厂均已迁到 `core:storage`；当前不再承担生产启动图的本地存储职责。 | `core:storage` 或平台 source set。 | 删除 shared 中已无人使用的 local 包；敏感凭据迁移到安全存储属于 L2。 |
| `shared/di` | 旧 Koin 组合入口；Auth、Community、Discovery、Task、Audio 和标准模型 Data 绑定均已迁到对应 Feature Data 模块，Android/iOS 生产启动层直接装配平台 runtime module 和 Feature Data 模块，不再装配 `sharedModule`。 | `composeApp/di` 只做装配，具体实现进入 Feature/Core。 | 删除或继续瘦身未接入生产启动图的 shared 旧兼容模块。 |

## 自动门禁

- `docs/migration/shared-baseline.txt` 固定 AC-11 时的 `shared` Git 跟踪文件基线。
- `./gradlew checkArchitectureBoundaries` 会拒绝未登记的新 `shared` 文件。
- 删除 `shared` 文件不需要同步修改基线；减少遗留面是允许的。
- 只有遗留维护任务才能更新基线，且必须在本文件补充目标归属和删除条件。

## 当前允许使用方

`docs/migration/shared-allowlist.txt` 只登记迁移期仍需从 `composeApp` 引用 `shared` 的文件。
当前 `composeApp/commonMain` 和 Android 启动层均不再依赖 `shared`，DataStore 初始化也已迁到 `core:storage`；
清单没有任何允许项。
该清单不是扩展点；新增条目必须说明为什么不能进入 Feature/Core，以及后续删除条件。

## 历史 androidApp 清理

`androidApp/` 的历史 Android 入口源码已删除，Git 跟踪文件不再保留旧
`RunningHubApplication.kt`、`MainActivity.kt`、`AndroidManifest.xml` 或
`build.gradle.kts`。`settings.gradle.kts` 当前没有 include `:androidApp`，
当前 Android 生产入口以 `composeApp/src/androidMain` 为唯一来源。

本地 `androidApp/build` 可能因历史构建残留而继续存在；它属于忽略的构建产物，
不代表 Gradle 模块、生产启动图或 `sharedModule` 依赖。后续如果决定恢复独立
Android App 模块，必须先新增独立 AC，并同步更新 `settings.gradle.kts`、本文件和
对应验证命令。
