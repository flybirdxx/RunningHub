# Performance Baselines

## 指标

RunningHub 长期维护以下 Android/iOS 性能基线：

### cold-start

- 冷启动和热启动耗时。

### first-render

- 首屏渲染耗时。

### steady-memory

- 常驻内存和峰值内存。

### image-cache

- 图片缓存命中、解码耗时和大图内存峰值。

### video-preview

- 视频预览加载耗时、播放错误率和释放时机。

### long-polling-network-idle

- 长轮询、任务刷新和 Tab 切换后的后台网络 in-flight 数量。

## 采集要求

- Android 使用 release 或接近 release 的构建采集，避免 debug 日志显著影响结果。
- iOS 使用真实 macOS runner 或真机/Simulator 明确记录设备、系统版本和构建配置。
- 性能 PR 必须说明基线、变更后数据、采集命令和不可比因素。

## 风险边界

没有设备或 macOS 环境时，不得声称 iOS 性能通过；只能说明未验证项和剩余风险。

## 自动门禁

`checkLongTermGovernance` 会读取 `docs/governance/performance-baseline-targets.txt`，
确保冷启动、首屏、内存、图片缓存、视频预览和长轮询网络空闲等指标都有平台、证据产物和责任域。
目标表中的证据路径必须指向仓库内真实文件；Markdown `#anchor` 必须有对应章节，避免指标目标
漂移成死链接。
该门禁只保证指标定义完整，不代表当前 Android 或 iOS 性能数据已经通过。
