# Tour 5, banc R.1 : relecture critique (relecteur neuf)
Relu : 7e5f766

Périmètre : `8f36fa3` (gardes rouges), `0b1cbbc` (réparation de `BenchPlayhead`), `7e5f766` (rapport), plus les verdicts
du même lot (`verdicts.tsv`, `hors-banc.tsv`). Essais locaux faits puis annulés par `git checkout` (rien de commité à part ce fichier),
tests lancés uniquement par `scripts/bench/gradle-file.sh`.

Ce qui a été vérifié :
- **tel-09** : au commit actuel, `BenchRecetteTest` 4/4 vertes, `BenchEngineTest` 4/4, `BenchSongClockTest` 5/5. Avec le
  `BenchPlayhead.kt` de `8f36fa3` remis sur `0b1cbbc`, la garde `simpleSongCounterFollowsTheEnginePastTheFirstPart` passe
  au rouge (« beat 73 shown, 41 played »). Le rouge/vert est réel. La réparation suit le téléphone : `SongClock` de l'écran
  lit `live.songStep` (AppRoot l. 2703 / 4361), que `pollLive` remplit avec `NativeEngine.songStep()`, et `pollPosition`
  calcule `playBar` avec `songStep / stepsPerBar`. Le banc écrit maintenant les mêmes valeurs depuis le même moteur. Ce n'est
  pas un bricolage qui masquerait l'appli.
- **tel-13** : la garde `proPartPickedWhilePlayingTakesOver` n'est pas verte d'office : sans les touches `top_scene` /
  `scene_pick_1`, elle passe au rouge (« 0 swaps (were 0) »). Le morceau ne change donc pas de partie tout seul pendant la
  fenêtre de 4 s. `selectScene` ne bascule `project.scene` qu'après le changement fait par le moteur (`pending`).
- **tel-16 / T5-Fc-31** : à la lecture du code, le rapport a raison. `starter_starter` ne fait que `pending = …`
  (`ask`), et `ModeStep` est dessiné par-dessus la page sans la retirer. Le `DisposableEffect` qui appelle `stopDemo()`
  ne s'exécute donc pas avant `startOf → leave()` ou la fermeture de l'écran.
  Les deux gardes ont été testées par mutation, en retirant `actions.stopDemo()` du `DisposableEffect` de `NewProjectPage` :
  **elles restent vertes toutes les deux** (voir G1).

