import re

with open('app/src/main/java/com/baris/tvappswitcher/KeyInterceptorService.kt', 'r') as f:
    content = f.read()

# Remove Last App block
content = re.sub(r'\s*// 5\. Last App Quick-Switch.*?return true\n\s*\}\n', '\n', content, flags=re.DOTALL)

# Remove Long Press block
content = re.sub(r'\s*// 6\. Long-Press Key Mapping.*?return super\.onKeyEvent\(event\)\n\s*\}\n\s*\}', '\n', content, flags=re.DOTALL)

# Remove the handler stuff
content = re.sub(r'\s*private val mainHandler.*?isLongPressTriggered = false\n', '\n', content, flags=re.DOTALL)

with open('app/src/main/java/com/baris/tvappswitcher/KeyInterceptorService.kt', 'w') as f:
    f.write(content)
