"""Measure the whole sound stock with Essentia, counter-measure with librosa.

Run with the venv that holds essentia + librosa, always with -I:
  python -I base.py mesure STOCK OUT.jsonl     # STOCK/lN/archive.bin, read in place (zip, never extracted)
  python -I base.py stats OUT.jsonl DOCDIR      # per-family stats + agreement, TSV only

The per-sound file (OUT.jsonl) holds one sound per line: it stays outside the
repository. Only family statistics (p10..p90, counts) reach DOCDIR.

Family and style come from the existing classification: the passe-2 tables
(docs/sons/passe2/tout, matched by tout.py's file_id = sha1(pack/rel)[:10]),
and tout.classify on the member path for archives whose paths match no row.
Same content (zip CRC + size) in several archives is measured once.

Fixed analysis: mono, 44.1 kHz (Essentia Resample), frame 2048, hop 512, Hann,
first 30 s. Pitch: frame 4096. Both tools get the same decoded array.
"""
import collections
import csv
import glob
import hashlib
import io
import json
import math
import os
import sys
import zipfile

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
TABLES = os.path.join(REPO, "docs", "sons", "passe2", "tout")
SR = 44100
FRAME, HOP, PFRAME = 2048, 512, 4096
MAX_S = 30.0
PITCH_S = 3.0
GATE_DB = -60.0  # active frame: frame RMS within 60 dB of the loudest frame
AUDIO = (".wav", ".wave", ".aif", ".aiff", ".aifc", ".flac", ".mp3", ".ogg")
PITCHED = {"oneshot_melodic", "kick_808"}  # types/subtypes whose f0 is measured
LOOPS = {"drum_loop", "melodic_loop", "kit_stem"}


# ---------------------------------------------------------------- classification
def table_rows():
    by_fid = {}
    for f in sorted(glob.glob(os.path.join(TABLES, "*.csv"))):
        with open(f, newline="", encoding="utf-8") as fh:
            for r in csv.DictReader(fh):
                if r.get("file_id") and r.get("type") not in ("kit", "midi"):
                    by_fid.setdefault(r["file_id"], r)
    return by_fid


def fid_match(name, by_fid):
    parts = name.split("/")
    for k in range(len(parts) - 1):
        key = parts[k] + "/" + "/".join(parts[k + 1:])
        fid = hashlib.sha1(key.encode("utf-8", "surrogateescape")).hexdigest()[:10]
        if fid in by_fid:
            return by_fid[fid]
    return None


def family_of(typ, subtype, role):
    if typ == "drum_oneshot":
        return "drum_oneshot/" + (subtype if subtype not in ("", "-") else "autre")
    return "%s/%s" % (typ or "inconnu", role if role not in ("", "-", None) else "autre")


def _tout():
    sys.path.insert(0, os.path.join(REPO, "scripts", "sons"))
    import tout  # noqa: E402  (the repository's own classifier)
    return tout


def index(stock):
    """[(stock_id, member, family, style, pack, cle)] for every distinct sound."""
    by_fid = table_rows()
    tout = None
    seen, jobs, counts = set(), [], collections.OrderedDict()
    dirs = sorted(glob.glob(os.path.join(stock, "l*")), key=lambda p: int(p.rsplit("l", 1)[1]))
    for d in dirs:
        sid = os.path.basename(d)
        z = zipfile.ZipFile(os.path.join(d, "archive.bin"))
        c = counts.setdefault(sid, {"audio": 0, "table": 0, "classify": 0, "copies": 0})
        for i in z.infolist():
            n = i.filename
            base = n.rsplit("/", 1)[-1]
            if i.is_dir() or "__MACOSX" in n or base.startswith(".") or not n.lower().endswith(AUDIO):
                continue
            c["audio"] += 1
            key = (i.CRC, i.file_size)
            if key in seen:
                c["copies"] += 1
                continue
            seen.add(key)
            r = fid_match(n, by_fid)
            if r is not None:
                c["table"] += 1
                fam = family_of(r["type"], r["subtype"], r["role"])
                jobs.append((sid, n, fam, r["style"], "r%s%s" % (r["rar"], r["pack"]), r["cle"]))
            else:
                tout = tout or _tout()
                parts = n.split("/")
                rel = "/".join(parts[1:]) if len(parts) > 1 else n
                try:
                    import soundfile as sf
                    dur = sf.info(io.BytesIO(z.read(n))).duration
                except Exception:
                    dur = None
                cl = tout.classify(rel, dur)
                c["classify"] += 1
                fam = family_of(cl["type"], cl["subtype"], cl["role"])
                jobs.append((sid, n, fam, cl["style"], sid, ""))
    return jobs, counts


