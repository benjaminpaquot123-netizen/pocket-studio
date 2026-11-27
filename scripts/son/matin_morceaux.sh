#!/usr/bin/env bash
# « Batterie du matin », before / after in the songs (docs/sons/matin/MORCEAUX.md).
#
#   scripts/son/matin_morceaux.sh OUT_DIR [--styles a,b,...]
#
# For each style: the song made by the assistant, recorded as the app plays it live (the live bench's
# path: LiveSongRecorder with « Écouter », -Pbench.listen=drop), then replayed by ps_live from the
# first Drop for 40 s, once with the app's drums and once with the measured drums (--measured-drums,
# a bench-only engine command), with 1 s of silence between. Same commands, same live gain: no
# normalisation between the halves.
#   OUT_DIR/morceau_<style>.mp3   the whole mix, before | silence | after
#   OUT_DIR/batterie_<style>.mp3  the drums' strip alone (mixer channel 0 soloed), same passage
# Each file is checked (halves of 40 s, silence between, no muted half, no clipping, the halves
# different, the drums' strip within 1 dB of « avant » (each song's trim: a first pass measures the
# strip's gap), no ps_live flag in the passage); exit 1 with the reason when one fails.
# --bestof (scripts/son/matin_bestof.sh): only morceau_<style>.mp3, the whole mix within 1 dB too.
# Builds what it needs (submodules, ps_live, the app's unit tests); 4 CPUs at most.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
[ $# -ge 1 ] || { echo "usage: $0 OUT_DIR [--styles a,b,...]" >&2; exit 2; }
OUT="$(realpath -m "$1")"; shift
case "$OUT/" in "$ROOT"/*) echo "ÉCHEC : le dossier de sortie doit être hors du dépôt ($ROOT)" >&2; exit 2 ;; esac
mkdir -p "$OUT"
rm -f "$OUT"/morceau_*.mp3 "$OUT"/batterie_*.mp3  # (the count at the end: this run's only)
# Style id -> measured drum style (0 house, 1 tech house, 2 techno, 3 trap, 4 dnb, 5 dubstep).
BESTOF=0
STYLES="house,tech_house,techno,trap,dnb,dubstep,deep_house,minimal,hard_techno,drill,liquid_dnb,riddim"
while [ $# -gt 0 ]; do
    case "$1" in
        --styles) STYLES="$2"; shift 2 ;;
        --bestof) BESTOF=1; shift ;;
        *) echo "unknown option $1" >&2; exit 2 ;;
    esac
done
JOBS=4
WORK="$OUT/.work"
TOOLS="$ROOT/build/matin-morceaux/tools"
start=$(date +%s)
command -v ffmpeg > /dev/null || { echo "ÉCHEC : ffmpeg est introuvable (apt-get install ffmpeg)" >&2; exit 1; }
python3 -c "import numpy" 2> /dev/null || { echo "ÉCHEC : numpy est introuvable (pip install numpy)" >&2; exit 1; }

echo "== Submodules"
# The engine's only (the separation's pull a large tree the engine never builds).
(cd "$ROOT" && git submodule update --init --recursive --depth 1 -- $(git config -f .gitmodules --get-regexp path | awk '$2 ~ /^engine\// {print $2}') > /dev/null) \
    || { echo "ÉCHEC : les sous-modules du moteur (git submodule update)" >&2; exit 1; }

echo "== Building ps_live"
mkdir -p "$TOOLS" "$WORK"
cmake -S "$ROOT/engine/bench" -B "$TOOLS" -DCMAKE_BUILD_TYPE=Release > "$WORK/cmake.log" 2>&1 \
    || { tail -30 "$WORK/cmake.log"; echo "ÉCHEC : configuration de ps_live" >&2; exit 1; }
cmake --build "$TOOLS" -j "$JOBS" --target ps_render ps_live >> "$WORK/cmake.log" 2>&1 \
    || { tail -30 "$WORK/cmake.log"; echo "ÉCHEC : compilation de ps_live" >&2; exit 1; }

echo "== Recording the songs as the app plays them (« Écouter »)"
rm -rf "$WORK/cmds"
(cd "$ROOT" && ./gradlew --max-workers=2 :app:testDebugUnitTest --tests 'com.pocketstudio.app.bench.LiveSongRecorder' --rerun \
    -Pbench.live="$WORK/cmds" -Pbench.render="$TOOLS/ps_render" -Pbench.seconds=75 -Pbench.listen=drop -Pbench.styles="$STYLES") \
    > "$WORK/gradle.log" 2>&1 || { tail -40 "$WORK/gradle.log"; echo "ÉCHEC : l'enregistrement des morceaux (Gradle)" >&2; exit 1; }

echo "== Playing them live, before and after ($JOBS at a time)"
# One line per run: style, measured style, where the Drop starts (s), the render's length (s).
plan="$WORK/plan.txt"
: > "$plan"
for s in ${STYLES//,/ }; do
    f="$WORK/cmds/$s.pslive"
    [ -f "$f" ] || { echo "ÉCHEC : $s n'a pas été enregistré" >&2; exit 1; }
    case "$s" in
        house|deep_house) m=0 ;; tech_house) m=1 ;; techno|minimal|hard_techno) m=2 ;;
        trap|drill) m=3 ;; dnb|liquid_dnb) m=4 ;; dubstep|riddim) m=5 ;;
        *) echo "ÉCHEC : pas de batterie mesurée pour $s" >&2; exit 1 ;;
    esac
    # The Drop: the app's pauses before ▶ (WAIT ms up to the drop_steps mark), then its steps at the tempo.
    read -r drop len < <(awk '
        /^SONG bpm=/ { split($2, a, "="); bpm = a[2] }
        /^WAIT ms/ && !seen { ms += $3 }
        /^META drop_steps/ { steps = $3; seen = 1 }
        END { if (!seen || bpm <= 0) { print "x x"; exit } d = ms / 1000 + steps * 15 / bpm; printf "%.4f %d\n", d, int(d + 43) }' "$f")
    [ "$drop" != x ] || { echo "ÉCHEC : $s n'a pas de Drop marqué (META drop_steps)" >&2; exit 1; }
    echo "$s $m $drop $len" >> "$plan"
done
export TOOLS WORK
run() {  # lines "style version kind seconds [ps_live options...]", JOBS at a time
    xargs -P "$JOBS" -L 1 sh -c 's=$1 v=$2 k=$3 len=$4; shift 4
    "$TOOLS/ps_live" --quiet --seconds "$len" --wav "$WORK/$s.$v.$k.wav" "$@" "$WORK/cmds/$s.pslive" > "$WORK/$s.$v.$k.txt"; st=$?
    [ $st = 0 ] || [ $st = 4 ] || { echo "ÉCHEC : ps_live $s $v $k (code $st)" >&2; exit 255; }' sh
}
# Pass 1, the drums' strip (--bestof: the whole mix) before and after without trim: the gap of each
# song's Drop gives its trim (the measured voices' level, --measured-trim), so after sits within 1 dB.
CK=drums; SOLO=" --solo 0"
[ "$BESTOF" = 1 ] && { CK=mix; SOLO=""; }
while read -r s m drop len; do
    echo "$s avant $CK $len$SOLO"
    echo "$s cal $CK $len$SOLO --measured-drums $m"
done < "$plan" | run
python3 -I - "$plan" "$WORK" "$CK" <<'PY'
import struct, sys
import numpy as np
plan, work, ck = sys.argv[1:4]
def rms(path, a0):
    b = open(path, 'rb').read(); i, ch, fmt = 12, 2, 3
    while i < len(b):
        cid, sz = b[i:i + 4], struct.unpack('<I', b[i + 4:i + 8])[0]
        if cid == b'fmt ':
            fmt, ch = struct.unpack('<HH', b[i + 8:i + 12])
        if cid == b'data':
            x = np.frombuffer(b[i + 8:i + 8 + sz], dtype=np.float32 if fmt == 3 else np.int16).astype(np.float64)
            x = (x if fmt == 3 else x / 32768)[ch * a0:ch * (a0 + 40 * 48000)]
            return 20 * np.log10(np.sqrt(np.mean(x ** 2)) + 1e-12)
        i += 8 + sz + (sz & 1)
with open(f"{work}/trims.txt", 'w') as out:
    for row in open(plan):
        s, m, drop, _ = row.split()
        a0 = int(round(float(drop) * 48000))
        gap = rms(f"{work}/{s}.cal.{ck}.wav", a0) - rms(f"{work}/{s}.avant.{ck}.wav", a0)
        out.write(f"{s} {-gap:.2f}\n")
        print(f"  {s} : {'batterie' if ck == 'drums' else 'morceau'} après sans calage {gap:+.1f} dB, calage {-gap:+.2f} dB")
PY
# Pass 2, with each song's trim: the whole mix before and after, the drums' strip after.
while read -r s m drop len; do
    t=$(awk -v s="$s" '$1 == s { print $2 }' "$WORK/trims.txt")
    md=" --measured-drums $m --measured-trim $t"
    echo "$s avant mix $len --csv $WORK/$s.avant.mix.csv"
    echo "$s apres mix $len --csv $WORK/$s.apres.mix.csv$md"
    [ "$BESTOF" = 1 ] || echo "$s apres drums $len --solo 0$md"
done < "$plan" | run

echo "== Cutting, checking, encoding"
python3 -I - "$plan" "$WORK" "$OUT" "$BESTOF" <<'PY'
import struct, subprocess, sys, re
import numpy as np
plan, work, out = sys.argv[1:4]
bestof = sys.argv[4] == '1'  # matin_bestof.sh: the songs only, each within 1 dB
R, HALF, GAP = 48000, 40 * 48000, 48000

def readwav(path):
    b = open(path, 'rb').read()
    i, ch, fmt = 12, 2, 3
    while i < len(b):
        cid, sz = b[i:i + 4], struct.unpack('<I', b[i + 4:i + 8])[0]
        if cid == b'fmt ':
            fmt, ch = struct.unpack('<HH', b[i + 8:i + 12])
        if cid == b'data':
            d = np.frombuffer(b[i + 8:i + 8 + sz], dtype=np.float32 if fmt == 3 else np.int16).astype(np.float64)
            return (d if fmt == 3 else d / 32768).reshape(-1, ch)
        i += 8 + sz + (sz & 1)
    raise SystemExit(f"ÉCHEC : {path} n'a pas de données")

def db(x):
    return 20 * np.log10(np.sqrt(np.mean(x ** 2)) + 1e-12)

def flags(path, t0, t1):
    bad = []
    for line in open(path):
        if not line.startswith('FLAG '):
            continue
        m = re.search(r' at (\d+) s', line)
        if m is None or t0 <= int(m.group(1)) <= t1:  # NAN, ALLOC (no time): always
            bad.append(line[5:].strip())
    return bad

errors, made = [], 0
for row in open(plan):
    style, m, drop, _ = row.split()
    a0 = int(round(float(drop) * R))
    for kind, name in (('mix', 'morceau'),) if bestof else (('mix', 'morceau'), ('drums', 'batterie')):
        halves = []
        for v in ('avant', 'apres'):
            x = readwav(f"{work}/{style}.{v}.{kind}.wav")[a0:a0 + HALF]
            halves.append(x)
            if len(x) != HALF:
                errors.append(f"{name}_{style} : la moitié « {v} » dure {len(x) / R:.2f} s au lieu de 40 s")
            for f in flags(f"{work}/{style}.{v}.{kind}.txt", int(float(drop)), int(float(drop)) + 41):
                errors.append(f"{name}_{style} « {v} » : alerte ps_live {f}")
        if any(len(h) != HALF for h in halves):
            continue
        fix = 0.0
        if bestof and abs(db(halves[1]) - db(halves[0])) > 0.5:
            # A drum bus that saturates (hard techno) only half follows the trim: the after half is
            # then leveled by a plain gain, said in the log, so the ears judge the sound only.
            fix = db(halves[0]) - db(halves[1])
            halves[1] = halves[1] * 10 ** (fix / 20)
        song = np.concatenate([halves[0], np.zeros((GAP, halves[0].shape[1])), halves[1]])
        mid = song[HALF + 2400:HALF + GAP - 2400]
        lv = [db(h) for h in halves]
        diff = db(halves[0] - halves[1]) - max(lv)
        peak = float(np.abs(song).max())
        if np.any(~np.isfinite(song)):
            errors.append(f"{name}_{style} : NaN")
        if db(mid) > -90:
            errors.append(f"{name}_{style} : pas de silence au milieu ({db(mid):.1f} dBFS)")
        for v, l in zip(('avant', 'après'), lv):
            if l < -40:
                errors.append(f"{name}_{style} : la moitié « {v} » est muette ({l:.1f} dBFS)")
        if peak >= 0.999:
            errors.append(f"{name}_{style} : saturation (crête {peak:.3f})")
        if (kind == 'drums' or bestof) and abs(lv[1] - lv[0]) > 1.0:  # the drums judged by their sound, not their level (each song's trim, pass 1)
            errors.append(f"{name}_{style} : « après » n'a pas le niveau de « avant » ({lv[1] - lv[0]:+.1f} dB)")
        if diff < -40:  # (uncorrelated halves read about +3 dB: only too alike fails)
            errors.append(f"{name}_{style} : les deux moitiés ne diffèrent pas assez ({diff:.1f} dB)")
        mp3 = f"{out}/{name}_{style}.mp3"
        pcm = np.clip(song, -1, 1).astype('<f4').tobytes()
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ar', str(R), '-ac', str(song.shape[1]), '-i', '-',
                        '-c:a', 'libmp3lame', '-b:a', '192k', mp3], input=pcm, check=True)
        dec = subprocess.run(['ffmpeg', '-v', 'error', '-i', mp3, '-f', 'f32le', '-ac', '1', '-ar', str(R), '-'],
                             capture_output=True, check=True).stdout
        secs = len(dec) / 4 / R
        if abs(secs - 81.0) > 0.1:
            errors.append(f"{name}_{style} : le MP3 dure {secs:.2f} s au lieu de 81 s")
        made += 1
        print(f"{name}_{style}.mp3  avant {lv[0]:.1f} dBFS, après {lv[1]:.1f} dBFS, écart {lv[1] - lv[0]:+.1f} dB, crête {peak:.2f}" + (f", remis à niveau {fix:+.1f} dB" if fix else ""))
print(f"{made} MP3")
if errors:
    print("ÉCHEC :\n  " + "\n  ".join(errors), file=sys.stderr)
    sys.exit(1)
PY
rm -f "$WORK"/*.wav  # about 1 GB for 12 styles
echo "== Fait en $(( $(date +%s) - start )) s : $(ls "$OUT"/*.mp3 | wc -l) MP3 dans $OUT"
