#!/usr/bin/env bash
# The pre-flight's own test (scripts/bench/pre-vol.sh): a small repository made here, an integration branch, one team
# branch per refusal, and what the pre-flight must say of each. Where the portal is there, the same branch is given to
# the real scripts/bench/fusion-equipes.sh too: both must refuse it. Seconds, no Gradle.
#   scripts/bench/test_pre_vol.sh
set -uo pipefail
src="$(cd "$(dirname "$0")/../.." && pwd)"
for f in fusion-equipes.sh relecture-verdict.py; do
  [ -f "$src/scripts/bench/$f" ] || { echo "SKIP: scripts/bench/$f is not in this repository (the portal lives on the audit branch)"; exit 0; }
done
t="$(mktemp -d)"
trap 'rm -rf "$t"' EXIT
cd "$t" || exit 2
git init -q -b fx-int .
git config user.name essai
git config user.email essai@localhost
mkdir -p scripts/bench docs/audits/ultra/rejeux/r1
cp "$src"/scripts/bench/pre-vol.sh "$src"/scripts/bench/relecture-verdict.py "$src"/scripts/bench/fusion-equipes.sh scripts/bench/
for f in portail-paquet.txt decisions-index.sh rapport-zone.py verifie-chantier.py; do [ ! -f "$src/scripts/bench/$f" ] || cp "$src/scripts/bench/$f" scripts/bench/; done
# The batch of merges in this little repository: 1 (a rule « from_paquet 2 » waits).
printf '# Paquet de fusions en cours\npaquet: 1\n' > scripts/bench/portail-paquet.txt
echo oracle > docs/audits/ultra/rejeux/r1/oracle.txt
printf '# Décisions\n- 2026-11-11 · une décision\n' > docs/DECISIONS.md
echo "# titre" > README.md
git add -A
git commit -q -m base

reds=0
# A team branch with one commit of work and a clean review of it.
review() { # <zone> [<verdict line>]
  mkdir -p docs/audits/ultra
  printf '# Relecture\nRelu : %s\n\n%s\n' "$(git rev-parse HEAD)" "${2:-Verdict : 0 bloquant, 0 grave}" > "docs/audits/ultra/tour-9-$1-relecture.md"
  git add "docs/audits/ultra/tour-9-$1-relecture.md"
  git commit -q -m "review of $1"
}
team() { # <zone>
  git checkout -q -b "eq/tour-9-$1" fx-int
  echo "$1" > "travail-$1.txt"
  git add "travail-$1.txt"
  git commit -q -m "work of $1"
}
# The pre-flight's exit code and one line it must print; then the real portal must refuse too (when asked).
expect() { # <label> <exit> <pattern> <portal: oui|non> <args…>
  local label="$1" want="$2" pattern="$3" portal="$4" out rc=0
  shift 4
  git checkout -q fx-int
  out="$(scripts/bench/pre-vol.sh "$@" 2>&1)" || rc=$?
  if [ "$rc" -ne "$want" ] || ! printf '%s\n' "$out" | grep -q -- "$pattern"; then
    reds=$((reds + 1))
    echo "ROUGE $label: exit $rc (wanted $want), « $pattern » $(printf '%s\n' "$out" | grep -q -- "$pattern" && echo found || echo missing)"
    printf '%s\n' "$out" | sed 's/^/      /'
    return
  fi
  if [ "$portal" = oui ]; then
    rc=0
    scripts/bench/fusion-equipes.sh "$@" > /dev/null 2>&1 || rc=$?
    if [ "$rc" -eq 0 ]; then reds=$((reds + 1)); echo "ROUGE $label: the pre-flight refuses what the portal accepts"; return; fi
    git merge --abort 2> /dev/null || true
  fi
  echo "vert  $label"
}

team ok && review ok
expect "a clean lot is ready" 0 "PRÉ-VOL : PRÊT" non tour-9 ok

team norev
expect "no review" 1 "pas de relecture neuve" oui tour-9 norev
expect "before the review, no review is no refusal" 0 "PRÉ-VOL : PRÊT" non --avant-relecture tour-9 norev

