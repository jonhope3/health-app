#!/bin/bash

# Define constants
ANDROID_SDK_PATH="$HOME/Library/Android/sdk"
PLATFORM_TOOLS_PATH="$ANDROID_SDK_PATH/platform-tools"
ANDROI_TOOLS_CASK_NAME="android-commandlinetools"

echo "=== Environment Verification Script ==="
echo ""

# 1. Check for Brew
echo "[1/4] Checking Homebrew..."
if ! command -v brew &> /dev/null; then
    echo "❌ Homebrew is NOT installed."
    echo "   Action: Install Homebrew from https://brew.sh/"
    exit 1
else
    echo "✅ Homebrew is installed."
fi

# 2. Check for Android Command Line Tools (via Brew or PATH)
echo ""
echo "[2/4] Checking Android Command Line Tools..."
if brew list --cask "$ANDROI_TOOLS_CASK_NAME" &> /dev/null; then
    echo "✅ android-commandlinetools cask is installed."
elif command -v sdkmanager &> /dev/null; then
    echo "✅ sdkmanager found in PATH."
else
    echo "⚠️  android-commandlinetools NOT found."
    echo "   Action: Run 'brew install --cask android-commandlinetools'"
    # We don't exit here because user might have manual installation
fi

# 3. Check for Local Properties Configuration
echo ""
echo "[3/4] Checking local.properties..."
if [ -f "local.properties" ]; then
    SDK_DIR=$(grep sdk.dir local.properties | cut -d'=' -f2)
    if [ "$SDK_DIR" == "$ANDROID_SDK_PATH" ]; then
        echo "✅ local.properties points to standard SDK location: $SDK_DIR"
    else
        echo "ℹ️  local.properties uses custom SDK location: $SDK_DIR"
    fi
else
    echo "❌ local.properties NOT found."
    echo "   Action: Create local.properties with 'sdk.dir=$ANDROID_SDK_PATH'"
    # We can create it automatically
    echo "   Attempting to create local.properties..."
    echo "sdk.dir=$ANDROID_SDK_PATH" > local.properties
    echo "   ✅ Created local.properties pointing to $ANDROID_SDK_PATH"
fi

# 4. Check SDK Components
echo ""
echo "[4/4] Checking Android SDK Components..."
if [ -d "$ANDROID_SDK_PATH" ]; then
    echo "✅ SDK Directory exists at $ANDROID_SDK_PATH"
    
    # Check platforms
    if ls "$ANDROID_SDK_PATH/platforms" &> /dev/null; then
         PLATFORMS=$(ls "$ANDROID_SDK_PATH/platforms")
         echo "   Installed Platforms: $PLATFORMS"
    else
         echo "❌ No platforms found in $ANDROID_SDK_PATH/platforms"
         echo "   Action: Run 'sdkmanager \"platforms;android-35\"'"
    fi

    # Check build-tools
    if ls "$ANDROID_SDK_PATH/build-tools" &> /dev/null; then
         BUILD_TOOLS=$(ls "$ANDROID_SDK_PATH/build-tools")
         echo "   Installed Build Tools: $BUILD_TOOLS"
    else
         echo "❌ No build-tools found in $ANDROID_SDK_PATH/build-tools"
         echo "   Action: Run 'sdkmanager \"build-tools;35.0.0\"'"
    fi
else
    echo "❌ SDK Directory NOT found at $ANDROID_SDK_PATH"
    echo "   Action: Run 'sdkmanager --sdk_root=$ANDROID_SDK_PATH \"platform-tools\" \"platforms;android-35\"'"
fi

echo ""
echo "=== Verification Complete ==="
echo "If all checks passed, you can run './gradlew assembleDebug' to build."
