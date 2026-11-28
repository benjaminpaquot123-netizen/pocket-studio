# Tour 9, lot « fable5 » (le rouge qui va et vient de Tour5BloquantsTest) : rapport

Branche : ccr-56d43d5b-vfdmoc (venue de 2c78c89b1). Rouge 21d11465, vert 6b644908.
Tout est « tenté » : aucun « corrigé » tant qu'un testeur neuf ne l'a pas constaté.

tests: *Tour5BloquantsTest human.Tour5* ui.Coquille* *FirstLaunch* onboarding.*

## Chemins

chemin: Mode simple, « Écouter » du Guide, onglet Clavier, une partie touchée dans le menu : l'en-tête la nomme à chaque image, du toucher jusqu'à son arrivée | test: Tour5BloquantsTest#everyLookAtTheHeaderNamesThePartPickedUntilItArrives
chemin: Mode simple, « Écouter » du Guide, des parties choisies puis modifiées (le test qui était instable) | test: Tour5BloquantsTest#simplePartsPickedAfterTheGuidesListenAreEdited

## Ce qui change (« tenté »)
- Cause prouvée : le test lisait l'en-tête en deux coups d'œil, le titre puis la note. Pendant que le morceau joue, chaque coup d'œil laisse passer une image de l'appli (15 ms). Quand la partie choisie arrive sur sa barre pendant le second, le test recolle le titre de l'écran d'avant à la note de l'écran d'après : « Partie : B · Couplet / Elle tourne en boucle… », un en-tête qu'aucun écran n'a montré, et qui ne nomme pas le choix.
- L'appli n'a pas de faute ici et ne change pas : à chaque image, son en-tête nomme la partie choisie (la note « … arrive à la prochaine mesure… » tant qu'elle attend, le titre dès qu'elle est là ; les deux changent dans la même image).
- Pourquoi c'était rare : le toucher du banc rend la main environ 1,4 s après le clic ; il faut que la barre (toutes les 2,9 s à 84 BPM) tombe dans les 15 ms du second coup d'œil. La charge de la machine seule ne le reproduit pas : c'est l'instant du toucher dans la mesure qui compte.
- Correction, dans le test seulement (`Tour5BloquantsTest.kt`) : l'en-tête se lit d'un seul coup d'œil (titre et note du même écran), comme le voit un humain. L'affirmation d'origine est la même, mot pour mot ; rien n'est sauté, désactivé ni affaibli.
- La preuve reste comme test : `everyLookAtTheHeaderNamesThePartPickedUntilItArrives` regarde l'en-tête à chaque image, du toucher à l'arrivée de la partie, sur dix choix. Avec l'ancienne lecture, il est rouge à coup sûr ; avec la nouvelle, vert.

## Tests lancés
- Rouge sur 21d11465 : `everyLookAtTheHeaderNamesThePartPickedUntilItArrives`, 8 lancers, 8 rouges, le même en-tête recollé (7 fois « pick 2, look 35 », 1 fois « pick 3, look 35 »).
- Vert sur 6b644908 : la classe `Tour5BloquantsTest` 20 fois de suite, 20 vertes (10 tests, 0 échec, 1 sauté : le siège `drive` du banc, qui ne joue qu'avec `-Phuman.dir`, comme avant).
- Sur 6b644908, une fois : human.Tour5*, ui.Coquille*, *FirstLaunch*, onboarding.* : 21 classes, 135 tests, 0 échec, 4 sautés (les sièges du banc et un test de marque, déjà sautés avant le lot).

## Ce qui n'est pas vert
- CI complète non lancée sur cette branche (c'est le chef qui la lance) : la cause est prouvée en local, pas relue sur la CI.
- Le texte exact du rouge de la CI n'a pas pu être relu (l'artefact n'est pas joignable d'ici) : le rouge local a le même message, avec « C · Refrain » au lieu de « B · Couplet ».
- Pas de relecture neuve (la fiche la laisse au chef). Pré-vol et `pret.sh` non lancés : la branche est celle de la discussion, pas `eq/tour-9-fable5`.

## Demandes (hors de mes fichiers)
Aucune. `headerTitle()` et `headerNote()` n'existent que dans ce fichier ; le banc (`HumanTester.kt`) n'est pas touché. Non cherché : si d'autres tests recollent deux lectures de l'écran sous d'autres noms pendant que le morceau joue.
