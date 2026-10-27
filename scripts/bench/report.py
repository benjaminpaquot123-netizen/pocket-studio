#!/usr/bin/env python3
"""Sums up the song bench (scripts/song-bench.sh) in a Markdown report, in French.

    report.py <json dir> --out report.md --json bench.json [--before old.json] [--ffmpeg ffmpeg.txt]
              [--hashes sha256.txt] [--renders dir] [--premaster dir] [--m4a dir] [--title "..."]

<json dir> holds one ps_measure JSON per style; --renders a folder of ps_render --report JSONs
(the loudness target's drive, its mode, the chain's loss); --premaster the ps_measure JSONs of the
same songs rendered before the master chain (ps_render --no-limiter: the arrangement's own section
contrast); --m4a those of the songs exported as the app's M4A (ceiling -2 dBTP), AAC-encoded at
256 kbit/s and decoded by ffmpeg. bench.json gathers them (the file to keep, to compare a later run
against with --before). Standard library only.

Exit status 1 (after the report is written) when a hard limit is broken: a non-finite sample, a
true peak over the ceiling (TP_MAX_DB; the decoded M4A's too when measured), an offset over DC_MAX,
a loudness off its style's target by more than LUFS_TOLERANCE while the master chain had room left
(a song the chain refused to push further, having already taken its cap off it, is listed as
capped, not failed), or (S5) a chain that took more than its cap. Since S5 (mix version 2, the
render's mode 1) the target is the Drop's: the main section's loudness is compared with it, and the
cap is MAX_LOSS_DROP_DB; the songs of before keep the whole song's loudness and MAX_LOSS_DB. Since
S5b, a Drop that lands less than DROP_OVER_MONTEE_MIN over the end of the Montée before it (its first
4 bars against the Montée's last 2, ps_measure's drop_minus_montee_end) fails too. The CI fails on them.

« Des basses qui tapent » (docs/hits/basses-qui-tapent.md § 8): the basses rendered alone (ps_measure
--bass, the song bench's bass pass) heard through a phone-speaker model, targets B1 to B12. Blocking
only the stable ones: what is under 30 Hz in the mix (B8, over -15 dB; -20 is the target), what the bass exciter adds to
the whole song (B12) and the bass heard through an average phone (B2, the floor of a transition
stage). The others (attack, its spread, the dip under the kick, the bass's place against the mix) are
listed: their reading depends on the line (notes too short for an attack window, a kick-less intro).
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

# S5 (docs/PLAN-SON-PRO.md): the Drop's loudness by style for the songs of mix version 2
# (LoudnessTargets.BY_STYLE_V2); the render's report gives the target it used, this is the fallback.
TARGETS_V2 = {
    **TARGETS,
    **dict.fromkeys(["techno", "acid_techno", "industrial_techno", "acidcore"], -7.5),
    **dict.fromkeys(["dnb", "jump_up", "jungle", "neurofunk", "dubstep", "riddim"], -7.5),
    "gabber": -7.0,
    **dict.fromkeys(["deep_house", "afro_house", "amapiano", "disco"], -9.0),
    "hard_techno": -6.0,
    "phonk": -7.0,
    **dict.fromkeys(["trap", "drill"], -8.5),
    **dict.fromkeys(["pop", "afrobeats", "reggaeton"], -9.0),
}

# Hard limits (son.md § 2.3): the report fails on them.
TP_MAX_DB = -1.0
DC_MAX = 1e-3
# The loudness target is met within this (LU).
LUFS_TOLERANCE = 1.0
# MasterChain::kMaxLossDb: past this much loudness taken off by the clipper and the limiter, the
# export's drive stops rising (a style under its target with this loss is capped, not broken).
MAX_LOSS_DB = 6.0
# MasterChain::kMaxLossDropDb: the cap in Drop mode (S5), over the loudest part.
MAX_LOSS_DROP_DB = 3.0
# The loss may read this far over the cap (the drive search's tolerance, the meters' rounding).
LOSS_SLACK = 0.2
# S5b: each Drop lands over the end of the Montée before it (its first 4 bars against the Montée's
# last 2, after the master): the A/B test heard Montées that ended louder than their Drop.
DROP_OVER_MONTEE_MIN = 1.0
# Audit final (lot son): the same landing through an average phone's speaker (ps_measure's
# drop_minus_montee_end_phone: a 4th-order high-pass at 300 Hz, then the K-weighting). A bass music
# Drop gained all its lift under 120 Hz, where a phone plays nothing: on the phone it did not fall
# (dubstep -1.0 dB, jump up +0.4). Blocking for the styles whose Drop is the point, listed for the rest.
DROP_OVER_MONTEE_PHONE_MIN = 1.5
DROP_PHONE_STYLES = {"dnb", "liquid_dnb", "neurofunk", "jungle", "jump_up", "dubstep", "riddim"}
# S5b: an Intro may be sparse, not far under the Drop (or the Refrain): listed, not blocking.
INTRO_UNDER_MAIN_MAX = 8.0
# S5b: a Montée rises at least this much over its last 4 bars (+3 in S2/S5): listed, not blocking (the
# measure needs 4 bars longer than its two 3 s windows: none at 170 BPM and more, and a Montée's rise
# swings 2 to 3 LU between styles of one family).
MONTEE_RISE_MIN = 2.0
# « Des basses qui tapent » (docs/hits/basses-qui-tapent.md § 8): B1/B2, the basses alone through a
# 200/300 Hz phone (loss against their own full band); B4, their 200-800 Hz against 30-100 Hz; B5/B6,
# their attack and its spread; B8, the mix's energy under 30 Hz; B9, the bass's dip under the kick;
# B12, what the exciter adds to the song before the master.
BASS_PHONE_200_MIN = -10.0
BASS_PHONE_300_MIN = -14.0
# B2's blocking floor during the transition (the target is BASS_PHONE_300_MIN).
BASS_PHONE_300_HARD = -16.0
BASS_HARMONICS_MIN = -14.0
BASS_ATTACK_MIN = 2.0
BASS_ATTACK_SPREAD_MAX = 0.5
INFRA_MAX = -20.0
# B8's blocking ceiling (the target is INFRA_MAX): the basses under 30 Hz are gone since « des basses qui
# tapent » (subs floored at 30 Hz, 28 Hz low cuts), but acidcore's hard kick alone leaves -17.4 dB there
# (before and after); the kick is another chantier. A bass falling under 30 Hz again (-3 to -13 dB before)
# still fails.
INFRA_HARD = -15.0
KICK_DIP_MAX = -3.0
EXCITER_MAX_LU = 1.0

# G0: the smallest share of bars that differ between two Drops (Refrains) of a song (DropVariation.MIN).
DROP_VARIATION_MIN = 0.25

# (key, label, how to read it from a style's JSON, target text, decimals)
METRICS = [
    ("lufs_i", "LUFS-I (morceau)", lambda m: m["lufs_i"], "cible du genre ±1 (avant S5)", 1),
    ("main_lufs", "LUFS du Drop / Refrain (sections entières)", lambda m: m["main_lufs"], "−8 à −6 en club", 1),
    ("loud_lufs", "LUFS de la partie la plus forte (court terme, 20 % du haut)", lambda m: m.get("loud_lufs"), "cible du style ±1 (S5)", 1),
    ("target_gap", "Écart à la cible (LU)", lambda m: m.get("target_gap"), "±1", 1),
    ("true_peak", "True peak (dBTP)", lambda m: m["true_peak"], "≤ −1,0", 2),
    ("plr", "PLR (TP − LUFS-I)", lambda m: m["plr"], "6 à 10 en club", 1),
    ("lra", "LRA (LU)", lambda m: m["lra"], "3 à 9 en club", 1),
    ("main_crest", "Facteur de crête du Drop (dB)", lambda m: m["main_crest"], "≥ 6", 1),
    ("air", "Air > 6 kHz (dB rel.)", lambda m: m["bands"]["air"], "−20 à −16", 1),
    ("slope_db_oct", "Pente 0,1–8 kHz (dB/oct)", lambda m: m["slope_db_oct"], "−4,5 ±0,7", 2),
    ("low", "Grave 60–250 Hz au Drop (dB rel.)", lambda m: m["bands"]["low"], "≥ −6 en bass music", 1),
    ("lead_centroid_var", "Mouvement du lead (centroïde, ±%)", lambda m: m.get("lead_centroid_var"), "≥ 15", 1),
    ("side_mid", "Côté/centre (dB)", lambda m: m["side_mid"], "−15 à −9", 1),
    ("corr", "Corrélation L/R", lambda m: m["corr"], "0,3 à 0,85", 2),
    ("corr_below_120", "Corrélation < 120 Hz", lambda m: m["corr_below_120"], "≥ 0,95", 3),
    ("mono_loss_lu", "Perte en mono (LU)", lambda m: m["mono_loss_lu"], "≥ −1,5", 2),
    ("drop_minus_groove", "Drop − Groove, Couplet ou Montée (LU)", lambda m: m["drop_minus_groove"], "≥ +2", 1),
    ("drop_minus_break", "Drop − Break (LU)", lambda m: m["drop_minus_break"], "≥ +4", 1),
    ("pre_drop_minus_groove", "Drop − Groove avant master (LU)", lambda m: m.get("pre_drop_minus_groove"), "≥ +2", 1),
    ("pre_drop_minus_break", "Drop − Break avant master (LU)", lambda m: m.get("pre_drop_minus_break"), "≥ +4", 1),
    ("m4a_true_peak_first", "True peak du M4A décodé, premier encodage (dBTP)", lambda m: m.get("m4a_true_peak_first"), "–", 2),
    ("m4a_true_peak", "True peak du M4A décodé, après la vérification de l'appli (dBTP)", lambda m: m.get("m4a_true_peak"), "≤ −1,0", 2),
    ("montee_rise", "Montée : sa montée sur ses 4 dernières mesures (LU)", lambda m: median(m.get("montee_rise") or []), "≥ +2", 1),
    ("pre_montee_rise", "Montée avant master (LU)", lambda m: median(m.get("pre_montee_rise") or []), "≥ +2", 1),
    ("drop_minus_montee_end", "Drop − fin de Montée (LU, le plus petit du morceau)", lambda m: lowest(m.get("drop_minus_montee_end")), "≥ +1 (+1,5 visé)", 1),
    ("drop_minus_montee_end_phone", "Drop − fin de Montée au téléphone (passe-haut 300 Hz, pondéré K ; dB, le plus petit)", lambda m: lowest(m.get("drop_minus_montee_end_phone")), "≥ +1,5 (bloquant en bass music)", 1),
    ("pre_drop_minus_montee_end", "Drop − fin de Montée avant master (LU)", lambda m: lowest(m.get("pre_drop_minus_montee_end")), "–", 1),
    ("intro_minus_main", "Intro − Drop / Refrain (LU)", lambda m: m.get("intro_minus_main"), "≥ −8", 1),
    # G0: how much the Drops (Refrains) differ, read from the model (META drop_variation of the .pscmd).
    ("drop_variation", "Variation entre Drops (part des mesures qui diffèrent, paire la plus proche)", lambda m: m.get("drop_variation"), "≥ 0,25", 2),
    ("dc", "Continu (moyenne absolue)", lambda m: m["dc"], "≤ 0,001", 4),
    ("drive_db", "Gain de la cible (dB)", lambda m: m.get("drive_db"), "–", 1),
    ("loss_db", "Perte dans la chaîne (LU)", lambda m: m.get("loss_db"), "≤ 3 (S5, au Drop ; 6 avant)", 1),
    ("bass_phone_200", "B1 Basse au téléphone 200 Hz (dB)", lambda m: m["bass"]["phone_200"], "≥ −10", 1),
    ("bass_phone_300", "B2 Basse au téléphone 300 Hz (dB)", lambda m: m["bass"]["phone_300"], "≥ −14", 1),
    ("bass_place_300", "B3 Basse dans le mix au téléphone (LU)", lambda m: m["bass"]["place_300"], "≥ −10 hip-hop et bass music, ≥ −14 club", 1),
    ("bass_harmonics", "B4 Harmoniques 200–800 / 30–100 Hz (dB)", lambda m: m["bass"]["harmonics"], "≥ −14", 1),
    ("bass_attack", "B5 Attaque de la basse (dB)", lambda m: m["bass"]["attack"], "≥ +2 (808, pluck : +4)", 1),
    ("bass_attack_spread", "B6 Régularité de l'attaque (dB)", lambda m: m["bass"]["attack_spread"], "≤ 0,5", 2),
    ("sub_40_60", "B7 Sub 40–60 Hz du mix (dB rel.)", lambda m: m.get("sub_40_60"), "−6 à −3 (le garder)", 1),
    ("infra_below_30", "B8 Infra < 30 Hz du mix (dB rel.)", lambda m: m.get("infra_below_30"), "≤ −20", 1),
    ("bass_kick_dip", "B9 Creux de la basse sous le kick (dB)", lambda m: m["bass"]["kick_dip"], "≤ −6 club, ≤ −3 ailleurs", 1),
    ("exciter_lu", "B12 Sonie ajoutée par l'exciteur (LU)", lambda m: m.get("exciter_lu"), "≤ +1", 2),
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


def lowest(values):
    """The smallest of a song's values (its worst Drop), None without any."""
    values = [v for v in (values or []) if v is not None]
    return min(values) if values else None


