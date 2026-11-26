#!/usr/bin/env bash
# Before/after MP3s of finished lots not yet in the app, one per style whose sound may change:
# 40 s of the song's main part (Drop or Refrain, not the intro) as the integration plays it, 1 s of
# silence, the same 40 s as the lot's branch plays it. Played as the phone does (scripts/live-bench.sh's
# path: the app test LiveSongRecorder records the app's commands, « Écouter » style (-Pbench.listen=drop),
# then ps_live plays them through the real-time engine). Never normalised: each half keeps the app's level.
#
#   scripts/son/avant_apres_styles.sh OUT_DIR [lot…]        (from the repository's root)
#
# Lots: house2 bass l12 techno l6 (all of them when none is given), the branch origin/eq/tour-9-gr-<lot>;
# the before is origin/ccr-80af1a1b-mn0wec. Override: AVANT=<rev>, LOT_<lot>=<rev> (e.g. LOT_l6=abc123).
# The styles: those whose recipe changed (StyleRecipes.kt); a lot that changes code every song goes through
# (engine, model, app) gets varied styles up to 8 (scripts/son/avant_apres_styles.py candidates).
# Writes OUT_DIR/<lot>_<style>.mp3, OUT_DIR/rapport.tsv (one line per style: status, levels before → after,
# ps_live alerts in the passage) and OUT_DIR/logs/. A style whose after is the same samples as its before
# is « identique » and gets no MP3. A lot that fails is reported at the end and the next one goes on.
# Work: git worktrees in a temporary folder outside the repository, removed as it goes (about 2 GB; the run stops a lot past 3 GB).
# Environment: JOBS (default 4), GRADLE_ARGS (default --max-workers=2), SECONDS_LIVE (default 90).
set -uo pipefail
ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "ERREUR : lance le script depuis le dépôt pocket-studio" >&2; exit 1; }
[ $# -ge 1 ] || { echo "usage : scripts/son/avant_apres_styles.sh DOSSIER_SORTIE [lot…]" >&2; exit 1; }
OUT="$(mkdir -p "$1" && cd "$1" && pwd)" || { echo "ERREUR : impossible de créer $1" >&2; exit 1; }
shift
LOTS=("$@")
[ ${#LOTS[@]} -gt 0 ] || LOTS=(house2 bass l12 techno l6)
AVANT="${AVANT:-origin/ccr-80af1a1b-mn0wec}"
JOBS="${JOBS:-4}"
SECONDS_LIVE="${SECONDS_LIVE:-90}"
HELPER="$ROOT/scripts/son/avant_apres_styles.py"
T0=$(date +%s)
for tool in ffmpeg cmake python3 git; do
    command -v "$tool" > /dev/null || { echo "ERREUR : $tool manque" >&2; exit 1; }
done
python3 -c "import numpy" 2> /dev/null || { echo "ERREUR : numpy manque (pip install numpy)" >&2; exit 1; }
ffmpeg -hide_banner -encoders 2> /dev/null | grep libmp3lame > /dev/null || { echo "ERREUR : ffmpeg sans libmp3lame" >&2; exit 1; }

lot_rev() {
    local var="LOT_$1"
    if [ -n "${!var:-}" ]; then echo "${!var}"; else echo "origin/eq/tour-9-gr-$1"; fi
}

echo "== Branches"
refs=("${AVANT#origin/}")
for lot in "${LOTS[@]}"; do refs+=("$(lot_rev "$lot" | sed 's#^origin/##')"); done
for r in "${refs[@]}"; do
    case "$r" in *[!0-9a-f]*) git -C "$ROOT" fetch -q origin "+$r:refs/remotes/origin/$r" 2> /dev/null || git -C "$ROOT" fetch -q origin "$r" 2> /dev/null || true ;; esac
done
git -C "$ROOT" rev-parse -q --verify "$AVANT^{commit}" > /dev/null || { echo "ERREUR : l'avant $AVANT est introuvable" >&2; exit 1; }

WORK="$(mktemp -d "${TMPDIR:-/tmp}/avant_apres.XXXXXX")"
WT="$WORK/wt"
# The links to the main checkout's submodules go first, so nothing ever deletes through them.
drop_worktree() {
    [ -d "$WT" ] || return 0
    find "$WT/engine/third_party" -maxdepth 1 -type l -delete 2> /dev/null
    git -C "$ROOT" worktree remove --force "$WT" > /dev/null 2>&1 || rm -rf "$WT"
    git -C "$ROOT" worktree prune > /dev/null 2>&1
}
cleanup() {
    drop_worktree
    rm -rf "$WORK"
}
trap cleanup EXIT
trap 'echo "interrompu" >&2; exit 130' INT TERM
mkdir -p "$OUT/logs"
: > "$OUT/rapport.tsv"
FAILED=()

# The worktree of REV in $WT: the engine's submodules (not tracked by the worktree) linked to the main
# checkout's when the commit pins the same ones, local.properties copied (the Android SDK's place).
checkout() {
    local rev="$1" sub pin have
    drop_worktree  # a lot that broke half-way leaves its worktree: never let it break the next one
    git -C "$ROOT" worktree add -q --detach "$WT" "$rev" || return 1
    while read -r sub; do
        pin="$(git -C "$ROOT" ls-tree "$rev" "$sub" | awk '{print $3}')"
        have="$(git -C "$ROOT/$sub" rev-parse -q --verify HEAD 2> /dev/null || true)"
        if [ -n "$pin" ] && [ "$pin" = "$have" ] && [ -n "$(ls -A "$ROOT/$sub" 2> /dev/null)" ]; then
            rmdir "$WT/$sub" 2> /dev/null; ln -sfn "$ROOT/$sub" "$WT/$sub"
        else
            git -C "$WT" submodule update -q --init --depth 1 "$sub" || { echo "submodule $sub" >&2; return 1; }
        fi
    done < <(git -C "$ROOT" config -f "$ROOT/.gitmodules" --get-regexp '^submodule\..*\.path$' | awk '{print $2}' | grep '^engine/')
    [ -f "$ROOT/local.properties" ] && cp "$ROOT/local.properties" "$WT/"
    return 0
}

# The 3 GB cap: the work folder (worktree, tools, WAVs) never grows past it; a lot that would is stopped.
MAX_KB=$((3 * 1024 * 1024))
disk_ok() {
    local used
    used="$(du -sk "$WORK" 2> /dev/null | awk '{print $1}')"
    [ "${used:-0}" -le "$MAX_KB" ]
}

# Renders STYLES (comma list) at REV into $WORK/<name>/<style>.wav, .csv, .txt as the phone plays them.
render() {
    local rev="$1" name="$2" styles="$3" dir="$WORK/$2" tools
    mkdir -p "$dir"
    disk_ok || { echo "dossier de travail au-delà de 3 Go" > "$OUT/logs/$name.err"; return 1; }
    checkout "$rev" || { echo "checkout de $rev raté" > "$OUT/logs/$name.err"; return 1; }
    # One build of the tools per engine tree (the recipe lots share the integration's engine).
    tools="$WORK/tools-$(git -C "$ROOT" rev-parse "$rev:engine" | cut -c1-12)"
    if [ ! -x "$tools/ps_live" ]; then
        { cmake -S "$WT/engine/bench" -B "$tools" -DCMAKE_BUILD_TYPE=Release \
            && cmake --build "$tools" -j "$JOBS" --target ps_render ps_live; } > "$OUT/logs/$name.cmake.log" 2>&1 \
            || { rm -rf "$tools"; echo "compilation de ps_live ratée (logs/$name.cmake.log)" > "$OUT/logs/$name.err"; return 1; }
        python3 -I "$WT/scripts/bench/live_selftest.py" "$tools/ps_live" "$WORK/selftest-$name" > "$OUT/logs/$name.selftest.log" 2>&1 \
            || { rm -rf "$tools"; echo "self-test de ps_live rouge (logs/$name.selftest.log)" > "$OUT/logs/$name.err"; return 1; }
        rm -rf "$WORK/selftest-$name"
    fi
    (cd "$WT" && ./gradlew ${GRADLE_ARGS:---max-workers=2} -q :app:testDebugUnitTest \
        --tests 'com.pocketstudio.app.bench.LiveSongRecorder' --rerun \
        -Pbench.live="$dir/cmds" -Pbench.render="$tools/ps_render" -Pbench.seconds="$SECONDS_LIVE" \
        -Pbench.listen=drop -Pbench.styles="$styles") > "$OUT/logs/$name.gradle.log" 2>&1 \
        || { echo "enregistrement Gradle raté (logs/$name.gradle.log)" > "$OUT/logs/$name.err"; return 1; }
    disk_ok || { echo "dossier de travail au-delà de 3 Go après Gradle" > "$OUT/logs/$name.err"; return 1; }
    export dir tools SECONDS_LIVE
    tr ',' '\n' <<< "$styles" | xargs -P "$JOBS" -I{} sh -c '
        [ -f "$dir/cmds/{}.pslive" ] || { echo "pas de .pslive pour {}" >&2; exit 0; }
        "$tools/ps_live" --seconds "$SECONDS_LIVE" --wav "$dir/{}.wav" --csv "$dir/{}.csv" "$dir/cmds/{}.pslive" > "$dir/{}.txt" 2>&1
        st=$?; [ $st = 0 ] || [ $st = 4 ] || { echo "ps_live a échoué sur {} ($st)" >&2; rm -f "$dir/{}.wav"; }' 2>> "$OUT/logs/$name.err"
    for f in "$dir"/*.txt; do [ -e "$f" ] && cp "$f" "$OUT/logs/$name.$(basename "$f")"; done
    rm -rf "$dir/cmds"
    # The worktree goes as soon as it has played: one at a time on the disk.
    drop_worktree
    return 0
}

echo "== Styles par lot"
declare -A STYLES_OF WHY_OF
UNION=""
for lot in "${LOTS[@]}"; do
    rev="$(lot_rev "$lot")"
    if ! git -C "$ROOT" rev-parse -q --verify "$rev^{commit}" > /dev/null; then
        echo "  $lot : branche $rev introuvable"; FAILED+=("$lot (branche introuvable)"); continue
    fi
    cap=""; [ "$lot" = l12 ] && cap="--max 8"
    list="$(python3 -I "$HELPER" candidates "$ROOT" "$AVANT" "$rev" $cap)" || { FAILED+=("$lot (styles)"); continue; }
    if [ -z "$list" ]; then echo "  $lot : aucun style touché"; printf '%s\t-\t-\taucun style touché\n' "$lot" >> "$OUT/rapport.tsv"; continue; fi
    STYLES_OF[$lot]="$(cut -f1 <<< "$list" | paste -sd,)"
    while IFS=$'\t' read -r s why; do WHY_OF[$lot/$s]="$why"; done <<< "$list"
    UNION="$UNION,${STYLES_OF[$lot]}"
    echo "  $lot ($(git -C "$ROOT" rev-parse --short "$rev")) : ${STYLES_OF[$lot]}"
done
UNION="$(tr ',' '\n' <<< "$UNION" | sed '/^$/d' | awk '!seen[$0]++' | paste -sd,)"
[ -n "$UNION" ] || { echo "ERREUR : aucun style à rendre" >&2; exit 1; }

echo "== Avant : $AVANT ($(tr ',' '\n' <<< "$UNION" | wc -l) styles)"
render "$AVANT" avant "$UNION" || { echo "ERREUR : rendu de l'avant raté : $(cat "$OUT/logs/avant.err")" >&2; exit 1; }

MP3=0
for lot in "${LOTS[@]}"; do
    [ -n "${STYLES_OF[$lot]:-}" ] || continue
    rev="$(lot_rev "$lot")"
    echo "== Après : $lot ($rev)"
    if ! render "$rev" "$lot" "${STYLES_OF[$lot]}"; then
        echo "  ÉCHEC : $(cat "$OUT/logs/$lot.err")"; FAILED+=("$lot ($(head -1 "$OUT/logs/$lot.err"))")
        rm -rf "$WORK/$lot"; continue
    fi
    for s in $(tr ',' ' ' <<< "${STYLES_OF[$lot]}"); do
        b="$WORK/avant/$s" a="$WORK/$lot/$s" mp3="$OUT/${lot}_$s.mp3"
        if [ ! -f "$b.wav" ] || [ ! -f "$a.wav" ]; then
            printf '%s\t%s\t%s\téchec (pas de rendu)\n' "$lot" "$s" "${WHY_OF[$lot/$s]}" >> "$OUT/rapport.tsv"; echo "  $s : échec (pas de rendu)"; continue
        fi
        res="$(python3 -I "$HELPER" assemble "$b.wav" "$b.csv" "$a.wav" "$a.csv" "$mp3" --log-before "$b.txt" --log-after "$a.txt" 2> "$OUT/logs/${lot}_$s.assemble.err" | tail -1)"
        status="$(python3 -c 'import json,sys; print(json.loads(sys.argv[1].splitlines()[-1])["status"])' "$res" 2> /dev/null || echo "échec")"
        meas="-"
        if [ "$status" = ok ]; then
            meas="$(python3 -I "$HELPER" measure "$mp3")" || status="à vérifier"
            MP3=$((MP3 + 1))
        fi
        printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$lot" "$s" "${WHY_OF[$lot/$s]}" "$status" "$res" "$meas" >> "$OUT/rapport.tsv"
        echo "  $s : $status"
    done
    rm -rf "$WORK/$lot"
done

DUR=$(( $(date +%s) - T0 ))
echo "== Fini : $MP3 MP3 écrits dans $OUT, en $((DUR / 60)) min $((DUR % 60)) s (rapport : $OUT/rapport.tsv)"
same="$(awk -F'\t' '$4=="identique"' "$OUT/rapport.tsv" | cut -f1,2 | tr '\t' _ | paste -sd' ')"
[ -n "$same" ] && echo "Identiques (pas de MP3) : $same"
check="$(awk -F'\t' '$4!="ok" && $4!="identique" && $4!="aucun style touché"' "$OUT/rapport.tsv" | cut -f1,2,4 | tr '\t' ' ' | paste -sd';')"
[ -n "$check" ] && echo "À vérifier ou en échec : $check"
if [ ${#FAILED[@]} -gt 0 ]; then
    echo "Lots en échec : ${FAILED[*]}" >&2
    exit 2
fi
exit 0
