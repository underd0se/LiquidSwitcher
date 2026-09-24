# TVAppSwitcher Context & Rules

## Android TV Screenshots
> [!CAUTION]
> **NEVER USE `adb exec-out screencap -p > file.png`!**
> The Android TV driver prints debug logs (`Init wrapper sys mutex successful...`) to stdout, corrupting the PNG header. When `view_file` reads corrupted PNG bytes, the Gemini API crashes with `INVALID_ARGUMENT (code 400)`.

### How to Take TV Screenshots
Always save to TV disk first and pull, or use the helper script:
```bash
./take_screenshot.sh <output_path.png>
```

Or manually:
```bash
adb -s 192.168.50.247:5555 shell "screencap -p /data/local/tmp/screen.png"
adb -s 192.168.50.247:5555 pull /data/local/tmp/screen.png <output_path.png>
adb -s 192.168.50.247:5555 shell "rm -f /data/local/tmp/screen.png"
```

## Autonomous Subagent Delegation & Dynamic Routing (Laya MLX Protocol)

To optimize context window usage and minimize cloud tokens in TVAppSwitcher:

### 1. Autonomous Task Decomposition
Do not run heavy or noisy operations monolithically in the primary thread. Autonomously decompose and delegate them to background subagents:
- **Builds & Tests**: Gradle builds (`./gradlew assembleDebug`, `./gradlew test`), linting, and APK verification.
- **Log Scraping & Device Diagnostics**: Long-running ADB logcat parsing, keycode event tests, or inspecting device state (`adb shell ...`).
- **Multi-File Audits**: Reviewing strings (`values/`, `values-tr/`), layout XMLs, or accessibility configs in parallel.

### 2. Zero-Token Local Routing via Laya-MLX
Before spawning each subagent via `invoke_subagent`:
1. Run `laya-route --tier-only "<subagent task description>"` locally in sub-15ms.
2. Pass the resulting tier directly to `invoke_subagent`'s `Model` parameter (`flash_lite`, `flash`, or `pro`).
