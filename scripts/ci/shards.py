#!/usr/bin/env python3
"""CI: the app's unit tests split into N shards that run side by side, and the control that nothing was lost.

Why: one job ran the 877 test classes one after the other (43 to 66 minutes; four runs were cut at 75 minutes without
a single red test). Each shard is a job of its own (.github/workflows/ci.yml, « Unit tests (shard k/N) ») that runs
its list of classes in one JVM, as before; the job « Build, unit tests, screenshots » then puts the results together.

    shards.py durations <junit xml dir> [--out scripts/ci/test-durations.tsv]
        The seconds of every test class of a past run (its unit-test-reports artifact): the split balances on them.
    shards.py plan --classes-dir <compiled test classes> --shard k/N --out <dir> [--durations <tsv>]
        Writes <dir>/all.txt (every top-level class, sorted) and <dir>/classes.txt (this shard's classes), and
        <dir>/expected.txt (the classes of this shard that hold a JUnit test, so must leave a result).
        The split is stable: it depends only on the set of classes and on the durations file, never on the runner.
    shards.py check <dir> --shards N [--merge-into <repo root>]
        <dir>/<k>/ holds the artifact of shard k. Fails (exit 1) when a shard is missing, when two shards did not see
        the same set of classes, when a class is in no shard or in two, when a class ran in a shard that does not own it
        or ran twice, when a class that holds a test left no result, or when a test is red. Prints the time and the
        number of tests of each shard, the total, and each red test with its shard. With --merge-into, the JUnit XML,
        the HTML reports and the Roborazzi comparison are gathered where the single job left them.
    shards.py selftest
"""
import argparse
import re
import glob
import json
import os
import shutil
import struct
import sys
import tempfile
import xml.etree.ElementTree as ET

DEFAULT_SECONDS = 3.0   # a class the durations file does not know yet (the median class takes about 1 s)
JUNIT_TEST = b"Lorg/junit/Test;"
XML_DIR = "app/build/test-results/testDebugUnitTest"
HTML_DIR = "app/build/reports/tests/testDebugUnitTest"
ROBO_RESULTS = "app/build/test-results/roborazzi/debug"
ROBO_REPORTS = "app/build/reports/roborazzi/debug"
ROBO_OUTPUTS = "app/build/outputs/roborazzi"
PLAN_DIR = "build/shard"


POLICY_MAX = 50  # minutes; the slowest job (emulator smoke) takes 16 to 19

def top_level(name):
    """com.x.Outer$Inner (or a path to its class file) -> com.x.Outer."""
    return name.split("$", 1)[0]


def is_abstract_or_interface(data):
    """Reads the access flags of a class file (after its constant pool). False when the file cannot be read."""
    try:
        count = struct.unpack_from(">H", data, 8)[0]
        pos, i = 10, 1
        while i < count:
            tag = data[pos]
            if tag == 1:
                pos += 3 + struct.unpack_from(">H", data, pos + 1)[0]
            elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
                pos += 5
            elif tag in (5, 6):
                pos += 9
                i += 1
            elif tag in (7, 8, 16, 19, 20):
                pos += 3
            elif tag == 15:
                pos += 4
            else:
                return False
            i += 1
        flags = struct.unpack_from(">H", data, pos)[0]
        return bool(flags & 0x0400) or bool(flags & 0x0200)
    except (struct.error, IndexError):
        return False


def scan_classes(classes_dir):
    """Every top-level class of the folder -> True when one of its concrete class files holds a JUnit @Test."""
    found = {}
    for root, _dirs, files in os.walk(classes_dir):
        for f in files:
            if not f.endswith(".class"):
                continue
            rel = os.path.relpath(os.path.join(root, f), classes_dir)[:-len(".class")]
            name = top_level(rel.replace(os.sep, "."))
            with open(os.path.join(root, f), "rb") as h:
                data = h.read()
            holds = JUNIT_TEST in data and not is_abstract_or_interface(data)
            found[name] = found.get(name, False) or holds
    return found


def read_durations(path):
    out = {}
    if path and os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            for line in f:
                parts = line.rstrip("\n").split("\t")
                if len(parts) == 2 and not line.startswith("#"):
                    out[parts[0]] = float(parts[1])
    return out


def split(classes, durations, n):
    """Longest first, each class to the lightest shard so far (ties: the lowest shard). Stable for a given input."""
    loads = [0.0] * n
    shards = [[] for _ in range(n)]
    for name in sorted(classes, key=lambda c: (-durations.get(c, DEFAULT_SECONDS), c)):
        k = min(range(n), key=lambda i: (loads[i], i))
        shards[k].append(name)
        loads[k] += durations.get(name, DEFAULT_SECONDS)
    return [sorted(s) for s in shards], loads


