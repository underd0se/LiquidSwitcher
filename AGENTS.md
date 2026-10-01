# TVAppSwitcher Context & Rules

## Android TV Screenshots
> [!CAUTION]
> **NEVER USE `adb exec-out screencap -p > file.png`!**
> The Android TV driver prints debug logs (`Init wrapper sys mutex successful...`) to stdout, corrupting the PNG header. When `view_file` reads corrupted PNG bytes, the Gemini API crashes with `INVALID_ARGUMENT (code 400)`.

### How to Take TV Screenshots
Always save to TV disk first and pull, or use the dev helper:
```bash
./dev.sh shot [output_path.png]
# or
./take_screenshot.sh <output_path.png>
```

Or manually:
```bash
adb -s 192.168.50.247:5555 shell "screencap -p /data/local/tmp/screen.png"
adb -s 192.168.50.247:5555 pull /data/local/tmp/screen.png <output_path.png>
adb -s 192.168.50.247:5555 shell "rm -f /data/local/tmp/screen.png"
```

## Developer CLI (`./dev.sh`)
Use `./dev.sh` for fast daily development tasks:
- `./dev.sh deploy` — Fast-builds, installs to TV, and ensures accessibility service is enabled.
- `./dev.sh trigger` — Broadcasts trigger intent to open dock on TV.
- `./dev.sh dismiss` — Dismisses dock on TV.
- `./dev.sh shot [name]` — Captures clean TV screenshot to `captures/` and opens it.
- `./dev.sh logs` — Follows filtered TVAppSwitcher logcat in color.
- `./dev.sh key <up|down|left|right|ok|back|menu|next|prev>` — Injects remote keyevents.
- `./dev.sh format` — Auto-formats Kotlin with Spotless + ktlint.
- `./dev.sh test` — Runs unit tests (including Robolectric).
- `./dev.sh clean` — Cleans build artifacts and local captures.

## Autonomous Subagent Delegation & Dynamic Routing (Laya MLX Protocol)

To eliminate the "micro-delegation tax" and keep iterations fast:

### 1. Direct Execution in Primary Thread
- **Always run directly via `run_command` in the primary thread**: Gradle builds (`./gradlew assembleDebug`, `./gradlew test`), ADB commands (`adb install`, `dumpsys`, logcat, keyevents), and screenshot pulls. Spawning subagents for these commands creates severe token and latency penalties.

### 2. When to Delegate to Subagents
Delegate via `invoke_subagent` ONLY for:
- **Parallel Multi-File Audits**: Concurrently auditing XML layouts, strings, and accessibility configs.
- **Broad Source Exploration**: Using `jg "<question>" .` across unfamiliar codebases.

### 3. Local Routing via Laya-MLX
Before spawning each subagent via `invoke_subagent`:
1. Run `laya-route --tier-only "<subagent task description>"` locally in sub-20ms.
2. Pass the resulting tier directly to `invoke_subagent`'s `Model` parameter (`flash_lite`, `flash`, or `pro`). Utility tasks must be capped to `flash_lite` or `flash`.
