#!/usr/bin/env python3
"""BenStudio Test, « Écoute au micro » (docs/PLAN-TEST-APK.md, T1b): the phone against the Linux bench.

The phone sends, in mesures.json (« micro »), its speaker's curve (third-octave, dB) and each song's fingerprint at
the microphone (energy per octave). This tool measures the same fingerprint on the bench's render of the same song,
heard « as on Benjamin's phone » (the bench's third-octave energies weighted by the phone's curve, in place of the
guessed 300 Hz high-pass), and compares the two, octave by octave.

  empreinte.py comparer mesures.json --wav STYLE=bench.wav [--wav …] [--out rapport.md]
  empreinte.py courbe mesures.json                     # the curve, one band per line
  empreinte.py filtrer mesures.json in.wav out.wav     # a WAV as the phone's speaker plays it (for the ear and the bench)

scripts/bentest/comparer-telephone.sh renders the four songs through the live bench and runs « comparer ».

Same analysis as the phone's (app/src/benTest/…/Spectre.kt): Hann window of 16384 samples, half overlapped, each
third-octave band the power of the bins from fc·2^-1/6 (in) to fc·2^1/6 (out), octave = 3 third-octaves.
Lights (starting values, set again after the first phone): vert ≤ 3 dB, orange ≤ 6 dB, rouge beyond, on the worst
octave both sides heard. A room, a microphone and a speaker are not a lab: 3 dB is the microphone's own wobble.
"""
import json
import math
import sys
import wave

import numpy as np

SIZE = 16384
CENTRES = [1000.0 * 2 ** ((n - 13) / 3) for n in range(26)]
CENTRES[13] = 1000.0
NAMES = ["50", "63", "80", "100", "125", "160", "200", "250", "315", "400", "500", "630", "800",
         "1000", "1250", "1600", "2000", "2500", "3150", "4000", "5000", "6300", "8000", "10000", "12500", "16000"]
OCTAVES = [63, 125, 250, 500, 1000, 2000, 4000, 8000]
MIDDLES = [NAMES.index(str(o)) for o in OCTAVES]
VERT_DB = 3.0
ORANGE_DB = 6.0


