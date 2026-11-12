#!/usr/bin/env python3
"""Checks scripts/packs/trial_drums.py, the drum packs of the modern-sounds trial
(docs/sons/PLAN-SONS-MODERNES.md § 6).

usage: python3 -I scripts/packs/test_trial_drums.py [--script PATH]

First checks the script's own measures (--report) on signals whose figures are known exactly.
Then builds the three packs twice in a temporary folder and checks, with this file's own measures:
- pack.json reads, its kinds are the app's (SoundPacks.KIND_LABELS, read from the Kotlin source),
  3 to 5 sounds of each kind the trial asks for, safe file names, the tech house « toc » first;
- each file is a 16-bit 48 kHz PCM WAV, mono for kicks and snares, stereo for the rest; kicks,
  hats, open hats and rides as long as the trial asks (snares, claps, percs within loose bounds);
  nothing at full scale, no DC above 0.001;
- the peak of each kind, within 0.3 dB (trial n° 2, Benjamin on n° 1: « tu rajoutes juste des
  cymbales on dirait »): kicks, snares and claps -1 dBFS, percussion -4, hats, open hats, rides -8;
- kicks: the fundamental over 60-200 ms, techno 43.65 Hz and tech house 49.0 Hz within 1.5 %,
  jump up 52-63 Hz; at least -12 dB of their energy in 250 Hz-1.5 kHz (the harmonics a phone's
  speaker plays) and at least -22 dB in 1.2-4.5 kHz (the click); at least -18.5 dB of full scale²
  x seconds in 60-250 Hz (the body, trial n° 3: the kicks of trials n° 1 and n° 2 have -23.9 to
  -18.9, n° 3 -16.4 to -13.5; a 0 dBFS sine lasting 1 s has -3);
- snares and claps: at least -6 dB of their energy in 250 Hz-2 kHz (the body), a tail (start to
  the last sample within 40 dB of the peak) of 0.25 to 0.6 s;
- hats, open hats and rides: no more energy above 10 kHz than in 4-10 kHz; hats with more than
  -15 dB of their energy above 8 kHz; claps and hats with side against centre between -25 and
  -3 dB (stereo, yet mono-compatible);
- mesures.json agrees with this file's measures; LICENSE.txt says the sounds are ours;
- the two builds are identical byte for byte.
Energies are read on the sum of the channels, each band against everything from 20 Hz up.
--script checks another copy of the script (to see this test fail on a broken one).
Exit code 0 when every check passes; each failure is printed.
"""
import importlib.util
import json
import math
import re
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

REPO = Path(__file__).resolve().parents[2]
SOUND_PACKS = REPO / 'project-model/src/main/kotlin/com/pocketstudio/model/SoundPacks.kt'
RATE = 48000
LICENSE = ('Sons originaux de Pocket Studio, fabriqués par synthèse maison (scripts/packs/trial_drums.py) : '
           'aucun enregistrement ni échantillon tiers.')
KINDS = {
    'essai_jump_up': {'kick', 'snare', 'clap', 'hat', 'perc'},
    'essai_tech_house': {'kick', 'clap', 'hat', 'open_hat', 'perc', 'ride'},
    'essai_techno': {'kick', 'clap', 'hat', 'open_hat', 'perc', 'ride'},
}
TUNED = {'essai_techno': 43.654, 'essai_tech_house': 48.999}
MONO = {'kick', 'snare'}
PEAKS = {'kick': -1.0, 'snare': -1.0, 'clap': -1.0, 'perc': -4.0, 'hat': -8.0, 'open_hat': -8.0, 'ride': -8.0}
PEAK_TOLERANCE = 0.3
KICK_HARMONICS = (250, 1500, -12.0)  # band (Hz) and the least share of a kick's energy in it (dB)
KICK_CLICK = (1200, 4500, -22.0)
KICK_BODY = (60, 250, -18.5)  # band (Hz) and the least energy in it, dB of full scale² x seconds
BODY = (250, 2000, -6.0)  # snares and claps
TAIL_BELOW_DB, TAIL_S = 40.0, (0.25, 0.6)
METAL = {'hat', 'open_hat', 'ride'}
# The report's bands (Hz), each against 20 Hz-20 kHz.
REPORT_BANDS = {'sub_db': (20, 60), 'low_db': (60, 250), 'mid_db': (250, 4000), 'high_db': (4000, 20000),
                'e250_1500_db': (250, 1500), 'e1200_4500_db': (1200, 4500), 'e250_2000_db': (250, 2000)}
