#!/bin/bash

echo "Starting HopeHealth Automated ADB UI Tests..."

# Build and install
echo "Building APK..."
./gradlew assembleDebug
echo "Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch App
echo "Launching App..."
adb shell pm clear com.hopehealth.app
adb shell am start -n com.hopehealth.app/.MainActivity
sleep 4

echo "Onboarding Step 1: Entering Name..."
adb shell input tap 540 1296
sleep 1
adb shell input text "Antigravity"
sleep 1
adb shell input tap 796 1081
sleep 2

echo "Onboarding Step 2: Calorie Goal..."
adb shell input tap 796 1527
sleep 2

echo "Onboarding Step 3: Step Goal..."
adb shell input tap 792 1507
sleep 3

echo "Navigating Main Screen..."
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml ./test_harness/window_dump.xml

if grep -q "HopeHealth" ./test_harness/window_dump.xml; then
    echo "SUCCESS: Onboarding complete and Home UI rendered correctly!"
else
    echo "FAILURE: App did not reach Home screen."
    exit 1
fi

echo "Testing Log Food Tab..."
adb shell input tap 403 2210
sleep 2
adb shell input swipe 500 2000 500 500 500
sleep 1

echo "Testing Steps Tab..."
adb shell input tap 678 2210
sleep 2

echo "Test suite complete."
