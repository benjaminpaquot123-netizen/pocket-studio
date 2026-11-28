#!/usr/bin/env bash
# Tour 9, « balayage » of the 37 courses: runs the three sweep tests (validation, words, screen) and gathers
# their rows. A measuring tool, never a gate: the tests only run with -Pbench.out and never fail.
# Usage: scripts/bench/tuto-sweep.sh [out_dir]   (default: build/tuto-sweep)
set -euo pipefail
cd "$(dirname "$0")/../.."
out="${1:-build/tuto-sweep}"
mkdir -p "$out"
out="$(cd "$out" && pwd)"
rm -rf "$out/screen"
bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*Tour9TutoSweep*' -Pbench.out="$out" --offline 2>&1 | grep -E "^e:|BUILD|FAILED" || true
{
  printf 'sev\tfamille\ttuto\tniveau\tchapitre\tlangue\tappareil\tpolice\tquoi\n'
  cat "$out"/screen/*.tsv 2>/dev/null || true
} > "$out/screen.tsv"
python3 - "$out" <<'PY'
import csv, collections, sys, os
out = sys.argv[1]
def rows(name):
    p = os.path.join(out, name)
    if not os.path.exists(p): return []
    with open(p, newline='') as f: return list(csv.DictReader(f, delimiter='\t'))
lines = []
for name, lang_col in (("validation.tsv", None), ("text.tsv", "langue"), ("screen.tsv", "langue")):
    r = rows(name)
    lines.append(f"== {name}: {len(r)} lignes")
    for (sev, fam), n in sorted(collections.Counter((x['sev'], x['famille']) for x in r).items()):
        lines.append(f"  {sev:9} {fam:32} {n}")
    for col in ("tuto", lang_col):
        if not col: continue
        c = collections.Counter(x[col] for x in r if x['sev'] == 'grave')
        if c: lines.append(f"  graves par {col}: " + ", ".join(f"{k} {v}" for k, v in c.most_common()))
open(os.path.join(out, "comptes.txt"), "w").write("\n".join(lines) + "\n")
print("\n".join(lines))
PY
