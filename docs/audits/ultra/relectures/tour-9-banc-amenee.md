# Relecture neuve : tour-9-banc-amenee

Relu : `git diff 2b18313c4 ddc06a950` (HumanTester.kt, BenchAmeneeLateKitCardTest.kt). Paire 8fdb49906 « rouge » puis ddc06a950 « vert ». Code testé : ddc06a950.

## Le rouge prouve la panne
- Sur 8fdb49906, `BenchAmeneeLateKitCardTest` : 4 tests, 2 échecs (reggaetonChapter8OnPhone360, liquidDnbChapter12OnPhone360), 1 ignoré (drive).
- Le crochet `ameneeAfterAim` est appelé entre `reachTag` et `centerOf`, avec l'ancienne condition (seulement « la cible a bougé depuis lastAt ») : les deux premières assertions passent (catalogue arrivé, « Chapitres » remonté), l'échec porte sur la troisième.
- Message lu : « amenée tuto:reggaeton:8 : les chapitres n'est jamais venu », journal « touche 398 492 (tuto_chapters) » puis « après touche tuto_chapters@398,336 », aucun tap-missed (idem liquid_dnb : 391 492 → 391,336). C'est exactement la panne des sièges t9-57 / t9-45.
- Sur ddc06a950 : la même classe est verte (3 passés, 1 ignoré), un seul « amenee.tap-missed » par cas.

## Correction
- Le point touché (`touched`) est gardé ; `beside` = ce point n'est plus dans les bornes dessinées de la cible. Retouche si `moved || beside`.
- Plafond : `retaps < ameneeRetaps` (2), remis à zéro seulement par une touche neuve (`again = false`) ; pas de boucle sans fin. `polls` est remis à 0 après chaque retouche, le filet `ameneeCapMs` reste.
- Pas de faux tap-missed pour une cible coupée par un bord : `touchedWasOn` est calculé au moment de la visée (avant le crochet) ; un point hors du dessiné à la visée n'est jamais jugé « à côté ». Nœud illisible : `missing = true`, donc pas « à côté ».
- Crochet : `null` par défaut, remis à `null` dans le test et dans `@After` ; sans lui le comportement normal ne change pas (un seul appel de sémantique de plus par touche).

## Non-régression (lancé sur ddc06a950)
- BenchAmeneesProfilesTest 68 (34 ignorés), BenchAmeneesTest 16, BenchFlagPathsTest 4 (dont le refus `ameneeRetaps = 0`), BenchSeparationWaitTest 4, BenchTutoAnchorTicksTest 148 : 0 échec.

## Points mineurs (non bloquants)
- `beside` peut aussi se déclencher si la touche a bien porté et que la cible se déplace ensuite pendant une attente lente : retouche en trop sur un bouton à bascule. Même risque déjà présent avec `moved`, plafonné à 2 et écrit au journal.
- `went ?: was!!` : `was!!` n'est plus atteignable en pratique (`touched` et `lastTag` sont posés ensemble) ; sans danger.

Verdict : 0 bloquant, 0 grave