# Lengths the trial asks for (s); the kicks' depend on the style.
LENGTHS = {'hat': (0.04, 0.09), 'open_hat': (0.2, 0.4), 'ride': (0.8, 1.5), 'snare': (0.2, 0.5),
           'clap': (0.15, 0.9), 'perc': (0.05, 0.4)}
KICK_LENGTHS = {'essai_techno': (0.45, 0.6), 'essai_tech_house': (0.3, 0.4), 'essai_jump_up': (0.2, 0.28)}

failures = []
checks = 0


def check(ok, what):
    global checks
    checks += 1
    if not ok:
        failures.append(what)
        print('FAIL', what, flush=True)


def app_kinds():
    text = SOUND_PACKS.read_text(encoding='utf-8')
    block = re.search(r'val KIND_LABELS = linkedMapOf\((.*?)\n    \)', text, re.S).group(1)
    return set(re.findall(r'"([^"]+)" to "', block))


def read(path):
    with wave.open(str(path), 'rb') as w:
        info = (w.getcomptype(), w.getsampwidth(), w.getframerate(), w.getnchannels())
        raw = np.frombuffer(w.readframes(w.getnframes()), dtype='<i2')
    return info, raw.reshape(-1, info[3])


def energy(x, lo, hi=math.inf):
    """Power of the channels' sum between lo and hi (Hz), from its unpadded spectrum."""
    p = np.abs(np.fft.rfft(x.sum(axis=1))) ** 2
    f = np.fft.rfftfreq(len(x), 1.0 / RATE)
    return float(p[(f >= lo) & (f < hi)].sum()) + 1e-30


def share(x, lo, hi=math.inf):
    """dB of the energy between lo and hi against everything from 20 Hz up."""
    return 10 * np.log10(energy(x, lo, hi) / energy(x, 20))


def band_energy_db(x, lo, hi):
    """Energy of the channels' sum between lo and hi (Hz), in dB of full scale² x seconds: a 0 dBFS
    sine lasting 1 s has -3.01 dB (Parseval on the one-sided spectrum)."""
    return 10 * np.log10(2 * energy(x, lo, hi) / (len(x) * RATE))


def tail_of(x):
    """Seconds from the start to the last sample of any channel within TAIL_BELOW_DB of the peak."""
    a = np.abs(x).max(axis=1)
    return (np.flatnonzero(a >= a.max() * 10 ** (-TAIL_BELOW_DB / 20))[-1] + 1) / RATE


def side_centre(x):
    left, right = x[:, 0], x[:, 1]
    return 10 * np.log10(np.sum((left - right) ** 2) / np.sum((left + right) ** 2))


def f0_of(x):
    """Fundamental over 60-200 ms: Blackman window, 2^19-point spectrum, the peak in 30-100 Hz
    refined on a parabola through the magnitudes."""
    seg = x.mean(axis=1)[int(0.06 * RATE):int(0.2 * RATE)]
    n = 1 << 19
    mag = np.abs(np.fft.rfft((seg - seg.mean()) * np.blackman(len(seg)), n))
    lo, hi = int(30 * n / RATE), int(100 * n / RATE)
    i = lo + int(np.argmax(mag[lo:hi]))
    a, b, c = mag[i - 1], mag[i], mag[i + 1]
    return (i + 0.5 * (a - c) / (a - 2 * b + c)) * RATE / n


