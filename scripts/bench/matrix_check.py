#!/usr/bin/env python3
"""Checks the export matrix's files with ffmpeg (lot E, scripts/export-matrix.sh) and writes report.md.

    matrix_check.py <matrix folder>

Per case: ffprobe reads the file (codec, rate, bits, length, title, artist), ffmpeg decodes it without
an error, ebur128 reads its integrated loudness (±0.5 LU of the target) and its true peak (under the
ceiling). Per style: the stems add up to the mix under -60 dB. Exit 1 on any failure.
"""
import json
import os
import re
import subprocess
import sys

import numpy as np

TARGETS = {"spotify": -14.0, "apple": -16.0, "club": -9.0, "free": -11.0}
CODECS = {"wav": ("pcm_s16le", "pcm_s24le", "pcm_f32le"), "flac": ("flac",), "m4a": ("aac",)}


def run(args):
    return subprocess.run(args, capture_output=True, text=True)


def ebur128(path):
    out = run(["ffmpeg", "-nostats", "-hide_banner", "-i", path, "-map", "0:a", "-af", "ebur128=peak=true", "-f", "null", "-"]).stderr
    summary = out[out.rfind("Summary:"):]
    i = re.search(r"I:\s+(-?[\d.]+) LUFS", summary)
    p = re.search(r"Peak:\s+(-?[\d.]+|-inf) dBFS", summary)
    return (float(i.group(1)) if i else None), (float(p.group(1)) if p and p.group(1) != "-inf" else None)


def read_wav(path):
    with open(path, "rb") as f:
        b = f.read()
    pos, fmt, data = 12, None, None
    while pos + 8 <= len(b):
        cid, size = b[pos:pos + 4], int.from_bytes(b[pos + 4:pos + 8], "little")
        if cid == b"fmt ":
            fmt = (int.from_bytes(b[pos + 8:pos + 10], "little"), int.from_bytes(b[pos + 10:pos + 12], "little"),
                   int.from_bytes(b[pos + 22:pos + 24], "little"))
        elif cid == b"data":
            data = b[pos + 8:pos + 8 + size]
            break
        pos += 8 + size + (size & 1)
    code, ch, bits = fmt
    if code == 3:
        x = np.frombuffer(data, dtype="<f4").astype(np.float64)
    elif bits == 16:
        x = np.frombuffer(data, dtype="<i2").astype(np.float64) / 32768.0
    else:
        raw = np.frombuffer(data, dtype=np.uint8).reshape(-1, 3).astype(np.int32)
        v = raw[:, 0] | (raw[:, 1] << 8) | (raw[:, 2] << 16)
        v = np.where(v >= 1 << 23, v - (1 << 24), v)
        x = v.astype(np.float64) / 8388608.0
    return x.reshape(-1, ch)


def check_case(folder, case):
    cid, style, fmt, rate, bits, target, ceiling = case
    path = os.path.join(folder, "cases", f"{cid}.{fmt}")
    report = json.load(open(os.path.join(folder, "cases", f"{cid}.json")))
    problems, notes = [], []
    if not os.path.exists(path):
        return {"case": cid, "ok": False, "problems": ["no file"], "row": case}
    probe = run(["ffprobe", "-v", "error", "-print_format", "json", "-show_streams", "-show_format", path])
    if probe.returncode != 0:
        problems.append("ffprobe: " + probe.stderr.strip()[:120])
        info = {"streams": [], "format": {}}
    else:
        info = json.loads(probe.stdout)
    audio = [s for s in info["streams"] if s.get("codec_type") == "audio"]
    tags = {k.lower(): v for k, v in info.get("format", {}).get("tags", {}).items()}
    for s in info["streams"]:
        tags.update({k.lower(): v for k, v in s.get("tags", {}).items()})
    if not audio:
        problems.append("no audio stream")
    else:
        a = audio[0]
        if a["codec_name"] not in CODECS[fmt]:
            problems.append(f"codec {a['codec_name']}")
        if int(a["sample_rate"]) != int(rate):
            problems.append(f"rate {a['sample_rate']}")
        if fmt != "m4a":
            got = int(a.get("bits_per_raw_sample") or a.get("bits_per_sample") or 0)
            want = 24 if (fmt == "flac" and bits == "32") else int(bits)
            if a["codec_name"] == "pcm_f32le":
                got = 32
            if got != want:
                problems.append(f"bits {got} (want {want})")
        dur = float(info["format"].get("duration", 0))
        if abs(dur - report["seconds"]) > 0.1:
            problems.append(f"length {dur:.2f} s vs {report['seconds']:.2f} s")
    if tags.get("title") != f"Matrice {cid}" or tags.get("artist") != "BenStudio":
        problems.append(f"tags {tags.get('title')!r} / {tags.get('artist')!r}")
    decode = run(["ffmpeg", "-v", "error", "-i", path, "-map", "0:a", "-f", "null", "-"])
    if decode.returncode != 0 or decode.stderr.strip():
        problems.append("decode: " + decode.stderr.strip()[:120])
    lufs, peak = ebur128(path)
    if target in TARGETS:
        if lufs is None or abs(lufs - TARGETS[target]) > 0.5:
            problems.append(f"LUFS {lufs} (target {TARGETS[target]})")
    elif target == "style":
        want, loud = report["target_lufs"], report["loud_lufs"]
        if abs(loud - want) > 0.5:
            # The chain stops short of a target that loud rather than squash the Drop (S5: 3 LU at most).
            if loud < want and report["loss_db"] >= 2.9:
                notes.append(f"Drop at {loud:.1f} for {want:.1f} (the chain's 3 LU taken)")
            else:
                problems.append(f"Drop {loud:.2f} LUFS (target {want:.2f})")
    if target != "none":
        if peak is None or peak > float(ceiling):
            problems.append(f"true peak {peak} over {ceiling}")
    return {"case": cid, "ok": not problems, "problems": problems, "notes": notes, "row": case, "lufs": lufs, "peak": peak,
            "lra": report.get("lra"), "plr": (report["true_peak_db"] - report["lufs"]) if report["lufs"] > -100 else None}


