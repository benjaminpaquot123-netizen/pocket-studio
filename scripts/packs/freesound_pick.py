#!/usr/bin/env python3
"""Picks modern one-shots of one drum family on Freesound, by measurement, and fetches the kept originals.

  freesound_pick.py search  <family.json> <work dir>   search + preview + measure + score -> candidates.json
  freesound_pick.py fetch   <family.json> <work dir>   originals of the kept sounds (OAuth) -> orig/, processed/, kept.json

<family.json> (e.g. scripts/packs/freesound/hats.rules.json in the work dir) lists, per sub-role, the
queries, the duration window, the number to keep, the longest processed tail and the target styles.
Audio never goes to git: everything lands in <work dir>. Secrets come from files named by environment
variables (FREESOUND_KEY_FILE, FREESOUND_OAUTH_FILE) and are never printed.
Only CC0, or CC-BY 4.0 when asked; a text naming a record, a song, a sample CD, a commercial pack or a
break is refused before any listening. Python standard library + numpy; ffmpeg decodes and encodes.
"""
import hashlib
import html
import json
import math
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

import numpy as np

BASE = "https://freesound.org"
AGENT = "pocket-studio-packs/1.0"
RATE = 48000
CC0 = "http://creativecommons.org/publicdomain/zero/1.0/"
BY4 = "http://creativecommons.org/licenses/by/4.0/"
FIELDS = ("id,name,username,license,description,tags,duration,samplerate,channels,type,filesize,"
          "previews,url,avg_rating,num_ratings,num_downloads,bitdepth")

# Words that make a sound unusable whatever it sounds like (the repo is public, the app is shared).
REFUSED_TEXT = re.compile(
    r"\b(vinyl|sample ?cds?|album|songs?|ripped|splice|loopmasters|vengeance|native instruments|"
    r"sampled (from|off)|taken from|cut from|from (a|the|my) (record|track|tune)|copyright(ed)?|"
    r"all rights reserved|breaks?|breakbeat|amen|acapella|vocals?|commercial (pack|library)|"
    r"(drum|beat) ?loops?|loops?|pattern|bpm|groove|full kit|drum ?kit pack)\b", re.I)
# Machine names are fine (a recording of a drum machine is not a record).


def refused_text(name, description, tags):
    """The reason a sound is refused on its words alone, or None."""
    text = " ".join([name or "", description or "", " ".join(tags or [])])
    m = REFUSED_TEXT.search(text)
    return f"text: {m.group(0).lower()}" if m else None


def licence_ok(url, allow_by):
    url = (url or "").replace("https://", "http://").rstrip("/") + "/"
    if url == CC0:
        return "CC0 1.0"
    if allow_by and url == BY4:
        return "CC BY 4.0"
    return None


def in_range(v, lo, hi, soft):
    """1 inside [lo, hi], falling to 0 at `soft` beyond either end."""
    if lo <= v <= hi:
        return 1.0
    d = lo - v if v < lo else v - hi
    return max(0.0, 1.0 - d / soft)


def db(x):
    return 20 * math.log10(max(float(x), 1e-9))


