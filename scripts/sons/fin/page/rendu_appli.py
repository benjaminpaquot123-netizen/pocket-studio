#!/usr/bin/env python3
"""Render the sound the app plays today for each "autres" line, measure it, compare it to the targets.

Reads the live-bench recordings (LIVE/cmds/<style>.pslive, made by scripts/live-bench.sh: the app's own
commands for a song of that style, as on a fresh phone) and replays variants of them through ps_live
(the real-time engine), never through an export:

- a single hit: the song up to the first scene where the track plays (its sound, settings and effects
  as the song set them), with the track's own notes left out, then, the song still running, one live
  note (SynthNoteOn / SynthNoteOff, as a key of the app's keyboard; a pad hit for the 808) at the
  track's usual pitch and velocity, held for the line's median target duration, recorded with its tail;
- a loop: the whole song with only those mixer channels (ps_live --solo), the 8 bars with the most
  energy, on the bar grid that starts with the first drum hit;
- an effect or a voice: the whole song with only that channel, its first event (start to the last
  sound before one second of silence).

No gain, no normalisation. Writes OUT/wav/<slug>.wav, OUT/appli/<slug>.mp3 (160 kb/s), OUT/labels.json
and the CSV given by --csv. Usage: rendu_appli.py --live LIVE --out OUT --csv FILE
"""
import argparse
import collections
import concurrent.futures
import csv
import json
import math
import os
import shutil
import struct
import subprocess
import sys

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))
TARGETS = os.path.join(ROOT, "docs/sons/fin/cibles-autres-definitives.tsv")
MESURE = os.path.join(ROOT, "scripts/sons/passe2/verif/mesure.py")
SR = 48000
DEFAULT_STYLE = "techno"  # StarterWizard.STYLES.first(): the style a fresh project starts with

# Per style: mixer channel of each part (META of the recording; channel = track + 4 from track 1,
# track 0 = channel 2, drums = 0, drum pad n = 16 + n), track of each part.
ST = {
    "techno": {"bpm": 132, "bass": [(0, 2)], "pad": (1, 5, "Pluck"), "lead": (2, 6, "Lead filtré"), "riser": (3, 7), "melo": [5, 6]},
    "house": {"bpm": 122, "bass": [(0, 2)], "lead": (2, 6, "Arpège")},
    "dnb": {"bpm": 174, "bass": [(0, 2), (3, 7)], "riser": (4, 8), "vocal": (5, 9), "melo": [5, 6]},
    "dubstep": {"bpm": 140, "bass": [(0, 2), (3, 7)], "lead": (2, 6, "Cloche FM"), "riser": (4, 8), "melo": [5, 6]},
    "trap": {"bpm": 140},
    "lofi": {"bpm": 84, "pad": (1, 5, "Rhodes FM")},
}
BASS_NAME = {"techno": "Basse analogique", "house": "Basse ronde", "dnb": "Basse reese (2 couches)",
             "dubstep": "Basse wobble (2 couches)"}
SAFE_TEMPO = {"dnb": 174, "dubstep": 140, "house": 125}  # maker's safe tempo (targets file)

NOT_IN_APP = "l'appli n'a pas ce son"


def fam(slug):
    kind, style = slug.rsplit("_", 1)
    f = {"basse": "basse (coups)", "bouclemelodique": "boucle mélodique", "bouclebatterie": "boucle de batterie",
         "voix": "voix", "effet": "effet", "coupdivers": "coup mélodique : -"}.get(kind, "coup mélodique : " + kind)
    return f, style


