#!/usr/bin/env bash
set -euo pipefail

DEST="${1:-captures/screenshot_$(date +%Y%m%d_%H%M%S).png}"
if [[ -z "${2:-}" ]]; then
    _CONNECTED="$(adb devices 2>/dev/null | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+' | head -n1 | awk '{print $1}')"
    DEVICE="${_CONNECTED:-192.168.50.11:5555}"
else
    DEVICE="$2"
fi

mkdir -p "$(dirname "$DEST")"

adb -s "$DEVICE" shell "screencap -p /data/local/tmp/screen.png"
adb -s "$DEVICE" pull /data/local/tmp/screen.png "$DEST"
adb -s "$DEVICE" shell "rm -f /data/local/tmp/screen.png"

echo "Screenshot saved successfully to $DEST"