team after && review after && echo more >> travail-after.txt && git commit -q -am "after the review"
expect "code changed after the review" 1 "changé après la relecture" oui tour-9 after

team unrelu && review unrelu && sed -i 's/^Relu : .*/Lu hier/' docs/audits/ultra/tour-9-unrelu-relecture.md && git commit -q -am "no relu line"
expect "the review names no commit" 1 "ne nomme pas le commit" oui tour-9 unrelu

team verdict && review verdict "Verdict : 0 bloquant, 1 grave"
expect "a grave left" 1 "relecture refusée" oui tour-9 verdict

team last && review last && echo "Une note après le verdict." >> docs/audits/ultra/tour-9-last-relecture.md && git commit -q -am "a line after the verdict"
expect "the verdict is not the last line" 1 "relecture refusée" oui tour-9 last

team open && git commit -q --allow-empty -m "nothing" && printf '# Relecture\nRelu : %s\n\n| 1 | grave | un défaut | à faire |\n\nVerdict : 0 bloquant, 0 grave\n' "$(git rev-parse HEAD)" > docs/audits/ultra/tour-9-open-relecture.md \
  && git add docs/audits/ultra/tour-9-open-relecture.md && git commit -q -m "review with an open grave row"
expect "a grave row not « corrigé »" 1 "relecture refusée" oui tour-9 open

team link && ln -s /etc/hostname lien && git add lien && git commit -q -m "a symlink" && review link
expect "a symbolic link" 1 "lien(s) symbolique(s)" oui tour-9 link

team trace && mkdir -p docs/audits/ultra/portail/faux && echo x > docs/audits/ultra/portail/faux/fusion.txt && git add docs/audits/ultra/portail && git commit -q -m "a forged trace" && review trace
expect "the portal's traces touched" 1 "traces du portail" oui tour-9 trace

team rejeu && git rm -q -r docs/audits/ultra/rejeux/r1 && git commit -q -m "the replay taken out" && review rejeu
expect "a replay taken out, unnamed" 1 "relecture refusée" oui tour-9 rejeu
expect "no replay left after the merge" 1 "plus aucun rejeu" non tour-9 rejeu

team z1 && sed -i '1s/.*/# titre de z1/' README.md && git commit -q -am "z1's title" && review z1
team z2 && sed -i '1s/.*/# titre de z2/' README.md && git commit -q -am "z2's title" && review z2
expect "each of two lots alone is ready" 0 "PRÉ-VOL : PRÊT" non tour-9 z2
expect "two lots that collide, given together" 1 "conflit de fusion" non tour-9 z1 z2

expect "a branch that is not there" 1 "n'existe pas" non tour-9 fantome

# Uncommitted work in the integration workshop: the portal refuses to start.
git checkout -q fx-int && echo sale >> README.md
expect "the integration workshop is not clean" 1 "changements non commités" oui tour-9 ok
git checkout -q -- README.md

# The integration's code moves after the teams left: said after the review, refused before it.
echo "du code" > code.txt && git add code.txt && git commit -q -m "the integration's code moves"
expect "an older base is said after the review" 0 "ancienne base" non tour-9 ok
expect "an older base is refused before the review" 1 "ancienne base" non --avant-relecture tour-9 norev

# The team's workshop: a tracked file that is a symbolic link there (a linked submodule, « T » in git status) is no
# uncommitted work; a file really changed there is.
shop="$(mktemp -d)"
trap 'rm -rf "$t" "$shop"' EXIT
team shop && echo lien-cible > lie.txt && git add lie.txt && git commit -q -m "a file the workshop links" && review shop
git checkout -q fx-int
git worktree add -q "$shop/eq" eq/tour-9-shop
rm "$shop/eq/lie.txt" && ln -s /etc/hostname "$shop/eq/lie.txt"
expect "a linked file in the team's workshop is no uncommitted work" 0 "PRÉ-VOL : PRÊT" non tour-9 shop
echo more >> "$shop/eq/travail-shop.txt"
expect "a file changed in the team's workshop is refused" 1 "l'atelier de l'équipe" non tour-9 shop
git -C "$shop/eq" checkout -q -- travail-shop.txt