def plan():
    """slug -> spec. kind: hit / pad / loop / event / none."""
    d = DEFAULT_STYLE
    p = {}
    for s in ["tous", "dnb", "dubstep", "house", "techno", "trap"]:
        st = d if s == "tous" else s
        if st == "trap":
            p["basse_" + s] = {"kind": "pad", "style": "trap", "lane": 1, "ch": [17], "sound": "trap : 808 (pad de batterie 1)"}
        else:
            p["basse_" + s] = {"kind": "hit", "style": st, "tracks": [t for t, _ in ST[st]["bass"]],
                               "ch": [c for _, c in ST[st]["bass"]], "sound": f"{st} : {BASS_NAME[st]}"}
    t, c, n = ST[d]["pad"]
    p["pad_tous"] = {"kind": "hit", "style": d, "tracks": [t], "ch": [c], "sound": f"{d} : {n} (partie nappe)"}
    p["pluck_tous"] = {"kind": "hit", "style": d, "tracks": [t], "ch": [c], "sound": f"{d} : {n}"}
    p["chords_tous"] = {"kind": "hit", "style": d, "tracks": [t], "ch": [c], "chord": True, "sound": f"{d} : {n} (accord)"}
    t, c, n = ST[d]["lead"]
    for s in ["lead_tous", "synth_tous", "synth_techno"]:
        p[s] = {"kind": "hit", "style": "techno", "tracks": [t], "ch": [c], "sound": f"techno : {n}"}
    for slug, st, part in [("synth_house", "house", "lead"), ("lead_dubstep", "dubstep", "lead"), ("keys_lofi", "lofi", "pad")]:
        t, c, n = ST[st][part]
        p[slug] = {"kind": "hit", "style": st, "tracks": [t], "ch": [c], "sound": f"{st} : {n}"}
    for s in ["keys", "arp", "strings", "bell", "guitar", "brass", "coupdivers"]:
        p[s + "_tous"] = {"kind": "none", "why": f"aucun son de ce type dans le style par défaut ({d})"}
    for s in ["tous", "dnb", "dubstep", "techno"]:
        st = d if s == "tous" else s
        bpm = SAFE_TEMPO.get(st, ST[st]["bpm"])
        p["bouclemelodique_" + s] = {"kind": "loop", "style": st, "ch": ST[st]["melo"], "bpm": bpm,
                                     "sound": f"{st} : mélodie construite par l'appli (lead + nappe)"}
    for s in ["tous", "dnb", "dubstep", "house", "techno"]:
        st = d if s == "tous" else s
        bpm = SAFE_TEMPO.get(st, ST[st]["bpm"])
        p["bouclebatterie_" + s] = {"kind": "loop", "style": st, "ch": [0], "bpm": bpm,
                                    "sound": f"{st} : batterie construite par l'appli"}
    p["voix_tous"] = {"kind": "none", "why": f"le style par défaut ({d}) n'a pas de piste de voix"}
    t, c = ST["dnb"]["vocal"]
    p["voix_dnb"] = {"kind": "event", "style": "dnb", "ch": [c], "sound": "dnb : Voix robot (piste « Voix »)"}
    for s in ["tous", "dnb", "dubstep", "techno"]:
        st = d if s == "tous" else s
        t, c = ST[st]["riser"]
        p["effet_" + s] = {"kind": "event", "style": st, "ch": [c], "sound": f"{st} : piste « Montée » (bruit qui monte + impact)"}
    p["effet_rave"] = {"kind": "none", "why": "l'appli n'a pas de style rave"}
    return p


ORDER = ["basse_tous", "basse_dnb", "basse_dubstep", "basse_house", "basse_techno", "basse_trap",
         "pad_tous", "coupdivers_tous", "arp_tous", "strings_tous", "bell_tous", "pluck_tous", "guitar_tous",
         "brass_tous", "chords_tous", "keys_tous", "keys_lofi", "lead_tous", "lead_dubstep", "synth_tous",
         "synth_house", "synth_techno",
         "bouclemelodique_tous", "bouclemelodique_dnb", "bouclemelodique_dubstep", "bouclemelodique_techno",
         "bouclebatterie_tous", "bouclebatterie_dnb", "bouclebatterie_dubstep", "bouclebatterie_house",
         "bouclebatterie_techno", "voix_tous", "voix_dnb",
         "effet_tous", "effet_dnb", "effet_dubstep", "effet_techno", "effet_rave"]


def targets():
    t = {}
    with open(TARGETS, encoding="utf-8") as f:
        for r in csv.DictReader(f, delimiter="\t"):
            t[(r["famille"], r["style"], r["mesure"])] = r
    return t


# ------------------------------------------------------------------ recordings

def parse(path):
    return open(path, encoding="utf-8").read().split("\n")


def cmd(line):
    p = line.split()
    if len(p) == 5 and p[0] == "C":
        return int(p[1]), float(p[2]), int(p[3]), int(p[4])
    return None


def track_notes(lines, track):
    """[(pitch, start, velocity, line index)] of the track's SetSynthNote."""
    out = []
    for i, l in enumerate(lines):
        c = cmd(l)
        if c and c[0] == 26 and (c[2] >> 16) & 0xFF == track:
            out.append(((c[3] >> 24) & 0x7F, c[3] & 0xFFF, c[1], i))
    return out


