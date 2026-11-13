# Tour 6 — lot « réparations » : relecture neuve (v2, après les retouches)
Relu : 20a1c61f058b7ebb3358280b990e4e7e9014c361

Seconde relecture, en lecture seule : le commit `20a1c61f0` en entier, puis `git diff 2bb86bc3a 20a1c61f0` (code de l'appli : `PianoRoll.kt` seul, fonctions `Grid` et `TutoNotesLeft`), `TutoSightHintTest.kt` en entier, le rapport et les décisions. Gradle n'a pas été lancé. La nouvelle assertion vérifie bien la note demandée par « Je le fais ». Le rapport et les décisions corrigés disent vrai à la lecture du code, à une imprécision près (voir Mineur). Le commit ne change aucun code de l'appli et je n'y trouve rien de faux. Je confirme « 0 bloquant, 0 grave » de la première relecture.

Trois points de la première relecture sont réglés par le commit et retirés :
- **Placement prouvé à moitié** : l'assertion nomme maintenant la note demandée.
- **Phrase « au plus deux recompositions »** : remplacée par une phrase exacte.
- **Nom du self-test** : `scripts/bench/rejeu-defauts.sh:67` appelle bien `scripts/bench/selftest.sh`.

## Bloquant
Aucun.

## Grave
Aucun.

## Moyen
- **`app/src/main/java/com/pocketstudio/app/synth/PianoRoll.kt:673-680` et `:695` — aucune branche de `placedEarly` n'a de test à elle (maintenu).**
  - Non couvert : « l'utilisateur a fait défiler, on ne replace pas », « le replacement n'a lieu qu'une fois », « la demande en attente est servie une seule fois » (`:708`).
  - Le rapport le déclare maintenant, sauf le « une seule fois » du replacement.
  - Les tests house ne prouvent que l'ensemble (attente de la demande + second placement). Rien ne montre que retirer `placedEarly` seul ferait rougir un test.
  - À la lecture, je ne trouve pas de défaut dans ces branches.
- **`PianoRoll.kt:657-666` — `steady` n'est remis à `null` que sous une rangée visible (maintenu, maintenant écrit).**
  - Pendant un mouvement ordinaire, il garde l'ancienne valeur.
  - Une étape qui commence pendant que le panneau change de hauteur est placée sur les rangées du moment (`:685-695`), et `placedEarly` reste `null` : pas de second placement.
  - Le défaut réparé pour « Je le fais » peut donc revenir sur « Étape suivante ».
  - Ce n'est pas une régression (comportement d'avant le lot). Le rapport et la décision le disent maintenant ; la limite reste sans test.
- **Hors diff : `app/src/main/java/com/pocketstudio/app/rhythm/RhythmScreen.kt:715` contre `:802` (commit `5c72d6867`, maintenu).**
  - Le chemin « peindre » teste `if (follow)`, le chemin « toucher » teste `if (followedBar >= 0)`.
  - Un trait peint à l'arrêt éteint « Suivre » sans rien dire ; une case touchée à l'arrêt ne l'éteint pas.
  - Aucun test ne couvre le trait à l'arrêt. Le rapport le note maintenant, à trancher par le lot « tutogrille ».

## Mineur
- **`app/src/test/java/com/pocketstudio/app/tuto/TutoSightHintTest.kt:130-134` — la nouvelle assertion est juste.**
  - `leftNotes().first()` suit le même ordre que `left` de `Grid` (`PianoRoll.kt:650` : début, puis hauteur décroissante). C'est la note que la demande allume (`:712`).
  - Elle est lue avant tout `addNote`, sur la même mesure `s` que le reste, et exige « dans la mesure visible » et « entière au-dessus du panneau ».
  - Limite : elle ne vaut que pour deux styles, au niveau Facile, à l'ouverture.
- **Rapport, « Vérifié » — le vert d'après retouche n'est pas écrit.**
  - Les « 63 tests verts » datent d'avant la nouvelle assertion.
  - Indice trouvé sur place : `app/build/test-results/testDebugUnitTest/TEST-com.pocketstudio.app.tuto.TutoSightHintTest.xml` montre 4 tests, 0 échec, à 05:27:36 UTC. La source du test date de 05:26:10, le commit de 05:27:59.
  - Le passage vert avec la nouvelle assertion est donc très probable, mais je ne l'ai pas rejoué.
- **Rapport, « Vérifié » — le rouge d'origine reste invérifiable ici.** « House rougissait sur le placement, note 45 dessinée de 519 à 549 px » est cohérent avec le code et l'ancienne assertion, mais repose sur la parole du rapport.
- **Rapport, « Laissé de côté » — « les rangées ne sont attendues stables qu'en sortant de sous le panneau » est un peu court.** `steady` vaut aussi `null` à la toute première ouverture et dès que le rouleau a moins d'une rangée visible, quelle qu'en soit la cause (`PianoRoll.kt:660`). Sans conséquence.
- **`PianoRoll.kt:669` et `:704` — le bon résultat dépend de l'ordre de deux effets.**
  - Quand `steady` arrive, le second placement (`scrollTo`) et la demande (`animateScrollTo`) partent ensemble sur le même défilement.
  - Le placement est déclaré en premier et se termine sans attendre ; la demande lit ensuite la position à jour.
  - Dans l'ordre inverse, le placement couperait l'animation. Rien dans le code ne dit cette dépendance ; la nouvelle assertion la garde pour house et jump up.
- **`PianoRoll.kt:677-679` — saut sec du rouleau (maintenu).**
  - Le second placement n'est pas animé, 0,2 s après la fin du mouvement de la page : le débutant voit le rouleau arriver puis sauter.
  - Un doigt posé sans glisser à cet instant tombe sur une autre rangée. Fenêtre courte.
- **`PianoRoll.kt:708` — demande en attente servie tard (maintenu).** Une demande faite sans rangée visible attend sans limite de temps. Elle n'est ni perdue ni répétée (`focusSeen` est écrit avant tout défilement), mais le rouleau peut bouger et allumer une note sans geste récent.
- **`PianoRoll.kt:670-671`, `:697` — `placedEarly` périmé (maintenu).** Il n'est pas remis à `null` quand les notes en pointillé disparaissent. Un second placement inattendu reste possible si elles reviennent avec la même étape et le même défilement. Très improbable.
- **`PianoRoll.kt:1359-1367` — flèche et touche ne parlent pas toujours de la même note (maintenu).**
  - Avec `.ifEmpty { left }`, une note d'une autre mesure sur une rangée visible suffit à supprimer la flèche. C'est cohérent avec « plus loin → », mais sans test.
  - La direction vient de la première note de la mesure visible ; la touche amène `left.first()`, toutes mesures confondues (déjà ainsi avant le lot).
- **`app/src/test/java/com/pocketstudio/app/tuto/TutoGrilleTest.kt:113` — adaptation honnête mais à sens unique (maintenu).** Elle couvre toujours le défaut T4 / N4. « Pas de flèche quand une note est visible » n'est vérifié que par `TutoSightHintTest`, au niveau Facile ; le niveau Pro n'a que le sens faible.
- **`app/src/test/java/com/pocketstudio/app/ui/GestureBudgetTest.kt:509` — adaptation honnête, non affaiblie (maintenu).** La coupe passe par l'action sémantique du numéro de mesure, pas par un second doigt réel ; le chemin de code est le même. Le test rougirait toujours si le `finally` de `RhythmScreen.kt:736-738` disparaissait.

Verdict : 0 bloquant, 0 grave — fusion possible