# A branch that is the integration itself (nothing to merge): said, never « duplicate parent » from git.
git branch -q -f eq/tour-9-meme fx-int
out="$(scripts/bench/pre-vol.sh --avant-relecture tour-9 meme 2>&1)"
if printf '%s\n' "$out" | grep -q "déjà dans l'intégration" && ! printf '%s\n' "$out" | grep -qi "duplicate parent\|^error"; then
  echo "vert  a branch already in the integration is said, git stays quiet"
else
  reds=$((reds + 1)); echo "ROUGE a branch already in the integration"; printf '%s\n' "$out" | sed 's/^/      /'
fi

# Review of banc7 (moyen 2): a branch the integration already holds, far behind while the integration's code moved, is
# said « déjà dans l'intégration », never refused for its old base.
git branch -q -f eq/tour-9-ancien "$(git rev-list --max-parents=0 fx-int)"
expect "a branch already in the integration is not refused for its base" 0 "déjà dans l'intégration" non --avant-relecture tour-9 ancien

# --main: main is a squash of an older state of this branch (same tree as one of its commits), then a real conflict.
git checkout -q fx-int
echo "# titre du lot fusionné" > README.md && git commit -q -am "work that main got as a squash"
old="$(git rev-parse HEAD)"
git branch -q -f main "$(git commit-tree "$old^{tree}" -p "$(git rev-list --max-parents=0 HEAD)" -m "squash of the branch so far")"
echo "# titre d'après" > README.md && git commit -q -am "the branch goes on"
out="$(scripts/bench/pre-vol.sh --main main 2>&1)"; rc=$?
if [ "$rc" -eq 1 ] && printf '%s\n' "$out" | grep -q "git merge -s ours main"; then echo "vert  --main: a squashed main brings nothing new"; else reds=$((reds + 1)); echo "ROUGE --main squash: exit $rc"; printf '%s\n' "$out" | sed 's/^/      /'; fi
before="$(git rev-parse 'HEAD^{tree}')"
git merge -q -s ours main -m "Record main as merged"
out="$(scripts/bench/pre-vol.sh --main main 2>&1)"; rc=$?
if [ "$rc" -eq 0 ] && [ "$before" = "$(git rev-parse 'HEAD^{tree}')" ]; then echo "vert  --main: ready after « merge -s ours », no file changed"; else reds=$((reds + 1)); echo "ROUGE --main after ours: exit $rc"; fi
git checkout -q main && echo "# titre de main" > README.md && git commit -q -am "main moves on its own" && git checkout -q fx-int
echo "# encore un titre" > README.md && git commit -q -am "and the branch too"
out="$(scripts/bench/pre-vol.sh --main main 2>&1)"; rc=$?
if [ "$rc" -eq 1 ] && printf '%s\n' "$out" | grep -q "fusionner main ici" && ! printf '%s\n' "$out" | grep -q "merge -s ours"; then echo "vert  --main: a real conflict is not papered over"; else reds=$((reds + 1)); echo "ROUGE --main real conflict: exit $rc"; printf '%s\n' "$out" | sed 's/^/      /'; fi

git branch -q -f ailleurs "$(git commit-tree "$(git rev-parse 'HEAD^{tree}')" -m "a history of its own")"
out="$(scripts/bench/pre-vol.sh --main ailleurs 2>&1)"; rc=$?
if [ "$rc" -eq 1 ] && printf '%s\n' "$out" | grep -q "aucun ancêtre commun"; then echo "vert  --main: no common ancestor is refused"; else reds=$((reds + 1)); echo "ROUGE --main unrelated: exit $rc"; printf '%s\n' "$out" | sed 's/^/      /'; fi

