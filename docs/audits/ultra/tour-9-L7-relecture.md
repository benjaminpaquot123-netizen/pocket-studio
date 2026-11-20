# Relecture neuve, tour-9 L7
Relu : 1ad7be09bd3f9777003fda53f96e9c5f359c863b

Diff relu : `git diff origin/ccr-80af1a1b-mn0wec 58fb8b0e` (branche eq/tour-9-L7). Les PNG de captures existent dans le diff ; ils n'ont pas été analysés (consigne).

## Bloquant

Aucun.

## Grave

Aucun.

## Moyen

- **F118, collages successifs renvoyés au milieu de l'écran.** `app/src/main/java/com/pocketstudio/app/synth/PianoRoll.kt:130-135` (`pasteBarFor`) et `:412-425` (usage). Après un collage, `targetBar = pasteBar + b.bars`. Si cette mesure est hors de la vue, `pasteBarFor` ne la garde pas : il prend la mesure du milieu de l'écran. Le bouton affiche alors par exemple « Coller mes. 1 » sur la mesure source, et le collage suivant écrase la source par elle-même : rien ne bouge à l'écran. Avant le lot, toucher « Coller » plusieurs fois remplissait 2, 3, 4… Le test de F118 dans `Tour9RollFinishTest.kt` ne vérifie que la fonction pure, jamais plusieurs collages de suite. Il faut un test « copier la mesure 1, coller trois fois, les mesures 2, 3 et 4 sont remplies », et garder la mesure suivante même quand elle sort de la vue (ou faire défiler jusqu'à elle).
- **F112, KeepHeaderInSight empêche d'atteindre ce qui est sous le rouleau.** `PianoRoll.kt:~985` (`.nestedScroll(KeepHeaderInSight)`) et l'objet `KeepHeaderInSight` en fin de fichier. Toute la poussée vers le haut qui reste après le défilement du rouleau est consommée (`onPostScroll` et `onPostFling` avec `available.y < 0`), donc la page parente ne descend plus. En mode Notes, « Gamme et accords », TranscribeCard et la zone du son sont sous le rouleau (`SynthScreen.kt`, rollHeight). En paysage, le rouleau occupe `maxHeight - 64 dp` : il ne reste presque aucune zone hors du rouleau pour faire défiler la page avec le doigt. Le test F112 vérifie seulement que la page reste à 0, pas qu'on peut encore atteindre les sections du dessous.

## Mineur

- **F118, traduction et rapport.** Le tri demandait de changer le libellé ; le lot a réinterprété la phrase (choix validé par le chef). Le rapport `docs/audits/ultra/tour-9-L7-rapport.md` dit pourtant que le libellé a changé, ce qui est faux (« Coller mes. %d » inchangé, par exemple « Tempel birama %d »).
- **S17, le geste pris près des notes en pointillé bloque le défilement.** `PianoRoll.kt:1076-1098`. Le glissé est capturé dès que le doigt tombe à portée de `tutoTapTarget`. Sur une grille dense en pointillés, faire défiler le rouleau à cet endroit devient impossible pendant le tuto. La variante « beside » de `PianoRollBigTextDottedTest.dragAlongDottedNote` ne vérifie pas que le rouleau n'a pas défilé, ni qu'un vrai défilement reste possible ailleurs.
- **D06, mesure figée.** `PianoRoll.kt` (LaunchedEffect D06 avec `delay(200)`) : `rowPxNow` est lu au lancement de l'effet. Un zoom pendant les 200 ms donne un calcul faux. Le rapport dit lui-même que D06 n'est pas corrigé sur 360 dp à 200 %.
- **F117, la page défile seule.** `SynthScreen.kt:423-445` : `LaunchedEffect(missing)` fait défiler la page pour libérer 110 dp et cache une partie de la barre de pistes. L'effet repart chaque fois que `barTall` change (rotation, changement de taille du texte) et peut ramener la page alors que l'utilisateur l'a remontée.
- **F116, nom du son approché.** `TrackBar.kt` (`trackSoundName`, TURNED_KNOBS = 3) : un synthé neuf réglé sur trois boutons au plus peut recevoir le nom d'un son de la bibliothèque qu'il n'a jamais chargé. La feuille de piste (TrackSheet) et `GuideSheets.soundName` (`project-model/.../guide/GuideSheets.kt:144`, comparaison exacte) gardent l'ancienne règle : le même son peut porter deux noms selon l'écran.
- **F142 hors de la zone du lot.** `finitions.tsv` attribue F142 à L10, mais L7 modifie `SynthScreen.kt` pour lui (`above` = 0 sans enregistrement). Risque de double correction ou de conflit avec L10.
- **Rapport.** Certains hashes de première passe cités dans `tour-9-L7-rapport.md` sont introuvables dans `git log` de la branche (historique réécrit) : la preuve « rouge puis vert » n'est plus vérifiable par ces hashes.

## Vérifié sans défaut

- Moteur C++ : aucun fichier `engine/` dans le diff.
- État enregistré : les collages passent par `NoteEdits.pasteOnBar` puis `paste()`, qui fusionne les doublons ; aucune perte de données dans le projet. `showGhosts` est faux par défaut.
- Note mise en évidence dans un accord (F113, `aimedNotes`) : la note du bas est gardée pour next, aside, brought et lit ; le test le prouve.
- Libellés à taille auto (F115, `RollKey`, `BarRuler` avec `WholeWordsAutoSize`) : testés sur téléphone et tablette.
- Aide du clavier (F117, `keyboardHelp`) : les deux textes existent dans les 14 `home.json`.
- `GestureBudgetTest.aBarIsCopiedAndPastedInThreeGestures` reste compatible avec le nouveau calcul de la mesure visée.

Verdict : 0 bloquant, 0 grave

## Complément (chef, 2026-11-20)
Entre 58fb8b0e et 1ad7be09, seul `TrackBar.kt` change hors captures : la résolution de la fusion avec l'intégration garde à la fois `trackSoundName(t)` (F116, ce lot) et `DefaultNames.soundBeside(shown(t.name), …)` (F133, lot L8). Rien d'autre.

Verdict : 0 bloquant, 0 grave
