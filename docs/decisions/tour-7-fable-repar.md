# Décisions du lot fable-repar (tour 7)

- Une seule façon de jouer une étape : `TutoPlay` (modèle), appelé par l'appli (`TutoController`) et par le test « Montre-moi partout ». Le test ne peut donc pas jouer une étape autrement que l'appli. Retour arrière : remettre les appels directs à `TutoChecks` dans `TutoController`.
- « Copier » compte comme le geste de l'étape (sa consigne le demande) ; créer une partie, nommer, donner les durées, créer une piste se font en entier par leur bouton (`TutoPlay.asksAGesture`). Retour arrière : `asksAGesture`.
- Arrivées B et b (boucle faite, aucune partie) : jouées seulement à partir du premier chapitre qui crée une partie ; les chapitres de la boucle sont déjà faits dans ce projet. Retour arrière : `opened(fromParts)` du test.
- R-C : ce qui manque des chapitres d'avant est offert par un bouton unique (« Poser ce qui manque », `TutoNeed.Course`), qui passe avant le bouton propre de l'étape ; il pose le Groove d'abord, puis les parties. Retour arrière : `TutoChecks.need` sans la branche `Course`.
- R-B : une partie créée par le cours garde ses coups de fin de mesure, son roulement à faire et n'a pas de crash ; c'est l'étape suivante qui les demande. Exceptions voulues : le Break de `retro_techno` garde ce que son étape garde ; le silence de `synthwave` n'existe qu'en Pro. Retour arrière : `TutoChecks.rawPart`.
- R-A : le niveau du cours (Facile ou Pro) est passé partout (bouton, « Montre-moi », ce qui manque). Sans lui, un cours Pro rejouait les chapitres Facile et cochait des étapes à l'arrivée. `level = null` garde l'ancien comportement. Les tests voisins reçoivent le niveau dans le même commit vert. Retour arrière : retirer le paramètre `level`.
- R-A : « Montre-moi » d'une ligne de batterie laisse les coups doux et les triolets à l'étape suivante qui les apprend ; la démonstration de basse s'arrête à la longueur de l'étape ; une piste neuve ne reçoit pas l'effet (pompe) qu'une étape plus loin apprend à poser. Retour arrière : `leftToLater`, `insertsLeft`, filtre de la démonstration (`TutoChecks.demo`).
- R-D : rien de l'étape n'est dessiné hors de sa partie (pointillé, croix), même avant que le panneau ait relu le projet ; la règle est dans `TutoPlay` (`hint`, `toErase`), pas dans l'écran. Retour arrière : retirer `inItsPart` de ces deux fonctions.
- La validité des cours reste prouvée par les tests (`TutoAllCoursesTest`, « Montre-moi partout »), pas par de nouvelles règles dans `TutoValidation`. Retour arrière : aucune.
- Guillemets doublés en coréen : déjà corrigés par le lot tuto3 (49561114) ; rien à refaire ici.
- La ligne accordée par le chef dans `AppRoot.kt` n'a pas servi : rien n'est touché hors de la liste de fichiers.
- Rouge de R-C vu dans une copie de travail à part (worktree), pour ne pas mêler le test aux corrections en cours.
- Commits : seule la ligne `Claude-Session` en pied ; pas de nom de modèle (interdit de la fiche, dépôt public).
- Outils de la machine : SDK Android installé et `local.properties` écrit sur place, jamais ajoutés au dépôt.
