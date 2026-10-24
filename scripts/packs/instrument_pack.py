"""Builds one multizone instrument pack (phase 38) from a free SFZ library listed in instruments.json.

usage: instrument_pack.py instruments.json INSTRUMENT_ID OUT_DIR [--cache DIR] [--source DIR]

The source is fetched once into the cache (a sparse git checkout at a pinned commit, only the
files the recipe needs), or read from --source (a folder already there). Its SFZ files are read
with a small SFZ v2 parser (headers, #define, #include, inheritance; ARIA extensions ignored;
regions kept as the instrument plays by default: the default keyswitch, the CC conditions met by
the default CC values, the sustain pedal up), then reduced to what a phone holds: the recipe's
keys (every N semitones at least), velocity layers per key (spread, the loudest kept) and round
robins. Each zone is cut to a length that shrinks with its pitch (`seconds`), faded out, given a
crossfaded sustain loop when the recipe says so (strings, brass, woodwinds held as long as the
key), resampled to 48 kHz, written as 16-bit FLAC z###.flac. The whole instrument is normalized
to -1 dBFS (the levels between zones and layers kept, the SFZ's volumes baked in) and its gain is
set so that middle C at velocity 100 plays about -20 dBFS RMS. pack.json lists the zones (the app's
MultiInstrument), one preview sound, the memory the zones take on the phone (16-bit, 48 kHz) and
the credits. A recipe over its memory budget is shortened until it fits.
"""
import json
import math
import re
import sys
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy.signal import resample_poly

sys.path.insert(0, str(Path(__file__).resolve().parent))
from library_pack import fetch  # noqa: E402  (same folder: the packs' sparse git fetch)

RATE = 48000
NOTE_NAMES = {'c': 0, 'd': 2, 'e': 4, 'f': 5, 'g': 7, 'a': 9, 'b': 11}


def note_number(v):
    """An SFZ key: a number, or a name like c4 / C#4 / db3 (C4 = 60, C-1 = 0)."""
    v = v.strip()
    if re.fullmatch(r'-?\d+', v):
        return int(v)
    m = re.fullmatch(r'([a-gA-G])([#b]?)(-?\d+)', v)
    if not m:
        raise ValueError(f'bad key {v!r}')
    n = NOTE_NAMES[m.group(1).lower()] + {'#': 1, 'b': -1, '': 0}[m.group(2)]
    return n + 12 * (int(m.group(3)) + 1)


# --- SFZ ------------------------------------------------------------------------------------

LEVELS = ['control', 'global', 'master', 'group', 'region']


class Sfz:
    """The regions of an SFZ file, each a dict of its opcodes with those it inherits."""

    def __init__(self, path, defines=None):
        self.root = Path(path).parent
        self.defines = dict(defines or {})
        self.levels = {k: {} for k in LEVELS}
        self.header = None
        self.regions = []
        self.control = {}
        self._read(Path(path))
        self._close_region()

    def _sub(self, text):
        # Longest names first: $DIR must not eat $DIRECTION.
        for k in sorted(self.defines, key=len, reverse=True):
            text = text.replace(k, self.defines[k])
        return text

    def _close_region(self):
        if self.header == 'region':
            merged = {}
            for k in LEVELS:
                merged.update(self.levels[k])
            self.regions.append(merged)
            self.levels['region'] = {}

    def _open(self, name):
        self._close_region()
        self.header = name
        if name in LEVELS:
            for k in LEVELS[LEVELS.index(name):]:
                self.levels[k] = {}

    def _opcode(self, key, value):
        if self.header in LEVELS:
            self.levels[self.header][key] = value
            if self.header == 'control':
                self.control[key] = value

    def _read(self, path):
        text = path.read_text(encoding='utf-8', errors='replace')
        text = re.sub(r'/\*.*?\*/', ' ', text, flags=re.S)
        for line in text.splitlines():
            line = re.sub(r'//.*', '', line)
            self._line(line)

    def _line(self, line):
        i, n = 0, len(line)
        while i < n:
            if line[i].isspace():
                i += 1
                continue
            m = re.match(r'#define\s+(\$\w+)\s+(\S+)', line[i:])
            if m:
                self.defines[m.group(1)] = m.group(2)
                i += m.end()
                continue
            m = re.match(r'#include\s+"([^"]+)"', line[i:])
            if m:
                inc = self.root / self._sub(m.group(1)).replace('\\', '/')
                self._read(inc)
                i += m.end()
                continue
            m = re.match(r'<(\w+)>', line[i:])
            if m:
                self._open(m.group(1))
                i += m.end()
                continue
            m = re.match(r'([\w$]+)=', line[i:])
            if not m:
                i += 1  # stray text: skipped
                continue
            key = self._sub(m.group(1))
            start = i + m.end()
            # A value runs to the next opcode, header or directive (sample names hold spaces).
            nxt = re.search(r'\s+(?=[\w$]+=|<\w+>|#define|#include)', line[start:])
            end = start + nxt.start() if nxt else n
            self._opcode(key, self._sub(line[start:end]).strip())
            i = end


