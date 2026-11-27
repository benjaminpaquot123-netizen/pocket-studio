# Conclusion : les sons hors coups de batterie (2026-11-27, v2 après critique)

Chiffres : `cibles-autres-definitives.tsv` (481 lignes), produit par `scripts/sons/fin/cibles_autres.py`
(self-test : `--selftest`). Packs écrits rNpN. Rien n'est appliqué dans l'appli : ce sont des idées classées.

## La règle (durcie après la critique à l'aveugle)
Une ligne est « définitive » seulement si les quatre conditions tiennent :
1. au moins 20 sons ;
2. au moins 2 packs qui ont chacun au moins 5 sons (colonne `packs_5`) ;
3. aucun pack ne pèse plus de 90 % des sons (colonne `part_pack_max`, en %) ;
4. la contre-mesure à l'aveugle retrouve la valeur au moins 98 fois sur 100 sur ces sons.

Sinon : « pas de cible, on garde le son actuel », avec la raison dans la colonne `raison`.
Une ligne définitive dont le plus gros pack pèse plus de 75 % est marquée « portée surtout par un pack » :
elle tient, mais un seul fabricant donne le ton.

Le tempo n'a pas de contre-mesure : il n'est jamais « définitif ». Il a son propre statut,
« tempo du fabricant sûr » : le tempo écrit par le fabricant, confirmé par notre estimation à ±2 %.
Chaque pack d'au moins 5 boucles confirmées vote avec sa médiane ; c'est sûr seulement si au moins
2 packs votent et tombent tous à ±2 % les uns des autres.

Bilan : 99 lignes définitives (124 avant), 3 tempos du fabricant sûrs, 379 lignes sans cible.

## Ce qui ne sert jamais de cible
- La tonalité estimée (21 % exacte sur 826 boucles), la note des sons mélodiques (81 % d'accord),
  la hauteur des voix, les enveloppes : jamais contre-mesurées ou pas en accord.
- Le MIDI : jamais contre-mesuré (chiffres à titre indicatif).

## Quels styles gardent une cible
- Avec au moins une cible : dnb, dubstep, techno (basse, boucles de batterie et mélodiques, effets ;
  plus lead en dubstep, synth en techno, voix en dnb), house (basse, synth, boucles de batterie),
  rave (effets, voix), lofi (keys seulement), trap (basse seulement).
- Sans aucune cible : afro-house, amapiano, ambient, drift-phonk, edm, hardstyle, hiphop, multi,
  rnb, synthwave, trance. Raison : moins de 2 packs de 5 sons dans le style.
- L'ancienne phrase « tout style d'un seul pack : pas de cible » était fausse (lofi, trap et rave
  avaient des cibles). Elle est remplacée par la condition 2 ci-dessus, appliquée à chaque ligne.

## Basse (coups mélodiques « bass »)
- Sûr : 749 sons, 16 packs (plus gros pack 38 %). Durée médiane 0,86 s (moitié entre 0,86 et 1,7 s) ;
  volume moyen −6,8 dB ; crête −0,1 dB.
- Par style : dnb (35 sons, −11,9 dB), dubstep (483, −6,3 dB), house (48, −13,2 dB), techno (30, −13 dB),
  trap (27 sons, 7,4 s, −10,7 dB, portée surtout par un pack : 81 %).
- Idées : 1) volume par défaut des basses près de −7 dB RMS ; 2) notes de basse d'usine d'environ 1 s.

## Coups mélodiques par sous-type
- Sûr (tous styles) : synth (140 sons, −17,2 dB, 2 s), lead (139, −16,5 dB, 3,2 s), keys (98, −20,6 dB,
  6,9 s), chords (76, −16,9 dB, 6,4 s), pad (61, −16,9 dB, 13,7 s), sans sous-type « - » (88, −13,2 dB,
  1 s, portée surtout par un pack : 82 %).
- Par style : lead dubstep (40), synth house (25, portée surtout par un pack : 80 %), synth techno (30),
  keys lofi (26, 12,9 s, −22,4 dB).
- Plus de cible : strings (106 sons, un pack pèse 93 %), arp (20 sons, un seul pack de 5 sons),
  keys trap (un seul pack de 5 sons) ; bell, pluck, guitar, brass, perc, drums, full (moins de 20 sons).
- Idées : 1) pads longs (11 à 15 s) : queue longue sans couper ; 2) keys 3,4 dB plus doux que synth
  (−20,6 contre −17,2) et 4,1 dB plus doux que lead (−16,5) : volume par défaut par sous-type ;
  3) crête d'usine à −0,1 dB : garder de la marge dans l'appli.

