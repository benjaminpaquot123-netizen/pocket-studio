#!/usr/bin/env bash
# BenStudio Test, « Écoute au micro » (docs/PLAN-TEST-APK.md, T1b), the Linux side: the four songs the phone heard
# (jump up, house, trap, techno, the assistant's songs, the same on both sides), played by the live bench as the phone
# plays them (scripts/live-bench.sh: the engine and the app's own commands), then each compared with the phone's
# fingerprint through the phone's speaker curve (scripts/bentest/empreinte.py comparer).
#
#   scripts/bentest/comparer-telephone.sh <mesures.json> [--out DIR] [--seconds S (60)] [--live DIR]
#
# --live DIR: a live bench already run (its cmds/ and tools/ps_live): no Gradle at all. Without it the live bench is run
#             first (it starts Gradle itself, so run this script alone, never beside another Gradle).
# Report: OUT/rapport.md. The renders (OUT/wav, about 20 MB a minute each) are deleted unless KEEP_WAV=1.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
[ $# -ge 1 ] || { sed -n '2,12p' "$0" >&2; exit 2; }
MES="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
shift
OUT="$ROOT/build/bentest-micro"
SECONDS_LIVE=60
LIVE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --out) OUT="$2"; shift 2 ;;
        --seconds) SECONDS_LIVE="$2"; shift 2 ;;
        --live) LIVE="$(cd "$2" && pwd)"; shift 2 ;;
        *) echo "unknown option $1" >&2; exit 2 ;;
    esac
done
STYLES="jump_up house trap techno"
mkdir -p "$OUT/wav"
# The curve first: no speaker's curve in the report, nothing to compare.
python3 -I "$HERE/empreinte.py" courbe "$MES" > "$OUT/courbe.tsv"
if [ -z "$LIVE" ]; then
    LIVE="$OUT/live"
    # live-bench fails when it flags a song (its own report says why); the songs are recorded all the same.
    "$ROOT/scripts/live-bench.sh" --styles "${STYLES// /,}" --out "$LIVE" --seconds "$SECONDS_LIVE" || echo "live-bench flagged something: $LIVE/report.md"
fi
args=()
for s in $STYLES; do
    cmds="$LIVE/cmds/$s.pslive"
    [ -f "$cmds" ] || { echo "no $cmds: the live bench did not record $s" >&2; exit 1; }
    # ps_live exits 4 when it flags something: the render is there all the same.
    st=0
    "$LIVE/tools/ps_live" --quiet --seconds "$SECONDS_LIVE" --wav "$OUT/wav/$s.wav" "$cmds" > /dev/null || st=$?
    [ "$st" = 0 ] || [ "$st" = 4 ] || { echo "ps_live failed on $s ($st)" >&2; exit 1; }
    args+=(--wav "$s=$OUT/wav/$s.wav")
done
python3 -I "$HERE/empreinte.py" comparer "$MES" "${args[@]}" --out "$OUT/rapport.md"
[ "${KEEP_WAV:-0}" = 1 ] || rm -f "$OUT"/wav/*.wav
echo "Report: $OUT/rapport.md"