def read_suites(xml_dir):
    """[(suite name, tests, skipped, [(class#method, message)], seconds)] of the TEST-*.xml of a folder (not recursive)."""
    suites = []
    for path in sorted(glob.glob(os.path.join(xml_dir, "TEST-*.xml"))):
        root = ET.parse(path).getroot()
        red = []
        for case in root.iter("testcase"):
            for kind in ("failure", "error"):
                node = case.find(kind)
                if node is not None:
                    message = (node.get("message") or (node.text or "").strip().splitlines()[0:1] or [""])
                    message = message if isinstance(message, str) else message[0]
                    red.append((f"{case.get('classname')}#{case.get('name')}", " ".join(message.split())[:300]))
                    break
        suites.append((root.get("name") or "", int(root.get("tests") or 0), int(root.get("skipped") or 0), red,
                       float(root.get("time") or 0)))
    return suites


def cmd_durations(args):
    totals = {}
    for name, _tests, _skipped, _red, seconds in read_suites(args.xml_dir):
        if "." in name and " " not in name:
            totals[top_level(name)] = totals.get(top_level(name), 0.0) + seconds
    if not totals:
        print(f"shards: no TEST-*.xml in {args.xml_dir}", file=sys.stderr)
        return 1
    with open(args.out, "w", encoding="utf-8") as f:
        f.write("# class\tseconds: one past CI run, read by scripts/ci/shards.py plan (refresh: shards.py durations).\n")
        for name in sorted(totals):
            f.write(f"{name}\t{totals[name]:.1f}\n")
    print(f"shards: {len(totals)} classes, {sum(totals.values()) / 60:.1f} min in all -> {args.out}")
    return 0


def cmd_plan(args):
    k, n = (int(x) for x in args.shard.split("/"))
    if not 1 <= k <= n:
        print(f"shards: --shard {args.shard}: k must be between 1 and N", file=sys.stderr)
        return 1
    found = scan_classes(args.classes_dir)
    if not found:
        print(f"shards: no class file in {args.classes_dir}: the tests were not compiled", file=sys.stderr)
        return 1
    durations = read_durations(args.durations)
    shards, loads = split(found, durations, n)
    os.makedirs(args.out, exist_ok=True)
    mine = shards[k - 1]
    for name, lines in (("all.txt", sorted(found)), ("classes.txt", mine),
                        ("expected.txt", [c for c in mine if found[c]])):
        with open(os.path.join(args.out, name), "w", encoding="utf-8") as f:
            f.write("".join(line + "\n" for line in lines))
    unknown = sum(1 for c in found if found[c] and c not in durations)
    print(f"shards: shard {k}/{n}: {len(mine)} of {len(found)} classes, about {loads[k - 1] / 60:.1f} min of tests "
          f"(the shards: {', '.join(f'{x / 60:.1f}' for x in loads)} min; {unknown} test class(es) without a known duration)")
    return 0


def lines_of(path):
    with open(path, encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip()]


def copy_tree(src, dst):
    if os.path.isdir(src):
        shutil.copytree(src, dst, dirs_exist_ok=True)


def merge_roborazzi(shard_dirs, into):
    """One results-summary.json for the whole run: the counts added up, the results one after the other."""
    summary, results, seen = {}, [], False
    for d in shard_dirs:
        path = os.path.join(d, ROBO_RESULTS, "results-summary.json")
        if not os.path.exists(path):
            continue
        seen = True
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        for key, value in (data.get("summary") or {}).items():
            if isinstance(value, (int, float)) and not isinstance(value, bool):
                summary[key] = summary.get(key, 0) + value
            else:
                summary.setdefault(key, value)
        results.extend(data.get("results") or [])
    if seen:
        os.makedirs(os.path.join(into, ROBO_RESULTS), exist_ok=True)
        with open(os.path.join(into, ROBO_RESULTS, "results-summary.json"), "w", encoding="utf-8") as f:
            json.dump({"summary": summary, "results": results}, f)
    return summary if seen else None


