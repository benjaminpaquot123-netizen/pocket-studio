# Tour 9, grand remplacement v2, lot L1 : rapport

Format de recette v2 (recipes_to_kotlin.py + champs de StarterWizard.Style) et corps par défaut (withBody : doublure des accords au Drop, texture tenue, contre-chant, shaker ; WizardMix.withStyleMix : colle parallèle, saturation du bus, pompes).

- rouge : 5d3ecd8a / 73c335f7 (tests seuls) ; vert : 9d9b1396 / f3c42537 ; relecture : dernier commit.
- Sans les nouveaux champs : StyleRecipes.kt régénéré octet pour octet (37 styles), withBody rend le même objet.
- Banc : song-bench trap/drill/techno contre apres-S7 (LUFS -10,3, TP -1,19, inchangés) ; live-bench 3/3 sans alerte.

tests: com.pocketstudio.model.RecipeFormatV2Test com.pocketstudio.model.StyleBodyTest com.pocketstudio.model.StarterWizardTest com.pocketstudio.app.bench.LiveSongRecorder

chemin: assistant → morceau en un geste → lecture en direct des styles (modèle seul, câblage appli à venir) | test: LiveSongRecorder#recordsTheLivePlaybackOfEveryStyle

Limite : aucun style n'active encore les nouveaux champs ; le câblage appli (sons lib/kit, couches, boucle, chop vocal, son du shaker) est hors lot. Statut : tenté.
