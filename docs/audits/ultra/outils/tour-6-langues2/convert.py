"""Tour 6, lot 5 (R15, n° 6): the trade's English words of a catalog said in the language's own script.

A Latin "run" (words joined by spaces or hyphens) is replaced only when the WHOLE run is a dictionary
entry, so compounds (« Low-pass », « Master KG ») are never half translated. Arabic: the clitics written
with a tatweel before a Latin word (الـ, للـ, بالـ, والـ, لـ, بـ, كـ) are joined to the Arabic word,
with its definite form after an article. Chinese and Japanese: no space left between two CJK words.
Usage: convert.py <dict-name> <apply|show> <files...>
"""
import json, re, sys, os
sys.dont_write_bytecode = True  # no __pycache__ left in the repository
sys.path.insert(0, os.path.dirname(__file__))
import catlib, dicts

RUN = re.compile(r"(?<![A-Za-z0-9_\\])[A-Za-z][A-Za-z'’]*(?:(?: |-)[A-Za-z][A-Za-z'’]*)*")
CJK = re.compile(r"[　-〿぀-ヿ一-鿿＀-￯]")
ARTICLE_CLITICS = [("والـ", "و"), ("بالـ", "ب"), ("كالـ", "ك"), ("فالـ", "ف"), ("للـ", "ل"), ("الـ", "")]
PLAIN_CLITICS = ["لـ", "بـ", "كـ", "وـ"]


def join(prefix, word):
    """An Arabic one-letter prefix joined to a word (ل + الـ = لل)."""
    if prefix == "ل" and word.startswith("ال"):
        return "لل" + word[2:]
    return prefix + word


def converter(name):
    d = dicts.DICTS[name]
    lang, words, case = d["lang"], d["words"], d.get("case", False)
    protected = d.get("protected", [])
    table = {(k if case else k.lower()): v for k, v in words.items()}

    def lookup(run):
        return table.get(run if case else run.lower())

    def conv(text):
        masked = text
        for i, p in enumerate(protected):
            masked = masked.replace(p, "\x00%d\x00" % i)
        out = []
        pos = 0
        for m in RUN.finditer(masked):
            run = m.group(0)
            # a placeholder (%s, %1$d) is not a word
            if m.start() > 0 and masked[m.start() - 1] in "%$":
                continue
            entry = lookup(run)
            if entry is None:
                continue
            indef, deff = entry if isinstance(entry, tuple) else (entry, entry if entry.startswith("ال") else "ال" + entry)
            before = masked[pos:m.start()]
            word = indef
            if lang == "ar":
                for c, pre in ARTICLE_CLITICS:
                    if before.endswith(c):
                        before = before[: -len(c)]
                        word = join(pre, deff)
                        break
                else:
                    for c in PLAIN_CLITICS:
                        if before.endswith(c):
                            before = before[: -len(c)]
                            word = join(c[0], indef)
                            break
            if lang in ("zh-CN", "ja"):
                if before.endswith(" ") and len(before) >= 2 and CJK.match(before[-2]) and CJK.match(word[0]):
                    before = before[:-1]
            out.append(before)
            out.append(word)
            pos = m.end()
            if lang in ("zh-CN", "ja") and masked[pos:pos + 1] == " " and len(masked) > pos + 1 and CJK.match(masked[pos + 1]) and CJK.match(word[-1]):
                pos += 1
        out.append(masked[pos:])
        res = "".join(out)
        for i, p in enumerate(protected):
            res = res.replace("\x00%d\x00" % i, p)
        return res
    return conv


def main():
    name, mode, files = sys.argv[1], sys.argv[2], sys.argv[3:]
    conv = converter(name)
    overrides = dicts.DICTS[name].get("overrides", {})
    total = 0
    for f in files:
        b = os.path.basename(f)
        if b == "mots_simples.json":
            continue
        def fn(k, v):
            if k in overrides:
                return overrides[k]
            return conv(v)
        if mode == "apply":
            ch = catlib.edit(f, fn)
        else:
            ch = [(k, v, fn(k, v)) for k, v in catlib.entries(f).items() if fn(k, v) not in (None, v)]
        total += len(ch)
        if mode == "show":
            for k, v, n in ch:
                print(f"{b}\t{k[:90]}\n   - {v}\n   + {n}")
    print("changed:", total, file=sys.stderr)


if __name__ == "__main__":
    main()
