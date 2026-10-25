#!/usr/bin/env bash
# The live bench: the assistant's songs as the PHONE plays them (not as an export renders them).
# For each style, the app test LiveSongRecorder opens the one-touch song as the app does, presses
# ▶ as « Ton morceau est prêt ! » does and stages the next parts as the app does after each scene
# swap; `ps_live` replays those commands through the engine driven like the phone's stream (live
# command budget, 96-frame callbacks) and logs each second: song step, swaps, the whole mix's
# song curve and its fade as heard, the live loudness gain and its loss, the limiter, the output's
# level (second, short-term), peak and steps. The same run checks that every sound the assistant
# offers is previewed at one level (PreviewLevelBench: tools/previews/previews.md).
# Fails when ps_live flags a song (docs/son/BANC.md,
# « Écoute en direct »): an unplanned fade, silence while the transport runs, a click, a NaN, the
# live gain out of its range. Report: OUT/report.md; per style: OUT/log/<style>.txt and .csv.
# « Basse au téléphone » (docs/hits/basses-qui-tapent.md § 8): each song is played a second time with
# its basses alone (META bass, ps_live --solo), and the report gives what a phone keeps of them live:
# their K-weighted level through a 4th-order high-pass at 300 Hz (and 200 Hz) against their own, the
# median over the seconds they play (OUT/log/<style>.bass.csv).
#
#   scripts/live-bench.sh [--styles a,b,c] [--out DIR] [--seconds S (150)] [--no-record] [--loop L] [--sub-layers]
#
# --loop L: the soak (docs/TESTS-REELS.md): each song made about L seconds long (cut, or its parts again
# after it) and looped for the S seconds; ps_live also flags a loop drifting from the others.
# --sub-layers: the bass music subs as layers of their bass (G1b, SubLayers) where they fold.
#
# Environment: GRADLE_ARGS (default --max-workers=1), CMAKE_EXTRA_ARGS, JOBS.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/live-bench"
STYLES=""
SECONDS_LIVE=150
RECORD=1
LOOP=""
SUBLAYERS=""
JOBS="${JOBS:-$(nproc 2>/dev/null || echo 2)}"
while [ $# -gt 0 ]; do
    case "$1" in
        --styles) STYLES="$2"; shift 2 ;;
        --out) OUT="$(mkdir -p "$2" && cd "$2" && pwd)"; shift 2 ;;
        --seconds) SECONDS_LIVE="$2"; shift 2 ;;
        --no-record) RECORD=0; shift ;;
        --loop) LOOP="$2"; shift 2 ;;
        --sub-layers) SUBLAYERS="-Pbench.sublayers=1"; shift ;;
        *) echo "unknown option $1" >&2; exit 1 ;;
    esac
done
TOOLS="$OUT/tools"
mkdir -p "$OUT"

echo "== Building ps_render and ps_live"
cmake -S "$ROOT/engine/bench" -B "$TOOLS" -DCMAKE_BUILD_TYPE=Release ${CMAKE_EXTRA_ARGS:-} > "$OUT/cmake.log"
cmake --build "$TOOLS" -j "$JOBS" --target ps_render ps_live >> "$OUT/cmake.log"
# The tool checks itself first: a steady sound passes, a click and a silence are flagged.
python3 -I "$ROOT/scripts/bench/live_selftest.py" "$TOOLS/ps_live" "$OUT/selftest"

if [ "$RECORD" = 1 ]; then
    echo "== Recording live playback (app test LiveSongRecorder)"
    rm -rf "$OUT/cmds"
    (cd "$ROOT" && ./gradlew ${GRADLE_ARGS:---max-workers=1} :app:testDebugUnitTest \
        --tests 'com.pocketstudio.app.bench.LiveSongRecorder' --tests 'com.pocketstudio.app.bench.PreviewLevelBench' --rerun \
        -Pbench.live="$OUT/cmds" -Pbench.render="$TOOLS/ps_render" -Pbench.seconds="$SECONDS_LIVE" ${STYLES:+-Pbench.styles="$STYLES"} ${LOOP:+-Pbench.loop="$LOOP"} $SUBLAYERS) > "$OUT/gradle.log" 2>&1 \
        || { tail -60 "$OUT/gradle.log"; exit 1; }
