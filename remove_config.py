import re

with open('app/src/main/java/com/baris/tvappswitcher/ConfigManager.kt', 'r') as f:
    content = f.read()

content = content.replace("    private const val KEY_LAST_APP_TRIGGER = \"last_app_trigger\"\n", "")
content = content.replace("    private const val KEY_LONG_PRESS_TRIGGER = \"long_press_trigger\"\n", "")

content = re.sub(r'\s*fun getLastAppKey.*?\}\n', '\n', content, flags=re.DOTALL)
content = re.sub(r'\s*fun setLastAppKey.*?\}\n', '\n', content, flags=re.DOTALL)
content = re.sub(r'\s*fun getLongPressKey.*?\}\n', '\n', content, flags=re.DOTALL)
content = re.sub(r'\s*fun setLongPressKey.*?\}\n', '\n', content, flags=re.DOTALL)

with open('app/src/main/java/com/baris/tvappswitcher/ConfigManager.kt', 'w') as f:
    f.write(content)
