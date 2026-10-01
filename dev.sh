#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEVICE="${TV_DEVICE:-192.168.50.247:5555}"
PACKAGE="com.baris.tvappswitcher"
SERVICE="${PACKAGE}/.KeyInterceptorService"
APK="${SCRIPT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
CAPTURES_DIR="${SCRIPT_DIR}/captures"

ensure_device() {
    if ! adb devices | grep -q "$DEVICE"; then
        echo "Connecting to TV at $DEVICE..."
        adb connect "$DEVICE" || true
    fi
}

cmd_deploy() {
    ensure_device
    echo "🔨 Building debug APK..."
    "${SCRIPT_DIR}/gradlew" assembleDebug

    echo "📲 Installing APK to $DEVICE..."
    adb -s "$DEVICE" install -r "$APK"

    echo "🛡️ Ensuring accessibility service is enabled..."
    adb -s "$DEVICE" shell settings put secure enabled_accessibility_services "$SERVICE"
    adb -s "$DEVICE" shell settings put secure accessibility_enabled 1

    echo "✅ Successfully deployed to TV!"
}

cmd_trigger() {
    ensure_device
    echo "🚀 Triggering dock overlay..."
    adb -s "$DEVICE" shell am broadcast -a "${PACKAGE}.TRIGGER_OVERLAY"
}

cmd_dismiss() {
    ensure_device
    echo "👋 Dismissing dock overlay..."
    adb -s "$DEVICE" shell am broadcast -a "${PACKAGE}.TRIGGER_DISMISS"
}

cmd_next() {
    ensure_device
    adb -s "$DEVICE" shell am broadcast -a "${PACKAGE}.TRIGGER_NEXT"
}

cmd_prev() {
    ensure_device
    adb -s "$DEVICE" shell am broadcast -a "${PACKAGE}.TRIGGER_PREV"
}

cmd_confirm() {
    ensure_device
    adb -s "$DEVICE" shell am broadcast -a "${PACKAGE}.TRIGGER_CONFIRM"
}

cmd_screenshot() {
    ensure_device
    local name="${1:-screenshot_$(date +%Y%m%d_%H%M%S).png}"
    if [[ "$name" != /* ]]; then
        mkdir -p "$CAPTURES_DIR"
        name="${CAPTURES_DIR}/${name}"
    fi

    echo "📸 Capturing TV screen..."
    adb -s "$DEVICE" shell "screencap -p /data/local/tmp/screen.png"
    adb -s "$DEVICE" pull /data/local/tmp/screen.png "$name"
    adb -s "$DEVICE" shell "rm -f /data/local/tmp/screen.png"
    echo "✅ Screenshot saved to $name"

    if [[ "$(uname)" == "Darwin" ]] && [[ "${OPEN:-1}" == "1" ]]; then
        open "$name" 2>/dev/null || true
    fi
}

cmd_logs() {
    ensure_device
    echo "📋 Streaming TVAppSwitcher logs (Ctrl+C to stop)..."
    adb -s "$DEVICE" logcat -v color -s AppSwitcherOverlay KeyInterceptorService AppRepository LiquidDockView ConfigManager SoundCueHelper
}

cmd_key() {
    ensure_device
    local target="${1:-}"
    local lower_target
    lower_target="$(echo "$target" | tr '[:upper:]' '[:lower:]')"
    local code=""
    case "$lower_target" in
        up) code=19 ;;
        down) code=20 ;;
        left) code=21 ;;
        right) code=22 ;;
        ok|enter|center) code=23 ;;
        back) code=4 ;;
        menu) code=82 ;;
        next|chup) code=166 ;;
        prev|chdown) code=167 ;;
        home) code=3 ;;
        [0-9]*) code="$target" ;;
        *)
            echo "Usage: ./dev.sh key <up|down|left|right|ok|back|menu|next|prev|home|<code_number>>"
            return 1
            ;;
    esac
    echo "🎮 Sending keyevent $code ($target)..."
    adb -s "$DEVICE" shell input keyevent "$code"
}

cmd_clean() {
    echo "🧹 Cleaning Gradle build and local captures..."
    "${SCRIPT_DIR}/gradlew" clean
    rm -rf "$CAPTURES_DIR"
    echo "✅ Clean completed!"
}

cmd_format() {
    echo "✨ Formatting code with Spotless + ktlint..."
    "${SCRIPT_DIR}/gradlew" spotlessApply
}

cmd_test() {
    echo "🧪 Running unit tests..."
    "${SCRIPT_DIR}/gradlew" test
}

cmd_lint() {
    echo "🔍 Running lint..."
    "${SCRIPT_DIR}/gradlew" lintDebug
}

usage() {
    cat <<EOF
Usage: ./dev.sh <command> [arguments]

Commands:
  deploy, install   Build and install APK on TV, auto-enable accessibility service
  trigger, dock     Open dock overlay on TV
  dismiss           Dismiss dock overlay on TV
  next              Cycle to next app in dock
  prev              Cycle to prev app in dock
  confirm           Launch selected app in dock
  shot, screenshot  Take clean TV screenshot (saved to captures/ and opened)
  logs, logcat      Follow filtered TVAppSwitcher logcat in color
  key <name|code>   Send remote keyevent (up, down, left, right, ok, back, menu, next, prev)
  test              Run local unit tests
  lint              Run Android lint checks
  format            Auto-format Kotlin code with Spotless + ktlint
  clean             Clean build cache and screenshots

Environment Variables:
  TV_DEVICE         Target TV ADB address (default: 192.168.50.247:5555)
  OPEN              Open screenshot after capture (1=yes, 0=no; default: 1)
EOF
}

case "${1:-}" in
    deploy|install) cmd_deploy ;;
    trigger|dock) cmd_trigger ;;
    dismiss) cmd_dismiss ;;
    next) cmd_next ;;
    prev) cmd_prev ;;
    confirm) cmd_confirm ;;
    shot|screenshot) shift; cmd_screenshot "${1:-}" ;;
    logs|logcat) cmd_logs ;;
    key) shift; cmd_key "${1:-}" ;;
    test) cmd_test ;;
    lint) cmd_lint ;;
    format) cmd_format ;;
    clean) cmd_clean ;;
    help|--help|-h|"") usage ;;
    *)
        echo "Unknown command: $1"
        usage
        exit 1
        ;;
esac
