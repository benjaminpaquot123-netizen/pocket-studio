#!/usr/bin/env python3
"""Tour 6, siège son: what the bench's renders (scripts/song-bench.sh --keep-wav) give through a phone's speaker, and
what the measure can say of the timbres. Reads, per style, OUT/wav/<style>.wav (the export) and OUT/json/<style>.json
(ps_measure); renders again, as the bench does (OUT/tools/ps_render, before the master: --no-limiter, --solo), the kick
(META kick, else pad 16), the basses (META bass), the lead (META lead) and the whole song, measures them and deletes them.
Writes one TSV row per style; no sound is kept.

  son6.py OUT_DIR out.tsv [--curve courbe.tsv]

Phone speaker: the phone's measured curve (scripts/bentest/empreinte.py courbe, third-octave dB) when given, else an
estimated small speaker: 4th-order high-pass at 220 Hz, 2nd-order low-pass at 12 kHz (no phone curve measured yet).
Loudness: BS.1770 K-weighting on 400 ms blocks (absolute and relative gates for the integrated value), applied as a
magnitude in the frequency domain (phase does not change a block's energy). Numbers only, standard library + numpy.
"""
import json
import math
import os
import subprocess
import sys

import numpy as np

RATE = 48000
BANDS = [("sub", 20, 60), ("basse", 60, 250), ("bas_medium", 250, 800), ("medium", 800, 2500), ("presence", 2500, 6000), ("air", 6000, 16000)]


