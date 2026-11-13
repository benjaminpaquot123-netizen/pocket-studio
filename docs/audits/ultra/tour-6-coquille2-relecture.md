# Relecture neuve — lot coquille2 (tour 6)

Relu : 8868eb9d

Portée : tout le lot (`git diff origin/ccr-80af1a1b-mn0wec...HEAD`). Quatrième passe : la 1re sur a2c12c1c, la 2e sur 98324743, la 3e sur c3190921 (P6), celle-ci sur `c3190921..a032ab20`. Depuis la 3e passe, seuls `HeardLength.kt` et `HeardLengthTest.kt` ont changé dans `app`, `project-model` et `engine` ; la fusion de l'intégration (`0ff032ee`, `9cacbbb2`) n'apporte que le banc et des docs. Lecture du code seulement, Gradle non lancé.

## 5e passe (`a032ab20..8868eb9d`)

- **Fusion de l'intégration (exports2) : rien à signaler.** Elle touche la page de partage et `export/` (`ShareSheet.kt`, `ShareSongUi.kt`, `SaveProjectZip.kt`), `ProjectsScreen.kt` et des traductions. Aucun fichier du lot n'a changé : `RhythmViewModel.kt`, `AppRoot.kt`, `CutScreen.kt`, `MixerScreen.kt`, `PlayStrip.kt` et `HeardLength.kt` sont identiques à a032ab20.
- **mineur** — `app/src/test/java/com/pocketstudio/app/ui/Coquille2SeatTest.kt:58` et `:160`. « Autres fichiers » est maintenant atteint par `performScrollTo().performClick()`. C'est juste : exports2 a descendu la ligne sur la page, et `share_other` est toujours là (`ShareSheet.kt:522`). Ce clic par la sémantique contourne toutefois le geste du siège (`tap` aux coordonnées). Comme les deux tests jugent l'écran d'après (E2 et N2), pas l'accès à la ligne, c'est acceptable. `performScrollTo` échoue si la page ne défile pas : ce serait un rouge visible, pas un faux vert.

## Points des passes précédentes

- **A6 dans une boucle (grave)** : **corrigé**. La phrase est juste dans une boucle, et « Allonger » n'accompagne que son propre avis. Le test A6 vérifie la structure créée. Le test N2 vise `export_cancel`. Constaté en 2e passe, toujours vrai.
- **E2 et les mineurs que le chef a notés : acceptés**, comme dit dans la dernière section du rapport.
- **Japonais de `coquille2` : réglé.** La phrase est maintenant au registre poli d'un bout à l'autre (« 曲はループより %s 長く、ループがくり返すあいだも鳴り続けます。 »), et `%s` est gardé.
- **Test de la cymbale : réglé.** `said.isNotEmpty()` empêche que le test passe sans aucun « → » à l'écran.
- **P5, écriture d'état pendant la composition : inchangé, mineur.**

## P6 (3e et 4e passes)

