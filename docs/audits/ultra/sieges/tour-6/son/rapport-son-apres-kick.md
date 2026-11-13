# Tour 6, siège son : le kick au téléphone, avant et après le lot « kick »

- **Version jugée :** tête `845c8beb` de l'intégration, qui contient le lot « kick qui tape au haut-parleur du téléphone ». L'« avant » est le siège son du `1585bb46` (`rapport-son.md`).
- **Fichiers :** tableau par style `kick-avant-apres.tsv` ; écoute en direct `kick-direct.tsv` ; décisions dans `docs/decisions/tour-6-son.md` (partie « Après le lot kick »).

## En bref (à lire à voix haute)

1. Le lot kick marche : au téléphone, le coup de kick passe de 1,8 à 5,3 dB en médiane. Les styles sous les 3 dB visés passent de 28 à 3, et plus aucun n'est sous 2 dB.
2. Sur un téléphone de moins bonne qualité, le gain reste net : 12 styles sous 3 dB au lieu de 26. Phonk, jungle, drum and bass et dubstep y restent sous 2 dB.
3. Rien d'autre ne bouge dans les fichiers exportés : les 37 exports sont identiques à l'octet, car le traitement ne joue qu'au haut-parleur.
4. Ce que ça coûte au haut-parleur :
   - un peu plus de bas-médium, +2,2 dB en médiane et jusqu'à +6 en Detroit ;
   - la basse et le lead un demi-décibel plus en retrait (médiane), au plus 2,7 dB ;
   - un niveau et une crête qui ne bougent presque pas.
5. Ce que je conseille : reprendre phonk, jungle et gabber en premier, puis drum and bass et dubstep pour les téléphones plus faibles. Le détail chiffré est plus bas.

## Comment c'est mesuré

- **Le principe du lot :** il n'agit que quand l'appli dit au moteur que le son sort du haut-parleur du téléphone (commande 210). Les mêmes morceaux qu'avant ont donc été rendus deux fois avec les outils de la nouvelle tête, haut-parleur éteint puis allumé (`scripts/son/haut_parleur.sh`).
  - Haut-parleur éteint, chaque rendu est identique à l'octet au rendu d'avant. C'est vrai pour les 37, ce qui prouve que l'export n'a pas changé.
  - Haut-parleur allumé : c'est l'« après ».
- **Les mesures :** les mêmes outils et les mêmes deux haut-parleurs que le siège son (`son6.py`, `son6_kick.py`).
  - Le téléphone coupe sous 300 Hz, le pire cas sous 500 Hz.
  - Le coup de kick est le même calcul que le banc : de combien un coup soulève le morceau, sur la partie principale (Drop, sinon Refrain).
- **Les coups de kick :** ils se lisent sur le kick seul, sans le traitement du haut-parleur, avant comme après.
  - Lus sur le kick traité, ils doublaient en techno, trap et deep house, car ses harmoniques font des « coups » en plus.
  - Mon premier passage, lu ainsi, montrait un faux recul de cinq styles. Je l'ai écarté.
- **L'écoute en direct** (`ps_live`, 150 s, le moteur comme sur le téléphone) a porté sur 6 styles : phonk, dubstep, trance, techno, drum and bass et house, haut-parleur éteint puis allumé. Le moteur n'a signalé aucun fondu, silence ou saut, ni éteint ni allumé.

## Le kick, avant → après (coup de kick sur la partie principale, en dB)

| Haut-parleur | Médiane | Sous 3 dB (visé) | Sous 2 dB (minimum) |
|---|---|---|---|
| Téléphone | 1,8 → 5,3 | 28 → 3 styles | 23 → 0 |
| Pire cas | 1,0 → 3,9 | 26 → 12 styles | 24 → 4 |
| Pleine bande, pour info : on ne l'entend pas au haut-parleur | 3,9 → 4,4 | 7 → 2 | 0 → 1 |

**Ceux qui restent sous 3 dB au téléphone :**

| Style | Partie | Téléphone | Pire cas |
|---|---|---|---|
| phonk | Refrain | −0,8 → 2,2 | −1,1 → 1,3 |
| jungle | Drop | 0,2 → 2,3 | −1,2 → 1,0 |
| gabber | Drop | 2,8 → 2,8 | 3,1 → 3,5 |

**Sous 2 dB sur le pire cas :**

| Style | Pire cas | Téléphone |
|---|---|---|
| phonk | 1,3 | 2,2 |
| jungle | 1,0 | 2,3 |
| drum and bass | 1,6 | 3,2 |
| dubstep | 1,9 | 3,5 |

**Les plus gros gains au téléphone :**

| Style | Avant → après |
|---|---|
| deep house | 1,2 → 6,4 |
| minimal | 1,1 → 6,0 |
| trap | 1,4 → 6,0 |
| drill | 1,8 → 6,6 |
| afro house | 5,5 → 10,2 |
| lo-fi | 4,2 → 10,1 |

