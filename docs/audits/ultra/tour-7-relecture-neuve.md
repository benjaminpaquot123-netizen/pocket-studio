# Relecture du tour 7 (fx-int, origin/main...HEAD, hors lot fable-repar)

Périmètre : fichiers .kt, .kts, .cpp, .h, .py et .sh. Aucun fichier C++ ni du moteur audio n'a changé. Le callback audio n'est donc pas touché.

## Bloquants

Aucun.

## Graves

Aucun.

## Moyens

### M1. MixerScreen.kt:~236-325 (FitFaders) : l'état de la console est perdu quand `fit` bascule
- **Défaut** : quand `fit` vaut faux, FitFaders appelle `content(fallback, ...)` directement puis fait un `return`. Sinon, elle passe par un `SubcomposeLayout` (slots « low », « full » et « shown »). Les deux branches produisent deux arbres de composition différents. Tout ce qui est mémorisé dans la Console est recréé au changement de branche : `rememberSaveable sheetFor` (la StripSheet ouverte), le défilement et les états `remember`.
- **Scénario** : `fit = !lying && songTracks == null && maxHeight != Infinity`.
  - Le manifeste déclare `configChanges=orientation`, donc l'activité n'est pas recréée à la rotation. Pivoter le téléphone pendant qu'une fiche de piste est ouverte inverse `lying`, change d'arbre et ferme la fiche.
  - Même chose quand un remix attache `songTracks` alors que la console est affichée : la position de défilement et la fiche ouverte sont perdues.
- **Correctif** : passer toujours par le `SubcomposeLayout`, en ne mesurant qu'un seul slot quand `fit` est faux. Autre solution : sortir `sheetFor` et l'état de défilement au-dessus de FitFaders.

### M2. TutoPanel.kt:~600-675 : la carte kit épinglée peut masquer la consigne de l'étape aux grandes tailles de police
- **Défaut** : TutoKitCard est maintenant épinglée sous la colonne de texte. L'ensemble est contraint par `heightIn(min = 52.dp, max = most)`, avec `most` entre 240 et 290 dp. La colonne de texte a `weight(1f, fill = false)` : c'est elle qui cède toute la place. La carte contient un bouton au libellé long (« Télécharger le kit %1$s (%2$s) ») et « Plus tard » sur deux lignes.
- **Scénario** : police système à 200 %, étape qui propose un kit (`kit != null`, pas de démo, pas de bravo). La carte mesure environ 170 dp. Avec Why et MoreWords, le reste dépasse 240 dp, et le texte de l'étape reçoit une hauteur nulle ou quasi nulle. L'élève voit le bouton de téléchargement mais plus la consigne. Tour7TutoKitInSightTest ne vérifie qu'à 100 % et ne contrôle jamais que le texte de l'étape reste visible.
- **Correctif** :
  - Réserver une hauteur minimale au texte de l'étape (`heightIn(min = …)` sur la colonne de texte).
  - Prévoir un libellé compact pour la carte aux grandes polices (par exemple « Télécharger (taille) »).
  - Ajouter un cas à 200 % dans le test, qui vérifie que la consigne est affichée.

### M3. MixerScreen.kt:~250-320 : trois Consoles recomposées à chaque mesure (travail lourd sur le thread principal)
- **Défaut** : les lambdas passées à `subcompose("low")`, `subcompose("full")` et `subcompose("shown")` sont recréées à chaque passe de mesure. `SubcomposeLayout` recompose un slot dès que son lambda change d'instance. Chaque remesure (redimensionnement, animation, changement d'insets) recompose donc deux Consoles sonde invisibles, plus l'AutomationEntry, en plus de la vraie. Les sondes incrémentent aussi `stripCompositions` (`SideEffect` dans ConsoleStrip), ce qui fausse le compteur utilisé par les tests de performance.
- **Scénario** : on ouvre le mixeur avec 16 pistes, puis l'animation d'ouverture d'une fiche ou du clavier provoque des remesures successives. Cela fait trois fois le coût d'une console à chaque image, d'où des saccades visibles sur un téléphone d'entrée de gamme.
- **Correctif** :
  - Mémoriser les lambdas des sondes (`remember { … }` avec des clés stables).
  - Ne mesurer les sondes qu'une fois par changement de contraintes, en mettant en cache le choix low/full selon `maxHeight`.
  - Exclure les sondes du compteur `stripCompositions`.