def median_int(v):
    s = sorted(v)
    return s[len(s) // 2]


def cut_after_first_use(lines, first_index):
    """Index just after the first 'WAIT swaps' that follows first_index (that scene is then playing)."""
    for i in range(first_index, len(lines)):
        if lines[i].startswith("WAIT swaps"):
            return i + 1
    return len(lines)


def start_index(lines):
    return next(i for i, l in enumerate(lines) if l.startswith("C 5 1.0"))


def chord_of(notes):
    """Most frequent set of pitches starting together (consecutive note lines with one start)."""
    groups = collections.Counter()
    run = []
    for n in notes + [(None, None, None, -9)]:
        if run and (n[3] != run[-1][3] + 1 or n[1] != run[-1][1]):
            if len(run) >= 3:
                groups[tuple(sorted(x[0] for x in run))] += 1
            run = []
        if n[0] is not None:
            run.append(n)
    return list(groups.most_common(1)[0][0]) if groups else None


def hit_variant(lines, spec, hold_ms, at=None):
    """The song (the track's own notes left out) up to the moment the track is heard in it, then one
    live note held hold_ms. at = (scene swaps to wait for, ms after that swap); None: the start of the
    first scene where the track plays."""
    st = start_index(lines)
    ch = spec["ch"]
    if spec["kind"] == "pad":
        first = next(i for i, l in enumerate(lines) if (c := cmd(l)) and c[0] == 10 and c[2] == spec["lane"] and c[1] > 0)
        vels = [c[1] for l in lines if (c := cmd(l)) and c[0] == 10 and c[2] == spec["lane"] and c[1] > 0]
        vel = sorted(vels)[len(vels) // 2]
        on = [f"C 11 {vel} {spec['lane']} 0"]
        off = []
        how = f"un coup du pad {spec['lane']} (vélocité {vel:.2f}), longueur naturelle"
    else:
        on, off, pitches, first = [], [], [], len(lines)
        for k, tr in enumerate(spec["tracks"]):
            ns = track_notes(lines, tr)
            first = min(first, ns[0][3])
            vel = sorted(n[2] for n in ns)[len(ns) // 2]
            ps = chord_of(ns) if spec.get("chord") and k == 0 else None
            ps = ps or [median_int([n[0] for n in ns])]
            for pt in ps:
                on.append(f"C 16 {vel} {(tr << 16) | pt} 0")
                off.append(f"C 17 0.0 {(tr << 16) | pt} 0")
            pitches.append("+".join(str(x) for x in ps))
        how = f"note MIDI {' / '.join(pitches)} (vélocité {vel:.2f}) tenue {hold_ms / 1000:.3f} s, queue comprise"
    cut = cut_after_first_use(lines, first)
    wait = 500
    if at is not None:
        swaps, wait = at
        cut = start_index(lines) + 2 if swaps == 0 else lines.index(f"WAIT swaps {swaps}") + 1
    # The song keeps running (as on the phone: a key pressed while the song plays), but the
    # track's own notes are left out, so the only note it plays is the live one.
    if spec["kind"] == "pad":
        drop = lambda c: c[0] == 10 and c[2] == spec["lane"] and c[1] > 0
    else:
        drop = lambda c: c[0] == 26 and (c[2] >> 16) & 0xFF in spec["tracks"]
    keep = [l for l in lines[:cut] if not ((c := cmd(l)) and drop(c))]
    body = keep + [f"WAIT ms {wait}"] + on + [f"WAIT ms {hold_ms}"] + off + ["WAIT ms 20"]
    return "\n".join(body) + "\n", how


def tempo_variant(lines, bpm):
    out, n = [], 0
    for l in lines:
        c = cmd(l)
        if c and c[0] == 6:
            l = f"C 6 {float(bpm)} 0 0"
            n += 1
        out.append(l)
    assert n >= 1
    return "\n".join(out)


# ------------------------------------------------------------------ audio

def read_wav(path):
    sys.path.insert(0, os.path.dirname(MESURE))
    import mesure  # noqa: E402  (the counter-measure's own reader)
    sr, x = mesure.read_audio(path)
    assert sr == SR
    return x


def write_wav(path, x):
    x = np.ascontiguousarray(x, dtype="<f4")
    ch = x.shape[1]
    data = x.tobytes()
    with open(path, "wb") as f:
        f.write(b"RIFF" + struct.pack("<I", 36 + len(data)) + b"WAVE")
        f.write(b"fmt " + struct.pack("<IHHIIHH", 16, 3, ch, SR, SR * ch * 4, ch * 4, 32))
        f.write(b"data" + struct.pack("<I", len(data)) + data)


def frame_db(x, n=480):
    m = (x ** 2).mean(axis=1)
    k = len(m) // n
    p = m[:k * n].reshape(k, n).mean(axis=1)
    return 10 * np.log10(np.maximum(p, 1e-20))


def cut_hit(x, hold_s):
    """From the note's start (back from its loudest 10 ms to where it rises out of the channel's
    floor) to its last frame above -60 dBFS (or 6 dB above the channel's own noise before the
    note), at most hold + 15 s."""
    db = frame_db(x)
    top = int(np.argmax(db))
    assert db[top] > -100, "no note"
    before = db[max(0, top - 250):max(0, top - 50)]
    floor = float(np.median(before)) if len(before) else -200.0
    thr = max(float(db[top]) - 35, floor + 10)
    on = top  # back from the loudest frame to where the note rises out of the floor (3 s at most)
    while on > 0 and top - on < 300 and db[on - 1] > thr:
        on -= 1
    end_cap = min(len(db), on + int((hold_s + 15) * 100))
    if floor <= -70:  # silent channel: the last frame above -60 dBFS
        act = np.flatnonzero(db[on:end_cap] > -60)
        end = on + int(act[-1]) + 1
    else:  # a bed under the note (the lofi track's noise): where the note sinks back into it for 1 s
        p = np.convolve(10 ** (db / 10), np.ones(10) / 10, mode="same")
        sm = 10 * np.log10(np.maximum(p, 1e-20))
        bed = float(np.median(sm[max(0, top - 250):max(0, top - 50)])) + 6
        end = next((j for j in range(on + 10, end_cap) if sm[j:j + 100].max() <= bed), end_cap)
    return x[on * 480:end * 480]


def cut_event(x):
    db = frame_db(x)
    act = db > -50
    on = int(np.argmax(act))
    assert act[on], "no event"
    i, quiet = on, 0
    while i < len(db) and quiet < 100:
        quiet = 0 if act[i] else quiet + 1
        i += 1
    end = i - quiet
    return x[on * 480:end * 480]


def first_hit(x):
    db = frame_db(x, 48)
    return int(np.argmax(db > -40)) * 48


def cut_loop(x, origin, bpm):
    bar = 4 * 60.0 / bpm * SR
    best, bi = -1.0, 0
    k = 0
    while True:
        a = int(round(origin + k * bar))
        b = int(round(origin + (k + 8) * bar))
        if b > len(x):
            break
        e = float((x[a:b] ** 2).mean())
        if e > best:
            best, bi = e, k
        k += 1
    a = int(round(origin + bi * bar))
    return x[a:int(round(origin + (bi + 8) * bar))], bi


def measure(path):
    out = subprocess.run([sys.executable, "-I", MESURE, path, "--family", "common"], check=True,
                         capture_output=True, text=True).stdout
    return json.loads(out)


# ------------------------------------------------------------------ main

def run_live(tool, pslive, wav, seconds, solo, log=False):
    """Plays pslive through ps_live; with log, returns the scene swaps counted at each second."""
    r = subprocess.run([tool] + ([] if log else ["--quiet"]) + ["--seconds", str(seconds), "--solo",
                       ",".join(map(str, solo)), "--wav", wav, pslive], check=False,
                       capture_output=True, text=True)
    if not os.path.exists(wav):
        raise RuntimeError("ps_live wrote nothing for " + pslive)
    sw = {}
    for l in (r.stdout + r.stderr).splitlines():
        p = l.replace("=", " ").split()
        if l.startswith("t=") and "swaps" in p:
            sw[int(p[1].rstrip("s"))] = int(p[p.index("swaps") + 1])
    return sw


def heard_at(lines, spec, wav, sw):
    """(swaps, ms): where, in the song as played, the track is first heard (above -45 dBFS) from the
    start of the first scene where it plays."""
    first = min(track_notes(lines, t)[0][3] for t in spec["tracks"])
    cut = cut_after_first_use(lines, first)
    k = int(lines[cut - 1].split()[2]) if lines[cut - 1].startswith("WAIT swaps") else 0
    secs = sorted(sw)
    t_of = lambda n: next((t for t in secs if sw[t] >= n), secs[-1]) - 0.5 if n > 0 else 1.15
    db = frame_db(read_wav(wav))
    sec = [float(db[i * 100:(i + 1) * 100].max()) for i in range(len(db) // 100)]
    # Held for 3 s at least: a lone click when the scene starts is not the track being heard.
    i = next((i for i in range(max(0, int(t_of(k) - 1.0)), len(sec) - 2)
              if min(sec[i:i + 3]) > -45), None)
    assert i is not None, "track never heard: " + wav
    t_a = (i * 100 + int(np.argmax(db[i * 100:(i + 1) * 100] > -45))) / 100.0
    s = max(k, sw.get(int(t_a), k))
    return s, max(300, int(round((t_a + 0.3 - t_of(s)) * 1000)))


def verdict(m, v, t):
    if t is None or t["statut"] != "définitive" or v is None:
        return None
    med, p25, p75 = float(t["mediane"]), float(t["p25"]), float(t["p75"])
    gap = v - med
    if p25 <= v <= p75:
        word = "dans la cible"
    elif m == "dur_s":
        word = f"trop {'court' if v < p25 else 'long'} de {abs(gap):.3f} s"
    else:
        word = f"trop {'faible' if v < p25 else 'fort'} de {abs(gap):.1f} dB"
    return {"mediane": med, "p25": p25, "p75": p75, "ecart": gap, "verdict": word}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--live", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--csv", required=True)
    ap.add_argument("--jobs", type=int, default=4)
    a = ap.parse_args()
    tool = os.path.join(a.live, "tools/ps_live")
    cmds = os.path.join(a.out, "cmds")
    shutil.rmtree(a.out, ignore_errors=True)
    shutil.copytree(os.path.join(a.live, "cmds"), cmds)
    for d in ["wav", "raw", "appli"]:
        os.makedirs(os.path.join(a.out, d))
    tg = targets()
    specs = plan()
    songs = {s: parse(os.path.join(cmds, s + ".pslive")) for s in ST}

    jobs, how = {}, {}
    # The song as played, each hit track alone: where it is first heard.
    probes = {}
    for slug in ORDER:
        sp = specs[slug]
        if sp["kind"] == "hit":
            probes.setdefault((sp["style"], tuple(sp["ch"])), sp)

    def probe(item):
        (st, ch), sp = item
        wav = os.path.join(a.out, "raw", f"_probe_{st}_{'-'.join(map(str, ch))}.wav")
        sw = run_live(tool, os.path.join(cmds, st + ".pslive"), wav, 200, list(ch), log=True)
        return (st, ch), heard_at(songs[st], sp, wav, sw)

    with concurrent.futures.ThreadPoolExecutor(a.jobs) as ex:
        at = dict(ex.map(probe, sorted(probes.items(), key=lambda kv: kv[0])))
    for slug in ORDER:
        sp = specs[slug]
        f, s = fam(slug)
        if sp["kind"] in ("hit", "pad"):
            hold = float(tg[(f, s, "dur_s")]["mediane"])
            where = at.get((sp["style"], tuple(sp["ch"]))) if sp["kind"] == "hit" else None
            text, how[slug] = hit_variant(songs[sp["style"]], sp, int(round(hold * 1000)), where)
            pth = os.path.join(cmds, f"hit_{slug}.pslive")
            open(pth, "w", encoding="utf-8").write(text)
            jobs[slug] = (pth, 240, sp["ch"])
        elif sp["kind"] == "loop":
            st, bpm = sp["style"], sp["bpm"]
            key = f"song_{st}_{bpm}"
            pth = os.path.join(cmds, key + ".pslive")
            if not os.path.exists(pth):
                open(pth, "w", encoding="utf-8").write(tempo_variant(songs[st], bpm) + "\n")
            jobs[slug] = (pth, 150, sp["ch"])
            jobs.setdefault(f"_drums_{st}_{bpm}", (pth, 150, [0]))
            tempo = "tempo sûr du fabricant" if st in SAFE_TEMPO else "tempo par défaut du style dans l'appli"
            how[slug] = f"8 mesures à {bpm} BPM ({tempo}), les 8 mesures les plus fournies du morceau"
        elif sp["kind"] == "event":
            jobs[slug] = (os.path.join(cmds, sp["style"] + ".pslive"), 150, sp["ch"])
            how[slug] = "le premier passage dans le morceau, tel que l'appli le joue"

    def render(item):
        slug, (pth, secs, solo) = item
        wav = os.path.join(a.out, "raw", slug + ".wav")
        run_live(tool, pth, wav, secs, solo)
        return slug

    with concurrent.futures.ThreadPoolExecutor(a.jobs) as ex:
        list(ex.map(render, sorted(jobs.items())))

    rows, labels = [], {}
    for slug in ORDER:
        sp = specs[slug]
        f, s = fam(slug)
        row = {"slug": slug, "famille": f, "style": s}
        if sp["kind"] == "none":
            row.update({"son_appli": NOT_IN_APP, "jeu": sp["why"]})
        else:
            x = read_wav(os.path.join(a.out, "raw", slug + ".wav"))
            if sp["kind"] in ("hit", "pad"):
                y = cut_hit(x, float(tg[(f, s, "dur_s")]["mediane"]))
            elif sp["kind"] == "event":
                y = cut_event(x)
            else:
                d = read_wav(os.path.join(a.out, "raw", f"_drums_{sp['style']}_{sp['bpm']}.wav"))
                y, bar = cut_loop(x, first_hit(d), sp["bpm"])
                how[slug] += f" (à partir de la mesure {bar + 1})"
            wav = os.path.join(a.out, "wav", slug + ".wav")
            write_wav(wav, y)
            m = measure(wav)
            row.update({"son_appli": sp["sound"], "jeu": how[slug]})
            for k in ("dur_s", "rms_dbfs", "peak_dbfs"):
                row[k] = m[k]
            subprocess.run(["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", wav, "-codec:a", "libmp3lame",
                            "-b:a", "160k", "-map_metadata", "-1", os.path.join(a.out, "appli", slug + ".mp3")], check=True)
        bad, defin = [], 0
        for k in ("dur_s", "rms_dbfs", "peak_dbfs"):
            t = tg.get((f, s, k))
            row["statut_" + k] = t["statut"] if t else "pas de ligne"
            v = verdict(k, row.get(k), t)
            for c in ("mediane", "p25", "p75", "ecart", "verdict"):
                row[f"{c}_{k}"] = v[c] if v else None
            if v:
                defin += 1
                if v["verdict"] != "dans la cible":
                    bad.append(v["verdict"])
        if sp["kind"] == "none":
            row["verdict"] = NOT_IN_APP
        elif defin == 0:
            row["verdict"] = "pas de cible définitive"
        else:
            row["verdict"] = "dans la cible" if not bad else " ; ".join(bad)
        rows.append(row)
        lab = {"famille": f, "style": s, "son": row["son_appli"], "jeu": row["jeu"], "verdict": row["verdict"]}
        if sp["kind"] != "none":
            lab.update({"dur_s": round(row["dur_s"], 3), "rms_dbfs": round(row["rms_dbfs"], 2),
                        "peak_dbfs": round(row["peak_dbfs"], 2)})
        labels[slug] = lab

    cols = ["slug", "famille", "style", "son_appli", "jeu", "dur_s", "rms_dbfs", "peak_dbfs"]
    for k in ("dur_s", "rms_dbfs", "peak_dbfs"):
        cols += [f"{c}_{k}" for c in ("statut", "mediane", "p25", "p75", "ecart", "verdict")]
    cols.append("verdict")

    def fmt(v, k):
        if v is None:
            return ""
        if isinstance(v, float):
            return f"{v:.3f}" if "dur" in k else f"{v:.2f}"
        return v

    os.makedirs(os.path.dirname(os.path.abspath(a.csv)), exist_ok=True)
    with open(a.csv, "w", encoding="utf-8", newline="") as fh:
        w = csv.writer(fh, lineterminator="\n")
        w.writerow(cols)
        for r in rows:
            w.writerow([fmt(r.get(c), c) for c in cols])
    json.dump(labels, open(os.path.join(a.out, "labels.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"{sum(1 for v in labels.values() if 'dur_s' in v)} sons rendus, CSV {a.csv}")


if __name__ == "__main__":
    main()
