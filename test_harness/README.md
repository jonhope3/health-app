# FitTrack Testing Harness

This directory contains our custom testing suite for UI, integration, and AI evaluation via ADB.

## Files

- `SearchTestRunner.kt`: A Jetpack Compose test UI that can be temporarily swapped into `MainActivity.kt` to programmatically evaluate the AI Search pipeline.
- `run_tests.sh`: A bash script to run automated UI workflows for app onboarding using `adb shell input tap`.
- `run_log_integration_tests.sh`: A bash script validating the Food Logging, History Browsing, and Diary Editing flow.

## How to use the Integration Tests

The application now supports an ADB intent flag `POPULATE_TEST_DATA` inside `MainActivity.kt`. This allows us to inject synthetic food log items reliably without breaking the production Compose layout inputs.

To execute the integration tests:

1. Ensure your device/emulator is connected via ADB.
2. Ensure you have built and installed the app (`./gradlew installDebug`).
3. Run the onboarding test: `./run_tests.sh`
4. Run the logging test: `./run_log_integration_tests.sh`

## Future Work

We will expand this directory with python/bash scripts that interact with the app locally over ADB, validating that all UI elements function end-to-end without needing the heavy JUnit/Espresso framework.
