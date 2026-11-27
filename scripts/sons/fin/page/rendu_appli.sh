#!/usr/bin/env bash
# Replays the whole "sons de l'appli" render in one command: the live bench (the app's own songs as
# the phone plays them), then rendu_appli.py (single notes, loops and effects through ps_live,
# measures, verdicts against docs/sons/fin/cibles-autres-definitives.tsv).
# Usage: scripts/sons/fin/page/rendu_appli.sh [OUT]   (default OUT: build/son-fin-page, gitignored)
# Writes OUT/appli/<slug>.mp3, OUT/labels.json and docs/sons/fin/page/appli-autres.csv. No sound
# file is ever written inside the repository outside build/.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
OUT="${1:-$ROOT/build/son-fin-page}"
mkdir -p "$OUT"
if [ "${SKIP_LIVE:-0}" != 1 ] || [ ! -x "$OUT/lb/tools/ps_live" ]; then
  flock /tmp/ps-build.lock "$ROOT/scripts/live-bench.sh" \
    --styles techno,house,dnb,dubstep,trap,lofi --seconds 150 --out "$OUT/lb"
fi
python3 -I "$ROOT/scripts/sons/fin/page/rendu_appli.py" --live "$OUT/lb" --out "$OUT/rendu" \
  --csv "$ROOT/docs/sons/fin/page/appli-autres.csv"
