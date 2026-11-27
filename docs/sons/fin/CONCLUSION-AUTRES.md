# Conclusion : les sons hors coups de batterie (2026-11-27, v3 après la seconde critique)

Chiffres : `cibles-autres-definitives.tsv` (481 lignes), produit par `scripts/sons/fin/cibles_autres.py` (`--selftest`).
Packs écrits rNpN. Rien n'est appliqué dans l'appli. Réponse à la seconde critique : `REPONSE-CRITIQUE2-AUTRES.md`.

## La règle : cinq conditions
Une ligne est « définitive » seulement si les cinq conditions tiennent :
1. au moins 20 sons ;
2. au moins 2 packs qui ont chacun au moins 5 sons (colonne `packs_5`) ;
3. aucun pack ne pèse plus de 90 % des sons (colonne `part_pack_max`, en %) ;
4. la contre-mesure à l'aveugle retrouve la valeur au moins 98 fois sur 100 sur ces sons ;
5. **la cible ne dépend pas d'un seul fabricant** : on refait la médiane sans le plus gros pack
   (colonne `mediane_sans_gros_pack`). Si elle sort de la moitié centrale de la ligne (p25 à p75)
   ET bouge d'au moins 1 dB (crête, volume moyen) ou d'au moins 20 % (durée), la ligne tombe
   (colonne `tient_sans_gros_pack` = non, raison « dépend d'un seul pack »).

Sinon : « pas de cible, on garde le son actuel » (raison dans la colonne `raison`). Une ligne définitive dont le plus
gros pack pèse plus de 75 % est « portée surtout par un pack » : sa médiane sans ce pack est écrite à côté.

Bilan : 78 lignes définitives (99 en v2, 124 en v1), 3 tempos du fabricant sûrs, 400 lignes sans cible.
La condition 5 retire 21 lignes : les 20 de la critique, plus la durée des boucles de batterie techno
(6,4 → 7,7 s sans r5p1 : +20 % tout juste, que la critique laissait passer par un arrondi).
8 lignes définitives sortent de p25–p75 sans le gros pack, mais de moins de 1 dB ou de 20 % : elles restent.

## Le tempo du fabricant
Le tempo n'a pas de contre-mesure : il n'est jamais « définitif ». Statut à part, « tempo du fabricant sûr » :
le tempo écrit par le fabricant, confirmé par notre estimation (à ±2 %, moitié et double compris).
Chaque pack d'au moins 5 boucles confirmées vote avec sa médiane. C'est sûr seulement si :
au moins 2 packs votent, tous à ±2 % les uns des autres, et au moins 90 % des boucles étiquetées
qui ont une estimation sont confirmées (ce dernier seuil ne refuse qu'afro-house, déjà refusé : un seul pack).
Colonnes `sons` et p10 à p90 : toutes les boucles étiquetées ; les votes, eux, ne prennent que les confirmées.

## Ce qui n'a pas de contre-mesure
Le tempo, le MIDI (notes par mesure, étendue, nombre de mesures), l'étendue des voix (`range_st`) et leur part
chantée (`voiced_pct`) n'ont jamais été remesurés par un outil indépendant : aucune n'est une cible définitive
(MIDI et voix : 63 lignes, toutes « pas de cible » ; tempo : statut à part, 3 sûrs et 10 non). Pas de cible non plus
sur la tonalité estimée (21 % exacte sur 826 boucles) ni sur la note des sons mélodiques (81 % d'accord).

## Quels styles gardent une cible
- Les sons sans étiquette de style n'ont pas de ligne de style à eux : ils ne comptent que dans « tous »
  (par exemple 1 219 voix sur 1 467, 99 strings sur 106, 99 effets sur 772).
- Avec au moins une cible : dnb, dubstep, techno, house, lofi (keys), trap (basse), rave (durée des effets).
- Sans aucune cible : afro-house, amapiano, ambient, drift-phonk, edm, hardstyle, hiphop, multi, rnb, synthwave,
  trance (moins de 2 packs de 5 sons dans le style).

## Basse (coups mélodiques « bass »)
- Définitif : 749 sons, 16 packs (plus gros 38 %). Durée médiane 0,86 s (moitié entre 0,86 et 1,7 s) ;
  volume moyen −6,8 dB ; crête −0,1 dB.
- Par style : dnb (35 sons, 0,86 s, −11,9 dB), dubstep (483, −6,3 dB), house (48, −13,2 dB),
  techno (30, 0,8 s, −13 dB), trap (27, −10,7 dB, portée surtout par un pack : 81 %, sans lui −11,3 dB).
- Tombent (un seul pack) : durée dubstep (0,86 → 1,7 s sans r8p1), house (1,05 → 0,72 s sans r10p2),
  trap (7,4 → 9,9 s sans r9p2).
- Idées : 1) volume par défaut des basses près de −7 dB RMS ; 2) notes de basse d'usine d'environ 1 s.

## Coups mélodiques par sous-type
- Définitif (tous styles) : synth (140 sons, 2 s, −17,2 dB), lead (139, 3,2 s, −16,5 dB),
  keys (98, 6,9 s, −20,6 dB), chords (76, 6,4 s, −16,9 dB), pad (61, 13,7 s, moitié entre 11,2 et 15 s),
  sans sous-type « - » (88, −13,2 dB, portée surtout par un pack : 82 %, sans lui −17 dB).
- Par style : lead dubstep (40, 1,7 s, −16,1 dB), keys lofi (26, 12,9 s, −22,4 dB) ;
  synth house et synth techno : la crête seulement.
- Tombent (un seul pack) : volume des pads (−16,9 → −14 dB sans r2p1), durée des coups « - »
  (1 → 7,3 s sans r11p1), synth house durée (1,8 → 0,96 s) et volume (−19,1 → −21,3 dB),
  synth techno durée (2 → 0,48 s) et volume (−20 → −17,4 dB).
- Plus de cible non plus : strings (un pack pèse 93 %), arp (un seul pack de 5 sons), keys trap ;
  bell, pluck, guitar, brass, perc, drums, full (moins de 20 sons).
- Idées : 1) pads longs (11 à 15 s) sans couper la queue ; 2) volume par défaut par sous-type : keys 3,4 dB sous
  synth (−20,6 contre −17,2) et 4,1 dB sous lead (−16,5) ; 3) crête d'usine à −0,1 dB : garder de la marge.

