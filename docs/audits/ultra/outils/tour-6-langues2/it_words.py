"""Italian: « kick » → « cassa » (feminine: articles, prepositions and the adjective after it follow),
« step » → « passo ». Pad names (keys that are a pad's name) and quoted names are left alone."""
import re, sys, os, json, glob
sys.dont_write_bytecode = True  # no __pycache__ left in the repository
sys.path.insert(0, os.path.dirname(__file__))
import catlib

PAD_NAMES = {"Kick", "Kicks", "Kick 2", "Kick studio", "Kick dur", "Kick analogique", "Kick (grosse caisse)",
             "Kick : la grosse caisse, le « boum » grave de la batterie."}
SG = {"il": "la", "un": "una", "del": "della", "al": "alla", "dal": "dalla", "nel": "nella", "sul": "sulla", "col": "con la", "lo": "la"}
PL = {"i": "le", "dei": "delle", "ai": "alle", "dai": "dalle", "nei": "nelle", "sui": "sulle", "coi": "con le", "gli": "le"}
ADJ = {"distorto": "distorta", "lungo": "lunga", "corto": "corta", "rotondo": "rotonda", "secco": "secca", "morbido": "morbida",
       "duro": "dura", "risuonato": "risuonata", "ultra-distorto": "ultra-distorta", "analogico": "analogica", "sfasato": "sfasata",
       "saturo": "satura", "saturato": "saturata", "pieno": "piena", "solo": "sola", "tondo": "tonda", "grosso": "grossa",
       "pesante": "pesante", "potente": "potente"}
QUOTED = re.compile(r"“[^”]*”|«[^»]*»")


def cap(src, word):
    return word[0].upper() + word[1:] if src[0].isupper() else word


def kick(text):
    # protect quoted names
    keep = []
    def mask(m):
        keep.append(m.group(0)); return "\x00%d\x00" % (len(keep) - 1)
    t = QUOTED.sub(mask, text)

    def rep(m):
        art, k, adj = m.group(1), m.group(2), m.group(3)
        plural = k.lower() == "kicks" or (art and art.strip().lower() in PL)
        noun = "casse" if plural else "cassa"
        out = ""
        if art:
            a = art.strip()
            table = PL if plural else SG
            new = table.get(a.lower(), a)
            out = cap(a, new) + " "
            noun_out = noun
        else:
            noun_out = cap(k, noun)
        res = out + noun_out
        if adj:
            a = adj.strip()
            fem = ADJ.get(a.lower(), a)
            if plural and fem.endswith("a"): fem = fem[:-1] + "e"
            res += " " + fem
        return res
    t = re.sub(r"(?<![\w-])((?:[Ii]l|[Uu]n|[Dd]el|[Aa]l|[Dd]al|[Nn]el|[Ss]ul|[Cc]ol|[Ii]|[Dd]ei|[Aa]i|[Dd]ai|[Nn]ei|[Ss]ui|[Cc]oi|[Gg]li) )?\b([Kk]icks?)\b( (?:" + "|".join(map(re.escape, ADJ)) + r")\b)?", rep, t)
    for i, k in enumerate(keep):
        t = t.replace("\x00%d\x00" % i, k)
    return t


if __name__ == "__main__":
    mode, files = sys.argv[1], sys.argv[2:]
    for f in files:
        b = os.path.basename(f)
        fn = lambda k, v: None if k in PAD_NAMES else kick(v)
        if mode == "apply":
            ch = catlib.edit(f, fn)
        else:
            ch = [(k, v, fn(k, v)) for k, v in catlib.entries(f).items() if fn(k, v) not in (None, v)]
        for k, v, n in ch:
            print(f"{b}\t{k[:70]}\n   - {v}\n   + {n}")
