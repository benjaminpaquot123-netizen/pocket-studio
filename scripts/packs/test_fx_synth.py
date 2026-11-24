"""python3 -m unittest scripts/packs/test_fx_synth.py"""
import os, sys, unittest
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import fx_synth as F


def env(y, hop=4800):
    m = y.mean(1)
    return np.array([np.sqrt((m[i:i + hop] ** 2).mean()) for i in range(0, len(m) - hop, hop)])


class FxSynthTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.m = F.make()

    def test_all_finite_and_peak_minus_one_db(self):
        for k, y in self.m.items():
            self.assertTrue(np.isfinite(y).all(), k)
            self.assertAlmostEqual(float(np.abs(y).max()), 0.89, places=3, msg=k)

    def test_lengths_within_role_max(self):
        mx = {'riser_tonal': 8.0, 'sub_drop': 3.0, 'tape_stop': 2.5}
        for k, y in self.m.items():
            self.assertLessEqual(len(y) / F.SR, mx[k.rsplit('_', 1)[0]], k)

    def test_riser_is_stereo_rises_and_ends_on_the_bar(self):
        y = self.m['riser_tonal_synth1']
        self.assertEqual(y.shape[1], 2)
        self.assertGreater(np.abs(y[:, 0] - y[:, 1]).max(), 0.05)
        e = env(y)
        self.assertGreater(e[-3:].mean(), 10 * e[:3].mean())
        self.assertAlmostEqual(len(y) / F.SR, 2 * F.BAR, places=2)

    def test_sub_drop_falls_in_pitch_and_decays(self):
        for k in ('sub_drop_synth1', 'sub_drop_synth2'):
            y = self.m[k][:, 0]
            zc = lambda a: np.count_nonzero(np.diff(np.sign(a)) != 0) / (len(a) / F.SR) / 2
            q = len(y) // 4
            self.assertGreater(zc(y[:q]), 1.5 * zc(y[-q:]), k)
            self.assertLess(zc(y[-q:]), 45, k)
            e = env(self.m[k])
            self.assertGreater(e[1], 10 * e[-1], k)

    def test_tape_stop_slows_down_to_silence(self):
        y = self.m['tape_stop_synth1']
        self.assertEqual(y.shape[1], 1)
        e = env(y)
        self.assertLess(e[-1], 0.1 * e[2])


if __name__ == '__main__':
    unittest.main()
