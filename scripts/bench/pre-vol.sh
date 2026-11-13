#!/usr/bin/env bash
# Pre-flight (« pré-vol »): everything that would get a merge or a CI run refused, said at once, in seconds, BEFORE
# asking for it. Reads only: never merges, never commits, never touches the working files or the index.
#
#   scripts/bench/pre-vol.sh <tour-N> <zone> [<zone> …]   before scripts/bench/fusion-equipes.sh (the portal): every
#                                                        reason the portal would refuse each zone, not just the first,
#                                                        the zones tried in the order given, each on top of the others
#   scripts/bench/pre-vol.sh --avant-relecture <tour-N> <zone> [<zone> …]
#                                                        the same BEFORE the fresh review is asked for: the review is
#                                                        not looked for, and a branch whose base has moved is refused
#                                                        (a conflict solved after the review costs a second review)
#   scripts/bench/pre-vol.sh --main [<ref>]              before a pull request to main (default ref: origin/main)
#   scripts/bench/pre-vol.sh --ci [--long]               before launching the CI by hand: its short, late steps run
#                                                        here first, as on a brand new runner; the engine's tests when
#                                                        the engine changed since they last passed here; --long adds
#                                                        scripts/real-projects.sh
#   scripts/bench/pre-vol.sh --ci --rapide               the CI's own first job (« quick »): only the checks of a few
#                                                        seconds (bench tests, the pre-flight's test, the workflows);
#                                                        nothing about pushes, the CI's state or the engine
#
# Exit 0: nothing found (« PRÊT »). Exit 1: at least one « RECALÉ » line. « À VOIR » lines never change the exit code.
# The portal stays the judge: this script repeats its checks (docs/AIDE-FUSIONS-TESTS.md) and warns when the portal's
# rules have changed since it was written.
set -uo pipefail
cd "$(dirname "$0")/../.." || exit 2

n_bad=0
n_warn=0
bad() { n_bad=$((n_bad + 1)); printf '  RECALÉ  %s\n' "$1"; [ -z "${2:-}" ] || printf '          → %s\n' "$2"; }
warn() { n_warn=$((n_warn + 1)); printf '  À VOIR  %s\n' "$1"; [ -z "${2:-}" ] || printf '          → %s\n' "$2"; }
ok() { printf '  ok      %s\n' "$1"; }
verdict() {
  echo
  if [ "$n_bad" -gt 0 ]; then
    echo "PRÉ-VOL : RECALÉ ($n_bad à corriger, $n_warn à voir). Ne rien lancer avant d'avoir corrigé."
    exit 1
  fi
  echo "PRÉ-VOL : PRÊT ($n_warn à voir)."
  exit 0
}
# « git merge-tree --write-tree » (git 2.38 and later) merges in memory. Prints the merged tree, then the files in
# conflict; returns 0 clean, 1 conflicts, 2 when this git cannot do it, 3 when the two have no common ancestor (a real
# merge refuses those).
dry_merge() {
  local out rc=0
  git merge-base "$1" "$2" >/dev/null 2>&1 || return 3
  out="$(git merge-tree --write-tree --name-only "$1" "$2" 2>/dev/null)" || rc=$?
  [ "$rc" -le 1 ] || return 2
  printf '%s\n' "$out" | sed -n '1p;2,/^$/p' | sed '/^$/d'
  return "$rc"
}

