#!/usr/bin/env python3
"""Test of the deep-pass merger on invented CSVs (no sound, no git).

  python3 -I scripts/sons/passe2/test_reunit.py

Proves the three refusals of reunit.py: duplicate key, unlisted error cell,
row total different from the control table (rows + kit_rows).
"""
import contextlib
import csv
import io
import os
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import reunit  # noqa: E402

FAILS = []


def check(cond, msg):
    print(("ok   " if cond else "FAIL ") + msg)
    if not cond:
        FAILS.append(msg)


CTRL = ["rar", "pack", "files_audio", "files_midi", "rows", "kit_rows", "errors", "count_ok",
        "tout_rc", "style_posed", "rows_pack_styled", "pack_line_ok", "style_forced",
        "rows_forced", "unresolved", "nested_archives", "skipped"]
DRUM = ["rar", "pack", "kit_id", "type", "subtype", "role", "style", "error", "t20_ms",
        "file_id", "cle"]
KIT = ["rar", "pack", "kit_id", "type", "subtype", "role", "style", "error", "file_id", "cle"]


def write(path, header, rows):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=header)
        w.writeheader()
        for r in rows:
            w.writerow({k: r.get(k, "") for k in header})


def drum(rar, n, error=""):
    return {"rar": rar, "pack": "p1", "type": "drum_oneshot", "subtype": "kick", "role": "kick",
            "style": "techno", "error": error, "t20_ms": "100", "file_id": "f%04d" % n,
            "cle": "r%s-p1-f%04d" % (rar, n)}


def make(root, rows_ctrl=3, kit_rows=1, dup=False, error="", errors_ctrl=0):
    write(os.path.join(root, "rar1", "rar1_controle.csv"), CTRL, [{
        "rar": "1", "pack": "p1", "files_audio": "3", "files_midi": "0", "rows": str(rows_ctrl),
        "kit_rows": str(kit_rows), "errors": str(errors_ctrl), "count_ok": "yes"}])
    rows = [drum("1", 1), drum("1", 2), drum("1", 3, error)]
    if dup:
        rows[2]["cle"] = rows[1]["cle"]
    write(os.path.join(root, "rar1", "rar1_drum_oneshot.csv"), DRUM, rows)
    write(os.path.join(root, "rar1", "rar1_kit.csv"), KIT, [{
        "rar": "1", "pack": "p1", "kit_id": "r1k001", "type": "kit", "style": "techno",
        "file_id": "k1", "cle": "r1-p1-k1"}])
    write(os.path.join(root, "rar1", "rar1_accuracy.csv"),
          ["rar", "pack", "type", "measure", "tagged", "agree", "half_double", "relative",
           "error_pct", "usable", "cle"], [{"rar": "1", "pack": "p1", "type": "drum_loop"}])


def run(root, out, admises=None):
    args = ["--src", root, "--out", out, "--no-passe1", "--quiet"]
    if admises:
        args += ["--admises", admises]
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
        try:
            rc = reunit.main(args)
        except SystemExit as e:
            rc = e.code if isinstance(e.code, int) else 1
    return rc, buf.getvalue()


def case(name, **kw):
    tmp = tempfile.mkdtemp(prefix="reunit-")
    src, out = os.path.join(tmp, "src"), os.path.join(tmp, "out")
    admises = kw.pop("admises", None)
    make(src, **kw)
    if admises is not None:
        path = os.path.join(tmp, "admises.csv")
        write(path, ["cle", "error", "raison"], admises)
        admises = path
    return run(src, out, admises) + (out,)


def main():
    rc, log, out = case("ok")
    check(rc == 0, "clean input accepted (rc %s)" % rc)
    with open(os.path.join(out, "drum_oneshot.csv"), newline="") as f:
        check(len(list(csv.DictReader(f))) == 3, "drum_oneshot table holds the 3 rows")
    check(os.path.exists(os.path.join(out, "kit.csv")), "kit table written")
    check(os.path.exists(os.path.join(out, "controle.csv")), "control table written")
    check(not os.path.exists(os.path.join(out, "accuracy_as_sound.csv")), "accuracy is not a sound table")

    rc, log, _ = case("dup", dup=True)
    check(rc != 0 and "duplicate" in log, "duplicate key refused (rc %s)" % rc)

    rc, log, _ = case("err", error="decode failed", errors_ctrl=1)
    check(rc != 0 and "error" in log, "unlisted error cell refused (rc %s)" % rc)

    rc, log, _ = case("err-admise", error="decode failed", errors_ctrl=1,
                      admises=[{"cle": "r1-p1-f0003", "error": "decode failed", "raison": "test"}])
    check(rc == 0, "listed error cell accepted (rc %s)" % rc)

    rc, log, _ = case("count", rows_ctrl=4)
    check(rc != 0 and "total" in log, "row total different from control refused (rc %s)" % rc)

    rc, log, _ = case("kitcount", kit_rows=0)
    check(rc != 0 and "total" in log, "kit rows counted in the total (rc %s)" % rc)

    print("%d failure(s)" % len(FAILS))
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main())
