Packet ID: packet-create

Objective:
Rewrite Creation Hub to closely match the Pencil redline/reference image while preserving CreateScreenModel data flow.

Files / sources:
- Own and edit only `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreen.kt`.
- Read `CreateScreenModel.kt` and theme files as needed, but do not edit them.
- Reference specs: Creation Hub redlines in `docs/superpowers/specs/assets/n4RET6.png`.

Do:
- Keep `CreateVoyagerScreen` and `CreateScreenContent` callable.
- Use black/dark surfaces, lime accent, 8-10dp radii, 18dp horizontal inset equivalent, dense spacing.
- Implement reference sections: title/action row, search field, recent strip, model list/cards, detail panel, submit/dynamic fields.
- Make compact phone layout resemble the reference: model list and detail/form panel occupy side-by-side regions where feasible, with scroll only where needed.

Do not:
- Edit ScreenModel, shared repositories, navigation, or theme files.
- Add real external calls, credentials, or new dependencies.
- Revert other changes.

Expected output:
- Direct code changes in CreateScreen.kt.
- Final note listing changed files and any compile concerns.

Verification:
- The worker may run a narrow Gradle command if cheap, but main agent will run final build.
