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

   Alternatively, launch it from VS Code using the **Google Android for VS Code** extension. This is the recommended "Antigravity-compatible" plugin for managing devices visually.

6. **Build & Run**
   After launching the emulator, you can install and run the app with:

   ```bash
   ./gradlew installDebug
   ```

### Machine 2 Setup

This setup uses shared paths for the SDK location.

1. **Configuration**
   Ensure your `local.properties` points to the shared SDK location:

   ```properties
   sdk.dir=/usr/local/share/android-commandlinetools
   ```

### Verification & Building (All Machines)

1. **Verify Environment**
   Run the helper script to verify your setup:

   ```bash
   chmod +x scripts/check_env.sh
   ./scripts/check_env.sh
   ```

2. **Build the Debug APK**

   ```bash
   ./gradlew assembleDebug
   ```

3. **Run Unit Tests**

   ```bash
   ./gradlew test
   ```

### Troubleshooting

If you encounter SDK location errors, ensure that `local.properties` points to the correct directory where your Android SDK is installed for your specific machine.
