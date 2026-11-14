# Relecture neuve — lot coquille4 (tour 6)
Relu : 20ff33ea

Portée : `git diff eq/tour-6-coquille3...HEAD` (RetoursUi.kt, AppRoot.kt, VideoSheet.kt, StateWrittenOnMainTest.kt, rapport, décisions). 2e passe : les commits 33232f5c (rouge), 34e52ae5 (vert) et 20ff33ea (docs) ont été relus depuis 383491db. Gradle n'a pas été lancé. Le test a été rejoué à l'identique (mêmes regex, même découpe de bloc) par un script sur les sources de git.

## Justesse des corrections
- `withContext(Dispatchers.Main)` dans `produceState` / `LaunchedEffect` : pas d'interblocage possible. C'est une suspension (un message posté sur la boucle principale), jamais une attente bloquante. Sur le téléphone, l'effet tourne déjà sur le fil principal : le saut coûte un passage de boucle. Sous Robolectric, `waitForIdle` vide la boucle principale et le message passe. Une annulation (clé qui change pendant le saut) saute l'écriture, comme avant pendant la lecture.
- `AppRoot.kt:1376-1385` : `?: return@LaunchedEffect` reste hors du bloc Main et garde le même sens. `openRemix()` (lambda ordinaire, `AppRoot.kt:1328`) est appelée dans le bloc, donc sur le fil principal : correct. `RemixFromHome.take` consomme toujours la chanson avant la lecture, comme avant (inchangé, hors lot).
- `AppRoot.kt:548-553`, `RetoursUi.kt:328` et `VideoSheet.kt:218-225` (`preview = drawn` dans `withContext(Main)`) : corrects.
- Sur le téléphone, la reprise se fait déjà sur le fil principal. Sur le banc, le répartiteur de test ne redistribue pas, et la reprise reste sur le fil de la lecture. La correction est sans risque et rend le comportement indépendant de l'environnement.

## Le test
- Il échoue bien sur le code d'avant. Rejoué sur `eq/tour-6-coquille3` avec la regex `IO|Default`, il trouve exactement `RetoursUi.kt:326`, `AppRoot.kt:547` et `AppRoot.kt:1372`. Sur 33232f5c (rouge), il trouve `VideoSheet.kt:218`. Sur `HEAD`, il ne trouve rien. Il lit 260 fichiers : la garde `sources.size > 100` tient.
- Le chemin `src/main/java` est relatif au dossier du module (répertoire de travail par défaut des tests unitaires Gradle) : correct.

## Constats
- **moyen** **corrigé** — `export/video/VideoSheet.kt:218` : même motif avec `Dispatchers.Default`, que le test ne voyait pas. La regex couvre maintenant `Dispatchers.(IO|Default)` et l'aperçu est écrit sur le fil principal. Rouge puis vert vérifiés. Le rapport est nuancé (limites listées).
- **mineur** — `StateWrittenOnMainTest.kt:39` : n'importe quelle mention de `Dispatchers.Main` dans le bloc satisfait le test, même si l'écriture visée reste hors du bloc Main. Les limites du rapport le disent.
- **mineur** — `StateWrittenOnMainTest.kt:33` : le test ne voit pas `x.y = …`, `rememberCoroutineScope().launch`, ni `withContext(NonCancellable + Dispatchers.Default)`. Aucun cas de ce genre dans les écrans aujourd'hui (grep fait). Les limites du rapport le disent, sauf le dernier cas.
- **mineur** — `StateWrittenOnMainTest.kt:18-27` : la découpe de bloc compte aussi les accolades des chaînes et des commentaires. Sans effet sur le code actuel.
- **mineur** — `StateWrittenOnMainTest.kt:12` : le KDoc a gardé un `* *` en double en tête de ligne (cosmétique).
- **mineur** — le test ne voit pas une lecture cachée derrière une fonction (`MyExports.kt:144`). C'est déjà écrit dans les limites du rapport.

Verdict : 0 bloquant, 0 grave
