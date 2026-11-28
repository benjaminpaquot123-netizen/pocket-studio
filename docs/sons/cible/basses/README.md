# Zone B basses: targeted variants

320 Surge XT renders (no sample, no pack-derived audio): 10 styles x 4 families
(sub, reese, 808, phrase) x 8 variants, each optimised with CMA-ES toward the
zone B targets (`docs/sons/fin/cibles/zone-b-basses.tsv`, eq/son-fin-essentia-01 @ faa70cb3a)
and scored by distance (RMS of robust z, lower = closer). `index.json` lists file,
style, family, distance, voice, root and the Surge parameters.

Rebuild: `python -I scripts/son/cible/basses/cible.py <targets.tsv> <out> 8`, then
`python -I scripts/son/cible/basses/export.py <out> docs/sons/cible/basses`.
Needs surgepy, essentia, librosa, pycma and `cibles_desc.py` + `base.py` from eq/son-fin-essentia-01.

Notes: reese aims at wide and sub-heavy (p90); sub keeps at least median length;
edm uses the generic rows without peaks/high band (they are mostly dubstep).
Techno and dnb subs stay far (distance 1.4-1.6): their target subs are short and tonal.
