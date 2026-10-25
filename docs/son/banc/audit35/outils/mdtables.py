"""Audit 35: the Markdown tables of docs/audit-35/mesures.md, from the bench, the phone pass and the live logs."""
import json, statistics as S

T = "/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35"
B = f"{T}/bench"
FAM = {
    "Techno": "techno detroit acid_techno hard_techno industrial_techno retro_techno acidcore trance minimal gabber".split(),
    "House": "house deep_house tech_house acid_house uk_garage afro_house speed_garage amapiano disco".split(),
    "Bass music": "dnb jump_up dubstep liquid_dnb neurofunk jungle riddim".split(),
    "Hip-hop": "trap drill phonk boom_bap grime jersey_club lofi".split(),
    "Autres": "reggaeton afrobeats synthwave pop".split(),
}
bench = json.load(open(f"{B}/bench.json"))["styles"]
bench = {s["style"]: s for s in bench} if isinstance(bench, list) else bench
phone = {json.loads(l)["style"]: json.loads(l)["m"] for l in open(f"{T}/phone.jsonl")}
live = json.load(open(f"{T}/live-all.json"))
flags = {}
for src in (f"{T}/live/log", f"{T}/live/log-long"):
    import glob, os
    for p in glob.glob(f"{src}/*.txt"):
        flags[os.path.basename(p)[:-4]] = sum(1 for l in open(p) if l.startswith("FLAG "))
render = {}
m4a = {}
for st in bench:
    try: render[st] = json.load(open(f"{B}/render/{st}.json"))
    except Exception: pass
    try: m4a[st] = json.load(open(f"{B}/json-m4a/{st}.json"))
    except Exception: pass


def f(x, n=1, plus=False):
    if x is None: return "–"
    s = f"{x:+.{n}f}" if plus else f"{x:.{n}f}"
    return s.replace(".", ",").replace("-", "−")


out = []
out.append("### A. Export (master, sonie, contraste)\n")
out.append("| Style | Partie forte (cible) | LUFS-I | Drop − Break | Drop − Groove | Drop − fin de Montée (min) | True peak | M4A : TP / écart LUFS | Gain cible / perte | LRA | PLR |")
out.append("|---|---|---|---|---|---|---|---|---|---|---|")
for fam, sts in FAM.items():
    out.append(f"| **{fam}** | | | | | | | | | | |")
    for st in sts:
        j, r, m = bench[st], render.get(st, {}), m4a.get(st, {})
        dme = j.get("drop_minus_montee_end") or []
        out.append(f"| {st} | {f(j.get('loud_lufs'))} ({f(j.get('target_lufs'),0)}) | {f(j['lufs_i'])} | {f(j.get('drop_minus_break'),1,True)} | {f(j.get('drop_minus_groove'),1,True)} | "
                   f"{f(min(dme),1,True) if dme else '–'} | {f(j['true_peak'],2)} | {f(m.get('true_peak'))} / {f(m['lufs_i'] - j['lufs_i'],1,True) if m.get('lufs_i') is not None else '–'} | "
                   f"{f(r.get('drive_db'),1,True)} / {f(r.get('loss_db'))} | {f(j.get('lra'))} | {f(j.get('plr'))} |")
out.append("\n### B. Le Drop : largeur, haut-parleur de téléphone, Drops répétés\n")
out.append("| Style | Drop à (s) | Drops | Côté/centre Drop (dB) | Corrélation (morceau) | Grave 60–250 (dB rel.) | Air > 6 k (dB rel.) | Perte tél. 300 Hz (LU) | Présence 1–4 k au tél. (dB) | 300–800 Hz au tél. (dB) | Drop 2 / Drop 1 : empreinte · Δ LU |")
out.append("|---|---|---|---|---|---|---|---|---|---|---|")
for fam, sts in FAM.items():
    out.append(f"| **{fam}** | | | | | | | | | | |")
    for st in sts:
        j, p = bench[st], phone.get(st, {})
        d2 = None if p.get("lufs_d2") is None else p["lufs_d2"] - p["lufs"]
        out.append(f"| {st} | {f(p.get('drop1_s'),0)} | {p.get('drops','–')} | {f(p.get('sm'))} | {f(j.get('corr'),2)} | {f(j['bands']['low'])} | {f(j['bands']['air'])} | "
                   f"{f(p.get('tel300'))} | {f(p.get('pres'))} | {f(p.get('lowphone'))} | {f(p.get('env_d12'),2)} · {f(d2,1,True)} |")
out.append("\n### C. Écoute en direct (ps_live, comme le téléphone)\n")
out.append("| Style | Alertes | Gain live max (dB) | Intro : niveau à 4 s → après la montée du gain (dBFS c.t.), en s | Intro direct à 4 s − Intro exportée (LU) | Drop direct (4 s après) − partie forte exportée (LU) | Limiteur le plus profond (dB) |")
out.append("|---|---|---|---|---|---|---|")
for fam, sts in FAM.items():
    out.append(f"| **{fam}** | | | | | | |")
    for st in sts:
        v, j = live.get(st, {}), bench[st]
        intro = None if j.get("intro_lufs") is None or v.get("st_start") is None else v["st_start"] - j["intro_lufs"]
        dd = None if v.get("st_drop") is None else v["st_drop"] - j["loud_lufs"]
        out.append(f"| {st} | {flags.get(st, '–')} | {f(v.get('drive_max'),1,True)} | {f(v.get('st_start'))} → {f(v.get('st_ramp'))} en {v.get('ramp_s')} s | {f(intro,1,True)} | {f(dd,1,True)} | {f(v.get('limiter_min_db'))} |")
open(f"{T}/tables.md", "w").write("\n".join(out) + "\n")
print("\n".join(out)[:3000])
