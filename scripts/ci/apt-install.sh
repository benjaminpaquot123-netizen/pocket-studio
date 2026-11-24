#!/usr/bin/env bash
# CI: installs system packages from a cached folder of .deb files first, from the network only when needed.
# Why: on run 45326803883 « apt-get install ffmpeg fdkaac » took 18.5 minutes of the song bench (a slow mirror),
# which alone pushed its job past 50 minutes. The .deb files are kept by actions/cache (folder: $PS_APT_CACHE,
# default ~/.cache/ps-apt); a download has retries and a short timeout; man-db's index is not rebuilt.
#   apt-install.sh <package>... [--optional <package>...]
# A package after --optional may be missing (a line says so); any other failure is the script's exit code.
set -uo pipefail
dir="${PS_APT_CACHE:-$HOME/.cache/ps-apt}"
sudo=""; [ "$(id -u)" = 0 ] || sudo="sudo"
need=(); optional=(); opt=0
for a in "$@"; do
  if [ "$a" = "--optional" ]; then opt=1; elif [ $opt = 1 ]; then optional+=("$a"); else need+=("$a"); fi
done
mkdir -p "$dir/partial"
$sudo rm -f /var/lib/man-db/auto-update
opts=(-y -qq --no-install-recommends -o "Dir::Cache::archives=$dir" -o Acquire::Retries=5
      -o Acquire::http::Timeout=20 -o Acquire::https::Timeout=20 -o Dpkg::Use-Pty=0)
updated=0
install() {   # from the cached .deb files; else the lists are refreshed (once) and the network is used
  $sudo env DEBIAN_FRONTEND=noninteractive apt-get install "${opts[@]}" --no-download "$@" > /dev/null 2>&1 && return 0
  if [ $updated = 0 ]; then $sudo apt-get update -qq -o Acquire::Retries=5 -o Acquire::http::Timeout=20 || true; updated=1; fi
  $sudo env DEBIAN_FRONTEND=noninteractive apt-get install "${opts[@]}" "$@" > /dev/null
}
status=0
if [ ${#need[@]} -gt 0 ]; then install "${need[@]}" || status=$?; fi
for p in "${optional[@]}"; do install "$p" || echo "apt-install: $p unavailable"; done
$sudo chown -R "$(id -u):$(id -g)" "$dir" 2> /dev/null || true
rm -rf "$dir/partial" "$dir/lock"
echo "apt-install: ${need[*]} ${optional[*]} (cache: $(ls "$dir" 2> /dev/null | grep -c '\.deb$') .deb, updated lists: $updated, exit $status)"
exit $status
