#!/usr/bin/env python3
"""Tests of scripts/bentest/empreinte.py (T1b): made-up sounds whose bands are known from their filter alone.

  python3 -I scripts/bentest/test_empreinte.py
"""
import json
import math
import os
import subprocess
import sys
import tempfile
import unittest
import wave

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import empreinte as E  # noqa: E402

RATE = 48000


def biquad_power(kind, f0, q=0.7071, db=0.0):
    """|H(f)|² of an RBJ biquad, as a function of f (Hz)."""
    w0 = 2 * math.pi * f0 / RATE
    alpha = math.sin(w0) / (2 * q)
    c = math.cos(w0)
    if kind == "hp":
        b, a = [(1 + c) / 2, -(1 + c), (1 + c) / 2], [1 + alpha, -2 * c, 1 - alpha]
    elif kind == "lp":
        b, a = [(1 - c) / 2, 1 - c, (1 - c) / 2], [1 + alpha, -2 * c, 1 - alpha]
    else:
        g = 10 ** (db / 40)
        b, a = [1 + alpha * g, -2 * c, 1 - alpha * g], [1 + alpha / g, -2 * c, 1 - alpha / g]

    def power(f):
        z = np.exp(-1j * 2 * np.pi * np.asarray(f, dtype=float) / RATE)
        h = (b[0] + b[1] * z + b[2] * z * z) / (a[0] + a[1] * z + a[2] * z * z)
        return np.abs(h) ** 2
    return power


def chain(*parts):
    return lambda f: np.prod([p(f) for p in parts], axis=0)


def pink(seconds, power=lambda f: 1.0, seed=1, rms=0.1):
    """Pink noise shaped in the frequency domain by [power] (the filter imposed), RMS [rms]."""
    n = int(seconds * RATE)
    rng = np.random.default_rng(seed)
    spec = np.fft.rfft(rng.standard_normal(n))
    f = np.fft.rfftfreq(n, 1 / RATE)
    f[0] = f[1]
    spec *= np.sqrt(np.asarray(power(f)) / f)
    x = np.fft.irfft(spec, n)
    return (x * rms / np.sqrt(np.mean(x * x))).astype(np.float64)


def expected_octaves(power):
    """What each octave of a 1/f sound through [power] reads, 0 dB = the power mean of 500 Hz … 4 kHz."""
    lin = []
    for nominal in E.OCTAVES:
        fc = 1000 * 2 ** (round(3 * math.log2(nominal / 1000)) / 3)
        f = fc * 2 ** np.linspace(-0.5, 0.5, 401)[:-1] * 2 ** (1 / 800)
        lin.append(float(np.mean(power(f))))
    ref = np.mean([v for o, v in zip(E.OCTAVES, lin) if 500 <= o <= 4000])
    return [10 * math.log10(v / ref) for v in lin]


def expected_curve(power):
    """The speaker's curve as the phone sends it: third-octave dB, 0 dB = the power mean of 500 Hz … 2 kHz."""
    lin = []
    for fc in E.CENTRES:
        f = fc * 2 ** np.linspace(-1 / 6, 1 / 6, 401)[:-1] * 2 ** (1 / 2400)
        lin.append(float(np.mean(power(f))))
    ref = np.mean([v for c, v in zip(E.CENTRES, lin) if 440 <= c <= 2250])
    return [10 * math.log10(v / ref) for v in lin]


def write_wav(path, x):
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())


SPEAKER = chain(biquad_power("hp", 400), biquad_power("pk", 3000, q=1.0, db=6.0))
TRAP = chain(biquad_power("lp", 5000), biquad_power("pk", 100, q=1.0, db=6.0))


