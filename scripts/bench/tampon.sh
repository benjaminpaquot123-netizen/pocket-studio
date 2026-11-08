#!/usr/bin/env bash
# Banc fiable 1.2: the self-test's stamp, written and checked by ONE script (written by selftest.sh, checked by
# human-tester.sh: two copies of the format drifted apart and no green stamp was ever recognised).
#   scripts/bench/tampon.sh ecris <lot folder> [quarantined reds…]   -> <lot>/selftest.ok
#   scripts/bench/tampon.sh verifie <lot folder>                       -> exit 0 when the stamp fits this very code
set -euo pipefail
[ -n "${2:-}" ] && lot_abs="$(cd "$2" && pwd)"
cd "$(dirname "$0")/../.."
# The commit, then a fingerprint of the uncommitted code (changes and new files under app/ and scripts/): the diff's
# summary line let two different changes, or a new test file, pass for the same code (review of session 1).
here() {
  git rev-parse HEAD
  { git diff HEAD -- app scripts; git ls-files -o --exclude-standard app scripts | sort | while read -r f; do echo "$f"; cat "$f"; done; } \
    | sha256sum | cut -d' ' -f1
}
case "${1:?ecris|verifie}" in
  ecris) lot="$lot_abs"; shift 2; { here; date -u +%FT%TZ; for q in "$@"; do echo "quarantaine: $q"; done; } > "$lot/selftest.ok" ;;
  verifie) stamp="$lot_abs/selftest.ok"; [ -f "$stamp" ] && [ "$(head -2 "$stamp")" = "$(here)" ] ;;
  *) echo "ecris|verifie" >&2; exit 2 ;;
esac