def cmd_check(args):
    n = args.shards
    problems, red, plans, alls, shard_dirs = [], [], {}, {}, []
    ran = {}            # top-level class -> shards where a result of it was found
    total_tests = total_skipped = total_suites = 0
    for k in range(1, n + 1):
        d = os.path.join(args.dir, str(k))
        plan = os.path.join(d, PLAN_DIR)
        if not os.path.exists(os.path.join(plan, "classes.txt")):
            problems.append(f"shard {k}/{n}: no result (its job did not reach the tests, or its artifact is missing)")
            continue
        shard_dirs.append(d)
        plans[k] = set(lines_of(os.path.join(plan, "classes.txt")))
        alls[k] = lines_of(os.path.join(plan, "all.txt"))
        suites = read_suites(os.path.join(d, XML_DIR))
        seen_here = set()
        tests = skipped = 0
        seconds = 0.0
        for name, t, s, r, sec in suites:
            tests, skipped, seconds = tests + t, skipped + s, seconds + sec
            red.extend((test, message, k) for test, message in r)
            if "." not in name or " " in name:
                continue   # « Gradle Test Run … »: a suite class that ran nothing (ShuffledOrderTest without -Pshuffle)
            cls = top_level(name)
            seen_here.add(cls)
            if cls not in plans[k]:
                problems.append(f"shard {k}/{n}: {cls} ran here but belongs to another shard")
        for cls in seen_here:
            ran.setdefault(cls, []).append(k)
        expected_path = os.path.join(plan, "expected.txt")
        for cls in (lines_of(expected_path) if os.path.exists(expected_path) else []):
            if cls not in seen_here:
                problems.append(f"shard {k}/{n}: {cls} holds a test but left no result")
        total_tests, total_skipped, total_suites = total_tests + tests, total_skipped + skipped, total_suites + len(suites)
        print(f"shard {k}/{n}: {len(suites)} classes, {tests} tests ({skipped} skipped), {seconds / 60:.1f} min of tests")
    if len(plans) == n:
        reference = alls[1]
        for k in range(2, n + 1):
            if alls[k] != reference:
                diff = sorted(set(alls[k]) ^ set(reference))
                problems.append(f"shard {k}/{n} did not see the same classes as shard 1/{n}: {', '.join(diff[:5])}")
        owners = {}
        for k, plan in plans.items():
            for cls in plan:
                owners.setdefault(cls, []).append(k)
        for cls in sorted(set(reference) - set(owners)):
            problems.append(f"{cls} is in no shard")
        for cls, ks in sorted(owners.items()):
            if len(ks) > 1:
                problems.append(f"{cls} is in shards {', '.join(map(str, ks))}")
            if cls not in set(reference):
                problems.append(f"{cls} (shard {ks[0]}) is not a class of the build")
    for cls, ks in sorted(ran.items()):
        if len(ks) > 1:
            problems.append(f"{cls} ran in shards {', '.join(map(str, ks))}")
    print(f"Tests : {total_tests} au total ({total_skipped} passés), {total_suites} classes, {len(red)} rouge(s), "
          f"{len(plans)}/{n} lots.")
    for test, message, k in red:
        print(f"ROUGE {test} (shard {k}/{n}) : {message}")
        print(f"::error title=Test rouge (shard {k}/{n})::{test} : {message}")
    for p in problems:
        print(f"PROBLEME {p}")
        print(f"::error title=Shards::{p}")
    if args.merge_into:
        for d in shard_dirs:
            k = os.path.basename(d)
            copy_tree(os.path.join(d, XML_DIR), os.path.join(args.merge_into, XML_DIR, f"shard-{k}"))
            copy_tree(os.path.join(d, HTML_DIR), os.path.join(args.merge_into, HTML_DIR, f"shard-{k}"))
            copy_tree(os.path.join(d, ROBO_OUTPUTS), os.path.join(args.merge_into, ROBO_OUTPUTS))
            copy_tree(os.path.join(d, ROBO_REPORTS), os.path.join(args.merge_into, ROBO_REPORTS, f"shard-{k}"))
            copy_tree(os.path.join(d, ROBO_RESULTS, "results"), os.path.join(args.merge_into, ROBO_RESULTS, "results"))
        merge_roborazzi(shard_dirs, args.merge_into)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as f:
            f.write(f"### Tests\n\n{total_tests} tests ({total_skipped} passés), {total_suites} classes, "
                    f"{len(red)} rouge(s), {len(plans)}/{n} lots.\n\n")
            f.write("".join(f"- ROUGE `{test}` (shard {k}/{n}) : {message}\n" for test, message, k in red))
            f.write("".join(f"- PROBLEME {p}\n" for p in problems))
    return 1 if problems or red else 0


