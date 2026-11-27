"""Self-test of the note-instrument fingerprint on fake sounds whose truth is known by construction.

Each measure AND its counter-measure must find the truth within the tolerance of empreinte.TOL.
Last line: "SELFTEST VERT" or "SELFTEST ROUGE (n échecs)".
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))  # works under python3 -I
import empreinte as E  # noqa: E402

SR = E.SR
RNG = np.random.default_rng(7)  # replaced per draw by run_seed()
FAILS = []


def check(name, key, got, truth, period=0.0):
    ok = E.agree(key, got, truth, period)
    print(f"  {'ok ' if ok else 'NON'} {name:<34} {E.fmt(got):>10}  vérité {E.fmt(truth)}")
    if not ok:
        FAILS.append(name)


def both(name, fp, key, truth, period=0.0):
    m = fp.get(key)
    if m is None:
        print(f"  NON {name:<34} absent")
        FAILS.append(name)
        return
    check(name + " (mesure)", key, m[0], truth, period)
    check(name + " (contre)", key, m[1], truth, period)


def no_vibrato(fp, name):
    """Either nothing seen or declared out of measure, never a vibrato number."""
    a, b = fp["vibrato_cents"][:2]
    ok = a is None and b is None
    print(f"  {'ok ' if ok else 'NON'} {name:<34} {E.fmt(a)} {E.fmt(b)}")
    if not ok:
        FAILS.append(name)


def additive(f0, amps_fn, total, inharm=0.0, voices=((0.0, 1.0),), vib=(0.0, 0.0), n_harm=40):
    """Sum of harmonics. amps_fn(t) -> array (n_harm, len(t)). voices = (cents, gain)."""
    t = np.arange(int(total * SR)) / SR
    a = amps_fn(t)
    out = np.zeros_like(t)
    vib_rate, vib_c = vib
    for cents, g in voices:
        for k in range(1, n_harm + 1):
            fk = k * f0 * math.sqrt(1 + inharm * k * k) * 2 ** (cents / 1200)
            if fk > 20000:
                break
            ph0 = RNG.uniform(0, 2 * math.pi)
            if vib_c:
                # Phase of a tone with sinusoidal pitch modulation of vib_c cents (peak).
                inst = fk * 2 ** (vib_c * np.sin(2 * math.pi * vib_rate * t) / 1200)
                ph = 2 * math.pi * np.cumsum(inst) / SR + ph0
            else:
                ph = 2 * math.pi * fk * t + ph0
            out += g * a[k - 1] * np.sin(ph)
    return out


def adsr(t, attack, tau_d, sustain, t_off, tau_r):
    """Linear attack, exponential decay to sustain, exponential release after t_off."""
    env = np.where(t < attack, t / attack, sustain + (1 - sustain) * np.exp(-(t - attack) / tau_d))
    lvl = sustain + (1 - sustain) * math.exp(-(t_off - attack) / tau_d)
    return np.where(t < t_off, env, lvl * np.exp(-(t - t_off) / tau_r))


def stereo(x, y=None):
    return np.stack([x, x if y is None else y], axis=1)


def saw_amps(n=40, p=1.0):
    return lambda t: np.array([np.full_like(t, k ** -p) for k in range(1, n + 1)])


def truth_power(amps):
    p = np.array(amps[: E.N_HARM]) ** 2
    return p / p.max()


def main():
    total, t_off = 2.5, 1.6

    print("1. dent de scie 110 Hz, enveloppe connue")
    env_fn = lambda t: adsr(t, 0.020, 0.15, 0.4, t_off, 0.08)
    x = additive(110.0, saw_amps(), total) * env_fn(np.arange(int(total * SR)) / SR)
    fp, st = E.fingerprint(stereo(0.3 * x), t_off)
    print("  statut :", st)
    if st != "une note":
        FAILS.append("statut dent de scie")
    both("note (Hz)", fp, "note_hz", 110.0)
    p = truth_power([k ** -1.0 for k in range(1, 17)])
    both("richesse (dB/oct)", fp, "pente_db_oct", E.slope_from_amps(p))
    both("pair/impair (dB)", fp, "pair_impair_db", E.even_odd_from_amps(p))
    both("inharmonicité B", fp, "inharm_b", 0.0)
    both("attaque 10-90 % (s)", fp, "attaque_s", 0.016)
    mid = math.sqrt(1.0 * (0.4 + 0.6 * math.exp(-(t_off - 0.02) / 0.15)))
    both("chute (s)", fp, "chute_s", -0.15 * math.log((mid - 0.4) / 0.6))
    both("relâchement -40 dB (s)", fp, "relache_s", 0.08 * math.log(100))
    both("désaccord (cents)", fp, "desaccord_cents", 0.0)
    both("largeur mono (dB)", fp, "largeur_sm_db", -60.0)
    m = fp["vibrato_cents"]
    ok = m[0] is None and m[1] is None
    print(f"  {'ok ' if ok else 'NON'} pas de vibrato vu                  {m[0]} {m[1]}")
    if not ok:
        FAILS.append("vibrato fantôme")

    print("2. carré 220 Hz (impairs seuls)")
    sq = lambda t: np.array([np.full_like(t, (1.0 / k) if k % 2 else 0.0) for k in range(1, 41)])
    x = additive(220.0, sq, total)
    fp, st = E.fingerprint(stereo(0.2 * x), t_off)
    both("note (Hz)", fp, "note_hz", 220.0)
    p = truth_power([(1.0 / k) if k % 2 else 0.0 for k in range(1, 17)])
    both("richesse (dB/oct)", fp, "pente_db_oct", E.slope_from_amps(p))
    both("pair/impair (dB)", fp, "pair_impair_db", -60.0)

    print("3. sinus doux 55 Hz (basse 808 longue), pente raide")
    x = additive(55.0, saw_amps(p=3.0), total) * env_fn(np.arange(int(total * SR)) / SR)
    fp, st = E.fingerprint(stereo(0.5 * x), t_off)
    both("note (Hz)", fp, "note_hz", 55.0)
    p = truth_power([k ** -3.0 for k in range(1, 17)])
    both("richesse (dB/oct)", fp, "pente_db_oct", E.slope_from_amps(p))

    print("4. corde inharmonique 220 Hz, B = 4e-4")
    x = additive(220.0, saw_amps(p=1.5), total, inharm=4e-4)
    fp, st = E.fingerprint(stereo(0.2 * x), t_off)
    both("inharmonicité B", fp, "inharm_b", 4e-4)

    print("5. deux voix désaccordées ±8 cents, 147 Hz")
    x = additive(147.0, saw_amps(p=1.2), total, voices=((-8.0, 0.7), (8.0, 0.7)))
    fp, st = E.fingerprint(stereo(0.2 * x), t_off)
    both("note (Hz)", fp, "note_hz", 147.0)
    both("désaccord (cents)", fp, "desaccord_cents", 8.0)
    no_vibrato(fp, "pas de vibrato vu dans les battements")

    print("5b. trois voix désaccordées ±12 cents, 65 Hz (reese grave), attaque 30 ms")
    t = np.arange(int(total * SR)) / SR
    x = additive(65.4, saw_amps(p=1.0), total, voices=((-12.0, 0.6), (0.0, 0.6), (12.0, 0.6)))
    fp, st = E.fingerprint(stereo(0.2 * x * adsr(t, 0.030, 0.2, 0.7, t_off, 0.1)), t_off)
    no_vibrato(fp, "pas de vibrato vu dans les battements")
    both("attaque 10-90 % (s)", fp, "attaque_s", 0.024, 1 / 65.4)

    print("6. vibrato 5.5 Hz, ±20 cents, 262 Hz")
    x = additive(262.0, saw_amps(p=1.5), total, vib=(5.5, 20.0))
    fp, st = E.fingerprint(stereo(0.2 * x), t_off)
    both("vibrato (Hz)", fp, "vibrato_hz", 5.5)
    both("vibrato (cents)", fp, "vibrato_cents", 20.0)

    print("7. largeur stéréo : L = a + 0.3 b, R = a - 0.3 b")
    a = additive(110.0, saw_amps(p=1.2), total)
    b = additive(110.0, saw_amps(p=1.2), total)
    sst = stereo(0.2 * (a + 0.3 * b), 0.2 * (a - 0.3 * b))
    seg = sst[int(0.25 * SR): int((t_off - 0.05) * SR)]
    s, mm = seg[:, 0] - seg[:, 1], seg[:, 0] + seg[:, 1]
    fp, st = E.fingerprint(sst, t_off)
    both("largeur S/M (dB)", fp, "largeur_sm_db", 10 * math.log10((s * s).sum() / (mm * mm).sum()))

    print("8. souffle : harmoniques + bruit blanc filtré < 11 kHz, HNR 15 dB")
    h = additive(196.0, saw_amps(p=1.5, n=50), total)
    spec = np.fft.rfft(RNG.standard_normal(len(h)))
    spec[np.fft.rfftfreq(len(h), 1 / SR) > 11000] = 0
    nz = np.fft.irfft(spec, len(h))
    nz *= math.sqrt((h * h).mean() / (nz * nz).mean()) * 10 ** (-15 / 20)
    fp, st = E.fingerprint(stereo(0.2 * (h + nz)), t_off)
    both("souffle HNR (dB)", fp, "souffle_hnr_db", 15.0)

    print("9. enveloppe de filtre : brillance qui retombe (tau 0.15 s)")
    f0, ca, cs, tau = 110.0, 12.0, 3.0, 0.15

    def filt(t):
        c = cs + (ca - cs) * np.exp(-t / tau)
        return np.array([np.exp(-k / c) for k in range(1, 41)])

    x = additive(f0, filt, total) * adsr(np.arange(int(total * SR)) / SR, 0.002, 1.0, 1.0, t_off, 0.05)
    fp, st = E.fingerprint(stereo(0.2 * x), t_off)
    tt = np.arange(0, t_off, 0.001)
    aa = filt(tt)
    kk = np.arange(1, 41)[:, None]
    br = np.sqrt((kk * kk * aa * aa).sum(0) / (aa * aa).sum(0))
    both("brillance attaque (harm.)", fp, "brillance_attaque", float(br[:60].mean()))
    both("brillance tenue (harm.)", fp, "brillance_tenue", float(br[-300:].mean()))
    top, held = float(br[:150].max()), float(br[-300:].mean())
    both("chute du filtre (s)", fp, "chute_filtre_s", float(np.argmax(br <= (top + held) / 2)) / 1000)

    print("10. nappe : brillance qui ondule (période 2.5 s), note tenue 8 s")
    tot2, off2 = 9.0, 8.5

    def wob(t):
        c = 5.0 * (1 + 0.3 * np.sin(2 * math.pi * t / 2.5))
        return np.array([np.exp(-k / c) for k in range(1, 41)])

    x = additive(220.0, wob, tot2)
    fp, st = E.fingerprint(stereo(0.2 * x), off2, pad=True)
    tt = np.arange(1.0, 8.0, 0.005)
    aa = wob(tt)
    br = np.sqrt((kk * kk * aa * aa).sum(0) / (aa * aa).sum(0))
    both("évolution (%)", fp, "evolution_pct",
         100 * float(np.percentile(br, 95) - np.percentile(br, 5)) / float(np.median(br)))
    both("évolution période (s)", fp, "evolution_periode_s", 2.5)

    print("11. accord do-mi-sol et bruit seul : pas de note")
    ch = sum(additive(f, saw_amps(), total) for f in (261.6, 329.6, 392.0))
    fp, st = E.fingerprint(stereo(0.1 * ch), t_off)
    print("  accord :", st)
    if st == "une note":
        FAILS.append("accord pris pour une note")
    fp, st = E.fingerprint(stereo(0.1 * RNG.standard_normal(int(total * SR))), t_off)
    print("  bruit  :", st)
    if st == "une note":
        FAILS.append("bruit pris pour une note")

    print(f"  tirage : {len(FAILS)} échecs" if FAILS else "  tirage vert")
    return 1 if FAILS else 0


SEEDS = (1, 3, 7, 11, 19, 23, 42, 101)


def run_seed(seed):
    global RNG
    RNG = np.random.default_rng(seed)
    FAILS.clear()
    print(f"=== tirage de phases, graine {seed}")
    main()
    return list(FAILS)


if __name__ == "__main__":
    seeds = [int(a) for a in sys.argv[1:]] or list(SEEDS)
    red = {}
    for sd in seeds:
        f = run_seed(sd)
        if f:
            red[sd] = f
    for sd, f in red.items():
        print(f"graine {sd} : {len(f)} échecs : {', '.join(f)}")
    if red:
        print(f"SELFTEST ROUGE ({len(red)} tirages rouges sur {len(seeds)})")
        sys.exit(1)
    print(f"SELFTEST VERT ({len(seeds)} tirages)")
