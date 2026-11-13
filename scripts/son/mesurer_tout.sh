#!/usr/bin/env bash
# Tour 6, siège son: son6_kick.py and son6.py on every style of OUT_DIR, one style per process, JOBS at a time, each
# style's rows in OUT_DIR/par-style/<tool>-<style>.tsv (resumable: a style already measured is skipped), then merged
# into OUT_DIR/kick.tsv and OUT_DIR/son6.tsv.
#   scripts/son/mesurer_tout.sh OUT_DIR [JOBS] [kick|son6|tout] [HITS_FROM_BENCH_DIR]
# HITS_FROM_BENCH_DIR: son6_kick.py's --hits-from (the kick's hits read without the speaker's processing).
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$(cd "$1" && pwd)"
JOBS="${2:-4}"
WHAT="${3:-tout}"
HITS="${4:-}"
mkdir -p "$OUT/par-style"
export HERE OUT HITS
for tool in kick son6; do
    [ "$WHAT" = tout ] || [ "$WHAT" = "$tool" ] || continue
    export tool
    ls "$OUT/json" | sed 's/\.json$//' | xargs -P "$JOBS" -I{} sh -c '
        f="$OUT/par-style/$tool-{}.tsv"
        [ -s "$f" ] && exit 0
        if [ "$tool" = kick ]; then py=son6_kick.py; else py=son6.py; fi
        extra=""
        [ "$tool" = kick ] && [ -n "$HITS" ] && extra="--hits-from $HITS"
        python3 -I "$HERE/$py" "$OUT" "$f.part" --styles {} $extra > /dev/null && mv "$f.part" "$f"
    '
    # Columns differ between styles (a style without a lead has no lead columns): merged by name.
    python3 -I - "$OUT" "$tool" <<'PY'
import csv, glob, os, sys
out, tool = sys.argv[1], sys.argv[2]
rows = []
for f in sorted(glob.glob(os.path.join(out, "par-style", tool + "-*.tsv"))):
    rows += list(csv.DictReader(open(f, encoding="utf-8"), delimiter="\t"))
keys = []
for r in rows:
    keys += [k for k in r if k not in keys]
with open(os.path.join(out, tool + ".tsv"), "w", encoding="utf-8") as o:
    o.write("\t".join(keys) + "\n")
    for r in rows:
        o.write("\t".join(r.get(k) or "" for k in keys) + "\n")
PY
done
echo "Measured: $OUT"
