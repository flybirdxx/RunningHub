# 快捷创作重构开发进度表

更新时间：2026-06-18
分支：`feature/kmp-refactoring`
依据：Web 抓包结果、移动端截图结构、`doc/quick_creation_feature_plan.md`

## 总体状态

| 阶段 | 范围 | 状态 | 完成度 | 说明 |
| --- | --- | --- | --- | --- |
| Phase 0 | v2 契约落地 | 已完成 | 100% | 已补 `prepare/commit/list/detail` 等 DTO，并用抓包样例做序列化/反序列化测试。 |
| Phase 1 | 图片 G-2.0 MVP | 部分完成 | 70% | 默认 `all-power-image-g2` 已切到 Web v2 的 `fee-preview -> prepare -> commit -> list`；图片提交会优先携带当前服务端模型的 `categoryId/bindingId/skuId`；真实端到端扣费生成尚未在 App 内复测。 |
| Phase 2 | 页面结构重构 | 部分完成 | 80% | 页面已按移动端截图边界调整为顶部轻量“创作/灵感”、中间可滚动内容区、底部固定模型参数和提示词输入面板；底部输入区已显示服务端模型摘要，Tune 高级页可切换服务端图片模型。 |
| Phase 3 | 灵感接口接入 | 部分完成 | 82% | 已接 tags/templates 列表和真实 template/detail；UI 使用真实 state 渲染，点击模板可“制作同款”并回填分类、服务端模型、prompt、基础参数和远端素材。 |
| Phase 3A | 服务端模型字段接入 | 部分完成 | 82% | 已接 `/api/qc/v2/models` 真实 catalog 响应结构，按 `data.categories[categoryId]` 读取模型并兼容 `groupName` 分组名；服务端 `defaultValue/options` 会初始化字段值，Tune 高级页可选择基础 options，文本/数值字段可输入，图片/视频/音频上传字段会按服务端字段映射已上传素材 URL 列表；图片 v2 提交会把字段值写入 `params`，视频请求结构已携带服务端模型 ID 和字段参数。 |
| Phase 4 | 视频 v2 链路 | 部分完成 | 45% | 当视频请求携带服务端 `bindingId/skuId` 时已切到 Web v2 的 `fee-preview -> prepare -> commit -> list`；Seedance2.0 多模态参数已按抓包结构生成，真实 App 端视频扣费生成尚未复测。 |
| Phase 5 | 历史/项目 | 部分完成 | 99% | `list/detail/cancel/project/list/project/tasks/project/pin/project/create/project/rename/project/delete/project/detail` DTO/API 已可解析，domain repository 已公开历史分页、按 `outputId` 获取详情、按 `taskId` 取消任务、项目列表分页、项目任务分页、项目置顶切换、项目创建/重命名/删除和项目详情；创作页中间区域已展示最近创作，可打开详情弹窗，已接入加载更多分页，会对非终态历史任务定时刷新，并可取消非终态任务；历史区顶部已展示项目筛选条和新建按钮，点击项目会加载项目内任务并可切回最近创作，项目 chip 可置顶、查看详情、重命名和删除；项目管理端点已通过 Chrome DevTools 使用登录态临时项目验证，仍缺 App 内端到端复测。 |

## 本轮已完成

