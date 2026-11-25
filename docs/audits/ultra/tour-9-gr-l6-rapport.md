# Tour 9, zone « gr-l6 » (grand remplacement v2, lot L6 : la flûte trap) : rapport

Branche : eq/tour-9-gr-l6, partie de eq/tour-9-gr-styles (8b905fe0) avec eq/recherche-trap-voix fusionnée (e4af8358).

Ce qui change (scripts/packs/instruments.json, scripts/packs/recipes/hiphop.json, StyleRecipes.kt régénéré) :
- Flûte VSCO-2 (vibrato tenu) jouée dès le do du milieu : toute la phrase trap est à la flûte (avant, ses notes graves passaient à la clarinette).
- Notes tenues jusqu'à 6 s : plus de boucle de vibrato audible toutes les 3 s ; attaque adoucie (40 ms) ; instrument « Bois » en version 2 (mise à jour proposée sur le téléphone).
- Trap : nouvelle phrase originale, liée, sombre, qui descend de l'octave à la tonique avec deux silences ; passe-bas vers 7 kHz et plate inchangés ; Cloche FM en secours.
- Drill et phonk vérifiés, inchangés. Variante sifflée et couche sans vibrato : pas faites (voir docs/decisions/tour-9-gr-l6.md).

Sons : aucun son neuf (VSCO-2 CE, CC0, déjà crédité). Aucun audio dans git.

Banc : live-bench n'installe pas les packs (le lead y joue la Cloche FM de secours) ; le niveau flûte contre 808 se mesure sur le téléphone ou après construction du pack par la CI « packs ».

tests: *OneTouchStyleTest *WizardTracksOfferedTest

chemin: Projets → « Partir d'un style » en une touche, morceau trap fait | test: OneTouchStyleTest#aModeChosenOnceOneTouchMakesTheSong
chemin: assistant, pistes proposées d'un style | test: WizardTracksOfferedTest#aSongWithoutTheAcidStepShowsNoAcidTrack

Tests du lot : scripts/packs/tests (TrapFlute : chaque note de la phrase à la flûte, notes tenues ≥ 4 s, version 2, phrase neuve liée), project-model HipHopRecipesTest, RecipeFormatV2Test, StyleBodyTest, GuideParcoursUpToDateTest, FirstListenTest.

Statut : tenté (aucun testeur neuf ne l'a encore constaté).
