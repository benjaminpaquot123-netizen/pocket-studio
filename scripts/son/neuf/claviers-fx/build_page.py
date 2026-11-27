"""Build the artifact page and the manifest from DIR/results.jsonl and an
asset map {file: url} (JSON). Writes PAGE (HTML) and MANIFEST (JSON).

Usage: python -I build_page.py --out DIR --assets assets.json --page page.html
           --manifest docs/sons/neuf/claviers-fx.json
"""
import argparse
import html
import json
import os

STYLES = [("pop", "Pop"), ("lofi", "Lo-fi"), ("house", "House"), ("trap", "Trap"),
          ("edm", "EDM / future"), ("jazz-soul", "Jazz / soul"), ("rnb", "R&B")]
FAMS = [("piano", "Piano"), ("piano-electrique", "Piano électrique"), ("orgue", "Orgue"),
        ("effet", "Effets"), ("voix", "Coupures de voix")]
KIND = {"riser": "montée", "impact": "impact", "downlifter": "descente", "sweep": "balayage"}
LIC = {"Salamander": ("Salamander Grand Piano V3 (Alexander Holm)", "CC BY 3.0"),
       "VCSL": ("VCSL, Yamaha TX81Z (Versilian Studios)", "CC0-1.0"),
       "VocalSet": ("VocalSet (Wilkins et al., 2018)", "CC BY 4.0")}
TOOL = "make.py : pedalboard (GPL-3.0) réglé par pycma vers l'ADN Essentia"


def source_licence(src):
    for k, v in LIC.items():
        if src.startswith(k):
            return "%s ; %s" % (v[0], src), v[1]
    return src + " (synthèse, sans échantillon)", "œuvre neuve, aucune source tierce"


def grade(d):
    return "très proche" if d < 0.5 else "proche" if d < 0.75 else "assez proche" if d < 1.0 else "éloigné"


