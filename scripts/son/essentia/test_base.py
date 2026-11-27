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
