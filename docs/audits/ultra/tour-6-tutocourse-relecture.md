# Tour 6 — lot « tutocourse » : relecture neuve
Relu : f23b1cc1

Relecture par un sous-agent neuf qui n'a pas écrit le lot : diff `5025b771..f23b1cc1` côté `app/` (`TutoController.kt` : `init`, `look()`, `looks()`, l'en-tête et la fin de `check()` ; `Tour6TutoLooksTest` neuf ; `Tour6TutoPanelAlreadyTest` et `Tour6TutoFlechesRollTest` modifiés), lu avec `check()` en entier, le rapport et les décisions du lot.

## Bloquant
Aucun.

## Grave
Aucun.
- Blocage ou boucle folle : non. `check()` ne rend `false` que si la progression, la cible ou le projet ont bougé pendant le regard, ou s'il vient de cocher (la progression change) ; le regard suivant, sur un état qui ne bouge plus, rend `true`. Chaque tour passe par `withContext(worker)` : pas de boucle sans suspension.
- `project.debounce(debounceMs).first()` : un `StateFlow` émet sa valeur tout de suite et `debounce` la rend après 250 ms de calme, même si le projet ne change plus jamais : pas d'attente sans fin.
- Annulation : `receive()`, `withContext` et `first()` sont des points d'annulation ; le scope annulé arrête `looks()` et le collecteur.
- Réponse périmée : plus aucune n'est appliquée sur une autre progression, une autre cible ou un autre projet (comparaison d'identité puis d'égalité) ; « Montre-moi » commencé pendant le regard : réponse ignorée, sa fin redemande un regard.
- `show()`, `save()` (arrivée sur une étape), `fulfilNeed`, la fin de `showMe` et le démarrage passent tous par `look()` ; le canal à une place ne perd aucune demande (une demande pendant un regard le fait refaire une fois).
- `asked` est déclaré avant le bloc `init` : le `show()` du chargement trouve le canal prêt.
- Pour l'utilisateur : même délai qu'avant (250 ms après une modification) ; un bouton touché pendant un regard attend sa fin, quelques millisecondes. Ni sauvegarde (toujours `tick()` → `save()`) ni son touchés.
- Pas de pause fixe ajoutée pour tenir un ordre : `Tour6TutoPanelAlreadyTest` perd son `Thread.sleep(500)` et ses pauses au profit de la porte `LooksGate`, avec un garde-fou de 200 tours.
- Aucun test retiré ni affaibli : `Tour6TutoPanelAlreadyTest` garde ses trois assertions ; `Tour6TutoFlechesRollTest` arrête d'écrire sur la règle du cours (`TutoNeed.Copy`), affirme qu'elle est atteinte, puis attend toujours le bouton « Copier » à l'écran (même exigence, plus une : ne pas écrire au-delà du motif).

## Moyen
Aucun.

## Mineur
- `TutoChecks.need(…)` est appelé dans `Tour6TutoFlechesRollTest` sans la base du contrôleur (`baseline = null`) : s'il divergeait, le test rougirait sur l'attente du bouton, il ne peut pas passer vert à tort.
- Une réponse dont seuls les chapitres ont changé (même progression) serait encore appliquée ; les chapitres ne changent qu'avec le niveau, donc avec la progression : sans effet aujourd'hui.
- Le rapport le dit : les 20 passages seuls et dans la suite complète, le self-test du banc et le second passage CI ne sont pas faits ; à faire avant le portail, ce n'est pas un défaut du code.

## Relancé et vu
- `bash scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests 'com.pocketstudio.app.tuto.Tour6TutoLooksTest'` sur `f23b1cc1` : BUILD SUCCESSFUL, 5 tests, 0 échec, 0 ignoré (résultat du 2026-11-13 18:52 UTC).
- Rouge sur le parent du vert pas rejoué ici (consigne du chef : pas de seconde construction en parallèle) ; repris du rapport : les cinq tests à porte rouges sur `abe754e0` sur leur assertion. Lu : les tests retiennent le travail des regards par `LooksGate` et le relâchent dans l'ordre qui fait mal ; sur le code d'avant, plusieurs regards répondent l'un après l'autre et le plus ancien écrase le plus neuf (`need` remis à `null`), ce que vérifient les assertions : ils ne peuvent pas être verts sans la correction.

Verdict : 0 bloquant, 0 grave