def default_cc(control):
    """The CC values the instrument starts with (set_ccN, set_hdccN); the pedal (64) up."""
    cc = {}
    for k, v in control.items():
        m = re.fullmatch(r'set_(hd)?cc(\d+)', k)
        if m:
            cc[int(m.group(2))] = float(v) * (127.0 if m.group(1) else 1.0)
    cc[64] = 0.0
    return cc


def plays_by_default(r, cc):
    """True when the region sounds with the default CCs and keyswitch (no pedal, no extras)."""
    for k, v in r.items():
        m = re.fullmatch(r'(lo|hi)cc(\d+)', k)
        if m:
            value = cc.get(int(m.group(2)), 0.0)
            if m.group(1) == 'lo' and value < float(v):
                return False
            if m.group(1) == 'hi' and value > float(v):
                return False
    if 'sw_last' in r and 'sw_default' in r and note_number(r['sw_last']) != note_number(r['sw_default']):
        return False
    if any(k in r for k in ('sw_down', 'sw_up', 'sw_previous')):
        return False
    return r.get('trigger', 'attack') in ('attack', 'first', 'release')


def region_info(r, sfz, base_gain_db=0.0):
    """What a zone needs from a region: its file, keys, velocities, root, gain, round robin, trigger."""
    sample = (sfz.control.get('default_path', '') + r['sample']).replace('\\', '/')
    lo = note_number(r['lokey']) if 'lokey' in r else note_number(r['key']) if 'key' in r else 0
    hi = note_number(r['hikey']) if 'hikey' in r else note_number(r['key']) if 'key' in r else 127
    if 'pitch_keycenter' in r and r['pitch_keycenter'] != 'sample':
        root = note_number(r['pitch_keycenter'])
    elif 'key' in r:
        root = note_number(r['key'])
    else:
        root = 60
    root -= float(r.get('tune', 0)) / 100.0 + float(r.get('transpose', 0))
    gain_db = base_gain_db + float(r.get('volume', 0)) + float(r.get('group_volume', 0))
    if 'amplitude' in r:
        gain_db += 20 * math.log10(max(float(r['amplitude']), 1e-3) / 100.0)
    return {
        'path': sfz.root / sample, 'lo': lo, 'hi': hi, 'root': root,
        'lovel': int(r.get('lovel', 1)) or 1, 'hivel': int(r.get('hivel', 127)),
        'gain_db': gain_db, 'seq_len': int(r.get('seq_length', 1)), 'seq_pos': int(r.get('seq_position', 1)),
        'release': r.get('trigger', 'attack') == 'release', 'ampeg_release': float(r.get('ampeg_release', 0) or 0),
        'rt_decay': float(r.get('rt_decay', 0) or 0),
    }


# --- Reduction -------------------------------------------------------------------------------

def spread(items, n):
    """At most n items, evenly spread, the first and the last kept."""
    if n is None or len(items) <= n:
        return list(items)
    if n == 1:
        return [items[-1]]
    return [items[round(i * (len(items) - 1) / (n - 1))] for i in range(n)]


