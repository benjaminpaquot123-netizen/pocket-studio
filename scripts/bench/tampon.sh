#!/usr/bin/env bash
# Banc fiable 1.2 (and the external audit of the bench): the self-test's stamp, written and checked by ONE script.
#   scripts/bench/tampon.sh empreinte                                 -> the code's fingerprint (2 lines)
#   scripts/bench/tampon.sh ecris <lot folder> <fingerprint file> [quarantined reds…]   -> <lot>/selftest.ok
#   scripts/bench/tampon.sh verifie <lot folder>                     -> exit 0 when the stamp fits this code AND this lot
# The fingerprint covers every file of the repository but docs/ (the app, the engine, the project model, Gradle's files,
# the scripts; changes and new files): a stamp no longer survives a change of the engine or of the model the bench uses.
# The stamp names its lot (its path and a fingerprint of its seats' start.txt): a stamp copied to another lot, or a lot
# whose seats changed, is refused.
# Audit « banc crédible » 2, P2: a stamp is written only on a GREEN self-test of that lot: <lot>/selftest-resume.txt
# says « N verts, 0 rouges » and is what selftest-verdict.py makes of <lot>/junit/ (run again here). The stamp seals
# both (« resume: », « junit: » sha256); `verifie` recomputes them and runs the verdict again. A stamp still CAN be
# forged (all of it is local), but only by making JUnit results, and the traces are kept (D1).
set -euo pipefail
[ -n "${2:-}" ] && lot_abs="$(cd "$2" && pwd)"
cd "$(dirname "$0")/../.."
here() {
  git rev-parse HEAD
  { git diff HEAD -- . ':!docs'; git ls-files -o --exclude-standard -- . ':!docs' | sort | while read -r f; do echo "$f"; cat "$f"; done; } \
    | sha256sum | cut -d' ' -f1
}
seats() { cat "$1"/*/start.txt 2>/dev/null | sha256sum | cut -d' ' -f1; }
sum() { sha256sum < "$1" | cut -d' ' -f1; }
# The lot's JUnit results, names and contents, in a fixed order.
junit_sum() {
  [ -d "$1/junit" ] || { echo "aucun"; return; }
  (cd "$1/junit" && find . -type f -name '*.xml' | LC_ALL=C sort | while read -r f; do echo "$f"; cat "$f"; done) \
    | sha256sum | cut -d' ' -f1
}
# Exit 0 when the lot's summary is green and is the verdict of its JUnit results (selftest.sh's own lines aside).
green_summary() {
  local lot="$1" again rc=0
  [ -f "$lot/selftest-resume.txt" ] && head -1 "$lot/selftest-resume.txt" | grep -Eq '^[1-9][0-9]* verts, 0 rouges,' \
    || { echo "tampon : pas de résumé vert du selftest dans $lot" >&2; return 1; }
  again="$(mktemp)"
  python3 -I scripts/bench/selftest-verdict.py "$lot/junit" scripts/bench/selftest-quarantaine.txt "$again" \
    "$lot/selftest-attendus.txt" "$lot/selftest-gradle-echecs.txt" scripts/bench/selftest-sauts-connus.txt >/dev/null 2>&1 || rc=1
  if [ "$rc" -eq 0 ] && ! diff -q "$again" <(grep -Ev '^(machine|code testé) : ' "$lot/selftest-resume.txt") >/dev/null; then rc=1; fi
  rm -f "$again"
  [ "$rc" -eq 0 ] || echo "tampon : le résumé de $lot n'est pas le verdict vert de ses résultats JUnit" >&2
  return "$rc"
}
case "${1:?empreinte|ecris|verifie}" in
  empreinte) here ;;
  ecris) lot="$lot_abs"; fp="${3:?fingerprint file}"; shift 3
    green_summary "$lot"
    { cat "$fp"; echo "lot: $lot"; echo "sieges: $(seats "$lot")"; echo "resume: $(sum "$lot/selftest-resume.txt")"
      echo "junit: $(junit_sum "$lot")"; date -u +%FT%TZ; for q in "$@"; do echo "quarantaine: $q"; done; } > "$lot/selftest.ok" ;;
  verifie) stamp="$lot_abs/selftest.ok"
    [ -f "$stamp" ] && [ "$(head -2 "$stamp")" = "$(here)" ] && [ "$(sed -n 3p "$stamp")" = "lot: $lot_abs" ] \
      && [ "$(sed -n 4p "$stamp")" = "sieges: $(seats "$lot_abs")" ] \
      && [ -f "$lot_abs/selftest-resume.txt" ] && grep -qxF "resume: $(sum "$lot_abs/selftest-resume.txt")" "$stamp" \
      && grep -qxF "junit: $(junit_sum "$lot_abs")" "$stamp" && green_summary "$lot_abs" ;;
  *) echo "empreinte|ecris|verifie" >&2; exit 2 ;;
esac
