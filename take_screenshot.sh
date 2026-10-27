#!/usr/bin/env bash
set -euo pipefail

DEST="${1:-captures/screenshot_$(date +%Y%m%d_%H%M%S).png}"
if [[ -n "${2:-}" ]]; then
    DEVICE="$2"
elif [[ -n "${TV_DEVICE:-}" ]]; then
    DEVICE="$TV_DEVICE"
else
    _CONNECTED="$(adb devices 2>/dev/null | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+' | head -n1 | awk '{print $1}')"
    DEVICE="${_CONNECTED:-}"
fi

if [[ -z "$DEVICE" ]]; then
    echo "❌ Error: No TV device specified or connected."
    echo "Usage: ./take_screenshot.sh [output_path.png] [TV_IP:5555]"
    echo "Or connect via ADB first: adb connect <TV_IP>:5555"
    echo "Or set the TV_DEVICE environment variable: export TV_DEVICE=<TV_IP>:5555"
    exit 1
fi

mkdir -p "$(dirname "$DEST")"

adb -s "$DEVICE" shell "screencap -p /data/local/tmp/screen.png"
adb -s "$DEVICE" pull /data/local/tmp/screen.png "$DEST"
adb -s "$DEVICE" shell "rm -f /data/local/tmp/screen.png"

echo "Screenshot saved successfully to $DEST"
