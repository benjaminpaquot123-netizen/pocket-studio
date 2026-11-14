#!/usr/bin/env bash
# PLAN-RESTE A.1 (lot « banc-fin », 2026-11-10): replay what the audit « banc crédible » could not run.
#   docs/audits/ultra/audit-banc/rejoue-a1.sh propre|m1|m2 <results folder>
# propre: the code as it is: the six probes (AuditFideliteTmpTest), Tour5AppProofTest and the six tests of L2.
# m1: the bench's engine put back as bebe0c9 had it, finding by finding (F1 the song's end freezes no take, F2 no
#     currentStep, F4 the transport's step counted from the last mark at the tempo of the moment, no count-in, F5 the
#     part's positions counted from ▶), and the wiring of « Sauvegarder le morceau en .zip » / « Partager » broken where
#     AppRoot calls it (L1 today: the defects T5-ZIP and T5-ALERTE are fixed, the audit's diff no longer applies; the
#     guards must see a break of the wiring they compose). Expected: the six probes red, the three share guards red.
# m2: mutation L2 of the audit (scratchpad mutation-l2.py): NativeEngine.toggleClip, launchClipRow and armLaunch return
#     true without posting. Expected: the three Bench guards and the three proofs without the bench red.
# Each mutation is taken away file by file (git checkout -- <file>), whatever happens. One Gradle, by gradle-file.sh.
set -euo pipefail
cd "$(dirname "$0")/../../../.."
mode="${1:?propre|m1|m2}"
out="${2:?results folder}"
mkdir -p "$out"
out="$(cd "$out" && pwd)"
bench=app/src/test/java/com/pocketstudio/app/human/BenchEngine.kt
zip=app/src/main/java/com/pocketstudio/app/export/SaveProjectZip.kt
share=app/src/main/java/com/pocketstudio/app/export/ShareSongUi.kt
native=engine/src/main/java/com/pocketstudio/engine/NativeEngine.kt
case "$mode" in
  propre) files=() ;;
  m1) files=("$bench" "$zip" "$share") ;;
  m2) files=("$native") ;;
  *) echo "rejoue-a1 : mode inconnu $mode" >&2; exit 2 ;;
esac
for f in "${files[@]}"; do
  git diff --quiet HEAD -- "$f" || { echo "rejoue-a1 : $f est déjà modifié, refusé" >&2; exit 2; }
done
restore() { for f in "${files[@]}"; do git checkout -- "$f"; done; }
trap restore EXIT
python3 -I - "$mode" "$bench" "$zip" "$share" "$native" <<'PY'
import sys
mode, bench, zip_, share, native = sys.argv[1:]
subs = {
    "m1": [
        (bench, "                    freezeTakes(Math.round(due * 15.0 * RATE / tempo))\n",
         "                    // MUTATION A.1 F1: the song's end freezes no take (bebe0c9)\n"),
        (bench, "    override fun currentStep(): Int = song.currentStep()",
         "    override fun currentStep(): Int = -1  // MUTATION A.1 F2: no currentStep (CommandSink's default, bebe0c9)"),
        (bench, "    override fun transportStep(): Int {\n        song.advance()\n        return stepsNow()\n    }",
         "    override fun transportStep(): Int {  // MUTATION A.1 F4: no count-in, the time counted at the tempo of the moment\n"
         "        song.advance()\n"
         "        return if (transportSince < 0) -1 else ((SystemClock.uptimeMillis() - transportSince) * tempo / 15_000.0).toInt()\n    }"),
        (bench, "        return if (now < 0) -1 else now - song.sceneStart",
         "        return if (now < 0) -1 else now  // MUTATION A.1 F5: from ▶, not from the scene's start (bebe0c9)"),
        (bench, "        return if (transportSince < 0) -1.0 else if (at < 0) at else at - song.sceneStart",
         "        return if (transportSince < 0) -1.0 else at  // MUTATION A.1 F5: from ▶ (bebe0c9)"),
        (zip_, "saved = zip.file.name to zip.kept",
         "saved = zip.file.name to false  /* MUTATION A.1 L1: the wiring forgets the copy */"),
        (share, '                rhythmVm.guide.markDone("share.ready", "keep")\n',
         '                rhythmVm.guide.markDone("share.ready", "keep")\n'
         '                rhythmVm.setTempo(rhythmVm.project.value.tempoBpm + 1.0)  // MUTATION A.1 L1: the wiring touches the song once shared\n'),
    ],
    "m2": [
        (native, "fun toggleClip(pad: Int, velocity: Float = 1f) = post(CMD_TOGGLE_CLIP, velocity, pad)",
         "fun toggleClip(pad: Int, velocity: Float = 1f) = true  // MUTATION A.1 L2: the engine never hears a pad"),
        (native, "fun launchClipRow(first: Int, count: Int) = post(CMD_LAUNCH_CLIP_ROW, 0f, first, count)",
         "fun launchClipRow(first: Int, count: Int) = true  // MUTATION A.1 L2: the engine never hears a row"),
        (native, "        return post(CMD_ARM_LAUNCH, scene.toFloat(), mask and ALL_SLOTS, q or (grace shl 17))",
         "        return true  // MUTATION A.1 L2: the engine never hears a launch or a stop"),
    ],
}.get(mode, [])
texts = {}
for path, old, new in subs:
    t = texts.get(path) or open(path, encoding="utf-8").read()
    assert t.count(old) == 1, f"{path}: {old[:60]!r} found {t.count(old)} times"
    texts[path] = t.replace(old, new)
