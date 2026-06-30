# RunningHub scoped code review - governance/history/design-system

## Scope

- Branch: `feature/kmp-refactoring`
- Review scope: governance unblock, History bridge/bus move, and previously added RM-01/RM-02 design-system files.
- Explicitly out of scope for code findings: unrelated `docs/migration/current-state.yaml` content changes and untracked AGENTS/product artifact files, except where they affect verifier truth.

## Skill Perspective Check

- `code_review` skill loaded: applied RunningHub architecture, KMP boundary, security, tests, and governance-risk checklist.
- `remove-ai-slops` skill loaded: reviewed changed production/test code for deletion-only or tautological tests, implementation-mirroring tests, needless abstraction, dead code, oversized modules, and unneeded production complexity.
- `programming` skill loaded: no Kotlin-specific reference exists in that skill, so I applied its shared criteria: strict boundaries, no untyped escape hatches, no needless validation/abstraction, meaningful tests, and 250 pure-LOC pressure check.
- Violations from these perspectives: one MEDIUM design-token bypass and one LOW component API duplication/test gap. No oversized scoped production file exceeded 250 pure LOC (`UnifiedGenerationHistoryRepository.kt` measured about 221 pure LOC).

## Findings

### CRITICAL

None.

### HIGH

1. `verifyL1Android` is currently not reproducibly green for this worktree.
   - File: `docs/migration/current-state.yaml` (file state, no stable line reference)
   - Evidence: `./gradlew.bat --console=plain verifyL1Android` failed with exit code 1 in `:checkMigrationScripts`.
   - Key error: `docs/migration/current-state.yaml has unstaged changes; stage it before using it as L1 evidence.`
   - Impact: the main-thread claim that `verifyL1Android` passed is not approvable against the current worktree. This file is outside the requested code-review scope, but it blocks the requested aggregate verifier and must be resolved or the PASS claim removed before approval.

### MEDIUM

1. `RhTaskStatusBadge` bypasses the design-system theme token for the canceled state.
   - File: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/badges/RhTaskStatusBadge.kt:78`
   - Evidence: line 69 maps canceled status to `colors.textTertiary`, but line 78 then replaces that with `Color(0xFF8B929D)`. That value is the dark palette tertiary text, not the light palette tertiary text (`RhLightColors.textTertiary = Color(0xFF64748B)`).
   - Impact: light-theme canceled badges ignore `LocalRhColors`, weakening RM-01/RM-02 token guarantees and risking low-contrast or inconsistent text. The current design-system test checks token constants and `tokenName`, but does not exercise component color mapping under dark/light `ProvideRhTheme`.

### LOW

1. `RhPriceBadgeState.Amount` stores a label while `RhPriceBadge` also requires a separate `label` parameter.
   - File: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/badges/RhPriceBadge.kt:43`
   - Related call surface: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/designsystem/components/badges/RhPriceBadge.kt:61`
   - Impact: callers can construct `Amount("37 RHB")` and render a different `label`, creating avoidable API divergence. The current test only asserts `Amount("37 RHB").tokenName`, so it would not catch this inconsistency. This is not a blocker because the component is not yet used by production pages, but it is a small design-system API cleanup candidate.

## Clean Areas Reviewed

- History bridge remains isolated in `UnifiedGenerationHistoryRepository.kt`; production `composeApp` references to QuickCreate history models are limited to that bridge file.
- `TaskHistoryInvalidationBus` moved to `feature:task:presentation` without Data/Ktor/platform imports and is wired through `TaskHistoryInvalidationEvents` / `TaskHistoryInvalidationNotifier`.
- `AppModule` still binds `GenerationHistoryRepository` in the composition root and does not introduce Data imports into `commonMain`.
- `checkLongTermGovernance` and `checkArchitectureBoundaries` passed after the scoped governance changes.

## Verification Run

- PASS: `./gradlew.bat --console=plain :feature:task:presentation:testDebugUnitTest --tests "com.runninghub.feature.task.presentation.TaskHistoryInvalidationBusTest" :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.feature.history.QuickCreateGenerationHistoryRepositoryAdapterTest" :composeApp:compileDebugKotlinAndroid checkLongTermGovernance checkArchitectureBoundaries`
- PASS: `./gradlew.bat --console=plain :composeApp:testDebugUnitTest --tests "com.runninghub.app.ui.designsystem.RhDesignSystemContractTest"`
- FAIL: `./gradlew.bat --console=plain verifyL1Android`
  - Exit code: 1
  - Failing task: `:checkMigrationScripts`
  - Cause: unstaged `docs/migration/current-state.yaml`
- PASS: scoped `git diff --check` for reviewed tracked files.

## Recommendation

- codeQualityStatus: `BLOCK`
- recommendation: `REQUEST_CHANGES`
- Blockers:
  - Resolve the current `verifyL1Android` failure caused by unstaged `docs/migration/current-state.yaml`, or stop claiming that verifier passed for the current worktree.

