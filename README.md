# Liquid Switcher

A fluid, translucent app switcher dock for Android TV and Google TV. Switch between apps with a remote-triggered obsidian-glass dock, spring-physics magnification, and zero-latency key interception.

## Highlights
- **Liquid Glass Dock**: Specular highlights, translucent obsidian gradient, and native GPU outline drop shadows.
- **Physics-Driven Motion**: Dynamic SpringAnimation bell-curve magnification with elevation lift on focused icons.
- **Global Key Capture**: Intercepts remote buttons (CH+/CH-, color keys, numeric keys) via Accessibility Service with microsecond navigation bypass.
- **Flexible Switching Modes**: Dual-key cycling, single-key repeat mode, or direct one-touch shortcuts to specific apps.
- **App Management**: Filter hidden/system apps, sort alphabetically, by recency, or via custom drag-and-drop order.
- **Full Customization**: In-app controls for dock scale (60%–140%), transparency (0%–90%), bottom margin, background dimming, and countdown timer.

## Requirements
- Android 8.0+ (API 26+)
- Android TV or Google TV device with ADB debugging enabled

## Quick Start (Developer CLI)
A unified `./dev.sh` script automates daily workflows:

```bash
# 1. Connect to your TV over Wi-Fi
adb connect <TV_IP>:5555

# 2. Build, install, and enable accessibility service
./dev.sh deploy

# 3. Test dock overlay on TV
./dev.sh trigger

# 4. Cycle apps or inject keys from terminal
./dev.sh next
./dev.sh key ok
./dev.sh dismiss
```

### CLI Commands
| Command | Description |
|---|---|
| `./dev.sh deploy` | Fast-builds, installs APK, and ensures accessibility permissions |
| `./dev.sh trigger` | Broadcasts overlay trigger intent to open dock on TV |
| `./dev.sh dismiss` | Closes dock overlay |
| `./dev.sh shot [name]` | Captures clean screenshot to `captures/` and opens it |
| `./dev.sh logs` | Streams color-filtered logcat for switcher components |
| `./dev.sh format` | Auto-formats Kotlin with Spotless + ktlint |
| `./dev.sh test` | Runs local unit and Robolectric tests |

## Manual Setup
1. Build debug APK: `./gradlew assembleDebug`
2. Install APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Launch **Liquid Switcher** on TV.
4. Select **Service Status** → enable **Liquid Switcher Service** in Android Accessibility settings.
   *(Or via ADB: `adb shell settings put secure enabled_accessibility_services com.baris.tvappswitcher/.KeyInterceptorService && adb shell settings put secure accessibility_enabled 1`)*
5. Configure trigger keys, switch delay, and dock geometry to taste.
