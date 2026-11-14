# Audit ultra, tour 2 : intégration

Six équipes ont commité en parallèle par morceaux dans le même index (5c133ee..529f7d0, be9c456). Relecture complète du diff Kotlin du tour (`git diff ecdab74 HEAD`), compilation, suite entière, captures.

## Morceaux mal placés (corrigés)
- `ShareSheet.kt` : la ligne « Volume visé » ouvrait la colonne `share_checks` au mauvais endroit, et l'argument `target =` était après la parenthèse fermante de `ShareSheetUi` (ne compilait pas).
- `RhythmViewModel.kt` : `var reportOk` déclaré dans la boucle de progression du rendu (hors de portée là où il sert) : remonté au début de l'export.
- `ScreenshotTest.kt` : `target =` passé après le `Modifier`, hors de `ShareSheetUi`.
- `AppRoot.kt` : le commentaire « Lot B3 : Pad Live au premier lancement » était resté au-dessus du nouvel effet « voix » ; remis sur son code, l'effet voix a le sien.
- `ThumbBar.kt` : la KDoc de `ThumbButton` séparée de sa fonction par la constante `THUMB_OFF_ALPHA`.
- `SynthScreen.kt` : la KDoc de `NotesView` posée au-dessus de `SOUND_AREA_SPOT`.
- Rien d'autre de déplacé trouvé (instructions, boucles, lambdas) dans les ~3 400 lignes de code principal et ~770 de tests modifiés.

## Tests rouges réparés à la racine
- `RemixFindableTest` (×2) : les cartes « Enregistrer ma voix » et « J'apprends » poussaient le Remix sous le pli d'un 360 × 780 (deux gestes au lieu d'un). Elles passent après Remix, Pad Live et Défis (le Remix « mis en avant » de Benjamin).
- `ScreenScanTest` (accueil, Pro et Simple) : le scanner comptait les textes du studio *sous* l'accueil opaque (`clearAndSetSemantics {}` : ni vus ni lus) et leurs faux contrastes. `LayoutScan.hidden` les ignore ; les deux écrans n'ont plus aucun contraste signalé.
- `SimpleModeAuditTest.theRemixAsksForNotifications` : passe par la nouvelle question « 174 Mo » (rien demandé avant, la notification après « Télécharger »).
- `ViewModelFuzzTest` : `runBlocking { flushNow() }` sur le fil principal bloquait pour toujours quand une sauvegarde automatique tenait le verrou (toute la suite gelée 1 h 30). Le flush tourne sur le fil principal, la boucle le fait avancer.
- Déjà verts à HEAD (corrigés par les équipes) : `I18nCatalogTest`, `NavigationMapTest`, `LaunchRuleTest`, `OnboardingUiTest`.
- `ForegroundSoundTest` : rouge seulement sous la charge de la suite entière, vert seul (connu).

## Défauts vus sur les captures (corrigés)
- Partager en Pro sans cible : « Volume visé : Désactivée » (accord faux, rien de visé) : la ligne ne s'affiche plus sans cible.
- Mode simple à 130 % : « Claqueme nt de ma… » sur un pad (mots du quotidien de la zone 6) : le nom d'un pad peut rétrécir un peu plus (0,6 au lieu de 0,72) pour garder ses mots entiers.

## Résultats
- Compilation : `assembleDebug`, tests unitaires et androidTest, `:project-model:test` : OK ; `assembleRelease` (-PallowDebugSigning, contrôle local) : OK.
- Suite entière, 1er passage : 7 909 tests, 6 rouges (ceux ci-dessus), relancés verts. 2e passage (sous `verifyRoborazziDebug`, après corrections) : 7 909 tests, 80 rouges = 79 captures changées par le tour 2 (réenregistrées) + `ForegroundSoundTest` (charge, vert seul). `:project-model:test` vert.
- Captures : ~215 réenregistrées (accueil, Changer le son, pad coché, volume du pad, prises numérotées, niveau du micro, mots du quotidien, unités du synthé, « Auto (House) », Défis, Guide sous la barre du pouce…), un échantillon de chaque famille regardé (rien de coupé ni de superposé).
- Tests moteur (`host-tests.sh`) : tout vert sauf `master_cpu_budget`, mesure de temps à la limite (2,27 % pour 2 %) ; relancé seul : 1,96 %, vert. Aucun code moteur touché au tour 2.

## Reste
- `master_cpu_budget` (tests moteur) est à la limite sur ce conteneur : vert ou rouge selon la charge.
- En Simple à 130 %, l'en-tête « PAS : GROSSE CAISSE 1 · INTRO » prend trois lignes (il défile, rien de coupé).
