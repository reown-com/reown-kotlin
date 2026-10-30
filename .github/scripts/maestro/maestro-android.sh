#!/bin/bash
# Android leg of the Maestro Pay suite, run from android-emulator-runner's
# `script` once the emulator is up: installs the wallet APK, smoke-checks its
# launch, captures logcat around maestro-suite.sh and exits with its code.
#
# Env: APP_ID, APK_DIR, ARTIFACTS_DIR (default: debug-artifacts), plus what
# maestro-suite.sh reads.

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
ARTIFACTS_DIR="${ARTIFACTS_DIR:-debug-artifacts}"
export ARTIFACTS_DIR
: "${APP_ID:?APP_ID is required}"
: "${APK_DIR:?APK_DIR is required}"

mkdir -p "$ARTIFACTS_DIR"

echo "Waiting for boot completion..."
adb wait-for-device
# shellcheck disable=SC2016 # expands on the device, not here
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
adb devices

echo "Installing wallet APK:"
APK_COUNT=0
while IFS= read -r apk; do
  echo "Installing: $apk"
  adb install -r "$apk" || exit 1
  APK_COUNT=$((APK_COUNT + 1))
done < <(find "$APK_DIR" -name "*.apk")
if [ "$APK_COUNT" -eq 0 ]; then
  echo "ERROR: no APK found in $APK_DIR"
  exit 1
fi
if ! adb shell pm list packages | grep -qx "package:$APP_ID"; then
  echo "ERROR: $APP_ID not installed"
  exit 1
fi

# Let the system settle after the install.
sleep 10

# Launch smoke check: fail fast if the APK is broken (e.g. dex missing the
# Application class -> ClassNotFoundException on startup). Without this, a bad
# build surfaces as every Maestro flow failing, which is hard to diagnose.
echo "Smoke-checking app launch:"
CRASH_PATTERN="FATAL EXCEPTION|ClassNotFoundException|Unable to instantiate application"
adb logcat -c
adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 || true
sleep 8
if adb logcat -d | grep -qE "$CRASH_PATTERN"; then
  echo "ERROR: App crashed on launch - APK is broken, not running Maestro."
  adb logcat -d | grep -E "$CRASH_PATTERN|AndroidRuntime" | head -40
  exit 1
fi
echo "App launched cleanly."
adb shell am force-stop "$APP_ID"
sleep 5

# Clear every buffer so the captures below only cover the suite.
adb logcat -b all -c || true
# Full logcat (uploaded only when the job fails).
adb logcat > "$ARTIFACTS_DIR/full-logcat.txt" 2>&1 &
FULL_PID=$!
# Small always-uploaded capture: crashes, the Pay screens' logs and launch
# diagnostics (info-level ActivityManager/ActivityTaskManager) for the flake
# where a launch never reaches the app.
adb logcat AndroidRuntime:E ActivityManager:I ActivityTaskManager:I \
  PaymentWebView:V PaymentViewModel:V '*:F' \
  > "$ARTIFACTS_DIR/android-logcat.log" 2>&1 &
FILTERED_PID=$!
# Events buffer: process start/death and resumed activity.
adb logcat -b events > "$ARTIFACTS_DIR/android-logcat-events.log" 2>&1 &
EVENTS_PID=$!

echo "Running Maestro Pay E2E tests:"
bash "$SCRIPTS_DIR/maestro-suite.sh" Android
maestro_exit_code=$?

kill "$FULL_PID" "$FILTERED_PID" "$EVENTS_PID" 2>/dev/null || true
wait "$FULL_PID" "$FILTERED_PID" "$EVENTS_PID" 2>/dev/null || true
adb logcat -d -b crash > "$ARTIFACTS_DIR/android-logcat-crash.log" 2>&1 || true
exit "$maestro_exit_code"
