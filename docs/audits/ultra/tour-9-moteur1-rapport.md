# Tour 9 : lot « moteur1 », la mesure (rapport)

Fiche B1. Rôle : correcteur. Branche `eq/tour-9-moteur1`, partie de `ccr-80af1a1b-mn0wec`.
Tout est « tenté » : rien n'est « corrigé » tant qu'un testeur ne l'a pas constaté, et la seule preuve de C1 (le son qui coupe) reste la tablette.

**Le son n'a pas changé.** Six morceaux (techno, jump_up, lofi, acidcore, phonk, grime), 30 s chacun, joués par `ps_live` avant le lot et après : fichiers son identiques à l'octet près. La mesure par brique n'est compilée que dans une construction de mesure (`PS_PROFILE`), jamais dans l'appli ni dans les tests.

tests: aucun : lot du moteur et de ses bancs, aucun fichier Kotlin touché ; preuves côté moteur : `scripts/host-tests.sh` (377 tests, 0 échec), dont `ht_worst_gap_unequal_blocks_on_time`, `ht_worst_gap_same_lateness_same_reading` et les cinq `sb_*` ; `scripts/bench/live_selftest.py` vert ; les 37 morceaux rejoués 150 s par `ps_live`, 37 « ok », 0 alerte.

## Chemins

chemin: Lecture d'un morceau de chaque style, moteur joué comme sur le téléphone (banc d'écoute en direct) | test: LiveSongRecorder#recordsTheLivePlaybackOfEveryStyle

## 1. Où part la charge : par style et par brique

**À lire avant les chiffres : sur Linux, c'est l'ordre qui vaut, pas les chiffres.** Un PC n'a ni le processeur ni les à-coups d'une tablette d'entrée de gamme. Ce tableau dit quelle brique regarder en premier ; il ne dit pas quel style coupe.

Pour le refaire : `scripts/cpu-bench.sh --bricks --cmds build/live-bench/cmds` (résultat : `build/cpu-bench/bricks.md`).

Ce qu'on en retient :
- Les **effets** pèsent le plus (45 % du total), devant les **sons** (31 %) et le **mixage** (19 %). La **réverbération** ne pèse presque rien (2 %) : ce n'est pas elle qu'il faut alléger.
- Quatre briques font plus de la moitié de la charge : le synthé (17 %), la saturation (14 %), les tranches du mixeur (12 %) et l'écrêteur (11 %).
- Les styles les plus lourds sont jump_up, liquid_dnb et neurofunk (famille drum and bass), à cause du synthé et, pour jump_up, de la distorsion. Les plus légers sont les styles acid.
- La mesure elle-même coûte peu : sur trois morceaux (acid_house, jump_up, lofi, 60 s), la charge avec la mesure va de 0,96 à 1,08 fois la charge sans.
- Le tableau ne compte que le morceau : la somme des briques retombe sur la charge du moteur (acid_house : 4,1 % par les briques, 4,13 % au compteur du banc). Une première version comptait aussi la scène de référence du budget (environ 1,3 point de trop par style, batterie gonflée) ; la relecture l'a vu, le banc remet le compteur à zéro avant le morceau et le tableau a été refait.


Machine : x86_64, 4 cœurs, Intel(R) Xeon(R) Processor @ 2.30GHz. Blocs de 96 échantillons, moteur joué comme sur le téléphone (ps_live).
**Sur un PC, c'est l'ordre des briques qui compte, pas les chiffres** : seule la tablette dit si un style coupe.
« Charge » = temps de calcul sur temps de musique. Les parts sont des parts de cette charge.

### Par style (du plus lourd au plus léger)

