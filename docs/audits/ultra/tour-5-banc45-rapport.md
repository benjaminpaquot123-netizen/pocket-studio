# Tour 5, banc 4.5 : détecteur de fuite, `reachTag` sur place, cinq gardes « banc »

Branche `eq/tour-5-banc45`. Lignes 4.5 et R.6 de `PLAN-RESTE.md`, plus les cinq verdicts « banc » sans garde
(`verifie-chantier.py`). Aucun code principal de l'appli modifié. `verdicts.tsv` non touché : les noms de gardes à y
reporter sont listés en fin de rapport (les 5 écarts de `verifie-chantier.py` tombent avec eux).

## En bref

- **4.5, le détecteur** (`BenchPristineRule`, posé devant chaque test `Bench*`, `Tour*AppProof*Test`, `Tour*IdleTest` et
  dans `HumanTester`) a nommé dès le premier passage deux fuites communes à tous les tests du banc (la fabrique de
  ViewModel d'Android gardait l'`Application` du test d'avant, la garde anti-double-toucher son dernier toucher), puis
  un export de 60 s qui continuait après son test. Corrigé dans `HumanTester.end()` et les autres `@After`.
- **Mais le renommage en quarantaine n'est pas une fuite de cet état** : rouge dans 4 passages combinés sur 5 avec tout
  l'état surveillé vierge. Le diagnostic ajouté au test dit où il casse : la liste des projets est **chargée** (1 projet,
  `loading=false` dans le ViewModel) mais **l'écran n'en montre rien** et le bouton « Plus tard » de l'accueil n'est
  jamais placé (`bounds = 0`) : les flux (`collectAsStateWithLifecycle`) ne parviennent plus aux écrans de ce test, qui
  restent sur leurs valeurs initiales. **Quarantaine maintenue**, lignes mises à jour, le message d'échec porte
  maintenant tout ce qu'il faut pour finir (voir « Le renommage »).
