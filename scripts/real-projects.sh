#!/usr/bin/env bash
# « Projets réels » (docs/TESTS-REELS.md): every project of app/src/test/resources/real-projects/
# (and of --dir) imported as the app imports a .zip, opened, played live 60 s and exported
# (RealProjectsTest), then the 60 s played by the real engine as the phone's stream drives it
# (ps_live: no fade, silence, click, NaN, heap call in the callback) and the export rendered and
# checked (ps_render: no NaN, true peak at most -1 dBTP, its length, no silence inside the song).
#
#   scripts/real-projects.sh [--fetch] [--dir DIR] [--out DIR]
#
# --fetch: also the full projects listed in real-projects/complets.txt (name, release asset,
#   sha256), downloaded once into build/real-projects/complets/ (gh when it is there, else curl);
#   a project not hosted is said and skipped, never a failure. The repository is public: a full
#   project is listed there only with Benjamin's OK and hosted privately (none is, today).
# --dir: another folder of projects (folders with project.json and samples/, or .zip files).
# Environment: GRADLE_ARGS (default --max-workers=1), CMAKE_EXTRA_ARGS, TOOLS (ps_live and
# ps_render already built, e.g. by scripts/live-bench.sh: build/live-bench/tools).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/real-projects"
FETCH=0
DIRS=""
while [ $# -gt 0 ]; do
    case "$1" in
        --fetch) FETCH=1; shift ;;
        --dir) DIRS="$(cd "$2" && pwd)"; shift 2 ;;
        --out) OUT="$(mkdir -p "$2" && cd "$2" && pwd)"; shift 2 ;;
        *) echo "unknown option $1" >&2; exit 1 ;;
    esac
done
mkdir -p "$OUT"
FIXTURES="$ROOT/app/src/test/resources/real-projects"

if [ -z "${TOOLS:-}" ]; then
    TOOLS="$OUT/tools"
    echo "== Building ps_render and ps_live"
    cmake -S "$ROOT/engine/bench" -B "$TOOLS" -DCMAKE_BUILD_TYPE=Release ${CMAKE_EXTRA_ARGS:-} > "$OUT/cmake.log"
    cmake --build "$TOOLS" -j "$(nproc 2>/dev/null || echo 2)" --target ps_render ps_live >> "$OUT/cmake.log"
fi

if [ "$FETCH" = 1 ] && [ -f "$FIXTURES/complets.txt" ]; then
    echo "== Fetching the full projects"
    FULL="$OUT/complets"
    mkdir -p "$FULL"
    REPO="$(cd "$ROOT" && git remote get-url origin 2>/dev/null | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
    { grep -v '^#' "$FIXTURES/complets.txt" || true; } | while read -r name tag file sha; do
        [ -n "${sha:-}" ] || continue
        target="$FULL/$name.zip"
        if [ -f "$target" ] && echo "$sha  $target" | sha256sum -c - > /dev/null 2>&1; then echo "$name: already here"; continue; fi
        rm -f "$target"
        if command -v gh > /dev/null && gh release download "$tag" -R "$REPO" -p "$file" -O "$target" > /dev/null 2>&1; then :;
        else curl -fsSL --retry 3 -o "$target" "https://github.com/$REPO/releases/download/$tag/$file" 2> /dev/null || true; fi
        if [ -f "$target" ] && echo "$sha  $target" | sha256sum -c - > /dev/null 2>&1; then echo "$name: fetched"
        else rm -f "$target"; echo "$name: not hosted yet (release $tag, $file): skipped"; fi
    done
    if ls "$FULL"/*.zip > /dev/null 2>&1; then
        # With --dir too: its projects and the downloaded ones, side by side in one folder of links.
        if [ -n "$DIRS" ]; then
            BOTH="$OUT/both"
            rm -rf "$BOTH" && mkdir -p "$BOTH"
            for f in "$DIRS"/* "$FULL"/*.zip; do [ -e "$f" ] && ln -s "$f" "$BOTH/$(basename "$f")"; done
            DIRS="$BOTH"
        else
            DIRS="$FULL"
        fi
    fi
fi

echo "== Importing, playing live 60 s and exporting each project (RealProjectsTest)"
STATUS=0
(cd "$ROOT" && ./gradlew ${GRADLE_ARGS:---max-workers=1} :app:testDebugUnitTest --tests 'com.pocketstudio.app.bench.RealProjectsTest' --rerun \
    -Preal.out="$OUT/run" -Preal.live="$TOOLS/ps_live" -Preal.render="$TOOLS/ps_render" ${DIRS:+-Preal.projects="$DIRS"}) > "$OUT/gradle.log" 2>&1 || STATUS=$?
# The test's own lines (what it imported, heard and exported), then the verdict.
find "$ROOT/app/build/test-results/testDebugUnitTest" -name '*RealProjectsTest.xml' -exec grep -ho 'real projects: [^<]*' {} \; 2> /dev/null | sort -u || true
if [ "$STATUS" != 0 ]; then
    grep -A12 'FAILED\|AssertionError' "$OUT/gradle.log" | head -60
    echo "Real projects: FAILED (logs: $OUT/run, $OUT/gradle.log)" >&2
    exit "$STATUS"
fi
echo "Real projects: OK (logs: $OUT/run)"