| Style | Charge % | Sons % | Réverbération % | Effets % | Mixage % | Reste du moteur % | Les trois briques les plus chères |
|---|---:|---:|---:|---:|---:|---:|---|
| jump_up | 11,2 | 32,3 | 0,7 | 52,6 | 11,6 | 2,7 | synth 26,2, fx-saturation 14,6, fx-distortion 14,3 |
| liquid_dnb | 9,8 | 43,9 | 0,9 | 39,0 | 13,1 | 3,2 | synth 30,4, fx-saturation 10,5, fx-clipper 9,5 |
| neurofunk | 9,3 | 36,1 | 0,9 | 47,1 | 12,9 | 3,0 | synth 28,8, fx-saturation 11,0, fx-clipper 9,7 |
| phonk | 7,8 | 31,4 | 2,8 | 37,5 | 24,9 | 3,3 | mixer-strips 18,2, synth 14,3, drums 14,2 |
| pop | 7,6 | 40,4 | 1,1 | 39,7 | 15,4 | 3,5 | synth 22,6, fx-clipper 13,0, drums 11,6 |
| dnb | 7,6 | 40,1 | 1,1 | 39,6 | 15,6 | 3,6 | synth 31,4, fx-saturation 12,8, fx-clipper 11,7 |
| reggaeton | 7,6 | 46,0 | 1,1 | 35,8 | 13,8 | 3,3 | synth 30,5, drums 13,8, fx-clipper 11,0 |
| trap | 7,6 | 30,6 | 5,0 | 40,8 | 20,2 | 3,4 | mixer-strips 14,4, synth 13,8, fx-clipper 11,0 |
| dubstep | 7,5 | 31,3 | 1,1 | 48,1 | 15,5 | 4,0 | synth 24,1, fx-saturation 13,0, fx-clipper 11,4 |
| riddim | 7,4 | 32,0 | 1,1 | 49,7 | 14,1 | 3,1 | synth 25,6, fx-saturation 12,7, fx-distortion 11,1 |
| synthwave | 7,4 | 37,7 | 1,1 | 41,8 | 16,0 | 3,3 | synth 21,8, fx-clipper 14,4, drums 12,0 |
| jungle | 7,2 | 32,0 | 1,1 | 47,6 | 15,6 | 3,7 | synth 22,7, fx-saturation 19,1, fx-clipper 12,0 |
| tech_house | 7,1 | 27,7 | 1,2 | 50,4 | 16,8 | 3,9 | fx-saturation 19,5, synth 18,2, fx-clipper 12,5 |
| lofi | 7,0 | 49,3 | 3,1 | 23,3 | 20,8 | 3,5 | plaits 22,0, synth 16,7, mixer-strips 15,6 |
| house | 6,7 | 29,0 | 1,2 | 48,9 | 16,9 | 4,0 | fx-saturation 15,0, fx-clipper 13,7, synth 13,6 |
| uk_garage | 6,5 | 30,0 | 1,2 | 46,2 | 18,6 | 4,0 | synth 14,2, fx-clipper 13,8, mixer-strips 12,0 |
| drill | 6,5 | 30,5 | 3,0 | 41,5 | 21,6 | 3,4 | mixer-strips 15,2, drums 12,7, synth 12,5 |
| amapiano | 6,5 | 30,7 | 1,3 | 46,4 | 17,5 | 4,1 | fx-saturation 15,8, synth 13,0, mixer-strips 11,6 |
| speed_garage | 6,4 | 27,6 | 1,2 | 48,1 | 19,0 | 4,1 | fx-saturation 15,3, fx-clipper 13,5, synth 13,2 |
| afro_house | 6,4 | 23,3 | 1,3 | 53,3 | 18,0 | 4,1 | fx-saturation 15,8, fx-clipper 13,8, synth 13,7 |
| grime | 6,3 | 30,3 | 5,9 | 43,2 | 16,8 | 3,8 | synth 18,9, fx-clipper 13,2, fx-saturation 11,8 |
| deep_house | 6,2 | 34,0 | 1,3 | 43,9 | 16,9 | 3,9 | synth 13,3, fx-saturation 12,1, fx-clipper 11,4 |
| industrial_techno | 6,1 | 16,2 | 1,4 | 53,7 | 24,5 | 4,3 | mixer-strips 17,5, fx-saturation 16,5, fx-clipper 12,8 |
| disco | 6,1 | 31,2 | 1,4 | 46,4 | 17,0 | 4,0 | synth 13,0, fx-saturation 12,5, fx-clipper 12,4 |
| afrobeats | 5,9 | 29,4 | 1,4 | 45,8 | 19,1 | 4,2 | synth 18,0, fx-saturation 13,0, fx-clipper 13,0 |
| boom_bap | 5,9 | 36,2 | 3,6 | 30,3 | 25,9 | 4,1 | synth 20,0, mixer-strips 18,9, fx-saturation 12,9 |
| gabber | 5,8 | 20,0 | 1,4 | 50,0 | 24,5 | 4,2 | mixer-strips 17,2, fx-saturation 17,2, fx-clipper 14,2 |
| techno | 5,7 | 20,2 | 1,5 | 53,9 | 19,8 | 4,7 | fx-saturation 17,9, fx-clipper 15,2, mixer-strips 12,9 |
| trance | 5,7 | 22,9 | 1,4 | 52,7 | 18,6 | 4,4 | fx-saturation 17,3, fx-clipper 15,0, synth 13,9 |
| hard_techno | 5,7 | 21,1 | 1,4 | 53,5 | 19,4 | 4,5 | fx-saturation 24,3, mixer-strips 12,5, fx-clipper 12,0 |
| jersey_club | 5,6 | 27,9 | 7,0 | 40,7 | 20,1 | 4,3 | drums 15,9, fx-saturation 13,5, mixer-strips 12,1 |
| detroit | 5,0 | 20,9 | 1,6 | 52,1 | 20,5 | 4,8 | fx-saturation 14,9, fx-clipper 14,6, mixer-strips 12,7 |
| retro_techno | 5,0 | 19,7 | 1,6 | 52,5 | 21,5 | 4,7 | fx-saturation 19,6, fx-clipper 14,0, mixer-strips 13,7 |
| minimal | 4,7 | 21,1 | 5,2 | 47,7 | 21,1 | 4,8 | fx-saturation 15,8, mixer-strips 13,0, synth 11,5 |
| acidcore | 4,4 | 20,7 | 1,8 | 45,0 | 27,5 | 4,9 | fx-saturation 22,5, mixer-strips 19,2, acid-303 15,6 |
| acid_house | 4,1 | 21,1 | 1,9 | 42,5 | 29,5 | 5,0 | mixer-strips 19,9, fx-saturation 17,9, fx-clipper 9,7 |
| acid_techno | 4,0 | 12,0 | 1,9 | 43,5 | 37,5 | 5,0 | mixer-strips 27,7, fx-saturation 18,1, fx-clipper 10,3 |

