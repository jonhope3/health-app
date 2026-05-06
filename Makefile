.PHONY: setup start build test ui-test exhaustive-test clean run log help phone deploy emu-start emu-stop emu-setup release

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
	@echo "Dev Lifecycle"
	@echo "  make start           - 🚀 Cold start: Start emulator + build + launch"
	@echo "  make run             - 🔄 Fast update: Re-build and launch on running device"
	@echo "  make log             - 📝 View app logs (Logcat)"
	@echo ""
	@echo "Testing"
	@echo "  make test            - 🧪 Unit tests (local)"
	@echo "  make ui-test         - 🖼️  UI navigation tests (emulator)"
	@echo "  make exhaustive-test - 💯 Comprehensive interaction tests (fills all forms)"
	@echo ""
	@echo "Physical Device & Release"
	@echo "  make phone           - 📱 Install & launch on connected USB device"
	@echo "  make deploy          - 🚀 Build and deploy signed production APK"
	@echo ""
	@echo "Emulator Control"
	@echo "  make emu-start       - 🖥️  Start emulator only"
	@echo "  make emu-stop        - 🛑 Stop emulator"
	@echo "  make emu-setup       - 🛠️  One-time AVD creation"
	@echo ""
	@echo "Utilities"
	@echo "  make build           - 🔨 Build debug APK"
	@echo "  make clean           - 🧹 Clean build artifacts"
	@echo "  make setup           - 🔍 Verify environment"

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

## Stop the emulator
emu-stop:
	@chmod +x scripts/emulator_stop.sh
	@./scripts/emulator_stop.sh


# ── Dev workflow ──────────────────────────────────────────────────────────────

## Full cold-start: Start emulator, wait for boot, build and launch.
start:
	@chmod +x scripts/start_dev.sh
	@./scripts/start_dev.sh

## Fast update: Rebuild and launch on the currently running emulator/device.
run:
	@echo "Updating app..."
	@$(GRADLE) installDebug || ($(ADB) uninstall $(PACKAGE_NAME) && $(GRADLE) installDebug)
	@$(ADB) shell monkey -p $(PACKAGE_NAME) -c android.intent.category.LAUNCHER 1

build:
	$(GRADLE) assembleDebug

# ── Testing ───────────────────────────────────────────────────────────────────

## Unit tests (no device)
test:
	$(GRADLE) test

## Compose UI tests — runs on the Pixel 10 Pro emulator.
## Emulator must be running (make emu-start) before calling this.
## These tests are UI-only and do NOT require Gemini Nano.
ui-test:
	@echo "Running standard UI navigation tests..."
	$(GRADLE) connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fittrack.app.ui.test.FitTrackUiTestSuite

## Exhaustive Interaction tests — fills every form and clicks every button.
## Emulator must be running (make emu-start) before calling this.
exhaustive-test:
	@echo "Running comprehensive interaction tests..."
	$(GRADLE) connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.fittrack.app.ui.test.ExhaustiveInteractionsTest

# ── Physical device ───────────────────────────────────────────────────────────
phone:
	@echo "Deploying to physical device..."
	@$(GRADLE) installDebug || ($(ADB) -d uninstall $(PACKAGE_NAME) && $(GRADLE) installDebug)
	@$(ADB) -d shell monkey -p $(PACKAGE_NAME) -c android.intent.category.LAUNCHER 1

release:
	$(GRADLE) assembleRelease
	@cp app/build/outputs/apk/release/app-release.apk ./fittrack-release.apk
	@echo "Production APK: ./fittrack-release.apk"

## Build, sign, and install production APK on the physical Pixel 10 Pro.
## Works over USB cable OR WiFi ADB. Override device with: make deploy DEVICE=ip:port
## Example: make deploy DEVICE=192.168.86.249:39781
DEVICE ?= $(shell $(ADB) devices | awk '/\tdevice$$/ && !/emulator/' | head -1 | awk '{print $$1}')
deploy: release
	@if [ -z "$(DEVICE)" ]; then \
		echo "❌  No physical device found. Connect via USB or run: adb connect <ip>:<port>"; \
		exit 1; \
	fi
	@echo "📱 Deploying to: $(DEVICE)"
	@$(ADB) -s $(DEVICE) uninstall $(PACKAGE_NAME) 2>/dev/null || true
	$(ADB) -s $(DEVICE) install -r ./fittrack-release.apk
	@echo "✅  Production APK deployed to $(DEVICE)"

# ── Utilities ─────────────────────────────────────────────────────────────────
clean:
	$(GRADLE) clean

log:
	$(ADB) logcat -s "FitTrack_App,FitTrack_HC,FitTrack_AI"

