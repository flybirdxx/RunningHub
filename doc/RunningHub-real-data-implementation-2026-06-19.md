# RunningHub Real Data Implementation Record

Date: 2026-06-19
Scope: Real-data integration for the restored Creation Hub, Plaza, and History screens.

## API Mapping

| Screen | Capability | Implementation | Source |
|---|---|---|---|
| Creation Hub | API model catalog | `ModelCatalogRepository.listStandardModels()` -> `POST /api/sku/list` | `doc/RunningHub-call-api-models-capture-2026-06-18.md` |
| Creation Hub | API model detail / dynamic fields | `ModelCatalogRepository.getStandardModelDetail()` -> `POST /api/sku/detail`; `ApiModelFieldMapper` parses `inputConfigJson` | Same |
| Creation Hub | Standard model submit | `ModelInvocationRepository.submitStandardModel()` -> `POST /openapi/v2{rhEndpoint}`; body built by `ModelInvocationRequestBuilder` | Same |
| Creation Hub | Recent tasks | `GenerationHistoryRepository.listHistory(page=1,size=3)` via the existing quick-creation history facade | Existing `QuickCreateRepository` |
| Plaza | Tags | `PlazaRepository.getTags()` -> `POST /api/portal/tag/tree` | `doc/RunningHub-explore-capture-2026-06-19.md` |
| Plaza | Creation cards / sort / tag / paging | `PlazaRepository.listCreations()` -> `POST /api/portal/creation/list` | Same |
| Plaza | Short categories / short cards | `listShortCategories()` -> `/canvas/community/category/list`; `listShorts()` -> `/canvas/community/composition/list` | Same |
| History | Generation history | `GenerationHistoryRepository.listHistory()` normalizes quick-creation history | Existing `QuickCreateRepository` |
| History | Output detail | `GenerationHistoryRepository.getHistoryDetail(outputId)` | Existing `QuickCreateRepository` |
| History | Cancel running task | `GenerationHistoryRepository.cancelTask(taskId)` -> `QuickCreateRepository.cancelQuickCreationTask()` | Existing `QuickCreateRepository` |

## Implementation Summary

- Creation Hub now prefers real repository detail over fallback catalog data, so real API model details and dynamic input fields are not overwritten by static fallback entries.
- Plaza now supports `PlazaMode`, refresh, paging, sort, tag selection, real creation cards, short categories, and short card loading.
- History now renders normalized real history rows, status filters, output viewing entry point, reuse-parameter state, cancel entry point, and polling for running tasks.
- `GenerationHistoryRepository` now exposes a unified `cancelTask()` facade.
- ScreenModel tests cover real detail priority, Plaza refresh/paging/shorts, History filtering/detail/reuse/cancel, and related state transitions.
- Static Chinese UI labels in Plaza and History are stored as Kotlin Unicode escapes to avoid Windows command-line encoding corruption while still rendering as Chinese at runtime.

## Verification

Passed:

```powershell
.\gradlew :shared:testDebugUnitTest --tests *ApiModelFieldMapperTest --tests *ModelCatalogRepositoryMapperTest --tests *ModelInvocationRequestBuilderTest --tests *GenerationHistoryMapperTest --tests *PlazaDtoTest --tests *ApiModelDtoTest
.\gradlew :composeApp:testDebugUnitTest --tests *CreateScreenModelTest --tests *PlazaScreenModelTest --tests *TaskHistoryScreenModelTest
.\gradlew :shared:compileDebugKotlinAndroid
.\gradlew :composeApp:assembleDebug
.\gradlew :composeApp:installDebug
git diff --check
```

Runtime verification on `emulator-5554` after installing the debug APK:

| Screen | Evidence | Observed real data |
|---|---|---|
| Creation Hub | `output/realdata_v2_create.png`, `output/realdata_v2_create.xml` | Real API model card, endpoint, price, model search, prompt field, aspect ratio, resolution, fee preview |
| Plaza | `output/realdata_v2_plaza.png`, `output/realdata_v2_plaza.xml` | Real Plaza cards such as owner names, media type, like count, use count; labels render correctly |
| History | `output/realdata_v2_history.png`, `output/realdata_v2_history.xml` | Real task IDs, success status, source, task rows, view/reuse actions; labels render correctly |

The latest XML scan found no mojibake markers in the three captured screens.

## Remaining Risk / Not Covered

- No paid production generation submit was executed. The submit request builder and repository path are implemented, but production verification requires explicit user authorization because it may consume balance.
- No production task cancel was executed. The UI and repository entry point are wired, but canceling a real running task needs explicit user authorization.
- Plaza detail page, comments, follow state, and advanced interaction endpoints are documented but not connected to a detail UI in this slice.
- History currently uses the quick-creation history facade. A merged legacy WebApp history stream still needs a separate field conflict and ordering design.
- The repository has unrelated pre-existing dirty files and generated output assets; this implementation did not revert them.

## Follow-up Implementation: Validation and Action Feedback

