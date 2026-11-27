# Batterie du matin : conclusion

## Ce qui change à l'oreille, style par style

1. **House** : kick plus rond et un peu plus court, clap et charley fermé plus nets ; la caisse claire, le charley ouvert et les toms restent ceux de l'appli.
2. **Tech house** : kick plus long et plus grave, clap plus sec, charleys plus courts ; caisse claire inchangée.
3. **Techno** : kick plus long et plus dur (il tient presque 0,4 s), clap court, charleys serrés ; caisse claire inchangée.
4. **Trap** : kick court et bas, caisse claire plus longue, charleys très courts ; le clap, le 808 et le rim restent ceux de l'appli.
5. **Drum and bass** : kick sec, caisse claire très courte et claquante ; clap et crash inchangés.
6. **Dubstep** : kick lourd, clap long, caisse claire longue (presque une demi-seconde), charley ouvert court.
7. **Deep house** suit la house, **minimal** et **hard techno** la techno, **drill** la trap, **liquid** la drum and bass, **riddim** le dubstep.
8. Le 808 ne change nulle part : sa cible vient d'un seul pack, on garde le son actuel.
9. Shaker, cowbell et conga ne changent pas : aucune mesure ne les sépare (pas de cible).
10. À volume égal : dans chaque morceau, la batterie « après » est calée sur « avant » (écart sous 1 dB), on juge le son, pas le niveau.

## La règle (écrite avant le calcul, rejouable)

Une seule commande : `scripts/son/matin_choix.sh` (cibles lues dans `cibles-batterie.tsv`,
règle dans `scripts/son/matin_choix.py`). Elle mesure chaque version (nouvelle, A grave et
long, B sec et court, et le kick « deep » en techno), puis :

- mesures fiables : longueur à −20 dB (t20), à −40 dB (t40), et la note pour le kick ;
- écart normalisé d'une mesure : 2 × |valeur − médiane| / (p90 − p10) ; score = moyenne ;
- le plus petit score gagne ; égalité (moins de 0,05) : « nouvelle » ;
- **sûr** si le gagnant reste le même avec la cible au p25, à la médiane et au p75 ; sinon
  **serré**, et on garde « nouvelle » ;
- une cible ne compte qu'avec au moins 20 coups et 2 packs ; sinon « pas de cible, on garde
  le son actuel ».

La commande finit par « table du moteur : identique » quand la table de
`MeasuredDrums.cpp` (`kChoice`) est celle que donne la règle.

## Son actuel → son retenu

Mesures en millisecondes (t20 / t40), note en Hz. Cible : médiane (p10–p90).
« Actuel » = la voix de l'appli (kit v3), qui reste.

### Kick (A, B et deep perdent partout)

| Style | Retenu | Marge | Retenu t20 / t40 / note | Cible t20 | Cible t40 | Cible note |
|---|---|---|---|---|---|---|
| house | nouvelle | serré | 215 / 263 / 41,7 | 219 (174–258) | 268 (223–326) | 42 (37–48) |
| tech house | nouvelle | serré | 248 / 310 / 49,7 | 250 (194–384) | 313 (223–396) | 48 (41–60) |
| techno | nouvelle | serré | 366 / 396 / 49,4 | 373 (205–388) | 394 (240–398) | 51 (44–64) |
| trap | nouvelle | serré | 94 / 121 / 44,5 | 97 (71–125) | 129 (96–156) | 46 (35–53) |
| dnb | nouvelle | serré | 122 / 173 / 50,2 | 129 (100–183) | 178 (153–261) | 52 (43–65) |
| dubstep | nouvelle | serré | 218 / 275 / 44,2 | 226 (190–261) | 284 (233–347) | 48 (40–55) |

**Kick deep contre kick techno** : scores nouvelle 0,09, A 0,34, deep 0,46, B 1,31. Le kick
techno retenu est « nouvelle » (sa recette a été allongée pour tenir la cible : 373 / 394 ms) ;
le deep reste une variante d'écoute.

### Clap, caisse claire, charleys

| Famille | Style | Retenu | Marge | Retenu t20 / t40 | Cible t20 | Cible t40 |
|---|---|---|---|---|---|---|
| clap | house | nouvelle | serré | 77 / 132 | 78 (50–114) | 142 (84–214) |
| clap | tech house | nouvelle | serré | 104 / 160 | 102 (68–166) | 168 (114–228) |
| clap | techno | nouvelle | serré | 124 / 169 | 116 (70–167) | 175 (142–240) |
| clap | trap / dnb | actuel | pas de cible (11 et 18 coups) | – | – | – |
| clap | dubstep | nouvelle | serré | 152 / 276 | 159 (93–289) | 266 (151–463) |
| caisse claire | house / tech house / techno | actuel | pas de cible (10 coups) | – | – | – |
| caisse claire | trap | nouvelle | sûr | 118 / 191 | 123 (80–159) | 196 (127–243) |
| caisse claire | dnb | nouvelle | sûr | 53 / 96 | 57 (42–74) | 100 (76–124) |
| caisse claire | dubstep | nouvelle | serré | 254 / 504 | 248 (104–392) | 546 (294–709) |
| charley fermé | house | nouvelle | serré | 49 / 82 | 54 (33–69) | 85 (47–114) |
| charley fermé | tech house | nouvelle | sûr | 64 / 88 | 68 (42–83) | 92 (59–129) |
| charley fermé | techno | nouvelle | serré | 66 / 91 | 72 (48–92) | 96 (87–147) |
| charley fermé | trap | nouvelle | serré | 42 / 66 | 46 (34–66) | 71 (61–101) |
| charley fermé | dnb | nouvelle | serré | 48 / 86 | 40 (28–92) | 77 (44–176) |
| charley fermé | dubstep | nouvelle | serré | 54 / 86 | 50 (35–58) | 82 (60–98) |

