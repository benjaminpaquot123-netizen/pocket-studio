# Relecture du lot « navigation » (tour 5, étape 4, chantier 4) par un relecteur neuf
Relu : 4f9625816539b16780c8c606a4d66dc959ba0203

Atelier `/home/user/eq-tour-5-navigation`, branche `eq/tour-5-navigation`, base fx-int dba28dab, tête de l'auteur a8bc7c92 (20 commits), plus un commit de relecture 9a8dccf5 (test renforcé, aucun code de l'appli touché). Relecteur qui n'a pas écrit ce code. Rien n'est poussé. Gradle uniquement par `scripts/bench/gradle-file.sh`, 8 passages ciblés ; chaque fichier muté remis par chemin (`git checkout -- <fichier>`), `git status` ensuite : seules les lignes « T engine/third_party », « T separation/third_party ».

## En bref

- Lecture : `togglePart` / `toggleWholeSong` font ce que disent leurs mots (relus contre `setLoopPart`, `loopPartOff`, `togglePlay`, `startPlaying`). `namedPlays` donne ■ au bon bouton ; sans morceau les deux disent ■, et chacun arrête.
- Tests ciblés verts sur a8bc7c92 : TwoPlaysNamed* (4 classes), ThumbBarBig* (4 classes), TutoBigFontTest, TabletUprightTest, TopBarTest, GestureBudgetTest, nav.PlayThenTabsTest, KeyActionsGestureTest, HelpLabelsTest, RetoursClearOfControlsTest ; GuideJourneyTest, Zone3Round2Test, Tour5BloquantsTest, CaptureMatrixBigFontTest (6 écrans à 200 %, aucun mot coupé) aussi.
- 14 langues : « cette partie », « le morceau », « Jouer cette partie », « Jouer le morceau », « Arrêter », « Guide », « replié », « Ouvrir le Guide » présents dans les 14 dossiers de `project-model/src/main/resources/i18n/`.
- Aucune capture protégée touchée (recette, cut_screen, cut_screen_magnifier, remix_stem_edit_menu, rhythm_scrolled_under_part) ; aucune réenregistrée par moi.

## Rouges vides (a6db7a52, 36e87014)

Commits vides, car les tests TutoBigFontTest#theNextStepsLineComesIntoSightWithItsCells et TabletUprightTest#theSecondRowOfStepsShowsAt130 échouaient déjà depuis 821bdab1 (même branche). Le juge les accepte : `python3 -I scripts/bench/verifie-chantier.py` sur la tête ne cite aucun commit de la branche (ses 28 écarts sont tous antérieurs, hors lot) ; chaque rouge est fermé par son vert de même sujet (40ea4dba, 0e5bed1b). La branche n'apporte aucune preuve sous `rouges-constates/`, ne retire ni ne change aucun rejeu : aucune ligne « rouge constaté » ni « rejeu retiré/changé » n'est exigée par `fusion-equipes.sh`.

## Mutations

1. `toggleWholeSong`, branche « en lecture » réduite à `togglePlay()` : TwoPlaysNamedTest#eachPlayPlaysWhatItSays rouge (« the song takes over »). Remis.
2. `MESSAGE_ABOVE_BARS` ramené à 72 dp (l'ancien pied du message) : RetoursClearOfControlsTest#theMessageLeavesTheSongsPlayFree rouge (message sur la barre du pouce). Remis.
3. `BIG_FONT` 1,5 → 2,5 : TutoBigFontTest#theNextStepsLineComesIntoSightWithItsCells rouge. Remis.
4. Sondes de trous : `loopPartOff()` retiré du départ de `toggleWholeSong`, puis `togglePart` en lecture réduit à `togglePlay()`. Les deux passaient VERTS sur toute la série ciblée (TwoPlaysNamed*, PlayThenTabs, KeyActionsGesture, GestureBudget, GuideJourney, Zone3Round2, Tour5Bloquants) : « ▶ le morceau » aurait pu rejouer la boucle de la partie (« ⟳ Partie » gardé avec le projet) sans qu'aucun test ne le voie.

## Test trop court, renforcé — **corrigé**

`eachPlayPlaysWhatItSays` annonçait « chacun joue ce qu'il dit » mais ne passait jamais par « ▶ le morceau » avec « ⟳ Partie » resté allumé, ni par « ▶ cette partie » pendant le morceau. Ajouté en 9a8dccf5 (test seul, l'appli était juste) : départ du morceau avec « ⟳ Partie » allumé (`playStartsForTest` = « song … », « ⟳ Partie » éteint), puis « ▶ cette partie » pendant le morceau (la partie prend le relais, ■ en haut, « Jouer le morceau » au pouce), puis ■. Les deux sondes du § Mutations 4 rougissent maintenant (4 classes sur 4), et le test est vert sur le code de l'auteur. **corrigé**

