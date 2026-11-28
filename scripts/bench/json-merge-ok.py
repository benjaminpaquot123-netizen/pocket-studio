#!/usr/bin/env python3
"""Exit 0 when a team's JSON (flat key/value) is a plain merge of its reviewed copy and fx-int.
Args: reviewed base fx team. For each key: if only the team changed it, the team's reviewed value
is expected; if only fx-int changed it, fx-int's; if both changed it, either is accepted."""
import json, sys
o, b, t, l = (json.load(open(p, encoding="utf-8")) for p in sys.argv[1:5])
M = object()
for k in set(o) | set(b) | set(t) | set(l):
    ov, bv, tv, lv = o.get(k, M), b.get(k, M), t.get(k, M), l.get(k, M)
    team, fx = ov != bv, tv != bv
    allowed = [ov, tv] if team and fx else ([ov] if team else [tv])
    if not any(lv is a or lv == a for a in allowed):
        sys.exit(1)
sys.exit(0)
