"""Phase 2: clean, reject failed takes, cut, score against the stock targets, export the best.

usage: python -I select.py CODE_DIR TARGET_DIR RAW_DIR OUT_DIR [MAX_TOTAL]

CODE_DIR: generate_pack.py (scripts/packs), cibles_desc.py + base.py (eq/son-fin-essentia-01 @ faa70cb3a),
zb.py (eq/son-cible-b). TARGET_DIR: zone-b-basses.tsv, zone-c-synthes.tsv from faa70cb3a.
Rejections (any one is enough): non-finite output, silence, digital clipping (flat tops), click
(sound shorter than the family minimum, or one isolated spike), and for bass families no bass
(low300 share too small) or unpitched noise (pYIN voiced share low and spectrum flat).
Score = zb.distance (robust z-score RMS, lower is better) to the (family, style) row, preferences as
in cible.py; re-measured on the exported MP3, which sets the final order.
"""
import json
import os
import subprocess
import sys
from multiprocessing import Pool

import numpy as np

CODE, TG, RAW, OUT = sys.argv[1:5]
MAX_TOTAL = int(sys.argv[5]) if len(sys.argv) > 5 else 240
sys.path.insert(0, CODE)
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cibles_desc  # noqa: E402
import generate_pack as gp  # noqa: E402
import prompts as P  # noqa: E402
import zb  # noqa: E402

SR = 44100
OUT_PEAK = 10 ** (-1.0 / 20)
TABLES = {z: zb.load(os.path.join(TG, z + ".tsv")) for z in ("zone-b-basses", "zone-c-synthes")}
MIN_ACTIVE = {"stab": 0.25, "impact": 0.3}
MAX_THRESHOLD = 3.0  # farther than this from the family: not kept, whatever the rank


def target(fam, style):
    zone, sf, keys_kind, pref = P.FAMILIES[fam]["tgt"]
    T = TABLES[zone]
    s = "*" if fam in P.ALL_STYLE_ROW else P.STOCK_STYLE[style]
    row, used = (T[(sf, s)], s) if (sf, s) in T else (T[(sf, "*")], "*")
    tg = {k: (dict(v, p50=v[pref[k]]) if k in pref else v) for k, v in row.items()}
    keys = list(zb.LOOP if keys_kind == "loop" else zb.ONESHOT)
    # degenerate columns of the stock (n_peaks capped at 60, dissonance stuck at 0.5)
    if tg.get("n_peaks", {}).get("p50", 0) >= 58:
        keys.remove("n_peaks")
    if "dissonance" in tg and tg["dissonance"]["p75"] - tg["dissonance"]["p25"] < 1e-3:
        keys.remove("dissonance")
    return tg, keys, f"{zone}:{sf}/{used}"


def flat_tops(a):
    """Share of samples sitting on a clipped plateau (>= 4 equal samples at the peak)."""
    worst = 0.0
    for c in range(a.shape[1]):
        x = a[:, c]
        pk = np.abs(x).max()
        if pk <= 0:
            continue
        hot = np.abs(x) >= 0.998 * pk
        flat = np.r_[False, np.abs(np.diff(x)) < 1e-6 * max(pk, 1e-9)]
        m = hot & flat
        # count runs of length >= 3 (so plateaus of >= 4 samples)
        d = np.diff(np.r_[0, m.astype(np.int8), 0])
        st, en = np.where(d == 1)[0], np.where(d == -1)[0]
        n = int(((en - st) >= 3).sum() and (en - st)[(en - st) >= 3].sum())
        worst = max(worst, n / len(x))
    return worst


def active_s(a):
    m = a.mean(1)
    w = int(0.01 * SR)
    k = len(m) // w
    if k < 2:
        return 0.0
    e = np.sqrt((m[:k * w].reshape(k, w) ** 2).mean(1) + 1e-20)
    return float((20 * np.log10(e / e.max()) > -30).sum() * w / SR)


def spike(a):
    m = np.abs(a.mean(1))
    return float(m.max() / (np.percentile(m, 99.9) + 1e-12))


def pitch(a, fam):
    import librosa
    y = librosa.resample(a.mean(1).astype(np.float32), orig_sr=SR, target_sr=11025)
    fmax = 1000 if fam == "screech" else 400
    f0, vf, vp = librosa.pyin(y, fmin=30, fmax=fmax, sr=11025, frame_length=1024, hop_length=256)
    voiced = float(np.mean(vf)) if len(vf) else 0.0
    hz = float(np.nanmedian(f0)) if np.any(vf) else 0.0
    S = np.abs(librosa.stft(y, n_fft=1024, hop_length=256))
    lib = dict(centroid_hz=float(np.median(librosa.feature.spectral_centroid(S=S, sr=11025))),
               rolloff_hz=float(np.median(librosa.feature.spectral_rolloff(S=S, sr=11025, roll_percent=0.85))),
               flatness_db=float(10 * np.log10(np.median(librosa.feature.spectral_flatness(S=S)) + 1e-12)))
    return voiced, hz, lib


