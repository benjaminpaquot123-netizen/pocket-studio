#!/usr/bin/env bash
# Replays the drum choice (docs/sons/matin/CONCLUSION-BATTERIE.md) in one command: builds the
# measurer, measures every version (nouvelle / A / B / deep) of the measured kit, applies the rule
# written in matin_choix.py to the targets of docs/sons/matin/cibles-batterie.tsv and checks that
# the engine's choice table (MeasuredDrums.cpp) is the one the rule gives.
# Usage: scripts/son/matin_choix.sh [CIBLES.tsv]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CIBLES="${1:-$ROOT/docs/sons/matin/cibles-batterie.tsv}"
TMP="$(mktemp -d)"
trap 'rm -rf "${TMP:?}"' EXIT
g++ -std=c++17 -O2 -I"$ROOT/engine/src/main/cpp/core" "$ROOT/scripts/son/matin_choix_mesure.cpp" \
    "$ROOT/engine/src/main/cpp/core/MeasuredDrums.cpp" -o "$TMP/mesure"
"$TMP/mesure" > "$TMP/mesures.tsv"
python3 "$ROOT/scripts/son/matin_choix.py" "$CIBLES" "$TMP/mesures.tsv" "$ROOT/engine/src/main/cpp/core/MeasuredDrums.cpp"