## Mineurs

### m1. PianoRoll.kt:~243-258 : le mode Accords choisi par l'utilisateur est éteint en fin de tuto
- **Défaut** : le `LaunchedEffect(tutoStep, singleNotes, chordHints, tutoOn)` remet `chordMode = false` quand `!tutoOn && chordSetFor != null`, sans distinguer un mode activé par le tuto d'un mode activé à la main.
- **Scénario** : l'utilisateur active Accords lui-même pendant une étape, puis le tuto se termine ou est fermé. Son réglage disparaît.
- **Correctif** : mémoriser que c'est le tuto qui a allumé Accords (un drapeau), et ne l'éteindre que dans ce cas.

### m2. CutScreen.kt:~121 et ~229 : lecture de `work.value` à 30 Hz au niveau de l'écran
- **Défaut** : `val said = work.value.busy ?: note ?: work.value.message` s'ajoute à une lecture déjà présente de `work.value` au niveau supérieur. Le routeur (StemBoardRouter ~164) publie `playFrame` toutes les 33 ms, donc tout l'écran Cut se recompose pendant la lecture. Le problème existait déjà, mais il est aggravé.
- **Correctif** : `derivedStateOf { work.value.busy ?: work.value.message }`, et isoler la lecture de `playFrame` dans le composant de courbe.

### m3. Tour7Guide2LookTest : aucune assertion
- Ce test ne fait qu'enregistrer une capture. Il passera quel que soit l'affichage.
- **Correctif** : vérifier au moins la présence et la position de la cible du guide, ou le renommer explicitement en capture.

### m4. Tour6TutoFlechesChordsTest : la couverture du toucher manuel sur Accords a été retirée
- Le test ne vérifie plus qu'un toucher manuel allume Accords. Or le nouveau `LaunchedEffect` (m1) écrit justement dans `chordMode` : une régression du toucher manuel ne serait plus détectée.
- **Correctif** : rétablir le cas « toucher Accords, l'état reste allumé tant que l'étape ne change pas ».

### m5. Fichiers JSON des tutos et TutoCourse.stepsFor : les nouveaux identifiants d'étapes font repasser un cours Facile terminé en « non terminé »
- **Défaut** : le découpage Facile ajoute des identifiants (`clap_2`, `bass_end_2`, `pad_end_2`, `lead_2`, `make_drop2_2`, etc.). La progression enregistrée (l'ensemble des étapes faites) ne les contient pas.
- **Scénario** : un élève a fini un cours Facile avant la mise à jour. Après la mise à jour, le cours s'affiche comme inachevé et la reprise le renvoie sur une étape `_2`.
- **Correctif** : à la lecture, considérer une pièce `_N` comme faite si l'étape de base est faite. Sinon, migrer l'ensemble des étapes faites.

### m6. Outils de banc (SeatOptions.reaimTag) : `removePrefix` en double
- Cosmétique : le second appel ne fait rien. On peut le supprimer.

## Points examinés sans défaut retenu
- Moteur audio et callback : aucun changement.
- Sauvegarde : aucun chemin d'écriture modifié dans ce tour.
- AppSpaces.kt (FoldColumn, tuiles Plus compactes), GuidePanel.kt (OpenRow et bringIntoView), TutoController.partMade : rien à signaler.
- TutoChecks : la vérification utilise `stepBass` (mesures de l'étape), alors que les indices (`lineOf`, `need`) utilisent `movedBass` (toute la ligne). C'est cohérent avec les textes de jump_up (`make_drop2` : « les notes en pointillé de la mesure 1 »). Aucun échec traçable, donc non retenu.
- Bench Python (`human-act.py image_allowed`, `test_ultra_bench.py`) : pas de bug réel.
- Les tests Tour7GuideTargetInPanel, Tour7PlusTilesAboveGuide, Tour7MixEntryAboveGuide, CutLeftThumbD2, Tour7TutoChordOn et Tour7TutoKitInSight contiennent de vraies assertions. Limites : un seul format de téléphone pour MixEntry, et 100 % seulement pour KitInSight (voir M2).