class Empreinte(unittest.TestCase):
    def test_the_bands_are_the_phones(self):
        self.assertEqual(26, len(E.CENTRES))
        self.assertAlmostEqual(49.6, E.CENTRES[0], delta=0.1)
        self.assertAlmostEqual(16000, E.CENTRES[-1], delta=0.01)
        self.assertEqual("1000", E.NAMES[E.CENTRES.index(1000.0)])

    def test_filtered_pink_noise_reads_its_filter(self):
        song = chain(biquad_power("hp", 200), biquad_power("pk", 2000, q=0.7, db=-6))
        energies, frames = E.third_octave_energies(pink(20, song), RATE)
        self.assertGreater(frames, 100)
        got = E.octaves(energies, frames)
        want = expected_octaves(song)
        self.assertEqual(8, len(got))
        for o, g, w in zip(E.OCTAVES, got, want):
            self.assertIsNotNone(g, o)
            self.assertLessEqual(abs(g - w), 1.0, f"{o} Hz: {g:.2f} dB, imposed {w:.2f} dB")

    def test_the_bench_heard_through_the_phones_curve_matches_the_phone(self):
        bench = pink(20, TRAP, seed=3)
        phone = expected_octaves(chain(TRAP, SPEAKER))
        curve = expected_curve(SPEAKER)
        energies, frames = E.third_octave_energies(bench, RATE)
        heard = E.octaves(E.apply_curve(energies, curve), frames)
        gaps = E.compare(phone, heard)
        self.assertEqual(8, len(gaps))
        for o, g in zip(E.OCTAVES, gaps):
            self.assertLessEqual(abs(g), 1.0, f"{o} Hz: {g:.2f} dB")
        self.assertEqual("vert", E.light(gaps)[0])
        # Without the curve the bench is far from the phone: the curve is what makes them meet.
        raw = E.compare(phone, E.octaves(energies, frames))
        self.assertGreater(max(abs(g) for g in raw if g is not None), 6.0)

    def test_a_missing_bass_on_the_phone_is_red_and_named(self):
        phone = expected_octaves(chain(TRAP, SPEAKER))
        phone[1] -= 10.0
        energies, frames = E.third_octave_energies(pink(20, TRAP, seed=3), RATE)
        gaps = E.compare(phone, E.octaves(E.apply_curve(energies, expected_curve(SPEAKER)), frames))
        feu, why = E.light(gaps)
        self.assertEqual("rouge", feu)
        self.assertIn("125 Hz", why)
        phone[1] += 5.0
        gaps = E.compare(phone, E.octaves(E.apply_curve(energies, expected_curve(SPEAKER)), frames))
        self.assertEqual("orange", E.light(gaps)[0])

    def test_an_octave_the_phone_did_not_hear_is_left_out(self):
        phone = expected_octaves(chain(TRAP, SPEAKER))
        phone[0] = None
        energies, frames = E.third_octave_energies(pink(20, TRAP, seed=3), RATE)
        gaps = E.compare(phone, E.octaves(E.apply_curve(energies, expected_curve(SPEAKER)), frames))
        self.assertEqual(8, len(gaps))
        self.assertIsNone(gaps[0])
        self.assertEqual("vert", E.light(gaps)[0])

    def test_a_band_the_phone_did_not_hear_is_not_a_missing_bass(self):
        # The phone's curve lost 50 and 63 Hz under its room, its microphone still heard the song's bass:
        # the 63 Hz octave cannot be compared (unknown), it is not « the bench has no bass ».
        curve = expected_curve(SPEAKER)
        curve[0] = curve[1] = None
        phone = expected_octaves(chain(TRAP, SPEAKER))
        energies, frames = E.third_octave_energies(pink(20, TRAP, seed=3), RATE)
        gaps = E.compare(phone, E.octaves(E.apply_curve(energies, curve), frames))
        self.assertEqual(8, len(gaps))
        self.assertIsNone(gaps[0], gaps)
        self.assertEqual("vert", E.light(gaps)[0])


