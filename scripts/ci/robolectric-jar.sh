#!/usr/bin/env bash
# Puts Robolectric's Android jar (213 MB) in ~/.m2 before the tests, trying several Maven Central mirrors.
# Why: on 2026-11-24 repo1.maven.org answered 429 (rate limited) to this download, and every Robolectric
# test class of the run failed at « Failed to fetch maven artifact ». The jar is checked against its sha1.
# A jar already there (CI cache) is kept when its sha1 matches. Exits 0 even if every mirror fails:
# Robolectric then tries its own repository (robolectric.dependency.repo.url, app/build.gradle.kts).
set -uo pipefail
ver="${1:-16-robolectric-13921718-i7}"   # Robolectric 4.17, sdk=36 (app/src/test/resources/robolectric.properties)
path="org/robolectric/android-all-instrumented/$ver"
file="android-all-instrumented-$ver"
dir="$HOME/.m2/repository/$path"
mirrors=(
  https://maven-central.storage-download.googleapis.com/maven2
  https://repo.maven.apache.org/maven2
  https://maven-central-eu.storage-download.googleapis.com/maven2
  https://maven.aliyun.com/repository/central
)
mkdir -p "$dir"
want=""
for m in "${mirrors[@]}"; do
  want="$(curl -fsS --retry 3 --max-time 30 "$m/$path/$file.jar.sha1" 2>/dev/null | cut -c1-40)" && [ ${#want} = 40 ] && break
  want=""
done
if [ -z "$want" ]; then echo "robolectric-jar: no mirror gave the sha1, Robolectric will fetch it itself"; exit 0; fi
if [ -f "$dir/$file.jar" ] && [ "$(sha1sum "$dir/$file.jar" | cut -c1-40)" = "$want" ]; then
  echo "robolectric-jar: already there ($ver)"; exit 0
fi
for m in "${mirrors[@]}"; do
  if curl -fsS --retry 3 --retry-all-errors --max-time 600 -o "$dir/$file.jar.part" "$m/$path/$file.jar" \
     && [ "$(sha1sum "$dir/$file.jar.part" | cut -c1-40)" = "$want" ]; then
    mv "$dir/$file.jar.part" "$dir/$file.jar"
    curl -fsS --retry 3 --max-time 60 -o "$dir/$file.pom" "$m/$path/$file.pom" || true
    echo "robolectric-jar: fetched from $m"; exit 0
  fi
  echo "robolectric-jar: $m failed, next mirror"
done
rm -f "$dir/$file.jar.part"
echo "robolectric-jar: every mirror failed, Robolectric will fetch it itself"
