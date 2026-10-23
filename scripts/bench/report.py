#!/usr/bin/env python3
"""Sums up the song bench (scripts/song-bench.sh) in a Markdown report, in French.

    report.py <json dir> --out report.md --json bench.json [--before old.json] [--ffmpeg ffmpeg.txt]
              [--hashes sha256.txt] [--renders dir] [--title "..."]

<json dir> holds one ps_measure JSON per style; --renders a folder of ps_render --report JSONs
(the loudness target's drive). bench.json gathers them (the file to keep, to compare a later run
against with --before). Standard library only.

Exit status 1 (after the report is written) when a hard limit is broken: a non-finite sample, a
true peak over the ceiling (TP_MAX_DB), an offset over DC_MAX, or a loudness off its style's target
by more than LUFS_TOLERANCE while the master chain had room left (a song the chain refused to push
further, having already taken MAX_LOSS_DB off it, is listed as capped, not failed: louder drums are
the next phase's work). The CI fails on them.
"""
import argparse
import json
import os
import statistics
import sys

# Loudness targets of a club master by style (docs/plan/son.md § 2.4), LUFS-I.
TARGETS = {
    **dict.fromkeys(["techno", "hard_techno", "acid_techno", "industrial_techno", "acidcore"], -7.0),
    "gabber": -6.0,
    **dict.fromkeys(["house", "tech_house", "afro_house", "uk_garage", "speed_garage", "disco", "amapiano", "acid_house"], -8.0),
    "deep_house": -9.0,
    **dict.fromkeys(["dnb", "jungle", "neurofunk", "dubstep", "riddim", "jump_up"], -7.0),
    **dict.fromkeys(["liquid_dnb", "trance", "synthwave", "retro_techno", "detroit", "minimal"], -8.0),
    **dict.fromkeys(["trap", "drill", "phonk", "grime", "jersey_club", "reggaeton", "afrobeats", "pop"], -8.5),
    "boom_bap": -10.0,
    "lofi": -12.0,
}

# Hard limits (son.md § 2.3): the report fails on them.
TP_MAX_DB = -1.0
DC_MAX = 1e-3
# The loudness target is met within this (LU).
LUFS_TOLERANCE = 1.0
# MasterChain::kMaxLossDb: past this much loudness taken off by the clipper and the limiter, the
# export's drive stops rising (a style under its target with this loss is capped, not broken).
MAX_LOSS_DB = 6.0

# (key, label, how to read it from a style's JSON, target text, decimals)
METRICS = [
    ("lufs_i", "LUFS-I (morceau)", lambda m: m["lufs_i"], "cible du genre ±1", 1),
    ("main_lufs", "LUFS du Drop / Refrain", lambda m: m["main_lufs"], "−8 à −6 en club", 1),
    ("true_peak", "True peak (dBTP)", lambda m: m["true_peak"], "≤ −1,0", 2),
    ("plr", "PLR (TP − LUFS-I)", lambda m: m["plr"], "6 à 10 en club", 1),
    ("lra", "LRA (LU)", lambda m: m["lra"], "3 à 9 en club", 1),
    ("main_crest", "Facteur de crête du Drop (dB)", lambda m: m["main_crest"], "≥ 6", 1),
    ("air", "Air > 6 kHz (dB rel.)", lambda m: m["bands"]["air"], "−20 à −16", 1),
    ("slope_db_oct", "Pente 0,1–8 kHz (dB/oct)", lambda m: m["slope_db_oct"], "−4,5 ±0,7", 2),
    ("side_mid", "Côté/centre (dB)", lambda m: m["side_mid"], "−15 à −9", 1),
    ("corr", "Corrélation L/R", lambda m: m["corr"], "0,3 à 0,85", 2),
    ("corr_below_120", "Corrélation < 120 Hz", lambda m: m["corr_below_120"], "≥ 0,95", 3),
    ("mono_loss_lu", "Perte en mono (LU)", lambda m: m["mono_loss_lu"], "≥ −1,5", 2),
    ("drop_minus_groove", "Drop − Groove, Couplet ou Montée (LU)", lambda m: m["drop_minus_groove"], "≥ +2", 1),
    ("drop_minus_break", "Drop − Break (LU)", lambda m: m["drop_minus_break"], "≥ +4", 1),
    ("dc", "Continu (moyenne absolue)", lambda m: m["dc"], "≤ 0,001", 4),
    ("drive_db", "Gain de la cible (dB)", lambda m: m.get("drive_db"), "–", 1),
    ("loss_db", "Perte dans la chaîne (LU)", lambda m: m.get("loss_db"), "≤ 6 (plafond)", 1),
]
BANDS = [("sub", "Sub < 60"), ("low", "Grave 60–250"), ("low_mid", "Bas-méd. 250–500"), ("mid", "Médium 0,5–2 k"),
         ("high_mid", "Haut-méd. 2–6 k"), ("air", "Air > 6 k")]


