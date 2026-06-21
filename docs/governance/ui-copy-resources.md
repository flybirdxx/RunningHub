# UI Copy and Compose Resources Governance

## 目标

用户可见文案逐步迁移到 Compose Resources，清理乱码和硬编码，避免 Data 层生成最终 UI 文案。

## 新增文案规则

- 新增用户可见文案优先使用 Compose Resources。
- Data 层只返回稳定错误码、状态码或领域错误，不返回最终界面中文句子。
- Presentation 层负责把领域错误映射为资源化文案。
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
