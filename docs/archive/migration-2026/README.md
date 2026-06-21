# RunningHub 2026 Migration Archive

本目录用于放置已封板后的迁移历史材料。当前 L1 尚未完成当前 HEAD 外部证据封板，
因此本目录暂只保留归档边界说明，不移动 `docs/migration/` 下仍被门禁读取的活动证据。

## 归档条件

以下条件全部满足后，才允许把迁移历史移动到本目录：

1. `checkL1SealEvidence` 基于当前 HEAD 通过。
2. Android/iOS GitHub Actions 证据、Android 运行观察证据、macOS iOS link/Simulator 证据均已更新。
3. `ARCHITECTURE.md` 和 `DEVELOPMENT.md` 已能独立指导日常开发。
4. 根工程正常验证不再依赖聊天记录解释迁移状态。

## 保留原则

归档后，仓库根文档只保留当前架构、开发命令和强规则；迁移过程、旧 Gate 证据和复核过程留在
archive，避免后续 AI 任务反复加载过期历史并重新规划已封板边界。
