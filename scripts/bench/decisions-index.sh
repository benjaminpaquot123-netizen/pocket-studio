#!/usr/bin/env bash
# Socle du banc, point 2: each lot writes its decisions in docs/decisions/<tour-N>-<zone>.md (one line per choice), never
# in docs/DECISIONS.md, so two lots never collide there. After the merge, the portal (fusion-equipes.sh) lists the lot's
# file at the end of docs/DECISIONS.md, once:
#   scripts/bench/decisions-index.sh <tour-N> <zone>      (from the repository's root; nothing to do without the file)
set -euo pipefail
tour="${1:?tour-N}"; zone="${2:?zone}"
file="docs/decisions/$tour-$zone.md"
[ -f "$file" ] || exit 0
grep -qF "\`$file\`" docs/DECISIONS.md && exit 0
count="$(grep -c '^- ' "$file" || true)"
echo "- $(date -u +%F) · lot « $zone » ($tour) : ${count:-0} décision(s) dans \`$file\`" >> docs/DECISIONS.md
