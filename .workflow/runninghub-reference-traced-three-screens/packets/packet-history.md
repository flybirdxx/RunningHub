Packet ID: packet-history

Objective:
Rewrite History to closely match the Pencil redline/reference image while preserving TaskHistoryScreenModel data flow and previews.

Files / sources:
- Own and edit only `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryScreen.kt`.
- Read `TaskHistoryScreenModel.kt`, preview data, and theme files as needed, but do not edit them.
- Reference specs: History redlines in `docs/superpowers/specs/assets/n4RET6.png`.

Do:
- Keep `TaskHistoryVoyagerScreen`, `TaskHistoryContent`, and preview composables callable.
- Use title/filter cluster, summary metric cards, dense task rows, status pills, failed row state, retry/reuse/detail actions.
- Use black/dark surfaces, lime accent, danger red, 8-10dp radii, compact typography.
- Preserve filters and list behavior.

Do not:
- Edit ScreenModel, shared repositories, navigation, theme, or preview data files.
- Add real external calls, credentials, or new dependencies.
- Revert other changes.

Expected output:
- Direct code changes in TaskHistoryScreen.kt.
- Final note listing changed files and any compile concerns.

Verification:
- The worker may run a narrow Gradle command if cheap, but main agent will run final build.