def read_wav(path):
    with open(path, "rb") as f:
        data = f.read()
    pos, fmt, body = 12, None, None
    while pos + 8 <= len(data):
        cid, size = data[pos:pos + 4], int.from_bytes(data[pos + 4:pos + 8], "little")
        if cid == b"fmt ":
            fmt = data[pos + 8:pos + 8 + size]
        elif cid == b"data":
            body = data[pos + 8:pos + 8 + size]
        pos += 8 + size + (size & 1)
    tag, ch, rate = int.from_bytes(fmt[0:2], "little"), int.from_bytes(fmt[2:4], "little"), int.from_bytes(fmt[4:8], "little")
    bits = int.from_bytes(fmt[14:16], "little")
    if tag == 0xFFFE:
        tag = int.from_bytes(fmt[24:26], "little")
    if tag == 3 and bits == 32:
        x = np.frombuffer(body[:len(body) // 4 * 4], dtype="<f4").astype(np.float64)
    elif tag == 1 and bits == 16:
        x = np.frombuffer(body[:len(body) // 2 * 2], dtype="<i2").astype(np.float64) / 32768
    elif tag == 1 and bits == 24:
        b = np.frombuffer(body[:len(body) // 3 * 3], dtype=np.uint8).reshape(-1, 3).astype(np.int32)
        v = b[:, 0] | (b[:, 1] << 8) | (b[:, 2] << 16)
        x = np.where(v >= 1 << 23, v - (1 << 24), v).astype(np.float64) / (1 << 23)
    else:
        raise SystemExit(f"{path}: format {tag}/{bits}")
    x = x[:len(x) // ch * ch].reshape(-1, ch)
    if ch == 1:
        x = np.repeat(x, 2, axis=1)
    assert rate == RATE, path
    return x[:, :2]


def biquad_power(f, b, a):
    z = np.exp(-1j * 2 * np.pi * f / RATE)
    return np.abs((b[0] + b[1] * z + b[2] * z * z) / (a[0] + a[1] * z + a[2] * z * z)) ** 2


def k_weight(f):
    # BS.1770 stage 1 (shelf) and stage 2 (RLB high-pass) at 48 kHz.
    return biquad_power(f, [1.53512485958697, -2.69169618940638, 1.19839281085285], [1.0, -1.69065929318241, 0.73248077421585]) * \
        biquad_power(f, [1.0, -2.0, 1.0], [1.0, -1.99004745483398, 0.99007225036621])


def butter_hp4(f, fc):
    r = (f / fc) ** 2
    return r ** 4 / (1 + r ** 4)


def butter_lp2(f, fc):
    r = (f / fc) ** 2
    return 1 / (1 + r ** 2)


def phone_power(f, curve=None):
    if curve:
        fc = np.array([c for c, _ in curve])
        db = np.array([d for _, d in curve])
        g = np.interp(np.log2(np.maximum(f, 1.0)), np.log2(fc), db, left=db[0] - 24, right=db[-1] - 12)
        return 10 ** (g / 10)
    return butter_hp4(f, 220.0) * butter_lp2(f, 12000.0)


def shape(x, power):
    """[x] (n, 2) through a magnitude response power(f)."""
    n = len(x)
    f = np.fft.rfftfreq(n, 1 / RATE)
    g = np.sqrt(power(f))
    return np.stack([np.fft.irfft(np.fft.rfft(x[:, c]) * g, n) for c in range(x.shape[1])], axis=1)


def blocks_ms(x, ms=400, hop_ms=100):
    """Mean square of K-weighted blocks (sum of the two channels' powers), one per hop."""
    n = int(RATE * ms / 1000)
    h = int(RATE * hop_ms / 1000)
    p = np.sum(x * x, axis=1)
    c = np.concatenate([[0.0], np.cumsum(p)])
    starts = np.arange(0, max(1, len(p) - n + 1), h)
    return (c[starts + n] - c[starts]) / n, starts


def lufs_i(xk):
    ms, _ = blocks_ms(xk)
    l = -0.691 + 10 * np.log10(np.maximum(ms, 1e-20))
    g = ms[l > -70]
    if len(g) == 0:
        return float("nan")
    rel = -0.691 + 10 * np.log10(np.mean(g)) - 10
    g2 = ms[(l > -70) & (l > rel)]
    return float(-0.691 + 10 * np.log10(np.mean(g2)))


def lufs_of(x, power=None):
    return lufs_i(shape(x, (lambda f: k_weight(f) * power(f)) if power else k_weight))


def band_shares(x, power=None):
    """Each band's power as a share of 20 Hz-16 kHz (dB)."""
    n = len(x)
    f = np.fft.rfftfreq(n, 1 / RATE)
    spec = sum(np.abs(np.fft.rfft(x[:, c])) ** 2 for c in range(2))
    if power:
        spec = spec * power(f)
    tot = spec[(f >= 20) & (f < 16000)].sum()
    return {name: 10 * math.log10(max(spec[(f >= lo) & (f < hi)].sum(), 1e-30) / tot) for name, lo, hi in BANDS}


def section_lufs(x, sections, power, labels):
    """Mean short-term level (K, through [power]) of the sections whose label starts with one of [labels]."""
    xk = shape(x, (lambda f: k_weight(f) * power(f)) if power else k_weight)
    out = []
    for i, (start, label) in enumerate(sections):
        end = sections[i + 1][0] if i + 1 < len(sections) else len(x)
        if any(label.startswith(l) for l in labels) and end - start > RATE:
            seg = xk[start:end]
            out.append(float(np.mean(np.sum(seg * seg, axis=1))))
    return -0.691 + 10 * math.log10(np.mean(out)) if out else None


def sections_of(pscmd):
    s = []
    with open(pscmd, encoding="utf-8", errors="replace") as f:
        for line in f:
            if line.startswith("SECTION "):
                p = line.rstrip("\n").split(" ", 3)
                s.append((int(p[2]), p[3] if len(p) > 3 else ""))
    return s


def timbre(x):
    """What a measure can say of a stem's colour: brightness (share over 2 kHz), flatness (noise-like, 1, or tonal, 0)
    in 200 Hz-8 kHz, movement (spectral flux of 46 ms frames, normalised), centroid wobble (octaves, its spread over time
    where it plays), width (side over mid, dB)."""
    m = x.mean(axis=1)
    s = (x[:, 0] - x[:, 1]) / 2
    n, hop = 2048, 1024
    w = np.hanning(n)
    frames = []
    for st in range(0, len(m) - n, hop * 4):
        frames.append(np.abs(np.fft.rfft(m[st:st + n] * w)) ** 2)
    if not frames:
        return {}
    P = np.array(frames)
    f = np.fft.rfftfreq(n, 1 / RATE)
    e = P.sum(axis=1)
    on = e > np.max(e) * 1e-3
    if on.sum() < 4:
        return {}
    P = P[on]
    band = (f >= 200) & (f < 8000)
    flat = np.exp(np.mean(np.log(P[:, band] + 1e-20), axis=1)) / (np.mean(P[:, band], axis=1) + 1e-20)
    cen = (P * f).sum(axis=1) / (P.sum(axis=1) + 1e-20)
    norm = P / (P.sum(axis=1, keepdims=True) + 1e-20)
    flux = np.sqrt(np.sum(np.diff(norm, axis=0) ** 2, axis=1))
    return {
        "brillance_db": 10 * math.log10(P[:, f >= 2000].sum() / P.sum()),
        "platitude": float(np.median(flat)),
        "mouvement": float(np.median(flux)),
        "centroide_hz": float(np.median(cen)),
        "centroide_ecart_oct": float(np.std(np.log2(np.maximum(cen, 20)))),
        "largeur_db": 10 * math.log10((np.mean(s * s) + 1e-20) / (np.mean(m * m) + 1e-20)),
    }


def jumps(x):
    """Silences (over 1 s under -50 LUFS short-term, inside the song) and jumps (a 400 ms block 12 dB over the second
    around it): counts."""
    ms, starts = blocks_ms(shape(x, k_weight))
    l = -0.691 + 10 * np.log10(np.maximum(ms, 1e-20))
    body = l[int(5 * 10):max(int(5 * 10), len(l) - 10 * 10)]
    sil = 0
    run = 0
    for v in body:
        run = run + 1 if v < -50 else 0
        if run == 10:
            sil += 1
    jumps = 0
    for i in range(10, len(l) - 10):
        around = np.concatenate([l[i - 10:i - 4], l[i + 5:i + 11]])
        if l[i] > -30 and l[i] - np.max(around) >= 12:
            jumps += 1
    return sil, jumps


def fmt(v):
    if v is None or (isinstance(v, float) and not math.isfinite(v)):
        return ""
    return f"{v:.2f}" if isinstance(v, float) else str(v)


def main(argv):
    out_dir, tsv = argv[0], argv[1]
    curve = None
    if "--curve" in argv:
        with open(argv[argv.index("--curve") + 1]) as f:
            curve = [(float(a), float(b)) for a, b in (l.split("\t") for l in f if "\t" in l and l.split("\t")[1].strip())]
    phone = lambda f: phone_power(f, curve)
    styles = sorted(p[:-5] for p in os.listdir(os.path.join(out_dir, "json")) if p.endswith(".json"))
    rows = []
    for s in styles:
        j = json.load(open(os.path.join(out_dir, "json", s + ".json")))
        x = read_wav(os.path.join(out_dir, "wav", s + ".wav"))
        sec = sections_of(os.path.join(out_dir, "cmds", s + ".pscmd"))
        r = {"style": s}
        for k in ("lufs_i", "lra", "true_peak", "plr", "clipped", "corr", "corr_below_120", "side_mid", "mono_loss_lu", "slope_db_oct", "drop_minus_groove", "main_lufs"):
            r[k] = j.get(k)
        r["bass_phone_300"] = (j.get("bass") or {}).get("phone_300")
        lf = lufs_of(x)
        lp = lufs_of(x, phone)
        r["lufs_i_mesure"] = lf
        r["lufs_tel"] = lp
        r["perte_tel"] = lp - lf
        for name, v in band_shares(x).items():
            r["b_" + name] = v
        for name, v in band_shares(x, phone).items():
            r["tel_" + name] = v
        d = section_lufs(x, sec, None, ["Drop", "Refrain"])
        g = section_lufs(x, sec, None, ["Groove", "Couplet"])
        dp = section_lufs(x, sec, phone, ["Drop", "Refrain"])
        gp = section_lufs(x, sec, phone, ["Groove", "Couplet"])
        r["drop_groove"] = d - g if d is not None and g is not None else None
        r["drop_groove_tel"] = dp - gp if dp is not None and gp is not None else None
        r["silences"], r["sauts"] = jumps(x)
        cmd = os.path.join(out_dir, "cmds", s + ".pscmd")
        meta = {}
        with open(cmd, encoding="utf-8", errors="replace") as f:
            for line in f:
                if line.startswith("META "):
                    p = line.split(" ", 2)
                    if len(p) == 3:
                        meta.setdefault(p[1], p[2].strip())
        tools = os.path.join(out_dir, "tools", "ps_render")
        tmp = os.path.join(out_dir, "son6-tmp")
        os.makedirs(tmp, exist_ok=True)
        pre = os.path.join(tmp, s + ".pre.wav")
        subprocess.run([tools, "--no-limiter", cmd, pre], check=True, capture_output=True)
        xp = read_wav(pre)
        os.remove(pre)
        pre_full, pre_tel = lufs_of(xp), lufs_of(xp, phone)
        del xp
        for stem, solo in (("kick", meta.get("kick", "16")), ("bass", meta.get("bass")), ("lead", meta.get("lead"))):
            if not solo:
                continue
            p = os.path.join(tmp, f"{s}.{stem}.wav")
            subprocess.run([tools, "--no-limiter", "--solo", solo, cmd, p], check=True, capture_output=True)
            y = read_wav(p)
            os.remove(p)
            full = lufs_of(y)
            tel = lufs_of(y, phone)
            # How much of the stem a phone keeps, and where it sits against the whole song (before the master) at full
            # range and on the phone: the phone's loss of the stem beyond the song's own.
            r[stem + "_garde_tel"] = tel - full
            r[stem + "_contre_mix"] = full - pre_full
            r[stem + "_contre_mix_tel"] = tel - pre_tel
            if stem in ("bass", "lead"):
                for k, v in timbre(y).items():
                    r[f"{stem}_{k}"] = v
            del y
        rows.append(r)
        print(s, f"LUFS {fmt(lf)} tel {fmt(lp)} kick_tel {fmt(r.get('kick_contre_mix_tel'))}", flush=True)
    keys = []
    for r in rows:
        for k in r:
            if k not in keys:
                keys.append(k)
    with open(tsv, "w", encoding="utf-8") as f:
        f.write("\t".join(keys) + "\n")
        for r in rows:
            f.write("\t".join(fmt(r.get(k)) for k in keys) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
