# Tour 5, lot « hardtechno » : relecture
Relu : 7fe6ddb3f15f34d43fbecd86584bdcefca3b0ef7

Relecteur neuf, par lecture du code (commits f24b82a5, 24f4a701, cdb98252, 623718b0 ; la fusion fx-int n'est pas relue). Les trois tests hôte `ht_*` rejoués ici : verts. Rien n'a été corrigé par le relecteur.

## Rappel audio (zone la plus stricte)
- `CallbackLoad::add` (appelé en fin de `Engine::process`) : arithmétique, un `fetch`/`store` relâché et une boucle CAS sur atomiques. Ni allocation, ni verrou, ni journal, ni JNI. `steady_clock::now()` était déjà appelé avant le lot ; le seul ajout est une conversion en double. OK.
- Concurrence : `smoothed_`, `lastStarted_`, `lastLasted_` ne sont touchés que par le fil audio. `worstBlock_` / `worstGap_` : le fil audio ne fait que monter (CAS), le lecteur fait `exchange(0)`. Un maximum posé entre deux relevés n'est pas perdu, et une remise à zéro ne peut pas écraser une valeur plus grande (la CAS relit). Pas de course. OK.
- Les valeurs passent à l'interface par atomiques seulement ; `healthMeters` (JNI) est appelé depuis le fil de l'interface, pas du rappel. Un seul lecteur (`HealthMonitor.feed`, AppRoot.kt:1086) : cohérent avec « prendre ».
- Un NaN de `took` est ignoré par `raise` ; le lissage a le même comportement qu'avant (calcul inchangé).

## Cœur sans Android
- `CallbackLoad.h` n'inclut que `<algorithm>` et `<atomic>` ; `Engine.cpp` garde `<chrono>`. Compile et passe dans `engine/host-tests`. OK.

## Kotlin / JNI
- JNI : `n = min(6, longueur)` ; `takeWorst*` appelés seulement si le tableau est assez long ; un tableau de 4 reste valide. `HEALTH` = 6 : `meters` est alloué avec cette constante. Valeurs à 0 : `stallText` rend « » (aucune ligne ajoutée). Pas de plantage prévisible.
- Journal : une seule ligne par relevé où les décrochages augmentent, pas de ligne en plus. Pas de spam.
- Le banc (`LiveSongRecorder`, `bench.listen`, `bench.packs`) est dans `app/src/test` seulement ; aucun effet sur l'application.

## Constats
1. (mineur) Écart parasite possible : si le flux est rouvert en moins d'une seconde, ou si `prepare()` ne remet pas `lastLasted_` à zéro, `takeWorstGap` peut rendre jusqu'à ~500 blocs pour un simple redémarrage, ligne « plus long écart » trompeuse. Le test ne couvre que la pause de 3 s. Noté, pas bloquant.
2. (mineur) Le premier rappel après l'ouverture compte dans le « pire bloc » (le test moteur voit 0,62 sur un premier appel à froid) : fausse alerte possible juste après un démarrage. Noté, pas bloquant.
3. (mineur) Le rapport ne dit pas que `cpuWorst` n'est « pris » qu'une fois par relevé : le pire bloc d'un décrochage est celui de la fenêtre de 250 ms qui précède le relevé, pas un cumul. La phrase de la ligne de journal ne le dit pas non plus. Noté.

## Tests
- `ht_worst_block_sees_one_slow_callback` : vérifie le vrai maximum (13), que le lissé le cache (< 0,6), la remise à zéro par `take`. Significatif.
- `ht_worst_gap_sees_a_late_callback` : écart 16 blocs, pire bloc < 0,5 (les deux causes se distinguent), flux rouvert ignoré. Significatif.
- `ht_engine_reports_its_worst_block` : moteur réel, borné par une mesure extérieure ; bornes larges (pas de test de temps fragile). Significatif.
- `JournalLogicTest#anUnderrunSaysWhetherACallbackStalledOrStartedLate` : vérifie les deux champs lus des mesures 4 et 5, la phrase exacte et le JSON. Significatif.

## Rouges constatés
- Le commit « rouge » 24f4a701 contient du code de production : l'en-tête `CallbackLoad.h` (API complète, mais `takeWorstBlock` et `takeWorstGap` rendent la charge lissée) et le branchement dans `Engine.cpp` / `Engine.h`. La fiche (point 3) demande « des tests seulement ». Jugé acceptable ici : en C++ les tests ne compilent pas sans l'API, donc le seul rouge possible par assertion passe par une doublure ; elle n'implémente pas la correction (le vert 24f4a701 → cdb98252 ajoute le maximum, l'écart et les atomiques), et le rapport l'annonce. Écart à la lettre de la fiche, à signaler au portail, pas un constat grave.
- Côté Kotlin, le test de journal est ajouté dans le commit vert cdb98252, sans commit rouge séparé (le rapport ne le revendique pas non plus) : mineur.
- rouge constaté : cdb98252 : rejoué sur 24f4a701 : non rejoué ici (lecture du code seulement) ; le rapport donne 0,45 avant et 13,00 après pour `ht_worst_block_sees_one_slow_callback`, cohérent avec la doublure lue (renvoie `smoothed()`).

Verdict : 0 bloquant, 0 grave — fusion possible
