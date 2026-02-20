.PHONY: setup start teardown build test clean emulator install run log help phone

# Configuration
GRADLE := ./gradlew
HOME_DIR := $(HOME)
EMULATOR := $(HOME_DIR)/Library/Android/sdk/emulator/emulator
ADB := $(HOME_DIR)/Library/Android/sdk/platform-tools/adb
AVD_NAME := Pixel_10_Pro
PACKAGE_NAME := com.fittrack.app

# Default target
all: help

help:
	@echo "FitTrack Android Makefile"
	@echo "-------------------------"
	@echo "make setup    - Verify development environment"
	@echo "make start    - 🚀 One-Step Dev: Setup, Build, & Launch Emulator/App"
	@echo "make phone    - 📱 Run on Physical Phone (Must be connected via USB/ADB)"
	@echo "make teardown - 🛑 Stop Emulator & Clean Build"
	@echo "make update   - 🔄 Fast Update: Reinstall & Launch App (Keep Emulator Running)"
	@echo "make build    - Build the debug APK"
	@echo "make test     - Run unit tests"
	@echo "make clean    - Clean build artifacts"
	@echo "make emulator - Launch the Pixel 10 Pro Emulator"
	@echo "make install  - Install the app to the running emulator/device"
	@echo "make run      - Install and launch the app on the emulator/device"
	@echo "make log      - View app logs (logcat)"

# Verify environment
setup:
	@chmod +x scripts/check_env.sh
	@./scripts/check_env.sh

# One-step Start
start:
	@chmod +x scripts/start_dev.sh
	@./scripts/start_dev.sh

# Run on physical phone
phone: install
	@echo "Launching app on physical device..."
	@$(ADB) -d shell monkey -p $(PACKAGE_NAME) -c android.intent.category.LAUNCHER 1

# Stop Emulator & Clean
teardown:
	@chmod +x scripts/teardown.sh
	@./scripts/teardown.sh

# Update app without restarting emulator
update: run

# Build the debug APK
build:
	$(GRADLE) assembleDebug

# Run unit tests
test:
	$(GRADLE) test

# Clean build artifacts
clean:
	$(GRADLE) clean

# Launch the Pixel 10 Pro Emulator
emulator:
	@echo "Starting Emulator: $(AVD_NAME)..."
	@$(EMULATOR) -avd $(AVD_NAME) &

# Install the app to the running emulator/device
install:
	$(GRADLE) installDebug

# Install and launch the app
run: install
	@echo "Launching app..."
	@$(ADB) shell monkey -p $(PACKAGE_NAME) -c android.intent.category.LAUNCHER 1

# View logs
log:
	$(ADB) logcat -s "FitTrackApp"
