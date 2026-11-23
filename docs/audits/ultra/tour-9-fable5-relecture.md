# Tour 9, lot « fable5 » : relecture neuve
Relu : be0069237

Lu : le lot ccr-56d43d5b-vfdmoc (rouge 21d11465, vert 6b644908, docs 0758fd6cf), fusionné avec origin/ccr-80af1a1b-mn0wec (docs seulement : CHEF-2H05, PLAN-APRES-ROBOT, EN-COURS, PROGRESS, DECISIONS ; aucun code). Aucun Gradle lancé (consigne de la fiche) : les rouges et verts sont ceux du rapport.

## Constats
- Appli inchangée : seul `app/src/test/.../human/Tour5BloquantsTest.kt` bouge côté code. Rien sur le thread audio, la sauvegarde ni le banc (`HumanTester.kt` intact).
- `header()` lit titre et note dans un seul `fetchSemanticsNodes()` (même arbre, même image) : la cause décrite (deux lectures, une image d'écart) est bien supprimée. Le repli « (no part header) » et la note vide gardent le comportement de `headerTitle()`/`headerNote()`.
- L'affirmation de `simplePartsPickedAfterTheGuidesListenAreEdited` est la même mot pour mot (`contains(name)`) : rien n'est sauté, désactivé ni affaibli.
- Le test neuf vérifie vraiment quelque chose : chaque lecture du toucher à l'arrivée doit nommer la partie ; la boucle fait une lecture après l'arrivée (`arrived` pris avant la lecture) ; `watched >= picks / 2` empêche un vert creux où la partie arriverait avant la première lecture. Rouge 8/8 sur le parent selon le rapport, cohérent avec le code.
- `waitForTheStartOfABar()` : `NativeEngine.playPosition()` est en doubles-croches dans la partie (doc du moteur), donc `% 16` donne bien la case de la mesure ; arrêt ou décompte (valeur négative) font échouer « it plays » avec l'écran, pas une boucle infinie ; plafond 60 s.
- Ordre des choix `2 - round % 3` (C, B, A, C…) : jamais la partie affichée, à condition que le morceau calme/lofi démarre sur A et ait trois parties (déjà supposé par le test voisin qui touche `part_pick_1` ; `part_pick_2` sinon introuvable = rouge franc, pas faux vert).

## Points mineurs (non bloquants)
- Seuils du test neuf (`watched >= picks / 2`, case < 5, 0,45 s, 1 000 lectures) : expliqués dans le KDoc et dans `docs/decisions/tour-9-fable5.md`, pas dans `docs/DECISIONS.md`. Le rapport ne prétend pas qu'ils y soient ; aucune décision de seuil citée n'y manque. Le seuil « la moitié des choix » n'a pas de ligne de décision à lui.
- Ligne 355 (test de la ligne d'attente) recolle encore `headerTitle()` et une note lue à part, mais seulement dans un message d'erreur : pas de faux rouge possible.
- Coût annoncé : environ 25 s de plus dans la classe.
- Pas de CI complète sur cette tête (c'est au chef) ; « tenté » reste juste dans `corrections.tsv`.

Verdict : 0 bloquant, 0 grave — fusion possible