def third_octave_energies(x, rate=48000):
    """(energies summed over the frames, frame count) of mono float samples: the phone's frames, the phone's bins."""
    x = np.asarray(x, dtype=np.float64)
    hop = SIZE // 2
    frames = 0 if len(x) < SIZE else 1 + (len(x) - SIZE) // hop
    if frames == 0:
        return [0.0] * len(CENTRES), 0
    win = 0.5 - 0.5 * np.cos(2 * np.pi * np.arange(SIZE) / SIZE)
    idx = np.arange(SIZE)[None, :] + hop * np.arange(frames)[:, None]
    power = np.zeros(SIZE // 2 + 1)
    for start in range(0, frames, 256):
        spec = np.fft.rfft(x[idx[start:start + 256]] * win, axis=1)
        power += np.sum(spec.real ** 2 + spec.imag ** 2, axis=0)
    out = []
    for fc in CENTRES:
        lo = math.ceil(fc * 2 ** (-1 / 6) * SIZE / rate)
        hi = min(math.ceil(fc * 2 ** (1 / 6) * SIZE / rate), SIZE // 2)
        out.append(float(np.sum(power[lo:hi])))
    return out, frames


def octaves(energies, frames, noise=None):
    """dB per octave, 0 dB = mean of 500 Hz … 4 kHz; None under the noise ([noise]: mean power per frame per band)."""
    known = [e for e in energies if e is not None]
    top = max(known) if known else 0.0
    lin = []
    for m in MIDDLES:
        thirds = energies[m - 1:m + 2]
        if any(e is None for e in thirds):
            # A third the phone's curve could not say (under its room): this octave cannot be compared.
            lin.append(None)
            continue
        e = sum(thirds)
        n = sum(noise[m - 1:m + 2]) * frames if noise else 0.0
        heard = e >= 4 * n if n > 0 else e - n > top * 1e-9
        lin.append(e - n if heard and e - n > 0 else None)
    mid = [v for o, v in zip(OCTAVES, lin) if 500 <= o <= 4000 and v is not None]
    if not mid:
        return [None] * len(OCTAVES)
    ref = sum(mid) / len(mid)
    return [None if v is None else 10 * math.log10(v / ref) for v in lin]


def apply_curve(energies, curve_db):
    """The energies as the phone's speaker gives them: each band weighted by the curve; a band the phone did not hear
    over its room (None) is unknown (None), not silent: the octave holding it is left out of the comparison."""
    return [None if c is None else e * 10 ** (c / 10) for e, c in zip(energies, curve_db)]


def compare(phone, bench):
    """Per octave: phone minus bench after lining both up on their common octaves of 500 Hz … 4 kHz (None: missing)."""
    if not phone or not bench:
        return []
    both = [i for i in range(len(OCTAVES)) if phone[i] is not None and bench[i] is not None]
    mid = [i for i in both if 500 <= OCTAVES[i] <= 4000]
    if not mid:
        return [None] * len(OCTAVES)
    offset = sum(phone[i] - bench[i] for i in mid) / len(mid)
    return [phone[i] - bench[i] - offset if i in both else None for i in range(len(OCTAVES))]


def fmt(v):
    return "—" if v is None else f"{v:+.1f}".replace(".", ",")


def light(gaps):
    """(feu, why) from the gaps: vert ≤ 3 dB, orange ≤ 6 dB, rouge beyond (the octaves over the limit named)."""
    seen = [(o, g) for o, g in zip(OCTAVES, gaps) if g is not None]
    if not seen:
        return "non concluant", "aucune octave entendue des deux côtés"
    worst = max(abs(g) for _, g in seen)
    if worst <= VERT_DB:
        return "vert", ""
    feu = "orange" if worst <= ORANGE_DB else "rouge"
    over = [f"{o} Hz {fmt(g)} dB" for o, g in seen if abs(g) > VERT_DB]
    return feu, "téléphone contre banc : " + ", ".join(over)


def read_wav(path, start_s=0.0, length_s=None):
    """Mono float samples of a 16-bit, 24-bit or 32-bit float WAV (channels averaged), from [start_s] for [length_s]."""
    with open(path, "rb") as f:
        data = f.read()
    if data[:4] != b"RIFF" or data[8:12] != b"WAVE":
        raise SystemExit(f"{path}: not a WAV")
    pos, fmt_chunk, body = 12, None, None
    while pos + 8 <= len(data):
        cid, size = data[pos:pos + 4], int.from_bytes(data[pos + 4:pos + 8], "little")
        if cid == b"fmt ":
            fmt_chunk = data[pos + 8:pos + 8 + size]
        elif cid == b"data":
            body = data[pos + 8:pos + 8 + size]
        pos += 8 + size + (size & 1)
    if fmt_chunk is None or body is None:
        raise SystemExit(f"{path}: no fmt or data chunk")
    tag, channels, rate = int.from_bytes(fmt_chunk[0:2], "little"), int.from_bytes(fmt_chunk[2:4], "little"), int.from_bytes(fmt_chunk[4:8], "little")
    bits = int.from_bytes(fmt_chunk[14:16], "little")
    if tag == 0xFFFE:
        tag = int.from_bytes(fmt_chunk[24:26], "little")
    if tag == 3 and bits == 32:
        x = np.frombuffer(body[:len(body) // 4 * 4], dtype="<f4").astype(np.float64)
    elif tag == 1 and bits == 16:
        x = np.frombuffer(body[:len(body) // 2 * 2], dtype="<i2").astype(np.float64) / 32768
    elif tag == 1 and bits == 24:
        b = np.frombuffer(body[:len(body) // 3 * 3], dtype=np.uint8).reshape(-1, 3).astype(np.int32)
        v = b[:, 0] | (b[:, 1] << 8) | (b[:, 2] << 16)
        x = np.where(v >= 1 << 23, v - (1 << 24), v).astype(np.float64) / (1 << 23)
    else:
        raise SystemExit(f"{path}: WAV format {tag}/{bits} bits not read")
    x = x[:len(x) // channels * channels].reshape(-1, channels).mean(axis=1)
    a = int(start_s * rate)
    b = len(x) if length_s is None else a + int(length_s * rate)
    return x[a:b], rate


def write_wav(path, x, rate):
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(pcm.tobytes())


def micro_of(path):
    with open(path, encoding="utf-8") as f:
        m = json.load(f).get("micro")
    if not m or not m.get("courbe_db"):
        raise SystemExit(f"{path}: no speaker's curve (« micro » › « courbe_db ») in it: was « Écoute au micro » run?")
    if all(v is None for v in m["courbe_db"]):
        raise SystemExit(f"{path}: courbe du haut-parleur vide : aucune bande entendue au-dessus du bruit de la pièce (son coupé, pièce trop bruyante ?)")
    return m


def filter_curve(x, rate, curve_db):
    """[x] through the curve: a linear-phase gain per frequency, interpolated between the band centres (log
    frequency); a band under the room's noise counts as the curve's lowest value minus 20 dB."""
    known = [(c, v) for c, v in zip(CENTRES, curve_db) if v is not None]
    floor = min(v for _, v in known) - 20
    pts = [(c, floor if v is None else v) for c, v in zip(CENTRES, curve_db)]
    n = len(x)
    f = np.fft.rfftfreq(n, 1 / rate)
    lf = np.log2(np.maximum(f, 1.0))
    gain_db = np.interp(lf, [math.log2(c) for c, _ in pts], [v for _, v in pts])
    return np.fft.irfft(np.fft.rfft(x) * 10 ** (gain_db / 20), n)


def cmd_courbe(args):
    m = micro_of(args[0])
    for name, v in zip(m.get("tiers", NAMES), m["courbe_db"]):
        print(f"{name}\t{'' if v is None else f'{v:.2f}'}")
    return 0


def cmd_filtrer(args):
    m = micro_of(args[0])
    x, rate = read_wav(args[1])
    y = filter_curve(x, rate, m["courbe_db"])
    peak = float(np.max(np.abs(y))) if len(y) else 0.0
    if peak > 0.99:
        y = y * 0.99 / peak
    write_wav(args[2], y, rate)
    print(f"{args[2]} : {len(y) / rate:.1f} s comme sur le haut-parleur du téléphone")
    return 0


def cmd_comparer(args):
    path, wavs, out = args[0], {}, None
    i = 1
    while i < len(args):
        if args[i] == "--wav" and i + 1 < len(args) and "=" in args[i + 1]:
            style, w = args[i + 1].split("=", 1)
            wavs[style] = w
            i += 2
        elif args[i] == "--out" and i + 1 < len(args):
            out = args[i + 1]
            i += 2
        else:
            raise SystemExit(f"unknown argument {args[i]}")
    m = micro_of(path)
    prints = m.get("empreintes", {})
    lines = ["# Écoute au micro : téléphone contre banc Linux", "",
             "Le banc entendu à travers la courbe du haut-parleur du téléphone (à la place du filtre 300 Hz deviné), "
             "comparé à ce que le micro du téléphone a entendu. Écart par octave en dB, aligné sur 500 Hz à 4 kHz ; "
             f"vert ≤ {VERT_DB:g} dB, orange ≤ {ORANGE_DB:g} dB, rouge au-delà.", "",
             "| Style | " + " | ".join(f"{o} Hz" for o in OCTAVES) + " | Feu | Pourquoi |",
             "|---|" + "---|" * len(OCTAVES) + "---|---|"]
    worst = 0
    for style, w in sorted(wavs.items()):
        p = prints.get(style)
        if not p:
            lines.append(f"| {style} |" + " |" * len(OCTAVES) + " non concluant | pas d'empreinte du téléphone |")
            continue
        x, rate = read_wav(w, float(p.get("debut_s", 0)), float(p.get("duree_s", 30)) or None)
        energies, frames = third_octave_energies(x, rate)
        gaps = compare(p["db"], octaves(apply_curve(energies, m["courbe_db"]), frames))
        feu, why = light(gaps)
        worst = max(worst, {"vert": 0, "non concluant": 0, "orange": 1, "rouge": 2}[feu])
        lines.append(f"| {style} | " + " | ".join(fmt(g) for g in gaps) + f" | {feu} | {why} |")
    text = "\n".join(lines) + "\n"
    if out:
        with open(out, "w", encoding="utf-8") as f:
            f.write(text)
    print(text, end="")
    return 0


def main(argv):
    if not argv or argv[0] not in ("comparer", "courbe", "filtrer"):
        print(__doc__, file=sys.stderr)
        return 2
    need = {"comparer": 2, "courbe": 1, "filtrer": 3}[argv[0]]
    if len(argv) - 1 < need:
        print(__doc__, file=sys.stderr)
        return 2
    return {"comparer": cmd_comparer, "courbe": cmd_courbe, "filtrer": cmd_filtrer}[argv[0]](argv[1:])


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
