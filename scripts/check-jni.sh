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

# Same for the separation module.
CLASS="$(find "$ROOT/separation/build" -path '*release*' -name Demucs.class | head -1)"
[ -n "$CLASS" ] || { echo "Demucs.class not found: build the separation module first" >&2; exit 1; }
kotlin="$(javap -p "$CLASS" | grep ' native ' | grep -o '[A-Za-z0-9_$]*(' | tr -d '(' | sort)"
native="$(grep -o 'Java_com_pocketstudio_separation_Demucs_[A-Za-z0-9_]*' "$ROOT/separation/src/main/cpp/SeparationJni.cpp" \
    | sed 's/^Java_com_pocketstudio_separation_Demucs_//' | sort -u)"
missing="$(comm -23 <(echo "$kotlin") <(echo "$native"))"
if [ -n "$missing" ]; then
    echo "FAIL: separation natives without a C++ implementation: $missing" >&2
    exit 1
fi
echo "Separation JNI symbols OK ($(echo "$kotlin" | wc -l) natives)"

# Native code calls Demucs.Progress.onProgress by name: in the shrunk release APK, every class
# implementing Progress must still have a method called onProgress (R8 renamed it once and the
# app died at the first progress report).
APK="$(ls "$ROOT"/app/build/outputs/apk/release/*.apk 2>/dev/null | head -1)"
if [ -n "$APK" ]; then
    DEXDUMP="$(ls -d "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"/build-tools/*/dexdump 2>/dev/null | sort -V | tail -1)"
    [ -n "$DEXDUMP" ] || { echo "dexdump not found" >&2; exit 1; }
    DEX_DIR="$(mktemp -d)"
    unzip -q -o "$APK" 'classes*.dex' -d "$DEX_DIR"
    report="$(for f in "$DEX_DIR"/classes*.dex; do "$DEXDUMP" "$f" 2>/dev/null; done | awk '
        /Class descriptor/ { cls = $4 }
        /Interfaces/ { inif = 1; next }
        inif && /#[0-9]+ *:/ { if ($0 ~ /separation\/Demucs\$Progress/) impl[cls] = 1; next }
        /Static fields|Instance fields/ { inif = 0 }
        /name *:/ { if ((cls in impl) && $3 == "'"'"'onProgress'"'"'") ok[cls] = 1 }
        END { n = 0; bad = 0; for (c in impl) { n++; if (!(c in ok)) { bad++; print "missing onProgress in " c } } print "implementers " n " broken " bad }')"
    rm -rf "$DEX_DIR"
    echo "$report" | grep -q "broken 0" && ! echo "$report" | grep -q "implementers 0" || {
        echo "FAIL: release APK lost Demucs.Progress.onProgress: $report" >&2
        exit 1
    }
    echo "Release callbacks OK ($(echo "$report" | tail -1))"
fi
