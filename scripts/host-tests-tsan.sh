#!/usr/bin/env bash
# The engine's threads under ThreadSanitizer (Audit stage 0): the audio callback while the UI thread
# posts commands, frees what was let go and reads the measures (engine/host-tests/thread_tests.cpp). Any
# data race reported fails. Its own build folder (TSan cannot share one with ASan or the normal build).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="${HOST_TESTS_BUILD_DIR:-$ROOT/build/host-tests-tsan}"
CXXFLAGS="${CXXFLAGS:-} -fsanitize=thread -g" CFLAGS="${CFLAGS:-} -fsanitize=thread -g" LDFLAGS="${LDFLAGS:-} -fsanitize=thread" \
    cmake -S "$ROOT/engine/host-tests" -B "$BUILD" -DCMAKE_BUILD_TYPE=Release
cmake --build "$BUILD" -j --target thread_tests
TSAN_OPTIONS="halt_on_error=1 exitcode=66 ${TSAN_OPTIONS:-}" "$BUILD/thread_tests"
