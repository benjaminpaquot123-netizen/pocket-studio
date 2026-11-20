Relu : bc5c014bb8903e264186216db3f296d6ccf2b30a
# Relecture neuve — lot tour-9 L15 (eq/tour-9-L15, b72de0c9..d99bbffb)

Périmètre : `git diff b72de0c9 HEAD`. Appli : `tuto/TutoPanel.kt` (ligne du tuto au-dessus de la grille), `tuto/TutoController.kt`
(`swingAim`), `rhythm/RhythmScreen.kt` (en-tête « Pas », bouton vers le pad demandé, Swing « − / + » et « Cible »),
`ui/AppRoot.kt` (une ligne), `project-model/.../DefaultNames.kt` (`shownOnPad`). Tests `Tour9L15*`, `Lot6VmTutoKitNameTest`
(attente changée), 3 captures `nav/`.

## Verdict : OK

Rien sur la sauvegarde, rien sur le son, thread audio et `engine/` non touchés. Pas de plantage trouvé : les deux mises
en page neuves (`HeadBesideChip`, `TutoLineBesideButton`) ne demandent la largeur intrinsèque qu'à du texte (pas de
`SubcomposeLayout` dedans), et les tests à 130 et 200 % les traversent. Textes neufs en français et passés par `tr` ;
« Cible : %s » et « barré : à enlever » ont déjà leurs traductions (I18nCatalogTest vert). « − » et « + » sont des signes,
comme dans PadLoopRow.

## Graves

Aucun.

## Tests existants touchés

- Test existant à l'attente changée : `Lot6VmTutoKitNameTest` (ligne 78-79) attendait « Techno · Kick », il attend
  maintenant `tr("Kick")`. Changement voulu (F132), écrit dans `docs/decisions/tour-9-L15.md` et annoncé au chef ; le test
  reste vert et son intention (le pad que l'étape nomme suit son nom à l'écran, via `tutoPadNames` qui lit `padName`) est
  gardée. Je ne le compte pas comme grave. Aucun autre test du dépôt n'attend « <kit> · <pad> » à travers `padName` ou
  `shownPad` (recherche faite ; seul `DefaultNamesTest` lit `shownSample`, inchangé).

## Moyens restants (une ligne chacun)

- M1 (RhythmScreen.kt, `HeadBesideChip`) : à 100 % aussi, dès que « Pas : <pad> · <partie> » ne tient pas sur une ligne à côté de « Tous les pads », la puce passe dessous : une ligne de plus (~60 px) avant les cases ; sur `rythme_empty_project` (360 dp) la première rangée de cases est maintenant à moitié sous la barre du Guide à l'ouverture (avant : entière). Un geste de défilement, pas un blocage. Piste : n'envoyer la puce dessous que si l'en-tête dépasse deux lignes.
- M2 (TutoController.kt `swingAim` + AppRoot.kt `LaunchedEffect`) : la chaîne étape Swing → `TutoHints.swing` → « Cible : 25 % » n'est pas testée de bout en bout ; `Tour9L15FineTest#f057SwingStepSaysItsAim` pose `TutoHints.swing = 25` à la main. Un test du contrôleur sur une étape `check == "swing"` fermerait le trou.
- M3 (DefaultNames.kt `shownOnPad`) : vérifie le nom de pad du kit (`KIT_PADS`) avant les parties de morceau (`STEMS`), à l'inverse de `shownSample` : un pad « Batterie · Clap » coupé d'un morceau nommé « Clap » s'afficherait « Clap ». Cas rare ; même effet pour un fichier de l'utilisateur nommé « X · Kick ».
- M4 (TutoPanel.kt `TutoGridLine`, `toPad`) : le bouton « <pad> → » passe avant « Copier » et « Mesure n · … » ; correct tant qu'une étape ne demande pas à la fois des cases sur un autre pad et une copie, à garder en tête.

## Ce que j'ai relancé et vu

- `scripts/bench/gradle-file.sh :app:testDebugUnitTest --tests '*Tour9L15*' --tests '*.Lot6VmTutoKitNameTest' --tests '*.TutoBigFontTest'` sur d99bbffb : sortie 0, 0 ligne `e:`, 9 classes, 39 tests, 0 échec, 0 erreur (XML horodatés 2026-11-19 21:06-21:07 UTC) : CapturesTest 9, FineTest 6, GuardTest 4, KitSoundNamesTest 3, PadJumpTest 3, StepHeadTest 3, GridLineTest 6, TutoBigFontTest 4, Lot6VmTutoKitNameTest 1.
- Voisins existants touchés par les mêmes balises ou noms : `TutoScreenTest` (5, utilise `tuto_bar_go`), `Tour6PlaitsWordTest` (1), `:project-model` `DefaultNamesTest` (8) et `I18nCatalogTest` (13) : tous verts, 0 échec.
- Captures regardées en entier : `nav/simple_rhythm.png`, `nav/rythme_empty_project.png`, `nav/rhythm_scrolled_under_part.png`, comparées à celles de b72de0c9 : seul changement, la puce « Tous les pads » sous l'en-tête « PAS : GROSSE CAISSE 1 · INTRO » (une ligne), grille descendue d'autant (voir M1). Pas de texte coupé, pas de chevauchement, le reste de l'écran identique.
- Rejeu du rouge F132 dans une copie jetable (`git worktree add` sur 651a1c81, supprimée après) : `--tests '*.Tour9L15KitSoundNamesTest.f132KitSoundsSayTheSameForm'` : 1 test, 1 échec, `ComparisonFailure: pad screen, Drum & bass · Kick expected:<[]Kick> but was:<[Drum & bass · ]Kick>`. Le même test est vert sur la tête.

rouge constaté : a12c97d0 : rejoué sur 651a1c81 : Tour9L15KitSoundNamesTest#f132KitSoundsSayTheSameForm échoue (« Drum & bass · Kick » au lieu de « Kick »), vert sur d99bbffb

Fusion possible.

Verdict : 0 bloquant, 0 grave
