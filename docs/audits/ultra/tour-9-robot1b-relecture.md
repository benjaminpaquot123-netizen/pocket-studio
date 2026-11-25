# Tour 9 : lot robot1b (relecture neuve)
Relu : df8946446

Relecteur : sous-agent neuf, qui n'a pas écrit le code. Périmètre : le rejeu de `eq/tour-9-robot1` (e468982bd) sur l'intégration 3d0bb823e, de 3d0bb823e à df8946446. Ce qui a déjà été relu (`tour-9-robot1-relecture.md`) n'est pas relu de zéro : on cherche ce que le rejeu a pu changer.

## 1. Contenu identique
- `git diff origin/eq/tour-9-robot1 df8946446 -- app scripts` : seuls `app/build.gradle.kts`, `Tour5BloquantsTest.kt` et `scripts/ci/robolectric-jar.sh` diffèrent, et ces trois fichiers sont exactement ceux de l'intégration (aucun écart entre 3d0bb823e et df8946446 sur eux). Le contenu du lot est le même.
- Fichiers touchés par le lot : `app/src/benTest`, `app/src/test/.../bentest`, `scripts/bentest/check-apk.sh` et `docs/` seulement. Rien dans le moteur, le callback audio, `project-model` ni les écrans de l'appli.

## 2. Paires rouge / vert
- Les 9 paires (C8, C11, C10, C5, C2, C3, revue, et les 4 paires en français) ont le même sujet mot pour mot : aucun rouge ni vert orphelin.
- C8 (260f27e89 / 58602e555) : le rouge ajoute `ScenariosVraisEcransTest#aBuildUpAddsItsCurveNotAPartThenUndoTakesItBack` ; le vert porte le correctif (`Scenarios.kt`, `Robot.kt`) et nomme ce test.
- C11 (ad0800add / 51f142ca2) : le rouge ajoute `ScenariosVraisEcransTest#aPadOfTheLiveIsFoundByItsRealNameThenTheSecondOne` ; le vert porte le correctif (`Scenarios.kt`, `Robot.kt`) et nomme ce test.
- C10 (da31bab02 / d58db3f1f) : le rouge ajoute `ScenariosTest#anEffectWithNoRoomIsNotMeasuredAndOneLeftBehindFails` et `ScenariosVraisEcransTest#theEffectsScenariosLeaveTheStripsAsTheyFoundThem` ; le vert porte le correctif (`retirer()` dans `Scenarios.kt`) et nomme les deux.
- Les verts de C8 et C11 retouchent aussi le faux studio des tests (`montee`, `padTags()`) : c'est l'adaptation du double au modèle `ProjetVu` élargi, pas un affaiblissement de l'oracle (les assertions du rouge restent).

## 3. Code (ce que le rejeu a pu changer)
- Rien de neuf : le code est celui déjà relu (verdict 0 bloquant, 0 grave). Les tests vérifient bien un résultat (effets égaux à l'état d'avant, sur le disque aussi ; courbe de montée retirée par « Annuler » ; ÉCHOUÉ quand « Annuler » ne marche pas).
- Mineur : dans `retirer()`, `return@repeat` ne sort pas de la boucle, il passe au tour suivant ; sans effet sur le résultat (contrôle refait à chaque tour), juste un tour inutile.
- Mineur : les messages d'échec de C10 lisent `lu().effets[ch]` sans garde ; `ch` venant de l'état d'avant, un plantage n'est possible que si le projet perd des pistes pendant le scénario (il serait alors compté comme l'échec du scénario, avec ses indices).

## 4. Rapport et décisions
- Tous les shas du rapport existent sur la branche, après 3d0bb823e ; les 8 tests nommés existent.
- Mineur : `docs/decisions/tour-9-robot1b.md` ne marque pas C8 « (tenté) » alors que le rapport dit tout « tenté ».

Constats : 0 bloquant, 0 grave, 3 mineurs (laissés pour le tri).

Verdict : 0 bloquant, 0 grave — fusion possible
