# FitTrack Testing Harness

Custom testing suite for UI, integration, and AI evaluation
via ADB.

## Files

- `SearchTestRunner.kt` — Compose test UI that can be swapped
  into `MainActivity.kt` to evaluate the AI Search pipeline.
- `run_tests.sh` — automated UI workflows for app onboarding
  using `adb shell input tap`.
- `run_log_integration_tests.sh` — validates Food Logging,
  History Browsing, and Diary Editing flows.

## How to Use the Integration Tests

The app supports an ADB intent flag `POPULATE_TEST_DATA` in
`MainActivity.kt`. This injects synthetic food log items
without breaking the production Compose layout.

To execute:

1. Ensure your device/emulator is connected via ADB.
2. Build and install the app (`./gradlew installDebug`).
3. Run the onboarding test: `./run_tests.sh`
4. Run the logging test: `./run_log_integration_tests.sh`

## Future Work

Expand with Python/bash scripts that interact with the app
over ADB, validating all UI elements end-to-end without
JUnit/Espresso.
