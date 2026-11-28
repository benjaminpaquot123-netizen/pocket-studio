"""Write docs/sons/neuf/synthes.json from export.jsonl and the asset url map.

usage: manifest.py export.jsonl assets.json artifact_url out.json
"""
import json
import sys

FAM_FR = {"lead": "lead", "pluck": "pluck", "pad": "nappe", "arp": "arpège",
          "stab": "stab", "accords": "accords de synthé"}


def main(exp, assets, art, out):
    urls = json.load(open(assets))
    rows = sorted((json.loads(l) for l in open(exp)), key=lambda r: r["fichier"])
    sons = []
    for r in rows:
        sons.append({
            "fichier": r["fichier"],
            "style": r["style"],
            "famille": FAM_FR[r["family"]],
            "outil": "Surge XT (surgepy) + pedalboard, réglage CMA-ES (pycma)",
            "source": "preset d'usine Surge XT « %s », phrase de 4 mesures écrite par le script" % r["patch"].split("patches_factory/")[-1].removesuffix(".fxp"),
            "licence": "GPL-3.0-or-later (Surge XT et ses presets d'usine)",
            "note_essentia_sigma": round(r["score_mp3"], 3),
            "crete_dbfs": round(r["peak_db"], 2),
            "url_asset": urls.get(r["fichier"], ""),
        })
    doc = {
        "zone": "Synthés et nappes",
        "artefact": art,
        "note": "écart RMS en écarts-types à l'ADN médian de la famille (docs/sons/fin/essentia-familles.tsv), 14 mesures Essentia ; plus petit = plus typique",
        "clap": "non utilisé (pas installé dans le temps imparti)",
        "url_asset_note": "MP3 publiés comme fichiers de l'artefact (chemin relatif à son URL) : la réserve d'assets refuse le MP3",
        "librosa": "contre-mesure librosa faite dans un sous-processus ; plantage numba du suivi de tempo sur 84 sons, champ ignoré",
        "mp3": "192 kb/s, crête <= -1 dBFS, aucune métadonnée",
        "sons": sons,
    }
    with open(out, "w") as f:
        json.dump(doc, f, ensure_ascii=False, indent=1)
        f.write("\n")


if __name__ == "__main__":
    main(*sys.argv[1:5])
