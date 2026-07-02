# iOS macOS link and Simulator smoke evidence

capturedAt: 2026-07-02T02:29:45Z
headSha: 11652fa627c9fa1d715190db75383b7018cd7bd7
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