# ---------------------------------------------------------------- decoding
def decode(data):
    import soundfile as sf
    x, sr = sf.read(io.BytesIO(data), dtype="float32", always_2d=True)
    return x, sr


def resample(x, sr):
    import essentia.standard as es
    if sr == SR:
        return x.astype(np.float32)
    rs = es.Resample(inputSampleRate=sr, outputSampleRate=SR, quality=1)
    return np.stack([rs(np.ascontiguousarray(x[:, c], dtype=np.float32)) for c in range(x.shape[1])], 1)


def db(v):
    return 20 * math.log10(v) if v > 0 else None


def med(a):
    return float(np.median(a)) if len(a) else None


# ---------------------------------------------------------------- Essentia
def ess_measure(stereo, typ, fam):
    import essentia.standard as es
    st = np.ascontiguousarray(stereo[:, :2] if stereo.shape[1] >= 2 else np.repeat(stereo[:, :1], 2, 1))
    y = np.ascontiguousarray(st.mean(1).astype(np.float32))
    out = {"dur_s": len(y) / SR}
    out["peak_db"] = db(float(np.abs(st).max()))
    w = es.Windowing(type="hann", size=FRAME, normalized=False)
    spec = es.Spectrum(size=FRAME)
    cen = es.Centroid(range=SR / 2)
    roll = es.RollOff(cutoff=0.85, sampleRate=SR)
    flat = es.Flatness()
    zcr = es.ZeroCrossingRate()
    rms = es.RMS()
    mfcc = es.MFCC(inputSize=FRAME // 2 + 1, sampleRate=SR, numberCoefficients=13)
    bands = [es.EnergyBand(startCutoffFrequency=a, stopCutoffFrequency=b, sampleRate=SR)
             for a, b in ((20, 250), (250, 4000), (4000, 20000))]
    F = {k: [] for k in ("cen", "roll", "flat", "zcr", "rms", "lo", "mi", "hi")}
    M = []
    for fr in es.FrameGenerator(y, frameSize=FRAME, hopSize=HOP, startFromZero=False):
        s = spec(w(fr))
        F["cen"].append(cen(s))
        F["roll"].append(roll(s))
        F["flat"].append(flat(np.maximum(s.astype(np.float64) ** 2, 1e-10).astype(np.float32)))
        F["zcr"].append(zcr(fr))
        F["rms"].append(rms(fr))
        p = (s ** 2).astype(np.float32)
        F["lo"].append(bands[0](p)), F["mi"].append(bands[1](p)), F["hi"].append(bands[2](p))
        M.append(mfcc(s)[1])
    F = {k: np.asarray(v, dtype=np.float64) for k, v in F.items()}
    on = F["rms"] > F["rms"].max() * 10 ** (GATE_DB / 20) if F["rms"].max() > 0 else F["rms"] > 1
    out["active_frames"] = int(on.sum())
    out["centroid_hz"] = med(F["cen"][on])
    out["rolloff_hz"] = med(F["roll"][on])
    fl = F["flat"][on]
    out["flatness_db"] = med(10 * np.log10(np.maximum(fl, 1e-10))) if len(fl) else None
    out["zcr"] = med(F["zcr"][on])
    out["rms_db"] = db(float(np.sqrt(np.mean(F["rms"][on] ** 2)))) if on.any() else None
    tot = F["lo"][on].sum() + F["mi"][on].sum() + F["hi"][on].sum()
    for k, n in (("lo", "low_share_db"), ("mi", "mid_share_db"), ("hi", "high_share_db")):
        out[n] = 10 * math.log10(F[k][on].sum() / tot) if tot > 0 and F[k][on].sum() > 0 else None
    if on.any():
        mm = np.median(np.asarray(M)[on], axis=0)
        for j in range(1, 6):
            out["mfcc%d" % j] = float(mm[j])
    try:
        if len(y) >= int(0.4 * SR):
            out["lufs"] = float(es.LoudnessEBUR128(sampleRate=SR)(st)[2])
    except Exception:
        pass
    try:
        env = es.Envelope(sampleRate=SR)(y)
        out["log_attack"] = float(es.LogAttackTime(sampleRate=SR)(env)[0])
        out["effective_dur_s"] = float(es.EffectiveDuration(sampleRate=SR)(y))
    except Exception:
        pass
    if typ in PITCHED or fam.endswith("/808") or fam.endswith("/bass"):
        yp = y[:int(PITCH_S * SR)]
        yin = es.PitchYin(frameSize=PFRAME, sampleRate=SR, minFrequency=30, maxFrequency=2000)
        f0 = []
        for fr in es.FrameGenerator(yp, frameSize=PFRAME, hopSize=HOP, startFromZero=False):
            f, c = yin(fr)  # time-domain YIN (same family as pyin); YinFFT biased low-frequency
            f0.append(f if c > 0.5 and f > 0 else np.nan)
        f0 = np.asarray(f0)
        out["f0_hz"] = med(f0[np.isfinite(f0)])
        out["_f0"] = f0
    if typ in LOOPS and len(y) >= 4 * SR:
        try:
            bpm = es.RhythmExtractor2013(method="degara")(y)[0]
            out["bpm"] = float(bpm)
        except Exception:
            pass
        try:
            k, sc, strength = es.KeyExtractor(sampleRate=SR)(y)
            out["key"], out["key_strength"] = "%s %s" % (k, sc), float(strength)
        except Exception:
            pass
    return out


# ---------------------------------------------------------------- librosa
def lib_measure(stereo, typ, fam):
    import librosa
    st = stereo[:, :2] if stereo.shape[1] >= 2 else np.repeat(stereo[:, :1], 2, 1)
    y = st.mean(1).astype(np.float32)
    S = np.abs(librosa.stft(y, n_fft=FRAME, hop_length=HOP, window="hann", center=True, pad_mode="constant"))
    cen = librosa.feature.spectral_centroid(S=S, sr=SR)[0]
    roll = librosa.feature.spectral_rolloff(S=S ** 2, sr=SR, roll_percent=0.85)[0]  # energy, as Essentia RollOff
    flat = librosa.feature.spectral_flatness(S=S)[0]
    zcr = librosa.feature.zero_crossing_rate(y, frame_length=FRAME, hop_length=HOP, center=True)[0]
    rms = librosa.feature.rms(y=y, frame_length=FRAME, hop_length=HOP, center=True, pad_mode="constant")[0]
    n = min(len(cen), len(zcr), len(rms))
    cen, roll, flat, zcr, rms = cen[:n], roll[:n], flat[:n], zcr[:n], rms[:n]
    on = rms > rms.max() * 10 ** (GATE_DB / 20) if rms.max() > 0 else rms > 1
    out = {"centroid_hz": med(cen[on]), "rolloff_hz": med(roll[on]), "zcr": med(zcr[on]),
           "flatness_db": med(10 * np.log10(np.maximum(flat[on], 1e-10))) if on.any() else None,
           "rms_db": db(float(np.sqrt(np.mean(rms[on].astype(np.float64) ** 2)))) if on.any() else None}
    if typ in PITCHED or fam.endswith("/808") or fam.endswith("/bass"):
        yp = y[:int(PITCH_S * SR)]
        f0, vf, _ = librosa.pyin(yp, fmin=30, fmax=2000, sr=SR, frame_length=PFRAME, hop_length=HOP)
        f0 = np.where(vf & np.isfinite(f0), f0, np.nan)
        out["f0_hz"] = med(f0[np.isfinite(f0)])
        out["_f0"] = f0
    if typ in LOOPS and len(y) >= 4 * SR:
        t, _ = librosa.beat.beat_track(y=y, sr=SR, hop_length=HOP)
        out["bpm"] = float(np.atleast_1d(t)[0])
    return out


def one(job):
    sid, member, fam, style, pack, cle, stock = job
    typ = fam.split("/")[0]
    if fam == "drum_oneshot/kick_808":
        typ = "kick_808"
    rec = {"sid": sid, "fam": fam, "style": style, "pack": pack,
           "id": hashlib.sha1((sid + "/" + member).encode("utf-8", "surrogateescape")).hexdigest()[:12]}
    try:
        with zipfile.ZipFile(os.path.join(stock, sid, "archive.bin")) as z:
            data = z.read(member)
        x, sr = decode(data)
        x = x[:int(MAX_S * sr)]
        st = resample(x, sr)
        if st.ndim == 1:
            st = st[:, None]
        rec["sr_in"] = sr
        rec["ess"] = ess_measure(st, typ, fam)
        rec["lib"] = lib_measure(st, typ, fam)
        a, b = rec["ess"].pop("_f0", None), rec["lib"].pop("_f0", None)
        if a is not None and b is not None:
            n = min(len(a), len(b))
            both = np.isfinite(a[:n]) & np.isfinite(b[:n])  # frames voiced for both tools
            rec["ess"]["f0_common_hz"] = med(a[:n][both])
            rec["lib"]["f0_common_hz"] = med(b[:n][both])
    except Exception as e:  # a sound that cannot be read is counted, not hidden
        rec["error"] = type(e).__name__
        if os.environ.get("ESS_DEBUG"):
            raise
    return rec


def cmd_mesure(stock, out):
    import multiprocessing
    jobs, counts = index(stock)
    done = set()
    if os.path.exists(out):
        with open(out) as f:
            for line in f:
                done.add(json.loads(line)["id"])
    todo = [j + (stock,) for j in jobs
            if hashlib.sha1((j[0] + "/" + j[1]).encode("utf-8", "surrogateescape")).hexdigest()[:12] not in done]
    with open(out + ".index.json", "w") as f:
        json.dump({"counts": counts, "distinct": len(jobs)}, f)
    print("distinct sounds %d, to measure %d" % (len(jobs), len(todo)), flush=True)
    with multiprocessing.Pool(4, maxtasksperchild=50) as pool, open(out, "a") as f:
        for k, rec in enumerate(pool.imap_unordered(one, todo, chunksize=4)):
            f.write(json.dumps(rec) + "\n")
            if k % 250 == 0:
                f.flush()
                print("%d/%d" % (k, len(todo)), flush=True)


# ---------------------------------------------------------------- stats
STAT_KEYS = ("dur_s", "peak_db", "rms_db", "lufs", "centroid_hz", "rolloff_hz", "flatness_db", "zcr",
             "low_share_db", "mid_share_db", "high_share_db", "log_attack", "effective_dur_s",
             "mfcc1", "mfcc2", "mfcc3", "mfcc4", "mfcc5", "f0_hz", "bpm", "key_strength")


def agree(k, a, b):
    """Counter-measure tolerance, per measure (None = not comparable)."""
    if a is None or b is None:
        return None
    if k in ("centroid_hz", "rolloff_hz"):
        return abs(a - b) <= 0.05 * max(abs(b), 1e-9) or abs(a - b) <= 25.0
    if k == "zcr":
        return abs(a - b) <= 0.05 * max(abs(b), 1e-9) or abs(a - b) <= 0.002
    if k in ("flatness_db", "rms_db"):
        return abs(a - b) <= (1.0 if k == "flatness_db" else 0.5)
    if k in ("f0_hz", "f0_common_hz"):
        return a > 0 and b > 0 and abs(1200 * math.log2(a / b)) <= 50
    if k == "bpm":
        return a > 0 and b > 0 and min(abs(a * m - b) / b for m in (0.5, 1, 2)) <= 0.02
    return None


AGREE_KEYS = ("centroid_hz", "rolloff_hz", "flatness_db", "zcr", "rms_db", "f0_hz", "f0_common_hz", "bpm")
MIN_FAMILY = 5  # a family with fewer sounds is listed, its percentiles are not


def q(v, p):
    return float(np.percentile(v, p))


def fmt(x):
    if x is None:
        return "-"
    return ("%.4f" % x) if abs(x) < 1 else ("%.1f" % x)


def cmd_stats(src, outdir):
    recs = [json.loads(line) for line in open(src)]
    ok = [r for r in recs if "ess" in r]
    by_fam = collections.defaultdict(list)
    for r in ok:
        by_fam[r["fam"]].append(r)
    os.makedirs(outdir, exist_ok=True)
    with open(os.path.join(outdir, "essentia-familles.tsv"), "w") as f:
        f.write("famille\tmesure\tn_sons\tn_packs\tp10\tp25\tp50\tp75\tp90\n")
        for fam in sorted(by_fam):
            rs = by_fam[fam]
            packs = len({r["pack"] for r in rs})
            for k in STAT_KEYS:
                v = [r["ess"][k] for r in rs if r["ess"].get(k) is not None]
                if not v:
                    continue
                if len(v) < MIN_FAMILY:
                    f.write("%s\t%s\t%d\t%d\t-\t-\t-\t-\t-\n" % (fam, k, len(v), packs))
                    continue
                f.write("%s\t%s\t%d\t%d\t%s\n" % (fam, k, len(v), packs,
                                                  "\t".join(fmt(q(v, p)) for p in (10, 25, 50, 75, 90))))
    by_style = collections.defaultdict(lambda: collections.Counter())
    for r in ok:
        by_style[r["fam"]][r["style"]] += 1
    with open(os.path.join(outdir, "essentia-styles.tsv"), "w") as f:
        f.write("famille\tstyle\tn_sons\n")
        for fam in sorted(by_style):
            for st, n in sorted(by_style[fam].items(), key=lambda t: (-t[1], t[0])):
                f.write("%s\t%s\t%d\n" % (fam, st, n))
    tot = collections.Counter()
    with open(os.path.join(outdir, "essentia-accord.tsv"), "w") as f:
        f.write("famille\tmesure\tcomparees\td_accord\taccord_pct\n")
        for fam in sorted(by_fam):
            for k in AGREE_KEYS:
                res = [agree(k, r["ess"].get(k), r["lib"].get(k)) for r in by_fam[fam]]
                res = [x for x in res if x is not None]
                if not res:
                    continue
                tot[(k, 0)] += len(res)
                tot[(k, 1)] += sum(res)
                f.write("%s\t%s\t%d\t%d\t%.1f\n" % (fam, k, len(res), sum(res), 100.0 * sum(res) / len(res)))
        for k in AGREE_KEYS:
            if tot[(k, 0)]:
                f.write("TOUT\t%s\t%d\t%d\t%.1f\n" % (k, tot[(k, 0)], tot[(k, 1)], 100.0 * tot[(k, 1)] / tot[(k, 0)]))
    errs = collections.Counter(r["error"] for r in recs if "error" in r)
    print("records %d, measured %d, errors %s, families %d" % (len(recs), len(ok), dict(errs), len(by_fam)))


def main(argv):
    if len(argv) == 3 and argv[0] == "mesure":
        cmd_mesure(argv[1], argv[2])
    elif len(argv) == 3 and argv[0] == "stats":
        cmd_stats(argv[1], argv[2])
    else:
        print(__doc__)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
