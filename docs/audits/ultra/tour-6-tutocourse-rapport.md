# Tour 6 — lot « tutocourse » : rapport

Fiche du chef : la vérification en ligne de l'intégration est rouge depuis le lot « tutofleches », sur un test du tuto qui change à chaque passage. Trouver la cause de fond, la prouver, dire si c'est l'appli ou les tests.
Branche : `eq/tour-6-tutocourse-contre`, partie de `2e29fdd7`, tête d'intégration `5025b771` fusionnée (lots « tutoordre » et « tutoordre2 » dedans). Décisions : `docs/decisions/tour-6-tutocourse.md`.
Fichier d'appli touché : `app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt` (la mécanique des regards : `init`, `look()`, `looks()`, l'en-tête et la dernière ligne de `check()`). Tests : `Tour6TutoLooksTest.kt` (neuf), `Tour6TutoPanelAlreadyTest.kt`, `Tour6TutoFlechesRollTest.kt`.

## 1. La cause de fond : une, côté appli
Le tuto « regarde » le projet pour cocher les étapes et dire ce qui manque (`TutoController.check()`). Ces regards partaient de cinq endroits (les modifications une fois posées, l'arrivée sur une étape, le bouton de l'étape, la fin de « Montre-moi », le démarrage), chacun lancé à part. Chacun lisait l'étape, la progression et le projet à son départ, travaillait hors du fil principal, puis appliquait sa réponse quand elle arrivait, **quoi qu'il se soit passé entre-temps et dans n'importe quel ordre**. Mesuré : quatre regards au travail en même temps dans un parcours ordinaire (`theLooksAreMadeOneAtATime`, attendu 1).

Sur une machine rapide les réponses arrivent avant le geste suivant et dans l'ordre : rien ne se voit. Sur une machine lente (la CI, un téléphone d'entrée de gamme, ou le premier regard d'un gros projet), une réponse sur un projet ou une étape d'avant arrive la dernière.

Les lots « tutoordre » et « tutoordre2 » ont chacun bouché une conséquence. La cause restait : d'où le test rouge différent à chaque passage.

## 2. Défauts de l'APPLI prouvés (ce qu'un débutant voit sur un téléphone lent)
Chaque test rejoue l'ordre qui fait mal, le travail des regards retenu par une porte (`LooksGate`), sans horloge ni thread : le même résultat à chaque passage. Tous rouges sur le code d'avant (`abe754e0`), verts après.

| Défaut | Ce que l'utilisateur voit | Test | Rouge sur `abe754e0` |
|---|---|---|---|
| « Copier » repris | le motif de basse est écrit, le bouton « Copier sur les mesures suivantes » apparaît puis disparaît, jusqu'à la note suivante | `anAnswerAboutTheProjectOfBeforeDoesNotTakeTheCopyButtonBack`, `theLookOfCreateTheTrackDoesNotTakeTheCopyButtonBack` | attendu `Copy(bass)`, obtenu `null` |
| L'ordre des parties jamais dit | les parties sont posées dans le désordre, aucun « Encore : … » ni « Pas dans l'ordre », jusqu'à la modification suivante | `whatTheSongLacksIsSaidWhenTheFirstLookAnswersLate` | « null » |
| « Déjà fait » perdu | une étape déjà juste à l'arrivée dit « Bravo » pour un geste jamais fait | `Tour6TutoPanelAlreadyTest#aLookBegunBeforeTheStepStillSaysAlreadyDone` | corrigé par « tutoordre » ; rouge ici contre un contrôleur sans la règle de ce lot ni la ligne de « tutoordre » |
| Les mots de l'étape montrée effacés | « Suivant » dit ce que coûte l'étape sautée, puis le texte disparaît | `anAnswerBegunOnTheStepBeforeLeavesTheWordsOfTheStepShown` | texte attendu, obtenu `null` |

Probable, pas prouvé : le « flottement après “Copier sur les mesures suivantes” » vu par un siège de contrôle du tuto (grave neuf du 13 novembre) est de la même famille (le regard de la dernière note répond après celui du bouton).

