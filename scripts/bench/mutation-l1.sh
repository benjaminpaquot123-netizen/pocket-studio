#!/usr/bin/env bash
# Audit « banc crédible » A.13 (G1): mutation L1, replayed by program.
#   scripts/bench/mutation-l1.sh [results folder]
# A @KnownDefect guard is worth something only if it goes through the code the team will fix. Mutation L1
# (docs/audits/ultra/audit-banc/mutation-l1.diff) « fixes » the app in its own wiring: « Envoyer le projet » copies the
# .zip to Musique › BenStudio at both of AppRoot's call sites (T5-ZIP), and ShareSongProgress no longer marks the Guide's
# card once the share menu opened (T5-ALERTE). With it, the two guards below must go RED « no longer fails »: they saw
# the fix. A guard that rewrites the wiring instead of calling it stays « known defect still there » for ever.
# Exit 0: both guards saw the mutation. Exit 1: at least one did not. Exit 2: refused (the files are not clean).
# The mutation is always taken away, file by file (never « git checkout -- . »). One Gradle, through gradle-file.sh.
set -euo pipefail
cd "$(dirname "$0")/../.."
diff=docs/audits/ultra/audit-banc/mutation-l1.diff
files=(app/src/main/java/com/pocketstudio/app/export/ShareSongUi.kt app/src/main/java/com/pocketstudio/app/ui/AppRoot.kt)
guards=(myExportsListsTheProjectZipJustSent noChangedWarningRightAfterSharing)
for f in "${files[@]}"; do
  git diff --quiet HEAD -- "$f" || { echo "mutation-l1 : $f est déjà modifié, refusé" >&2; exit 2; }
done
out="${1:-$(mktemp -d)}"
mkdir -p "$out"
out="$(cd "$out" && pwd)"
restore() { for f in "${files[@]}"; do git checkout -- "$f"; done; }
trap restore EXIT
git apply "$diff"
PS_GRADLE_LOG="$out/gradle.log" scripts/bench/gradle-file.sh :app:testDebugUnitTest \
  --tests 'com.pocketstudio.app.human.Tour5AppProofTest' "-Ptest.results.dir=$out/junit" --rerun --continue > "$out/gradle.txt" 2>&1 || true
python3 -I - "$out/junit" "${guards[@]}" <<'PY'
import glob, html, os, re, sys
folder, guards = sys.argv[1], sys.argv[2:]
text = "".join(open(f, encoding="utf-8").read() for f in glob.glob(os.path.join(folder, "TEST-*Tour5AppProofTest.xml")))
bad = 0
for g in guards:
    m = re.search(r'<testcase name="' + re.escape(g) + r'"[^>]*?(/>|>\s*(?:<(failure|error|skipped)[^>]*?message="([^"]*)")?)', text)
    if m is None:
        print(f"mutation L1 : {g} : pas joué (aucun résultat)")
        bad += 1
    elif m.group(2) == "failure" and "no longer fails" in html.unescape(m.group(3) or ""):
        print(f"mutation L1 : {g} : rouge « no longer fails » (la garde a vu la correction)")
    elif m.group(2) is None:
        print(f"mutation L1 : {g} : verte « known defect still there » (la garde n'a PAS vu la correction)")
        bad += 1
    else:
        print(f"mutation L1 : {g} : {m.group(2)} pour une autre raison : {html.unescape(m.group(3) or '')[:200]}")
        bad += 1
sys.exit(1 if bad else 0)
PY
