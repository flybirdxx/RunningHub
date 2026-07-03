# proactive-correction Agent

用途：在初始化、较大重构、测试失败或规则变更后主动扫描自洽性、存量风险和遗漏验证。

## 维度 1：规则自洽性

- [ ] 根 `AGENTS.md`、`.codex/rules/project_rule.md` 与局部 `AGENTS.md` 没有互相矛盾的依赖方向。
- [ ] `.codex/settings.json`、hooks、skills、agents 中没有双大括号形式的模板占位符。
- [ ] CodeGraph 状态与 `settings.gradle.kts` 模块清单、`.codex/references/*.md` 覆盖一致。
- [ ] 新增文档脱离聊天上下文后仍可理解执行。

## 维度 2：存量代码合规性

- [ ] 未新增 `shared` 依赖或导入。
- [ ] 未新增生产 `runBlocking`、`GlobalScope`、空 `catch`。
- [ ] 未新增敏感日志或明文凭据。
- [ ] 未新增旧 QuickCreate 入口或 ScreenModel 状态回流。
- [ ] 未新增 `composeApp/commonMain` 到 Data 实现的依赖。

## 维度 3：验证合理性

- [ ] 文档-only 改动至少完成占位符扫描和目标文件存在性检查。
- [ ] 代码改动运行了与风险匹配的 Gradle task。
- [ ] UI 改动有真实渲染、截图、运行观察或可复现交互验证。
- [ ] 无法在当前机器验证的部分明确记录原因和补验条件。

## 推荐命令

```powershell
rg -n "\{\{[A-Za-z0-9_:-]+\}\}" AGENTS.md .codex
python - <<'PY'
import re, pathlib
# 模块清单以 settings.gradle.kts 为权威来源（历史 _scan.json 可选，默认不生成）。
settings = pathlib.Path('settings.gradle.kts').read_text(encoding='utf-8')
modules = sorted(m.lstrip(':').replace(':', '_') for m in re.findall(r'include\("(:[^"]+)"\)', settings))
docs = {p.stem for p in pathlib.Path('.codex/references').glob('*.md') if p.stem not in {'dependencies','conventions'}}
missing = [m for m in modules if m not in docs]
print('modules', len(modules), 'docs', len(docs), 'missing', missing)
PY
codegraph status
```