def load(script):
    sys.dont_write_bytecode = True
    spec = importlib.util.spec_from_file_location('trial_drums_under_test', script)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def check_measures(td):
    """The script's --report measures on signals whose figures are known."""
    t = np.arange(RATE) / RATE
    tone = np.sin(2 * np.pi * 1000 * t)
    m = td.measure(np.stack([tone, tone], axis=1), RATE)
    check(abs(m['peak_dbfs']) < 0.01 and abs(m['rms_dbfs'] + 3.01) < 0.02 and abs(m['crest_db'] - 3.01) < 0.02,
          f'report: a full-scale sine reads peak {m["peak_dbfs"]}, rms {m["rms_dbfs"]}, crest {m["crest_db"]}')
    check(m['side_centre_db'] < -100, f'report: identical sides read side/centre {m["side_centre_db"]} dB')
    d = 0.1 * np.sin(2 * np.pi * 3000 * t)
    m = td.measure(np.stack([0.5 * tone + d, 0.5 * tone - d], axis=1), RATE)
    check(abs(m['side_centre_db'] + 13.98) < 0.05, f'report: side 0.1 against centre 0.5 reads {m["side_centre_db"]} dB')
    m = td.measure(0.4 * (tone + np.sin(2 * np.pi * 12000 * t))[:, None], RATE)
    check(abs(m['air_10k_plus_db'] + 3.01) < 0.1 and abs(m['centroid_hz'] - 6500) < 30 and m['side_centre_db'] is None,
          f'report: 1 kHz + 12 kHz reads air {m["air_10k_plus_db"]} dB, centroid {m["centroid_hz"]} Hz')
    m = td.measure((0.3 * np.ones(RATE) + 0.1 * tone)[:, None], RATE)
    check(abs(m['dc'] - 0.3) < 1e-3, f'report: an offset of 0.3 reads dc {m["dc"]}')
    tk = t[:int(0.3 * RATE)]
    m = td.measure((0.8 * np.sin(2 * np.pi * 43.654 * tk) * np.exp(-tk / 0.12))[:, None], RATE, 'kick')
    check(m['f0_hz'] is not None and abs(m['f0_hz'] / 43.654 - 1) < 0.002, f'report: a 43.654 Hz decaying sine reads {m["f0_hz"]} Hz')
    # Seven sines, each band's share known from their amplitudes (no two bands share a figure).
    tones = {40: 0.6, 100: 0.5, 1000: 0.4, 1800: 0.32, 3000: 0.35, 6000: 0.3, 12000: 0.15}
    m = td.measure(0.35 * sum(a * np.sin(2 * np.pi * f * t) for f, a in tones.items())[:, None], RATE)
    total = sum(a * a for a in tones.values())
    for key, (lo, hi) in REPORT_BANDS.items():
        want = 10 * np.log10(sum(a * a for f, a in tones.items() if lo <= f < hi) / total)
        check(isinstance(m.get(key), float) and abs(m[key] - want) < 0.1, f'report: {key} reads {m.get(key)}, not {want:.2f}')
    want = 20 * np.log10(tones[12000] / tones[6000])
    got = m.get('top_10k_vs_4_10k_db')
    check(isinstance(got, float) and abs(got - want) < 0.1, f'report: 12 kHz against 6 kHz reads {got}, not {want:.2f}')
    # Two decaying sines: the tail is the longer one's, 40 dB down at tau x ln 100.
    left, right = (np.sin(2 * np.pi * 1000 * t + ph) * np.exp(-t / tau) for ph, tau in ((0.0, 0.04), (1.0, 0.08)))
    m = td.measure(np.stack([left, right], axis=1), RATE)
    want = 0.08 * np.log(100)
    check(abs(m.get('tail_s', 99) - want) < 0.002, f'report: a tail of {want:.3f} s reads {m.get("tail_s")}')


