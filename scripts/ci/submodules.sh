#!/usr/bin/env bash
# CI: fetches the submodules, every host with retries (the checkout step runs with submodules: false).
# Why: eigen lives on GitLab, sometimes overloaded (10 tries, 30 s apart, since 2026-10); the seven
# GitHub ones had a single try, so one network hiccup turned a whole job red (docs/AIDE-TELECHARGEMENTS.md).
# A failed update aborts before checking out the others, hence --force and the final check.
set -uo pipefail
paths=$(git config -f .gitmodules --get-regexp '\.path$' | awk '{print $2}')
fetch() {   # fetch <label> <tries> <seconds between tries> <path>...
  local label="$1" tries="$2" wait="$3" i; shift 3
  for i in $(seq 1 "$tries"); do
    git submodule update --init --depth=1 --force -- "$@" && return 0
    echo "$label: attempt $i of $tries failed, waiting $wait s"; sleep "$wait"
  done
  return 1
}
status=0
# shellcheck disable=SC2046
fetch "github submodules" 4 20 $(echo "$paths" | grep -v eigen) || status=1
fetch "eigen" 10 30 separation/third_party/eigen || status=1
if [ $status != 0 ] || git submodule status | grep -q '^-'; then git submodule status; exit 1; fi
