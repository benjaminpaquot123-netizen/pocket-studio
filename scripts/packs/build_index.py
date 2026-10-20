"""Zips each generated pack (flat) and writes index.json for the app.

usage: build_index.py PACKS_DIR OUT_DIR
PACKS_DIR holds one folder per pack (pack.json + FLAC files); OUT_DIR gets <id>.zip and index.json.
"""
import hashlib
import json
import sys
import zipfile
from pathlib import Path


def main():
    packs, out = Path(sys.argv[1]), Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    entries = []
    for d in sorted(p for p in packs.iterdir() if (p / 'pack.json').is_file()):
        manifest = json.loads((d / 'pack.json').read_text())
        if not manifest['sounds']:
            print(f'{d.name}: empty, left out')
            continue
        zip_path = out / f"{manifest['id']}.zip"
        with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_STORED) as z:  # FLAC is already compressed
            for f in ['pack.json'] + [s['file'] for s in manifest['sounds']]:
                info = zipfile.ZipInfo(f, date_time=(2026, 1, 1, 0, 0, 0))
                z.writestr(info, (d / f).read_bytes())
        data = zip_path.read_bytes()
        entries.append({
            'id': manifest['id'], 'name': manifest['name'], 'file': zip_path.name,
            'sha256': hashlib.sha256(data).hexdigest(), 'sizeBytes': len(data), 'sounds': len(manifest['sounds']),
        })
        print(f"{manifest['id']}: {len(manifest['sounds'])} sounds, {len(data) / 1e6:.1f} MB")
    (out / 'index.json').write_text(json.dumps({'packs': entries}, ensure_ascii=False, indent=1))
    index = (out / 'index.json').read_bytes()
    print(f'index.json sha256 {hashlib.sha256(index).hexdigest()} size {len(index)}')


if __name__ == '__main__':
    main()