def pick_keys(centres, step):
    """Sampled keys at least `step` semitones apart, from the lowest (the highest always kept)."""
    centres = sorted(set(centres))
    kept = []
    for c in centres:
        if not kept or c - kept[-1] >= step:
            kept.append(c)
    if centres and kept[-1] != centres[-1]:
        if centres[-1] - kept[-1] < step / 2 and len(kept) > 1:
            kept[-1] = centres[-1]
        else:
            kept.append(centres[-1])
    return kept


def reduce_part(regions, part):
    """The zones of one part: its keys, layers and round robins, ranges rebuilt to cover it."""
    lo_part, hi_part = part.get('keys', [0, 127])
    attack = [r for r in regions if not r['release']]
    centres = [round(r['root']) for r in attack if lo_part - 12 <= round(r['root']) <= hi_part + 12]
    inside = [c for c in centres if lo_part <= c <= hi_part] or centres
    keys = pick_keys(inside, part.get('key_step', 1))
    zones = []
    for ki, key in enumerate(keys):
        # Each kept key covers half-way to its neighbours, the ends out to the part's own.
        lo = lo_part if ki == 0 else (keys[ki - 1] + key) // 2 + 1
        hi = hi_part if ki == len(keys) - 1 else (key + keys[ki + 1]) // 2
        mine = [r for r in attack if round(r['root']) == key]
        layers = sorted({(r['lovel'], r['hivel']) for r in mine}, key=lambda v: v[1])
        # The softest layers a phone speaker would not tell apart (or that need a filter we do not have).
        layers = layers[min(part.get('skip_soft_layers', 0), len(layers) - 1):]
        chosen = spread(layers, part.get('layers'))
        for li, layer in enumerate(chosen):
            vlo = 1 if li == 0 else chosen[li - 1][1] + 1
            vhi = 127 if li == len(chosen) - 1 else layer[1]
            turns = sorted([r for r in mine if (r['lovel'], r['hivel']) == layer], key=lambda r: (r['seq_pos'], str(r['path'])))
            # One region per round-robin turn (a layer's duplicates are the same turn).
            by_turn = {}
            for r in turns:
                by_turn.setdefault(r['seq_pos'] if r['seq_len'] > 1 else 1, r)
            picked = [by_turn[k] for k in sorted(by_turn)][: part.get('round_robins', 1)]
            for ti, r in enumerate(picked):
                zones.append({**r, 'lo': lo, 'hi': hi, 'lovel': vlo, 'hivel': vhi,
                              'seq_len': len(picked) if len(picked) > 1 else 1, 'seq_pos': ti + 1 if len(picked) > 1 else 0})
    if part.get('release_match'):
        rx = re.compile(part['release_match'])
        for r in regions:
            if r['release'] and rx.search(r['path'].name) and lo_part <= r['lo'] <= hi_part:
                zones.append({**r, 'lovel': max(1, r['lovel']), 'seq_len': 1, 'seq_pos': 0})
    return zones


def seconds_for(note, curve):
    """The piecewise-linear length (seconds) of a zone recorded at `note`."""
    pts = sorted(curve)
    if note <= pts[0][0]:
        return pts[0][1]
    for (n0, s0), (n1, s1) in zip(pts, pts[1:]):
        if note <= n1:
            return s0 + (s1 - s0) * (note - n0) / (n1 - n0)
    return pts[-1][1]


