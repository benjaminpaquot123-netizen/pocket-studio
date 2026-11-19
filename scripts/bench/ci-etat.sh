#!/usr/bin/env bash
# PLAN-RESTE 6.5 (H1 of the dashboard): the CI's state for one commit, read from GitHub by program.
#   scripts/bench/ci-etat.sh [<commit>] [--ecris <hors-banc.tsv>]
# Prints one line per workflow run of that commit (« <workflow>\t<conclusion> »), then « ci_build <état> » and
# « ci_emulateur <état> » where <état> is vert | rouge | en cours | absent. With --ecris, rewrites those two lines in
# the given hors-banc.tsv (the dashboard reads them). Reads GitHub with the gh CLI when it is signed in, else with curl
# on GitHub's public API, no token (tour 9, L9: a lot's machine has no gh); prints « ? » lines when neither answers.
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
sha="$(git -C "$root" rev-parse "${1:-HEAD}" 2>/dev/null || echo "")"
out=""
for a in "$@"; do if [ "${prev:-}" = "--ecris" ]; then out="$a"; fi; prev="$a"; done
repo="$(git -C "$root" remote get-url origin | sed -E 's#.*github.com[:/]##; s#\.git$##')"
# « gh api <path> --jq … » lines, or the same lines from the public API (curl, then python3 for the jq part).
api_lines() {  # <path> <runs|jobs>
  local json
  if gh api "$1" --jq "$( [ "$2" = runs ] && echo '.workflow_runs[] | "\(.id)\t\(.name)\t\(.status)\t\(.conclusion)"' || echo '.jobs[] | "\(.name)\t\(.status)\t\(.conclusion)"')" 2>/dev/null; then
    return 0
  fi
  json="$(curl -fsS --max-time 20 -H 'Accept: application/vnd.github+json' "${PS_GITHUB_API:-https://api.github.com}/$1" 2>/dev/null)" || return 1
  python3 -I -c '
import json, sys
d = json.loads(sys.stdin.read())
s = lambda v: "null" if v is None else str(v)
if sys.argv[1] == "runs":
    for r in d["workflow_runs"]: print("\t".join(s(r.get(k)) for k in ("id", "name", "status", "conclusion")))
else:
    for j in d["jobs"]: print("\t".join(s(j.get(k)) for k in ("name", "status", "conclusion")))
' "$2" <<<"$json"
}
state_of() {  # <status> <conclusion>
  case "$1/$2" in
    completed/success) echo vert ;;
    completed/*) echo rouge ;;
    "") echo absent ;;
    *) echo "en cours" ;;
  esac
}
build="absent"; emu="absent"
# The CI workflow holds the emulator smoke test and every other check as jobs; audit-emulator.yml is a workflow of its own.
if [ -n "$sha" ] && runs="$(api_lines "repos/$repo/actions/runs?head_sha=$sha&per_page=20" runs)"; then
  while IFS=$'\t' read -r id name status conclusion; do
    [ -n "$name" ] || continue
    printf '%s\t%s\n' "$name" "${conclusion:-$status}"
    case "$name" in
      *mulat*) emu="$(state_of "$status" "$conclusion")" ;;
    esac
    jobs="$(api_lines "repos/$repo/actions/runs/$id/jobs" jobs || true)"
    while IFS=$'\t' read -r jname jstatus jconclusion; do
      [ -n "$jname" ] && printf '  %s\t%s\n' "$jname" "${jconclusion:-$jstatus}"
      case "$jname" in
        "") ;;
        *mulat*) emu="$(state_of "$jstatus" "$jconclusion")" ;;
        Publish*) ;;
        *)
          # Lot « ci3 » (review, G1): the CI's long job is three jobs side by side and none waits for the engine, so
          # « ci_build » is every job of the CI workflow but the emulator's and the publication (which only follows
          # the others): red wins, then « en cours », then green. Another workflow keeps the old reading (« *uild* »).
          if [ "$name" = "CI" ]; then
            one="$(state_of "$jstatus" "$jconclusion")"
            case "$build/$one" in
              rouge/*) ;;
              */rouge) build="rouge" ;;
              "en cours"/*) ;;
              *) build="$one" ;;
            esac
          else
            case "$jname" in *uild*) build="$(state_of "$jstatus" "$jconclusion")" ;; esac
          fi ;;
      esac
    done <<<"$jobs"
    # A run that exists but has not created a job yet: that job is on its way, not absent.
    [ "$status" = "completed" ] || { [ "$build" = absent ] && build="en cours"; [ "$emu" = absent ] && emu="en cours"; }
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
