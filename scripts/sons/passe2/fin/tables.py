#!/usr/bin/env python3
"""Final passe-2 tables after the blind review (one command, reads only CSV, never a sound).

  python3 -I scripts/sons/passe2/fin/tables.py

Base: the merged tables of docs/sons/passe2/tout/ as committed at BASE (git show).
Adds: the rows of the nested archives measured with scripts/sons/tout.py (unmodified),
stored anonymously in docs/sons/passe2/fin/imbriquees/ (nRAR_<type>.csv, pack p3 of RAR 11 and 12).
Adds the column bpm_gap_folded_pct (half/double tempo brought back) to drum_loop and melodic_loop.
Writes docs/sons/passe2/tout/*.csv, controle.csv, accuracy.csv and chiffres.md (this script is now
the only writer of chiffres.md; reunit.py stays as the first merge).

Target rule: a figure is "définitif" only with >= 20 sounds AND >= 2 distinct packs, else "pas de cible".
Outlier rule (drum hits), a value is dropped from the "sans aberrants" medians when:
  pitch_attack_hz or pitch_steady_hz < 20 Hz or > 5000 Hz;
  t20_ms / t60_ms longer than the file (dur_s);
  centroid_hz outside 20 Hz - 20 kHz.
"""
import collections
import csv
import io
import math
import os
import statistics
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
import reunit  # noqa: E402

ROOT = reunit.ROOT
BASE = "eefa5745d"
TOUT = "docs/sons/passe2/tout"
NESTED = os.path.join(ROOT, "docs/sons/passe2/fin/imbriquees")
NESTED_PACK = {11: "p3", 12: "p3"}
NO_PITCH = {"clap", "hat_closed", "hat_open", "shaker", "ride", "crash", "cymbal", "snap"}
FOLD_TOL = 2.0  # % tempo agreement after folding
f = reunit.to_float


def base_table(name):
    return reunit.read_csv_text(reunit.git("show", "%s:%s/%s.csv" % (BASE, TOUT, name)))


def folded_gap(tag, est):
    t, e = f(tag), f(est)
    if not t or not e or t <= 0 or e <= 0:
        return None
    k = round(math.log2(t / e))
    return 100 * abs(e * 2 ** k - t) / t


def status(n, packs):
    return "définitif" if n >= 20 and packs >= 2 else "pas de cible"


def pk(r):
    return "r%s%s" % (r["rar"], r["pack"])


def stat(rows, col, keep=None):
    vals = [(v, pk(r)) for r in rows for v in [keep(r, col) if keep else f(r.get(col))] if v is not None]
    packs = len({p for _, p in vals})
    return "%s (n %d, %d packs, %s)" % (reunit.fmt_med([v for v, _ in vals]), len(vals), packs,
                                        status(len(vals), packs)) if vals else "- (n 0)"


def outlier(r, col, v):
    if col in ("pitch_attack_hz", "pitch_steady_hz"):
        return v < 20 or v > 5000
    if col in ("t20_ms", "t60_ms"):
        d = f(r.get("dur_s"))
        return d is not None and v > d * 1000 + 1
    if col == "centroid_hz":
        return v < 20 or v > 20000
    return False


def keep_clean(r, col):
    v = reunit.trusted(r, col)
    return None if v is None or outlier(r, col, v) else v


def share(rows):
    c = collections.Counter("r" + r["rar"] for r in rows)
    return ", ".join("%s %d %%" % (k, round(100 * v / len(rows))) for k, v in c.most_common())


def passe1_types(n):
    ref = "origin/eq/mine-or-rar%d-tout" % n
    c = collections.Counter()
    for suffix in ("", "-tout", "-midi"):
        try:
            rows = reunit.read_csv_text(reunit.git("show", "%s:docs/sons/rar/rar%d%s.csv" % (ref, n, suffix)))[1]
        except Exception:
            continue
        for r in rows:
            c[(r.get("type") or ("midi" if suffix == "-midi" else "coup")).strip()] += 1
    return c