# --ci: a bench test that needs a git identity is red here as on a new runner, in seconds.
cat > scripts/bench/test_report.py <<'EOF'
import os, subprocess, sys, tempfile
d = tempfile.mkdtemp()
subprocess.run(["git", "init", "-q", d], check=True)
r = subprocess.run(["git", "-C", d, "commit", "-q", "--allow-empty", "-m", "x"], capture_output=True, text=True)
if r.returncode != 0:
    print("FAIL: a commit made by the test:", r.stderr.strip().splitlines()[-1])
    sys.exit(1)
EOF
git add scripts/bench/test_report.py && git commit -q -m "a bench test that commits"
out="$(scripts/bench/pre-vol.sh --ci 2>&1)"; rc=$?
if [ "$rc" -eq 1 ] && printf '%s\n' "$out" | grep -q "runner neuf"; then echo "vert  --ci: a test that needs a git identity is caught"; else reds=$((reds + 1)); echo "ROUGE --ci identity: exit $rc"; printf '%s\n' "$out" | sed 's/^/      /'; fi
sed -i 's/\["git", "-C", d, "commit"/["git", "-C", d, "-c", "user.name=t", "-c", "user.email=t@localhost", "commit"/' scripts/bench/test_report.py
git commit -q -am "the test brings its own identity"
out="$(scripts/bench/pre-vol.sh --ci 2>&1)"; rc=$?
if [ "$rc" -eq 0 ]; then echo "vert  --ci: ready once the test brings its identity"; else reds=$((reds + 1)); echo "ROUGE --ci fixed: exit $rc"; printf '%s\n' "$out" | sed 's/^/      /'; fi
echo more >> README.md
out="$(scripts/bench/pre-vol.sh --ci 2>&1)"; rc=$?
if [ "$rc" -eq 1 ] && printf '%s\n' "$out" | grep -q "la CI ne les verra pas"; then echo "vert  --ci: uncommitted changes are said"; else reds=$((reds + 1)); echo "ROUGE --ci dirty: exit $rc"; fi
git checkout -q -- README.md
# Banc7b (signalé par la discussion principale): a linked third-party folder (« T » in git status) and the portal's kept
# failures (docs/audits/ultra/rejeux/_echecs/) are no uncommitted work for --ci either, as for « tour-N <zone> ».
mkdir -p third/lib docs/audits/ultra/rejeux/_echecs/x && echo code > third/lib/a.c && echo vu > docs/audits/ultra/rejeux/_echecs/x/rejeu.txt
git add third docs/audits/ultra/rejeux/_echecs && git commit -q -m "a vendored file and a kept failure"
rm third/lib/a.c && ln -s /etc/hostname third/lib/a.c && echo encore >> docs/audits/ultra/rejeux/_echecs/x/rejeu.txt
out="$(scripts/bench/pre-vol.sh --ci 2>&1)"; rc=$?
if ! printf '%s\n' "$out" | grep -q "la CI ne les verra pas"; then echo "vert  --ci: a linked folder and the kept failures are no uncommitted work"; else reds=$((reds + 1)); echo "ROUGE --ci links and _echecs: exit $rc"; printf '%s\n' "$out" | sed 's/^/      /'; fi
rm third/lib/a.c && git checkout -q -- third docs/audits/ultra/rejeux/_echecs

