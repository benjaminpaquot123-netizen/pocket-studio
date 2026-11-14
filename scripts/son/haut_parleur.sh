#!/usr/bin/env bash
# Tour 6, siège son, after the « kick » lot: the songs as the phone's own speaker plays them. The lot's SpeakerKick
# works only when the app tells the engine the music goes to the phone's speaker (command 210, SetSpeakerOutput, sent
# live, never to an export): each recorded song of a bench run is copied with « C 210 1 » before its render (the way
# song-bench.sh --kit3 adds its line), rendered by ps_render and measured by ps_measure.
#
#   scripts/son/haut_parleur.sh BENCH_DIR TOOLS_DIR OUT_DIR [JOBS]
#
# BENCH_DIR: a scripts/song-bench.sh work folder (its recorded cmds/ and hash/ are used as they are: the lot changed the
# engine only). TOOLS_DIR: ps_render and ps_measure built from the version judged. OUT_DIR gets:
#   cmds/      the copies with the speaker on; tools -> TOOLS_DIR (so son6.py and son6_kick.py run on OUT_DIR)
#   wav/       the speaker renders (kept for son6*.py; never commit them)
#   json/      ps_measure of the speaker renders
#   export.tsv style, the export rendered again without the speaker: « identique » when its SHA-256 equals the bench's
#              (the lot left the export as it was), else « différent »
#   time.tsv   style, render seconds with the speaker, without (one render each, JOBS at a time: a rough cost)
# Resumable: a style whose json/<style>.json exists is skipped (the container may restart).
set -euo pipefail
BENCH="$(cd "$1" && pwd)"
TOOLS="$(cd "$2" && pwd)"
OUT="$(mkdir -p "$3" && cd "$3" && pwd)"
JOBS="${4:-4}"
mkdir -p "$OUT/cmds" "$OUT/wav" "$OUT/json" "$OUT/tmp"
ln -sfn "$TOOLS" "$OUT/tools"
# The recordings read their samples beside them (cmds/samples/…).
[ -d "$BENCH/cmds/samples" ] && ln -sfn "$BENCH/cmds/samples" "$OUT/cmds/samples"
for f in "$BENCH"/cmds/*.pscmd; do
    s="$(basename "$f" .pscmd)"
    awk '/^RENDER /{print "AT -1"; print "C 210 1 0 0"} {print}' "$f" > "$OUT/cmds/$s.pscmd"
    grep -q "^C 210 1 0 0$" "$OUT/cmds/$s.pscmd" || { echo "no RENDER line in $f" >&2; exit 1; }
done
export BENCH OUT
ls "$OUT/cmds" | grep "\.pscmd$" | sed "s/\.pscmd$//" | xargs -P "$JOBS" -I{} sh -c '
    [ -s "$OUT/json/{}.json" ] && exit 0
    t0=$(date +%s.%N)
    "$OUT/tools/ps_render" "$OUT/cmds/{}.pscmd" "$OUT/tmp/{}.wav" > /dev/null || { echo "FAILED: {}" >&2; exit 255; }
    t1=$(date +%s.%N)
    "$OUT/tools/ps_render" "$BENCH/cmds/{}.pscmd" "$OUT/tmp/{}.plain.wav" > /dev/null || { echo "FAILED: {} (export)" >&2; exit 255; }
    t2=$(date +%s.%N)
    now="$(cd "$OUT/tmp" && sha256sum {}.plain.wav | cut -d" " -f1)"
    was="$(cut -d" " -f1 "$BENCH/hash/{}.txt")"
    rm -f "$OUT/tmp/{}.plain.wav"
    if [ "$now" = "$was" ]; then same=identique; else same=différent; fi
    mv "$OUT/tmp/{}.wav" "$OUT/wav/{}.wav"
    "$OUT/tools/ps_measure" "$OUT/wav/{}.wav" "$OUT/cmds/{}.pscmd" > "$OUT/tmp/{}.json" || { echo "FAILED: {} (measure)" >&2; exit 255; }
    echo "{}	$same" >> "$OUT/export.tsv"
    echo "{}	$(echo "$t1 - $t0" | bc)	$(echo "$t2 - $t1" | bc)" >> "$OUT/time.tsv"
    mv "$OUT/tmp/{}.json" "$OUT/json/{}.json"
'
echo "Speaker renders: $OUT"
