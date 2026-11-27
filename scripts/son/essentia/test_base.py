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
