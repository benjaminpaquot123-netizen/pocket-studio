# Tour 9, lot L3 (zone tuto) : rapport

Branche : eq/tour-9-L3, avec l'intégration ccr-80af1a1b-mn0wec fusionnée (tête ddf08286).
Défauts visés : F031, F033, F035, F037, F038, F041, F043, F045, F138.

Phrases d'acceptation (mot pour mot, `tour-9-tri/finitions.tsv`) :
- F031 : « Une étape qui demande des coups doux ou un accent ne se valide que si la force est réglée comme demandé (ou le texte ne le demande plus). »
- F033 : « « Je le fais » m'emmène au bon endroit mais me laisse faire le geste ; rien n'est coché avant que je le fasse. »
- F035 : « Après une démo, mon réglage revient comme avant, l'étape reste à faire et je garde « Je le fais ». »
- F037 : « Si j'annule la création d'une partie, l'étape redevient à faire. »
- F038 : « « Aller à la partie X » ouvre la partie X et l'étape avance. »
- F041 : « Les cases guides de l'étape suivante n'apparaissent qu'à cette étape. »
- F043 : « Après la création d'une piste par le tuto, l'écran ouvre cette piste. »
- F045 : « Le message « partie créée » est écrit entièrement dans ma langue, avec sa ponctuation. »
- F138 : « « Je le fais » garde mes courbes et l'étape se coche même quand toutes les pistes de courbe sont prises. »

Ce qui change :
- F033 : « Je le fais » d'une étape d'effet (envoi, insert, filtre) ouvre l'endroit où travailler et ne fait plus le geste ; l'étape se coche quand l'utilisateur l'a fait (remplace le choix du lot T0b du tour 8).
- F037 : une étape cochée dont le travail a disparu (partie annulée) redevient à faire.
- F043 : une piste faite pour l'étape (rôle nouveau sur une piste, même la piste 0) est ouverte à l'écran.
- F045 : la liste de ce que la partie laisse de côté prend la virgule de la langue (« 、 » en chinois et japonais, « ، » en arabe). Aucun fichier i18n touché.
- F138 : « Je le fais » d'une courbe garde les courbes de l'utilisateur, et l'étape se coche même quand toutes les pistes de courbe sont prises.

Laissés de côté :
- F031 : tenté puis retiré après la relecture (grave) : vérifier la force des coups doux coûte 3 gestes par coup en Facile (24 de plus sur le charley de detroit), sans case qui montre lesquels ; le remède sort des fichiers du lot. Au chef de trancher : texte Facile sans coups doux, ou cases marquées réglables d'un geste.
- F041 : la phrase contredit `Tour6TutoCellsPanel100Test`, qui exige les cases guides visibles sur la carte « Passer à 2 mesures » (décision du tour 6). Au chef de trancher ; le test rouge n'a pas été poussé, rien n'a changé dans l'appli.
- F035 et F038 : non reproduits. Les tests de garde `Tour9TutoAfterTheDemoTest` et `Tour9TutoGoToThePartTest` passent sans correction et restent dans le dépôt.

Tests ajustés :
- `Tour8TutoGesturesDoItTest` (suit F033, signalé au chef) : après « Je le fais », le test fait le geste lui-même, puis l'étape se coche.
- `TutoLot4Test#theReverbsSecondHalfIsTheSnaresPadAndItIsSaid` (suit F033, signalé au chef) : après le second « Je le fais », l'envoi reste à 0 ; le test règle l'envoi de la caisse, puis la note a ses deux ✓.

Captures : aucune réenregistrée. Le lot ne change ni la mise en page ni les textes des deux captures du tuto dans l'appli (`tuto_app_snare.png`, `tuto_app_bass.png`) ; `TutoAppScreenshotTest` les vérifie.

Décisions : docs/decisions/tour-9-L3.md.

Preuves : un commit « rouge: » puis « vert: » pour chaque défaut tenté (F033, F037, F043, F045, F138), dont, dans project-model, `Tour9TutoCurveKeepsMineTest#theStepTicksWithEveryCurveLaneTaken` (F138) ; tests ciblés app `com.pocketstudio.app.tuto.*` et project-model `com.pocketstudio.model.tuto.*` verts après la fusion de l'intégration b81c56d3 ; après celle de ddf08286 : project-model 220 verts, app 378 dont 2 rouges dans `TutoReadabilityBudgetTest` (12 consignes Facile de/hi hors budget, cliquet Pro 3749 > 3725), rouges à l'identique sur l'intégration ddf08286 seule (venus des textes du lot L5b, pas de ce lot) ; rejeu du self-test vert (6 oracles tenus, 0 rouge).

tests: *Tour9Tuto* *Tour8TutoGesturesDoItTest* *TutoLot4Test* *TutoJourneyTest* *Tour6TutoCellsPanel100Test* *TutoAppScreenshotTest*

chemin: tuto, étape d'envoi ou d'insert, « Je le fais » | test: Tour9TutoDoItLeavesTheGestureTest#doItGoesThereAndLeavesTheGestureToTheUser
chemin: tuto, « Créer la partie » puis Annuler | test: Tour9TutoUndoMakesItToDoAgainTest#undoingThePartMakesTheStepToDoAgain
chemin: tuto techno, étape basse, « Créer la piste » sur la piste 0 | test: Tour9TutoNewTrackShownTest#aTrackMadeForTheStepIsShown
chemin: tuto en zh, ja, ar, message « partie créée » | test: Tour9TutoPartMadeInItsLanguageTest#whatThePartLeavesOutIsListedWithTheLanguagesOwnCommas
chemin: tuto, étape de courbe, « Je le fais » avec toutes les pistes de courbe prises | test: Tour8TutoGesturesDoItTest#theCurvesDoItChangesOnlyTheCurveAndItsFilter

État : tenté (pas « corrigé » : un siège neuf doit le constater).
