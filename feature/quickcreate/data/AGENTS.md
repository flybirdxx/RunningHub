# feature:quickcreate:data Instructions

## Responsibility

本模块实现 `feature:quickcreate:domain` 中的仓库接口，并负责远端 API、DTO、Mapper、Data DI 和数据层错误映射。

## Allowed

- 当前 Feature 的 Domain。
- Core network/storage/common/model。
- Ktor、kotlinx.serialization、SQLDelight 或 DataStore，仅限确有数据层职责时。
- Koin module 只绑定本 Feature Data 实现。

## Forbidden

- 依赖 `composeApp`。
- 依赖任何 Feature Presentation。
- 依赖其他 Feature Data 实现。
- 导入 `com.runninghub.shared.*`。
- 直接输出最终 UI 文案。
- 裸 `println`、空 `catch`、生产 `runBlocking`。
- 日志输出 Token、Cookie、API Key、验证码 token、密码、请求体或完整认证头。

## Feature Rules

- 该模块不得依赖 `shared`。
- 一个实现同时实现多个窄接口只允许作为过渡；新增职责应拆分协作者。
- 计费预览、提交和上传必须共享同一参数快照或请求指纹。
- 任务成功、失败、取消、超时都必须停止轮询。

## Testing

新增或修改 API/DTO/Mapper/Repository 时必须补测试：

- 请求路径和请求体；
- DTO 反序列化；
- DTO-to-Domain 映射；
- 网络失败和业务失败；
- token 失效重试；
- 兼容字段和空响应。

## 注释与交付

- 新增或修改 public/internal Kotlin 类型、函数和关键字段必须遵守根目录中文注释规范。
- 复杂流程注释解释业务原因、边界条件、并发约束和失败策略，不复述语法。
- 任务结束时说明实际执行的验证命令；未执行项必须说明原因和剩余风险。