def rms_env(audio, hop, win):
    """RMS of [win]-frame windows every [hop] frames (window i centred on i * hop)."""
    power = np.concatenate([[0.0], np.cumsum((audio ** 2).mean(axis=1))])
    centres = np.arange(0, len(audio), hop)
    lo = np.clip(centres - win // 2, 0, len(audio))
    hi = np.clip(centres + win // 2, 0, len(audio))
    return np.sqrt((power[hi] - power[lo]) / np.maximum(hi - lo, 1))


def make_loop(audio, length):
    """The sustain loop of a held note, made in place; returns (loop start, loop end).

    The wrap must not be heard: no level step (vibrato strings, brass and woodwinds swell and
    fade, and a 3 dB step every loop pumps), no click, no phasey bump.
      1. Where: the loop ends at the zone's end; its start is searched in the steadier half after
         the attack (at least 1.2 s of loop), where the level and the waveform around it best match
         those just before the end.
      2. Flat: from 0.4 s before the start to the end the slow level (300 ms RMS) is evened out to
         its value at the start (at most ±6 dB, smooth): the swell or fade of the recording no
         longer jumps back at the wrap. Vibrato and the faster tremolo stay.
      3. Level: a slow gain along the loop brings its end to the level of what precedes its start.
      4. Seamless: the last 0.4 s are crossfaded with the 0.4 s before the start, the gains set for
         how alike the two are (equal gain when in phase, equal power when not: the sum keeps its level).
    """
    hop = RATE // 100
    env = rms_env(audio, hop, int(0.05 * RATE))
    # The loop ends before the recording's own release (its level 3 dB under the held part's).
    held = np.median(env[int(0.3 * length) // hop:length // hop])
    steady = np.nonzero(env[:length // hop] >= held * 10 ** (-3 / 20))[0]
    le = min(length - 4, int(steady[-1]) * hop) if len(steady) else length - 4
    le = max(le, int(0.3 * length) + int(1.6 * RATE))
    le = min(le, length - 4)
    xf = min(int(0.4 * RATE), int(0.3 * (le - int(0.3 * length))))
    first = max(int(0.3 * length), xf + int(0.05 * RATE))
    last = le - int(1.2 * RATE)
    if last <= first:
        last = first + 1
    mono = audio.mean(axis=1)
    end_env = env[min(len(env) - 1, (le - RATE // 40) // hop)]
    tail = mono[le - RATE // 50:le]
    best, ls = None, first
    for cand in range(first, last, hop):
        e = env[min(len(env) - 1, (cand - RATE // 40) // hop)]
        level = abs(20 * np.log10(max(e, 1e-9) / max(end_env, 1e-9)))
        head = mono[cand - RATE // 50:cand]
        corr = np.dot(head, tail) / (np.linalg.norm(head) * np.linalg.norm(tail) + 1e-12)
        # Where the recording itself holds still (its 200 ms before and after alike): the wrap
        # replays that spot, so a swell there would be heard at every loop.
        w = RATE // 5
        still = abs(20 * np.log10((np.sqrt(np.mean(audio[cand:cand + w] ** 2)) + 1e-9) / (np.sqrt(np.mean(audio[cand - w:cand] ** 2)) + 1e-9)))
        cost = level + 1.5 * (1 - corr) + 2.0 * still
        if best is None or cost < best:
            best, ls = cost, cand
    # 2. The slow level evened out from 0.4 s before the start to the end.
    from_ = max(0, ls - xf)
    slow = rms_env(audio, hop, int(0.3 * RATE))
    idx = np.arange(from_, length)
    level = np.interp(idx, np.arange(len(slow)) * hop, slow)
    target = np.interp(ls, np.arange(len(slow)) * hop, slow)
    gain = np.clip(target / np.maximum(level, 1e-9), 10 ** (-6 / 20), 10 ** (6 / 20))
    # Eased in over the 0.1 s before: no step where the evening out starts.
    ramp = min(len(gain), RATE // 10)
    gain[:ramp] = 1 + (gain[:ramp] - 1) * np.linspace(0, 1, ramp)
    audio[from_:length] *= gain[:, None]
    # 3. What is left of a level difference between the end and the start (the evening out is
    #    smooth): a gain moving slowly along the loop (a fraction of a dB a second, not heard)
    #    brings the loop's last 0.4 s to the level of the 0.4 s before its start, which it is
    #    crossfaded into. Before the crossfade: the sample just before the wrap is then the one just
    #    before the start, untouched (no click).
    for _ in range(2):
        tail_rms = np.sqrt(np.mean(audio[le - xf:le] ** 2))
        head_rms = np.sqrt(np.mean(audio[ls - xf:ls] ** 2))
        if tail_rms <= 0 or head_rms <= 0:
            break
        g = head_rms / tail_rms
        ramp_to = le - xf
        audio[ls:ramp_to] *= np.linspace(1.0, g, ramp_to - ls)[:, None]
        audio[ramp_to:le] *= g
    # 4. The crossfade, its gains for how alike the two are; the outgoing end first follows the
    #    level of what comes in, 50 ms at a time (a swell of the bow inside the crossfade).
    tail = audio[le - xf:le].copy()
    head = audio[ls - xf:ls].copy()
    win = RATE // 20
    follow = np.clip(rms_env(head, 1, win) / np.maximum(rms_env(tail, 1, win), 1e-9), 0.5, 2.0)[:len(tail)]
    tail *= follow[:, None]
    # How alike they are, 50 ms at a time (a vibrato drifts in and out of phase).
    def sliding(x):
        cs = np.concatenate([[0.0], np.cumsum(x)])
        i = np.arange(len(x))
        lo, hi = np.clip(i - win // 2, 0, len(x)), np.clip(i + win // 2, 0, len(x))
        return cs[hi] - cs[lo]
    cross = sliding((tail * head).sum(axis=1))
    energy = np.sqrt(sliding((tail ** 2).sum(axis=1)) * sliding((head ** 2).sum(axis=1)))
    c = np.clip(cross / np.maximum(energy, 1e-12), 0.0, 1.0)[:, None]
    t = np.linspace(0, 1, xf)[:, None]
    fin, fout = np.sin(t * np.pi / 2) ** 2, np.cos(t * np.pi / 2) ** 2  # equal gain (sum 1)
    norm = np.sqrt(fin ** 2 + fout ** 2 + 2 * fin * fout * c)
    audio[le - xf:le] = (tail * fout + head * fin) / norm
    audio[le:le + 4] = audio[ls:ls + 4]
    return ls, le


def prepare(path, gain_db, seconds, loop, release_seconds):
    """The zone's audio at 48 kHz: cut, faded, looped when asked. Returns (audio, loop_start, loop_end)."""
    audio, rate = sf.read(path, always_2d=True, dtype='float64')
    if audio.shape[1] > 2:
        audio = audio[:, :2]
    if rate != RATE:
        g = math.gcd(RATE, rate)
        audio = resample_poly(audio, RATE // g, rate // g, axis=0)
    audio *= 10 ** (gain_db / 20)
    if audio.shape[1] == 2 and np.abs(audio[:, 0] - audio[:, 1]).max() < 1e-3 * max(np.abs(audio).max(), 1e-9):
        audio = audio[:, :1]
    mono = np.abs(audio).max(axis=1)
    peak = mono.max()
    if peak <= 0:
        return None
    # The attack is where the sound starts (a little silence before it is trimmed, 1 ms kept).
    first = int(np.argmax(mono > peak * 1e-3))
    audio = audio[max(0, first - RATE // 1000):]
    # The natural end: where it falls 70 dB under its peak.
    loud = np.nonzero(np.abs(audio).max(axis=1) > peak * 10 ** (-70 / 20))[0]
    natural = (loud[-1] + 1) if len(loud) else len(audio)
    length = min(natural, int(seconds * RATE))
    audio = audio[:length].copy()
    if loop and length > RATE:
        ls, le = make_loop(audio, length)
        return audio[:le + 4], ls, le
    # Cut short: fades out over the last fifth (0.15..1 s), like a damped string; whole: 20 ms.
    fade = min(len(audio) // 2, int(np.clip(0.2 * len(audio), 0.15 * RATE, RATE))) if length < natural else min(len(audio) // 2, RATE // 50)
    if fade > 0:
        audio[-fade:] *= (np.cos(np.linspace(0, np.pi / 2, fade)) ** 2)[:, None]
    return audio, 0, 0


def write_preview(path, zones, audios, norm, gain_db, inst):
    """A few seconds that show the instrument before it is downloaded: an arpeggio on C major then
    the chord (strings and brass: the chord held), each note from the zone the engine would play at
    velocity 90, shifted there (linear interpolation: a preview), at the instrument's gain."""
    lo = min(z['lo'] for z in zones)
    hi = max(z['hi'] for z in zones)
    base = 48 if hi < 72 else 60
    while base + 12 > hi and base - 12 >= lo:
        base -= 12
    while base < lo:
        base += 12
    notes = [base, base + 4, base + 7, base + 12]
    held = inst.get('category') in ('strings', 'brass', 'woodwinds')
    step = 0.45 if not held else 0.6
    length = int((len(notes) * step + 2.6) * RATE)
    mix = np.zeros((length, 2))
    starts = [i * step for i in range(len(notes))] + [len(notes) * step] * len(notes)
    for k, (note, at) in enumerate(zip(notes + notes, starts)):
        pick = [i for i, z in enumerate(zones) if not z['release'] and z['lo'] <= note <= z['hi'] and z['lovel'] <= 90 <= z['hivel']]
        if not pick:
            continue
        i = min(pick, key=lambda j: zones[j]['seq_pos'])
        a, ls, le = audios[i]
        if a.shape[1] == 1:
            a = np.repeat(a, 2, axis=1)
        ratio = 2 ** ((note - zones[i]['root']) / 12)
        dur = (len(notes) * step + 2.4 - at) if k >= len(notes) or not held else step
        frames = int(dur * RATE)
        pos = np.arange(frames) * ratio
        if le > ls:  # looped: the loop goes on as in the engine
            over = pos >= le
            pos[over] = ls + np.mod(pos[over] - ls, le - ls)
        pos = pos[pos < len(a) - 1]
        i0 = pos.astype(int)
        t = (pos - i0)[:, None]
        note_audio = a[i0] * (1 - t) + a[i0 + 1] * t
        fade = min(len(note_audio), int(0.3 * RATE))
        note_audio[-fade:] *= np.linspace(1, 0, fade)[:, None]
        begin = int(at * RATE)
        n = min(len(note_audio), length - begin)
        mix[begin:begin + n] += note_audio[:n] * (0.7 if k >= len(notes) else 1.0)
    mix *= norm * 10 ** (gain_db / 20)
    peak = np.abs(mix).max()
    if peak > 0.89:
        mix *= 0.89 / peak
    tail = min(len(mix), int(0.5 * RATE))
    mix[-tail:] *= np.linspace(1, 0, tail)[:, None]
    sf.write(path, mix.astype(np.float32), RATE, subtype='PCM_16')
    return path.name


def main():
    lib_path, inst_id, out_dir = sys.argv[1:4]
    cache = Path(sys.argv[sys.argv.index('--cache') + 1]) if '--cache' in sys.argv else Path('cache')
    lib = json.loads(Path(lib_path).read_text())
    sources = {s['id']: s for s in lib['sources']}
    inst = next(i for i in lib['instruments'] if i['id'] == inst_id)
    src = sources[inst['source']]
    if '--source' in sys.argv:
        root = Path(sys.argv[sys.argv.index('--source') + 1])
    else:
        # One checkout per instrument: each fetches only its own recordings.
        root = fetch({**src, 'id': inst_id, 'paths': inst['paths']}, cache) / 'repo'
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)

    zones = []
    for part in inst['parts']:
        sfz = Sfz(root / part['sfz'], part.get('defines'))
        cc = default_cc(sfz.control)
        regions = [region_info(r, sfz, part.get('gain_db', 0.0)) for r in sfz.regions if 'sample' in r and plays_by_default(r, cc)]
        # Only the recordings fetched (the recipe's sparse paths pick the layers worth the download).
        regions = [r for r in regions if r['path'].is_file()]
        got = reduce_part(regions, part)
        print(f"{inst_id}: {part['sfz']}: {len(regions)} regions, {len(got)} zones", flush=True)
        zones += [{**z, 'part': part} for z in got]
    if not zones:
        raise SystemExit(f'{inst_id}: no zone')

    budget = inst.get('max_ram_mb', 64) * 1e6
    scale = 1.0
    while True:
        audios, total = [], 0
        for z in zones:
            p = z['part']
            secs = seconds_for(z['root'], p.get('seconds', inst.get('seconds', [[0, 4], [127, 4]]))) * scale
            if z['release']:
                secs = min(secs, p.get('release_seconds', 1.0))
            got = prepare(z['path'], z['gain_db'], secs, p.get('loop', False) and not z['release'], 0)
            if got is None:
                raise SystemExit(f"{inst_id}: {z['path'].name} is silent")
            audios.append(got)
            total += got[0].size * 2
        if total <= budget or scale < 0.3:
            break
        scale *= 0.9 * budget / total
        print(f'{inst_id}: {total / 1e6:.1f} MB over the budget, lengths x {scale:.2f}', flush=True)

    peak = max(np.abs(a).max() for a, _, _ in audios)
    norm = 10 ** (-1 / 20) / peak
    # Gain: middle C (or the nearest key) at velocity 100 plays about -20 dBFS RMS on its first second.
    ref = min((z for z in zones if not z['release'] and z['lovel'] <= 100 <= z['hivel']),
              key=lambda z: (abs(60 - (z['lo'] + z['hi']) / 2), z['seq_pos']))
    ra = audios[zones.index(ref)][0][:RATE] * norm
    ref_db = 20 * math.log10(max(np.sqrt(np.mean(ra ** 2)), 1e-9))
    gain_db = round(float(np.clip(inst.get('target_rms_db', -20.0) - ref_db, -12.0, 12.0)), 1)

    manifest_zones, ram = [], 0
    for n, (z, (a, ls, le)) in enumerate(zip(zones, audios), start=1):
        name = f'z{n:03d}.flac'
        sf.write(out / name, (a * norm).astype(np.float32), RATE, subtype='PCM_16')
        ram += a.size * 2
        manifest_zones.append({
            'file': name, 'lokey': z['lo'], 'hikey': z['hi'], 'root': round(z['root'], 3),
            'lovel': z['lovel'], 'hivel': z['hivel'], 'seq': z['seq_pos'], 'seqLength': z['seq_len'],
            'release': z['release'], 'loopStart': int(ls), 'loopEnd': int(le),
        })
    preview = write_preview(out / 'apercu.flac', zones, audios, norm, gain_db, inst)
    releases = [z['ampeg_release'] for z in zones if not z['release'] and z['ampeg_release'] > 0]
    release = inst.get('release', round(float(np.median(releases)), 2) if releases else 0.5)
    rt = [z['rt_decay'] for z in zones if z['release'] and z['rt_decay'] > 0]
    credit = {'title': src['title'], 'author': src['author'], 'licence': src['licence'], 'page': src['page']}
    manifest = {
        'id': inst['id'], 'name': inst['name'], 'family': 'Instruments', 'bpm': 120, 'version': inst['version'],
        'sounds': [{'file': preview, 'kind': inst.get('kind', 'keys'), 'name': inst['name'], 'tags': inst.get('tags', [])}],
        'instrument': {
            'category': inst['category'], 'release': release, 'attack': inst.get('attack', 0.001),
            'velTrack': inst.get('veltrack', 0.5), 'gainDb': gain_db, 'releaseDecay': rt[0] if rt else 0.0,
            'fallback': inst.get('fallback', ''), 'memoryBytes': ram, 'zones': manifest_zones,
            'attribution': src.get('attribution', ''),
        },
        'credits': [credit],
    }
    (out / 'pack.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=1))
    disk = sum((out / z['file']).stat().st_size for z in manifest_zones)
    print(f"{inst_id}: {len(manifest_zones)} zones, {ram / 1e6:.1f} MB on the phone, {disk / 1e6:.1f} MB of FLAC, gain {gain_db:+.1f} dB", flush=True)


if __name__ == '__main__':
    main()
