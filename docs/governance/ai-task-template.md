# AI Task Template

本模板用于启动 RunningHub 仓库内的 AI 协作任务。它把复核文件中的上下文控制规则转成
可复制的任务输入，避免后续协作者让 AI 自动重新读取完整迁移历史或扩大修改范围。

```text
任务 ID：
唯一目标：
允许修改目录：
禁止修改目录：
验收命令：
最大重试次数：
完成后更新的文档：
已知外部依赖：
未验证项记录位置：
```

## 填写规则

- `任务 ID` 使用 issue、PR、AC、Gate 或团队内部编号；没有编号时写明临时编号来源。
- `唯一目标` 只描述本次要完成的一个结果，不混入后续排队事项。
- `允许修改目录` 和 `禁止修改目录` 必须具体到目录或文件；不允许写“全仓随意改”。
- `验收命令` 必须是可执行命令、人工验收步骤或外部证据采集入口。
- `最大重试次数` 用于限制自动化循环；超过次数后必须记录证据并交回人类判断。
- `完成后更新的文档` 应指向当前态文档、治理文档或迁移证据，不依赖聊天记录。
- `已知外部依赖` 写明 CI、设备、账号、真机、macOS、服务端接口或人工授权。
- `未验证项记录位置` 用于承接无法在当前环境验证的风险，禁止把未执行写成通过。

## 示例

```text
任务 ID：RH-L1-P9-001
唯一目标：为视频预览流程补充性能基线采集说明和本地防退化检查。
允许修改目录：docs/governance/、build-logic/
禁止修改目录：composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/
验收命令：./gradlew checkLongTermGovernance
最大重试次数：2
完成后更新的文档：docs/governance/performance-baselines.md
已知外部依赖：真实 iOS 性能数据需要 macOS runner 或真机。
未验证项记录位置：docs/migration/current-state.yaml
```
