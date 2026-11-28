"""Zone C musical templates: the synthes phrases (scripts/son/neuf/synthes/
music.py) extended to every big style of the "Nouveaux sons" page.

Families (French names, as on the page): stab, pluck, nappe (pad), accords,
boucle (melodic loop / lead). Each is played by one of the synthes phrase
generators; new styles get their own tempo, key, progression and rhythms.
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
                                "neuf", "synthes"))
import music as _m  # noqa: E402

MAJ, MIN = _m.MAJ, _m.MIN
BARS = _m.BARS
QUAL = _m.QUAL
STYLES = dict(_m.STYLES)
STYLES.update({
    "drill":          dict(bpm=142, swing=0.5, tonic=56, scale=MIN, prog=[(0, "m"), (5, "M"), (4, "m"), (5, "M")]),
    "boom-bap":       dict(bpm=90, swing=0.58, tonic=57, scale=MIN, prog=[(0, "m7"), (3, "m7"), (5, "M7"), (4, "m7")]),
    "reggaeton-afro": dict(bpm=100, swing=0.52, tonic=55, scale=MIN, prog=[(0, "m"), (5, "M"), (2, "M"), (6, "M")]),
    "rnb":            dict(bpm=76, swing=0.56, tonic=58, scale=MAJ, prog=[(1, "m7"), (4, "7"), (0, "M7"), (3, "M7")]),
})
STYLES["lo-fi"] = STYLES.pop("lofi")
STYLE_ORDER = ["trap", "drill", "boom-bap", "lo-fi", "house", "techno", "edm", "dnb",
               "reggaeton-afro", "pop", "synthwave", "rnb"]
STYLES = {k: STYLES[k] for k in STYLE_ORDER}

# page family -> synthes phrase family
PHRASE = {"stab": "stab", "pluck": "pluck", "nappe": "pad", "accords": "accords", "boucle": "lead"}
FAMILIES = list(PHRASE)

_RHYTHMS = {  # new styles, 16th grid over one bar
    "drill": dict(lead=[0, 3, 7, 10, 13], stab=[0, 3, 10], chord=[0, 7, 10], pluck=[0, 3, 7, 10, 13, 14]),
    "boom-bap": dict(lead=[0, 4, 7, 10, 14], stab=[0, 7, 10], chord=[0, 10], pluck=[0, 3, 6, 10, 12]),
    "reggaeton-afro": dict(lead=[0, 3, 6, 8, 11, 14], stab=[3, 6, 11, 14], chord=[0, 3, 6, 8, 11, 14],
                           pluck=[0, 3, 6, 8, 11, 14]),
    "rnb": dict(lead=[0, 3, 6, 10, 13], stab=[0, 6, 10], chord=[0, 6, 10], pluck=[0, 3, 6, 10, 12, 14]),
}
for st, r in _RHYTHMS.items():
    _m.LEAD_RHYTHM[st] = r["lead"]
    _m.STAB_RHYTHM[st] = r["stab"]
    _m.CHORD_RHYTHM[st] = r["chord"]
    _m.PLUCK_RHYTHM[st] = r["pluck"]
for tbl in (_m.LEAD_RHYTHM, _m.STAB_RHYTHM, _m.CHORD_RHYTHM, _m.PLUCK_RHYTHM):
    tbl["lo-fi"] = tbl["lofi"]
_m.STYLES.update(STYLES)


def phrase(style, family, variant=0):
    return _m.phrase(style, PHRASE.get(family, family), variant)