def extra(tables, ctrl, acc):
    L = []
    drums = tables["drum_oneshot"]
    fams = sorted({reunit.family(r["subtype"]) for r in drums})
    cols = [("t20_ms", "t20 ms"), ("t60_ms", "t60 ms"), ("pitch_attack_hz", "hauteur attaque Hz"),
            ("pitch_steady_hz", "hauteur stable Hz"), ("tonal_db", "tonal dB"), ("centroid_hz", "centroïde Hz"),
            ("hf_db", "hf dB"), ("transient_body_db", "transitoire/corps dB")]
    L += ["", "## Coups de batterie : médianes par famille, n réel par mesure (règles de confiance, puis sans aberrants)", "",
          "Chaque case : médiane [p25–p75] (n = lignes gardées par les règles de confiance, nombre de packs distincts, statut). "
          "« sans aberrants » applique en plus la règle écrite en tête de scripts/sons/passe2/fin/tables.py. "
          "Pas de hauteur pour %s : sons de bruit, la hauteur n'y veut rien dire." % ", ".join(sorted(NO_PITCH)), ""]
    for fa in fams:
        sub = [r for r in drums if reunit.family(r["subtype"]) == fa]
        L.append("### %s : %d lignes, %d packs" % (fa, len(sub), len({pk(r) for r in sub})))
        L.append("")
        L.append("| mesure | règles de confiance | sans aberrants (retirés) |")
        L.append("|---|---|---|")
        for c, label in cols:
            if c.startswith("pitch") and fa in NO_PITCH:
                L.append("| %s | retirée (bruit) | retirée |" % label)
                continue
            gone = sum(1 for r in sub for v in [reunit.trusted(r, c)] if v is not None and outlier(r, c, v))
            L.append("| %s | %s | %s (%d) |" % (label, stat(sub, c, reunit.trusted), stat(sub, c, keep_clean), gone))
        L.append("")

    L += ["## Grosse caisse par style (M1)", "",
          "Pas de « la grosse caisse » globale : une ligne par style. Parts = part de chaque RAR dans les lignes du style.", "",
          "| style | lignes | packs | t20 ms | t60 ms | hauteur stable Hz | durée s | parts par RAR |", "|---|---|---|---|---|---|---|---|"]
    kicks = [r for r in drums if reunit.family(r["subtype"]) == "kick"]
    for s, n in collections.Counter(r["style"] or "(aucun)" for r in kicks).most_common():
        sub = [r for r in kicks if (r["style"] or "(aucun)") == s]
        L.append("| %s | %d | %d | %s | %s | %s | %s | %s |" % (
            s, n, len({pk(r) for r in sub}), stat(sub, "t20_ms", keep_clean), stat(sub, "t60_ms", keep_clean),
            stat(sub, "pitch_steady_hz", keep_clean), stat(sub, "dur_s"), share(sub)))
    k8 = [r for r in drums if reunit.family(r["subtype"]) == "kick_808"]
    k8p = collections.Counter(pk(r) for r in k8)
    top, topn = k8p.most_common(1)[0]
    L += ["", "## kick_808 (M2)", "",
          "%d lignes dans %d pack(s) (%s), dont %d dans le seul pack %s ; les durées (t20, t60) ne viennent que de lui. "
          "kick_808 est donc « un seul pack » partout : aucune valeur kick_808 n'est une cible." % (
              len(k8), len(k8p), ", ".join("%s %d" % kv for kv in k8p.most_common()), topn, top)]

    hats = [r for r in drums if reunit.family(r["subtype"]) in ("hat_closed", "hat_open") and r.get("hat_measured")]
    contra = [r for r in hats if r["hat_measured"] not in r["subtype"]]
    L += ["", "## Charlestons : étiquette contre mesure (M4)", "",
          "%d charlestons mesurés ; %d (%d %%) ont une étiquette que la durée mesurée contredit "
          "(%d « fermé » mesurés ouverts, %d « ouvert » mesurés fermés)." % (
              len(hats), len(contra), round(100 * len(contra) / len(hats)),
              sum(1 for r in contra if r["subtype"].startswith("hat_closed")),
              sum(1 for r in contra if r["subtype"].startswith("hat_open"))), "",
          "| groupe | lignes | packs | durée s | t60 ms |", "|---|---|---|---|---|"]
    for label, sub in (("fermé (étiquette)", [r for r in hats if r["subtype"].startswith("hat_closed")]),
                       ("ouvert (étiquette)", [r for r in hats if r["subtype"].startswith("hat_open")]),
                       ("fermé (mesure)", [r for r in hats if r["hat_measured"] == "closed"]),
                       ("ouvert (mesure)", [r for r in hats if r["hat_measured"] == "open"])):
        L.append("| %s | %d | %d | %s | %s |" % (label, len(sub), len({pk(r) for r in sub}), stat(sub, "dur_s"),
                                                 stat(sub, "t60_ms", keep_clean)))

    ml = tables["melodic_loop"]
    L += ["", "## Tonalité estimée contre étiquette, par RAR (G1)", "",
          "La tonalité estimée n'est pas utilisable : moins d'une boucle sur quatre tombe juste. "
          "« même fondamentale » (root) compte la bonne note avec le mauvais mode : ce n'est pas un accord, il ne sauve rien.", "",
          "| RAR | boucles étiquetées | exacte | même fondamentale | relative | autre |", "|---|---|---|---|---|---|"]
    for n in sorted({int(r["rar"]) for r in ml}) + [None]:
        sub = [r for r in ml if r["key_tag"] and r["key_match"] and (n is None or int(r["rar"]) == n)]
        if not sub:
            continue
        c = collections.Counter(r["key_match"] for r in sub)
        p = lambda k: "%d (%d %%)" % (c[k], round(100 * c[k] / len(sub)))
        L.append("| %s | %d | %s | %s | %s | %s |" % (n or "total", len(sub), p("exact"), p("root"), p("relative"), p("other")))

    L += ["", "## Tempo replié (M6)", "",
          "Colonne bpm_gap_folded_pct : l'estimation ramenée au double ou à la moitié la plus proche de l'étiquette. "
          "Accord = écart ≤ %.0f %%." % FOLD_TOL, "",
          "| type | RAR | boucles avec étiquette et estimation | accord brut | accord replié |", "|---|---|---|---|---|"]
    for g in ("drum_loop", "melodic_loop"):
        rows = [r for r in tables[g] if r.get("bpm_gap_folded_pct") not in ("", None)]
        for n in sorted({int(r["rar"]) for r in rows}) + [None]:
            sub = [r for r in rows if n is None or int(r["rar"]) == n]
            raw = sum(1 for r in sub if f(r["bpm_gap_pct"]) is not None and abs(f(r["bpm_gap_pct"])) <= FOLD_TOL)
            fo = sum(1 for r in sub if f(r["bpm_gap_folded_pct"]) <= FOLD_TOL)
            L.append("| %s | %s | %d | %d (%d %%) | %d (%d %%) |" % (g, n or "total", len(sub), raw, round(100 * raw / len(sub)),
                                                                   fo, round(100 * fo / len(sub))))
    L += ["", "Tempo cible par style : lu sur l'étiquette (bpm_tag), jamais sur l'estimation.", "",
          "| style | boucles étiquetées | tempo étiqueté |", "|---|---|---|"]
    loops = [r for g in ("drum_loop", "melodic_loop") for r in tables[g] if f(r.get("bpm_tag"))]
    for s, n in collections.Counter(r["style"] or "(aucun)" for r in loops).most_common():
        sub = [r for r in loops if (r["style"] or "(aucun)") == s]
        L.append("| %s | %d | %s |" % (s, n, stat(sub, "bpm_tag")))

    sound = [r for g in reunit.GENRES if g != "midi" for r in tables.get(g, [])]
    allr = [r for g in reunit.GENRES for r in tables.get(g, [])]
    nostyle = sum(1 for r in allr if r.get("style") in ("", "unlabelled", None))
    L += ["", "## Lignes sans style (m1)", "",
          "%d lignes du tableau (sons, MIDI et lignes de kit, %d en tout) n'ont aucun style. "
          "Le style « posé » d'un pack (colonne rows_pack_styled de controle.csv) compte les lignes "
          "qui le reçoivent par le nom du dossier du pack ; ce n'est pas un style lu dans le son." % (nostyle, len(allr))]
    notes = [r for r in acc if r["measure"] == "note"]
    L += ["", "## Note des coups et des sons mélodiques (m2)", "",
          "Table de précision, mesure « note » : %d sons étiquetés, %d d'accord. "
          "Le chiffre de note des coups de batterie (%d étiquetés) est trop petit pour être vérifié : pas de cible." % (
              sum(int(r["tagged"] or 0) for r in notes), sum(int(r["agree"] or 0) for r in notes),
              sum(int(r["tagged"] or 0) for r in notes if r["type"] == "drum_oneshot"))]

    L += ["", "## Première passe contre deuxième passe, par type (RAR 7, 9, 11)", "",
          "Les lignes de kit ne sont comptées ni dans la première passe ni dans les « lignes 2e (rows) ».", ""]
    for n in (7, 9, 11):
        one = passe1_types(n)
        two = collections.Counter(g for g in reunit.GENRES for r in tables.get(g, []) if int(r["rar"]) == n)
        L.append("- RAR %d, 1re passe : %s." % (n, ", ".join("%s %d" % kv for kv in one.most_common())))
        L.append("- RAR %d, 2e passe : %s (dont kit %d lignes hors comptes)." % (
            n, ", ".join("%s %d" % kv for kv in two.most_common()), two.get("kit", 0)))
    return "\n".join(L) + "\n"