def load(path):
    out = {}
    for name in sorted(os.listdir(path)):
        if name.endswith(".json"):
            with open(os.path.join(path, name)) as f:
                m = json.load(f)
            out[m.get("style") or name[:-5]] = m
    return out


def drop_mode(m):
    """The song's target aims at its Drop (S5, mix version 2: the render's mode 1)."""
    return m.get("mode") == 1


def target_of(style, m):
    """The LUFS the song's target aimed at: the render's, else the style's table."""
    t = m.get("target_lufs")
    if t is not None and t < 0:
        return t
    return (TARGETS_V2 if drop_mode(m) else TARGETS).get(style)


def lufs_gap(style, m):
    """The measured loudness minus the target (None without one): the Drop's in Drop mode, else the whole song's."""
    t = target_of(style, m)
    # Drop mode: the loudest part's short-term loudness as ps_measure reads it on the file (the same
    # measure as the engine's LoudPart, read independently), the main section's when missing.
    value = (m.get("loud_lufs") if m.get("loud_lufs") is not None else m.get("main_lufs")) if drop_mode(m) else m.get("lufs_i")
    return None if t is None or value is None else value - t


def max_loss(m):
    return MAX_LOSS_DROP_DB if drop_mode(m) else MAX_LOSS_DB


def capped(style, m):
    """The chain refused to push this song further (loss at the cap) and it sits under its target."""
    gap, loss = lufs_gap(style, m), m.get("loss_db")
    return gap is not None and gap < 0 and loss is not None and loss >= max_loss(m) - 0.15


