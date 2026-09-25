#!/usr/bin/env bash
set -e

APK_PATH="app/build/outputs/apk/release/app-release.apk"
ADB_BIN="${ANDROID_HOME:-/root/android-sdk}/platform-tools/adb"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH. Building release APK first..."
    ./gradlew assembleRelease
fi

echo "=============================================="
echo "📦 StudyOS Release APK Deployment"
echo "=============================================="
echo "APK Path : $APK_PATH"
echo "APK Size : $(du -h "$APK_PATH" | cut -f1)"
echo "Version  : 1.1.0"
echo "=============================================="

if command -v adb &> /dev/null; then
    ADB_CMD="adb"
elif [ -f "$ADB_BIN" ]; then
    ADB_CMD="$ADB_BIN"
else
    echo "⚠️ ADB not found. Sideload the APK directly onto your device."
    exit 0
fi

echo "🔍 Checking for connected devices..."
"$ADB_CMD" devices

DEVICE_COUNT=$("$ADB_CMD" devices | grep -v "List" | grep "device" | wc -l)

if [ "$DEVICE_COUNT" -gt 0 ]; then
    echo "🚀 Installing StudyOS on connected device..."
    "$ADB_CMD" install -r "$APK_PATH"
    echo "✅ StudyOS successfully installed!"
    echo "📱 Launching StudyOS..."
    "$ADB_CMD" shell am start -n com.studyos.app/.MainActivity
else
    echo "⚠️ No Android device or emulator currently connected."
    echo "👉 To install via USB/WiFi:"
    echo "   1. Enable Developer Options & USB Debugging on your phone."
    echo "   2. Connect phone via USB or WiFi adb connect <IP>:5555."
    echo "   3. Run: adb install -r $APK_PATH"
fi