def main():
    tables, headers = {}, {}
    for g in reunit.GENRES:
        headers[g], tables[g] = base_table(g)
    ctrl_h, ctrl = base_table("controle")
    acc_h, acc = base_table("accuracy")
    p1_h, p1 = base_table("passe1_comptes")
    added = collections.Counter()
    for name in sorted(os.listdir(NESTED)):
        n, kind = int(name[1:name.index("_")]), name[name.index("_") + 1:-4]
        with open(os.path.join(NESTED, name), newline="") as fh:
            rows = list(csv.DictReader(fh))
        for r in rows:
            r["pack"] = NESTED_PACK[n]
            if kind == "accuracy":
                acc.append(r)
                continue
            r["cle"] = "r%d-%s-%s" % (n, r["pack"], r["file_id"])
            tables[kind].append(r)
            added[n] += 1
    for c in ctrl:
        n = int(c["rar"])
        if c["pack"] == NESTED_PACK.get(n) and added[n]:
            for k in ("files_audio", "rows", "rows_pack_styled"):
                c[k] = str(int(c[k] or 0) + added[n])
            c["nested_archives"] = "1 (lue : %d lignes)" % added[n]
    keys = [r["cle"] for g in reunit.GENRES for r in tables[g]]
    dup = [k for k, v in collections.Counter(keys).items() if v > 1]
    if dup:
        print("REFUSED: duplicate cle %s" % dup[:3])
        return 1
    for g in ("drum_loop", "melodic_loop"):
        if "bpm_gap_folded_pct" not in headers[g]:
            headers[g].append("bpm_gap_folded_pct")
        for r in tables[g]:
            v = folded_gap(r.get("bpm_tag"), r.get("bpm_est"))
            r["bpm_gap_folded_pct"] = "" if v is None else "%.2f" % v
    out = os.path.join(ROOT, TOUT)
    keyf = lambda r: (int(r.get("rar") or 0), r.get("pack") or "", r.get("cle") or "")
    for g in reunit.GENRES:
        reunit.write_table(os.path.join(out, g + ".csv"), headers[g], sorted(tables[g], key=keyf))
    reunit.write_table(os.path.join(out, "controle.csv"), ctrl_h, ctrl)
    reunit.write_table(os.path.join(out, "accuracy.csv"), acc_h, sorted(acc, key=keyf))
    for p in p1:
        p["total"] = int(p["total"])
    text = reunit.chiffres(tables, ctrl, p1)
    text = text.replace("(écrit par reunit.py, ne pas modifier à la main)",
                        "(écrit par scripts/sons/passe2/fin/tables.py, ne pas modifier à la main)")
    a = text.index("## Coups de batterie : médianes par famille")
    b = text.index("## Coups de batterie par RAR")
    text = text[:a] + text[b:] + extra(tables, ctrl, acc)
    with open(os.path.join(out, "chiffres.md"), "w") as fh:
        fh.write(text)
    print("ok: %d rows, nested added %s" % (len(keys), dict(added)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
