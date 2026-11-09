#!/usr/bin/env bash
# PLAN-RESTE 6.5 (H1 of the dashboard): the CI's state for one commit, read from GitHub by program.
#   scripts/bench/ci-etat.sh [<commit>] [--ecris <hors-banc.tsv>]
# Prints one line per workflow run of that commit (« <workflow>\t<conclusion> »), then « ci_build <état> » and
# « ci_emulateur <état> » where <état> is vert | rouge | en cours | absent. With --ecris, rewrites those two lines in
# the given hors-banc.tsv (the dashboard reads them). Needs the gh CLI signed in; prints « ? » lines when it is not.
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
sha="$(git -C "$root" rev-parse "${1:-HEAD}" 2>/dev/null || echo "")"
out=""
for a in "$@"; do if [ "${prev:-}" = "--ecris" ]; then out="$a"; fi; prev="$a"; done
repo="$(git -C "$root" remote get-url origin | sed -E 's#.*github.com[:/]##; s#\.git$##')"
state_of() {  # <status> <conclusion>
  case "$1/$2" in
    completed/success) echo vert ;;
    completed/*) echo rouge ;;
    "") echo absent ;;
    *) echo "en cours" ;;
  esac
}
build="absent"; emu="absent"
# The CI workflow holds the build and the emulator smoke test as two jobs; audit-emulator.yml is a workflow of its own.
if [ -n "$sha" ] && runs="$(gh api "repos/$repo/actions/runs?head_sha=$sha&per_page=20" --jq '.workflow_runs[] | "\(.id)\t\(.name)\t\(.status)\t\(.conclusion)"' 2>/dev/null)"; then
  while IFS=$'\t' read -r id name status conclusion; do
    [ -n "$name" ] || continue
    printf '%s\t%s\n' "$name" "${conclusion:-$status}"
    case "$name" in
      *mulat*) emu="$(state_of "$status" "$conclusion")" ;;
    esac
    jobs="$(gh api "repos/$repo/actions/runs/$id/jobs" --jq '.jobs[] | "\(.name)\t\(.status)\t\(.conclusion)"' 2>/dev/null || true)"
    while IFS=$'\t' read -r jname jstatus jconclusion; do
      [ -n "$jname" ] && printf '  %s\t%s\n' "$jname" "${jconclusion:-$jstatus}"
      case "$jname" in
        *mulat*) emu="$(state_of "$jstatus" "$jconclusion")" ;;
        *uild*) build="$(state_of "$jstatus" "$jconclusion")" ;;
      esac
    done <<<"$jobs"
  done <<<"$runs"
else
  build="?"; emu="?"
fi
echo "ci_build	$build"
echo "ci_emulateur	$emu"
if [ -n "$out" ]; then
  python3 -I - "$out" "$build" "$emu" "${sha:0:7}" <<'PY'
import sys, pathlib
p, build, emu, sha = sys.argv[1:]
f = pathlib.Path(p)
lines = f.read_text(encoding="utf-8").splitlines() if f.is_file() else []
seen = set()
for i, l in enumerate(lines):
    k = l.split("\t")[0]
    if k == "ci_build": lines[i] = f"ci_build\t{build} ({sha})"; seen.add(k)
    if k == "ci_emulateur": lines[i] = f"ci_emulateur\t{emu} ({sha})"; seen.add(k)
for k, v in (("ci_build", build), ("ci_emulateur", emu)):
    if k not in seen: lines.append(f"{k}\t{v} ({sha})")
f.write_text("\n".join(lines) + "\n", encoding="utf-8")
PY
fi
