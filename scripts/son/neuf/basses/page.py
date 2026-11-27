"""Build the « Nouveaux sons Basses » artifact page from the manifest.

usage: python -I page.py <basses.json> <out.html>
"""
import html
import json
import sys

STYLE_NAMES = {"trap": "Trap", "drill": "Drill", "house": "House", "techno": "Techno", "dnb": "Drum & bass",
               "edm": "EDM / future", "pop": "Pop", "lofi": "Lo-fi", "reggaeton": "Reggaeton / afro"}
FAM_NAMES = {"808": "808", "sub": "Sub", "basse-synthe": "Basse synthé", "reese": "Reese",
             "basse-acoustique": "Basse acoustique (contrebasse pizz)", "phrase": "Phrase de 2 mesures"}
ORDER = list(STYLE_NAMES)
FORDER = ["808", "basse-synthe", "sub", "reese", "basse-acoustique", "phrase"]

CSS = """
:root{--bg:#f6f7f9;--card:#ffffff;--fg:#1d2230;--muted:#5f6878;--line:#dfe3ea;--accent:#3b5bdb;
--good:#2b8a3e;--warn:#b7791f;--bad:#c92a2a;
--display:"Space Grotesk",system-ui,sans-serif;--text:"Inter",system-ui,sans-serif}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#14171d;--card:#1c2029;--fg:#e8ebf1;
--muted:#9aa3b2;--line:#2c323e;--accent:#8ea4ff;--good:#69db7c;--warn:#f0b54a;--bad:#ff8787;color-scheme:dark}}
:root[data-theme="dark"]{--bg:#14171d;--card:#1c2029;--fg:#e8ebf1;--muted:#9aa3b2;--line:#2c323e;--accent:#8ea4ff;
--good:#69db7c;--warn:#f0b54a;--bad:#ff8787;color-scheme:dark}
body{background:var(--bg);color:var(--fg);font-family:var(--text);line-height:1.5}
main{max-width:860px;margin:0 auto;padding-block:24px 48px;padding-left:max(16px,env(safe-area-inset-left,0px));
padding-right:max(16px,env(safe-area-inset-right,0px))}
h1{font-family:var(--display);font-size:1.9rem;margin:0 0 4px;text-wrap:balance}
.lead{color:var(--muted);margin:0 0 20px;max-width:65ch}
nav{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:24px}
nav a{color:var(--accent);text-decoration:none;border:1px solid var(--line);border-radius:999px;padding:3px 12px;font-size:.85rem}
nav a:focus-visible,audio:focus-visible{outline:2px solid var(--accent);outline-offset:2px}
section.style{margin-top:28px}
h2{font-family:var(--display);font-size:1.4rem;margin:0 0 8px;border-bottom:1px solid var(--line);padding-bottom:4px}
h2 small{font-family:var(--text);font-size:.8rem;color:var(--muted);font-weight:400;margin-left:8px}
h3{font-size:.8rem;text-transform:uppercase;letter-spacing:.06em;color:var(--muted);margin:16px 0 6px}
.list{display:grid;gap:8px}
.snd{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:10px 12px;display:grid;gap:6px}
.top{display:flex;flex-wrap:wrap;justify-content:space-between;gap:8px;align-items:baseline}
.name{font-family:ui-monospace,monospace;font-size:.85rem;min-width:0;overflow-wrap:anywhere}
.score{font-size:.85rem;font-variant-numeric:tabular-nums;color:var(--muted)}
.score b{font-weight:600}
.g{color:var(--good)}.w{color:var(--warn)}.b{color:var(--bad)}
audio{width:100%;height:36px}
.legend{font-size:.85rem;color:var(--muted);max-width:65ch}
"""


def chip(sd):
    c = "g" if sd < 1.0 else ("w" if sd < 2.0 else "b")
    word = "très proche" if sd < 1.0 else ("proche" if sd < 2.0 else "éloigné")
    return f'<span class="score">écart à l\'ADN : <b class="{c}">{sd:.2f} σ</b> ({word})</span>'


def main():
    man = json.load(open(sys.argv[1]))
    rows = man["sounds"]
    styles = [s for s in ORDER if any(r["style"] == s for r in rows)]
    out = ['<title>Nouveaux sons Basses</title>', '<meta name="claude:viewport-fit-sides" content="cover">',
           '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&family=Space+Grotesk:wght@600&display=swap">',
           f'<style>{CSS}</style><main>', '<h1>Nouveaux sons Basses</h1>',
           f'<p class="lead">{len(rows)} sons neufs, fabriqués à partir de l\'ADN mesuré des packs : 808, sub, basse synthé, '
           'reese et contrebasse, en notes tenues, plus une phrase de 2 mesures par style.</p>',
           '<p class="legend">Sous chaque son : son écart à la médiane de sa famille, en écarts-types '
           '(0 = pile au centre des packs). Vert sous 1, orange de 1 à 2, rouge au-delà.</p><nav>']
    out += [f'<a href="#{s}">{STYLE_NAMES[s]}</a>' for s in styles]
    out.append('</nav>')
    for s in styles:
        rs = [r for r in rows if r["style"] == s]
        avg = sum(r["essentia_sd"] for r in rs) / len(rs)
        out.append(f'<section class="style" id="{s}"><h2>{STYLE_NAMES[s]}<small>{len(rs)} sons · écart moyen {avg:.2f} σ</small></h2>')
        for f in FORDER:
            fr = [r for r in rs if r["family"] == f]
            if not fr:
                continue
            out.append(f'<h3>{FAM_NAMES[f]}</h3><div class="list">')
            for r in fr:
                src = html.escape(r.get("asset_url") or ("audio/" + r["file"]), quote=True)
                out.append(f'<div class="snd"><div class="top"><span class="name">{html.escape(r["file"])}</span>'
                           f'{chip(r["essentia_sd"])}</div><audio controls preload="none" src="{src}"></audio></div>')
            out.append('</div>')
        out.append('</section>')
    out.append('</main>')
    open(sys.argv[2], "w").write("\n".join(out))


if __name__ == "__main__":
    main()
