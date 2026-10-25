"""Audit 35: the per-style numbers table (bench json + render report + m4a + pre-master + phone + live)."""
import json, os, statistics as S, sys

B = "/home/user/ps-audit/build/bench"
T = "/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35"
FAM = {
    "Techno": "techno detroit acid_techno hard_techno industrial_techno retro_techno acidcore trance minimal gabber".split(),
    "House": "house deep_house tech_house acid_house uk_garage afro_house speed_garage amapiano disco".split(),
    "Bass music": "dnb jump_up dubstep liquid_dnb neurofunk jungle riddim".split(),
    "Hip-hop": "trap drill phonk boom_bap grime jersey_club lofi".split(),
    "Autres": "reggaeton afrobeats synthwave pop".split(),
}
phone = {json.loads(l)["style"]: json.loads(l)["m"] for l in open(f"{T}/phone.jsonl")}
live = json.load(open(f"{T}/live.json"))


def load(p):
    try:
        return json.load(open(p))
    except Exception:
        return None


def f(x, n=1, plus=False):
    if x is None: return "–"
    if isinstance(x, list):
        return " / ".join(f(v, n, plus) for v in x) if x else "–"
    s = f"{x:+.{n}f}" if plus else f"{x:.{n}f}"
    return s.replace(".", ",").replace("-", "−")


rows, agg = [], {}
for fam, styles in FAM.items():
    for st in styles:
        j, r, m, pre = load(f"{B}/json/{st}.json"), load(f"{B}/render/{st}.json"), load(f"{B}/json-m4a/{st}.json"), load(f"{B}/json-pre/{st}.json")
        ph, lv = phone.get(st, {}), live.get(st, {})
        if not j: continue
        d = dict(style=st, fam=fam, lufs=j["lufs_i"], loud=j.get("loud_lufs"), target=j.get("target_lufs") or (r or {}).get("target_lufs"),
                 tp=j["true_peak"], db=j.get("drop_minus_break"), dg=j.get("drop_minus_groove"), dme=j.get("drop_minus_montee_end"),
                 lra=j.get("lra"), plr=j.get("plr"), sm=j.get("side_mid"), corr=j.get("corr"), low=j["bands"]["low"], air=j["bands"]["air"],
                 crest=j.get("main_crest"), loss=(r or {}).get("loss_db"), drive=(r or {}).get("drive_db"),
                 m4a_lufs=(m or {}).get("lufs_i"), m4a_tp=(m or {}).get("true_peak"),
                 pre_db=(pre or {}).get("drop_minus_break"), intro=j.get("intro_minus_main"),
                 tel=ph.get("tel300"), pres=ph.get("pres"), lowph=ph.get("lowphone"), smd=ph.get("sm"), env=ph.get("env_d12"),
                 d2=(ph.get("lufs_d2") - ph.get("lufs")) if ph.get("lufs_d2") is not None else None, drops=ph.get("drops"), drop_s=ph.get("drop1_s"),
                 ldrive=lv.get("drive_max"), lramp=lv.get("ramp_s"), lst=(lv.get("st_start"), lv.get("st_ramp")), llim=lv.get("limiter_min_db"),
                 ldrop=lv.get("st_drop"), seconds=j.get("seconds"))
        rows.append(d)
json.dump(rows, open(f"{T}/table.json", "w"), ensure_ascii=False, indent=0)
print(len(rows), "styles")
for d in rows:
    m4 = None if d["m4a_lufs"] is None else d["m4a_lufs"] - d["lufs"]
    print(f"| {d['style']} | {f(d['loud'])} ({f(d['target'],0)}) | {f(d['lufs'])} | {f(d['db'],1,True)} | {f(d['dme'],1,True)} | {f(d['tp'],2)} | {f(d['m4a_tp'],1)} ({f(m4,1,True)}) | {f(d['smd'])} | {f(d['tel'])} | {f(d['pres'])} | {f(d['lowph'])} | {f(d['env'],2)} / {f(d['d2'],1,True)} | {f(d['drive'])} / {f(d['loss'])} | {f(d['ldrive'])} · {d['lramp']} s · {f(d['llim'])} |")
# medians
def med(k, fam=None):
    v = [d[k] for d in rows if d[k] is not None and not isinstance(d[k], list) and (fam is None or d["fam"] == fam)]
    return round(S.median(v), 2) if v else None
keys = ["loud", "lufs", "db", "dg", "tp", "lra", "plr", "sm", "smd", "low", "air", "crest", "loss", "drive", "tel", "pres", "lowph", "env", "d2", "pre_db", "ldrive", "lramp", "llim", "m4a_tp"]
for fam in [None] + list(FAM):
    print(fam or "TOUT", {k: med(k, fam) for k in keys})
dme = [min(d["dme"]) for d in rows if d["dme"]]
print("dme min-of-song median", S.median(dme) if dme else None, "count", len(dme), "under 1.5:", sum(1 for x in dme if x < 1.5))
m4 = [d["m4a_lufs"] - d["lufs"] for d in rows if d["m4a_lufs"] is not None]
print("m4a - wav LUFS median", S.median(m4) if m4 else None, min(m4) if m4 else None, max(m4) if m4 else None)
print("Drop-Break >= 4:", sum(1 for d in rows if d["db"] is not None and d["db"] >= 4), "of", sum(1 for d in rows if d["db"] is not None))
print("Drop-Groove >= 2:", sum(1 for d in rows if d["dg"] is not None and d["dg"] >= 2), "of", sum(1 for d in rows if d["dg"] is not None))
print("sm in -15..-9:", sum(1 for d in rows if d["smd"] is not None and -15 <= d["smd"] <= -9))
