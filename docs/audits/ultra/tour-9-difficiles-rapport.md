# Tour 9, lot « difficiles » (problèmes difficiles) : rapport

Branche : eq/tour-9-difficiles (venue de `ccr-69119313-or4u42`), à jour de l'intégration `ccr-80af1a1b-mn0wec` (1f1d6ac15, fusion cce40f8d5).
Tout est « tenté » : aucun « corrigé » tant qu'un testeur neuf ne l'a pas constaté.

tests: *T9D09CopyOneTouchTest *T9F034DemoShowsThePartMadeTest *Coquille2SeatTest *ProjectsSafetyTest *BenchProjectsListTest

## Chemins

chemin: Tuto, étape « Copier sur les mesures suivantes » | test: T9D09CopyOneTouchTest#oneTouchOnCopyCopiesTheBassBarAndEndsTheStep
chemin: Tuto, « Montre-moi » d'une partie à créer (Structure) | test: T9F034DemoShowsThePartMadeTest#theDemoOfAPartToMakePointsWhereThePartComes
chemin: Grille de la Montée, ligne du pad (siège Coquille2) | test: Coquille2SeatTest#cymbalGridLineNamesThePadShown
chemin: Mes projets, liste lue et montrée | test: BenchProjectsListTest#theProjectsListIsReadAndShownWithoutTheBench
chemin: Mes projets, sauvegarde avant copie | test: ProjectsSafetyTest#aCopyOrAZipIsRefusedWhenTheOpenProjectCouldNotBeSaved

corrige: D09 | gravité: grave | rejeu impossible : aucun rejeu joué dans ce lot, preuve par test Compose rouge (44fc6706) puis vert (1dc12b71)
corrige: F034 | gravité: mineur
ne corrige pas: F038 : non reproduit à neuf, déjà gardé par des tests
ne corrige pas: F101 : non reproduit à neuf, déjà gardé par des tests
ne corrige pas: F124 : non reproduit à neuf, déjà gardé par des tests
ne corrige pas: F134 : non reproduit à neuf, déjà gardé par des tests
ne corrige pas: F135 : non reproduit à neuf, déjà gardé par des tests

## Ce qui change (tous « tentés »)
- D09 (grave) : un seul toucher sur « Copier sur les mesures suivantes » copie la mesure et fait avancer l'étape (`TutoChecks.copied` prend aussi les copies faites dans le même toucher). Rouge 44fc6706, vert 1dc12b71.
- F034 (cas t9-08, t9-38, t9-65, zone vide) : « Montre-moi » d'une partie à créer met le doigt sur « Ajouter : » (`TutoController.showMe`, lieu « song.make » de `GuideNav`, repli sur « song.parts » hors écran ; la basse du Drop 2 garde son écran de notes). Rouge 59adb30c, vert fc8051fd, e643f643, 80563220.
- p5 « Mes projets » bloquée : cause non prouvée ; écritures d'état de `ProjectsViewModel` rendues atomiques (b808b72f) et mouchard du banc (`HumanTester`, 00b950fa). Sans test rouge.
- `Coquille2SeatTest` (b122cb4c) : le test replie le panneau avant de lire la ligne de la grille (adaptation du test, pas de l'appli).

## Registre
Deux lignes ajoutées dans `corrections.tsv`, état « tenté » : D09 et F034 (cas de la partie à créer).

## Ce qui n'est pas vert
- Contrôle du tuto (débutant neuf) : non joué (lot du tuto : `TutoController.kt`, `TutoChecks.kt`) ; reporté en mode boost (`PS_JUGE_BOOST=1`) à la grande vérification, comme L13.
- CI complète non lancée (mode boost : la grande vérification la fera).

## Demandes (hors de mes fichiers)
Aucune.
