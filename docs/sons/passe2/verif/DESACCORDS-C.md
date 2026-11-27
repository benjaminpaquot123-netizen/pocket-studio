# Désaccords de la contre-mesure C (RAR 9, 10, 11)

Outil indépendant : `scripts/sons/passe2/verif/` (appelé « mesure.py » ci-dessous).
Tables comparées : `docs/sons/passe2/tout/` (appelées « la table »).
Détail son par son : `rar9.csv`, `rar10.csv`, `rar11.csv` et leurs résumés.
Les sons sont désignés par leur clé de table (`rN-pM-xxxxxxxxxx`), jamais par leur nom.

## En bref

| colonne | sons comparés | d'accord |
|---|---|---|
| durée, crête, RMS | 3 524 | 100 % |
| t20, t60, centre de gravité, aigus, attaque/corps | 637 coups | 100 % |
| part tonale (`tonal_db`) | 631 coups | 96,5 % |
| hauteur stable (`pitch_steady_hz`) | 309 coups tonals | 61,8 % |
| hauteur d'attaque (`pitch_attack_hz`) | 309 coups tonals | 43,7 % |
| note des one-shots mélodiques (`f0_hz`) | 911 | 70,6 % |

Les deux hauteurs ne se comparent que pour les coups assez tonals (part tonale ≥ -10 dB) ; les 328 autres sont « nc ».

