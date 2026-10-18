#!/usr/bin/env bash
# Builds and runs the audio engine tests on the host (Linux/macOS), no phone needed.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="${HOST_TESTS_BUILD_DIR:-$ROOT/build/host-tests}"
cmake -S "$ROOT/engine/host-tests" -B "$BUILD" -DCMAKE_BUILD_TYPE=Release ${CMAKE_EXTRA_ARGS:-}
cmake --build "$BUILD" -j
"$BUILD/engine_tests" "$@"
