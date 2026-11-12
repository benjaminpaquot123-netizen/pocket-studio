# Relecture du lot « mixwake » (tour 5)
Relu : 5893508991c2e5dfe7b550222b529c0b5e9e6ca6

Lu : a240a4b1 (rouge, tests seulement), b49d8f74 (vert), 0302da8a (docs) ; 58935089 est la fusion de fx-int. Tests ciblés rejoués ici : verts (Gradle « up-to-date », déjà vert sur ce code).

## Constats

1. **Moyen** (ni bloquant ni grave) : un « Ça sature » bref peut ne plus s'afficher. Les mots de « Sortie » (`OutputWordsText`) et la ligne du limiteur (`LimiterLine`) sont échantillonnés toutes les 250 ms, et `snapshotFlow` ne garde que la dernière valeur. Or `METER_LIMITER` est le gain le plus bas depuis la lecture précédente, repris tel quel par `holdMeterPeaks` sans maintien, et `METER_MASTER_IN` retombe de 15 % par lecture. Une saturation qui ne dure qu'une lecture (50 ms) est donc vue par la composition une fois sur cinq environ. Avant le lot, chaque lecture changeait le texte. Le conseil « Ça sature » (Mode simple) reste, lui, suivi à chaque lecture : les mots peuvent dire « Bien » pendant que le conseil apparaît. Piste : garder le pire des lectures de la période (min du limiteur, max de l'entrée) plutôt que la dernière. Le rapport et les décisions ne mentionnent pas ce compromis (« toujours la dernière valeur » y est dit comme une qualité).
2. Faible : `hasOwnLayer` lit `getInnerLayerCoordinator` par réflexion. Cela casse en silence à une mise à jour de Compose (le test échoue alors, ce qui se voit), et passerait à tort si un autre modificateur à calque (alpha, clip) était posé sur le même nœud. Acceptable pour une preuve que Robolectric ne peut pas donner autrement.
3. Faible : `SimpleMixUi` mémorisé sur (projet, boucle, écoute seule, conseil). Les seules entrées lues dans le bloc sont ces quatre valeurs et `DefaultNames.shown` ; rien d'obsolète trouvé. Le choix de ne pas clé sur la langue est écrit dans les décisions.

## Points demandés

- Exactitude : fader et vumètres lisent leur valeur au dessin, `graphicsLayer()` est posé en fin de chaîne, après `pointerInput` et `semantics` : le glissement et l'automation ne sont pas touchés, rien n'est écrêté (clip faux par défaut). La puce de loudness « hot » n'est pas freinée (`rememberChangesOf`). « Ça sature » du conseil : `snapshotFlow` + `collectLatest` + délai de 150 ms ; il apparaît et disparaît (faux relance `MixTips.of(..., false)`) ; un changement de projet relance le `produceState` et réémet la valeur courante. Seul le constat 1 touche aux données montrées.
- Fuites : `rememberChangesEvery` n'a qu'un `LaunchedEffect(state, periodMillis)`, annulé avec la composition (ou si les clés changent) ; aucun minuteur ni coroutine hors composition. Le `delay` dans `collect` ne perd pas la dernière valeur (flux conflaté). La première valeur est lue tout de suite, sans abonnement.
- `graphicsLayer` : un calque par vumètre, fader, chip et ligne, sans `compositingStrategy` coûteuse ; quelques dizaines de calques au plus, pas de croissance par canal au-delà de ce qui est affiché. Rien d'inquiétant.
- Captures : aucun fichier `.png` dans le diff `a240a4b1~1..0302da8a` (contrôlé : zéro). Les « 513 inchangées » du rapport sont cohérentes avec cela ; non rejouées ici (consigne).
- Tests : le rouge ne change que des tests ; seuils de 374 à 120 et de 2995 à 160 réellement abaissés, plus la présence d'un calque sur les tags qui bougent. Ils vérifient une conséquence réelle (portées et calques), pas le temps d'image, ce que le rapport dit (« Limites »). Rouge non rejoué par moi sur le parent. Aucun test ne vérifie le constat 1 (valeur affichée après le freinage).
- Partie B : relue, aucun changement de code, conclusion sobre (« cause non trouvée dans le code »). Rien à reprocher.

Verdict : 0 bloquant, 0 grave — fusion possible
