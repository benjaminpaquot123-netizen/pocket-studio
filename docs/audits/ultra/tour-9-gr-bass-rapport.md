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


## Lot gr-bass2 : du corps, kick et basse devant

Consigne de Benjamin : « du corps ; kick et basse devant ». Fichier : bass.json seul (StyleRecipes.kt et guides régénérés par le chef à la fusion).

- Corps : cloche large vers 120 Hz sur la basse de dubstep, neurofunk, jump_up, riddim (PARAM_EQ).
- Kick : cloche vers 60 Hz sur le kick de dnb, dubstep, pop (PARAM_EQ : un EQ en plateau est interdit sur un pad, GainStagingTest).
- Largeur : accords doublés et contre-chants resserrés (WIDENER 0,25–0,35, grave au centre) ; dubstep : bus_drive 0,4 → 0,2, pompe de basse plus douce.
- Jungle : sans pompe « chords » (interdite, HonestMixTest), son Drop passait sous sa Montée au téléphone ; basse « Sub chaud » + accords resserrés le remettent au-dessus.
- rouge : 2b414710 ; vert : cef3630b. Test : BassRecipesTest#theDropHasBodyWithKickAndBassUpFront.

Banc (song-bench, Drop ; grave 60–250 dB / largeur side−mid dB / montée du kick dB / Drop − fin de Montée au téléphone) :

| style | avant (tour 8) | début du lot (ccf50247) | après |
|---|---|---|---|
| dnb | −6,9 / −7,61 / 3,2 | −4,5 / −7,01 / 2,5 | −4,3 / −9,22 / 3,0 |
| liquid_dnb | −6,3 / −6,75 / 4,7 | −4,6 / −7,74 / 3,6 | inchangé |
| jungle | −4,7 / −9,45 / 4,2 / [1,8 3,4 1,9] | −4,0 / −9,60 / 3,2 / [1,4 2,7 1,2] | −5,0 / −10,03 / 4,2 / [2,3 3,6 2,2] |
| neurofunk | −7,3 / −5,66 / 4,1 | −8,2 / −5,72 / 4,2 | −6,0 / −6,70 / 3,5 |
| jump_up | −8,2 / −6,57 / 4,8 | −8,2 / −6,57 / 4,7 | −5,9 / −8,65 / 5,0 |
| dubstep | −8,8 / −5,18 / 2,2 | −8,2 / −4,38 / 2,1 | −4,7 / −8,30 / 5,7 |
| riddim | −8,4 / −6,35 / 5,4 | −6,9 / −8,24 / 5,0 | −5,9 / −8,52 / 4,8 |
| synthwave | −4,6 / −8,12 / 6,3 | −3,5 / −8,74 / 4,5 | inchangé |
| pop | −4,1 / −9,00 / 3,6 | −3,1 / −9,93 / 2,6 | −3,2 / −10,47 / 3,3 |

Grave ≥ −6 sur les quatre styles visés, kick ≥ +3 sur dnb, dubstep, pop, largeur en baisse partout. Live-bench 9 styles : 9/9 sans alerte (basse au téléphone 300 Hz de −2,1 à −10,9, cible ≥ −14).

Mineurs laissés : B9 pop (−2,7) et riddim (−2,8) ; B8 infra jungle (−19,2) ; sonie plafonnée jump_up, neurofunk, riddim ; riddim Drop − Break +3,6 ; Drop − Groove jungle et synthwave +2,0 (limite) ; jungle perd 1 dB de grave par rapport au début du lot (reste au-dessus de −6) ; B6 0 sur 9 (existait avant).

tests: com.pocketstudio.model.BassRecipesTest com.pocketstudio.model.RecipeFormatV2Test com.pocketstudio.model.HonestMixTest com.pocketstudio.model.RoleMixTest com.pocketstudio.app.bench.LiveSongRecorder

chemin: assistant → morceau en un geste → lecture en direct (9 styles bass) | test: LiveSongRecorder#recordsTheLivePlaybackOfEveryStyle