def check_stems(folder, style):
    d = os.path.join(folder, "stems", style)
    mode = open(os.path.join(d, "mode.txt")).read().strip()
    mix = read_wav(os.path.join(d, "mix.wav"))
    total = np.zeros_like(mix)
    count = 0
    for name in sorted(os.listdir(d)):
        if not name.startswith("stem-"):
            continue
        s = read_wav(os.path.join(d, name))
        n = min(len(s), len(total))
        if np.max(np.abs(s)) > 0:
            count += 1
        total[:n] += s[:n]
        if len(s) > len(total):
            extra = s[len(total):]
            if np.max(np.abs(extra)) > 1e-4:
                return {"style": style, "mode": mode, "ok": False, "residual": None, "count": count, "why": f"{name} longer than the mix"}
    peak = np.max(np.abs(mix))
    residual = 20 * np.log10(max(np.max(np.abs(total - mix)), 1e-12) / max(peak, 1e-12))
    return {"style": style, "mode": mode, "ok": residual < -60.0 and peak > 0.01, "residual": residual, "count": count, "why": ""}


def main():
    folder = sys.argv[1]
    lines = [l.rstrip("\n").split("\t") for l in open(os.path.join(folder, "cases.tsv")) if l.strip()]
    header, cases = lines[0], lines[1:]
    results = [check_case(folder, c) for c in cases if os.path.exists(os.path.join(folder, "cases", f"{c[0]}.json"))]
    stems = [check_stems(folder, s) for s in sorted(os.listdir(os.path.join(folder, "stems"))) if os.path.isdir(os.path.join(folder, "stems", s))]
    md = ["# Matrice des exports (paires)", "",
          "| cas | style | format | Hz | bits | cible | plafond | LUFS | crête | LRA | PLR | résultat |",
          "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"]
    for r in results:
        c = r["row"]
        fmt = lambda v, d=1: "—" if v is None else f"{v:.{d}f}"
        verdict = "OK" if r["ok"] else "ÉCHEC : " + "; ".join(r["problems"])
        if r.get("notes"):
            verdict += " (" + "; ".join(r["notes"]) + ")"
        md.append(f"| {c[0]} | {c[1]} | {c[2]} | {c[3]} | {c[4]} | {c[5]} | {c[6]} | {fmt(r.get('lufs'))} | {fmt(r.get('peak'))} | "
                  f"{fmt(r.get('lra'))} | {fmt(r.get('plr'))} | {verdict} |")
    md += ["", "| stems | mode | pistes | résidu (dB) | résultat |", "| --- | --- | --- | --- | --- |"]
    for s in stems:
        res = "—" if s["residual"] is None else f"{s['residual']:.1f}"
        verdict = "OK" if s["ok"] else "ÉCHEC " + s["why"]
        md.append(f"| {s['style']} | {s['mode']} | {s['count']} | {res} | {verdict} |")
    failed = [r for r in results if not r["ok"]] + [s for s in stems if not s["ok"]]
    md += ["", f"{len(results)} cas, {len(stems)} styles en stems, {len(failed)} échec(s)."]
    open(os.path.join(folder, "report.md"), "w").write("\n".join(md) + "\n")
    print("\n".join(md))
    if len(results) != len(cases):
        print(f"{len(cases) - len(results)} case(s) not rendered", file=sys.stderr)
        sys.exit(1)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
