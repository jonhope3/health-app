# FitTrack Android

This is the Android mobile application for FitTrack.

## Build Setup

Choose the setup instructions corresponding to your environment.

### Machine 1 Setup (*)

This setup uses Homebrew and standard Android SDK locations (e.g., `$HOME/Library/Android/sdk`).

1. **Install Java 17**
   Ensure Java 17 is installed (Run `java -version`).
   If not, install via Homebrew:

   ```bash
   brew install openjdk@17
   ```

2. **Install Android Command Line Tools**
   Install the Android CLI tools using Homebrew:

   ```bash
   brew install --cask android-commandlinetools
   ```

3. **Install Android SDK Components**
   Run the following command to install the required SDK platforms and tools:

   ```bash
   yes | sdkmanager --sdk_root=$HOME/Library/Android/sdk "platform-tools" "platforms;android-35" "build-tools;35.0.0" "emulator" "system-images;android-35;google_apis;x86_64"
   ```

4. **Configuration**
   Create a `local.properties` file in the root directory.
   For this machine, it should point to your user library:

   ```properties
   sdk.dir=/Users/jon/Library/Android/sdk
   ```

5. **Emulator Setup**
   A Pixel 10 Pro (modeled on Pixel 9 Pro specs) AVD has been created.
   To launch it from the terminal:

   ```bash
   $HOME/Library/Android/sdk/emulator/emulator -avd Pixel_10_Pro
   ```

   Alternatively, launch it from VS Code using the **Google Android for VS Code** extension.

   **🚀 One-Command Start (Recommended)**:
   This command will check dependencies, create the emulator if missing, launch it, build the app, and run it.

   ```bash
   make start
   ```

   **🔄 Update App (Keep Emulator Running)**:
   Use this to fast re-deploy your changes without restarting the emulator.

   ```bash
   make update
   ```

   **🛑 Teardown (Stop & Clean)**:

   ```bash
   make teardown
   ```

### Machine 2 Setup

This setup uses shared paths for the SDK location.

1. **Configuration**
   Ensure your `local.properties` points to the shared SDK location:

   ```properties
   sdk.dir=/usr/local/share/android-commandlinetools
   ```

### Verification & Building (All Machines - Using Makefile)

1. **Verify Environment**

   ```bash
   make setup
   ```

2. **Build the Debug APK**

   ```bash
   make build
   ```

3. **Run Unit Tests**

   ```bash
   make test
   ```

### Release Builds (Optimized)

To build a production-ready version of the app with code minification and resource shrinking enabled:

1. **Build the Release APK**

   ```bash
   make release
   ```

   The unsigned APK will be generated at `app/build/outputs/apk/release/app-release-unsigned.apk`.

2. **Sign the APK (Required for installation)**

   For security, **never commit your production keystore or passwords to the repository.**

   You can sign the APK using `apksigner`. If you don't have a keystore yet, you can create a temporary one for testing:

   ```bash
   # Generate a temporary keystore (Fill in your details when prompted)
   keytool -genkey -v -keystore release.keystore -alias releaseAlias -keyalg RSA -keysize 2048 -validity 10000
   ```

   ```bash
   # Sign the APK
   apksigner sign --ks release.keystore --out app/build/outputs/apk/release/app-release-signed.apk app/build/outputs/apk/release/app-release-unsigned.apk
   ```

   The final signed APK will be available at `app/build/outputs/apk/release/app-release-signed.apk`.

### Troubleshooting

If you encounter SDK location errors, ensure that `local.properties` points to the correct directory where your Android SDK is installed for your specific machine.

---

## UI Testing & Screenshots

You can take screenshots and interact with the app programmatically using ADB — no need to manually tap the device.

### Prerequisite: Ensure ADB is connected

```bash
# List connected devices (emulator + physical phone)
adb devices

# Target emulator only
adb -e <command>

# Target physical phone only  
adb -d <command>
```

### Take a Screenshot

```bash
# Capture to device, then pull to your Mac
adb -d shell screencap -p /sdcard/screen.png
adb -d pull /sdcard/screen.png ~/Desktop/screen.png

# One-liner: emulator
adb -e shell screencap -p /sdcard/screen.png && adb -e pull /sdcard/screen.png /tmp/screen.png
```

### Navigate Between Screens Programmatically

The app uses a bottom navigation bar with 4 tabs. Tap coordinates are based on the screen width divided into quarters.

```bash
# First: get screen size
adb -d shell wm size
# → Physical size: 1080x2400

# Tap bottom nav tabs (y ≈ 90% of screen height)
# Home (leftmost tab)
adb -d shell input tap 135 2160

# Log Food
adb -d shell input tap 405 2160

# Steps
adb -d shell input tap 675 2160

# Settings
adb -d shell input tap 945 2160
```

### Launch the App

```bash
adb -d shell am start -n com.fittrack.app/.MainActivity
```

### Wake Device / Dismiss Lock Screen

```bash
adb -d shell input keyevent 26   # Power button (wake)
adb -d shell input keyevent 82   # Menu key (dismiss lock)
```

### Full Screenshot Workflow Example

```bash
# Wake phone, open app, wait, take screenshot of each tab
adb -d shell input keyevent 26 && sleep 1
adb -d shell am start -n com.fittrack.app/.MainActivity && sleep 2

for tab in 135 405 675 945; do
  adb -d shell input tap $tab 2160 && sleep 1
  adb -d shell screencap -p /sdcard/tab_$tab.png
  adb -d pull /sdcard/tab_$tab.png /tmp/tab_$tab.png
done
```

### Deploy to Physical Phone

```bash
make phone
```

This builds the debug APK and installs it on your connected USB phone automatically.

