"""Build the review page data (data.json) from sons/ia-bass/index.json.

usage: python -I page.py INDEX_JSON OUT_JSON
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import prompts as P  # noqa: E402

STYLE = {"dubstep": "Dubstep", "dnb": "Drum and bass"}


def mot(s):
    return "très proche" if s < 0.6 else "proche" if s < 1 else "assez proche" if s < 1.5 else "éloigné"


idx = json.load(open(sys.argv[1]))
groups = []
for fam in P.FAMILIES:
    for style in P.BPM:
        ss = sorted((s for s in idx["sounds"] if s["family"] == fam and s["style"] == style), key=lambda s: s["rank"])
        if not ss:
            continue
        groups.append(dict(style=style, styleLabel=STYLE[style], fam=fam, famLabel=P.FAMILIES[fam]["label"],
                           bpm=P.BPM[style],
                           sons=[dict(id="ia-bass/" + s["file"][:-4], src="sons/" + s["file"],
                                      mot=mot(s["score_mp3"]), sig=round(s["score_mp3"], 2),
                                      tag=s["cut"] + ", " + s["proc"]) for s in ss]))
data = dict(total=len(idx["sounds"]), familles=len({g["fam"] for g in groups}),
            generes=sum(v["generated"] for v in idx["stats"].values()), groupes=groups)
json.dump(data, open(sys.argv[2], "w"), ensure_ascii=False, indent=0)
print(data["total"], data["familles"], data["generes"])
