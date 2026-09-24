#!/usr/bin/env bash
set -euo pipefail

DEST="${1:-screenshot.png}"
DEVICE="${2:-192.168.50.247:5555}"

adb -s "$DEVICE" shell "screencap -p /data/local/tmp/screen.png"
adb -s "$DEVICE" pull /data/local/tmp/screen.png "$DEST"
adb -s "$DEVICE" shell "rm -f /data/local/tmp/screen.png"

echo "Screenshot saved successfully to $DEST"
