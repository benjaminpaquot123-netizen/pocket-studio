#!/usr/bin/env bash
# PLAN-RESTE 4.0: after a team's fix is merged, mark one verdict « corrigé » everywhere at once:
#   scripts/bench/marque-corrige.sh <item> <commit> <tour-N> [<lot>/selftest-resume.txt]
# verdicts.tsv: the item's etat → corrigé and its commit column; base.tsv: its key → corrigé, corrige_au = tour.
# Then verifie-chantier.py --resume says what is left (audit « banc crédible » A.10: the summary of the latest
# self-test is the 4th argument or $PS_SELFTEST_RESUME; without it the judge prints « résumé absent »). Never commits.
set -euo pipefail
item="${1:?item (ex. T5-Cd-68)}"; commit="${2:?commit}"; tour="${3:?tour-N}"
resume="${4:-${PS_SELFTEST_RESUME:-}}"
root="$(cd "$(dirname "$0")/../.." && pwd)"
git -C "$root" cat-file -e "$commit^{commit}" 2>/dev/null || { echo "unknown commit: $commit" >&2; exit 2; }
python3 -I - "$root/docs/audits/ultra" "$item" "$commit" "$tour" <<'PY'
import sys
ultra, item, commit, tour = sys.argv[1:]
def edit(path, key_col, key, change):
    lines = open(path, encoding="utf-8").read().split("\n")
    header, hit = None, None
    for i, l in enumerate(lines):
        if not l or l.startswith("#"):
            continue
        cells = l.split("\t")
        if header is None:
            header = cells
            continue
        row = dict(zip(header, cells + [""] * (len(header) - len(cells))))
        if row.get(key_col) == key:
            change(row)
            lines[i] = "\t".join(row.get(h, "") for h in header)
            hit = row
    open(path, "w", encoding="utf-8").write("\n".join(lines))
    return hit
def fix_verdict(r):
    r["etat"] = "corrigé"; r["commit"] = commit
v = edit(f"{ultra}/verdicts.tsv", "item", item, fix_verdict)
if v is None:
    sys.exit(f"{item}: not in verdicts.tsv")
def fix_base(r):
    r["etat"] = "corrigé"; r["corrige_au"] = tour
if edit(f"{ultra}/base.tsv", "cle", v["cle"], fix_base) is None:
    with open(f"{ultra}/base.tsv", "a", encoding="utf-8") as f:
        f.write(f"{v['cle']}\tcorrigé\t{v['tour']}\t{item} ({commit})\t{tour}\n")
print(f"{item}: corrigé ({commit}, {tour}) ; clé {v['cle']}")
PY
python3 -I "$root/scripts/bench/verifie-chantier.py" --root "$root" --resume "$resume" || true
