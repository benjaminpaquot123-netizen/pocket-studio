#!/usr/bin/env bash
# Tour 6, siège son, after the « kick » lot: the songs as the phone's own speaker plays them. The lot's SpeakerKick
# works only when the app tells the engine the music goes to the phone's speaker (command 210, SetSpeakerOutput, sent
# live, never to an export): each recorded song of a bench run is copied with « C 210 1 » before its render (the way
# song-bench.sh --kit3 adds its line), rendered by the same ps_render and measured by ps_measure.
#
#   scripts/son/haut_parleur.sh BENCH_DIR OUT_DIR [JOBS]
#
# BENCH_DIR: a scripts/song-bench.sh work folder (cmds/, tools/). OUT_DIR gets cmds/ (the copies), wav/, json/,
# tools (a link to BENCH_DIR/tools, so that son6.py and son6_kick.py can run on OUT_DIR), and time.tsv (each render's
# seconds with and without the speaker, one thread each: the lot's cost in render time). Never commit OUT_DIR.
set -euo pipefail
BENCH="$(cd "$1" && pwd)"
OUT="$(mkdir -p "$2" && cd "$2" && pwd)"
JOBS="${3:-4}"
mkdir -p "$OUT/cmds" "$OUT/wav" "$OUT/json"
ln -sfn "$BENCH/tools" "$OUT/tools"
for f in "$BENCH"/cmds/*.pscmd; do
    s="$(basename "$f" .pscmd)"
    awk '/^RENDER /{print "AT -1"; print "C 210 1 0 0"} {print}' "$f" > "$OUT/cmds/$s.pscmd"
    grep -q "^C 210 1 0 0$" "$OUT/cmds/$s.pscmd" || { echo "no RENDER line in $f" >&2; exit 1; }
done
export BENCH OUT
ls "$OUT/cmds" | sed 's/\.pscmd$//' | xargs -P "$JOBS" -I{} sh -c '
    t0=$(date +%s.%N)
    "$OUT/tools/ps_render" "$OUT/cmds/{}.pscmd" "$OUT/wav/{}.wav" > /dev/null || { echo "FAILED: {}" >&2; exit 255; }
    t1=$(date +%s.%N)
    "$OUT/tools/ps_render" "$BENCH/cmds/{}.pscmd" "$OUT/wav/{}.plain.wav" > /dev/null || { echo "FAILED: {} (plain)" >&2; exit 255; }
    t2=$(date +%s.%N)
    rm -f "$OUT/wav/{}.plain.wav"
    "$OUT/tools/ps_measure" "$OUT/wav/{}.wav" "$OUT/cmds/{}.pscmd" > "$OUT/json/{}.json" || { echo "FAILED: {} (measure)" >&2; exit 255; }
    echo "{}	$(echo "$t1 - $t0" | bc)	$(echo "$t2 - $t1" | bc)" >> "$OUT/time.tsv"
'
echo "Speaker renders: $OUT"
