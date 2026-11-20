#!/usr/bin/env bash
# Pre-fetch the Android jars Robolectric downloads at test time (android-all-instrumented), with retries and checksums.
#
# Why: Robolectric fetches them from Maven Central during the first test of each JVM, once per CI machine, and gives
# up on the first network error, failing every test of the machine at once (run 401, part 2: 533 tests). It skips the
# download when the jar is already in ~/.m2/repository, so this script puts it there first (and the CI caches it).
#
# Only an optimisation: an artifact this table does not know is still fetched by Robolectric itself, as before.
# When the Robolectric version (gradle/libs.versions.toml) or the sdk (app/src/test/resources/robolectric.properties)
# is not in the table, the script warns and does nothing.
#
# Refresh the table: run one Robolectric test locally, then ls ~/.m2/repository/org/robolectric/android-all-instrumented/
# (or read DefaultSdkProvider in the robolectric-<version>.jar for the build number of each sdk).
#
# Usage: scripts/ci-robolectric-jars.sh            fetch what is missing (no-op when everything is there)
#        scripts/ci-robolectric-jars.sh --names    print the artifact versions, one per line (for the cache key)
# Env:   ROBOLECTRIC_M2_DIR   local Maven repository (default ~/.m2/repository)
#        MAVEN_CENTRAL        remote repository (default https://repo1.maven.org/maven2)
set -euo pipefail

# "<robolectric version> <sdk>" -> android-all-instrumented version.
known_version() {
  case "$1 $2" in
    "4.17 36") echo "16-robolectric-13921718-i7" ;;
    *) return 1 ;;
  esac
}

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
robolectric="$(sed -n 's/^robolectric *= *"\([^"]*\)".*/\1/p' "$root/gradle/libs.versions.toml" | head -n 1)"
sdks="$(sed -n 's/^sdk *= *//p' "$root/app/src/test/resources/robolectric.properties" | tr ',' '\n' | tr -d ' \r')"

versions=()
for sdk in $sdks; do
  if v="$(known_version "$robolectric" "$sdk")"; then
    versions+=("$v")
  else
    echo "ci-robolectric-jars: no entry for Robolectric $robolectric, sdk $sdk; Robolectric will fetch it itself (refresh the table in this script)." >&2
  fi
done

if [ "${1:-}" = "--names" ]; then
  [ "${#versions[@]}" -gt 0 ] && printf '%s\n' "${versions[@]}"
  exit 0
fi

repo="${ROBOLECTRIC_M2_DIR:-$HOME/.m2/repository}"
remote="${MAVEN_CENTRAL:-https://repo1.maven.org/maven2}"
group="org/robolectric/android-all-instrumented"

# fetch <url> <dest>: download with retries into a temp file next to dest, then move it in place.
fetch() {
  local tmp="$2.part.$$"
  if curl --fail --silent --show-error --location --retry 5 --retry-all-errors --retry-delay 5 \
      --connect-timeout 30 -o "$tmp" "$1"; then
    mv -f "$tmp" "$2"
  else
    rm -f "$tmp"
    return 1
  fi
}

for v in "${versions[@]}"; do
  dir="$repo/$group/$v"
  mkdir -p "$dir"
  for ext in pom jar; do
    file="android-all-instrumented-$v.$ext"
    [ -s "$dir/$file" ] && [ -s "$dir/$file.sha512" ] && continue
    url="$remote/$group/$v/$file"
    echo "ci-robolectric-jars: fetching $file"
    sum=""
    for attempt in 1 2 3; do
      if fetch "$url.sha512" "$dir/$file.sha512.new" && fetch "$url" "$dir/$file.new"; then
        sum="$(tr -d ' \r\n' < "$dir/$file.sha512.new" | cut -c1-128)"
        if [ "$(sha512sum "$dir/$file.new" | cut -d' ' -f1)" = "$sum" ]; then
          mv -f "$dir/$file.new" "$dir/$file"
          mv -f "$dir/$file.sha512.new" "$dir/$file.sha512"
          break
        fi
        echo "ci-robolectric-jars: checksum mismatch for $file (attempt $attempt)" >&2
      fi
      rm -f "$dir/$file.new" "$dir/$file.sha512.new"
      sum=""
      sleep 5
    done
    if [ ! -s "$dir/$file" ]; then
      echo "ci-robolectric-jars: could not fetch $url after retries (Maven Central unreachable or checksum wrong)." >&2
      exit 1
    fi
  done
done
[ "${#versions[@]}" -gt 0 ] && echo "ci-robolectric-jars: ready: ${versions[*]}"
exit 0