Bilan : les colonnes de niveau et de durée sont justes des deux côtés. Sous 98 %, c'est le plus souvent
**mesure.py qui se trompe** (part tonale, hauteur d'attaque, octaves de la note, hauteur des kicks à queue courte).
La table se trompe sur un cas net : les accords et certains sons sans fondamentale, où elle donne une note
beaucoup trop grave.

## 1. Part tonale des coups (`tonal_db`) : 22 sur 631 en désaccord

- Où : 22 coups (RAR 9 : 4, RAR 10 : 9, RAR 11 : 9). Exemples : `r9-p3-2570207b76` (shaker),
  `r11-p2-23b5b04b45` (charleston fermée).
- Qui se trompe : **mesure.py**.
  - Cause principale : mesure.py compte, autour de la hauteur, l'énergie sous 20 Hz et le continu, alors que son total
    s'arrête à 20 Hz. D'où des valeurs trop hautes, parfois même au-dessus de 0 dB, ce qui est impossible
    (`r11-p2-7758ea4f40` : table -0,04, mesure.py +1,52).
  - Preuve à la main sur 3 sons, avec la même bande des deux côtés : `r9-p3-2570207b76` table -38,02,
    mesure.py -26,78, corrigé -38,02 ; `r11-p2-23b5b04b45` -23,52 / -1,06 / corrigé -23,52 ;
    `r11-p3-018419398e` -42,87 / -23,36 / corrigé -42,87. Avec la bande corrigée, 18 valeurs sur 22 tombent
    exactement sur la table.
  - Les 4 restants : la règle du mono diffère. La table garde le milieu (G+D) sauf s'il est à plus de 3 dB
    sous la **moyenne** des canaux ; mesure.py compare au canal **le plus fort**. Exemples : `r10-p1-3bdd1512b0`
    (milieu -1,51 dB sous la moyenne, -3,58 dB sous le plus fort), `r11-p2-83bf889fdd`. Ici ce n'est pas une
    erreur de la table, juste une autre règle ; celle de la table est celle de l'outil officiel.

## 2. Hauteur d'attaque (`pitch_attack_hz`) : 174 sur 309 en désaccord

- Où : surtout les kicks (110 sur 117 en désaccord, dont 29 « vide d'un côté »), puis perc 31, snare 19,
  clap 11. Exemples : `r9-p3-0093989357`, `r9-p3-05fc42c437`.
- Qui se trompe : **mesure.py**. Sa fenêtre d'attaque dure une période de la hauteur **stable** : 20 ms pour
  un kick à 50 Hz. Elle déborde dans la descente, donc la valeur est trop basse.
- Preuve par signal connu (kick synthétique, vraie hauteur moyenne sur 2–12 ms) :

  | kick synthétique | vérité | table | mesure.py |
  |---|---|---|---|
  | 150 → 50 Hz, 20 ms | 121,2 | 117,6 | 105,5 |
  | 300 → 50 Hz, 10 ms | 179,4 | 181,8 | 139,8 |
  | 400 → 55 Hz, 8 ms | 208,4 | 208,6 | 161,5 |
  | 180 → 42 Hz, 12 ms | 121,3 | 117,9 | 91,5 |

  La table reste à moins de 60 cents de la vérité ; mesure.py est 150 à 470 cents trop bas.
  Le self-test de mesure.py se trompe de la même façon (sa vérité « kick qui descend » prend aussi 2–22 ms).
- Les 29 « vide d'un côté » sont les kicks à queue courte du point 3.

## 3. Hauteur stable (`pitch_steady_hz`) : 118 sur 309 en désaccord

- Kicks (62 : 33 en désaccord, 29 « vide d'un côté »). Exemples : `r9-p3-152c75ca7a`, `r11-p2-68910b5522`.
  - Qui se trompe : **mesure.py**, quand la queue est courte. Il compte les passages par zéro entre -10 et
    -40 dB ; un kick court n'en a que 1 à 3 sur 55–75 ms, et il en exige 4 : il rend vide.
  - Preuve par signal connu : sinus constant de 50 Hz qui s'éteint en 150 ms : table 50,02, mesure.py 50,02 ;
    même sinus éteint en 25 ms : table 50,06, mesure.py 46,64 (-120 cents) ; 45 Hz éteint en 18 ms :
    table 44,5, mesure.py 41,28 (-150 cents). Même écart que sur les vrais kicks (-110 à -200 cents).
  - Preuve à la main sur 3 vrais kicks (période des passages par zéro entre -10 et -20 dB) :
    `r11-p2-68910b5522` main 39,0, table 39,05, mesure.py 34,89 ; `r11-p2-9569e9d3b9` 51,2 / 51,96 / 48,74 ;
    `r9-p3-152c75ca7a` 43,3 / 43,68 / vide.
  - Exception : `r10-p1-582ac7a8de` (main 46,4, table 50,8, mesure.py 46,47) : là, c'est la table qui est haute.
- Snares, claps, percs (56 : clap 18, perc 21, snare 15, plus 2 isolés). Exemples : `r9-p3-563ad1ca07`,
  `r10-p1-083b725735`.
  - Qui se trompe : **personne de façon prouvée**. Ces sons sont surtout du bruit ; à la main, la « hauteur »
    saute de 200 à 600 cents d'une tranche de 10 dB à l'autre (`r9-p3-080a9ef455` : 1 334, 1 955, 1 049,
    2 019 Hz). Sur un bruit synthétique centré sur 1 500 Hz, les deux outils donnent environ 1 530 Hz : pas de
    biais d'un côté. La valeur n'a simplement pas de sens précis pour un clap.

## 4. Note des one-shots mélodiques (`f0_hz`) : 268 sur 911 en désaccord

- Octaves (107, toujours mesure.py une octave au-dessus). Exemples : `r9-p1-225fd48b51`, `r11-p1-0609003281`.
  - Qui se trompe : **mesure.py**. Il prend le pic le plus fort ; quand la 2ᵉ harmonique domine, il donne
    l'octave.
  - Preuve par signal connu : 110 Hz avec fondamentale faible : table 110,0, mesure.py 220,0.
  - Preuve à la main sur 3 sons (niveau des harmoniques de la note de la table) : `r9-p1-2f00ea0109`
    h1 -2,4 dB, h3 -12,6 dB ; `r11-p1-0609003281` h1 -9,2, h3 -25,2 ; `r11-p1-07fe4b5647` h1 -5,6, h3 -5,4.
    La 3ᵉ harmonique existe : la note de la table est bien la vraie, l'octave de mesure.py est fausse.
- Table très grave, sous 45 Hz (46). Exemples : `r9-p2-b2be9222fc`, `r10-p1-0209fa37da`.
  - Qui se trompe : **les deux selon le son**. Sur un accord do-mi-sol synthétique, la table donne 32,8 Hz
    (faux, elle cherche une période commune) et mesure.py 130,8 Hz (juste). Mais à la main :
    `r11-p1-2180e4d09e` n'a rien à 49 Hz ni à ses 5 premières harmoniques (table fausse), alors que
    `r10-p1-0209fa37da` et `r10-p1-15658e50b9` ont bien 44 Hz et des harmoniques impaires (table juste,
    mesure.py prend la 3ᵉ).
- Autres désaccords (73). Exemples : `r9-p2-09944b7604`, `r10-p1-083ef2a45e`. Souvent une harmonique 3 ou plus
  (524 contre 1 575 Hz) ou une note qui glisse : sur un glissé 220 → 110 Hz, table 156,6, mesure.py 172,5,
  les deux mesurent sur des durées différentes.
- Vide d'un côté (42, toujours vide dans la table). Exemples : `r9-p3-0387bb0d73`, `r11-p2-bfe82b2740`. La table
  ne trouve pas assez de tranches avec une note nette (bruit, accord) ; mesure.py rend toujours son pic le
  plus fort. Pas d'erreur prouvée.

## 5. Étiquettes contredites par la mesure

Aucun son n'est étiqueté « kick 808 » dans les tables : la paire kick/808 ne peut se contredire que par la mesure.

| RAR | pack | kick ↔ 808 | snare ↔ clap | charleston fermée ↔ ouverte | autre |
|---|---|---|---|---|---|
| 9 | p3 | 0 | 0 | 1 : `r9-p3-3d8b0b8352` étiqueté ouvert, queue de 145 ms | 0 |
| 10 | p1 | 1 : `r10-p1-5dda95bbf5` étiqueté charleston fermée, mesuré comme un kick grave (centre 84,5 Hz, hauteur 53,8 Hz) | 1 douteux : `r10-p1-083b725735` (le chemin dit snare, la mesure ne tranche pas) | 0 | 0 |
| 11 | p2 | 0 | 0 | 0 (3 « ouvertes » que le chemin dit fermées : la mesure confirme ouvertes) | 0 |
| 11 | p3 | 2 candidats 808 : `r11-p3-15c1a62557`, `r11-p3-91badefa10` (kick, queue de 1,3 s, hauteur ≈ 49 Hz) | 0 | 0 | 0 |

Snare contre clap : les colonnes mesurées ne les séparent pas de façon sûre ; seul le cas ci-dessus est signalé.

## 6. Sons « 808 » par pack

- Étiquetés 808 dans les tables : 0 partout.
- Chemin qui dit « 808 » : RAR 10 p1 : 1 (une boucle mélodique, pas un coup). Ailleurs : 0.
- Kicks qui sonnent 808 à la mesure (queue > 600 ms et hauteur < 70 Hz) : RAR 10 p1 : 1, RAR 11 p3 : 2,
  RAR 9 : 0.

## 7. Fichiers sans ligne et lignes sans fichier

- RAR 9 et RAR 10 : 0 et 0.
- RAR 11 : 0 fichier sans ligne, 2 lignes sans fichier dans p3 (`r11-p3-7d27ef27f9`, `r11-p3-a3031c9637`).
  Ce sont les 2 sons d'une archive cachée dans p3 : leur clé est calculée depuis le dossier où l'archive avait
  été ouverte. Ouverte au même endroit, les 2 sons sont là ; mesurés à part (durée, crête, RMS d'accord), ajoutés
  en fin de `rar11.csv`, hors pourcentages. L'archive cachée de p2 ne contient que des réglages de synthé.

## Ce qui reste douteux

- La hauteur des snares, claps et percs bruités : pas de vraie note, aucun outil n'est fiable ; mieux vaut ne
  pas s'en servir pour accorder.
- La note des one-shots qui sont des accords ou qui glissent : « une note » n'existe pas ; la table descend
  parfois sous 45 Hz à tort (au moins `r11-p1-2180e4d09e`).
- Les 42 one-shots sans note dans la table : peut-être des accords ou du bruit, non vérifiés un par un.
- Le kick `r10-p1-582ac7a8de` (table 50,8 Hz, main 46,4 Hz) et le kick qui glisse `r9-p3-d439f2d798`
  (45 → 38 Hz) : la « hauteur stable » dépend de l'endroit mesuré.
- L'étiquette du son `r10-p1-5dda95bbf5` (rangé en charleston, sonne comme un kick) et de `r10-p1-083b725735`
  (clap ou snare) : à réécouter.
- Le self-test de mesure.py a la même erreur que lui sur la hauteur d'attaque : à corriger avant de s'en servir
  comme juge.
