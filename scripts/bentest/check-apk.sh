#!/usr/bin/env bash
# BenStudio Test (docs/PLAN-TEST-APK.md, T0): checks an APK built with -PbenTest before it goes to the phone.
#   scripts/bentest/check-apk.sh [app/build/outputs/apk/release/app-release.apk]
# - it is BenStudio Test (com.pocketstudio.test), signed, and holds the robot (its classes, kept by name);
# - after R8, the app's own code (not the robot's) still sets each test tag the essai éclair touches: the robot finds
#   the screens by those tags, so a tag shrunk away would only show on the phone.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APK="${1:-$(ls "$ROOT"/app/build/outputs/apk/release/*.apk 2>/dev/null | head -1)}"
[ -f "$APK" ] || { echo "no APK: build it with -PbenTest :app:assembleRelease" >&2; exit 1; }
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"
BT="$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)"
[ -x "$BT/dexdump" ] || { echo "dexdump not found" >&2; exit 1; }

pkg="$("$BT/aapt2" dump packagename "$APK")"
[ "$pkg" = "com.pocketstudio.test" ] || { echo "FAIL: package $pkg, not BenStudio Test" >&2; exit 1; }
"$BT/apksigner" verify "$APK" >/dev/null || { echo "FAIL: APK not signed" >&2; exit 1; }

DEX_DIR="$(mktemp -d)"
trap 'rm -rf "${DEX_DIR:?}"' EXIT
unzip -q -o "$APK" 'classes*.dex' -d "$DEX_DIR"
for f in "$DEX_DIR"/classes*.dex; do "$BT/dexdump" -d "$f" 2>/dev/null; done > "$DEX_DIR/dump.txt"

for cls in bentest/BenTestActivity bentest/InAppDriver bentest/EssaiEclair bentest/Carnet app/journey/Journey; do
    grep -q "Class descriptor *: 'Lcom/pocketstudio/$cls;'" "$DEX_DIR/dump.txt" || { echo "FAIL: robot class $cls missing" >&2; exit 1; }
done

# The tags the essai éclair touches or waits for (EssaiEclair.kt, Journey.kt); a trailing _ is a prefix the app completes
# (« _family_ »: StyleFamilies builds « wizard_family_… » from its own tag).
TAGS="intro welcome welcome_later top_mode mode_card_pro mode_switch launch launch_continue launch_new starter_wizard
mode_card_simple wizard _family_ wizard_style_ wizard_style_choose_ wizard_create_now guide_card_ guide_option_
guide_panel guide_bar thumb_play top_song top_projects projects projects_back tab_ structure_parts mixer export_what_
export_format_ export live_screen space_bar space_rail"
app_strings="$(awk '
    /Class descriptor/ { robot = ($0 ~ /Lcom\/pocketstudio\/(bentest|app\/journey)\//) }
    /const-string/ && !robot { if (match($0, /"[^"]*"/)) print substr($0, RSTART + 1, RLENGTH - 2) }
' "$DEX_DIR/dump.txt" | sort -u)"
missing=""
for t in $TAGS; do
    grep -qx -F "$t" <<<"$app_strings" || missing="$missing $t"
done
[ -z "$missing" ] || { echo "FAIL: tags no longer set by the app after R8:$missing" >&2; exit 1; }
echo "BenStudio Test OK: $pkg, signed, robot in, $(wc -w <<<"$TAGS") tags set by the app ($(du -h "$APK" | cut -f1))"
