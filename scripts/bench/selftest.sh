#!/usr/bin/env bash
# Audit method v2 (docs/audits/ultra/META-AUDIT.md, change 1): the bench's self-tests, BEFORE every lot.
#   scripts/bench/selftest.sh <lot folder>
# A red check forbids the lot: scripts/human-tester.sh refuses to start without the stamp this writes
# (<lot folder>/selftest.ok, holding the commit and the tree it passed on).
# Checks (analysis Q2 §5):
#   machine   ≥ 2 GB of disk, ≥ PS_MEM_MIN_GB of memory, one Gradle (gradle-file.sh does it);
#   bench     every Bench*Test and Tour*AppProofTest/Tour*RenameIdleTest of app/src/test/…/human (engine render length,
#             play head, note takes, rename dialogs, Tout couper / ■, share, microphone, pads, amenées…);
#   scripts   scripts/bench/test_ultra_bench.py (fuser, metrics, dashboard).
set -euo pipefail
lot="$(cd "${1:?usage: scripts/bench/selftest.sh <lot folder>}" && pwd)"
cd "$(dirname "$0")/../.."
fail() { echo "SELFTEST ROUGE : $*" >&2; rm -f "$lot/selftest.ok"; exit 1; }

free_gb=$(df -BG --output=avail / | tail -1 | tr -dc 0-9)
[ "$free_gb" -ge 2 ] || fail "disque : ${free_gb} Go libres (< 2)"

python3 -I scripts/bench/test_ultra_bench.py >"$lot/selftest-scripts.log" 2>&1 || fail "scripts du banc (voir $lot/selftest-scripts.log)"

rm -rf app/build/test-results/testDebugUnitTest/binary
PS_GRADLE_LOG="$lot/selftest-gradle.log" scripts/bench/gradle-file.sh :app:testDebugUnitTest \
  --tests 'com.pocketstudio.app.human.Bench*' \
  --tests 'com.pocketstudio.app.human.Tour*AppProofTest' \
  --tests 'com.pocketstudio.app.human.Tour*RenameIdleTest' \
  --rerun >/dev/null || fail "tests du banc (voir $lot/selftest-gradle.log)"

{ git rev-parse HEAD; git diff HEAD --stat -- app scripts | tail -1; date -u +%FT%TZ; } >"$lot/selftest.ok"
echo "Selftest vert : $lot/selftest.ok"
