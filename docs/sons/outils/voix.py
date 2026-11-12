#!/usr/bin/env python3
"""Timbre of one voice rendered alone (docs/sons/PLAN-SONS-MODERNES.md § 2.2). Standard library + numpy.

  python3 -I docs/sons/outils/voix.py <voice.wav>

The WAV is a voice rendered alone before the master: ps_render --no-limiter --solo <channel> <song.pscmd> <out.wav>
(float 32 or int 16, any channel count). Measured on the frames where the voice plays (8192-point Hann windows,
hop 4096, a window kept when its RMS is over -50 dBFS), mono sum for the spectrum, (L - R) / 2 for the side.
Prints one JSON line:
  centroid_hz                   spectral centroid
  air_10k_plus_db               10-20 kHz against 20 Hz-20 kHz (the bench's own « air », ps_measure, is 6-20 kHz)
  air_12k_plus_db               12-20 kHz against 20 Hz-20 kHz
  brillance_4_10k_vs_1_4k_db    4-10 kHz against 1-4 kHz (docs/hits/leads-pros.md)
  boue_150_500_vs_1_4k_db       150-500 Hz against 1-4 kHz (same)
  presence_tel_1_4k_db          1-4 kHz against 300 Hz-20 kHz (what a phone's speaker keeps, roughly)
  cote_centre_db                side energy against centre energy
Checked by test_voix.py on a band-limited sawtooth whose figures are known exactly.
"""
import json
import struct
import sys

import numpy as np

N = 8192
HOP = 4096
GATE_DBFS = -50.0


def read_wav(path):
    b = open(path, 'rb').read()
    if b[:4] != b'RIFF' or b[8:12] != b'WAVE':
        raise ValueError(f'{path}: not a WAV file')
    i, fmt, data = 12, None, None
    while i + 8 <= len(b):
        cid = b[i:i + 4]
        size = struct.unpack('<I', b[i + 4:i + 8])[0]
        body = b[i + 8:i + 8 + size]
        if cid == b'fmt ':
            fmt = struct.unpack('<HHIIHH', body[:16])
        elif cid == b'data':
            data = body
        i += 8 + size + (size & 1)
    if fmt is None or data is None:
        raise ValueError(f'{path}: no fmt or data chunk')
    _, channels, rate, _, _, bits = fmt
    if bits == 32:
        x = np.frombuffer(data, dtype='<f4').astype(np.float64)
    elif bits == 16:
        x = np.frombuffer(data, dtype='<i2').astype(np.float64) / 32768.0
    else:
        raise ValueError(f'{path}: {bits}-bit samples not read')
    return x.reshape(-1, channels), rate


def measure(x, rate):
    mono = x.mean(axis=1)
    side = (x[:, 0] - x[:, -1]) / 2.0
    window = np.hanning(N)
    freqs = np.fft.rfftfreq(N, 1.0 / rate)
    power = np.zeros(len(freqs))
    centre_energy = side_energy = 0.0
    frames = 0
    gate = 10.0 ** (GATE_DBFS / 20.0)
    for k in range(0, len(mono) - N + 1, HOP):
        seg = mono[k:k + N]
        if np.sqrt(np.mean(seg ** 2)) < gate:
            continue
        power += np.abs(np.fft.rfft(seg * window)) ** 2
        centre_energy += float(np.sum(seg ** 2))
        side_energy += float(np.sum(side[k:k + N] ** 2))
        frames += 1
    if frames == 0:
        return {'frames': 0}

    def band(lo, hi):
        return float(power[(freqs >= lo) & (freqs < hi)].sum()) + 1e-30

    def db(v):
        return round(10.0 * float(np.log10(v)), 1)

    total = band(20, 20000)
    return {
        'frames': frames,
        'centroid_hz': round(float((power * freqs).sum() / power.sum())),
        'air_10k_plus_db': db(band(10000, 20000) / total),
        'air_12k_plus_db': db(band(12000, 20000) / total),
        'brillance_4_10k_vs_1_4k_db': db(band(4000, 10000) / band(1000, 4000)),
        'boue_150_500_vs_1_4k_db': db(band(150, 500) / band(1000, 4000)),
        'presence_tel_1_4k_db': db(band(1000, 4000) / band(300, 20000)),
        'cote_centre_db': db((side_energy + 1e-30) / (centre_energy + 1e-30)),
    }


def main(argv):
    if len(argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    x, rate = read_wav(argv[1])
    print(json.dumps(measure(x, rate)))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
