# HopeHealth Android

Privacy-first health tracking app for Google Pixel 10 Pro.
Combines food logging, step tracking, fertility/cycle tracking,
and on-device AI — all data stays on the device.

## Quick Start

```bash
# Verify environment
make setup

# Build debug APK
make build

# Install + launch on connected device
make run
```

## Build Setup

### Prerequisites

- Android Studio (latest stable)
- JDK 17+
- ADB installed and in PATH

### SDK Configuration

Create a `local.properties` file in the project root pointing
to your Android SDK:

```properties
# macOS default (Homebrew)
sdk.dir=/Users/<you>/Library/Android/sdk

# Shared/CI path
sdk.dir=/usr/local/share/android-commandlinetools
```

### Install SDK Components

```bash
yes | sdkmanager \
  --sdk_root=$HOME/Library/Android/sdk \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "emulator" \
  "system-images;android-35;google_apis;x86_64"
```

## Make Targets

```text
Dev Lifecycle
  make start       Cold start: emulator + build + launch
  make run         Fast update: rebuild + launch on device
  make log         View app logs (Logcat)

Physical Device & Release
  make phone       Install + launch debug on USB device
  make deploy      Build, sign, deploy production APK

Testing
  make test            Unit tests (local)
  make ui-test         UI navigation tests (emulator)
  make exhaustive-test Full interaction tests (emulator)

Emulator Control
  make emu-start   Start emulator
  make emu-stop    Stop emulator
  make emu-setup   One-time AVD creation

Utilities
  make build       Build debug APK
  make release     Build signed production APK
  make clean       Clean build artifacts
  make setup       Verify environment
```

## ADB Cheat Sheet

```bash
# List connected devices
adb devices

# Target physical phone only
adb -d <command>

# Target emulator only
adb -e <command>

# Launch the app
adb -d shell am start -n com.hopehealth.app/.MainActivity

# Take a screenshot
adb -d shell screencap -p /sdcard/screen.png
adb -d pull /sdcard/screen.png ~/Desktop/screen.png

# Wake device + dismiss lock
adb -d shell input keyevent 26
adb -d shell input keyevent 82
```

### Navigate Between Tabs

The app uses a bottom nav bar with 5 tabs (Home, Diary, Steps,
Family, Settings). Tap coordinates for 1080-wide screens:

```bash
adb -d shell input tap 108 2160   # Home
adb -d shell input tap 324 2160   # Diary
adb -d shell input tap 540 2160   # Steps
adb -d shell input tap 756 2160   # Family
adb -d shell input tap 972 2160   # Settings
```

## Troubleshooting

If you encounter SDK location errors, verify that
`local.properties` points to the correct Android SDK directory
for your machine.