| 类型 | 文件 | 内容 |
| --- | --- | --- |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationV2DtoTest.kt` | 覆盖 nested `commit` 请求、`prepare` 响应、任务列表输出结构。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt` | 锁定图片 G-2.0 默认 `bindingId`、`skuId`、`categoryId` 和参数映射。 |
| 测试 | `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt` | 增加创作/灵感模式切换测试、服务端模型加载默认选中测试、字段默认值初始化测试，以及图片/视频生成请求携带服务端模型 ID、字段 params 和上传列表 params 的回归测试。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt` | 覆盖服务端字段 params、列表 params 与 prompt/比例/分辨率/质量的合并和覆盖规则。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt` | 锁定视频 v2 默认请求结构：服务端 ID、`ratio/aspectRatio`、`resolution`、`duration`、`generateAudio`、`realPersonMode`、多模态 `creationMode/creationSubMode*` 和 `imageUrls`。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImplVideoV2Test.kt` | 覆盖视频请求在携带 quick-creation ID 时走 `fee-preview -> prepare -> commit -> list`，并从 `outputList` 产出视频结果。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImplHistoryTest.kt` | 覆盖 `/task/quick-creation/list` 历史分页映射、扣费字段、`apiRequestParams` 解析、输出尺寸解析、`/task/quick-creation/detail` 按 `outputId` 获取详情、`/task/quick-creation/cancel` 使用 URL 编码后的 `taskId` 请求体、`/task/quick-creation/project/list` 项目分页映射和请求体、`/task/quick-creation/project/tasks` 按真实 `projectId/page/size` 请求并解析 `records/current` 分页、`/task/quick-creation/project/pin` 按真实 `projectId/pinned` 提交置顶状态，以及 `project/create/rename/delete/detail` 的请求体和项目映射。 |
| 测试 | `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt` | 覆盖 ScreenModel 初始化加载 quick-creation 历史和项目列表、选择项目后加载项目内任务、清除项目筛选后回到最近创作、加载更多追加下一页、非终态历史任务定时刷新到终态、取消历史任务后刷新列表、选择 output 后按 `outputId` 加载详情并写入 UI state、切换项目置顶时调用 repository 并更新本地项目 state、创建/重命名/删除项目时更新项目列表并在删除当前筛选项目后回到最近创作，以及按 `projectId` 加载项目详情写入 UI state。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationModelDtoTest.kt` / `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationModelMapperTest.kt` / `QuickCreateRepositoryImplHistoryTest.kt` | 覆盖 `/api/qc/v2/models` 真实 `data.categoryMeta/categories` catalog 结构、分组、模型、字段、options/defaultValue 解析和 domain flatten 映射。 |
| 数据层 | `QuickCreationV2Dto.kt` | 新增 Web quick-creation v2 DTO。 |
| 数据层 | `QuickCreateApi.kt` | 新增 `categories/models/fee-preview/prepare/commit/list/detail/inspiration` 端点封装。 |
| 数据层 | `QuickCreateRepositoryImpl.kt` | 默认图片 G-2.0 改走 v2 提交和列表轮询，视频请求携带服务端 quick-creation ID 时也走 v2 提交和列表轮询；旧模型保留回退。 |
| 数据层 | `QuickCreationV2Defaults.kt` | 新增视频 v2 `createRequest` 构造，支持字段 params、列表 params、参考图/视频/音频 URL 和抓包确认的多模态参数。 |
| 数据层 | `QuickCreateRepository.kt` / `QuickCreateRepositoryImpl.kt` | 新增 `QuickCreationHistoryPage/Item/Output`、`QuickCreationProjectPage/Project` domain 模型和 `listQuickCreationHistory/getQuickCreationHistoryDetail/cancelQuickCreationTask/listQuickCreationProjects/listQuickCreationProjectTasks/createQuickCreationProject/renameQuickCreationProject/deleteQuickCreationProject/pinQuickCreationProject/getQuickCreationProjectDetail` 方法，后续 UI 可直接消费 quick-creation v2 历史数据、取消非终态任务、读取项目列表、按项目读取任务分页并管理项目。 |
| 网络 | `SharedModule.kt` | RunningHub 请求补 `user-language: zh_CN`，并在存在 Cookie 时自动携带。 |
| UI | `QuickCreateUiState.kt` / `QuickCreateScreenModel.kt` | 新增 `QuickCreateMode` 和 `switchMode`。 |
| UI | `QuickCreateScreen.kt` | 改为顶部模式切换、中间内容区、底部固定输入区；灵感页使用真实 tags/templates state 渲染。 |
| 灵感 | `QuickCreateRepository.kt` / `QuickCreateRepositoryImpl.kt` / `QuickCreateScreenModel.kt` | 接入真实 tags/templates/template detail 数据并替换灵感区占位内容；`template/detail` 请求体为 `{"templateId":"..."}`，响应中的 `snapshot.presetParams` 会映射到制作同款状态。 |
| 服务端模型 | `QuickCreationModelMapper.kt` / `QuickCreateRepositoryImpl.kt` / `QuickCreateScreenModel.kt` | 加载 IMAGE/VIDEO 服务端模型，默认选中首个可用模型，并在图片 v2 提交中使用选中模型的 `categoryId/bindingId/skuId`。 |
| 服务端字段 | `QuickCreateScreenModel.kt` / `QuickCreationV2Defaults.kt` / `QuickCreationModelMapper.kt` | 服务端字段默认值进入 UI state，用户更新后的字段值覆盖默认值，未知字段会被过滤；上传限制、multipleInputs 和 `skuInputExtraJson` 已保留到 domain model；图片/视频/音频上传字段会按字段类型把已上传素材 URL 写入对应服务端 `paramKey` 的数组参数。 |
| UI | `QuickCreateScreen.kt` / `TuneBottomSheet.kt` | 底部输入区展示当前服务端模型摘要，Tune 高级页展示并切换服务端图片模型，基础 options 字段可点选，文本/数值字段可输入，上传字段显示服务端限制摘要，保留本地模型作为兼容 fallback。 |
| UI | `QuickCreateScreen.kt` | 灵感模板卡片已接入“制作同款”，点击后加载模板详情并切回创作页。 |
| UI | `QuickCreateUiState.kt` / `QuickCreateScreenModel.kt` / `QuickCreateScreen.kt` | 新增历史加载状态、分页 state、历史列表 state、取消中 state、详情 state、项目列表 state、项目任务筛选 state、项目置顶中 state、项目变更中 state 和项目详情 state；创作页在无当前结果/任务时展示最近创作，支持图片/视频预览、状态、分类和扣费金额摘要；历史区顶部展示项目横向筛选条和新建按钮，点击项目会加载项目内任务，点击“最近创作”会清除筛选，点击项目 chip 图钉会调用置顶/取消置顶，更多菜单可打开详情、重命名/删除项目；历史列表可加载更多并去重追加；非终态历史任务会每 5 秒刷新当前已加载范围并显示取消入口；点击历史项按 `outputId` 加载详情弹窗；生成成功后刷新历史。 |

## 已验证

| 命令 | 结果 | 备注 |
| --- | --- | --- |
| `./gradlew.bat :shared:testDebugUnitTest` | 通过 | 存在既有 Kotlin warning，未新增失败。 |
| `./gradlew.bat :composeApp:testDebugUnitTest` | 通过 | 存在既有 Profile/MediaChip warning。 |
| `./gradlew.bat :composeApp:assembleDebug` | 通过 | Debug APK 打包成功；native strip 提示为既有库处理信息。 |
| `adb -s emulator-5554 install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk` + UI tree/screenshot | 通过 | App 可启动到“创作”页；已验证项目列表、最近创作、项目操作菜单、项目详情弹窗和真实服务端模型摘要。修复了长项目名挤压项目操作按钮的问题，详情弹窗时间已从毫秒值格式化为可读日期时间；底部模型摘要已显示 `全能图片G-2.0-文生图-官方版` 和 `4 个参数`。 |

## 未完成与风险

| 风险 | 影响 | 下一步 |
| --- | --- | --- |
| App 内尚未实测 v2 真实扣费生成 | 不能宣称图片 MVP 端到端完成 | 用已登录账号在真机/模拟器提交一次低成本图片任务，确认余额、任务结果和输出展示。 |
| 服务端字段尚未完全覆盖所有 fieldType | 基础 options、文本/数值字段和图片/视频/音频上传字段已进入请求结构，但条件字段和复杂 `skuInputExtraJson` 仍未完整动态化 | 下一步覆盖条件字段和复杂 `skuInputExtraJson`，并用更多服务端模型验证字段映射。 |
| 模板详情和制作同款仍需增强 | 已能按真实 `template/detail` 回填分类、服务端模型、prompt、基础参数和远端素材，但复杂模板字段和后续视频 v2 提交消费仍需继续验证 | 继续补图片模板、复杂多输入模板和视频 v2 提交流程的端到端测试。 |
| 历史能力仍不完整 | 已能在创作页展示最近创作、项目横向筛选、项目内任务、项目置顶切换、项目创建/重命名/删除/详情、加载更多历史、定时刷新非终态任务、取消非终态任务并打开详情弹窗；项目管理端点已用 Chrome DevTools 临时项目验证，App 内已验证项目列表、菜单和详情只读流程 | 下一步在用户确认可改动数据后，验证项目创建、筛选、置顶、重命名和删除完整流程。 |
| 视频 v2 仍缺少真实扣费复测 | 当前仅用抓包结构和 MockEngine 验证请求构造、路由和结果解析；不能宣称视频端到端完成 | 需要用户明确授权后，用低成本视频模型在 App 内提交一次真实任务；此前抓包的 Seedance2.0 模板预估价格为 9.60 元，不在既有 0.76 元授权范围内。 |
| `AuthRepositoryImpl.kt` 有既有未提交修改 | 本轮未审查，可能影响登录态 | 后续提交时不要误包含，除非确认是本任务需要。 |

## 2026-06-18 App 端补充验证

本轮使用 `adb -s emulator-5554` 重新拉起 `com.runninghub.app/.MainActivity`，从底部导航进入“创作”页，并用 UI tree 而不是截图坐标定位关键控件。验证结果：

- 创作页顶部结构符合当前截图理解：顶部“创作/灵感”切换，中间历史/项目内容区，底部固定模型参数与 prompt 输入区。
- 底部模型摘要已加载真实服务端模型：`全能图片G-2.0-文生图-官方版`，副标题为 `全能图片G-2.0-官方版 · 4 个参数`。
- 最近创作列表能显示真实成功任务：prompt 为“测试生成一张极简风格的绿色圆形图标，纯白背景，中心是 RunningHub 风格的绿色圆形符号”，状态为 `IMAGE · SUCCESS · PNG`，费用显示 `0.8 CNY`。
- 点击最近创作项后，详情弹窗能显示输出预览区域、prompt、`IMAGE · SUCCESS · PNG · 2048x1152` 和 `0.8 CNY`。
- 为避免超过用户已授权的 0.76 元扣费范围，本轮没有再次点击“生成”提交新的真实扣费任务；App 端新增验证范围限定为“读取并展示已成功扣费任务的历史和详情”。后续若要验证“从 App 再次发起 prepare/commit 并扣费”，需要用户重新确认一次扣费授权。

本轮证据文件保存在未跟踪目录 `output/`，包括：

- `output/quickcreate_app_create_before_generation.xml`
- `output/quickcreate_app_create_before_generation.png`
- `output/quickcreate_app_history_detail_success.xml`
- `output/quickcreate_app_history_detail_success.png`
