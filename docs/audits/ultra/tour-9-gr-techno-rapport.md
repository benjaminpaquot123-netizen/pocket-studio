# Tour 9 : lot « gr-techno » (L3 + L10), rapport

Branche : `eq/tour-9-gr-techno` (= `eq/gr-techno`). Fichiers du lot : `scripts/packs/recipes/techno.json` et `scripts/packs/recipes/techno_hard.json`. `StyleRecipes.kt` régénéré en local seulement, à régénérer à la fusion. Tout est « tenté ».

- L3 : lead « Oxygène » retiré ; kick et basse devant ; acid techno avec une 303 moderne ; lead solo au Drop entre 8,3 et 18,8 dB sous le kick (gabber 9,6 ; hard techno 18,8 ; trance 8,3).
- L10 : rumble en couches, textures, impacts (crash) écrits dans les recettes, sans Freesound ni audio commité.
- Corps (consigne « du corps ; kick et basse devant ») : une texture tenue sous chaque style (Vent ou Souffle) et les accords doux doublés (top : techno, minimal, trance ; octave : detroit, industrial). Pas de `body:true` (son glue a fait perdre acid techno, son contre-chant ramène un lead). Un second kick a été essayé puis retiré : il creusait le sub et cassait le kick de techno.
- Acidcore (retour de Benjamin, « fort aigu ») : résonance 0,72, ton de la saturation 0,75 → 0,45, −6 dB vers 5 kHz. Piste acide seule au Drop : 4–8 kHz −34,3 → −44,7 dB ; 8–16 kHz −39,8 → −48,1 ; 250 Hz–2 kHz inchangé (grain gardé).
- live-bench : 10/10 sans alerte (basse au téléphone d'industrial techno −15,4, cible ≥ −14, listée).
- song-bench 10 styles contre la tête d'avant le corps : aucune limite dure cassée ; sub à 0,2 dB près (minimal, detroit −0,2), grave médian égal.
- Pas vert, déjà là avant : trance Drop − Break +2,7 ; retro techno Drop − Groove +2,0 ; kick « ≥ 3 dB » listé (techno 2,1, trance 2,9, hard techno 2,7).

tests: *TechnoStylesTest *StyleRecipesUpToDateTest *BodyHeardTest *StyleBodyTest *SongBodyTest *RecipeFormatV2Test *StyleMusicTest *StyleSoundsTest

## Chemins

chemin: Projets → « Partir d'un style » en une touche, morceau techno fait | test: OneTouchStyleTest#aModeChosenOnceOneTouchMakesTheSong
chemin: assistant → lecture en direct des styles techno | test: LiveSongRecorder#recordsTheLivePlaybackOfEveryStyle