### Les briques classées par coût (tous styles ensemble)

| Rang | Brique | Famille | Part du total % | Charge moyenne % | Pire style | Charge dans ce style % |
|---:|---|---|---:|---:|---|---:|
| 1 | synth | Sons | 16,9 | 1,1 | liquid_dnb | 3,0 |
| 2 | fx-saturation | Effets | 13,9 | 0,9 | jump_up | 1,6 |
| 3 | mixer-strips | Mixage | 12,4 | 0,8 | phonk | 1,4 |
| 4 | fx-clipper | Effets | 11,3 | 0,7 | synthwave | 1,1 |
| 5 | drums | Sons | 8,3 | 0,5 | phonk | 1,1 |
| 6 | master-chain | Mixage | 6,2 | 0,4 | phonk | 0,5 |
| 7 | engine-rest | Reste du moteur | 3,8 | 0,3 | liquid_dnb | 0,3 |
| 8 | plaits | Sons | 3,3 | 0,2 | lofi | 1,5 |
| 9 | fx-filter | Effets | 2,4 | 0,2 | afro_house | 0,3 |
| 10 | fx-compressor | Effets | 2,2 | 0,1 | jump_up | 0,2 |
| 11 | fx-ping-pong | Effets | 2,2 | 0,1 | afrobeats | 0,3 |
| 12 | fx-bass-exciter | Effets | 1,9 | 0,1 | neurofunk | 0,3 |
| 13 | live-fx | Effets | 1,7 | 0,1 | phonk | 0,1 |
| 14 | fx-distortion | Effets | 1,7 | 0,1 | jump_up | 1,6 |
| 15 | reverb-bus | Réverbération | 1,6 | 0,1 | phonk | 0,2 |
| 16 | fx-widener | Effets | 1,5 | 0,1 | liquid_dnb | 0,2 |
| 17 | fx-param-eq | Effets | 1,3 | 0,1 | liquid_dnb | 0,1 |
| 18 | delay-bus | Effets | 1,1 | 0,1 | tech_house | 0,1 |
| 19 | fx-chorus | Effets | 1,0 | 0,1 | liquid_dnb | 0,2 |
| 20 | fx-auto-pan | Effets | 0,9 | 0,1 | liquid_dnb | 0,3 |
| 21 | fx-none | Effets | 0,8 | 0,1 | jump_up | 0,1 |
| 22 | fx-ott | Effets | 0,7 | 0,0 | neurofunk | 0,3 |
| 23 | acid-303 | Sons | 0,7 | 0,0 | acidcore | 0,7 |
| 24 | audio-clips | Sons | 0,5 | 0,0 | neurofunk | 0,0 |
| 25 | pad-sampler | Sons | 0,3 | 0,0 | afro_house | 0,1 |
| 26 | fx-reverb | Réverbération | 0,3 | 0,0 | jersey_club | 0,2 |
| 27 | multi-sampler | Sons | 0,2 | 0,0 | liquid_dnb | 0,0 |
| 28 | bass-808 | Sons | 0,2 | 0,0 | liquid_dnb | 0,0 |
| 29 | fx-flanger | Effets | 0,2 | 0,0 | riddim | 0,2 |
| 30 | sample-instrument | Sons | 0,1 | 0,0 | phonk | 0,1 |
| 31 | soundfont | Sons | 0,1 | 0,0 | liquid_dnb | 0,0 |
| 32 | fx-pump | Effets | 0,1 | 0,0 | jersey_club | 0,0 |
| 33 | fx-vinyl | Effets | 0,1 | 0,0 | lofi | 0,1 |
| 34 | fx-echo | Effets | 0,0 | 0,0 | minimal | 0,1 |
| 35 | fx-eq | Effets | 0,0 | 0,0 | lofi | 0,0 |
| 36 | fx-crusher | Effets | 0,0 | 0,0 | phonk | 0,0 |
| 37 | fx-tape-stop | Effets | 0,0 | 0,0 | jungle | 0,0 |

