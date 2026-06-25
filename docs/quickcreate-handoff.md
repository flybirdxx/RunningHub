# QuickCreate Handoff

本文档记录快捷创作当前态、已知未完成问题和后续接手入口。它用于承接后续协作者，不替代
`DEVELOPMENT.md` 中的验证要求。

## 2026-06-26 模型目录与快捷创作面板交接

本轮已完成快捷创作模型目录接入、模型选择 sheet 分类展示、模型列表快照、首次安装预加载、
最近选择模型持久化、图片模型按次计费跳过无意义价格确认、输入法顶起位置回归修正，以及模型
sheet 右上角快速关闭入口。

已知问题，单独后续修复：

1. 视频分类切换后，顶部模型标签会先显示“全能视频 S”，等待短时间后跳转为 `Seedance2.0`。
   后续需要检查视频模型默认选择、缓存目录回填和远端刷新选择优先级，避免 UI 先显示临时首项再
   被刷新结果改写。
2. 有一组“开源自部署”模型缺少常用可配置参数。后续需要比对 RunningHub API 页面中的模型分组、
   筛选分类和接口返回字段，确认是接口未返回参数，还是 DTO/Mapper 过滤或归类时丢失字段。
3. 快捷创作底部的模型与参数圆角胶囊组件在部分机型/文本组合下会多出明显空白宽度。后续需要
   检查 compact composer 中按钮、文本截断和 Row 权重约束，保证模型名较短或较长时胶囊宽度都
   贴合设计。

本轮验证：

- `.\gradlew.bat :feature:quickcreate:presentation:allTests`
- `.\gradlew.bat :composeApp:assembleDebug`
- `git diff --check`
- adb 安装并启动 `com.runninghub.app.debug`，进程存活且启动日志未发现 `FATAL EXCEPTION`、
  `AndroidRuntime` 或 Koin 注入错误。

未验证项：

- 未进行真实生成和扣费。
- 未在 macOS/iOS Simulator 验证。
- 上述 3 个已知问题仅记录，不在本轮修复范围内。