def cmd_selftest(_args):
    """The split is a partition, stable and balanced; the check sees a lost class, a class run twice and a red test."""
    fails = []

    def expect(label, cond):
        if not cond:
            fails.append(label)

    def class_file(holds_test, abstract=False):
        pool = [b"\x01" + struct.pack(">H", len(JUNIT_TEST)) + JUNIT_TEST] if holds_test else [b"\x01\x00\x01x"]
        pool.append(b"\x05" + b"\x00" * 8)   # a long: two slots
        return (b"\xca\xfe\xba\xbe\x00\x00\x00\x3d" + struct.pack(">H", len(pool) + 2) + b"".join(pool)
                + struct.pack(">H", 0x0421 if abstract else 0x0021))

    def xml(folder, cls, tests=2, red=False, name=None):
        os.makedirs(folder, exist_ok=True)
        body = "".join(f'<testcase name="t{i}" classname="{cls}" time="0.1"/>' for i in range(tests - 1))
        body += (f'<testcase name="last" classname="{cls}" time="0.1"><failure message="expected 1 but was 2">x</failure></testcase>'
                 if red else f'<testcase name="last" classname="{cls}" time="0.1"/>')
        with open(os.path.join(folder, f"TEST-{cls}.xml"), "w", encoding="utf-8") as f:
            f.write(f'<testsuite name="{name or cls}" tests="{tests}" skipped="0" time="1.5">{body}</testsuite>')

    with tempfile.TemporaryDirectory() as tmp:
        classes = os.path.join(tmp, "classes", "com", "x")
        os.makedirs(classes)
        names = [f"T{i:02d}Test" for i in range(20)]
        for name in names:
            with open(os.path.join(classes, name + ".class"), "wb") as f:
                f.write(class_file(True))
            with open(os.path.join(classes, name + "$Inner.class"), "wb") as f:
                f.write(class_file(False))
        for name, data in (("Helper", class_file(False)), ("BaseTest", class_file(True, abstract=True))):
            with open(os.path.join(classes, name + ".class"), "wb") as f:
                f.write(data)
        found = scan_classes(os.path.join(tmp, "classes"))
        expect("scan: 22 top-level classes", len(found) == 22)
        expect("scan: a helper and an abstract base hold no test of their own",
               not found["com.x.Helper"] and not found["com.x.BaseTest"] and found["com.x.T00Test"])
        durations = {f"com.x.{n}": float(i * i) for i, n in enumerate(names)}
        shards, loads = split(found, durations, 3)
        expect("split: a partition", sorted(c for s in shards for c in s) == sorted(found))
        expect("split: stable", split(dict(reversed(list(found.items()))), durations, 3)[0] == shards)
        expect("split: balanced", max(loads) - min(loads) <= max(durations.values()))
        tsv = os.path.join(tmp, "d.tsv")
        with open(tsv, "w", encoding="utf-8") as f:
            f.write("".join(f"{c}\t{s}\n" for c, s in durations.items()))

        def run(tag, lose=None, twice=None, red=None, drop_shard=None, stray=None):
            base = os.path.join(tmp, tag)
            for k in (1, 2, 3):
                if k == drop_shard:
                    continue
                d = os.path.join(base, str(k))
                cmd_plan(argparse.Namespace(classes_dir=os.path.join(tmp, "classes"), shard=f"{k}/3",
                                            out=os.path.join(d, PLAN_DIR), durations=tsv))
                for cls in lines_of(os.path.join(d, PLAN_DIR, "expected.txt")):
                    if cls != lose:
                        xml(os.path.join(d, XML_DIR), cls, red=(cls == red))
                if k == 1:
                    xml(os.path.join(d, XML_DIR), "Gradle-Test-Run", tests=1, name="Gradle Test Run :app:testDebugUnitTest")
                if twice and twice not in lines_of(os.path.join(d, PLAN_DIR, "classes.txt")):
                    xml(os.path.join(d, XML_DIR), twice)
                if stray and k == 2:
                    with open(os.path.join(d, PLAN_DIR, "all.txt"), "a", encoding="utf-8") as f:
                        f.write(stray + "\n")
                os.makedirs(os.path.join(d, ROBO_RESULTS), exist_ok=True)
                with open(os.path.join(d, ROBO_RESULTS, "results-summary.json"), "w", encoding="utf-8") as f:
                    json.dump({"summary": {"total": 10, "changed": k, "added": 0}, "results": [{"type": "changed", "k": k}]}, f)
            out = os.path.join(base, "merged")
            code = cmd_check(argparse.Namespace(dir=base, shards=3, merge_into=out))
            return code, out

        devnull = open(os.devnull, "w")
        real_stdout, sys.stdout = sys.stdout, devnull
        summary_env = os.environ.pop("GITHUB_STEP_SUMMARY", None)
        try:
            code, out = run("green")
            expect("check: a whole green run passes", code == 0)
            merged = glob.glob(os.path.join(out, XML_DIR, "shard-*", "TEST-*.xml"))
            expect("check: every XML is gathered, one folder per shard", len(merged) == 21)
            with open(os.path.join(out, ROBO_RESULTS, "results-summary.json"), encoding="utf-8") as f:
                data = json.load(f)
            expect("check: the Roborazzi counts add up", data["summary"] == {"total": 30, "changed": 6, "added": 0}
                   and len(data["results"]) == 3)
            expect("check: a class that left no result is seen", run("lost", lose="com.x.T07Test")[0] == 1)
            expect("check: a class run in two shards is seen", run("twice", twice="com.x.T03Test")[0] == 1)
            expect("check: a red test is seen", run("red", red="com.x.T11Test")[0] == 1)
            expect("check: a missing shard is seen", run("drop", drop_shard=2)[0] == 1)
            expect("check: shards that saw different classes are seen", run("stray", stray="com.x.NewTest")[0] == 1)
        finally:
            sys.stdout = real_stdout
            devnull.close()
            if summary_env is not None:
                os.environ["GITHUB_STEP_SUMMARY"] = summary_env
    for label in fails:
        print(f"FAIL {label}")
    print(f"shards selftest: {'OK' if not fails else f'{len(fails)} failure(s)'}")
    return 1 if fails else 0


