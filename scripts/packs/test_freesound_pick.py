"""freesound_pick.py on synthetic sounds and toy metadata (no network, no ffmpeg, seconds).

    python3 -I scripts/packs/test_freesound_pick.py
"""
import os
import sys
import unittest

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import freesound_pick as fp  # noqa: E402

R = fp.RATE


def hat(length_s, bright=True, seed=1, delay_s=0.0, noise_floor=0.0, rumble=0.0):
    rng = np.random.default_rng(seed)
    n = int(delay_s * R) + int(length_s * 3 * R)
    x = rng.standard_normal(n) * noise_floor
    t = np.arange(int(length_s * 3 * R)) / R
    body = rng.standard_normal(len(t)) * np.exp(-t / (length_s / 4.6))
    if bright:
        body = np.diff(body, prepend=0)  # tilt up: most energy high
    body += rumble * np.sin(2 * np.pi * 80 * t) * np.exp(-t / (length_s / 4.6))
    d = int(delay_s * R)
    x[d:d + len(body)] += body / np.abs(body).max() * 0.8
    return x[:, None]


class Words(unittest.TestCase):
    def test_refuses_records_songs_breaks_and_loops(self):
        for text in ("hat sampled from an old vinyl", "taken from my song", "Amen break hat",
                     "from a Sample CD", "hihat loop 120 bpm", "hat from the album X", "Splice hat"):
            self.assertIsNotNone(fp.refused_text(text, "", []), text)

    def test_accepts_plain_recordings_and_machines(self):
        for text in ("Closed hi-hat, recorded with a Zoom H4n", "909 style open hat", "Ride cymbal bell hit"):
            self.assertIsNone(fp.refused_text(text, "", ["hihat", "drum"]), text)

    def test_licences(self):
        self.assertEqual(fp.licence_ok("http://creativecommons.org/publicdomain/zero/1.0/", False), "CC0 1.0")
        self.assertEqual(fp.licence_ok("https://creativecommons.org/licenses/by/4.0/", True), "CC BY 4.0")
        self.assertIsNone(fp.licence_ok("https://creativecommons.org/licenses/by/4.0/", False))
        self.assertIsNone(fp.licence_ok("http://creativecommons.org/licenses/by/3.0/", True))
        self.assertIsNone(fp.licence_ok("http://creativecommons.org/licenses/by-nc/4.0/", True))


class Measures(unittest.TestCase):
    def test_short_bright_hat_measures_short_and_bright(self):
        f = fp.features(hat(0.08))
        self.assertLess(f["length"], 0.16)
        self.assertGreater(f["centroid"], 6000)
        self.assertEqual(f["hits"], 1)
        self.assertLess(f["start"], 0.005)

    def test_late_start_noise_floor_and_double_hit_are_seen(self):
        self.assertGreater(fp.features(hat(0.08, delay_s=0.1))["start"], 0.09)
        self.assertGreater(fp.features(hat(0.08, delay_s=0.1, noise_floor=0.01))["pre_db"], -50)
        two = np.concatenate([hat(0.05), hat(0.05, seed=2)])
        self.assertEqual(fp.features(two)["hits"], 2)

    def test_scores_prefer_the_modern_closed_hat(self):
        good = fp.score("closed_hat", fp.features(hat(0.08)))
        for bad in (hat(0.08, delay_s=0.1, noise_floor=0.01), hat(0.6), hat(0.08, bright=False, rumble=2.0),
                    np.concatenate([hat(0.05), hat(0.05, seed=2)])):
            self.assertGreater(good, fp.score("closed_hat", fp.features(bad)))

    def test_open_hat_prefers_its_own_length(self):
        self.assertGreater(fp.score("open_hat", fp.features(hat(0.5))), fp.score("open_hat", fp.features(hat(0.06))))

    def test_silence_is_no_sound(self):
        self.assertIsNone(fp.features(np.zeros((R, 1))))


class Choice(unittest.TestCase):
    def test_pick_caps_one_author_and_keeps_the_best(self):
        c = [{"author": "a", "score": 10 - i, "id": i, "licence": "CC0 1.0"} for i in range(5)]
        c += [{"author": "b", "score": 1, "id": 9, "licence": "CC0 1.0"}]
        got = fp.pick(c, 4, per_author=3)
        self.assertEqual([x["id"] for x in got], [0, 1, 2, 9])
        self.assertEqual([x["id"] for x in fp.pick(c, 2, exclude={"0": "why"})], [1, 2])

    def test_cc_by_only_when_clearly_better(self):
        c = [{"author": "a", "score": 10.5, "id": 1, "licence": "CC BY 4.0"},
             {"author": "b", "score": 10.0, "id": 2, "licence": "CC0 1.0"},
             {"author": "c", "score": 12.0, "id": 3, "licence": "CC BY 4.0"}]
        self.assertEqual([x["id"] for x in fp.pick(c, 3)], [3, 2, 1])

    def test_wide_phasey_sound_loses(self):
        f = fp.features(hat(0.08))
        self.assertGreater(fp.score("closed_hat", f), fp.score("closed_hat", dict(f, width=1.8)))

    def test_process_trims_fades_and_peaks_at_minus_one_db(self):
        y = fp.process(np.concatenate([np.zeros((4800, 2)), np.repeat(hat(0.5), 2, axis=1)]), R, 0.3)
        self.assertEqual(y.shape[1], 1)  # same on both sides: mono
        self.assertLessEqual(len(y), int(0.3 * R) + 1)
        self.assertAlmostEqual(float(np.abs(y).max()), 0.89, places=2)
        self.assertLess(abs(y[0, 0]), 0.05)
        self.assertLess(abs(y[-1, 0]), 1e-3)


if __name__ == "__main__":
    unittest.main()