fi
ls "$OUT"/cmds/*.pslive > /dev/null 2>&1 || { echo "no .pslive recorded in $OUT/cmds" >&2; exit 1; }

echo "== Playing live ($JOBS at a time, $SECONDS_LIVE s each)"
rm -rf "$OUT/log" && mkdir -p "$OUT/log"
export OUT TOOLS SECONDS_LIVE
# ps_live exits 4 when it flags something (the report says what); any other failure stops the bench.
for f in "$OUT"/cmds/*.pslive; do
    s="$(basename "$f" .pslive)"
    if [ -z "$STYLES" ] || [[ ",$STYLES," == *",$s,"* ]]; then echo "$s"; fi
done | xargs -P "$JOBS" -I{} sh -c '"$TOOLS/ps_live" --seconds "$SECONDS_LIVE" --csv "$OUT/log/{}.csv" "$OUT/cmds/{}.pslive" > "$OUT/log/{}.txt"; st=$?; [ $st = 0 ] || [ $st = 4 ] || { echo "FAILED: {} ($st)" >&2; exit 255; }
    bass="$(sed -n "s/^META bass //p" "$OUT/cmds/{}.pslive" | head -1)"
    if [ -n "$bass" ]; then
        "$TOOLS/ps_live" --quiet --seconds "$SECONDS_LIVE" --solo "$bass" --csv "$OUT/log/{}.bass.csv" "$OUT/cmds/{}.pslive" > /dev/null; st=$?
        [ $st = 0 ] || [ $st = 4 ] || { echo "FAILED: {} bass ($st)" >&2; exit 255; }
    fi'

echo "== Report"
python3 -I - "$OUT/log" "$OUT/report.md" <<'PY'
import csv, glob, os, sys
log, out = sys.argv[1], sys.argv[2]
lines, bad = [], 0
def phone(path):
    """What a phone keeps of the basses played alone live: median over the seconds they play of their
    K-weighted level through the 300 Hz (and 200 Hz) model against their own (dB)."""
    if not os.path.exists(path):
        return "–"
    rows = [r for r in csv.DictReader(open(path)) if r.get("running") == "1" and "k300_db" in r]
    if not rows:
        return "–"
    top = max(float(r["k_db"]) for r in rows)
    if top < -70.0:
        return "–"  # the basses never play in the seconds heard (a long intro)
    heard = [r for r in rows if float(r["k_db"]) > top - 20.0]
    def med(key):
        v = sorted(float(r[key]) - float(r["k_db"]) for r in heard)
        return v[len(v) // 2]
    return f"{med('k300_db'):.1f} / {med('k200_db'):.1f}".replace("-", "−").replace(".", ",")
for path in sorted(glob.glob(os.path.join(log, "*.csv"))):
    if path.endswith(".bass.csv"):
        continue
    style = os.path.basename(path)[:-4]
    rows = list(csv.DictReader(open(path)))
    text = open(path[:-4] + ".txt").read().splitlines()
    flags = [l[5:] for l in text if l.startswith("FLAG ")]
    drive = [float(r["drive_db"]) for r in rows]
    short = [float(r["short_db"]) for r in rows if r["running"] == "1"][3:] or [-200.0]
    lane = [float(r["lane"]) for r in rows]
    bad += 1 if flags else 0
    lines.append(f"| {'ok' if not flags else '**à voir**'} | {style} | {min(drive):+.1f} .. {max(drive):+.1f} | {min(short):.1f} .. {max(short):.1f} | "
                 f"{min(lane):.2f} | {phone(path[:-4] + '.bass.csv')} | {len(flags)} | {'; '.join(flags[:3])} |")
md = ["# Écoute en direct (ps_live)", "",
      f"{len(lines) - bad}/{len(lines)} styles sans alerte. Gain live = la cible de loudness (dB) ; niveau = court terme (3 s) pendant la lecture ;"
      " courbe = la courbe du morceau sur la sortie générale (1 = pleine) ; basse au téléphone = les basses jouées seules en direct,"
      " leur sonie à travers un téléphone (passe-haut d'ordre 4 à 300 Hz / 200 Hz, pondération K) moins leur sonie pleine bande"
      " (dB, médiane des secondes où elles jouent ; plus proche de 0, mieux elles passent ; cible ≥ −14 / −10).", "",
      "| | style | gain live (dB) | niveau (dBFS) | courbe min | basse au téléphone (300 / 200 Hz) | alertes | premières alertes |",
      "|---|---|---|---|---|---|---|---|"] + lines
open(out, "w").write("\n".join(md) + "\n")
print("\n".join(md))
sys.exit(1 if bad else 0)
PY
