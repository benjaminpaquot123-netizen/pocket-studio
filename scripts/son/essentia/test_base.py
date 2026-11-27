"""Self-tests for base.py, on synthetic signals only (no sound from the stock).

  python -I scripts/son/essentia/test_base.py          # every test
  python -I scripts/son/essentia/test_base.py NAME...  # some tests
Exit 0 = all green. Needs the venv that holds essentia + librosa.
"""
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


TESTS = [v for k, v in sorted(globals().items()) if k.startswith("test_")]


def main(names):
    bad = 0
    for t in TESTS:
        if names and t.__name__ not in names:
            continue
        try:
            t()
            print("ok   " + t.__name__)
        except AssertionError as e:
            bad += 1
            print("FAIL %s: %s" % (t.__name__, e))
    print("%d failure(s)" % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
