#!/usr/bin/env bash
# Method v3 (docs/audits/AUDIT-ULTRA.md §13.4, §13.9): an instruction card is copied from the template, only its
# variables change. Prints a card of docs/audits/ultra/GABARIT-FICHES-V3.md, ready to paste, its variables filled.
#   scripts/bench/fiche.sh <fiche: 0..7> [CLE=valeur …]     the card; fails, listing them, when a variable is missing
#   scripts/bench/fiche.sh --brouillon <fiche> [CLE=valeur …]   the same, the missing variables left as « <…> »
#   scripts/bench/fiche.sh --variables <fiche>              its variables: « CLE <tab> <placeholder> <tab> what to do »
# The text is read from the template each time (the « > » lines of the card's section), never kept here. Fiche 3 is
# fiche 2 plus the lines of section 3; fiche 6 is « La fiche du relecteur » of docs/AIDE-FUSIONS-TESTS.md plus the
# lines of section 6; fiche 4 holds two cards (4a the reader, 4b the sorter; 4 prints both).
# A key is its placeholder in capitals, without accents, « _ » for anything else: <siège> → SIEGE, <dossier du lot> →
# DOSSIER_DU_LOT. Which « <…> » are variables (docs/decisions/tour-6-portail3.md):
#   - a placeholder seen at least once outside « … » is the chief's: « à remplir », and replaced everywhere;
#   - one seen only inside « … » is a template the recipient fills (« rouge: <sujet> »): left as it is, unless given;
#   - « <branche> <sha> » side by side inside « … » is the recipient's own branch and commit: never replaced;
#   - a few are command templates the recipient fills though they sit outside « … » (RECIPIENT below), and the
#     portal's line of fiche 7 is the chief's though it sits inside (CHIEF below);
#   - « <a | b | c> » is a choice: key CHOIX, one of its values (its first letters are enough, accents or not); an
#     alternative that holds a placeholder of its own asks for it;
#   - <numéro du paquet suivant> is read from scripts/bench/portail-paquet.txt (+ 1) when not given.
# Tests (scripts/bench/test_portail3.py): PS_FICHE_GABARIT=<a toy template>, PS_FICHE_AIDE=<a toy help>.
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
exec python3 -I - "$root" "$@" <<'PY'
import os
import re
import sys
import unicodedata

USAGE = "usage: scripts/bench/fiche.sh [--brouillon | --variables] <fiche: 0..7, 4a, 4b> [CLE=valeur …]\n"
# Placeholders the recipient fills himself though they sit outside « … » (keys, by card), and the reverse.
RECIPIENT = {"2": {"DOSSIER_DU_SIEGE", "N", "ID", "ECRAN"}, "4": {"BRANCHE", "DOSSIER_DU_SIEGE", "N"}, "5": {"DATE", "TOUR", "ZONE"}}
RECIPIENT["3"] = RECIPIENT["2"]
CHIEF = {"7": {"LIGNE"}}
NEXT_BATCH = "NUMERO_DU_PAQUET_SUIVANT"


def key_of(text):
    plain = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^A-Za-z0-9]+", "_", plain).strip("_").upper()


def tokens(line):
    """The line cut in ("text", s) and ("hole", inside) pieces; a hole is a balanced « <…> » (it may hold others)."""
    out, i, last = [], 0, 0
    while i < len(line):
        if line[i] == "<":
            depth, j = 1, i + 1
            while j < len(line) and depth:
                depth += {"<": 1, ">": -1}.get(line[j], 0)
                j += 1
            if depth == 0:
                out.append(("text", line[last:i]))
                out.append(("hole", line[i + 1:j - 1]))
                i = last = j
                continue
        i += 1
    out.append(("text", line[last:]))
    return out


def alternatives(inside):
    """The alternatives of a choice « a | b <x> | c », or [] when the hole is a plain variable."""
    parts, depth, cur = [], 0, ""
    for piece in re.split(r"( \| |<|>)", inside):
        if piece == "<":
            depth += 1
        elif piece == ">":
            depth -= 1
        if piece == " | " and depth == 0:
            parts.append(cur)
            cur = ""
        else:
            cur += piece
    parts.append(cur)
    return parts if len(parts) > 1 else []


