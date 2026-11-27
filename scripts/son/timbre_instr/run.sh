#!/usr/bin/env bash
# Note-instrument fingerprint: build the host renderer, self-test, trial on our synth, CMA demo.
# Doc: docs/sons/fin/TIMBRE-INSTRUMENTS-OUTIL.md. Writes nothing in the repo.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
D="$(mktemp -d)"
trap 'rm -rf "${D:?}"' EXIT
DS="$ROOT/engine/third_party/DaisySP/Source"
gcc -O2 -c "$ROOT/engine/src/main/cpp/core/ps_rand.c" -o "$D/ps_rand.o"
g++ -std=c++17 -O2 -w -include "$ROOT/engine/third_party/daisysp_rand_override.h" \
    -I "$ROOT/engine/src/main/cpp/core" -I "$DS" -I "$DS/Utility" -I "$ROOT/engine/third_party/hiir" \
    "$HERE/render_instr.cpp" "$ROOT/engine/src/main/cpp/core/Synth.cpp" "$D/ps_rand.o" \
    "$DS/Filters/svf.cpp" "$DS/Filters/ladder.cpp" "$DS/Control/adsr.cpp" "$DS/Synthesis/oscillator.cpp" \
    -o "$D/render_instr"
cd "$HERE"
python3 -B selftest.py | tee "$D/selftest.txt"
if [ "$(tail -n 1 "$D/selftest.txt")" != "SELFTEST VERT" ]; then
    echo "self-test rouge : on ne mesure pas" >&2
    exit 1
fi
python3 -B essai.py instruments "$D/render_instr"
python3 -B essai.py cma