La deuxième caisse claire (pad Snare2) suit la caisse claire : remplacée là où elle l'est.

### Les autres familles

| Famille | Style | Retenu | Retenu t20 / t40 | Cible t20 | Cible t40 |
|---|---|---|---|---|---|
| charley ouvert | house | actuel (10 coups) | – | – | – |
| charley ouvert | tech house / techno / dnb | nouvelle (sûr) | 167 / 304 | 131 (70–911) | 210 (107–1006) |
| charley ouvert | trap | nouvelle (sûr) | 855 / 942 | 865 (551–1495) | 951 (623–1937) |
| charley ouvert | dubstep | nouvelle (sûr) | 109 / 134 | 113 (71–194) | 138 (109–226) |
| tom | house | actuel (10 coups) | – | – | – |
| tom | les cinq autres | nouvelle (sûr) | 254 / 506 | 262 (119–634) | 514 (216–1031) |
| crash | house / tech house / techno | nouvelle (sûr) | 986 / 1348 | 1151 (499–1893) | 1693 (836–2630) |
| crash | trap | nouvelle (sûr) | 548 / 780 | 1151 (499–1893) | 1693 (836–2630) |
| crash | dnb | actuel (6 coups) | – | – | – |
| crash | dubstep | nouvelle (sûr) | 1159 / 1641 | 1213 (1020–1537) | 1693 (1342–2320) |
| rim | tous | actuel (5 coups) | – | – | – |
| 808 | tous | actuel (23 coups, 1 seul pack) | – | – | – |
| shaker, cowbell, conga | tous | actuel (pas de cible : mêlés sous « percussions ») | – | – | – |

## Volume : avant / après dans les morceaux

`scripts/son/matin_morceaux.sh` cale maintenant chaque morceau : un premier passage mesure
l'écart de la piste batterie, le second rejoue avec ce calage (`--measured-trim`). Le garde-fou
passe de 2 dB à 1 dB. Le best-of (`scripts/son/matin_bestof.sh DOSSIER`) donne un MP3 par style
(40 s avant, 1 s de silence, 40 s après) et refuse aussi plus de 1 dB d'écart sur le morceau entier.

Mesuré le 27 novembre (12 styles) : piste batterie avant / après entre −0,3 et +0,3 dB partout,
sauf hard techno +0,9 dB ; best-of entre −0,2 et +0,3 dB. En hard techno, le bus batterie sature
et ne suit qu'à moitié le calage : le best-of remet la moitié « après » à niveau par un simple
gain (−1,7 dB), écrit dans le journal.

## Drill

Le drill joue la batterie trap. Restent comme avant : le 808 (pas de cible), le clap (cible
trap de 11 coups), le rim (5 coups), et deux sons du pack du morceau (une caisse claire et un
effet), qui sont des échantillons, pas des voix du kit : le banc ne les remplace pas. Ce que les
mesures fiables permettent est fait : kick, caisse claire, charleys, toms et crash trap, au même
volume qu'avant grâce au calage par morceau.

## Plan d'entrée dans l'appli (pas fait : rien n'entre dans l'appli dans ce lot)

1. **Fichiers** : `engine/src/main/cpp/core/MeasuredDrums.{h,cpp}` (recettes et table
   `kChoice`), `DrumKit.{h,cpp}` (`setMeasured`), `Engine.cpp` (la commande du banc devient une
   commande de projet, par exemple « kit v4 » dans `setVersion`), côté appli la version du kit
   dans le projet et l'assistant (choix du style mesuré selon le style du morceau).
2. **Réglages moteur** : un style mesuré par style de morceau (table du banc :
   house/deep house → house, techno/minimal/hard techno → techno, trap/drill → trap…), le
   calage de niveau par style (`kMeasuredStyleTrimDb`), plus le calage par morceau mesuré ici
   à figer en table.
3. **Tests qui prouvent** : `matin_measured_chosen_in_range` (chaque version retenue dans sa
   plage), `matin_measured_kit_default_identical` (les anciens projets sonnent pareil au bit
   près), `matin_measured_kit_command` (les voix gardées ne bougent pas), le rejeu de
   `scripts/son/matin_choix.sh` (« table du moteur : identique »), puis l'écoute en direct
   (`scripts/live-bench.sh`) et un test de niveau du mix par style.

## Ce qui n'est pas réglé et pourquoi

- **Nombre de packs** : il n'est écrit dans le dépôt que pour le 808. Ailleurs la règle compte
  les coups et marque « packs? » : une cible d'un seul pack pourrait passer.
- **Marges serrées** : la plupart des choix sont « serré », parce que A et B sont construits au
  p25 et au p75 : au p25, A gagne. On garde donc « nouvelle », comme la règle le dit.
- **808** : 21 coups sur 23 viennent d'un seul pack : pas de cible, son actuel.
- **Crash trap** : la cible est celle de tous les styles (836 ms et plus à −40 dB), alors que
  le test serré, fait sur les crashs trap, veut plus court (−60 dB avant 1 038 ms). La recette
  est allongée juste assez pour les deux (780 ms à −40 dB), mais elle reste au bas de la plage.
- **Charley ouvert dubstep** : le test serré utilisait la plage de tous les styles ; il prend
  maintenant la cible dubstep (20 coups, 71–194 ms à −20 dB), plus courte.
- **Snare2** : ses rendus diffèrent déjà d'un moteur à l'autre ; quand elle est gardée, le test
  ne peut pas la comparer.
- **Hard techno** : le calage ne suffit pas (bus batterie saturé) ; le best-of corrige par un gain
  sur la moitié « après », pas par le moteur.
- Les mesures fiables sont la longueur et la note ; le timbre (brillance) n'entre pas dans la
  règle.
