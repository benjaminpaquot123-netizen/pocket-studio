# Conclusion : les sons hors coups de batterie (2026-11-27)

Chiffres : `cibles-autres-definitives.tsv` (481 lignes, 124 « définitive »), produit par
`scripts/sons/fin/cibles_autres.py` (self-test : `--selftest`). Packs écrits rarN/pN.
Règle : « définitive » = au moins 20 sons, au moins 2 packs, et une mesure que la contre-mesure
à l'aveugle retrouve au moins 98 fois sur 100. Durée, crête et volume moyen (RMS) : 10 441 sur 10 441.
Rien de ce qui suit n'est appliqué dans l'appli : ce sont des idées classées.

## Ce qui ne sert jamais de cible
- La tonalité estimée (21 % exacte sur 826 boucles), le tempo brut, la note des sons mélodiques
  (81 % d'accord seulement), la hauteur des voix, les enveloppes (attaque, tenue) : jamais contre-mesurées
  ou pas en accord.
- Le MIDI : jamais contre-mesuré, donc aucune cible (les chiffres sont donnés à titre indicatif).
- Tout style d'un seul pack (amapiano, lofi, trap, hiphop, rave, multi, drift-phonk, afro-house en tempo).

## Basse (coups mélodiques « bass »)
- Sûr : 749 sons, 16 packs. Durée médiane 0,86 s (moitié des sons entre 0,86 et 1,7 s) ; volume moyen
  −6,8 dB (entre −9,7 et −6,1) : les basses sont très fortes ; crête à −0,1 dB.
- Sûr aussi par style : dnb, dubstep, house, techno, trap.
- Pas de 808 étiquetée hors batterie : le kick 808 reste dans la famille batterie (hors de ce lot).
- Idées : 1) régler le volume par défaut des basses de l'appli près de −7 dB RMS ; 2) durée des notes de
  basse d'usine autour d'une seconde.

## Coups mélodiques par sous-type
- Sûr : synth (140 sons, 11 packs, 2 s, −17 dB), lead (139, 9 packs, 3,2 s, −16,5 dB), strings (106,
  3 packs, 0,7 s, −12,5 dB), keys (98, 7 packs, 6,9 s, −20,6 dB), chords (76, 8 packs, 6,4 s, −17 dB),
  pad (61, 7 packs, 13,7 s, −17 dB), arp (20, 5 packs, 4 s, −24 dB), sans sous-type (88, 4 packs).
- Pas de cible : bell, pluck, guitar, brass, perc, drums, full (moins de 20 sons).
- Idées : 1) les pads tiennent longtemps (11 à 15 s) : prévoir une queue longue sans couper ;
  2) keys et arp sont 4 à 7 dB plus doux que synth et lead : volume par défaut différent par sous-type ;
  3) crête des sons d'usine à −0,1 dB : garder de la marge dans l'appli (le mélange les additionne).

## Boucles mélodiques
- Sûr : 982 boucles, 13 packs ; durée médiane 7,7 s ; volume moyen −14 dB (entre −17 et −11).
  Par style : dnb, dubstep, house, lofi, techno.
- Pas utilisable : tonalité estimée (21 % exacte) ; tempo replié à 88 % d'accord seulement.
- Idées : 1) lire tempo et tonalité dans le nom du son plutôt que les deviner ; 2) aligner le volume
  des boucles importées vers −14 dB RMS.

## Boucles de batterie
- Sûr : 1 858 boucles, 13 packs ; durée 6,9 s ; volume moyen −17 dB (entre −22 et −11).
- Tempo (lu sur l'étiquette, gardé seulement quand l'estimation repliée est d'accord à ±2 %, 97 %
  des boucles) : dnb 174, dubstep 140, house 125 (tous trois très serrés), techno 125 à 150
  (médiane 150). La ligne « tous » n'est pas une cible : elle mélange les styles.
- Pas de cible : les autres styles (un seul pack).
- Idées : 1) tempo par défaut des modèles : dnb 174, dubstep 140, house 125 ; 2) techno : proposer 125
  et 150 ; 3) caler une boucle importée sur le tempo de son nom.

## Voix
- Sûr : 1 467 sons, 11 packs (dont les 622 petites voix d'ornement de r7p2 remises à leur place) ;
  durée 4,7 s ; volume moyen −19 dB ; crête autour de −3 dB (plus de marge que les autres sons).
  Par style : dnb, house, rave.
- Pas de cible : étendue de la mélodie et part chantée (jamais contre-mesurées).
- Idées : 1) volume par défaut des voix un peu plus haut que les boucles (elles sont plus douces) ;
  2) ajouter « adlib » et « sung » au repérage automatique des voix (`tout.py`, non modifié).

## Effets
- Sûr : 772 sons, 17 packs ; durée 7,7 s (entre 3,4 et 11 s) ; volume moyen −17 dB.
  Par style : dnb, dubstep, house, rave, techno.
- Idée : prévoir des effets longs (montées de 8 s) sans coupure ni boucle forcée.

## MIDI
- Pas de cible (non contre-mesuré). Indication seulement : 1 817 fichiers, 19 packs, 4 notes par
  mesure, étendue d'une dixième (10 demi-tons), 8 mesures.
- Idée : une contre-mesure MIDI (lecture par un second lecteur) avant d'en tirer des cibles.

## Doutes restants
- Aucun qui change une cible : voir `DOUTES.md` (chaque doute levé ou expliqué).
