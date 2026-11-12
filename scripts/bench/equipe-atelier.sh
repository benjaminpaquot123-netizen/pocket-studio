#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 7): one git workshop and one branch per correction team, never two teams in one folder.
#   scripts/bench/equipe-atelier.sh cree <tour-N> <zone>     -> /home/user/eq-<tour-N>-<zone>, branch eq/<tour-N>-<zone>
#   scripts/bench/equipe-atelier.sh retire <tour-N> <zone>   -> the workshop removed (its branch kept until merged)
# Made from the integration workshop's HEAD (fx-int). The submodules' folders are linked to the main clone's, as in fx-int
# (their links are never committed: `git ls-tree -r HEAD | grep -c '^120000'` stays 0). Gradle only through
# scripts/bench/gradle-file.sh (one Gradle on the machine); app/build removed when the workshop goes.
set -euo pipefail
cmd="${1:?cree|retire}"; tour="${2:?tour-N}"; zone="${3:?zone}"
int="$(cd "$(dirname "$0")/../.." && pwd)"
main="${PS_MAIN_CLONE:-/home/user/pocket-studio}"
dir="/home/user/eq-$tour-$zone"
branch="eq/$tour-$zone"
case "$cmd" in
  cree)
    # PLAN-RESTE 7.2: the Gradle guard rail (a direct ./gradlew is refused) installed once on this machine.
    mkdir -p ~/.gradle/init.d && cp "$(dirname "$0")/pocket-studio-file.gradle" ~/.gradle/init.d/pocket-studio-file.gradle
    free_gb=$(df -BG --output=avail / | tail -1 | tr -dc 0-9)
    [ "$free_gb" -ge 4 ] || { echo "Only ${free_gb} GB free: no new workshop (4 GB needed)." >&2; exit 75; }
    git -C "$int" worktree add -b "$branch" "$dir" HEAD
    for sub in $(git -C "$int" ls-files -s | awk '$1 == "160000" {print $4}'); do
      [ -e "$main/$sub" ] && rm -rf "${dir:?}/$sub" && ln -s "$main/$sub" "$dir/$sub"
    done
    cp -n "$int/local.properties" "$dir/" 2>/dev/null || true
    echo "$dir ($branch)"
    ;;
  retire)
    rm -rf "${dir:?}/app/build" "${dir:?}/build"
    git -C "$int" worktree remove --force "$dir"
    echo "removed $dir (branch $branch kept)"
    ;;
  *) echo "cree|retire" >&2; exit 2 ;;
esac
