"""Indonesian: « bar » (a measure) → « birama », « take » → « rekaman » (with -mu, -ku, -nya); the top bar → « bilah atas »."""
import re, sys, os
sys.dont_write_bytecode = True  # no __pycache__ left in the repository
sys.path.insert(0, os.path.dirname(__file__))
import catlib

SPECIAL = {"Enregistrer ta prise": "Mulai rekaman"}


def conv(t):
    t = t.replace("Bar-bar", "Birama-birama").replace("bar-bar", "birama-birama")
    t = t.replace("Bar atas", "Bilah atas").replace("bar atas", "bilah atas")
    t = re.sub(r"(?<![\w-])([Tt])ake(?:-(mu|ku|nya))?(?![\w-])", lambda m: ("R" if m.group(1) == "T" else "r") + "ekaman" + (m.group(2) or ""), t)
    t = re.sub(r"(?<![\w-])([Bb])ar(?:-(nya))?(?![\w-])", lambda m: ("B" if m.group(1) == "B" else "b") + "irama" + (m.group(2) or ""), t)
    return t


if __name__ == "__main__":
    mode, files = sys.argv[1], sys.argv[2:]
    n = 0
    for f in files:
        fn = lambda k, v: SPECIAL.get(k, conv(v))
        ch = catlib.edit(f, fn) if mode == "apply" else [(k, v, fn(k, v)) for k, v in catlib.entries(f).items() if fn(k, v) != v]
        n += len(ch)
        if mode == "show":
            for k, v, nv in ch: print(f"{os.path.basename(f)}\t{k[:60]}\n   - {v}\n   + {nv}")
    print("changed", n, file=sys.stderr)