## 3. Une deuxième cause, côté tests
`Tour6TutoFlechesRollTest` écrivait une note, laissait passer 2 s de temps **virtuel** (quelques millisecondes réelles), puis regardait si « Copier » était proposé, alors que le regard travaille en temps **réel** sur un autre fil. Sur la CI la boucle allait au bout des notes : `NoSuchElementException: List is empty`, ligne 97 (CI 43449478539, puis CI 43474046159 sur le premier correctif de tuto3, `5838dd92`, où « réponse sur un projet changé = jetée » rendait ce test plus fragile encore). Ce n'est pas un défaut de l'appli : un utilisateur met plus de 250 ms entre deux notes.
`Tour6TutoPanelAlreadyTest` tenait son ordre par `Thread.sleep(500)` et dix petites pauses.

## 4. Faire tomber à volonté
- Les cinq tests à porte : rouges à chaque passage sur `abe754e0`, sur leur assertion.
- Les tests d'origine, sur `2e29fdd7`, les trois classes ensemble, `--rerun`, machine de 4 cœurs : `TutoLot5ControllerTest#aWrongOrderIsSaid` rouge **4 passages sur 9** (six ordinaires, trois avec des options de la machine Java sans effet visible) (« what is left said: not in time »).
- Sur un seul cœur (`taskset -c 0`, deux boucles qui occupent le cœur) : 0 rouge sur 1 passage sur `2e29fdd7`, et 0 sur 3 sur le code de tuto3 (`917d094d`). La course a besoin de vrais cœurs ; ralentir toute la machine ne suffit pas, il faut que le regard soit plus lent que le fil principal (première classe à froid, CI chargée).
- `Tour6TutoFlechesRollTest` : pas tombé ici sur `2e29fdd7` (0 sur 9) ; tombé sur la CI (deux passages cités au § 3).

## 5. La correction
- **Un seul regard à la fois** : `look()` demande, par un canal qui ne garde qu'une demande en attente ; `looks()`, une seule coroutine, les fait dans l'ordre.
- **Jamais une réponse périmée** : si l'étape, la progression ou le projet ont bougé pendant le travail, la réponse est jetée et le regard refait. Un projet qui a bougé est regardé une fois ses modifications posées (le délai de toute modification).
- **Un regard qui a coché est refait** : ce qui reste sur l'étape montrée est dit par un regard qui ne trouve rien de neuf. Le bloc « softSaid » du lot tuto3 (`found.isEmpty()`) reste juste tel quel.
- Le corps de `check()` n'est pas touché (les lignes de « tutoordre » et « tutoordre2 » y restent).
- Tests : `Tour6TutoPanelAlreadyTest` à la porte, sans pause ; `Tour6TutoFlechesRollTest` lit la fin du motif sur le projet (règle du cours) puis attend le bouton. Aucun test retiré ni affaibli.

## 6. Repris de la branche de tuto3 (`eq/tour-6-tutocourse`, `8beef96a`)
- Son scénario « Créer la piste Basse, puis le motif » : rejoué à la porte (`theLookOfCreateTheTrackDoesNotTakeTheCopyButtonBack`). Son test `Tour6TutoLateAnswerTest` n'est pas repris tel quel : il suppose deux regards au travail en même temps et tient par de vraies pauses.
- Sa règle « réponse sur un projet changé = jetée » : gardée, étendue à l'étape et à la progression, et suivie d'un nouveau regard.
- Le remède du test du rouleau : le même dans les deux branches.
- Pas repris : ses conditions « l'étape montrée n'est pas dans found » dans le corps de `check()` (inutiles ici, le regard étant refait).

