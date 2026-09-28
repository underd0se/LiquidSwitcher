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
