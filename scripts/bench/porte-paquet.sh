#!/usr/bin/env bash
# Batch gate (lot « porte-paquet », tour 9): up to three zones through the portal with ONE self-test.
#   scripts/bench/porte-paquet.sh [--limite <min>] <tour-N> <zone> [<zone> <zone>] [-- <gradle test filters…>]
# The portal (scripts/bench/fusion-equipes.sh) took 46 to 75 min per lot, almost all of it in the bench's self-test
# and the replays, run again for each zone. Here, in this order:
#   0. a pre-check of a few seconds, which refuses (« REFUSÉ : … ») before anything long: free disk under
#      PS_PAQUET_DISQUE_MIN_GO (8) GB; a known time limit under 90 min (--limite, PS_PAQUET_LIMITE_MIN, or the calling
#      tool's BASH_DEFAULT_TIMEOUT_MS / BASH_MAX_TIMEOUT_MS); a team branch eq/<tour-N>-<zone> missing here and on
#      origin (when only on origin, it is created here); scripts/bench/pre-vol.sh not « PRÊT »; a zone's checks
#      refused; a test filter (given here or in a zone's report) matching no test class of app/src/test*;
#   1. every zone's merge, each after its checks (the portal's « fusion » step);
#   2. ONE self-test with the replays (rejeu-defauts.sh), then the union of the zones' filters through the Gradle queue;
#   3. each zone's portal traces (the portal's « traces » step), each naming the common self-test.
# Red after step 0: everything back to the starting commit, then the portal zone by zone as before, which names the
# zone at fault (its exit code is this script's). Never pushes; never moves scripts/bench/portail-paquet.txt.
set -euo pipefail
int="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$int"
limit_arg=""
if [ "${1:-}" = "--limite" ]; then limit_arg="${2:?--limite <minutes>}"; shift 2; fi
tour="${1:?tour-N}"; shift
zones=(); filters=()
while [ $# -gt 0 ]; do
  if [ "$1" = "--" ]; then shift; filters=("$@"); break; fi
  zones+=("$1"); shift
done
refuse() { echo "REFUSÉ : $*" >&2; exit 2; }
[ ${#zones[@]} -ge 1 ] || refuse "aucune zone donnée."
[ ${#zones[@]} -le 3 ] || refuse "3 zones au plus par paquet (${#zones[@]} données : ${zones[*]})."
portal=(bash scripts/bench/fusion-equipes.sh "$tour" "${zones[@]}" -- "${filters[@]}")

# --- 0. Pre-check ------------------------------------------------------------------------------------------------
[ -z "$(git status --porcelain --untracked-files=no | grep -v '^ T')" ] || refuse "l'atelier d'intégration a des changements non commités."
min_go="${PS_PAQUET_DISQUE_MIN_GO:-8}"
free_kb="$(df -Pk . | awk 'NR == 2 { print $4 }')"
[ "$free_kb" -ge $((min_go * 1024 * 1024)) ] || refuse "disque libre $((free_kb / 1024 / 1024)) Go, il en faut au moins $min_go."
for spec in "--limite:$limit_arg:1" "PS_PAQUET_LIMITE_MIN:${PS_PAQUET_LIMITE_MIN:-}:1" \
            "BASH_DEFAULT_TIMEOUT_MS:${BASH_DEFAULT_TIMEOUT_MS:-}:60000" "BASH_MAX_TIMEOUT_MS:${BASH_MAX_TIMEOUT_MS:-}:60000"; do
  IFS=: read -r name value per_min <<< "$spec"
  [ -n "$value" ] || continue
  [[ "$value" =~ ^[0-9]+$ ]] || refuse "limite de temps $name illisible (« $value »)."
  [ $((value / per_min)) -ge 90 ] || refuse "limite de temps $name = $((value / per_min)) min : il faut au moins 90 min pour un paquet."
done
for zone in "${zones[@]}"; do
  b="eq/$tour-$zone"
  git rev-parse -q --verify "refs/heads/$b" > /dev/null && continue
  if git rev-parse -q --verify "refs/remotes/origin/$b" > /dev/null; then git branch -q "$b" "origin/$b"; continue; fi
  git fetch -q origin "refs/heads/$b:refs/heads/$b" 2> /dev/null || refuse "la branche $b n'existe ni ici ni sur origin."
  echo "$b : créée depuis origin"
done
prevol_out="$(bash scripts/bench/pre-vol.sh "$tour" "${zones[@]}" 2>&1 || true)"
printf '%s\n' "$prevol_out" | grep -q 'PRÉ-VOL : PRÊT' || { printf '%s\n' "$prevol_out" >&2; refuse "PRÉ-VOL pas PRÊT (ci-dessus)."; }
paquet="$(mktemp -d)"
trap 'rm -rf "${paquet:?}"' EXIT
: > "$paquet/fusions"
PS_PAQUET_ETAPE=controle PS_PAQUET_LOT="$paquet" "${portal[@]}" || refuse "les contrôles d'une zone ont refusé (ci-dessus)."
mapfile -t union < <(cat "$paquet"/filtres-* 2> /dev/null | awk 'NF && !seen[$0]++')
revs=(HEAD); for zone in "${zones[@]}"; do revs+=("eq/$tour-$zone"); done
# Gradle's --tests: a pattern (« * » any run of characters) on the class's full name or « class.method »; without a
# « * », a name starting with a capital also matches the simple name.
unknown="$(python3 -I - "${revs[@]}" -- "${union[@]}" <<'PY'
import re, subprocess, sys
args = sys.argv[1:]; cut = args.index("--"); revs, pats = args[:cut], args[cut + 1:]
decl = re.compile(r"^\s*(?:(?:abstract|open|internal|private|public|data|sealed|final|static)\s+)*(?:class|object)\s+([A-Z]\w*)")
classes = set()
for rev in revs:
    out = subprocess.run(["git", "grep", "-I", "-E", r"^(package |\s*((abstract|open|internal|private|public|data|sealed|final|static)\s+)*(class|object)\s+[A-Z])",
                          rev, "--", "app/src/test*/**.kt", "app/src/test*/**.java"], capture_output=True, text=True).stdout
    pkg = {}
    for line in out.splitlines():
        _, path, text = line.split(":", 2)
        if text.startswith("package "):
            pkg[path] = text[8:].strip().rstrip(";").strip("`")
    for line in out.splitlines():
        _, path, text = line.split(":", 2)
        m = decl.match(text)
        if m:
            classes.add((pkg.get(path, "") + "." if pkg.get(path) else "") + m.group(1))
def rx(p):
    return re.compile(".*".join(re.escape(x) for x in p.split("*")))
def matches(p):
    forms = [p]
    head, _, last = p.rpartition(".")
    if head and last[:1].islower() and "*" not in last:
        forms.append(head)  # « class.method »: the class part
    for fq in classes:
        if any(rx(f).fullmatch(fq) for f in forms):
            return True
        if "*" not in p and p[:1].isupper() and p.split(".")[0] == fq.rsplit(".", 1)[-1]:
            return True
    return False
for p in pats:
    if not matches(p):
        print(p)
PY
)"
[ -z "$unknown" ] || refuse "filtre(s) de tests sans aucune classe : $(printf '« %s » ' $unknown)"
echo "Pré-contrôle : PRÊT (${zones[*]}; ${#union[@]} filtre(s))."

# --- 1-3. The batch ---------------------------------------------------------------------------------------------
start="$(git rev-parse HEAD)"
fallback() {
  echo "Paquet rouge ($1) : retour à $(git rev-parse --short "$start"), puis zone par zone." >&2
  git merge --abort 2> /dev/null || true
  git reset -q --keep "$start" 2> /dev/null || git reset -q --hard "$start"
  set +e; "${portal[@]}"; code=$?; set -e
  exit "$code"
}
PS_PAQUET_ETAPE=fusion PS_PAQUET_LOT="$paquet" "${portal[@]}" || fallback "fusions"
green=1
env -u PS_REJEUX_DIR PS_REJEUX_SELFTEST="$paquet" scripts/bench/rejeu-defauts.sh > "$paquet/rejeux.txt" 2>&1 || green=0
cat "$paquet/rejeux.txt"
[ -f "$paquet/selftest.ok" ] || green=0
if [ "$green" -eq 1 ] && [ ${#union[@]} -gt 0 ]; then
  args=(:app:testDebugUnitTest); for f in "${union[@]}"; do args+=(--tests "$f"); done
  scripts/bench/gradle-file.sh "${args[@]}" || green=0
fi
[ "$green" -eq 1 ] || fallback "self-test commun, rejeux ou tests"
PS_PAQUET_ETAPE=traces PS_PAQUET_LOT="$paquet" "${portal[@]}" || fallback "traces"
echo "Paquet ${zones[*]} : fusionné avec un seul self-test ($(cat "$paquet/commun")). Rien n'est poussé."
