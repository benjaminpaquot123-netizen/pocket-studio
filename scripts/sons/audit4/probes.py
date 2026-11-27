"""Audit 4: probes of tout.py through the real CLI (synthetic sounds, work dir outside the repo).

Run: AUDIT2_WORK=/some/dir/outside/repo python3 -I scripts/sons/audit4/probes.py
Prints one line per case: label, expected, obtained, PASS/FAIL. Exit code = number of FAIL.
"""
import math, os, shutil, struct, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(HERE), "audit2"))
from lib import *  # noqa: F401,F403  (S, SR, generators, writers, run_pack, tout)
from lib import _vlq

FAIL = 0


def pack(name):
    root = os.path.join(S, "a4", name)
    shutil.rmtree(root, ignore_errors=True)
    os.makedirs(root, exist_ok=True)
    return root


def wav(name, rel, x, sr=SR, **kw):
    root = pack(name)
    write_wav(os.path.join(root, *rel.split("/")), x, sr, **kw)
    return run_pack(root, name)


def midi(name, rel, notes, bpm=None, ppq=480, extra=b""):
    root = pack(name)
    write_midi(os.path.join(root, *rel.split("/")), notes, bpm, ppq, extra)
    return run_pack(root, name)


def check(label, rows, code, col, ok, want):
    global FAIL
    got = rows[0].get(col) if rows else "NO ROW (%s)" % code
    try:
        good = bool(rows) and ok(got)
    except Exception:
        good = False
    FAIL += not good
    print("%s | %-58s | %-9s | want %-22s | got %s" % ("PASS" if good else "FAIL", label, col, want, got))


def num(v):
    return float(v) if v not in (None, "") else None


def ref_lufs(x, sr):
    """Reference BS.1770 with linear (non circular) filtering and full gating."""
    n = x.shape[0]
    nf = 1 << int(math.ceil(math.log2(2 * n + 64)))
    f = np.fft.rfftfreq(nf, 1.0 / sr)
    h = tout._biquad_resp(f, sr, "shelf") * tout._biquad_resp(f, sr, "hp")
    y = np.fft.irfft(np.fft.rfft(x, nf, axis=0) * h[:, None], nf, axis=0)[:n]
    p = np.sum(y ** 2, axis=1)
    blk = int(round(0.4 * sr)); hop = blk // 4
    z = np.array([np.mean(p[s:s + blk]) for s in range(0, len(p) - blk + 1, hop)])
    lk = -0.691 + 10 * np.log10(np.maximum(z, 1e-30))
    z1 = z[lk > -70]
    if not len(z1):
        return None
    rel = -0.691 + 10 * math.log10(np.mean(z1)) - 10
    return -0.691 + 10 * math.log10(np.mean(z[(lk > -70) & (lk > rel)]))


# ---- level ----------------------------------------------------------------
# circular wrap of k_weight when the length is an exact power of two:
# a loud bass hit at the very end leaks its filter tail into the start
for n_len, lab in ((1 << 16, "2^16"), ((1 << 16) + 7, "2^16+7")):
    x = np.zeros(n_len)
    x[-4000:] = 0.9 * np.sin(2 * np.pi * 40 * np.arange(4000) / SR)
    x[:20000] += 0.001 * np.sin(2 * np.pi * 1000 * np.arange(20000) / SR)
    r = ref_lufs(x[:, None], SR)
    rows, con, code = wav("lufs_" + lab.replace("^", "p").replace("+", "_"), "Synth/Bass/Sub Hit C1.wav", x)
    check("LUFS, 40 Hz hit at end, len %s (ref %.2f)" % (lab, r), rows, code, "lufs_i",
          lambda g: abs(num(g) - r) <= 0.5, "%.2f +-0.5" % r)

# short-file branch (< 0.4 s) has no absolute -70 gate: a -90 dBFS tick is reported
x = 10 ** (-90 / 20) * sine(1000.0, 0.3, 1.0)
rows, con, code = wav("lufs_short_quiet", "Drums/Hats/Hat 01.wav", x, bits=24)
check("LUFS of a 0.3 s file at -90 dBFS (gate -70)", rows, code, "lufs_i", lambda g: g in ("", None), "empty (gated)")

# ---- one-shots ------------------------------------------------------------
rows, con, code = wav("silent_lead", "Synth/Leads/Lead C3.wav", np.zeros(SR))
check("silent melodic one-shot: no crash, no note", rows, code, "error", lambda g: "IndexError" not in str(g), "no IndexError")

# ---- melodic loop shorter than one chroma frame ---------------------------
x = tone(261.63, 0.2, 0.5)
rows, con, code = wav("short_loop", "Loops/Melodic/Keys Loop 120 BPM C major.wav", x)
check("0.2 s melodic loop: a row without a crash", rows, code, "error", lambda g: g in ("", None), "empty error")

