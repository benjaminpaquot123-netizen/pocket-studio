"""Final zone-A targets (docs/sons/fin/cibles/zone-a-kicks-boucles.tsv) measured
with the same code that built them (scripts/son/essentia/cibles_desc.py).

Score = RMS of z-scores (z = (x - p50) / (IQR / 1.349), clipped at 4) over the
descriptors that mean something for the family, plus hinge terms from the blind
critique: a kick must have a noisy strike (flatness_db at least p25 of its case,
'hollow' = near-pure sine), and its -20 dB tail must not run past the case p50;
a loop must not be dark (centroid and high4k_db at least p25)."""
import csv, os, subprocess, sys
import numpy as np

SR = 44100
OUT_PEAK = 10 ** (-1.0 / 20)
CASE_K = {"trap": "*", "drill": "*", "boom-bap": "*", "house": "house", "techno": "techno",
          "drum-and-bass": "dnb", "edm": "*", "reggaeton-afro": "*", "lo-fi": "lofi"}
CASE_L = {"trap": "*", "drill": "*", "boom-bap": "*", "house": "house", "techno": "techno",
          "drum-and-bass": "dnb", "edm": "*", "reggaeton-afro": "*", "lo-fi": "*"}
DK = {"dur_s": 0.5, "attack_ms": 0.5, "decay20_s": 1, "crest_db": 1, "lufs": 1, "sub120": 1, "low300": 0.5,
      "high4k_db": 0.5, "centroid_hz": 1, "rolloff_hz": 0.5, "flatness_db": 1.5, "lowflux": 0.5}
DL = {"decay20_s": 0.5, "crest_db": 1, "lufs": 1, "sub120": 1, "low300": 0.5, "high4k_db": 1, "centroid_hz": 1,
      "rolloff_hz": 1, "flatness_db": 1, "width_db": 1, "lowflux": 0.5, "low_beat": 0.5}
T = {}
CD = None


def load(tsv, desc_dir):
    global CD
    sys.path.insert(0, desc_dir)
    import cibles_desc
    CD = cibles_desc
    with open(tsv, newline="") as f:
        for r in csv.DictReader(f, delimiter="\t"):
            q = []
            for k in ("p10", "p25", "p50", "p75", "p90"):
                try:
                    q.append(float(r[k]))
                except ValueError:
                    q.append(None)  # 'mono'
            T.setdefault((r["famille"], r["style"]), {})[r["descripteur"]] = q


def measure_st(st):
    st = np.asarray(st, dtype=np.float64)
    if st.ndim == 1:
        st = np.repeat(st[:, None], 2, 1)
    st = st / max(np.abs(st).max(), 1e-9) * OUT_PEAK
    m = CD.measure(st.astype(np.float32))
    m["high4k_db"] = 10 * np.log10(m["high4k"] + 1e-12)
    return m


def zs(m, fam, case, w):
    t = T[(fam, case)]
    z = {}
    for d, wt in w.items():
        q = t.get(d)
        if not q or q[2] is None or q[1] is None or m.get(d) is None:
            continue
        sd = (q[3] - q[1]) / 1.349 or (q[4] - q[0]) / 2.563 or max(abs(q[2]) * 0.05, 1e-6)
        x = m[d]
        if d == "width_db":
            x = max(x, q[1] - 2 * sd)
        z[d] = (x - q[2]) / sd
    return z


def score(m, fam, case, kind):
    w = DK if kind == "kick" else DL
    z = zs(m, fam, case, w)
    s = np.sqrt(sum(w[k] * min(abs(v), 4.0) ** 2 for k, v in z.items()) / sum(w[k] for k in z))
    t = T[(fam, case)]
    if kind == "kick":
        s += min(3.0, max(0.0, (t["flatness_db"][1] - m["flatness_db"]) / 10)) ** 2
        s += min(3.0, max(0.0, (m["decay20_s"] - t["decay20_s"][2]) / 0.05)) ** 2
    else:
        s += 0.5 * min(3.0, max(0.0, np.log(t["centroid_hz"][1] / max(m["centroid_hz"], 1)) / 0.3)) ** 2
        s += 0.5 * min(3.0, max(0.0, (t["high4k_db"][1] - m["high4k_db"]) / 3)) ** 2
    return float(s), z


def zone(m, fam, case, w):
    t = T[(fam, case)]
    a = b = n = 0
    for d in w:
        q = t.get(d)
        if not q or None in q or m.get(d) is None:
            continue
        n += 1
        a += q[1] <= m[d] <= q[3]
        b += q[0] <= m[d] <= q[4]
    return "%d/%d" % (a, n), "%d/%d" % (b, n)


def to_mp3_st(st, path):
    st = np.asarray(st, dtype=np.float32)
    if st.ndim == 1:
        st = np.repeat(st[:, None], 2, 1)
    st = st / max(np.abs(st).max(), 1e-9) * OUT_PEAK
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", "2", "-i", "-",
                    "-codec:a", "libmp3lame", "-b:a", "192k", path], input=np.ascontiguousarray(st).tobytes(), check=True)


def report(path, fam, case, kind):
    """Remeasure the exported mp3 with cibles_desc's own decoder (same recipe as the targets)."""
    with open(path, "rb") as fh:
        m = CD.from_bytes(fh.read())
    m["high4k_db"] = 10 * np.log10(m["high4k"] + 1e-12)
    s, z = score(m, fam, case, kind)
    w = DK if kind == "kick" else DL
    a, b = zone(m, fam, case, w)
    t = T[(fam, case)]
    rk = {d: rank(m[d], t[d]) for d in ("flatness_db", "decay20_s", "centroid_hz", "high4k_db", "sub120", "lufs")
          if d in t and m.get(d) is not None}
    return dict(target="%s/%s" % (fam, case), score=round(s, 2), in_p25_p75=a, in_p10_p90=b, ranks=rk,
                measured={k: (round(v, 4) if isinstance(v, float) else v) for k, v in m.items() if k != "path"},
                worst=sorted(((k, round(v, 1)) for k, v in z.items()), key=lambda u: -abs(u[1]))[:3])


def rank(x, q):
    """approximate percentile of x inside the case (from p10..p90)."""
    ps = [10, 25, 50, 75, 90]
    pts = [(p, v) for p, v in zip(ps, q) if v is not None]
    if not pts:
        return None
    if x <= pts[0][1]:
        return "<p%d" % pts[0][0]
    if x >= pts[-1][1]:
        return ">p%d" % pts[-1][0]
    for (p0, v0), (p1, v1) in zip(pts, pts[1:]):
        if v0 <= x <= v1:
            return "p%d" % round(p0 + (p1 - p0) * (x - v0) / max(v1 - v0, 1e-12))