Points vérifiés et justes :
- **Le calcul ne sert qu'à l'affichage.** `heardSceneSteps` n'est appelé que dans `PlayStrip.playPosition` (`PlayStrip.kt:105`) et `RhythmViewModel.barNow` (`:3274`). `playBar` ne sert qu'aux compteurs (`UnderTopBar`, `RecordBar.takeCounter`, `ReportWiring`). La boucle du moteur, `sceneBars` de l'export, `exportEnd` et `SongLength` n'y touchent pas.
- **Le mode morceau ne change pas.** `barNow` sort avant pour `songIndex >= 0`, et la branche morceau de `playPosition` lit `songBars`. Seules les branches « boucle » (`playing` sans morceau, `loopPart`) changent.
- **Les clips de morceau sont exclus** (`clip.song`), et **la brique d'une autre partie aussi** (`belongsTo(scene)`, couvert par `HeardLengthTest`).
- **Le cas du siège tombe juste.** Le moteur relance un clip de partie sur `(local − start) % loopSteps` (`AudioTracks.cpp:166-168`), et `absoluteStep` est le pas depuis le début de la partie (`Engine.cpp:975`). Pour une brique posée par `placeInCell` (début 0, boucle d'une puissance de 2 mesures, `LiveCells.loopBars`) dans une partie d'une mesure, « mesure x/N » suit bien ce qu'on entend.

Constats :

1. **Réglé (4e passe)** — `project-model/src/main/kotlin/com/pocketstudio/model/HeardLength.kt:10-21`. Chaque clip est maintenant compté comme le moteur le joue :
   - la longueur vient de `engineLoopSteps(notes, tempoBpm)` : `loopSteps` pour une boucle, des passages entiers de la partie pour un clip « une fois » (au plus `ENGINE_MAX_LOOP`) ;
   - `belongsTo(scene)` fait compter aussi un clip sans partie ;
   - les clips de morceau restent exclus ;
   - une piste muette, à volume nul ou au gain nul ne compte pas. La voie est lue par `AudioTrack.mixerChannel(t.id)` avec `getOrNull`, ce qui évite un plantage sur une table courte.

   `HeardLengthTest#bricksAreCountedAsTheEngineLoopsThem` vérifie les trois cas (clip de toutes les parties : 4 mesures ; clip « une fois » de 3 mesures dans une partie d'une mesure : 3 mesures ; piste muette : la longueur des notes), et les valeurs attendues correspondent au code. Reste un point mineur, accepté pour de l'affichage : le solo d'une autre piste n'est pas pris en compte. Le moteur n'est pas touché.

2. **mineur** — `RhythmViewModel.kt:3274` (`barNow`). Le modulo sur le maximum n'est juste que si les longueurs se divisent entre elles. Avec une partie de 3 mesures et une brique de 4, le vrai cycle fait 12 mesures, et la brique suit son propre départ (`start`, ou le `localOf(audioSlot)` d'une case lancée en direct), pas forcément le début de la partie. « mesure x/N » peut alors ne pas tomber sur la reprise de la brique. Ça reste de l'affichage, et le cas courant (partie d'une mesure, briques en puissances de 2, début 0) est juste. On peut l'accepter et le noter.

3. **mineur** — `app/src/main/java/com/pocketstudio/app/record/Takes.kt:77-78` (`takeCounter`), une incohérence entre écrans. Le compteur de la prise de notes garde `sceneSteps` : « 1/1 », alors que la barre du haut dit « 3/4 » pendant la même lecture. C'est défendable, la prise de notes boucle sur la partie, mais il faut le savoir pour la recette.

4. **mineur** — `RhythmViewModel.kt:3274` contre `PlayStrip.kt:105`. Le numérateur boucle sur `heardScene ?: p.scene`, le dénominateur sur `p.scene`. Si la partie entendue n'est pas celle affichée, et maintenant que leurs briques peuvent différer, le compteur reste bloqué sur « N/N » à cause de `coerceIn`. Ce problème existait déjà avec `sceneSteps`, P6 le rend seulement plus visible. À faire : lire la même partie des deux côtés.

5. **mineur** — `app/src/test/java/com/pocketstudio/app/ui/Coquille2SeatTest.kt` (`p6_…`). Sous Robolectric, `NativeEngine.isAvailable` est faux : `playBar` vaut -1 et le test ne vérifie que le dénominateur, venu de `playPosition`. `HeardLengthTest` couvre bien la fonction du modèle avec 6, 2 et 5 mesures et la brique d'une autre partie, et depuis la 4e passe aussi le clip « une fois », le clip sans partie et la piste muette. Le clip de morceau reste sans cas dédié, ce qui est sans conséquence vu la condition `clip.song`. Le modulo de `barNow` n'est toujours couvert par aucun test.

Rien qui plante, qui perde une sauvegarde ou qui touche au moteur ou aux exports.

Verdict : 0 bloquant, 0 grave
