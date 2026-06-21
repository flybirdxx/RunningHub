# iOS macOS link and Simulator smoke evidence

capturedAt: 2026-06-21T15:09:25Z
headSha: d7510d8e398134dab92ce5a3ac38d42ff9762df2
overallResult: skipped
host: Windows local environment; macOS unavailable
linkCommand: ./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64
linkResult: skipped
xcodebuildCommand: xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination generic/platform=iOS Simulator build CODE_SIGNING_ALLOWED=NO
xcodebuildResult: skipped
simulatorSmokeResult: skipped
skipReason: macOS test environment is currently unavailable, so iOS framework link, Xcode build, and Simulator login/logout/QuickCreate smoke cannot be executed in this workspace.
followUpRequired: Re-run docs/migration/collect-ios-macos-evidence.sh on macOS or trigger docs/migration/request-ios-macos-evidence.ps1 after real Simulator smoke is manually verified.

## Simulator smoke scope

- Simulator login flow observed: skipped
- Simulator logout/session flow observed: skipped
- Simulator QuickCreate flow observed: skipped

## Notes

User explicitly allowed retaining a skip record for the unavailable macOS environment. This file is not a passing iOS runtime proof; it records the current limitation and preserves the follow-up required before claiming macOS/iOS runtime coverage.

## Gradle output tail

```text
Skipped because macOS is unavailable in the current Windows workspace.
```

## Xcode build output tail

```text
Skipped because xcodebuild and iOS Simulator are unavailable in the current Windows workspace.
```