def fr(v, d=1, sign=False):
    """A number the French way (comma, true minus sign)."""
    if v is None:
        return "–"
    s = f"{v:+.{d}f}" if sign else f"{v:.{d}f}"
    return s.replace("-", "−").replace(".", ",")


def median(values):
    values = [v for v in values if v is not None]
    return statistics.median(values) if values else None


def load(path):
    out = {}
    for name in sorted(os.listdir(path)):
        if name.endswith(".json"):
            with open(os.path.join(path, name)) as f:
                m = json.load(f)
            out[m.get("style") or name[:-5]] = m
    return out


def lufs_gap(style, m):
    """LUFS-I minus the style's target (None without a target)."""
    t = TARGETS.get(style)
    return None if t is None or m.get("lufs_i") is None else m["lufs_i"] - t


def capped(style, m):
    """The chain refused to push this song further (loss at the cap) and it sits under its target."""
    gap, loss = lufs_gap(style, m), m.get("loss_db")
    return gap is not None and gap < 0 and loss is not None and loss >= MAX_LOSS_DB - 0.15


def criteria(styles):
    """(label, styles that meet it, styles that fail it, hard) for each acceptance criterion."""
    out = []
    def add(label, ok, hard=False):
        met = [s for s, m in styles.items() if ok(s, m)]
        out.append((label, met, [s for s in styles if s not in met], hard))
    add("True peak ≤ −1,0 dBTP", lambda s, m: m["true_peak"] is not None and m["true_peak"] <= TP_MAX_DB, hard=True)
    add("LUFS-I à ±1 LU de la cible du style", lambda s, m: lufs_gap(s, m) is not None and abs(lufs_gap(s, m)) <= LUFS_TOLERANCE)
    add("LUFS-I à ±1 LU de la cible, ou plafonné (perte ≥ 6 LU)",
        lambda s, m: lufs_gap(s, m) is not None and (abs(lufs_gap(s, m)) <= LUFS_TOLERANCE or capped(s, m)), hard=True)
    add("Continu ≤ 0,001", lambda s, m: m["dc"] <= DC_MAX, hard=True)
    add("Aucun NaN", lambda s, m: not m["non_finite"], hard=True)
    add("Aucun échantillon écrêté (≥ 0,999)", lambda s, m: not m["clipped"])
    add("PLR ≥ 6 dB", lambda s, m: m["plr"] is not None and m["plr"] >= 6.0)
    add("Facteur de crête du Drop ≥ 6 dB", lambda s, m: m["main_crest"] is not None and m["main_crest"] >= 6.0)
    return out


