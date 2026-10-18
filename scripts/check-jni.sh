#!/usr/bin/env bash
# Every Kotlin `external fun` of NativeEngine must have a matching C++ JNI symbol
# (an `internal` modifier silently renames it and the app crashes at the first call).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASS="$(find "$ROOT/engine/build" -path '*release*' -name NativeEngine.class | head -1)"
[ -n "$CLASS" ] || { echo "NativeEngine.class not found: build the engine first" >&2; exit 1; }
kotlin="$(javap -p "$CLASS" | grep ' native ' | grep -o '[A-Za-z0-9_$]*(' | tr -d '(' | sort)"
native="$(grep -o 'Java_com_pocketstudio_engine_NativeEngine_[A-Za-z0-9_]*' "$ROOT/engine/src/main/cpp/android/JniBridge.cpp" \
    | sed 's/^Java_com_pocketstudio_engine_NativeEngine_//' | sort -u)"
missing="$(comm -23 <(echo "$kotlin") <(echo "$native"))"
if [ -n "$missing" ]; then
    echo "FAIL: Kotlin natives without a C++ implementation: $missing" >&2
    exit 1
fi
echo "JNI symbols OK ($(echo "$kotlin" | wc -l) natives)"
