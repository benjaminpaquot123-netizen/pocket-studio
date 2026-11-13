# Tour 6 — lot « coquille2 » (fichiers partagés `RhythmViewModel.kt` / `AppRoot.kt`) : rapport

Branche `eq/tour-6-coquille2`, depuis la tête poussée `ccr-80af1a1b-mn0wec`, une paire rouge/vert par test. Restes « pas pris » de remix2b (siège B a) et d'exports2 (siège A c), plus P5 de padlive2 (siège A b). Preuves rejouées par le banc dans les conditions du siège d'origine, appli entière (`Coquille2SeatTest`), jamais sur un écran isolé.

Fichiers touchés : `rhythm/RhythmViewModel.kt`, `ui/AppRoot.kt`, `remix/CutScreen.kt`, `mixer/MixerScreen.kt`, `I18n.kt` (aire `coquille2`) et `i18n/<14 langues>/coquille2.json` (accord du chef), `ui/PlayStrip.kt` et `project-model/…/HeardLength.kt` (neuf, accord du chef pour P6) ; tests `ui/Coquille2SeatTest.kt` (neuf), `remix/CutScreenTightTest.kt` (lit `cut_auto` en vue au lieu de défiler jusqu'à lui : il ne défile plus).

tests: com.pocketstudio.app.ui.Coquille2SeatTest com.pocketstudio.app.remix.CutScreenTightTest com.pocketstudio.model.i18n.I18nCatalogTest com.pocketstudio.model.HeardLengthTest

## Tenté, preuve rejouée (banc, conditions du siège)
- **E2** (A c, fr, Simple) : choisir seulement le volume du prochain fichier (« Streaming ») ne fait plus dire « Le morceau a changé depuis cet export ». `#e2_noSongChangeSaidWhenOnlyTheFilesVolumeIsChosen` (rouge 8eace0da, vert 00d8dafd).
- **P5** (A b, fr, Simple) : le projet Live rouvert depuis « Mes projets » s'ouvre sur Pad Live. `#p5_theLiveProjectOpensAgainOnPadLive` (rouge ad354da4, vert c63656f2).
- **A6** (B a, es, Simple, main gauche) : la chanson (0:40) entendue au-delà du projet : la fenêtre le dit ET propose « Alargar el tema » ; le toucher crée une structure qui couvre toute la chanson. Morceau à structure : « su final se cortará » ; boucle (relecture, grave) : phrase juste, « sigue sonando mientras el bucle se repite » (rien n'est coupé). `#a6_aSongLongerThanTheProjectIsSaidWithTheOfferToLengthen` (rouge f77cff89, vert b57a9c7c), puis `#a6_aSongLongerThanTheLoopIsSaidTrulyWithTheOfferToLengthen` (le même test resserré : rouge et vert ci-dessous).
- **Cymbale** (D a, ja, Simple, tuto techno, chap. 12, écran 177 ; demande du chef) : le « → スタジオのバスドラム » au-dessus de la grille de la cymbale porte le nom du pad affiché (« → シンバル »). `#cymbalGridLineNamesThePadShown`. La course (bandeau de l'effacement, puis le pad de l'étape suivante dans la même seconde) est provoquée par le test, les gestes du banc durant ~1,5 s chacun.
- **A4** (B a, geste 19) : en « Frases », « ✂ Frases ▾ » et « En el tiempo ▾ » restent entiers, plus coupés sous le panneau des actions. `#a4_thePhrasesModeMenusStayWholeInView` (rouge db8ecbe3, vert 41035050).
- **N2** (A c, geste 21) : pendant « Fabrication du son » de l'export Pro, « Annuler » est entier, plus sous la barre « Guide ». `#n2_theExportsCancelIsWholeWhileTheSoundIsMade` (rouge 44264af9, vert d5705280).

- **P6** (A b, fr, Simple ; signalé par padlive2) : une prise rangée dans une brique du Live, la partie jouée : « mesure x/4 » (la boucle de la brique), plus « A mesure 1/1 ». `#p6_aTakeInABrickIsCountedAtTheLengthHeard`. Le banc n'a pas de micro (« Pas assez de mémoire pour enregistrer ») : la prise est posée par `placeInCell`, comme « Dans une brique du Live ». Une prise de 6 mesures tourne sur 8 (mesures entières, `LiveCells.loopBars`) : c'est ce qu'on entend. Affichage seulement : boucle du moteur, export et mode morceau inchangés. Même compte que `SongLength.of` de padlive2 (bricks : `loopSteps` de la scène) ; padlive2 n'étant pas encore sur la tête, le test 6/2/5 porte sur ma fonction (`HeardLengthTest`) : à aligner à l'entrée du second.

chemin: Partager › Autres fichiers › Volume visé Streaming (fr, téléphone, 100 %, Mode simple) | test: Coquille2SeatTest#e2_noSongChangeSaidWhenOnlyTheFilesVolumeIsChosen
chemin: Pad Live › Mes projets › autre projet › projet Live (fr, Mode simple) | test: Coquille2SeatTest#p5_theLiveProjectOpensAgainOnPadLive
chemin: Remix › Cortar lo que quiero › voz encendida (es, Mode simple, main gauche) | test: Coquille2SeatTest#a6_aSongLongerThanTheLoopIsSaidTrulyWithTheOfferToLengthen
chemin: Pad Live › prise dans une brique › Plus › Briques › ▶ cette partie (fr, Mode simple) | test: Coquille2SeatTest#p6_aTakeInABrickIsCountedAtTheLengthHeard
chemin: Apprendre › techno › chapitre 12 « トランジション » (ja, Mode simple) | test: Coquille2SeatTest#cymbalGridLineNamesThePadShown
chemin: Remix › Cortar la canción › Frases (es, Mode simple, main gauche) | test: Coquille2SeatTest#a4_thePhrasesModeMenusStayWholeInView
chemin: Partager › Autres fichiers › Exporter (Fabrication du son) (fr, Mode simple) | test: Coquille2SeatTest#n2_theExportsCancelIsWholeWhileTheSoundIsMade

## Pas pris (pour un coquille3)
- **E7** (« Retour » après un export revient d'où je viens) : contredit la règle du propriétaire (2026-10-29, « Mes projets › ← » va à l'accueil) et le lot « l'accueil arrête la musique » ; six tests l'encodent. Tentative annulée avant poussée. À trancher.
- **P1** (« Notes » montre tout ce que je joue) : le cadrage vit dans `synth/PianoRoll.kt` (zone tuto) et la règle « rien ne zoome tout seul » (lot tuto 5) ; la puce de `SynthScreen.kt` ne peut pas cadrer sans lui.
- **A10** (un seul « Descargar el modelo » par panneau) : retirer le bouton de la carte casse `nav/SimpleModeAuditTest` et `audit/BenchContrastRemixLibraryTest` (hors liste) ; retirer celui du pouce casse la portée du pouce gauche (`RemixThumbReachTest`). À autoriser.
- **N1, N4, N5, A11** (phrases du siège A c et B a, numérotées dans la fiche du chef) : au-delà des 8 du lot (consigne du chef). Ces N4 / N5 ne sont pas ceux du tri (`verdicts.tsv`, piano roll du tuto) :
ne corrige pas: N4 : le N4 du tri (flèche « plus bas » du piano roll, zone tuto) n'est pas ce lot ; le N4 de la fiche (« Mes fichiers » vide après un envoi) est laissé pour coquille3
ne corrige pas: N5 : le N5 du tri (mesure dite par la ligne du piano roll, zone tuto) n'est pas ce lot ; le N5 de la fiche (« détails ? » au milieu d'une phrase en Pro) est laissé pour coquille3

## Relecture neuve (1re passe) : suites
- Grave A6 (boucle) : phrase juste dans les 14 langues (accord du chef), test resserré (structure créée et couvrante).
- Moyen : « Allonger » n'accompagne plus que l'avis auquel il appartient ; le test N2 vise `export_cancel`.
- Moyen E2 laissé, voulu : le volume visé règle le prochain fichier (phrase E2 du siège) ; le fichier déjà fait reste partageable. Le cas « vraie modification → a changé » reste couvert par les tests du tour 3 (n° 12).
- Mineurs laissés (noté) : solo / volume d'une voix ne disent rien ; « Allonger » plafonne à 256 mesures ; ordre TalkBack des menus en « Phrases » (sous la courbe, avant le panneau).

## Relecture neuve (3e passe, P6) : suites
- Mineur 1 pris : les briques comptées comme le moteur les joue (clip de toutes les parties, clip « une fois » sur ses passages, piste muette non entendue) ; `HeardLengthTest#bricksAreCountedAsTheEngineLoopsThem`.
- Mineurs laissés (noté) : le compteur boucle sur la plus longue des longueurs, pas leur PPCM (partie de 3 mesures + brique de 4) ; `takeCounter` (`record/Takes.kt`, hors liste) dit encore « 1/1 » pendant une prise ; numérateur sur la partie entendue, dénominateur sur la partie affichée (défaut d'avant) ; le modulo de `barNow` n'est pas couvert sous Robolectric (`playBar` vaut -1).
- À aligner à l'entrée du second (chef) : `SongLength.of` de padlive2 compte `loopSteps` des briques de la partie ; `heardSceneSteps` suit `engineLoopSteps` / `belongsTo` / sourdine : même nombre pour une prise posée dans une brique (6, 2, 5 mesures → 8, 2, 4).
