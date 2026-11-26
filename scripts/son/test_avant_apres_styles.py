#!/usr/bin/env python3
"""Tests of scripts/son/avant_apres_styles.py on a small fake case (no engine, no Gradle).

Run: python3 -I scripts/son/test_avant_apres_styles.py
Proves: an MP3 is two halves of the asked length around the silence, neither half muted nor clipped,
the passage is the loud part (not the intro), and a lot whose after equals its before is reported
« identique » with no file written.
"""
import json
import os
import subprocess
import sys
import tempfile
import unittest

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
HELPER = os.path.join(HERE, "avant_apres_styles.py")
RATE = 48000


def write_wav(path, x):
    subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-f", "f32le", "-ar", str(RATE), "-ac", "2", "-i", "-",
                    "-c:a", "pcm_f32le", path], input=x.astype("<f4").tobytes(), check=True)


def fake_song(seconds, loud_from, loud_to, freq, quiet=0.03, loud=0.4):
    """A sine, quiet (the intro) except a loud part [loud_from, loud_to)."""
    t = np.arange(int(seconds * RATE)) / RATE
    amp = np.where((t >= loud_from) & (t < loud_to), loud, quiet)
    s = (amp * np.sin(2 * np.pi * freq * t)).astype(np.float32)
    return np.stack([s, s], axis=1)


def write_csv(path, seconds, swaps_at, loud_from, loud_to):
    """ps_live's CSV, the columns the helper reads: playback from second 1, a part swap at each SWAPS_AT."""
    with open(path, "w", encoding="utf-8") as f:
        f.write("second,song_step,swaps,running,rms_db\n")
        swaps = 0
        for s in range(seconds):
            swaps += s in swaps_at
            running = int(s >= 1)
            level = -11.0 if loud_from <= s < loud_to else -33.0
            f.write(f"{s},0,{swaps},{running},{level}\n")


def run(*args):
    p = subprocess.run([sys.executable, "-I", HELPER, *args], capture_output=True, text=True)
    return p.returncode, p.stdout, p.stderr


class AvantApres(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        d = self.tmp.name
        self.before_wav, self.after_wav = os.path.join(d, "b.wav"), os.path.join(d, "a.wav")
        self.before_csv, self.after_csv = os.path.join(d, "b.csv"), os.path.join(d, "a.csv")
        # 90 s: intro until 20 s (quiet), the Drop from 20 s to 62 s (loud), then quiet again.
        write_wav(self.before_wav, fake_song(90, 20, 62, 220))
        write_wav(self.after_wav, fake_song(90, 20, 62, 330, loud=0.3))
        write_csv(self.before_csv, 90, {20, 62}, 20, 62)
        write_csv(self.after_csv, 90, {20, 62}, 20, 62)

    def tearDown(self):
        self.tmp.cleanup()

    def test_two_halves_around_the_silence(self):
        out = os.path.join(self.tmp.name, "lot_style.mp3")
        code, stdout, err = run("assemble", self.before_wav, self.before_csv, self.after_wav, self.after_csv, out)
        self.assertEqual(code, 0, err)
        res = json.loads(stdout)
        self.assertEqual(res["status"], "ok")
        # The Drop, not the intro: half a second before the swap at 20 s, the same passage after.
        self.assertAlmostEqual(res["start_before"], 19.5, places=2)
        self.assertAlmostEqual(res["start_after"], 19.5, places=2)
        self.assertTrue(os.path.exists(out))
        code, stdout, err = run("measure", out)
        m = json.loads(stdout)
        self.assertEqual(code, 0, m)
        self.assertTrue(m["ok"], m["fails"])
        self.assertAlmostEqual(m["duration_s"], 81.0, delta=0.1)
        self.assertLess(m["gap_rms_db"], -60.0)
        # Not normalised: each half keeps its own level (0.4 vs 0.3 sine, about 2.5 dB apart).
        self.assertGreater(m["rms_before_db"] - m["rms_after_db"], 1.5)
        self.assertGreater(m["rms_after_db"], -40.0)
        self.assertEqual(m["clipped_before"], 0)
        self.assertEqual(m["clipped_after"], 0)

    def test_identical_is_reported_and_not_written(self):
        out = os.path.join(self.tmp.name, "same.mp3")
        code, stdout, err = run("assemble", self.before_wav, self.before_csv, self.before_wav, self.before_csv, out)
        self.assertEqual(code, 0, err)
        self.assertEqual(json.loads(stdout)["status"], "identique")
        self.assertFalse(os.path.exists(out))

    def test_measure_catches_a_muted_or_clipped_half(self):
        d = self.tmp.name
        silent = np.zeros((int(81 * RATE), 2), dtype=np.float32)
        loud = fake_song(81, 0, 40, 220, loud=0.4)
        loud[int(40 * RATE):] = 0.0
        muted = os.path.join(d, "muted.wav")
        write_wav(muted, loud)  # after half is silent
        code, stdout, _ = run("measure", muted)
        self.assertEqual(code, 1)
        self.assertIn("moitié after muette", json.loads(stdout)["fails"])
        clip = fake_song(81, 0, 81, 110, loud=3.0)
        clip = np.clip(clip, -1.0, 1.0)
        clip[int(40 * RATE):int(41 * RATE)] = 0.0
        clipped = os.path.join(d, "clipped.wav")
        write_wav(clipped, clip)
        code, stdout, _ = run("measure", clipped)
        fails = json.loads(stdout)["fails"]
        self.assertEqual(code, 1)
        self.assertIn("moitié before écrêtée", fails)
        self.assertNotIn("pas de silence au milieu", fails)
        silent_path = os.path.join(d, "silent.wav")
        write_wav(silent_path, silent)
        code, stdout, _ = run("measure", silent_path)
        self.assertIn("moitié before muette", json.loads(stdout)["fails"])


if __name__ == "__main__":
    unittest.main()
