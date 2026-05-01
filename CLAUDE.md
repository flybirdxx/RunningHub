# RunningHub - AI Application Hub Mobile Client
KMP + Compose Multiplatform | Android + iOS 双端

Tech Stack: Kotlin 2.1.20, Compose Multiplatform 1.7.3, Koin 4.0.4, Ktor 3.1.2, SQLDelight 2.0.2, Coil 3.1.0

<directory>
shared/ - KMP 共享业务模块 (commonMain/androidMain/iosMain)
  data/ - 远程 API (Ktor)、本地缓存 (SQLDelight)、DTO 模型
  domain/ - 纯 Kotlin Domain 层 (model/repository/usecase)
  di/ - Koin DI 模块
  platform/ - expect/actual 平台抽象

composeApp/ - 跨平台 UI 模块 (commonMain/androidMain/iosMain)
  ui/theme/ - Design Token (AppColors/AppTypography/AppDimens)
  ui/component/ - 共享组件库 (AppCard/SearchBar/CategoryChip/StatusBadge)
  ui/feature/ - 业务屏幕 (discovery/detail/profile/search/creator/community)
  ui/navigation/ - 路由与导航
  di/ - ViewModel Koin 模块

gradle/ - Gradle Wrapper + libs.versions.toml 版本目录
</directory>

<config>
gradle/libs.versions.toml - 统一依赖版本管理
build.gradle.kts - 顶级 KMP 插件声明
settings.gradle.kts - 模块成员: shared, composeApp
gradle.properties - JVM/KMP 环境变量
CLAUDE.md - L1 项目宪法·全局地图·技术栈
</config>

法则: 极简·稳定·导航·版本精确·共享优先·平台隔离

语言: 中文优先 — 所有回复以中文为第一语言，保持沟通流畅。

[PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
