# Liquid Switcher

A premium, fluid dock switcher for Android TV & Google TV. Allows switching between installed apps with a translucent dock tray using remote control buttons (e.g. Program Up/Down, color buttons).

## Features
- **Fluid Translucent Dock**: Beautiful floating dock overlay with magnification bell-curve, elevation lift, and glowing specular glass materials.
- **Key Interception**: Uses Android's Accessibility Service to capture remote button presses globally with zero latency.
- **Single-Button & Dual-Button Modes**: Cycle through apps with CH+/CH- or a single designated remote button.
- **App Visibility Management**: Easily show/hide specific apps from appearing in the switcher tray.
- **Custom Sort & Reordering**: Supports Alphabetical, Recency, or fully customized ordering.
- **Custom App Shortcuts**: Map dedicated remote buttons (Red, Green, Yellow, Blue, etc.) directly to apps.
- **Safe TV Navigation**: Passes essential navigation keys (D-Pad, OK, Back, Home) to prevent ever locking out TV controls.

## How to Build & Install
1. Open this project in **Android Studio** or build with `./gradlew assembleDebug`.
2. Connect your TV via ADB (`adb connect <TV_IP_ADDRESS>`).
3. Deploy the APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Setup on TV
1. Launch **Liquid Switcher**.
2. Click on the **Service Status** button to open Accessibility settings.
3. Enable **Liquid Switcher Service**.
4. Return to the app to customize your dock size, transparency, margins, and key shortcuts!
