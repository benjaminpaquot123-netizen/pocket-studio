#!/usr/bin/env python3
"""Audit « banc crédible » 2, P1: the fresh review's verdict, as fusion-equipes.sh reads it.

  relecture-verdict.py [<replay id taken out of rejeux/> …] [changé:<replay id changed> …] [constat:<green sha>[:<its parent>] …] < review.md

Accepted only when:
  - its LAST non-empty line is « Verdict : 0 bloquant, 0 grave » (« — fusion possible » may follow, nothing else): a
    phrase found anywhere let « Verdict : 0 bloquant, 0 grave (avant correction du constat 1) » under a table row
    « | 1 | bloquant | … » through (forge2);
  - no table row has a cell « bloquant » or « grave » unless the row says « corrigé » (not « non corrigé » / « pas
    corrigé »);
  - each replay taken out of docs/audits/ultra/rejeux/ by the branch is named on a line « rejeu retiré : <id> : <raison> »,
    each one whose files it changes on a line « rejeu changé : <id> : <raison> » (review of the « portail » lot, M5).
Prints one line per refusal; exit 1 when there is one. Standard library only."""
import re
import sys

LAST = re.compile(r"^Verdict\s*:\s*0 bloquant,\s*0 grave(?:\s*[—-]+\s*fusion possible)?\s*\.?\s*$")
# Review of the « portail » lot (M1): a finding is blocking or grave when a table cell, a bullet or a heading STARTS
# with the word (« bloquant (P1) », « Grave ⚠ », « Gravité : grave »); it is fixed only on a positive « corrigé »:
# the row's last cell starting with it (« **Corrigé** en abc1234 »; not « corrigé ? non »), or « **corrigé** » in
# bold in a bullet or in each paragraph under a heading. A count table (« | bloquant | 0 | ») is no finding.
SEVERE = re.compile(r"^\W*(?:très\s+)?(?:gravit[ée]\s*:\s*)?(bloquant|grave)s?(?![\w-])", re.I)
# « G1 (grave) » in a heading or a bullet (second review of the lot, M8).
SEVERE_PAREN = re.compile(r"\((?:très\s+)?(bloquant|grave)s?\)", re.I)
FIXED_CELL = re.compile(r"^\W*corrigée?s?(?![\w-])(?!\s*\?)", re.I)
# The word followed at once by « corrigé » (« bloquant (corrigé en 7c84433) », « Grave — **corrigé (3d5f335)** »,
# « **Grave, corrigé.** »): the forms of the real reviews.
SEVERE_FIXED = re.compile(r"^\W*(?:gravit[ée]\s*:\s*)?(?:bloquant|grave)s?[\s\W]*corrigée?s?(?![\w-])(?!\s*\?)", re.I)
BOLD_FIXED = re.compile(r"\*\*\s*corrigée?s?\s*\*\*(?!\s*\?)", re.I)
# Third review of the « portail » lot (M10): the words just before the bold are read, whatever the spaces, colons or
# emphasis between them: « pas encore », « jamais », « non : », « *non* », « toujours pas » make it open.
NEGATION = {"non", "pas", "jamais", "nullement", "aucunement"}


class _Fixed:
    """FIXED_BOLD.search(text): a « **corrigé** » whose last word before it (or the one before « encore ») is no
    negation."""

    @staticmethod
    def search(text):
        for m in BOLD_FIXED.finditer(text):
            words = re.findall(r"[^\W\d_]+", text[:m.start()].lower())[-2:]
            if words and words[-1] == "encore":
                words = words[:-1]
            if not words or words[-1] not in NEGATION:
                return m
        return None


FIXED_BOLD = _Fixed()
NONE_AFTER = re.compile(r"^\W*(?:bloquants?|graves?)(?:\W+(?:et\s+)?(?:bloquants?|graves?))*\W*:?\s*aucun", re.I)


