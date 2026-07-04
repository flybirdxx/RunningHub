# 快捷创作结果卡 v2(叠加式)实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补上批 0 遗漏的对话流结果卡重构:按定案 mockup A(整图叠加式)重做生成中/完成态结果卡;完成态只保留「下载」「复制到素材区」两个叠加操作;新增长按图片弹工具条(收纳再来一张/复用参数/复制 Prompt);「下载」接通 Android 本地保存(全 app 首个媒体保存平台实现)。

**定案记录(2026-07-05,用户确认,v3 mockup 定案):**
1. 方案 A 整图叠加式:状态徽/过期徽叠图顶部,操作叠图底部 scrim;任务 ID 移入详情,不占对话流。
2. 完成态操作只留两个:**下载**(保存到本地相册,半透明白圆钮)+ **复制**(把结果图填入 composer 上传素材区,橄榄实心圆钮为主操作)。
3. **长按图片弹出工具条**(占位,首版收纳现有已接线动作:再来一张/复用参数/复制 Prompt;长按时图片橄榄描边高亮,点空白收起)。
4. 生成中态:流体蒙版铺满整卡 + 「x%」状态小徽叠加;不加取消按钮(本批不引入新任务控制流)。
5. 失败/取消态:无结果图,保持现有紧凑信息卡形态(仅重皮对齐,不叠加)。
6. **卡片尺寸规则(v3 核心)——比例驱动 + 宽高双上限,聊天气泡式不再全宽:**
   - `maxCardWidth = 可用内容宽 × 0.70`(头像与间距之外的行宽为基数),`maxCardHeight = 320.dp`;
   - `cardWidth = min(maxCardWidth, maxCardHeight × ratio)`,`cardHeight = cardWidth / ratio`;
   - ratio 来源:生成中 = 提交参数快照 `item.aspectRatio`(解析 "9:16"/"3：4"/"16/9" 形式,恢复原 `quickCreateConversationGeneratingAspectRatio` 的解析逻辑与测试);完成 = 服务端宽高优先、图片 intrinsic 兜底(现有 `quickCreateConversationResultAspectRatio`);
   - ratio 未知(旧任务/无参数)回退 1:1;
   - 生成中与完成同一规则同一比例,出图无缝替换;
   - 徽标只留图标/百分比短文案,圆钮 30dp,随小卡收敛。
   - 数据链路即现有 StateFlow 观察者(参数快照 → UiState → Compose 订阅),不引入新机制。

**Architecture:**
- 动作真源在 `feature:quickcreate:presentation` 的 `quickCreateResultActions`(SUCCESS 分支改为 Download + CopyToComposer),UI 只消费。
- 新叠加卡组件进 `composeApp/ui/designsystem/components/result/ConversationResultCard.kt`;历史抽屉的 `ResultPreview` 保持不动(非本批范围)。
- 「复制到素材区」复用现有远端引用机制(Plaza 用同款的 `referenceMediaUrl` 先例);实施时核实 coordinator 对 https 引用的上传语义。
- 「下载」新增 expect/actual 平台边界(`composeApp/platform`),Android 用 MediaStore(API 29+ 免权限),iOS actual 留 TODO 占位并在交付说明未验证。
- 媒体叠加层的 `Color.Black/White` scrim/徽底延续批 1 Hero 封板例外,加中文注释。

**强制规约:** 禁实验性布局 API;色只取 RhTheme(叠加层例外注释);文案进 Compose Resources;commit path-limited;不碰用户在制改动;禁 git reset。

---

### Task 1: presentation 动作模型收敛

**Files:** `feature/quickcreate/presentation/.../result/QuickCreateResultUiModel.kt` + commonTest

- [ ] `QuickCreateResultAction` 增加 `CopyToComposer`(把结果图作为素材引用填入 composer)。
- [ ] `quickCreateResultActions` SUCCESS 分支:`hasResults` 时改为 `[Download, CopyToComposer]`;不再产出 ViewResult/TryAgain/Save/ReuseParameters/CopyPrompt(TryAgain/ReuseParameters/CopyPrompt 移入长按工具条,由 UI 层直接组装,不走动作列表)。
- [ ] QUEUING/RUNNING 分支改为 `emptyList()`(生成中叠加卡不显示动作按钮;ViewTask 收进长按工具条后续扩展位)。
- [ ] FAILED/CANCELED 分支保持不变。
- [ ] 更新 `feature:quickcreate:presentation` commonTest 中动作断言。
- [ ] Run: `.\gradlew.bat --console=plain :feature:quickcreate:presentation:testAndroidHostTest`

### Task 2: designsystem 叠加结果卡组件

