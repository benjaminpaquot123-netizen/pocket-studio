"""The hip-hop recipes (grand remplacement v2, lots L2 and L8): each style's modern body, read from
scripts/packs/recipes/hiphop.json. Trap: room to breathe around the 808; boom bap: body in the mids."""
import json
import unittest

from helpers import PACKS_DIR

RECIPES = PACKS_DIR / 'recipes' / 'hiphop.json'


def styles():
    return {s['id']: s for s in json.loads(RECIPES.read_text())}


def wood():
    inst = json.loads((PACKS_DIR / 'instruments.json').read_text())
    return next(i for i in inst['instruments'] if i['id'] == 'instrument_bois')


NAMES = {'C': 0, 'C#': 1, 'D': 2, 'D#': 3, 'E': 4, 'F': 5, 'F#': 6, 'G': 7, 'G#': 8, 'A': 9, 'A#': 10, 'B': 11}
SCALES = {'minor': [0, 2, 3, 5, 7, 8, 10], 'major': [0, 2, 4, 5, 7, 9, 11]}


def lead_notes(style):
    """The MIDI notes of a riff lead in every mood's scale: 'R' root, '2'..'7' degrees, 'O' octave."""
    lead = style['lead']
    base = NAMES[style['root']] + 12 * lead['octave']
    out = []
    for prog in style['progressions'].values():
        ints = SCALES[prog['scale']]
        for c in lead['pattern'] + lead.get('answer', ''):
            low = c.lower()
            if low == 'r':
                out.append(base)
            elif low == 'o':
                out.append(base + 12)
            elif c in '234567':
                out.append(base + ints[int(c) - 1])
    return out


def lines(style):
    return {d['role']: d for d in style['drums']}


