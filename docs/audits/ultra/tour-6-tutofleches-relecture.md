# Tour 6 — lot « tutofleches » : relecture neuve
Relu : 862b3f7fc4b0482c029ab9aac4044a68c62f16d8

Relecture en lecture seule de `git diff 911c61959 862b3f7fc` : tout le code de l'appli (`PianoRoll.kt`, `TutoController.kt`, `TutoPanel.kt`, `TutoChecks.kt`), les sept fichiers de tests, les 17 `tutos/*.json` et les 42 `i18n/*/tuto*.json` (contrôlés par script : tous se lisent, 15 phrases nouvelles présentes dans les 14 langues, `%d` gardé, aucune restée en français, chaque phrase française nouvelle a sa clé), le rapport et les décisions. Gradle n'a pas été lancé. Aucune expression régulière ajoutée dans le code de l'appli (rien pour ICU). Je ne trouve ni plantage, ni boucle de recomposition (la marge du bas ne change ni la taille du rouleau ni ses rangées visibles), ni étape devenue impossible à valider, ni flèche qui se contredit : la règle unique sert à la ligne et au toucher, avec les mêmes mesures.

Paire « Accords » (`79a966cca` → `862b3f7fc`) : le rouge est vraiment rouge, à la lecture. Le vert n'ajoute que six lignes à `PianoRoll.kt` ; sans elles rien ne touche `chordMode` au changement d'étape, donc `Tour6TutoFlechesChordsTest.kt:70` (« the riff's step begins with « Accords » off ») échoue après le toucher de la ligne 63. Les deux premières assertions passent déjà, et le second test est un témoin vert des deux côtés. Non rejoué.

## Bloquant
Aucun.

## Grave
Aucun.

## Moyen
- **`app/src/main/java/com/pocketstudio/app/tuto/TutoController.kt:438-442` — « Un morceau neuf démarre à %d BPM » est dit pour n'importe quel tempo qui n'est pas celui du style.**
  - Scénario : à l'étape du tempo (house, 122), l'utilisateur monte de 100 à 110 et lâche. La vérification repasse (`:815`) et la note devient « Un morceau neuf démarre à 110 BPM » : faux.
  - Même chose pour un projet déjà réglé ailleurs (128) qui revient sur cette étape.
  - `Tour6TutoPanelTempoNoteTest` ne voit que 100, puis le tempo juste.
- **`app/src/main/java/com/pocketstudio/app/synth/PianoRoll.kt:720` — une demande faite sans note en pointillé reste en attente, sans limite.**
  - Avant le lot elle était consommée (`focusSeen` écrit, puis retour).
  - Scénario 1 : toutes les notes posées, un toucher sur la ligne « Toutes les notes en pointillé sont posées » (toujours cliquable, `:1397`).
  - Scénario 2 : « Copier » sur la grille des charleys ou sur la 303 (`TutoController.kt:533` compte pour tout `TutoNeed.Copy`, pas seulement le rouleau).
  - Effet : à la prochaine étape de notes, le rouleau défile et allume une note sans geste, en même temps que le placement de début d'étape (`:679-707`, `scrollTo` contre `animateScrollTo` sur le même défilement).
  - Aucun test.
- **`PianoRoll.kt:220-221` — « rallumé par l'utilisateur, il reste » ne tient que tant que le rouleau ne bouge pas.**
  - L'effet repart dès que `singleNotes` repasse de faux à vrai, ou que le rouleau revient à l'écran (`chordMode` est restauré allumé, puis l'effet l'éteint).
  - Scénario : étape du riff, l'utilisateur rallume « Accords », regarde une autre piste (plus de pointillés), revient : « Accords » est éteint sans un mot.
  - `Tour6TutoFlechesChordsTest` ne couvre que le cas où l'on reste sur le rouleau.

## Mineur
- **`project-model/src/test/kotlin/com/pocketstudio/model/tuto/Tour6TutoSixteenthsTest.kt` — les deux étapes réparées sortent du test.** Leur texte ne contient plus « en doubles croches » ; `seen >= 2` tient par d'autres étapes. Le test garde la règle, pas la nouvelle phrase. Phrase vérifiée à la main : techno `ox.xox.xox.xox.x` et acidcore `xx.xxx.xxx.xxx.x` laissent bien vides les seuls contretemps.
- **`project-model/src/main/kotlin/com/pocketstudio/model/tuto/TutoChecks.kt:1730-1756` — Montée de 4 mesures : les pistes sont allongées sans être répétées** (`Notes.kt:137`). Une piste gardée de moins de 4 mesures jouerait puis se tairait. Déjà ainsi avant le lot pour les Grooves de 4 mesures ; pas vérifié qu'un tuto soit dans ce cas. L'exemple passe par la même fonction : la validation n'est pas touchée.
- **`TutoChecks.kt:2337-2345` — « plus haut ↑ » sans issue quand le haut du rouleau est coupé par la page** et que la note est sur les toutes premières rangées : la marge ajoutée ne vaut que pour le bas (`PianoRoll.kt:779`, `:828`). Très improbable (notes vers Do8).
- **`TutoController.kt:533` et `AppRoot.kt:2211` — deux demandes pour un seul « Copier »** quand l'étape ouvre ses notes. Sans effet : une seule est servie.
- **`PianoRoll.kt:256` — la double croche descend à 14 dp hors tuto aussi** (téléphones étroits) : cases plus petites sous le doigt. Choix assumé, noté dans les décisions.
- **`TutoChecks.kt:2352-2362` — calage de la mesure au bord gauche à 1 px près.** Si la largeur mesurée fait un pixel de moins que la mesure, le toucher retombe sur l'ancien calage (deux doubles croches avant la note). Pas d'impasse.
- **Rapport — le rouge de la paire « Accords » n'a pas été rejoué**, ni par l'agent de reprise ni ici : établi à la lecture seulement.

Verdict : 0 bloquant, 0 grave — fusion possible