**Files:** Create `composeApp/.../designsystem/components/result/ConversationResultCard.kt`(+ commonTest 契约测试可选)

- [ ] 状态模型:媒体(url/previewUrl/mediaType/aspectRatio)、statusLabel+RhTaskStatus(顶左徽)、expiryLabel(顶右徽)、overlayActions(圆钮列表:icon+contentDescription+enabled)、onLongPress 回调、mediaContent slot。
- [ ] 布局按定案第 6 条尺寸规则:`BoxWithConstraints` 或调用方传入可用宽,`cardWidth = min(maxW×0.70, 320.dp×ratio)`、`cardHeight = cardWidth/ratio`;ratio 未知回退 1:1;卡片整体即媒体(叠加式,无图下信息块);圆角整卡。纯尺寸计算函数抽为顶层 internal 函数并配 commonTest(9:16/16:9/1:1/未知 四例)。
- [ ] 顶部徽:半透明黑底胶囊(`Color.Black.copy(alpha≈0.55)`,媒体叠加例外注释),状态色文字。
- [ ] 底部 scrim:透明→黑渐变,scrim 上圆钮行(右对齐或均布,32dp 圆钮,半透明白底)。
- [ ] `combinedClickable`(稳定 API,非实验性——实施前核实当前 compose 版本签名;若为实验性则用 `pointerInput` + `detectTapGestures(onLongPress)`)接整卡点击与长按。

### Task 3: 对话流接线 + 长按工具条

**Files:** `composeApp/.../quickcreate/QuickCreateConversationContent.kt`、`QuickCreateScreen.kt`、strings.xml

- [ ] `GeneratedPosterCard` 成功/生成中态改用 `ConversationResultCard`;失败/取消态维持现有 `ResultPreview` 紧凑卡。
- [ ] 生成中:蒙版铺满(已修)+ 顶左「生成中 x%」徽;无操作钮。
- [ ] 完成:顶左「生成完成」徽 + 顶右「24 小时后过期」徽 + 底部圆钮 [下载, 复制]。
- [ ] 长按 → 底部弹出工具条(轻量 Surface 行,含:再来一张/复用参数/复制 Prompt,复用现有 handler;点外关闭)。
- [ ] 新文案(下载/复制到素材/工具条项)进 Compose Resources。

### Task 4: 复制到素材区

**Files:** `QuickCreateScreen.kt`(handleResultAction)+ 必要时 presentation 增补

- [ ] `CopyToComposer` → 把 `result.url` 填入当前 tab 的媒体引用(优先 `pickImageReference(url)` 直通;核实 coordinator/上传流程对 https 引用的处理,若需要走 Plaza reuse 的 `referenceMediaUrl` 路径则复用之)。
- [ ] 成功后 RhSnackbar 提示「已添加到素材」。
- [ ] 视频结果:本批只处理图片结果的复制;视频结果隐藏复制钮(动作模型按 mediaType 过滤)。

### Task 5: 下载到本地(Android)

**Files:** Create `composeApp/.../platform/MediaSaver.kt`(expect)+ androidMain actual + iosMain actual(TODO 占位)

- [ ] expect `suspend fun saveImageToGallery(url: String, fileName: String): Result<Unit>`(命名实施时定,走结构化协程,可取消)。
- [ ] Android actual:Ktor 下载字节 → MediaStore.Images 插入(API 29+ scoped storage 免权限;minSdk 若 <29 则加 WRITE_EXTERNAL_STORAGE 兼容分支或本批限 29+,实施时按 minSdk 定)。
- [ ] iOS actual:返回未实现 Result.failure + TODO 注释(Windows 无法验证,交付说明)。
- [ ] `Download` action 接通:下载中钮置 loading/禁用,完成/失败 RhSnackbar 反馈(文案进资源,不透传原始错误)。
- [ ] URL/凭据不入日志。

### Task 6: 真机验收(一次真实生成覆盖全链路)

- [ ] `checkArchitectureBoundaries` + `:composeApp:testDebugUnitTest` + `:feature:quickcreate:presentation:testAndroidHostTest` 全绿。
- [ ] 模拟器真实生成一单:蒙版铺满流动、完成叠加卡(徽+2 钮)、长按工具条、复制→素材区出现引用、下载→系统相册可见,逐项截图。
- [ ] 更新 `docs/superpowers/specs/2026-07-03-ui-redesign-design.md`:登记「对话流结果卡 v2 封板」,勾销批 0 顺延的对话流验收债。

**未验证项(交付声明):** iOS 编译/渲染/保存(Windows 无 macOS 证据);国产 ROM 相册写入差异(仅模拟器验证)。
