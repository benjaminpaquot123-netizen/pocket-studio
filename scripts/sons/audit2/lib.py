"""Own generators + runner for the independent audit (nothing from the tool's self-tests)."""
import contextlib, csv, glob, io, os, shutil, struct, sys, tempfile
import numpy as np

# work folder for the synthetic packs and the tables: always outside the repository
S = os.environ.get("AUDIT2_WORK") or os.path.join(tempfile.gettempdir(), "audit2_tout")
os.makedirs(os.path.join(S, "out"), exist_ok=True)
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import tout  # noqa: E402

SR = 48000
np.seterr(all="ignore")


def T(d, sr=SR):
    return np.arange(int(round(d * sr))) / sr


def sine(f, d, a=0.5, sr=SR):
    return a * np.sin(2 * np.pi * f * T(d, sr))


def noise(d, a=0.5, seed=1, sr=SR):
    return a * np.random.default_rng(seed).uniform(-1, 1, int(round(d * sr)))


def hp(x):  # crude high-pass (twice differentiated)
    y = np.diff(x, prepend=0.0)
    y = np.diff(y, prepend=0.0)
    return y / (np.max(np.abs(y)) + 1e-12)


def kick(d=0.4, f0=160.0, f1=50.0, taup=0.03, taua=0.10, a=0.9, sr=SR):
    t = T(d, sr)
    f = f1 + (f0 - f1) * np.exp(-t / taup)
    ph = 2 * np.pi * np.cumsum(f) / sr
    return a * np.sin(ph) * np.exp(-t / taua)


def snare(d=0.3, a=0.8, seed=2, sr=SR):
    t = T(d, sr)
    return a * (0.7 * noise(d, 1.0, seed, sr) * np.exp(-t / 0.05) + 0.3 * np.sin(2 * np.pi * 190 * t) * np.exp(-t / 0.04))


def hat(d=0.08, tau=0.012, a=0.6, seed=3, sr=SR):
    t = T(d, sr)
    return a * hp(noise(d, 1.0, seed, sr)) * np.exp(-t / tau)


def clap(d=0.3, a=0.8, seed=4, sr=SR):
    t = T(d, sr)
    env = sum(np.where(t >= o, np.exp(-(t - o) / 0.02), 0) for o in (0, 0.01, 0.02, 0.03))
    return a * hp(noise(d, 1.0, seed, sr)) * env / 2.0


def b808(f=55.0, d=1.6, tau=0.45, a=0.9, sr=SR):
    t = T(d, sr)
    return a * np.sin(2 * np.pi * f * t) * np.exp(-t / tau)


def tone(f, d, a=0.5, harm=6, sr=SR):
    t = T(d, sr)
    return a * sum(np.sin(2 * np.pi * f * k * t) / k for k in range(1, harm + 1)) / 1.8


def fade(x, ms, sr=SR, kind="lin"):
    n = int(ms * sr / 1000)
    y = x.copy()
    if n > 0:
        r = np.linspace(1, 0, n) if kind == "lin" else np.cos(np.linspace(0, np.pi / 2, n)) ** 2
        y[-n:] = y[-n:] * (r if y.ndim == 1 else r[:, None])
    return y


def place(buf, x, at, sr=SR):
    i = int(round(at * sr))
    n = min(len(x), len(buf) - i)
    if n > 0:
        buf[i:i + n] += x[:n]


def drum_loop(bpm, bars=2, kind="four", hats="8th", swing=0.0, sr=SR, tail=False):
    beat = 60.0 / bpm
    d = bars * 4 * beat
    buf = np.zeros(int(round(d * sr)) + (int(sr * 0.3) if tail else 0))
    for b in range(bars):
        o = b * 4 * beat
        if kind == "four":
            ks, ss = [0, 1, 2, 3], [1, 3]
        elif kind == "boombap":
            ks, ss = [0, 2.5], [1, 3]
        elif kind == "half":
            ks, ss = [0, 2.75], [2]
        elif kind == "dnb":
            ks, ss = [0, 2.5], [1, 3]
        elif kind == "waltz":
            ks, ss = [0], [1, 2]
        else:
            ks, ss = [], []
        nb = 3 if kind == "waltz" else 4
        if kind == "waltz":
            o = b * 3 * beat
        for k in ks:
            place(buf, kick(0.3, sr=sr), o + k * beat, sr)
        for s in ss:
            place(buf, snare(0.2, sr=sr), o + s * beat, sr)
        if hats != "none":
            step = {"8th": 0.5, "16th": 0.25, "off": 1.0, "trip": 1 / 3}[hats]
            n = int(round(nb / step))
            for i in range(n):
                pos = i * step + (0.5 if hats == "off" else 0)
                if swing and hats == "16th" and i % 2 == 1:
                    pos += swing * step
                place(buf, hat(0.05, sr=sr) * 0.5, o + pos * beat, sr)
    if kind == "waltz":
        buf = buf[:int(round(bars * 3 * beat * sr))]
    return np.clip(buf * 0.8, -1, 1)