**Le seul recul est l'acidcore**, de 6,5 → 4,7 au téléphone et 6,3 → 5,4 sur le pire cas. Il reste bien au-dessus du seuil.

**En direct, sur toute l'écoute** (plusieurs parties mêlées, à ne pas comparer un à un avec le tableau au-dessus), haut-parleur éteint → allumé :

| Style | Téléphone | Pire cas |
|---|---|---|
| phonk | −0,2 → 3,1 | −0,5 → 2,1 |
| dubstep | 2,2 → 7,1 | |
| trance | 3,6 → 7,9 | |
| techno | 3,0 → 8,4 | |
| drum and bass | 2,5 → 6,6 | 1,3 → 4,7 |
| house | 5,4 → 9,6 | |

En direct comme à l'export, le phonk reste le plus faible.

## Ce que le lot coûte ailleurs (au haut-parleur seulement)

- **Niveau :** +0,1 LU en médiane sur le morceau (de −0,2 à +0,6, le plus en jungle). Le Drop ne bouge pas : de −0,2 à +0,4.
- **Crête :** toujours sous −1 dBTP (au plus −1,16), 0 échantillon écrêté, 0 silence, 0 saut.
- **Bas-médium (250-800 Hz, part du morceau) :** +2,2 dB en médiane.
  - Ce sont les harmoniques ajoutées au kick.
  - C'est le plus en Detroit (+6,1), acid techno (+5,8), acid house (+5,6) et acidcore (+4,0).
  - À l'oreille, ça peut épaissir ou « boucher » un peu le son : c'est à vérifier sur ces quatre styles.
  - Le médium (800 Hz-2,5 kHz) ne monte que de +0,4 dB.
- **Basse et lead face au morceau, au téléphone :** −0,6 dB en médiane (de −2,7 à +0,6).
  - Le kick prend de la place, donc le reste paraît un peu moins fort.
  - Le plus touché est l'afro house (−2,7), puis boom bap, tech house, house et deep house (environ −1,6 à −1,8).
  - Le kick face au morceau gagne lui +8,9 dB en médiane.
- **Temps de calcul :**
  - Le rendu hors ligne dure autant avec ou sans haut-parleur (rapport médian 1,01, de 0,82 à 1,16). C'est une mesure grossière : quatre rendus en même temps.
  - La charge du fil audio en direct n'a pas été mesurée ici.

## Ce que je conseille (chiffré, sans le coder)

1. **Phonk et jungle d'abord** : il leur manque 0,8 et 0,7 dB pour 3 dB au téléphone, et 1,7 et 2,0 dB pour 3 dB sur le pire cas.
   - Ce sont les deux styles où l'export lui-même donne déjà le coup le plus faible (2,3 et 2,1 dB en pleine bande, contre 3,9 de médiane) : leur kick est noyé avant même le haut-parleur.
   - Je conseille de relever le kick lui-même dans ces deux recettes, en volume ou en attaque, d'environ +2 dB, plutôt que de pousser le traitement du haut-parleur pour tous.
   - À vérifier ensuite par cette même mesure : coup au téléphone d'au moins 3 dB, sans que le morceau perde plus de 0,5 LU.
2. **Gabber** : 2,8 dB, aucun gain du lot (2,8 → 2,8).
   - Son kick est déjà saturé, donc riche en harmoniques : celles du lot n'ajoutent rien.
   - Il manque 0,2 dB : un clic un peu plus fort (bande 1,2-4,5 kHz, environ +2 dB) devrait suffire.
3. **Drum and bass et dubstep sur les téléphones faibles** (pire cas 1,6 et 1,9 dB) : il manque 1,4 et 1,1 dB.
   - Même conseil que pour le gabber, un clic plus marqué sur ces deux styles.
   - Leur bas-médium ne monte que de +1,1 et +0,7 dB : il reste de la place, mais le clic touche moins au grave que des harmoniques en plus.
4. **Detroit, acid techno, acid house, acidcore** : leur bas-médium monte de 4 à 6 dB au haut-parleur.
   - Je conseille de réduire les harmoniques pour ces quatre-là d'environ un tiers : leur kick reste au-dessus de 5 dB au téléphone, il y a de la marge.
   - À confirmer à l'oreille sur ton téléphone, car la mesure ne dit pas si ça « bouche ».

## Ce qui n'a pas pu être fait ici

- La vraie courbe de ton téléphone : « Écoute au micro » n'a pas encore tourné. Les haut-parleurs sont ceux que proposent les sources.
- La charge du fil audio en direct avec le haut-parleur allumé.
- L'écoute en direct ne couvre que 6 styles ; les 31 autres sont jugés sur le rendu hors ligne, fait par le même moteur avec les mêmes commandes.
