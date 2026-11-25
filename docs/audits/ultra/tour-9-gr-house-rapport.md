# Tour 9 : lot « gr-house » (L9 + tech house de L2), rapport

Branche : `eq/tour-9-gr-house` (= `eq/gr-house`). Seul fichier du lot : `scripts/packs/recipes/house.json` (+ une ligne retirée dans `HouseSignatureTest.kt`). `StyleRecipes.kt` régénéré en local seulement, à régénérer à la fusion. Tout est « tenté ».

- Tech house : vieux clap gelé, effets validés gardés, pas de `body:true`, conga/shaker en lit aigu, snaps sur les relances, stabs d'orgue doublés.
- `body:true` remplacé à la main (contre-chant Pluck + doublure d'accords) sur house, deep_house, uk_garage, speed_garage, afro_house, amapiano : le corps forcé ajoutait une montée et dépassait la plage.
- Acid : 303 « acid techno » sur une mesure (accents, glisses, filtre qui bouge), kit 909 et kick « punch ».
- Amapiano : log drum sur une texture de fond, pompé par le kick.
- song-bench 9 styles : aucune limite dure cassée ; Drop − fin de Montée ≥ 2,7 LU. live-bench : 9/9 sans alerte.
- Rouges connus hors lot (L1 / textes / guides) : SongShapeTest:109 (amapiano), SimpleMelodyTest:27/52, StarterWizardTest:121, GuideParcoursTest, I18nCatalogTest, ModelTextsTest.

tests: *HouseSignatureTest *OneTouchStyleTest

## Chemins

chemin: Projets → « Partir d'un style » en une touche, morceau fait | test: OneTouchStyleTest#aModeChosenOnceOneTouchMakesTheSong
chemin: assistant → lecture en direct des styles house | test: LiveSongRecorder#recordsTheLivePlaybackOfEveryStyle
