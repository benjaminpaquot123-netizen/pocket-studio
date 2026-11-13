#!/usr/bin/env bash
# Audit method v2 (META-AUDIT change 7): the merge agent's script. Merges the teams' branches into the integration
# workshop ONE AT A TIME, in the order given (smallest and most central first), each followed by its checks:
#   scripts/bench/fusion-equipes.sh <tour-N> <zone> [<zone> …] [-- <gradle test filters…>]
# For each zone: the fresh review of its branch must exist (docs/audits/ultra/<tour-N>-<zone>-relecture.md, written
# BEFORE the merge: a review after the push came too late in rounds 1-3), committed in the branch, ending on the line
# « Verdict : 0 bloquant, 0 grave » with no open blocking or grave row (scripts/bench/relecture-verdict.py), then
# `git merge --no-ff eq/<tour-N>-<zone>`, then the bench's self-test on the merged code, the replay tests of fixed
# defects (scripts/bench/rejeu-defauts.sh, never none) and the team's targeted tests through the one Gradle queue; then
# the portal's traces of the merge, committed (docs/audits/ultra/portail/<date>-<tour>-<zone>/). A red step stops everything: the merge is undone (git merge --abort / reset to the commit before)
# and the zone is named. Never pushes.
set -euo pipefail
int="$(cd "$(dirname "$0")/../.." && pwd)"
tour="${1:?tour-N}"; shift
zones=(); filters=()
while [ $# -gt 0 ]; do
  if [ "$1" = "--" ]; then shift; filters=("$@"); break; fi
  zones+=("$1"); shift
done
cd "$int"
traces_root="docs/audits/ultra/portail"
# Socle du banc, point 5: the rules are frozen per batch of merges. The batch in progress is in portail-paquet.txt; a
# rule added while batch N is open is written « from_paquet N+1 … » ON ITS refusal line, and only applies from the next
# batch (scripts/bench/test_socle.py checks it). Only the main discussion moves the number, when it opens a batch.
# (No batch file: batch 1. « || true »: a failing substitution would end this script silently under set -e.)
paquet="$(sed -n 's/^paquet: *\([0-9][0-9]*\).*/\1/p' scripts/bench/portail-paquet.txt 2>/dev/null | head -1 || true)"
from_paquet() { [ "${paquet:-1}" -ge "$1" ]; }
# The replay tests of a commit: the folders of docs/audits/ultra/rejeux/ (« _… » are kept failures, not tests).
replays() { git ls-tree --name-only "$1:docs/audits/ultra/rejeux" 2>/dev/null | grep -v '^_' | sort || true; }
[ -z "$(git status --porcelain --untracked-files=no | grep -v '^ T')" ] || { echo "The integration workshop has uncommitted changes: commit or set them aside first." >&2; exit 2; }
for zone in "${zones[@]}"; do
  review="docs/audits/ultra/$tour-$zone-relecture.md"
  # Audit « banc crédible » 2, P4: the review is read in the branch only (a file left in the workshop, never committed,
  # was accepted and then missing from the history).
  text="$(git show "eq/$tour-$zone:$review" 2>/dev/null)" || true
  [ -n "$text" ] || { echo "$zone: no fresh review ($review) committed in eq/$tour-$zone: not merged." >&2; exit 3; }
  # PLAN-RESTE 4.0: the review names the commit it read (« relu : <sha> ») and nothing but the review itself changed
  # after it; its verdict says 0 blocking and 0 grave.
  read_sha="$(printf '%s\n' "$text" | sed -n 's/^.*[Rr]elu *: *\([0-9a-f]\{7,40\}\).*$/\1/p' | head -1)"
  [ -n "$read_sha" ] && git cat-file -e "$read_sha^{commit}" 2>/dev/null \
    || { echo "$zone: the review does not name the commit it read (« relu : <sha> »): not merged." >&2; exit 3; }
  # (A file changed after the review only because the team merged fx-int in, i.e. now equal to fx-int's, is not the team's.)
  after=""
  for f in $(git diff --name-only "$read_sha" "eq/$tour-$zone" | grep -vxF "$review" || true); do
    git diff --quiet HEAD "eq/$tour-$zone" -- "$f" || after="$after $f"
  done
  [ -z "$after" ] || { echo "$zone: changed after the review ($read_sha): $after: not merged." >&2; exit 3; }
  # Audit « banc crédible » 2, P1: the verdict is the review's LAST line, no table row « bloquant »/« grave » is left
  # without « corrigé », and each replay the branch takes out of rejeux/ is named with its reason (relecture-verdict.py).
  base="$(git merge-base HEAD "eq/$tour-$zone")"
  removed="$(comm -23 <(replays "$base") <(replays "eq/$tour-$zone"))"
  # Review of the « portail » lot (M5): a replay kept but whose files the branch changes (its oracle, its gestures)
  # needs « rejeu changé : <id> : <raison> » too.
  changed="$(comm -12 <(replays "$base") <(replays "eq/$tour-$zone") | while read -r id; do
    git diff --quiet "$base" "eq/$tour-$zone" -- "docs/audits/ultra/rejeux/$id" || echo "changé:$id"; done)"
  # Second review of the « portail » lot (G2): red evidence (rouges-constates/<sha>/) the branch brings or changes is
  # named in its review (« rouge constaté : <sha> : rejoué sur <parent> : … »; third review, M9: the parent passed).
  evidence="$(git diff --name-only "$base" "eq/$tour-$zone" -- docs/audits/ultra/rouges-constates \
    | sed -n 's#^docs/audits/ultra/rouges-constates/\([^/]*\)/.*#\1#p' | sort -u | while read -r sha; do
      echo "constat:$sha:$(git rev-parse -q --verify "$sha^1" 2>/dev/null || true)"; done)"
  # shellcheck disable=SC2086
  why="$(printf '%s\n' "$text" | python3 -I scripts/bench/relecture-verdict.py $removed $changed $evidence)" \
    || { echo "$zone: review refused: $why: not merged." >&2; exit 3; }
  # Only this script writes the portal's traces (D1): a branch never brings or changes one.
  touched="$(git diff --name-only "$base" "eq/$tour-$zone" -- "$traces_root")"
  [ -z "$touched" ] || { echo "$zone: the branch touches the portal's traces: $touched: not merged." >&2; exit 3; }
  # Socle du banc, point 2: a lot writes its decisions in docs/decisions/<tour-N>-<zone>.md only (never in
  # docs/DECISIONS.md, which the portal alone extends, nor in another lot's file).
  decisions="$(git diff --name-only "$base" "eq/$tour-$zone" -- docs/DECISIONS.md docs/decisions | grep -vxF "docs/decisions/$tour-$zone.md" | tr '\n' ' ' || true)"
  if from_paquet 2 && [ -n "$decisions" ]; then echo "$zone: decisions written outside docs/decisions/$tour-$zone.md (docs/DECISIONS.md or another lot's file): $decisions: not merged." >&2; exit 3; fi
  # Socle du banc (review, moyen 3): only the main discussion moves the batch, when it opens one.
  moved_batch="$(git diff --name-only "$base" "eq/$tour-$zone" -- scripts/bench/portail-paquet.txt)"
  if from_paquet 2 && [ -n "$moved_batch" ]; then echo "$zone: the branch moves the batch (scripts/bench/portail-paquet.txt): not merged." >&2; exit 3; fi
  # PLAN-RESTE 7.1 (from batch 2): the zone's report, read in the branch (docs/audits/ultra/<tour-N>-<zone>-rapport.md):
  # its « tests: » line gives the team's targeted tests, added to the filters given here; its list of paths names a
  # test of app/src/test for each place; each replay the branch adds to rejeux/ has its last verdict there, « oracle
  # tenu » (scripts/bench/rapport-zone.py).
  report="docs/audits/ultra/$tour-$zone-rapport.md"
  added="$(comm -13 <(replays "$base") <(replays "eq/$tour-$zone") | sed 's/^/ajout:/')"
  report_out=""
  # shellcheck disable=SC2086
  if from_paquet 2 && ! report_out="$(python3 -I scripts/bench/rapport-zone.py "eq/$tour-$zone" "$report" $added)"; then echo "$zone: zone report refused: $(printf '%s' "$report_out" | tr '\n' ';'): not merged." >&2; exit 7; fi
  # AUDIT-ULTRA §13.9 (lot « portail3 », from batch 4): a lot that says it fixes a blocking or grave defect adds its
  # replay. One line per defect fixed in the zone report, « corrige: <id> | gravité: … | rejeu: <id du rejeu> » (or
  # « … | rejeu impossible : <raison> »); a grave the tables know (verdicts.tsv turned « corrigé », the round's triage
  # named by the report or a commit) cannot go without its line (scripts/bench/rapport-zone.py --corrections).
  fix_out=""
  # shellcheck disable=SC2086
  if from_paquet 4 && ! fix_out="$(python3 -I scripts/bench/rapport-zone.py --corrections "eq/$tour-$zone" "$report" "base:$base" "tete:$(git rev-parse HEAD)" "tour:$tour" $added)"; then echo "$zone: fix without its replay: $(printf '%s' "$fix_out" | tr '\n' ';'): not merged." >&2; exit 8; fi
  # AUDIT-ULTRA §13.5, last point (lot « portail4 », from batch 7): no lot of the tutorial enters before a fresh
  # beginner replayed it on the lot's branch. A lot that changes the tutorial zone (code and resources of src/main:
  # …/tuto/, …/tutos/, i18n/*/tuto*.json; PianoRoll.kt when its report says « tuto ») carries, per control seat,
  # « controle: <id> | code testé: <sha> | bloqués: 0 | graves neufs: 0 » in its zone report: <sha> a commit of the
  # branch not older than its last change of the tutorial, <id> a seat whose report is in the branch and names the
  # same commit (scripts/bench/rapport-zone.py, its « --tuto » mode). A lot outside the zone: nothing asked, nothing said.
  tuto_out=""
  if from_paquet 7 && ! tuto_out="$(python3 -I scripts/bench/rapport-zone.py --tuto "eq/$tour-$zone" "$report" "base:$base" "tete:$(git rev-parse HEAD)" "tour:$tour")"; then echo "$zone: tutorial lot without its control seat: $(printf '%s' "$tuto_out" | tr '\n' ';'): not merged." >&2; exit 10; fi
  # PLAN-REPARATIONS-CONSTATEES.md, rule 5 (lot « constate », from batch 6): a repair is « done » only when a tester saw
  # it. In docs/audits/ultra/corrections.tsv a lot writes « tenté » (or « pas pris »), never « corrigé », « rejoué » (the
  # portal's, set below after the merge) nor « constaté » (the chief's, after a control seat); nor the sorter's
  # acceptance sentence or seat of origin. Its own lines are those neither the base nor the integration's head has
  # (scripts/bench/reparations.py --lot).
  states_out=""
  if from_paquet 6 && ! states_out="$(python3 -I scripts/bench/reparations.py --lot "eq/$tour-$zone" "base:$base" "tete:$(git rev-parse HEAD)")"; then echo "$zone: the lot gives itself a state of the repairs register: $(printf '%s' "$states_out" | tr '\n' ';'): not merged." >&2; exit 11; fi
  zone_filters=("${filters[@]}")
  while IFS= read -r f; do [ -z "$f" ] || zone_filters+=("$f"); done < <(printf '%s\n' "$report_out" | sed -n 's/^filtre: //p')
  # PLAN-RESTE 7.1: a team branch never brings a symbolic link (a third-party checkout linked in, a secret linked out).
  links="$(git ls-tree -r "eq/$tour-$zone" | awk '$1 == "120000" {print $4}')"
  [ -z "$links" ] || { echo "$zone: symbolic link(s) in the branch: $links: not merged." >&2; exit 6; }
  before="$(git rev-parse HEAD)"
  if ! git merge --no-ff -m "Merge team $zone of $tour (audit ultra)" "eq/$tour-$zone"; then
    git merge --abort || true
    echo "$zone: merge conflict: undone. The team rebases on fx-int and comes back." >&2; exit 4
  fi
  # Audit « banc crédible » 2, P1: a merge without any replay test is refused (moving the one replay out of rejeux/
  # took the self-test away with it).
  if [ -z "$(replays HEAD)" ]; then
    git reset --keep "$before"
    echo "$zone: no replay test left in docs/audits/ultra/rejeux/ after the merge: undone." >&2; exit 5
  fi
  args=(:app:testDebugUnitTest)
  for f in "${zone_filters[@]}"; do args+=(--tests "$f"); done
  # The self-test (every Bench*, Tour*AppProof*, Tour*IdleTest and @KnownDefect class, the bench's scripts, the judge)
  # always runs on the merged code: PLAN-RESTE 7.7, once, as the gate of the repository's replays' first lot (never
  # PS_REJEUX_DIR's), whose summary, stamp, judge and JUnit results rejeu-defauts.sh leaves in $lot for the traces;
  # then the team's filters.
  lot="$(mktemp -d)"
  green=1
  env -u PS_REJEUX_DIR PS_REJEUX_SELFTEST="$lot" scripts/bench/rejeu-defauts.sh > "$lot/rejeux.txt" 2>&1 || green=0
  cat "$lot/rejeux.txt"
  [ -f "$lot/selftest.ok" ] || green=0
  if [ "$green" -eq 1 ] && [ ${#zone_filters[@]} -gt 0 ]; then scripts/bench/gradle-file.sh "${args[@]}" || green=0; fi
  if [ "$green" -eq 0 ]; then
    git reset --keep "$before"
    echo "$zone: red after the merge (self-test, replays or tests; kept in $lot): undone." >&2; exit 5
  fi
  # Audit « banc crédible » 2, D1: the portal's traces of this merge (the self-test's summary, stamp and judge, and
  # fusion.txt) are committed right after it, so every figure quoted later can be checked; verifie-chantier.py counts
  # « fusion sans trace du portail » for a merge without them.
  merged="$(git rev-parse HEAD)"
  # Review of the « portail » lot (M2): any step that fails from here to the traces' commit undoes the merge too, and
  # leaves no half-written traces.
  trace=""
  computed=()
  # (What a failed step after the checks takes back: the trace folder, the computed files, the merge itself.)
  clean_merge() {
    [ -z "$trace" ] || rm -rf "${trace:?}"
    # The files fuse-reports.py and tableau-de-bord.py wrote: back to the merge's version, or gone when it had none.
    for f in "${computed[@]}"; do
      if git cat-file -e "HEAD:$f" 2>/dev/null; then git checkout -q HEAD -- "$f" 2>/dev/null || true; else rm -f -- "$f"; fi
    done
    # From HEAD, not the index: a failed traces' commit after « git add » has the index's line staged already.
    git checkout -q HEAD -- docs/DECISIONS.md 2>/dev/null || true
    git reset -q --keep "$before"
  }
  undo_merge() {
    clean_merge
    echo "$zone: a step after the checks failed (traces of the portal): undone (kept in $lot)." >&2
    exit 5
  }
  trap undo_merge ERR
  trace="$traces_root/$(date -u +%F)-$tour-$zone"
  n=2
  while [ -e "$trace" ]; do trace="$traces_root/$(date -u +%F)-$tour-$zone-$n"; n=$((n + 1)); done
  mkdir -p "$trace"
  cp "$lot/selftest-resume.txt" "$lot/selftest.ok" "$lot/selftest-chantier.txt" "$trace/"
  { echo "fusion: $merged"; echo "branche: eq/$tour-$zone"; echo "tete: $(git rev-parse "eq/$tour-$zone")"
    echo "relecture: $review"; echo "relu: $read_sha"; [ ${#zone_filters[@]} -eq 0 ] || echo "filtres: ${zone_filters[*]}"
    echo "rejeux :"; sed 's/^/  /' "$lot/rejeux.txt"; } > "$trace/fusion.txt"
  # The judge again, with the traces there (the self-test's ran before them and counted this merge « sans trace »);
  # it reads the lot's summary, sealed with its JUnit results (the trace keeps no XML).
  python3 -I scripts/bench/verifie-chantier.py --resume "$lot/selftest-resume.txt" > "$trace/selftest-chantier.txt" 2>&1 || true
  # AUDIT-ULTRA §13.9 (lot « portail3 », from batch 4): a lot never makes the judge go up. Its count here, the traces
  # in place, against the count of the previous portal trace: the one of the last « Portal traces of the merge … »
  # commit before this merge, found by the history (the folders' names sort by date and zone, not by time). The gaps
  # named are the new ones: lines the previous trace did not have, the shas cut to 7 letters on both sides (older
  # traces print 9 letters, newer ones 8). Nothing to compare with (no previous trace, or one without its count):
  # said, and the lot goes on. A judge without its « chantier : » line counts as a judge gone up.
  count_of() { sed -n 's/^chantier : \([0-9][0-9]*\) écart.*/\1/p' | tail -1; }
  short_shas() { sed -E 's/\b([0-9a-f]{7})[0-9a-f]{1,33}\b/\1/g'; }
  judge_now="$(count_of < "$trace/selftest-chantier.txt")"
  prev_commit="$(git log -1 --format=%H --grep='^Portal traces of the merge ' "$before" -- "$traces_root" || true)"
  prev_file=""; prev_text=""
  [ -z "$prev_commit" ] || prev_file="$(git diff-tree --no-commit-id --name-only -r "$prev_commit" -- "$traces_root" | grep '/selftest-chantier\.txt$' | head -1 || true)"
  [ -z "$prev_file" ] || prev_text="$(git show "$prev_commit:$prev_file" 2>/dev/null || true)"
  judge_prev="$(printf '%s\n' "$prev_text" | count_of)"
  judge_why=""
  if [ -z "$judge_now" ]; then
    judge_why="the judge printed no « chantier : N écarts » line"
  elif [ -z "$prev_commit" ]; then
    echo "$zone: judge: $judge_now écarts (no previous portal trace: not compared)"
  elif [ -z "$judge_prev" ]; then
    echo "$zone: judge: $judge_now écarts (the previous portal trace, of $(git rev-parse --short "$prev_commit"), has no « chantier : » line: not compared)"
  else
    echo "$zone: judge: $judge_now écarts (previous portal trace: $judge_prev, $(dirname "$prev_file"))"
    if [ "$judge_now" -gt "$judge_prev" ]; then
      # (Each line as the judge printed it, beside its form with short shas: the second is compared, the first said.)
      new_gaps="$(grep -v '^chantier : ' "$trace/selftest-chantier.txt" | short_shas | paste -d '\t' <(grep -v '^chantier : ' "$trace/selftest-chantier.txt") - \
        | awk -F '\t' 'NR == FNR { seen[$0] = 1; next } !($2 in seen) { print $1 }' <(printf '%s\n' "$prev_text" | short_shas) - | head -12 | tr '\n' ';' || true)"
      judge_why="the judge goes up ($judge_prev → $judge_now écarts since $(dirname "$prev_file")); new gap(s): $new_gaps"
    fi
  fi
  if [ -n "$judge_why" ]; then
    # (The judge's lines of this merge stay in the lot, the trace folder going away with the merge.)
    cp "$trace/selftest-chantier.txt" "$lot/selftest-chantier-apres.txt"
    if from_paquet 4; then clean_merge; echo "$zone: $judge_why: undone (kept in $lot)." >&2; exit 9; fi
    echo "$zone: $judge_why — refused from batch 4 (batch in progress: ${paquet:-1})"
  fi
  # Socle du banc, point 2: the lot's decisions file, listed once at the end of docs/DECISIONS.md.
  scripts/bench/decisions-index.sh "$tour" "$zone"
  index=(); [ ! -f docs/DECISIONS.md ] || index=(docs/DECISIONS.md)
  # PLAN-REPARATIONS-CONSTATEES.md, rule 5 (lot « constate », from batch 6): « rejoué » is the portal's. Each line of
  # docs/audits/ultra/corrections.tsv the lot brought in « tenté » whose proof held on the merged head (a replay of
  # rejeux/ « oracle tenu » in this merge's self-test, or a test among the filters run green above) becomes « rejoué »,
  # written in the traces' commit (taken back with the merge when a later step fails).
  if from_paquet 6 && [ -f docs/audits/ultra/corrections.tsv ] && [ -f scripts/bench/reparations.py ]; then
    computed+=(docs/audits/ultra/corrections.tsv)
    proofs=(); for f in "${zone_filters[@]}"; do proofs+=("filtre:$f"); done
    python3 -I scripts/bench/reparations.py --rejoue "eq/$tour-$zone" "base:$base" "tete:$before" "rejeux:$lot/rejeux.txt" "fusion:$merged" "${proofs[@]}" | sed "s/^/$zone: /"
    git diff --quiet -- docs/audits/ultra/corrections.tsv || index+=(docs/audits/ultra/corrections.tsv)
  fi
  # PLAN-RESTE 7.1: the round's reports fused again (the new keys into base.tsv) and the dashboard recomputed on every
  # round, on the merged code; what they write goes into the traces' commit.
  round="docs/audits/ultra/sieges/$tour"
  # (Its failure undoes the merge: a rule of the portal, frozen until batch 2 like the others.)
  if from_paquet 2 && [ -d "$round" ] && [ -f scripts/bench/fuse-reports.py ] && [ -f scripts/bench/tableau-de-bord.py ]; then
    computed+=(docs/audits/ultra/base.tsv "$round/clusters.md" "$round/clusters-complet.md" "$round/promesses.tsv" "$round/incidents-banc.tsv")
    python3 -I scripts/bench/fuse-reports.py "$round" --tour "$tour" --maj-base > /dev/null
    mapfile -t rounds < <(find docs/audits/ultra/sieges -mindepth 1 -maxdepth 1 -type d -name 'tour-[0-9]*' | sort -V)
    computed+=("${rounds[-1]}/tableau-de-bord.md")
    # (Its trace folder and DECISIONS.md, committed with the dashboard just below, are not « fichiers non commités ».)
    PS_PORTAIL_ECRIT="$trace ${index[*]}" python3 -I scripts/bench/tableau-de-bord.py "${rounds[@]}" > /dev/null
    for f in "${computed[@]}"; do if [ -f "$f" ]; then index+=("$f"); fi; done
  fi
  git add -- "$trace" "${index[@]}"
  git commit -q -m "Portal traces of the merge of $zone ($tour)" -- "$trace" "${index[@]}"
  trap - ERR
  rm -rf "${lot:?}"
  echo "$zone: merged ($(git rev-parse --short "$merged")), traces in $trace"
done
