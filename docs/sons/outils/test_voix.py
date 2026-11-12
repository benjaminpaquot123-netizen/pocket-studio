#!/usr/bin/env python3
"""voix.py checked before it measures (CLAUDE.md: « l'outil de mesure se vérifie avant de mesurer »).

  python3 -I docs/sons/outils/test_voix.py

A band-limited sawtooth (harmonics 1/n up to 20 kHz) has known figures: at 261.6 Hz its energy over 10 kHz is
-21.0 dB of the whole, its centroid 788 Hz, its 4-10 kHz band -7.6 dB under its 1-4 kHz band (sum of 1/n² over
the harmonics in each band); every published column (air, centroid, brillance, boue, présence) is checked so on
sawtooths at 65.4, 261.6 and 523.3 Hz. The same sawtooth written as 16-bit and as float 32, a wide copy (side measured),
a panned copy (side / centre known exactly) and a silent file (nothing measured) are checked too.
"""
import os
import struct
import sys
import tempfile

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import voix  # noqa: E402

RATE = 48000


def saw(f0, seconds=3.0, amp=0.3):
    t = np.arange(int(RATE * seconds)) / RATE
    x = np.zeros_like(t)
    n = 1
    while n * f0 < 20000:
        x += np.sin(2 * np.pi * n * f0 * t) / n
        n += 1
    return amp * x / np.max(np.abs(x))


def expected(f0):
    n = np.arange(1, int(20000 / f0) + 1)
    if n[-1] * f0 >= 20000:
        n = n[:-1]
    e, fr = 1.0 / n ** 2, n * f0
    db = lambda v: 10 * np.log10(v) if v > 0 else -np.inf
    return {
        'air_10k_plus_db': db(e[fr >= 10000].sum() / e.sum()),
        'centroid_hz': (e * fr).sum() / e.sum(),
        'brillance_4_10k_vs_1_4k_db': db(e[(fr >= 4000) & (fr < 10000)].sum() / e[(fr >= 1000) & (fr < 4000)].sum()),
        'boue_150_500_vs_1_4k_db': db(e[(fr >= 150) & (fr < 500)].sum() / e[(fr >= 1000) & (fr < 4000)].sum()),
        'presence_tel_1_4k_db': db(e[(fr >= 1000) & (fr < 4000)].sum() / e[fr >= 300].sum()),
    }


def write_wav(path, x, bits):
    frames, channels = x.shape
    if bits == 32:
        data, tag = x.astype('<f4').tobytes(), 3
    else:
        data, tag = (np.clip(x, -1, 1) * 32767).astype('<i2').tobytes(), 1
    fmt = struct.pack('<HHIIHH', tag, channels, RATE, RATE * channels * bits // 8, channels * bits // 8, bits)
    with open(path, 'wb') as f:
        f.write(b'RIFF' + struct.pack('<I', 4 + 8 + len(fmt) + 8 + len(data)) + b'WAVE')
        f.write(b'fmt ' + struct.pack('<I', len(fmt)) + fmt)
        f.write(b'data' + struct.pack('<I', len(data)) + data)


def check(cond, what):
    if not cond:
        print('FAIL', what)
        sys.exit(1)


def main():
    with tempfile.TemporaryDirectory() as d:
        for f0 in (65.4, 261.6, 523.3):
            want = expected(f0)
            mono = saw(f0)
            for bits in (32, 16):
                p = os.path.join(d, f'saw{f0}-{bits}.wav')
                write_wav(p, np.stack([mono, mono], axis=1), bits)
                got = voix.measure(*voix.read_wav(p))
                check(got['frames'] > 0, f'{f0} Hz {bits}-bit: no frame measured')
                check(abs(got['air_10k_plus_db'] - want['air_10k_plus_db']) <= 0.3,
                      f"{f0} Hz {bits}-bit air {got['air_10k_plus_db']} vs {want['air_10k_plus_db']:.1f}")
                check(abs(got['centroid_hz'] / want['centroid_hz'] - 1) <= 0.03,
                      f"{f0} Hz {bits}-bit centroid {got['centroid_hz']} vs {want['centroid_hz']:.0f}")
                check(abs(got['brillance_4_10k_vs_1_4k_db'] - want['brillance_4_10k_vs_1_4k_db']) <= 0.3,
                      f"{f0} Hz {bits}-bit brillance {got['brillance_4_10k_vs_1_4k_db']} vs {want['brillance_4_10k_vs_1_4k_db']:.1f}")
                for key in ('boue_150_500_vs_1_4k_db', 'presence_tel_1_4k_db'):
                    if np.isfinite(want[key]):
                        check(abs(got[key] - want[key]) <= 0.3, f"{f0} Hz {bits}-bit {key} {got[key]} vs {want[key]:.1f}")
                    else:  # no harmonic in the band (523.3 Hz: none from 150 to 500 Hz): only the window's leak
                        check(got[key] < -40, f"{f0} Hz {bits}-bit {key} {got[key]}: an empty band read")
                check(got['cote_centre_db'] < -100, f'{f0} Hz {bits}-bit: a mono file has no side')
        # Panned: right = half the left, so side / centre = (0.25 / 0.75)^2 = -9.5 dB exactly.
        mono = saw(261.6)
        p = os.path.join(d, 'panned.wav')
        write_wav(p, np.stack([mono, 0.5 * mono], axis=1), 32)
        got = voix.measure(*voix.read_wav(p))
        check(abs(got['cote_centre_db'] - 20 * np.log10(0.25 / 0.75)) <= 0.1, f"panned side {got['cote_centre_db']}")
        # Silence: nothing over the gate, nothing measured.
        p = os.path.join(d, 'silence.wav')
        write_wav(p, np.full((RATE * 2, 2), 1e-4), 32)
        check(voix.measure(*voix.read_wav(p)) == {'frames': 0}, 'silence measured')
    print('voix.py: OK (sawtooth 65.4, 261.6 and 523.3 Hz, every column, 16-bit and float, panned copy, silence)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