def cmd_policy(args):
    """The permanent rule (Benjamin, 2026-11-24: fast CI, many checks side by side, for the rest of the project):
    every job of the workflow has a time limit of POLICY_MAX minutes at most, the unit tests stay a matrix of shards,
    and the shard counts written in the workflow agree. A job that grows past the limit is split, not given more time."""
    text = open(args.workflow, encoding="utf-8").read()
    body = text.split("\njobs:\n", 1)[1] if "\njobs:\n" in text else ""
    jobs = re.split(r"(?m)^  (?=[A-Za-z_][\w-]*:\s*$)", body)[1:]
    problems = []
    if not jobs:
        problems.append("no job found")
    for j in jobs:
        jid = j.split(":", 1)[0]
        m = re.search(r"(?m)^    timeout-minutes:\s*(\d+)\s*$", j)
        if not m:
            problems.append(f"job « {jid} » has no timeout-minutes")
        elif int(m.group(1)) > POLICY_MAX:
            problems.append(f"job « {jid} » may run {m.group(1)} minutes (limit {POLICY_MAX}): split it instead")
    tests = next((j for j in jobs if j.startswith("tests:")), "")
    m = re.search(r"(?m)^        shard:\s*\[([\d, ]+)\]", tests)
    n = len(m.group(1).split(",")) if m else 0
    if n < 2:
        problems.append("the unit tests are not a matrix of shards (job « tests », matrix.shard)")
    else:
        counts = set(re.findall(r"matrix\.shard \}\}/(\d+)", text)) | set(re.findall(r"--shards (\d+)", text))
        if counts != {str(n)}:
            problems.append(f"shard counts disagree: matrix has {n}, the workflow writes {sorted(counts)}")
    for pr in problems:
        print(f"PROBLEME {pr}")
    print(f"ci policy: {len(jobs)} jobs, {n} shards, limit {POLICY_MAX} min: {'OK' if not problems else 'REFUSED'}")
    return 1 if problems else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("durations")
    p.add_argument("xml_dir")
    p.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "test-durations.tsv"))
    p.set_defaults(func=cmd_durations)
    p = sub.add_parser("plan")
    p.add_argument("--classes-dir", required=True)
    p.add_argument("--shard", required=True)
    p.add_argument("--out", required=True)
    p.add_argument("--durations", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "test-durations.tsv"))
    p.set_defaults(func=cmd_plan)
    p = sub.add_parser("check")
    p.add_argument("dir")
    p.add_argument("--shards", type=int, required=True)
    p.add_argument("--merge-into")
    p.set_defaults(func=cmd_check)
    p = sub.add_parser("policy")
    p.add_argument("workflow", nargs="?", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".github", "workflows", "ci.yml"))
    p.set_defaults(func=cmd_policy)
    p = sub.add_parser("selftest")
    p.set_defaults(func=cmd_selftest)
    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
