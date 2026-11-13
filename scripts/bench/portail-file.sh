#!/usr/bin/env bash
# Audit method v3 (docs/audits/AUDIT-ULTRA.md §13.6, §13.9): the conductor's queue of the portal. Run by the main
# discussion only, in the integration workshop, on the integration branch:
#   scripts/bench/portail-file.sh <tour-N>:<zone>[:<pushed branch>] [<tour-N>:<zone>[:<pushed branch>] …] [-- <gradle test filters…>]
# (The filters after « -- » go to the portal for every lot, on top of each zone report's « tests: » line.)
# For each lot, in the order given, one after the other:
#   1. the workshop is the pushed head of the integration, nothing else (§13.6: nobody merges anything but the pushed
#      head), clean, and no wave of testers is playing (EN-COURS.md, « Temps en cours : VAGUE … »: §13.1);
#   2. the lot's pushed branch is fetched (default name eq/<tour-N>-<zone>) and becomes the local eq/<tour-N>-<zone>:
#      never a local leftover;
#   3. the pushed head of the integration is merged into the lot's branch (a conflict sends the lot back);
#   4. the pre-flight (pre-vol.sh), then the portal (fusion-equipes.sh): a refusal stops the queue, its line is printed
#      as the conductor's message to the team (GABARIT-FICHES-V3.md, fiche 7);
#   5. the links' check: no symbolic link committed in the merged tree;
#   6. the judge is read before and after (the « chantier : N écarts » of the portal's last trace and of this one);
#   7. the integration is pushed, after EACH success (a restart of the machine then costs one portal, not the batch).
# A batch is three lots at most (§13.6): from batch 4 on, a fourth « Merge team … » since the batch was opened
# (the last change of portail-paquet.txt) is refused: the CI goes first (paquet-verifie.sh).
# Exit: 0 every lot merged and pushed; 2 the workshop is not ready; 3 a lot refused (the lots before it are pushed).
set -euo pipefail
int="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$int"
branch="${PS_INTEGRATION:-ccr-80af1a1b-mn0wec}"
remote="${PS_REMOTE:-origin}"
[ $# -gt 0 ] || { echo "usage: scripts/bench/portail-file.sh <tour-N>:<zone>[:<pushed branch>] …" >&2; exit 2; }
paquet="$(sed -n 's/^paquet: *\([0-9][0-9]*\).*/\1/p' scripts/bench/portail-paquet.txt 2>/dev/null | head -1 || true)"
from_paquet() { [ "${paquet:-1}" -ge "$1" ]; }
say() { echo "portail-file: $*"; }
stop() { code="$1"; shift; echo "portail-file: $*" >&2; exit "$code"; }

push_head() {
  local n delay=2
  for n in 1 2 3 4; do
    if git push -q "$remote" "HEAD:refs/heads/$branch"; then return 0; fi
    sleep "$delay"; delay=$((delay * 2))
  done
  return 1
}

# What the workshop must be before each lot: on the integration branch, clean, equal to the pushed head.
ready() {
  [ "$(git rev-parse --abbrev-ref HEAD)" = "$branch" ] || stop 2 "the workshop is not on $branch."
  # (« T »: a linked submodule of engine/third_party/…, as in the portal's own check.)
  [ -z "$(git status --porcelain --untracked-files=no | grep -v '^ T')" ] || stop 2 "the workshop has uncommitted changes."
  git fetch -q "$remote" "$branch" || stop 2 "the pushed head of $branch could not be fetched."
  pushed="$(git rev-parse FETCH_HEAD)"
  if [ "$(git rev-parse HEAD)" != "$pushed" ]; then
    if git merge-base --is-ancestor HEAD "$pushed"; then
      git merge -q --ff-only "$pushed"
    else
      stop 2 "the workshop is not the pushed head of $branch ($(git rev-parse --short HEAD) here, $(git rev-parse --short "$pushed") pushed): push or drop what is here first."
    fi
  fi
  # §13.1: nothing enters the application or the bench while a wave of testers plays the departure.
  if grep -q '^- \*\*Temps en cours\*\* *: *VAGUE' docs/audits/ultra/EN-COURS.md 2>/dev/null; then
    stop 2 "a wave of testers is playing (EN-COURS.md, « Temps en cours : VAGUE »): nothing is merged."
  fi
}

lots=(); filters=()
while [ $# -gt 0 ]; do
  if [ "$1" = "--" ]; then shift; filters=("$@"); break; fi
  lots+=("$1"); shift
done
# The judge's count in the portal's trace a commit brought (« Portal traces of the merge … »), or « ? ».
judge_of() {
  local f
  f="$(git diff-tree --no-commit-id --name-only -r "$1" 2>/dev/null | grep '/selftest-chantier.txt$' | head -1 || true)"
  if [ -n "$f" ]; then git show "$1:$f" | sed -n 's/^chantier : \([0-9][0-9]*\) écarts.*/\1/p' | tail -1; fi
}
last_judge() {
  local c
  c="$(git log -1 --format=%H --grep='^Portal traces of the merge ' "$1" 2>/dev/null || true)"
  if [ -n "$c" ]; then judge_of "$c"; fi
}
done_lots=()
for lot in "${lots[@]}"; do
  IFS=: read -r tour zone lot_branch <<<"$lot"
  [ -n "${tour:-}" ] && [ -n "${zone:-}" ] || stop 2 "« $lot »: expected <tour-N>:<zone>[:<pushed branch>]."
  name="eq/$tour-$zone"
  lot_branch="${lot_branch:-$name}"
  ready
  # §13.6: three lots at most per batch, then its CI.
  opened="$(git log -1 --format=%H -- scripts/bench/portail-paquet.txt 2>/dev/null || true)"
  in_batch="$(git log --first-parent --merges --format=%s ${opened:+"$opened..HEAD"} | grep -c '^Merge team ' || true)"
  if [ "$in_batch" -ge 3 ]; then
    if from_paquet 4; then stop 3 "$zone: from_paquet 4: the batch $paquet already holds $in_batch lots (three at most): its CI first (scripts/bench/paquet-verifie.sh), then the next batch."; fi
    say "À VOIR: the batch ${paquet:-1} already holds $in_batch lots (refused from batch 4 on)."
  fi
  git fetch -q "$remote" "$lot_branch" || stop 3 "$zone: the pushed branch $lot_branch could not be fetched: not merged."
  lot_head="$(git rev-parse FETCH_HEAD)"
  # The pushed head of the integration merged into the lot's branch, in a throwaway worktree (the workshop's files do
  # not move); the local name eq/<tour-N>-<zone> is the pushed lot plus that merge, never a local leftover.
  work="$(mktemp -d)"
  git worktree add -q --detach "$work" "$lot_head"
  if ! git -C "$work" merge -q --no-edit -m "Merge the integration into $zone (no code change of the lot)" "$pushed" >/dev/null 2>&1; then
    conflicts="$(git -C "$work" diff --name-only --diff-filter=U | tr '\n' ' ')"
    git -C "$work" merge --abort 2>/dev/null || true
    git worktree remove --force "$work"
    stop 3 "$zone: the pushed head of $branch does not merge into $lot_branch (conflict: $conflicts): not merged. The team merges the pushed head, replays its tests and comes back."
  fi
  merged_lot="$(git -C "$work" rev-parse HEAD)"
  git worktree remove --force "$work"
  # (Review of « portail3 », M2: said, never silent; a branch held by another worktree is a refusal, not git's raw error.)
  was="$(git rev-parse -q --verify "refs/heads/$name" || true)"
  if [ -n "$was" ] && ! git merge-base --is-ancestor "$was" "$merged_lot"; then
    say "$zone: the local $name ($(git rev-parse --short "$was")) is not in the pushed lot: replaced by the pushed one (kept as refs/portail-file/$name)."
    git update-ref "refs/portail-file/$name" "$was"
  fi
  git branch -q -f "$name" "$merged_lot" 2>/dev/null \
    || stop 3 "$zone: the local branch $name cannot be moved (checked out in another worktree?): free it, then run the queue again."
  if [ -x scripts/bench/pre-vol.sh ] && ! prevol="$(scripts/bench/pre-vol.sh "$tour" "$zone" 2>&1)"; then
    printf '%s\n' "$prevol" | tail -15
    stop 3 "$zone: the pre-flight refuses the lot (pre-vol.sh $tour $zone): the portal is not disturbed."
  fi
  before="$(git rev-parse HEAD)"
  log="$(mktemp)"
  code=0
  judge_before="$(last_judge HEAD)"
  portal=(scripts/bench/fusion-equipes.sh "$tour" "$zone")
  [ ${#filters[@]} -eq 0 ] || portal+=(-- "${filters[@]}")
  "${portal[@]}" >"$log" 2>&1 || code=$?
  if [ "$code" -ne 0 ]; then
    line="$(grep -E "^$zone: " "$log" | tail -1 || true)"
    tail -20 "$log"
    [ "$(git rev-parse HEAD)" = "$before" ] || stop 3 "$zone: the portal failed (exit $code) and left the workshop moved: look before anything else (log: $log)."
    echo "Chef d'orchestre → <NOM> : le portail refuse $zone (sortie $code). Sa ligne exacte : « ${line:-voir $log} »."
    stop 3 "$zone: refused by the portal (exit $code); pushed before it: ${done_lots[*]:-none}."
  fi
  rm -f "$log"
  # The links' check, on what would be pushed.
  links="$(git ls-tree -r HEAD | awk '$1 == "120000" {print $4}' | tr '\n' ' ')"
  if [ -n "$links" ]; then
    git reset -q --keep "$before"
    stop 3 "$zone: symbolic link(s) committed after the merge: $links: undone, not pushed."
  fi
  push_head || stop 3 "$zone: merged here ($(git rev-parse --short HEAD)) but the push of $branch failed four times: push it before the next lot."
  done_lots+=("$zone")
  say "$zone: merged and pushed ($(git rev-parse --short HEAD)); judge: $(judge_of HEAD | grep . || echo '?') (before: ${judge_before:-?})."
done
say "queue done: ${done_lots[*]}. Batch ${paquet:-1}: at its end, the CI (scripts/bench/paquet-verifie.sh)."
