# .codex 初始化记录

## v1.0.0 - 2026-06-27

- 完成 RunningHub AI 辅助体系初始化。
- 基于 25 个 Gradle 模块、Kotlin Multiplatform/Compose Multiplatform 架构和 CodeGraph 索引生成规则。
- 渲染 `.codex/rules/`、`.codex/skills/`、`.codex/agents/`、`.codex/references/` 与 hooks。
- 将根 `AGENTS.md` 从脚手架占位替换为可执行的项目协作入口。
- `_scan.json` 使用 CodeGraph 轻量模式；模块细节由 CodeGraph 与 `.codex/references/*.md` 补充。
