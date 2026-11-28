"""Build the listening page (HTML) from export.jsonl and an asset url map.

usage: page.py export.jsonl assets.json out.html
assets.json maps an mp3 file name to its artifact asset url.
"""
import html
import re
import json
import sys

STYLE_ORDER = ["trap", "house", "techno", "edm", "pop", "lofi", "synthwave", "dnb"]
STYLE_NAME = {"trap": "Trap", "house": "House", "techno": "Techno", "edm": "EDM / future",
              "pop": "Pop", "lofi": "Lo-fi", "synthwave": "Synthwave", "dnb": "Drum & bass"}
FAM_ORDER = ["lead", "pluck", "pad", "arp", "stab", "accords"]
FAM_NAME = {"lead": "Leads", "pluck": "Plucks", "pad": "Nappes", "arp": "Arpèges",
            "stab": "Stabs", "accords": "Accords de synthé"}


def grade(s):
    if s < 0.8:
        return "g1", "très proche"
    if s < 1.3:
        return "g2", "proche"
    if s < 2.0:
        return "g3", "correct"
    return "g4", "éloigné"


def main(exp, assets, out):
    rows = [json.loads(l) for l in open(exp)]
    urls = json.load(open(assets))
    by = {}
    for r in rows:
        by.setdefault(r["style"], {}).setdefault(r["family"], []).append(r)
    styles = [s for s in STYLE_ORDER if s in by]
    nav = "".join(f'<a href="#{s}">{STYLE_NAME[s]}</a>' for s in styles)
    secs = []
    for s in styles:
        fams = []
        for f in FAM_ORDER:
            rs = sorted(by[s].get(f, []), key=lambda r: r["fichier"])
            if not rs:
                continue
            items = []
            for r in rs:
                sc = r["score_mp3"]
                cls, word = grade(sc)
                u = urls.get(r["fichier"], "")
                ok = u.startswith(("/_blob/", "https://")) or re.fullmatch(r"mp3/[a-z0-9-]+\.mp3", u)
                if not ok:  # only artifact-hosted files
                    u = ""
                items.append(
                    f'<li class="snd"><div class="top"><span class="fn">{html.escape(r["fichier"])}</span>'
                    f'<span class="sc {cls}">{sc:.2f} σ · {word}</span></div>'
                    f'<audio controls preload="none" src="{html.escape(u)}"></audio></li>')
            fams.append(f'<div class="fam"><h3>{FAM_NAME[f]}</h3><ul>{"".join(items)}</ul></div>')
        secs.append(f'<section id="{s}"><h2>{STYLE_NAME[s]}</h2><div class="fams">{"".join(fams)}</div></section>')
    n = len(rows)
    med = sorted(r["score_mp3"] for r in rows)[n // 2] if n else 0
    doc = TEMPLATE.replace("%NAV%", nav).replace("%SECS%", "".join(secs)) \
        .replace("%N%", str(n)).replace("%NS%", str(len(styles))).replace("%MED%", f"{med:.2f}")
    open(out, "w").write(doc)


TEMPLATE = """<title>Nouveaux sons Synthés et nappes</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;700&family=Inter:wght@400;600&family=JetBrains+Mono:wght@400&display=swap">
<style>
/* Layout: sticky style tabs, then one band per style with family columns that wrap */
:root{--bg:#f5f6fa;--panel:#ffffff;--fg:#1d2030;--muted:#5d6378;--line:#dde0ea;--accent:#ff7a1a;
--g1:#1f9d55;--g2:#4d8f2a;--g3:#b07a00;--g4:#c0392b;
--display:"Space Grotesk",system-ui,sans-serif;--body:"Inter",system-ui,sans-serif;--mono:"JetBrains Mono",ui-monospace,monospace}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#15171f;--panel:#1e212c;--fg:#e9ebf2;--muted:#9aa0b4;--line:#2d3140;--accent:#ff8f3d;--g1:#4cd38a;--g2:#9bd35a;--g3:#f0b43c;--g4:#ff6b5e;color-scheme:dark}}
:root[data-theme="dark"]{--bg:#15171f;--panel:#1e212c;--fg:#e9ebf2;--muted:#9aa0b4;--line:#2d3140;--accent:#ff8f3d;--g1:#4cd38a;--g2:#9bd35a;--g3:#f0b43c;--g4:#ff6b5e;color-scheme:dark}
body{background:var(--bg);color:var(--fg);font-family:var(--body);line-height:1.5}
.wrap{max-width:1100px;margin:0 auto;padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px));padding-block:24px 48px}
h1{font-family:var(--display);font-size:1.9rem;margin:0 0 4px;text-wrap:balance}
.lede{color:var(--muted);margin:0 0 16px;max-width:65ch}
nav{position:sticky;top:env(safe-area-inset-top,0px);z-index:5;background:var(--bg);display:flex;flex-wrap:wrap;gap:6px;padding-block:10px;border-bottom:1px solid var(--line)}
body::before{content:"";position:fixed;inset:0 0 auto;height:env(safe-area-inset-top,0px);z-index:10;background:var(--bg)}
nav a{font-family:var(--display);font-size:.9rem;color:var(--fg);text-decoration:none;padding:4px 10px;border:1px solid var(--line);border-radius:999px;background:var(--panel)}
nav a:hover,nav a:focus-visible{border-color:var(--accent);outline:none}
section{padding-top:28px;scroll-margin-top:56px}
h2{font-family:var(--display);font-size:1.4rem;margin:0 0 12px;display:flex;align-items:center;gap:10px}
h2::before{content:"";width:10px;height:10px;border-radius:2px;background:var(--accent)}
.fams{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:16px}
.fam{background:var(--panel);border:1px solid var(--line);border-radius:10px;padding:12px;min-width:0}
h3{font-family:var(--display);font-size:1rem;margin:0 0 8px;letter-spacing:.02em}
ul{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:10px}
.top{display:flex;justify-content:space-between;gap:8px;flex-wrap:wrap;align-items:baseline}
.fn{font-family:var(--mono);font-size:.8rem;color:var(--muted);overflow-wrap:anywhere}
.sc{font-size:.8rem;font-weight:600;font-variant-numeric:tabular-nums}
.g1{color:var(--g1)}.g2{color:var(--g2)}.g3{color:var(--g3)}.g4{color:var(--g4)}
audio{width:100%;height:36px;margin-top:4px}
.key{font-size:.85rem;color:var(--muted)}
</style>
<div class="wrap">
<h1>Nouveaux sons Synthés et nappes</h1>
<p class="lede">%N% phrases de 4 mesures en %NS% styles, faites avec Surge XT (logiciel libre) puis ajustées une à une pour ressembler à l'ADN de leur famille. Sous chaque son : son écart à cet ADN, en écarts-types (plus c'est petit, plus c'est typique). Écart médian : %MED% σ.</p>
<p class="key"><span class="g1">● très proche &lt; 0,8</span> · <span class="g2">● proche &lt; 1,3</span> · <span class="g3">● correct &lt; 2</span> · <span class="g4">● éloigné</span></p>
<nav>%NAV%</nav>
%SECS%
</div>
"""

if __name__ == "__main__":
    main(*sys.argv[1:4])
