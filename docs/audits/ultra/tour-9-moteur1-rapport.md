# Tour 9 : lot « moteur1 », la mesure (rapport)

Fiche B1. Rôle : correcteur. Branche `eq/tour-9-moteur1`, partie de `ccr-80af1a1b-mn0wec`.
Tout est « tenté » : rien n'est « corrigé » tant qu'un testeur ne l'a pas constaté, et la seule preuve de C1 (le son qui coupe) reste la tablette.

**Le son n'a pas changé.** Six morceaux (techno, jump_up, lofi, acidcore, phonk, grime), 30 s chacun, joués par `ps_live` avant le lot et après : fichiers son identiques à l'octet près. La mesure par brique n'est compilée que dans une construction de mesure (`PS_PROFILE`), jamais dans l'appli ni dans les tests.

tests: aucun : lot du moteur et de ses bancs, aucun fichier Kotlin touché ; preuves côté moteur : `scripts/host-tests.sh` (382 tests, 0 échec), dont `ht_worst_gap_unequal_blocks_on_time`, `ht_worst_gap_same_lateness_same_reading` et les cinq `sb_*` ; `scripts/bench/live_selftest.py` vert ; les 37 morceaux rejoués 150 s par `ps_live`, 37 « ok », 0 alerte.

## Chemins

chemin: Lecture d'un morceau de chaque style, moteur joué comme sur le téléphone (banc d'écoute en direct) | test: LiveSongRecorder#recordsTheLivePlaybackOfEveryStyle

## 1. Où part la charge : par style et par brique

**À lire avant les chiffres : sur Linux, c'est l'ordre qui vaut, pas les chiffres.** Un PC n'a ni le processeur ni les à-coups d'une tablette d'entrée de gamme. Ce tableau dit quelle brique regarder en premier ; il ne dit pas quel style coupe.

Pour le refaire : `scripts/cpu-bench.sh --bricks --cmds build/live-bench/cmds` (résultat : `build/cpu-bench/bricks.md`).

Ce qu'on en retient :
- Les **effets** pèsent le plus (44 % du total), devant les **sons** (32 %) et le **mixage** (18 %). La **réverbération** ne pèse presque rien (2 %) : ce n'est pas elle qu'il faut alléger.
- Quatre briques font plus de la moitié de la charge : le synthé (18 %), la saturation (14 %), les tranches du mixeur (12 %) et l'écrêteur (11 %).
- Les styles les plus lourds sont la famille drum and bass (jump_up, liquid_dnb, neurofunk, dnb), à cause du synthé et, pour jump_up, de la distorsion. Les plus légers sont les styles acid.
- La mesure elle-même ne coûte presque rien : charge égale avec et sans, à la précision du banc.


Machine : x86_64, 4 cœurs, Intel(R) Xeon(R) Processor @ 2.10GHz. Blocs de 96 échantillons, moteur joué comme sur le téléphone (ps_live).
**Sur un PC, c'est l'ordre des briques qui compte, pas les chiffres** : seule la tablette dit si un style coupe.
« Charge » = temps de calcul sur temps de musique. Les parts sont des parts de cette charge.

### Par style (du plus lourd au plus léger)

