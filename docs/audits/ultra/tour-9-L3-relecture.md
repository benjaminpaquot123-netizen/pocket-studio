# Tour 9, lot L3 (zone tuto) : relecture neuve
Relu : 4c8a5ee7d4552cfcbfab3095d3b6e211145dd692

Base : `git diff b81c56d3 4c8a5ee7` (intégration fusionnée b81c56d3). Fichiers de l'appli : `TutoController.kt`, `TutoChecks.kt`.

rouge constaté : 32e55352 : rejoué sur 47ca6a1d : `Tour9TutoNewTrackShownTest#aTrackMadeForTheStepIsShown` échoue (« techno/bass_do: the track 0 made, the screen stays where it was ([]) »), dans un worktree temporaire retiré ensuite.

Tests relancés sur la tête :
- project-model `com.pocketstudio.model.tuto.*` : vert.
- app `com.pocketstudio.app.tuto.*` : 343 tests, 0 échec, 70 sautés (les sauts connus `-Phuman.dir`, `-Pbench.out`, `-Pbench.render`).
- `verifyRoborazziDebug` sur `TutoAppScreenshotTest` : vert (2 tests joués). `tuto_app_snare.png` et `tuto_app_bass.png` regardées en entier : elles collent encore (carte « Caisse claire et clap » 1/2, cases en pointillé ; piste Basse ouverte sur ses notes en pointillé, ce que F043 fait aussi). Rien à réenregistrer.

Par défaut :
- F033 : « Je le fais » mène au bon endroit (effets de la tranche, envoi en pointillé ; pour une courbe, la zone de l'étape) et ne coche plus rien ; l'étape reste faisable à la main. Pas de plantage, pas de sauvegarde ni de son touché.
- F037 : la branche `TutoPlay.undone` ne joue que sur l'étape affichée et seulement quand son travail a disparu ; le bouton de l'étape revient.
- F043 : `tracksSeen` est remis à zéro à chaque changement d'étape et après le bouton de l'étape : pas de double navigation.
- F045 : séparateurs justes ; aucun fichier i18n touché.
- F138 : aucune courbe de l'utilisateur perdue (piste non dessinée libérée seulement au plafond, sinon rien ; boucle convertie en « Par partie » sans perte ; courbe « Morceau » gardée, seules les mesures de la partie réécrites ; ordre des pistes gardé).

Grave ouvert (non corrigé : le remède touche les textes des cours ou le choix de niveau, hors des fichiers du lot) :
- GRAVE F031 : coût pour un débutant. Neuf étapes se vérifient maintenant à la force ; au niveau Facile : detroit `hat_do` (8 coups doux), minimal `clap_do` (3), industrial_techno `kick_do` (2), speed_garage `snare_do` (2), disco `snare_do` (2), riddim `kick_do` (1) ; au niveau Pro seulement : retro_techno `hat_do` (8), boom_bap et pop `shaker_do` (4). Chaque coup doux coûte 3 gestes (appui long, « Doux », « Fermer ») : 24 gestes de plus pour le charley de detroit en Facile. Une fois toutes les cases touchées, plus aucune case n'est en pointillé, et la note dit comment rendre un coup doux mais pas lesquels : un débutant qui adoucit les mauvaises cases reste bloqué sans indice (seule sortie : « Montre-moi » ou passer). Remède à trancher par le chef : en Facile, texte sans coup doux (la phrase d'acceptation le permet : « ou le texte ne le demande plus »), ou cases à adoucir marquées et réglables en un geste.

Moyens ouverts :
- Moyen F031 : le bloc « Doux » de `TutoJourneyTest` règle la force par `vm.setHitVelocity` quand la case est hors écran ou sans menu : le chemin appui long › « Doux » n'est plus prouvé pour ces cases (même repli que pour poser un coup, accord du chef).
- Moyen F031 : `softHitsAsked` lit toute la ligne du style (`line(role)`), pas la ligne de la partie de l'étape (`partOf`) : une étape d'une partie sans coup doux pourrait demander de la douceur.
- Moyen F033 : `TutoPlay.doHere` n'a plus d'appelant dans l'appli (seuls des tests l'appellent) et le commentaire de `doIt` cite « le bouton de l'étape », alors qu'aucune étape d'envoi, d'insert ou de courbe n'en a un.
- Moyen F033 : `Tour9TutoDoItLeavesTheGestureTest` fait le geste par `TutoPlay.doHere` (modèle), pas par l'écran : rien ne prouve que le geste se fait à l'écran où « Je le fais » mène.
- Moyen F138 : dans le rapport, la ligne de chemin « courbe, toutes les pistes prises » pointe sur `Tour8TutoGesturesDoItTest#theCurvesDoItChangesOnlyTheCurveAndItsFilter`, qui ne remplit pas les pistes ; la preuve est `Tour9TutoCurveKeepsMineTest` (project-model). Et « Je le fais » ne dessine plus de courbe (F033) : F138 vaut pour « Montre-moi » et « Poser ce qui manque ».
- Moyen F043 : un changement de rôle d'une piste existante (renommée, ou ses premières notes) pendant l'étape qui la demande ouvre aussi ses notes, même sans piste neuve.

Verdict : 0 bloquant, 1 grave
