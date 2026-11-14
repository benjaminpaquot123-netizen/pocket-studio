# Relecture neuve — lot coquille4 (tour 6)
Relu : 383491db

Portée : `git diff eq/tour-6-coquille3...HEAD` (RetoursUi.kt, AppRoot.kt, StateWrittenOnMainTest.kt, rapport, décisions). Gradle non lancé ; le test a été rejoué à l'identique (mêmes regex, même découpe de bloc) par un script sur les sources de git.

## Justesse des trois corrections
- `withContext(Dispatchers.Main)` dans `produceState` / `LaunchedEffect` : pas d'interblocage possible. C'est une suspension (un message posté sur la boucle principale), jamais une attente bloquante ; sur le téléphone l'effet tourne déjà sur le fil principal (AndroidUiDispatcher), le saut coûte un passage de boucle, rien d'autre. Sous Robolectric, la boucle principale est vidée par `waitForIdle` : le message passe. L'annulation (clé qui change pendant le saut) saute l'écriture, comme avant pendant la lecture.
- `AppRoot.kt:1376-1385` : `?: return@LaunchedEffect` reste hors du bloc Main, même sens qu'avant. `openRemix()` (lambda ordinaire, `AppRoot.kt:1328`) appelée dans le bloc suspendu : correct, et sur le fil principal comme il se doit. `RemixFromHome.take` consomme toujours la chanson avant la lecture : une annulation pendant la lecture ou le saut la perd, comme avant (inchangé, hors lot).
- `AppRoot.kt:548-553` (`value = read` dans `withContext(Main)`) et `RetoursUi.kt:328` : corrects.
- Remarque sur la cause : sur le téléphone la reprise se fait déjà sur le fil principal ; la différence vient du banc (répartiteur de test sans redistribution, la reprise reste sur le fil de la lecture). Le rapport le dit pour coquille3 ; la correction est sans risque et rend le comportement indépendant de l'environnement.

## Le test
- Il échoue bien sur le code d'avant : rejoué sur `eq/tour-6-coquille3`, il trouve exactement `RetoursUi.kt:326`, `AppRoot.kt:547`, `AppRoot.kt:1372` ; sur `HEAD`, rien. 260 fichiers lus : la garde `sources.size > 100` tient.
- Chemin `src/main/java` : relatif au dossier du module (répertoire de travail par défaut des tests unitaires Gradle), correct ; la garde du nombre de fichiers empêche un passage à vide.

## Constats
- **moyen** — `app/src/main/java/com/pocketstudio/app/export/video/VideoSheet.kt:218-223` : même motif, non corrigé et non vu par le test : `preview = withContext(Dispatchers.Default) { … }` dans un `LaunchedEffect(plan)` (l'image d'aperçu de la vidéo). La regex ne cherche que `Dispatchers.IO`. Sur le banc, l'aperçu peut rester vide comme « Mes fichiers ». À faire : élargir la regex à `Dispatchers\.(IO|Default)` (et `NonCancellable + …`), corriger cet effet de la même façon, rejouer rouge puis vert. Le rapport dit « cherché dans toute l'appli » : à nuancer tant que ce n'est pas fait.
- **mineur** — `StateWrittenOnMainTest.kt:39` : `"Dispatchers.Main" !in body` accepte n'importe quelle mention dans le bloc (un commentaire, un autre `launch`), même si l'écriture visée reste hors du bloc Main. Faux négatif possible ; à resserrer (chercher l'affectation à l'intérieur d'un `withContext(…Dispatchers.Main…)`) si le test doit rester une garde durable.
- **mineur** — `StateWrittenOnMainTest.kt:34` : l'affectation doit suivre un début de ligne, `{` ou `;` ; `state.value = withContext(Dispatchers.IO)` ou `x.y = …` lui échappent, ainsi que `rememberCoroutineScope().launch { x = withContext(IO) … }`. Aucun cas de ce genre trouvé aujourd'hui dans les écrans (grep fait) ; à noter dans les limites du rapport.
- **mineur** — `StateWrittenOnMainTest.kt:18-27` : la découpe de bloc compte les accolades des chaînes et commentaires ; un `'{'` isolé ou une accolade dans une chaîne non équilibrée fausserait la fin du bloc. Sans effet sur le code actuel.
- **mineur** — le test ne voit pas une lecture cachée derrière une fonction (cas d'origine de « Mes fichiers », `MyExports.kt:144`) : déjà écrit dans les limites du rapport, rien à ajouter.

Verdict : 0 bloquant, 0 grave