| Style | Charge % | Sons % | Réverbération % | Effets % | Mixage % | Reste du moteur % | Les trois briques les plus chères |
|---|---:|---:|---:|---:|---:|---:|---|
| jump_up | 15,6 | 33,3 | 0,7 | 51,9 | 11,3 | 2,8 | synth 27,1, fx-saturation 14,5, fx-distortion 13,7 |
| liquid_dnb | 12,8 | 45,0 | 0,8 | 38,3 | 12,7 | 3,3 | synth 31,5, fx-saturation 10,4, fx-clipper 9,2 |
| neurofunk | 12,3 | 37,2 | 0,9 | 46,4 | 12,5 | 3,1 | synth 29,8, fx-saturation 10,9, fx-clipper 9,5 |
| dnb | 11,3 | 41,5 | 1,0 | 38,8 | 14,9 | 3,8 | synth 32,6, fx-saturation 12,6, fx-clipper 11,4 |
| dubstep | 10,4 | 32,5 | 1,0 | 47,7 | 15,1 | 3,6 | synth 25,1, fx-saturation 13,0, fx-clipper 11,2 |
| riddim | 10,4 | 32,6 | 1,0 | 48,9 | 13,5 | 4,0 | synth 26,2, fx-saturation 12,6, fx-distortion 10,9 |
| reggaeton | 10,3 | 47,5 | 1,0 | 34,8 | 13,3 | 3,4 | synth 31,9, drums 13,9, fx-clipper 10,8 |
| pop | 10,0 | 41,5 | 1,1 | 38,8 | 14,9 | 3,7 | synth 23,9, fx-clipper 12,7, drums 11,4 |
| phonk | 9,9 | 32,3 | 2,7 | 36,9 | 24,7 | 3,4 | mixer-strips 18,1, synth 15,2, drums 14,3 |
| trap | 9,8 | 31,6 | 5,0 | 40,1 | 19,7 | 3,6 | synth 14,7, mixer-strips 14,0, fx-clipper 10,8 |
| house | 9,7 | 30,1 | 1,2 | 48,2 | 16,3 | 4,3 | fx-saturation 15,0, synth 14,2, fx-clipper 13,5 |
| jungle | 9,7 | 32,8 | 1,1 | 47,1 | 15,1 | 3,9 | synth 23,4, fx-saturation 19,1, fx-clipper 11,7 |
| drill | 9,6 | 31,4 | 3,0 | 40,9 | 20,9 | 3,7 | mixer-strips 14,8, synth 13,2, drums 12,8 |
| lofi | 9,6 | 50,8 | 3,0 | 22,5 | 20,0 | 3,7 | plaits 22,6, synth 17,7, mixer-strips 15,1 |
| synthwave | 9,4 | 38,4 | 1,1 | 41,2 | 15,7 | 3,6 | synth 22,6, fx-clipper 14,0, drums 11,9 |
| amapiano | 9,2 | 31,7 | 1,3 | 45,5 | 17,0 | 4,4 | fx-saturation 15,7, synth 13,4, plaits 11,8 |
| deep_house | 9,0 | 35,5 | 1,3 | 42,8 | 16,2 | 4,3 | synth 14,1, fx-saturation 11,9, plaits 11,6 |
| grime | 8,9 | 31,6 | 5,8 | 42,2 | 16,3 | 4,2 | synth 20,0, fx-clipper 12,8, fx-saturation 11,5 |
| afro_house | 8,9 | 24,1 | 1,3 | 52,5 | 17,7 | 4,5 | fx-saturation 15,7, synth 14,4, fx-clipper 13,6 |
| tech_house | 8,8 | 28,4 | 1,1 | 49,9 | 16,5 | 4,1 | fx-saturation 19,5, synth 18,8, fx-clipper 12,4 |
| boom_bap | 8,6 | 37,4 | 3,7 | 29,3 | 25,2 | 4,4 | synth 21,1, mixer-strips 18,3, drums 13,0 |
| disco | 8,6 | 31,9 | 1,4 | 45,9 | 16,6 | 4,3 | synth 13,5, fx-saturation 12,4, fx-clipper 12,3 |
| uk_garage | 8,3 | 30,7 | 1,2 | 45,7 | 18,2 | 4,2 | synth 14,9, fx-clipper 13,5, mixer-strips 11,6 |
| hard_techno | 8,2 | 21,7 | 1,4 | 53,2 | 18,8 | 4,9 | fx-saturation 24,3, mixer-strips 12,1, fx-clipper 11,9 |
| speed_garage | 8,2 | 27,9 | 1,2 | 47,7 | 18,7 | 4,5 | fx-saturation 15,3, synth 13,5, fx-clipper 13,4 |
| industrial_techno | 8,1 | 16,5 | 1,3 | 53,5 | 24,2 | 4,4 | mixer-strips 17,2, fx-saturation 16,6, fx-clipper 12,9 |
| gabber | 8,0 | 20,2 | 1,4 | 50,0 | 24,0 | 4,5 | fx-saturation 17,3, mixer-strips 16,9, fx-clipper 14,2 |
| trance | 7,9 | 23,8 | 1,4 | 51,8 | 18,2 | 4,9 | fx-saturation 17,2, fx-clipper 14,8, synth 14,4 |
| afrobeats | 7,6 | 30,4 | 1,4 | 44,9 | 18,7 | 4,6 | synth 18,8, fx-clipper 13,0, fx-saturation 12,9 |
| jersey_club | 7,2 | 28,5 | 6,5 | 40,4 | 19,9 | 4,7 | drums 16,5, fx-saturation 13,5, mixer-strips 12,1 |
| detroit | 7,0 | 21,6 | 1,6 | 51,2 | 20,3 | 5,2 | fx-saturation 14,7, fx-clipper 14,5, mixer-strips 12,4 |
| retro_techno | 7,0 | 20,0 | 1,6 | 52,4 | 21,0 | 5,0 | fx-saturation 19,5, fx-clipper 13,8, mixer-strips 13,4 |
| techno | 6,8 | 20,7 | 1,4 | 53,6 | 19,5 | 4,8 | fx-saturation 18,0, fx-clipper 15,1, mixer-strips 12,8 |
| minimal | 6,3 | 22,1 | 4,9 | 47,0 | 20,8 | 5,3 | fx-saturation 15,6, mixer-strips 12,9, synth 12,1 |
| acidcore | 6,2 | 20,8 | 1,8 | 45,0 | 27,4 | 5,1 | fx-saturation 22,6, mixer-strips 19,1, acid-303 15,5 |
| acid_techno | 6,0 | 12,4 | 1,9 | 43,2 | 36,8 | 5,6 | mixer-strips 27,3, fx-saturation 17,7, fx-clipper 10,4 |
| acid_house | 5,9 | 21,4 | 1,9 | 42,0 | 29,1 | 5,5 | mixer-strips 19,6, fx-saturation 17,8, drums 10,1 |

