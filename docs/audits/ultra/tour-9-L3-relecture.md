# Tour 9, lot L3 (zone tuto) : relecture neuve
Relu : 62436d22a447c87bee306c541f9a77aad74d8349

Base : `git diff origin/ccr-80af1a1b-mn0wec 62436d22 -- app/src project-model/src` (intégration ddf08286 fusionnée). Fichiers de l'appli touchés : `TutoController.kt`, `TutoChecks.kt` seulement, dans la liste permise. Tests du lot : `Tour9Tuto*` (app et project-model), `Tour8TutoGesturesDoItTest`, `TutoLot4Test`. Aucune capture, aucun fichier i18n, aucun cours, rien du moteur ni du thread audio.

Retrait de F031 (revert e7c795c5, commentaire b871a524) : complet. Plus aucune trace de `softHitsAsked`, de `Tour9TutoSoftWordsTest` ni de « F031 » dans `app/src`, `project-model/src` et `scripts` ; `TutoJourneyTest` est identique à celui de l'intégration (bloc « Doux » revenu) ; aucun test rouge orphelin. Le commentaire de `TutoController.doIt` ne cite plus de bouton d'étape.

rouge constaté : 094c2f1a : rejoué sur bde998b3 : `Tour9TutoCurveKeepsMineTest#theStepTicksWithEveryCurveLaneTaken` échoue (« techno/filter: nothing done with every lane taken ») et `myOwnCurveOnTheSameKnobIsKept` aussi (« techno/filter: my loop curve is gone from the other parts »), dans un worktree détaché du scratchpad, retiré ensuite.

Tests relancés sur la tête (`scripts/bench/gradle-file.sh`) :
- project-model `com.pocketstudio.model.tuto.*` : 220 tests, 0 échec.
- app `com.pocketstudio.app.tuto.*` : 378 tests, 2 échecs, 70 sautés (les sauts connus `-Phuman.dir`, `-Pbench.out`, `-Pbench.render`). Les 2 échecs sont dans `TutoReadabilityBudgetTest` : `everyFacileConsigneFitsOrIsAKnownException` (12 consignes Facile de/hi hors budget) et `proOutOfBudgetOnlyGoesDown` (3749 > 3725).
- Hérités de l'intégration, vérifié moi-même : `TutoReadabilityBudgetTest` rejoué sur ddf08286 seule (worktree détaché, retiré) : les deux mêmes rouges, mêmes chiffres (12 consignes, 3749 > 3725). Le lot ne touche aucun texte de consigne. Ils ne comptent pas contre ce lot.

Captures : `tuto_app_snare.png` et `tuto_app_bass.png` regardées en entier ; non modifiées par le lot et toujours justes (carte « Caisse claire et clap » 1/2 avec ses deux cases en pointillé ; piste Basse ouverte sur ses notes en pointillé, ce que F043 fait aussi). Rien à réenregistrer.

Par défaut (phrases d'acceptation du rapport) :
- F033 : tenue. « Je le fais » n'émet plus que le déplacement (`goFor`) et replie le panneau ; le projet ne bouge pas et rien n'est coché (`Tour9TutoDoItLeavesTheGestureTest`, `Tour8TutoGesturesDoItTest`, `TutoLot4Test`). Le geste reste court pour un débutant (curseur d'envoi, case d'insert sur la tranche ouverte). Seuls autres appelants de `doIt` : `TutoTest`, `TutoJourneyTest` et `Tour5BloquantsTest` (tempo, gamme, swing : pas touchés).
- F037 : tenue. La branche `TutoPlay.undone` ne joue que sur l'étape affichée et cochée ; elle enlève la coche du progrès du tuto (`save`), jamais le projet ; « Créer la partie » revient (test vert sur techno et jungle). `undone` ne dépend pas de la mesure de départ pour fill, gap et crash : pas de va-et-vient coche / décoche.
- F043 : tenue. `tracksSeen` est remis à zéro à l'arrivée sur une étape et après son bouton : pas de double déplacement.
- F045 : tenue. Séparateur de la langue (« 、 » zh/ja, « ، » ar) dans les quatre listes de `partMade` et dans `laid` ; aucun texte nouveau.
- F138 : tenue pour « Montre-moi » et « Poser ce qui manque » : piste vide libérée seulement au plafond, aucune courbe dessinée perdue, boucle passée « Par partie » sans perte, courbe « Morceau » gardée hors des mesures de la partie, ordre des pistes gardé.

Plantages : aucun chemin neuf qui lève (les `?: return null` restent des sorties propres). Sauvegarde : le projet n'est changé que par les courbes de F138, qui gardent celles de l'utilisateur. Son : rien de touché.

Moyens laissés ouverts :
- Moyen F138 : une courbe « Morceau » à un point par temps (`songPerBeat`, celles de l'assistant de départ) sur le même filtre est encore remplacée par la courbe de l'étape (comme avant le lot) ; de même une boucle dessinée quand la partie de l'étape n'existe pas.
- Moyen F138 : dans le rapport, la ligne de chemin « courbe, toutes les pistes prises » pointe sur `Tour8TutoGesturesDoItTest#theCurvesDoItChangesOnlyTheCurveAndItsFilter`, qui ne remplit pas les pistes ; la preuve est `Tour9TutoCurveKeepsMineTest` (project-model).
- Moyen F033 : `TutoPlay.doHere` n'a plus d'appelant dans l'appli (seuls des tests l'appellent), et `Tour9TutoDoItLeavesTheGestureTest` fait le geste par le modèle, pas par l'écran où « Je le fais » mène.
- Moyen F043 : un changement de rôle d'une piste existante (renommée, ou ses premières notes) pendant l'étape qui la demande ouvre aussi ses notes, même sans piste neuve.

Verdict : 0 bloquant, 0 grave