# --- Before a pull request to main -----------------------------------------------------------------------------------
if [ "${1:-}" = "--main" ]; then
  ref="${2:-origin/main}"
  echo "== Avant une fusion vers main ($ref)"
  case "$ref" in origin/*) git fetch -q origin "${ref#origin/}" 2>/dev/null || warn "$ref n'a pas pu être rafraîchi : contrôle fait sur sa dernière copie connue" ;; esac
  git rev-parse -q --verify "$ref^{commit}" >/dev/null || { bad "$ref est introuvable"; verdict; }
  [ -z "$(git status --porcelain --untracked-files=no)" ] || warn "des changements ne sont pas commités : ils ne partiront pas dans la fusion"
  behind="$(git rev-list --count "HEAD..$ref")"
  merged="$(dry_merge "$ref" HEAD)"
  rc=$?
  if [ "$rc" -eq 0 ]; then
    ok "la branche se fusionne dans $ref sans conflit ($behind commit(s) de $ref pas encore ici)"
  elif [ "$rc" -eq 2 ]; then
    warn "ce git ne sait pas fusionner à blanc (2.38 au moins) : conflits non vérifiés"
  elif [ "$rc" -eq 3 ]; then
    bad "cette branche et $ref n'ont aucun ancêtre commun : git refusera la fusion" "vérifier le nom de la branche visée ; ne jamais forcer avec --allow-unrelated-histories sans l'accord de Benjamin"
  else
    files="$(printf '%s\n' "$merged" | sed 1d)"
    count="$(printf '%s\n' "$files" | grep -c .)"
    # A squash merge leaves main with the content of one of this branch's own commits: main then brings nothing new,
    # and every file changed since is a conflict. Recording main as merged, without taking anything, is exact.
    same="$(git log --format='%H %T' HEAD | awk -v t="$(git rev-parse "$ref^{tree}")" '$2 == t {print $1; exit}')"
    if [ -n "$same" ]; then
      bad "$count fichier(s) en conflit avec $ref, alors que $ref n'apporte rien de neuf : son contenu est exactement celui du commit $(git rev-parse --short "$same") de cette branche (fusion « squash »)" \
        "git merge -s ours $ref -m \"Record $ref as merged: same tree as $(git rev-parse --short "$same")\"   (aucun fichier ne change), puis relancer ce contrôle"
    else
      bad "$count fichier(s) en conflit avec $ref : $(printf '%s\n' "$files" | head -8 | tr '\n' ' ')$([ "$count" -le 8 ] || echo …)" \
        "fusionner $ref ici (git merge $ref), résoudre, rejouer les tests des fichiers touchés, puis relancer ce contrôle"
    fi
  fi
  verdict
fi

# --- Before launching the CI by hand ---------------------------------------------------------------------------------
if [ "${1:-}" = "--ci" ]; then
  long=0; quick=0
  for a in "${@:2}"; do
    case "$a" in
      --long) long=1 ;;
      --rapide) quick=1 ;;
      *) echo "pre-vol --ci : option inconnue « $a » (--long, --rapide)" >&2; exit 2 ;;
    esac
  done
  if [ "$quick" -eq 1 ]; then
    echo "== Contrôles rapides ($(git rev-parse --short HEAD))"
  else
    echo "== Avant de lancer la CI à la main ($(git rev-parse --short HEAD))"
    # (Not a linked third-party folder, « T » in git status, nor the portal's kept failures: as for « tour-N <zone> ».)
    [ -z "$(git status --porcelain --untracked-files=no | grep -v '^ T' | grep -v ' docs/audits/ultra/rejeux/_echecs/')" ] \
      || bad "des changements ne sont pas commités : la CI ne les verra pas" "committer, ou les mettre de côté"
  fi
  if [ "$quick" -eq 1 ]; then
    :
  elif git rev-parse -q --verify '@{upstream}' >/dev/null 2>&1; then
    [ "$(git rev-list --count '@{upstream}..HEAD')" -eq 0 ] || bad "des commits ne sont pas poussés : la CI tournerait sur l'ancien code" "git push, puis lancer la CI"
  else
    warn "cette branche ne suit aucune branche distante : vérifier que tout est poussé"
  fi
  # The same commit already red on GitHub: launching it again cannot turn it green.
  if [ "$quick" -eq 0 ] && [ -x scripts/bench/ci-etat.sh ]; then
    state="$(scripts/bench/ci-etat.sh HEAD 2>/dev/null | sed -n 's/^ci_build //p')"
    case "$state" in
      rouge) bad "la CI de ce commit est déjà rouge : la relancer telle quelle donnera le même rouge" "lire l'étape en échec, la reproduire ici, corriger, committer, puis seulement relancer" ;;
      "en cours") bad "une CI tourne déjà pour ce commit" "attendre son résultat" ;;
      vert) warn "la CI de ce commit est déjà verte : rien à relancer" ;;
    esac
  fi
  # The short checks the CI only reaches after an hour of tests, run here as a brand new runner would: an empty home,
  # no git identity, no user settings (a test that needs one fails here in seconds instead of there after an hour).
  bare="$(mktemp -d)"
  run_bare() { # <label> <command…>
    local label="$1" log
    shift
    log="$bare/$(echo "$label" | tr -c 'a-zA-Z0-9' _).log"
    if env -i PATH="$PATH" HOME="$bare" LANG=C.UTF-8 GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_NOSYSTEM=1 "$@" > "$log" 2>&1; then
      ok "$label"
    else
      bad "$label : rouge dans les conditions d'un runner neuf (journal : $log)" "$(grep -m3 -E '^FAIL|^ERROR|Error:|error:|fatal:|AssertionError' "$log" | cut -c1-160 | tr '\n' ' ')"
    fi
  }
  found=0
  for t in scripts/bench/test_socle.py scripts/bench/test_cpu_report.py scripts/bench/test_ultra_bench.py scripts/bench/test_report.py scripts/bench/test_defauts_caches.py; do
    [ -f "$t" ] || continue
    found=1
    run_bare "$t" python3 -I "$t"
  done
  # The pre-flight's own test (and, through it, the portal's refusals).
  [ ! -f scripts/bench/test_pre_vol.sh ] || run_bare "scripts/bench/test_pre_vol.sh" bash scripts/bench/test_pre_vol.sh
  [ "$found" -eq 1 ] || warn "aucun test Python du banc ici (scripts/bench/test_*.py)"
  # (scripts/check-jni.sh needs a built engine: only where there is one.)
  if [ -x scripts/check-jni.sh ] && [ -n "$(find engine/build -path '*release*' -name NativeEngine.class 2>/dev/null | head -1)" ]; then
    run_bare "scripts/check-jni.sh" scripts/check-jni.sh
  fi
  python3 -I -c 'import PIL' 2>/dev/null || warn "Pillow manque ici pour python3 -I (la CI l'installe ; le banc « regarde » en a besoin)"
  # A workflow file that no longer parses stops every job at once.
  for y in .github/workflows/*.yml; do
    [ -f "$y" ] || continue
    python3 -I -c 'import sys, yaml; yaml.safe_load(open(sys.argv[1]))' "$y" 2>/dev/null || {
      python3 -I -c 'import yaml' 2>/dev/null && bad "$y ne se lit plus (YAML invalide)" "corriger le fichier avant tout lancement"
    }
  done
  # The quick mode stops here: the engine has its own job in the CI, the real projects their own step.
  [ "$quick" -eq 0 ] || verdict
  # The Gradle guard rail (PLAN-RESTE 7.2): its own test, about 15 s, needs a Gradle distribution already here.
  # Run with this home (it reads the Gradle distribution cached there; it never touches ~/.gradle); a skip is said.
  if [ -f scripts/bench/test_garde_gradle.sh ]; then
    if bash scripts/bench/test_garde_gradle.sh > "$bare/garde-gradle.log" 2>&1; then
      if grep -q '^SKIP' "$bare/garde-gradle.log"; then warn "scripts/bench/test_garde_gradle.sh : sauté ($(grep -m1 '^SKIP' "$bare/garde-gradle.log" | cut -c7-))"
      else ok "scripts/bench/test_garde_gradle.sh"; fi
    else
      bad "scripts/bench/test_garde_gradle.sh : rouge (journal : $bare/garde-gradle.log)" "$(grep -m3 -E '^(ROUGE|FAIL)' "$bare/garde-gradle.log" | cut -c1-160 | tr '\n' ' ')"
    fi
  fi
  free_gb="$(df -BG --output=avail . | tail -1 | tr -dc 0-9)"
  [ "${free_gb:-0}" -ge 4 ] || warn "disque : ${free_gb} Go libres ici (les tests longs en demandent 4)"
  # The engine's tests on Linux: every other job of the CI waits for them (« needs: engine »), so a red there costs
  # the whole run. Run here only when the engine or its script changed since they last passed here.
  if [ -x scripts/host-tests.sh ]; then
    stamp="build/pre-vol/host-tests.ok"
    engine="$(git rev-parse HEAD:engine 2>/dev/null) $(git rev-parse HEAD:scripts/host-tests.sh 2>/dev/null)"
    if [ -f "$stamp" ] && [ "$(cat "$stamp")" = "$engine" ] && git diff --quiet HEAD -- engine scripts/host-tests.sh; then
      ok "scripts/host-tests.sh : déjà verts ici pour ce moteur"
    else
      echo "  …       scripts/host-tests.sh (le moteur a changé depuis leur dernier vert ici)"
      if scripts/host-tests.sh > "$bare/host-tests.log" 2>&1; then
        mkdir -p "$(dirname "$stamp")" && echo "$engine" > "$stamp"
        ok "scripts/host-tests.sh"
      else
        bad "scripts/host-tests.sh : rouge (journal : $bare/host-tests.log)" "$(grep -m3 -E 'FAILED|FAIL:|error:|Error' "$bare/host-tests.log" | cut -c1-160 | tr '\n' ' ')"
      fi
    fi
  fi
  if [ "$long" -eq 1 ] && [ -x scripts/real-projects.sh ]; then
    echo "  …       scripts/real-projects.sh (long)"
    if scripts/real-projects.sh > "$bare/real-projects.log" 2>&1; then
      ok "scripts/real-projects.sh"
    else
      bad "scripts/real-projects.sh : rouge (journal : $bare/real-projects.log)" "$(grep -m3 -E 'FAILED|FAIL:|ALERTE|error:|Error' "$bare/real-projects.log" | cut -c1-160 | tr '\n' ' ')"
    fi
  else
    echo "  (scripts/real-projects.sh n'est pas lancé : « --ci --long » pour l'ajouter)"
  fi
  verdict
fi

# --- Before the portal (scripts/bench/fusion-equipes.sh) -------------------------------------------------------------
early=0
[ "${1:-}" = "--avant-relecture" ] && { early=1; shift; }
tour="${1:?usage: pre-vol.sh [--avant-relecture] <tour-N> <zone> [<zone> …] | --main [<ref>] | --ci [--long]}"
shift
[ $# -gt 0 ] || { echo "usage: pre-vol.sh [--avant-relecture] <tour-N> <zone> [<zone> …]" >&2; exit 2; }
traces_root="docs/audits/ultra/portail"
portal="scripts/bench/fusion-equipes.sh"
[ -f "$portal" ] && [ -f scripts/bench/relecture-verdict.py ] || { echo "pre-vol : le portail ($portal, relecture-verdict.py) n'est pas dans ce dépôt : à lancer depuis l'atelier d'intégration." >&2; exit 2; }
# The portal's refusals as this script knew them (their count and a fingerprint of their lines): a rule added or
# reworded since is said, never silently missed.
KNOWN_RULES="16 bf1142b44d"
rules="$(grep -E 'not merged|undone' "$portal" | grep -c .) $(grep -E 'not merged|undone' "$portal" | sed 's/^ *//' | sha256sum | cut -c1-10)"
[ "$rules" = "$KNOWN_RULES" ] || warn "le portail a changé depuis l'écriture du pré-vol (règles : $rules, connues : $KNOWN_RULES)" \
  "relire les lignes « not merged » / « undone » de $portal, mettre à jour pre-vol.sh et sa ligne KNOWN_RULES"
