# Tour 6 — lot « tutoordre » : relecture neuve
Relu : e337db4ed6a1d5e6253e0213b3b2d8b38a738713

Relecture par un sous-agent neuf qui n'a pas écrit le lot : diff `ce0aed6f..e337db4e` (une condition dans `TutoController.check()`, un test neuf, rapport, décisions), lu avec `save()`, `tick()`, `goTo()` et `check()` en entier.

## Bloquant
Aucun.

## Grave
Aucun.
- Diagnostic : juste. `show()` lance un `check(project.value)` sur la première étape ; `u` est figé avant `withContext(worker)`. Si sa réponse revient après `goTo(tempo)`, `tick()` coche « tempo » (il agit sur `_ui.value`, donc sur l'étape montrée) mais l'ancienne condition testait `u.current` (la première étape) : pas de « Déjà fait ». Le `check` lancé par l'arrivée (`save()`, `moved`) filtre ensuite `s.id !in u.progress.done` : « tempo » n'est plus dans `found`, rien n'est dit. La course est réelle, sur la CI comme sur le téléphone.
- « Déjà fait » à tort ? Non trouvé. La garde `arrived == p` reste : le regard ne parle que s'il a regardé un projet identique à celui de l'arrivée sur l'étape montrée. Un geste de l'utilisateur sur l'étape change le projet, donc `p != arrived` pour tout regard qui le voit ; un regard ancien qui répond après le geste a regardé l'état de l'arrivée, où l'étape était déjà tenue : « Déjà fait » y est vrai. Les étapes `changed:` n'entrent dans `found` que pour l'étape où le regard a commencé : la nouvelle condition ne peut pas les toucher pour une autre étape. Seul cas limite : un projet modifié puis remis à l'identique avant l'arrivée compte comme « déjà là » (comportement d'avant, inchangé).
- Aucun test existant ne change de couleur parmi ceux relancés ; ni sauvegarde ni son touchés (seul `_ui.already` change, la sauvegarde passe par `tick()` comme avant).

## Moyen
- Le nouveau test tient son ordre par `Thread.sleep(500)` : sur une machine très lente, le regard de l'arrivée peut répondre avant le premier et le test passerait vert même sans la correction (jamais rouge à tort avec elle). Il prouve la course ici (rouge constaté ci-dessous) mais pas sur toute machine ; un `worker` de test contrôlé le rendrait déterministe.

## Mineur
- La ligne suivante (`made = null` pour la basse du Drop 2) teste encore `u.current` : même course possible en théorie, sans effet visible car l'arrivée sur une étape remet déjà `made` à `null`.

## Relancé et vu
- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --no-configuration-cache --rerun-tasks --tests …Tour6TutoPanelAlreadyTest --tests …Tour6TutoFlechesRollTest` sur `e337db4e` : BUILD SUCCESSFUL, PanelAlready 3/3, FlechesRoll 6/6, 0 échec (le premier lancement sans `--rerun-tasks` était UP-TO-DATE, d'où le second).
- rouge constaté : ab8fd49c : rejoué sur 713d66f6 : `TutoController.kt` de 713d66f6 remis seul, `Tour6TutoPanelAlreadyTest` 3 tests, 1 échec : `aLookBegunBeforeTheStepStillSaysAlreadyDone` ligne 114, « a look begun before the step was reached lost « Déjà fait » » ; fichier remis à HEAD ensuite, arbre propre.

Verdict : 0 bloquant, 0 grave — fusion possible
