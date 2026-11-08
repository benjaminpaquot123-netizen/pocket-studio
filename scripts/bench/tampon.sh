#!/usr/bin/env bash
# Banc fiable 1.2: the self-test's stamp, written and checked by ONE script (written by selftest.sh, checked by
# human-tester.sh: two copies of the format drifted apart and no green stamp was ever recognised).
#   scripts/bench/tampon.sh ecris <lot folder> [quarantined reds…]   -> <lot>/selftest.ok
#   scripts/bench/tampon.sh verifie <lot folder>                       -> exit 0 when the stamp fits this very code
set -euo pipefail
cd "$(dirname "$0")/../.."
here() { git rev-parse HEAD; echo "$(git diff HEAD --stat -- app scripts | tail -1)"; }
case "${1:?ecris|verifie}" in
  ecris) lot="${2:?lot}"; shift 2; { here; date -u +%FT%TZ; for q in "$@"; do echo "quarantaine: $q"; done; } > "$lot/selftest.ok" ;;
  verifie) stamp="${2:?lot}/selftest.ok"; [ -f "$stamp" ] && [ "$(head -2 "$stamp")" = "$(here)" ] ;;
  *) echo "ecris|verifie" >&2; exit 2 ;;
esac