### Les briques classées par coût (tous styles ensemble)

| Rang | Brique | Famille | Part du total % | Charge moyenne % | Pire style | Charge dans ce style % |
|---:|---|---|---:|---:|---|---:|
| 1 | synth | Sons | 17,6 | 1,6 | jump_up | 4,2 |
| 2 | fx-saturation | Effets | 13,8 | 1,2 | jump_up | 2,3 |
| 3 | mixer-strips | Mixage | 12,1 | 1,1 | phonk | 1,8 |
| 4 | fx-clipper | Effets | 11,1 | 1,0 | jump_up | 1,4 |
| 5 | drums | Sons | 8,4 | 0,8 | reggaeton | 1,4 |
| 6 | master-chain | Mixage | 6,1 | 0,5 | phonk | 0,7 |
| 7 | engine-rest | Reste du moteur | 4,1 | 0,4 | dnb | 0,4 |
| 8 | plaits | Sons | 3,4 | 0,3 | lofi | 2,2 |
| 9 | fx-filter | Effets | 2,5 | 0,2 | afro_house | 0,5 |
| 10 | fx-compressor | Effets | 2,3 | 0,2 | jump_up | 0,3 |
| 11 | live-fx | Effets | 1,9 | 0,2 | boom_bap | 0,2 |
| 12 | fx-bass-exciter | Effets | 1,9 | 0,2 | neurofunk | 0,4 |
| 13 | fx-ping-pong | Effets | 1,9 | 0,2 | trap | 0,3 |
| 14 | fx-distortion | Effets | 1,7 | 0,2 | jump_up | 2,1 |
| 15 | reverb-bus | Réverbération | 1,6 | 0,1 | boom_bap | 0,3 |
| 16 | fx-widener | Effets | 1,5 | 0,1 | liquid_dnb | 0,2 |
| 17 | fx-param-eq | Effets | 1,3 | 0,1 | jump_up | 0,2 |
| 18 | delay-bus | Effets | 1,0 | 0,1 | amapiano | 0,1 |
| 19 | fx-chorus | Effets | 0,9 | 0,1 | liquid_dnb | 0,3 |
| 20 | fx-auto-pan | Effets | 0,9 | 0,1 | liquid_dnb | 0,4 |
| 21 | fx-none | Effets | 0,8 | 0,1 | jump_up | 0,1 |
| 22 | fx-ott | Effets | 0,7 | 0,1 | jump_up | 0,4 |
| 23 | acid-303 | Sons | 0,7 | 0,1 | acidcore | 1,0 |
| 24 | audio-clips | Sons | 0,5 | 0,0 | neurofunk | 0,0 |
| 25 | pad-sampler | Sons | 0,3 | 0,0 | afro_house | 0,2 |
| 26 | fx-reverb | Réverbération | 0,3 | 0,0 | grime | 0,2 |
| 27 | multi-sampler | Sons | 0,2 | 0,0 | liquid_dnb | 0,0 |
| 28 | bass-808 | Sons | 0,2 | 0,0 | liquid_dnb | 0,0 |
| 29 | fx-flanger | Effets | 0,2 | 0,0 | riddim | 0,3 |
| 30 | sample-instrument | Sons | 0,1 | 0,0 | phonk | 0,1 |
| 31 | soundfont | Sons | 0,1 | 0,0 | liquid_dnb | 0,0 |
| 32 | fx-pump | Effets | 0,1 | 0,0 | drill | 0,0 |
| 33 | fx-vinyl | Effets | 0,1 | 0,0 | lofi | 0,2 |
| 34 | fx-echo | Effets | 0,0 | 0,0 | minimal | 0,1 |
| 35 | fx-eq | Effets | 0,0 | 0,0 | boom_bap | 0,0 |
| 36 | fx-crusher | Effets | 0,0 | 0,0 | industrial_techno | 0,0 |
| 37 | fx-tape-stop | Effets | 0,0 | 0,0 | jungle | 0,0 |

### Les familles

| Famille | Part du total % |
|---|---:|
| Effets | 44,4 |
| Sons | 31,5 |
| Mixage | 18,2 |
| Reste du moteur | 4,1 |
| Réverbération | 1,8 |

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
- Il ne juge pas : un morceau en solo, un style sans budget (« none »), ni une construction qui ne compte pas les allocations ou qui profile (« not-counted »).

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
- Les budgets sont posés sur x86 seulement.
