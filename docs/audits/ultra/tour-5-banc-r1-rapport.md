# Tour 5, banc R.1 : les trois faux défauts de la recette de Benjamin

Branche `eq/tour-5-banc-r1`. Gardes dans `app/src/test/java/com/pocketstudio/app/human/BenchRecetteTest.kt`.
Commits : rouge `8f36fa3`, vert `0b1cbbc`.
Suites relancées après le vert : BenchRecetteTest 4/4, BenchSongClockTest 5/5, BenchEngineTest 4/4,
BenchPlayThenTabsTest 4/4 (plus `drive` ignoré dans chaque classe HumanTester, comme d'habitude).

## 1. tel-09 : « Jouer » en Simple (T4-16, T5-Ab-14) : **banc**

- **Cause, round 5** (banc `d3eccff`) : le moteur du banc n'armait aucun changement de partie (`sceneSwaps()` restait à 0).
  L'appli restait donc sur la partie 1 (`songIndex` 0) et `BenchPlayhead` comptait « pas depuis ▶ » plafonnés à la fin
  du morceau : compteur bloqué sur « 0:12 / 0:13 · comp. 7.4 / 7 », toujours en lecture (fin jamais armée ni jouée).
- **Cause, banc actuel** (avec le `SongClock` de `4168889`) : les parties s'enchaînent, mais `BenchPlayhead` ajoutait les pas
  depuis ▶ au début de la partie en cours : dès la 2e partie, la 1re était comptée deux fois (garde rouge : « 73 temps
  affichés, 41 joués »), puis le plafond figeait l'horloge sur le dernier temps.
- **Réparation** (`0b1cbbc`) : avec le moteur du banc présent, `BenchPlayhead` lit la position du morceau dans le moteur
  (`songStep()`, `absoluteStep()`), comme `pollLive` de l'appli lit Engine.cpp sur un téléphone.
- **Garde** : `BenchRecetteTest#simpleSongCounterFollowsTheEnginePastTheFirstPart` (Simple, assistant Techno, onglet
  « Mon morceau », gros ▶ `thumb_play`, au-delà de la 1re partie : « mes. B.t / T » de l'écran = position du moteur à un
  temps près, et dans le morceau). Rouge à `8f36fa3`, verte à `0b1cbbc`.

## 2. tel-13 : changer de partie pendant la lecture (T5-Fb-73) : **banc, déjà réparé avant R.1**

- **Cause** : au round 5 (banc `d3eccff`), `BenchEngine` ignorait la commande 40 (ARM_SCENE_SWAP). `selectScene` arme
  bien la partie choisie (`stageScene(index, 0)` → `armSceneSwap(0, leavesSong = true)`, soit 40 avec a = 0, b = 3), mais
  le moteur du banc ne faisait jamais le changement : l'appli attendait (`pending` = B), l'écran restait sur A et « Boucle »
  s'allumait sur A, exactement ce que le siège F b a vu.
- **Réparation** : déjà faite par `ffd58ce` (premier armement) puis `4168889` (`SongClock`, copie d'Engine.cpp : un
  changement attendu à la fois, sur une barre de mesure ; b = 3 quitte le morceau). Rien à changer dans R.1.
- **Garde** : `BenchRecetteTest#proPartPickedWhilePlayingTakesOver` (Pro, assistant Techno, onglet Sons, ▶, `top_scene` ›
  `scene_pick_1`, 4 s : le moteur a changé de partie et B est la partie montrée). Verte sur le banc actuel ; **rouge avec le
  `BenchEngine.kt` du round 5** (`d3eccff`, remis le temps d'un essai puis restauré, non commité) : « 0 swaps (were 0) ».
  Pas de couple rouge/vert propre à R.1 : la réparation précède la garde.

## 3. tel-16 : la démo continue après la liste (T5-Fc-31) : **pas le banc, l'appli sur un autre chemin**

- La recette tel-16 quitte la liste par « Retour » : `NewProjectPage` est retirée, son `DisposableEffect` appelle
  `stopDemoPreview()`, la démo s'arrête. Le banc fait pareil : `BenchRecetteTest#theDemoStopsWhenTheNewProjectListIsLeft`
  est verte (le banc était déjà d'accord avec le téléphone sur ce chemin).
- Le siège F c (gestes 29 à 32) n'a pas pris ce chemin : démo lancée, puis « Le beat de départ » (`starter_starter`).
  Ce choix pose seulement `pending` : l'étape « Simple ou Pro ? » s'affiche **par-dessus** la page, qui reste composée ;
  rien n'arrête la démo avant le choix du mode (`startOf` → `leave()`). Le code de l'appli garde donc la démo sous
  « Simple ou Pro ? » et la coupe à l'ouverture du projet : c'est ce que le siège a écrit (« s'arrête à l'ouverture du
  projet »). Le banc entendait juste.
- **Preuve** : `BenchRecetteTest#theDemoGoesOnUnderTheModeStepUntilAModeIsChosen` (verte : démo encore entendue sous
  l'étape du mode, arrêtée après « Simple »). Si l'appli arrête un jour la démo à cette étape, ce test passera au rouge :
  c'est voulu, il faudra alors l'inverser.
- **Verdict proposé** : T5-Fc-31 = **appli** (mineur), sur le chemin « démo › un départ › Simple ou Pro ? » ; tel-16
  ✅ ne le contredit pas (autre chemin). Correction possible côté appli : `stopDemo()` quand `pending` passe à non nul.
  Ou vérifier sur le téléphone : lancer une démo, toucher « Le beat de départ », écouter sur « Simple ou Pro ? ».
