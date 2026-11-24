#!/usr/bin/env python3
"""Where the audio callback's time goes, style by style and brick by brick (tour 9, B1).

Reads the outputs of a measuring ps_live (-DPS_PROFILE=ON, core/Profile.h), named
<style>-<run>.txt, and writes a Markdown report: the load per style split by family (sound
sources, effects, reverb, mixing), each style's dearest bricks, and the bricks ranked by cost.
The fastest run of each style is kept (another process only ever slows a run down).

On a Linux PC the ORDER of the bricks is what counts, not the figures: only the tablet tells
whether a style cuts out.
"""
import argparse
import glob
import os
import re

FAMILIES = [
    ("Sons", lambda b: b in ("drums", "pad-sampler", "acid-303", "synth", "soundfont", "plaits", "sample-instrument",
                             "multi-sampler", "bass-808", "audio-clips")),
    ("Réverbération", lambda b: b in ("reverb-bus", "fx-reverb")),
    ("Effets", lambda b: b.startswith("fx-") or b in ("live-fx", "delay-bus")),
    ("Mixage", lambda b: b in ("mixer-strips", "master-chain")),
    ("Reste du moteur", lambda b: True),
]


def family(brick):
    for name, test in FAMILIES:
        if test(brick):
            return name
    return FAMILIES[-1][0]


def read(path):
    bricks, seconds = {}, 0.0
    for line in open(path, encoding="utf-8"):
        m = re.match(r"BRICK (\S+) ([0-9.]+)", line)
        if m:
            bricks[m.group(1)] = float(m.group(2))
        m = re.match(r"CPU burst=(\d+) .* blocks=(\d+)", line)
        if m:
            seconds = int(m.group(1)) * int(m.group(2)) / 48000.0
    return bricks, seconds


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir")
    ap.add_argument("--out", required=True)
    ap.add_argument("--machine", default="")
    args = ap.parse_args()
    styles = {}
    for path in sorted(glob.glob(os.path.join(args.dir, "*.txt"))):
        style = os.path.basename(path).rsplit("-", 1)[0]
        bricks, seconds = read(path)
        if not bricks or seconds <= 0.0:
            raise SystemExit(f"{path}: no BRICK line (is ps_live built with -DPS_PROFILE=ON?)")
        if style not in styles or sum(bricks.values()) < sum(styles[style][0].values()):
            styles[style] = (bricks, seconds)
    if not styles:
        raise SystemExit(f"no measure in {args.dir}")
    pct = lambda v: f"{100.0 * v:.1f}".replace(".", ",")
    out = ["# Charge du moteur par style et par brique", "",
           f"Machine : {args.machine}. Blocs de 96 échantillons, moteur joué comme sur le téléphone (ps_live).",
           "**Sur un PC, c'est l'ordre des briques qui compte, pas les chiffres** : seule la tablette dit si un style coupe.",
           "« Charge » = temps de calcul sur temps de musique. Les parts sont des parts de cette charge.", "",
           "## Par style (du plus lourd au plus léger)", "",
           "| Style | Charge % | " + " | ".join(f"{n} %" for n, _ in FAMILIES) + " | Les trois briques les plus chères |",
           "|---|---:|" + "---:|" * len(FAMILIES) + "---|"]
    total_by_brick, worst = {}, {}
    order = sorted(styles, key=lambda s: -sum(styles[s][0].values()) / styles[s][1])
    for style in order:
        bricks, seconds = styles[style]
        whole = sum(bricks.values())
        shares = {n: 0.0 for n, _ in FAMILIES}
        for b, v in bricks.items():
            shares[family(b)] += v / whole
            total_by_brick[b] = total_by_brick.get(b, 0.0) + v / seconds
            if v / seconds > worst.get(b, (0.0, ""))[0]:
                worst[b] = (v / seconds, style)
        top = sorted(bricks, key=lambda b: -bricks[b])[:3]
        out.append(f"| {style} | {pct(whole / seconds)} | " + " | ".join(pct(shares[n]) for n, _ in FAMILIES) + " | "
                   + ", ".join(f"{b} {pct(bricks[b] / whole)}" for b in top) + " |")
    grand = sum(total_by_brick.values())
    out += ["", "## Les briques classées par coût (tous styles ensemble)", "",
            "| Rang | Brique | Famille | Part du total % | Charge moyenne % | Pire style | Charge dans ce style % |", "|---:|---|---|---:|---:|---|---:|"]
    for rank, b in enumerate(sorted(total_by_brick, key=lambda b: -total_by_brick[b]), 1):
        out.append(f"| {rank} | {b} | {family(b)} | {pct(total_by_brick[b] / grand)} | {pct(total_by_brick[b] / len(styles))} | {worst[b][1]} | {pct(worst[b][0])} |")
    fam = {n: 0.0 for n, _ in FAMILIES}
    for b, v in total_by_brick.items():
        fam[family(b)] += v
    out += ["", "## Les familles", "", "| Famille | Part du total % |", "|---|---:|"]
    out += [f"| {n} | {pct(fam[n] / grand)} |" for n in sorted(fam, key=lambda n: -fam[n])]
    out += ["", "Briques : `fx-*` = un effet d'insert (pistes et master), `mixer-strips` = tranches, envois et sommes,",
            "`master-chain` = chaîne du master (limiteur compris), `live-fx` = effets du Live, `engine-rest` = séquenceur et notes.", ""]
    with open(args.out, "w", encoding="utf-8") as f:
        f.write("\n".join(out))
    print("\n".join(out[: 10 + len(styles)]))


if __name__ == "__main__":
    main()
