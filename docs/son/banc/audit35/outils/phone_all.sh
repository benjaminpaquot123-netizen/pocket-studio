#!/usr/bin/env bash
# Render each style's export again (ps_render, as the bench does), measure phone/width/drop-repeat (phone.py),
# delete the WAV. The 6 listening styles keep theirs in $KEEP for extracts.py.
set -u
B=/home/user/ps-audit/build/bench
T=/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/audit-35
KEEP=$T/wav
mkdir -p $KEEP $T/tmpwav
: > $T/phone.jsonl
for f in $B/cmds/*.pscmd; do
    s=$(basename $f .pscmd)
    free=$(df --output=avail -k / | tail -1)
    if [ "$free" -lt 3500000 ]; then echo "LOW DISK, stop at $s" >&2; break; fi
    case " techno house trap jump_up acid_techno dubstep " in *" $s "*) w=$KEEP/$s.wav ;; *) w=$T/tmpwav/$s.wav ;; esac
    $B/tools/ps_render "$f" "$w" > /dev/null 2>&1 || { echo "render failed $s" >&2; continue; }
    echo "{\"style\": \"$s\", \"m\": $(python3 -I $T/tools/phone.py "$f" "$w")}" >> $T/phone.jsonl
    [ "$w" = "$T/tmpwav/$s.wav" ] && rm -f "$w"
done
echo done