CSS = """
:root{--bg:#f6f4ef;--card:#fffdf8;--fg:#1f1d1a;--muted:#6b665d;--line:#e3ded3;--accent:#c2410c;--good:#15803d;--mid:#a16207;--far:#b91c1c}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#16140f;--card:#1f1c16;--fg:#efe9dd;--muted:#a59f92;--line:#36312a;--accent:#fb923c;--good:#4ade80;--mid:#facc15;--far:#f87171;color-scheme:dark}}
:root[data-theme="dark"]{--bg:#16140f;--card:#1f1c16;--fg:#efe9dd;--muted:#a59f92;--line:#36312a;--accent:#fb923c;--good:#4ade80;--mid:#facc15;--far:#f87171;color-scheme:dark}
body{background:var(--bg);color:var(--fg);font:15px/1.5 Inter,system-ui,sans-serif}
.wrap{max-width:960px;margin:0 auto;padding-block:24px 64px;padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px))}
h1{font:600 1.7rem/1.2 "Space Grotesk",Inter,sans-serif;margin:0 0 6px;text-wrap:balance}
.lead{color:var(--muted);max-width:65ch;margin:0 0 18px}
nav{display:flex;flex-wrap:wrap;gap:6px;margin:0 0 22px}
nav a{color:var(--fg);text-decoration:none;border:1px solid var(--line);border-radius:999px;padding:4px 12px;background:var(--card)}
h2{font:600 1.35rem "Space Grotesk",Inter,sans-serif;margin:34px 0 4px;border-bottom:2px solid var(--accent);padding-bottom:4px}
.meta{color:var(--muted);font-size:.85rem;margin:0 0 10px}
h3{font-size:1rem;margin:18px 0 8px}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:10px}
.card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px 12px;min-width:0}
.name{font:500 .85rem ui-monospace,monospace;word-break:break-all}
audio{width:100%;margin:6px 0 4px;height:36px}
.score{font-size:.85rem;display:flex;flex-wrap:wrap;gap:4px 12px}
.d{font-weight:600}.g0{color:var(--good)}.g1{color:var(--good)}.g2{color:var(--mid)}.g3{color:var(--far)}
.src{color:var(--muted);font-size:.78rem;margin-top:4px}
.legend{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px 14px;font-size:.88rem;color:var(--muted);max-width:65ch}
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--assets", required=True)
    ap.add_argument("--page", required=True)
    ap.add_argument("--manifest", required=True)
    a = ap.parse_args()
    recs = [json.loads(l) for l in open(os.path.join(a.out, "results.jsonl"))]
    urls = json.load(open(a.assets))
    man = []
    for r in sorted(recs, key=lambda r: r["file"]):
        src, lic = source_licence(r["meta"]["source"])
        man.append({"fichier": r["file"], "style": r["style"], "famille": r["famille"],
                    "famille_adn": r["family"], "sorte": r["meta"].get("kind"), "bpm": r["meta"].get("bpm"),
                    "outil": TOOL, "source": src, "licence": lic,
                    "note_essentia_sd": r["essentia_distance_sd"], "z_essentia": r["essentia_z"],
                    "note_librosa_sd": r["librosa_distance_sd"], "clap_texte": r.get("clap_text_sim"),
                    "clap_prompt": r.get("clap_prompt"), "duree_s": r["dur_s"], "crete_dbfs": r["peak_dbfs"],
                    "asset_url": urls.get(r["file"])})
    os.makedirs(os.path.dirname(a.manifest), exist_ok=True)
    with open(a.manifest, "w") as f:
        json.dump(man, f, ensure_ascii=False, indent=1)
        f.write("\n")

    e = html.escape
    by = {}
    for m in man:
        by.setdefault(m["style"], {}).setdefault(m["famille"], []).append(m)
    out = ['<title>Nouveaux sons Claviers, effets et voix</title>',
           '<meta name="claude:viewport-fit-sides" content="cover">',
           '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Space+Grotesk:wght@600&display=swap">',
           '<style>%s</style><div class="wrap">' % CSS,
           '<h1>Nouveaux sons Claviers, effets et voix</h1>',
           '<p class="lead">%d sons neufs, fabriqués pour Pocket Studio et réglés pour ressembler à l\'ADN mesuré de chaque famille. '
           'Rangés par style, puis par famille.</p>' % len(man),
           '<div class="legend"><b>Lire la note.</b> Distance Essentia à l\'ADN de la famille, en écarts-types : 0 = pile au milieu de la famille. '
           'Moins de 0,5 très proche, moins de 0,75 proche, moins de 1 assez proche. La longueur d\'une note de clavier n\'est pas comptée à part : '
           'une phrase dure des secondes, la famille mesure des notes courtes. « librosa » est la contre-mesure, « CLAP » dit à quel point le son colle aux mots de son style (de 0 à 1, rarement au-dessus de 0,5).</div>',
           '<nav>' + "".join('<a href="#%s">%s</a>' % (s, l) for s, l in STYLES if s in by) + '</nav>']
    for s, sl in STYLES:
        if s not in by:
            continue
        bpm = next(iter(by[s].values()))[0]["bpm"]
        out.append('<h2 id="%s">%s</h2><p class="meta">%s bpm</p>' % (s, sl, bpm))
        for f, fl in FAMS:
            items = by[s].get(f)
            if not items:
                continue
            out.append('<h3>%s</h3><div class="grid">' % fl)
            for m in items:
                d = m["note_essentia_sd"]
                g = 0 if d < .5 else 1 if d < .75 else 2 if d < 1 else 3
                kind = (" · " + KIND.get(m["sorte"], m["sorte"])) if m["sorte"] else ""
                clap = ('<span>CLAP %.2f</span>' % m["clap_texte"]) if m["clap_texte"] is not None else ""
                out.append('<div class="card"><div class="name">%s%s</div>'
                           '<audio controls preload="none" src="%s"></audio>'
                           '<div class="score"><span class="d g%d">%.2f σ · %s</span><span>librosa %.2f</span>%s</div>'
                           '<div class="src">%s · %s</div></div>'
                           % (e(m["fichier"]), e(kind), e(m["asset_url"] or ""), g, d, grade(d),
                              m["note_librosa_sd"], clap, e(m["source"]), e(m["licence"])))
            out.append('</div>')
    out.append('</div>')
    with open(a.page, "w") as f:
        f.write("\n".join(out))


if __name__ == "__main__":
    main()
