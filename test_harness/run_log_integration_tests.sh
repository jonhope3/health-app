#!/bin/bash
# Integration test for the Food Logging, History, and Edit flow using ADB.
# This script requires the auto-populate block in LogViewModel to be active for initial state,
# or it can be run after manually logging 3 foods.

set -e

echo "Starting Log & History Integration Test..."

# Wait for app to be ready
echo "Restarting app with POPULATE_TEST_DATA intent..."
adb shell am force-stop com.hopehealth.app
adb shell am start -n com.hopehealth.app/.MainActivity --ez POPULATE_TEST_DATA true
sleep 3

# Tap Log Food tab
echo "Navigating to Log Food..."
adb shell input tap 403 2210
sleep 2

# Tap Browse History
echo "Opening History..."
adb shell input tap 882 1374
sleep 2

# Tap "Apple (Search)" item in history (adjust Y coord if list order varies)
echo "Selecting Apple from History..."
adb shell input tap 540 610
sleep 2

# Tap "Add" button on the bottom modal sheet
echo "Adding item to diary..."
adb shell input tap 540 2090
sleep 2

# Tap Options icon for the Apple log item
echo "Opening Options menu for log item..."
adb shell input tap 960 2104
sleep 1

# Tap Edit Entry in the popup menu
echo "Tapping Edit Entry..."
adb shell input tap 897 1807
sleep 2

# Tap Save in the Edit Modal
echo "Saving Edit..."
adb shell input tap 795 1904
sleep 2

echo "Integration Test completed successfully."
