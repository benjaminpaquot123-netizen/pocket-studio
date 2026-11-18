# Tour 7, p67-repli : relecture neuve
Relu : 191042384ce641a6cf4f420d090ccf2cea5ada35

Portée : `git diff 3cd69a96..HEAD` (rouge 428957c2, wip e944f59d, vert 80a2b698, docs 19104238). Lecture seule, Gradle non joué.

Points vérifiés sans défaut :
- Unités : dans le `SideEffect` de `TutoPanel.kt:563-570`, toutes les grandeurs passées à `TutoReadability.layout` sont en px (viewport, `maxValue`, `examplesHeight`, `gapPx`, `wordsLineHeight()` qui rend des px, `MORE_DP` converti par `roundToPx`). Les paramètres s'appellent « Dp » mais la fonction ne fait que des comparaisons et une division, cohérentes dans une seule unité. Pas de mélange px/dp.
- Boucle de recomposition : le `SideEffect` n'écrit `examplesFolded` qu'une fois (false → true) puis sa condition devient fausse ; le `ScrollState` recréé sur `examplesFolded` ne relance pas l'écriture. `moreHeight` et `examplesHeight` sont écrits dans `onSizeChanged`, sans retour sur leur propre taille. Pas de boucle.
- Remise à zéro : `examplesFolded` et `examplesHeight` sont clés sur `step.id`, `ui.open`, `ui.progress.level` : remis à zéro à chaque étape, à chaque dépliage et au changement Facile/Pro.
- Calcul du repli : `rest + examplesHeight + gap` = hauteur du contenu défilant ; le repli se fait si ce contenu dépasse `viewport + « Lire la suite » + écart`, c'est-à-dire si les mots défileraient encore sans les exemples. Logique juste. La hauteur du panneau (`held`) n'est pas reprise : les mots gagnent la place des exemples, le panneau ne bouge pas.
- `TutoReadability.layout` (`TutoReadability.kt:127-129`) : coupée malgré tout, la consigne garde le kit et « Pourquoi ? », au moins une ligne (coerceIn(1, …)). Les attentes des tests (4 lignes ; 3 lignes à 34,3 px sous 48 + 36 + 32 dans 240) sont justes.
- Le commit rouge ne touche que les tests ; le code vient après.

