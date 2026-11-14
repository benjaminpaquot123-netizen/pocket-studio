#!/usr/bin/env bash
# Audit ultra §5 bis: the project files of an old version, written by that version's own model code.
# For each tag: project-model/src/main at the tag, compiled with the Kotlin compiler Gradle already
# downloaded (no Gradle, no Android), then Gen1.kt (a small beat; from v0.9.95 the same with four
# notes) run against it; its JSON goes to app/src/test/resources/upgrade/<tag>/projects/.
# The settings of each tag (shared_prefs/*.xml) are written by hand from that tag's Settings.kt keys.
# Usage: scripts/upgrade-fixtures/make.sh <work dir> [tag...]
set -euo pipefail
W=${1:?work dir}; shift
TAGS=("$@"); [ ${#TAGS[@]} -gt 0 ] || TAGS=(v0.1.23 v0.9.95 v0.22.173 v0.22.175 audit-1)
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
C=~/.gradle/caches/modules-2/files-2.1
jar() { find "$C/$1" -name '*.jar' ! -name '*sources*' | sort | tail -1; }
CP="$(jar org.jetbrains.kotlin/kotlin-compiler-embeddable/2.4.21):$(jar org.jetbrains.kotlin/kotlin-stdlib/2.4.21):$(jar org.jetbrains.kotlin/kotlin-daemon-embeddable/2.4.21):$(jar org.jetbrains.kotlin/kotlin-script-runtime/2.4.21):$(jar org.jetbrains.kotlin/kotlin-reflect/2.4.10):$(jar org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.11.0):$(jar org.jetbrains/annotations/23.0.0)"
LIB="$(jar org.jetbrains.kotlinx/kotlinx-serialization-core-jvm/1.11.0):$(jar org.jetbrains.kotlinx/kotlinx-serialization-json-jvm/1.11.0):$(jar org.jetbrains.kotlin/kotlin-stdlib/2.4.21):$(jar org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.11.0)"
PLUG=$(jar org.jetbrains.kotlin/kotlin-serialization-compiler-plugin-embeddable/2.4.21)
kotlinc() { java -cp "$CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -nowarn "$@"; }
for T in "${TAGS[@]}"; do
  D="$W/$T"; rm -rf "${D:?}"; mkdir -p "$D/src"
  git -C "$ROOT" archive "$T" project-model/src/main | tar -x -C "$D/src"
  kotlinc -cp "$LIB" -Xplugin="$PLUG" -d "$D/out" $(find "$D/src" -name '*.kt')
  G="$ROOT/scripts/upgrade-fixtures/Gen1.kt"
  # v0.1.23 has no notes yet: the beat alone.
  if [ "$T" = v0.1.23 ]; then sed 's/val p = if (args.firstOrNull() == "notes").*/val p = beat()/' "$G" > "$D/Gen1.kt"; G="$D/Gen1.kt"; fi
  kotlinc -cp "$LIB:$D/out" -d "$D/gen" "$G"
  OUT="$ROOT/app/src/test/resources/upgrade/$T/projects"
  mkdir -p "$OUT/projet-1"
  java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "$LIB:$D/out:$D/gen" Gen1Kt beat > "$OUT/projet-1/project.json"
  if [ "$T" != v0.1.23 ]; then mkdir -p "$OUT/notes"; java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "$LIB:$D/out:$D/gen" Gen1Kt notes > "$OUT/notes/project.json"; fi
  echo "$T: written"
done