def criteria(styles):
    """(label, styles that meet it, styles that fail it, hard) for each acceptance criterion."""
    out = []
    def add(label, ok, hard=False):
        met = [s for s, m in styles.items() if ok(s, m)]
        out.append((label, met, [s for s in styles if s not in met], hard))
    add("True peak ≤ −1,0 dBTP", lambda s, m: m["true_peak"] is not None and m["true_peak"] <= TP_MAX_DB, hard=True)
    if any(m.get("m4a_true_peak") is not None for m in styles.values()):
        add("True peak du M4A décodé ≤ −1,0 dBTP",
            lambda s, m: m.get("m4a_true_peak") is None or m["m4a_true_peak"] <= TP_MAX_DB, hard=True)
    add("Sonie à ±1 LU de la cible du style (le Drop depuis S5), sans plafonnement",
        lambda s, m: lufs_gap(s, m) is not None and abs(lufs_gap(s, m)) <= LUFS_TOLERANCE)
    add("Sonie à ±1 LU de la cible, ou plafonnée (perte au plafond : 3 LU au Drop, 6 avant S5)",
        lambda s, m: lufs_gap(s, m) is not None and (abs(lufs_gap(s, m)) <= LUFS_TOLERANCE or capped(s, m)), hard=True)
    add("Perte dans la chaîne sous son plafond",
        lambda s, m: m.get("loss_db") is None or m["loss_db"] <= max_loss(m) + LOSS_SLACK, hard=True)
    add("Continu ≤ 0,001", lambda s, m: m["dc"] <= DC_MAX, hard=True)
    add("Aucun NaN", lambda s, m: not m["non_finite"], hard=True)
    add("Aucun échantillon écrêté (≥ 0,999)", lambda s, m: not m["clipped"])
    add("PLR ≥ 6 dB", lambda s, m: m["plr"] is not None and m["plr"] >= 6.0)
    add("Facteur de crête du Drop ≥ 6 dB", lambda s, m: m["main_crest"] is not None and m["main_crest"] >= 6.0)
    # S2 / S5: the sections' contrast kept after the master (styles that have the sections).
    add("Drop − Break ≥ +4 LU (exporté ; sans Break : –)", lambda s, m: m.get("drop_minus_break") is None or m["drop_minus_break"] >= 4.0)
    add("Drop − Groove, Couplet ou Montée ≥ +2 LU (exporté)", lambda s, m: m.get("drop_minus_groove") is None or m["drop_minus_groove"] >= 2.0)
    add(f"Chaque Montée monte d'au moins +{fr(MONTEE_RISE_MIN, 0)} LU sur ses 4 dernières mesures (exporté ; sans Montée : –)",
        lambda s, m: all(r >= MONTEE_RISE_MIN for r in (m.get("montee_rise") or [])))
    # S5b: the Drop lands louder than the Montée's end (its first 4 bars against the Montée's last 2).
    add(f"Chaque Drop au-dessus de la fin de sa Montée : Drop − fin de Montée ≥ +{fr(DROP_OVER_MONTEE_MIN, 0)} LU (exporté ; sans Montée : –)",
        lambda s, m: all(d >= DROP_OVER_MONTEE_MIN for d in (m.get("drop_minus_montee_end") or [])), hard=True)
    add(f"Au téléphone, chaque Drop de bass music au-dessus de la fin de sa Montée : ≥ +{fr(DROP_OVER_MONTEE_PHONE_MIN)} dB après passe-haut 300 Hz (pondéré K)",
        lambda s, m: s not in DROP_PHONE_STYLES or all(d >= DROP_OVER_MONTEE_PHONE_MIN for d in (m.get("drop_minus_montee_end_phone") or [])), hard=True)
    add(f"Au téléphone, chaque Drop au-dessus de la fin de sa Montée : ≥ +{fr(DROP_OVER_MONTEE_PHONE_MIN)} dB (tous les styles, listé)",
        lambda s, m: all(d >= DROP_OVER_MONTEE_PHONE_MIN for d in (m.get("drop_minus_montee_end_phone") or [])))
    add(f"Intro à moins de {fr(INTRO_UNDER_MAIN_MAX, 0)} LU sous le Drop ou le Refrain (exporté)",
        lambda s, m: m.get("intro_minus_main") is None or m["intro_minus_main"] >= -INTRO_UNDER_MAIN_MAX)
    # G0: no two Drops alike (listed, not blocking; measured on the song's model, not on its sound).
    if any(m.get("drop_variation") is not None for m in styles.values()):
        add(f"Variation entre Drops ≥ {fr(DROP_VARIATION_MIN, 2)} (modèle : aucune paire de Drops identique)",
            lambda s, m: m.get("drop_variation") is None or m["drop_variation"] >= DROP_VARIATION_MIN)
    # « Des basses qui tapent »: only the songs whose basses the bench rendered alone (a style without bass: –).
    def bass(m, key):
        return (m.get("bass") or {}).get(key)
    if any(m.get("bass") for m in styles.values()):
        add(f"B2 Basse au téléphone (300 Hz) ≥ {fr(BASS_PHONE_300_HARD, 0)} dB (cible {fr(BASS_PHONE_300_MIN, 0)})",
            lambda s, m: bass(m, "phone_300") is None or bass(m, "phone_300") >= BASS_PHONE_300_HARD, hard=True)
        add(f"B2 Basse au téléphone (300 Hz) ≥ {fr(BASS_PHONE_300_MIN, 0)} dB (la cible)",
            lambda s, m: bass(m, "phone_300") is None or bass(m, "phone_300") >= BASS_PHONE_300_MIN)
        add(f"B1 Basse au téléphone (200 Hz) ≥ {fr(BASS_PHONE_200_MIN, 0)} dB",
            lambda s, m: bass(m, "phone_200") is None or bass(m, "phone_200") >= BASS_PHONE_200_MIN)
        add(f"B4 Harmoniques de la basse ≥ {fr(BASS_HARMONICS_MIN, 0)} dB",
            lambda s, m: bass(m, "harmonics") is None or bass(m, "harmonics") >= BASS_HARMONICS_MIN)
        add(f"B5 Attaque de la basse ≥ +{fr(BASS_ATTACK_MIN, 0)} dB (notes assez longues pour la lire)",
            lambda s, m: bass(m, "attack") is None or bass(m, "attack") >= BASS_ATTACK_MIN)
        add(f"B6 Écart de l'attaque ≤ {fr(BASS_ATTACK_SPREAD_MAX, 1)} dB",
            lambda s, m: bass(m, "attack_spread") is None or bass(m, "attack_spread") <= BASS_ATTACK_SPREAD_MAX)
        add(f"B9 La basse s'efface sous le kick (≤ {fr(KICK_DIP_MAX, 0)} dB)",
            lambda s, m: bass(m, "kick_dip") is None or bass(m, "kick_dip") <= KICK_DIP_MAX)
    if any(m.get("infra_below_30") is not None for m in styles.values()):
        add(f"B8 Rien sous 30 Hz : infra ≤ {fr(INFRA_HARD, 0)} dB du Drop (cible {fr(INFRA_MAX, 0)})",
            lambda s, m: m.get("infra_below_30") is None or m["infra_below_30"] <= INFRA_HARD, hard=True)
        add(f"B8 Rien sous 30 Hz : infra ≤ {fr(INFRA_MAX, 0)} dB du Drop (la cible)",
            lambda s, m: m.get("infra_below_30") is None or m["infra_below_30"] <= INFRA_MAX)
    if any(m.get("exciter_lu") is not None for m in styles.values()):
        add(f"B12 L'exciteur ajoute au plus +{fr(EXCITER_MAX_LU, 0)} LU au morceau (avant master)",
            lambda s, m: m.get("exciter_lu") is None or m["exciter_lu"] <= EXCITER_MAX_LU, hard=True)
    return out


