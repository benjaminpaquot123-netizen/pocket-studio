"""Audit 4: speed of tout.py on N varied synthetic files (8 s loops, one-shots, MIDI).

Run: AUDIT2_WORK=/dir/outside/repo python3 -I scripts/sons/audit4/speed.py 500
Prints files, seconds, seconds per file and the extrapolation to 2000 files.
"""
import os, shutil, sys, time
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(HERE), "audit2"))
from lib import *  # noqa: F401,F403

N = int(sys.argv[1]) if len(sys.argv) > 1 else 500
root = os.path.join(S, "a4speed")
shutil.rmtree(root, ignore_errors=True)
rng = np.random.default_rng(4)
kinds = {"drum_loop": 0, "melodic_loop": 0, "oneshot": 0, "midi": 0}
for i in range(N):
    k = i % 10
    if k < 3:
        bpm = int(rng.integers(80, 170))
        bars = max(1, int(round(8.0 * bpm / 240)))
        write_wav(os.path.join(root, "Loops", "Drums", "Drum Loop %03d %d BPM.wav" % (i, bpm)),
                  drum_loop(bpm, bars, ["four", "trap", "break"][i % 3]))
        kinds["drum_loop"] += 1
    elif k < 5:
        bpm = int(rng.integers(80, 140))
        write_wav(os.path.join(root, "Loops", "Melodic", "Keys Loop %03d %d BPM C minor.wav" % (i, bpm)),
                  chord_loop(bpm, max(1, int(round(8.0 * bpm / 240)))))
        kinds["melodic_loop"] += 1
    elif k < 8:
        g = [("Drums/Kicks/Kick %03d.wav", kick(0.5)), ("Drums/Snares/Snare %03d.wav", snare()),
             ("Drums/Hats/Hat %03d.wav", hat()), ("Bass/808 %03d C1.wav", b808()),
             ("Synth/Leads/Lead %03d C3.wav", tone(130.81, 1.5))][i % 5]
        write_wav(os.path.join(root, *(g[0] % i).split("/")), g[1])
        kinds["oneshot"] += 1
    else:
        write_midi(os.path.join(root, "MIDI", "Chords %03d 120 BPM C major.mid" % i),
                   [(b * 4 + q, 0.9, 60 + 4 * (q % 3), 100, 0) for b in range(4) for q in range(4)], 120)
        kinds["midi"] += 1
t0 = time.time()
rows, con, code = run_pack(root, "speed")
dt = time.time() - t0
print("files=%d %s code=%s rows=%d" % (N, kinds, code, len(rows)))
print("seconds=%.1f per_file=%.3f s  -> 2000 files ~ %.1f min" % (dt, dt / N, dt / N * 2000 / 60))