for path, t in texts.items():
    open(path, "w", encoding="utf-8").write(t)
print(f"rejoue-a1 {mode} : {len(subs)} substitution(s)")
PY
[ ${#files[@]} -eq 0 ] || git diff -- "${files[@]}" > "$out/mutation.diff" || true
tests=(--tests 'com.pocketstudio.app.human.AuditFideliteTmpTest' --tests 'com.pocketstudio.app.human.Tour5AppProofTest')
l2=(--tests 'com.pocketstudio.app.human.BenchTour3Test.padLiveRowsStartAndStopAndStopAllCutsThem'
    --tests 'com.pocketstudio.app.human.BenchAmeneesTest.padLiveLightsStopsAndRecordsAsOnAPhone'
    --tests 'com.pocketstudio.app.human.BenchTour4Test.liveStopsReachTheirLaunchLineOnTheBench'
    --tests 'com.pocketstudio.app.padlive.Tour3Zone1Test.aLivePadTouchedGoesToTheEngine'
    --tests 'com.pocketstudio.app.human.Tour3AppProofTest.padLiveRowsAndStopAllGoToTheEngine'
    --tests 'com.pocketstudio.app.human.Tour4AppProofTest.liveStopAllAndATracksStopGoToTheEngineAndTheGridFollows')
case "$mode" in
  propre) sel=("${tests[@]}" "${l2[@]}") ;;
  m1) sel=("${tests[@]}") ;;
  m2) sel=("${l2[@]}") ;;
esac
PS_GRADLE_LOG="$out/gradle.log" scripts/bench/gradle-file.sh :app:testDebugUnitTest "${sel[@]}" \
  "-Ptest.results.dir=$out/junit" --rerun --continue > "$out/gradle.txt" 2>&1 && echo "exit=0" >> "$out/gradle.txt" || echo "exit=$?" >> "$out/gradle.txt"
python3 -I - "$out/junit" <<'PY' | tee "$out/resume.txt"
import glob, html, os, re, sys
seen = set()
for f in sorted(glob.glob(os.path.join(sys.argv[1], "TEST-*.xml"))):
    text = open(f, encoding="utf-8").read()
    cls = re.search(r'<testsuite name="([^"]+)"', text).group(1).rsplit(".", 1)[-1]
    for m in re.finditer(r'<testcase name="([^"]+)"[^>]*?(?:/>|>(.*?)</testcase>)', text, re.S):
        body = m.group(2) or ""
        bad = re.search(r'<(failure|error) message="([^"]*)"', body)
        skipped = "<skipped" in body
        verdict = "sauté" if skipped else (f"rouge : {html.unescape(bad.group(2))[:220]}" if bad else "vert")
        if (cls, m.group(1)) not in seen:
            seen.add((cls, m.group(1)))
            print(f"{cls}#{m.group(1)} : {verdict}")
PY