- **R.6** : `reachTag` glissait 8 fois (24,5 s d'horloge du banc, mesurés par la garde) pour un élément entièrement
  visible près d'un bord. Corrigé : un élément dessiné en entier est touché là où il est. Rustine de `BenchRecetteTest`
  retirée.
- **Cinq gardes « banc »** : trois cas « rien ne s'écrit après ✕ Effacer » (les testeurs écrivaient sans avoir touché le
  champ ; le banc le disait, il le dit plus clairement ; champ touché, le curseur reste après ✕ comme sur le téléphone),
  la prise micro qui ne s'arrêtait pas à la fin du morceau (le moteur du banc oubliait de clore la prise : corrigé), la
  taille des exports (le banc dit qu'elle n'est pas celle d'un téléphone).

## Par tâche

| Tâche | Cause | Correction | Garde | Commits |
|---|---|---|---|---|
| 4.5 détecteur | état hérité par les tests d'un même processus Robolectric (statiques du bac à sable) | `BenchPristine.kt` : règle JUnit devant chaque test ; vérifie LaunchRule, SettingsStore, SeparationJob, I18n, TutoTestHooks, NativeEngine.sink, Retours, Intro, TutoHints, SpotChecks, ProjectOpening.prepared, ClosingSaves, fabrique de ViewModel, BenchMic, DoubleTouchGuard, TakeVault, fils de travail tenus par l'appli (en cours ou bloqués), `projects/`, CurrentProject ; échoue en nommant le test d'avant (`lastTest`), puis remet l'état | la règle elle-même | `9499841`, `5c79ebe` |
| 4.5 fuites trouvées | `AndroidViewModelFactory._instance` (l'`Application` du test d'avant), `DoubleTouchGuard` (dernier toucher), une prise encore en écriture, l'export de 60 s de `BenchTour1Test#nothingChangesIgnoresLooksAndLongWork` (le rendu du banc ne voyait l'annulation qu'au pas suivant, 2,5 s) | `HumanTester.end()`, `BenchShareTest.end()`, `Tour3RenameIdleBase.close()` remettent l'état ; `BenchEngine` attend par tranches de 50 ms et `cancelAllRenders()` est appelé en premier dans `end()` | `BenchPristineRule` (rouge = fuite nommée) | `9499841`, `5c79ebe` |
| 4.5 quarantaine | pas une fuite de l'état surveillé ; les écrans du test ne reçoivent plus leurs flux (voir ci-dessous) | diagnostic complet dans le message d'échec de `Tour3RenameIdleBase` ; lignes de quarantaine mises à jour | — | `5c79ebe`, docs |
| R.6 `reachTag` | 8 « glisse + attends 1 » pour amener au milieu un élément déjà visible près d'un bord | `HumanTester.shownWhole(node)` : bornes dessinées (rognées par les parents) ≥ 90 % de la taille et dans l'écran → touché sur place ; glissements gardés pour un élément coupé ou hors écran | `BenchSelfTest#reachTagTouchesAVisibleElementInPlace` (rouge : 24 545 ms ; vert : < 300 ms) | rouge `d503254`, vert `87b6693` |
| T5-Dc-4, T5-Ac-61, T5-Db-16 | journaux des sièges (A c #60-62, D b #16-18, D c #4-6) : ✕ Effacer touché **sans jamais toucher le champ**, puis « ecris » ; aucun champ n'a le curseur (la fenêtre ne le donne à personne : pas de `FocusRequester` dans ces dialogues, un téléphone n'afficherait pas de clavier) ; le banc le disait (« touche d'abord le champ ») mais les testeurs l'ont pris pour un défaut. Le champ touché, puis ✕, puis « ecris » marche sur le banc (siège A c #50-53 l'a fait) comme sur le téléphone (recette tel-01) | message du banc reformulé : « Rien ne s'écrit : aucun champ n'a le curseur (un téléphone n'afficherait pas de clavier) : touche d'abord le champ, puis écris. » | `BenchTour4Test#theClearButtonKeepsTheFieldsFocus` (les deux chemins : à l'aveugle = dit et rien d'écrit ; champ touché = le curseur reste après ✕, le nom s'écrit) | `87b6693` (vert d'emblée : le banc avait raison) |
| T5-Ab-27 | `BenchEngine.SongClock` : la fin du morceau (swap de type 2) arrêtait le transport sans geler la prise micro : `micStatus` repassait à « attend le 1 », 0 image ; le moteur natif clôt la prise sur la dernière ligne de mesure et l'appli la garde (`songEnded`) | `freezeTakes(upTo = images jusqu'à la ligne de mesure)` à la fin du morceau | `BenchSongClockTest#aMicTakeEndsWithTheSongsEnd` (rouge : état 0 ; vert : état 2, 4 s) | rouge `d503254`, vert `87b6693` |
| T5-Cc-41b | l'encodeur du banc est un shadow qui recopie ce qu'on lui donne, son moteur un remplaçant : taille et durée d'un export n'ont rien d'un téléphone (42 Mo pour 2 min 26 sur le téléphone) ; le banc ne le disait pas | ligne de son du banc : au menu de partage (`HumanTester.shareNote`, taille en Ko) et quand « Mes exports » est ouvert ou qu'un export tourne : « (banc : taille et durée d'un export non représentatives, encodeur et moteur du banc ; un téléphone fait bien plus gros) ». Reste de 4.7 (profil de l'export, `seat-metrics.py`) non fait | `BenchEngineTest#anExportSizeIsSaidToBeTheBenchs` | rouge `d503254`, vert `87b6693` |
| selftest.sh | `Tour3NameFieldIdleTest` et `Tour5AppProof2Test` (nom en `AppProof2Test`) n'étaient ni attendus ni lancés | motifs `Tour*IdleTest` et `Tour*AppProof[0-9]*Test` ; `test_ultra_bench.py` vert | `selftest-attendus.txt` les liste | `5c79ebe` |

## Le renommage (`Tour3RenameIdleTest`, `Tour3StudioRenameIdleTest`)

Ce que les passages ont montré, dans l'ordre :

1. Passage A (code de la séance d'avant, détecteur posé) : 97 verts, aucun état hérité non vierge, et le renommage Nuit
   rouge quand même (« no project's ⋮ », les tags montrent le studio, l'accueil et la fenêtre « Mes projets » sans
   aucune ligne de projet).
2. Passages 1 et 2 (diagnostic ajouté) : `ProjectsViewModel.state` = `loading=false`, 1 projet, même `Application` que
   le test, projet courant et sauvegardé identiques, `guideSeen=false`, aucun fil de l'appli sur les ouvriers de
   coroutines. Donc **l'appli a bien la liste ; l'écran ne la dessine pas**.
3. Passage 4 (diagnostic étendu) : trois fenêtres de 588 × 1278 toutes posées (`laidOut`), la colonne `projects` placée en
   552 × 1242, `top_projects` placé, **`welcome_later` taille 388 × 72 mais bornes (0, 0, 0, 0)**, horloge du looper en
   pause normale, Choreographer non en pause, thème Nuit ou Studio indifférent, `uiScale` 1. Ni `projects_empty` ni les
   lignes : la fenêtre « Mes projets » est restée sur `ProjectsUi()` initial (`loading=true`) → les états collectés par
   `collectAsStateWithLifecycle` n'ont pas atteint la composition. Dans un passage vert, le même test renomme en 160 à
   460 ms.
4. Le message d'échec porte maintenant aussi : les recomposeurs en cours (`hasPendingWork`), `mainClock`
   (`autoAdvance`, temps), `Dispatchers.Main`, l'état du scénario, et **un essai de 3 images forcées à la main** suivi d'un
   nouveau décompte des ⋮ et de `projects_empty` : si les lignes apparaissent alors, c'est l'horloge d'images du test qui
   ne tourne pas pendant `waitUntil` ; sinon, c'est la collecte des flux qui n'a jamais démarré (effets lancés jamais
   dispatchés).

Pistes à trancher avec ce message (séance suivante, 1 h) : un `Recomposer` de fenêtre créé par la fabrique d'un
environnement de test précédent (statique `WindowRecomposerPolicy`), ou une tâche du test d'avant posée tard sur le
looper principal partagé. Hors du champ du détecteur d'état : à ajouter au détecteur une fois nommée.

## Les passages combinés (`Bench*` + `Tour*AppProofTest` + `Tour*RenameIdleTest`, un Gradle chacun)

| Passage | Code | Résultat | Renommage Nuit / Studio |
|---|---|---|---|
| A | détecteur de la séance d'avant | 97 verts, 1 rouge (`BenchAmeneesTest#aTapDuringTheExampleIsNotCounted`, défi : `YourTurn(c, 0)` attendu, non revu depuis), 1 quarantaine rouge, 40 sautés | rouge / vert |
| 1 | + gardes, correctifs R.6/Ab-27/Cc-41b, détecteur étendu, diagnostic | 101 verts, 1 rouge (le détecteur nomme l'export de 60 s de `BenchTour1Test`), 2 quarantaine rouges, 40 sautés | rouge / rouge |
| 2 | idem | 102 verts, 0 rouge, 1 quarantaine rouge, 41 sautés (BenchSelfTest#drive) | rouge / vert |
| 3 | + export annulé en fin de test | 103 verts, 0 rouge, 0 quarantaine rouge | vert / vert |
| 4 | idem | 101 verts, 0 rouge, 2 quarantaine rouges | rouge / rouge |
| 5 | + diagnostic images/recomposeurs ; filtre `Tour*AppProof*Test` (Tour5AppProof2Test inclus, 4 verts) | 107 verts, 0 rouge, 0 quarantaine rouge | vert / vert — mais Gradle a changé l'ordre avec ce filtre : le renommage Nuit a tourné **en premier** (JVM froide, lancement 3,3 s), ce passage ne dit rien du « milieu des autres » |

Le critère « 3 passages verts de suite » n'est pas atteint (rouge au passage 4 entre deux verts) : la quarantaine reste.
Toutes les gardes neuves sont vertes dans chaque passage (1 à 5). Pour reproduire le rouge, garder le filtre d'origine
(`Bench*` + `Tour*AppProofTest` + `Tour*RenameIdleTest`) : avec lui, Gradle place les deux tests de renommage après
`BenchTour4Test` et `Tour3AppProofTest`, dans une JVM chaude (lancement de l'activité en 70 à 160 ms).

## Gardes à reporter dans `verdicts.tsv` (colonne `garde_banc`)

- T5-Dc-4, T5-Ac-61, T5-Db-16 : `BenchTour4Test#theClearButtonKeepsTheFieldsFocus`
- T5-Ab-27 : `BenchSongClockTest#aMicTakeEndsWithTheSongsEnd`
- T5-Cc-41b : `BenchEngineTest#anExportSizeIsSaidToBeTheBenchs`
