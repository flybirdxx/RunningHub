# Gradle Wrapper Instructions

## Scope

This directory owns the Gradle wrapper and version catalog.

## Rules

- `libs.versions.toml` is the central dependency and plugin version catalog.
- Do not edit `gradle-wrapper.jar` manually.
- Wrapper or major tooling upgrades require Android and iOS verification planning; they are not routine docs changes.
- Keep Gradle, AGP, Kotlin, Compose Multiplatform and KSP versions compatible with `build-logic`.
- Do not add repository declarations here; repository policy belongs in `settings.gradle.kts`.
- Do not commit downloaded caches or generated Gradle output.

## Verification

- Version catalog changes should run at least `.\gradlew.bat --console=plain help`.
- Build-tool upgrades should run `.\gradlew.bat --console=plain checkArchitectureBoundaries checkLongTermGovernance` and the relevant L1 verifier.

