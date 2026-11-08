#!/usr/bin/env bash
# Banc fiable 1.2 (and the external audit of the bench): the self-test's stamp, written and checked by ONE script.
#   scripts/bench/tampon.sh empreinte                                 -> the code's fingerprint (2 lines)
#   scripts/bench/tampon.sh ecris <lot folder> <fingerprint file> [quarantined reds…]   -> <lot>/selftest.ok
#   scripts/bench/tampon.sh verifie <lot folder>                     -> exit 0 when the stamp fits this code AND this lot
# The fingerprint covers every file of the repository but docs/ (the app, the engine, the project model, Gradle's files,
# the scripts; changes and new files): a stamp no longer survives a change of the engine or of the model the bench uses.
# The stamp names its lot (its path and a fingerprint of its seats' start.txt): a stamp copied to another lot, or a lot
# whose seats changed, is refused.
set -euo pipefail
[ -n "${2:-}" ] && lot_abs="$(cd "$2" && pwd)"
cd "$(dirname "$0")/../.."
here() {
  git rev-parse HEAD
  { git diff HEAD -- . ':!docs'; git ls-files -o --exclude-standard -- . ':!docs' | sort | while read -r f; do echo "$f"; cat "$f"; done; } \
    | sha256sum | cut -d' ' -f1
}
seats() { cat "$1"/*/start.txt 2>/dev/null | sha256sum | cut -d' ' -f1; }
case "${1:?empreinte|ecris|verifie}" in
  empreinte) here ;;
  ecris) lot="$lot_abs"; fp="${3:?fingerprint file}"; shift 3
    { cat "$fp"; echo "lot: $lot"; echo "sieges: $(seats "$lot")"; date -u +%FT%TZ; for q in "$@"; do echo "quarantaine: $q"; done; } > "$lot/selftest.ok" ;;
  verifie) stamp="$lot_abs/selftest.ok"
    [ -f "$stamp" ] && [ "$(head -2 "$stamp")" = "$(here)" ] && [ "$(sed -n 3p "$stamp")" = "lot: $lot_abs" ] \
      && [ "$(sed -n 4p "$stamp")" = "sieges: $(seats "$lot_abs")" ] ;;
  *) echo "empreinte|ecris|verifie" >&2; exit 2 ;;
esac