def check_pack(root, pack, kinds_ok, report):
    d = root / pack
    try:
        manifest = json.loads((d / 'pack.json').read_text(encoding='utf-8'))
    except (OSError, ValueError) as e:
        check(False, f'{pack}: pack.json unreadable ({e})')
        return
    check(manifest.get('id') == pack and isinstance(manifest.get('name'), str) and 0 < len(manifest['name']) <= 40
          and isinstance(manifest.get('family'), str) and isinstance(manifest.get('bpm'), float)
          and isinstance(manifest.get('version'), int) and manifest['version'] >= 1,
          f'{pack}: pack.json id/name/family/bpm/version wrong: { {k: v for k, v in manifest.items() if k != "sounds"} }')
    sounds = manifest.get('sounds', [])
    check((d / 'LICENSE.txt').is_file() and (d / 'LICENSE.txt').read_text(encoding='utf-8').strip() == LICENSE,
          f'{pack}: LICENSE.txt missing or not the trial\'s text')
    kinds = [s.get('kind') for s in sounds]
    check(set(kinds) == KINDS[pack], f'{pack}: kinds {sorted(set(kinds))}, expected {sorted(KINDS[pack])}')
    check(set(kinds) <= kinds_ok, f'{pack}: kinds unknown to the app: {sorted(set(kinds) - kinds_ok)}')
    for kind in KINDS[pack]:
        check(3 <= kinds.count(kind) <= 5, f'{pack}: {kinds.count(kind)} sounds of kind {kind} (3 to 5 wanted)')
    files = [s.get('file', '') for s in sounds]
    check(len(set(files)) == len(files), f'{pack}: a file is listed twice')
    check(sorted(p.name for p in d.glob('*.wav')) == sorted(files), f'{pack}: WAV files not listed in pack.json, or missing')
    if pack == 'essai_tech_house':
        percs = [s for s in sounds if s.get('kind') == 'perc']
        check(bool(percs) and 'toc' in percs[0].get('tags', []), f'{pack}: the first perc is not the « toc »')
        check(any('shaker' in s.get('tags', []) for s in percs), f'{pack}: no shaker among the percs')

    for s in sounds:
        file, kind = s.get('file', ''), s.get('kind')
        where = f'{pack}/{file}'
        check(1 <= len(file) <= 80 and re.fullmatch(r'[A-Za-z0-9._-]+', file) is not None and not file.startswith('.')
              and re.fullmatch(rf'{re.escape(str(kind))}_\d\d\.wav', file) is not None, f'{where}: unsafe or unexpected name')
        check(isinstance(s.get('name'), str) and 0 < len(s['name']) <= 40 and isinstance(s.get('tags'), list)
              and all(isinstance(x, str) and len(x) <= 30 for x in s['tags']), f'{where}: name or tags wrong')
        if not (d / file).is_file():
            check(False, f'{where}: missing')
            continue
        try:
            (comp, width, rate, channels), raw = read(d / file)
        except (wave.Error, EOFError, ValueError) as e:
            check(False, f'{where}: not a readable WAV ({e})')
            continue
        check(comp == 'NONE' and width == 2 and rate == RATE, f'{where}: {comp} {8 * width}-bit {rate} Hz, not 16-bit PCM 48 kHz')
        check(channels == (1 if kind in MONO else 2), f'{where}: {channels} channel(s)')
        x = raw.astype(float) / 32768.0
        if len(x) == 0:
            check(False, f'{where}: empty')
            continue
        seconds = len(x) / RATE
        lo, hi = KICK_LENGTHS[pack] if kind == 'kick' else LENGTHS.get(kind, (0.0, 2.0))
        check(lo - 1e-6 <= seconds <= hi + 1e-6, f'{where}: lasts {seconds:.3f} s, wanted {lo}-{hi}')
        peak = 20 * np.log10(np.max(np.abs(x)) + 1e-12)
        want = PEAKS.get(kind, -1.0)
        check(abs(peak - want) <= PEAK_TOLERANCE, f'{where}: peak {peak:.2f} dBFS, wanted {want} within {PEAK_TOLERANCE} dB')
        check(np.max(np.abs(raw.astype(int))) < 32767, f'{where}: samples at full scale (clipped)')
        dc = float(np.max(np.abs(x.mean(axis=0))))
        check(dc <= 0.001, f'{where}: DC {dc:.5f}')
        f0 = None
        if kind == 'kick':
            f0 = f0_of(x)
            if pack in TUNED:
                check(abs(f0 / TUNED[pack] - 1) <= 0.015, f'{where}: fundamental {f0:.2f} Hz, not {TUNED[pack]} Hz within 1.5 %')
            else:
                check(52 <= f0 <= 63, f'{where}: fundamental {f0:.2f} Hz, not 52-63 Hz')
            for lo, hi, least in (KICK_HARMONICS, KICK_CLICK):
                got = share(x, lo, hi)
                check(got >= least, f'{where}: {got:.1f} dB of its energy in {lo}-{hi} Hz (at least {least} wanted)')
            lo, hi, least = KICK_BODY
            got = band_energy_db(x, lo, hi)
            check(got >= least, f'{where}: {got:.1f} dB FS²·s in {lo}-{hi} Hz, the body (at least {least} wanted)')
        if kind in ('snare', 'clap'):
            got = share(x, BODY[0], BODY[1])
            check(got >= BODY[2], f'{where}: {got:.1f} dB of its energy in {BODY[0]}-{BODY[1]} Hz (at least {BODY[2]} wanted)')
            got = tail_of(x)
            check(TAIL_S[0] <= got <= TAIL_S[1], f'{where}: tail {got:.3f} s ({TAIL_S[0]}-{TAIL_S[1]} wanted)')
        top = 10 * np.log10(energy(x, 10000) / energy(x, 4000, 10000))
        if kind in METAL:
            check(top <= 0, f'{where}: {top:.1f} dB more energy above 10 kHz than in 4-10 kHz (at most 0 wanted)')
        if kind in ('hat', 'open_hat'):
            air = share(x, 8000)
            check(air > -15, f'{where}: {air:.1f} dB above 8 kHz (more than -15 wanted)')
        sc = side_centre(x) if channels == 2 else None
        if kind in ('clap', 'hat', 'open_hat') and sc is not None:
            check(-25 <= sc <= -3, f'{where}: side/centre {sc:.1f} dB (-25 to -3 wanted)')

        m = (report or {}).get(pack, {}).get(file)
        if m is None:
            check(False, f'{where}: missing from mesures.json')
            continue
        numbers = [v for v in m.values() if v is not None]
        check(all(isinstance(v, (int, float)) and math.isfinite(v) for v in numbers), f'{where}: mesures.json has NaN or text: {m}')
        check(abs(m.get('peak_dbfs', 99) - peak) < 0.05, f'{where}: mesures.json peak {m.get("peak_dbfs")} against {peak:.2f}')
        check(abs(m.get('duration_s', 99) - seconds) < 0.002, f'{where}: mesures.json duration {m.get("duration_s")}')
        check(abs(m.get('dc', 99) - dc) < 1e-5, f'{where}: mesures.json dc {m.get("dc")} against {dc:.6f}')
        if sc is None:
            check(m.get('side_centre_db') is None, f'{where}: mesures.json gives a side to a mono file')
        else:
            got = m.get('side_centre_db')
            check(got is not None and abs(got - sc) < 0.1, f'{where}: mesures.json side/centre {got} against {sc:.2f}')
        if f0 is not None:
            got = m.get('f0_hz')
            check(got is not None and abs(got / f0 - 1) < 0.005, f'{where}: mesures.json f0 {got} against {f0:.2f}')
        for key in ('rms_dbfs', 'crest_db', 'air_10k_plus_db', 'centroid_hz', 'top_10k_vs_4_10k_db', 'tail_s',
                    *REPORT_BANDS):
            check(key in m, f'{where}: mesures.json lacks {key}')
        got = m.get('tail_s')
        check(got is not None and abs(got - tail_of(x)) < 0.002, f'{where}: mesures.json tail {got} against {tail_of(x):.3f}')
        own = {'e250_1500_db': share(x, 250, 1500), 'e1200_4500_db': share(x, 1200, 4500),
               'e250_2000_db': share(x, 250, 2000), 'top_10k_vs_4_10k_db': top}
        keys = {'kick': ('e250_1500_db', 'e1200_4500_db'), 'snare': ('e250_2000_db',), 'clap': ('e250_2000_db',)}
        for key in keys.get(kind, ('top_10k_vs_4_10k_db',) if kind in METAL else ()):
            got = m.get(key)
            check(got is not None and abs(got - own[key]) < 0.2, f'{where}: mesures.json {key} {got} against {own[key]:.2f}')