def hits_per_step(style):
    """How many drum lines hit on each sixteenth, the lines looped over 32 steps."""
    pats = [d['pattern'] * (32 // len(d['pattern'])) for d in style['drums']]
    return [sum(p[i] != '.' for p in pats) for i in range(32)]


def sounds(style):
    """Every sound a drum line plays: its source and its layers."""
    out = set()
    for d in style['drums']:
        out.add(d.get('source', style['id']))
        out.update(d.get('layers', []))
    return out


class Trap(unittest.TestCase):
    def setUp(self):
        self.s = styles()['trap']

    def test_six_rhythm_lines_but_room_to_breathe(self):
        roles = set(lines(self.s))
        self.assertTrue({'kick', 'clap', 'snare', 'hat', 'open_hat'} <= roles, sorted(roles))
        self.assertTrue(roles & {'snap', 'perc', 'perc2'}, sorted(roles))
        self.assertLessEqual(max(hits_per_step(self.s)), 5)

    def test_clap_snare_and_snap_stacked_on_three(self):
        ls = lines(self.s)
        for role in ('clap', 'snare', 'snap'):
            self.assertEqual('X', ls[role]['pattern'][8], role)

    def test_hats_roll_with_dynamics(self):
        hat = lines(self.s)['hat']['pattern']
        self.assertTrue(set(hat) & set('Tt'), 'a triplet')
        self.assertTrue(set(hat) & set('234'), 'a 1/32 roll')
        self.assertTrue('X' in hat and set(hat) & set('xo'), 'accents and softer hits')

    def test_space_no_pad_counter_melody_and_the_808_pumps(self):
        self.assertNotEqual('Nappe', (self.s.get('counter') or {}).get('sound'))
        self.assertIn('808', self.s.get('mix', {}).get('pump', []))
        self.assertTrue(self.s['bass']['glide'])


class BoomBap(unittest.TestCase):
    def setUp(self):
        self.s = styles()['boom_bap']

    def test_snare_clap_and_snap_on_the_backbeat(self):
        ls = lines(self.s)
        self.assertIn('snare', ls)
        self.assertTrue('clap' in ls or any(x.startswith('kit:clap') for x in ls['snare'].get('layers', [])))
        self.assertIn('snap', ls)
        self.assertEqual('X', ls['snap']['pattern'][4])

    def test_hand_percussion_and_body_in_the_mids(self):
        ls = lines(self.s)
        self.assertTrue({'shaker', 'perc2', 'perc'} & set(ls))
        self.assertIn(self.s['chords'].get('double'), ('octave', 'both'))
        self.assertTrue(any(fx.get('insert') == 'VINYL' for fx in self.s['fx']), 'a vinyl texture')

    def test_drums_compressed_in_parallel_with_mpc_swing(self):
        mix = self.s.get('mix', {})
        self.assertLess(mix.get('glue_blend', 1), 1)
        self.assertGreater(mix.get('bus_drive', 0), 0)
        self.assertGreaterEqual(self.s['swing'], 0.2)


class FullHipHop(unittest.TestCase):
    """Lot L8: every other hip-hop style gets the full body and keeps (or finds) its modern signature."""
    STYLES = ('drill', 'lofi', 'phonk', 'grime', 'reggaeton', 'afrobeats', 'jersey_club')

    def setUp(self):
        self.all = styles()

    def test_every_style_has_a_body_and_breathes(self):
        for sid in self.STYLES:
            s = self.all[sid]
            self.assertTrue(s.get('body'), sid)
            self.assertLessEqual(max(hits_per_step(s)), 5, sid)

    def test_drill_sliding_808_and_triplet_hats(self):
        s = self.all['drill']
        self.assertTrue(s['bass']['glide'])
        self.assertGreaterEqual(s['bass']['pattern'].count('/'), 3)
        self.assertTrue(set(lines(s)['hat']['pattern']) & set('Tt'))
        self.assertTrue(any(x == 'kit:clap' for x in lines(s)['snare'].get('layers', [])) or 'clap' in lines(s))

    def test_lofi_dusty_keys_and_rain(self):
        s = self.all['lofi']
        self.assertTrue(any(fx.get('insert') == 'VINYL' for fx in s['fx']))
        self.assertIn(s['chords'].get('double'), ('octave', 'both'))
        self.assertEqual('Pluie', s.get('texture', {}).get('sound'))
        self.assertIn('snap', lines(s))

    def test_phonk_cowbell_and_a_driven_808(self):
        s = self.all['phonk']
        self.assertEqual('kit:cowbell', s['lead']['sound'])
        self.assertIn('808', s.get('mix', {}).get('pump', []))
        self.assertGreater(s.get('mix', {}).get('bus_drive', 0), 0)
        self.assertNotEqual('Nappe', s['chords']['sound'])

    def test_grime_square_stabs_and_a_hard_backbeat(self):
        s = self.all['grime']
        self.assertEqual('Lead carré', s['chords']['sound'])
        self.assertIn('snare', lines(s))
        self.assertTrue(set(lines(s)['hat']['pattern']) & set('X234'))

    def test_reggaeton_dembow_with_hand_percussion(self):
        s = self.all['reggaeton']
        self.assertEqual('...X..X....X..X.', lines(s)['snare']['pattern'])
        self.assertTrue({'conga', 'rim'} & set(lines(s)))
        self.assertIn('808', s.get('mix', {}).get('pump', []))

    def test_afrobeats_shakers_congas_and_air(self):
        s = self.all['afrobeats']
        ls = lines(s)
        self.assertEqual('shaker', ls['hat'].get('voice'))
        self.assertTrue({'conga', 'perc'} & set(ls))
        self.assertTrue({'open_hat', 'shaker', 'perc2'} & set(ls), 'air above the shaker')

    def test_jersey_club_bouncing_kick_and_snaps(self):
        s = self.all['jersey_club']
        self.assertEqual('X...X...X..X..X.', lines(s)['kick']['pattern'])
        self.assertIn('snap', lines(s))
        self.assertIn('808', s.get('mix', {}).get('pump', []))


class TrapFlute(unittest.TestCase):
    """Lot L6: the VSCO-2 flute (sustain vibrato) lets go fast, as on a trap flute loop."""

    def test_wood_release_is_short(self):
        inst = json.loads((PACKS_DIR / 'instruments.json').read_text())
        bois = next(i for i in inst['instruments'] if i['id'] == 'instrument_bois')
        self.assertTrue(0.15 <= bois['release'] <= 0.25, bois['release'])
        flute = next(p for p in bois['parts'] if p['sfz'] == 'FluteSusVib.sfz')
        self.assertTrue(flute.get('loop'), 'the held note keeps its vibrato')

    def test_trap_lead_is_the_flute_dark_and_in_a_plate(self):
        t = styles()['trap']
        self.assertTrue(t['lead']['sound'].startswith('lib:instrument_bois:flute'), t['lead']['sound'])
        self.assertTrue(t['lead']['sound'].endswith('|Cloche FM'), 'the old bell when the flute is not installed')
        fx = [f for f in t['fx'] if f['on'] == 'lead']
        lp = next(f for f in fx if f.get('insert') == 'FILTER')
        # cutoff 40 Hz * 450^x: 6 to 8 kHz, low-pass (type 0)
        self.assertTrue(6000 <= 40 * 450 ** lp['params'][0] <= 8000, lp)
        self.assertLess(lp['params'][2], 1 / 3)
        self.assertTrue(any(f.get('insert') == 'REVERB' or f.get('reverb', 0) >= 0.25 for f in fx), 'a plate')

    def test_every_note_of_the_trap_phrase_is_played_by_the_flute(self):
        # Before: the flute began at G4 (67), so the phrase's F4 fell on the clarinet.
        bois = wood()
        flute = next(p for p in bois['parts'] if p['sfz'] == 'FluteSusVib.sfz')
        lo, hi = flute['keys']
        t = styles()['trap']
        notes = lead_notes(t)
        self.assertTrue(notes)
        self.assertTrue(all(lo <= n <= hi for n in notes), (lo, hi, sorted(set(notes))))
        self.assertEqual([lo, hi], bois['recipes']['flute'])
        clar = next(p for p in bois['parts'] if p['sfz'] == 'ClarinetSus.sfz')
        self.assertEqual(lo - 1, clar['keys'][1], 'no gap and no overlap between clarinet and flute')

    def test_the_held_flute_does_not_loop_its_vibrato_every_three_seconds(self):
        bois = wood()
        flute = next(p for p in bois['parts'] if p['sfz'] == 'FluteSusVib.sfz')
        curve = flute.get('seconds', bois['seconds'])
        self.assertTrue(all(sec >= 4.0 for _, sec in curve), curve)
        self.assertGreaterEqual(bois['version'], 2, 'the phone offers the new flute as an update')

    def test_the_trap_phrase_is_a_new_legato_one(self):
        pat = styles()['trap']['lead']['pattern']
        self.assertNotEqual('O-----5-3-----..R-----3-5---....', pat)
        self.assertEqual(32, len(pat))
        # Legato: every note is held at least one more step; and it breathes (rests).
        for i, c in enumerate(pat):
            if c not in '-./':
                self.assertEqual('-', pat[(i + 1) % len(pat)], (i, pat))
        self.assertGreaterEqual(pat.count('.'), 2, pat)


class TrapVoices(unittest.TestCase):
    """Lot L7: real ad-lib voices (CC0 on Freesound, each page read), open to recipes."""

    def setUp(self):
        self.lib = json.loads((PACKS_DIR / 'libraries.json').read_text())
        self.pack = next((p for p in self.lib['packs'] if p['id'] == 'voix_adlib'), None)

    def test_adlib_pack_open_to_recipes(self):
        self.assertIsNotNone(self.pack)
        self.assertTrue(self.pack.get('recipes'))
        slugs = {r['slug'] for r in self.pack['rules']}
        self.assertTrue({'yeah', 'skrrt', 'brrr'} <= slugs, sorted(slugs))
        self.assertTrue(all(r['kind'] == 'vocal' for r in self.pack['rules']))

    def test_every_voice_is_a_cc0_freesound_preview(self):
        sources = {s['id']: s for s in self.lib['sources']}
        for sid in self.pack['sources']:
            src = sources[sid]
            self.assertEqual('CC0-1.0', src['licence'], sid)
            self.assertTrue(src['page'].startswith('https://freesound.org/people/'), sid)
            for url in src['urls']:
                self.assertRegex(url, r'^https://cdn\.freesound\.org/previews/\d+/\d+_\d+-hq\.ogg$')

    def test_adlibs_sung_in_the_hiphop_styles(self):
        slugs = {r['slug'] for r in self.pack['rules']}
        for sid in ('trap', 'drill', 'phonk', 'jersey_club'):
            vocal = styles()[sid].get('vocal')
            self.assertIsInstance(vocal, dict, sid)
            pack, _, slug = vocal['sample'].partition(':')
            self.assertEqual('voix_adlib', pack, sid)
            self.assertIn(slug, slugs, sid)


if __name__ == '__main__':
    unittest.main()
