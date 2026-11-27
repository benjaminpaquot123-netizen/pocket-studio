#!/usr/bin/env python3
"""Blind re-computation of the non-drum family targets (critic of cibles_autres).

Reads only the per-sound tables (docs/sons/passe2/tout/*.csv) and the
counter-measure files (docs/sons/passe2/verif/rar*.csv), recomputes per family
and style: count, packs, quantiles and the counter-measure agreement on the
group's own sounds, then compares line by line with the published tsv.
No import from, and no copy of, cibles_autres.py.

Usage: python3 -I scripts/sons/fin/critique_autres.py [--selftest] [--out ecarts.tsv]
"""
import csv, glob, math, os, sys
from collections import Counter, defaultdict

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
TOUT = os.path.join(ROOT, 'docs/sons/passe2/tout')
VERIF = os.path.join(ROOT, 'docs/sons/passe2/verif')
TSV = os.path.join(ROOT, 'docs/sons/fin/cibles-autres-definitives.tsv')
MIN_N, MIN_PACKS, MIN_AGREE = 20, 2, 98.0
TEMPO = 'tempo'


def q(vals, p):
    """Linear-interpolation quantile (type 7), written here from scratch."""
    v = sorted(vals)
    if not v:
        return None
    h = (len(v) - 1) * p
    lo = math.floor(h)
    hi = min(lo + 1, len(v) - 1)
    return v[lo] + (v[hi] - v[lo]) * (h - lo)


def num(s):
    try:
        x = float(s)
    except (TypeError, ValueError):
        return None
    return x if math.isfinite(x) else None


def fold_gap(tag, est):
    """Smallest relative gap (%) between tag and est after x0.5 / x2 folding."""
    best = None
    for k in (0.5, 1.0, 2.0):
        g = abs(est * k - tag) / tag * 100
        best = g if best is None or g < best else best
    return best


def load_rows():
    fam = defaultdict(list)  # family -> list of rows
    def rd(name):
        with open(os.path.join(TOUT, name + '.csv'), newline='') as f:
            return list(csv.DictReader(f))
    for r in rd('oneshot_melodic'):
        fam['basse (coups)' if r['role'] == 'bass' else 'coup mélodique : ' + r['role']].append(r)
    for t, name in (('melodic_loop', 'boucle mélodique'), ('drum_loop', 'boucle de batterie'),
                    ('vocal', 'voix'), ('fx', 'effet'), ('midi', 'midi')):
        for r in rd(t):
            if t == 'midi':
                a, b = num(r['pitch_min']), num(r['pitch_max'])
                r['etendue_demitons'] = '' if a is None or b is None else str(b - a)
            fam[name].append(r)
    return fam


def load_verif():
    v = {}
    for p in glob.glob(os.path.join(VERIF, 'rar*.csv')):
        with open(p, newline='') as f:
            for r in csv.DictReader(f):
                v[r['cle']] = r
    return v


def pack(r):
    return 'r%sp%s' % (r['rar'], r['pack'].lstrip('p'))


def agreement(rows, col, verif):
    if col + '_ok' not in next(iter(verif.values())):
        return None, 0, 0
    yes = tot = 0
    for r in rows:
        x = verif.get(r['cle'])
        if not x or not x.get(col + '_ok'):
            continue
        ok = x[col + '_ok']
        if ok in ('yes', 'no'):
            tot += 1
            yes += ok == 'yes'
    return (100.0 * yes / tot if tot else None), yes, tot


def group_stats(rows, col, verif, tempo=False):
    if tempo:
        tagged = [(num(r['bpm_tag']), num(r['bpm_est']), r) for r in rows]
        tagged = [t for t in tagged if t[0]]
        both = [t for t in tagged if t[1]]
        agree = [t for t in both if fold_gap(t[0], t[1]) <= 2.0]
        vals = [t[0] for t in tagged]
        use = [t[2] for t in tagged]
        rate = 100.0 * len(agree) / len(both) if both else None
        ag = (rate, len(agree), len(both))
    else:
        use = [r for r in rows if num(r.get(col)) is not None]
        vals = [num(r[col]) for r in use]
        ag = agreement(use, col, verif)
    packs = Counter(pack(r) for r in use)
    s = dict(n=len(vals), packs=packs, agree=ag,
             qs=[q(vals, p) for p in (.1, .25, .5, .75, .9)])
    rate = ag[0]
    s['ok'] = (s['n'] >= MIN_N and len(packs) >= MIN_PACKS and rate is not None
               and rate >= MIN_AGREE)
    return s


