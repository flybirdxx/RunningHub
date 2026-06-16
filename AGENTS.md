# Repository Guidelines

## Project Structure & Module Organization

RunningHub is a Kotlin Multiplatform mobile client. `settings.gradle.kts` includes `:shared`, `:composeApp`, and `:app`.

- `shared/` contains common business logic, Ktor API clients, SQLDelight database definitions, repositories, use cases, Koin modules, and platform `expect/actual` code under `src/commonMain`, `src/androidMain`, and `src/iosMain`.
- `composeApp/` is the current cross-platform Compose UI. Keep screens in `ui/feature`, shared widgets in `ui/component`, design tokens in `ui/theme`, and navigation in `ui/navigation`.
- `app/` is the legacy Android-only module using Hilt, Retrofit, and Room. Prefer new shared work in `shared` and new UI work in `composeApp` unless maintaining legacy behavior.
- `doc/` stores API and product integration notes. `gradle/libs.versions.toml` is the dependency version catalog.

## Build, Test, and Development Commands

This checkout does not include `gradlew`; use a local Gradle installation, or replace `gradle` with `./gradlew` if the wrapper scripts are restored.

- `gradle projects` lists included modules.
- `gradle :composeApp:assembleDebug` builds the main Android debug app.
- `gradle :app:assembleDebug` builds the legacy Android app.
- `gradle :shared:compileKotlinAndroid` checks shared Android Kotlin compilation.
- `gradle test` runs available unit tests across modules.
- `gradle connectedAndroidTest` runs instrumented tests on a connected device or emulator.

## Coding Style & Naming Conventions

Use Kotlin official style (`kotlin.code.style=official`), 4-space indentation, and JVM target 17. Keep package names under `com.runninghub`. Name Compose screens `*Screen`, state holders `*UiState`, ViewModels or Voyager models `*ViewModel` / `*ScreenModel`, repositories `*Repository`, and platform implementations `Name.android.kt` or `Name.ios.kt`. Prefer Koin in KMP modules; avoid introducing new Hilt, Retrofit, or Room dependencies outside legacy `app/`.

## Testing Guidelines

Put shared tests in `shared/src/commonTest` when logic is platform-neutral. Use Android unit tests in `src/test` and instrumented tests in `src/androidTest`. Name test files `SubjectTest.kt` and test behavior at repository/use-case boundaries before UI details. For permission, media picker, persistence, and API changes, add regression coverage or document the manual device checks performed.

## Commit & Pull Request Guidelines

Recent history uses Conventional Commits, for example `fix(picker): ...`, `fix(kmp): ...`, and `feat(permission): ...`. Keep commits scoped and imperative. PRs should include a short behavior summary, affected modules, linked issue or task, screenshots for UI changes, and exact verification commands. Do not commit `local.properties`, build outputs, or machine-specific IDE files.

## Security & Configuration Tips

Treat `local.properties` as local-only configuration. Keep API keys and signing material out of Git. When changing `gradle/libs.versions.toml`, verify all affected modules compile.
