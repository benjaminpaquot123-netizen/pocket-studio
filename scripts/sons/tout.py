#!/usr/bin/env python3
"""Analyse every sound of a sample pack that is not a plain drum one-shot,
and drum one-shots again with the columns mesure.py lacks.

Usage (samples are untrusted data: isolated mode, paths as arguments):

    python3 -I scripts/sons/tout.py <pack_dir> --rar N --out <csv_prefix>

Writes <csv_prefix>_<type>.csv, one table per content type, and
<csv_prefix>_accuracy.csv. No file or folder name is ever written: a row
holds numbers and a few fixed labels (type, role, style). The tool still
refuses to write inside this git repository (public repo, bought samples).
Needs numpy only. Self-test: python3 -I scripts/sons/selftest_tout.py

Every run uses the same columns, so tables of the 11 packs add up.

Common columns (first in every table)
-------------------------------------
rar        pack number given by --rar ("app" for BenStudio's own renders)
kit_id     r<rar>k<nnn> shared by the stems and MIDI of one construction kit
type       oneshot_melodic | drum_loop | melodic_loop | kit | kit_stem |
           vocal | fx | midi | drum_oneshot
subtype    finer label: drum family, "<family>:attack"/":tail" for layer
           files, "untagged" for long unnamed audio, "by_sound" for a loop
           whose name does not say drums or notes (the sound decided)
role       full, 808, bass, kick, snare, hats, perc, drums, vocal, fx, lead,
           pad, keys, chords, pluck, arp, guitar, strings, brass, bell,
           synth, or "-" (from the words of the name and folders)
style      genre label from the folders, "unlabelled" when none says it
dur_s sr ch    length in seconds, sample rate, channels
peak_dbfs  sample peak
rms_dbfs   RMS of the channel-summed power (a 0.1 sine reads -23.01)
lufs_i     integrated loudness, ITU-R BS.1770-4 (K-weighting, 400 ms blocks,
           75 % overlap, -70 LUFS and -10 LU gates; under 400 ms: ungated)
error      why a file could not be measured (row kept, numbers empty)

End of file (last in every audio table, not kit or midi; see end_cut())
end_cut    1 = the file really ends cut: a click (last sample above -60 dB
           and over 4x the sample-to-sample motion of its last 5 ms) or an
           abrupt stop (above -45 dB 5 ms before the end, no final fade of
           20 dB or more), levels relative to the peak, a steady noise floor
           at the end ignored. 0 otherwise, a trimmed tail with a fade
           included. A reversed sound ends loud: 1 by design (abrupt end).
end_fade_ms length of the final fade, empty when there is none, rounded to
           10 ms. Read it as a yes/no grade with an order of size: a real
           fade can be read at half or twice its length.
Both are filled for one-shots only (oneshot_melodic, drum_oneshot) and left
empty for loops, stems, fx, vocals, full mixes and any one-shot with a bpm
tag or a wav loop: there the last hit rings at the bar line and the verdict
is a coin toss (blind re-check, docs/sons/REVERIF-TOUT.md).
"Not yet at -60 dB when the file ends" (empty t60, end_level_db above -60)
only means a long sound trimmed short; it is information, never a cut.

Per type (TYPE_COLUMNS below holds the exact list)
--------------------------------------------------
oneshot_melodic  note_tag, note_measured, f0_hz, cents_gap (measured minus
    tagged pitch class, -600..600), attack_ms (10->90 % of peak, RMS
    envelope), decay_ms (peak -> 90 % of the way to sustain), sustain_db
    (median level of the held part, dB re peak), release_ms (90 % -> 10 %
    of the sustain at the end), centroid_start/mid/end_hz (first 50 ms,
    middle, last part above -40 dB), harmonic_ratio (energy within +-3 % of
    k*f0, k<=20, 50 Hz-20 kHz), stereo_width (side/(mid+side) power),
    loopable (wav smpl loop, or a held sustain part above -20 dB).
drum_loop  bpm_tag, bpm_measured, bpm_gap_pct, bars, grid_kick, grid_snare,
    grid_hats (16 chars, step of a bar: 'x' hit in at least half the bars,
    'o' in some, '.' never), swing_pct (position of the off 16ths inside
    their 8th, 50 = straight), density_per_beat (onsets per beat).
melodic_loop  bpm_tag, bpm_measured, bpm_gap_pct, bars, key_tag,
    key_measured, key_match (exact | relative | other), key_strength
    (correlation of the best key), low_db/mid_db/high_db (share of energy
    under 250 Hz, 250 Hz-4 kHz, above 4 kHz, dB). On an untagged loop or
    non-drum stem, bpm_est and key_est stay empty unless the accuracy table
    marks that measure usable.
kit        kit row: n_stems, n_midi, roles ("+"-joined), bpm_tag,
    bpm_measured (on the sum of the stems), key_tag, key_measured (on the
    sum of the non-drum stems), key_match, bars.
kit_stem   one row per stem: the melodic_loop columns + level_rel_kit_db
    (stem power / kit sum power), lufs_rel_kit_db. A "full" mix is left out
    of the sum.
vocal      wet (left empty for now: the dry / wet verdict is not reliable;
    the column stays), tail_db (level 150-300 ms after each steep phrase
    end, dB re phrase),
    pitch_median_hz (median inside the fullest semitone), range_st (5th-95th percentile, semitones), voiced_pct.
fx         bpm_tag, length_beats (dur * bpm / 60, empty without bpm),
    level_shape and bright_shape (rising | falling | flat: slope of the 8
    segment level, +-6 dB, and of the centroid, x1.5), peak_pos (0..1).
midi       bpm_file (first tempo), bars, notes, notes_per_bar, pitch_min,
    pitch_max, poly_max, vel_mean, vel_sd, swing_pct, key_tag, key_measured,
    key_match, scale_fit (share of note time in the tagged key's scale),
    chords, chord_changes_per_bar, chord_len_beats, drum_track (channel 10).
    key_measured, key_est, key_match and the three chord columns are left
    empty for now (the measured key is wrong too often and the chords hang
    on it); scale_fit is filled only against a tagged key.
drum_oneshot  mesure.py columns (onset, t20/t40/t60, pitch, harmonics,
    centroid...) + hat_measured (closed if t40 < 200 ms else open, hats
    only), drop_start_hz, drop_end_hz, drop_t50_ms, drop_t90_ms (time for
    the kick pitch to fall 50 / 90 % of its drop), drop_shape (exp | lin |
    step from t90/t50), note_measured. 808 and bass one-shots are kept.
    On kicks note_measured is approximate (within 1 semitone) and
    drop_start_hz is a lower bound: the first milliseconds of the sweep are
    higher than the pitch tracker can follow.

Estimates and accuracy
----------------------
Measured columns that have a tag (note, bpm, key) are compared on every
tagged file, one row per pair in <csv_prefix>_accuracy.csv:
  rar, type, measure, tagged, agree, half_double, relative, error_pct, usable
agree: note within 50 cents (pitch class), bpm within 2 %, key exact. A bpm at
half or double the tag and a relative key are near misses, counted in their
own column and never as agreement. usable=yes only with 10 tagged files or more and error_pct < 5:
only then may the measured column of untagged files enter means.

Methods
-------
Pitch: YIN on the mid signal (difference function by FFT, threshold 0.15,
parabolic refinement), median over frames with energy. Tempo: spectral flux
novelty, autocorrelation scored on beat multiples 60-200 bpm with a prior
around 120, refined to the bar grid when the length is a whole number of
bars. Key: chroma (50-5000 Hz) against Krumhansl-Kessler profiles. Grid:
per step peak of three band fluxes (kick 30-150 Hz, snare 1-5 kHz, hats
7-16 kHz). See the functions for details.
"""
import argparse
import csv
import math
import os
import re
import struct
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mesure  # noqa: E402
from mesure import BadAudio  # noqa: E402,F401
import mine_or  # noqa: E402

MAX_BYTES = 512 * 1024 * 1024
MAX_SECONDS = 600.0
AUDIO_EXTS = (".wav", ".wave", ".aif", ".aiff", ".aifc")
MIDI_EXTS = (".mid", ".midi")

# Fixed columns, same order in every table (blind audit B2): a reader can stack all tables.
COMMON = ["rar", "kit_id", "type", "subtype", "role", "style", "dur_s", "sr", "ch", "size_kb",
          "peak_dbfs", "rms_dbfs", "lufs_i", "bpm_tag", "key_tag", "bpm_est", "key_est", "error"]

LOOP_COLS = ["bpm_tag", "bpm_measured", "bpm_gap_pct", "bars", "key_tag", "key_measured", "key_match",
             "key_strength", "low_db", "mid_db", "high_db"]
TYPE_COLUMNS = {
    "oneshot_melodic": ["note_tag", "note_measured", "f0_hz", "cents_gap", "attack_ms", "decay_ms",
                        "sustain_db", "release_ms", "centroid_start_hz", "centroid_mid_hz", "centroid_end_hz",
                        "harmonic_ratio", "stereo_width", "loopable"],
    "drum_loop": ["bpm_tag", "bpm_measured", "bpm_gap_pct", "bars", "grid_kick", "grid_snare", "grid_hats",
                  "swing_pct", "density_per_beat"],
    "melodic_loop": LOOP_COLS,
    "kit": ["n_stems", "n_midi", "roles", "bpm_tag", "bpm_measured", "bpm_gap_pct", "key_tag", "key_measured",
            "key_match", "bars"],
    "kit_stem": LOOP_COLS + ["level_rel_kit_db", "lufs_rel_kit_db"],
    "vocal": ["wet", "tail_db", "pitch_median_hz", "range_st", "voiced_pct"],
    "fx": ["bpm_tag", "length_beats", "level_shape", "bright_shape", "peak_pos"],
    "midi": ["bpm_file", "bars", "notes", "notes_per_bar", "pitch_min", "pitch_max", "poly_max", "vel_mean",
             "vel_sd", "swing_pct", "key_tag", "key_measured", "key_match", "scale_fit", "chords",
             "chord_changes_per_bar", "chord_len_beats", "drum_track"],
    "drum_oneshot": [c for c in mesure.COLUMNS if c not in ("folder", "file", "sr", "channels", "dur_ms",
                                                            "peak_dbfs", "error")]
                    + ["hat_measured", "drop_start_hz", "drop_end_hz", "drop_t50_ms", "drop_t90_ms",
                       "drop_shape", "note_measured"],
}
END_COLS = ["end_cut", "end_fade_ms"]
for _t in TYPE_COLUMNS:  # tag and estimate live in the fixed columns (bpm_est, key_est)
    TYPE_COLUMNS[_t] = [c for c in TYPE_COLUMNS[_t] if c not in ("bpm_tag", "key_tag", "bpm_measured",
                                                                 "key_measured")]
for _t in TYPE_COLUMNS:
    if _t not in ("midi", "kit"):
        TYPE_COLUMNS[_t] = TYPE_COLUMNS[_t] + END_COLS
TYPES = list(TYPE_COLUMNS)

NOTE_NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
KK_MAJOR = np.array([6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88])
KK_MINOR = np.array([6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17])
MAJOR_SCALE = (0, 2, 4, 5, 7, 9, 11)
MINOR_SCALE = (0, 2, 3, 5, 7, 8, 10)


# ================================================================ reading

def read_audio(path):
    """(sr, float64 [frames, ch], loop_flag) for wav/aiff up to MAX_SECONDS."""
    if os.path.islink(path) or not os.path.isfile(path):
        raise BadAudio("not a regular file")
    if os.path.getsize(path) > MAX_BYTES:
        raise BadAudio("file too large")
    with open(path, "rb") as f:
        buf = f.read(MAX_BYTES + 1)
    if len(buf) < 12:
        raise BadAudio("too short")
    loop = 0
    if buf[:4] == b"RIFF" and buf[8:12] == b"WAVE":
        sr, x = mesure._read_wav(buf)
        for cid, off, size in mesure._chunks(buf, 12, big=False):
            if cid == b"smpl" and size >= 36:
                if struct.unpack("<I", buf[off + 28:off + 32])[0] > 0:
                    loop = 1
    elif buf[:4] == b"FORM" and buf[8:12] in (b"AIFF", b"AIFC"):
        sr, x = mesure._read_aiff(buf)
    else:
        raise BadAudio("not a wav or aiff file")
    mesure.check_whole(buf)
    if x.shape[0] > MAX_SECONDS * sr:
        raise BadAudio("longer than %d s" % MAX_SECONDS)
    if x.shape[0] < 16:
        raise BadAudio("too short")
    # a constant offset with nothing on top is no sound (audit 2, defect 35)
    mean = np.mean(x, axis=0)
    if np.max(np.abs(mean)) > 1e-3 and np.max(np.abs(x - mean)) < 1e-4:
        raise BadAudio("constant dc offset, no sound")
    return sr, x, loop


# ================================================================ helpers

def _db(p):
    return 10.0 * math.log10(p) if p > 1e-30 else None


def _mono(x):
    """Mid signal, or the loudest channel when the mid loses > 3 dB."""
    if x.shape[1] == 1:
        return x[:, 0].copy()
    mid = x.mean(axis=1)
    pc = np.mean(x ** 2, axis=0)
    if np.mean(mid ** 2) < 0.5 * np.max(pc):
        return x[:, int(np.argmax(pc))].copy()
    return mid


def _frames(sig, n, hop):
    if len(sig) < n:
        sig = np.concatenate([sig, np.zeros(n - len(sig))])
    count = 1 + (len(sig) - n) // hop
    idx = np.arange(n)[None, :] + hop * np.arange(count)[:, None]
    return sig[idx]


