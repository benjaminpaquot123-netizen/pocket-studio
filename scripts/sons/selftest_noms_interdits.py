#!/usr/bin/env python3
"""Guard: the commercial name of the vendor of the bought sample packs never
appears in a file tracked by git (the repository is public).

The name itself is not written here: every 'ghost' followed by an optional
space, dash or underscore and four letters is hashed (lower case, separator
removed) and compared with the vendor's sha256. Binary files are skipped.

  python3 -I scripts/sons/selftest_noms_interdits.py      # exit 0 = clean
"""
import hashlib
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
VENDOR_SHA256 = {"7b9d81678d8fb41e84e9a9bdba11463dec063dca4a83684ba9812a78412a936b"}
CANDIDATE = re.compile(rb"ghost[ \t_\-]?([a-z]{4})", re.I)


def vendor_hits(blob):
    hits = []
    for m in CANDIDATE.finditer(blob):
        word = b"ghost" + m.group(1).lower()
        if hashlib.sha256(word.lower()).hexdigest() in VENDOR_SHA256:
            hits.append(blob[:m.start()].count(b"\n") + 1)
    return hits


def selfcheck():
    """The detector itself, on names built at run time (never stored in clear)."""
    name = (b"Ghost", b"hack")
    for sep in (b"", b" ", b"-", b"_"):
        for case in (bytes.lower, bytes.upper, lambda b: b):
            sample = b"x\n" + case(name[0]) + sep + case(name[1]) + b" - Pack"
            if vendor_hits(sample) != [2]:
                return "detector misses %r" % sample
    for clean in (b"ghost note", b"Ghost bass", b"ghostLane", b"ghost-kick"):
        if vendor_hits(clean):
            return "false alarm on %r" % clean
    return None


def main():
    err = selfcheck()
    if err:
        print("FAIL " + err)
        return 1
    files = subprocess.run(["git", "-C", REPO, "ls-files", "-z"], capture_output=True, check=True).stdout
    bad = []
    for rel in files.decode("utf-8", "replace").split("\0"):
        path = os.path.join(REPO, rel)
        if not rel or not os.path.isfile(path) or os.path.islink(path):
            continue
        with open(path, "rb") as fh:
            blob = fh.read()
        if b"\0" in blob[:8192]:
            continue
        for line in vendor_hits(blob):
            bad.append("%s:%d" % (rel, line))
    for b in bad:
        print("FAIL vendor name in " + b)
    print("%d failure(s)" % len(bad))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
