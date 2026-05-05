.PHONY: setup start teardown build test ui-test clean emulator install run log help phone release deploy emu-setup emu-start emu-start-dark emu-stop emulate

# Configuration
GRADLE        := ./gradlew
HOME_DIR      := $(HOME)
SDK           := /usr/local/share/android-commandlinetools
EMULATOR      := $(HOME_DIR)/Library/Android/sdk/emulator/emulator
ADB           := $(HOME_DIR)/Library/Android/sdk/platform-tools/adb
AVD_NAME      := FitTrack_Test
PACKAGE_NAME  := com.fittrack.app

# Default target
all: help

help:
	@echo "FitTrack Android Makefile"
	@echo "─────────────────────────────────────────────────"
	@echo "Emulator (Pixel 10 Pro AVD)"
	@echo "  make emu-setup       - One-time AVD creation & hardware config"
	@echo "  make emu-start       - Start emulator (light mode)"
	@echo "  make emu-start-dark  - Start emulator (dark mode)"
	@echo "  make emu-stop        - Stop emulator"
	@echo ""
	@echo "Development"
	@echo "  make emulate   - 🟢 Start emulator + build + run (cold start)"
	@echo "  make start    - 🚀 One-Step Dev: build & launch on emulator"
	@echo "  make update   - 🔄 Fast update: reinstall without restarting emulator"
	@echo "  make build    - Build debug APK"
	@echo "  make run      - Install & launch on running emulator"
	@echo "  make install  - Install debug APK to running emulator/device"
	@echo "  make log      - Tail app logcat"
	@echo ""
	@echo "Testing"
	@echo "  make test     - Unit tests (no device needed)"
	@echo "  make ui-test  - Compose UI tests on emulator (emulator must be running)"
	@echo ""
	@echo "Physical Device"
	@echo "  make phone    - Install & launch on connected USB device"
	@echo "  make deploy   - Build, sign, and install production APK to phone"
	@echo "  make release  - Build & sign production APK only"
	@echo ""
	@echo "  make setup    - Verify development environment"
	@echo "  make teardown - Stop emulator & clean build"
	@echo "  make clean    - Clean build artifacts"

# ── Environment ───────────────────────────────────────────────────────────────
setup:
	@chmod +x scripts/check_env.sh
	@./scripts/check_env.sh

# ── Emulator lifecycle ────────────────────────────────────────────────────────

## One-time AVD creation
emu-setup:
	@chmod +x scripts/emulator_setup.sh
	@./scripts/emulator_setup.sh

## Start emulator in light mode (default)
emu-start:
	@chmod +x scripts/emulator_start.sh
	@./scripts/emulator_start.sh

## Start emulator in dark mode (for dark-mode UI testing)
emu-start-dark:
	@chmod +x scripts/emulator_start.sh
	@./scripts/emulator_start.sh --dark

## Stop the emulator
emu-stop:
	@chmod +x scripts/emulator_stop.sh
	@./scripts/emulator_stop.sh

# Legacy alias kept for backward compatibility
emulator: emu-start

# ── Dev workflow ──────────────────────────────────────────────────────────────

## Start emulator, wait for boot, build debug APK, install and launch.
## Use this for a full cold-start run on the emulator.
emulate:
	@echo "▶  Starting emulator..."
	@chmod +x scripts/emulator_start.sh
	@./scripts/emulator_start.sh &
	@echo "⏳  Waiting for emulator to boot..."
	@$(ADB) wait-for-device
	@$(ADB) shell 'while [[ "$$(getprop sys.boot_completed)" != "1" ]]; do sleep 2; done'
	@echo "✅  Emulator ready. Installing app..."
	@$(GRADLE) installDebug || ($(ADB) uninstall $(PACKAGE_NAME) && $(GRADLE) installDebug)
	@echo "🚀  Launching app..."
	@$(ADB) shell monkey -p $(PACKAGE_NAME) -c android.intent.category.LAUNCHER 1

start:
	@chmod +x scripts/start_dev.sh
	@./scripts/start_dev.sh

teardown:
	@chmod +x scripts/teardown.sh
	@./scripts/teardown.sh

update: run

build:
	$(GRADLE) assembleDebug

# Install the app (handles signature conflict by uninstalling first if needed)
install:
	@$(GRADLE) installDebug || ($(ADB) uninstall $(PACKAGE_NAME) && $(GRADLE) installDebug)

run: install
	@echo "Launching app..."
	@$(ADB) shell monkey -p $(PACKAGE_NAME) -c android.intent.category.LAUNCHER 1

# ── Testing ───────────────────────────────────────────────────────────────────

## Unit tests (no device)
test:
	$(GRADLE) test

## Compose UI tests — runs on the Pixel 10 Pro emulator.
## Emulator must be running (make emu-start) before calling this.
## These tests are UI-only and do NOT require Gemini Nano.
ui-test:
	@echo "Running Compose UI tests on emulator..."
	$(GRADLE) connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fittrack.app.ui.test.FitTrackUiTestSuite

# ── Physical device ───────────────────────────────────────────────────────────
phone: install
	@echo "Launching app on physical device..."
	@$(ADB) -d shell monkey -p $(PACKAGE_NAME) -c android.intent.category.LAUNCHER 1

release:
	$(GRADLE) assembleRelease
	@cp app/build/outputs/apk/release/app-release.apk ./fittrack-release.apk
	@echo "Production APK: ./fittrack-release.apk"

## Build, sign, and install production APK on the physical Pixel 10 Pro.
## Uninstalls first if a debug build is present (avoids signature conflict).
deploy: release
	@$(ADB) -d uninstall $(PACKAGE_NAME) 2>/dev/null || true
	$(ADB) -d install -r ./fittrack-release.apk
	$(ADB) -d push ./fittrack-release.apk /sdcard/Download/
	@echo "✅  Production APK deployed to device."

# ── Utilities ─────────────────────────────────────────────────────────────────
clean:
	$(GRADLE) clean

log:
	$(ADB) logcat -s "FitTrack_App,FitTrack_HC,FitTrack_AI"