def _stft_mag(sig, n, hop):
    fr = _frames(sig, n, hop) * np.hanning(n)[None, :]
    out = np.empty((fr.shape[0], n // 2 + 1))
    for i in range(0, fr.shape[0], 512):  # bounded memory
        out[i:i + 512] = np.abs(np.fft.rfft(fr[i:i + 512], axis=1))
    return out


def midi_name(m):
    m = int(round(m))
    return "%s%d" % (NOTE_NAMES[m % 12], m // 12 - 1)


def hz_to_midi(f):
    return 69.0 + 12.0 * math.log2(f / 440.0)


def key_name(k):
    return None if k is None else "%s %s" % (NOTE_NAMES[k[0]], k[1])


def key_match(measured, tag):
    if measured is None or tag is None:
        return None
    if measured == tag:
        return "exact"
    rel = ((tag[0] + 3) % 12, "major") if tag[1] == "minor" else ((tag[0] - 3) % 12, "minor")
    return "relative" if measured == rel else "other"


# ================================================================ tags

_NOTE_RX = r"([A-Ga-g])(#|♯|♭|[ _-]?(?i:sharp|flat)|b(?![A-LN-Za-ln-z]))?"
_BPM_RX = re.compile(r"(?i)(?<![\d.])(\d{2,3}(?:\.\d+)?)\s*-?\s*bpm\b")
# bare numbers: no leading zero (Loop_073 is an index) and not '90s'
# a number followed by k / kHz / Hz / bit(s) is a sample rate or a bit depth
# '100%', "90's" and '96kHz24bit' are not tempos (audit 2 #6)
_BARE_RX = re.compile(r"(?<![\d.])([1-9]\d{1,2})"
                      r"(?![\d.%]|s\b|['’]s\b|\s*-?\s*(?i:bars?|x|k|khz|hz|bits?)(?![a-z]))")
# 'Loop BPM 140', 'bpm140' (audit 2 #10)
_BPM_PRE_RX = re.compile(r"(?i)(?<![a-z])bpm\s*[-:_]?\s*(\d{2,3}(?:\.\d+)?)(?![\d.])")
# a number right after a gear or model word is a model number, not a tempo (Juno 106, Route 66)
_GEAR_WORDS = re.compile(r"(?i)(?<![a-z])(?:juno|rhodes|mpc|route|tr|sh|cs|jx|sp|dx|tb|mc|op|mk|ob|"
                         r"prophet|jupiter|model|vol|volume|track|pack|take|part|no|nr|room|studio|"
                         r"series|type|version|ver|v)[ _-]*$")
# 'SH-101', 'TB-303': a short capital prefix and a dash name a model ('Bass-Loop-120' keeps its tempo)
_MODEL_PREFIX = re.compile(r"(?<![A-Za-z])[A-Z]{1,3}-$")
_ONLY_NUMBER = re.compile(r"\s*([1-9]\d{1,2})\s*")
BPM_CONFIRM_TOL = 0.03  # a bare number is trusted when the sound is within 3 % of it, its half or its double
_MODE_RX = r"(?:[ _-]?(?i:(maj)(?:or)?|(min)(?:or)?)|[ _-]?(m))(?:\d{1,2})?"  # 'C# m' (audit 2 #8)
_KEY_RX = re.compile(r"(?<![A-Za-z0-9])" + _NOTE_RX + r"(?:" + _MODE_RX + r")?(?![A-Za-z0-9#])")
_ONE_NOTE_RX = re.compile(r"(?<![A-Za-z0-9])" + _NOTE_RX + r"(-?\d)?(?![A-Za-z0-9#])")
# a lone letter after these words is a version or a folder letter, not a key
_VERSION_WORDS = re.compile(r"(?i)(?:kit|take|version|ver|v|part|pt|var|variation|alt|scene|"
                            r"section|group|set|bank|vol|side|mix|layer|sample|type|option)[ _-]*$")
# 'Fm Synth' is FM synthesis, 'Bb Trumpet' names a transposing instrument (audit 2 #8)
_NOT_KEY_AFTER = re.compile(r"(?i)[ _-]*(?:synth|trumpet|clarinet|sax\w*|tuba|horns?|cornet|flugel\w*)\b")
# a lone letter after these words is a key: 'Key F', 'in F', 'Blues in A' (audit 2 #11)
_KEY_WORDS = re.compile(r"(?i)(?<![a-z])(?:key|in)[ _-]*$")
FLAT_NAMES = ["C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"]


def _acc(acc):
    return (acc or "").strip(" _-").lower() or None


def _pc(letter, acc):
    pc = {"c": 0, "d": 2, "e": 4, "f": 5, "g": 7, "a": 9, "b": 11}[letter.lower()]
    acc = _acc(acc)
    if acc in ("#", "♯", "sharp"):
        pc += 1
    elif acc in ("b", "♭", "flat"):
        pc -= 1
    return pc % 12


def _clean(name):
    name = re.sub(r"(?i)\.(wav|wave|aiff?|aifc|midi?)$", "", name)
    return re.sub(r"[_]+", " ", name)


def _key_candidates(text):
    """(strong, lone) key matches: strong has an accidental or a mode."""
    strong, lone = [], []
    for m in _KEY_RX.finditer(text):
        letter, acc = m.group(1), _acc(m.group(2))
        mode = "major" if m.group(3) else ("minor" if (m.group(4) or m.group(5)) else None)
        word_mode = bool(m.group(3) or m.group(4))
        if letter.islower() and not word_mode and not (
                (acc == "#" or m.group(5)) and re.search(r"\d\s*$", text[:m.start()])):
            continue  # 'ab', 'e', 'am' are words or noise, but '120 am', '120 f#m' are keys
        if not word_mode and _NOT_KEY_AFTER.match(text, m.end()) and (
                (m.group(5) and letter == "F" and not acc) or (acc in ("b", "♭", "flat") and not mode)):
            continue
        key = (_pc(letter, acc), mode or "major")
        (strong if (acc or mode) else lone).append((m, key))
    return strong, lone


def _prep(text):
    """Separators a name uses instead of spaces: dots between words, dashes, brackets,
    glued digits ('BassLoop120Am', 'Loop.120.Am', '[140] [Am]')."""
    text = re.sub(r"(?<!\d)\.|\.(?!\d)", " ", text)
    text = re.sub(r"[\[\](){}–—]", " ", text)
    text = re.sub(r"(?<=[A-Za-z]{3})(?=\d{2,3}(?!\d))", " ", text)
    text = re.sub(r"(?<=\d)(?=[A-G](?:#|b|♯|♭)?m?(?![a-z]))", " ", text)
    if re.search(r"[a-z]", text) is None:  # 'BASS LOOP 120 AM': all capitals, so 'AM' is A minor
        text = re.sub(r"(?<![A-Za-z0-9])([A-G](?:#|♯|♭)?)M(?![A-Za-z0-9#])", r"\1m", text)
    return text


def _bare_numbers(text):
    """Bare numbers that may be a tempo: not a model number, a rate or a bit depth."""
    return [m for m in _BARE_RX.finditer(text) if not _GEAR_WORDS.search(text[:m.start()])
            and not _MODEL_PREFIX.search(text[:m.start()])]


def _bare_ok(v):
    return 50 <= v <= 250


def parse_tags(parts, typ):
    """Tags from the file name, then the folders (innermost first).
    parts: path components below the pack folder, file name last.
    A 'BPM' tag anywhere beats a bare number; a bare number counts only beside a real key
    and when it is the only one there (or alone in a folder name); any other bare number
    goes to bpm_bare, to be confirmed by the measured tempo (confirm_tags, audit 2 #6/#10).
    A key with an accidental or a mode beats a lone letter, which counts only as the last
    word of a name that also carries a tempo (blind audit I4/I5)."""
    out = {"bpm": None, "key": None, "note": None, "bpm_bare": [], "key_lone": [], "flat": False}
    loopish = typ not in ("oneshot_melodic", "drum_oneshot")
    texts = [_prep(_clean(raw)) for raw in reversed(parts)]
    for text in texts:
        for m in list(_BPM_RX.finditer(text)) + list(_BPM_PRE_RX.finditer(text)):
            v = float(m.group(1))
            if 40 <= v <= 250:
                out["bpm"] = v
                break
        if out["bpm"] is not None:
            break
    any_bpm = any(_BPM_RX.search(t) or _BPM_PRE_RX.search(t) for t in texts)
    if out["bpm"] is None and loopish:
        key_folder = any(_key_only(t) for t in texts[1:])
        for i, text in enumerate(texts):
            bare = [float(m.group(1)) for m in _bare_numbers(text)]
            only = _ONLY_NUMBER.fullmatch(text) if i else None
            keyed = bool(_key_candidates(text)[0]) or (i == 0 and key_folder)
            if (keyed or only) and len(bare) == 1:
                v = bare[0]
                if 60 <= v <= 200:
                    out["bpm"] = v
                    break
            out["bpm_bare"] += [v for v in bare if _bare_ok(v) and v not in out["bpm_bare"]]
    if out["bpm"] is not None:
        out["bpm_bare"] = []
    if loopish:
        drums = "drum" in typ
        for text in texts:
            strong, lone = _key_candidates(text)
            if strong:
                out["key"] = strong[0][1]
                out["flat"] = _is_flat(strong[0][0])
                break
            if lone and not drums:  # a lone letter on a drum loop is a variation (audit 2 #8)
                m, key = lone[-1]
                head, tail = text[:m.start()], text[m.end():].strip(" -")
                if _VERSION_WORDS.search(head):
                    continue
                if tail == "" and (any_bpm or _BARE_RX.search(head)) and m.group(1) not in "AB":
                    out["key"] = key
                    out["flat"] = _is_flat(m)
                    break
                # 'Synth Loop 120 A' (A/B are variations), 'Pad Loop F', 'Key F', 'Piano Loop C 120':
                # only a guess, kept when the measured key has the same root (confirm_tags, #11)
                if tail == "" or _KEY_WORDS.search(head) or _ONLY_NUMBER.fullmatch(tail):
                    out["key_lone"].append((key, _is_flat(m)))
    if not loopish and parts:
        text = texts[0]
        octaved, lone = [], []
        for m in _ONE_NOTE_RX.finditer(text):
            letter, acc, octave = m.group(1), _acc(m.group(2)), m.group(3)
            if letter.islower() and not octave:
                continue
            if octave:
                octaved.append((_pc(letter, acc), _is_flat(m)))
                continue
            if re.match(r"\s*[-_]?\s*\d", text[m.end():]) or _VERSION_WORDS.search(text[:m.start()]):
                continue  # 'Synth A 01', 'Kit B': an index or a version, not a note
            if not acc and text[m.end():].strip(" -"):
                continue  # 'E Piano', 'G Funk Lead', 'Plan B Lead': a word, not a note (audit 2 #12)
            lone.append((_pc(letter, acc), _is_flat(m)))
        if octaved:
            out["note"] = octaved[-1][0]
            out["flat"] = octaved[-1][1]
        elif lone:
            out["note"] = lone[-1][0]
            out["flat"] = lone[-1][1]
            out["note_lone"] = True  # no octave: checked against the measured note (confirm_tags)
    return out


def _is_flat(m):
    return _acc(m.group(2)) in ("b", "♭", "flat")


def spell(name, flat):
    """'A# minor' -> 'Bb minor' when the name wrote a flat (audit 2 #38)."""
    if not name or not flat:
        return name
    head = re.match(r"[A-G]#?", name)
    return FLAT_NAMES[NOTE_NAMES.index(head.group(0))] + name[head.end():] if head else name


def _key_only(text):
    """A folder named only by a key ('Am', 'F# minor')."""
    strong = _key_candidates(text)[0]
    return len(strong) == 1 and text.strip(" -_") == strong[0][0].group(0).strip(" -_")


def _bpm_near(tag, measured):
    return any(abs(measured * f - tag) <= BPM_CONFIRM_TOL * tag for f in (1.0, 0.5, 2.0))


def confirm_tags(row, tags):
    """Fill the tag cells a name only suggests once the measured sound agrees (audit 2 #6/#10):
    a bare number is the tempo when the measured tempo is equal, half or double (3 %);
    a lone key letter is the key when the measured key has the same root (#11);
    a note without an octave is dropped when the measured note differs (#12);
    a flat written in the name is shown as a flat (#38).
    row: the measure_* output (bpm_tag, bpm_measured...), changed in place; a sure tag stays."""
    meas = row.get("bpm_measured")
    if row.get("bpm_tag") is None and meas:
        near = [v for v in tags.get("bpm_bare") or [] if _bpm_near(v, meas)]
        if len(near) == 1:
            row["bpm_tag"] = near[0]
    kmeas = row.get("key_measured")
    if row.get("key_tag") is None and kmeas and tags.get("key_lone"):
        root = re.match(r"[A-G]#?", kmeas)
        hits = [flat for key, flat in tags["key_lone"]
                if root and NOTE_NAMES[key[0]] == root.group(0)]
        if hits:
            row["key_tag"] = spell(kmeas, hits[-1])
            if "key_match" in row:
                row["key_match"] = "exact"
    elif tags.get("flat"):
        row["key_tag"] = spell(row.get("key_tag"), True)
    nmeas = re.match(r"[A-G]#?", row.get("note_measured") or "")
    if tags.get("note_lone") and row.get("note_tag") and nmeas and row["note_tag"] != nmeas.group(0):
        row["note_tag"] = None  # 'Pad Soft F' that sounds A3 (audit 2 #12)
    if tags.get("flat") and row.get("note_tag"):
        row["note_tag"] = spell(row["note_tag"], True)
    return row


# ================================================================ classify

# whole folder names only: "Vendor Construction Pack" is not a kit root (blind audit I7)
KIT_ROOT = re.compile(r"(?i)^(?:(?:construction|song|melody)[ _-]?kits?|kits?)$")
KIT_SELF = re.compile(r"(?i)^(?:construction|song|melody)[ _-]?kit\b.+")
STEMS_DIR = re.compile(r"(?i)^stems?$")
VOCAL_RX = re.compile(r"(?i)\bvo(x|cals?|ice)\b|vocal|acapella|a capella|\bchant|\bspoken|\bphrase")
FX_RX = re.compile(r"(?i)\bfx\b|sfx|riser|uplifter|downlifter|sweep|whoosh|impact|noise|reverse|\bdrop\b"
                   r"|transition|atmos|texture|foley fx|downer|\bhit fx|swell|crash fx|braam|zap")
LOOP_RX = re.compile(r"(?i)\bloops?\b|\bstems?\b|\bgrooves?\b|\bbeats?\b|\bfills?\b")
DRUM_WORDS = re.compile(r"(?i)drum|\btop\b|\btops\b|kick|snare|clap|hat|perc|groove|beat|break|shaker|"
                        r"\bride\b|cymbal|tom|fill")
MELODIC_WORDS = re.compile(r"(?i)bass|synth|lead|pad|key|piano|chord|pluck|arp|melod|guitar|string|brass|"
                           r"bell|organ|rhodes|stab")
ROLES = [
    ("full", r"\bfull\b|\bmix\b|\bdemo\b|master|full ?track|\bsong\b"),
    ("kick", r"kick\s*drums?|\bbass\s*drums?\b"),
    ("kick", r"\bkicks?\b|\bbd\b"),
    ("snare", r"snare|clap|\bsd\b"),
    ("hats", r"\bhats?\b|hi.?hat|cymbal|\bride\b|shaker|\b[co]?hh\b|\bcrash"),
    ("perc", r"cow\s*bells?"),  # drum words before 808 and bass (defect 23)
    ("drums", r"drums?\s*(?:&|and|n|'n'|\+)\s*bass"),
    ("808", r"808"),
    ("bass", r"bass|\bsub\b|reese"),
    ("perc", r"perc|conga|bongo|\btoms?\b|\brims?\b|shaker|tamb"),
    ("drums", r"drum|\btop\b|\btops\b|\bbeats?\b|\bgrooves?\b|\bbreaks?\b|breakbeat|\bfills?\b"),
    ("vocal", r"vocal|acapella"),
    ("fx", r"\bfx\b|sfx|riser|impact"),
    ("lead", r"lead|melody|hook|\bline\b"),
    ("pad", r"\bpads?\b|drone|ambien"),
    ("keys", r"\bkeys\b|piano|rhodes|organ|wurli|\bep\b|\bkey\b"),
    ("chords", r"chord|stab"),
    ("pluck", r"pluck"),
    ("arp", r"\barp"),
    ("guitar", r"guitar|gtr"),
    ("strings", r"string|violin|cello|orch"),
    ("brass", r"brass|horn|trumpet|sax"),
    ("bell", r"bell|mallet|marimba|kalimba|glock"),
    ("synth", r"synth|saw|\bsquare|\bsupersaw"),
    ("vocal", r"\bvox\b|voice|chant"),  # after the instruments: 'Vox Lead' is a lead (defect 9)
    ("fx", r"sweep|noise|atmos|texture"),  # 'Atmosphere Pad' is a pad
]
ROLES = [(r, re.compile(rx, re.I)) for r, rx in ROLES]
GENRES = [("techno", r"techno"), ("house", r"house"), ("dnb", r"dnb|drum ?(and|&|n) ?bass|jungle"),
          ("dubstep", r"dubstep|riddim|deathstep"), ("trap", r"trap"), ("hiphop", r"hip.?hop|boom.?bap"),
          ("lofi", r"lo.?fi|chillhop"), ("phonk", r"phonk"), ("trance", r"trance"), ("rave", r"rave|keta"),
          ("amapiano", r"amapiano"), ("afro", r"afro"), ("garage", r"garage|ukg|2.?step"),
          ("edm", r"edm|big ?room|future ?bass|electro"), ("pop", r"\bpop\b"), ("rnb", r"r ?n ?b|r&b|soul"),
          ("ambient", r"ambient|cinematic"), ("rock", r"rock"), ("reggaeton", r"reggaeton|dembow"),
          ("drill", r"drill"), ("disco", r"disco"), ("funk", r"funk"), ("hardstyle", r"hardstyle|hardcore|gabber")]
GENRES = [(g, rx.replace("afro", "afro(?:beats?|house)?") if g == "afro" else rx) for g, rx in GENRES]
# A style is a whole word ('Warehouse', 'Strap', 'Soulful', 'Entrance' name no style), plural allowed.
GENRES = [(g, re.compile(r"(?<![a-z])(?:%s)s?(?![a-z])" % rx, re.I)) for g, rx in GENRES]
_PACK_STYLES = [(re.compile(r"(?<![a-z])%s(?![a-z])" % re.escape(k), re.I), st) for k, st in mine_or.STYLES]
_STYLE_NOISE = re.compile(r"(?i)jungle ?perc\w*|garage ?doors?")
_CAMEL = re.compile(r"(?<=[a-z])(?=[A-Z])")


def role_of(text):
    for r, rx in ROLES:
        if rx.search(text):
            return r
    return "-"


def _style_in(part):
    """Style named by one path component: a known pack name first, else the genre word that ends last
    ('Hip-Hop & Trap' -> trap, 'Lo-Fi House' -> house)."""
    part = _STYLE_NOISE.sub(" ", part)
    for text in (part, _CAMEL.sub(" ", part)):
        for rx, st in _PACK_STYLES:
            if rx.search(text):
                return st
        best = None
        for g, rx in GENRES:
            for m in rx.finditer(text):
                if best is None or m.end() > best[0]:
                    best = (m.end(), g)
        if best:
            return best[1]
    return None


def style_of(rel, pack=None):
    """Innermost component first ('Techno/House Kicks' -> house); the pack folder's own name last."""
    parts = [p for p in re.split(r"[\\/]", rel) if p]
    if pack:
        parts = [os.path.basename(os.path.normpath(pack))] + parts
    for part in reversed(parts):
        st = _style_in(part)
        if st:
            return st
    return "unlabelled"


def kit_split(parts):
    """(index of the kit folder in parts, kit folder name) or (None, None)."""
    folders = parts[:-1]
    for i, p in enumerate(folders):
        if KIT_ROOT.search(p) and i + 1 < len(folders):
            return i + 1, folders[i + 1]
        if KIT_SELF.search(p):
            return i, p
        if STEMS_DIR.search(p) and i >= 1:
            return i - 1, folders[i - 1]
    return None, None


# finer drum families the shared rules miss (blind audit I6)
EXTRA_RULES = [(f, re.compile(r, re.I)) for f, r in [
    ("kick", r"\bkicks?\b.*\b808\b|\btr\W?808\b.*\bkicks?\b"),  # 'Kick 808 Style', 'TR-808 Kick' (defect 23)
    ("kick", r"\bbass\s*drums?\b|\bkick\s*drums?\b"),
    ("hat_open", r"\bo ?hh\b|(?:hi.?hats?|\bhats?|\bhh)\s*open|open\s*(?:hi.?hats?|hats?|hh)\b"),
    ("hat_closed", r"\bc ?hh\b|(?:hi.?hats?|\bhats?|\bhh)\s*closed|closed\s*(?:hi.?hats?|hats?|hh)\b"),
    ("perc_high", r"\bperc(?:ussion)?s?\s*(?:high|hi)\b|\b(?:high|hi)\s*percs?\b"),
    ("perc_low", r"\bperc(?:ussion)?s?\s*low\b|\blow\s*percs?\b"),
    ("perc_mid", r"\bperc(?:ussion)?s?\s*(?:mid|middle)\b|\b(?:mid|middle)\s*percs?\b"),
    ("hat", r"\bhh\b")]]
LONG_FAMILIES = ("kick_808", "crash", "cymbal", "ride")


def drum_family(name, folders):
    """Drum family from the name, else from the folders, innermost first (blind audit B1)."""
    if isinstance(folders, str):
        folders = [folders]
    texts = [re.sub(r"[_\-]", " ", t) for t in [name] + list(reversed(folders))]
    for nm in texts:
        for f, rx in EXTRA_RULES + mine_or.RULES:
            if rx.search(nm):
                return _refine(f, texts)
    return None


# a bare 'hat' or 'perc' takes its kind from a short word in the name or a folder (audit 2, defects 1, 24)
REFINE = {"hat": [("hat_open", re.compile(r"(?i)\bopen(?:ed)?\b|\bop\b|\bouverte?s?\b")),
                  ("hat_closed", re.compile(r"(?i)\bclosed?\b|\bcl\b|\bpedal\b|\bferm[ée]e?s?\b"))],
          "perc": [("perc_high", re.compile(r"(?i)\bhigh\b|\bhi\b")),
                   ("perc_mid", re.compile(r"(?i)\bmid(?:dle)?\b")),
                   ("perc_low", re.compile(r"(?i)\blow\b"))]}


def _refine(fam, texts):
    for nm in texts:  # name first, then the folders, innermost first
        for f, rx in REFINE.get(fam, ()):
            if rx.search(nm):
                return f
    return fam


# a single note with its octave ('C2', 'F#3') and long held sounds (defects 25, 26)
NOTE_OCT_RX = re.compile(r"(?<![A-Za-z0-9])[A-G](?:#|b|s)?-?[0-8](?![0-9])")
SUSTAIN_RX = re.compile(r"(?i)\bpads?\b|\bdrones?\b|\bambien\w*|\bsustain\w*|\blong\b|"
                        r"(?<![A-Za-z0-9])[A-G](?:#|b)?(?:m|min|maj)?(?![A-Za-z0-9])")
ONESHOT_DIR = re.compile(r"(?i)one.?shots?|\bhits?\b|\bshots\b")
WEAK_LOOP_RX = re.compile(r"(?i)\bbeats?\b|\bfills?\b")  # also one-shot words: a 'fill' hit, a 'beat' pack


def _loop_word(t, dur):
    if not LOOP_RX.search(t):
        return False
    strong = LOOP_RX.search(WEAK_LOOP_RX.sub(" ", t)) is not None
    return strong or dur is None or dur >= 1.5


# loops whose name has no 'loop': a tempo or a loop-only word (audit 2, defect 4)
LOOP_NAME_RX = re.compile(r"(?i)\bmelod(?:y|ies)\b|\bbasslines?\b|\bbreaks?\b|\bbreakbeats?\b|\btops?\b|\bgrooves?\b")
# whole words only: 'Organic', 'Sharp', 'Monkey', 'What', 'Custom', 'Upbeat' say nothing (defect 5)
DRUM_W = re.compile(r"(?i)\b(?:hi.?|super)?(?:hats?|hihats?|drums?|kicks?|kickdrums?|snares?|claps?|percs?|percussions?|"
                    r"grooves?|beats?|breaks?|breakbeats?|shakers?|rides?|cymbals?|toms?|fills?|hh)\b")
MELODIC_W = re.compile(r"(?i)\b(?:bass(?:es|lines?)?|synths?|leads?|pads?|keys|piano|pianos|chords?|plucks?|arps?|"
                       r"arpeggi\w*|melod\w*|guitars?|strings?|brass|bells?|organs?|rhodes|stabs?|reese)\b")
_DNB_RX = re.compile(r"(?i)\bdrums?\s*(?:&|and|n|'n'|\+)\s*bass\b")


def _loop_by_name(name, dur):
    if dur is None or dur < 1.5:
        return False
    if LOOP_NAME_RX.search(name):
        return True
    if NOTE_OCT_RX.search(name) and dur < 4.0:  # 'Lead C3 127': a velocity next to a played note
        return False
    return any(60 <= int(m.group(1)) <= 200 for m in _BARE_RX.finditer(name)) and not re.search(r"808|909", name)


def _loop_kind(name, folders):
    """drum_loop / melodic_loop from whole words of the name, else of the nearest folder or of a folder that names
    its loops ('Drum Loops/Techno'); None: the sound decides (a pack name like 'Hat Tricks' says nothing)."""
    near = folders[-1:]
    for t in [name] + near + [f for f in reversed(folders[:-1]) if LOOP_RX.search(f)]:
        t = _DNB_RX.sub(" drum ", t)
        d, m = DRUM_W.search(t), MELODIC_W.search(t)
        if d or m:
            return "drum_loop" if d and not m else "melodic_loop"
    return None


def _drumminess(x):
    """Energy-weighted spectral flatness: drum loops are noisy (about 0.04), notes are peaky (below 1e-6)."""
    sig = _mono(x)
    if len(sig) < 4096:
        return 0.0
    m = _stft_mag(sig, 2048, 1024) ** 2 + 1e-12
    e = m.sum(axis=1)
    keep = e > e.max() * 1e-3
    m, e = m[keep], e[keep]
    fl = np.exp(np.mean(np.log(m), axis=1)) / np.mean(m, axis=1)
    return float(np.sum(fl * e) / np.sum(e))


DRUMMY_FLATNESS = 0.02


# Defect 9: fx and vocal words only describe a sound when no instrument word says otherwise.
STRONG_FX_RX = re.compile(r"(?i)\bfx\b|sfx|riser|uplifter|downlifter|whoosh|impact|transition|downer|braam|zap")
REVERSE_RX = re.compile(r"(?i)\breverse|\brev\b")


def _fx_named(name, folders):
    """An fx word was found: False when an instrument word in the name says otherwise."""
    if MELODIC_W.search(name):  # 'Bass Sweep', 'Texture Pad', but 'Piano Riser' is still fx
        return STRONG_FX_RX.search(name) is not None
    return True


def _vocal_named(name, folders):
    """True for a voice: 'vocal' anywhere, or 'vox' / 'voice' / 'chant' / 'phrase' without an instrument word."""
    if re.search(r"(?i)\bvocoder\b|vox ?synth", name) or drum_family(name, []) is not None:
        return False  # 'Vocal Perc' is a percussion hit
    if any(VOCAL_RX.search(f) for f in folders):
        return True
    strong = re.search(r"(?i)vocal|acapella|a capella|\bspoken", name)
    if strong:
        return True
    if not VOCAL_RX.search(name) or MELODIC_W.search(name):
        return False  # 'Vox Lead', 'Voice Pad', 'Guitar Phrase'
    if re.search(r"(?i)\bvo(x|ice)\b", name):
        return True
    return not LOOP_RX.search(name)  # 'Chanting Monks Loop' is a melodic loop, 'Chant 01' a voice


def _loop_named(name, folders, dur):
    """A loop word in the name, or in a folder that no deeper folder contradicts (audit 2, defect 3):
    'Loops & One Shots/Kicks', 'Trap Beats/One Shots', 'Beat Makers Pack/Hats' hold one-shots."""
    if _loop_word(name, dur):
        return True
    for i, f in enumerate(folders):
        if not _loop_word(f, dur) or ONESHOT_DIR.search(f):
            continue
        deeper = folders[i + 1:]
        if any(ONESHOT_DIR.search(g) for g in deeper):
            continue
        if (dur is None or dur < 1.5) and any(drum_family(g, []) is not None for g in deeper):
            continue
        return True
    return False


def classify(rel, dur):
    """rel: path inside the pack (with or without the pack folder).
    dur: length in seconds (None for MIDI). Returns type/subtype/role/style."""
    parts = [p for p in re.split(r"[\\/]+", rel) if p]
    name = _clean(parts[-1])
    folders = parts[:-1]
    inner = "/".join(folders)  # every folder counts: "Vocals/Dry" is still a vocal (blind audit B1)
    text = (inner + "/" + name)
    style = style_of("/".join(parts))
    role = role_of(name)
    if role == "-":
        for f in reversed(folders):
            role = role_of(f)
            if role != "-":
                break
    out = {"type": None, "subtype": "-", "role": role, "style": style}
    if parts[-1].lower().endswith(MIDI_EXTS):
        out["type"] = "midi"
        return out
    looped_any = LOOP_RX.search(name) is not None
    ki, _ = kit_split(parts)
    if ki is not None:
        below = folders[ki + 1:]
        short_drum = drum_family(name, []) is not None and dur is not None and dur < 6.0
        one_note = NOTE_OCT_RX.search(name) and not looped_any and not _BPM_RX.search(name) \
            and not _loop_by_name(name, dur)  # 'Bass C2' is one note, 'Bass 120 C2' a stem (defect 26)
        if not short_drum and not one_note and not any(ONESHOT_DIR.search(f) for f in below):
            out["type"] = "kit_stem"
            return out
    if VOCAL_RX.search(text) and _vocal_named(name, folders):
        out["type"] = "vocal"
        out["role"] = "vocal"
        return out
    looped = _loop_named(name, folders, dur)
    drum_named = drum_family(name, []) is not None or (DRUM_WORDS.search(name) and looped)
    if drum_named and REVERSE_RX.search(name) and drum_family(name, []) in ("crash", "cymbal", "ride"):
        drum_named = False  # a reversed cymbal is a riser (defect 9)
    if FX_RX.search(text) and not drum_named:  # 'Kick Impact', 'Noise Hat Loop' stay drums (I7)
        if _fx_named(name, folders):
            out["type"] = "fx"
            return out
    has_bpm = _BPM_RX.search(name) is not None
    is_loop = looped or (has_bpm and dur is not None and dur >= 1.5) or _loop_by_name(name, dur)
    if is_loop and not looped and re.search(r"808", name):  # '808 Long 140bpm C' is a one-shot
        is_loop = False
    melodic = MELODIC_WORDS.search(name) or (not DRUM_WORDS.search(name) and MELODIC_WORDS.search(inner))
    if is_loop:
        drummy = DRUM_WORDS.search(name) or (not MELODIC_WORDS.search(name) and DRUM_WORDS.search(inner))
        out["type"] = "drum_loop" if drummy and not melodic else "melodic_loop"
        kind = _loop_kind(name, folders)
        if kind is not None:
            out["type"] = kind
        else:
            out["subtype"] = "by_sound"  # a guess until _measure_file hears it
        return out
    fam = drum_family(name, folders)
    if fam is not None and (dur is None or dur < (15.0 if fam in LONG_FAMILIES else 6.0)):  # an 11 s 808 or crash (defect 25)
        if fam == "kick_808" or drum_family(name, []) is not None or not re.search(r"(?i)bass|synth|lead|pad|key|piano|chord|pluck", name + "/" +
                                              (folders[-1] if folders else "")):
            sub = fam
            if fam == "kick" and re.search(r"808", inner):
                if any(re.fullmatch(r"(?i)808s?(?: ?(?:kicks?|bass(?:es)?|subs?))?", f.strip()) for f in folders):
                    sub = "kick_808"  # 'TR-808/Kick 01', 'Kit 808/Kick 01' are kicks (defect 23)
            layer = re.search(r"(?i)\b(attack|tail|click|body|sub)\b", name)
            if layer and layer.group(1).lower() in ("attack", "tail"):
                sub += ":" + layer.group(1).lower()
            out["type"] = "drum_oneshot"
            out["subtype"] = sub
            return out
    if fam is None and WEAK_LOOP_RX.search(name) and DRUM_WORDS.search(inner) and not MELODIC_WORDS.search(name) \
            and dur is not None and dur < 1.5:  # a short 'Solo Fill' hit: a drum without a family (defect 3)
        out["type"] = "drum_oneshot"
        return out
    if (dur is not None and dur < 8.0) or ONESHOT_DIR.search(inner):
        out["type"] = "oneshot_melodic"
        return out
    if not LOOP_RX.search(name) and not has_bpm and (SUSTAIN_RX.search(name) or NOTE_OCT_RX.search(name)):
        out["type"] = "oneshot_melodic"  # a 9 s 'Long Pad Cm', a 12 s 'Drone Dark' (defect 25)
        return out
    out["type"] = "melodic_loop"
    out["subtype"] = "untagged"
    return out


# ================================================================ common

def _biquad_resp(f, sr, kind):
    if kind == "shelf":
        f0, g, q = 1681.974450955533, 3.999843853973347, 0.7071752369554196
        k = math.tan(math.pi * f0 / sr)
        vh = 10 ** (g / 20.0)
        vb = vh ** 0.4996667741545416
        a0 = 1.0 + k / q + k * k
        b = [(vh + vb * k / q + k * k) / a0, 2.0 * (k * k - vh) / a0, (vh - vb * k / q + k * k) / a0]
        a = [1.0, 2.0 * (k * k - 1.0) / a0, (1.0 - k / q + k * k) / a0]
    else:
        f0, q = 38.13547087602444, 0.5003270373238773
        k = math.tan(math.pi * f0 / sr)
        a0 = 1.0 + k / q + k * k
        b = [1.0, -2.0, 1.0]
        a = [1.0, 2.0 * (k * k - 1.0) / a0, (1.0 - k / q + k * k) / a0]
    z = np.exp(-2j * np.pi * f / sr)
    return (b[0] + b[1] * z + b[2] * z * z) / (a[0] + a[1] * z + a[2] * z * z)


def k_weight(x, sr):
    n = x.shape[0]
    nf = 1 << int(math.ceil(math.log2(max(n, 2))))
    f = np.fft.rfftfreq(nf, 1.0 / sr)
    h = _biquad_resp(f, sr, "shelf") * _biquad_resp(f, sr, "hp")
    return np.fft.irfft(np.fft.rfft(x, nf, axis=0) * h[:, None], nf, axis=0)[:n]


def lufs(x, sr):
    y = k_weight(x[: int(sr * 120)], sr)  # 2 minutes are plenty for a loudness figure
    p = np.sum(y ** 2, axis=1)
    blk = int(round(0.4 * sr))
    if len(p) < blk:
        z = float(np.mean(p))
        return -0.691 + 10 * math.log10(z) if z > 0 else None
    hop = blk // 4
    c = np.concatenate([[0.0], np.cumsum(p)])
    starts = np.arange(0, len(p) - blk + 1, hop)
    z = (c[starts + blk] - c[starts]) / blk
    lk = np.where(z > 0, -0.691 + 10 * np.log10(np.maximum(z, 1e-30)), -np.inf)
    z1 = z[lk > -70]
    if len(z1) == 0:
        return None
    rel = -0.691 + 10 * math.log10(np.mean(z1)) - 10
    z2 = z[(lk > -70) & (lk > rel)]
    if len(z2) == 0:
        return None
    return -0.691 + 10 * math.log10(np.mean(z2))


def common(sr, x):
    p = np.mean(x ** 2, axis=1)  # mean power per channel: two channels are not twice as loud (I12)
    pk = float(np.max(np.abs(x)))
    return {"dur_s": x.shape[0] / sr, "sr": sr, "ch": x.shape[1],
            "peak_dbfs": 20 * math.log10(pk) if pk > 0 else None,
            "rms_dbfs": _db(float(np.mean(p))),
            "lufs_i": lufs(x, sr), **end_cut(sr, x)}



def _lpc(seg, order):
    """Least-squares linear predictor of seg (coefficients for x[t-1..t-order])."""
    rows = np.lib.stride_tricks.sliding_window_view(seg, order + 1)
    a, *_ = np.linalg.lstsq(rows[:, :-1][:, ::-1], rows[:, -1], rcond=None)
    return a


def _predict(hist, a, n):
    order = a.shape[0]
    buf = list(hist[-order:])
    out = np.empty(n)
    for i in range(n):
        v = float(np.dot(a, buf[::-1][:order]))
        out[i] = v
        buf.append(v)
        buf.pop(0)
    return out


def _short_fade(s, sr):
    """Length in ms of a fade of 4-15 ms at the end, or None (blind audit I1).
    Tonal sounds: the last 15 ms are compared, 1 ms at a time, to their
    prediction from the 65 ms before. Noise, which cannot be predicted, falls
    back on the energy of its last 3 ms (second audit, defect 15)."""
    lpc = _short_fade_lpc(s, sr)
    return lpc if lpc is not None else _short_fade_noise(s, sr)


def _short_fade_noise(s, sr):
    """Fade length in ms on noise-like sounds, or None when the method does not apply.

    A linear fade of L ms on noise of power p leaves E = p * t^3 / (3 L^2) in
    the last t = 3 ms (L >= t), p * (t - 2L/3) otherwise; an abrupt stop leaves
    p * t. p is the trend of 3 ms windows 45-15 ms before the end, extrapolated.
    The method only applies when no 3 ms window of that reference region falls
    under 0.4 of the trend: low tones (a kick) dip at every zero crossing."""
    n3 = max(2, int(round(0.003 * sr)))
    h = max(1, n3 // 6)
    n = s.shape[0]
    a, b = n - int(round(0.045 * sr)), n - int(round(0.015 * sr))
    if a < 0:
        return None
    e = s * s
    c = np.concatenate([[0.0], np.cumsum(e)])
    st = np.arange(a, b - n3 + 1, h)
    win = (c[st + n3] - c[st]) / n3
    if win.size < 8 or float(np.min(win)) <= 0.0:
        return None
    tc = (n - (st + n3 / 2.0)) * 1000.0 / sr  # ms before the end
    slope, icpt = np.polyfit(tc, np.log(win), 1)
    trend = np.exp(slope * tc + icpt)
    if float(np.min(win / trend)) < 0.4:
        return None
    p = math.exp(slope * (n3 / 2.0 * 1000.0 / sr) + icpt)
    r = float(c[n] - c[n - n3]) / n3 / p
    t = n3 * 1000.0 / sr
    if r >= 1.0 / 3.0:
        est = max(0.0, 1.5 * t * (1.0 - r))
    else:
        est = t / math.sqrt(3.0 * max(r, 1e-9))
    # Guessed margin: on the audit fuzz a 5 ms fade reads 3.9-6.7 ms and a
    # 2 ms fade at most 3.2 ms, so 0.5 ms moves the 4 ms limit between them.
    return est + 0.5 if est > 0.0 else est


def _short_fade_lpc(s, sr):
    """_short_fade on tonal sounds (LPC prediction), or None."""
    n15, n80, n1 = int(0.015 * sr), int(0.080 * sr), max(1, int(round(0.001 * sr)))
    if s.shape[0] < n80 + n15:
        return None
    seg = s[-n80:-n15]
    if float(np.sqrt(np.mean(seg ** 2))) < 10 ** (-60 / 20):
        return None
    try:
        a = _lpc(seg, 24)
    except np.linalg.LinAlgError:
        return None
    pred = _predict(seg, a, n15)
    act = s[-n15:]
    blocks = n15 // n1
    ratio = []
    for b in range(blocks):
        pe = float(np.sqrt(np.mean(pred[b * n1:(b + 1) * n1] ** 2)))
        ae = float(np.sqrt(np.mean(act[b * n1:(b + 1) * n1] ** 2)))
        if pe < 10 ** (-60 / 20):
            return None  # nothing tonal to predict (noise, silence)
        ratio.append(20 * math.log10(ae / pe + 1e-9))
    ratio = np.array(ratio)
    if abs(float(np.median(ratio[:3]))) > 3.0 or ratio[-1] > -12.0:
        return None  # the prediction does not hold, or the end is not faded out
    start = int(np.nonzero(ratio > -1.5)[0][-1]) + 1 if np.any(ratio > -1.5) else 0
    seg_r = ratio[start:]
    if seg_r.size >= 2 and float(np.max(np.diff(seg_r))) > 3.0:
        return None
    return float(blocks - start)


def _seamless(s, sr, x0):
    """True when a sustained file of 1 s or more continues into its own start (a loop)."""
    n80, n5 = int(0.080 * sr), int(0.005 * sr)
    if s.shape[0] < sr or float(np.sqrt(np.mean(s[-n80:] ** 2))) < 10 ** (-40 / 20):
        return False
    try:
        a = _lpc(s[-n80:], 24)
    except np.linalg.LinAlgError:
        return False
    pred = _predict(s[-n80:], a, n5)
    err = float(np.sqrt(np.mean((pred - x0[:n5]) ** 2)))
    lvl = float(np.sqrt(np.mean(x0[:n5] ** 2))) + 1e-12
    return err < 0.1 * lvl


def _past_floor(s, sr):
    """The channel up to where its sound ends, when it ends in a steady floor.

    Floor: the two 20 ms halves of the last 40 ms within 3 dB of each other
    and 30 dB or more under the peak (hiss, hum, dither, a slow natural tail).
    The sound ends at the last sample 20 dB over the floor; if the 5 ms before
    it are 30 dB or more over the floor the sound was stopped there (a cut
    hidden under the floor, second audit defect 16) and the channel is cut
    there; otherwise it decayed into the floor and None is returned (not cut,
    defect 30). Without a floor the channel is returned unchanged. Guessed
    limits: 3, 20 and 30 dB.
    """
    n20 = int(round(0.020 * sr))
    n5 = max(1, int(round(0.005 * sr)))
    if s.shape[0] < 4 * n20:
        return s
    r1 = math.sqrt(float(np.mean(s[-2 * n20:-n20] ** 2)))
    r2 = math.sqrt(float(np.mean(s[-n20:] ** 2)))
    peak = float(np.max(np.abs(s)))
    fl = math.sqrt(0.5 * (r1 * r1 + r2 * r2))
    if min(r1, r2) <= 0.0 or abs(20 * math.log10(r1 / r2)) > 3.0 or fl > peak * 10 ** (-30 / 20):
        return s
    over = np.nonzero(np.abs(s) > 10.0 * fl)[0]
    if over.size == 0:
        return None
    k = int(over[-1])
    if float(np.max(np.abs(s[max(0, k - n5):k + 1]))) >= fl * 10 ** (30 / 20):
        return s[:k + 1]
    return None


def _quant_step(x):
    """The step of a coarse integer quantization (8 to 16 bit), or None."""
    for bits in range(8, 17):
        v = x * float(1 << (bits - 1))
        if float(np.max(np.abs(v - np.round(v)))) < 1e-6:
            return 1.0 / float(1 << (bits - 1))
    return None


def _end_one(s, sr):
    """(cut, fade_ms or None) for one channel."""
    x0 = s
    nz = np.nonzero(np.abs(s) > 10 ** (-90 / 20))[0]
    if nz.size == 0:
        return 0, None
    s = s[:int(nz[-1]) + 1]  # trailing digital silence hides nothing (blind audit I2)
    # DC offset is not a step (I2). Only a steady offset is removed: the mean of
    # the last 100 ms of a short burst that fades out is the burst's own
    # leftover, and subtracting it left a constant step at the end (re-check 5).
    # Steady = the last 1 ms sits on the same offset as the last 100 ms.
    dc = float(np.mean(s[-int(0.1 * sr):]))
    dc_end = float(np.mean(s[-max(1, int(0.001 * sr)):]))
    if abs(dc_end - dc) <= 0.5 * abs(dc):
        s = s - dc
    # The levels below are relative to the channel's peak, so a quiet file cut
    # hard reads as cut (second audit, defect 17). Gain capped at +50 dB; a
    # channel whose peak is under -70 dBFS is noise, not a sound (both guessed).
    peak = float(np.max(np.abs(s)))
    if peak * 10 ** (70 / 20) < 1.0:
        return 0, None
    g = min(1.0 / peak, 10 ** (50 / 20))
    qs = _quant_step(x0)
    s, x0 = s * g, x0 * g
    s = _past_floor(s, sr)
    if s is None:
        return 0, None
    if qs is not None and float(np.max(np.abs(s[-max(1, int(round(0.005 * sr))):]))) <= 2.0 * qs * g:
        return 0, None  # the sound faded into its last quantization steps (second audit, defect 30)
    n = s.shape[0]
    n1 = max(1, int(round(0.001 * sr)))
    n5 = max(2, int(round(0.005 * sr)))
    a = np.abs(s)
    if n < 2 * n5:
        # under 10 ms there is no fade to measure: cut when the last 1 ms is
        # still within 20 dB of the peak (second audit, defect 29; guessed)
        if n < 2 * n1:
            return 0, None
        r1 = math.sqrt(float(np.mean(s[-n1:] ** 2)))
        return (1 if r1 >= float(np.max(a)) * 10 ** (-20 / 20) else 0), None
    if float(np.max(a[-min(n, int(0.1 * sr)):])) < 10 ** (-90 / 20):
        return 0, None
    # final fade: forward-window peak envelope (10 ms) every 0.5 ms over the last 200 ms
    w = max(1, int(round(0.010 * sr)))
    h = max(1, int(round(0.0005 * sr)))
    span = min(n, int(round(0.2 * sr)))
    starts = np.arange(n - span, n - n1 + 1, h)
    env = np.array([float(np.max(a[i:min(i + w, n)])) for i in starts]) + 1e-12
    edb = 20 * np.log10(env)
    tms = (n - starts) * 1000.0 / sr  # ms before the end
    end_db = 20 * math.log10(float(np.max(a[-n1:])) + 1e-12)
    trend = tms >= 60.0
    if np.count_nonzero(trend) >= 4 and tms[0] - 60.0 >= 15.0:
        slope, icpt = np.polyfit(tms[trend], edb[trend], 1)
        ref = slope * tms + icpt
    else:
        ref = np.full_like(edb, float(np.max(edb)))
    fade_ms = None
    near = np.nonzero(edb >= ref - 1.5)[0]
    if near.size:
        i0 = int(near[-1])
        seg = edb[i0:]
        drop = float(edb[i0]) - end_db
        rises = float(np.max(np.diff(seg))) if seg.size > 1 else 0.0
        if drop >= 20.0 and rises <= 3.0:
            fade_ms = float(tms[i0])
    # (a) step to silence bigger than the waveform's own motion
    last = float(a[-1])
    motion = float(np.median(np.abs(np.diff(s[-n5:]))))
    rms5 = float(np.sqrt(np.mean(s[-n5:] ** 2)))
    # noise moves as much as its own level: a random last sample is no step (blind audit I2)
    step = last > 10 ** (-60 / 20) and last > 4.0 * motion and motion < 0.3 * rms5
    # (b) still loud 5 ms before the end with no fade of 5 ms or more
    c = n - n5
    lo, hi = max(0, c - n1 // 2), min(n, c + (n1 + 1) // 2)
    lvl = math.sqrt(float(np.mean(s[lo:hi] ** 2)))
    if fade_ms is None or fade_ms < 4.0:
        sf = _short_fade(s, sr)
        if sf is not None and sf >= 4.0:
            fade_ms = sf
    faded = fade_ms is not None and fade_ms >= 4.0
    loud = lvl > 10 ** (-45 / 20) and not faded
    cut = (step and not faded) or loud
    if cut and _seamless(s, sr, x0):
        cut = False
    return (1 if cut else 0), fade_ms


def end_cut(sr, x):
    """Is the file cut (click or abrupt stop) at its end?

    Each channel is read alone, with its levels relative to the channel's
    peak (gain capped at +50 dB; a peak under -70 dBFS is not a sound).
    end_cut = 1 when a channel either ends on a step: |last sample| above
    -60 dB and over 4 times the median sample-to-sample motion of its last
    5 ms; or is still above -45 dB (1 ms RMS) 5 ms before the end without a
    final fade of 4 ms or more. Fade: the 10 ms peak envelope leaves the
    natural decay trend (fitted 200-60 ms before the end, within 1.5 dB) and
    drops 20 dB or more to the last 1 ms, never rising by more than 3 dB; on
    noise, the energy of the last 3 ms falling below its 45-15 ms trend also
    counts. end_fade_ms is its length (loudest channel), empty when there is
    none. Trailing digital silence and a steady DC offset are ignored. A
    steady noise floor (hiss, hum, dither) at the end is ignored too: the
    sound ends where it last rose 20 dB above that floor, and it is cut only
    if that point is 30 dB above the floor; a sound that faded into the floor
    or into its last quantization steps is not cut. A file under 10 ms is cut
    when its last 1 ms is within 20 dB of its peak. A channel that went silent
    10 ms or more before the others is not cut. A seamless loop is not cut.
    A reversed sound (reverse cymbal, riser) ends loud: it reads as cut by
    design, the column says "abrupt end", not "mistake".
    """
    if x.ndim == 1:
        x = x[:, None]
    # a channel that went silent 10 ms or more before the others ended early on
    # purpose: the file ends where its last channel ends (second audit, defect 30)
    ends = [np.nonzero(np.abs(x[:, c]) > 10 ** (-90 / 20))[0] for c in range(x.shape[1])]
    last = max((int(e[-1]) for e in ends if e.size), default=0)
    early = [e.size == 0 or int(e[-1]) < last - int(round(0.01 * sr)) for e in ends]
    res = [(0, None) if early[c] else _end_one(x[:, c].astype(np.float64), sr) for c in range(x.shape[1])]
    tail = max(1, int(round(0.05 * sr)))
    loud = int(np.argmax(np.mean(x[-tail:] ** 2, axis=0)))
    return {"end_cut": max(r[0] for r in res), "end_fade_ms": res[loud][1]}


# ================================================================ pitch

def yin(sig, sr, fmin=30.0, fmax=2000.0, thr=0.15):
    """Per frame f0 (Hz or nan) and frame rms. 40 ms+ frames, 10 ms hop."""
    tmax = int(sr / fmin)
    tmin = max(2, int(sr / fmax))
    w = max(int(0.04 * sr), 2 * tmax)
    hop = int(0.01 * sr)
    if len(sig) < w + tmax:
        sig = np.concatenate([sig, np.zeros(w + tmax - len(sig))])
    fr = _frames(sig, w + tmax, hop)
    nf = 1 << int(math.ceil(math.log2(2 * (w + tmax))))
    f0 = np.full(fr.shape[0], np.nan)
    rms = np.sqrt(np.mean(fr[:, :w] ** 2, axis=1))
    loud = rms > max(np.max(rms) * 10 ** (-35 / 20.0), 1e-6)
    for i in np.nonzero(loud)[0]:
        x = fr[i]
        spec = np.fft.rfft(x, nf)
        win = np.fft.rfft(x[:w], nf)
        acf = np.fft.irfft(spec * np.conj(win), nf)[:tmax + 1]
        c = np.concatenate([[0.0], np.cumsum(x ** 2)])
        e0 = c[w]
        et = c[np.arange(tmax + 1) + w] - c[np.arange(tmax + 1)]
        d = e0 + et - 2 * acf
        d[0] = 0
        cm = np.cumsum(d[1:])
        dn = np.ones_like(d)
        dn[1:] = d[1:] * np.arange(1, tmax + 1) / np.maximum(cm, 1e-20)
        cand = np.nonzero(dn[tmin:tmax] < thr)[0]
        if len(cand):
            t = cand[0] + tmin
            while t + 1 < tmax and dn[t + 1] < dn[t]:
                t += 1
        else:
            t = int(np.argmin(dn[tmin:tmax])) + tmin
            if dn[t] > 0.35:
                continue
        if 1 <= t < tmax:
            a, b, cc = dn[t - 1], dn[t], dn[t + 1]
            den = a - 2 * b + cc
            t = t + (0.5 * (a - cc) / den if den > 0 else 0.0)
        f0[i] = sr / t
    return f0, rms


def median_f0(sig, sr, fmin=30.0, fmax=2000.0):
    f0, rms = yin(sig, sr, fmin, fmax)
    ok = np.isfinite(f0)
    if ok.sum() < 2:
        return None, f0
    # weight the louder half of the voiced frames
    keep = ok & (rms >= np.median(rms[ok]) * 0.5)
    return float(np.median(f0[keep])), f0


# ================================================================ one-shot melodic

def _envelope_db(p, sr, win, hop_s=None):
    """Level in dB of the power p smoothed by two boxcars of length win
    (a triangle of 2*win): one period cancels the tone, the second the
    ripple a ramp leaves. Returns centre times and levels."""
    n = max(1, int(round(win * sr)))
    hop = max(1, int(hop_s * sr)) if hop_s else max(1, n // 4)
    pp = np.concatenate([np.zeros(n), p, np.zeros(n)])
    c1 = np.concatenate([[0.0], np.cumsum(pp)])
    b1 = (c1[n:] - c1[:-n]) / n
    c2 = np.concatenate([[0.0], np.cumsum(b1)])
    starts = np.arange(0, len(b1) - n + 1, hop)
    e = (c2[starts + n] - c2[starts]) / n
    t = (starts + n - n) / float(sr)  # centre of the triangle, in input samples
    keep = (t >= 0) & (t <= len(p) / float(sr))
    return t[keep], 10 * np.log10(np.maximum(e[keep], 1e-20))


def _centroid(sig, sr):
    if len(sig) < 64:
        return None
    n = 1 << int(math.ceil(math.log2(len(sig))))
    s = np.abs(np.fft.rfft(sig * np.hanning(len(sig)), n)) ** 2
    f = np.fft.rfftfreq(n, 1.0 / sr)
    m = (f >= 20) & (f <= 20000)
    tot = np.sum(s[m])
    return float(np.sum(f[m] * s[m]) / tot) if tot > 0 else None


def _cross(t, e, level, start, direction):
    """First time at or after index start where e crosses level, interpolated."""
    for i in range(max(start, 1), len(e)):
        if (direction < 0 and e[i] <= level < e[i - 1]) or (direction > 0 and e[i] >= level > e[i - 1]):
            fr = (level - e[i - 1]) / (e[i] - e[i - 1])
            return t[i - 1] + fr * (t[i] - t[i - 1])
    return None


def measure_oneshot(sr, x, tags, loop_flag=0):
    out = {"note_tag": NOTE_NAMES[tags["note"]] if tags.get("note") is not None else None}
    sig = _mono(x)
    f0, _ = median_f0(sig, sr)
    if f0 is not None:
        out["f0_hz"] = f0
        out["note_measured"] = midi_name(hz_to_midi(f0))
        if tags.get("note") is not None:
            cents = (hz_to_midi(f0) - tags["note"]) * 100.0
            out["cents_gap"] = (cents + 600.0) % 1200.0 - 600.0
    p = np.sum(x ** 2, axis=1)
    # one period window: no ripple on a periodic tone, little smoothing
    win = 1.0 / f0 if f0 and f0 > 60 else 0.01
    t, e = _envelope_db(p, sr, win, hop_s=0.001)
    lin = 10 ** (e / 20.0)
    ip = int(np.argmax(lin))
    peak = lin[ip]
    # attack 10 -> 90 % of peak amplitude
    i10 = np.nonzero(lin[:ip + 1] >= 0.1 * peak)[0]
    i90 = np.nonzero(lin[:ip + 1] >= 0.9 * peak)[0]
    if len(i10) and len(i90):
        ta = _cross(t, lin, 0.1 * peak, 0, 1) if i10[0] > 0 else t[0]
        tb = _cross(t, lin, 0.9 * peak, 0, 1) if i90[0] > 0 else t[0]
        if ta is not None and tb is not None:
            out["attack_ms"] = max(0.0, 1000 * (tb - ta))
    # sustain: median level of the held part, found iteratively
    above = np.nonzero(e > e[ip] - 60)[0]
    end_i = above[-1] if len(above) else len(e) - 1
    body = lin[ip:end_i + 1]
    s = None
    if len(body) > 4:
        s = float(np.median(body))
        for _ in range(4):
            held = np.nonzero(np.abs(20 * np.log10(np.maximum(body, 1e-12) / max(s, 1e-12))) < 1.5)[0]
            if len(held) < 3:
                break
            s = float(np.median(body[held]))
        held = np.nonzero(np.abs(20 * np.log10(np.maximum(body, 1e-12) / max(s, 1e-12))) < 1.5)[0]
        held_t = len(held) * (t[1] - t[0]) if len(t) > 1 else 0
        if held_t < 0.1 or s < 0.03 * peak:
            s = None
    held_s = False
    if s is not None:
        out["sustain_db"] = 20 * math.log10(s / peak)
        tgt = peak - 0.9 * (peak - s)
        td = _cross(t, lin, tgt, ip, -1)
        if td is not None:
            out["decay_ms"] = 1000 * (td - t[ip])
        above9 = np.nonzero(lin >= 0.9 * s)[0]
        i9 = above9[-1]
        if i9 + 1 < len(lin):
            r1 = t[i9] + (lin[i9] - 0.9 * s) / max(lin[i9] - lin[i9 + 1], 1e-20) * (t[i9 + 1] - t[i9])
            r2 = _cross(t, lin, 0.1 * s, i9 + 1, -1)
            if r2 is None and lin[i9 + 1] < 0.1 * s:
                r2 = r1
            if r2 is not None:
                out["release_ms"] = 1000 * (r2 - r1)
        held_s = True
    else:
        # no held part: sustain is where the note ends, release is the whole fall
        out["sustain_db"] = float(e[end_i] - e[ip]) if end_i > ip else None
        if out["sustain_db"] is not None and out["sustain_db"] > -30:
            out["sustain_db"] = -60.0
    # centroids
    on = int(np.nonzero(np.abs(sig) >= 10 ** (-30 / 20.0) * np.max(np.abs(sig)))[0][0])
    pk40 = np.nonzero(e > e[ip] - 40)[0]
    stop = int(round(t[pk40[-1]] * sr)) if len(pk40) else len(sig)
    seg = int(0.05 * sr)
    out["centroid_start_hz"] = _centroid(sig[on:on + seg], sr)
    mid = (on + stop) // 2
    out["centroid_mid_hz"] = _centroid(sig[max(on, mid - seg // 2):mid + seg // 2], sr)
    out["centroid_end_hz"] = _centroid(sig[max(on, stop - seg):stop], sr)
    # harmonic ratio
    out["harmonic_ratio"] = None
    if f0:
        a, b = on, min(len(sig), stop)
        n = 1 << int(math.ceil(math.log2(max(64, b - a))))
        spec = np.abs(np.fft.rfft(sig[a:b] * np.hanning(b - a), n)) ** 2
        f = np.fft.rfftfreq(n, 1.0 / sr)
        band = (f >= 50) & (f <= 20000)
        harm = np.zeros_like(band)
        for k in range(1, 21):
            harm |= (f >= k * f0 * 0.97) & (f <= k * f0 * 1.03)
        tot = np.sum(spec[band])
        out["harmonic_ratio"] = float(np.sum(spec[band & harm]) / tot) if tot > 0 else None
    if x.shape[1] >= 2:
        m = (x[:, 0] + x[:, 1]) / 2
        sd = (x[:, 0] - x[:, 1]) / 2
        pm, ps = float(np.sum(m ** 2)), float(np.sum(sd ** 2))
        out["stereo_width"] = ps / (pm + ps) if pm + ps > 0 else None
    else:
        out["stereo_width"] = 0.0
    # loopable: a smpl loop, or a held sustain part a sampler can loop
    out["loopable"] = 1 if loop_flag or (held_s and out["sustain_db"] > -20) else 0
    return out


# ================================================================ tempo

def _novelty(sig, sr, lo=None, hi=None, hop_s=0.004, n=1024, log=True):
    """Spectral flux; frame i is centred on time i * hop (n/2 zeros padded in front)."""
    hop = max(1, int(hop_s * sr))
    sig = np.concatenate([np.zeros(n // 2), sig])
    mag = _stft_mag(sig, n, hop)
    f = np.fft.rfftfreq(n, 1.0 / sr)
    if lo is not None:
        mag = mag[:, (f >= lo) & (f <= hi)]
    lm = np.log1p(100 * mag) if log else mag
    d = np.diff(lm, axis=0)
    nov = np.concatenate([[0.0], np.sum(np.maximum(d, 0), axis=1)])
    return nov, hop / sr, 0.0


def _band_hits(sig, sr, lo, hi):
    nov, dt, _ = _novelty(sig, sr, lo, hi)
    return _peaks(nov, dt, 0.0, 0.3) if len(nov) >= 50 else []


def _regular_spacing(pk):
    """Median spacing (s) of hits when they are regular, else None.
    Regular: at least 3 hits and 75 % of the gaps within 8 % of the median."""
    if len(pk) < 3:
        return None
    gaps = np.diff(pk)
    med = float(np.median(gaps))
    if med <= 0.05:
        return None
    return med if np.mean(np.abs(gaps - med) <= 0.08 * med) >= 0.75 else None


def _band_factor(c, kick_s, snare_s):
    """Tempo-octave hint from regular kick and snare hits (blind audit B4):
    a snare on every beat means the tempo is too slow, a kick on every beat
    (four on the floor) means it is right, a kick every half beat that it is too slow."""
    f = 1.0

    def near(r, t):
        return abs(r / t - 1.0) < 0.08

    if snare_s:
        r = snare_s * c / 60.0
        if near(r, 1.0) or near(r, 0.5):
            f *= 0.5
    if kick_s:
        r = kick_s * c / 60.0
        if near(r, 1.0):
            f *= 1.3
        elif near(r, 0.5):
            f *= 0.5
    return f


def tempo(sig, sr, dur):
    """bpm or None, on the first 40 s."""
    return tempo_detail(sig, sr, dur)[0]


def tempo_detail(sig, sr, dur):
    """(bpm or None, info) on the first 40 s. info: "scores" {candidate:
    score}, "sure" (False when the octave or the triplet feel is a guess:
    audit 2, defect 7), "kick_beats" (beats between regular kicks, or None)."""
    bpm, scores, kick_s = _tempo_core(sig, sr, dur)
    info = {"scores": scores, "sure": False, "kick_beats": None}
    if bpm is None or not scores:
        return bpm, info  # sparse hits: the octave is a guess
    kb = kick_s * bpm / 60.0 if kick_s else None
    info["kick_beats"] = kb
    near = [c for c in scores if abs(c - bpm) / bpm < 0.03]
    top = max(scores[c] for c in near) if near else max(scores.values())
    # a metric relative (double, half, triplet feel) scoring close: a guess
    # (a kick on every beat, four on the floor, settles the double)
    four = kb is not None and abs(kb - 1.0) < 0.08
    ratios = (0.5, 1.5, 2.0 / 3, 4.0 / 3, 0.75) if four else (2.0, 0.5, 1.5, 2.0 / 3, 4.0 / 3, 0.75)
    rival = any(sc >= 0.6 * top for c, sc in scores.items()
                for r in ratios if abs(c / bpm / r - 1.0) < 0.03)
    # regular kicks must land on whole beats (every 1, 2, 3 or 4 beats);
    # every half beat or 1.5 / 0.75 beat means the tempo is off by an octave
    # or a triplet ratio
    kick_ok = kb is None or any(abs(kb / t - 1.0) < 0.08 for t in (1.0, 2.0, 3.0, 4.0))
    if kb is not None and abs(kb / 2.0 - 1.0) < 0.08 and bpm / 2.0 < 60.0:
        kick_ok = False  # a kick on every beat of bpm/2, below the range: octave unknown
    info["sure"] = not rival and kick_ok
    return bpm, info


def _tempo_core(sig, sr, dur):
    """(bpm or None, scores {candidate: score}, regular kick spacing in s)."""
    sig = sig[: int(40 * sr)]
    scores = {}
    kick_s = None
    nov, dt, off = _novelty(sig, sr)
    if len(nov) < 50 or np.max(nov) <= 0:
        return None, scores, kick_s
    nv = nov - np.convolve(nov, np.ones(25) / 25, mode="same")
    nv = np.maximum(nv, 0)
    nf = 1 << int(math.ceil(math.log2(2 * len(nv))))
    sp = np.fft.rfft(nv, nf)
    acf = np.fft.irfft(sp * np.conj(sp), nf)[:len(nv)]
    acf /= acf[0] + 1e-20

    def acf_at(lag):
        i = int(lag)
        if i + 1 >= len(acf):
            return 0.0
        fr = lag - i
        return acf[i] * (1 - fr) + acf[i + 1] * fr

    bpms = np.arange(60.0, 200.01, 0.25)
    raw = np.zeros(len(bpms))
    for j, bpm in enumerate(bpms):
        lag = 60.0 / bpm / dt
        raw[j] = sum(wk * acf_at(lag * k) for k, wk in ((1, 1.0), (2, 0.6), (4, 0.5), (0.5, 0.3)))
    if np.max(raw) <= 0:
        return None, scores, kick_s
    # onsets (with strength) to test which tempo puts them on a 16th grid
    pk = _peaks(nov, dt, 0.0, 0.1)

    def fallback():
        # sparse hits (one chord per bar): trust the ACF only when the file
        # holds a whole number of bars at that tempo
        if len(pk) < 2:
            return None
        rb = float(bpms[int(np.argmax(raw))])
        fits = []
        for r in (1.0, 0.5, 2.0):
            c = rb * r
            nb = round(dur * c / 240.0)
            if nb >= 1 and 60 <= 240.0 * nb / dur <= 200 and abs(dur * c / 240.0 - nb) / nb < 0.02:
                fits.append(240.0 * nb / dur)
        # octave unknown from sparse hits: the one nearest a common tempo
        return min(fits, key=lambda c: abs(math.log2(c / 95.0))) if fits else None

    if len(pk) < 4:
        return fallback(), {}, kick_s
    w = np.array([nov[int(round(tt / dt))] for tt in pk])
    pk = np.array(pk)
    # candidates: local maxima of the ACF score and their metric relatives
    loc = [j for j in range(1, len(raw) - 1) if raw[j] >= raw[j - 1] and raw[j] >= raw[j + 1]]
    loc = sorted(loc, key=lambda j: -raw[j])[:6]
    cands = set()
    for j in loc:
        for r in (1.0, 2.0, 0.5, 1.5, 2.0 / 3, 0.75, 4.0 / 3):
            c = bpms[j] * r
            if 60 <= c <= 200:
                cands.add(round(c * 4) / 4.0)
    kicks = _band_hits(sig, sr, 30.0, 120.0)
    kick_s = _regular_spacing(kicks)
    # snare hits: mid-band onsets that are not a kick's click
    snare_s = _regular_spacing([t for t in _band_hits(sig, sr, 1000.0, 5000.0)
                                if not any(abs(t - k) < 0.03 for k in kicks)])
    best, best_s = None, -1.0
    for c in sorted(cands):
        j = int(round((c - 60.0) * 4))
        sc = max(raw[j], 1e-6) ** 0.5
        fit = _grid_fit(pk, w, c)
        bars = dur * c / 240.0
        nb = round(bars)
        whole = nb >= 1 and bars <= 64 and abs(bars - nb) < max(0.04, 0.01 * nb)
        half = (not whole) and abs(bars * 2 - round(bars * 2)) < 0.04
        sc *= fit ** 8 * (1.0 if whole else (0.8 if half else 0.6))
        sc *= math.exp(-0.5 * (math.log2(c / 95.0) / 1.0) ** 2)
        sc *= _band_factor(c, kick_s, snare_s)
        scores[c] = sc
        if sc > best_s:
            best, best_s = c, sc
    if best is None or _grid_fit(pk, w, best) < 0.6:
        return fallback(), {}, kick_s
    # snap to a whole number of bars over the file
    bars = dur * best / 240.0
    nb = round(bars)
    if nb >= 1 and abs(bars - nb) / nb < 0.01:
        cand = 240.0 * nb / dur
        if 60 <= cand <= 200:
            return cand, scores, kick_s
    return best, scores, kick_s


def _grid_fit(pk, w, bpm):
    """Share of onset strength on a 16th grid (off 16ths may swing to 70 %),
    with the best grid offset."""
    e8 = 30.0 / bpm
    u0 = (pk / e8) % 1.0
    best = 0.0
    for off in np.linspace(0, 1, 48, endpoint=False):
        u = (u0 - off) % 1.0
        on = (u < 0.07) | (u > 0.93) | ((u > 0.45) & (u < 0.72))
        best = max(best, float(np.sum(w[on]) / np.sum(w)))
    return best


def bars_of(dur, bpm):
    if not bpm:
        return None
    b = dur * bpm / 240.0
    nb = round(b * 4) / 4.0
    return float(round(b)) if abs(b - round(b)) < 0.1 else nb


def bpm_gap(meas, tag):
    if meas is None or tag is None:
        return None
    return 100.0 * (meas - tag) / tag


# ================================================================ drum loop

def measure_drum_loop(sr, x, tags):
    sig = _mono(x)
    dur = len(sig) / sr
    out = {"bpm_tag": tags.get("bpm")}
    bpm, tinfo = tempo_detail(sig, sr, dur)
    out["_bpm_sure"] = tinfo["sure"]  # read by fence_row (audit 2, defect 7)
    out["bpm_measured"] = bpm
    out["bpm_gap_pct"] = bpm_gap(bpm, tags.get("bpm"))
    use = tags.get("bpm") or bpm
    if use and bpm and tags.get("bpm") and abs(bpm - tags["bpm"]) / tags["bpm"] > 0.02:
        use = tags["bpm"]
    if not use:
        return out
    out["bars"] = bars_of(dur, use)
    step = 60.0 / use / 4
    nsteps = int(round(dur / step))
    if nsteps < 16:
        return out
    bands = {"kick": (30, 120), "snare": (1000, 5000), "hats": (7000, 16000)}
    novs = {}
    for k, (lo, hi) in bands.items():
        novs[k] = _novelty(sig, sr, lo, min(hi, sr / 2 - 100), hop_s=0.0025,
                           n=512 if k != "kick" else 2048, log=False)
    # onset times: union of the band onsets, merged within 30 ms
    allt = sorted(tt for k in bands for tt in _peaks(novs[k][0], novs[k][1], 0.0, 0.2))
    on_t = []
    for tt in allt:
        if not on_t or tt - on_t[-1] > 0.03:
            on_t.append(tt)
    # swing: position of off-16th onsets inside their 8th
    e8 = 2 * step
    us = []
    for tt in on_t:
        u = (tt / e8) % 1.0
        if 0.35 <= u <= 0.8:
            us.append(u)
    on_grid = [((tt / e8) + 0.5) % 1.0 - 0.5 for tt in on_t if abs(((tt / e8) + 0.5) % 1.0 - 0.5) < 0.12]
    bias = float(np.median(on_grid)) if on_grid else 0.0
    swing = float(np.median(us)) - bias if len(us) >= 4 else 0.5
    out["swing_pct"] = 100 * swing
    out["density_per_beat"] = len(on_t) / (dur / (4 * step))
    for k in ("kick", "snare", "hats"):
        nov, dt, off = novs[k]
        steps = np.zeros(nsteps)
        for s in range(nsteps):
            when = (s // 2) * e8 + (s % 2) * e8 * swing
            # frames are centred, so the flux peak of an onset sits within a few ms of it
            c = when / dt
            lo, hi = int(c - 0.015 / dt), int(c + 0.02 / dt) + 1
            seg = nov[max(0, lo):max(0, min(len(nov), hi))]
            steps[s] = np.max(seg) if len(seg) else 0.0
        out["grid_" + k] = _grid_string(steps, nsteps)
    # 3 beats per bar (waltz): the kick lands on every third beat only, so the
    # bars are counted in 3/4, not 4/4 (audit 2, defect 7)
    nov, dt, off = novs["kick"]
    nb = int(round(dur * use / 60.0))
    if nb >= 6:
        beat = np.zeros(nb)
        for b in range(nb):
            c = b * 60.0 / use / dt
            seg = nov[max(0, int(c - 0.015 / dt)):max(0, min(len(nov), int(c + 0.02 / dt) + 1))]
            beat[b] = np.max(seg) if len(seg) else 0.0
        idx = np.arange(nb)

        def accent(m):
            return float(np.mean(beat[idx % m == 0])) / max(float(np.mean(beat[idx % m != 0])), 1e-12)

        if accent(3) >= 3.0 and accent(4) < 1.5:
            out["bars"] = bars_of(dur, use * 4.0 / 3.0)
    b = out.get("bars")
    if b is not None and not tags.get("bpm") and b != int(b) and b - int(b) < 0.5 and b >= 1:
        out["bars"] = float(int(b))  # a ringing tail after the last bar (audit 2, defect 7)
    return out


def _peaks(nov, dt, off, rel):
    """Onset times: local maxima above rel * max and above a local mean."""
    if np.max(nov) <= 0:
        return []
    loc = np.convolve(nov, np.ones(41) / 41, mode="same")
    thr = rel * np.max(nov)
    w = max(1, int(0.03 / dt))
    out = []
    last = -1e9
    for i in range(1, len(nov) - 1):
        if nov[i] >= thr and nov[i] > loc[i] * 1.5 and nov[i] == np.max(nov[max(0, i - w):i + w + 1]):
            tt = i * dt
            if tt - last > 0.03:
                out.append(tt)
                last = tt
    return out


def _grid_string(steps, nsteps):
    bars = nsteps // 16
    if bars < 1:
        return None
    st = steps[:bars * 16].reshape(bars, 16)
    mx = np.max(st)
    if mx <= 0:
        return "." * 16
    hits = st >= 0.3 * mx
    # a step counts as hit relative to the strongest step of its own column too
    cnt = hits.sum(axis=0)
    return "".join("x" if c * 2 >= bars else ("o" if c > 0 else ".") for c in cnt)


# ================================================================ key

def chroma(sig, sr, seconds=60.0, bass=False):
    """12-bin chroma (50 Hz - 5 kHz); with bass=True also the 40-260 Hz chroma
    of the opening."""
    sig = sig[: int(seconds * sr)]
    n = 16384 if sr >= 32000 else 8192
    hop = n // 4
    mag = _stft_mag(sig, n, hop)
    f = np.fft.rfftfreq(n, 1.0 / sr)
    ener = np.sum(mag ** 2, axis=0)
    # loops start on their tonic chord far more often than not: the bass
    # chroma looks at the opening only (2 s, at most a quarter of the file)
    first = max(1, min(int(2.0 * sr / hop), mag.shape[0] // 4))
    ener_first = np.sum(mag[:first] ** 2, axis=0)

    def fold(lo, hi, e=ener):
        m = (f >= lo) & (f <= hi)
        pc = np.mod(np.round(12 * np.log2(f[m] / 440.0)) + 9, 12).astype(int)
        ch = np.zeros(12)
        # sqrt compression avoids one loud bass owning the key
        np.add.at(ch, pc, np.sqrt(e[m]))
        return ch

    ch = fold(50, 5000)
    return (ch, fold(40, 260, ener_first)) if bass else ch


def key_of_chroma(ch, bass=None):
    """Krumhansl key. Major and its relative minor share their notes, so when
    both fit almost as well the bass decides: the tonic sits in the bass."""
    if isinstance(ch, tuple):
        ch, bass = ch
    if not np.any(ch > 0):
        return None, None
    rs = {}
    for mode, prof in (("major", KK_MAJOR), ("minor", KK_MINOR)):
        for k in range(12):
            rs[(k, mode)] = float(np.corrcoef(ch, np.roll(prof, k))[0, 1])
    best = max(rs, key=rs.get)
    if bass is not None and np.any(bass > 0):
        k, mode = best
        rel = ((k + 9) % 12, "minor") if mode == "major" else ((k + 3) % 12, "major")
        if rs[rel] > rs[best] - 0.12 and bass[rel[0]] > 1.2 * bass[k]:
            best = rel
    return best, rs[best]


def band_shares(x, sr):
    sig = _mono(x)[: int(60 * sr)]
    n = 1 << int(math.ceil(math.log2(max(len(sig), 64))))
    s = np.abs(np.fft.rfft(sig, n)) ** 2
    f = np.fft.rfftfreq(n, 1.0 / sr)
    tot = np.sum(s[(f >= 20) & (f <= 20000)])
    if tot <= 0:
        return {}
    return {"low_db": _db(np.sum(s[(f >= 20) & (f < 250)]) / tot),
            "mid_db": _db(np.sum(s[(f >= 250) & (f < 4000)]) / tot),
            "high_db": _db(np.sum(s[(f >= 4000) & (f <= 20000)]) / tot)}


def measure_melodic_loop(sr, x, tags):
    sig = _mono(x)
    dur = len(sig) / sr
    out = {"bpm_tag": tags.get("bpm"), "key_tag": key_name(tags.get("key"))}
    bpm, tinfo = tempo_detail(sig, sr, dur)
    out["_bpm_sure"] = tinfo["sure"]
    out["bpm_measured"] = bpm
    out["bpm_gap_pct"] = bpm_gap(bpm, tags.get("bpm"))
    out["bars"] = bars_of(dur, tags.get("bpm") or bpm)
    k, r = key_of_chroma(chroma(sig, sr, bass=True))
    out["key_measured"] = key_name(k)
    out["key_strength"] = r
    out["key_match"] = key_match(k, tags.get("key"))
    out.update(band_shares(x, sr))
    return out


# ================================================================ midi

def _read_vlq(b, i):
    v = 0
    for _ in range(4):
        if i >= len(b):
            raise BadAudio("truncated midi")
        c = b[i]
        i += 1
        v = (v << 7) | (c & 0x7F)
        if not c & 0x80:
            return v, i
    raise BadAudio("bad midi length")


def parse_midi(b):
    """(tpq, notes [(start, end, pitch, vel, ch)], tempos [(tick, us)], timesig (num, den) or None
    when the file has no time signature event)."""
    if len(b) < 14 or b[:4] != b"MThd":
        raise BadAudio("not a midi file")
    hl = struct.unpack(">I", b[4:8])[0]
    if hl < 6 or 8 + hl > len(b):
        raise BadAudio("bad midi header")
    fmt, ntrk, div = struct.unpack(">HHH", b[8:14])
    if div & 0x8000 or div == 0:
        raise BadAudio("smpte midi not supported")
    pos = 8 + hl
    notes, tempos, ts = [], [], None
    cut = False
    open_ = {}
    for _ in range(min(ntrk, 256)):
        if pos + 8 > len(b) or b[pos:pos + 4] != b"MTrk":
            raise BadAudio("bad midi track")
        size = struct.unpack(">I", b[pos + 4:pos + 8])[0]
        i, end = pos + 8, min(len(b), pos + 8 + size)
        short = pos + 8 + size > len(b)
        pos += 8 + size
        tick, status = 0, None
        eot = False
        while i < end:
            d, i = _read_vlq(b, i)
            tick += d
            if i >= end:
                break
            c = b[i]
            if c == 0xFF:
                if i + 2 > end:
                    break
                typ = b[i + 1]
                ln, j = _read_vlq(b, i + 2)
                data = b[j:j + ln]
                i = j + ln
                if typ == 0x51 and ln == 3:
                    tempos.append((tick, (data[0] << 16) | (data[1] << 8) | data[2]))
                elif typ == 0x58 and ln >= 2 and ts is None:
                    ts = (data[0], 2 ** data[1])
                elif typ == 0x2F:
                    eot = True
                    break
                continue
            if c in (0xF0, 0xF7):
                ln, j = _read_vlq(b, i + 1)
                i = j + ln
                continue
            if c & 0x80:
                status = c
                i += 1
            elif status is None:
                raise BadAudio("midi data without status")
            hi = status & 0xF0
            nd = 1 if hi in (0xC0, 0xD0) else 2
            if i + nd > end:
                break
            data = b[i:i + nd]
            i += nd
            ch = status & 0x0F
            if hi == 0x90 and data[1] > 0:
                open_.setdefault((ch, data[0]), []).append((tick, data[1]))
            elif hi == 0x80 or (hi == 0x90 and data[1] == 0):
                st = open_.get((ch, data[0]))
                if st:
                    s, v = st.pop(0)
                    notes.append((s, max(tick, s + 1), data[0], v, ch))
        if len(notes) > 200000:
            raise BadAudio("too many midi notes")
        # a track declared longer than the file and never ended: a cut file
        # whose open notes would be invented (audit 2, defect 34)
        cut = cut or (short and not eot)
    if cut:
        raise BadAudio("truncated midi")
    for (ch, p), st in open_.items():
        for s, v in st:
            notes.append((s, s + div // 4, p, v, ch))
    notes.sort()
    return div, notes, tempos, ts


def _chord_of(weights):
    """(root, quality) or None from 12 pitch-class weights (bass bonus applied by caller)."""
    if np.count_nonzero(weights > 0) < 2:
        return None
    best, best_s = None, 0.0
    for root in range(12):
        for q, iv in (("maj", (0, 4, 7)), ("min", (0, 3, 7)), ("dim", (0, 3, 6))):
            s = sum(weights[(root + i) % 12] for i in iv) - 0.5 * (np.sum(weights) - sum(
                weights[(root + i) % 12] for i in iv))
            s += 0.1 * weights[root]
            if s > best_s + 1e-9:
                best, best_s = (root, q), s
    return best


ROMAN = ["I", "bII", "II", "bIII", "III", "IV", "#IV", "V", "bVI", "VI", "bVII", "VII"]
ROMAN_MINOR = {0: "I", 1: "bII", 2: "II", 3: "III", 4: "#III", 5: "IV", 6: "#IV", 7: "V", 8: "VI", 9: "#VI",
               10: "VII", 11: "#VII"}


def roman(chord, key):
    if chord is None:
        return "."
    deg = (chord[0] - key[0]) % 12
    r = (ROMAN_MINOR if key[1] == "minor" else dict(enumerate(ROMAN)))[deg]
    if chord[1] == "min":
        r = r.lower()
    elif chord[1] == "dim":
        r = r.lower() + "o"
    return r


def _looks_triple(notes, origin, tpq):
    """True when the distinct onsets sit on a 3-beat grid and not on a 4-beat one."""
    on = {int(round((n[0] - origin) / (tpq / 8.0))) for n in notes}
    if len(on) < 3:
        return False
    s3 = sum(1 for o in on if o % 24 == 0) / len(on)
    s4 = sum(1 for o in on if o % 32 == 0) / len(on)
    return s3 >= 0.75 and s4 <= 0.5


def measure_midi_bytes(b, tags):
    tpq, notes, tempos, ts = parse_midi(b)
    ts_known = ts is not None
    ts = ts or (4, 4)
    out = {"key_tag": key_name(tags.get("key"))}
    if not notes:
        raise BadAudio("midi without notes")
    out["bpm_file"] = 60e6 / tempos[0][1] if tempos and tempos[0][1] > 0 else None
    # a tempo change inside the notes: no single file tempo (audit 2, defect 37)
    end_tick = max(n[1] for n in notes)
    if len({round(60e6 / us, 2) for tk, us in tempos if tk < end_tick and us > 0}) > 1:
        out["bpm_file"] = None
    bar = int(tpq * 4 * ts[0] / ts[1])
    last = max(n[1] for n in notes)
    # bars start at the first note: a part exported from bar 9 or starting on
    # beat 2 is not padded with empty bars (audit 2, defect 22)
    origin = min(n[0] for n in notes)
    # the last note may ring past the bar line by a 32nd of release plus a
    # 32nd of legato overlap without making a new bar
    legato = tpq // 8
    nbars = max(1, int(math.ceil((last - origin - legato - tpq // 8) / bar)))
    nbars = max(nbars, (max(n[0] for n in notes) - origin) // bar + 1)
    out["bars"] = nbars
    out["notes"] = len(notes)
    out["notes_per_bar"] = len(notes) / nbars
    pitches = [n[2] for n in notes]
    out["pitch_min"], out["pitch_max"] = min(pitches), max(pitches)
    # a 16th of legato overlap is not one more voice (audit 2, defect 22)
    ends = [max(n[0] + 1, n[1] - tpq // 4) for n in notes]
    ev = sorted([(n[0], 1) for n in notes] + [(e, -1) for e in ends], key=lambda e: (e[0], e[1]))
    cur = mx = 0
    for _, d in ev:
        cur += d
        mx = max(mx, cur)
    out["poly_max"] = mx
    v = np.array([n[3] for n in notes], dtype=float)
    out["vel_mean"], out["vel_sd"] = float(np.mean(v)), float(np.std(v))
    # swing from tick positions (16ths inside 8ths)
    e8 = tpq / 2.0
    us = [((n[0] / e8) % 1.0) for n in notes if 0.35 <= (n[0] / e8) % 1.0 <= 0.8]
    out["swing_pct"] = 100 * float(np.median(us)) if len(us) >= 4 else 50.0
    out["drum_track"] = 1 if all(n[4] == 9 for n in notes) else 0
    if not ts_known and _looks_triple(notes, origin, tpq):
        # no time signature and onsets on a 3-beat grid: the bar length is a
        # guess, so the bar cells stay empty (audit 2, defect 37)
        out["bars"] = out["notes_per_bar"] = None
    if out["drum_track"]:
        return out
    pcw = np.zeros(12)
    for s, e, p, vel, ch in notes:
        if ch != 9:
            pcw[p % 12] += e - s
    # tonic cue: the lowest note sounding at the first onset
    mel = [n for n in notes if n[4] != 9]
    t0 = min(n[0] for n in mel)
    low = min(n[2] for n in mel if n[0] <= t0 + tpq // 8)
    bass = np.zeros(12)
    bass[low % 12] = 1.0
    k, _ = key_of_chroma(pcw, bass)
    out["key_measured"] = key_name(k)
    out["key_match"] = key_match(k, tags.get("key"))
    ref = tags.get("key") or k
    if ref is not None:
        scale = MINOR_SCALE if ref[1] == "minor" else MAJOR_SCALE
        inscale = sum(pcw[(ref[0] + i) % 12] for i in scale)
        out["scale_fit"] = float(inscale / np.sum(pcw)) if np.sum(pcw) > 0 else None
        chords = []
        for bi in range(nbars):
            chords.append(_chord_window(notes, origin + bi * bar, origin + (bi + 1) * bar))
        out["chords"] = "-".join(roman(c, ref) for c in chords)
        changes = sum(1 for a, c in zip(chords, chords[1:]) if a != c)
        out["chord_changes_per_bar"] = changes / nbars
        beats = [_chord_window(notes, origin + i * tpq, origin + (i + 1) * tpq)
                 for i in range(int(math.ceil(nbars * bar / tpq)))]
        runs, cur_run = [], 1
        for a, c in zip(beats, beats[1:]):
            if a == c:
                cur_run += 1
            else:
                runs.append(cur_run)
                cur_run = 1
        runs.append(cur_run)
        out["chord_len_beats"] = float(np.mean(runs))
    return out


def _chord_window(notes, a, b):
    w = np.zeros(12)
    lowest, lowp = None, 999
    for s, e, p, vel, ch in notes:
        if ch == 9 or e <= a or s >= b:
            continue
        ov = min(e, b) - max(s, a)
        w[p % 12] += ov
        if p < lowp:
            lowp, lowest = p, p % 12
    if lowest is not None:
        w[lowest] += 0.25 * np.max(w)
    return _chord_of(w / (np.max(w) + 1e-20))


# ================================================================ vocal

def measure_vocal(sr, x, tags):
    sig = _mono(x)
    out = {}
    p = np.sum(x ** 2, axis=1)
    t, e = _envelope_db(p, sr, 0.02)
    top = np.max(e)
    # phrase ends: steep drops (>= 15 dB in 60 ms) from a loud part;
    # tail = level 150-300 ms after the drop, relative to the level before it
    tails = []
    dt = t[1] - t[0] if len(t) > 1 else 0.005
    k = max(1, int(0.03 / dt))
    a0, b0 = int(0.15 / dt), int(0.3 / dt)
    last = -10 ** 9
    for i in range(k, len(e) - b0):
        if i - last < int(0.3 / dt):
            continue
        before = e[i - k]
        if before > top - 20 and before - e[i + k] >= 15:
            tails.append(float(np.mean(e[i + a0:i + b0])) - before)
            last = i
    if tails:
        td = float(np.median(tails))
        out["tail_db"] = td
        out["wet"] = "wet" if td > -28 else ("dry" if td < -45 else "some")
    f0, fr = median_f0(sig, sr, 60.0, 1200.0)
    if f0:
        # most sung pitch: the fullest semitone, then the median inside it
        v = fr[np.isfinite(fr)]
        semi = np.round(12 * np.log2(v / 440.0)).astype(int)
        vals, cnt = np.unique(semi, return_counts=True)
        f0 = float(np.median(v[semi == vals[np.argmax(cnt)]]))
        out["pitch_median_hz"] = f0
        v = fr[np.isfinite(fr)]
        st = 12 * np.log2(v / f0)
        out["range_st"] = float(np.percentile(st, 95) - np.percentile(st, 5))
        loud = np.sum(np.isfinite(fr)) / max(1, len(fr))
        out["voiced_pct"] = 100 * float(loud)
    return out


# ================================================================ fx

def measure_fx(sr, x, tags):
    sig = _mono(x)
    out = {"bpm_tag": tags.get("bpm")}
    dur = len(sig) / sr
    out["length_beats"] = dur * tags["bpm"] / 60.0 if tags.get("bpm") else None
    nseg = 8
    seg = len(sig) // nseg
    if seg < 64:
        return out
    lv, br = [], []
    for i in range(nseg):
        s = sig[i * seg:(i + 1) * seg]
        lv.append(_db(float(np.mean(s ** 2))) or -120.0)
        br.append(_centroid(s, sr) or 0.0)
    lv = np.array(lv)
    xs = np.arange(nseg)
    slope = np.polyfit(xs, lv, 1)[0] * (nseg - 1)
    first, last = np.mean(lv[:2]), np.mean(lv[-2:])
    if slope > 6 and last > first:
        out["level_shape"] = "rising"
    elif slope < -6 and last < first:
        out["level_shape"] = "falling"
    else:
        out["level_shape"] = "flat"
    loud = lv > np.max(lv) - 40
    b = np.array(br)[loud]
    if len(b) >= 2 and np.min(b) > 0:
        ratio = np.mean(b[-2:]) / np.mean(b[:2])
        out["bright_shape"] = "rising" if ratio > 1.5 else ("falling" if ratio < 1 / 1.5 else "flat")
    p = np.sum(x ** 2, axis=1)
    tt, e = _envelope_db(p, sr, 0.05)
    out["peak_pos"] = float(tt[int(np.argmax(e))] / dur)
    return out


# ================================================================ drum one-shot

def measure_drum_oneshot(sr, x, family):
    out = {k: v for k, v in mesure.measure(sr, x).items() if k not in ("sr", "channels", "dur_ms", "peak_dbfs")}
    fam = (family or "").split(":")[0]
    if fam.startswith("hat"):
        t40 = out.get("t40_ms")
        if t40 is not None and math.isfinite(t40):
            out["hat_measured"] = "closed" if t40 < 200 else "open"
    if fam.startswith("kick") or fam in ("808", "bass", "tom"):
        out.update(_kick_drop(_mono(x), sr))
    st = out.get("pitch_steady_hz")
    if st and math.isfinite(st) and (out.get("tonal_db") is None or out["tonal_db"] > -10):
        out["note_measured"] = midi_name(hz_to_midi(st))
    return out


def _kick_drop(sig, sr):
    """Pitch curve of a kick from zero crossings of the low band."""
    n = len(sig)
    nf = 1 << int(math.ceil(math.log2(n)))
    f = np.fft.rfftfreq(nf, 1.0 / sr)
    lp = np.fft.irfft(np.fft.rfft(sig, nf) * (1.0 / (1.0 + (f / 400.0) ** 8)), nf)[:n]
    a = np.abs(lp)
    pk = np.max(a)
    if pk <= 0:
        return {}
    on = int(np.nonzero(a >= 0.1 * pk)[0][0])
    s = np.sign(lp[on:])
    zc = np.nonzero((s[:-1] < 0) & (s[1:] >= 0))[0]
    if len(zc) < 4:
        return {}
    # interpolated upward crossings
    zt = []
    for i in zc:
        y0, y1 = lp[on + i], lp[on + i + 1]
        zt.append((i + (-y0 / (y1 - y0) if y1 != y0 else 0.0)) / sr)
    zt = np.array(zt)
    per = np.diff(zt)
    tm = (zt[:-1] + zt[1:]) / 2
    fr = 1.0 / per
    # keep cycles while the low band is still above -40 dB of its peak
    env = np.array([np.max(a[on + int(z * sr): on + int(z * sr) + int(p * sr) + 1]) for z, p in zip(zt[:-1], per)])
    ok = env > pk * 0.01
    tm, fr = tm[ok], fr[ok]
    if len(fr) < 3:
        return {}
    f_end = float(np.median(fr[-max(3, len(fr) // 4):]))
    f_start = float(fr[0])
    out = {"drop_start_hz": f_start, "drop_end_hz": f_end}
    span = f_start - f_end
    if span < 0.1 * f_end:
        out["drop_shape"] = "flat"
        return out
    # start time of the drop = first cycle midpoint; extrapolate to the onset-ish start
    t0 = tm[0]

    def t_at(level):
        for i in range(1, len(fr)):
            if fr[i] <= level < fr[i - 1]:
                return tm[i - 1] + (fr[i - 1] - level) / (fr[i - 1] - fr[i]) * (tm[i] - tm[i - 1])
        return None

    t50 = t_at(f_end + 0.5 * span)
    t90 = t_at(f_end + 0.1 * span)
    if t50 is not None:
        out["drop_t50_ms"] = 1000 * (t50 - t0) + 1000 * _lead(fr, tm, span, f_end)
    if t90 is not None:
        out["drop_t90_ms"] = 1000 * (t90 - t0) + 1000 * _lead(fr, tm, span, f_end)
    a50, a90 = out.get("drop_t50_ms"), out.get("drop_t90_ms")
    if a50 and a90 and a50 > 0:
        r = a90 / a50
        out["drop_shape"] = "exp" if r >= 2.4 else ("lin" if r >= 1.3 else "step")
    return out


def _lead(fr, tm, span, f_end):
    """Time from the true glide start to the first measured cycle midpoint,
    from the slope of the first cycles (the first period averages the glide)."""
    if len(fr) < 3:
        return 0.0
    slope = (fr[1] - fr[0]) / (tm[1] - tm[0])
    if slope >= 0:
        return 0.0
    # the first cycle starts at zt0 = tm0 - per0/2; the glide starts there
    return 0.5 / fr[0]


# ================================================================ accuracy

class Accuracy:
    def __init__(self):
        self.d = {}

    def add(self, typ, measure, agree):
        if agree is None:
            return
        k = (typ, measure)
        n, a, hd, rel = self.d.get(k, (0, 0, 0, 0))
        self.d[k] = (n + 1, a + (1 if agree is True else 0), hd + (agree == "half_double"),
                     rel + (agree == "relative"))

    def rows(self):
        out = []
        for (typ, m), (n, a, hd, rel) in sorted(self.d.items()):
            err = 100.0 * (n - a) / n if n else None
            out.append({"type": typ, "measure": m, "tagged": n, "agree": a, "half_double": hd, "relative": rel,
                        "error_pct": err, "usable": "yes" if n >= 10 and err is not None and err < 5 else "no"})
        return out

    def lines(self, typ):
        return ["# accuracy type=%s measure=%s tagged=%d agree=%d error_pct=%s usable=%s" % (
            r["type"], r["measure"], r["tagged"], r["agree"], _fmt(r["error_pct"]), r["usable"])
            for r in self.rows() if r["type"] == typ]


def _bpm_agree(m, t):
    if m is None or t is None:
        return None
    return abs(m - t) / t <= 0.02


def _bpm_class(m, t):
    """True, False, or "half_double": a near miss counted apart, never as agreement (I9)."""
    agree = _bpm_agree(m, t)
    if agree is None or agree:
        return agree
    for f in (2.0, 0.5):
        if abs(m - t * f) / (t * f) <= 0.02:
            return "half_double"
    return False


def _key_agree(match):
    if match is None:
        return None
    return match == "exact"


def _key_class(match):
    return "relative" if match == "relative" else _key_agree(match)


# ================================================================ fences

END_TYPES = ("oneshot_melodic", "drum_oneshot")


def fence_row(row, usable=()):
    """Empty the cells that would show a number that looks true but is not
    (blind re-check, docs/sons/REVERIF-TOUT.md). Applied to every row before
    it is written; the measuring functions stay untouched.
    usable: set of (type, measure) pairs the accuracy table marks usable."""
    typ = row.get("type")
    # a drum hit named "Mastered", "Full", "Mix" or "Song" is still one hit:
    # its end is measured like any one-shot (audit 2, defect 18)
    hit_full = typ == "drum_oneshot" and row.get("role") == "full" \
        and row.get("bpm_tag") is None and not row.get("_loop")
    hit_end = (row.get("end_cut"), row.get("end_fade_ms"))
    # end_cut is reliable on one-shots only: on anything with a loop or tempo
    # nature the last hit still rings at the bar line (re-check 1)
    if typ not in END_TYPES or row.get("role") == "full" or row.get("bpm_tag") is not None \
            or row.get("_loop"):
        row["end_cut"] = None
        row["end_fade_ms"] = None
    elif row.get("end_fade_ms") is not None:
        # a presence / order-of-size grade, not a length to the millisecond
        row["end_fade_ms"] = max(10, int(round(float(row["end_fade_ms"]) / 10.0)) * 10)
    if hit_full:
        row["end_cut"] = hit_end[0]
        if hit_end[1] is not None:
            row["end_fade_ms"] = max(10, int(round(float(hit_end[1]) / 10.0)) * 10)
    # untagged loops: bpm and key are wrong too often on pads and basslines
    # (re-check 6, I8); shown only when the accuracy table says usable
    if typ in ("melodic_loop", "kit_stem"):
        ok = set(usable)
        drums = typ == "kit_stem" and row.get("role") == "drums"
        if row.get("bpm_tag") is None and not drums \
                and not ({(typ, "bpm"), ("melodic_loop", "bpm")} & ok):
            row["bpm_est"] = None
        if row.get("key_tag") is None and not ({(typ, "key"), ("melodic_loop", "key")} & ok):
            row["key_est"] = None
        if row.get("bpm_est") is None and row.get("bpm_tag") is None:
            row["bars"] = None  # counted from the hidden guess (audit 2, defect 19)
    elif typ == "drum_loop":
        # untagged loop whose tempo octave or feel is a guess (audit 2, defect 7):
        # the tempo and everything counted from it stay empty
        if row.get("bpm_tag") is None and row.get("_bpm_sure") is False:
            for k in ("bpm_est", "bpm_measured", "bars", "swing_pct", "density_per_beat",
                      "grid_kick", "grid_snare", "grid_hats"):
                row[k] = None
    elif typ == "kit":
        # untagged kits: same fence as untagged loops (audit 2, defect 20)
        ok = set(usable)
        roles = set(str(row.get("roles") or "").split("+"))
        drums = row.get("_bpm_sure") is True and bool(roles & {"drums", "kick", "snare", "hats", "perc"})
        if row.get("bpm_tag") is None and not drums \
                and not ({("kit", "bpm"), ("melodic_loop", "bpm")} & ok):
            for k in ("bpm_est", "bpm_measured", "bars"):
                row[k] = None
        if row.get("key_tag") is None and not ({("kit", "key"), ("melodic_loop", "key")} & ok):
            for k in ("key_est", "key_measured", "key_match"):
                row[k] = None
    elif typ == "vocal":
        row["wet"] = None  # the dry / wet verdict is not reliable yet (I11)
    elif typ == "midi":
        # the measured key is wrong too often and the chords hang on it (I8)
        for k in ("key_measured", "key_est", "key_match", "chords", "chord_changes_per_bar",
                  "chord_len_beats"):
            row[k] = None
        if row.get("key_tag") is None:
            row["scale_fit"] = None
    return row


# ================================================================ cli

def _fmt(v):
    if v is None:
        return ""
    if isinstance(v, (float, np.floating)):
        v = float(v) + 0.0  # -0.0 -> 0.0
        if math.isfinite(v) and 100 <= abs(v) < 1e4:
            # 4 significant digits turn 123.45 into 123.5 and 9999.7 into 1e+04 (defect 32)
            return ("%.2f" % v).rstrip("0").rstrip(".")
        return "" if not math.isfinite(v) else "%.4g" % v if abs(v) < 1e4 else "%.1f" % v
    if isinstance(v, (int, np.integer)):
        return str(int(v))
    text = str(v)
    # a lone "-" is not a formula in any spreadsheet; "-A1" or "-1+1" is, so
    # anything longer that starts with "-" keeps the quote
    if text == "-":
        return text
    return mesure._safe_cell(text)


def _walk(folder):
    audio, midi, skipped = [], [], {}
    for root, dirs, files in os.walk(folder, followlinks=False):
        dirs.sort()
        dirs[:] = [d for d in dirs if not d.startswith(".") and d != "__MACOSX"]
        for name in sorted(files):
            if name.startswith("._") or name.startswith("."):
                continue
            low = name.lower()
            p = os.path.join(root, name)
            if low.endswith(AUDIO_EXTS):
                audio.append(p)
            elif low.endswith(MIDI_EXTS):
                midi.append(p)
            else:
                ext = os.path.splitext(low)[1] or "(none)"
                skipped[ext] = skipped.get(ext, 0) + 1
    return audio, midi, skipped


def _size_kb(p):
    try:
        return os.path.getsize(p) / 1024.0
    except OSError:
        return None


def _measure_file(p, rel, parts, acc, rar):
    row = {"rar": rar, "kit_id": "", "error": "", "size_kb": _size_kb(p)}
    row.update(classify(rel, None))  # an unreadable file keeps the type its path gives (blind audit I3)
    try:
        with np.errstate(all="ignore"):
            sr, x, loop = read_audio(p)
            row["_loop"] = loop
            cl = classify(rel, x.shape[0] / sr)
            if cl["subtype"] == "by_sound":  # the name is silent: the sound decides
                cl["type"] = "drum_loop" if _drumminess(x) > DRUMMY_FLATNESS else "melodic_loop"
            row.update(cl)
            row.update(common(sr, x))
            tags = parse_tags(parts, cl["type"])
            typ = cl["type"]
            if typ == "oneshot_melodic":
                m = measure_oneshot(sr, x, tags, loop)
                if m.get("cents_gap") is not None:
                    acc.add(typ, "note", abs(m["cents_gap"]) <= 50)
            elif typ == "drum_loop":
                m = measure_drum_loop(sr, x, tags)
                acc.add(typ, "bpm", _bpm_class(m.get("bpm_measured"), tags.get("bpm")))
            elif typ == "melodic_loop":
                m = measure_melodic_loop(sr, x, tags)
                acc.add(typ, "bpm", _bpm_class(m.get("bpm_measured"), tags.get("bpm")))
                acc.add(typ, "key", _key_class(m.get("key_match")))
            elif typ == "vocal":
                m = measure_vocal(sr, x, tags)
            elif typ == "fx":
                m = measure_fx(sr, x, tags)
            elif typ == "drum_oneshot":
                m = measure_drum_oneshot(sr, x, cl["subtype"])
                if cl["subtype"] == "hat" and m.get("hat_measured"):  # the name is silent: the sound decides
                    row["subtype"] = "hat_" + m["hat_measured"]
                if tags.get("note") is not None and m.get("note_measured"):
                    pc = NOTE_NAMES.index(re.match(r"[A-G]#?", m["note_measured"]).group(0))
                    acc.add(typ, "note", pc == tags["note"])
            else:
                m = {}
            row.update(m)
            row.setdefault("bpm_tag", tags.get("bpm"))
            row.setdefault("key_tag", key_name(tags.get("key")))
    except BadAudio as e:
        row["error"] = str(e)[:80]
    except Exception as e:  # a hostile file must not stop the batch
        row["error"] = "internal: %s" % type(e).__name__
    return row


def _run_kits(kits, acc, rar):
    """kits: {kit folder key: [(path, rel, parts)]}; returns kit rows, stem rows, midi rows."""
    kit_rows, stem_rows, midi_rows = [], [], []
    for n, key in enumerate(sorted(kits)):
        kid = "r%sk%03d" % (rar, n + 1)
        items = kits[key]
        stems, mids = [], []
        for p, rel, parts in items:
            if p.lower().endswith(MIDI_EXTS):
                mids.append((p, rel, parts))
            else:
                stems.append((p, rel, parts))
        kit_tags = parse_tags(list(key), "kit")
        loaded = []
        for p, rel, parts in stems:
            row = {"rar": rar, "kit_id": kid, "type": "kit_stem", "error": "", "size_kb": _size_kb(p)}
            cl = classify(rel, None)
            row.update({"subtype": "-", "role": cl["role"], "style": cl["style"]})
            try:
                with np.errstate(all="ignore"):
                    sr, x, _ = read_audio(p)
                    row.update(common(sr, x))
                    tags = parse_tags(parts, "kit_stem")
                    for k in ("bpm", "key"):
                        if tags.get(k) is None:
                            tags[k] = kit_tags.get(k)
                    m = measure_melodic_loop(sr, x, tags)
                    if row["role"] in ("drums", "kick", "snare", "hats", "perc"):
                        m["key_measured"] = m["key_match"] = m["key_strength"] = None
                    row.update(m)
                    loaded.append((row, sr, x))
            except BadAudio as e:
                row["error"] = str(e)[:80]
            except Exception as e:
                row["error"] = "internal: %s" % type(e).__name__
            stem_rows.append(row)
        kr = {"rar": rar, "kit_id": kid, "type": "kit", "subtype": "-", "role": "-", "error": "",
              "n_stems": len(stems), "n_midi": len(mids),
              "size_kb": sum(_size_kb(i[0]) or 0.0 for i in items), "bpm_tag": kit_tags.get("bpm"),
              "key_tag": key_name(kit_tags.get("key"))}
        roles = sorted({r["role"] for r in stem_rows if r["kit_id"] == kid and r["role"] not in ("-",)})
        kr["roles"] = "+".join(roles)
        kr["style"] = stem_rows[-1]["style"] if stems else (classify(mids[0][1], None)["style"] if mids else "")
        if loaded:
            try:
                with np.errstate(all="ignore"):
                    sr = loaded[0][1]
                    same = [(r, x) for r, s, x in loaded if s == sr]
                    n60 = int(60 * sr)
                    ln = max(x.shape[0] for _, x in same)
                    ln = min(ln, n60)
                    tot = np.zeros(ln)
                    melo = np.zeros(ln)
                    sums = []
                    for r, x in same:
                        m = _mono(x)[:ln]
                        if r["role"] == "full":
                            continue
                        tot[:len(m)] += m
                        if r["role"] not in ("drums", "kick", "snare", "hats", "perc", "fx", "vocal"):
                            melo[:len(m)] += m
                        sums.append(r)
                    ptot = float(np.mean(tot ** 2))
                    ltot = lufs(tot.reshape(-1, 1), sr)
                    for r, x in same:
                        if r["role"] == "full" or ptot <= 0:
                            continue
                        m = _mono(x)[:ln]
                        r["level_rel_kit_db"] = _db(float(np.sum(m ** 2)) / len(tot) / ptot)
                        lm = lufs(m.reshape(-1, 1), sr)
                        if lm is not None and ltot is not None:
                            r["lufs_rel_kit_db"] = lm - ltot
                    dur = ln / sr
                    bpm, tinfo = tempo_detail(tot, sr, dur)
                    kr["_bpm_sure"] = tinfo["sure"]
                    kr["bpm_measured"] = bpm
                    kr["bpm_gap_pct"] = bpm_gap(bpm, kit_tags.get("bpm"))
                    kr["bars"] = bars_of(dur, kit_tags.get("bpm") or bpm)
                    acc.add("kit", "bpm", _bpm_class(bpm, kit_tags.get("bpm")))
                    if np.any(melo != 0):
                        k, _ = key_of_chroma(chroma(melo, sr, bass=True))
                        kr["key_measured"] = key_name(k)
                        kr["key_match"] = key_match(k, kit_tags.get("key"))
                        acc.add("kit", "key", _key_class(kr["key_match"]))
                        kr["_key"] = k
            except Exception as e:
                kr["error"] = "internal: %s" % type(e).__name__
        kit_rows.append(kr)
        for p, rel, parts in mids:
            tags = parse_tags(parts, "midi")
            if tags.get("key") is None:
                tags["key"] = kit_tags.get("key") or kr.get("_key")
            row = _midi_row(p, rel, tags, rar, acc)
            row["kit_id"] = kid
            midi_rows.append(row)
    return kit_rows, stem_rows, midi_rows


def _midi_row(p, rel, tags, rar, acc):
    cl = classify(rel, None)
    row = {"rar": rar, "kit_id": "", "type": "midi", "subtype": "-", "role": cl["role"], "style": cl["style"],
           "error": "", "size_kb": _size_kb(p), "bpm_tag": tags.get("bpm")}
    if row["bpm_tag"] is not None:
        row["bpm_est"] = row["bpm_tag"]  # the name's tempo wins over the file's (audit 2, defect 21)
    try:
        if os.path.islink(p) or os.path.getsize(p) > 16 * 1024 * 1024:
            raise BadAudio("midi file too large or a link")
        with open(p, "rb") as fh:
            b = fh.read()
        row.update(measure_midi_bytes(b, tags))
        acc.add("midi", "key", _key_class(row.get("key_match")))
    except BadAudio as e:
        row["error"] = str(e)[:80]
    except Exception as e:
        row["error"] = "internal: %s" % type(e).__name__
    return row


def _file_id(rel):
    """Short stable id of a file inside its pack: no name in the table, but
    `--trace ID` finds the file again (audit 2, defect 28)."""
    import hashlib
    return hashlib.sha1(rel.encode("utf-8", "surrogateescape")).hexdigest()[:10]


def main(argv=None):
    ap = argparse.ArgumentParser(description="Measure every sound of a sample pack into anonymous CSV tables.")
    ap.add_argument("folder", help="pack folder (searched recursively)")
    ap.add_argument("--rar", required=True, help="pack number (or 'app')")
    ap.add_argument("--out", help="CSV prefix, outside the git repository")
    ap.add_argument("--trace", metavar="FILE_ID", help="print the file a table row's file_id stands for, write nothing")
    args = ap.parse_args(argv)
    if not os.path.isdir(args.folder):
        ap.error("not a folder: %s" % args.folder)
    if args.trace:
        # the tables stay anonymous; the id leads back to the file only with the pack at hand (defect 28)
        audio, midi, _ = _walk(args.folder)
        hits = [r for r in (os.path.relpath(p, args.folder).replace(os.sep, "/") for p in audio + midi)
                if _file_id(r) == args.trace]
        for r in hits:
            print(r)
        return 0 if hits else 1
    if not args.out:
        ap.error("the following arguments are required: --out")
    if mesure._inside_repo(args.out) or mesure._inside_repo(os.path.dirname(os.path.abspath(args.out))):
        ap.error("refusing to write measurements of samples inside the repository")
    if not os.path.isdir(os.path.dirname(os.path.abspath(args.out))):
        # said before measuring, not after (audit 2, defect 27)
        ap.error("--out: no such folder: %s" % os.path.dirname(os.path.abspath(args.out)))
    rar = re.sub(r"[^A-Za-z0-9]", "", args.rar)[:12] or "0"
    audio, midi, skipped = _walk(args.folder)
    acc = Accuracy()
    rows = {t: [] for t in TYPES}
    kits = {}
    for p in audio + midi:
        rel = os.path.relpath(p, args.folder).replace(os.sep, "/")
        parts = rel.split("/")
        ki, kname = kit_split(parts)
        if ki is not None and p not in midi:
            cl = classify(rel, None)
            if cl["type"] != "kit_stem" or drum_family(_clean(parts[-1]), []) is not None:
                row = _measure_file(p, rel, parts, acc, rar)  # one-shots inside a kit (I7)
                if row["type"] != "kit_stem" or row["error"]:
                    row["file_id"] = _file_id(rel)
                    rows[row["type"]].append(row)
                    continue
        if ki is not None:
            kits.setdefault(tuple(parts[:ki + 1]), []).append((p, rel, parts))
            continue
        if p in midi:
            rows["midi"].append(_midi_row(p, rel, parse_tags(parts, "midi"), rar, acc))
            rows["midi"][-1]["file_id"] = _file_id(rel)
            continue
        row = _measure_file(p, rel, parts, acc, rar)
        row["file_id"] = _file_id(rel)
        rows[row["type"]].append(row)
    kr, sr_, mr = _run_kits(kits, acc, rar)
    rows["kit"] += kr
    rows["kit_stem"] += sr_
    rows["midi"] += mr
    total = 0
    usable = {(r["type"], r["measure"]) for r in acc.rows() if r["usable"] == "yes"}
    for typ in TYPES:
        if not rows[typ]:
            # a table left by an earlier run would read as this run's (defect 33)
            if os.path.isfile("%s_%s.csv" % (args.out, typ)):
                os.remove("%s_%s.csv" % (args.out, typ))
            continue
        cols = COMMON + TYPE_COLUMNS[typ] + ["file_id"]
        with open("%s_%s.csv" % (args.out, typ), "w", newline="", encoding="utf-8") as fh:
            wr = csv.DictWriter(fh, fieldnames=cols, extrasaction="ignore")
            wr.writeheader()
            for r in rows[typ]:
                r.setdefault("type", typ)
                if r.get("bpm_est") is None:
                    r["bpm_est"] = r.get("bpm_measured", r.get("bpm_file"))
                if r.get("key_est") is None:
                    r["key_est"] = r.get("key_measured")
                fence_row(r, usable)
                wr.writerow({k: _fmt(r.get(k)) for k in cols})
                total += 1
    with open("%s_accuracy.csv" % args.out, "w", newline="", encoding="utf-8") as fh:
        wr = csv.DictWriter(fh, fieldnames=["rar", "type", "measure", "tagged", "agree", "half_double", "relative", "error_pct", "usable"])
        wr.writeheader()
        for r in acc.rows():
            r["rar"] = rar
            wr.writerow({k: _fmt(v) for k, v in r.items()})
    counts = ", ".join("%s %d" % (t, len(rows[t])) for t in TYPES if rows[t])
    print("%d rows (%s)" % (total, counts))
    if skipped:
        print("skipped: " + ", ".join("%s %d" % kv for kv in sorted(skipped.items())))
    if not total:
        # nothing measured is a failure the caller must see (defect 36)
        print("no wav/aiff/midi file measured%s" % (
            ": skipped " + ", ".join("%s %d" % kv for kv in sorted(skipped.items())) if skipped else ""),
            file=sys.stderr)
    return 0 if total else 1


if __name__ == "__main__":
    sys.exit(main())
