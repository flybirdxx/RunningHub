Packet ID: packet-plaza

Objective:
Rewrite Plaza to closely match the Pencil redline/reference image while preserving PlazaScreenModel data flow.

Files / sources:
- Own and edit only `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/plaza/PlazaScreen.kt`.
- Read `PlazaScreenModel.kt`, SmartAsyncImage, and theme files as needed, but do not edit them.
- Reference specs: Plaza redlines in `docs/superpowers/specs/assets/n4RET6.png`.

Do:
- Keep `PlazaVoyagerScreen` and `PlazaScreenContent` callable.
- Use black/dark surfaces, lime accent, compact chip rows, two-column dense media cards, metadata/actions, and bottom-safe spacing.
- Preserve sort/tag interactions.
- Cards should be stable even when mediaUrl is null by rendering a designed dark media placeholder.

Do not:
- Edit ScreenModel, shared repositories, navigation, or theme files.
- Add real external calls, credentials, or new dependencies.
- Revert other changes.

Expected output:
- Direct code changes in PlazaScreen.kt.
- Final note listing changed files and any compile concerns.

Verification:
- The worker may run a narrow Gradle command if cheap, but main agent will run final build.
