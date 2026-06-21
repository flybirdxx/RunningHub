# shared 兼容模块归属

`shared` 在 L1 阶段只作为历史兼容模块保留，不再承接新 Feature、新 Repository 或新的跨平台业务模型。
新增业务代码应优先进入 `feature/*` 或 `core/*`，除非当前任务明确属于遗留维护。

## 当前边界

| 遗留区域 | 当前职责 | 目标归属 | 删除条件 |
|---|---|---|---|
| `shared/data/remote/api` | 未迁移业务的 Ktor API 调用；Auth API 已迁出到 `feature:auth:data`，Plaza API 已迁出到 `feature:community:data`，WebApp 公开目录 API 已迁出到 `feature:discovery:data`，WebApp Task API 已迁出到 `feature:task:data`。 | 对应 Feature 的 `data` 模块或 `core:network`。 | Audio 等剩余功能完成 Feature/Data 拆分，旧 Auth/Plaza/Discovery/Task 调用点全部删除。 |
| `shared/data/remote/dto` | 未迁移接口 DTO 与旧响应兼容；Auth/User DTO 已迁出到 `feature:auth:data`，Plaza DTO 已迁出到 `feature:community:data`，WebApp 公开目录 DTO 已迁出到 `feature:discovery:data`，WebApp Task DTO 已迁出到 `feature:task:data`。 | 对应 Feature 的 `data/remote/dto`。 | 对应 Repository 不再从 `composeApp` 直接依赖 `shared`，旧 Auth/Plaza/Discovery/Task DTO 无使用方。 |
| `shared/data/repository` | Audio 等旧仓库实现；Auth/User/ProfileCredential/BalanceSnapshot 实现已迁出到 `feature:auth:data`，Plaza 实现已迁出到 `feature:community:data`，WebApp 公开目录实现已迁出到 `feature:discovery:data`，WebApp Task 实现已迁出到 `feature:task:data`。 | 对应 Feature 的 `data/repository`。 | Presentation 只依赖窄 Domain Repository，组合根完成实现绑定迁移。 |
| `shared/domain/model` | 旧 UI 和旧仓库共享的业务模型；权限模型、WebApp 任务模型、统一生成历史模型和 Plaza 模型已迁出，Audio 等遗留模型暂留。 | `core:model`、`core:storage` 或对应 Feature Domain。 | 所有使用方完成模型迁移并删除 `composeApp` allowlist 条目。 |
| `shared/domain/repository` | 旧仓库接口；统一生成历史仓库契约、WebApp 任务执行仓库契约和 WebApp 任务历史仓库契约已迁出到 `feature:task:domain`，Plaza 仓库契约已迁出到 `feature:community:domain`。 | 对应 Feature Domain。 | `composeApp` 不再直接引用这些接口。 |
| `shared/data/local` | 旧设置存储实现、权限状态临时实现、DataStore 平台工厂均已迁到 `core:storage`；当前不再承担生产启动图的本地存储职责。 | `core:storage` 或平台 source set。 | 删除 shared 中已无人使用的 local 包；敏感凭据迁移到安全存储属于 L2。 |
| `shared/di` | 旧 Koin 组合入口；Auth Data 绑定已迁到 `feature:auth:data`，Community Data 绑定已迁到 `feature:community:data`，Discovery Data 绑定已迁到 `feature:discovery:data`，Task Data 绑定已迁到 `feature:task:data`，Android 生产启动层已改为装配 `androidRuntimeModule`，不再装配 `sharedModule`。 | `composeApp/di` 只做装配，具体实现进入 Feature/Core。 | 删除或继续瘦身未接入生产启动图的 shared 旧兼容模块。 |

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

## 历史目录说明

`androidApp/` 仍作为 Git 跟踪的历史 Android 目录存在，且其中 `RunningHubApplication.kt`
保留了旧 `sharedModule` 引用。`settings.gradle.kts` 当前没有 include `:androidApp`，
因此该目录不属于 L1 当前 Gradle 模块图、生产启动图或 `checkArchitectureBoundaries`
的 composeApp allowlist 范围。

后续如果决定恢复、删除或迁移 `androidApp/`，必须先新增独立 AC，并同步更新
`settings.gradle.kts`、本文件和对应验证命令；在当前 AC-11 中不得把该历史目录当作
`composeApp` 仍依赖 `shared` 的证据。
