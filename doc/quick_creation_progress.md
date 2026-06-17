# 快捷创作重构开发进度表

更新时间：2026-06-18
分支：`feature/kmp-refactoring`
依据：Web 抓包结果、移动端截图结构、`doc/quick_creation_feature_plan.md`

## 总体状态

| 阶段 | 范围 | 状态 | 完成度 | 说明 |
| --- | --- | --- | --- | --- |
| Phase 0 | v2 契约落地 | 已完成 | 100% | 已补 `prepare/commit/list/detail` 等 DTO，并用抓包样例做序列化/反序列化测试。 |
| Phase 1 | 图片 G-2.0 MVP | 部分完成 | 70% | 默认 `all-power-image-g2` 已切到 Web v2 的 `fee-preview -> prepare -> commit -> list`；图片提交会优先携带当前服务端模型的 `categoryId/bindingId/skuId`；真实端到端扣费生成尚未在 App 内复测。 |
| Phase 2 | 页面结构重构 | 部分完成 | 80% | 页面已调整为顶部“创作/灵感”、中间滚动内容、底部固定输入面板；底部输入区已显示服务端模型摘要，Tune 高级页可切换服务端图片模型。 |
| Phase 3 | 灵感接口接入 | 部分完成 | 82% | 已接 tags/templates 列表和真实 template/detail；UI 使用真实 state 渲染，点击模板可“制作同款”并回填分类、服务端模型、prompt、基础参数和远端素材。 |
| Phase 3A | 服务端模型字段接入 | 部分完成 | 78% | 已接 `/api/qc/v2/models` DTO、mapper、repository 和 ScreenModel 状态；服务端 `defaultValue/options` 会初始化字段值，Tune 高级页可选择基础 options，文本/数值字段可输入，图片/视频/音频上传字段会按服务端字段映射已上传素材 URL 列表；图片 v2 提交会把字段值写入 `params`，视频请求结构已携带服务端模型 ID 和字段参数。 |
| Phase 4 | 视频 v2 链路 | 部分完成 | 45% | 当视频请求携带服务端 `bindingId/skuId` 时已切到 Web v2 的 `fee-preview -> prepare -> commit -> list`；Seedance2.0 多模态参数已按抓包结构生成，真实 App 端视频扣费生成尚未复测。 |
| Phase 5 | 历史/项目 | 未完成 | 10% | `list/detail` DTO 已可解析，尚未做历史页面和项目管理交互。 |

## 本轮已完成