def sections(path):
    """{digit: [the lines of its « ## N. » section]}."""
    out, cur = {}, None
    with open(path, encoding="utf-8") as f:
        for line in f.read().splitlines():
            m = re.match(r"^## (\d+)\. ", line)
            if m:
                cur = out.setdefault(m.group(1), [])
            elif line.startswith("## "):
                cur = None
            elif cur is not None:
                cur.append(line)
    return out


def quotes(lines):
    """The cards of a section: each run of « > » lines, without the mark."""
    cards, cur = [], None
    for line in lines:
        if line.startswith(">"):
            if cur is None:
                cur = []
                cards.append(cur)
            cur.append(line[1:].lstrip(" ") if line[1:].strip() else "")
        else:
            cur = None
    return cards


def card_lines(n, gabarit, aide):
    """The lines of card n, or None."""
    secs = sections(gabarit)
    digit, part = n[0], n[1:]

    def own(d):
        return quotes(secs.get(d, []))

    if digit == "3":
        base, extra = own("2"), own("3")
        return base[0] + [l for c in extra for l in c] if base and extra and not part else None
    if digit == "6":
        text = open(aide, encoding="utf-8").read() if os.path.isfile(aide) else ""
        m = re.search(r"^## La fiche du relecteur\s*\n(.*?)(?=^## |\Z)", text, re.M | re.S)
        extra = own("6")
        if not m or not extra or part:
            return None
        return [l for l in m.group(1).strip("\n").splitlines()] + [l for c in extra for l in c]
    cards = own(digit)
    if not cards:
        return None
    if part:
        index = "ab".find(part)
        return cards[index] if len(part) == 1 and 0 <= index < len(cards) and len(cards) > 1 else None
    out = []
    for c in cards:
        out += ([""] if out else []) + c
    return out


class Card:
    def __init__(self, n, lines, values, root):
        self.n, self.lines, self.values, self.root = n, lines, dict(values), root
        self.outside, self.inside_only, self.choices, self.nested = {}, {}, [], {}
        for line in lines:
            self.walk(line, collect=True)
        for k in list(self.outside):
            if k in RECIPIENT.get(n[0], ()):
                self.inside_only.setdefault(k, self.outside.pop(k))
        for k in list(self.inside_only):
            if k in CHIEF.get(n[0], ()):
                self.outside[k] = self.inside_only.pop(k)
        for k in self.outside:
            self.inside_only.pop(k, None)
        if NEXT_BATCH in self.outside and not self.values.get(NEXT_BATCH):
            try:
                text = open(os.path.join(root, "scripts", "bench", "portail-paquet.txt"), encoding="utf-8").read()
                self.values[NEXT_BATCH] = str(int(re.search(r"^paquet:[ \t]*(\d+)", text, re.M).group(1)) + 1)
            except (OSError, AttributeError):
                pass

    def choice_key(self, inside):
        if inside not in self.choices:
            self.choices.append(inside)
        i = self.choices.index(inside)
        return "CHOIX" if i == 0 else f"CHOIX{i + 1}"

    def chosen(self, inside):
        """(the alternative chosen or None, why not)."""
        alts = alternatives(inside)
        given = key_of(self.values.get(self.choice_key(inside), ""))
        if not given:
            return None, "manque"
        hits = [a for a in alts if key_of(re.sub(r"<.*", "", a)).startswith(given) or key_of(a) == given]
        if len(hits) != 1:
            return None, "valeur"
        return hits[0], ""

    def walk(self, line, collect=False, draft=False):
        """The line filled (or, with collect, its holes recorded)."""
        out, quoted = "", False
        toks = tokens(line)
        for i, (kind, s) in enumerate(toks):
            if kind == "text":
                for ch in s:
                    quoted = True if ch == "«" else False if ch == "»" else quoted
                out += s
                continue
            alts = alternatives(s)
            if alts:
                if collect:
                    self.choice_key(s)
                    for a in alts:
                        for k2, s2 in tokens(a):
                            if k2 == "hole":
                                self.nested[key_of(s2)] = (s2, s)
                    continue
                alt, _ = self.chosen(s)
                out += f"<{s}>" if alt is None else self.walk(alt, draft=draft)
                continue
            key = key_of(s)
            # « <branche> <sha> » side by side inside « … »: the recipient's own branch and commit.
            pair = quoted and ((key == "BRANCHE" and toks[i + 1:i + 3] == [("text", " "), ("hole", "sha")])
                               or (key == "SHA" and toks[max(0, i - 2):i] == [("hole", "branche"), ("text", " ")]))
            if collect:
                if not pair:
                    (self.inside_only if quoted else self.outside).setdefault(key, s)
                continue
            value = self.values.get(key, "")
            out += value if value and not pair else f"<{s}>"
        return out

    def known_keys(self):
        keys = set(self.outside) | set(self.inside_only) | set(self.nested)
        return keys | {self.choice_key(c) for c in self.choices}

    def missing(self):
        """[(key, placeholder, why)] of what stops the card."""
        out = []
        for c in self.choices:
            alt, why = self.chosen(c)
            if alt is None:
                values = ", ".join(alternatives(c))
                out.append((self.choice_key(c), f"<{c}>", "à choisir parmi : " + values if why == "manque" else
                            f"« {self.values.get(self.choice_key(c))} » n'est aucune de : {values}"))
            else:
                for k2, s2 in tokens(alt):
                    if k2 == "hole" and not self.values.get(key_of(s2)):
                        out.append((key_of(s2), f"<{s2}>", f"demandé par « {alt} »"))
        for key, s in self.outside.items():
            if not self.values.get(key) and not any(key == m[0] for m in out):
                out.append((key, f"<{s}>", "à remplir"))
        return out

    def listing(self):
        rows = []
        for c in self.choices:
            rows.append((self.choice_key(c), f"<{c}>", "à remplir : une des valeurs « " + " », « ".join(alternatives(c)) + " »"))
        for key, s in self.outside.items():
            default = f" (défaut : {self.values[key]}, lu dans scripts/bench/portail-paquet.txt)" if key == NEXT_BATCH and self.values.get(key) else ""
            rows.append((key, f"<{s}>", "facultatif" + default if default else "à remplir"))
        for key, (s, c) in self.nested.items():
            if key not in self.outside:
                rows.append((key, f"<{s}>", f"selon {self.choice_key(c)}"))
        for key, s in self.inside_only.items():
            if key not in self.nested:
                rows.append((key, f"<{s}>", "laissé au destinataire (rempli seulement si la clé est donnée)"))
        return rows