def table_refusals(line):
    cells = [c.strip() for c in line.strip().strip("|").split("|")]
    severe = [c for c in cells if SEVERE.match(c)]
    if not severe:
        return []
    numbers = [c.strip("* ") for c in cells if re.fullmatch(r"\**\s*\d+\s*\**", c)]
    if numbers and len(cells) - len(severe) == len(numbers):
        return [] if all(n == "0" for n in numbers) else [f"a count row of the review is not 0: {line.strip()[:100]}"]
    if FIXED_CELL.match(cells[-1]) or all(SEVERE_FIXED.match(c) for c in severe):
        return []
    return [f"a {SEVERE.match(severe[0]).group(1).lower()} row of the review is not « corrigé » (last cell): {line.strip()[:100]}"]


def refusals(text, removed, changed=(), evidence=()):
    out = []
    lines = [l.rstrip() for l in text.splitlines() if l.strip()]
    if not lines or not LAST.match(lines[-1].strip()):
        out.append("the review's last line is not « Verdict : 0 bloquant, 0 grave »"
                   + (f" (it is « {lines[-1].strip()[:80]} »)" if lines else " (empty review)"))
    raw = text.splitlines()
    for i, line in enumerate(raw):
        s = line.strip()
        if s.startswith("|"):
            out += table_refusals(s)
            continue
        bullet = re.match(r"^(?:[-*+•]|\d+[.)])\s+(.*)$", s)
        if bullet and (SEVERE.match(bullet.group(1)) or SEVERE_PAREN.search(bullet.group(1))) and not FIXED_BOLD.search(s) and not NONE_AFTER.match(bullet.group(1)) \
                and not SEVERE_FIXED.match(bullet.group(1)):
            out.append(f"a blocking or grave bullet of the review is not « **corrigé** »: {s[:100]}")
            continue
        heading = re.match(r"^#{1,6}\s+(.*)$", s)
        if heading and (SEVERE.match(heading.group(1)) or SEVERE_PAREN.search(heading.group(1))):
            section = []
            for nxt in raw[i + 1:]:
                if re.match(r"^\s*#{1,6}\s", nxt):
                    break
                section.append(nxt)
            paragraphs = [p.strip() for p in "\n".join(section).split("\n\n") if p.strip()]
            if paragraphs and re.match(r"^\W*aucun", paragraphs[0], re.I):
                paragraphs = paragraphs[1:]
            open_ = [p for p in paragraphs if not FIXED_BOLD.search(p) and not p.startswith("Verdict")]
            if open_:
                out.append(f"a finding under « {s[:40]} » is not « **corrigé** »: {open_[0][:80]}")
    for rid in removed:
        if not re.search(r"^rejeu retiré\s*:\s*" + re.escape(rid) + r"\s*:\s*\S", text, re.M):
            out.append(f"replay {rid} taken out of rejeux/ without « rejeu retiré : {rid} : <raison> » in the review")
    for rid in changed:
        if not re.search(r"^rejeu changé\s*:\s*" + re.escape(rid) + r"\s*:\s*\S", text, re.M):
            out.append(f"replay {rid} changed in rejeux/ without « rejeu changé : {rid} : <raison> » in the review")
    # Third review of the « portail » lot (M9): the line says the red was replayed on the green's parent, by its sha
    # (« rejoué sur <parent> », not « pas rejoué ») when the portal knows it (« constat:<green>:<parent> »).
    for item in evidence:
        sha, _, parent = item.partition(":")
        replayed = False
        for m in re.finditer(r"^rouge constaté\s*:\s*" + re.escape(sha) + r"\s*:\s*(.*)$", text, re.M):
            for r in re.finditer(r"(?<![\w-])(pas\s+)?rejoué\s+sur\s+([0-9a-f]{7,40})\b", m.group(1), re.I):
                if not r.group(1) and (not parent or parent.startswith(r.group(2).lower())):
                    replayed = True
        if not replayed:
            out.append(f"red evidence rouges-constates/{sha} brought without « rouge constaté : {sha} : rejoué sur "
                       f"{parent[:8] or '<parent du vert>'} : <ce qui a été vu> » in the review")
    return out


def main(argv):
    ids = argv[1:]
    found = refusals(sys.stdin.read(), [i for i in ids if ":" not in i],
                     [i.split(":", 1)[1] for i in ids if i.startswith("changé:")],
                     [i.split(":", 1)[1] for i in ids if i.startswith("constat:")])
    for r in found:
        print(r)
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
