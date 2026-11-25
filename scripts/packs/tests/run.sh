#!/usr/bin/env bash
# Tests of the pack scripts (no network, no audio): licence whitelist, recipe sources, no audio in git.
set -euo pipefail
cd "$(dirname "$0")"
exec python3 -I -m unittest discover -s . -p 'test_*.py' "$@"