| Gravité | Constat | Suite |
|---|---|---|
| Grave | **G1. Les gardes de « la démo s'arrête » ne vérifient rien.** Avec `stopDemo()` retiré de `NewProjectPage`, `theDemoStopsWhenTheNewProjectListIsLeft` et la 2e assertion de `theDemoGoesOnUnderTheModeStepUntilAModeIsChosen` restent vertes. Une sonde (trace de `PcmPlayer.stop`) montre que la démo s'arrête via `BenchAudioTrack.onMarkerReached`, donc parce qu'elle arrive à sa fin. Mesuré sur l'horloge du banc : la démo dure 15,1 s, et la touche « Retour » vers l'accueil prend **26,4 s** d'horloge (`settle` / attente du travail de fond). Toute démo est donc finie quand le test la cherche, que l'appli l'arrête ou non. Les phrases du rapport « le banc était déjà d'accord avec le téléphone sur ce chemin » et « arrêtée après Simple » ne sont pas prouvées par ces tests. Le verdict T5-Fc-31 = appli reste valable : il repose sur le code et sur la 1re assertion de 3b, qui elle compte. | Réécrire les deux assertions « arrêtée » pour qu'elles ne dépendent pas de la fin naturelle de la démo : la vérifier juste après le geste avec une écoute plus longue que le geste (démo en boucle sur le banc), ou relever un appel à `stopDemoPreview` (espion sur `ProjectsActions` / compteur dans le VM). Refaire la mutation : les deux doivent passer au rouge. |
| Moyen | **M1. `verdicts.tsv` contredit le rapport sur T5-Fc-31.** La ligne dit `banc` (« RECETTE tel-16 ✅ … garde R.1 »). Le rapport R.1 conclut **appli (mineur)** sur le chemin « démo › un départ › Simple ou Pro ? », et dit que tel-16 a pris un autre chemin. | Passer T5-Fc-31 à `appli` (mineur) avec renvoi au rapport R.1, ou le laisser `non-tranché` en attendant l'essai sur téléphone proposé. |
| Moyen | **M2. Pas de garde pour « au-delà de la fin » (T5-Ab-14, 2e moitié).** La garde de tel-09 s'arrête juste après le début de la 2e partie. Rien ne joue le morceau Simple jusqu'au bout pour vérifier que la fin est armée (b = 2), que la lecture s'arrête et que le compteur ne dépasse jamais « T ». `BenchSongClockTest#theSongsEndStopsTheTransportOnItsBarLine` couvre seulement le moteur du banc, pas l'appli qui arme la fin. | Ajouter une garde : morceau court (ou tempo élevé) joué jusqu'au bout depuis `thumb_play`, avec à chaque échantillon `beat < total × temps par mesure`, puis lecture arrêtée. |
| Mineur | **m1. L'ancien calcul reste dans la branche « sans moteur ».** Elle compte toujours `songStepOf(songIndex) + pas depuis ▶`, donc compte deux fois les parties dès la 2e. Aujourd'hui on n'y passe pas pendant un morceau : `ENGINE_ALWAYS` vaut « toujours » par défaut, et en mode `liste` le moteur reste tant que le morceau joue (l. 829). On peut y passer avec `-Dps.human.moteur=liste` hors de ces cas, et `BenchEngineTest#thePlayHeadMovesWithTheTempo` n'utilise qu'une partie. | Supprimer cette branche pour le morceau (rien plutôt qu'un faux compteur), ou la corriger comme la branche moteur. Noter dans PLAN-RESTE. |
| Mineur | **m2. `currentStep` ne vient pas du même endroit que sur le téléphone.** Le téléphone lit `NativeEngine.currentStep()`. Le banc calcule `songStep − songStepOf(songIndex)` avec le `songIndex` de l'interface (`BenchEngine` ne fournit pas `currentStep`). Autour d'un changement de partie, l'écart peut aller jusqu'à un tick. | Faire répondre `BenchEngine.currentStep()` (position dans la partie de `SongClock`) et le lire, comme `pollLive`. |
| Mineur | **m3. La garde de tel-13 ne vérifie pas que le moteur joue bien B.** Elle vérifie qu'il y a eu au moins un changement de partie et que l'écran montre B. Elle ne vérifie ni la partie que le moteur joue, ni la sortie du morceau (b = 3), ni le « Boucle » allumé sur A que le siège F b a vu. Le message « shown and heard » en dit plus que ce qui est testé. | Ajouter la partie jouée par le moteur (`SongClock`, hors morceau) et l'état de « ⟳ Partie », ou corriger le message. |
| Mineur | **m4. La garde de tel-09 compare le moteur du banc à lui-même.** Une fois réparée, l'écran et la référence lisent la même source. C'est voulu, pour vérifier le câblage (le rouge à `8f36fa3` le montre). La fidélité de `SongClock` à Engine.cpp reste couverte par `BenchSongClockTest` seulement. | Rien à faire, à garder en tête : cette garde ne prouve pas que le moteur du banc est juste. |
| Info | **i1. Le banc se fige longtemps sur certaines touches.** Une simple touche « Retour » avance l'horloge de 26 s (attente du travail de fond). Pour un testeur IA, tout ce qui dépend du temps (démo, lecture, compteurs) peut sauter d'un coup. C'est un risque de faux défauts du même genre que ceux de cette recette. Antérieur à R.1. | Mesurer l'avance d'horloge par geste dans le journal du banc et l'afficher au testeur quand elle dépasse 2 s. Point pour PLAN-RESTE. |
| Info | **i2. Hygiène correcte.** `NativeEngine.sink` est remis par `HumanTester` / `BenchEngineTest@After`. `waitFor` n'utilise l'horloge réelle que comme délai maximal. Aucune nouvelle pause réelle (les `Thread.sleep(40)` de `settle` existaient déjà). `assumeTrue` évite de lancer les tests en même temps que des sièges. Les quatre gardes contiennent de vraies assertions (sauf G1). Pas d'instabilité vue sur les 5 exécutions de ces gardes faites pendant la relecture. | Rien. |

Verdict : 0 bloquant, 1 grave — fusion refusée