Additional work on 2026-06-19:

- Creation Hub submit now runs schema-aware validation before calling `ModelInvocationRepository.submitStandardModel()`.
  - Required fields are still blocked.
  - String length, numeric/integer parsing, numeric min/max, list option membership, and upload/input count limits are blocked before submit.
  - Empty recent history now renders a real empty state instead of mock sample tasks.
- History output/reuse actions now have visible UI feedback.
  - `selectOutput()` sets the selected output and action message after detail loading.
  - `prepareReuseParams()` exposes reusable params and a visible count message.
  - The History screen renders an action panel with output type, size, expiry, URL, and the first reusable params.

Additional verification passed:

```powershell
.\gradlew :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.feature.create.CreateScreenModelTest --tests com.runninghub.app.ui.feature.plaza.PlazaScreenModelTest --tests com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest
.\gradlew :shared:compileDebugKotlinAndroid
.\gradlew :composeApp:assembleDebug
.\gradlew :composeApp:installDebug
git diff --check
```

Runtime verification on `emulator-5554` after this follow-up:

| Screen / Flow | Evidence | Observed result |
|---|---|---|
| Creation Hub validation | `output/realdata_v3_create_validation.png`, `output/realdata_v3_create_validation.xml` | Tapping Generate with an empty required prompt shows `Prompt is required`; no production submit was executed. |
| History view output | `output/realdata_v3_history_view_output.png`, `output/realdata_v3_history_view_output.xml` | Tapping View shows the loaded-output-detail panel, output type, image size, expiry, and real output URL. |
| History reuse params | `output/realdata_v3_history_reuse_params.png`, `output/realdata_v3_history_reuse_params.xml` | Tapping Reuse shows a reusable-parameter count and real prompt params. |

Notes:

- A broad wildcard command, `.\gradlew :composeApp:testDebugUnitTest --tests *CreateScreenModelTest --tests *PlazaScreenModelTest --tests *TaskHistoryScreenModelTest`, also matched the pre-existing `QuickCreateScreenModelTest` class and hit a Looper/MainDispatcher test-environment failure unrelated to this change. The exact target classes above passed.
- XML scans for the v3 runtime captures found no mojibake markers.

## Follow-up Implementation: Plaza Categories and History Retry Entry

Additional work on 2026-06-19:

- Plaza now exposes real short categories in the Shorts mode row.
  - Selecting a short category resets the short list, sets the selected category code, and reloads page 1 with that category.
  - Short pagination now guards against duplicate load-more requests while a short request is already in flight.
  - Plaza now renders explicit empty states for both creation cards and shorts instead of an empty grid.
- History failed-task Retry is now a parameter-preparation entry point instead of a plain list refresh.
  - Retry does not submit a paid production request.
  - It selects the failed task, exposes its saved params, and tells the user to confirm in Creation before generating again.

Additional verification passed:

```powershell
.\gradlew :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.feature.plaza.PlazaScreenModelTest --tests com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest
.\gradlew :composeApp:assembleDebug
.\gradlew :shared:compileDebugKotlinAndroid
.\gradlew :composeApp:installDebug
git diff --check
```

Runtime verification on `emulator-5554` after this follow-up:

| Screen / Flow | Evidence | Observed result |
|---|---|---|
| Plaza shorts mode | `output/realdata_v4_plaza_shorts.png`, `output/realdata_v4_plaza_shorts.xml` | Shorts mode renders real category chips such as `Hot collection`, `Narrative short`, `TV ad`, and short cards. |
| History list | `output/realdata_v4_history.png`, `output/realdata_v4_history.xml` | History screen renders real task rows, filter tabs, view/reuse actions, and no UI crash after retry-entry changes. |

Notes:

- `:shared:compileDebugKotlinAndroid` emitted an Android SDK XML version compatibility warning, but completed successfully.
- No paid production generation submit or production task cancel was executed in this follow-up.
- The live History account state did not expose a failed production task during runtime verification, so the retry button path is covered by unit tests and awaits a real failed task or approved test fixture for end-to-end runtime tapping.

## Follow-up Implementation: Create Real-First Catalog and Empty Search

Additional work on 2026-06-19:

- Creation Hub model catalog loading is now real-data first.
  - Local fallback catalog entries are no longer pre-seeded before the API response.
  - If the real API succeeds with an empty result set, the UI now shows a real empty state instead of fallback models.
  - Fallback catalog entries are shown only when the catalog request fails or times out, with a visible visitor/interface-unavailable notice.
  - Auth-like catalog failures are converted to user-facing login guidance instead of exposing raw token text.
- Creation Hub empty state now keeps the API model search field visible, so users can continue searching even when the current query has no real matches.

Additional verification passed:

```powershell
.\gradlew :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.feature.create.CreateScreenModelTest
.\gradlew :composeApp:assembleDebug
.\gradlew :shared:compileDebugKotlinAndroid
.\gradlew :composeApp:installDebug
```

