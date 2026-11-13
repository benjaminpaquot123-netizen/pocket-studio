# Relecture neuve — lot coquille2 (tour 6)

Relu : c3190921

Portée : tout le lot (`git diff origin/ccr-80af1a1b-mn0wec...HEAD`). Troisième passe : la 1re sur a2c12c1c, la 2e sur 98324743, celle-ci sur les commits `d0fbbcd9` à `c3190921`, c'est-à-dire P6 (`HeardLength.kt`, `PlayStrip.playPosition`, `RhythmViewModel.barNow`) et les mineurs de la 2e passe. Lecture du code seulement, Gradle non lancé.

## Points des passes précédentes

- **A6 dans une boucle (grave) : réglé.** La phrase est juste dans une boucle, et « Allonger » n'accompagne que son propre avis. Le test A6 vérifie la structure créée. Le test N2 vise `export_cancel`. Constaté en 2e passe, toujours vrai.
- **E2 et les mineurs que le chef a notés : acceptés**, comme dit dans la dernière section du rapport.
- **Japonais de `coquille2` : réglé.** La phrase est maintenant au registre poli d'un bout à l'autre (« 曲はループより %s 長く、ループがくり返すあいだも鳴り続けます。 »), et `%s` est gardé.
- **Test de la cymbale : réglé.** `said.isNotEmpty()` empêche que le test passe sans aucun « → » à l'écran.
- **P5, écriture d'état pendant la composition : inchangé, mineur.**

## P6 (3e passe)

Points vérifiés et justes :
- **Le calcul ne sert qu'à l'affichage.** `heardSceneSteps` n'est appelé que dans `PlayStrip.playPosition` (`PlayStrip.kt:105`) et `RhythmViewModel.barNow` (`:3274`). `playBar` ne sert qu'aux compteurs (`UnderTopBar`, `RecordBar.takeCounter`, `ReportWiring`). La boucle du moteur, `sceneBars` de l'export, `exportEnd` et `SongLength` n'y touchent pas.
- **Le mode morceau ne change pas.** `barNow` sort avant pour `songIndex >= 0`, et la branche morceau de `playPosition` lit `songBars`. Seules les branches « boucle » (`playing` sans morceau, `loopPart`) changent.
- **Les clips de morceau sont exclus** (`!it.song`), et **la brique d'une autre partie aussi** (`it.scene == scene`, couvert par `HeardLengthTest`).
- **Le cas du siège tombe juste.** Le moteur relance un clip de partie sur `(local − start) % loopSteps` (`AudioTracks.cpp:166-168`), et `absoluteStep` est le pas depuis le début de la partie (`Engine.cpp:975`). Pour une brique posée par `placeInCell` (début 0, boucle d'une puissance de 2 mesures, `LiveCells.loopBars`) dans une partie d'une mesure, « mesure x/N » suit bien ce qu'on entend.

Constats :

1. **mineur** — `project-model/src/main/kotlin/com/pocketstudio/model/HeardLength.kt:11`. La longueur prise est `loopSteps` pour tous les clips. Or le moteur joue un clip « une fois » (`once`) sur `engineLoopSteps(sceneSteps, tempo)` (`AudioTracks.kt:201-209`), pas sur `loopSteps`. La valeur rangée dans `loopSteps` d'un tel clip n'a pas de sens pour le moteur et peut gonfler ou réduire le compteur. Autres oublis : un clip sans partie (`scene == null`, vieux projets, fichier importé), qui joue pourtant dans toutes les parties (`belongsTo`), n'est pas compté ; une piste audio coupée, ou dont le clip est éteint, l'est. À faire : prendre `engineLoopSteps(sceneSteps(scene), tempoBpm)` et `belongsTo(scene)` plutôt que `loopSteps` et `scene == scene`, et ignorer les pistes muettes. On peut aussi le laisser en l'état : ça ne touche que l'affichage.

2. **mineur** — `RhythmViewModel.kt:3274` (`barNow`). Le modulo sur le maximum n'est juste que si les longueurs se divisent entre elles. Avec une partie de 3 mesures et une brique de 4, le vrai cycle fait 12 mesures, et la brique suit son propre départ (`start`, ou le `localOf(audioSlot)` d'une case lancée en direct), pas forcément le début de la partie. « mesure x/N » peut alors ne pas tomber sur la reprise de la brique. Ça reste de l'affichage, et le cas courant (partie d'une mesure, briques en puissances de 2, début 0) est juste. On peut l'accepter et le noter.

3. **mineur** — `app/src/main/java/com/pocketstudio/app/record/Takes.kt:77-78` (`takeCounter`), une incohérence entre écrans. Le compteur de la prise de notes garde `sceneSteps` : « 1/1 », alors que la barre du haut dit « 3/4 » pendant la même lecture. C'est défendable, la prise de notes boucle sur la partie, mais il faut le savoir pour la recette.

4. **mineur** — `RhythmViewModel.kt:3274` contre `PlayStrip.kt:105`. Le numérateur boucle sur `heardScene ?: p.scene`, le dénominateur sur `p.scene`. Si la partie entendue n'est pas celle affichée, et maintenant que leurs briques peuvent différer, le compteur reste bloqué sur « N/N » à cause de `coerceIn`. Ce problème existait déjà avec `sceneSteps`, P6 le rend seulement plus visible. À faire : lire la même partie des deux côtés.

5. **mineur** — `app/src/test/java/com/pocketstudio/app/ui/Coquille2SeatTest.kt` (`p6_…`). Sous Robolectric, `NativeEngine.isAvailable` est faux : `playBar` vaut -1 et le test ne vérifie que le dénominateur, venu de `playPosition`. Le modulo de `barNow` n'est couvert par aucun test. `HeardLengthTest` couvre bien la fonction du modèle avec 6, 2 et 5 mesures et la brique d'une autre partie, mais ni un clip « une fois », ni un clip de morceau, ni un clip sans partie. À faire : ajouter ces trois cas à `HeardLengthTest`.

Rien qui plante, qui perde une sauvegarde ou qui touche au moteur ou aux exports.

Verdict : 0 bloquant, 0 grave
