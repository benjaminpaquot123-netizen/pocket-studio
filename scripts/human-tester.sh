#!/usr/bin/env bash
# The « testeur humain » bench (owner, 2026-10-28): the whole app on a phone-sized Robolectric
# screen, driven by a tester who only sees the screen (HumanTester.kt). One seat a folder:
#   <folder>/a/start.txt, <folder>/b/start.txt (simple=true|false, timeout.minutes=20)
# The tester plays with scripts/human-act.py <folder>/<seat> <gesture>.
set -euo pipefail
dir="$(cd "${1:?usage: scripts/human-tester.sh <folder>}" && pwd)"
cd "$(dirname "$0")/.."
# shellcheck disable=SC2086
./gradlew ${GRADLE_ARGS:-} :app:testDebugUnitTest --tests 'com.pocketstudio.app.human.*' -Phuman.dir="$dir" --rerun
