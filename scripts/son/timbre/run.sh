#!/usr/bin/env bash
# Timbre study (docs/sons/fin/RECHERCHE-TIMBRE.md): builds the host renderer, checks the measuring
# tools, then replays the experiments. Everything runs on the engine's own voice and on invented
# sounds; nothing is written into the repository.
#
#   scripts/son/timbre/run.sh                 the whole study (about thirty minutes on four cores)
#   scripts/son/timbre/run.sh cibles note     only these sections (cibles aveugle bruit modes copie
#                                             contre note famille)
#   scripts/son/timbre/run.sh bench           cost of the candidate blocks
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../../.." && pwd)"
work="${PS_TIMBRE_WORK:-$(mktemp -d)}"
mkdir -p "$work"

g++ -std=c++17 -O2 -Wall -Wextra -I "$root/engine/src/main/cpp/core" \
    "$here/render_voice.cpp" "$root/engine/src/main/cpp/core/MeasuredDrums.cpp" -o "$work/render_voice"

if [ "${1:-}" = "bench" ]; then
    exec "$work/render_voice" bench
fi

# The tool is checked before it measures: no study on a red self-test.
PYTHONDONTWRITEBYTECODE=1 python3 -I "$here/selftest.py" | tail -n 1
PYTHONDONTWRITEBYTECODE=1 python3 -I "$here/etude.py" "$work/render_voice" "$@"
