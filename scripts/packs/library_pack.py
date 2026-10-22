"""Builds one sound pack from free sound libraries (CC0 / CC-BY) listed in libraries.json.

usage: library_pack.py libraries.json PACK_ID OUT_DIR [--cache DIR] [--only SOURCE_ID ...]

Each source is fetched once into the cache (a zip, single files, a Kenney page's zip, or a sparse
git checkout at a pinned commit). The pack's rules pick files by regular expression on
"<source id>/<path inside it>" (first matching rule wins, files no rule takes are left out), at
most the rule's `max` per rule, spread evenly over the matches. Each sound is trimmed (silence
before a hit), cut to `max_seconds` with a fade, normalized to -1 dBFS, made mono when both sides
are the same, and written as 16-bit FLAC <kind>_<slug>_<n>.flac; pack.json lists names, kinds,
tags (the app searches them) and the credits of each source used.
"""
import io
import json
import re
import subprocess
import sys
import urllib.request
import zipfile
from pathlib import Path

import numpy as np
import soundfile as sf

AUDIO = ('.wav', '.ogg', '.oga', '.flac', '.aif', '.aiff')
UA = {'User-Agent': 'pocket-studio-packs (+https://github.com/benjaminpaquot123-netizen/pocket-studio)'}

# The kinds the app knows (SoundPacks.KIND_LABELS): a sound of another kind would be dropped.
KINDS = {'kick', 'snare', 'clap', 'hat', 'open_hat', 'ride', 'perc', 'tom', '808', 'bass', 'stab', 'pad', 'lead',
         'pluck', 'keys', 'mallet', 'vocal', 'foley', 'fx', 'loop'}


def get(url, tries=4):
    last = None
    for _ in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120) as r:
                return r.read()
        except Exception as e:  # network hiccup: try again
            last = e
    raise RuntimeError(f'{url}: {last}')


def fetch(source, cache):
    """The source's folder in the cache, downloaded once."""
    d = cache / source['id']
    if (d / '.done').exists():
        return d
    d.mkdir(parents=True, exist_ok=True)
    kind = source['type']
    if kind == 'kenney':
        page = get(source['page']).decode('utf-8', 'replace')
        slug = source['page'].rstrip('/').rsplit('/', 1)[1]
        m = re.search(rf"https://kenney\.nl/media/pages/assets/{re.escape(slug)}/[^'\"]+\.zip", page)
        if not m:
            raise RuntimeError(f"{source['id']}: no zip on {source['page']}")
        unzip(get(m.group(0)), d)
    elif kind == 'zip':
        unzip(get(source['url']), d)
    elif kind == 'files':
        for url in source['urls']:
            (d / url.rsplit('/', 1)[1]).write_bytes(get(url))
    elif kind == 'git':
        repo = d / 'repo'
        subprocess.run(['git', 'clone', '-q', '--filter=blob:none', '--no-checkout', source['repo'], str(repo)], check=True)
        subprocess.run(['git', '-C', str(repo), 'sparse-checkout', 'set', '--no-cone', *source['paths']], check=True)
        subprocess.run(['git', '-C', str(repo), 'checkout', '-q', source['commit']], check=True)
    else:
        raise ValueError(kind)
    (d / '.done').write_text('ok')
    return d


def unzip(data, into):
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        for info in z.infolist():
            name = info.filename
            if info.is_dir() or '..' in Path(name).parts or name.startswith('/') or '__MACOSX' in name:
                continue
            out = into / name
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(z.read(info))


def natural(s):
    return [int(t) if t.isdigit() else t for t in re.split(r'(\d+)', s)]


def spread(items, n):
    """At most n items, evenly spread (velocity layers, notes across the range)."""
    if n is None or len(items) <= n:
        return items
    return [items[round(i * (len(items) - 1) / (n - 1))] for i in range(n)] if n > 1 else items[:1]


