#!/usr/bin/env bash
# Builds and runs the audio engine tests on the host (Linux/macOS), no phone needed.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="${HOST_TESTS_BUILD_DIR:-$ROOT/build/host-tests}"
cmake -S "$ROOT/engine/host-tests" -B "$BUILD" -DCMAKE_BUILD_TYPE=Release ${CMAKE_EXTRA_ARGS:-}
cmake --build "$BUILD" -j
"$BUILD/engine_tests" "$@"

# Real-time rule: no call to libc rand()/random() (they lock on Android) may reach the engine.
if nm "$BUILD/libps_core.a" 2>/dev/null | grep -qwE "U (rand|random)"; then
    echo "FAIL: libc rand()/random() referenced by the engine core" >&2
    exit 1
fi
