# RunningHub module AGENTS package

生成目的：为 `feature/kmp-refactoring` 最新模块图中缺失局部 `AGENTS.md` 的目录补齐模块级规则。

当前审查到的模块来自 `settings.gradle.kts`：

- core: model/common/network/storage
- feature: auth/community/discovery/detail/task/audio/model/quickcreate
- composeApp 已有 `composeApp/AGENTS.md`，本包不覆盖它
- iosApp 是 Xcode 包装工程，本包也提供局部规则
- build-logic 是 included build，本包提供局部规则

## 使用方式

在仓库根目录解压本 ZIP。建议先预览：

```bash
unzip -l runninghub-missing-agents.zip
```

解压：

```bash
unzip runninghub-missing-agents.zip -d .
```

Windows PowerShell：

```powershell
Expand-Archive .\runninghub-missing-agents.zip -DestinationPath . -Force
```

## 注意

- 本包只添加缺失的局部 `AGENTS.md`，不覆盖根目录 `AGENTS.md` 和已存在的 `composeApp/AGENTS.md`。
- 如果某个目录后来已经手动添加了 AGENTS，请先 diff 再覆盖。
- 添加后建议执行：

```bash
./gradlew checkArchitectureBoundaries checkLongTermGovernance
```
