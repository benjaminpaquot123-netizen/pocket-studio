# Relecture neuve : tour 9, lot moteur1
Relu : 0e80f4dcd

Périmètre : les commits du lot sur `eq/tour-9-moteur1` (`git log --first-parent ea301e4f0..HEAD`) ; la fusion 614d7a3e6 n'apporte rien sous `engine/` ni `scripts/`.

Ce qui a été vérifié, en le refaisant :
- Le défaut grave de la relecture d'avant est bien réparé. Avec l'outil de mesure (`build/brick-bench-out/brick-tools/ps_live`), techno 20 s : la somme des lignes BRICK fait 1,100 s, soit 5,50 % ; le compteur du banc dit `mean=5.50%`. Dans les fichiers du tableau : acid_house 4,13 % par les briques contre 4,13 % au compteur, jump_up 11,23 % contre 11,2 %. La scène de référence ne compte plus. Elle tourne avant la remise à zéro (ps_live.cpp:612-615), et la seconde passe après l'écriture des lignes BRICK (ps_live.cpp:742-753). Le programme n'a qu'un fil, donc rien d'autre ne tourne entre-temps.
- Les tableaux du §1 du rapport sont identiques à `build/brick-bench-out/bricks.md`, à la ligne près.
- Le son n'a pas changé : jump_up, lofi et acidcore, 20 s, joués par un `ps_live` construit depuis ea301e4f0 et par celui de la tête. Les fichiers son sont identiques à l'octet. L'outil de mesure donne aussi le même fichier son que l'outil normal.
- Construction normale : `Profile.h` donne des macros vides sans `PS_PROFILE`. Aucune ligne BRICK ne sort. Les tests et le moteur de l'appli ne définissent pas `PS_PROFILE` : seul `-DPS_PROFILE=ON`, passé par `cpu-bench.sh --bricks`, l'active. Les blocs ajoutés dans `InstrumentTrack.h`, `Engine.cpp` et `Mixer.cpp` gardent le même ordre et les mêmes conditions de calcul.
- Fil audio : `CallbackLoad` ne fait que des calculs simples, sans allocation, verrou, journal ni appel JNI. Ses signatures publiques et celles d'`Engine.h` n'ont pas changé : seuls des champs et une fonction privés ont été ajoutés.
- Le budget n'est jugé que pour un passage de 150 s joué une seule fois, en blocs de 96, sans profilage, dans une construction qui compte les allocations. En solo, la scène de référence n'est pas jouée.
- Tests : les deux `ht_worst_gap_*` prouvent les deux mensonges de C3 (fausse alerte avec des blocs inégaux, lecture différente pour un même retard). Les `sb_*` vérifient le verdict, la table et la stabilité de la référence.
- Le rapport dit « tenté » et jamais « corrigé ». Il dit que sur Linux, c'est l'ordre des briques qui vaut, pas les chiffres.

## Bloquant
aucun

## Grave
aucun

## Moyen
- engine/bench/brick_report.py:34-44 : rien ne surveille automatiquement que la somme des briques retombe sur la charge du banc. Le script lit la ligne CPU, mais seulement pour la durée ; il ne compare pas `mean=` à la somme des BRICK. Scénario : une prochaine modification de `ps_live.cpp` fait de nouveau passer un rendu hors morceau dans les briques (la scène de référence déplacée, ou un préchauffage ajouté). Le tableau regonfle la batterie et le synthé sans qu'aucun script ni test ne le voie, comme dans la version b54ac27e4. Il suffirait que `brick_report.py` refuse un fichier où la somme s'écarte de plus de quelques % de `mean=`.

## Mineur
- engine/bench/ps_live.cpp:759 : la condition « joué une seule fois » repose sur `loops.size() < 2`. Un essai d'endurance (`soak.sh --minutes`) réglé pour durer 150 s avec un morceau bouclé de plus de 75 s n'a qu'une boucle entière. Il serait alors jugé alors que le passage est différent (morceau coupé). Cas peu probable : `soak.sh` dure plusieurs minutes par défaut.
- engine/bench/StyleBudgets.h:27 et ps_live.cpp:753 : en CI, `live-bench.sh` joue plusieurs morceaux à la fois sur un petit runner. La référence est mesurée avant et après le morceau, pas pendant. Un runner chargé de façon inégale peut donc faire échouer le banc sans que le moteur ait changé. Le rapport le reconnaît (« reposer la table ou la marge »), et la marge de 1,5 et le « garder la plus lente des deux » limitent ce risque.
- engine/src/main/cpp/core/CallbackLoad.h:41-47 : un retard de moins d'un bloc ne recale pas l'heure prévue. Si le téléphone garde ensuite ce décalage, chaque bloc suivant se lit environ 1,5 bloc jusqu'à ce que la tolérance de dérive (0,1 % par bloc) l'efface, soit environ 1 s pour des blocs de 2 ms. De même, une horloge audio plus lente de plus de 0,1 % fait monter la lecture vers 2 de temps en temps. Ce n'est qu'une mesure, sans effet sur le son ; à garder en tête en lisant C3 sur la tablette, comme la limite des blocs coup sur coup déjà notée dans le rapport.

Verdict : 0 bloquant, 0 grave