## Captures regardées

tabA11/130_nav_simple_song (pastille « Guide 45 % » au bout de la barre du pouce, entière), tabA11/130_land_nav_simple_keys (tablette couchée : « cette partie » et « le morceau » côte à côte en haut), scale130/fr_nav_simple_rhythm (130 % téléphone, comparée à la base), i18n/de_guide_panel (« dieser Teil », « der Song » entiers, « Rückgängig » / « Wiederholen » entiers), tablet/nav_help_labels, tuto_app_snare (comparée à la base), et à 200 % (build/audit-captures, non commitées) phone360 fr_top_bar et fr_rhythm : « cette partie » entier.

## Moyen (restes, non bloquants pour la fusion)

- M1. À 130 % sur un 411 dp en Mode simple, le nom du projet tombe à « M… » sur la ligne unique (avant : « Mon premier rythme » sur sa propre ligne, scale130/fr_nav_simple_rhythm). Le rapport dit « le nom du projet garde sa place » : la place oui, le nom non. À trancher en design (seuil de `topBarLayout` pour renvoyer le nom sur sa ligne).
- M2. Pendant une prise (●) sur le morceau, « ▶ cette partie » montre ▶ et un toucher passe la partie en boucle au lieu d'arrêter ; l'arrêt est sur « ▶ le morceau ». Cohérent avec 2B, mais non testé pendant une prise.
- M3. Le rejeu `t4-26-prise-clavier` (téléphone, police 200 %, touches en pixels) a été enregistré sans barre du pouce ; elle est là maintenant à 200 %, les touches du bas tombent ailleurs. Son oracle (« absent ») peut tenir pour une mauvaise raison. Non rejoué ici (il demande tout le selftest) : `fusion-equipes.sh` le rejoue de toute façon.
- M4. Bancs encore sur `top_play` au sens « jouer » (BenchTour4, Tour5Bloquants, BenchPlaceAndPadLive, BenchRecette), déjà notés au rapport ; verts.

## Tests à rejouer à la fusion

`*TwoPlaysNamed*` `*ThumbBarBig*` `*TutoBigFontTest` `*TabletUprightTest` `*RetoursClearOfControlsTest` `*.ui.TopBarTest` `*GestureBudgetTest` `*.nav.PlayThenTabsTest` `*KeyActionsGestureTest` `*HelpLabelsTest` `*GuideJourneyTest` `*CaptureMatrixBigFontTest`


## Delta après fusion de fx-int

Relu : commit de synchro DECISIONS 10a18e2c et fusion 4f962581 (fx-int). Seul conflit, `BenchTour1Test#theLiveMetersRead` : version fx-int gardée (contrôle 5.11, compteurs qui baissent quand les pistes baissent) avec `thumb_play` à la place de `top_play`. Aucune assertion perdue : les deux constantes -14,0 et -14,2 de ce côté sont remplacées par les mesures relatives de fx-int ; le test lance le morceau (`playing` vérifié), puis l'arrête (lecture court terme absente, intégrée conservée). Aucun autre fichier de l'appli touché par la résolution.

Verdict : 0 bloquant, 0 grave — fusion possible