| 类型 | 文件 | 内容 |
| --- | --- | --- |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationV2DtoTest.kt` | 覆盖 nested `commit` 请求、`prepare` 响应、任务列表输出结构。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt` | 锁定图片 G-2.0 默认 `bindingId`、`skuId`、`categoryId` 和参数映射。 |
| 测试 | `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/quickcreate/QuickCreateScreenModelTest.kt` | 增加创作/灵感模式切换测试、服务端模型加载默认选中测试、字段默认值初始化测试，以及图片/视频生成请求携带服务端模型 ID、字段 params 和上传列表 params 的回归测试。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt` | 覆盖服务端字段 params、列表 params 与 prompt/比例/分辨率/质量的合并和覆盖规则。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationV2DefaultsTest.kt` | 锁定视频 v2 默认请求结构：服务端 ID、`ratio/aspectRatio`、`resolution`、`duration`、`generateAudio`、`realPersonMode`、多模态 `creationMode/creationSubMode*` 和 `imageUrls`。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreateRepositoryImplVideoV2Test.kt` | 覆盖视频请求在携带 quick-creation ID 时走 `fee-preview -> prepare -> commit -> list`，并从 `outputList` 产出视频结果。 |
| 测试 | `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/QuickCreationModelDtoTest.kt` / `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/QuickCreationModelMapperTest.kt` | 覆盖 `/api/qc/v2/models` 分组、模型、字段、options/defaultValue 解析和 domain flatten 映射。 |
| 数据层 | `QuickCreationV2Dto.kt` | 新增 Web quick-creation v2 DTO。 |
| 数据层 | `QuickCreateApi.kt` | 新增 `categories/models/fee-preview/prepare/commit/list/detail/inspiration` 端点封装。 |
| 数据层 | `QuickCreateRepositoryImpl.kt` | 默认图片 G-2.0 改走 v2 提交和列表轮询，视频请求携带服务端 quick-creation ID 时也走 v2 提交和列表轮询；旧模型保留回退。 |
| 数据层 | `QuickCreationV2Defaults.kt` | 新增视频 v2 `createRequest` 构造，支持字段 params、列表 params、参考图/视频/音频 URL 和抓包确认的多模态参数。 |
| 网络 | `SharedModule.kt` | RunningHub 请求补 `user-language: zh_CN`，并在存在 Cookie 时自动携带。 |
| UI | `QuickCreateUiState.kt` / `QuickCreateScreenModel.kt` | 新增 `QuickCreateMode` 和 `switchMode`。 |
| UI | `QuickCreateScreen.kt` | 改为顶部模式切换、中间内容区、底部固定输入区；灵感页使用真实 tags/templates state 渲染。 |
| 灵感 | `QuickCreateRepository.kt` / `QuickCreateRepositoryImpl.kt` / `QuickCreateScreenModel.kt` | 接入真实 tags/templates/template detail 数据并替换灵感区占位内容；`template/detail` 请求体为 `{"templateId":"..."}`，响应中的 `snapshot.presetParams` 会映射到制作同款状态。 |
| 服务端模型 | `QuickCreationModelMapper.kt` / `QuickCreateRepositoryImpl.kt` / `QuickCreateScreenModel.kt` | 加载 IMAGE/VIDEO 服务端模型，默认选中首个可用模型，并在图片 v2 提交中使用选中模型的 `categoryId/bindingId/skuId`。 |
| 服务端字段 | `QuickCreateScreenModel.kt` / `QuickCreationV2Defaults.kt` / `QuickCreationModelMapper.kt` | 服务端字段默认值进入 UI state，用户更新后的字段值覆盖默认值，未知字段会被过滤；上传限制、multipleInputs 和 `skuInputExtraJson` 已保留到 domain model；图片/视频/音频上传字段会按字段类型把已上传素材 URL 写入对应服务端 `paramKey` 的数组参数。 |
| UI | `QuickCreateScreen.kt` / `TuneBottomSheet.kt` | 底部输入区展示当前服务端模型摘要，Tune 高级页展示并切换服务端图片模型，基础 options 字段可点选，文本/数值字段可输入，上传字段显示服务端限制摘要，保留本地模型作为兼容 fallback。 |
| UI | `QuickCreateScreen.kt` | 灵感模板卡片已接入“制作同款”，点击后加载模板详情并切回创作页。 |

## 已验证

| 命令 | 结果 | 备注 |
| --- | --- | --- |
| `./gradlew.bat :shared:testDebugUnitTest` | 通过 | 存在既有 Kotlin warning，未新增失败。 |
| `./gradlew.bat :composeApp:testDebugUnitTest` | 通过 | 存在既有 Profile/MediaChip warning。 |
| `./gradlew.bat :composeApp:assembleDebug` | 通过 | Debug APK 打包成功；native strip 提示为既有库处理信息。 |

## 未完成与风险

| 风险 | 影响 | 下一步 |
| --- | --- | --- |
| App 内尚未实测 v2 真实扣费生成 | 不能宣称图片 MVP 端到端完成 | 用已登录账号在真机/模拟器提交一次低成本图片任务，确认余额、任务结果和输出展示。 |
| 服务端字段尚未完全覆盖所有 fieldType | 基础 options、文本/数值字段和图片/视频/音频上传字段已进入请求结构，但条件字段和复杂 `skuInputExtraJson` 仍未完整动态化 | 下一步覆盖条件字段和复杂 `skuInputExtraJson`，并用更多服务端模型验证字段映射。 |
| 模板详情和制作同款仍需增强 | 已能按真实 `template/detail` 回填分类、服务端模型、prompt、基础参数和远端素材，但复杂模板字段和后续视频 v2 提交消费仍需继续验证 | 继续补图片模板、复杂多输入模板和视频 v2 提交流程的端到端测试。 |
| 视频 v2 仍缺少真实扣费复测 | 当前仅用抓包结构和 MockEngine 验证请求构造、路由和结果解析；不能宣称视频端到端完成 | 需要用户明确授权后，用低成本视频模型在 App 内提交一次真实任务；此前抓包的 Seedance2.0 模板预估价格为 9.60 元，不在既有 0.76 元授权范围内。 |
| `AuthRepositoryImpl.kt` 有既有未提交修改 | 本轮未审查，可能影响登录态 | 后续提交时不要误包含，除非确认是本任务需要。 |
