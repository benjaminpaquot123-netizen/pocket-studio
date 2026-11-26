#!/usr/bin/env bash
# « Batterie du matin »: before / after MP3s of the measured drum variants (docs/sons/matin/BATTERIE.md).
# Usage: scripts/son/matin_batterie.sh <output dir outside the repo>
# Writes famille_style.mp3 (the app's sound x4, 1 s, the measured variant x4, same loudness) and
# boucle_style.mp3 (8 bars before, 1 s, 8 bars after).
# Variants: kick_style_grave / _sec, clap|snare|hh_closed_style_long / _court (the measured recipe x4,
# 1 s, the variant x4) and kick_deep_techno.mp3 (the app's techno kick, then the pure deep kick). Needs g++ and ffmpeg (libmp3lame).
set -euo pipefail

die() { echo "matin_batterie: ERREUR: $*" >&2; exit 1; }

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
OUT=${1:-}
[ -n "$OUT" ] || die "give an output directory outside the repo (e.g. /tmp/matin)"
OUT=$(realpath -m "$OUT")
case "$OUT/" in "$ROOT"/*) die "the output directory must be outside the repo ($ROOT)";; esac
mkdir -p "$OUT" || die "cannot create $OUT"
command -v g++ >/dev/null || die "g++ is missing"
command -v ffmpeg >/dev/null || die "ffmpeg is missing"
command -v ffprobe >/dev/null || die "ffprobe is missing"
grep -q libmp3lame <<<"$(ffmpeg -hide_banner -encoders 2>/dev/null)" || die "ffmpeg has no libmp3lame"

START=$(date +%s)
cd "$ROOT"
if [ ! -f engine/third_party/DaisySP/Source/Drums/hihat.h ]; then
    git submodule update --init --depth 1 engine/third_party/DaisySP >/dev/null 2>&1 \
        || git submodule update --init engine/third_party/DaisySP \
        || die "cannot fetch the DaisySP submodule"
fi

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
CORE=engine/src/main/cpp/core
DSP=engine/third_party/DaisySP/Source
EBU=engine/third_party/libebur128
echo "matin_batterie: building the renderer..."
gcc -O2 -c "$CORE/ps_rand.c" -o "$WORK/ps_rand.o" || die "build failed (ps_rand.c)"
gcc -O2 -I"$EBU" -I"$EBU/queue" -c "$EBU/ebur128.c" -o "$WORK/ebur128.o" || die "build failed (ebur128.c)"
g++ -std=c++17 -O2 -include engine/third_party/daisysp_rand_override.h \
    -I"$CORE" -I"$DSP" -I"$DSP/Utility" -I"$EBU" \
    scripts/son/matin_batterie_render.cpp "$CORE/DrumKit.cpp" "$CORE/MeasuredDrums.cpp" \
    "$DSP"/Drums/{analogbassdrum,analogsnaredrum,hihat,synthbassdrum,synthsnaredrum}.cpp \
    "$DSP"/Filters/svf.cpp "$DSP"/Synthesis/oscillator.cpp \
    "$WORK/ps_rand.o" "$WORK/ebur128.o" -o "$WORK/render" 2>"$WORK/build.log" \
    || { tail -20 "$WORK/build.log" >&2; die "build failed (renderer)"; }

echo "matin_batterie: rendering..."
"$WORK/render" "$WORK" >/dev/null || die "the render failed"

n=0
for wav in "$WORK"/*.wav; do
    name=$(basename "$wav" .wav)
    ffmpeg -hide_banner -loglevel error -y -i "$wav" -c:a libmp3lame -b:a 192k "$OUT/$name.mp3" \
        || die "MP3 failed for $name"
    # Checks: length, not silent, no clipping.
    stats=$(ffmpeg -hide_banner -i "$OUT/$name.mp3" -af volumedetect -f null - 2>&1)
    dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/$name.mp3")
    mean=$(sed -n 's/.*mean_volume: \(-\?[0-9.]*\) dB.*/\1/p' <<<"$stats")
    peak=$(sed -n 's/.*max_volume: \(-\?[0-9.]*\) dB.*/\1/p' <<<"$stats")
    awk -v d="$dur" 'BEGIN{exit !(d>2)}' || die "$name is too short (${dur}s)"
    awk -v m="$mean" 'BEGIN{exit !(m>-45)}' || die "$name is nearly silent (mean ${mean} dB)"
    awk -v p="$peak" 'BEGIN{exit !(p<0)}' || die "$name clips (peak ${peak} dB)"
    n=$((n + 1))
done
[ "$n" -gt 0 ] || die "no MP3 written"
echo "matin_batterie: $n MP3 dans $OUT ($(( $(date +%s) - START )) s)"
