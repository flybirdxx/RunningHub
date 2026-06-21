# ADR 0002: Runtime Environment and Secure Storage

日期：2026-06-21

## 决策

API 环境由平台 runtime module 注入，生产默认环境通过 `ApiEnvironment` 明确声明 origin、
API base URL 和可信 Host。认证凭据通过 `SecureCredentialStore` 存储：
Android 使用 Keystore backed 加密存储，iOS 使用 Keychain。
Android 通过 build type 注入的 `RUNNINGHUB_*` BuildConfig 字段覆盖环境，iOS 通过同名
进程环境变量覆盖环境；未配置 staging/dev 时默认回退 production。

## 原因

硬编码 Production 环境会阻碍 staging/dev 回归；普通偏好存储不适合保存 access token、
refresh token、Cookie 或 API Key。环境配置与凭据存储必须在平台启动层隔离，避免泄漏到
Domain 或 UI 状态。

## 约束

- 可信 Host 使用精确白名单，不使用 `contains` 判断。
- Runtime module 不得退回普通 Preferences 作为生产认证凭据存储。
- 日志不得输出 Token、Cookie、API Key、密码或完整认证请求头。
- staging/dev 真实地址登记前，不在源码中伪造环境。

## 后果

测试可通过注入 `ApiEnvironment` 覆盖 Host 和 base URL；旧凭据通过迁移包装懒迁移到安全存储。