NOTE = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}


def mhz(m):
    return 440.0 * 2 ** ((m - 69) / 12.0)


def chord_loop(bpm, bars=4, chords=None, pluck=True, sr=SR, with_drums=False):
    chords = chords or [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]  # Am F C G
    beat = 60.0 / bpm
    d = bars * 4 * beat
    buf = np.zeros(int(round(d * sr)))
    for b in range(bars):
        ch = chords[b % len(chords)]
        for q in range(4 if pluck else 1):
            ln = beat if pluck else 4 * beat
            t = T(ln, sr)
            env = np.exp(-t / (0.25 if pluck else 5.0)) * np.minimum(1, t / 0.005) * np.minimum(1, (ln - t) / 0.01)
            x = sum(tone(mhz(m), ln, 0.2, 5, sr) for m in ch) * env
            place(buf, x, (b * 4 + q * (1 if pluck else 4)) * beat, sr)
        place(buf, tone(mhz(ch[0] - 12), 4 * beat, 0.25, 3, sr) * np.minimum(1, (4 * beat - T(4 * beat, sr)) / 0.01), b * 4 * beat, sr)
    if with_drums:
        buf = buf + 0.6 * drum_loop(bpm, bars, sr=sr)[:len(buf)]
    return np.clip(buf, -1, 1)


def vox(d=3.0, f0=220.0, sr=SR):
    t = T(d, sr)
    f = f0 * (1 + 0.01 * np.sin(2 * np.pi * 5.5 * t))
    ph = 2 * np.pi * np.cumsum(f) / sr
    x = sum(np.sin(k * ph) / k * (1.0 if abs(k * f0 - 700) > 300 else 2.5) for k in range(1, 12))
    env = np.minimum(1, t / 0.05) * np.minimum(1, (d - t) / 0.2)
    return 0.3 * x * env


def riser(d=4.0, sr=SR):
    t = T(d, sr)
    return 0.5 * noise(d, 1.0, 9, sr) * (t / d) ** 2


def pcm_bytes(x, bits=16, big=False):
    x = np.clip(x, -1, 1)
    if bits == 16:
        return (x * 32767).astype(">i2" if big else "<i2").tobytes()
    if bits == 24:
        v = (x * 8388607).astype(np.int32).reshape(-1)
        b = np.empty((v.size, 3), dtype=np.uint8)
        if big:
            b[:, 0] = (v >> 16) & 255; b[:, 1] = (v >> 8) & 255; b[:, 2] = v & 255
        else:
            b[:, 0] = v & 255; b[:, 1] = (v >> 8) & 255; b[:, 2] = (v >> 16) & 255
        return b.tobytes()
    if bits == 8:
        return ((x * 127) + 128).astype(np.uint8).tobytes()
    if bits == 32:
        return (x * 2147483647).astype(">i4" if big else "<i4").tobytes()
    raise ValueError(bits)


