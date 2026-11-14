# Tour 6 — lot « tutoordre2 » : rapport

Fichier d'appli touché : `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt` (`check()`, trois lignes). Un test neuf : `Tour6TutoOrderRaceTest`.

## Le défaut (CI rouge)
- CI 43448175188 (tête `2e29fdd75`, 8879 tests, 1 échec) : `TutoLot5ControllerTest#aWrongOrderIsSaid`, « what is left said: not in time » (ligne 132 : le message « Encore … » sur l'ordre des parties n'arrive jamais).
- **Pas causé par tutoordre** : sa ligne ne touche que « Déjà fait ». C'est une course voisine, révélée par une CI plus chargée (lot « ci3 », travaux en parallèle) : le premier regard sur le projet (au démarrage, toutes les étapes que le projet tient déjà) travaille encore quand les parties de la chanson changent ; le regard suivant part de la progression d'avant la réponse du premier, retrouve ces étapes, et un regard qui trouve quelque chose ne dit jamais ce qui manque à l'ordre.
- **Reproduction déterministe** : `Tour6TutoOrderRaceTest#whatTheOrderLacksIsSaidWhenAnEarlierLookAnswersLate`, qui retient les regards hors du fil principal jusqu'au changement des parties. Rouge sur le parent du vert (« made » reste vide), vert après.
- **Correction** : `check()` ne garde que ce qui n'est pas encore coché au moment de sa réponse (`vu − coché maintenant`).
- Localement, la classe `TutoLot5ControllerTest` passe seule (3 fois) : la course ne se montre qu'avec la charge de la CI.
- rejeu impossible : défaut de la CI, pas d'un siège ; remplacé par le test déterministe.

## À noter (moyen)
- Une fois sur trois passages de la suite du tuto, `Tour6TutoFlechesRollTest#houseBassAfterTheCopyTheRollShowsTheNextNoteAt130` a échoué (« Copier » pas proposé dans le temps du test, `List is empty` ligne 97) ; vert seul deux fois et dans la suite suivante. Test sensible au minutage (attentes de 2 s) : à surveiller.

## Paires
- `rouge: a late look hides what the song order lacks` → `vert: a late look hides what the song order lacks`.

tests: com.pocketstudio.app.tuto.Tour6TutoOrderRaceTest com.pocketstudio.app.tuto.TutoLot5ControllerTest com.pocketstudio.app.tuto.Tour6TutoPanelAlreadyTest

chemin: Tuto de A à Z › jump up Facile › « Mets les parties dans l'ordre » › quatre parties posées | test: Tour6TutoOrderRaceTest#whatTheOrderLacksIsSaidWhenAnEarlierLookAnswersLate
chemin: Tuto de A à Z › jump up Facile › « Mets les parties dans l'ordre » › quatre parties posées | test: TutoLot5ControllerTest#aWrongOrderIsSaid