def norm(a):
    return a / max(np.abs(a).max(), 1e-9) * OUT_PEAK


def saturate(a, drive=2.5):
    """Smooth tanh drive (no flat tops): the 'gras' variant, closer to the loud, dense stock basses."""
    return norm(np.tanh(drive * a / max(np.abs(a).max(), 1e-9)))


def variants(raw, fam, style):
    out = []
    for tag, a in cuts(raw, fam, style):
        out.append((tag, "brut", a))
        out.append((tag, "saturé", saturate(a)))
    return out


def cuts(raw, fam, style):
    f = P.FAMILIES[fam]
    out = []
    if f["kind"] == "loop":
        bar = 4 * 60.0 / P.BPM[style]
        m = np.abs(raw.mean(1))
        on = int(np.argmax(m > 0.05 * m.max()))
        start = max(0, on - int(0.002 * SR)) if on > int(0.05 * SR) else 0
        for b in f["bars"]:
            L = int(round(b * bar * SR))
            if start + L > len(raw):
                continue
            a = raw[start:start + L].copy()
            fi, fo = int(0.001 * SR), int(0.01 * SR)
            a[:fi] *= np.linspace(0, 1, fi)[:, None]
            a[-fo:] *= np.linspace(1, 0, fo)[:, None]
            out.append((f"{b} mesure" + ("s" if b > 1 else ""), norm(a)))
    else:
        c = gp.clean(raw, True)
        if c is None:
            return []
        for L in [None] + f["bars"]:
            a = c.copy()
            if L is not None:
                if len(a) <= int(L * SR):
                    continue
                a = a[:int(L * SR)]
                fo = min(int(0.25 * L * SR), int(0.3 * SR))
                a[-fo:] *= np.linspace(1, 0, fo)[:, None] ** 2
            out.append((f"{len(a) / SR:.1f} s", norm(a)))
    return out


def one(rec):
    fam, style = rec["fam"], rec["style"]
    f = P.FAMILIES[fam]
    raw = np.load(os.path.join(RAW, rec["name"] + ".npy")).astype(np.float64)
    if not np.all(np.isfinite(raw)):
        return dict(rec, reject="sortie invalide")
    if np.abs(raw).max() < 1e-3:
        return dict(rec, reject="silence")
    ft = flat_tops(raw)
    if ft > 5e-4:
        return dict(rec, reject="saturation numérique", flat=ft)
    tg, keys, row = target(fam, style)
    best = None
    for tag, proc, a in variants(raw, fam, style):
        if 20 * np.log10(OUT_PEAK / (np.sqrt(np.mean(a ** 2)) + 1e-12)) < 3.0:
            continue  # crest under 3 dB: a square wave, not a bass (seen on driven sub drops)
        act = active_s(a)
        if act < MIN_ACTIVE.get(fam, 0.5):
            if best is None:
                best = dict(reject="clic / trop court", active_s=act)
            continue
        if spike(a) > 4.0 and act < 1.0:
            if best is None:
                best = dict(reject="clic (pic isolé)")
            continue
        m = zb.measure(a)
        if 20 * np.log10(np.sqrt(np.mean(a ** 2)) + 1e-12) < -40:
            best = best or dict(reject="silence")
            continue
        s, z = zb.distance(m, tg, keys)
        s = float(s) if np.isfinite(s) else 99.0
        if best is None or "reject" in best or s < best["score"]:
            best = dict(score=s, tag=tag, proc=proc, a=a, z=z, m=m)
    if best is None:
        return dict(rec, reject="silence")
    if "reject" in best:
        return dict(rec, **best)
    voiced, hz, lib = pitch(best["a"], fam)
    if f["pitched"]:
        if fam != "screech" and best["m"]["low300"] < 0.08:
            return dict(rec, reject="pas de basse", score=best["score"])
        if voiced < 0.1 and best["m"]["flatness_db"] > -14:
            return dict(rec, reject="bruit sans hauteur", score=best["score"])
    best.update(voiced=voiced, f0_hz=hz, librosa=lib, row=row)
    return dict(rec, **best)


def _enc(a, path):
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", "2", "-i", "-",
                    "-codec:a", "libmp3lame", "-b:a", "192k", path],
                   input=np.ascontiguousarray(a.astype(np.float32)).tobytes(), check=True)


def _peak(path):
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", path, "-f", "f32le", "-"], capture_output=True,
                         check=True).stdout
    return float(np.abs(np.frombuffer(raw, np.float32)).max())


def to_mp3(a, path):
    """Encode, then scale down until the decoded MP3 peaks at -1 dBFS or less (dense saturated audio
    overshoots full scale once encoded)."""
    for _ in range(4):
        _enc(a, path)
        pk = _peak(path)
        if pk <= OUT_PEAK * 1.0001:
            return
        a = a * (OUT_PEAK / pk) * 0.995