replays() { git ls-tree --name-only "$1:docs/audits/ultra/rejeux" 2>/dev/null | grep -v '^_' | sort || true; }
# The batch of merges in progress (the portal's rules are frozen per batch: « from_paquet N » in the portal).
paquet="$(sed -n 's/^paquet: *\([0-9][0-9]*\).*/\1/p' scripts/bench/portail-paquet.txt 2>/dev/null | head -1)"
paquet="${paquet:-1}"
# A rule of the portal that starts with batch <N>: refused from then on, said before (« à partir du paquet N »).
rule_from() { # <N> <message> <fix>
  if [ "$paquet" -ge "$1" ]; then bad "$2" "$3"; else warn "$2 — refusé à partir du paquet $1 (paquet en cours : $paquet)" "$3"; fi
}

echo "== Atelier d'intégration ($(git rev-parse --abbrev-ref HEAD), $(git rev-parse --short HEAD))"
if [ -z "$(git status --porcelain --untracked-files=no | grep -v '^ T')" ]; then
  ok "rien d'en cours dans l'atelier d'intégration"
else
  bad "l'atelier d'intégration a des changements non commités" "les committer ou les mettre de côté : le portail refuse de démarrer"
fi
free_gb="$(df -BG --output=avail . | tail -1 | tr -dc 0-9)"
[ "${free_gb:-0}" -ge 4 ] || warn "disque : ${free_gb} Go libres (le selftest du portail tombe rouge sous 2 Go)" "libérer de la place avant le portail (app/build des ateliers finis)"
# AUDIT-ULTRA §13.9: the portal plays the judge again after the merge and takes the merge back when its count went up
# since the previous portal trace (from batch 4). Not checked here (the judge reads the merged tree, its traces and
# the last self-test's summary): said once, with the count to stay under.
last_count="$(c="$(git log -1 --format=%H --grep='^Portal traces of the merge ' HEAD -- "$traces_root" 2>/dev/null)"; [ -z "$c" ] || git show "$c" -- "$traces_root" 2>/dev/null | sed -n 's/^+chantier : \([0-9][0-9]*\) écart.*/\1/p' | tail -1)"
[ -z "$last_count" ] || echo "  …       juge du chantier : $last_count écarts à la dernière trace du portail ; le portail annule la fusion d'un lot qui le fait monter (à partir du paquet 4, non contrôlé ici)"