## Boucles mélodiques
- Définitif : 982 boucles, 13 packs (plus gros 15 %) ; durée 7,7 s ; volume moyen −14,1 dB.
  Par style : dnb (98, 11 s ; son volume tombe : −13,3 → −16,2 dB sans r11p2), dubstep (150, 6,9 s,
  −7,3 dB), techno (179, 7,7 s, −14,3 dB).
- Plus de cible : house (un pack pèse 99 %) et lofi (95 %).
- Idées : 1) lire tempo et tonalité dans le nom plutôt que les deviner ; 2) boucles importées vers −14 dB RMS.

## Boucles de batterie
- Définitif : 1 858 boucles, 13 packs (plus gros 25 %) ; durée 6,9 s ; volume moyen −17,3 dB.
  Par style : dnb (224, 5,5 s, −15 dB), dubstep (165, 6,9 s, −13,5 dB), house (213, 7,7 s, −19,9 dB,
  portée surtout par un pack : 89 % ; sans lui 7,75 s et −21 dB), techno (la crête seulement).
- Tombent (un seul pack) : techno volume (−14,6 → −21,1 dB sans r5p1) et durée (6,4 → 7,7 s).
- Tempo du fabricant :
  - dnb sûr : 174 (votes r10p1 174, r11p2 174, r1p1 172) ; 221 boucles, accord 217 sur 221 ;
  - dubstep sûr : 140 (r1p1, r8p1, r9p3 à 140) ; 164 boucles, accord 161 sur 164 ;
  - house sûr : 125 (r12p2 125, r1p1 124) ; 213 boucles, accord 209 sur 209 ;
  - techno : pas de tempo unique, 125 ou 150 selon le pack (r3p1 125 ; r1p1 et r5p1 150) ;
  - un seul pack qui vote, donc pas sûr : afro-house 120 (accord 57 %), amapiano 113, drift-phonk 120
    (médiane de toutes ses boucles : 125), hiphop 85, lofi 77, multi 128, rave 155, trap 95 ; « tous » : jamais.
- Idées : 1) tempo par défaut des modèles : dnb 174, dubstep 140, house 125 ; 2) techno : proposer
  125 et 150 ; 3) caler une boucle importée sur le tempo de son nom.

## Voix
- Définitif : la durée seulement, 4,7 s sur 1 467 sons (11 packs), portée surtout par un pack :
  r7p2 pèse 83 % (1 221 sons) ; sans lui 6,2 s.
- Tombent (un seul pack) : volume moyen −19,3 dB (−16,1 sans r7p2) et crête −3 dB (−0,1 sans r7p2) :
  la « crête −3 dB » est celle de r7p2 seul, ce n'est pas une cible. Voix dnb : il ne reste que la crête
  (portée par un pack, 83 % ; −0,1 dB, sans lui −0,2) ; voix rave : plus aucune cible ; house non plus.
- Idées : 1) les voix sont plus douces que les boucles (−19,3 dB contre −14,1 mélodiques et −17,3 batterie),
  mais ce volume n'est pas une cible : réglage actuel gardé ; 2) « adlib » et « sung » au repérage (`tout.py`).

## Le déplacement des 622 voix de r7p2
- 622 sons de r7p2 rangés hors des voix (605 boucles mélodiques, 17 coups « - ») passent en « voix » (`DOUTES.md`, B1).
- Boucles mélodiques : 1 587 → 982 sons, 7,2 → 7,7 s, −16,1 → −14,1 dB. Coups « - » : 105 → 88 sons, 3,2 → 1 s
  (durée plus cible), −14,4 → −13,2 dB. Voix : 845 → 1 467 sons, 4,8 → 4,7 s, −20,5 → −19,3 dB
  (sans ce déplacement, r7p2 pèserait 71 % des voix au lieu de 83 %).

## Effets
- Définitif : 772 sons, 17 packs (plus gros 33 %) ; durée 7,7 s (moitié entre 3,4 et 11 s) ; volume −17,1 dB.
  Par style : dnb (88, 11 s, −17,2 dB), dubstep (109, 3,4 s, −14,6 dB), techno (40, 6,4 s, −21,1 dB),
  rave (45, durée 12,4 s seulement, portée surtout par un pack : 89 %, sans lui 7,7 s).
- Tombent (un seul pack) : rave volume (−16,1 → −27,2 dB sans le gros pack) et crête (−0,1 → −7,1 dB).
- Idée : effets longs (montées de 8 s) sans coupure ni boucle forcée.

## MIDI
- Pas de cible (non contre-mesuré). Indication : 1 817 fichiers, 19 packs, 4 notes par mesure, étendue 10 demi-tons,
  8 mesures. Idée : une contre-mesure MIDI avant toute cible.

## Doutes
- B1 (les 622 voix de r7p2) changeait des cibles : levé par preuve ; les autres doutes ouverts n'en touchent aucune.
- Les lignes « portées surtout par un pack » passent les cinq conditions, mais un nouveau pack dans leur style
  peut les faire bouger : à revoir à chaque ajout de pack.