class Commands(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()
        curve = expected_curve(SPEAKER)
        micro = {
            "rate": RATE, "tiers": E.NAMES, "courbe_db": [round(v, 2) for v in curve], "octaves": E.OCTAVES,
            "empreintes": {"trap": {"db": [round(v, 2) for v in expected_octaves(chain(TRAP, SPEAKER))], "debut_s": 2.0, "duree_s": 15.0}},
        }
        self.mesures = os.path.join(self.dir, "mesures.json")
        with open(self.mesures, "w") as f:
            json.dump({"Version": "test", "steps": [], "reads": [], "micro": micro}, f)
        self.bench = os.path.join(self.dir, "trap.wav")
        # The bench's song: 2 s of something else first (the window starts at debut_s), then the trap.
        write_wav(self.bench, np.concatenate([pink(2, biquad_power("hp", 4000), seed=9), pink(18, TRAP, seed=3)]))

    def run_tool(self, *args):
        return subprocess.run([sys.executable, "-I", os.path.join(HERE, "empreinte.py"), *args], capture_output=True, text=True)

    def test_comparer_writes_the_table_with_its_light(self):
        out = os.path.join(self.dir, "rapport.md")
        r = self.run_tool("comparer", self.mesures, "--wav", f"trap={self.bench}", "--out", out)
        self.assertEqual(0, r.returncode, r.stderr)
        with open(out, encoding="utf-8") as f:
            text = f.read()
        self.assertIn("| trap |", text)
        self.assertIn("vert", text)
        self.assertIn("125 Hz", text)

    def test_a_curve_never_heard_is_said_not_a_crash(self):
        with open(self.mesures) as f:
            m = json.load(f)
        m["micro"]["courbe_db"] = [None] * 26
        with open(self.mesures, "w") as f:
            json.dump(m, f)
        r = self.run_tool("filtrer", self.mesures, self.bench, os.path.join(self.dir, "x.wav"))
        self.assertNotEqual(0, r.returncode)
        self.assertNotIn("Traceback", r.stderr)
        self.assertIn("aucune bande", r.stderr)

    def test_courbe_prints_the_phones_curve(self):
        r = self.run_tool("courbe", self.mesures)
        self.assertEqual(0, r.returncode, r.stderr)
        self.assertIn("1000\t", r.stdout)
        self.assertEqual(26, len([l for l in r.stdout.splitlines() if "\t" in l]))

    def test_filtrer_makes_a_wav_as_the_phones_speaker_plays_it(self):
        out = os.path.join(self.dir, "trap-telephone.wav")
        src = os.path.join(self.dir, "flat.wav")
        write_wav(src, pink(12, seed=4))
        r = self.run_tool("filtrer", self.mesures, src, out)
        self.assertEqual(0, r.returncode, r.stderr)
        with wave.open(out) as w:
            self.assertEqual(RATE, w.getframerate())
            x = np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(np.float64) / 32768
        energies, frames = E.third_octave_energies(x, RATE)
        got = E.octaves(energies, frames)
        want = expected_octaves(SPEAKER)
        for o, g, w in zip(E.OCTAVES[1:], got[1:], want[1:]):
            self.assertLessEqual(abs(g - w), 1.5, f"{o} Hz: {g} against {w}")


class Wrapper(unittest.TestCase):
    """comparer-telephone.sh on a live bench already run (--live): its ps_live renders each song, the report compares them."""

    def test_the_four_songs_rendered_and_compared_without_gradle(self):
        d = tempfile.mkdtemp()
        live = os.path.join(d, "live")
        os.makedirs(os.path.join(live, "cmds"))
        os.makedirs(os.path.join(live, "tools"))
        styles = ["jump_up", "house", "trap", "techno"]
        for s in styles:
            with open(os.path.join(live, "cmds", s + ".pslive"), "w") as f:
                f.write("META test\n")
        # A stand-in ps_live: « --wav OUT » gets the trap song (16-bit, mono) whatever the commands.
        src = os.path.join(d, "song.wav")
        write_wav(src, pink(10, TRAP, seed=3))
        fake = os.path.join(live, "tools", "ps_live")
        with open(fake, "w") as f:
            f.write("#!/bin/sh\nwhile [ $# -gt 0 ]; do [ \"$1\" = --wav ] && cp '" + src + "' \"$2\"; shift; done\nexit 4\n")
        os.chmod(fake, 0o755)
        print_ = [round(v, 2) for v in expected_octaves(chain(TRAP, SPEAKER))]
        micro = {"courbe_db": [round(v, 2) for v in expected_curve(SPEAKER)], "tiers": E.NAMES, "octaves": E.OCTAVES,
                 "empreintes": {s: {"db": print_, "debut_s": 0.0, "duree_s": 8.0} for s in styles}}
        mes = os.path.join(d, "mesures.json")
        with open(mes, "w") as f:
            json.dump({"micro": micro}, f)
        out = os.path.join(d, "out")
        r = subprocess.run(["bash", os.path.join(HERE, "comparer-telephone.sh"), mes, "--live", live, "--out", out, "--seconds", "10"],
                           capture_output=True, text=True)
        self.assertEqual(0, r.returncode, r.stdout + r.stderr)
        with open(os.path.join(out, "rapport.md"), encoding="utf-8") as f:
            text = f.read()
        for s in styles:
            self.assertRegex(text, r"\| " + s + r" \|[^\n]*\| vert \|")
        self.assertEqual([], [n for n in os.listdir(os.path.join(out, "wav")) if n.endswith(".wav")], "renders deleted")


if __name__ == "__main__":
    unittest.main()