Runtime verification on `emulator-5554` after this follow-up:

| Screen / Flow | Evidence | Observed result |
|---|---|---|
| Create real empty catalog | `output/realdata_v5_create_empty_search.png`, `output/realdata_v5_create_empty_search.xml` | Live catalog response produced an empty state; the UI kept the API model search field visible and did not show fallback/reference models. |
| Create empty search query | `output/realdata_v5_create_empty_search_query.png`, `output/realdata_v5_create_empty_search_query.xml` | Entering a search query kept the empty real-data state and still did not show fallback/reference models. |

Notes:

- Running `:composeApp:testDebugUnitTest` and `:composeApp:assembleDebug` in parallel caused a Kotlin daemon incremental-cache close warning; Gradle automatically retried without the daemon and both tasks finished with `BUILD SUCCESSFUL`. This is local build cache concurrency behavior, not a source error.
- The current live account/session returned no API model catalog rows during runtime verification, so successful non-empty Create catalog rendering still depends on a session/backend state with available standard models.


## Follow-up Implementation: Standard Model Catalog Response Compatibility

Date: 2026-06-19

### Cause

Runtime validation showed that the Create screen still displayed the catalog fallback after login. The web Nuxt bundle for `/call-api/search-api/:categoryType` confirmed the standard model catalog reads `data.page.records`, while the app DTO only accepted top-level `data.records` / `data.list` shapes.

A temporary sanitized diagnostic then showed a second DTO mismatch: `page.records[].price` can be an object instead of a string. The diagnostic was removed after the fix. No token, cookie, or raw credential value is logged.

### Changes

- `SkuListPageDto` now accepts `data.page.records` and `data.page.list` in addition to the previous top-level list shapes.
- SKU `id` now accepts either numeric or string JSON values.
- SKU `price` now accepts string, object, numeric, or null JSON values through a flexible string serializer. UI pricing continues to prefer normalized summary fields such as `priceSummary`.
- `CreateScreenModel.isAuthCatalogError()` was tightened so serialization offsets containing numbers like `401` are not misclassified as authentication failures.
- Temporary catalog diagnostics were removed after verification.

### Runtime Evidence

Captured after rebuilding and installing the final APK on emulator `emulator-5554`:

- `output/realdata_final_create.png` and `output/realdata_final_create.xml`
  - Create now shows real standard catalog data.
  - Visible model example: a real PRO text-to-image SKU was selected from the live catalog.
  - Visible endpoint example: `/rhart-image-n-pro/text-to-image`.
  - Visible price example: normalized CNY unit price rendered from the live SKU data.
  - Dynamic prompt and aspect-ratio fields render from the real model schema.
  - No visible fallback/auth/interface unavailable notice.
- `output/realdata_final_plaza.png` and `output/realdata_final_plaza.xml`
  - Plaza feed renders real recommendation data with sort tabs, mode chips, category chips, media cards, like/use actions, authors, and titles.
  - No empty, fallback, or interface unavailable state was visible.
- `output/realdata_final_history.png` and `output/realdata_final_history.xml`
  - History renders real task data for 2026-06-19.
  - Visible filters include all/running/success/failed states.
  - Visible records include real task IDs, model names, success status, cost, output count, and view/reuse actions.
  - No empty, fallback, or interface unavailable state was visible.

### Verification

Passed:

```powershell
.\gradlew :shared:testDebugUnitTest --tests com.runninghub.shared.data.remote.dto.ApiModelDtoTest --tests com.runninghub.shared.data.repository.ModelCatalogRepositoryMapperTest
.\gradlew :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.feature.create.CreateScreenModelTest
.\gradlew :shared:compileDebugKotlinAndroid
.\gradlew :composeApp:assembleDebug
.\gradlew :composeApp:installDebug
.\gradlew :shared:testDebugUnitTest --tests com.runninghub.shared.data.remote.dto.PlazaDtoTest --tests com.runninghub.shared.data.repository.GenerationHistoryMapperTest --tests com.runninghub.shared.data.repository.ModelInvocationRequestBuilderTest --tests com.runninghub.shared.data.repository.ApiModelFieldMapperTest --tests com.runninghub.shared.data.repository.ModelCatalogRepositoryMapperTest --tests com.runninghub.shared.data.remote.dto.ApiModelDtoTest
.\gradlew :composeApp:testDebugUnitTest --tests com.runninghub.app.ui.feature.create.CreateScreenModelTest --tests com.runninghub.app.ui.feature.plaza.PlazaScreenModelTest --tests com.runninghub.app.ui.feature.history.TaskHistoryScreenModelTest
.\gradlew :composeApp:assembleDebug
.\gradlew :composeApp:installDebug
git diff --check
```

Additional runtime log check used a narrow app-relevant pattern for catalog serialization/auth failures.

### Boundaries

- No paid production generation request was submitted.
- No production cancel/delete/mutation action was executed.
- These actions remain behind explicit user authorization by design.

