import re

with open('app/src/main/java/com/baris/tvappswitcher/MainActivity.kt', 'r') as f:
    content = f.read()

content = re.sub(r'\s*tvLastAppKey = findViewById.*?\n', '\n', content)
content = re.sub(r'\s*btnAssignLastApp = findViewById.*?\n', '\n', content)
content = re.sub(r'\s*btnClearLastApp = findViewById.*?\n', '\n', content)
content = re.sub(r'\s*tvLongPressKey = findViewById.*?\n', '\n', content)
content = re.sub(r'\s*btnAssignLongPress = findViewById.*?\n', '\n', content)
content = re.sub(r'\s*btnClearLongPress = findViewById.*?\n', '\n', content)

# Remove setOnClickListener blocks
content = re.sub(r'\s*btnAssignLastApp\.setOnClickListener \{.*?\n\s*\}\n', '\n', content, flags=re.DOTALL)
content = re.sub(r'\s*btnClearLastApp\.setOnClickListener \{.*?\n\s*\}\n', '\n', content, flags=re.DOTALL)
content = re.sub(r'\s*btnAssignLongPress\.setOnClickListener \{.*?\n\s*\}\n', '\n', content, flags=re.DOTALL)
content = re.sub(r'\s*btnClearLongPress\.setOnClickListener \{.*?\n\s*\}\n', '\n', content, flags=re.DOTALL)

# Remove from setupFocusEffects list
content = content.replace("            btnAssignLastApp,\n", "")
content = content.replace("            btnClearLastApp,\n", "")
content = content.replace("            btnAssignLongPress,\n", "")
content = content.replace("            btnClearLongPress\n", "")
content = content.replace("            btnTestKey,\n", "            btnTestKey\n")

# Remove update methods calls
content = content.replace("        updateLastAppKeyUi()\n", "")
content = content.replace("        updateLongPressKeyUi()\n", "")

# Remove updateLastAppKeyUi block
content = re.sub(r'\s*private fun updateLastAppKeyUi\(\) \{.*?\n    \}\n', '\n', content, flags=re.DOTALL)
# Remove updateLongPressKeyUi block
content = re.sub(r'\s*private fun updateLongPressKeyUi\(\) \{.*?\n    \}\n', '\n', content, flags=re.DOTALL)

# Remove BroadcastReceiver logic
content = re.sub(r'\s*"LAST_APP" -> \{.*?\}\n', '\n', content, flags=re.DOTALL)
content = re.sub(r'\s*"LONG_PRESS" -> \{.*?\}\n', '\n', content, flags=re.DOTALL)

with open('app/src/main/java/com/baris/tvappswitcher/MainActivity.kt', 'w') as f:
    f.write(content)
