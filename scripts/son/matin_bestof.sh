#!/usr/bin/env bash
# « Batterie du matin », the best-of to listen to (docs/sons/matin/CONCLUSION-BATTERIE.md): one MP3
# per style, 40 s of the song with the app's drums, 1 s of silence, 40 s with the chosen measured
# drums (the versions of scripts/son/matin_choix.sh), at the same loudness (each song's drums
# trimmed to the app's, the whole mix within 1 dB). Recorded as the app plays it live (the live
# bench's path, scripts/live-bench.sh's LiveSongRecorder), then replayed by ps_live.
#
#   scripts/son/matin_bestof.sh OUT_DIR [--styles a,b,...]
#
# Checks each file (40 s halves, the silence between, no muted half, no clipping, gap under 1 dB,
# MP3 length 81 s); refuses an output folder inside the repository. Exit 1 with the reason.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
[ $# -ge 1 ] || { echo "usage: $0 OUT_DIR [--styles a,b,...]" >&2; exit 2; }
exec "$ROOT/scripts/son/matin_morceaux.sh" "$@" --bestof
