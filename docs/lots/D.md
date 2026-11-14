# Lot D, Restes (1re partie, sans le moteur) : note de suivi

Branche fx-d (rangée sur fx-int au départ, fx-int refusionnée avant la dernière vérification). La case Remix du Live (moteur) vient plus tard, dans un lot à part.

## Fait (2026-11-03)
- **MIDI vers la 303 pendant l'enregistrement d'une case (n° 9)** : quand une case Acid 303 du Live enregistre, le clavier MIDI joue la 303 (plus le synthé) et ses notes vont dans la case, comme les touches de Live › Notes. `MidiViewModel.keysToAcid` (suivi de `cellRecording` dans `AppRoot`), `RhythmViewModel.midiNote` (section Live recording) envoie vers `acidKeyOn/acidKeyOff`. La case finie, le clavier rejoue le synthé ; une touche tenue au débranchement est relâchée aussi côté 303.
- **Anciens écrans branchés comme le Live (reste du n° 11)** : `T/journey/OldScreensLikeLiveTest` : Rythme (pads du contrôleur dans une prise), Clavier (touches MIDI dans une prise), Acid 303 (potard du contrôleur enregistré en courbe), Structure (partie ajoutée, Start/Stop du contrôleur) : chacun écrit dans le projet, Annuler, Rétablir, sauvegarde → réouverture, et l'export rend la différence (annulé : export identique à avant). Aucun trou trouvé : rien à corriger dans le code.
- **Raccourcis du clavier physique** (`A/ui/KeyShortcuts.kt`, `MainActivity.dispatchKeyEvent`) : espace = ▶/■, Ctrl+Z = Annuler, Ctrl+Maj+Z et Ctrl+Y = Rétablir (Meta = Ctrl, Chromebook) ; ce que l'écran prend d'abord (un texte en cours de frappe) reste à l'écran ; une touche tenue ne répète rien.
- **Taille de fenêtre qui change en pleine lecture** : `configChanges` sur `MainActivity` (orientation, taille, plus petite largeur, disposition, densité, clavier, navigation) + `resizeableActivity` : l'activité est redessinée sur place, jamais recréée ; la musique continue, l'état de chaque écran reste, Compose suit la nouvelle taille.
- **Tablette, largeur « étendue » (≥ 840 dp)** : `U/Adaptive.kt` reçoit les 5 classes de taille de Material (`MEDIUM_FROM`, `EXPANDED_FROM`, `LARGE_FROM`, `EXTRA_LARGE_FROM`, `widthClass`).
  - Live : briques à gauche, pads live à droite dès l'ouverture (onglet Pads dans le panneau, plus de pads en plein écran à cette largeur) ; repliable en rail comme couché.
  - Remix › Découper la chanson : la Structure du projet à droite (`CutWithStructure`, `RemixScreen(structure = …)`) ; dans `AppRoot`, la vue Structure est devenue un lambda partagé `structureView` (Morceau et Remix).
