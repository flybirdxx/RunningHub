# RunningHub iOS App

## Setup

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. The project depends on `ComposeApp` framework from the `:composeApp` module
3. Build and run on an iOS simulator or device

## Requirements

- Xcode 15+
- iOS 16+
- CocoaPods (if needed for dependencies)

## Architecture

The iOS app is a thin wrapper around the Compose Multiplatform UI.
All business logic and UI components are shared via the `:composeApp` module.
