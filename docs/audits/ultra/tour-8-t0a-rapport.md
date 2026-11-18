# Tour 8, lot T0a « Touche ici » (zone tuto) : rapport

Branche : eq/tour-8-t0a, partie de l'intégration ccr-80af1a1b-mn0wec.
Défauts visés : a-V1 (fr, house), a-V1 ja (siège b), compteur ja, alternance La 45 / La 57 (relecture p8).

Phrase d'acceptation (mot pour mot) : « téléphone, 100 %, house leçon 3 : à chaque note demandée, la note à poser est en vue au-dessus du panneau et le rond "Touche ici" est dessus ; je la pose d'un toucher ».

Ce qui change :
- Un toucher sur « Touche ici » : si les notes en pointillé de la mesure ne tiennent pas au-dessus du panneau, les rangées se replient (plancher 5 dp) puis la vue les cadre toutes ; un second toucher ne fait plus alterner grave / aigu.
- Le nombre affiché par la ligne (« あと N 個 », toutes langues) : sans flèche, c'est le nombre de notes en pointillé en vue.
- Aucun texte touché, aucun fichier i18n.

Décisions : docs/decisions/tour-8-t0a.md.

Preuves :
- rouge puis vert : Tour8TutoTouchInSightTest (project-model : chaque ligne de chaque cours, rouleaux de téléphone avec panneau et marge `covered`) ; Tour8TutoTouchHereTest et Tour8TutoTouchHereLargeTextTest (app, 360 dp, 100 % et 130 %, Simple et Pro).
- Rejeu : aucun `cmd-*.txt` de p8b dans le dépôt ; le contrôle se fait par le siège neuf (tuto2/a fr house:3, tuto2/b ja).

tests: *Tour8TutoTouch* *Tour7TutoTouch* *Tour7TutoArrowInSight* *Tour6TutoLineGo* *Tour7TutoPanelFold* *TutoGrilleTest*

chemin: rouleau du tuto, Simple, « Touche ici » (100 % et 130 %) | test: Tour8TutoTouchHere#simpleOneTouchShowsTheBar
chemin: rouleau du tuto, Pro, « Touche ici » (100 % et 130 %) | test: Tour8TutoTouchHere#proOneTouchShowsTheBar

État : tenté, prêt pour contrôle (pas « corrigé » : un siège neuf doit le constater).
