#!/usr/bin/env python3
"""BenStudio Test, « Écoute au micro » (docs/PLAN-TEST-APK.md, T1b): the phone against the Linux bench.

The phone sends, in mesures.json, its speaker's curve (third-octave, dB) and each song's fingerprint at the microphone
(energy per octave). This tool measures the same fingerprint on the bench's render of the same song, heard « as on
Benjamin's phone » (the bench's third-octave energies weighted by the phone's curve, in place of the guessed 300 Hz
high-pass), and compares the two, octave by octave.

  empreinte.py comparer mesures.json --wav STYLE=bench.wav [--wav …] [--out rapport.md]
  empreinte.py courbe mesures.json                     # the curve, one band per line
  empreinte.py filtrer mesures.json in.wav out.wav     # a WAV as the phone's speaker plays it

Same analysis as the phone's (app/src/benTest/…/Spectre.kt): Hann window of 16384 samples, half overlapped, each
third-octave band the power of the bins from fc·2^-1/6 (in) to fc·2^1/6 (out), octave = 3 third-octaves.
"""
import sys

SIZE = 16384
CENTRES = []
NAMES = []
OCTAVES = [63, 125, 250, 500, 1000, 2000, 4000, 8000]


def third_octave_energies(x, rate=48000):
    """(energies summed over the frames, frame count) of mono float samples."""
    return [0.0] * len(CENTRES), 0


def octaves(energies, frames, noise=None):
    """dB per octave, 0 dB = mean of 500 Hz … 4 kHz; None under the noise."""
    return []


def apply_curve(energies, curve_db):
    """The energies as the phone's speaker gives them: each band weighted by the curve (None: band left as is)."""
    return list(energies)


def compare(phone, bench):
    """Per octave: phone minus bench after lining both up on their common octaves of 500 Hz … 4 kHz (None: missing)."""
    return []


def light(gaps):
    """(feu, why) from the gaps: vert ≤ 3 dB, orange ≤ 6 dB, rouge beyond."""
    return "non concluant", ""


def main(argv):
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
