# RunningHub iOS App

## Setup

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select the `RunningHub` scheme and an iOS 16+ simulator
3. Build and run. The Xcode target executes `:composeApp:embedAndSignAppleFrameworkForXcode`
   before compiling Swift, so the `ComposeApp` framework is built from the current checkout.

## Requirements

- Xcode 16+ recommended
- iOS 16+
- JDK 17 available to the Xcode build script
- Gradle Wrapper executable on macOS

## Architecture

The iOS app is a thin wrapper around the Compose Multiplatform UI.
All business logic and UI components are shared via the `:composeApp` module.

Runtime setup is split by layer:

- SwiftUI owns only the app lifecycle and embeds `MainViewControllerKt.MainViewController()`.
- `composeApp/src/iosMain` starts Koin with the iOS runtime module and Feature Data modules.
- `composeApp/commonMain` owns the root UI, session restoration and navigation.

Before L1 can be sealed, run the app on macOS Simulator and collect
`docs/migration/evidence/ios-macos-link-and-simulator.md` with
`docs/migration/collect-ios-macos-evidence.sh`.
