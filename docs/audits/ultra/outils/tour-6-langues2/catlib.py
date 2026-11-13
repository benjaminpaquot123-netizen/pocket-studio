"""Line-preserving edits of the app's i18n JSON catalogs (one "key": "value" per line)."""
import json, re

LINE = re.compile(r'^(\s*)("(?:[^"\\]|\\.)*")(\s*:\s*)("(?:[^"\\]|\\.)*")(,?)(\s*)$')


def _encode(new, old_raw):
    out = json.dumps(new, ensure_ascii=False)
    # keep the original's \uXXXX escapes for the characters it escaped
    for m in set(re.findall(r'\\u([0-9a-fA-F]{4})', old_raw)):
        ch = chr(int(m, 16))
        out = out.replace(ch, '\\u' + m)
    return out


def edit(path, fn):
    """fn(key, value) -> new value or None. Returns [(key, old, new)]."""
    with open(path, encoding='utf-8') as f:
        text = f.read()
    lines = text.split('\n')
    changes = []
    for i, line in enumerate(lines):
        m = LINE.match(line)
        if not m:
            continue
        key = json.loads(m.group(2)); val = json.loads(m.group(4))
        new = fn(key, val)
        if new is None or new == val:
            continue
        lines[i] = m.group(1) + m.group(2) + m.group(3) + _encode(new, m.group(4)) + m.group(5) + m.group(6)
        changes.append((key, val, new))
    if changes:
        out = '\n'.join(lines)
        json.loads(out)  # still valid
        with open(path, 'w', encoding='utf-8') as f:
            f.write(out)
    return changes


def entries(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)
