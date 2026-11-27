"""Builds the review page and the manifest from results.json + uploaded asset urls.

  python page.py OUT_DIR ASSETS_JSON PAGE_HTML MANIFEST_JSON
ASSETS_JSON maps each mp3 file name to its artifact asset url.
"""
import html
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import styles  # noqa: E402

FAMS = ["kick", "snare", "clap", "hat_closed", "hat_open", "perc", "loop"]
TOOL = "homemade numpy + pedalboard synth, tuned by pycma (CMA-ES) towards the Essentia family DNA"

CSS = """
:root{--bg:#f6f4ef;--card:#fff;--fg:#1d1b18;--mute:#6b665d;--line:#e2ddd2;--good:#2f7a4a;--mid:#a26a12;--bad:#b23b2c;--acc:#3b5bdb}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#15140f;--card:#1f1d18;--fg:#eeeae0;--mute:#a19b8f;--line:#33302a;--good:#6cc58c;--mid:#e0a84a;--bad:#ef7a6a;--acc:#8ea6ff;color-scheme:dark}}
:root[data-theme="dark"]{--bg:#15140f;--card:#1f1d18;--fg:#eeeae0;--mute:#a19b8f;--line:#33302a;--good:#6cc58c;--mid:#e0a84a;--bad:#ef7a6a;--acc:#8ea6ff;color-scheme:dark}
body{background:var(--bg);color:var(--fg);font-family:Inter,system-ui,sans-serif;padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px));padding-block:24px 48px}
main{max-width:980px;margin:0 auto}
h1{font-family:"Space Grotesk",Inter,sans-serif;font-size:1.8rem;margin:0 0 .3rem;text-wrap:balance}
h2{font-family:"Space Grotesk",Inter,sans-serif;font-size:1.3rem;margin:2rem 0 .2rem;border-bottom:2px solid var(--line);padding-bottom:.3rem}
h2 small{color:var(--mute);font-weight:400;font-size:.85rem}
h3{font-size:.95rem;margin:1rem 0 .4rem;color:var(--mute);text-transform:uppercase;letter-spacing:.04em}
p.lead{color:var(--mute);max-width:65ch;line-height:1.5}
nav{display:flex;flex-wrap:wrap;gap:.4rem;margin:1rem 0}
nav a{color:var(--acc);text-decoration:none;border:1px solid var(--line);border-radius:999px;padding:.2rem .7rem;background:var(--card)}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:.6rem}
.s{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:.6rem}
.s .n{font-size:.8rem;font-family:ui-monospace,monospace;word-break:break-all}
audio{width:100%;height:36px;margin:.3rem 0}
.sc{font-size:.8rem;color:var(--mute);line-height:1.4}
.b{font-weight:700}.g{color:var(--good)}.m{color:var(--mid)}.r{color:var(--bad)}
"""


def cls(sd):
    return "g" if sd <= 1.0 else ("m" if sd <= 2.0 else "r")


def main():
    out, assets_p, page_p, man_p = sys.argv[1:5]
    res = json.load(open(os.path.join(out, "results.json")))
    assets = json.load(open(assets_p))
    man = []
    body = []
    for s, st in styles.STYLES.items():
        body.append('<h2 id="%s">%s <small>%d BPM</small></h2>' % (s, html.escape(st["label"]), st["bpm"]))
        for f in FAMS:
            rows = sorted((r for r in res if r["style"] == s and r["family"] == f), key=lambda r: r["file"])
            if not rows:
                continue
            body.append("<h3>%s</h3><div class=grid>" % html.escape(styles.FAM_LABEL[f]))
            for r in rows:
                url = assets.get(r["file"])
                fam_dna = "drum_loop/drums" if f == "loop" else "drum_oneshot/" + f
                clap = ""
                if r.get("clap_cos") is not None:
                    clap = "CLAP %.2f (rang %d/6)" % (r["clap_cos"], r["clap_rank"]) if f != "loop" \
                        else "CLAP %.2f" % r["clap_cos"]
                extra = ""
                if f == "loop" and r.get("bpm_measured"):
                    extra = " · tempo mesuré %s" % r["bpm_measured"]
                body.append(
                    '<div class=s><div class=n>%s</div>%s<div class=sc>'
                    'Essentia <span class="b %s">%.2f σ</span> · %s mesures dans p10–p90 · librosa %s<br>%s%s</div></div>'
                    % (html.escape(r["file"]),
                       '<audio controls preload=none src="%s"></audio>' % html.escape(url) if url else "",
                       cls(r["essentia_sd"]), r["essentia_sd"], r["in_p10_p90"], r.get("librosa_agree", "-"),
                       clap, extra))
                man.append(dict(file=r["file"], style=s, family=f, dna_family=fam_dna, tool=TOOL,
                                source="in-house synthesis (no sample, no recording)",
                                license="project creation", essentia_sd=r["essentia_sd"],
                                in_p10_p90=r["in_p10_p90"], librosa_agree=r.get("librosa_agree"),
                                clap_cos=r.get("clap_cos"), clap_rank=r.get("clap_rank"),
                                bpm=r.get("bpm"), bpm_measured=r.get("bpm_measured"),
                                params=r.get("params"), seed=r.get("seed"), asset_url=url))
            body.append("</div>")
    nav = "".join('<a href="#%s">%s</a>' % (s, html.escape(st["label"])) for s, st in styles.STYLES.items())
    one = [m["essentia_sd"] for m in man if m["family"] != "loop"]
    lead = ("%d sons neufs fabriqués par synthèse (aucun échantillon), réglés pour se rapprocher de l'ADN "
            "Essentia de leur famille. La note Essentia est une distance en écarts-types à la médiane de la famille "
            "(0 = parfait ; vert ≤ 1, orange ≤ 2). CLAP : ressemblance avec la description texte de la famille. "
            "Médiane des coups : %.2f σ." % (len(man), sorted(one)[len(one) // 2] if one else 0))
    page = ('<title>Nouveaux sons Batterie électronique</title>\n'
            '<meta name="claude:viewport-fit-sides" content="cover">\n'
            '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;700&family=Space+Grotesk:wght@600&display=swap">\n'
            "<style>%s</style>\n<main><h1>Nouveaux sons Batterie électronique</h1><p class=lead>%s</p>"
            "<nav>%s</nav>%s</main>\n" % (CSS, html.escape(lead), nav, "\n".join(body)))
    open(page_p, "w").write(page)
    json.dump(man, open(man_p, "w"), indent=1, ensure_ascii=False)
    print(len(man), "sounds")


if __name__ == "__main__":
    main()
