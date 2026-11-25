"""Shared helpers of the pack tests: the packs folder on the import path, fake built packs."""
import json
import sys
from pathlib import Path

PACKS_DIR = Path(__file__).resolve().parents[1]
REPO = PACKS_DIR.parents[1]
if str(PACKS_DIR) not in sys.path:
    sys.path.insert(0, str(PACKS_DIR))


def fake_pack(root, pack_id, sounds, credits=None, instrument=None):
    """A built pack folder (pack.json + tiny placeholder files, not audio) as the build jobs leave it."""
    d = Path(root) / pack_id
    d.mkdir(parents=True)
    for s in sounds:
        (d / s).write_bytes(b'not audio, placeholder')
    manifest = {'id': pack_id, 'name': pack_id, 'family': 'Test', 'version': 1,
                'sounds': [{'file': s, 'kind': 'open_hat' if s.startswith('open_hat_') else s.split('_', 1)[0], 'name': s, 'tags': []} for s in sounds]}
    if credits is not None:
        manifest['credits'] = credits
    if instrument is not None:
        manifest['instrument'] = instrument
    (d / 'pack.json').write_text(json.dumps(manifest))
    return d
