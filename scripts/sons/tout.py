#!/usr/bin/env python3
"""Analyse every non-drum-one-shot sound of a sample pack (stub, see the self-test)."""
import sys

COMMON = ["rar", "kit_id", "type", "subtype", "role", "style", "dur_s", "sr", "ch",
          "peak_dbfs", "rms_dbfs", "lufs_i", "error"]


class BadAudio(Exception):
    pass


def _todo(*a, **k):
    raise NotImplementedError("tout.py not written yet")


parse_tags = classify = common = measure_oneshot = measure_drum_loop = _todo
measure_melodic_loop = measure_midi_bytes = measure_vocal = measure_fx = _todo
measure_drum_oneshot = main = _todo

if __name__ == "__main__":
    sys.exit(main())