def write_wav(path, x, sr=SR, bits=16, fl=False, extra=b"", smpl_loop=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    x = np.asarray(x, dtype=np.float64)
    ch = 1 if x.ndim == 1 else x.shape[1]
    if fl:
        data = x.astype("<f4").tobytes(); fmt_tag = 3; bits = 32
    else:
        data = pcm_bytes(x, bits); fmt_tag = 1
    fmt = struct.pack("<HHIIHH", fmt_tag, ch, sr, sr * ch * bits // 8, ch * bits // 8, bits)
    chunks = b"fmt " + struct.pack("<I", len(fmt)) + fmt + extra
    if smpl_loop:
        n = x.shape[0]
        sm = struct.pack("<9I", 0, 0, int(1e9 / sr), 60, 0, 0, 0, 1, 0) + struct.pack("<6I", 0, 0, 0, n - 1, 0, 0)
        chunks += b"smpl" + struct.pack("<I", len(sm)) + sm
    chunks += b"data" + struct.pack("<I", len(data)) + data + (b"\x00" if len(data) % 2 else b"")
    with open(path, "wb") as fh:
        fh.write(b"RIFF" + struct.pack("<I", 4 + len(chunks)) + b"WAVE" + chunks)


def _ext80(v):
    import math
    if v == 0:
        return b"\x00" * 10
    e = int(math.floor(math.log2(v)))
    m = int(v / 2 ** e * (1 << 63))
    return struct.pack(">HQ", 16383 + e, m)


def write_aiff(path, x, sr=SR, bits=16):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    x = np.asarray(x, dtype=np.float64)
    ch = 1 if x.ndim == 1 else x.shape[1]
    data = pcm_bytes(x, bits, big=True)
    comm = struct.pack(">hIh", ch, x.shape[0], bits) + _ext80(sr)
    ssnd = struct.pack(">II", 0, 0) + data
    body = b"AIFF" + b"COMM" + struct.pack(">I", len(comm)) + comm + b"SSND" + struct.pack(">I", len(ssnd)) + ssnd
    with open(path, "wb") as fh:
        fh.write(b"FORM" + struct.pack(">I", len(body)) + body)


def _vlq(n):
    out = [n & 0x7F]
    n >>= 7
    while n:
        out.append((n & 0x7F) | 0x80)
        n >>= 7
    return bytes(reversed(out))


def write_midi(path, notes, bpm=None, ppq=480, extra_meta=b""):
    """notes: (start_beat, len_beat, pitch, vel, channel0)."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    ev = []
    for s, l, p, v, c in notes:
        ev.append((int(round(s * ppq)), 1, bytes([0x90 | c, p, v])))
        ev.append((int(round((s + l) * ppq)), 0, bytes([0x80 | c, p, 0])))
    ev.sort(key=lambda e: (e[0], e[1]))
    trk = b""
    if bpm:
        trk += b"\x00\xff\x51\x03" + struct.pack(">I", int(60e6 / bpm))[1:]
    trk += extra_meta
    last = 0
    for tick, _, b in ev:
        trk += _vlq(tick - last) + b
        last = tick
    trk += b"\x00\xff\x2f\x00"
    with open(path, "wb") as fh:
        fh.write(b"MThd" + struct.pack(">IHHH", 6, 0, 1, ppq) + b"MTrk" + struct.pack(">I", len(trk)) + trk)


FAULTS = []


def fault(msg):
    """A real fault seen by a batch: the replay ends with exit code 1 (audit 3, defect 14)."""
    FAULTS.append(msg)


def off(cell, true, tol):
    """True only for a value written in the table and off by more than tol (relative);
    an empty cell is a fenced guess, not a wrong value."""
    if cell in (None, ""):
        return False
    return abs(float(cell) - true) > tol * abs(true)


def done():
    """End of a batch: say the faults and exit 1 if there is any."""
    print("FAULTS: %d" % len(FAULTS))
    for f in FAULTS[:20]:
        print("  fault: " + f)
    sys.exit(1 if FAULTS else 0)


def run_pack(root, tag="t"):
    """Run the real CLI on a pack folder; return rows (all tables, in order) + console text."""
    out = os.path.join(S, "out", "run_" + tag)
    for f in glob.glob(out + "_*.csv"):
        os.remove(f)
    buf = io.StringIO()
    code = None
    with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
        try:
            code = tout.main([root, "--rar", "1", "--out", out])
        except SystemExit as e:
            code = "exit %s" % e.code
        except Exception as e:
            code = "CRASH %s: %s" % (type(e).__name__, e)
            fault("%s: %s" % (root, code))
    rows = []
    for f in sorted(glob.glob(out + "_*.csv")):
        if f.endswith("_accuracy.csv"):
            continue
        with open(f, newline="", encoding="utf-8") as fh:
            rows += list(csv.DictReader(fh))
    return rows, buf.getvalue().strip(), code


def one(batch, idx, rel, x, sr=SR, writer=None, **kw):
    """One file = one pack (so the row can be traced: the tables carry no file name)."""
    root = os.path.join(S, "pack", batch, "%03d" % idx)
    shutil.rmtree(root, ignore_errors=True)
    p = os.path.join(root, *rel.split("/"))
    (writer or write_wav)(p, x, sr, **kw) if writer is not write_midi else None
    rows, con, code = run_pack(root, batch)
    return rows, con, code


def show(rel, rows, cols, con="", code=0):
    if not rows:
        print("%-52s -> NO ROW (%s) %s" % (rel, code, con[:80]))
        return
    for r in rows:
        print("%-52s -> %s" % (rel, " | ".join("%s=%s" % (c, r.get(c, "?")) for c in cols if r.get(c, "") != "" or c in ("type", "subtype", "role"))))
