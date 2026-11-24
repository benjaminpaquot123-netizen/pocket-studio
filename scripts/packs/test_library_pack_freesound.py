"""The « freesound » source of library_pack.py, without the network (a fake downloader).

run: python3 -m unittest scripts/packs/test_library_pack_freesound.py
"""
import hashlib
import io
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import numpy as np
import soundfile as sf

sys.path.insert(0, str(Path(__file__).resolve().parent))
import library_pack  # noqa: E402


def hit(seed):
    """A short percussive sound, as WAV bytes."""
    rate = 48000
    t = np.arange(int(0.3 * rate)) / rate
    x = np.sin(2 * np.pi * (300 + 50 * seed) * t) * np.exp(-t * 20)
    buf = io.BytesIO()
    sf.write(buf, x, rate, format='WAV')
    return buf.getvalue()


def ogg(seed):
    rate = 44100
    t = np.arange(int(0.3 * rate)) / rate
    x = np.sin(2 * np.pi * (500 + 50 * seed) * t) * np.exp(-t * 20)
    buf = io.BytesIO()
    sf.write(buf, x, rate, format='OGG')
    return buf.getvalue()


ORIG = {1: hit(1), 2: hit(2), 3: hit(3)}
LIST = [
    {'id': 1, 'name': 'Finger snap 1', 'author': 'alice', 'url': 'https://freesound.org/people/alice/sounds/1/',
     'licence': 'CC0 1.0', 'sha256': hashlib.sha256(ORIG[1]).hexdigest(), 'role': 'snap'},
    {'id': 2, 'name': 'Conga hit', 'author': 'bob', 'url': 'https://freesound.org/people/bob/sounds/2/',
     'licence': 'CC BY 4.0', 'sha256': hashlib.sha256(ORIG[2]).hexdigest(), 'role': 'hand_perc'},
    {'id': 2, 'name': 'Conga hit', 'author': 'bob', 'url': 'https://freesound.org/people/bob/sounds/2/',
     'licence': 'CC BY 4.0', 'sha256': hashlib.sha256(ORIG[2]).hexdigest(), 'role': 'hand_perc'},
    {'id': 3, 'name': 'Rim', 'author': 'carol', 'url': 'https://freesound.org/people/carol/sounds/3/',
     'licence': 'CC0 1.0', 'sha256': 'f' * 64, 'role': 'rim'},
    {'id': None, 'name': 'logdrum_syn_a', 'author': 'Pocket Studio (synthesized)', 'url': None,
     'licence': 'none (own synthesis)', 'sha256': None, 'role': 'log_drum', 'file': 'logdrum_syn_a.flac'},
]


class FakeNet:
    """Answers the Freesound API like the real one, and records what was asked (and with which header)."""

    def __init__(self):
        self.calls = []

    def __call__(self, url, headers):
        self.calls.append((url, dict(headers)))
        auth = headers.get('Authorization', '')
        if '/download/' in url:
            assert auth == 'Bearer oauth-secret', auth
            return ORIG[int(url.rstrip('/').split('/')[-2])]
        if url.startswith('https://cdn.example/'):
            return ogg(int(url.rsplit('/', 1)[1].split('.')[0]))
        assert auth == 'Token api-secret', auth
        sid = int(url.rstrip('/').rsplit('/', 1)[1].split('?')[0])
        return json.dumps({'id': sid, 'previews': {'preview-hq-ogg': f'https://cdn.example/{sid}.ogg'}}).encode()


class FreesoundSourceTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        (self.tmp / 'freesound').mkdir()
        (self.tmp / 'freesound' / 'mix.json').write_text(json.dumps(LIST))
        self.lib = self.tmp / 'libraries.json'
        self.lib.write_text(json.dumps({
            'sources': [{'id': 'fs-mix', 'type': 'freesound', 'list': 'freesound/mix.json', 'roles': ['snap', 'hand_perc', 'rim', 'log_drum'],
                         'title': 'Freesound', 'author': 'several', 'licence': 'CC0-1.0', 'page': 'https://freesound.org'}],
            'packs': [{'id': 'grand', 'name': 'Grand', 'family': 'drums', 'version': 1, 'sources': ['fs-mix'],
                       'rules': [{'match': r'^fs-mix/snap/', 'kind': 'snap', 'slug': 'doigts', 'label': 'Claquement de doigts', 'tags': ['doigts']},
                                 {'match': r'^fs-mix/', 'kind': 'perc', 'slug': 'perc', 'label': 'Percussion'}]}],
        }))

    def build(self, env):
        out = self.tmp / 'out'
        net = FakeNet()
        argv = ['library_pack.py', str(self.lib), 'grand', str(out), '--cache', str(self.tmp / 'cache')]
        with mock.patch.dict(os.environ, env, clear=False), mock.patch.object(sys, 'argv', argv), \
                mock.patch.object(library_pack, 'http_get', net):
            for k in ('FREESOUND_API_KEY', 'FREESOUND_OAUTH_TOKEN'):
                if k not in env:
                    os.environ.pop(k, None)
            library_pack.main()
        pack = json.loads((out / 'pack.json').read_text()) if (out / 'pack.json').exists() else None
        return pack, net

    def test_originals_with_a_token_are_checked_and_credited_one_by_one(self):
        pack, net = self.build({'FREESOUND_OAUTH_TOKEN': 'oauth-secret', 'FREESOUND_API_KEY': 'api-secret'})
        # Sound 3's original does not match its fingerprint: left out. Sound 2 is listed twice: once.
        self.assertEqual([('snap', 'Claquement de doigts 1'), ('perc', 'Percussion 1')], [(s['kind'], s['name']) for s in pack['sounds']])
        self.assertEqual(['snap_doigts_01.flac', 'perc_perc_01.flac'], [s['file'] for s in pack['sounds']])
        self.assertEqual([
            {'title': 'Finger snap 1', 'author': 'alice', 'licence': 'CC0 1.0', 'page': 'https://freesound.org/people/alice/sounds/1/'},
            {'title': 'Conga hit', 'author': 'bob', 'licence': 'CC BY 4.0', 'page': 'https://freesound.org/people/bob/sounds/2/'},
        ], pack['credits'])
        self.assertTrue(all('/download/' in u for u, _ in net.calls))
        self.assertEqual(3, len(net.calls))  # 1, 2 (once), 3 — nothing for the synthesized one

    def test_previews_with_only_the_api_key(self):
        pack, net = self.build({'FREESOUND_API_KEY': 'api-secret'})
        # No original, so no fingerprint to check: every downloadable sound comes, in its HQ preview.
        self.assertEqual(['snap_doigts_01.flac', 'perc_perc_01.flac', 'perc_perc_02.flac'], [s['file'] for s in pack['sounds']])
        self.assertEqual(['alice', 'bob', 'carol'], [c['author'] for c in pack['credits']])
        self.assertFalse(any('/download/' in u for u, _ in net.calls))

    def test_without_a_key_the_source_is_skipped_and_no_pack_is_written(self):
        pack, net = self.build({})
        self.assertIsNone(pack)
        self.assertEqual([], net.calls)

    def test_the_secrets_are_never_printed(self):
        err, out = io.StringIO(), io.StringIO()
        with mock.patch.object(sys, 'stdout', out), mock.patch.object(sys, 'stderr', err):
            self.build({'FREESOUND_OAUTH_TOKEN': 'oauth-secret', 'FREESOUND_API_KEY': 'api-secret'})
        self.assertNotIn('secret', out.getvalue() + err.getvalue())

    def test_snap_is_a_kind_the_app_knows(self):
        self.assertIn('snap', library_pack.KINDS)


if __name__ == '__main__':
    unittest.main()
