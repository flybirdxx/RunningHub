# UI Copy and Compose Resources Governance

## 目标

用户可见文案逐步迁移到 Compose Resources，清理乱码和硬编码，避免 Data 层生成最终 UI 文案。

## 新增文案规则

- 新增用户可见文案优先使用 Compose Resources。
- Data 层只返回稳定错误码、状态码或领域错误，不返回最终界面中文句子。
- Presentation 层负责把领域错误映射为资源化文案。
- QuickCreate Presentation 错误语义统一映射到 `quick_create_error_*` Compose Resources，
  不在 Presentation 状态或错误端口保存最终中文文案。
- QuickCreate 本地兼容模型名使用 `ImageModelLabel`/`VideoModelLabel` 稳定语义，
  固定中文名称映射到 `quick_create_image_model_*` 与 `quick_create_video_model_*` Compose Resources。
- QuickCreate 服务字段校验使用 `QuickCreationServiceValidationIssue` 稳定语义，
  服务端字段标题只作为运行时参数传入 `quick_create_runtime_service_*` Compose Resources 格式串。
- Plaza 不再维护本地 fallback 内容目录；广场标签、卡片简介、作者、媒体类型和指标只来自接口返回。
  缺媒体 URL 时 UI 只能显示占位视觉，不得生成本地作品数据或 `plaza_fallback_*` 文案资源。
- Core 权限模型只保存 `PermissionTextKey` 稳定语义，权限引导弹层通过 `permission_*`
  Compose Resources 映射说明和用途摘要。
- 临时硬编码文案必须写明原因、风险和删除条件。

## 迁移顺序

1. 登录、权限、验证码、上传和支付等高频错误文案。
2. QuickCreate 表单、计费和任务状态文案。
3. Discovery、Plaza、History 和 Profile 页面文案。
4. 旧乱码、英文占位和重复文案。

## 与模块拆分的关系

大体量页面在迁移文案时应优先同步拆分 StateHolder、子组件或 Feature Presentation 模块。
`checkLongTermGovernance` 已用 Feature 行数基线阻止无 Presentation 模块的历史页面继续膨胀；
文案资源化不应成为继续向 `composeApp` 大文件追加 UI 分支的理由。

## 自动门禁

`checkLongTermGovernance` 会读取 `docs/governance/ui-copy-hardcoded-baseline.txt`。
该基线记录当前迁移期仍存在硬编码用户可见文案的文件和匹配数量；扫描范围覆盖
`composeApp/src/commonMain/kotlin` 的 Compose UI 文案，以及 `feature/*/presentation/src/commonMain/kotlin`
中集中生成的 Presentation 文案。新增硬编码文案文件，或现有文件匹配数量超过基线，都会失败。
减少硬编码数量不会失败，后续可逐步下调基线。

## 盘点与批量收口

当前剩余问题清单见 `docs/governance/ui-copy-message-inventory.md`。
处理用户可见提示、错误、空态、按钮、无障碍描述、fallback 文案或 `Throwable.message`
展示风险时，必须先按该清单确认分类，再按语义批量覆盖调用点、测试和治理基线。
不得只围绕单个页面或单个文件反复提交碎片化修正。
