# Relecture neuve du lot « courbes » (tour 5, étape 4, chantier 8)
Relu : 883fdb2a2603de5fa8c247ba405ba15c1dcf9e58

Relecteur neuf (ni auteur du code). Lu : `git diff c4743738...HEAD` (AutomationCard, ControlSpot, EffectsBox, EffectsPanel, QuickFx, AppRoot, CurvesFindTest, ScreenshotTest, recensement, 14 `courbes.json`), le rapport et `docs/decisions/tour-5-courbes.md`.

## Constats
- Aucun bloquant, aucun grave.
- Double courbe : impossible par ce chemin. Le choix d'un réglage déjà pris mène à sa courbe (`there != null`, pas d'`add`) et `RhythmViewModel.addAutomation` refuse de toute façon une cible déjà présente. Le seul autre appelant du sélecteur (`TargetPicker` d'export) n'est pas celui-ci.
- Réglage ou effet supprimé : pas de plantage. `isSpot` ne trouve rien, aucun encadré ; `InsertChain.open` sur un emplacement vide n'est lu que dans `used.forEach` ; `fxSpot` filtré par `it.ch == fxChannel`, la feuille ne s'ouvre pas si la piste n'existe plus (comportement d'avant).
- Lecture, rotation, retour : `spot`/`awaiting`/`fxSpot` sont en `remember` : perdus à la rotation ou en quittant Mix (rien ne se déclenche à tort, l'encadré disparaît simplement). En lecture, les courbes ne changent pas (égalité de données) : pas de défilement ; en enregistrement, la courbe touchée perd son encadré, c'est le « premier trait » voulu (mineur).
- Défilement inattendu (mineur) : une courbe encadrée reste encadrée tant qu'on ne la touche pas ; supprimer une courbe placée au-dessus change son index, et `LaneBlock` (non indexé par clé) relance `bringIntoView` : l'écran revient sur la courbe encadrée juste après une suppression. Même schéma qu'avant avec `focusLane` de « Ce qui joue ».
- `awaiting` jamais vidé si `add` est refusé pour une cible invalide (mineur) : une courbe revenue ensuite (annuler) serait dite « Nouvelle courbe ». « Automatiser un réglage » est déjà désactivé au plafond de courbes, seul cas courant.
- Attente de 300 ms de `SpotFrame` : dans les tests, horloge virtuelle (`mainClock.advanceTimeBy`, 3 × 500 ms), donc déterministe, pas de dépendance au temps réel. Sur appareil, la feuille est disposée bien avant ; un appareil très lent ferait au pire ouvrir la feuille sans défiler (mineur, non essayé sur appareil). `runCatching` autour de `bringIntoView` avale aussi une annulation : sans effet ici, la coroutine se termine.
- Feuille ouverte depuis le tuto ET « Voir le réglage » sur le même envoi : l'encadré de la courbe remplace le pointillé `tuto_send_*` (`else if`) (mineur, cas improbable).
- Contraste après retrait du fond ambre : « Vider » mesuré sur `automation.png`, 4,43:1 avant, 5,03:1 après (orange #FF9F43 sur #3D4148). Palettes toutes sombres ; ambre sur `SurfaceHigh` du sélecteur bien au-dessus de 4,5:1. `ScreenScanTest` vert (307 tests, 3 sautés).
- Captures regardées en entier : `automation_new_curve`, `automation_picker_has_curve`, `effects_spot_send`, `effects_spot_macro`, `automation` (avant/après découpé : seule la zone de la courbe encadrée change, fond retiré), `tablet8/automation` (différence limitée à la même zone ; la courbe du bas coupée par la hauteur de capture, déjà ainsi avant). Aucune capture 130 % ni langue longue de ces écrans n'existe (moyen restant : non vu en allemand ni à 130 % ; les textes passent à la ligne, aucun `maxLines`). Rien réenregistré par moi.
- Tests non affaiblis : aucun test existant modifié ; 3 lignes HELP ajoutées au recensement (pas de CONTRAST). CurvesFindTest : 7 verts sur la tête.
- Mutation 1 : clé `asked` retirée de `LaunchedEffect` de `LaneBlock` : `pickedAgainTheCurveComesBackIntoView` rougit (ligne 125).
- Mutation 2 : `bringIntoView` retiré de `SpotFrame` : `seeTheSettingOpensTheEffectsOnTheSend` rougit (ligne 172), 3 échecs au total sur 7. Fichiers remis, arbre propre.

## Tests à rejouer
`*CurvesFindTest`, `*ScreenScanTest`, `*ScreenshotTest` (automation, automationNewCurve, automationPickerHasCurve, effectsSpotSend, effectsSpotMacro), `*I18nCatalogTest`, `*TutoLot4Test`.

## Delta après fusion de fx-int
Le merge 4617e1d9 (lots access, banc7, socle, remix) n'a qu'un conflit, résolu à la main dans `I18n.kt` : la liste des fichiers de textes garde les deux lignes « courbes » et « access ».
Rien d'autre du lot n'a bougé depuis 965c9b6d hors apports de fx-int ; tests ciblés verts après le merge.

Verdict : 0 bloquant, 0 grave — fusion possible
