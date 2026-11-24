# Tour 9, grand remplacement v2, lot L11 (gr-bass) : rapport

Basse music et voisins (dnb, liquid_dnb, neurofunk, jungle, jump_up, dubstep, riddim, synthwave, pop) passent au son moderne, avec du corps écrit à la main, kick et basse devant.

Fichier : scripts/packs/recipes/bass.json seul. StyleRecipes.kt et les guides (dnb, dubstep, jungle, neurofunk, pop, riddim, synthwave) sont régénérés en local seulement : le chef les régénère à la fusion.

- Corps : contre-chant « Pluck » et accords doublés à l'octave sur chaque style ; liquid_dnb garde sa « Cloche FM » ; jump_up sans contre-chant ni doublure (sinon il perd son Sub drop et son guide de 40 cartes).
- body: true impossible : MusicTheoryTest (doublure hors tessiture), RoleMixTest (texture à 0, Sub drop de jump_up), HonestMixTest.
- Pompe : « chords » et « 808 » retirés de mix.pump, plus de PUMP propre aux accords (le bus musique les baisse une seule fois).
- Grave rendu : OTT retiré de la basse et du lead ; growl → fat_bass sur dubstep, neurofunk, riddim.
- rouge : eaf546ff ; vert : 4a7aec68 ; relecture : d2538f65 (réverbe des pads synthwave rendue).
- Banc (song-bench Drop, grave 60–250) : dnb −4,5, jungle −4,0, liquid −4,6, synthwave −3,5, pop −3,1 ; dubstep −8,2, neurofunk −8,2, jump_up −8,2, riddim −6,9 (sous la cible −6). Live-bench 9/9 sans alerte.
- A/B : /tmp/claude-0/-home-user-pocket-studio/94c2dbf7-5428-546f-bcdc-51b55439e7f8/scratchpad/ab (avant / après par style).

tests: com.pocketstudio.model.RecipeFormatV2Test com.pocketstudio.model.HonestMixTest com.pocketstudio.model.RoleMixTest com.pocketstudio.app.bench.LiveSongRecorder

chemin: assistant → morceau en un geste → lecture en direct (9 styles bass) | test: LiveSongRecorder#recordsTheLivePlaybackOfEveryStyle