### Les familles

| Famille | Part du total % |
|---|---:|
| Effets | 45,1 |
| Sons | 30,6 |
| Mixage | 18,6 |
| Reste du moteur | 3,8 |
| Réverbération | 1,9 |

Briques : `fx-*` = un effet d'insert (pistes et master), `mixer-strips` = tranches, envois et sommes,
`master-chain` = chaîne du master (limiteur compris), `live-fx` = effets du Live, `engine-rest` = séquenceur et notes.

## 2. L'écart entre blocs (C3) mentait

Oui, il mentait, dans les deux sens. L'ancienne mesure comparait le temps écoulé entre deux blocs à la durée du bloc qui arrive. Quand le téléphone demande des blocs de tailles inégales (ce que fait une sortie audio ordinaire) :
- un petit bloc demandé à l'heure après un grand se lisait comme un grand retard (fausse alerte) ;
- un même retard de 6 ms se lisait différemment selon la taille du bloc voisin.

Réparation 1 sur 3, tentée, rouge puis vert (`3e69dd8d7`, `c090b1c35`) : la mesure compte maintenant le retard par rapport à l'heure prévue du bloc, rapporté au bloc habituel. Aucune signature de `CallbackLoad.h` ni d'`Engine.h` n'a changé.
- `ht_worst_gap_unequal_blocks_on_time` : blocs de 16 et 176 échantillons, aucun en retard, lecture 1,49 bloc (avant : alerte).
- `ht_worst_gap_same_lateness_same_reading` : 6 ms de retard après un grand ou un petit bloc, même lecture (2,77 et 2,77).

Limite connue : deux blocs égaux demandés coup sur coup se lisent toujours 2,0. Ce n'est pas un retard, c'est le rythme normal de certains téléphones ; à garder en tête en lisant C3 sur la tablette.

## 3. Un budget de charge par style, relatif

Le budget n'est pas en millisecondes. Dans le même passage et sur le même fil, `ps_live` mesure une scène de référence fixe (toute la batterie sur chaque pas et un accord de six notes au synthé, à 140), avant et après le morceau, et garde la plus lente. Le budget d'un style est le rapport « charge du morceau sur charge de la référence ». Une machine deux fois plus lente ralentit les deux : le rapport ne bouge pas. Une brique qui devient plus chère pour un style le fait monter.

