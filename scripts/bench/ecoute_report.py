#!/usr/bin/env python3
"""The « ecoute » report (scripts/ecoute.sh): an engineer's reading of rendered songs.

  ecoute_report.py --entry OUT NAME KIND WAV [REF.wav]   one render: its spectrum (1/3 octave, long-term),
                                                          and with REF the null residual; OUT/NAME-KIND.entry.json
  ecoute_report.py --report OUT                           every entry of OUT in OUT/report.md

ps_measure's JSON (OUT/NAME-KIND.json) gives loudness, LRA, true peak, clipping, DC, stereo and bands; this
adds the long-term spectrum, the residual and the flags an engineer would raise. Measures only: what it
sounds like is for a listener (the report ends with what to listen to).
"""
import json
import math
import struct
import sys
from pathlib import Path

import numpy as np

TARGETS = {"spotify": -14.0, "club": -9.0}
CEILING_DBTP = -1.0


def read_wav(path):
    """(rate, float32 array of shape (frames, channels)): PCM 16/24/32 or IEEE float 32."""
    data = Path(path).read_bytes()
    if data[:4] != b"RIFF" or data[8:12] != b"WAVE":
        raise ValueError(f"{path}: not a WAV file")
    pos, fmt, raw = 12, None, None
    while pos + 8 <= len(data):
        cid, size = data[pos:pos + 4], struct.unpack("<I", data[pos + 4:pos + 8])[0]
        body = data[pos + 8:pos + 8 + size]
        if cid == b"fmt ":
            tag, channels, rate = struct.unpack("<HHI", body[:8])
            bits = struct.unpack("<H", body[14:16])[0]
            if tag == 0xFFFE and len(body) >= 26:
                tag = struct.unpack("<H", body[24:26])[0]
            fmt = (tag, channels, rate, bits)
        elif cid == b"data":
            raw = body
        pos += 8 + size + (size & 1)
    if fmt is None or raw is None:
        raise ValueError(f"{path}: no fmt or data chunk")
    tag, channels, rate, bits = fmt
    if tag == 3 and bits == 32:
        x = np.frombuffer(raw, dtype="<f4")
    elif tag == 1 and bits == 16:
        x = np.frombuffer(raw, dtype="<i2").astype(np.float32) / 32768.0
    elif tag == 1 and bits == 24:
        b = np.frombuffer(raw[: len(raw) // 3 * 3], dtype=np.uint8).reshape(-1, 3).astype(np.int32)
        v = b[:, 0] | (b[:, 1] << 8) | (b[:, 2] << 16)
        v = np.where(v >= 1 << 23, v - (1 << 24), v)
        x = v.astype(np.float32) / float(1 << 23)
    elif tag == 1 and bits == 32:
        x = np.frombuffer(raw, dtype="<i4").astype(np.float32) / 2147483648.0
    else:
        raise ValueError(f"{path}: format {tag}/{bits} bits not read")
    frames = len(x) // channels
    return rate, x[: frames * channels].reshape(frames, channels)


def db(v):
    return 20.0 * math.log10(v) if v > 0 else -400.0


def third_octaves(x, rate):
    """Long-term spectrum of the mid signal in 1/3-octave bands (20 Hz to 20 kHz), dB relative to the loudest band."""
    mono = x.mean(axis=1) if x.shape[1] > 1 else x[:, 0]
    n = 16384
    if len(mono) < n:
        return {}
    win = np.hanning(n).astype(np.float32)
    hop = n // 2
    acc = np.zeros(n // 2 + 1)
    count = 0
    # At most ~400 frames spread over the song: the long-term shape, not every frame.
    step = max(hop, (len(mono) - n) // 400)
    for start in range(0, len(mono) - n, step):
        spec = np.fft.rfft(mono[start:start + n] * win)
        acc += (spec.real ** 2 + spec.imag ** 2)
        count += 1
    acc /= max(1, count)
    freqs = np.fft.rfftfreq(n, 1.0 / rate)
    bands = {}
    centre = 20.0
    while centre <= 20000.0:
        lo, hi = centre / 2 ** (1 / 6), centre * 2 ** (1 / 6)
        sel = (freqs >= lo) & (freqs < hi)
        if sel.any():
            bands[round(centre, 1)] = float(acc[sel].sum())
        centre *= 2 ** (1 / 3)
    top = max(bands.values()) if bands else 1.0
    return {f"{k:g}": round(10 * math.log10(v / top + 1e-30), 1) for k, v in bands.items()}


def residual(x, ref):
    """Peak and RMS (dBFS) of x - ref after the best whole-frame lag (within ±4096 frames), and that lag."""
    frames = min(len(x), len(ref))
    a, b = x[:frames, 0].astype(np.float64), ref[:frames, 0].astype(np.float64)
    probe = slice(frames // 4, min(frames, frames // 4 + (1 << 16)))
    best, lag = -1.0, 0
    sa = a[probe]
    for d in range(-4096, 4097, 1):
        lo, hi = probe.start + d, probe.stop + d
        if lo < 0 or hi > frames:
            continue
        c = float(np.dot(sa, b[lo:hi]))
        if c > best:
            best, lag = c, d
    if lag >= 0:
        xa, rb = x[: frames - lag], ref[lag:frames]
    else:
        xa, rb = x[-lag:frames], ref[: frames + lag]
    n = min(len(xa), len(rb))
    diff = xa[:n].astype(np.float64) - rb[:n].astype(np.float64)
    peak = float(np.max(np.abs(diff))) if n else 0.0
    rms = float(np.sqrt(np.mean(diff ** 2))) if n else 0.0
    return {"lag_frames": lag, "peak_dbfs": round(db(peak), 1), "rms_dbfs": round(db(rms), 1),
            "length_frames": [len(x), len(ref)]}


def entry(out, name, kind, wav, ref=None):
    rate, x = read_wav(wav)
    measure = json.loads((Path(out) / f"{name}-{kind}.json").read_text())
    render_path = Path(out) / f"{name}-{kind}.render.json"
    e = {"name": name, "kind": kind, "rate": rate, "seconds": round(len(x) / rate, 2), "measure": measure,
         "render": json.loads(render_path.read_text()) if render_path.exists() else None,
         "third_octaves": third_octaves(x, rate), "spectrogram": f"{name}-{kind}.png"}
    if ref:
        _, r = read_wav(ref)
        e["against"] = {"file": str(ref), **residual(x, r)}
    (Path(out) / f"{name}-{kind}.entry.json").write_text(json.dumps(e, indent=1))


def pick(m, *keys):
    for k in keys:
        if isinstance(m, dict) and k in m and m[k] is not None:
            return m[k]
    return None


def flags(e):
    m, kind = e["measure"], e["kind"]
    out = []
    lufs, tp, lra = pick(m, "lufs_i", "lufs"), pick(m, "true_peak"), pick(m, "lra")
    if pick(m, "non_finite"):
        out.append("**NaN/Inf dans le fichier**")
    if tp is not None and tp > CEILING_DBTP + 0.05:
        out.append(f"crête vraie {tp:.2f} dBTP au-dessus du plafond {CEILING_DBTP:.0f} dBTP")
    if pick(m, "clipped"):
        out.append(f"{m['clipped']} échantillons à pleine échelle")
    dc = pick(m, "dc")
    if isinstance(dc, (int, float)) and abs(dc) > 1e-3:
        out.append(f"DC {dc:.4f}")
    if kind in TARGETS and lufs is not None and abs(lufs - TARGETS[kind]) > 0.5:
        out.append(f"{lufs:.1f} LUFS pour une cible de {TARGETS[kind]:.0f}")
    if lra is not None and lra < 3.0 and e["seconds"] > 30:
        out.append(f"LRA {lra:.1f} LU : très peu de mouvement (écrasé, ou une boucle)")
    if lufs is not None and tp is not None and tp - lufs < 6.0 and lufs > -30:
        out.append(f"PLR {tp - lufs:.1f} dB : très dense, attaques aplaties")
    corr = pick(m, "corr")
    if isinstance(corr, (int, float)) and corr < 0.2:
        out.append(f"corrélation stéréo {corr:.2f} : fragile en mono")
    if isinstance(corr, (int, float)) and corr > 0.98:
        out.append(f"corrélation {corr:.2f} : presque mono (aucune largeur)")
    mono = pick(m, "mono_loss_lu")
    if isinstance(mono, (int, float)) and mono > 3.0:
        out.append(f"le mono perd {mono:.1f} LU")
    infra = pick(m, "infra_below_30")
    if isinstance(infra, (int, float)) and infra > -18.0:
        out.append(f"infra-basses sous 30 Hz à {infra:.1f} dB du total : de la marge perdue au limiteur")
    against = e.get("against")
    if against and against["peak_dbfs"] > -90.0:
        out.append(f"diffère de la référence : résidu crête {against['peak_dbfs']:.1f} dBFS")
    return out


def fmt(v, spec):
    return format(v, spec) if isinstance(v, (int, float)) else "–"


def report(out):
    entries = [json.loads(p.read_text()) for p in sorted(Path(out).glob("*.entry.json"))]
    lines = ["# Écoute : rapport de mesure", "",
             "Rendu par le vrai moteur (ps_render), mesuré par ps_measure (libebur128) et numpy. "
             "Une mesure ne dit pas ce que ça donne à l'oreille : la liste d'écoute est en bas.", "",
             "| Morceau | Rendu | Durée | LUFS-I | LRA | Crête vraie | PLR | Corrélation | Pente dB/oct | Résidu (crête / RMS) | Remarques |",
             "|---|---|---|---|---|---|---|---|---|---|---|"]
    for e in entries:
        m = e["measure"]
        lufs, tp = pick(m, "lufs_i", "lufs"), pick(m, "true_peak")
        plr = tp - lufs if isinstance(lufs, (int, float)) and isinstance(tp, (int, float)) else None
        res = e.get("against")
        res_txt = ("identique" if res["peak_dbfs"] <= -399 else f"{res['peak_dbfs']:.1f} / {res['rms_dbfs']:.1f} dBFS") if res else "–"
        lines.append(f"| {e['name']} | {e['kind']} | {e['seconds']:.0f} s | {fmt(lufs, '.1f')} | {fmt(pick(m, 'lra'), '.1f')} | "
                     f"{fmt(tp, '.2f')} | {fmt(plr, '.1f')} | {fmt(pick(m, 'corr'), '.2f')} | {fmt(pick(m, 'slope_db_oct'), '.1f')} | "
                     f"{res_txt} | {'; '.join(flags(e)) or 'rien à signaler'} |")
    lines += ["", "## Spectre moyen (1/3 d'octave, dB sous la bande la plus forte)", ""]
    if entries:
        centres = list(entries[0]["third_octaves"].keys())
        nominal = (31.5, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 12500, 16000)
        show = sorted({min(centres, key=lambda c: abs(math.log(float(c) / f))) for f in nominal}, key=float) if centres else []
        lines.append("| Morceau | Rendu | " + " | ".join(f"{float(c):.0f} Hz" for c in show) + " |")
        lines.append("|---|---|" + "---|" * len(show))
        for e in entries:
            row = [fmt(e["third_octaves"].get(c), ".1f") for c in show]
            lines.append(f"| {e['name']} | {e['kind']} | " + " | ".join(row) + " |")
    lines += ["", "## Spectrogrammes", ""] + [f"- {e['name']} « {e['kind']} » : `{e['spectrogram']}`" for e in entries]
    lines += ["", "## À écouter (l'oreille, c'est Benjamin)", "",
              "- Chaque rendu signalé ci-dessus, à l'endroit indiqué ; puis l'export, le master Spotify et le master club "
              "du même morceau à la suite, au même volume du téléphone : le club doit sonner plus fort sans pomper ni grésiller, "
              "le Spotify plus aéré.", ""]
    (Path(out) / "report.md").write_text("\n".join(lines))


def main(argv):
    if len(argv) >= 6 and argv[1] == "--entry":
        entry(argv[2], argv[3], argv[4], argv[5], argv[6] if len(argv) > 6 else None)
    elif len(argv) == 3 and argv[1] == "--report":
        report(argv[2])
    else:
        print(__doc__, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
