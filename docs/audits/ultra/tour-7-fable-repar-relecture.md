# Tour 7, lot « fable-repar » : relecture neuve
Relu : 209bb5d9

Portée : `git diff 13dcec5f..HEAD` sur `app/.../tuto`, `app/.../synth/PianoRoll.kt`, `project-model/.../model/tuto`, `project-model/src/main/resources` (tutos, i18n) et leurs tests. Les fichiers du lot banc8b (human/, scripts/bench/, rejeux) ne sont pas relus ici.
Relecteur : sous-agent neuf, qui n'a pas écrit le code. Lecture seule : **aucun test relancé** (une vérification Gradle tournait, le dossier de build ne devait pas être dérangé). Les couleurs des tests sont donc celles du rapport du lot, pas les miennes.

## Grave
Rien de grave trouvé : pas de mauvais bouton, pas d'étape impossible à cocher, pas de plantage, pas de perte de projet repérés à la lecture.

## Moyen
- **M1, calcul lourd sur le fil de l'écran.** `TutoChecks.need` (`project-model/.../tuto/TutoChecks.kt` ~l.2256-2265) est devenu coûteux : `helps` rejoue `fulfil` + `sanitized`, la branche sans bouton propre fabrique un `demo(...)` entier juste pour savoir s'il existe (~l.2261), et `advances` / `withEarlier` rejouent toutes les étapes d'avant (jusqu'à 3 passes de 14 touches). Or il est appelé sur le fil principal dans `TutoController.save()` (`app/.../tuto/TutoController.kt` l.807) et deux fois dans `fulfilNeed` (l.554 et l.573). Pour l'utilisateur : un à-coup possible en changeant d'étape sur un téléphone lent. Non mesuré. Correction proposée : `show(...) != null` au lieu de `demo`, calcul hors du fil principal (comme `check()` l.873-876) ou résultat gardé en mémoire ; une mesure avant de trancher.
- **M2, « Poser ce qui manque » ne dit pas ce qu'il a posé.** `TutoNeed.Course` (`withEarlier`, `TutoChecks.kt`) réécrit en une touche les lignes du Groove de l'utilisateur (le `show` d'une étape `lane:` remplace les coups de la ligne). C'est annulable (un seul `edit`), mais aucun mot ne dit ce qui a été posé : le message `made` n'existe que pour une partie (`TutoController.kt` l.563). Pour l'utilisateur : ses coups changent sans explication. Correction proposée : une phrase « Posé : … » comme pour « Créer la partie », ou la liste dans le texte avant la touche.
- **M3, une vérification retirée.** `app/src/test/.../tuto/Tour7TutoPartMadeTest.kt` : la ligne `assertFalse("« $made » names what the Groove does not hold", "Basse" in made || "Clap" in made)` a été supprimée. Depuis R-C le Groove pauvre est d'abord complété, donc la phrase peut nommer la basse à bon droit ; mais plus rien ne prouve que « Dedans : … » ne nomme que ce que la partie contient. Correction proposée : comparer les noms de la phrase aux pistes non vides de la partie créée.
- **M4, une branche de test qui ne peut pas rougir.** `project-model/src/test/.../tuto/TutoShowMeEverywhereTest.kt`, `hintFault` : le cas « dotted cells… not the step's part » est inatteignable, car `TutoPlay.hint` et `toErase` rendent déjà `null` par le même `inItsPart`. La règle R-D n'est donc prouvée que par `Tour7TutoDottedInItsPartTest` (lui est réel, avec son garde « the test checks nothing »), sur techno / Facile seulement, crash et silence. Correction proposée : ôter la branche morte ou l'appuyer sur un calcul indépendant ; étendre le test R-D à deux autres styles.

## Mineur
- `TutoRollHint.brought` (`TutoChecks.kt` ~l.2716-2733) : quand les notes cachées ne tiennent pas et que la note demandée est déjà au bord, le déplacement vaut 0 ; la pastille « Touche ici » et la flèche restent (`PianoRoll.kt` l.1480) mais la touche ne fait rien. Proposition : avancer d'au moins une rangée, ou cacher la pastille.
- `PianoRoll.kt` l.789-792 : `lit` est calculé avant le `coerceIn` du défilement ; au bord du clavier, la note allumée peut rester hors de vue.
- `TutoController.canShow()` (l.654) lit `show`, alors que `need` et `TutoPlay.play` lisent `demo` : les deux peuvent diverger quand `demo` rend `null`.
- `TutoChecks.kt` l.2273 : `private fun changes` n'est plus appelée ; le commentaire « The chapters of the level that holds [step] » est posé au-dessus des constantes `LATER_SOFT` au lieu de `chaptersOf`.
- `TutoPanel.kt` l.976-992 : le bloc Course ajoute un texte et un lien de 48 dp à la barre d'action ; panneau plus haut sur petit écran (à regarder sur capture).
- `Tour7TutoMissingOfferedTest` joue `TutoPlay.play(s, p, target)` sans le niveau, alors qu'il parcourt les niveaux ; les trois autres tests R-A / R-B le passent.
- `Tour6TutoFlechesRollTest`, `TutoSightHintTest` : l'attente « aucune flèche » est devenue « "à toucher" présent, ni →, ni ←, ni "plus" » ; voulu (flèche nue = une note de plus), mais ↑ / ↓ ne sont plus contrôlées dans ces cas.
- `TutoShowMeEverywhereTest` : le plancher `played >= MIN_COURSES` (37) est bas.

## Vérifié sans défaut
- `TutoHints.focusTouched` (`TutoPanel.kt` l.167 et l.213, `PianoRoll.kt` l.776-795 et l.1462) : posé juste après `++focus`, lu ensuite, remis à -1 ; « Je le fais » et « Copier » ne déplacent pas le clavier (`touched = false`, couvert par `Tour7TutoHiddenNoteTest`).
- « Réduire » (`TutoPanel.kt` l.674 et l.731-742, tag `tuto_text_less`) : montré seulement quand la place manque et que le texte est déplié ; `Tour7TutoReduceTest` est réel.
- i18n : « Réduire », « Poser ce qui manque », « Il te manque ce que font les chapitres d'avant. », « Aller au chapitre %d » présents dans les 14 `i18n/*/tuto.json`, JSON valide, pas de clé en double.
- Tests R-A, R-B, R-C (`Tour7TutoNextStepLeftToDoTest`, `Tour7TutoPartNeverBornDoneTest`, `Tour7TutoMissingOfferedTest`), `Tour7TutoHiddenNoteTest` : ils jouent tous les styles et niveaux par `TutoPlay`, avec un plancher de cas joués ; ils vérifient vraiment quelque chose.
- Périmètre : sous `app/src/main` et `project-model/src/main`, seuls `tuto/*`, `PianoRoll.kt`, `model/tuto/*`, `i18n/*/tuto.json`, `tutos/retro_techno.json` et `tutos/synthwave.json` sont touchés. Rien hors liste.

## Ce qui n'est pas vert
- Les tests n'ont pas été relancés par le relecteur.
- M1 à M4 ouverts ; M1 n'est pas mesuré.

Verdict : 0 bloquant, 0 grave
