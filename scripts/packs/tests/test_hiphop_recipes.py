"""The hip-hop recipes (grand remplacement v2, lots L2 and L8): each style's modern body, read from
scripts/packs/recipes/hiphop.json. Trap: room to breathe around the 808; boom bap: body in the mids."""
import json
import unittest

from helpers import PACKS_DIR

RECIPES = PACKS_DIR / 'recipes' / 'hiphop.json'


def styles():
    return {s['id']: s for s in json.loads(RECIPES.read_text())}


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


if __name__ == '__main__':
    unittest.main()
