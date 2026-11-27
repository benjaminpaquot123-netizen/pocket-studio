"""Self-tests for base.py, on synthetic signals only (no sound from the stock).

  python -I scripts/son/essentia/test_base.py          # every test
  python -I scripts/son/essentia/test_base.py NAME...  # some tests
Exit 0 = all green. Needs the venv that holds essentia + librosa.
"""
import io
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import base  # noqa: E402

SR = base.SR
RNG = np.random.default_rng(7)


def stereo(y):
    y = np.asarray(y, dtype=np.float32)
    return np.stack([y, y], 1)


def tone(f, dur, amp=1.0):
    t = np.arange(int(dur * SR)) / SR
    return amp * np.sin(2 * np.pi * f * t)


# ---------------------------------------------------------------- 1. band shares
def test_band_shares_are_energy():
    """1 kHz at amplitude 1 + 100 Hz at amplitude 0.5: energy 4:1, so the mid
    band holds -0.97 dB of the total and the low band -6.99 dB."""
    y = tone(1000, 2.0) + tone(100, 2.0, 0.5)
    out = base.ess_measure(stereo(0.5 * y), "melodic_loop", "melodic_loop/lead")
    mid, low = out["mid_share_db"], out["low_share_db"]
    want_mid, want_low = 10 * math.log10(0.8), 10 * math.log10(0.2)
    assert abs(mid - want_mid) < 0.3, "mid share %.2f dB, want %.2f" % (mid, want_mid)
    assert abs(low - want_low) < 0.3, "low share %.2f dB, want %.2f" % (low, want_low)


# ---------------------------------------------------------------- 2. effective duration
def test_effective_duration_reads_the_envelope():
    """Noise decaying with tau = 45 ms: its envelope stays above 40 % of the
    peak for about 41 ms (tau * ln 2.5). The raw signal gives a few ms."""
    t = np.arange(SR) / SR
    y = 0.8 * RNG.standard_normal(SR) * np.exp(-t / 0.045)
    out = base.ess_measure(stereo(y / np.abs(y).max() * 0.8), "fx", "fx/fx")
    d = out["effective_dur_s"]
    assert 0.025 < d < 0.070, "effective duration %.4f s, want about 0.041" % d


# ---------------------------------------------------------------- 3. short hits
def test_kick_rolloff_is_finer_than_a_bin():
    """A kick-like 75 Hz decaying sine: the rolloff must land near 75 Hz with a
    resolution finer than the 21.5 Hz bin of a 2048-point frame."""
    t = np.arange(SR) / SR
    y = 0.9 * np.sin(2 * np.pi * 75 * t) * np.exp(-t / 0.15)
    out = base.ess_measure(stereo(y), "drum_oneshot", "drum_oneshot/kick")
    res = out.get("rolloff_res_hz")
    assert res is not None and res < 5, "rolloff resolution %s Hz, want < 5" % res
    assert abs(out["rolloff_hz"] - 75) < 6, "rolloff %.1f Hz, want about 75" % out["rolloff_hz"]


def test_short_hit_describes_its_body_not_its_tail():
    """A bright 30 ms noise burst, then 0.5 s of a quiet 200 Hz tail (about
    -30 dB): the hat is bright, its tail must not drag the centroid down."""
    burst = RNG.standard_normal(int(0.030 * SR))
    burst = 0.8 * burst / np.abs(burst).max()
    y = np.concatenate([burst, tone(200, 0.5, 0.01)])
    out = base.ess_measure(stereo(y), "drum_oneshot", "drum_oneshot/hat_closed")
    assert out.get("mode") == "coup", "mode %s, want coup" % out.get("mode")
    assert out["centroid_hz"] > 4000, "centroid %.0f Hz, want > 4000" % out["centroid_hz"]


# ---------------------------------------------------------------- 4. independent counter-measure
def wav_bytes(y, sr):
    import soundfile as sf
    b = io.BytesIO()
    sf.write(b, np.asarray(y, dtype=np.float32), sr, format="WAV", subtype="FLOAT")
    return b.getvalue()


def test_counter_measure_loads_on_its_own():
    """If Essentia's resampling went wrong (here: halves the level), librosa,
    which loads and resamples the file itself, must still read the true level."""
    t = np.arange(int(2.0 * 48000)) / 48000
    data = wav_bytes(0.5 * np.sin(2 * np.pi * 1000 * t), 48000)  # RMS -9.03 dBFS
    orig = base.resample
    base.resample = lambda x, sr: orig(x, sr) * 0.5
    try:
        rec = base.measure_bytes(data, "melodic_loop", "melodic_loop/lead")
    finally:
        base.resample = orig
    got = rec["lib"]["rms_db"]
    assert abs(got - (-9.03)) < 0.3, "librosa rms %.2f dB, want -9.03 (it reused Essentia's array)" % got


