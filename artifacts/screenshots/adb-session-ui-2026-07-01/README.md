# ADB Session UI Screenshots - 2026-07-01

This directory contains Android emulator screenshots and UI XML dumps captured for the RunningHub redesign session UI review.

- Device serial: `emulator-5554`
- Package: `com.runninghub.app.debug`
- Activity: `com.runninghub.app.MainActivity`
- APK source: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
- Capture method: `adb shell screencap -p`, `adb pull`, and `adb shell uiautomator dump`

## Coverage

- `00-create-main.png`: Create main screen and bottom navigation
- `01-create-model-picker-sheet.png`: model picker sheet
- `02-create-params-sheet.png`: parameter sheet
- `03-create-prompt-filled.png`: prompt-filled create state
- `04-create-generate-action-after-tap.png`: generation confirmation sheet
- `05-discovery-app-cards.png`: Discovery app cards
- `06-discovery-app-detail.png`: app detail screen
- `07-plaza-cards.png`: Plaza inspiration cards
- `08-plaza-use-same-or-reuse-sheet.png`: reuse confirmation sheet
- `09-task-history.png`: task history list
- `10-task-history-detail.png`: task detail and result preview
- `11-profile-asset-center.png`: profile asset center

## Notes

- Each `.png` has a same-name `.xml` UI tree dump.
- All retained XML dumps were checked for `package="com.runninghub.app.debug"`.
- The profile screenshot includes masked account display and runtime balance values from the emulator state; review before publishing externally.
