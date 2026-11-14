# Audit ultra, tour 3 : l'équipe du banc

Zone : le banc du « testeur humain » (`app/src/test/.../human/`). Aucun fichier de l'appli touché.
Méthode : chaque point est d'abord rejoué SANS le banc (Robolectric + les view models ou l'activité de l'appli), puis sur le banc.

## 1. Exports « 3 s » et mêmes LUFS pour tout projet (A d, C a, C c) : banc, corrigé
- Preuve : `BenchEngine.exportRender` écrivait toujours 3 s d'un même son de 220 Hz, quel que soit `frames`.
- Correction : le rendu dure ce que l'appli demande (`frames` + traîne, coupée à 0,5 s quand le son s'éteint avec `autoTail`, au débit du fichier), et son son dépend du projet décrit au moteur privé (notes, volumes des voies, tempo, empreinte de toutes les commandes). Généré par deux petites tables : rapide. La cible club / streaming du tour 2 est gardée, mesurée par le mètre de l'appli sur tout le rendu.
- Tests : `BenchEngineTest.aRenderLastsWhatIsAskedAndSoundsLikeItsProject` (boucle 1 mesure 2 s contre morceau 4 mesures 8 s, LUFS différents de plus de 1 LU, même projet = même son, traîne, cibles −8 et −14 ± 0,5), `aWavIsMadeAndShared` (la durée du morceau), `OneTargetExportTest` toujours vert.

## 2. Pad Live : ▶ / ■ de ligne et « Tout couper » sans effet (C b) : banc, corrigé
- Sans le banc, l'appli est juste : `Tour3AppProofTest.padLiveRowsAndStopAllGoToTheEngine` (ligne lancée = un seul `LaunchClipRow` à la mesure, ses boucles qui jouent = arrêt, « Tout couper » = `toggleClip(-1)` ; le moteur natif est testé par `padlive_tests.inc`).
- Banc : ses pads ne suivaient que le toucher d'un pad (`toggleForTest`) ; la ligne et « Tout couper » partent au moteur, que le banc avait lâché : rien ne s'allumait ni ne s'arrêtait.
- Correction : `HumanTester.rowLaunched` (la ligne de l'appli, puis ses boucles allumées ou éteintes, un coup dit pour les sons courts) et « Tout couper » (`padlive_stop_all`, `live_stop_all`, `live_pads_full_stop`) éteint les pads du banc.
- Garde : `BenchTour3Test.padLiveRowsStartAndStopAndStopAllCutsThem`.

## 3. « Écouter depuis une partie / depuis ma prise / tout le morceau » muet, puis boucle d'1 mesure (A b, C a) : banc, corrigé
- Sans le banc : `Tour3AppProofTest.theSongPlaysFromAPartOrATakeWithAnEngine` (partie 2, « ▶ Depuis ma prise », « Tout le morceau, du début » : le morceau joue depuis la bonne partie) et `withoutAnEngineNothingPlays` (sans moteur, `playSongPart` / `playFromTake` / `playSongFrom` sortent : rien ne joue).
- Banc : il ne donnait son moteur qu'à ▶ et au guide ; ces touchers arrivaient sans moteur, puis ▶ rejouait la boucle de la partie.
- Correction : `part_play_*`, `play_from_*`, `audio_play_from_*` reçoivent le moteur (il reste tant que le morceau joue). Garde : `BenchTour3Test.theSongPlaysFromAPartAndFromItsStart`.

## 4. Gel après « ecris » dans « Nom du projet » (B a ; aussi E c, champ « Name » d'une piste) : APPLI, preuve seulement
- Sans le banc : `Tour3RenameIdleTest` (vraie activité, Mode simple, thème nuit puis studio : Mes projets › ⋮ › Renommer, toucher du doigt sur le champ). Compose ne revient jamais au repos : `AppNotIdleException` après 60 s, dès le toucher du champ (avant toute frappe), dans les deux thèmes. Le thème nuit n'y est pour rien.
- Le même `NameField` / `SelectAllTextField` seul, touché puis tapé (`Tour3NameFieldIdleTest`), revient au repos : la boucle vient du champ dans sa fenêtre de l'appli (dialogue `ScaledAlertDialog` / `RescaledWindow`, sélection de tout le texte au focus, n° 26 du tour 2). Isolé : le même `NameField` dans `ScaledAlertDialog` (thème nuit), sans le reste de l'appli, ne se repose déjà plus dès `setContent`, avant tout toucher (`Tour3NameFieldIdleTest.aNameFieldInTheRenameWindowTouchedThenTypedInRests`, `AppNotIdleException`). Suspect : `NameField` (état écrit pendant la composition, ligne 38, et sélection au focus) avec `RescaledWindow` du dialogue.
- Sur un téléphone, c'est un gel possible (ANR) : pour l'équipe de correction du tour 3. Les trois tests de preuve sont `@Ignore` (la suite reste verte) : retirer `@Ignore` une fois corrigé.
- Le banc n'est pas en cause (sa boucle `previewsWanted` ne fait qu'attendre le repos de Compose, comme le test sans banc).

Commit : voir `git log` (« Bench round 3 … »).
