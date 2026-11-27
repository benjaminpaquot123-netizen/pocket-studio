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

Fixed analysis: mono, 44.1 kHz (Essentia Resample), first 30 s, Hann.
Mode "long": frame 2048, hop 512, gate -60 dB under the loudest frame.
Mode "coup" (drum one-shots, sounds under 0.5 s): frame 512 zero-padded to
2048, hop 128, gate -20 dB, so a hit is described by its body, not its tail.
One-shot rolloff: one zero-padded spectrum of the body (about 2.7 Hz per bin).
Pitch: frame 4096, one-shots only.
Counter-measure: librosa loads, resamples, frames and gates on its own.
Truth: tempo and note written in file names, compared in aggregate only.
"""
import collections
import csv
import glob
import hashlib
import io
import json
import math
import os
import re
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
# Short hits ("coup": every drum one-shot, and any sound under COUP_MAX_S) are
# described by their body, not their tail: 512/128 frames (zero-padded to
# FRAME, so every spectral instrument keeps its 2048-point grid) and a gate
# 20 dB under the loudest frame.
# Flatness stops at 19 kHz: any resampler from 48 kHz empties the band above
# about 20 kHz, which drags a full-band flatness down by 1 to 2 dB.
FLAT_TOP_HZ = 19000.0
FLAT_BINS = int(FLAT_TOP_HZ / SR * FRAME) + 1
COUP_FRAME, COUP_HOP, COUP_GATE_DB, COUP_MAX_S = 512, 128, -20.0, 0.5
ONESHOTS = {"drum_oneshot", "oneshot_melodic"}
# One-shot rolloff: one spectrum of the body (gated span, Hann, at most
# BODY_MAX_S), zero-padded to at least BODY_NFFT points (2.7 Hz per bin).
BODY_MAX_S, BODY_NFFT = 2.0, 16384
AUDIO = (".wav", ".wave", ".aif", ".aiff", ".aifc", ".flac", ".mp3", ".ogg")
PITCHED = {"oneshot_melodic"}  # types/subtypes whose f0 is measured
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


# Family refinement: generic keywords of the file name only (never a pack or
# vendor name). Order: 808, merged/absurd families, then words for "autre".
_808_FROM = {"drum_oneshot/kick_808", "oneshot_melodic/bass", "oneshot_melodic/autre",
             "oneshot_melodic/synth", "melodic_loop/bass", "melodic_loop/autre", "kit_stem/bass"}
MERGE = {
    "drum_loop/pad": "melodic_loop/pad",          # texture loops
    "drum_loop/synth": "drum_loop/full",
    "fx/drums": "fx/fx",
    "fx/chords": "melodic_loop/chords",
    "melodic_loop/drums": "drum_loop/drums",      # top loops
    "melodic_loop/perc": "drum_loop/perc",        # cowbell loops
    "oneshot_melodic/drums": "oneshot_melodic/bell",
    "drum_oneshot/perc_high": "drum_oneshot/perc",
    "drum_oneshot/perc_mid": "drum_oneshot/perc",
    "drum_oneshot/perc_low": "drum_oneshot/perc",
    "drum_oneshot/snap": "drum_oneshot/clap",
    "drum_oneshot/kick_808": "oneshot_melodic/808",  # an 808 is a tuned bass, not a drum hit
    "melodic_loop/vocal": "vocal/vocal",
}
WORDS = (  # role for a */autre family, first match wins
    ("vocal", r"ad ?lib|sung|vocal|vox|choir|chant"),
    ("strings", r"viola|violin|cello|pizz"),
    ("brass", r"flute|sax|horn|trombone|trumpet|oboe"),
    ("bell", r"kalimba|khalimba|chime|steel ?drum|vibraphone|dulcimer|bell"),
    ("keys", r"piano|keys|organ|wurl|rhodes"),
    ("guitar", r"guitar|mandolin|koto|oud"),
    ("pad", r"pad|atmo|texture"),
    ("perc", r"claves|darbouka"),
)
_FXWORD = re.compile(r"fx|riser|sweep|impact|noise|screech", re.I)


def _has(word_re, text):
    return re.search(r"(?<![a-z])(?:%s)" % word_re, text) is not None


def refine(fam, member):
    """Family after the generic rules written in ESSENTIA-BASE.md."""
    name = member.rsplit("/", 1)[-1].lower()
    path = member.lower()
    typ, _, sub = fam.partition("/")
    if typ == "drum_oneshot" and ":" in sub:  # "kick:attack", "kick:tail": the same family
        fam, sub = "drum_oneshot/" + sub.split(":")[0], sub.split(":")[0]
    if fam in _808_FROM and re.search(r"(?<!\d)808(?!\d)", path):
        return ("oneshot_melodic/808" if typ in ("drum_oneshot", "oneshot_melodic")
                else "%s/808" % typ)
    if fam in MERGE:
        return MERGE[fam]
    if fam == "oneshot_melodic/perc":
        return "fx/fx" if _FXWORD.search(name) else "oneshot_melodic/bell"
    if fam == "oneshot_melodic/full" and _has("bass", name):
        return "oneshot_melodic/bass"
    if fam == "fx/fx" and re.search(r"bass ?(hit|shot)", name):
        return "oneshot_melodic/bass"
    if sub == "autre":
        for role, w in WORDS:
            if _has(w, name):
                return "vocal/vocal" if role == "vocal" else "%s/%s" % (typ, role)
    return fam


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
                fam = refine(family_of(r["type"], r["subtype"], r["role"]), n)
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
                fam = refine(family_of(cl["type"], cl["subtype"], cl["role"]), n)
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


def rms_env(y, win_s=0.010):
    """Centred moving RMS over win_s, one value per sample (float32)."""
    n = max(1, int(win_s * SR))
    c = np.concatenate(([0.0], np.cumsum(np.asarray(y, dtype=np.float64) ** 2)))
    lo = np.clip(np.arange(len(y)) - n // 2, 0, len(y))
    hi = np.clip(lo + n, 0, len(y))
    return np.sqrt((c[hi] - c[lo]) / n).astype(np.float32)


def body_span(y, on, hop):
    """Samples from the first to the last gated frame (frames centred on k*hop),
    at most BODY_MAX_S."""
    k = np.flatnonzero(on)
    a = max(0, int(k[0]) * hop - hop // 2)
    b = min(len(y), int(k[-1]) * hop + hop // 2 + 1)
    return y[a:min(b, a + int(BODY_MAX_S * SR))]


def body_rolloff(y, on, hop, cutoff=0.85):
    """85 % energy rolloff of one zero-padded Hann spectrum of the body;
    returns (Hz, bin width in Hz)."""
    seg = np.asarray(body_span(y, on, hop), dtype=np.float64)
    if len(seg) < 16:
        return None, None
    n = max(BODY_NFFT, 1 << int(math.ceil(math.log2(len(seg)))))
    p = np.abs(np.fft.rfft(seg * np.hanning(len(seg)), n)) ** 2
    c = np.cumsum(p)
    if c[-1] <= 0:
        return None, None
    i = int(np.searchsorted(c, cutoff * c[-1]))
    return i * SR / n, SR / n


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
    coup = typ == "drum_oneshot" or len(y) < COUP_MAX_S * SR
    fsz, hop, gate = (COUP_FRAME, COUP_HOP, COUP_GATE_DB) if coup else (FRAME, HOP, GATE_DB)
    out["mode"] = "coup" if coup else "long"
    w = es.Windowing(type="hann", size=fsz, zeroPadding=FRAME - fsz, normalized=False)
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
    for fr in es.FrameGenerator(y, frameSize=fsz, hopSize=hop, startFromZero=False):
        s = spec(w(fr))
        F["cen"].append(cen(s))
        F["roll"].append(roll(s))
        F["flat"].append(flat(np.maximum(s[:FLAT_BINS].astype(np.float64) ** 2, 1e-10).astype(np.float32)))
        F["zcr"].append(zcr(fr))
        F["rms"].append(rms(fr))
        # EnergyBand squares its input itself: give it the magnitude spectrum.
        F["lo"].append(bands[0](s)), F["mi"].append(bands[1](s)), F["hi"].append(bands[2](s))
        M.append(mfcc(s)[1])
    # FrameGenerator adds one trailing frame (mostly padding) beyond the centred
    # frames k*hop, k <= len/hop; drop it so short one-shots are not skewed.
    nf = 1 + len(y) // hop
    F = {k: np.asarray(v[:nf], dtype=np.float64) for k, v in F.items()}
    M = M[:nf]
    on = F["rms"] > F["rms"].max() * 10 ** (gate / 20) if F["rms"].max() > 0 else F["rms"] > 1
    out["active_frames"] = int(on.sum())
    out["centroid_hz"] = med(F["cen"][on])
    out["rolloff_hz"] = med(F["roll"][on])
    out["rolloff_res_hz"] = SR / FRAME
    if typ in ONESHOTS and on.any():
        r, res = body_rolloff(y, on, hop)
        if r is not None:
            out["rolloff_hz"], out["rolloff_res_hz"] = r, res
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
        # EffectiveDuration wants an envelope, not the raw signal; Envelope's
        # default 1.5 s release would hold it up, so a 10 ms RMS envelope.
        out["effective_dur_s"] = float(es.EffectiveDuration(sampleRate=SR)(rms_env(y)))
    except Exception:
        pass
    if pitched(typ, fam):
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
# The counter-measure is independent: librosa loads and resamples the file
# itself (soundfile + soxr), with its own frames (centred STFT), its own gate
# and its own body detection (librosa.effects.trim). Only the definitions of
# the measures and the time grid of f0 are shared, so the two can be compared.
LIB_BEAT_HOP = 128  # tempo lag step at 145 BPM: about 0.7 % (hop 512: 2.8 %)


def lib_measure(data, typ, fam):
    import librosa
    import scipy.fft
    import scipy.signal
    y, _ = librosa.load(io.BytesIO(data), sr=SR, mono=True, duration=MAX_S, res_type="soxr_hq")
    y = np.asarray(y, dtype=np.float32)
    coup = typ == "drum_oneshot" or len(y) < COUP_MAX_S * SR
    win, hop, gate = (COUP_FRAME, COUP_HOP, COUP_GATE_DB) if coup else (FRAME, HOP, GATE_DB)
    S = np.abs(librosa.stft(y, n_fft=FRAME, hop_length=hop, win_length=win, window="hann",
                            center=True, pad_mode="constant"))
    cen = librosa.feature.spectral_centroid(S=S, sr=SR)[0]
    roll = librosa.feature.spectral_rolloff(S=S ** 2, sr=SR, roll_percent=0.85)[0]  # energy, as Essentia RollOff
    flat = librosa.feature.spectral_flatness(S=S[:FLAT_BINS])[0]
    zcr = librosa.feature.zero_crossing_rate(y, frame_length=win, hop_length=hop, center=True)[0]
    rms = librosa.feature.rms(y=y, frame_length=win, hop_length=hop, center=True, pad_mode="constant")[0]
    n = min(len(cen), len(zcr), len(rms))
    cen, roll, flat, zcr, rms = cen[:n], roll[:n], flat[:n], zcr[:n], rms[:n]
    on = rms > rms.max() * 10 ** (gate / 20) if rms.max() > 0 else rms > 1
    out = {"mode": "coup" if coup else "long", "dur_s": len(y) / SR,
           "centroid_hz": med(cen[on]), "rolloff_hz": med(roll[on]), "rolloff_res_hz": SR / FRAME,
           "zcr": med(zcr[on]),
           "flatness_db": med(10 * np.log10(np.maximum(flat[on], 1e-10))) if on.any() else None,
           "rms_db": db(float(np.sqrt(np.mean(rms[on].astype(np.float64) ** 2)))) if on.any() else None}
    if typ in ONESHOTS and len(y) >= 16 and np.abs(y).max() > 0:
        _, (a, b) = librosa.effects.trim(y, top_db=-gate, frame_length=win, hop_length=hop)
        seg = y[a:min(b, a + int(BODY_MAX_S * SR))].astype(np.float64)
        if len(seg) >= 16:
            nf = max(BODY_NFFT, 1 << int(math.ceil(math.log2(len(seg)))))
            p = np.abs(scipy.fft.rfft(seg * scipy.signal.get_window("hann", len(seg)), nf)) ** 2
            c = np.cumsum(p)
            if c[-1] > 0:
                out["rolloff_hz"] = float(np.argmax(c >= 0.85 * c[-1])) * SR / nf
                out["rolloff_res_hz"] = SR / nf
    if pitched(typ, fam):
        yp = y[:int(PITCH_S * SR)]
        f0, vf, _ = librosa.pyin(yp, fmin=30, fmax=2000, sr=SR, frame_length=PFRAME, hop_length=HOP)
        f0 = np.where(vf & np.isfinite(f0), f0, np.nan)
        out["f0_hz"] = med(f0[np.isfinite(f0)])
        out["_f0"] = f0
    if typ in LOOPS and len(y) >= 4 * SR:
        t, _ = librosa.beat.beat_track(y=y, sr=SR, hop_length=LIB_BEAT_HOP)
        out["bpm"] = float(np.atleast_1d(t)[0])
    return out


def pitched(typ, fam):
    """f0 is measured on one-shots only (melodic, 808, bass hits), never on loops."""
    return typ not in LOOPS and (typ in PITCHED or fam.endswith("/808") or fam.endswith("/bass"))


# ---------------------------------------------------------------- ground truth
NOTE = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
_SEP = re.compile(r"[ _\-.()\[\]]+")
_BPM = re.compile(r"(\d{2,3}(?:\.\d+)?)\s*bpm", re.I)
_NOTE = re.compile(r"^([A-G])([#b]?)(-?\d)?(m|min|maj|minor|major)?$")


def truth(base_name, typ):
    """Tempo and note written in a file name, when unambiguous. Used only in
    aggregate (never per sound in the repository)."""
    stem = os.path.splitext(os.path.basename(base_name))[0]
    out = {}
    if typ in LOOPS:
        m = _BPM.search(stem)
        if m:
            out["bpm"] = float(m.group(1))
        else:
            c = [f for f in re.split(r"[ _]+", stem) if f.isdigit() and 60 <= int(f) <= 200]
            if len(c) == 1:
                out["bpm"] = int(c[0])
    if typ in ONESHOTS:
        c = []
        for f in _SEP.split(stem.replace(" #", "#")):
            m = _NOTE.match(f)
            if m and (m.group(2) or m.group(3) is not None or m.group(4)):  # a lone letter is a variant
                c.append((NOTE[m.group(1)] + {"#": 1, "b": -1, "": 0}[m.group(2)]) % 12)
        if len(set(c)) == 1:
            out["note"] = c[0]
    return out


def measure_bytes(data, typ, fam):
    """Both tools on one encoded file: Essentia on our decode + resample,
    librosa on its own load."""
    x, sr = decode(data)
    x = x[:int(MAX_S * sr)]
    st = resample(x, sr)
    if st.ndim == 1:
        st = st[:, None]
    rec = {"sr_in": sr, "ess": ess_measure(st, typ, fam), "lib": lib_measure(data, typ, fam)}
    a, b = rec["ess"].pop("_f0", None), rec["lib"].pop("_f0", None)
    if a is not None and b is not None:
        n = min(len(a), len(b))
        a, b = a[:n], b[:n]
        both = np.isfinite(a) & np.isfinite(b)  # frames voiced for both tools
        rec["ess"]["f0_common_hz"] = med(a[both])
        rec["lib"]["f0_common_hz"] = med(b[both])
        either = np.isfinite(a) | np.isfinite(b)
        if either.any():  # voiced for one tool only = a disagreement
            ok = both & (np.abs(1200 * np.log2(np.where(both, a, 1) / np.where(both, b, 1))) <= 50)
            rec["f0_frames_agree"] = float(ok.sum() / either.sum())
    return rec


def one(job):
    sid, member, fam, style, pack, cle, stock = job
    typ = fam.split("/")[0]  # 808 one-shots are oneshot_melodic/808: pitched
    rec = {"sid": sid, "fam": fam, "style": style, "pack": pack,
           "id": hashlib.sha1((sid + "/" + member).encode("utf-8", "surrogateescape")).hexdigest()[:12]}
    try:
        with zipfile.ZipFile(os.path.join(stock, sid, "archive.bin")) as z:
            data = z.read(member)
        rec.update(measure_bytes(data, typ, fam))
        gt = truth(member, typ)
        if gt:
            rec["truth"] = gt
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


def cents(a, b):
    return 1200 * math.log2(a / b)


BPM_FOLDS = (0.5, 2 / 3, 1, 1.5, 2)


def agree(k, a, b, res=None):
    """Counter-measure tolerance, per measure (None = not comparable).
    res: the coarser spectral bin of the two measures, for rolloff."""
    if a is None or b is None:
        return None
    if k == "rolloff_hz":
        return abs(a - b) <= max(0.05 * max(abs(b), 1e-9), 1.5 * (res or 0.0))
    if k == "centroid_hz":
        return abs(a - b) <= 0.05 * max(abs(b), 1e-9) or abs(a - b) <= 25.0
    if k == "zcr":
        return abs(a - b) <= 0.05 * max(abs(b), 1e-9) or abs(a - b) <= 0.002
    if k in ("flatness_db", "rms_db"):
        return abs(a - b) <= (1.0 if k == "flatness_db" else 0.5)
    if k in ("f0_hz", "f0_common_hz"):
        return a > 0 and b > 0 and abs(cents(a, b)) <= 50
    if k == "f0_hz_octave":  # same pitch class, octave errors forgiven
        return a > 0 and b > 0 and abs((cents(a, b) + 600) % 1200 - 600) <= 50
    if k == "bpm":  # strict
        return a > 0 and b > 0 and abs(a - b) / b <= 0.02
    if k == "bpm_replie":  # half, two thirds, x1, three halves, double
        return a > 0 and b > 0 and min(abs(a * m - b) / b for m in BPM_FOLDS) <= 0.02
    return None


AGREE_KEYS = ("centroid_hz", "rolloff_hz", "flatness_db", "zcr", "rms_db",
              "f0_hz", "f0_hz_octave", "f0_common_hz", "bpm", "bpm_replie")
SOURCE_KEY = {"f0_hz_octave": "f0_hz", "bpm_replie": "bpm"}  # folded keys read the plain value
F0_FRAMES_OK = 0.8  # a sound's f0 tracks agree when >= 80 % of its voiced frames match
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
            for k in AGREE_KEYS + ("f0_trames",):
                if k == "f0_trames":
                    res = [r["f0_frames_agree"] >= F0_FRAMES_OK for r in by_fam[fam] if "f0_frames_agree" in r]
                else:
                    sk = SOURCE_KEY.get(k, k)
                    res = [agree(k, r["ess"].get(sk), r["lib"].get(sk),
                                 max(r["ess"].get("rolloff_res_hz") or 0, r["lib"].get("rolloff_res_hz") or 0))
                           for r in by_fam[fam]]
                    res = [x for x in res if x is not None]
                if not res:
                    continue
                tot[(k, 0)] += len(res)
                tot[(k, 1)] += sum(res)
                f.write("%s\t%s\t%d\t%d\t%.1f\n" % (fam, k, len(res), sum(res), 100.0 * sum(res) / len(res)))
        for k in AGREE_KEYS + ("f0_trames",):
            if tot[(k, 0)]:
                f.write("TOUT\t%s\t%d\t%d\t%.1f\n" % (k, tot[(k, 0)], tot[(k, 1)], 100.0 * tot[(k, 1)] / tot[(k, 0)]))
    # Ground truth written in file names, in aggregate per type only.
    with open(os.path.join(outdir, "essentia-verite.tsv"), "w") as f:
        f.write("type\tmesure\tsons_avec_verite\tessentia_pct\tlibrosa_pct\n")
        by_typ = collections.defaultdict(list)
        for r in ok:
            if r.get("truth"):
                by_typ[r["fam"].split("/")[0]].append(r)
        for typ in sorted(by_typ):
            for k, test in (("bpm", lambda v, t: agree("bpm", v, t)),
                            ("bpm_replie", lambda v, t: agree("bpm_replie", v, t)),
                            ("note", lambda v, t: abs(((12 * math.log2(v / 440.0) + 9) - t + 6) % 12 - 6) <= 0.5)):
                src, tk = ("f0_hz", "note") if k == "note" else ("bpm", "bpm")
                rs = [r for r in by_typ[typ] if tk in r["truth"]]
                if len(rs) < MIN_FAMILY:
                    continue
                pct = []
                for tool in ("ess", "lib"):
                    v = [test(r[tool][src], r["truth"][tk]) if r[tool].get(src) else False for r in rs]
                    pct.append(100.0 * sum(bool(x) for x in v) / len(rs))
                f.write("%s\t%s\t%d\t%.1f\t%.1f\n" % (typ, k, len(rs), pct[0], pct[1]))
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