Remarques :
1. mineur, `app/src/test/java/com/pocketstudio/app/tuto/Tour7TutoPanelFoldTest.kt:91-104` : seule la vérification des exemples dépend du lot. La carte du kit et « Pourquoi ? » étaient déjà épinglés hors du défilement avant le lot (le panneau ne les replie jamais, il ne lit pas `kitCard`/`why` de `layout`) ; ces deux vérifications passaient donc déjà sur 3cd69a96. De plus la carte du kit est un faux `Text("Kit")` d'une ligne, pas la vraie offre (deux boutons) : sa hauteur réelle n'est pas mesurée. Le rapport ne prétend pas le contraire, mais la preuve « le kit reste visible » repose surtout sur les tests antérieurs (`TutoKitTest`, `Tour6TutoPanelFitWhyTest`).
2. mineur, `TutoPanel.kt:537-538` et `563` : `examplesFolded` n'est pas clé sur `cellsToTouch`/`readAll`. Repliés dans la chambre basse (cases à toucher), les exemples restent repliés après « Lire la suite » qui rend toute la hauteur aux mots, alors qu'ils tiendraient. Pas de perte d'information grave (exemples d'aide seulement).
3. mineur, `TutoPanel.kt:563-566` : `examplesHeight` garde sa dernière valeur quand les exemples disparaissent pour une autre raison (Mode simple coupé en cours d'étape, `momentWords` non nul pendant « Montre-moi »). Le `SideEffect` peut alors calculer avec une hauteur périmée et passer `examplesFolded` à vrai, ce qui recrée le `ScrollState` : remise en haut du défilement des mots en cours d'étape. Cas rare, sans plantage.
4. mineur, `TutoPanel.kt:539` : la recréation du `ScrollState` au repli donne une composition où `maxValue` vaut `Int.MAX_VALUE` (`scrolls` faux) : « Lire la suite » et « … » disparaissent le temps d'une image puis reviennent si les mots défilent encore. Un frame de saut possible, comme déjà au changement d'étape.
5. mineur, `TutoPanel.kt:562` : `moreHeight` est un `remember` sans clé ; la hauteur de « Lire la suite » d'une étape sert aux suivantes. Sans effet tant que l'échelle de police ne change pas dans la même composition (elle recrée l'activité).
6. mineur, `TutoReadability.kt:116` : les paramètres s'appellent `roomDp`, `lineHeightDp`… alors que le panneau y passe des px ; à renommer (ou documenter « même unité partout ») pour éviter une future erreur d'unité.
7. mineur, test : le panneau n'est testé qu'en 360 × 800 dp (« Pourquoi ? » épinglé) ; le cas téléphone couché (`pinned` faux, « Pourquoi ? » dans le défilement, compté dans `rest`) n'est pas couvert. Le calcul le traite correctement à la lecture.

Verdict : 0 bloquant, 0 grave — fusion possible

## Relecture 2 (CI 353)
Relu : 516f2515..6e114e2b (TutoPanel.kt, Tour7TutoReadAllKitTest.kt, docs).

Lecture seule, Gradle non joué. Rouge cd611c7c = test seul, vert a27e0224 = `TutoPanel.kt` seul : ordre respecté.

Points vérifiés sans défaut :
- Pas de boucle : `wholeWords` ne fait que monter (`maxOf`), écrit dans un `SideEffect` (après la composition, pas pendant) seulement si `whole > wholeWords`. Quand les mots tiennent, `maxValue` = 0 donc `whole` = 0 et la valeur gardée ne bouge plus : pas d'aller-retour entre `least` et le défilement.
- `held`/`regrown` sont clés sur `cellsToTouch` : « Lire la suite » (readAll) les remet à zéro. Chemin normal : 1re image sans `held`, mots mesurés trop longs, `wholeWords` posé, `held` pris trop bas, `starved` → une regrowth (2 au plus, garde existante). Converge.
- Débordement : `WordsRoom` plafonne à `c.maxHeight` ; épinglés (kit, « Pourquoi ? », « Lire la suite », « Réduire ») mesurés d'abord et entiers. En état final (`holding`, `weight(1f)`), l'`ActionBar` est mesurée avant les mots : boutons jamais cachés, mots qui défilent si l'écran ne suffit pas.
- Hors readAll ou hors chambre basse : `least` = `lineAndHalf` comme avant. `wholeWords` remis à zéro par « Réduire » (clé `readAll`).
- Le test n'est pas vide : il attend la vraie carte du kit (`tuto_kit`), vérifie d'abord que la consigne est coupée avant le toucher, puis entière après, et la carte entière.

Remarques :
1. mineur, `TutoPanel.kt:665-667` : en paysage (`pinned` faux), `least` valait toujours 0 ; il vaut maintenant `wholeWords` sous readAll en chambre basse, ce qui active `starved` et la regrowth couchée, contraire au commentaire juste au-dessus (« a phone on its side has its panel as before »). Les boutons restent protégés en état final (`holding`), mais une image intermédiaire non tenue peut écraser l'`ActionBar` (la Column donne au bloc des mots tout le reste). Non couvert par un test. À aligner : `if (pinned && lowRoom && readAll)` ou corriger le commentaire.
2. mineur, `TutoPanel.kt:665` : `wholeWords` ne redescend pas si le contenu rétrécit sous readAll (« Pourquoi ? » refermé, exemples repliés) : place vide sous les mots dans la hauteur tenue jusqu'au prochain `held = 0`. Visuel seulement.
3. mineur, test : il ne vérifie ni « Pourquoi ? » ni l'`ActionBar` en vue après « Lire la suite » ; un seul format (392 × 852 dp, hdpi), pas de paysage ni de petit écran où la hauteur entière dépasse.
4. mineur, test : attente en temps réel (`System.currentTimeMillis`, 30 s) sur le chargement de l'index des packs ; une CI lente échouerait par « not in time » (rouge franc, pas faux vert).

Verdict : 0 bloquant, 0 grave — fusion possible
