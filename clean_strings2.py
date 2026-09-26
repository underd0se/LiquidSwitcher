for file_path in ['app/src/main/res/values/strings.xml', 'app/src/main/res/values-tr/strings.xml']:
    with open(file_path, 'r') as f:
        content = f.read()

    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if 'instructions_' in line or 'about_' in line:
            continue
        new_lines.append(line)

    with open(file_path, 'w') as f:
        f.write('\n'.join(new_lines))