def clean(audio, rate, max_seconds, one_shot):
    """Trimmed, cut, faded, normalized; None when unusable (silent, broken, too short)."""
    if audio.size == 0 or not np.all(np.isfinite(audio)):
        return None
    mono = audio.mean(axis=1)
    peak = np.abs(audio).max()
    if peak < 1e-3:
        return None
    env = np.abs(mono) / max(np.abs(mono).max(), 1e-9)
    loud = np.nonzero(env > 0.05)[0]
    if len(loud) == 0:
        return None
    start = max(0, loud[0] - int(0.002 * rate)) if one_shot else 0
    quiet = np.nonzero(env > 0.003)[0]
    natural_end = min(len(mono), quiet[-1] + int(0.02 * rate))
    end = min(natural_end, start + int(max_seconds * rate))
    out = audio[start:end].copy()
    if len(out) < int(0.03 * rate):
        return None
    fade_in = min(int(0.001 * rate), len(out))
    out[:fade_in] *= np.linspace(0, 1, fade_in)[:, None]
    # A sound cut short fades out over a quarter second, a whole one over 30 ms.
    fade = min(int((0.25 if end < natural_end else 0.03) * rate), len(out) // 3)
    if fade > 0:
        out[-fade:] *= np.linspace(1, 0, fade)[:, None]
    out *= 0.89 / np.abs(out).max()
    # Mono when both sides carry the same sound: half the size on the phone.
    if out.shape[1] == 2 and np.abs(out[:, 0] - out[:, 1]).max() < 0.02:
        out = out.mean(axis=1, keepdims=True)
    elif out.shape[1] > 2:
        out = out[:, :2]
    return out


def main():
    lib_path, pack_id, out_dir = sys.argv[1:4]
    cache = Path(sys.argv[sys.argv.index('--cache') + 1]) if '--cache' in sys.argv else Path('cache')
    only = set(sys.argv[sys.argv.index('--only') + 1:]) if '--only' in sys.argv else None
    lib = json.loads(Path(lib_path).read_text())
    sources = {s['id']: s for s in lib['sources']}
    pack = next(p for p in lib['packs'] if p['id'] == pack_id)
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)

    used = [sources[s] for s in pack['sources'] if only is None or s in only]
    files = []
    for s in used:
        root = fetch(s, cache)
        base = root / 'repo' if s['type'] == 'git' else root
        for f in sorted(base.rglob('*'), key=lambda p: natural(str(p))):
            if f.is_file() and f.suffix.lower() in AUDIO and '.git' not in f.parts:
                files.append((f"{s['id']}/{f.relative_to(base).as_posix()}".lower(), f, s['id']))

    exclude = [re.compile(x) for x in pack.get('exclude', [])] + [re.compile(r'preview')]
    picked = {i: [] for i in range(len(pack['rules']))}
    rules = [re.compile(r['match']) for r in pack['rules']]
    for key, f, sid in files:
        if any(x.search(key) for x in exclude):
            continue
        for i, rx in enumerate(rules):
            if rx.search(key):
                picked[i].append((key, f, sid))
                break

    sounds, credited = [], set()
    for i, rule in enumerate(pack['rules']):
        kind = rule['kind']
        if kind not in KINDS:
            raise ValueError(f'{pack_id}: unknown kind {kind}')
        max_seconds = rule.get('max_seconds', pack.get('max_seconds', 3.0))
        one_shot = kind not in ('loop', 'pad') and not rule.get('ambience')
        n = 0
        for key, f, sid in spread(picked[i], rule.get('max', pack.get('max_per_rule', 12))):
            try:
                audio, rate = sf.read(f, always_2d=True, dtype='float64')
            except Exception as e:
                print(f'{pack_id}: {key} unreadable ({e})', flush=True)
                continue
            take = clean(audio, rate, max_seconds, one_shot)
            if take is None:
                print(f'{pack_id}: {key} left out (silent or too short)', flush=True)
                continue
            n += 1
            name = f"{kind}_{rule['slug']}_{n:02d}.flac"
            sf.write(out / name, take.astype(np.float32), rate, subtype='PCM_16')
            sounds.append({'file': name, 'kind': kind, 'name': f"{rule['label']} {n}", 'tags': rule.get('tags', [])})
            credited.add(sid)
        print(f"{pack_id}: {rule['slug']} {n} sounds", flush=True)

    credits = [{'title': sources[s]['title'], 'author': sources[s]['author'], 'licence': sources[s]['licence'],
                'page': sources[s]['page']} for s in pack['sources'] if s in credited]
    (out / 'pack.json').write_text(json.dumps({
        'id': pack['id'], 'name': pack['name'], 'family': pack['family'], 'bpm': 120, 'version': pack['version'],
        'sounds': sounds, 'credits': credits,
    }, ensure_ascii=False, indent=1))
    total = sum((out / s['file']).stat().st_size for s in sounds)
    print(f'{pack_id}: {len(sounds)} sounds, {total / 1e6:.1f} MB', flush=True)


if __name__ == '__main__':
    main()