# Point 2 and 5 of the bench's base: a lot writes its decisions in docs/decisions/<tour>-<zone>.md, never in
# docs/DECISIONS.md; a rule added during batch 1 (« from_paquet 2 ») is only said, then refused from batch 2.
git checkout -q fx-int
git checkout -q -b eq/tour-9-decold fx-int && echo "- 2026-11-11 · du lot decold" >> docs/DECISIONS.md && git commit -q -am "decold writes DECISIONS.md" && review decold
expect "DECISIONS.md written during batch 1: only said" 0 "paquet 2" non tour-9 decold
git checkout -q fx-int && sed -i 's/^paquet: .*/paquet: 2/' scripts/bench/portail-paquet.txt && git commit -q -am "batch 2 opens"
# PLAN-RESTE 7.1 (from batch 2): the zone report of each lot, in its branch, with its « tests: » line and its list of
# paths (« chemin: <écran> | test: Classe#méthode », a @Test of app/src/test in the branch).
mkdir -p app/src/test/java/t && printf 'package t\n\nimport org.junit.Test\n\nclass FooTest {\n    @Test\n    fun works() {}\n}\n' > app/src/test/java/t/FooTest.kt
git add app && git commit -q -m "a test of the app"
rapport() { # <zone> [<text>]: the zone report, committed in the current branch
  mkdir -p docs/audits/ultra
  printf '%s\n' "${2:-$(printf '# Rapport\n\ntests: FooTest\n\n- chemin: Accueil | test: FooTest#works')}" > "docs/audits/ultra/tour-9-$1-rapport.md"
  git add "docs/audits/ultra/tour-9-$1-rapport.md" && git commit -q -m "report of $1"
}
team rapnone && review rapnone
expect "no zone report: refused" 1 "no zone report" oui tour-9 rapnone
team rapnotests && rapport rapnotests "$(printf '# Rapport\n\n- chemin: Accueil | test: FooTest#works')" && review rapnotests
expect "a zone report without « tests: »: refused" 1 "no line « tests: »" oui tour-9 rapnotests
team rapnopath && rapport rapnopath "$(printf '# Rapport\n\ntests: FooTest')" && review rapnopath
expect "a zone report without its list of paths: refused" 1 "no list of paths" oui tour-9 rapnopath
team rapbadtest && rapport rapbadtest "$(printf '# Rapport\n\ntests: FooTest\n- chemin: Accueil | test: BarTest#works')" && review rapbadtest
expect "a path whose test does not exist: refused" 1 "BarTest#works is not a @Test of app/src/test" oui tour-9 rapbadtest
team rapnontenu && mkdir -p docs/audits/ultra/rejeux/r9 && echo oracle > docs/audits/ultra/rejeux/r9/oracle.txt && git add docs/audits/ultra/rejeux \
  && rapport rapnontenu "$(printf '# Rapport\n\ntests: FooTest\n- chemin: Accueil | test: FooTest#works\n\nr9 : oracle non tenu (écran 2)')" && review rapnontenu
expect "a replay added « non tenu »: refused" 1 "r9 added to rejeux/: its last verdict" oui tour-9 rapnontenu
git checkout -q -b eq/tour-9-decnew fx-int && echo "- 2026-11-11 · du lot decnew" >> docs/DECISIONS.md && git commit -q -am "decnew writes DECISIONS.md" && review decnew
expect "DECISIONS.md written from batch 2: refused" 1 "docs/decisions/tour-9-decnew.md" oui tour-9 decnew
git checkout -q -b eq/tour-9-decown fx-int && mkdir -p docs/decisions && echo "- 2026-11-11 · un choix" > docs/decisions/tour-9-decown.md && git add docs/decisions && git commit -q -m "decown's own file" && rapport decown && review decown
expect "a lot's own decisions file is ready" 0 "PRÉ-VOL : PRÊT" non tour-9 decown
git checkout -q -b eq/tour-9-decother fx-int && mkdir -p docs/decisions && echo "- 2026-11-11 · un choix" > docs/decisions/tour-9-autre.md && git add docs/decisions && git commit -q -m "decother writes another lot's file" && review decother
expect "another lot's decisions file: refused" 1 "autre lot" oui tour-9 decother
# Review of the lot (grave 1): the portal refuses for THIS reason from batch 2, and not at batch 1 (the toy has no
# self-test, so any branch is refused later on: the reason is what is checked).
portal_says() { # <label> <zone> <yes|no>
  local out
  git checkout -q fx-int
  out="$(scripts/bench/fusion-equipes.sh tour-9 "$2" 2>&1)"
  git merge --abort 2> /dev/null || true
  git reset -q --hard fx-int
  if { [ "$3" = yes ] && printf '%s\n' "$out" | grep -q "decisions written outside"; } || { [ "$3" = no ] && ! printf '%s\n' "$out" | grep -q "decisions written outside"; }; then
    echo "vert  $1"
  else
    reds=$((reds + 1)); echo "ROUGE $1"; printf '%s\n' "$out" | sed 's/^/      /'
  fi
}
portal_says "the portal names the decisions rule from batch 2" decnew yes
portal_says "the portal names the decisions rule for another lot's file" decother yes
portal_says "the portal leaves a lot's own decisions file alone" decown no
git checkout -q fx-int && sed -i 's/^paquet: .*/paquet: 1/' scripts/bench/portail-paquet.txt && git commit -q -am "back to batch 1 (test)"
portal_says "the decisions rule waits at batch 1" decold no
sed -i 's/^paquet: .*/paquet: 2/' scripts/bench/portail-paquet.txt && git commit -q -am "batch 2 again (test)"
# A lot never moves the batch itself (review, moyen 3): refused from batch 2.
git checkout -q -b eq/tour-9-paquet fx-int && sed -i 's/^paquet: .*/paquet: 9/' scripts/bench/portail-paquet.txt && git commit -q -am "a lot moves the batch" && review paquet
expect "a lot that moves the batch: refused" 1 "portail-paquet.txt" oui tour-9 paquet
git checkout -q fx-int
out="$(scripts/bench/fusion-equipes.sh tour-9 paquet 2>&1)"; git merge --abort 2> /dev/null || true; git reset -q --hard fx-int
if printf '%s\n' "$out" | grep -q "moves the batch"; then echo "vert  the portal names the batch rule"; else reds=$((reds + 1)); echo "ROUGE the portal names the batch rule"; printf '%s\n' "$out" | sed 's/^/      /'; fi