def rnd(d):
    return {k: round(float(v), 4) for k, v in d.items() if isinstance(v, (int, float, np.floating)) and v is not None}


def main():
    os.makedirs(OUT, exist_ok=True)
    recs = [json.loads(x) for x in open(os.path.join(RAW, "meta.jsonl"))]
    recs = list({r["name"]: r for r in recs}.values())
    with Pool(4) as pool:
        res = pool.map(one, recs, chunksize=2)
    cells = {}
    for r in res:
        cells.setdefault((r["style"], r["fam"]), []).append(r)
    chosen = []
    stats = {}
    for (style, fam), rs in cells.items():
        ok = sorted((r for r in rs if "reject" not in r), key=lambda r: r["score"])
        rej = {}
        for r in rs:
            if "reject" in r:
                rej[r["reject"]] = rej.get(r["reject"], 0) + 1
        far = [r for r in ok if r["score"] > MAX_THRESHOLD]
        if far:
            rej["trop loin de la famille"] = len(far)
        ok = [r for r in ok if r["score"] <= MAX_THRESHOLD]
        keep = ok[:P.FAMILIES[fam]["keep"]]
        stats[f"{style}/{fam}"] = dict(generated=len(rs), rejected=rej, passed=len(ok), kept=len(keep),
                                       all_scores=[round(r["score"], 3) for r in ok])
        chosen += keep
    if len(chosen) > MAX_TOTAL:  # drop the farthest overall, never below 4 per cell
        chosen.sort(key=lambda r: r["score"])
        cnt = {}
        for r in chosen:
            cnt[(r["style"], r["fam"])] = cnt.get((r["style"], r["fam"]), 0) + 1
        for r in reversed(list(chosen)):
            if len(chosen) <= MAX_TOTAL:
                break
            if cnt[(r["style"], r["fam"])] > 4:
                cnt[(r["style"], r["fam"])] -= 1
                chosen.remove(r)
                stats[f"{r['style']}/{r['fam']}"]["kept"] -= 1
    index = {"model": "Stable Audio Open Small (Stability AI), ONNX export atauser001 rev 8f9544d, fp32, 8 steps",
             "license": "Stability AI Community License (outputs usable; attribution 'Powered by Stability AI')",
             "targets": "eq/son-fin-essentia-01 @ faa70cb3a (zone-b-basses.tsv, zone-c-synthes.tsv)",
             "distance": "zb.distance (eq/son-cible-b): RMS of robust z-scores vs the family p50 (p25-p75 spread)",
             "stats": stats, "sounds": []}
    by = {}
    for r in chosen:
        by.setdefault((r["style"], r["fam"]), []).append(r)
    for (style, fam), rs in by.items():
        tg, keys, _ = target(fam, style)
        scored = []
        for k, r in enumerate(sorted(rs, key=lambda r: r["score"])):
            tmp = os.path.join(OUT, f"_tmp-{style}-{fam}-{k}.mp3")
            to_mp3(r["a"], tmp)
            with open(tmp, "rb") as fh:
                m = cibles_desc.from_bytes(fh.read())
            m["high4k_db"] = 10 * np.log10(max(m["high4k"], 1e-12))
            s, z = zb.distance(m, tg, keys)
            scored.append((float(s), z, m, tmp, r))
        scored.sort(key=lambda u: u[0])
        far = [u for u in scored if u[0] > MAX_THRESHOLD]  # the threshold also holds after encoding
        for u in far:
            os.remove(u[3])
            stats[f"{style}/{fam}"]["kept"] -= 1
            stats[f"{style}/{fam}"]["rejected"]["trop loin après MP3"] = \
                stats[f"{style}/{fam}"]["rejected"].get("trop loin après MP3", 0) + 1
        scored = [u for u in scored if u[0] <= MAX_THRESHOLD]
        for k, (s, z, m, tmp, r) in enumerate(scored, 1):
            fn = f"{style}-{fam}-{k:02d}.mp3"
            os.replace(tmp, os.path.join(OUT, fn))
            index["sounds"].append(dict(
                file=fn, style=style, family=fam, rank=k, prompt=r["prompt"], seed=r["seed"], cut=r["tag"], proc=r["proc"],
                dur_s=round(len(r["a"]) / SR, 2), target=r["row"], keys=keys, score_array=round(r["score"], 3),
                score_mp3=round(s, 3), worst=sorted(((a, round(float(b), 1)) for a, b in z.items()),
                                                     key=lambda u: -abs(u[1]))[:4],
                voiced=round(r["voiced"], 3), f0_hz=round(r["f0_hz"], 1), librosa=rnd(r["librosa"]),
                measured=rnd(m)))
    with open(os.path.join(OUT, "index.json"), "w") as f:
        json.dump(index, f, indent=1, ensure_ascii=False)
    for k, v in sorted(stats.items()):
        print(k, v["generated"], v["passed"], v["kept"], v["rejected"], v["all_scores"][:3])
    print("total kept", len(index["sounds"]))


if __name__ == "__main__":
    main()
