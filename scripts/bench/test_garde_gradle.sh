#!/usr/bin/env bash
# PLAN-RESTE 7.2: the Gradle guard rail's own test (scripts/bench/pocket-studio-file.gradle, installed in
# ~/.gradle/init.d/ by scripts/bench/equipe-atelier.sh). A direct Gradle on pocket-studio must be refused with the
# guard's message; with PS_GRADLE_FILE=1 (what scripts/bench/gradle-file.sh sets) it must run.
#   scripts/bench/test_garde_gradle.sh
#   PS_GARDE=<file> scripts/bench/test_garde_gradle.sh   # test another guard file (an empty one must make it red)
# Light by design, the real ~/.gradle is never touched:
# - a temporary GRADLE_USER_HOME holds only init.d/ with the guard (an empty home would download Gradle and every
#   plugin of the real build again: minutes, and network);
# - a tiny project named « pocket-studio » with no plugin and the real build's configuration cache on (the guard
#   must hold on a cache hit too), run by the Gradle distribution already on the machine (the wrapper's own one in
#   ~/.gradle/wrapper/dists, else /opt/gradle*), --offline: nothing to fetch, about 20 s;
# - one daemon for the whole test, like gradle-file.sh leaves the daemon to the next run: a refused direct run AFTER
#   an accepted one checks that the daemon does not keep the accepted run's environment. Stopped at the end.
set -uo pipefail
src="$(cd "$(dirname "$0")/../.." && pwd)"
guard="${PS_GARDE:-$src/scripts/bench/pocket-studio-file.gradle}"
msg="run Gradle through scripts/bench/gradle-file.sh"

want="$(sed -n 's/^distributionUrl=.*gradle-\([0-9.]*\)-bin\.zip$/\1/p' "$src/gradle/wrapper/gradle-wrapper.properties")"
gradle_bin=""
for g in "$HOME"/.gradle/wrapper/dists/gradle-"$want"-bin/*/gradle-"$want"/bin/gradle /opt/gradle*/bin/gradle; do
  [ -x "$g" ] && { gradle_bin="$g"; break; }
done
[ -n "$gradle_bin" ] || { echo "SKIP: no Gradle distribution on this machine (wrapper cache or /opt/gradle*)"; exit 0; }

t="$(mktemp -d)"
export GRADLE_USER_HOME="$t/home"
cleanup() { env -u PS_GRADLE_FILE "$gradle_bin" --stop >/dev/null 2>&1; rm -rf "$t"; }
trap cleanup EXIT
mkdir -p "$GRADLE_USER_HOME/init.d" "$t/ps" "$t/autre"
cp "$guard" "$GRADLE_USER_HOME/init.d/pocket-studio-file.gradle"
for d in ps autre; do
  printf 'org.gradle.configuration-cache=true\norg.gradle.jvmargs=-Xmx512m\n' > "$t/$d/gradle.properties"
done
echo 'rootProject.name = "pocket-studio"' > "$t/ps/settings.gradle"
echo 'rootProject.name = "autre-projet"' > "$t/autre/settings.gradle"

fails=0
run() { # <dir> <expect: refused|ok> <label> [env assignments…]
  local dir="$1" expect="$2" label="$3"; shift 3
  local out status
  out="$(cd "$t/$dir" && env -u PS_GRADLE_FILE "$@" "$gradle_bin" help --offline --console=plain 2>&1)"; status=$?
  if [ "$expect" = refused ]; then
    if [ "$status" -ne 0 ] && grep -qF "$msg" <<<"$out"; then echo "ok    $label: refused with the guard's message"
    else echo "ROUGE $label: expected a refusal with « $msg », got exit $status"; tail -5 <<<"$out"; fails=$((fails + 1)); fi
  else
    if [ "$status" -eq 0 ] && ! grep -qF "$msg" <<<"$out"; then echo "ok    $label: accepted"
    else echo "ROUGE $label: expected to run, got exit $status"; tail -5 <<<"$out"; fails=$((fails + 1)); fi
  fi
}

run ps refused "direct ./gradlew on pocket-studio"
run ps ok "through gradle-file.sh (PS_GRADLE_FILE=1)" PS_GRADLE_FILE=1
run ps refused "direct again, same daemon, configuration cache stored"
run autre ok "direct on another project (the guard is for pocket-studio only)"

if [ "$fails" -gt 0 ]; then echo "test_garde_gradle: $fails ROUGE"; exit 1; fi
echo "test_garde_gradle: 4 ok"
