# Réponse à la critique à l'aveugle (2026-11-27)

Critique : `CRITIQUE-AUTRES.md`. Rejouer : `python3 -I scripts/sons/fin/cibles_autres.py --selftest`
(cas rouges avant, verts après), puis `python3 -I scripts/sons/fin/cibles_autres.py` et
`git diff --exit-code docs/sons/fin/cibles-autres-definitives.tsv` (aucun écart).

## Points graves
1. Tempo jugé « définitif » sans contre-mesure : corrigé. Nouveau statut « tempo du fabricant sûr »,
   jamais « définitive » (self-test : « tempo jamais définitive »). Rejouer : colonne `statut`, lignes 296 à 344.
2. Tempo d'un seul pack et techno : corrigé. Un vote par pack (médiane, au moins 5 boucles), sûr si au
   moins 2 packs à ±2 % ; techno : « pas de tempo unique » (r3p1 125 ; r1p1 et r5p1 150).
   « sons » = boucles étiquetées, accord sur celles qui ont une estimation (house 213 et 209/209).
3. Lignes d'un pack dominant ou trop minces : corrigé, sans exception à la main. Règle durcie (2 packs de
   5 sons, aucun pack au-dessus de 90 %), colonnes `packs_5` et `part_pack_max`. Lignes 284-289, 378, 420,
   230-232, 155 et 53 perdent leur statut ; 38, 41, 251, 363, 383, 426 restent, « portées surtout par un pack » ;
   8, 35, 152, 260 restent (2 packs de 5 sons, plus gros pack 57 à 67 %).
4. « Tout style d'un seul pack : pas de cible » : corrigé, phrase retirée ; la conclusion liste les styles
   avec et sans cible (lofi, trap et rave gardent une cible chacun, avec 2 packs de 5 sons).
5. Déplacement des 622 voix non dit : corrigé. Conclusion, section « Le déplacement des 622 voix »
   (605 boucles mélodiques, 17 coups « - », effet chiffré sur les deux familles et sur les voix) ;
   preuve que les 17 sont des voix dans `DOUTES.md`, B1.
6. « Aucun doute qui change une cible » : corrigé. B1 changeait des cibles ; il est levé par preuve.

## Points non graves
- « keys et arp 4 à 7 dB plus doux » : corrigé. Keys −20,6 dB contre synth −17,2 (3,4 dB) et lead −16,5
  (4,1 dB) ; arp n'a plus de cible.
- Accord du tempo house : corrigé. 213 boucles étiquetées, 209 avec estimation, 209 d'accord.

Aucun point refusé.

## Comptes
- Lignes définitives : 124 avant, 99 après ; tempos du fabricant sûrs : 3 (dnb 174, dubstep 140, house 125).
- Perdent leur statut (25) : 53-55 arp tous, 155-157 keys trap, 230-232 strings tous, 284-286 boucle
  mélodique house, 287-289 boucle mélodique lofi, 378-380 voix house, 420-422 effet house (21 lignes de
  mesure), plus les tempos 308, 316, 324 (« tempo du fabricant sûr ») et 340 (techno, pas sûr).
- Gardent leur statut, « portées surtout par un pack » (24 lignes) : 38-40 basse trap 81 %, 41-43 coup « - »
  tous 82 %, 251-253 synth house 80 %, 325-327 boucle de batterie house 89 %, 348-350 voix tous 83 %,
  363-365 voix dnb 83 %, 383-385 voix rave 80 %, 426-428 effet rave 89 %.