def build(script, out):
    run = subprocess.run([sys.executable, '-I', str(script), str(out), '--report'], capture_output=True, text=True)
    check(run.returncode == 0, f'{script.name} {out.name} failed: {run.stderr.strip()[-600:]}')
    return run.returncode == 0


def main(argv):
    script = Path(argv[argv.index('--script') + 1]) if '--script' in argv else Path(__file__).resolve().parent / 'trial_drums.py'
    sine = np.sin(2 * np.pi * 100 * np.arange(RATE) / RATE)[:, None]
    got = band_energy_db(sine, 60, 250)
    check(abs(got + 3.01) < 0.01, f'this test: a 0 dBFS 100 Hz sine of 1 s reads {got:.2f} dB FS²·s, not -3.01')
    kinds_ok = app_kinds()
    check({'kick', 'snare', 'clap', 'hat', 'open_hat', 'perc', 'ride'} <= kinds_ok, f'KIND_LABELS read as {sorted(kinds_ok)}')
    try:
        check_measures(load(script))
    except Exception as e:  # a script that does not even load
        check(False, f'{script.name}: measures not checkable ({type(e).__name__}: {e})')
    with tempfile.TemporaryDirectory() as tmp:
        a, b = Path(tmp) / 'a', Path(tmp) / 'b'
        if build(script, a):
            try:
                report = json.loads((a / 'mesures.json').read_text(encoding='utf-8'))
            except (OSError, ValueError) as e:
                check(False, f'mesures.json unreadable ({e})')
                report = None
            for pack in KINDS:
                check_pack(a, pack, kinds_ok, report)
            if build(script, b):
                files_a = sorted(p.relative_to(a) for p in a.rglob('*') if p.is_file())
                files_b = sorted(p.relative_to(b) for p in b.rglob('*') if p.is_file())
                check(files_a == files_b, 'the two builds write different files')
                differ = [str(p) for p in files_a if p in files_b and (a / p).read_bytes() != (b / p).read_bytes()]
                check(not differ, f'the two builds differ: {differ[:5]}{" …" if len(differ) > 5 else ""}')
    print(f'{checks} checks, {len(failures)} failed')
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
