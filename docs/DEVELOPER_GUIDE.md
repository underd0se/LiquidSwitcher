# Liquid Switcher — Developer Guide

This guide covers building, testing, deploying, and debugging Liquid Switcher on Android TV and Google TV devices.

---

## 🚀 Quick Start with Developer CLI (`./dev.sh`)

Liquid Switcher includes a unified developer CLI script (`./dev.sh`) that automates everyday development workflows:

```bash
# 1. Connect to TV over ADB Wi-Fi
adb connect <TV_IP>:5555

# 2. Fast-build, deploy, and ensure accessibility permissions
./dev.sh deploy

# 3. Test dock overlay on TV
./dev.sh trigger

# 4. Inject navigation or remote keys from terminal
./dev.sh next
./dev.sh key ok
./dev.sh dismiss
```

### CLI Command Reference

| Command | Arguments | Description |
|---|---|---|
| `./dev.sh deploy` | None | Fast-builds debug APK, installs to TV, and ensures accessibility service is bound. |
| `./dev.sh trigger` | None | Broadcasts overlay trigger intent (`TRIGGER_OVERLAY`) to open dock on TV. |
| `./dev.sh next` | None | Broadcasts `TRIGGER_NEXT` to cycle to next app. |
| `./dev.sh prev` | None | Broadcasts `TRIGGER_PREV` to cycle to previous app. |
| `./dev.sh dismiss` | None | Dismisses active dock overlay (`TRIGGER_DISMISS`). |
| `./dev.sh shot` | `[name]` | Captures clean TV screenshot to `captures/` and opens it locally. |
| `./dev.sh logs` | None | Streams color-filtered logcat for switcher and accessibility service. |
| `./dev.sh key` | `<key_name>` | Injects remote keyevent (`up`, `down`, `left`, `right`, `ok`, `back`, `menu`, `next`, `prev`). |
| `./dev.sh format` | None | Auto-formats Kotlin with Spotless + ktlint. |
| `./dev.sh test` | None | Runs local unit and Robolectric test suites. |
| `./dev.sh clean` | None | Cleans build artifacts and local captures. |

---

## 🛠️ Manual Build & Installation

### 1. Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Sideload via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant Accessibility Service Permissions
Liquid Switcher requires Android Accessibility permissions to intercept remote control keys:
```bash
adb shell settings put secure enabled_accessibility_services com.baris.tvappswitcher/.KeyInterceptorService
adb shell settings put secure accessibility_enabled 1
```

---

## 🧪 Testing & Code Hygiene

### Unit & Robolectric Tests
Liquid Switcher has comprehensive Robolectric and unit test suites covering configuration persistence, key interception logic, dock layout, and app sorting algorithms.

Run tests:
```bash
./dev.sh test
# or
./gradlew testDebugUnitTest
```

### Formatting & Linting
Run Spotless and ktlint:
```bash
./dev.sh format
# or
./gradlew spotlessApply
```

---

## 🏗️ Architecture & Component Overview

- **`KeyInterceptorService` (`AccessibilityService`)**:
  - Global key event capture (`onKeyEvent`) with microsecond bypass for standard navigation keys when dock is inactive.
  - Broadcast receiver for external automation intents (`TRIGGER_OVERLAY`, `TRIGGER_NEXT`, `TRIGGER_PREV`, `TRIGGER_CONFIRM`, `TRIGGER_DISMISS`).
- **`AppSwitcherOverlay`**:
  - Floating `WindowManager` view (`TYPE_ACCESSIBILITY_OVERLAY`).
  - Fluid spring physics magnification using dynamic `SpringAnimation`.
  - Translucent obsidian glass dock with custom specular shaders and drop shadows.
- **`AppRepository`**:
  - Caches installed leanback TV apps asynchronously.
  - Monitors package install/uninstall/update broadcasts.
- **`ConfigManager`**:
  - Manages persisted preferences (`switch_delay_seconds`, `dock_scale_percent`, `dock_transparency_percent`, app ordering, direct key shortcuts).
- **`SoundCueHelper`**:
  - Low-latency sound pool for navigation feedback.