head="$(git rev-parse HEAD)" # moves on, in memory, as each clean zone is merged
for zone in "$@"; do
  branch="eq/$tour-$zone"
  review="docs/audits/ultra/$tour-$zone-relecture.md"
  echo "== $tour $zone ($branch)"
  git rev-parse -q --verify "$branch^{commit}" >/dev/null || { bad "la branche $branch n'existe pas ici"; continue; }
  base="$(git merge-base "$head" "$branch")" || base="$branch"
  # Work left uncommitted in the team's workshop never reaches the portal.
  shop="$(git worktree list --porcelain | awk -v b="refs/heads/$branch" '$1 == "worktree" {w = $2} $1 == "branch" && $2 == b {print w}')"
  # (« T »: a file the workshop links, a linked submodule of engine/third_party/…, as in the integration's own check.)
  if [ -n "$shop" ] && [ -d "$shop" ] && [ -n "$(git -C "$shop" status --porcelain --untracked-files=no 2>/dev/null | grep -v '^ T')" ]; then
    bad "l'atelier de l'équipe ($shop) a des changements non commités : ils ne seraient pas fusionnés" "committer dans la branche (avant la relecture), ou les jeter"
  fi

  # 1. The fresh review: committed in the branch, names the commit it read, nothing changed after it.
  text="$(git show "$branch:$review" 2>/dev/null)" || text=""
  if [ "$early" -eq 1 ]; then
    ok "relecture : pas encore demandée (contrôle d'avant relecture)"
  elif [ -z "$text" ]; then
    bad "pas de relecture neuve commitée dans la branche ($review)" "faire relire par un agent neuf qui n'a pas écrit le code ; le fichier se committe DANS la branche, avant le portail"
  else
    read_sha="$(printf '%s\n' "$text" | sed -n 's/^.*[Rr]elu *: *\([0-9a-f]\{7,40\}\).*$/\1/p' | head -1)"
    if [ -z "$read_sha" ] || ! git cat-file -e "$read_sha^{commit}" 2>/dev/null; then
      bad "la relecture ne nomme pas le commit qu'elle a lu (ligne « relu : <sha> »${read_sha:+ ; « $read_sha » est inconnu ici})" "ajouter « relu : <sha du commit relu> » dans $review"
    else
      after=""
      for f in $(git diff --name-only "$read_sha" "$branch" | grep -vxF "$review" || true); do
        git diff --quiet "$head" "$branch" -- "$f" || after="$after $f"
      done
      if [ -z "$after" ]; then
        ok "relecture commitée, « relu : $(git rev-parse --short "$read_sha") », rien n'a changé depuis"
      else
        bad "changé après la relecture ($(git rev-parse --short "$read_sha")) :$after" \
          "faire relire ces changements (relecture mise à jour, nouveau « relu : »), ou les retirer. Après la relecture, seuls bougent la relecture elle-même et une fusion de l'intégration"
      fi
    fi
    # 2. The verdict, as the portal reads it (every refusal of relecture-verdict.py).
    removed="$(comm -23 <(replays "$base") <(replays "$branch"))"
    changed="$(comm -12 <(replays "$base") <(replays "$branch") | while read -r id; do
      git diff --quiet "$base" "$branch" -- "docs/audits/ultra/rejeux/$id" || echo "changé:$id"; done)"
    evidence="$(git diff --name-only "$base" "$branch" -- docs/audits/ultra/rouges-constates \
      | sed -n 's#^docs/audits/ultra/rouges-constates/\([^/]*\)/.*#\1#p' | sort -u | while read -r sha; do
        echo "constat:$sha:$(git rev-parse -q --verify "$sha^1" 2>/dev/null || true)"; done)"
    # shellcheck disable=SC2086
    if why="$(printf '%s\n' "$text" | python3 -I scripts/bench/relecture-verdict.py $removed $changed $evidence)"; then
      ok "verdict de la relecture accepté (0 bloquant, 0 grave, rejeux et rouges constatés nommés)"
    else
      while IFS= read -r line; do
        [ -n "$line" ] && bad "relecture refusée : $line" "corriger le défaut ou la ligne de la relecture ; sa DERNIÈRE ligne est « Verdict : 0 bloquant, 0 grave »"
      done <<< "$why"
    fi
  fi

  # 3. What a team branch never brings.
  touched="$(git diff --name-only "$base" "$branch" -- "$traces_root" | tr '\n' ' ')"
  [ -z "$touched" ] || bad "la branche touche aux traces du portail : $touched" "retirer ces fichiers de la branche : seul le portail les écrit"
  decisions="$(git diff --name-only "$base" "$branch" -- docs/DECISIONS.md docs/decisions | grep -vxF "docs/decisions/$tour-$zone.md" | tr '\n' ' ')"
  [ -z "$decisions" ] || rule_from 2 "décisions écrites ailleurs que dans docs/decisions/$tour-$zone.md (docs/DECISIONS.md ou le fichier d'un autre lot) : $decisions" \
    "déplacer ces lignes dans docs/decisions/$tour-$zone.md (une ligne par choix) ; le portail le liste lui-même dans docs/DECISIONS.md après la fusion"
  git diff --quiet "$base" "$branch" -- scripts/bench/portail-paquet.txt || rule_from 2 "la branche change le paquet en cours (scripts/bench/portail-paquet.txt)" \
    "retirer ce changement : seule la discussion principale change le paquet, quand elle en ouvre un"
  # PLAN-RESTE 7.1: the zone report, as the portal reads it (scripts/bench/rapport-zone.py).
  report="docs/audits/ultra/$tour-$zone-rapport.md"
  if [ -f scripts/bench/rapport-zone.py ]; then
    added="$(comm -13 <(replays "$base") <(replays "$branch") | sed 's/^/ajout:/')"
    # shellcheck disable=SC2086
    if why="$(python3 -I scripts/bench/rapport-zone.py "$branch" "$report" $added)"; then
      ok "rapport de zone accepté ($report)"
    else
      while IFS= read -r line; do
        [ -z "$line" ] || rule_from 2 "rapport de zone refusé : $line" \
          "dans $report, commité dans la branche : une ligne « tests: <filtres Gradle> » (ou « tests: aucun : <raison> ») et la liste des chemins, une ligne « chemin: <écran> | test: Classe#méthode » par endroit, chaque test un @Test de app/src/test ; pour chaque rejeu ajouté à rejeux/, sa dernière ligne « <id> : oracle tenu » (rejeu-defauts.sh, joué avec la correction)"
      done <<< "$why"
    fi
    # AUDIT-ULTRA §13.9 (from batch 4): a blocking or grave fix adds its replay (rapport-zone.py --corrections).
    # shellcheck disable=SC2086
    if why="$(python3 -I scripts/bench/rapport-zone.py --corrections "$branch" "$report" "base:$base" "tete:$head" "tour:$tour" $added)"; then
      ok "corrections : chaque bloquant ou grave dit corrigé ajoute son rejeu (ou aucun n'est dit corrigé)"
    else
      while IFS= read -r line; do
        [ -z "$line" ] || rule_from 4 "correction sans son rejeu : $line" \
          "dans $report : une ligne par défaut corrigé, « corrige: <id> | gravité: bloquant|grave|moyen|mineur | rejeu: <id du rejeu> » ; pour un bloquant ou un grave, le rejeu est un dossier que la branche AJOUTE à docs/audits/ultra/rejeux/ (avec sa ligne « <id> : oracle tenu »), ou bien « … | rejeu impossible : <raison> » ; un grave du tri seulement cité : « ne corrige pas: <id> : <raison> »"
      done <<< "$why"
    fi
  fi
  links="$(git ls-tree -r "$branch" | awk '$1 == "120000" {print $4}' | tr '\n' ' ')"
  [ -z "$links" ] || bad "lien(s) symbolique(s) commité(s) : $links" "git rm --cached ces liens (les sous-modules liés de l'atelier ne se committent jamais)"

  # 4. The merge itself, in memory, on top of the zones before it.
  merged="$(dry_merge "$head" "$branch")"
  rc=$?
  behind="$(git rev-list --count "$branch..$head")"
  if [ "$rc" -eq 2 ]; then
    warn "ce git ne sait pas fusionner à blanc (2.38 au moins) : conflits non vérifiés"
  elif [ "$rc" -eq 3 ]; then
    bad "la branche n'a aucun ancêtre commun avec l'intégration : elle n'a pas été créée par equipe-atelier.sh" "repartir d'un atelier créé par scripts/bench/equipe-atelier.sh cree"
  elif [ "$rc" -eq 1 ]; then
    files="$(printf '%s\n' "$merged" | sed 1d)"
    bad "conflit de fusion sur $(printf '%s\n' "$files" | grep -c .) fichier(s) : $(printf '%s\n' "$files" | head -6 | tr '\n' ' ')" \
      "dans l'atelier de l'équipe : fusionner l'intégration, résoudre, rejouer les tests ciblés, PUIS la relecture (un fichier résolu à la main compte comme « changé après la relecture »)"
    printf '%s\n' "$files" | head -3 | while read -r f; do
      by="$(git log -1 --format=%s "$base..$head" -- "$f" | cut -c1-90)"
      [ -z "$by" ] || printf '          %s : touché côté intégration par « %s »\n' "$f" "$by"
    done
  elif git merge-base --is-ancestor "$branch" "$head"; then
    # A branch the integration already holds (the same commit, or one of its ancestors) brings nothing: no commit in
    # memory (git said « duplicate parent » for it), no word on its base.
    warn "la branche est déjà dans l'intégration : rien à fusionner" "vérifier le nom de la branche, ou la retirer de la liste"
  else
    tree="$(printf '%s\n' "$merged" | head -1)"
    ok "se fusionne sans conflit ($behind commit(s) d'intégration que la branche n'a pas)"
    if [ -z "$(git ls-tree --name-only "$tree:docs/audits/ultra/rejeux" 2>/dev/null | grep -v '^_')" ]; then
      bad "plus aucun rejeu dans docs/audits/ultra/rejeux/ après la fusion" "garder au moins un rejeu : le portail annule une fusion qui les retire tous"
    fi
    # The team's targeted tests ran on an older base when the integration's code has moved since.
    if [ "$behind" -gt 0 ] && ! git diff --quiet "$base" "$head" -- . ':!docs'; then
      if [ "$early" -eq 1 ]; then
        bad "le code de l'intégration a bougé depuis le départ de l'équipe ($behind commit(s)) : ses tests ciblés ont tourné sur l'ancienne base" \
          "fusionner l'intégration dans la branche, rejouer les tests ciblés, puis seulement demander la relecture"
      else
        warn "le code de l'intégration a bougé depuis le départ de l'équipe ($behind commit(s)) : ses tests ciblés ont tourné sur l'ancienne base" \
          "fusionner l'intégration dans la branche et rejouer les tests ciblés AVANT la relecture ; sinon c'est le portail qui trouvera le rouge, après la fusion"
      fi
    fi
    # The next zone is tried on top of this one, as the portal does.
    head="$(GIT_AUTHOR_NAME=pre-vol GIT_AUTHOR_EMAIL=pre-vol@localhost GIT_COMMITTER_NAME=pre-vol GIT_COMMITTER_EMAIL=pre-vol@localhost \
      git commit-tree "$tree" -p "$head" -p "$branch" -m "pre-vol: $zone (in memory)")"
  fi
done
verdict