## Boucles mélodiques
- Sûr : 982 boucles, 13 packs (plus gros 15 %) ; durée 7,7 s ; volume moyen −14 dB.
  Par style : dnb (98), dubstep (150, −7,3 dB), techno (179).
- Plus de cible : house (un pack pèse 99 %) et lofi (95 %).
- Idées : 1) lire tempo et tonalité dans le nom plutôt que les deviner ; 2) aligner les boucles
  importées vers −14 dB RMS.

## Boucles de batterie
- Sûr : 1 858 boucles, 13 packs (plus gros 25 %) ; durée 6,9 s ; volume moyen −17,3 dB.
  Par style : dnb, dubstep, techno, house (213, portée surtout par un pack : 89 %).
- Tempo du fabricant (« sons » = boucles étiquetées ; accord compté sur celles qui ont une estimation) :
  - dnb sûr : 174 (votes r10p1 174, r11p2 174, r1p1 172) ; 221 boucles, accord 217 sur 221 ;
  - dubstep sûr : 140 (r1p1, r8p1, r9p3 à 140) ; 164 boucles, accord 161 sur 164 ;
  - house sûr : 125 (r12p2 125, r1p1 124) ; 213 boucles, dont 209 avec estimation, accord 209 sur 209 ;
  - techno : pas de tempo unique, 125 ou 150 selon le pack (r3p1 125 ; r1p1 et r5p1 150) ;
  - un seul pack qui vote, donc pas sûr : afro-house 120 (accord 57 % seulement), amapiano 113,
    drift-phonk 120, hiphop 85, lofi 77, multi 128, rave 155, trap 95 ;
  - « tous » : mélange de styles, jamais une cible.
- Idées : 1) tempo par défaut des modèles : dnb 174, dubstep 140, house 125 ; 2) techno : proposer
  125 et 150 ; 3) caler une boucle importée sur le tempo de son nom.

## Voix
- Sûr : 1 467 sons, 11 packs ; durée 4,7 s ; volume moyen −19,3 dB ; crête −3 dB. Portée surtout
  par un pack : r7p2 pèse 83 % (1 221 sons).
- Par style : dnb (36, portée surtout par un pack : 83 %), rave (50, 80 %). House n'a plus de cible
  (un seul pack de 5 sons).
- Idées : 1) voix un peu plus fortes par défaut que les boucles ; 2) ajouter « adlib » et « sung »
  au repérage des voix (`tout.py`, non modifié).

## Le déplacement des 622 voix de r7p2
- 622 sons de r7p2 étaient rangés hors des voix : 605 en boucles mélodiques, 17 en coups mélodiques
  sans sous-type « - ». Ils sont déplacés vers « voix » (preuve : `DOUTES.md`, B1).
- Boucles mélodiques (tous) : 1 587 → 982 sons ; durée 7,2 → 7,7 s ; volume −16,1 → −14,1 dB.
- Coups « - » (tous) : 105 → 88 sons ; durée 3,2 → 1 s ; volume −14,4 → −13,2 dB.
- Voix (tous) : 845 → 1 467 sons ; durée 4,8 → 4,7 s ; volume −20,5 → −19,3 dB ; crête −3 dB
  dans les deux cas. Sans ce déplacement, r7p2 pèserait 71 % des voix au lieu de 83 %.

## Effets
- Sûr : 772 sons, 17 packs (plus gros 33 %) ; durée 7,7 s (moitié entre 3,4 et 11 s) ; volume −17,1 dB.
  Par style : dnb, dubstep, techno, rave (45, portée surtout par un pack : 89 %). House n'a plus de cible.
- Idée : effets longs (montées de 8 s) sans coupure ni boucle forcée.

## MIDI
- Pas de cible (non contre-mesuré). Indication : 1 817 fichiers, 19 packs, 4 notes par mesure,
  étendue de 10 demi-tons, 8 mesures. Idée : une contre-mesure MIDI avant toute cible.

## Doutes
- Un doute changeait des cibles : B1, le rangement des 622 voix de r7p2. Il touche les lignes voix,
  boucles mélodiques et coups « - ». Il est levé par preuve (`DOUTES.md`) ; les chiffres ci-dessus
  sont ceux d'après le déplacement.
- Les doutes qui restent ouverts ne touchent aucune cible de ce lot (batterie, note, hauteur : voir
  `DOUTES.md`, une raison par ligne).
- Les lignes « portées surtout par un pack » sont sûres au sens de la règle, mais à revoir si un
  nouveau pack arrive dans leur style.