# --- §13.9, first point (lot « portail3 », rules « from_paquet 4 ») --------------------------------------------------
# From here the toy's portal can go to the end of a merge: stand-ins for the replays with their self-test, for Gradle
# and for the judge (the real judge's functions, which rapport-zone.py reads; it prints the lines of juge.txt, then
# « chantier : N écarts », or no such line when the file juge-muet is there). A branch moves the judge by changing
# juge.txt.
git checkout -q fx-int
cat > scripts/bench/rejeu-defauts.sh <<'STUB'
#!/usr/bin/env bash
echo "1 verts, 0 rouges, 0 en quarantaine, 0 sautés" > "$PS_REJEUX_SELFTEST/selftest-resume.txt"
echo "a stamp" > "$PS_REJEUX_SELFTEST/selftest.ok"
echo "chantier : 0 écarts" > "$PS_REJEUX_SELFTEST/selftest-chantier.txt"
echo "r1 : oracle tenu"
STUB
printf '#!/usr/bin/env bash\nexit 0\n' > scripts/bench/gradle-file.sh
sed '/^if __name__ == "__main__":/,$d' "$src/scripts/bench/verifie-chantier.py" > scripts/bench/verifie-chantier.py
cat >> scripts/bench/verifie-chantier.py <<'STUB'
if __name__ == "__main__":
    gaps = [l for l in open("juge.txt", encoding="utf-8").read().splitlines() if l] if os.path.exists("juge.txt") else []
    for gap in gaps:
        print(gap)
    if not os.path.exists("juge-muet"):
        print(f"chantier : {len(gaps)} écarts")