def mine_for(fam, verif, family, style, col):
    rows = fam.get(family, [])
    if style != 'tous':
        rows = [r for r in rows if r['style'] == style]
    tempo = col.startswith('tempo')
    return group_stats(rows, 'bpm_tag' if tempo else col, verif, tempo)


def close(a, b):
    if a is None or b is None:
        return a is None and b is None
    return abs(a - b) <= max(1e-3, 0.005 * abs(b))


def compare(out_path=None):
    fam, verif = load_rows(), load_verif()
    with open(TSV, newline='') as f:
        lines = list(csv.DictReader(f, delimiter='\t'))
    gaps, conf_def, n_def = [], 0, 0
    for i, L in enumerate(lines, start=2):
        s = mine_for(fam, verif, L['famille'], L['style'], L['mesure'])
        their_def = L['statut'] == 'définitive'
        n_def += their_def
        problems = []
        if int(L['sons']) != s['n']:
            problems.append('sons %s vs %d' % (L['sons'], s['n']))
        if int(L['packs']) != len(s['packs']):
            problems.append('packs %s vs %d' % (L['packs'], len(s['packs'])))
        for k, name in enumerate(('p10', 'p25', 'mediane', 'p75', 'p90')):
            if not close(s['qs'][k], num(L[name])):
                problems.append('%s %s vs %.4g' % (name, L[name], s['qs'][k]))
        if their_def != s['ok']:
            problems.append('statut %s vs %s' % ('def' if their_def else 'non', 'def' if s['ok'] else 'non'))
        top = s['packs'].most_common(1)[0][1] / s['n'] if s['n'] else 0
        rate = s['agree'][0]
        info = dict(ligne=i, famille=L['famille'], style=L['style'], mesure=L['mesure'],
                    statut_tsv='def' if their_def else 'non', statut_moi='def' if s['ok'] else 'non',
                    n=s['n'], packs=len(s['packs']), part_pack_max='%.3f' % top,
                    accord='' if rate is None else '%.2f' % rate,
                    accord_sur='%d/%d' % s['agree'][1:], ecarts='; '.join(problems))
        if their_def and not problems:
            conf_def += 1
        if problems or (their_def and top > 0.9):
            gaps.append(info)
    if out_path:
        with open(out_path, 'w', newline='') as f:
            w = csv.DictWriter(f, fieldnames=list(gaps[0].keys()) if gaps else ['ligne'], delimiter='\t')
            w.writeheader()
            w.writerows(gaps)
    print('lignes %d, définitives %d, définitives confirmées %d, lignes à écart %d'
          % (len(lines), n_def, conf_def, sum(1 for g in gaps if g['ecarts'])))
    for g in gaps:
        print('\t'.join(str(g[k]) for k in g))
    return gaps


def selftest():
    assert q([1, 2, 3, 4], .5) == 2.5 and q([5], .9) == 5 and q([], .5) is None
    assert abs(q([0, 10], .25) - 2.5) < 1e-12
    assert fold_gap(174, 87) == 0 and fold_gap(140, 280) == 0
    assert abs(fold_gap(100, 101) - 1.0) < 1e-9 and fold_gap(100, 150) > 2
    assert num('nan') is None and num('') is None and num('-0.1') == -0.1
    rows = [dict(rar='1', pack='p1', cle='a', x='1'), dict(rar='2', pack='p1', cle='b', x='3')]
    ver = {'a': {'x_ok': 'yes'}, 'b': {'x_ok': 'no'}}
    s = group_stats(rows, 'x', ver)
    assert s['n'] == 2 and len(s['packs']) == 2 and s['agree'][:3] == (50.0, 1, 2) and not s['ok']
    rows = [dict(rar=str(i % 2), pack='p1', cle=str(i), x=str(i)) for i in range(20)]
    s = group_stats(rows, 'x', {str(i): {'x_ok': 'yes'} for i in range(20)})
    assert s['ok']
    s = group_stats(rows, 'x', {str(i): {'x_ok': 'yes' if i else 'no'} for i in range(20)})
    assert not s['ok']  # 95 % < 98 %
    assert close(1.0, 1.004) and not close(1.0, 1.01)
    print('selftest ok')