- Table : `engine/bench/StyleBudgets.h`, un rapport mesuré par style ; la limite est ce rapport fois 1,5.
- Où il est jugé : `scripts/live-bench.sh`, déjà lancé par la CI. Chaque morceau écrit une ligne `BUDGET style=… ratio=… ok`, et un dépassement devient une alerte (le banc échoue). Les morceaux sont trop gros pour les tests du moteur, d'où ce partage.
- Dans les tests du moteur (`engine/host-tests/stylebudget_tests.inc`) : le verdict lit le rapport et pas la vitesse, il dit quand il ne peut pas juger, la table couvre les 37 styles une fois chacun, la scène de référence est stable dans un même passage (5,82 % puis 5,81 %) et coûte bien plus qu'un moteur au repos (5,75 % contre 1,09 %).
- Tenue de la mesure : rejoués une seconde fois, quatre morceaux à la fois, les 37 rapports restent entre 0,92 et 1,09 fois la première mesure, loin de la marge de 1,5.
- Il ne juge pas : un morceau en solo, un style sans budget (« none »), une construction qui ne compte pas les allocations ou qui profile, ni un passage différent de celui qui a servi à poser la table, c'est-à-dire autre chose que 150 s jouées une fois, sans boucle (« not-counted »). Raison : le rapport dépend du passage joué (acid_house : 0,58 sur 20 s, 0,80 sur 150 s).

| Style | Rapport mesuré | Limite (× 1,5) | Rapport mesuré / première mesure, second passage |
|---|---:|---:|---:|
| jump_up | 2,26 | 3,39 | 0,93 |
| liquid_dnb | 2,25 | 3,38 | 1,00 |
| dnb | 1,95 | 2,92 | 0,95 |
| neurofunk | 1,86 | 2,79 | 0,95 |
| dubstep | 1,55 | 2,33 | 0,96 |
| reggaeton | 1,48 | 2,22 | 0,98 |
| pop | 1,44 | 2,16 | 1,01 |
| riddim | 1,43 | 2,15 | 0,96 |
| jungle | 1,41 | 2,11 | 0,99 |
| tech_house | 1,33 | 2,00 | 0,98 |
| afro_house | 1,33 | 2,00 | 0,98 |
| uk_garage | 1,32 | 1,98 | 0,96 |
| speed_garage | 1,30 | 1,95 | 0,95 |
| trap | 1,30 | 1,95 | 1,06 |
| house | 1,29 | 1,94 | 1,02 |
| amapiano | 1,26 | 1,89 | 0,97 |
| synthwave | 1,26 | 1,89 | 1,07 |
| disco | 1,22 | 1,83 | 0,96 |
| phonk | 1,22 | 1,83 | 0,97 |
| drill | 1,16 | 1,74 | 1,01 |
| deep_house | 1,15 | 1,72 | 0,97 |
| lofi | 1,13 | 1,69 | 1,00 |
| gabber | 1,11 | 1,67 | 1,09 |
| afrobeats | 1,10 | 1,65 | 1,00 |
| grime | 1,06 | 1,59 | 1,06 |
| trance | 1,03 | 1,54 | 0,93 |
| detroit | 1,01 | 1,52 | 0,99 |
| techno | 0,99 | 1,48 | 0,92 |
| industrial_techno | 0,99 | 1,48 | 1,00 |
| hard_techno | 0,96 | 1,44 | 1,03 |
| boom_bap | 0,93 | 1,40 | 0,97 |
| jersey_club | 0,91 | 1,36 | 1,08 |
| retro_techno | 0,89 | 1,33 | 0,97 |
| acidcore | 0,87 | 1,30 | 0,96 |
| acid_house | 0,80 | 1,20 | 1,01 |
| minimal | 0,80 | 1,20 | 0,98 |
| acid_techno | 0,70 | 1,05 | 1,00 |

Limite connue : les rapports ont été posés sur un processeur x86. Sur un ARM, le poids relatif des briques peut différer ; la marge de 1,5 est là pour ça, et le premier passage sur la tablette ou sur un runner ARM dira s'il faut reposer la table (une ligne par style à recopier).

## Ce qui n'est pas vert

- Rien de rouge sur la machine du lot. CI complète non attendue (règle du tour).
- C1 n'est pas prouvé : aucune mesure sur la tablette dans ce lot. Le classement donne l'ordre des briques à alléger, pas la garantie que le son ne coupe plus.
- C3 : « tenté ». La limite des blocs égaux coup sur coup (lecture 2,0) reste.
- Les budgets sont posés sur x86 seulement, sur la machine du lot. Un dépassement fait échouer le banc d'écoute de la CI : à la première CI, si un style dépasse sur le runner de GitHub sans que le moteur ait changé, c'est la table qu'il faut reposer (ou la marge), pas le moteur.
- Trois morceaux rejoués 150 s après la correction du banc (acid_house, techno, lofi) : « ok ». Les 37 n'ont pas été rejoués après elle (le chemin jugé n'a pas changé pour un passage de 150 s).
