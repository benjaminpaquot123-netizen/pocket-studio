# Critique à l'aveugle : cibles hors coups de batterie

Base : `eq/son-fin-autres` (f919dc7d). Mon chemin : `scripts/sons/fin/critique_autres.py`
(rien d'importé de `cibles_autres.py`). Rejouer : `python3 -I scripts/sons/fin/critique_autres.py --out x.tsv`
(`--selftest` : vert). Écarts : `critique-autres-ecarts.tsv`. Lignes = numéro de ligne du tsv.

## Grave

1. Tempo des boucles de batterie (lignes 308, 316, 324, 340, « définitives ») : la règle annoncée
   (accord ≥ 98 % de la contre-mesure) n'est pas appliquée : `verif/rar*.csv` n'a aucune colonne bpm.
   Le « 98-99 % » est l'accord étiquette / estimation du même outil, pas une contre-mesure.
   Rejouer : `head -1 docs/sons/passe2/verif/rar1.csv | tr , '\n' | grep -c bpm` → 0.
2. Tempo techno 150 (ligne 340) et house 125 (ligne 324) : chaque pack a un seul tempo.
   Techno : r3p1 95 sons tous à 125, r5p1 225 sons tous à 150, r1p1 25 sons (150/140).
   House : r12p2 189 sons tous à 125, r1p1 24 sons (120-128). La médiane n'est que le poids du plus
   gros pack ; les « 345 » et « 213 sons » valent 3 et 2 avis. Annoncé : techno 150, house 125
   définitifs ; moi : non définitifs (techno partagé 125/150). dnb 174 et dubstep 140 tiennent (3 packs d'accord).
3. « 2 packs » sans second pack réel (lignes définitives, sons hors pack principal) :
   284-286 boucle mélodique house 77 + 1 ; 287-289 boucle mélodique lofi 63 + 3 ;
   378 voix house 18 + 3 ; 420 effet house 20 + 3 ; 155 keys trap 16 + 4 ; 251 synth house 20 + 5 ;
   38 basse trap 22 + 5 ; 426 effet rave 40 + 5 ; 230-232 strings tous 99 sur 106 (r11p1, 93 %).
   La règle (≥ 2 packs) est tenue à la lettre, pas dans l'esprit. Je propose : au moins 2 packs avec
   ≥ 5 sons chacun, ou aucun pack > 90 %. Rejouer : colonne `part_pack_max` du tsv d'écarts.
4. Contradiction (question a) : la conclusion dit « tout style d'un seul pack (… lofi, trap, rave …) :
   pas de cible », mais lofi (boucle mélodique 287-289, keys 152-154), trap (basse 38, keys 155) et
   rave (voix 383, effet 426) ont des lignes définitives. Une des deux phrases est fausse ; ces lignes
   sont justement celles du point 3.
5. Les 622 sons passés en voix (question c) : 605 venaient de boucle mélodique et 17 de coup
   mélodique « - » (pas 622 de boucle mélodique). Effet > 5 % sur deux autres familles :
   boucle mélodique tous, médiane durée 7,2 → 7,68 s (+6,7 %), RMS −16,1 → −14,05 dB ;
   coup mélodique « - » tous (ligne 41, définitive) durée 3,2 → 1,02 s (−68 %), RMS −14,4 → −13,2.
   Les chiffres du tsv sont bien ceux d'après le déplacement, mais la conclusion n'en dit rien.
6. « Aucun doute qui change une cible » (conclusion) : faux au vu des points 2 et 3.

## À corriger sans gravité

- Tempo, colonne « sons » : le tsv compte les sons gardés par le filtre ±2 %, pas tous les sons
  étiquetés (dnb 217/221, dubstep 161/164, house 209/213, techno 341/345). Les quantiles ne bougent
  pas : le filtre ne biaise pas ces 4 lignes (question b).
- House tempo : « accord 98 % » = 209/213 car les 4 sons sans estimation comptent en désaccord ; 209/209 sinon.
- Hors cible (non définitives, sans effet) : 300 afro-house p75 122 → 120 sans filtre ; 312 drift-phonk
  médiane 120 → 125 sans filtre ; 296 et 332 : n seul.
- « keys et arp 4 à 7 dB plus doux que synth et lead » : keys −20,6 contre synth −17,2 = 3,4 dB.
- Autres « 2 packs » minces (< 20 sons hors pack principal) : 8, 35, 41, 53, 152, 260, 363, 383 ;
  voix tous (348) : 83 % de r7p2.

## Confirmé

- Statuts : 481 sur 481 identiques (124 définitives des deux côtés).
- Chiffres : 120 lignes définitives sur 124 identiques à 0,5 % près (n, packs, p10 à p90, accord) ;
  les 4 autres (tempo) ne diffèrent que par n. Hors tempo : toutes les 481 lignes identiques.
- Contre-mesure : durée, crête, RMS « 10 441 sur 10 441 » : je retrouve 10 441 « yes » sur chaque colonne.
- Question d, 15 chiffres de la conclusion relevés : basse 0,86 s / −6,8 dB ; lead 3,2 s / −16,5 ;
  strings 0,7 s / −12,5 ; keys 6,9 s / −20,6 ; pad 13,7 s ; synth 140 sons 11 packs ; boucle
  mélodique 7,7 s / −14 ; boucle de batterie 6,9 s / −17 ; voix 4,7 s / −19 / crête −3 ; effets
  7,7 s / −17 ; MIDI 4 notes par mesure, 10 demi-tons, 8 mesures : tous = tsv (arrondis justes).
- Remesure (point 4), graine 20261127, 60 sons tirés parmi oneshot_melodic, melodic_loop, vocal et fx
  de r3 et r12 (34 effets, 23 boucles mélodiques, 3 coups) ; mon décodeur (ffmpeg → float) :
  60 sur 60 d'accord ; écart max durée 0 %, crête 0,0 dB, RMS 0,0 dB (RMS sur tous les canaux).
  Rejouer : `--remesure 3=DOSSIER 12=DOSSIER --out f.tsv` sur les archives extraites hors du dépôt.

## Non vérifié

- Tempo par contre-mesure indépendante : pas de colonne bpm dans `verif/`, pas de détecteur à moi.
- MIDI, étendue de voix et « voiced » : hors contre-mesure ; recalculés depuis les tables, pas remesurés.
- Aucune voix dans le tirage : r3 n'en a pas, r12 n'en a que 28 sur 1 467 (le gros est r7).
- Bonne attribution d'un son à son style ou à son rôle (étiquetage) : hors de ma fiche.