def main(argv):
    root, args = argv[0], argv[1:]
    draft = "--brouillon" in args
    listing = "--variables" in args
    args = [a for a in args if a not in ("--brouillon", "--variables")]
    if not args or not re.fullmatch(r"[0-7][ab]?", args[0]) or any("=" not in a for a in args[1:]):
        sys.stderr.write(USAGE)
        return 2
    n = args[0]
    gabarit = os.environ.get("PS_FICHE_GABARIT") or os.path.join(root, "docs", "audits", "ultra", "GABARIT-FICHES-V3.md")
    aide = os.environ.get("PS_FICHE_AIDE") or os.path.join(root, "docs", "AIDE-FUSIONS-TESTS.md")
    try:
        lines = card_lines(n, gabarit, aide)
    except OSError as e:
        sys.stderr.write(f"fiche : gabarit illisible : {e}\n")
        return 2
    if not lines:
        sys.stderr.write(f"fiche : pas de fiche {n} dans {gabarit}\n")
        return 2
    values = {}
    for a in args[1:]:
        k, v = a.split("=", 1)
        values[key_of(k)] = v.strip()
    card = Card(n, lines, values, root)
    if listing:
        for row in card.listing():
            print("\t".join(row))
        return 0
    unknown = sorted(set(values) - card.known_keys())
    if unknown:
        sys.stderr.write(f"fiche {n} : clé(s) inconnue(s) : {', '.join(unknown)} ; ses clés : {', '.join(sorted(card.known_keys()))}\n")
        return 2
    missing = card.missing()
    if missing and not draft:
        sys.stderr.write(f"fiche {n} : {len(missing)} variable(s) à donner (CLE=valeur), ou --brouillon :\n")
        for key, hole, why in missing:
            sys.stderr.write(f"  {key}\t{hole}\t{why}\n")
        return 1
    for line in lines:
        print(card.walk(line, draft=draft))
    return 0


sys.exit(main(sys.argv[1:]))
PY