def summary_rows(styles):
    rows = {}
    for key, _, get, _, _ in METRICS:
        vals = [get(m) for m in styles.values()]
        vals = [v for v in vals if v is not None]
        rows[key] = (median(vals), min(vals) if vals else None, max(vals) if vals else None)
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("json_dir")
    ap.add_argument("--out", required=True)
    ap.add_argument("--json", required=True)
    ap.add_argument("--before")
    ap.add_argument("--ffmpeg")
    ap.add_argument("--hashes")
    ap.add_argument("--renders", help="folder of ps_render --report JSONs (<style>.json): the target's drive")
    ap.add_argument("--title", default="Banc de mesure du son")
    a = ap.parse_args()

    styles = load(a.json_dir)
    if not styles:
        sys.exit("no measure in " + a.json_dir)
    if a.renders and os.path.isdir(a.renders):
        for s, m in styles.items():
            path = os.path.join(a.renders, s + ".json")
            if os.path.exists(path):
                with open(path) as f:
                    r = json.load(f)
                m["drive_db"] = r.get("drive_db")
                m["export_lufs"] = r.get("lufs")
                m["export_true_peak"] = r.get("true_peak_db")
                music = r.get("music_lufs")
                # What the clipper and the limiter took off the driven music (LU).
                if music is not None and music > -200 and m["drive_db"] is not None and r.get("lufs") is not None:
                    m["loss_db"] = round(music + m["drive_db"] - r["lufs"], 2)
    with open(a.json, "w") as f:
        json.dump({"styles": styles}, f, indent=1, ensure_ascii=False, sort_keys=True)
        f.write("\n")
    before = None
    if a.before:
        with open(a.before) as f:
            before = json.load(f)["styles"]

    lines = [f"# {a.title}", ""]
    lines += [f"{len(styles)} morceaux de l'assistant (« Partir d'un style », sons intégrés, aucun pack), exportés par le "
              "code de l'appli (`SongBenchExport`), rejoués par le moteur sur Linux (`ps_render`) et mesurés par `ps_measure` "
              "(libebur128 pour la sonie ; true peak suréchantillonné ×4 comme ffmpeg). Le « Drop » est la section principale "
              "(Drop, sinon Refrain) : "
              "bandes, pente et stéréo y sont mesurées. Méthode : `docs/plan/son.md` § 2 ; relancer : `scripts/song-bench.sh`.", ""]

    rows = summary_rows(styles)
    lines += ["## Résumé (médiane sur les styles)", ""]
    if before:
        brows = summary_rows({k: v for k, v in before.items() if k in styles})
        lines += ["| Mesure | Avant | Après | Écart | Min – max (après) | Cible |", "|---|---|---|---|---|---|"]
        for key, label, _, target, d in METRICS:
            med, lo, hi = rows[key]
            bmed = brows[key][0]
            delta = None if med is None or bmed is None else med - bmed
            lines.append(f"| {label} | {fr(bmed, d)} | {fr(med, d)} | {fr(delta, d, True)} | {fr(lo, d)} – {fr(hi, d)} | {target} |")
    else:
        lines += ["| Mesure | Médiane | Min – max | Cible |", "|---|---|---|---|"]
        for key, label, _, target, d in METRICS:
            med, lo, hi = rows[key]
            lines.append(f"| {label} | {fr(med, d)} | {fr(lo, d)} – {fr(hi, d)} | {target} |")
    lines += ["", "## Critères (docs/plan/son.md § 2.3 et étape 2)", "", "| Critère | Morceaux conformes | Hors critère |", "|---|---|---|"]
    crit = criteria(styles)
    for label, met, failed, hard in crit:
        detail = ""
        if failed:
            if label.startswith("LUFS"):
                detail = ", ".join(f"{s} ({fr(lufs_gap(s, styles[s]), 1, True)}{', plafonné' if capped(s, styles[s]) else ''})" for s in failed)
            elif label.startswith("True peak"):
                detail = ", ".join(f"{s} ({fr(styles[s]['true_peak'], 2)})" for s in failed)
            elif label.startswith("Continu"):
                detail = ", ".join(f"{s} ({fr(styles[s]['dc'], 4)})" for s in failed)
            else:
                detail = ", ".join(failed)
        lines.append(f"| {label}{' (bloquant)' if hard else ''} | {len(met)} sur {len(styles)} | {detail or '–'} |")
    lines += ["", "Plafonné : la chaîne retirait déjà 6 LU (écrêteur et limiteur) au gain trouvé ; pousser plus écraserait sans "
              "gagner de sonie (`MasterChain::kMaxLossDb`). Ces morceaux attendent une batterie plus dense (étape 3).", ""]

    lines += ["## Par style", "",
              "Gain : ce que la cible de sonie ajoute avant l'écrêteur ; Perte : ce que l'écrêteur et le limiteur retirent (LU).", "",
              "| Style | LUFS-I | Cible | Gain | Perte | Drop | TP | PLR | LRA | Air | Pente | Côté/centre | Corr. | Corr. < 120 | Drop − avant | Drop − Break |",
              "|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|"]
    for s, m in sorted(styles.items()):
        def cell(key, d, get=None):
            v = (get or (lambda x: x[key]))(m)
            if before and s in before:
                try:
                    old = (get or (lambda x: x[key]))(before[s])
                except (KeyError, TypeError):
                    old = None
                if v is not None and old is not None and abs(v - old) >= 0.05:
                    return f"{fr(v, d)} ({fr(v - old, d, True)})"
            return fr(v, d)
        lines.append(f"| {s} | {cell('lufs_i', 1)} | {fr(TARGETS.get(s), 1)} | {fr(m.get('drive_db'), 1, True) if m.get('drive_db') is not None else '–'} | "
                     f"{fr(m.get('loss_db'), 1)} | {cell('main_lufs', 1)} | {cell('true_peak', 2)} | "
                     f"{cell('plr', 1)} | {cell('lra', 1)} | {cell('air', 1, lambda x: x['bands']['air'])} | {cell('slope_db_oct', 2)} | "
                     f"{cell('side_mid', 1)} | {cell('corr', 2)} | {cell('corr_below_120', 3)} | {cell('drop_minus_groove', 1)} | "
                     f"{cell('drop_minus_break', 1)} |")

    lines += ["", "## Bandes du Drop (dB relatifs à l'énergie totale)", "",
              "| Style | " + " | ".join(label for _, label in BANDS) + " |", "|---|" + "---|" * len(BANDS)]
    for s, m in sorted(styles.items()):
        lines.append(f"| {s} | " + " | ".join(fr(m["bands"][k], 1) for k, _ in BANDS) + " |")
    lines.append("| **médiane** | " + " | ".join(fr(median([m["bands"][k] for m in styles.values()]), 1) for k, _ in BANDS) + " |")

    lines += ["", "## Défauts", "", "| Style | NaN | Écrêtés | DC | Fin (dBFS, 500 ms) | Durée (s) |", "|---|---|---|---|---|---|"]
    for s, m in sorted(styles.items()):
        lines.append(f"| {s} | {m['non_finite']} | {m['clipped']} | {fr(m['dc'], 4)} | {fr(m['end_dbfs'], 1)} | {fr(m['seconds'], 1)} |")

    if a.ffmpeg and os.path.exists(a.ffmpeg):
        lines += ["", "## Contre-vérification ffmpeg (`ebur128=peak=true`)", "",
                  "| Style | LUFS-I banc | LUFS-I ffmpeg | TP banc | TP ffmpeg |", "|---|---|---|---|---|"]
        worst_i = worst_tp = 0.0
        with open(a.ffmpeg) as f:
            for row in f:
                parts = row.split()
                if len(parts) < 3 or parts[0] not in styles:
                    continue
                s, fi, ftp = parts[0], float(parts[1]), float(parts[2])
                m = styles[s]
                worst_i = max(worst_i, abs(m["lufs_i"] - fi))
                worst_tp = max(worst_tp, abs(m["true_peak"] - ftp))
                lines.append(f"| {s} | {fr(m['lufs_i'], 2)} | {fr(fi, 1)} | {fr(m['true_peak'], 2)} | {fr(ftp, 1)} |")
        # ffmpeg prints one decimal: ±0.05 of the difference is its rounding.
        ok = worst_i <= 0.1 + 0.05 and worst_tp <= 0.1 + 0.05
        lines += ["", f"Plus grand écart : {fr(worst_i, 2)} LU (sonie), {fr(worst_tp, 2)} dB (true peak) ; ffmpeg arrondit au dixième. "
                  + ("Concordant." if ok else "**Écart trop grand.**")]
        if not ok:
            print(f"ffmpeg cross-check failed: {worst_i:.2f} LU, {worst_tp:.2f} dB", file=sys.stderr)
            write(a.out, lines)
            sys.exit(1)

    if a.hashes and os.path.exists(a.hashes):
        lines += ["", "## Empreintes des rendus (SHA-256, WAV float 32 bits)", "",
                  "Deux passages du banc sur le même code donnent les mêmes octets.", "", "```"]
        with open(a.hashes) as f:
            lines += [row.rstrip() for row in f if row.strip()]
        lines.append("```")
    write(a.out, lines)
    # Hard limits (son.md § 2.3, phase 33's acceptance): no NaN, true peak <= -1 dBTP, DC <= 1e-3.
    broken = [f"{label}: {', '.join(failed)}" for label, _, failed, hard in crit if hard and failed]
    if broken:
        sys.exit("hard limit broken: " + "; ".join(broken))


def write(path, lines):
    with open(path, "w") as f:
        f.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
