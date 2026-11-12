#!/usr/bin/env python3
"""A small pairwise (all-pairs) case generator for the export matrix (lot E, scripts/export-matrix.sh).

Every pair of values of any two factors appears in at least one case, among the pairs some valid case
can hold (the constraints below). Deterministic greedy construction (AETG's idea: each new case is the
candidate that covers the most pairs not yet covered, candidates built factor by factor), so the same
factors give the same cases on every run. No third-party package: the in-repo generator the lot's rule
asks for when PICT or allpairspy is not installed.

    pairwise.py [--check]   prints the cases as TSV (header first); --check also proves the coverage
"""
import itertools
import sys

# The matrix of PLAN-AUDITS-FINAUX §1 bis: format x rate x bits x target x ceiling, on 5 styles.
FACTORS = [
    ("style", ["techno", "house", "dnb", "trap", "lofi"]),
    ("format", ["wav", "flac", "m4a"]),
    ("rate", ["44100", "48000", "96000"]),
    ("bits", ["16", "24", "32"]),
    # spotify -14, apple -16, club -9, the style's own (the Drop), a free value (-11), none (no limiter).
    ("target", ["spotify", "apple", "club", "style", "free", "none"]),
    ("ceiling", ["-1", "-0.5", "-0.3"]),
]


def valid(case):
    """The constraints of the app's export options (a partial case: only the factors it has)."""
    f, r, b, t = case.get("format"), case.get("rate"), case.get("bits"), case.get("target")
    # M4A: 44.1 or 48 kHz, encoded from 16 bits.
    if f == "m4a" and (r == "96000" or (b is not None and b != "16")):
        return False
    # FLAC holds integers only.
    if f == "flac" and b == "32":
        return False
    # Without the limiter only a 32-bit float WAV keeps the peaks over 0 dBFS.
    if t == "none" and ((f is not None and f != "wav") or (b is not None and b != "32")):
        return False
    return True


def possible_pairs():
    names = [n for n, _ in FACTORS]
    values = dict(FACTORS)
    pairs = set()
    for (i, a), (j, b) in itertools.combinations(enumerate(names), 2):
        for va in values[a]:
            for vb in values[b]:
                # A pair counts if some full valid case holds it.
                rest = [values[n] if n not in (a, b) else [va if n == a else vb] for n in names]
                if any(valid(dict(zip(names, c))) for c in itertools.product(*rest)):
                    pairs.add(((i, va), (j, vb)))
    return pairs


def pairs_of(case):
    names = [n for n, _ in FACTORS]
    return {((i, case[a]), (j, case[b])) for (i, a), (j, b) in itertools.combinations(enumerate(names), 2)}


def completable(partial):
    names = [n for n, _ in FACTORS]
    values = dict(FACTORS)
    rest = [values[n] if n not in partial else [partial[n]] for n in names]
    return any(valid(dict(zip(names, c))) for c in itertools.product(*rest))


def generate():
    names = [n for n, _ in FACTORS]
    values = dict(FACTORS)
    todo = possible_pairs()
    cases = []
    while todo:
        best, best_gain = None, -1
        # Seeds: each uncovered pair (in a fixed order) starts a candidate, filled factor by factor.
        for seed in sorted(todo)[:60]:
            (i, va), (j, vb) = seed
            case = {names[i]: va, names[j]: vb}
            for k, n in enumerate(names):
                if n in case:
                    continue
                choice, gain = None, -1
                for v in values[n]:
                    trial = dict(case, **{n: v})
                    if not completable(trial):
                        continue
                    g = sum(1 for (x, xv), (y, yv) in todo
                            if (x == k and xv == v and names[y] in case and case[names[y]] == yv)
                            or (y == k and yv == v and names[x] in case and case[names[x]] == xv))
                    if g > gain:
                        choice, gain = v, g
                case[n] = choice
            g = len(pairs_of(case) & todo)
            if g > best_gain:
                best, best_gain = case, g
        cases.append(best)
        todo -= pairs_of(best)
    return cases


def main():
    cases = generate()
    names = [n for n, _ in FACTORS]
    print("\t".join(["case"] + names))
    for k, c in enumerate(cases, 1):
        print("\t".join([f"c{k:02d}"] + [c[n] for n in names]))
    if "--check" in sys.argv:
        need = possible_pairs()
        have = set().union(*(pairs_of(c) for c in cases))
        missing = need - have
        bad = [c for c in cases if not valid(c)]
        print(f"# {len(cases)} cases, {len(need)} pairs, missing {len(missing)}, invalid {len(bad)}", file=sys.stderr)
        if missing or bad:
            sys.exit(1)


if __name__ == "__main__":
    main()