def summary_rows(styles):
    rows = {}
    for key, _, get, _, _ in METRICS:
        vals = []
        for m in styles.values():
            try:
                vals.append(get(m))
            except (KeyError, TypeError):
                vals.append(None)
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
    ap.add_argument("--premaster", help="folder of ps_measure JSONs of the songs rendered before the master chain")
    ap.add_argument("--m4a", help="folder of ps_measure JSONs of the songs as decoded M4A (ceiling -2 dBTP, AAC 256 kbit/s)")
    ap.add_argument("--cmds", help="folder of the recorded .pscmd files: their META lines (the model's drop_variation, G0)")
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
                m["mode"] = r.get("mode", 0)
                if r.get("target_lufs"):
                    m["target_lufs"] = r["target_lufs"]
                if r.get("loud_lufs") is not None and r["loud_lufs"] > -200:
                    m["render_loud_lufs"] = r["loud_lufs"]
                music = r.get("music_lufs")
                # What the clipper and the limiter took off the driven music (LU): the renderer's own
                # figure since S5 (over the loud part in Drop mode), else from the integrated loudness.
                if r.get("loss_db") is not None and m["drive_db"]:
                    m["loss_db"] = round(r["loss_db"], 2)
                elif music is not None and music > -200 and m["drive_db"] is not None and r.get("lufs") is not None:
                    m["loss_db"] = round(music + m["drive_db"] - r["lufs"], 2)
    for s, m in styles.items():
        m["target_gap"] = None if lufs_gap(s, m) is None else round(lufs_gap(s, m), 2)
    if a.cmds and os.path.isdir(a.cmds):
        for s, m in styles.items():
            meta = read_meta(os.path.join(a.cmds, s + ".pscmd"))
            if "drop_variation" in meta:
                try:
                    m["drop_variation"] = round(float(meta["drop_variation"]), 3)
                except ValueError:
                    pass
    if a.premaster and os.path.isdir(a.premaster):
        for s, pre in load(a.premaster).items():
            if s in styles:
                for key in ("drop_minus_groove", "drop_minus_break", "lra", "main_lufs", "lufs_i", "montee_rise", "drop_minus_montee_end", "intro_minus_main"):
                    styles[s]["pre_" + key] = pre.get(key)
    if a.m4a and os.path.isdir(a.m4a):
        for s, dec in load(a.m4a).items():
            if s in styles:
                styles[s]["m4a_true_peak"] = dec.get("true_peak")
                styles[s]["m4a_lufs"] = dec.get("lufs_i")
            elif s.endswith(".first") and s[:-6] in styles:
                # The first encode, before the app's check encodes it again lower.
                styles[s[:-6]]["m4a_true_peak_first"] = dec.get("true_peak")
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
            if label.startswith("Sonie"):
                detail = ", ".join(f"{s} ({fr(lufs_gap(s, styles[s]), 1, True)}{', plafonné' if capped(s, styles[s]) else ''})" for s in failed)
            elif label.startswith("Perte"):
                detail = ", ".join(f"{s} ({fr(styles[s].get('loss_db'), 1)})" for s in failed)
            elif label.startswith("Drop − Break"):
                detail = ", ".join(f"{s} ({fr(styles[s].get('drop_minus_break'), 1, True)})" for s in failed)
            elif label.startswith("Drop − Groove"):
                detail = ", ".join(f"{s} ({fr(styles[s].get('drop_minus_groove'), 1, True)})" for s in failed)
            elif label.startswith("Chaque Montée"):
                detail = ", ".join(f"{s} ({' / '.join(fr(r, 1, True) for r in styles[s].get('montee_rise') or [])})" for s in failed)
            elif label.startswith("Chaque Drop"):
                detail = ", ".join(f"{s} ({' / '.join(fr(r, 1, True) for r in styles[s].get('drop_minus_montee_end') or [])})" for s in failed)
            elif label.startswith("Au téléphone"):
                detail = ", ".join(f"{s} ({' / '.join(fr(r, 1, True) for r in styles[s].get('drop_minus_montee_end_phone') or [])})" for s in failed)
            elif label.startswith("Variation entre Drops"):
                detail = ", ".join(f"{s} ({fr(styles[s].get('drop_variation'), 2)})" for s in failed)
            elif label.startswith("Intro"):
                detail = ", ".join(f"{s} ({fr(styles[s].get('intro_minus_main'), 1, True)})" for s in failed)
            elif label.startswith("True peak du M4A"):
                detail = ", ".join(f"{s} ({fr(styles[s].get('m4a_true_peak'), 2)})" for s in failed)
            elif label.startswith("True peak"):
                detail = ", ".join(f"{s} ({fr(styles[s]['true_peak'], 2)})" for s in failed)
            elif label.startswith("B2") or label.startswith("B1") or label.startswith("B4") or label.startswith("B5") or label.startswith("B6") or label.startswith("B9"):
                key = {"B1": "phone_200", "B2": "phone_300", "B4": "harmonics", "B5": "attack", "B6": "attack_spread", "B9": "kick_dip"}[label[:2]]
                detail = ", ".join(f"{s} ({fr((styles[s].get('bass') or {}).get(key), 1)})" for s in failed)
            elif label.startswith("B8"):
                detail = ", ".join(f"{s} ({fr(styles[s].get('infra_below_30'), 1)})" for s in failed)
            elif label.startswith("B12"):
                detail = ", ".join(f"{s} ({fr(styles[s].get('exciter_lu'), 2, True)})" for s in failed)
            elif label.startswith("Continu"):
                detail = ", ".join(f"{s} ({fr(styles[s]['dc'], 4)})" for s in failed)
            else:
                detail = ", ".join(failed)
        lines.append(f"| {label}{' (bloquant)' if hard else ''} | {len(met)} sur {len(styles)} | {detail or '–'} |")
    lines += ["", "Plafonné : la chaîne retirait déjà son plafond (écrêteur et limiteur) au gain trouvé : 3 LU sur la partie la plus "
              "forte depuis S5 (`MasterChain::kMaxLossDropDb`, le Drop), 6 LU sur le morceau avant (`kMaxLossDb`). Pousser plus "
              "écraserait sans gagner de sonie : c'est aux sons et au mix d'être plus denses.", ""]

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
        lines.append(f"| {s} | {cell('lufs_i', 1)} | {fr(target_of(s, m), 1)}{' (Drop)' if drop_mode(m) else ''} | {fr(m.get('drive_db'), 1, True) if m.get('drive_db') is not None else '–'} | "
                     f"{fr(m.get('loss_db'), 1)} | {cell('main_lufs', 1)} | {cell('true_peak', 2)} | "
                     f"{cell('plr', 1)} | {cell('lra', 1)} | {cell('air', 1, lambda x: x['bands']['air'])} | {cell('slope_db_oct', 2)} | "
                     f"{cell('side_mid', 1)} | {cell('corr', 2)} | {cell('corr_below_120', 3)} | {cell('drop_minus_groove', 1)} | "
                     f"{cell('drop_minus_break', 1)} |")

    # S5b: how each Drop lands after its Montée, and how far under it the Intro sits.
    lines += ["", "## Montées, Drops et Intro (S5b)", "",
              "Fin de Montée : la sonie de ses 2 dernières mesures (sans son temps de silence) ; Drop : celle de ses 4 premières "
              "mesures (un faux drop entre les deux est sauté) ; une valeur par Drop, exporté (avant master entre parenthèses). "
              f"Bloquant : Drop − fin de Montée ≥ +{fr(DROP_OVER_MONTEE_MIN, 0)} LU. Intro − Drop : l'Intro du début entière contre "
              "les sections Drop (ou Refrain), à titre indicatif. Au téléphone : le même écart après un passe-haut de 300 Hz "
              f"(4e ordre) et la pondération K (dB), bloquant en bass music à +{fr(DROP_OVER_MONTEE_PHONE_MIN)}.", "",
              "| Style | Fin de Montée (LUFS) | Début du Drop (LUFS) | Drop − fin de Montée | Au téléphone | Intro − Drop |", "|---|---|---|---|---|---|"]
    for s, m in sorted(styles.items()):
        def seq(key, d=1, sign=False):
            v = m.get(key) or []
            return " / ".join(fr(x, d, sign) for x in v) if v else "–"
        pre = m.get("pre_drop_minus_montee_end") or []
        landing = seq("drop_minus_montee_end", 1, True) + (f" ({' / '.join(fr(x, 1, True) for x in pre)})" if pre else "")
        intro = m.get("intro_minus_main")
        was = ""
        if before and s in before and intro is not None and before[s].get("intro_minus_main") is not None:
            delta = intro - before[s]["intro_minus_main"]
            was = f" ({fr(delta, 1, True)})" if abs(delta) >= 0.05 else ""
        lines.append(f"| {s} | {seq('montee_end_lufs')} | {seq('drop_start_lufs')} | {landing} | {seq('drop_minus_montee_end_phone', 1, True)} | {fr(intro, 1, True)}{was} |")

    lines += ["", "## Bandes du Drop (dB relatifs à l'énergie totale)", "",
              "| Style | " + " | ".join(label for _, label in BANDS) + " |", "|---|" + "---|" * len(BANDS)]
    for s, m in sorted(styles.items()):
        lines.append(f"| {s} | " + " | ".join(fr(m["bands"][k], 1) for k, _ in BANDS) + " |")
    lines.append("| **médiane** | " + " | ".join(fr(median([m["bands"][k] for m in styles.values()]), 1) for k, _ in BANDS) + " |")

    # S4: width and movement, each style against the targets of docs/PLAN-SON-PRO.md.
    lines += ["", "## Largeur et mouvement (S4)", "",
              "Côté/centre −15 à −9 dB, corrélation 0,4 à 0,85 (≥ 0,95 sous 120 Hz), lead dont le centroïde bouge d'au moins ±15 % "
              "d'une mesure à l'autre (sa piste seule, quand l'enregistrement la nomme), grave 60–250 Hz ≥ −6 dB en bass music.", "",
              "| Style | Côté/centre | Corr. | Corr. < 120 | Lead ±% | Grave 60–250 |", "|---|---|---|---|---|---|"]
    for s, m in sorted(styles.items()):
        def was(key, get):
            if not before or s not in before:
                return ""
            try:
                old = get(before[s])
            except (KeyError, TypeError):
                return ""
            v = get(m)
            return f" ({fr(v - old, 1, True)})" if v is not None and old is not None and abs(v - old) >= 0.05 else ""
        lines.append(f"| {s} | {fr(m['side_mid'], 1)}{was('side_mid', lambda x: x['side_mid'])} | {fr(m['corr'], 2)} | "
                     f"{fr(m['corr_below_120'], 3)} | {fr(m.get('lead_centroid_var'), 1)} | "
                     f"{fr(m['bands']['low'], 1)}{was('low', lambda x: x['bands']['low'])} |")

    # « Des basses qui tapent »: the basses alone through the phone model, style by style.
    if any(m.get("bass") for m in styles.values()):
        lines += ["", "## Basse au téléphone (docs/hits/basses-qui-tapent.md § 8)", "",
                  "Les basses du morceau (pistes de basse, Sub, Sub drop, pad de la 808 : `META bass`) rendues seules avant la chaîne "
                  "master, au Drop (ou au Refrain), à travers un « téléphone » : passe-haut de Butterworth d'ordre 4 à 200 Hz (bon "
                  "téléphone, B1) ou 300 Hz (téléphone moyen, B2), puis la pondération K. Perte de sonie contre la basse en pleine "
                  "bande : plus proche de 0, mieux elle passe. B3 : la basse filtrée à 300 Hz contre le morceau filtré pareil. B4 : "
                  "énergie 200–800 Hz contre 30–100 Hz. B5 : RMS 5 ms le plus fort des 40 premières ms d'une note contre la médiane "
                  "80–250 ms (notes de 250 ms au moins). B7, B8 : le morceau exporté. B9 : RMS de la basse 0–50 ms après chaque kick "
                  "contre 100–200 ms après. B12 : le morceau avant master avec l'exciteur contre sans.", "",
                  "| Style | B1 tél. 200 | B2 tél. 300 | B3 place | B4 harm. | B5 attaque (notes) | B6 écart | B7 sub | B8 infra | B9 kick | B12 exciteur |",
                  "|---|---|---|---|---|---|---|---|---|---|---|"]
        for s, m in sorted(styles.items()):
            b = m.get("bass") or {}
            def bv(key, d=1, sign=False):
                v = b.get(key)
                old = ((before or {}).get(s, {}).get("bass") or {}).get(key) if before else None
                if v is not None and old is not None and abs(v - old) >= 0.05:
                    return f"{fr(v, d, sign)} ({fr(v - old, d, True)})"
                return fr(v, d, sign)
            def mv(key, d=1, sign=False):
                v = m.get(key)
                old = (before or {}).get(s, {}).get(key) if before else None
                if v is not None and old is not None and abs(v - old) >= 0.05:
                    return f"{fr(v, d, sign)} ({fr(v - old, d, True)})"
                return fr(v, d, sign)
            lines.append(f"| {s} | {bv('phone_200')} | {bv('phone_300')} | {bv('place_300')} | {bv('harmonics')} | "
                         f"{bv('attack', 1, True)} ({b.get('notes', 0)}) | {bv('attack_spread', 2)} | {mv('sub_40_60')} | {mv('infra_below_30')} | "
                         f"{bv('kick_dip', 1, True)} | {mv('exciter_lu', 2, True)} |")

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


def read_meta(path, limit=200):
    """The META lines at the head of a .pscmd recording (key → value), {} when there is none."""
    out = {}
    if not os.path.exists(path):
        return out
    with open(path, errors="replace") as f:
        for i, row in enumerate(f):
            if i >= limit:
                break
            if row.startswith("META "):
                parts = row.rstrip("\n").split(" ", 2)
                if len(parts) == 3:
                    out[parts[1]] = parts[2]
    return out


def write(path, lines):
    with open(path, "w") as f:
        f.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