## 7. Vérifié
- Paire : `rouge: the tutorial's looks at the project cross each other` (`abe754e0`) → `vert: the tutorial's looks at the project cross each other` (`041f19ea`). Deux tests ajoutés après la paire (`ee07394f`, `db8ba2c8`) : vus rouges sur `abe754e0` en les y rejouant, pas commités en paire.
- `Tour6TutoOrderRaceTest` (lot « tutoordre2 ») : vert sur ce code.
- Suite du tuto entière (`com.pocketstudio.app.tuto.*`, 193 tests) : verte, 8 min 35 s.
- Répétitions sur `db8ba2c8` : les cinq classes du lot seules, 3 passages verts sur 3 (20 tests chacun). **Arrêté là sur demande de Benjamin (« c'est trop long »)** : les 20 passages seuls et les 20 passages dans la suite complète demandés par la fiche ne sont PAS faits.
- Self-test du banc et rejeu `t4-26` : lancés, arrêtés avant leur résultat. PAS faits.
- CI : 43483426711 lancée sur `db8ba2c8`, en cours à l'écriture de ce rapport. Le second passage n'est pas lancé.
- Relecture neuve et pré-vol : PAS faits.
- rejeu impossible : défauts de la CI et de la mécanique, pas d'un siège ; remplacés par les tests à ordre forcé.
- Captures : aucun écran touché.

## Laissé de côté
- Demande : `app/src/test/java/com/pocketstudio/app/nav/AppHarness.kt` : les autres tests du harnais qui attendent en temps virtuel une réponse du tuto ont la fragilité du § 3 en puissance (aucun n'est tombé dans la suite du tuto). Un moyen commun d'attendre « le tuto a regardé » serait à poser là : hors de ma liste de fichiers.
- `TutoHints` (objet global) : `Tour6TutoFlechesRollTest` ne remet que `cells` et `notes` (tuto3 a `TutoHints.reset()` dans son lot).
- Le délai : quand un regard est jeté parce que le projet a bougé, le suivant attend 250 ms de calme. Voulu (pas de « Bravo » au milieu d'un trait).

tests: com.pocketstudio.app.tuto.Tour6TutoLooksTest com.pocketstudio.app.tuto.Tour6TutoOrderRaceTest com.pocketstudio.app.tuto.Tour6TutoPanelAlreadyTest com.pocketstudio.app.tuto.TutoLot5ControllerTest com.pocketstudio.app.tuto.Tour6TutoFlechesRollTest

chemin: Tuto de A à Z › house Facile › « La basse » › motif écrit à la main › « Copier sur les mesures suivantes » | test: Tour6TutoLooksTest#anAnswerAboutTheProjectOfBeforeDoesNotTakeTheCopyButtonBack
chemin: Tuto de A à Z › jump up Facile › « La basse » › « Créer la piste Basse » puis le motif › « Copier » | test: Tour6TutoLooksTest#theLookOfCreateTheTrackDoesNotTakeTheCopyButtonBack
chemin: Tuto de A à Z › jump up Facile › « Mets les parties dans l'ordre » › quatre parties posées › « Encore : … » | test: Tour6TutoLooksTest#whatTheSongLacksIsSaidWhenTheFirstLookAnswersLate
chemin: Tuto de A à Z › jump up Facile › « Mets les parties dans l'ordre » › ordre faux puis juste | test: TutoLot5ControllerTest#aWrongOrderIsSaid
chemin: Tuto de A à Z › jump up Facile › « Mets les parties dans l'ordre » › premier regard en retard | test: Tour6TutoOrderRaceTest#whatTheOrderLacksIsSaidWhenAnEarlierLookAnswersLate
chemin: Tuto de A à Z › jump up Facile › l'étape après l'ordre › « Suivant » dit ce que ça coûte | test: Tour6TutoLooksTest#anAnswerBegunOnTheStepBeforeLeavesTheWordsOfTheStepShown
chemin: Tuto de A à Z › techno Facile › étape « tempo » déjà juste à l'arrivée › « Déjà fait » (regard en retard) | test: Tour6TutoPanelAlreadyTest#aLookBegunBeforeTheStepStillSaysAlreadyDone
chemin: Tuto de A à Z › techno Facile › étape « tempo » déjà juste à l'arrivée › « Déjà fait » | test: Tour6TutoPanelAlreadyTest#aStepTheProjectHeldAlreadyIsSaidAlreadyDone
chemin: Tuto de A à Z › tous les regards d'un parcours (arrivée, modification, bouton) | test: Tour6TutoLooksTest#theLooksAreMadeOneAtATime
chemin: Rouleau › house › la basse › « Copier » sur la ligne, puis la note suivante (100 %) | test: Tour6TutoFlechesRollTest#houseBassAfterTheCopyTheRollShowsTheNextNote
chemin: Rouleau › house › la basse › « Copier » sur la ligne, puis la note suivante (130 %) | test: Tour6TutoFlechesRollTest#houseBassAfterTheCopyTheRollShowsTheNextNoteAt130
chemin: Rouleau › techno › la basse › chaque note atteinte en suivant la ligne | test: Tour6TutoFlechesRollTest#technoBassEveryNoteIsReachedByFollowingTheLine
