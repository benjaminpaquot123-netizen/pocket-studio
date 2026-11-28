# Décisions du lot T0b (tour 8, « tuto-gestes »)

- « Je le fais » d'une étape d'effet fait le geste (envoi, insert, courbe) au lieu d'ouvrir seulement la feuille. Raison : siège e, ch.13, l'envoi restait à 0 %. Une tranche par toucher : le toucher suivant passe à la tranche suivante. Retour arrière : retirer l'appel à `TutoPlay.doHere` dans `TutoController.doIt`.
- L'outil de la grille revient à « Coups » au début d'une étape qui demande des cases ou des croix. Raison : siège e, ch.12, un toucher faisait ×2. Retour arrière : retirer le `LaunchedEffect` sur `TutoHints.step` dans `RhythmScreen.kt`.
- La Montée se place juste avant le Drop 2. Retour arrière : la règle de placement dans `TutoChecks.kt`.
- Une partie vide n'est plus dite absente (`TutoUi.partEmpty`) ; son bouton reste et la remplit. Retour arrière : retirer `ui.partEmpty` du `takeUnless` de `TutoPanel.ActionBar`.
- Morceau d'exemple : sa Montée moins dense est voulue (c'est une montée). live-bench : 0 alerte, creux brefs à −16,2 dB puis montée jusqu'à −9,5 dB. Rien changé.
- `ui/PlayStrip.kt` l. 321 (« A mesure 1/16 ») : position de lecture, hors zone tuto ; rien changé, au chef de décider d'un lot.
- TutoJourneyTest (accord du chef) : quand « Je le fais » a déjà fait l'étape, le test vérifie qu'elle est faite au lieu de la rejouer. Retour arrière : remettre `assertNotNull(show)`.