def rms_env(m, win):
    """RMS over `win` samples, one value per `win // 2` hop."""
    hop = max(1, win // 2)
    n = max(1, (len(m) - win) // hop + 1)
    idx = np.arange(n)[:, None] * hop + np.arange(win)[None, :]
    idx = np.minimum(idx, len(m) - 1)
    return np.sqrt(np.mean(m[idx] ** 2, axis=1)), hop


def count_hits(env_db, top_db):
    """Strong hits: rises of 12 dB or more above the quiet before them, to within 20 dB of the top.
    A file is taken as starting from silence."""
    hits, floor, in_hit, hit_peak = 0, -120.0, False, -120.0
    for v in env_db:
        if not in_hit:
            floor = min(floor, v)
            if v > top_db - 20 and v - floor >= 12:
                hits, in_hit, hit_peak = hits + 1, True, v
        else:
            hit_peak = max(hit_peak, v)
            if v < hit_peak - 12:
                in_hit, floor = False, v
    return hits


def features(x, rate=RATE):
    """Objective measures of a one-shot. x: (n, channels) float."""
    if x.ndim == 1:
        x = x[:, None]
    m = x.mean(axis=1)
    peak = float(np.max(np.abs(x))) if len(x) else 0.0
    if peak <= 1e-5 or len(m) < rate // 100:
        return None
    env, hop = rms_env(m, max(16, rate // 400))  # 2.5 ms windows
    top = float(env.max())
    top_db = db(top)
    env_db = 20 * np.log10(np.maximum(env, 1e-9))
    on = int(np.argmax(env > top * 0.1))
    pk = int(np.argmax(env))
    above = np.nonzero(env_db > top_db - 40)[0]
    end = int(above[-1]) if len(above) else on
    pre = env[:max(on - 2, 0)]
    pre_db = db(np.sqrt(np.mean(pre ** 2))) - top_db if len(pre) > 2 else -120.0
    tail = env[-max(4, len(env) // 20):]
    tail_db = db(np.sqrt(np.mean(tail ** 2))) - top_db
    body = m[on * hop:(end + 2) * hop]
    if len(body) < 64:
        body = m[on * hop:]
    spec = np.abs(np.fft.rfft(body * np.hanning(len(body)))) + 1e-12
    f = np.fft.rfftfreq(len(body), 1 / rate)
    p = spec ** 2
    side = (x[:, 0] - x[:, -1]) / 2
    return {
        "peak_db": db(peak), "start": on * hop / rate, "attack": max(0, pk - on) * hop / rate,
        "length": max(0, end - on) * hop / rate, "pre_db": pre_db, "tail_db": tail_db,
        "centroid": float((f * p).sum() / p.sum()), "low": float(p[f < 250].sum() / p.sum()),
        "hf": float(p[f > 8000].sum() / p.sum()),
        "flatness": float(np.exp(np.mean(np.log(spec))) / np.mean(spec)),
        "width": float(np.sqrt(np.mean(side ** 2)) / max(np.sqrt(np.mean(m ** 2)), 1e-9)),
        "clipped": float(np.mean(np.abs(x) > 0.995)), "hits": count_hits(env_db, top_db),
    }


# Per sub-role: (length window s, soft, centroid window Hz, attack max s, max hits)
SHAPES = {
    "closed_hat": ((0.03, 0.16), 0.2, (6000, 12500), 0.004, 1),
    "pedal_hat": ((0.04, 0.22), 0.2, (4500, 11000), 0.006, 1),
    "open_hat": ((0.22, 0.85), 0.5, (5000, 12000), 0.006, 1),
    "ride": ((0.7, 3.5), 1.5, (3500, 10000), 0.008, 1),
    "crash": ((1.0, 4.0), 2.0, (3500, 10000), 0.01, 1),
    "swell": ((0.8, 4.0), 2.0, (3000, 11000), 4.0, 2),
    "shaker": ((0.05, 0.3), 0.25, (4500, 11000), 0.02, 1),
}


def score(role, f, rating=0.0, downloads=0):
    """Higher is better: clean start, right length and brightness for the role, no low rumble, one hit,
    no noise floor, not crushed; a small nudge from what listeners rated."""
    (lo, hi), soft, (clo, chi), att, max_hits = SHAPES[role]
    s = 0.0
    s += 2.0 * in_range(f["start"], 0, 0.006, 0.05) if role != "swell" else 0.0
    s += 2.0 * in_range(f["attack"], 0, att, att * 4 + 0.01) if role != "swell" else 2.0 * in_range(f["attack"], 0.4, 4.0, 0.5)
    s += 3.0 * in_range(f["length"], lo, hi, soft)
    s += 2.0 * in_range(f["centroid"], clo, chi, 3000)
    s += 1.5 * in_range(f["low"], 0, 0.02, 0.1)
    s += 1.0 * in_range(f["hf"], 0.25, 1.0, 0.25)
    s += 1.5 * in_range(f["pre_db"], -120, -50, 20)
    s += 1.0 * in_range(f["tail_db"], -120, -45, 20) if role not in ("ride", "crash", "swell") else 0.5 * in_range(f["tail_db"], -120, -40, 20)
    s -= 3.0 * min(1.0, f["clipped"] * 300)
    s -= 4.0 * max(0, f["hits"] - max_hits)
    s -= 1.5 * min(1.0, max(0.0, f["width"] - 1.0))  # more side than middle: hollow on a phone speaker
    s += 0.5 * min(1.0, rating / 5.0) + 0.5 * min(1.0, math.log10(downloads + 1) / 4)
    return round(s, 3)


BY_PENALTY = 0.75  # CC BY only when clearly better than the CC0 sounds (a credit line to carry)


def pick(cands, keep, per_author=3, exclude=()):
    """The best `keep` by score (CC BY handicapped), at most `per_author` from one author (one series),
    none of the ids a reviewer excluded."""
    out, seen = [], {}
    ranked = sorted(cands, key=lambda c: -(c["score"] - (BY_PENALTY if c["licence"].startswith("CC BY") else 0)))
    for c in ranked:
        a = c["author"]
        if str(c["id"]) in exclude:
            continue
        if seen.get(a, 0) >= per_author:
            continue
        seen[a] = seen.get(a, 0) + 1
        out.append(c)
        if len(out) == keep:
            break
    return out


# ---------------------------------------------------------------- network (counted)
class Net:
    def __init__(self, budget_api, budget_dl):
        self.key = Path(os.environ["FREESOUND_KEY_FILE"]).read_text().strip()
        self.api = self.dl = 0
        self.budget_api, self.budget_dl = budget_api, budget_dl

    def call(self, url, headers=None, data=None, api=True):
        if api:
            self.api += 1
            if self.api > self.budget_api:
                raise SystemExit("API budget spent")
        req = urllib.request.Request(url, data=data, headers={"User-Agent": AGENT, **(headers or {})})
        tries = 3 if api else 5
        for t in range(tries):
            try:
                with urllib.request.urlopen(req, timeout=120) as r:
                    return r.status, r.read()
            except urllib.error.HTTPError as e:
                if e.code in (429, 502, 503) and t < tries - 1:
                    # downloads are throttled harder than searches: honour Retry-After, else back off
                    wait = e.headers.get("Retry-After", "") if e.headers else ""
                    time.sleep(min(300, int(wait)) if wait.isdigit() else 5 * 3 ** t)
                    continue
                return e.code, b""
            except Exception:
                if t == tries - 1:
                    return 0, b""
                time.sleep(3)
        return 0, b""

    def search(self, query, filt, sort, size=100):
        q = urllib.parse.urlencode({"query": query, "filter": filt, "fields": FIELDS, "page_size": size, "sort": sort})
        st, body = self.call(f"{BASE}/apiv2/search/text/?{q}", {"Authorization": f"Token {self.key}"})
        return json.loads(body).get("results", []) if st == 200 else []


def decode(data_or_path, out_rate=RATE):
    """Any audio -> (n, ch) float32 at out_rate, channels kept (max 2)."""
    src = ["-i", str(data_or_path)] if isinstance(data_or_path, (str, Path)) else ["-i", "pipe:0"]
    inp = None if isinstance(data_or_path, (str, Path)) else data_or_path
    probe = subprocess.run(["ffprobe", "-v", "quiet", "-show_entries", "stream=channels", "-of", "csv=p=0", *src[1:]],
                           input=inp, capture_output=True)
    try:
        ch = min(2, int(probe.stdout.decode().split()[0]))
    except Exception:
        ch = 1
    raw = subprocess.run(["ffmpeg", "-v", "quiet", *src, "-ac", str(ch), "-ar", str(out_rate), "-f", "f32le", "-"],
                         input=inp, capture_output=True).stdout
    a = np.frombuffer(raw, np.float32)
    return a.reshape(-1, ch) if ch > 1 else a[:, None]


def cmd_search(rules, work):
    net = Net(rules["budget_api"], 0)
    cands, refused = {}, []
    for role, r in rules["roles"].items():
        lo, hi = r["duration"]
        lic = '"Creative Commons 0" OR "Attribution"' if r.get("allow_by", True) else '"Creative Commons 0"'
        filt = f"license:({lic}) duration:[{lo} TO {hi}] samplerate:[44100 TO 96000]"
        found = {}
        for q in r["queries"]:
            for s in net.search(q, filt, "downloads_desc"):
                found.setdefault(s["id"], s)
        pool = []
        for s in found.values():
            lic = licence_ok(s["license"], r.get("allow_by", True))
            why = None if lic else f"licence {s['license']}"
            why = why or refused_text(s["name"], s.get("description"), s.get("tags"))
            if why:
                refused.append({"id": s["id"], "role": role, "why": why})
                continue
            pool.append((s, lic))
        pool.sort(key=lambda t: -(t[0].get("num_downloads", 0) + 200 * t[0].get("avg_rating", 0)))
        n = 0
        for s, lic in pool:
            if n >= r.get("listen", 40):
                break
            if s["id"] in cands:
                continue
            st, body = net.call(s["previews"]["preview-hq-ogg"], api=False)
            if st != 200:
                continue
            f = features(decode(body))
            if f is None:
                continue
            n += 1
            cands[s["id"]] = {"id": s["id"], "name": s["name"], "author": s["username"], "url": s["url"],
                              "licence": lic, "licence_url": s["license"], "role": role, "type": s["type"],
                              "samplerate": s["samplerate"], "channels": s["channels"], "duration": s["duration"],
                              "filesize": s["filesize"], "rating": s.get("avg_rating", 0), "downloads": s.get("num_downloads", 0),
                              "features": {k: round(v, 4) for k, v in f.items()},
                              "score": score(role, f, s.get("avg_rating", 0), s.get("num_downloads", 0))}
        print(f"{role}: {len(found)} found, {len(pool)} pass the words, {n} measured", flush=True)
    (work / "candidates.json").write_text(json.dumps({"candidates": list(cands.values()), "refused": refused}, indent=1))
    print(f"API requests: {net.api}")


def page_licence(net, url):
    """The licence as the sound's page shows it today (the link in the licence box)."""
    st, body = net.call(url, api=False)
    if st != 200:
        return None
    m = re.findall(r'href="(https?://creativecommons\.org/[^"]+)"', body.decode("utf-8", "replace"))
    return html.unescape(m[0]) if m else None


def process(x, rate, max_s):
    """library_pack.clean, at 48 kHz: trimmed before the hit, cut with a fade, -1 dBFS, mono unless truly stereo."""
    m = x.mean(axis=1)
    peak = np.abs(m).max()
    env = np.abs(m) / max(peak, 1e-9)
    loud = np.nonzero(env > 0.05)[0]
    start = max(0, loud[0] - int(0.002 * rate))
    quiet = np.nonzero(env > 0.001)[0]
    natural_end = min(len(m), quiet[-1] + int(0.02 * rate))
    end = min(natural_end, start + int(max_s * rate))
    out = x[start:end].copy()
    fi = min(int(0.001 * rate), len(out))
    out[:fi] *= np.linspace(0, 1, fi)[:, None]
    fade = min(int((0.25 if end < natural_end else 0.03) * rate), len(out) // 3)
    if fade > 0:
        out[-fade:] *= np.linspace(1, 0, fade)[:, None] ** 2
    out *= 0.89 / np.abs(out).max()
    if out.shape[1] == 2:
        mid = out.mean(axis=1)
        side = (out[:, 0] - out[:, 1]) / 2
        if np.sqrt(np.mean(side ** 2)) < 0.12 * np.sqrt(np.mean(mid ** 2)):
            out = mid[:, None] * (0.89 / max(np.abs(mid).max(), 1e-9))
    return out


def cmd_fetch(rules, work):
    net = Net(rules["budget_api"], rules["budget_dl"])
    cand = load_candidates(work)
    oauth_file = Path(os.environ["FREESOUND_OAUTH_FILE"])
    token = json.loads(oauth_file.read_text())["access_token"]
    (work / "orig").mkdir(exist_ok=True)
    (work / "processed").mkdir(exist_ok=True)
    kept = []
    for role, r in rules["roles"].items():
        best = pick([c for c in cand if c["role"] == role], r["keep"], r.get("per_author", 3), rules.get("exclude_ids", {}))
        for c in best:
            orig = work / "orig" / f"{c['id']}.{c['type']}"
            if not orig.exists():
                if net.dl >= net.budget_dl:
                    raise SystemExit("download budget spent")
                net.dl += 1
                st, body = net.call(f"{BASE}/apiv2/sounds/{c['id']}/download/", {"Authorization": f"Bearer {token}"})
                if st != 200 or len(body) != c["filesize"]:
                    print(f"STOP: original {c['id']} HTTP {st} ({len(body)} bytes)")
                    raise SystemExit(2)
                orig.write_bytes(body)
                time.sleep(4)  # stay well under the per-minute download throttle
            lic_page = page_licence(net, c["url"])
            x = decode(orig)
            y = process(x, RATE, r["max_seconds"])
            name = f"{role}_{len([k for k in kept if k['role'] == role]) + 1:02d}"
            out = work / "processed" / f"{name}.flac"
            subprocess.run(["ffmpeg", "-v", "quiet", "-y", "-f", "f32le", "-ar", str(RATE), "-ac", str(y.shape[1]), "-i", "-",
                            "-sample_fmt", "s16", "-c:a", "flac", str(out)], input=y.astype(np.float32).tobytes(), check=True)
            kept.append({**c, "file": out.name, "sha256": hashlib.sha256(orig.read_bytes()).hexdigest(),
                         "licence_page": lic_page, "styles": r["styles"], "out_channels": y.shape[1],
                         "out_seconds": round(len(y) / RATE, 3)})
            print(f"{role}: {c['id']} {c['name']!r} by {c['author']} -> {out.name} ({y.shape[1]} ch, {len(y)/RATE:.2f} s)", flush=True)
    (work / "kept.json").write_text(json.dumps(kept, indent=1))
    print(f"API requests: {net.api}, downloads: {net.dl}")


def load_candidates(work):
    """The measured candidates, scored again (a change of the scoring needs no new search)."""
    cand = json.loads((work / "candidates.json").read_text())["candidates"]
    for c in cand:
        c["score"] = score(c["role"], c["features"], c["rating"], c["downloads"])
    return cand


def cmd_review(rules, work):
    """The words of the sounds that would be kept (description, tags), for a reader to refuse what measures miss."""
    net = Net(rules["budget_api"], 0)
    cand = load_candidates(work)
    for role, r in rules["roles"].items():
        for c in pick([c for c in cand if c["role"] == role], r["keep"], r.get("per_author", 3), rules.get("exclude_ids", {})):
            st, body = net.call(f"{BASE}/apiv2/sounds/{c['id']}/?fields=description,tags,license,pack_name",
                                {"Authorization": f"Token {net.key}"})
            d = json.loads(body) if st == 200 else {}
            text = " ".join((d.get("description") or "").split())[:300]
            print(f"{role} {c['id']} {c['licence']} {c['author']} | {c['name']} | pack: {d.get('pack_name')} | {text} | {' '.join(d.get('tags', []))[:120]}")
    print(f"API requests: {net.api}")


def main():
    cmd, rules_path, work = sys.argv[1], Path(sys.argv[2]), Path(sys.argv[3])
    rules = json.loads(rules_path.read_text())
    work.mkdir(parents=True, exist_ok=True)
    {"search": cmd_search, "review": cmd_review, "fetch": cmd_fetch}[cmd](rules, work)


if __name__ == "__main__":
    main()