- **Pad Live couché** (à partir de 520 dp de large et plus large que haut) : pads à gauche, pages/pack/défi, effets, « Plus » et « Enregistrer ce que je joue » dans une colonne à droite (environ un tiers, 250 à 480 dp). Debout : inchangé.
- **Dix doigts** : `T/padlive/TenFingersTest` (tablette couchée) : 10 doigts posés en même temps jouent 10 pads à la descente ; un pad « tenir » tenu par un doigt sonne jusqu'à ce que ce doigt se lève, même si les 9 autres tapent.
- **Vieux restes triés** (moins d'une heure) : rien de petit dans mes fichiers ; tout part à l'étage 1 de l'Audit (liste plus bas).

## Tests
- `T/live/LiveMidiJourneyTest.aMidiKeyboardPlaysThe303IntoItsCellRecording` (n° 9 : la 303 sonne, jamais le synthé, notes dans la case, sauvegarde, annulation, retour au synthé).
- `T/journey/OldScreensLikeLiveTest` (4 écrans × prise/annuler/rétablir/sauvegarde/export/MIDI).
- `T/ui/KeyShortcutsTest` (correspondances, AppRoot entier : espace, Ctrl+Z, Ctrl+Maj+Z, Ctrl+Y, répétition ; un champ de texte garde son espace).
- `T/ui/ResizeWhilePlayingTest` (manifeste ; vraie `MainActivity` : téléphone → tablette couchée → demi-écran → pliable → téléphone en pleine lecture : même activité, transport jamais arrêté ni relancé, même projet, barre ↔ rail).
- `T/padlive/TenFingersTest`, `T/ui/TabletSideBySideTest` (Live et découpe côte à côte en étendu ; découpe seule sur téléphone ; 5 classes).
- Captures (regardées) : `app/screenshots/sizes/{compact,medium,expanded,large,xlarge}_{padlive,live,cut_structure}.png` (`T/ui/SizeClassesScreenshotTest`). Réenregistrées car changées : `tablet8/land_live_*` (Galaxy Tab couchée = étendu : panneau à 42 %), `tablet/land_nav_live`, `tabA11/land_nav_live`, `tablet/welcome`, `tablet/land_welcome`, `tablet8/welcome` (carte Pad Live de B3, pas réenregistrées à l'époque).

## Décisions à recopier dans DECISIONS
- 2026-11-03 · Taille de fenêtre : `configChanges` sur `MainActivity` plutôt que la sauvegarde d'état seule. Raison : appli tout Compose qui suit déjà la taille ; rien à reconstruire (gestes en cours, feuilles, défilements), le son ne voit rien. Retour : retirer `configChanges` du manifeste.
- 2026-11-03 · Raccourcis : seulement ce que l'écran n'a pas pris (après `super.dispatchKeyEvent`) ; Ctrl+Y en plus de Ctrl+Maj+Z ; Meta compté comme Ctrl. Retour : `MainActivity.dispatchKeyEvent`.
- 2026-11-03 · Côte à côte à partir de la classe « étendue » de Material (840 dp de large) et d'une hauteur moyenne (480 dp) : un téléphone couché (890 × 400) garde sa grille et son rail, la Galaxy Tab A11 couchée (1006 × 600) a le côte à côte (`U/Adaptive.sideBySide`). Retour : `liveExpanded`, `CutWithStructure`.
- 2026-11-03 · Live en étendu : les pads live s'ouvrent à côté de la grille dès l'arrivée, l'onglet Pads ne passe plus en plein écran à cette largeur. Retour : `expanded` dans `LiveScreen`.
- 2026-11-03 · Pad Live couché dès 520 dp de large : pads à gauche, commandes à droite (la main libre). Retour : `padLiveLying`.
- 2026-11-03 · n° 9 : le clavier MIDI joue la 303 seulement pendant l'enregistrement d'une case Acid (pas sur l'écran Acid 303, qui reste un pas à pas). Retour : `keysToAcid`.
- 2026-11-03 · Lumières des pads du MiniLab : non activées (passage en mode DAW non testable sans l'appareil, docs/MINILAB.md). Retour : sans objet.

## Règle « partout »
- Pads qui ne prennent qu'un doigt à la fois : chaque pad a son propre geste (`pointerInput` par pad) sur Pad Live, Fête, pads live (carte et plein écran), Rythme ; un parent qui capterait les doigts n'existe nulle part ; `TenFingersTest` couvre Pad Live (les autres partagent le même schéma, à étendre à l'étage 1 si besoin).
- Écrans qui se reconstruisaient au changement de taille : un seul endroit (`MainActivity`), couvert par `ResizeWhilePlayingTest`.
- Raccourcis : un seul point d'entrée (`KeyShortcuts.dispatch`), tous les écrans passent par lui.

## Fichiers touchés hors sections strictes (à savoir au rangement)
- `RhythmViewModel.pollLive` et `pollLiveStates` : garde `isNative` au lieu de `isAvailable` (un puits de commandes de test n'a pas `laneTriggers` : plantait la vraie activité en test). Une ligne.
- `AppRoot` : `setBufferBursts` gardé par `isNative` (même raison).
- `remix/RemixScreen.kt` (C, puis D : permis) : paramètre `structure`, `CutWithStructure`.
- Aucun texte nouveau à l'écran : pas de `restes.json` (rien à traduire).

## Reste / pour l'Audit, étage 1
- Vieux restes : jungle encore 7 dB sous l'assistant dans les médiums (REVIEW « Tuto tous styles ») ; basse au-dessus de la Sub (passe-haut du tuto) ; MINEUR 3 (course mode / `follow`), MINEUR 5 (triolet qui déborde, `DrumPattern.sanitized`), MINEUR 7 (pompe), MINEUR 8 (808 du trap) ; APK 228 : cymbale « une seule fois », rangée d'outils du piano roll sous « Partie : A », « → Pad » toujours le premier pad libre, rangée des pistes de Sons qui ne défile pas jusqu'à la piste ouverte, mots « Scie »/« Triangle », « −100 dB » ; sons manquants des fiches `scratchpad/styles/*-synthese.md` (break échantillonné CC0, log drum, orgue M1, stabs, effets rewind/scratch/bande, swing batterie seule…) : gros, moteur ou sons.
- Pistes audio : la prise au micro n'est pas testable ici (Robolectric sans micro) ; le clip est couvert ailleurs.
- Pad Live debout sur 360 dp : « Monté/e » coupé en deux lignes dans un pad étroit, titre « Pad… » (reste de B3).
- Dix doigts testés sur Pad Live seulement.
- Test : dans Robolectric, après un export, `exportJob` garde parfois un travail terminé (le test le remet à zéro) : à faire regarder par E/F (si cela arrivait sur un téléphone, « Exporter » ne ferait plus rien).
- Case Remix du Live : lot suivant (moteur).

## Pour Benjamin (liste du moment 1)
- Sur sa tablette : Live couché (briques et pads côte à côte), Remix › Découper (Structure à droite), Pad Live couché, dix doigts sur les pads.
- Avec un clavier Bluetooth ou USB sur la tablette : espace, Ctrl+Z, Ctrl+Maj+Z.
- Écran partagé ou rotation en pleine lecture : la musique continue, rien ne se ferme.
- MiniLab 3 : enregistrer une case Acid 303 du Live en jouant les touches du MiniLab.
- Lumières des pads du MiniLab : un essai à faire avec lui (mode DAW à activer, risque de laisser le MiniLab dans ce mode si l'appli s'arrête mal) ; à décider après l'essai.
