"""Names of bought packs, kept out of the public repository.

A pack title the scripts must recognise is stored as "sha:<16 hex>", the
sha256 of its words in lower case joined without separators ('Pack B' ->
'packb'). It matches a text when one to four consecutive words of the text
(maximal runs of letters and digits) hash the same, i.e. as a whole-word
phrase. A key without the prefix is a plain phrase, matched as whole words.
"""
import hashlib
import re

_WORD = re.compile(r"[a-z0-9]+")
MAX_WORDS = 4


def key(phrase):
    return "sha:" + hashlib.sha256("".join(_WORD.findall(phrase.lower())).encode()).hexdigest()[:16]


def _windows(text):
    w = _WORD.findall(text.lower())
    for i in range(len(w)):
        for n in range(1, MAX_WORDS + 1):
            if i + n <= len(w):
                yield "".join(w[i:i + n])


def matcher(k):
    """Callable text -> bool for a key ('sha:...' or a plain phrase)."""
    if k.startswith("sha:"):
        h = k[4:]

        def m(text):
            return any(hashlib.sha256(s.encode()).hexdigest()[:16] == h for s in _windows(text))
        return m
    rx = re.compile(r"(?<![a-z])%s(?![a-z])" % re.escape(k), re.I)
    return lambda text: bool(rx.search(text))