# ---- MIDI, audit 2 defects 22 and 37 --------------------------------------
four = [(32 + b * 4 + q, 0.9, 60 + (q % 3) * 4, 100, 0) for b in range(4) for q in range(4)]
rows, con, code = midi("d22_bar9", "MIDI/Chords 120 BPM C major.mid", four, 120)
check("D22: 4 bars exported from bar 9", rows, code, "bars", lambda g: g == "4", "4")

ch = struct.pack(">I", int(60e6 / 140))[1:]
tempo_change = b"\x00\xff\x51\x03" + struct.pack(">I", int(60e6 / 100))[1:]
notes = [(b, 0.9, 60, 100, 0) for b in range(16)]
# 2nd tempo at beat 8 (ticks 3840) written in a second track would be complex; use a meta at delta 0 then notes:
root = pack("d37_change")
p = os.path.join(root, "MIDI", "Lead Melody C major.mid")
os.makedirs(os.path.dirname(p), exist_ok=True)
trk = tempo_change
last = 0
for t, kind, msg in sorted([(int(b * 480), 1, bytes([0x90, 60, 100])) for b in range(16)] +
                           [(int(b * 480 + 430), 0, bytes([0x80, 60, 0])) for b in range(16)] +
                           [(8 * 480, -1, b"\xff\x51\x03" + ch)], key=lambda e: (e[0], e[1])):
    trk += _vlq(t - last) + msg
    last = t
trk += b"\x00\xff\x2f\x00"
open(p, "wb").write(b"MThd" + struct.pack(">IHHH", 6, 0, 1, 480) + b"MTrk" + struct.pack(">I", len(trk)) + trk)
rows, con, code = run_pack(root, "d37_change")
check("D37: tempo 100 then 140 at beat 8 -> no single bpm", rows, code, "bpm_est",
      lambda g: g in ("", None), "empty (two tempos)")
check("D37: tempo change, bars still 4", rows, code, "bars", lambda g: g == "4", "4")

# ---- drum grid: band bleed with a threshold relative to the band maximum ---
def hatloop(hamp, snare_on):
    beat = 0.5
    buf = np.zeros(int(8 * beat * SR))
    for b in range(2):
        o = b * 4 * beat
        for k in range(4):
            place(buf, kick(0.3), o + k * beat)
        if snare_on:
            for s in (1, 3):
                place(buf, snare(0.2), o + s * beat)
        for i in range(8):
            place(buf, hat(0.05) * hamp, o + i * 0.5 * beat)
    return np.clip(buf * 0.8, -1, 1)


rows, con, code = wav("grid_hats_snare", "Loops/Drums/Drum Loop 120 BPM.wav", hatloop(0.5, True))
check("8th hats 6 dB under the snare: hats grid", rows, code, "grid_hats",
      lambda g: g == "x.x.x.x.x.x.x.x.", "x.x.x.x.x.x.x.x.")
rows, con, code = wav("grid_no_snare", "Loops/Drums/Drum Loop 120 BPM.wav", hatloop(0.5, False))
check("kick + hats, no snare at all: snare grid", rows, code, "grid_snare",
      lambda g: "x" not in g, "no x")

# ---- MIDI waltz with a note on every beat, no time signature ---------------
waltz = [(b * 3 + q, 0.9, 48 if q == 0 else 64, 110 if q == 0 else 70, 0) for b in range(4) for q in range(3)]
rows, con, code = midi("d37_waltz", "MIDI/Waltz Piano C major.mid", waltz, 120)
check("D37: 3/4 without time signature -> bars not guessed", rows, code, "bars", lambda g: g in ("", None, "4"), "empty or 4")

ts34 = b"\x00\xff\x58\x04\x03\x02\x18\x08"
rows, con, code = midi("d37_waltz_ts", "MIDI/Waltz Piano C major.mid", waltz, 120, extra=ts34)
check("3/4 with time signature -> 4 bars", rows, code, "bars", lambda g: g == "4", "4")

# swing from MIDI ticks: straight 16ths = 50, swung 16ths (2/3) = 67
st = [(i * 0.25, 0.2, 42, 90, 9) for i in range(32)]
rows, con, code = midi("swing_straight", "MIDI/Drums/Hats 120 BPM.mid", st, 120)
check("MIDI straight 16ths swing", rows, code, "swing_pct", lambda g: abs(num(g) - 50) < 2, "50")
sw = [((i // 2) * 0.5 + (i % 2) * 0.5 * 0.667, 0.15, 42, 90, 9) for i in range(32)]
rows, con, code = midi("swing_67", "MIDI/Drums/Hats 120 BPM.mid", sw, 120)
check("MIDI swung 16ths swing", rows, code, "swing_pct", lambda g: abs(num(g) - 66.7) < 2, "66.7")

print("FAIL=%d" % FAIL)
sys.exit(min(FAIL, 100))
