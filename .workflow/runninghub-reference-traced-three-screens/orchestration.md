Goal:
Rewrite the RunningHub Compose Creation Hub, Plaza, and History screens so their first-screen structure, density, black/lime palette, spacing, and component hierarchy follow the Pencil redline and reference images.

Success criteria:
- Creation Hub uses the reference layout: title/action row, 60dp search field, compact recent strip, left model list, right detail/form panel on compact phone.
- Plaza uses the reference layout: title/action row, sort chips, tag chips, dense two-column feed cards with media placeholders/loaded images and compact metadata/actions.
- History uses the reference layout: title/filter cluster, metric summary cards, dense task rows, failed/running/completed states, and reuse/action controls.
- The screens preserve existing ScreenModel data flow and do not require real external API calls to render fallback content.
- Shared tokens are black/lime/low-radius and line up with the Pencil redline values.

Current context:
- Reference Pencil file: docs/superpowers/specs/runninghub-reference-traced-design-sketch.pen
- Exported redline: docs/superpowers/specs/assets/n4RET6.png
- Reference images: docs/superpowers/specs/assets/wireframe-01-creation-hub.png, wireframe-02-plaza-first.png, wireframe-03-history-console.png
- Existing first-pass screens already exist under composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create, plaza, and history.

Constraints:
- Do not touch legacy app/ unless directly required.
- Do not introduce new backend, account, payment, production data, or true external API writes.
- Do not revert unrelated dirty worktree changes.
- Keep Kotlin official style and Compose Multiplatform patterns.

Risks:
- Multiple workers could conflict if they edit shared theme/navigation files. Shared token edits stay with the main agent.
- The current sandbox may block visual screenshot tooling; Gradle compilation is the primary automated gate.

Approval required:
- None for local non-destructive code edits in the assigned files.
- Ask before destructive git/file operations, dependency downloads, deployment, credentials, or production data.

Workflow artifact path:
.workflow/runninghub-reference-traced-three-screens

Work packets:
- packet-create: Rewrite CreateScreen.kt only.
- packet-plaza: Rewrite PlazaScreen.kt only.
- packet-history: Rewrite TaskHistoryScreen.kt only.
- main: Shared tokens, navigation integration review, Gradle verification, final synthesis.

Integration policy:
- Accept direct worker edits only when they stay inside assigned files and preserve public composable entry points used by navigation/tests.
- Resolve compile errors in the narrowest file possible.
- If shared helpers are needed by multiple screens, main agent creates them after reviewing all worker outputs.

Verification:
- Run `gradle :shared:compileKotlinAndroid`.
- Run `gradle :composeApp:assembleDebug`.
- If simulator/screenshot is available, capture three tabs and compare to the reference redline.
- Otherwise document screenshot limitation and perform code-level redline checklist.

Reusable artifacts:
- Keep this workflow as the recipe for future reference-image-to-Compose screen rewrites.
