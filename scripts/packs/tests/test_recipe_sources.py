"""The library sounds a recipe may name (lib:<pack>:<sound>, format of lot L1) are listed in the
catalogues and carried by the published index: VCSL percussions, finger snaps, VSCO-2 flute."""
import json
import tempfile
import unittest
from pathlib import Path

from helpers import PACKS_DIR, fake_pack

import build_index

CC0 = {'title': 't', 'author': 'a', 'licence': 'CC0-1.0', 'page': 'https://example.org'}
WANTED = {
    'percussions_acoustiques': {'shaker', 'tambourin', 'cabasa', 'conga', 'bongo', 'claves'},
    'bruitages_maison': {'doigts'},
}


class RecipeSources(unittest.TestCase):
    def setUp(self):
        self.sources = build_index.recipe_sources(PACKS_DIR / 'libraries.json', PACKS_DIR / 'instruments.json')

    def test_the_three_sources_are_open_to_recipes(self):
        for pack, slugs in WANTED.items():
            self.assertIn(pack, self.sources)
            self.assertTrue(slugs <= set(self.sources[pack]), f'{pack}: {sorted(slugs - set(self.sources[pack]))}')
        self.assertIn('instrument_bois', self.sources)

    def test_snaps_come_from_opengameart_snaps(self):
        lib = json.loads((PACKS_DIR / 'libraries.json').read_text())
        pack = next(p for p in lib['packs'] if p['id'] == 'bruitages_maison')
        rule = next(r for r in pack['rules'] if r['slug'] == 'doigts')
        self.assertIn('oga-snaps', rule['match'])
        self.assertEqual('oga-snaps', pack['sources'][0])  # first: its files are met first

    def test_flute_is_the_vsco2_flute(self):
        inst = json.loads((PACKS_DIR / 'instruments.json').read_text())
        bois = next(i for i in inst['instruments'] if i['id'] == 'instrument_bois')
        self.assertEqual('vsco2', bois['source'])
        flute = next(p for p in bois['parts'] if p['sfz'] == 'FluteSusVib.sfz')
        self.assertEqual(flute['keys'], bois['recipes']['flute'])

    def test_only_whitelisted_packs_are_open_to_recipes(self):
        lib = json.loads((PACKS_DIR / 'libraries.json').read_text())
        licences = {s['id']: s['licence'] for s in lib['sources']}
        for p in lib['packs']:
            if p.get('recipes'):
                for s in p['sources']:
                    self.assertTrue(build_index.licence_ok(licences[s]), f"{p['id']}: {s}")

    def test_index_lists_the_recipe_sounds_actually_built(self):
        with tempfile.TemporaryDirectory() as t:
            packs = Path(t) / 'packs'
            fake_pack(packs, 'percussions_acoustiques', ['perc_shaker_01.flac', 'perc_shaker_02.flac', 'perc_conga_01.flac',
                                                         'kick_grosse_caisse_01.flac', 'open_hat_ride_01.flac'], credits=[CC0])
            fake_pack(packs, 'bruitages_maison', ['clap_doigts_01.flac', 'perc_pingpong_01.flac'], credits=[CC0])
            fake_pack(packs, 'instrument_bois', ['apercu.flac'], credits=[CC0], instrument={'zones': []})
            fake_pack(packs, 'sons_jeux', ['fx_piece_01.flac'], credits=[CC0])
            build_index.build(packs, Path(t) / 'dist')
            entries = {e['id']: e for e in json.loads((Path(t) / 'dist' / 'index.json').read_text())['packs']}
        self.assertEqual(['conga', 'grosse_caisse', 'ride', 'shaker'], entries['percussions_acoustiques']['recipeSounds'])
        self.assertEqual(['doigts', 'pingpong'], entries['bruitages_maison']['recipeSounds'])
        self.assertEqual({'flute': [60, 100]}, entries['instrument_bois']['recipeParts'])
        self.assertNotIn('recipeSounds', entries['sons_jeux'])


if __name__ == '__main__':
    unittest.main()