STUB
chmod +x scripts/bench/rejeu-defauts.sh scripts/bench/gradle-file.sh scripts/bench/decisions-index.sh
old_gap="abcdef123 « vert: un ancien » : vert sans rouge du même test avant lui"
printf '%s\n%s\n' "$old_gap" "résumé refusé : un écart qui s'en ira" > juge.txt
git add scripts/bench juge.txt && git commit -q -m "the toy's portal goes to the end of a merge"
batch() { git checkout -q fx-int && sed -i "s/^paquet: .*/paquet: $1/" scripts/bench/portail-paquet.txt && git commit -q -am "batch $1 (test)"; }
# The real portal on one zone: its exit code in $prc, all it said in $pout, the integration's head before in $pbefore.
portal() { # <zone>
  git checkout -q fx-int
  pbefore="$(git rev-parse HEAD)"
  prc=0
  pout="$(scripts/bench/fusion-equipes.sh tour-9 "$1" 2>&1)" || prc=$?
}
said() { # <label> <0 when what was wanted was seen>
  if [ "$2" -eq 0 ]; then echo "vert  $1"; else reds=$((reds + 1)); echo "ROUGE $1 (portal: exit $prc)"; printf '%s\n' "$pout" | sed 's/^/      /'; fi
}
has() { printf '%s\n' "$pout" | grep -qF -- "$1"; }
# Nothing left of an undone merge: the head back where it was, no file changed or left behind, no trace folder.
untouched() { [ "$(git rev-parse HEAD)" = "$pbefore" ] && [ -z "$(git status --porcelain)" ] && [ -z "$(ls docs/audits/ultra/portail 2>/dev/null | grep -- "-$1\$")" ]; }

# Rule 1: the portal undoes a merge that makes the judge go up, counted against the previous portal trace.
batch 3
# (The zones' names: the first trace's folder « …-jzz » sorts AFTER the second's « …-jaa »; the previous trace is the
# last one committed, by the history, never the last by name.)
team jzz && rapport jzz && review jzz
portal jzz
said "the judge is not compared without a previous portal trace" "$( [ "$prc" -eq 0 ] && has "no previous portal trace" && echo 0 || echo 1)"
team jaa && printf '%s\n' "$old_gap" > juge.txt && git commit -q -am "one gap closed" && rapport jaa && review jaa
portal jaa
said "a lot that makes the judge go down is merged" "$( [ "$prc" -eq 0 ] && has "judge: 1 écarts (previous portal trace: 2" && echo 0 || echo 1)"
# The lot that goes up: the old gap is still there, printed with a shorter sha (8 letters where the trace has 9), and
# one gap is new.
new_gap="1234abcd « vert: un neuf » : vert sans rouge du même test avant lui"
team jup && printf '%s\n%s\n' "${old_gap/abcdef123/abcdef12}" "$new_gap" > juge.txt && git commit -q -am "one gap more" && rapport jup && review jup
portal jup
said "before batch 4 a judge that goes up is only said" "$( [ "$prc" -eq 0 ] && has "the judge goes up (1 → 2 écarts" && has "refused from batch 4" && echo 0 || echo 1)"
git reset -q --hard "$pbefore"
batch 4
portal jup
said "from batch 4 a lot that makes the judge go up is undone" "$( [ "$prc" -ne 0 ] && has "jup: the judge goes up (1 → 2 écarts" && has "undone" && untouched jup && echo 0 || echo 1)"
said "the refusal names the new gap, not the old one printed with a shorter sha" "$( has "$new_gap" && ! has "un ancien" && echo 0 || echo 1)"
team jmuet && echo x > juge-muet && git add juge-muet && git commit -q -m "the judge says no count" && rapport jmuet && review jmuet
portal jmuet
said "from batch 4 a judge without its « chantier : » line is undone" "$( [ "$prc" -ne 0 ] && has "jmuet: the judge printed no « chantier : N écarts » line" && untouched jmuet && echo 0 || echo 1)"
# A previous trace without the judge's count (an older portal's): nothing to compare with, the lot is merged.
git checkout -q fx-int && mkdir -p docs/audits/ultra/portail/2026-01-01-tour-9-faux \
  && echo "résumé absent" > docs/audits/ultra/portail/2026-01-01-tour-9-faux/selftest-chantier.txt \
  && git add docs/audits/ultra/portail && git commit -q -m "Portal traces of the merge of faux (tour-9)"
portal jup
said "a previous trace without the judge's count refuses nothing" "$( [ "$prc" -eq 0 ] && has "has no « chantier : » line" && echo 0 || echo 1)"

echo
if [ "$reds" -eq 0 ]; then echo "test_pre_vol : tout est vert"; else echo "test_pre_vol : $reds ROUGE(S)"; exit 1; fi