def test_counter_measure_tempo_is_fine():
    """A 145 BPM click track: librosa's own tempo must be within 0.5 %."""
    n = int(20 * SR)
    y = np.zeros(n)
    click = np.exp(-np.arange(400) / 60.0) * RNG.standard_normal(400)
    for k in range(int(20 * 145 / 60)):
        i = int(k * 60 / 145 * SR)
        y[i:i + 400] += 0.8 * click[:max(0, min(400, n - i))]
    rec = base.measure_bytes(wav_bytes(y, SR), "drum_loop", "drum_loop/full")
    b = rec["lib"].get("bpm")
    assert b is not None and abs(b - 145) / 145 < 0.005, "librosa bpm %s, want 145 +- 0.5 %%" % b


def test_agree_folds_octaves_and_tempo_ratios():
    assert base.agree("f0_hz_octave", 110.0, 220.5)
    assert not base.agree("f0_hz", 110.0, 220.5)
    assert base.agree("bpm_replie", 90.0, 135.0)
    assert not base.agree("bpm", 90.0, 135.0)
    assert base.agree("bpm", 140.0, 141.0)
    assert not base.agree("bpm", 140.0, 70.0)


def test_ground_truth_from_the_file_name():
    tr = base.truth
    assert tr("Dark Loop 145 BPM Am.wav", "drum_loop").get("bpm") == 145
    assert tr("loop_07_128.wav", "drum_loop").get("bpm") == 128
    assert tr("loop_90_128.wav", "drum_loop").get("bpm") is None  # two candidates: no truth
    assert tr("bass_hit_C#1.wav", "oneshot_melodic").get("note") == 1
    assert tr("pluck Bb.wav", "oneshot_melodic").get("note") == 10
    assert tr("Kick_A.wav", "drum_oneshot").get("note") is None  # a variant letter, not a note


# ---------------------------------------------------------------- 5. families
def test_families_are_refined():
    """Generic keyword rules only (never a pack or vendor name)."""
    cases = [
        ("drum_oneshot/kick_808", "a/b/808 Boom C.wav", "oneshot_melodic/808"),
        ("oneshot_melodic/bass", "a/808 Long F.wav", "oneshot_melodic/808"),
        ("melodic_loop/bass", "a/808 Loop 140 BPM.wav", "melodic_loop/808"),
        ("drum_oneshot/hat_closed", "a/808 Hat.wav", "drum_oneshot/hat_closed"),  # a drum-machine hat
        ("melodic_loop/autre", "a/Adlib Hey 140 BPM.wav", "vocal/vocal"),
        ("melodic_loop/autre", "a/Sung Word 90 BPM.wav", "vocal/vocal"),
        ("oneshot_melodic/autre", "a/Viola Held C.wav", "oneshot_melodic/strings"),
        ("oneshot_melodic/autre", "a/Kalimba A.wav", "oneshot_melodic/bell"),
        ("kit_stem/autre", "a/Claves 100 BPM.wav", "kit_stem/perc"),
        ("drum_loop/pad", "a/texture.wav", "melodic_loop/pad"),
        ("drum_loop/synth", "a/x.wav", "drum_loop/full"),
        ("fx/drums", "a/x.wav", "fx/fx"),
        ("fx/chords", "a/x.wav", "melodic_loop/chords"),
        ("melodic_loop/drums", "a/top.wav", "drum_loop/drums"),
        ("melodic_loop/perc", "a/cowbell.wav", "drum_loop/perc"),
        ("oneshot_melodic/drums", "a/x.wav", "oneshot_melodic/bell"),
        ("oneshot_melodic/full", "a/Bass Stab.wav", "oneshot_melodic/bass"),
        ("fx/fx", "a/Bass Hit C.wav", "oneshot_melodic/bass"),
        ("fx/fx", "a/Riser.wav", "fx/fx"),
        ("drum_oneshot/kick:attack", "a/x.wav", "drum_oneshot/kick"),
        ("drum_oneshot/perc_high", "a/x.wav", "drum_oneshot/perc"),
        ("drum_oneshot/snap", "a/x.wav", "drum_oneshot/clap"),
        ("drum_oneshot/kick", "a/Kick 3.wav", "drum_oneshot/kick"),
    ]
    bad = [(f, m, base.refine(f, m), w) for f, m, w in cases if base.refine(f, m) != w]
    assert not bad, "; ".join("%s %s -> %s, want %s" % b for b in bad)


# ---------------------------------------------------------------- 6. flatness and source rate
def test_flatness_does_not_depend_on_the_source_rate():
    """White noise recorded at 48 kHz or at 44.1 kHz is the same sound. The
    resampler empties the band above about 20 kHz, which drags a full-band
    flatness down by 1 to 2 dB: both tools must read the same as native."""
    r = np.random.default_rng(3)
    a = base.measure_bytes(wav_bytes(0.3 * r.standard_normal(2 * 44100), 44100), "fx", "fx/fx")
    b = base.measure_bytes(wav_bytes(0.3 * r.standard_normal(2 * 48000), 48000), "fx", "fx/fx")
    for tool in ("ess", "lib"):
        fa, fb = a[tool]["flatness_db"], b[tool]["flatness_db"]
        assert abs(fa - fb) < 0.3, "%s flatness %.2f dB at 44.1 kHz, %.2f dB at 48 kHz" % (tool, fa, fb)


