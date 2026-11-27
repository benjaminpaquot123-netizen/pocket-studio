#!/usr/bin/env bash
# Deep pass over one RAR archive: see lance.py. This wrapper owns the work folder
# (download + extraction) and deletes it whatever happens.
#   lance.sh --rar N --url 'DRIVE LINK' --out OUT_DIR [--plan] [--pack REL] [--force-style REGEX=STYLE]
# PASSE2_WORK: parent of the work folder (default: $TMPDIR or /tmp), never inside the repository.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
D="$(mktemp -d "${PASSE2_WORK:-${TMPDIR:-/tmp}}/passe2.XXXXXX")"
cleanup() { rm -rf -- "${D:?}"; }
trap cleanup EXIT INT TERM
python3 -I "$HERE/lance.py" --work "$D" "$@"
