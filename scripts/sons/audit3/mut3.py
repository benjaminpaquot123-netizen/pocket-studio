"""Auditor's own mutations of tout.py (audit 3), run through audit2/mutate.py's runner.
usage: python3 -I mut3.py <audit2 dir> <pristine copy> <work dir> <out file> <budget seconds>"""
import sys
sys.path.insert(0, sys.argv[1])
import mutate  # noqa: E402

orig = list(mutate.M)
mutate.M[:] = []
MINE = [
    ("confirm: two bare numbers near -> first taken", 'if len(near) == 1:', 'if near:'),
    ("confirm: bare bpm tolerance 3 % -> 8 %", 'BPM_CONFIRM_TOL = 0.03', 'BPM_CONFIRM_TOL = 0.08'),
    ("confirm: lone key kept whatever the measured root", 'if root and NOTE_NAMES[key[0]] == root.group(0)]', 'if root]'),
    ("pack style overrides the inner style", 'if r.get("style") in (None, "", "unlabelled"):', 'if True:'),
    ("kit row file_id from the last folder only", '"file_id": _file_id("/".join(key))}', '"file_id": _file_id(key[-1])}'),
    ("no second measure after a confirmed bare bpm", 'tags = dict(tags, bpm=bpm)\n            m = measure(sr, x, tags)', 'tags = dict(tags, bpm=bpm)'),
    ("kit stems not re-measured after kit bpm confirmed", 'for r, s, x in loaded:  # the stems', 'for r, s, x in []:  # the stems'),
    ("melodic one-shot of role full gets end_cut", 'hit_full = typ == "drum_oneshot" and row.get("role") == "full"', 'hit_full = typ in END_TYPES and row.get("role") == "full"'),
    ("drum stem keeps the kit's lone key letter", 'tags["key_lone"] = None  # no key is heard', 'pass  # no key is heard'),
    ("bare hat no longer decided by the sound", 'if cl["subtype"] == "hat" and m.get("hat_measured"):', 'if False:'),
    ("midi: name tempo no longer in bpm_est", 'row["bpm_est"] = row["bpm_tag"]  # the name', 'pass  # the name'),
    ("untagged melodic loop keeps bars of hidden guess", 'row["bars"] = None  # counted from the hidden guess', 'pass  # counted from the hidden guess'),
    ("flat key no longer spelled as flat", 'elif tags.get("flat"):', 'elif False:'),
    ("kit midi row loses its file_id", 'row["file_id"] = _file_id(rel)\n            midi_rows.append(row)', 'midi_rows.append(row)'),
    ("--trace no longer finds a kit folder", 'rels.append("/".join(r.split("/")[:ki + 1]))', 'pass'),
    ("one-shot keeps the name tempo as bpm_tag", 'if typ in END_TYPES and row.get("bpm_tag") is not None:', 'if False:'),
    ("one-shot note accuracy: 50 cents -> 500", 'acc.add(typ, "note", abs(m["cents_gap"]) <= 50)', 'acc.add(typ, "note", abs(m["cents_gap"]) <= 500)'),
    ("kit bars not recounted from the confirmed tempo", 'kr["bars"] = bars_of(dur, kr["bpm_tag"])', 'pass'),
    ("any kit stem shows its guessed tempo", 'drums = typ == "kit_stem" and row.get("role") == "drums"', 'drums = typ == "kit_stem"'),
    ("bare number sure without a key beside it", 'if (keyed or only) and len(bare) == 1:', 'if len(bare) == 1:'),
    ("kit stem row loses its file_id", '"error": "", "size_kb": _size_kb(p),\n                   "file_id": _file_id(rel)}', '"error": "", "size_kb": _size_kb(p)}'),
    ("kit stem drum key cells kept", 'm["key_measured"] = m["key_match"] = m["key_strength"] = None', 'pass'),
]
for i, (name, old, new) in enumerate(MINE):
    mutate.m("a3-%02d" % i, name, old, new)
mutate.M.extend(orig)
sys.argv = [sys.argv[0]] + sys.argv[2:]
mutate.main()