# ---------------------------------------------------------------- 7. contre-lecture of ec2059e41
FIN = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "docs", "sons", "fin")


def test_doc_family_sizes_match_the_tsv():
    """The "Sons" column of ESSENTIA-BASE.md is the family size: the largest
    n_sons of that family in essentia-familles.tsv, not one measure's count."""
    import csv
    size = {}
    with open(os.path.join(FIN, "essentia-familles.tsv")) as f:
        for r in csv.DictReader(f, delimiter="\t"):
            size[r["famille"]] = max(size.get(r["famille"], 0), int(r["n_sons"]))
    bad = []
    with open(os.path.join(FIN, "ESSENTIA-BASE.md")) as f:
        for line in f:
            c = [x.strip() for x in line.strip().strip("|").split("|")]
            if len(c) > 2 and c[0] in size and c[1].isdigit() and int(c[1]) != size[c[0]]:
                bad.append("%s %s != %d" % (c[0], c[1], size[c[0]]))
    assert not bad, "; ".join(bad)


def test_doc_names_only_files_that_exist():
    """Every `NAME.md` the doc cites sits next to it or in the repository."""
    import re
    root = os.path.join(FIN, "..", "..", "..")
    with open(os.path.join(FIN, "ESSENTIA-BASE.md")) as f:
        names = set(re.findall(r"`([^`\s]+\.md)`", f.read()))
    miss = [n for n in names if not (os.path.exists(os.path.join(FIN, n)) or os.path.exists(os.path.join(root, n)))]
    assert not miss, "dead names: %s" % miss


def test_truth_leaves_unmeasured_loops_out():
    """A loop with no measured tempo is not a wrong tempo: it leaves the
    denominator and is counted in its own column."""
    import csv
    import json
    import tempfile
    d = tempfile.mkdtemp()
    src = os.path.join(d, "b.jsonl")
    with open(src, "w") as f:
        for i in range(8):
            m = {"bpm": 120.0} if i < 5 else {}
            f.write(json.dumps({"sid": "s", "id": str(i), "fam": "drum_loop/full", "style": "x", "pack": "p",
                                "ess": dict(m), "lib": dict(m), "truth": {"bpm": 120}}) + "\n")
    base.cmd_stats(src, d)
    with open(os.path.join(d, "essentia-verite.tsv")) as f:
        rows = {r["mesure"]: r for r in csv.DictReader(f, delimiter="\t")}
    r = rows["bpm"]
    assert float(r["essentia_pct"]) == 100.0, "essentia_pct %s, want 100" % r["essentia_pct"]
    assert r.get("essentia_non_mesures") == "3", "essentia_non_mesures %s, want 3" % r.get("essentia_non_mesures")


def test_effective_duration_of_a_low_808():
    """A 30 and a 40 Hz sine decaying with tau = 150 ms: the envelope must not
    ripple with the period, so the duration stays near tau * ln 2.5 (137 ms)."""
    t = np.arange(int(1.5 * SR)) / SR
    for f0 in (30, 40):
        y = 0.9 * np.sin(2 * np.pi * f0 * t) * np.exp(-t / 0.15)
        d = base.ess_measure(stereo(y), "oneshot_melodic", "oneshot_melodic/808")["effective_dur_s"]
        assert abs(d - 0.1374) / 0.1374 < 0.05, "%d Hz: effective duration %.4f s, want 0.137 +- 5 %%" % (f0, d)


def test_kick_attack_survives_the_body_window():
    """A 60 Hz kick with a loud 10 ms click: the click holds most of the
    energy, so the rolloff is high. A full Hann window on the body erases
    the attack; only the end of the body may be tapered."""
    t = np.arange(int(2 * SR)) / SR
    r = np.random.default_rng(1)
    m = int(0.010 * SR)
    click = np.zeros_like(t)
    click[:m] = r.standard_normal(m) * np.exp(-np.arange(m) / (0.002 * SR))
    y = 0.9 * np.sin(2 * np.pi * 60 * t) * np.exp(-t / 0.12) + 3 * click
    y = 0.9 * y / np.abs(y).max()
    rec = base.measure_bytes(wav_bytes(y, SR), "drum_oneshot", "drum_oneshot/kick")
    for tool in ("ess", "lib"):
        assert rec[tool]["rolloff_hz"] > 4000, "%s rolloff %.0f Hz, want > 4000" % (tool, rec[tool]["rolloff_hz"])


TESTS = [v for k, v in sorted(globals().items()) if k.startswith("test_")]


def main(names):
    bad = 0
    for t in TESTS:
        if names and t.__name__ not in names:
            continue
        try:
            t()
            print("ok   " + t.__name__)
        except Exception as e:  # an assertion, or a missing function before the fix
            bad += 1
            print("FAIL %s: %s: %s" % (t.__name__, type(e).__name__, e))
    print("%d failure(s)" % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
