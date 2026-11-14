#!/usr/bin/env bash
# Tour 6, lot 5: the same word changes on the tutorial's texts (lot 2's files), so that a tutorial quotes what the
# screen now says. Usage: apply_to_tutos.sh <path to project-model/src/main/resources/i18n> [show|apply]
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"; I="$1"; mode="${2:-show}"
python3 -I "$here/convert.py" ar-trade "$mode" "$I"/ar/tuto*.json
python3 -I "$here/convert.py" hi-sections "$mode" "$I"/hi/tuto*.json
python3 -I "$here/convert.py" zh-sections "$mode" "$I"/zh-CN/tuto*.json
python3 -I "$here/it_words.py" "$mode" "$I"/it/tuto*.json | grep -c "^   +" || true
python3 -I "$here/id_words.py" "$mode" "$I"/id/tuto*.json