# ---- Remeasure (point 4): own decoder (ffmpeg -> float32), own peak / RMS / duration ----
SEED = 20261127
FAM_AUDIO = ('oneshot_melodic', 'melodic_loop', 'vocal', 'fx')


def decode(path):
    import subprocess
    import numpy as np
    pr = subprocess.run(['ffprobe', '-v', 'error', '-select_streams', 'a:0', '-show_entries',
                         'stream=sample_rate,channels', '-of', 'csv=p=0', path], capture_output=True, text=True)
    sr, ch = [int(x) for x in pr.stdout.strip().split(',')[:2]]
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', path, '-f', 'f32le', '-acodec', 'pcm_f32le', '-'],
                         capture_output=True).stdout
    x = np.frombuffer(raw, dtype='<f4').reshape(-1, ch).astype('float64')
    return x, sr


def measure(path):
    import numpy as np
    x, sr = decode(path)
    peak = float(np.max(np.abs(x)))
    rms_all = float(np.sqrt(np.mean(x * x)))       # every sample of every channel
    rms_mono = float(np.sqrt(np.mean(x.mean(axis=1) ** 2)))
    db = lambda v: 20 * math.log10(v) if v > 0 else -200.0
    return x.shape[0] / sr, db(peak), db(rms_all), db(rms_mono)


def remeasure(roots, n, out_path):
    """roots: {rar: extracted archive folder outside the repository}. Writes only keys and numbers."""
    import random
    sys.path.insert(0, os.path.join(ROOT, 'scripts', 'sons', 'passe2'))
    import lance  # pack order and file_id -> path, the same mapping the tables were built with
    files = {}
    for rar, root in roots.items():
        for i, pk in enumerate(lance.find_packs(root), 1):
            for fid, rel in lance.id_map(pk).items():
                files['r%d-p%d-%s' % (rar, i, fid)] = os.path.join(pk, rel)
    rows = []
    for t in FAM_AUDIO:
        with open(os.path.join(TOUT, t + '.csv'), newline='') as fh:
            rows += list(csv.DictReader(fh))
    pool = sorted([r for r in rows if r['type'] in FAM_AUDIO and r['dur_s'] and int(r['rar']) in roots], key=lambda r: r['cle'])
    pick = random.Random(SEED).sample(pool, n)
    lines, ok = [], 0
    for r in pick:
        f = files.get(r['cle'])
        if not f or not os.path.isfile(f):
            lines.append([r['cle'], r['type'], 'introuvable'] + [''] * 8)
            continue
        d, pk, ra, rm = measure(f)
        td, tp, tr = num(r['dur_s']), num(r['peak_dbfs']), num(r['rms_dbfs'])
        e_d = abs(d - td) / td * 100
        e_p = abs(pk - tp)
        e_r = min(abs(ra - tr), abs(rm - tr))
        good = e_d < 1 and e_p < 0.5 and e_r < 0.5
        ok += good
        lines.append([r['cle'], r['type'], 'oui' if good else 'non', td, '%.4g' % d, tp, '%.4g' % pk,
                      tr, '%.4g' % ra, '%.4g' % rm, 'rms_all' if abs(ra - tr) <= abs(rm - tr) else 'rms_mono'])
    head = ['cle', 'type', 'accord', 'dur_table', 'dur_moi', 'crete_table', 'crete_moi',
            'rms_table', 'rms_moi_tous', 'rms_moi_mono', 'rms_proche']
    with open(out_path, 'w') as fh:
        fh.write('\t'.join(head) + '\n')
        for L in lines:
            fh.write('\t'.join(str(v) for v in L) + '\n')
    print('remesure graine %d : %d sons, %d d accord' % (SEED, len(pick), ok))
    print(Counter(r['type'] + ' r' + r['rar'] for r in pick))


if __name__ == '__main__':
    if '--remesure' in sys.argv:  # --remesure 3=DIR 12=DIR --out FILE
        i = sys.argv.index('--remesure')
        roots = {int(a.split('=')[0]): a.split('=', 1)[1] for a in sys.argv[i + 1:] if '=' in a}
        remeasure(roots, 60, sys.argv[sys.argv.index('--out') + 1])
        sys.exit(0)
    if '--selftest' in sys.argv:
        selftest()
        sys.exit(0)
    out = sys.argv[sys.argv.index('--out') + 1] if '--out' in sys.argv else None
    compare(out)
