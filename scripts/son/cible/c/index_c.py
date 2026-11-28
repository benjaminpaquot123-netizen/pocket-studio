"""Write the zone C index (docs/sons/cible/c/index.json) and copy the MP3s
next to it, from OUT/export.jsonl. Each entry: fichier, style, famille,
variante, outil, source, licence, distance to the target cell (sigma), peak dBFS.
No pack sound is used or derived: synths are Surge XT factory patches,
effects are synthesised from noise and oscillators.

  python -I index_c.py OUT
"""
import json
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(HERE))))
DEST = os.path.join(REPO, "docs", "sons", "cible", "c")
FAM_FR = {"stab": "stab", "pluck": "pluck", "nappe": "nappe", "accords": "accords de synthé",
          "boucle": "boucle mélodique", "arpege": "arpège", "ambiance": "nappe d'ambiance",
          "effet": "effet"}
KIND_FR = {"montant": "montant (riser)", "descendant": "descendant (downlifter)", "impact": "impact",
           "balayage": "balayage (sweep)"}


def main(out):
    rows = {}
    for l in open(os.path.join(out, "export.jsonl")):
        if l.strip():
            r = json.loads(l)
            rows[r["fichier"]] = r
    os.makedirs(os.path.join(DEST, "mp3"), exist_ok=True)
    entries = []
    for name in sorted(rows):
        r = rows[name]
        fam = r["family"]
        e = {"fichier": "mp3/" + name, "style": r["style"], "famille": FAM_FR[fam]}
        if fam == "effet":
            e["type"] = KIND_FR[r["kind"]]
            e["outil"] = "synthèse bruit et oscillateurs (numpy) + pedalboard, réglée par pycma"
            e["source"] = "synthèse maison, aucun échantillon"
            e["licence"] = "CC0 (son fabriqué ici)"
        else:
            patch = os.path.relpath(r["patch"], os.environ.get(
                "SURGE_PATCHES", "/home/user/surge-synthesizer/surge/resources/data/patches_factory"))
            e["outil"] = "Surge XT (surgepy) + pedalboard, réglé par pycma"
            e["source"] = "preset d'usine Surge XT « %s »" % patch[:-4]
            e["licence"] = "GPL-3.0-or-later (Surge XT, presets inclus)"
        e["variante"] = int(name.rsplit("-", 1)[1][:2])
        e["distance_adn_sigma"] = round(r["score_mp3"], 3)
        e["crete_dbfs"] = round(r["peak_db"], 2)
        if r.get("cible"):
            e["cible"] = r["cible"]
        if r.get("z"):
            e["z"] = {k: round(v, 2) for k, v in r["z"].items()}
        entries.append(e)
        shutil.copyfile(os.path.join(out, "mp3", name), os.path.join(DEST, "mp3", name))
    doc = {"zone": "C — synthés", "mesure": "distance Essentia à la cible ADN remesurée (famille × style, "
           "docs/sons/fin/cibles/zone-c-synthes.tsv), en écarts-types (0 = parfait)", "sons": entries}
    with open(os.path.join(DEST, "index.json"), "w") as f:
        json.dump(doc, f, ensure_ascii=False, indent=1)
    print(len(entries), "sons")


if __name__ == "__main__":
    main(sys.argv[1])
